# 29. Error Handling Strategy

**Last Updated**: January 6, 2026  
**Purpose**: Comprehensive error handling strategy for FastMediaSorter v2.

This document defines exception hierarchy, retry policies, user-facing messaging, logging strategies, and recovery mechanisms.

---

## Overview

Robust error handling is critical for a media management app dealing with:
- **Local file system**: Permission denials, disk full, corrupted files
- **Network protocols**: SMB/SFTP/FTP timeouts, authentication failures, connection drops
- **Cloud storage**: OAuth token expiration, quota exceeded, rate limiting
- **Media processing**: Codec errors, OutOfMemory, corrupted media

### Core Principles

1. **Fail Fast**: Detect errors early, propagate clearly
2. **User-Friendly**: Translate technical errors to actionable messages
3. **Recoverable**: Retry transient failures automatically
4. **Debuggable**: Log everything with proper context
5. **Non-Blocking**: Never crash the app; graceful degradation

---

## Table of Contents

1. [Exception Hierarchy](#1-exception-hierarchy)
2. [Result Types](#2-result-types)
3. [Retry Policies](#3-retry-policies)
4. [User-Facing Messages](#4-user-facing-messages)
5. [Logging Strategy](#5-logging-strategy)
6. [Recovery Mechanisms](#6-recovery-mechanisms)
7. [Protocol-Specific Errors](#7-protocol-specific-errors)
8. [Testing Error Scenarios](#8-testing-error-scenarios)

---

## 1. Exception Hierarchy

### Base Exception Classes

```kotlin
/**
 * Base exception for all FastMediaSorter errors
 */
sealed class FmsException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * User-facing error message (localized)
     */
    abstract val userMessage: String
    
    /**
     * Whether this error can be retried
     */
    abstract val isRetriable: Boolean
    
    /**
     * Logging severity
     */
    abstract val severity: LogSeverity
}

enum class LogSeverity {
    DEBUG,   // Verbose info (not errors)
    INFO,    // Normal operations
    WARN,    // Recoverable issues
    ERROR,   // Failures requiring attention
    FATAL    // Critical errors (app crash imminent)
}
```

---

### Storage Exceptions

```kotlin
sealed class StorageException(
    message: String,
    cause: Throwable? = null
) : FmsException(message, cause) {
    
    override val severity = LogSeverity.ERROR
}

/**
 * Permission denied (READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE)
 */
data class PermissionDeniedException(
    val permission: String,
    val path: String? = null
) : StorageException("Permission denied: $permission for path $path") {
    override val userMessage = "Permission denied. Grant storage access in Settings."
    override val isRetriable = false // User must grant permission
}

/**
 * File/folder not found
 */
data class FileNotFoundException(
    val path: String
) : StorageException("File not found: $path") {
    override val userMessage = "File not found. It may have been deleted."
    override val isRetriable = false
}

/**
 * Disk full (no space left on device)
 */
data class DiskFullException(
    val requiredBytes: Long,
    val availableBytes: Long
) : StorageException("Disk full: need $requiredBytes bytes, have $availableBytes") {
    override val userMessage = "Not enough storage space. Free up ${(requiredBytes - availableBytes) / 1024 / 1024} MB."
    override val isRetriable = false
}

/**
 * Corrupted or unsupported file format
 */
data class InvalidFileException(
    val path: String,
    val reason: String
) : StorageException("Invalid file: $path ($reason)") {
    override val userMessage = "File is corrupted or unsupported format."
    override val isRetriable = false
}
```

---

### Network Exceptions

```kotlin
sealed class NetworkException(
    message: String,
    cause: Throwable? = null
) : FmsException(message, cause) {
    
    override val severity = LogSeverity.WARN // Network failures often transient
}

/**
 * Connection timeout (SMB/SFTP/FTP/HTTP)
 */
data class ConnectionTimeoutException(
    val host: String,
    val port: Int,
    val timeoutMs: Long
) : NetworkException("Connection timeout: $host:$port after ${timeoutMs}ms") {
    override val userMessage = "Connection timed out. Check network and retry."
    override val isRetriable = true
}

/**
 * Authentication failed (wrong credentials)
 */
data class AuthenticationException(
    val protocol: String, // "SMB", "SFTP", "FTP", "Google Drive"
    val username: String?
) : NetworkException("Authentication failed for $protocol (user: $username)") {
    override val userMessage = "Login failed. Check username/password."
    override val isRetriable = false // Wrong credentials
}

/**
 * Server unreachable (DNS failure, network down)
 */
data class ServerUnreachableException(
    val host: String
) : NetworkException("Server unreachable: $host") {
    override val userMessage = "Server unreachable. Check network connection."
    override val isRetriable = true
}

/**
 * Connection lost during operation
 */
data class ConnectionLostException(
    val protocol: String
) : NetworkException("Connection lost: $protocol") {
    override val userMessage = "Connection lost. Operation aborted."
    override val isRetriable = true
}
```

---

### Cloud Storage Exceptions

```kotlin
sealed class CloudException(
    message: String,
    cause: Throwable? = null
) : FmsException(message, cause) {
    
    override val severity = LogSeverity.ERROR
}

/**
 * OAuth token expired or revoked
 */
data class TokenExpiredException(
    val provider: String // "Google Drive", "OneDrive", "Dropbox"
) : CloudException("Token expired for $provider") {
    override val userMessage = "Session expired. Please sign in again."
    override val isRetriable = false // Requires re-authentication
}

/**
 * Quota exceeded (storage full)
 */
data class QuotaExceededException(
    val provider: String,
    val limitBytes: Long
) : CloudException("Quota exceeded for $provider (limit: $limitBytes bytes)") {
    override val userMessage = "Cloud storage quota exceeded. Free up space or upgrade plan."
    override val isRetriable = false
}

/**
 * Rate limiting (too many requests)
 */
data class RateLimitException(
    val provider: String,
    val retryAfterSeconds: Long?
) : CloudException("Rate limit exceeded for $provider") {
    override val userMessage = "Too many requests. Try again in ${retryAfterSeconds ?: 60} seconds."
    override val isRetriable = true
}

/**
 * File not found in cloud (deleted or moved)
 */
data class CloudFileNotFoundException(
    val provider: String,
    val fileId: String
) : CloudException("File not found: $fileId in $provider") {
    override val userMessage = "File not found in cloud storage."
    override val isRetriable = false
}
```

---

### Media Processing Exceptions

```kotlin
sealed class MediaException(
    message: String,
    cause: Throwable? = null
) : FmsException(message, cause) {
    
    override val severity = LogSeverity.ERROR
}

/**
 * Video codec not supported
 */
data class UnsupportedCodecException(
    val codecName: String,
    val filePath: String
) : MediaException("Unsupported codec: $codecName in $filePath") {
    override val userMessage = "Video codec not supported on this device."
    override val isRetriable = false
}

/**
 * Out of memory during image/video processing
 */
data class OutOfMemoryException(
    val operation: String, // "decode", "scale", "rotate"
    val fileSizeMb: Int
) : MediaException("Out of memory during $operation (file size: ${fileSizeMb}MB)") {
    override val userMessage = "File too large to process. Try a smaller file."
    override val isRetriable = false
}

/**
 * Corrupted media file (cannot decode)
 */
data class CorruptedMediaException(
    val filePath: String,
    val mediaType: String // "image", "video", "audio"
) : MediaException("Corrupted $mediaType: $filePath") {
    override val userMessage = "Media file is corrupted or damaged."
    override val isRetriable = false
}
```

---

### Database Exceptions

```kotlin
sealed class DatabaseException(
    message: String,
    cause: Throwable? = null
) : FmsException(message, cause) {
    
    override val severity = LogSeverity.FATAL // DB errors are critical
}

/**
 * Database migration failed
 */
data class MigrationException(
    val fromVersion: Int,
    val toVersion: Int,
    override val cause: Throwable
) : DatabaseException("Migration failed: $fromVersion → $toVersion", cause) {
    override val userMessage = "App update failed. Try reinstalling."
    override val isRetriable = false
}

/**
 * Unique constraint violation
 */
data class DuplicateResourceException(
    val resourcePath: String
) : DatabaseException("Duplicate resource: $resourcePath") {
    override val userMessage = "Resource already exists."
    override val isRetriable = false
    override val severity = LogSeverity.WARN // Not critical
}
```

---

## 2. Result Types

### Standard Result Type

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    
    data class Failure(
        val exception: FmsException,
        val message: String = exception.userMessage
    ) : Result<Nothing>()
    
    // Convenience methods
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    fun getOrElse(default: T): T = when (this) {
        is Success -> data
        is Failure -> default
    }
    
    fun exceptionOrNull(): FmsException? = when (this) {
        is Success -> null
        is Failure -> exception
    }
}

// Extension functions
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (FmsException) -> Unit): Result<T> {
    if (this is Result.Failure) action(exception)
    return this
}
```

---

### Protocol-Specific Result Types

```kotlin
// For SMB operations
sealed class SmbResult<out T> {
    data class Success<T>(val data: T) : SmbResult<T>()
    
    sealed class Failure : SmbResult<Nothing>() {
        data class AuthenticationFailed(val username: String) : Failure()
        data class ConnectionTimeout(val host: String, val timeoutMs: Long) : Failure()
        data class ServerUnreachable(val host: String) : Failure()
        data class PathNotFound(val path: String) : Failure()
        data class PermissionDenied(val path: String) : Failure()
    }
}

// Convert to standard Result
fun <T> SmbResult<T>.toResult(): Result<T> = when (this) {
    is SmbResult.Success -> Result.Success(data)
    is SmbResult.Failure.AuthenticationFailed -> Result.Failure(
        AuthenticationException("SMB", username)
    )
    is SmbResult.Failure.ConnectionTimeout -> Result.Failure(
        ConnectionTimeoutException(host, 445, timeoutMs)
    )
    // ... other conversions
}
```

---

## 3. Retry Policies

### Exponential Backoff

```kotlin
class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val factor: Double = 2.0,
    val shouldRetry: (Throwable) -> Boolean = { it is FmsException && it.isRetriable }
)

suspend fun <T> retryWithPolicy(
    policy: RetryPolicy = RetryPolicy(),
    block: suspend () -> T
): Result<T> {
    var currentDelay = policy.initialDelayMs
    
    repeat(policy.maxAttempts) { attempt ->
        try {
            return Result.Success(block())
        } catch (e: Exception) {
            // Check if should retry
            if (!policy.shouldRetry(e) || attempt == policy.maxAttempts - 1) {
                return Result.Failure(e.toFmsException())
            }
            
            // Log retry attempt
            Timber.w(e, "Attempt ${attempt + 1} failed. Retrying in ${currentDelay}ms...")
            
            // Wait before retry
            delay(currentDelay)
            
            // Increase delay (exponential backoff)
            currentDelay = (currentDelay * policy.factor).toLong().coerceAtMost(policy.maxDelayMs)
        }
    }
    
    error("Should not reach here")
}

// Usage
suspend fun downloadFile(url: String): Result<ByteArray> = retryWithPolicy(
    policy = RetryPolicy(
        maxAttempts = 3,
        initialDelayMs = 2000,
        shouldRetry = { it is ConnectionTimeoutException || it is ConnectionLostException }
    )
) {
    httpClient.download(url)
}
```

---

### Smart Retry (Rate Limit Aware)

```kotlin
suspend fun <T> retryWithRateLimit(
    maxAttempts: Int = 3,
    block: suspend () -> T
): Result<T> {
    repeat(maxAttempts) { attempt ->
        try {
            return Result.Success(block())
        } catch (e: RateLimitException) {
            if (attempt == maxAttempts - 1) {
                return Result.Failure(e)
            }
            
            // Wait for rate limit reset
            val waitTime = (e.retryAfterSeconds ?: 60) * 1000
            Timber.w("Rate limited. Waiting ${waitTime}ms before retry...")
            delay(waitTime)
        } catch (e: Exception) {
            return Result.Failure(e.toFmsException())
        }
    }
    error("Should not reach here")
}
```

---

## 4. User-Facing Messages

### Message Mapping

```kotlin
object ErrorMessageMapper {
    
    fun getLocalizedMessage(exception: FmsException, context: Context): String {
        return when (exception) {
            is PermissionDeniedException -> {
                context.getString(R.string.error_permission_denied, exception.permission)
            }
            
            is DiskFullException -> {
                val requiredMb = exception.requiredBytes / 1024 / 1024
                context.getString(R.string.error_disk_full, requiredMb)
            }
            
            is ConnectionTimeoutException -> {
                context.getString(R.string.error_connection_timeout, exception.host)
            }
            
            is TokenExpiredException -> {
                context.getString(R.string.error_token_expired, exception.provider)
            }
            
            is RateLimitException -> {
                val seconds = exception.retryAfterSeconds ?: 60
                context.getString(R.string.error_rate_limit, seconds)
            }
            
            else -> exception.userMessage
        }
    }
    
    /**
     * Get action button text (if recoverable)
     */
    fun getActionText(exception: FmsException, context: Context): String? {
        return when (exception) {
            is PermissionDeniedException -> context.getString(R.string.action_grant_permission)
            is TokenExpiredException -> context.getString(R.string.action_sign_in)
            is ConnectionTimeoutException -> context.getString(R.string.action_retry)
            is ServerUnreachableException -> context.getString(R.string.action_retry)
            is ConnectionLostException -> context.getString(R.string.action_retry)
            else -> if (exception.isRetriable) context.getString(R.string.action_retry) else null
        }
    }
}
```

---

### Error Dialog Pattern

```kotlin
fun showErrorDialog(exception: FmsException, onAction: (() -> Unit)? = null) {
    val context = this // Activity/Fragment
    
    val message = ErrorMessageMapper.getLocalizedMessage(exception, context)
    val actionText = ErrorMessageMapper.getActionText(exception, context)
    
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.error_title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .apply {
            if (actionText != null && onAction != null) {
                setNeutralButton(actionText) { _, _ -> onAction() }
            }
        }
        .show()
}

// Usage in ViewModel
sealed class BrowseEvent {
    data class ShowError(val exception: FmsException, val canRetry: Boolean) : BrowseEvent()
}

// In Activity
lifecycleScope.launch {
    viewModel.events.collect { event ->
        when (event) {
            is BrowseEvent.ShowError -> {
                showErrorDialog(event.exception) {
                    if (event.canRetry) viewModel.retryLastOperation()
                }
            }
        }
    }
}
```

---

## 5. Logging Strategy

### Timber Setup

```kotlin
// Application.onCreate()
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(CrashReportingTree())
}

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            // Send to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                log("$tag: $message")
                t?.let { recordException(it) }
            }
        }
    }
}
```

---

### Logging Patterns

```kotlin
// ✅ GOOD: Context + exception + details
try {
    smbClient.connect(host, port)
} catch (e: Exception) {
    Timber.e(e, "SMB connection failed: host=$host, port=$port, user=$username")
    throw ConnectionTimeoutException(host, port, timeoutMs)
}

// ✅ GOOD: Success with timing
val startTime = System.currentTimeMillis()
val result = loadFiles(resource)
val duration = System.currentTimeMillis() - startTime
Timber.i("Loaded ${result.size} files in ${duration}ms from ${resource.name}")

// ✅ GOOD: Warning for recoverable issues
if (connection.isStale()) {
    Timber.w("Connection stale. Reconnecting to ${resource.uri}")
    connection.reconnect()
}

// ❌ BAD: No context
Timber.e("Error occurred")

// ❌ BAD: Exception swallowed
try {
    operation()
} catch (e: Exception) {
    // Silent failure - no log!
}
```

---

### Structured Logging

```kotlin
object Logger {
    
    fun logOperation(
        operation: String,
        resource: MediaResource,
        duration: Long? = null,
        fileCount: Int? = null,
        error: Throwable? = null
    ) {
        val message = buildString {
            append("[$operation] ")
            append("resource=${resource.name} ")
            append("type=${resource.type} ")
            duration?.let { append("duration=${it}ms ") }
            fileCount?.let { append("files=$it ") }
        }
        
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.i(message)
        }
    }
}

// Usage
Logger.logOperation(
    operation = "SCAN_FOLDER",
    resource = resource,
    duration = 2345,
    fileCount = 158
)
```

---

## 6. Recovery Mechanisms

### Connection Pool Recovery

```kotlin
class SmbConnectionPool {
    
    suspend fun <T> executeWithRecovery(
        resource: MediaResource,
        block: suspend (SmbClient) -> T
    ): Result<T> = retryWithPolicy(
        policy = RetryPolicy(
            maxAttempts = 2,
            shouldRetry = { it is ConnectionLostException }
        )
    ) {
        val connection = getConnection(resource) // May throw
        
        try {
            block(connection)
        } catch (e: Exception) {
            // Connection invalid - remove from pool
            if (e is ConnectionLostException) {
                Timber.w("Connection lost. Evicting from pool.")
                invalidateConnection(resource)
            }
            throw e
        }
    }
}
```

---

### Graceful Degradation

```kotlin
suspend fun loadThumbnail(file: MediaFile): Bitmap? {
    return try {
        // Try network thumbnail (fast)
        downloadThumbnail(file.thumbnailUrl)
    } catch (e: NetworkException) {
        Timber.w(e, "Network thumbnail failed. Falling back to local generation.")
        
        try {
            // Fallback: Download full file and generate thumbnail (slow)
            generateThumbnailFromFullFile(file)
        } catch (e: Exception) {
            Timber.e(e, "Thumbnail generation failed. Using placeholder.")
            // Fallback: Show placeholder
            null
        }
    }
}
```

---

### Partial Success Handling

```kotlin
data class BatchOperationResult<T>(
    val succeeded: List<T>,
    val failed: List<Pair<T, FmsException>>
) {
    val successCount get() = succeeded.size
    val failureCount get() = failed.size
    val isPartialSuccess get() = successCount > 0 && failureCount > 0
    val isCompleteSuccess get() = failureCount == 0
}

suspend fun deleteFiles(files: List<MediaFile>): BatchOperationResult<MediaFile> {
    val succeeded = mutableListOf<MediaFile>()
    val failed = mutableListOf<Pair<MediaFile, FmsException>>()
    
    files.forEach { file ->
        try {
            deleteFile(file)
            succeeded.add(file)
        } catch (e: FmsException) {
            Timber.w(e, "Failed to delete ${file.name}")
            failed.add(file to e)
        }
    }
    
    return BatchOperationResult(succeeded, failed)
}

// UI handling
val result = deleteFiles(selectedFiles)
when {
    result.isCompleteSuccess -> showToast("${result.successCount} files deleted")
    result.isPartialSuccess -> showToast("${result.successCount} deleted, ${result.failureCount} failed")
    else -> showToast("All deletions failed")
}
```

---

## 7. Protocol-Specific Errors

### SMB Error Mapping

```kotlin
fun Exception.toSmbException(): NetworkException {
    return when {
        this is SMBApiException && status == NtStatus.STATUS_ACCESS_DENIED ->
            AuthenticationException("SMB", null)
        
        this is SMBApiException && status == NtStatus.STATUS_OBJECT_NAME_NOT_FOUND ->
            FileNotFoundException(this.message ?: "")
        
        this is SocketTimeoutException ->
            ConnectionTimeoutException("unknown", 445, 30000)
        
        this is java.net.ConnectException ->
            ServerUnreachableException(this.message ?: "unknown")
        
        else ->
            NetworkException("SMB error: ${this.message}", this)
    }
}
```

---

### SFTP Error Mapping

```kotlin
fun Exception.toSftpException(): NetworkException {
    return when (this) {
        is UserAuthException ->
            AuthenticationException("SFTP", null)
        
        is ConnectionException ->
            ServerUnreachableException(this.message ?: "unknown")
        
        is SFTPException -> when (this.statusCode) {
            SSH_FX_NO_SUCH_FILE -> FileNotFoundException(this.message ?: "")
            SSH_FX_PERMISSION_DENIED -> PermissionDeniedException("SFTP", null)
            else -> NetworkException("SFTP error: ${this.message}", this)
        }
        
        else ->
            NetworkException("SFTP error: ${this.message}", this)
    }
}
```

---

### Cloud API Error Mapping

```kotlin
fun HttpException.toCloudException(provider: String): CloudException {
    return when (code()) {
        401 -> TokenExpiredException(provider)
        403 -> QuotaExceededException(provider, 0) // Get from response
        404 -> CloudFileNotFoundException(provider, "")
        429 -> {
            val retryAfter = response()?.headers()?.get("Retry-After")?.toLongOrNull()
            RateLimitException(provider, retryAfter)
        }
        else -> CloudException("$provider error: ${message()}", this)
    }
}
```

---

## 8. Testing Error Scenarios

### Unit Test: Exception Handling

```kotlin
@Test
fun `deleteFile handles permission denied`() = runTest {
    // Arrange
    val mockRepository = mock<FileRepository> {
        on { deleteFile(any()) } doThrow PermissionDeniedException("WRITE_EXTERNAL_STORAGE", "/path")
    }
    val viewModel = BrowseViewModel(mockRepository)
    
    // Act
    viewModel.deleteFile(testFile)
    advanceUntilIdle()
    
    // Assert
    val state = viewModel.uiState.value
    assertTrue(state.error is PermissionDeniedException)
    assertFalse(state.error!!.isRetriable)
}
```

---

### Integration Test: Retry Logic

```kotlin
@Test
fun `retryWithPolicy retries transient failures`() = runTest {
    var attemptCount = 0
    
    val result = retryWithPolicy(
        policy = RetryPolicy(maxAttempts = 3, initialDelayMs = 100)
    ) {
        attemptCount++
        if (attemptCount < 3) {
            throw ConnectionTimeoutException("host", 445, 1000)
        }
        "Success"
    }
    
    assertEquals(3, attemptCount)
    assertTrue(result is Result.Success)
    assertEquals("Success", result.getOrNull())
}

@Test
fun `retryWithPolicy fails non-retriable errors immediately`() = runTest {
    var attemptCount = 0
    
    val result = retryWithPolicy(
        policy = RetryPolicy(maxAttempts = 3)
    ) {
        attemptCount++
        throw PermissionDeniedException("READ", "/path")
    }
    
    assertEquals(1, attemptCount) // No retry
    assertTrue(result is Result.Failure)
}
```

---

## Best Practices Summary

### ✅ DO

1. **Use Custom Exceptions**
```kotlin
throw PermissionDeniedException("READ_EXTERNAL_STORAGE", path)
```

2. **Log with Context**
```kotlin
Timber.e(exception, "Failed to load files: resource=$resource, count=$count")
```

3. **Provide User-Friendly Messages**
```kotlin
override val userMessage = "Connection timed out. Check network and retry."
```

4. **Retry Transient Failures**
```kotlin
retryWithPolicy(policy = RetryPolicy(maxAttempts = 3)) { operation() }
```

5. **Handle Partial Success**
```kotlin
BatchOperationResult(succeeded = [...], failed = [...])
```

---

### ❌ DON'T

1. **Don't Swallow Exceptions**
```kotlin
// ❌ BAD
try { operation() } catch (e: Exception) {}
```

2. **Don't Use Generic Messages**
```kotlin
// ❌ BAD
throw Exception("Error occurred")
```

3. **Don't Retry Non-Retriable Errors**
```kotlin
// ❌ BAD: Retrying permission denial
retryWithPolicy { operation() } // Will fail 3 times
```

4. **Don't Block Main Thread**
```kotlin
// ❌ BAD
runBlocking { suspendOperation() }
```

5. **Don't Expose Technical Details to Users**
```kotlin
// ❌ BAD
"SMBApiException: NtStatus.STATUS_ACCESS_DENIED at line 42"
```

---

## Reference Files

### Source Code
- **Exception Classes**: `domain/exception/FmsException.kt`
- **Result Type**: `domain/model/Result.kt`
- **Retry Logic**: `data/util/RetryPolicy.kt`
- **Error Mapping**: `ui/util/ErrorMessageMapper.kt`
- **Logging**: `util/Logger.kt`

### Related Documents
- [28. State Management Strategy](28_state_management.md) - Error states in UI
- [27. API Contracts & Interfaces](27_api_contracts.md) - Repository error handling
- [21. Common Pitfalls](21_common_pitfalls.md) - FTP timeout handling

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
