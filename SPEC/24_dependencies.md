# 24. Dependencies Reference

**Last Updated**: January 6, 2026  
**Build Tool**: Gradle 8.7.3 | Kotlin 1.9.24 | Java 17

This document lists all dependencies used in the project with version numbers, purpose, and implementation recommendations.

---

## Build System

| Dependency | Version | Purpose |
|------------|---------|---------|
| Android Gradle Plugin | 8.7.3 | Android build configuration |
| Kotlin Gradle Plugin | 1.9.24 | Kotlin language support |
| Hilt Gradle Plugin | 2.50 | Dependency injection configuration |
| Navigation SafeArgs | 2.7.6 | Type-safe navigation arguments |

---

## Core Android Libraries

### AndroidX Core
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.core:core-ktx | 1.12.0 | Kotlin extensions for Android framework | Essential for all Android apps |
| androidx.appcompat:appcompat | 1.6.1 | Backward compatibility for Material Design | Always use with Material 3 |
| androidx.constraintlayout:constraintlayout | 2.1.4 | Complex layouts with flat hierarchy | Use for complex UI screens |
| androidx.recyclerview:recyclerview | 1.3.2 | Efficient list display | Core for any list-based UI |
| androidx.swiperefreshlayout:swiperefreshlayout | 1.1.0 | Pull-to-refresh gesture | Standard pattern for refreshing content |
| androidx.viewpager2:viewpager2 | 1.0.0 | Swipeable pages | Use for media galleries |
| androidx.documentfile:documentfile | 1.0.1 | SAF (Storage Access Framework) support | Required for scoped storage |
| androidx.exifinterface:exifinterface | 1.3.7 | EXIF metadata extraction | Essential for image metadata |
| androidx.security:security-crypto | 1.1.0-alpha06 | Encrypted storage | Use for sensitive data (OAuth tokens, passwords) |

### Material Design
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.google.android.material:material | 1.12.0 | Material Design 3 components | Use for all UI components |

### Lifecycle & Architecture
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.7.0 | ViewModel with coroutines | Core for MVVM architecture |
| androidx.lifecycle:lifecycle-livedata-ktx | 2.7.0 | LiveData with coroutines | Prefer StateFlow/SharedFlow |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.7.0 | Lifecycle-aware coroutines | Use `lifecycleScope` for UI operations |
| androidx.activity:activity-ktx | 1.8.2 | Activity extensions | Use for result contracts |
| androidx.fragment:fragment-ktx | 1.6.2 | Fragment extensions | Use for modern fragment APIs |

### Navigation
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.navigation:navigation-fragment-ktx | 2.7.6 | Fragment-based navigation | Use SafeArgs for type safety |
| androidx.navigation:navigation-ui-ktx | 2.7.6 | UI components integration | Auto-handle drawer/toolbar |

---

## Dependency Injection

| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.google.dagger:hilt-android | 2.50 | DI framework | Always use constructor injection |
| com.google.dagger:hilt-android-compiler | 2.50 | Annotation processor | Required for code generation |
| androidx.hilt:hilt-work | 1.1.0 | WorkManager integration | Use for background tasks |
| androidx.hilt:hilt-compiler | 1.1.0 | WorkManager annotation processor | Required for WorkManager DI |

**Best Practices**:
- Annotate ViewModels with `@HiltViewModel`
- Use `@Singleton` for repository implementations
- Inject UseCases into ViewModels only (not repositories)
- Use `@ApplicationContext` for application-level dependencies

---

## Database & Storage

### Room Database
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.room:room-runtime | 2.6.1 | SQLite ORM | Use for all persistent data |
| androidx.room:room-ktx | 2.6.1 | Coroutines support | Always use with runtime |
| androidx.room:room-compiler | 2.6.1 | Annotation processor | Required for code generation |

**Best Practices**:
- Always increment DB version on schema changes
- Create `Migration` objects for schema updates
- Use `@Transaction` for multi-table operations
- Prefer `Flow<T>` for reactive queries

### DataStore
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.datastore:datastore-preferences | 1.0.0 | Key-value storage | Replacement for SharedPreferences |

**Best Practices**:
- Use for app settings and user preferences
- Never store sensitive data (use security-crypto instead)
- Always use `Flow` for reactive updates

### Paging
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.paging:paging-runtime-ktx | 3.2.1 | Large dataset pagination | Use for 1000+ items |

**Best Practices**:
- Activate when file count > `PAGINATION_THRESHOLD` (1000)
- Implement `PagingSource` for custom data sources
- Use `PagingDataAdapter` with `DiffUtil`

---

## Asynchronous Programming

| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.7.3 | Android coroutines | Use for all async operations |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.7.3 | Core coroutines API | Required for structured concurrency |
| org.jetbrains.kotlinx:kotlinx-coroutines-play-services | 1.7.3 | Google Play Services integration | Required for Google Drive OAuth |

**Best Practices**:
- Use `Dispatchers.IO` for file/network operations
- Use `viewModelScope` in ViewModels
- Never block main thread with `runBlocking`
- Always handle cancellation with `Job.isActive` checks

---

## Background Work

| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.work:work-runtime-ktx | 2.9.0 | Background task scheduling | Use for deferrable work |

**Best Practices**:
- Use for trash cleanup, cache management
- Configure constraints (network, battery)
- Use `CoroutineWorker` for suspend functions

---

## Media & Images

### ExoPlayer (Media Playback)
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.media3:media3-exoplayer | 1.2.1 | Video/audio player | Exclude streaming modules for local playback |
| androidx.media3:media3-ui | 1.2.1 | Player UI components | Use `PlayerView` for video |
| androidx.media3:media3-common | 1.2.1 | Common media APIs | Required dependency |
| androidx.media3:media3-decoder | 1.2.1 | Audio decoders | Required for WAV and other formats |

**Configuration**:
```kotlin
implementation("androidx.media3:media3-exoplayer:1.2.1") {
    exclude(group = "androidx.media3", module = "media3-exoplayer-dash")
    exclude(group = "androidx.media3", module = "media3-exoplayer-hls")
    exclude(group = "androidx.media3", module = "media3-exoplayer-smoothstreaming")
}
```

**Best Practices**:
- Release player in `onPause()` or `onStop()`
- Use `SimpleExoPlayer.Builder` for initialization
- Handle player errors with custom `EventListener`

### Image Loading (Glide)
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.github.bumptech.glide:glide | 4.16.0 | Image loading & caching | Use for all image loading |
| com.github.bumptech.glide:compiler | 4.16.0 | Annotation processor | Required for code generation |
| com.github.bumptech.glide:okhttp3-integration | 4.16.0 | OkHttp integration | Required for network images |

**Best Practices**:
- Use `DiskCacheStrategy.RESOURCE` for thumbnails
- Create custom `ModelLoader` for network files (SMB/SFTP/FTP)
- Clear cache on file delete/move/rename
- Default cache size: 2 GB (configurable in settings)

### Image Viewing
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.github.chrisbanes:PhotoView | 2.3.0 | Pinch-to-zoom & rotation | Use for full-screen image viewing |

### UI Components
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| me.zhanghai.android.fastscroll:library | 1.3.0 | Interactive scrollbar | Use for large lists (>100 items) |

---

## Network & Cloud Storage

### SMB Protocol
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.hierynomus:smbj | 0.12.1 | SMB client | Requires BouncyCastle 1.78.1 for stability |

**Best Practices**:
- Use connection pooling (max 5 connections per share)
- Implement 45s idle timeout for cleanup
- Handle authentication errors gracefully
- Support NTLM and Kerberos authentication

### SFTP Protocol
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.github.mwiede:jsch | 0.2.16 | SFTP/SSH client | Better KEX support than SSHJ |

**Best Practices**:
- Verify host keys for security
- Support Ed25519 keys (Curve25519)
- Implement connection pooling

### FTP Protocol
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| commons-net:commons-net | 3.10.0 | FTP client | Use with active mode fallback |

**Best Practices**:
- Never call `completePendingCommand()` after exceptions
- Handle PASV mode timeouts with active mode fallback
- Use binary transfer mode for all files

### HTTP Client
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client | Use for cloud API calls |
| com.squareup.retrofit2:retrofit | 2.9.0 | REST API client | Use for structured APIs |
| com.squareup.retrofit2:converter-gson | 2.9.0 | JSON serialization | Use with Retrofit |

### Cloud Storage APIs
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.google.android.gms:play-services-auth | 21.0.0 | Google OAuth | Required for Google Drive |
| com.dropbox.core:dropbox-core-sdk | 5.4.5 | Dropbox API | Use v5 for OAuth 2.0 |
| com.microsoft.identity.client:msal | 6.0.1 | Microsoft OAuth | Required for OneDrive |

**OAuth Configuration**:
- **Google Drive**: Create Android OAuth client in Google Cloud Console (SHA-1 fingerprint required)
- **Dropbox**: Register app key in Dropbox App Console (`manifestPlaceholders["dropboxAppKey"]`)
- **OneDrive**: Register app in Azure Portal (redirect URI: `msal{CLIENT_ID}://auth`)

---

## Machine Learning & OCR

### ML Kit
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.google.mlkit:translate | 17.0.3 | On-device translation | Use for text translation |
| com.google.mlkit:text-recognition | 16.0.1 | Latin/Cyrillic OCR | Fast but limited accuracy |
| com.google.mlkit:language-id | 17.0.6 | Language detection | Use before translation |

### Tesseract OCR
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| cz.adaptech:tesseract4android | 4.8.0 | Offline OCR | Better Cyrillic support, requires language data |

**Configuration**:
```kotlin
implementation("cz.adaptech:tesseract4android:4.8.0") {
    exclude(group = "cz.adaptech.tesseract4android", module = "tesseract4android-openmp")
}
```

**Best Practices**:
- Download language data files (`.traineddata`) to app storage
- Use ML Kit for quick scans, Tesseract for high accuracy
- Process OCR in background thread

---

## Document Support

### EPUB Reader
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| io.documentnode:epub4j-core | 4.2 | EPUB parsing | Exclude xmlpull/kxml2 to avoid conflicts |
| org.jsoup:jsoup | 1.17.2 | HTML parsing | Use for EPUB HTML content |

**Configuration**:
```kotlin
implementation("io.documentnode:epub4j-core:4.2") {
    exclude(group = "xmlpull", module = "xmlpull")
    exclude(group = "net.sf.kxml", module = "kxml2")
}
```

### PDF Support
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| Android PdfRenderer | API 21+ | Built-in PDF rendering | No external dependency needed |

**Best Practices**:
- Use `PdfRenderer` for thumbnail generation
- Render pages at appropriate resolution (reduce memory usage)
- Close `ParcelFileDescriptor` after use

---

## Jetpack Compose (Experimental)

| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.compose:compose-bom | 2024.02.00 | Compose version management | Use BOM for version consistency |
| androidx.compose.ui:ui | (BOM) | Compose UI toolkit | Use for modern UI screens |
| androidx.compose.material3:material3 | (BOM) | Material Design 3 | Use for new features |
| androidx.compose.material:material-icons-extended | (BOM) | Icon library | Large (~13 MB), use selectively |
| androidx.activity:activity-compose | 1.8.2 | Compose-Activity integration | Required for Compose screens |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.7.0 | ViewModel-Compose integration | Use with `viewModel()` function |

**Best Practices**:
- Use for new UI features (existing screens are XML-based)
- Prefer `LazyColumn` over `RecyclerView` in Compose
- Use `remember` for state management
- Use `derivedStateOf` for computed values

---

## Logging

| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| com.jakewharton.timber:timber | 5.0.1 | Logging framework | Use instead of Log.d/e/i |

**Best Practices**:
- Initialize in `Application.onCreate()`
- Plant `DebugTree` for debug builds only
- Use format strings: `Timber.d("Action: %s", action)`
- Always log exceptions: `Timber.e(exception, "Context")`

---

## Testing

### Unit Testing
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| junit:junit | 4.13.2 | Unit testing framework | Standard JUnit 4 |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.7.3 | Coroutines testing | Use `TestScope` and `runTest` |
| androidx.arch.core:core-testing | 2.2.0 | ViewModel testing | Use `InstantTaskExecutorRule` |
| io.mockk:mockk | 1.13.9 | Mocking framework | Kotlin-first mocking |
| org.robolectric:robolectric | 4.11.1 | Android framework in JVM | Use for Android SDK testing |

### Instrumentation Testing
| Dependency | Version | Purpose | Recommendations |
|------------|---------|---------|-----------------|
| androidx.test.ext:junit | 1.1.5 | Android JUnit runner | Required for device tests |
| androidx.test.espresso:espresso-core | 3.5.1 | UI testing | Use for view interactions |
| androidx.navigation:navigation-testing | 2.7.6 | Navigation testing | Test navigation graphs |
| com.google.dagger:hilt-android-testing | 2.50 | Hilt testing | Use with custom test modules |
| androidx.room:room-testing | 2.6.1 | Room testing | Test database migrations |

**Best Practices**:
- Use `@HiltAndroidTest` for instrumentation tests
- Mock repositories in ViewModels (inject via Hilt)
- Test database migrations with `MigrationTestHelper`
- Use `launchFragmentInContainer` for fragment tests

---

## Build Configuration Recommendations

### ProGuard Rules
```proguard
# Keep model classes for Retrofit/Gson
-keep class com.sza.fastmediasorter.data.model.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Glide modules
-keep class com.sza.fastmediasorter.data.glide.** { *; }
```

### APK Size Optimization
1. **Resource Configurations**: Keep only `en`, `ru`, `uk` locales
2. **Exclude Unused Dependencies**: Remove streaming modules from ExoPlayer
3. **Native Libraries**: Use 16 KB page size alignment for Android 15+
4. **Compose Icons**: Use selectively (material-icons-extended is ~13 MB)

### Android 15+ Compatibility
- Set `jniLibs.useLegacyPackaging = false` for 16 KB page alignment
- Update Tesseract to 4.8.0 (November 2024 release)
- Google Play requires 16 KB alignment since November 1, 2025

---

## Version Update Strategy

### Quarterly Updates (Recommended)
- AndroidX libraries: Check for updates every 3 months
- Compose BOM: Update when stable releases available
- Hilt: Update with Kotlin version compatibility
- ExoPlayer: Update for security fixes only

### Annual Updates
- Gradle Plugin: Major version updates (7.x → 8.x)
- Kotlin: Update with stable releases (avoid EAP)
- Target SDK: Update annually (Google Play requirement)

### Critical Updates (Immediate)
- Security vulnerabilities in network libraries
- Native library crashes (BouncyCastle, SMBJ)
- Google Play policy changes (page size, permissions)

---

## Known Issues & Workarounds

### BouncyCastle Native Crash
**Issue**: SMBJ 0.12.1 with BouncyCastle 1.78.1 causes native library crash (`libpenguin.so`)  
**Solution**: Use exact version 1.78.1, avoid 1.79+

### FTP PASV Timeouts
**Issue**: Passive mode connections timeout on some servers  
**Solution**: Fallback to active mode, never call `completePendingCommand()` after errors

### Compose Icons Size
**Issue**: `material-icons-extended` adds ~13 MB to APK  
**Solution**: Use selectively, consider custom vector drawables

### 16 KB Page Size
**Issue**: Android 15+ devices with 16 KB page size reject apps without alignment  
**Solution**: Set `jniLibs.useLegacyPackaging = false` and `splits.abi.isEnable = false`

---

## Dependency Graph Summary

```
FastMediaSorter v2
├── UI Layer (Activities/Fragments)
│   ├── Jetpack Compose (experimental)
│   ├── Material Design 3
│   └── Navigation Component
│
├── ViewModel Layer
│   ├── Hilt DI
│   ├── Lifecycle components
│   └── Coroutines
│
├── Domain Layer (UseCases)
│   └── Repository interfaces
│
├── Data Layer
│   ├── Room Database
│   ├── DataStore Preferences
│   ├── Network Clients (SMB/SFTP/FTP)
│   ├── Cloud APIs (Drive/Dropbox/OneDrive)
│   └── Glide Image Loading
│
└── Support Libraries
    ├── ExoPlayer (media playback)
    ├── ML Kit + Tesseract (OCR)
    ├── EPUB4J + JSoup (documents)
    └── Timber (logging)
```

---

## Maintenance Checklist

### Monthly
- [ ] Check for security advisories in network libraries
- [ ] Monitor crash reports for native library issues
- [ ] Review ProGuard warnings in release builds

### Quarterly
- [ ] Update AndroidX libraries to latest stable
- [ ] Test on latest Android version (beta channel)
- [ ] Review dependency sizes with `./gradlew :app_v2:dependencies`

### Annually
- [ ] Update target SDK (Google Play requirement)
- [ ] Audit unused dependencies
- [ ] Review ProGuard rules for new libraries
- [ ] Test on latest Android Studio version

---

**Last Updated**: January 6, 2026  
**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
