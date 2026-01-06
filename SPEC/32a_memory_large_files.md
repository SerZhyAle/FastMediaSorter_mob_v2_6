# 32a. Memory Management & Large Files (Addition to 32)

**Last Updated**: January 6, 2026  
**Purpose**: Define file size limits and strategies for handling large files

---

## File Size Limits

### Operation Limits

| Operation | Max Size | Reason | Fallback |
|-----------|----------|--------|----------|
| **Preview (Image)** | 50 MB | Bitmap memory | Downsampled preview |
| **Preview (Video)** | Unlimited | ExoPlayer streams | N/A |
| **Edit (Image)** | 20 MB | In-memory bitmap | Show warning |
| **Edit (GIF)** | 10 MB | Frame extraction | Show warning |
| **Upload/Download** | 2 GB | Android file API limit | Chunked transfer |
| **Copy/Move (Local)** | Unlimited | Buffered streams | Progress only |

### Detection

```kotlin
object FileSizeLimits {
    const val MAX_IMAGE_PREVIEW_SIZE = 50L * 1024 * 1024  // 50 MB
    const val MAX_IMAGE_EDIT_SIZE = 20L * 1024 * 1024     // 20 MB
    const val MAX_GIF_EDIT_SIZE = 10L * 1024 * 1024       // 10 MB
    const val MAX_CHUNKED_THRESHOLD = 100L * 1024 * 1024  // 100 MB
    
    fun canEdit(file: MediaFile): Boolean {
        return when (file.type) {
            MediaType.IMAGE -> file.size <= MAX_IMAGE_EDIT_SIZE
            MediaType.GIF -> file.size <= MAX_GIF_EDIT_SIZE
            else -> true
        }
    }
    
    fun needsChunkedTransfer(size: Long): Boolean {
        return size > MAX_CHUNKED_THRESHOLD
    }
}
```

---

## Chunked Upload/Download

### Chunked Download

```kotlin
class ChunkedDownloader {
    
    suspend fun downloadLargeFile(
        url: String,
        destination: File,
        chunkSize: Long = 5L * 1024 * 1024,  // 5 MB chunks
        progressCallback: (Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val totalSize = getContentLength(url)
            var downloadedBytes = 0L
            
            destination.outputStream().use { output ->
                var offset = 0L
                
                while (offset < totalSize) {
                    val endByte = min(offset + chunkSize - 1, totalSize - 1)
                    
                    val chunk = downloadChunk(url, offset, endByte)
                    output.write(chunk)
                    
                    downloadedBytes += chunk.size
                    progressCallback(downloadedBytes, totalSize)
                    
                    offset = endByte + 1
                }
            }
            
            Result.Success(destination)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Chunked Upload (Google Drive)

```kotlin
class ChunkedUploader {
    
    suspend fun uploadLargeFile(
        file: File,
        driveService: Drive,
        chunkSize: Int = 5 * 1024 * 1024
    ): Result<String> {
        val mediaContent = FileContent(file.mimeType, file)
        
        val request = driveService.files().create(
            com.google.api.services.drive.model.File().apply {
                name = file.name
            },
            mediaContent
        )
        
        request.mediaHttpUploader.apply {
            isDirectUploadEnabled = false
            chunkSize = this@ChunkedUploader.chunkSize
            progressListener = MediaHttpUploaderProgressListener { uploader ->
                val progress = uploader.uploadState
                // Emit progress
            }
        }
        
        return try {
            val uploadedFile = request.execute()
            Result.Success(uploadedFile.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

---

## OOM Protection

### Bitmap Loading with Sampling

```kotlin
object BitmapLoader {
    
    fun loadSafely(file: File, maxSize: Int = 2048): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        BitmapFactory.decodeFile(file.path, options)
        
        options.apply {
            inSampleSize = calculateInSampleSize(this, maxSize, maxSize)
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565  // Use less memory
        }
        
        return try {
            BitmapFactory.decodeFile(file.path, options)
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "OOM loading bitmap: ${file.path}")
            null
        }
    }
    
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight &&
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
```

### Memory Monitor

```kotlin
class MemoryMonitor @Inject constructor() {
    
    fun checkMemoryAvailable(requiredBytes: Long): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = maxMemory - usedMemory
        
        Timber.d("Memory: available=${availableMemory / 1024 / 1024}MB, required=${requiredBytes / 1024 / 1024}MB")
        
        return availableMemory > requiredBytes * 1.5  // 50% safety margin
    }
    
    fun lowMemoryWarning(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedPercent = (runtime.totalMemory() - runtime.freeMemory()) * 100 / runtime.maxMemory()
        return usedPercent > 80
    }
}
```

---

## User Warnings

```kotlin
sealed class FileSizeWarning {
    data class TooLargeToEdit(val sizeMB: Int, val maxMB: Int) : FileSizeWarning()
    data class SlowOperation(val sizeMB: Int) : FileSizeWarning()
    object LowMemory : FileSizeWarning()
}

fun FileSizeWarning.toUserMessage(context: Context): String {
    return when (this) {
        is TooLargeToEdit -> context.getString(
            R.string.file_too_large_to_edit,
            sizeMB,
            maxMB
        )
        is SlowOperation -> context.getString(
            R.string.large_file_warning,
            sizeMB
        )
        LowMemory -> context.getString(R.string.low_memory_warning)
    }
}
```

---

## References

- [32_performance_metrics.md](32_performance_metrics.md) - Main performance doc
- [21_common_pitfalls.md](21_common_pitfalls.md) - OOM issues
