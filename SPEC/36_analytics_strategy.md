# Analytics & Crash Reporting Strategy

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Status**: Ready for Implementation

---

## 1. Analytics Provider

### Firebase Analytics + Crashlytics

```gradle
// app_v2/build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}

plugins {
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}
```

**Privacy Compliance**: All analytics opt-in based, GDPR-compliant.

---

## 2. Key Performance Indicators (KPIs)

### User Engagement

| Metric | Target | Measurement |
|--------|--------|-------------|
| Daily Active Users (DAU) | Track baseline | Firebase automatic |
| Session Duration | >5 min/session | Custom event tracking |
| Retention (Day 7) | >40% | Firebase automatic |
| Retention (Day 30) | >20% | Firebase automatic |

### Feature Usage

| Feature | Event Name | Target Adoption |
|---------|------------|-----------------|
| Local Files Browse | `browse_local` | 100% (core) |
| SMB Connection | `connect_smb` | >30% |
| SFTP Connection | `connect_sftp` | >15% |
| Cloud Integration | `connect_cloud_*` | >25% |
| Media Player | `play_media` | >80% |
| Image Editor | `edit_image` | >40% |
| Favorites | `add_favorite` | >50% |

### Performance Metrics

| Metric | Event Name | Target |
|--------|------------|--------|
| App Start Time | `app_start` | <2s (cold), <1s (warm) |
| Browse Load Time | `browse_load_complete` | <1s for 1000 files |
| Network Connection Time | `network_connect_time` | <5s average |
| Crash-Free Users | Crashlytics automatic | >99.5% |

---

## 3. Custom Events Implementation

### Event Tracking Wrapper

```kotlin
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    
    fun logEvent(event: AnalyticsEvent) {
        if (!isAnalyticsEnabled()) return
        
        val bundle = Bundle().apply {
            event.parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        firebaseAnalytics.logEvent(event.name, bundle)
        Timber.d("Analytics: ${event.name} with ${event.parameters}")
    }
    
    private fun isAnalyticsEnabled(): Boolean {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("analytics_enabled", false)
    }
}

sealed class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, Any> = emptyMap()
) {
    // Browse Events
    data class BrowseLocal(val fileCount: Int) : AnalyticsEvent(
        "browse_local",
        mapOf("file_count" to fileCount)
    )
    
    data class BrowseNetwork(
        val protocol: String, // "smb", "sftp", "ftp"
        val fileCount: Int,
        val loadTimeMs: Long
    ) : AnalyticsEvent(
        "browse_network",
        mapOf(
            "protocol" to protocol,
            "file_count" to fileCount,
            "load_time_ms" to loadTimeMs
        )
    )
    
    // Connection Events
    data class NetworkConnect(
        val protocol: String,
        val success: Boolean,
        val durationMs: Long,
        val errorType: String? = null
    ) : AnalyticsEvent(
        "network_connect",
        mapOf(
            "protocol" to protocol,
            "success" to success,
            "duration_ms" to durationMs
        ).plus(errorType?.let { mapOf("error_type" to it) } ?: emptyMap())
    )
    
    data class CloudConnect(
        val provider: String, // "google_drive", "onedrive", "dropbox"
        val success: Boolean,
        val authMethod: String // "oauth", "cached_token"
    ) : AnalyticsEvent(
        "cloud_connect",
        mapOf(
            "provider" to provider,
            "success" to success,
            "auth_method" to authMethod
        )
    )
    
    // File Operations
    data class FileOperation(
        val operation: String, // "copy", "move", "delete"
        val sourceType: String, // "local", "smb", "cloud"
        val destType: String?,
        val fileSize: Long,
        val success: Boolean,
        val durationMs: Long
    ) : AnalyticsEvent(
        "file_operation",
        mapOf(
            "operation" to operation,
            "source_type" to sourceType,
            "file_size" to fileSize,
            "success" to success,
            "duration_ms" to durationMs
        ).plus(destType?.let { mapOf("dest_type" to it) } ?: emptyMap())
    )
    
    // Media Player
    data class PlayMedia(
        val mediaType: String, // "video", "audio", "image", "gif"
        val sourceType: String,
        val durationMs: Long? = null
    ) : AnalyticsEvent(
        "play_media",
        mapOf(
            "media_type" to mediaType,
            "source_type" to sourceType
        ).plus(durationMs?.let { mapOf("duration_ms" to it) } ?: emptyMap())
    )
    
    // Image Editor
    data class EditImage(
        val editType: String, // "crop", "rotate", "filter"
        val sourceType: String
    ) : AnalyticsEvent(
        "edit_image",
        mapOf(
            "edit_type" to editType,
            "source_type" to sourceType
        )
    )
    
    // Errors
    data class ErrorOccurred(
        val errorType: String,
        val errorContext: String,
        val isFatal: Boolean
    ) : AnalyticsEvent(
        "error_occurred",
        mapOf(
            "error_type" to errorType,
            "error_context" to errorContext,
            "is_fatal" to isFatal
        )
    )
}
```

### Usage in ViewModels

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    // ... other dependencies
) : ViewModel() {
    
    fun loadMediaFiles(resource: Resource) {
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            when (val result = getMediaFilesUseCase(resource.id)) {
                is Result.Success -> {
                    val loadTime = System.currentTimeMillis() - startTime
                    
                    when (resource.type) {
                        ResourceType.LOCAL -> {
                            analyticsTracker.logEvent(
                                AnalyticsEvent.BrowseLocal(result.data.size)
                            )
                        }
                        ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP -> {
                            analyticsTracker.logEvent(
                                AnalyticsEvent.BrowseNetwork(
                                    protocol = resource.type.name.lowercase(),
                                    fileCount = result.data.size,
                                    loadTimeMs = loadTime
                                )
                            )
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    analyticsTracker.logEvent(
                        AnalyticsEvent.ErrorOccurred(
                            errorType = "browse_failed",
                            errorContext = resource.type.name,
                            isFatal = false
                        )
                    )
                }
            }
        }
    }
}
```

---

## 4. Crash Reporting

### Firebase Crashlytics Setup

```kotlin
class FastMediaSorterApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set custom keys for crash context
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
        }
        
        // Set user ID (anonymized)
        val anonymousId = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("anonymous_user_id", null) ?: UUID.randomUUID().toString()
        FirebaseCrashlytics.getInstance().setUserId(anonymousId)
    }
}
```

### Non-Fatal Exception Reporting

```kotlin
// In UseCase or ViewModel
try {
    smbClient.connect(credentials)
} catch (e: Exception) {
    Timber.e(e, "SMB connection failed")
    
    // Report to Crashlytics with context
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("smb_server", credentials.server)
        setCustomKey("smb_port", credentials.port)
        recordException(e)
    }
    
    return Result.Error("Connection failed: ${e.message}")
}
```

### Critical Crash Context

```kotlin
@Singleton
class CrashReportingHelper @Inject constructor() {
    
    fun setNetworkOperationContext(
        protocol: String,
        operation: String,
        resourcePath: String
    ) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("network_protocol", protocol)
            setCustomKey("network_operation", operation)
            setCustomKey("resource_path_hash", resourcePath.hashCode().toString())
        }
    }
    
    fun clearNetworkContext() {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("network_protocol", "none")
            setCustomKey("network_operation", "none")
        }
    }
}
```

---

## 5. User Properties

Track user segments for targeted improvements:

```kotlin
@Singleton
class UserPropertiesTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    
    fun updateUserProperties() {
        // Device info
        firebaseAnalytics.setUserProperty("device_model", Build.MODEL)
        firebaseAnalytics.setUserProperty("android_version", Build.VERSION.SDK_INT.toString())
        
        // Feature usage
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        firebaseAnalytics.setUserProperty(
            "uses_network",
            prefs.getBoolean("has_used_network", false).toString()
        )
        firebaseAnalytics.setUserProperty(
            "uses_cloud",
            prefs.getBoolean("has_used_cloud", false).toString()
        )
        
        // Language preference
        firebaseAnalytics.setUserProperty("language", Locale.getDefault().language)
    }
}
```

---

## 6. Privacy & Opt-In

### Settings UI

```kotlin
// SettingsFragment.kt
private fun setupAnalyticsToggle() {
    binding.switchAnalytics.isChecked = prefs.getBoolean("analytics_enabled", false)
    
    binding.switchAnalytics.setOnCheckedChangeListener { _, isChecked ->
        prefs.edit().putBoolean("analytics_enabled", isChecked).apply()
        
        // Disable Firebase collection
        FirebaseAnalytics.getInstance(requireContext()).setAnalyticsCollectionEnabled(isChecked)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isChecked)
        
        Toast.makeText(
            requireContext(),
            if (isChecked) R.string.analytics_enabled else R.string.analytics_disabled,
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

### Privacy Policy Compliance

**GDPR/CCPA Requirements**:
- Analytics disabled by default (opt-in)
- Clear explanation in settings: "Help improve app by sharing anonymous usage data"
- Privacy policy link in settings
- No PII collected (anonymous user IDs only)
- User can disable at any time

---

## 7. Performance Monitoring

### Custom Traces

```kotlin
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    fun traceFileOperation(
        operation: String,
        block: suspend () -> Unit
    ) {
        val trace = FirebasePerformance.getInstance()
            .newTrace("file_operation_$operation")
        
        trace.start()
        try {
            runBlocking { block() }
            trace.putAttribute("result", "success")
        } catch (e: Exception) {
            trace.putAttribute("result", "failure")
            throw e
        } finally {
            trace.stop()
        }
    }
}

// Usage
performanceMonitor.traceFileOperation("copy_large_file") {
    copyFileUseCase(source, destination)
}
```

---

## 8. Dashboard Metrics (Firebase Console)

### Monitoring After Release

**Daily Checks**:
1. Crashlytics → Crash-free users (target: >99.5%)
2. Analytics → Active Users (DAU trend)
3. Performance → App start time (target: <2s)

**Weekly Reviews**:
1. Analytics → Feature usage (which features underused?)
2. Crashlytics → Top crashes (prioritize fixes)
3. Performance → Network request durations

**Monthly Analysis**:
1. Retention cohorts (7-day, 30-day)
2. User properties breakdown (device models, Android versions)
3. Custom events trends (growing/declining features)

---

## 9. A/B Testing (Future)

Firebase Remote Config for feature flags:

```kotlin
// Example: Test new thumbnail cache size
val remoteConfig = FirebaseRemoteConfig.getInstance()
remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val cacheSize = remoteConfig.getLong("thumbnail_cache_size_mb")
        GlideImageCache.updateCacheSize(cacheSize)
    }
}
```

---

## 10. Summary

**Must Track**:
- App crashes (Crashlytics)
- Connection success rates (SMB/SFTP/Cloud)
- File operation performance
- Media player usage

**Nice to Track**:
- Feature adoption trends
- User retention cohorts
- Performance regressions

**Privacy First**:
- Opt-in only
- Anonymous user IDs
- GDPR-compliant
