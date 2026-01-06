# Network Resilience Patterns

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Status**: Ready for Implementation

---

## 1. Overview

Unified strategy for handling network failures across all protocols (SMB/SFTP/FTP/Cloud).

**Core Principles**:
- Fail fast, recover gracefully
- User always informed about network state
- Automatic retry with exponential backoff
- Degrade features, don't crash

---

## 2. Circuit Breaker Pattern

### Implementation

```kotlin
enum class CircuitState {
    CLOSED,   // Normal operation
    OPEN,     // Failures detected, reject requests
    HALF_OPEN // Testing if recovered
}

data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,        // Open after 5 failures
    val successThreshold: Int = 2,        // Close after 2 successes in HALF_OPEN
    val openDurationMs: Long = 30_000,    // Stay open for 30s
    val halfOpenMaxAttempts: Int = 3      // Max attempts in HALF_OPEN
)

@Singleton
class NetworkCircuitBreaker @Inject constructor() {
    
    private val circuits = ConcurrentHashMap<String, CircuitState>()
    private val failureCounts = ConcurrentHashMap<String, Int>()
    private val openTimestamps = ConcurrentHashMap<String, Long>()
    private val successCounts = ConcurrentHashMap<String, Int>()
    
    private val config = CircuitBreakerConfig()
    
    fun <T> execute(
        circuitKey: String,
        operation: suspend () -> T
    ): Result<T> {
        return when (getState(circuitKey)) {
            CircuitState.OPEN -> {
                if (shouldAttemptReset(circuitKey)) {
                    setState(circuitKey, CircuitState.HALF_OPEN)
                    executeInHalfOpen(circuitKey, operation)
                } else {
                    Result.Error("Circuit breaker open for $circuitKey")
                }
            }
            CircuitState.HALF_OPEN -> {
                executeInHalfOpen(circuitKey, operation)
            }
            CircuitState.CLOSED -> {
                executeInClosed(circuitKey, operation)
            }
        }
    }
    
    private suspend fun <T> executeInClosed(
        circuitKey: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            recordSuccess(circuitKey)
            Result.Success(result)
        } catch (e: Exception) {
            recordFailure(circuitKey)
            Result.Error(e.message ?: "Operation failed")
        }
    }
    
    private suspend fun <T> executeInHalfOpen(
        circuitKey: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            recordHalfOpenSuccess(circuitKey)
            Result.Success(result)
        } catch (e: Exception) {
            tripCircuit(circuitKey)
            Result.Error(e.message ?: "Operation failed")
        }
    }
    
    private fun recordSuccess(circuitKey: String) {
        failureCounts[circuitKey] = 0
    }
    
    private fun recordFailure(circuitKey: String) {
        val failures = failureCounts.getOrDefault(circuitKey, 0) + 1
        failureCounts[circuitKey] = failures
        
        if (failures >= config.failureThreshold) {
            tripCircuit(circuitKey)
        }
    }
    
    private fun recordHalfOpenSuccess(circuitKey: String) {
        val successes = successCounts.getOrDefault(circuitKey, 0) + 1
        successCounts[circuitKey] = successes
        
        if (successes >= config.successThreshold) {
            resetCircuit(circuitKey)
        }
    }
    
    private fun tripCircuit(circuitKey: String) {
        setState(circuitKey, CircuitState.OPEN)
        openTimestamps[circuitKey] = System.currentTimeMillis()
        Timber.w("Circuit breaker tripped for $circuitKey")
    }
    
    private fun resetCircuit(circuitKey: String) {
        setState(circuitKey, CircuitState.CLOSED)
        failureCounts.remove(circuitKey)
        successCounts.remove(circuitKey)
        openTimestamps.remove(circuitKey)
        Timber.i("Circuit breaker reset for $circuitKey")
    }
    
    private fun shouldAttemptReset(circuitKey: String): Boolean {
        val openTime = openTimestamps[circuitKey] ?: return false
        return System.currentTimeMillis() - openTime >= config.openDurationMs
    }
    
    private fun getState(circuitKey: String): CircuitState {
        return circuits.getOrDefault(circuitKey, CircuitState.CLOSED)
    }
    
    private fun setState(circuitKey: String, state: CircuitState) {
        circuits[circuitKey] = state
    }
}
```

### Usage

```kotlin
@Singleton
class SmbRepository @Inject constructor(
    private val smbClient: SmbClient,
    private val circuitBreaker: NetworkCircuitBreaker
) {
    
    suspend fun listFiles(resourceId: Long, path: String): Result<List<MediaFile>> {
        val circuitKey = "smb_$resourceId"
        
        return circuitBreaker.execute(circuitKey) {
            smbClient.listFiles(path)
        }
    }
}
```

---

## 3. Connection Health Monitoring

### Health Check Worker

```kotlin
class NetworkHealthWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val resources = resourceDao.getAllNetworkResources()
        
        resources.forEach { resource ->
            val health = checkResourceHealth(resource)
            healthDao.insertOrUpdate(health)
        }
        
        return Result.success()
    }
    
    private suspend fun checkResourceHealth(resource: ResourceEntity): ResourceHealth {
        val startTime = System.currentTimeMillis()
        
        return try {
            when (resource.type) {
                ResourceType.SMB -> smbClient.testConnection(resource)
                ResourceType.SFTP -> sftpClient.testConnection(resource)
                ResourceType.FTP -> ftpClient.testConnection(resource)
                else -> return ResourceHealth.unknown(resource.id)
            }
            
            val latency = System.currentTimeMillis() - startTime
            ResourceHealth.healthy(resource.id, latency)
            
        } catch (e: Exception) {
            ResourceHealth.unhealthy(resource.id, e.message)
        }
    }
}

data class ResourceHealth(
    val resourceId: Long,
    val status: HealthStatus,
    val latencyMs: Long?,
    val lastChecked: Long,
    val errorMessage: String?
) {
    enum class HealthStatus {
        HEALTHY,
        DEGRADED,  // Latency > 3s
        UNHEALTHY,
        UNKNOWN
    }
    
    companion object {
        fun healthy(resourceId: Long, latency: Long) = ResourceHealth(
            resourceId = resourceId,
            status = if (latency > 3000) HealthStatus.DEGRADED else HealthStatus.HEALTHY,
            latencyMs = latency,
            lastChecked = System.currentTimeMillis(),
            errorMessage = null
        )
        
        fun unhealthy(resourceId: Long, error: String?) = ResourceHealth(
            resourceId = resourceId,
            status = HealthStatus.UNHEALTHY,
            latencyMs = null,
            lastChecked = System.currentTimeMillis(),
            errorMessage = error
        )
        
        fun unknown(resourceId: Long) = ResourceHealth(
            resourceId = resourceId,
            status = HealthStatus.UNKNOWN,
            latencyMs = null,
            lastChecked = System.currentTimeMillis(),
            errorMessage = null
        )
    }
}
```

### Health UI Indicator

```kotlin
// BrowseViewModel.kt
private val _resourceHealth = MutableStateFlow<ResourceHealth?>(null)
val resourceHealth: StateFlow<ResourceHealth?> = _resourceHealth

fun loadResourceHealth(resourceId: Long) {
    viewModelScope.launch {
        healthDao.getHealthForResource(resourceId).collectLatest { health ->
            _resourceHealth.value = health
        }
    }
}

// BrowseFragment.kt
binding.healthIndicator.apply {
    when (health?.status) {
        HealthStatus.HEALTHY -> {
            setImageResource(R.drawable.ic_health_good)
            setColorFilter(getColor(R.color.success_green))
        }
        HealthStatus.DEGRADED -> {
            setImageResource(R.drawable.ic_health_warning)
            setColorFilter(getColor(R.color.warning_yellow))
        }
        HealthStatus.UNHEALTHY -> {
            setImageResource(R.drawable.ic_health_error)
            setColorFilter(getColor(R.color.error_red))
        }
        else -> visibility = View.GONE
    }
}
```

---

## 4. Graceful Degradation

### Feature Availability Matrix

| Connection State | Browse | Preview | Copy/Move | Edit | Cloud Sync |
|------------------|--------|---------|-----------|------|------------|
| **Online (Good)** | ✅ Full | ✅ Full | ✅ Fast | ✅ Full | ✅ Auto |
| **Online (Slow)** | ✅ Paginated | ⚠️ Low-res | ⚠️ Chunked | ❌ Disabled | ⏸️ Manual |
| **Degraded** | ✅ Cached only | ⚠️ Cached only | ❌ Disabled | ❌ Disabled | ❌ Disabled |
| **Offline** | ✅ Local only | ✅ Local only | ✅ Local→Local | ✅ Local only | ❌ Queued |

### Implementation

```kotlin
@Singleton
class FeatureAvailabilityManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val resourceHealthDao: ResourceHealthDao
) {
    
    suspend fun isFeatureAvailable(
        feature: Feature,
        resourceId: Long
    ): FeatureAvailability {
        val networkState = networkMonitor.networkState.value
        val resourceHealth = resourceHealthDao.getHealthForResource(resourceId).firstOrNull()
        
        return when (feature) {
            Feature.BROWSE -> {
                if (networkState == NetworkState.Offline) {
                    FeatureAvailability.Degraded("Showing cached files only")
                } else {
                    FeatureAvailability.Available
                }
            }
            Feature.PREVIEW -> {
                when {
                    networkState == NetworkState.Offline -> {
                        FeatureAvailability.Degraded("Cached previews only")
                    }
                    resourceHealth?.status == HealthStatus.DEGRADED -> {
                        FeatureAvailability.Degraded("Low-resolution previews")
                    }
                    else -> FeatureAvailability.Available
                }
            }
            Feature.EDIT -> {
                if (networkState != NetworkState.Online) {
                    FeatureAvailability.Unavailable("Editing requires network connection")
                } else {
                    FeatureAvailability.Available
                }
            }
            Feature.FILE_OPERATION -> {
                when (resourceHealth?.status) {
                    HealthStatus.HEALTHY -> FeatureAvailability.Available
                    HealthStatus.DEGRADED -> FeatureAvailability.Degraded("Slow network - operations may take longer")
                    else -> FeatureAvailability.Unavailable("Network unavailable")
                }
            }
            Feature.CLOUD_SYNC -> {
                if (networkState == NetworkState.Online) {
                    FeatureAvailability.Available
                } else {
                    FeatureAvailability.Unavailable("Sync disabled offline")
                }
            }
        }
    }
    
    enum class Feature {
        BROWSE,
        PREVIEW,
        EDIT,
        FILE_OPERATION,
        CLOUD_SYNC
    }
    
    sealed class FeatureAvailability {
        object Available : FeatureAvailability()
        data class Degraded(val reason: String) : FeatureAvailability()
        data class Unavailable(val reason: String) : FeatureAvailability()
    }
}
```

---

## 5. Network Type Detection

### WiFi vs Mobile Data Warning

```kotlin
@Singleton
class NetworkTypeMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    
    val networkType = MutableStateFlow<NetworkType>(NetworkType.UNKNOWN)
    
    init {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                networkType.value = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        NetworkType.WIFI
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        NetworkType.MOBILE
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        NetworkType.ETHERNET
                    }
                    else -> NetworkType.UNKNOWN
                }
            }
            
            override fun onLost(network: Network) {
                networkType.value = NetworkType.NONE
            }
        }
        
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)
    }
    
    enum class NetworkType {
        WIFI,
        MOBILE,
        ETHERNET,
        NONE,
        UNKNOWN
    }
    
    fun shouldWarnMobileData(): Boolean {
        return networkType.value == NetworkType.MOBILE
    }
}
```

### Mobile Data Warning Dialog

```kotlin
// BrowseViewModel.kt
fun checkMobileDataWarning(fileSize: Long) {
    if (networkTypeMonitor.shouldWarnMobileData() && fileSize > 10_000_000) { // >10MB
        _events.emit(BrowseEvent.ShowMobileDataWarning(fileSize))
    }
}

// BrowseFragment.kt
private fun showMobileDataWarning(fileSize: Long) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.mobile_data_warning_title)
        .setMessage(getString(R.string.mobile_data_warning_message, fileSize.formatFileSize()))
        .setPositiveButton(R.string.continue_anyway) { _, _ ->
            viewModel.proceedWithOperation()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
```

**Settings Option**:
```xml
<SwitchPreferenceCompat
    app:key="warn_mobile_data"
    app:title="@string/settings_warn_mobile_data"
    app:summary="@string/settings_warn_mobile_data_summary"
    app:defaultValue="true" />
```

---

## 6. Timeout Configuration

### Protocol-Specific Timeouts

```kotlin
data class TimeoutConfig(
    val connectTimeoutMs: Long,
    val readTimeoutMs: Long,
    val writeTimeoutMs: Long
)

object NetworkTimeouts {
    val SMB = TimeoutConfig(
        connectTimeoutMs = 15_000,
        readTimeoutMs = 30_000,
        writeTimeoutMs = 60_000
    )
    
    val SFTP = TimeoutConfig(
        connectTimeoutMs = 20_000,
        readTimeoutMs = 45_000,
        writeTimeoutMs = 90_000
    )
    
    val FTP = TimeoutConfig(
        connectTimeoutMs = 10_000,
        readTimeoutMs = 30_000,
        writeTimeoutMs = 60_000
    )
    
    val CLOUD = TimeoutConfig(
        connectTimeoutMs = 30_000,  // OAuth can be slow
        readTimeoutMs = 60_000,     // Large file metadata
        writeTimeoutMs = 120_000    // Chunked uploads
    )
}
```

### Adaptive Timeout

```kotlin
@Singleton
class AdaptiveTimeoutManager @Inject constructor(
    private val healthDao: ResourceHealthDao
) {
    
    suspend fun getTimeoutForResource(resourceId: Long, baseTimeout: Long): Long {
        val health = healthDao.getHealthForResource(resourceId).firstOrNull()
        
        return when (health?.status) {
            HealthStatus.DEGRADED -> (baseTimeout * 1.5).toLong()  // +50% for slow network
            HealthStatus.UNHEALTHY -> (baseTimeout * 0.5).toLong() // -50% fail fast
            else -> baseTimeout
        }
    }
}
```

---

## 7. Retry Strategy Summary

| Protocol | Max Retries | Backoff | Circuit Breaker | Mobile Data Warning |
|----------|-------------|---------|-----------------|---------------------|
| SMB | 3 | Exponential (1s, 2s, 4s) | ✅ | ✅ (>10MB) |
| SFTP | 3 | Exponential (2s, 4s, 8s) | ✅ | ✅ (>10MB) |
| FTP | 2 | Linear (5s, 10s) | ✅ | ✅ (>10MB) |
| Google Drive | 5 | Exponential with jitter | ✅ | ✅ (always) |
| OneDrive | 5 | Exponential with jitter | ✅ | ✅ (always) |
| Dropbox | 5 | Exponential with jitter | ✅ | ✅ (always) |

---

## 8. Testing Network Resilience

### Test Cases

```kotlin
@RunWith(AndroidJUnit4::class)
class CircuitBreakerTest {
    
    private lateinit var circuitBreaker: NetworkCircuitBreaker
    
    @Before
    fun setup() {
        circuitBreaker = NetworkCircuitBreaker()
    }
    
    @Test
    fun `circuit opens after failure threshold`() = runTest {
        repeat(5) {
            circuitBreaker.execute("test") {
                throw IOException("Connection failed")
            }
        }
        
        val result = circuitBreaker.execute("test") {
            "Success"
        }
        
        assertTrue(result is Result.Error)
        assertTrue(result.message?.contains("Circuit breaker open") == true)
    }
    
    @Test
    fun `circuit recovers after timeout`() = runTest {
        // Trip circuit
        repeat(5) {
            circuitBreaker.execute("test") { throw IOException() }
        }
        
        // Wait for timeout (30s in production, mock for test)
        delay(100) // Use shorter timeout in test config
        
        // Should attempt recovery
        val result = circuitBreaker.execute("test") { "Success" }
        assertTrue(result is Result.Success)
    }
}
```

---

## 9. User Communication

### Network Error Messages

```kotlin
sealed class NetworkError(val userMessage: Int, val technicalDetails: String) {
    class ConnectionTimeout(details: String) : NetworkError(
        R.string.error_connection_timeout,
        details
    )
    
    class CircuitBreakerOpen(details: String) : NetworkError(
        R.string.error_service_unavailable,
        details
    )
    
    class SlowNetwork(details: String) : NetworkError(
        R.string.warning_slow_network,
        details
    )
}

// strings.xml
<string name="error_connection_timeout">Connection timed out. Check network and try again.</string>
<string name="error_service_unavailable">Service temporarily unavailable. Trying again in 30 seconds.</string>
<string name="warning_slow_network">Network is slow. Operations may take longer than usual.</string>
```

---

## 10. Summary

**Implemented Patterns**:
- ✅ Circuit breaker for all network protocols
- ✅ Connection health monitoring
- ✅ Graceful feature degradation
- ✅ Network type detection (WiFi/Mobile)
- ✅ Adaptive timeouts based on health

**User Benefits**:
- Predictable failure behavior
- Faster failure detection (fail fast)
- Automatic recovery
- Clear warnings for mobile data
- No unexpected crashes
