# 31a. API Rate Limiting & Throttling Strategy

**Last Updated**: January 6, 2026  
**Purpose**: Define rate limits, throttling strategies, and quota management for all cloud providers

---

## Overview

Cloud providers enforce API rate limits to prevent abuse. Exceeding these limits results in HTTP 429 errors and service degradation. This document establishes proactive strategies to stay within limits.

**Key Principles**:
- **Respect Limits**: Never exceed provider quotas
- **Cache Aggressively**: Minimize redundant API calls
- **Fail Gracefully**: Show user-friendly messages on quota exhaustion
- **Monitor Usage**: Track consumption to predict limits

---

## Cloud Provider Rate Limits

### Google Drive API

**Official Limits** (per user per 100 seconds):
- **Queries**: 1,000 requests
- **Uploads**: 200 requests  
- **Downloads**: No explicit limit, but subject to bandwidth throttling

**Per-Project Quotas** (default):
- 1,000,000,000 queries per day
- 10,000 queries per 100 seconds per user

**Recommended Limits (Our Implementation)**:
- Max 100 requests per minute per user
- Batch operations when possible (up to 100 items)
- Cache metadata for 5 minutes

**Sources**:
- [Drive API Usage Limits](https://developers.google.com/drive/api/guides/limits)

---

### OneDrive / Microsoft Graph API

**Official Limits**:
- **General**: 10,000 requests per 10 minutes per app
- **Per User**: 2,000 requests per second (burst), sustained at lower rate
- **Throttling**: HTTP 429 with `Retry-After` header

**Recommended Limits (Our Implementation)**:
- Max 150 requests per minute per user
- Respect `Retry-After` header strictly
- Cache metadata for 5 minutes

**Sources**:
- [Microsoft Graph Throttling Guidance](https://learn.microsoft.com/en-us/graph/throttling)

---

### Dropbox API

**Official Limits**:
- **Standard**: ~200 requests per minute per app
- **Uploads**: ~20 GB per day per app (free tier)
- **No per-user limits** (shared app quota)

**Throttling Behavior**:
- HTTP 429 with `Retry-After` header (seconds)
- Exponential backoff recommended

**Recommended Limits (Our Implementation)**:
- Max 100 requests per minute (conservative)
- Batch operations with `files/list_folder/continue` for pagination
- Cache metadata for 5 minutes

**Sources**:
- [Dropbox API Rate Limits](https://www.dropbox.com/developers/reference/rate-limiting)

---

## Exponential Backoff Strategy

### Algorithm

```kotlin
class ExponentialBackoff(
    private val baseDelayMs: Long = 1000,  // 1 second
    private val maxDelayMs: Long = 60000,  // 60 seconds
    private val maxRetries: Int = 5,
    private val jitterFactor: Double = 0.2  // ±20% randomization
) {
    
    suspend fun <T> executeWithBackoff(
        operation: suspend () -> Result<T>
    ): Result<T> {
        var attempt = 0
        
        while (attempt < maxRetries) {
            when (val result = operation()) {
                is Result.Success -> return result
                is Result.Error -> {
                    val exception = result.exception
                    
                    // Check if retryable
                    if (!isRetryable(exception)) {
                        return result
                    }
                    
                    // Calculate delay
                    val delayMs = calculateDelay(attempt, exception)
                    Timber.d("Rate limited. Retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                    
                    delay(delayMs)
                    attempt++
                }
            }
        }
        
        return Result.Error(MaxRetriesExceededException("Failed after $maxRetries retries"))
    }
    
    private fun calculateDelay(attempt: Int, exception: Throwable): Long {
        // Check for Retry-After header (HTTP 429)
        val retryAfter = extractRetryAfter(exception)
        if (retryAfter != null) {
            return retryAfter
        }
        
        // Exponential backoff: 2^attempt * baseDelay
        val exponentialDelay = (baseDelayMs * (1 shl attempt)).coerceAtMost(maxDelayMs)
        
        // Add jitter to prevent thundering herd
        val jitter = (exponentialDelay * jitterFactor * (Random.nextDouble() - 0.5)).toLong()
        
        return exponentialDelay + jitter
    }
    
    private fun isRetryable(exception: Throwable): Boolean {
        return when (exception) {
            is HttpException -> exception.code() == 429 || exception.code() in 500..599
            is IOException -> true  // Network errors
            is CloudRateLimitException -> true
            else -> false
        }
    }
    
    private fun extractRetryAfter(exception: Throwable): Long? {
        if (exception is HttpException) {
            val retryAfter = exception.response()?.headers()?.get("Retry-After")
            return retryAfter?.toLongOrNull()?.times(1000)  // Convert seconds to ms
        }
        return null
    }
}
```

**Usage**:
```kotlin
val backoff = ExponentialBackoff()
val result = backoff.executeWithBackoff {
    googleDriveClient.listFiles(folderId)
}
```

---

## Request Throttling Implementation

### Token Bucket Algorithm

```kotlin
class RequestThrottler(
    private val maxRequestsPerMinute: Int,
    private val provider: CloudProvider
) {
    private val semaphore = Semaphore(maxRequestsPerMinute)
    private val refillScheduler = CoroutineScope(Dispatchers.Default)
    
    init {
        startRefillScheduler()
    }
    
    suspend fun <T> throttle(operation: suspend () -> T): T {
        semaphore.acquire()
        try {
            return operation()
        } finally {
            // Token released by scheduler
        }
    }
    
    private fun startRefillScheduler() {
        refillScheduler.launch {
            while (isActive) {
                delay(60_000 / maxRequestsPerMinute)  // Refill interval
                semaphore.release()
            }
        }
    }
}
```

### Per-Provider Throttlers

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ThrottlingModule {
    
    @Provides
    @Singleton
    @Named("GoogleDriveThrottler")
    fun provideGoogleDriveThrottler(): RequestThrottler {
        return RequestThrottler(
            maxRequestsPerMinute = 100,
            provider = CloudProvider.GOOGLE_DRIVE
        )
    }
    
    @Provides
    @Singleton
    @Named("OneDriveThrottler")
    fun provideOneDriveThrottler(): RequestThrottler {
        return RequestThrottler(
            maxRequestsPerMinute = 150,
            provider = CloudProvider.ONEDRIVE
        )
    }
    
    @Provides
    @Singleton
    @Named("DropboxThrottler")
    fun provideDropboxThrottler(): RequestThrottler {
        return RequestThrottler(
            maxRequestsPerMinute = 100,
            provider = CloudProvider.DROPBOX
        )
    }
}
```

---

## Metadata Caching Strategy

### Cache Configuration

**Glide Disk Cache**: Already configured (2GB)  
**Metadata Cache**: Separate in-memory + Room persistence

```kotlin
@Entity(tableName = "cloud_metadata_cache")
data class CloudMetadataCacheEntity(
    @PrimaryKey
    val cacheKey: String,  // "${provider}_${folderId}"
    val metadata: String,  // JSON serialized
    val cachedAt: Long,
    val expiresAt: Long = cachedAt + CACHE_TTL_MS
) {
    companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 minutes
    }
}

@Dao
interface CloudMetadataCacheDao {
    @Query("SELECT * FROM cloud_metadata_cache WHERE cacheKey = :key AND expiresAt > :now")
    suspend fun getCached(key: String, now: Long = System.currentTimeMillis()): CloudMetadataCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cache(entry: CloudMetadataCacheEntity)
    
    @Query("DELETE FROM cloud_metadata_cache WHERE expiresAt < :now")
    suspend fun clearExpired(now: Long = System.currentTimeMillis())
}
```

### Cache-First Strategy

```kotlin
class CloudMetadataRepository @Inject constructor(
    private val cacheDao: CloudMetadataCacheDao,
    private val googleDriveClient: GoogleDriveClient,
    private val backoff: ExponentialBackoff
) {
    
    suspend fun getFiles(provider: CloudProvider, folderId: String): Result<List<CloudFile>> {
        val cacheKey = "${provider.name}_$folderId"
        
        // Try cache first
        val cached = cacheDao.getCached(cacheKey)
        if (cached != null) {
            Timber.d("Cache hit: $cacheKey")
            return Result.Success(deserialize(cached.metadata))
        }
        
        // Cache miss: fetch from API
        Timber.d("Cache miss: $cacheKey, fetching from API...")
        return backoff.executeWithBackoff {
            when (provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    googleDriveClient.listFiles(folderId).also { result ->
                        if (result is Result.Success) {
                            cacheDao.cache(
                                CloudMetadataCacheEntity(
                                    cacheKey = cacheKey,
                                    metadata = serialize(result.data),
                                    cachedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                // Similar for OneDrive, Dropbox
            }
        }
    }
}
```

---

## Quota Tracking & Monitoring

### Quota Usage Tracker

```kotlin
@Entity(tableName = "api_quota_usage")
data class ApiQuotaUsageEntity(
    @PrimaryKey
    val provider: CloudProvider,
    val requestCount: Int,
    val windowStartMs: Long,
    val windowEndMs: Long
)

class QuotaTracker @Inject constructor(
    private val quotaDao: ApiQuotaUsageDao
) {
    
    suspend fun recordRequest(provider: CloudProvider) {
        val now = System.currentTimeMillis()
        val windowStart = now - WINDOW_SIZE_MS
        
        val usage = quotaDao.getUsage(provider, windowStart) ?: ApiQuotaUsageEntity(
            provider = provider,
            requestCount = 0,
            windowStartMs = windowStart,
            windowEndMs = now
        )
        
        quotaDao.update(usage.copy(requestCount = usage.requestCount + 1))
    }
    
    suspend fun isWithinQuota(provider: CloudProvider): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - WINDOW_SIZE_MS
        val usage = quotaDao.getUsage(provider, windowStart)
        
        val limit = when (provider) {
            CloudProvider.GOOGLE_DRIVE -> 100 * 60  // 100 req/min
            CloudProvider.ONEDRIVE -> 150 * 60
            CloudProvider.DROPBOX -> 100 * 60
        }
        
        return usage == null || usage.requestCount < limit
    }
    
    companion object {
        const val WINDOW_SIZE_MS = 60 * 60 * 1000L  // 1 hour
    }
}
```

### User-Facing Quota Warning

```kotlin
sealed class CloudQuotaEvent {
    data class NearingLimit(val provider: CloudProvider, val percentUsed: Int) : CloudQuotaEvent()
    data class LimitExceeded(val provider: CloudProvider, val retryAfterMs: Long) : CloudQuotaEvent()
}

class CloudOperationUseCase @Inject constructor(
    private val quotaTracker: QuotaTracker,
    private val throttler: RequestThrottler
) {
    
    suspend fun execute(provider: CloudProvider, operation: suspend () -> Result<T>): Result<T> {
        // Check quota before operation
        if (!quotaTracker.isWithinQuota(provider)) {
            return Result.Error(QuotaExceededException(provider))
        }
        
        // Throttle request
        return throttler.throttle {
            quotaTracker.recordRequest(provider)
            operation()
        }
    }
}
```

---

## Batch Operations Strategy

### Google Drive Batch Requests

```kotlin
class GoogleDriveBatchOperations @Inject constructor(
    private val driveService: Drive
) {
    
    suspend fun batchListFiles(folderIds: List<String>): Result<Map<String, List<CloudFile>>> {
        val batch = driveService.batch()
        val results = mutableMapOf<String, List<CloudFile>>()
        
        folderIds.chunked(100).forEach { chunk ->  // Max 100 per batch
            chunk.forEach { folderId ->
                val request = driveService.files().list()
                    .setQ("'$folderId' in parents")
                    .setFields("files(id, name, size, modifiedTime)")
                
                batch.queue(request, null, null) { files, error ->
                    if (error == null) {
                        results[folderId] = files.files.map { /* convert */ }
                    }
                }
            }
            
            batch.execute()
        }
        
        return Result.Success(results)
    }
}
```

---

## Error Messages for Users

```kotlin
fun CloudQuotaEvent.toUserMessage(context: Context): String {
    return when (this) {
        is CloudQuotaEvent.NearingLimit -> {
            context.getString(
                R.string.cloud_quota_warning,
                provider.displayName,
                percentUsed
            )
        }
        is CloudQuotaEvent.LimitExceeded -> {
            val retryMinutes = (retryAfterMs / 60000).toInt()
            context.getString(
                R.string.cloud_quota_exceeded,
                provider.displayName,
                retryMinutes
            )
        }
    }
}
```

**strings.xml**:
```xml
<string name="cloud_quota_warning">%1$s quota usage: %2$d%%. Please reduce operations.</string>
<string name="cloud_quota_exceeded">%1$s rate limit exceeded. Please try again in %2$d minutes.</string>
```

---

## Testing Strategy

### Simulate Rate Limiting

```kotlin
@Test
fun `exponential backoff retries on HTTP 429`() = runTest {
    var callCount = 0
    val operation = suspend {
        callCount++
        if (callCount < 3) {
            Result.Error(HttpException(Response.error<Any>(429, null)))
        } else {
            Result.Success("Success")
        }
    }
    
    val backoff = ExponentialBackoff(baseDelayMs = 100)
    val result = backoff.executeWithBackoff { operation() }
    
    assertTrue(result is Result.Success)
    assertEquals(3, callCount)
}
```

---

## References

- [25_implementation_roadmap.md](25_implementation_roadmap.md) - Epic 5 Cloud Integration
- [29_error_handling.md](29_error_handling.md) - Error hierarchy
- [31_security_requirements.md](31_security_requirements.md) - OAuth tokens

---

**Implementation Priority**: Must implement before Epic 5 (Cloud Integration)
