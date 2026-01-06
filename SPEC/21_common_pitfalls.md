# 21. Common Pitfalls & Solutions

## Overview

This document catalogs issues discovered during FastMediaSorter v2 development, their root causes, and proven solutions. Reading this before implementing similar features can save hours of debugging.

---

## 1. Parallel Loading Race Conditions

### Problem

Multiple coroutines loading the same resource simultaneously cause:
- Duplicate network requests
- UI flickering (list loads 2-3 times)
- Wasted bandwidth and battery
- Inconsistent state (last-finish-wins)

### Root Cause

User actions trigger coroutines faster than they complete:
```kotlin
// BAD: Each click starts new coroutine
fun loadFiles() {
    viewModelScope.launch {
        val files = repository.getFiles() // 2-3 seconds
        _files.value = files
    }
}

// User double-clicks → 2 coroutines → 2 network requests
```

### Solution

**Check and cancel existing jobs before starting new one:**

```kotlin
private var loadingJob: Job? = null

fun loadFiles(resourceId: Long) {
    // Cancel previous loading if still active
    if (loadingJob?.isActive == true) {
        loadingJob?.cancel()
    }
    
    loadingJob = viewModelScope.launch {
        _uiState.value = BrowseUiState.Loading
        
        try {
            val files = getMediaFilesUseCase(resourceId).getOrThrow()
            _uiState.value = BrowseUiState.Success(files)
        } catch (e: CancellationException) {
            // Cancelled by newer request, ignore
        } catch (e: Exception) {
            _uiState.value = BrowseUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

**Benefits**:
- Only one request in-flight at a time
- Previous requests cancelled immediately
- No wasted resources
- Predictable state

---

## 2. Async ListAdapter Timing Issues

### Problem

Code checks `adapter.itemCount` immediately after `submitList()` but always sees 0:

```kotlin
// BUG: itemCount always 0
adapter.submitList(files)
if (adapter.itemCount == 0) {
    showEmptyState() // Always shows, even when files exist
}
```

### Root Cause

`ListAdapter.submitList()` is **asynchronous**:
1. Method returns immediately
2. Diff calculation happens on background thread
3. UI update happens on next frame (16ms+ later)

`itemCount` reflects the **old list** until diff completes.

### Solution

**Move logic into `submitList()` callback:**

```kotlin
adapter.submitList(files) {
    // Callback runs AFTER list actually updates
    if (adapter.itemCount == 0) {
        binding.emptyView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    } else {
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }
}
```

**Alternative**: Use StateFlow

```kotlin
// In ViewModel
private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
val files: StateFlow<List<MediaFile>> = _files.asStateFlow()

// In Activity
lifecycleScope.launch {
    viewModel.files.collect { files ->
        adapter.submitList(files)
        
        // React to state, not adapter
        if (files.isEmpty()) {
            showEmptyState()
        }
    }
}
```

---

## 3. FTP PASV Mode Timeouts

### Problem

FTP file downloads timeout after 30 seconds on some networks:
```
java.net.SocketTimeoutException: Read timed out
```

Works fine on local network, fails on remote servers.

### Root Cause

**PASV (Passive) mode** uses random high ports (1024-65535) for data connection:
1. Control connection: port 21 (open)
2. Data connection: random port (firewall blocks)
3. Client can't reach data port → timeout

**Active mode** uses client-initiated connections (works through NAT/firewalls).

### Solution

**Try PASV first, fallback to Active mode:**

```kotlin
suspend fun downloadFile(
    remotePath: String,
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        // Try PASV mode first (works in most cases)
        ftpClient.enterLocalPassiveMode()
        
        try {
            ftpClient.retrieveFile(remotePath, outputStream)
            Result.success(Unit)
        } catch (e: SocketTimeoutException) {
            Timber.w("PASV mode timeout, trying active mode")
            
            // Fallback to active mode
            ftpClient.enterLocalActiveMode()
            ftpClient.retrieveFile(remotePath, outputStream)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        // CRITICAL: Do NOT call completePendingCommand() here
        Result.failure(e)
    }
}
```

**⚠️ Critical Rule**: **NEVER** call `completePendingCommand()` after exceptions
- It hangs indefinitely waiting for server reply
- Server already closed connection after error
- Thread blocks forever

---

## 4. Network Image Editing

### Problem

Image editing (rotation, filters) fails on SMB/SFTP files:
```
java.io.IOException: Cannot write to network stream
```

Local files edit fine.

### Root Cause

Image libraries (PhotoEditor, Glide BitmapTransformation) expect **file paths**, not input streams:
- Load from path → decode → edit → save to **same path**
- Network files have no local path
- Can't write directly to `smb://server/share/file.jpg`

### Solution

**Download → Edit → Upload pattern:**

```kotlin
class NetworkImageEditUseCase @Inject constructor(
    private val networkFileDownloader: NetworkFileDownloader,
    private val imageEditUseCase: ImageEditUseCase,
    private val fileOperationHandler: FileOperationHandler
) {
    suspend operator fun invoke(
        networkFile: MediaFile,
        editOperation: EditOperation
    ): Result<Unit> {
        return try {
            // Step 1: Download to temp file
            val tempFile = File.createTempFile("edit_", ".jpg", context.cacheDir)
            networkFileDownloader.download(networkFile, tempFile).getOrThrow()
            
            // Step 2: Edit local temp file
            imageEditUseCase(tempFile, editOperation).getOrThrow()
            
            // Step 3: Upload edited file back
            fileOperationHandler.uploadFile(tempFile, networkFile.path).getOrThrow()
            
            // Step 4: Clean up
            tempFile.delete()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Benefits**:
- Works with all image editing libraries
- User sees progress for download + upload
- Temp file auto-cleaned on failure (try-finally)

**Performance**:
- 5MB image on SMB: ~2 seconds download + 1 second edit + 2 seconds upload = 5 seconds total
- Acceptable for user-initiated actions

---

## 5. Touch Zones Blocking Video Controls

### Problem

Touch zones for image navigation (tap left/right to go Previous/Next) remain active during video playback. Users tap ExoPlayer controls (play/pause, speed adjust, forward 30s) but app navigates to next file instead.

**Symptoms**:
- Can't change playback speed
- Can't seek video
- Forward/Rewind buttons don't work
- Every tap on lower screen navigates to next file

### Root Cause

Touch zones designed for image viewing covered **entire screen**:

```xml
<!-- Touch zones overlay - BLOCKS ExoPlayer controls -->
<com.sza.fastmediasorter.ui.custom.TouchZonesOverlay
    android:id="@+id/touchZonesOverlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent" /> <!-- Full screen! -->
```

ExoPlayer controls (bottom toolbar) were behind touch zones:
1. User taps "Speed" button
2. Touch zone intercepts event first
3. `onPreviousClick()` or `onNextClick()` triggered
4. Video never sees the tap

### Solution

**Disable touch zones completely for video/audio:**

```kotlin
fun adjustTouchZonesForVideo(mediaType: MediaType) {
    when (mediaType) {
        MediaType.VIDEO, MediaType.AUDIO -> {
            // Hide touch zones completely
            binding.touchZonesOverlay.visibility = View.GONE
            
            // ExoPlayer has its own Previous/Next buttons in command panel
        }
        MediaType.IMAGE, MediaType.GIF -> {
            // Show touch zones for quick navigation
            binding.touchZonesOverlay.visibility = View.VISIBLE
        }
        MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> {
            // Hide for documents (use swipe gestures instead)
            binding.touchZonesOverlay.visibility = View.GONE
        }
    }
}
```

**Alternative Approach** (rejected): Resize touch zones to top 50% of screen
- **Problem**: Users expect bottom controls to work, but touch zones still blocked some area
- **Better**: Complete separation by media type

**Result**:
- Video/audio: Full ExoPlayer control panel access
- Images: Quick tap navigation works as expected
- No accidental navigation during video playback

---

## 6. 16 KB Page Size Compatibility (Android 15+)

### Problem

APK fails to install on Android 15+ devices with 16 KB page size:
```
INSTALL_FAILED_BAD_DEX: Failed to extract native libraries, res=-2
```

**Affected devices**: Pixel 9, OnePlus 13, Samsung Galaxy S25 (all 2025+ flagship phones)

### Root Cause

Native libraries (Tesseract OCR: `libtesseract.so`, `libleptonica.so`, `libjpeg.so`, `libpng.so`) compiled with 4 KB page alignment:

```bash
# Check ELF headers
readelf -l lib/arm64-v8a/libtesseract.so | grep LOAD
LOAD  0x000000 0x00000000 0x00000000 0x123000 0x123000 R E 0x1000  # 4KB alignment
```

Android 15+ requires **16 KB** alignment for 16 KB page size devices.

### Solution

**Configure AGP for 16 KB alignment:**

```properties
# gradle.properties
android.bundle.enableNativeLibraryAlignment=true
```

```kotlin
// app_v2/build.gradle.kts
android {
    packaging {
        jniLibs {
            useLegacyPackaging = false  // Use new packaging with alignment
        }
    }
    
    androidResources {
        noCompress += "so"  // Prevent compression of native libraries
    }
}
```

**Verification**:
```powershell
# Extract APK
unzip -q app_v2-debug.apk -d apk_extracted

# Check alignment (should show 0x4000 = 16 KB)
readelf -l apk_extracted/lib/arm64-v8a/libtesseract.so | grep LOAD
LOAD  0x000000 0x00000000 0x00000000 0x123000 0x123000 R E 0x4000  # 16KB ✓
```

**Google Play Requirement**: Mandatory since November 1, 2025 for all apps targeting Android 15+

**Documentation**: See [docs/16KB_PAGE_SIZE_FIX.md](external_docs/docs/16KB_PAGE_SIZE_FIX.md)

---

## 7. SAF (Storage Access Framework) Performance

### Problem

Scanning folders with subfolders (e.g., WhatsApp Media: 1,500 files in 200 subfolders) takes 20+ seconds. Users think app is frozen.

### Root Cause

Sequential scanning with SAF `DocumentFile.listFiles()` is slow:
```kotlin
// SLOW: Sequential scanning
fun scanFolder(folder: DocumentFile): List<MediaFile> {
    return folder.listFiles().flatMap { file ->
        if (file.isDirectory) {
            scanFolder(file) // Recursive
        } else {
            listOf(file.toMediaFile())
        }
    }
}
```

SAF requires IPC call per file/folder. 1,500 files = 1,500+ IPC calls = 20 seconds.

### Solution

**Two-phase parallel scanning:**

```kotlin
suspend fun scanFolderParallel(
    folder: DocumentFile,
    parallelism: Int = 4 // Configurable in settings
): List<MediaFile> = coroutineScope {
    // Phase 1: BFS folder discovery (sequential, avoids deadlock)
    val allFolders = mutableListOf(folder)
    val queue = ArrayDeque<DocumentFile>().apply { add(folder) }
    
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        current.listFiles().filter { it.isDirectory }.forEach { subfolder ->
            allFolders.add(subfolder)
            queue.add(subfolder)
        }
    }
    
    // Phase 2: Parallel file scanning (limited concurrency)
    allFolders
        .chunked((allFolders.size / parallelism).coerceAtLeast(1))
        .map { chunk ->
            async(Dispatchers.IO) {
                chunk.flatMap { dir ->
                    dir.listFiles()
                        .filter { !it.isDirectory }
                        .map { it.toMediaFile() }
                }
            }
        }
        .awaitAll()
        .flatten()
}
```

**Result**: WhatsApp folder: 20s → 7s (~3x speedup)

**Settings Integration**: `parallelism` limited to 2-8 for SAF (prevents deadlock)

---

## 8. Glide Memory Leaks with Network Files

### Problem

App memory usage grows 200MB+ after browsing 500+ network images. `OutOfMemoryError` on large collections.

### Root Cause

Glide's default `DiskCacheStrategy.AUTOMATIC` doesn't cache network thumbnails well:
- Downloads full 5MB image every time
- Decodes in memory
- No disk cache hit
- 500 images × 5MB = 2.5GB memory thrashing

### Solution

**Custom NetworkFileModelLoader + RESOURCE cache strategy:**

```kotlin
@GlideModule
class NetworkFileGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            NetworkFileModel::class.java,
            InputStream::class.java,
            NetworkFileModelLoaderFactory(context)
        )
    }
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setMemoryCache(
            LruResourceCache((Runtime.getRuntime().maxMemory() / 4).toLong()) // 40% heap
        )
        
        val cacheSize = settingsRepository.getThumbnailCacheSize() // User configurable: 2GB default
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "image_cache", cacheSize))
    }
}

// Usage in adapter
Glide.with(imageView)
    .load(NetworkFileModel(mediaFile.path, mediaFile.size))
    .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache decoded thumbnail
    .thumbnail(0.1f) // 10% size placeholder
    .into(imageView)
```

**Benefits**:
- Thumbnail cached after first load
- Subsequent loads: disk cache hit < 50ms
- Memory usage stable at ~80MB for 500 images

---

## Prevention Strategies

### 1. Early Testing with Edge Cases

Test with:
- **Large collections**: 1,000+ files
- **Slow networks**: Enable network throttling in Android Studio
- **Low memory**: Run on 2GB RAM device or set `android:largeHeap="false"`
- **Different Android versions**: Min API 28, target API 34

### 2. Code Reviews Focus on

- ✅ Job cancellation before new coroutine launch
- ✅ Async callback handling (ListAdapter, network requests)
- ✅ Try-catch on all network/file operations
- ✅ Resource cleanup in finally blocks

### 3. Logging for Diagnostics

```kotlin
// GOOD: Structured logging with context
Timber.d("Loading files: resourceId=$resourceId, protocol=${resource.type}")
Timber.d("Files loaded: count=${files.size}, duration=${elapsed}ms")

// BAD: Vague logging
Timber.d("Loading...")
Timber.d("Done")
```

### 4. User Feedback During Long Operations

```kotlin
// Show progress for > 1 second operations
if (fileCount > 100) {
    showProgressDialog("Loading $fileCount files...")
}

// Update progress for > 3 second operations
downloadFile(file) { progress, total ->
    updateProgressDialog("${progress / total * 100}%")
}
```

---

## Related Documentation

- [17. Architecture Patterns](17_architecture_patterns.md) - Preventing issues with good architecture
- [18. Development Workflows](18_development_workflows.md) - Testing for these issues
- [20. Protocol Implementations](20_protocol_implementations.md) - Network protocol specifics
- [22. Cache Strategies](22_cache_strategies.md) - Glide configuration details
