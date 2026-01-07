package com.sza.fastmediasorter.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified cache for network files (SMB, SFTP, FTP, Cloud).
 * 
 * Features:
 * - LRU eviction when cache exceeds size limit
 * - Hash-based file naming for collision avoidance
 * - 24-hour expiration for cached files
 * - Thread-safe operations
 */
@Singleton
class UnifiedFileCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CACHE_DIR_NAME = "network_cache"
        private const val MAX_CACHE_SIZE_MB = 500L
        private const val MAX_CACHE_SIZE_BYTES = MAX_CACHE_SIZE_MB * 1024 * 1024
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
                Timber.d("Created cache directory: $absolutePath")
            }
        }
    }

    /**
     * Get cached file or null if not cached/expired.
     * 
     * @param remotePath Remote file path
     * @param modifiedTime Last modified timestamp (for cache validation)
     * @return Cached file or null
     */
    suspend fun getCachedFile(remotePath: String, modifiedTime: Long): File? {
        return withContext(Dispatchers.IO) {
            val cacheKey = generateCacheKey(remotePath, modifiedTime)
            val cachedFile = File(cacheDir, cacheKey)

            if (!cachedFile.exists()) {
                Timber.d("Cache miss: $remotePath")
                return@withContext null
            }

            // Check expiration
            val age = System.currentTimeMillis() - cachedFile.lastModified()
            if (age > CACHE_EXPIRATION_MS) {
                Timber.d("Cache expired: $remotePath (age: ${age / 1000}s)")
                cachedFile.delete()
                return@withContext null
            }

            Timber.d("Cache hit: $remotePath")
            cachedFile
        }
    }

    /**
     * Cache a remote file.
     * 
     * @param remotePath Remote file path
     * @param modifiedTime Last modified timestamp
     * @param inputStream Input stream with file data
     * @return Cached file
     */
    suspend fun cacheFile(
        remotePath: String,
        modifiedTime: Long,
        inputStream: InputStream
    ): File {
        return withContext(Dispatchers.IO) {
            val cacheKey = generateCacheKey(remotePath, modifiedTime)
            val cachedFile = File(cacheDir, cacheKey)

            // Write to cache
            inputStream.use { input ->
                cachedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Timber.d("Cached file: $remotePath -> $cacheKey (${cachedFile.length()} bytes)")

            // Perform LRU eviction if needed
            evictIfNeeded()

            cachedFile
        }
    }

    /**
     * Cache a remote file from byte array.
     * 
     * @param remotePath Remote file path
     * @param modifiedTime Last modified timestamp
     * @param data File data
     * @return Cached file
     */
    suspend fun cacheFile(
        remotePath: String,
        modifiedTime: Long,
        data: ByteArray
    ): File {
        return withContext(Dispatchers.IO) {
            val cacheKey = generateCacheKey(remotePath, modifiedTime)
            val cachedFile = File(cacheDir, cacheKey)

            cachedFile.writeBytes(data)

            Timber.d("Cached file from bytes: $remotePath -> $cacheKey (${data.size} bytes)")

            evictIfNeeded()

            cachedFile
        }
    }

    /**
     * Remove specific file from cache.
     * 
     * @param remotePath Remote file path
     * @param modifiedTime Last modified timestamp
     */
    suspend fun invalidate(remotePath: String, modifiedTime: Long) {
        withContext(Dispatchers.IO) {
            val cacheKey = generateCacheKey(remotePath, modifiedTime)
            val cachedFile = File(cacheDir, cacheKey)

            if (cachedFile.exists()) {
                cachedFile.delete()
                Timber.d("Invalidated cache: $remotePath")
            }
        }
    }

    /**
     * Clear all cached files.
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            val files = cacheDir.listFiles() ?: emptyArray()
            var deletedCount = 0
            var freedBytes = 0L

            files.forEach { file ->
                freedBytes += file.length()
                if (file.delete()) {
                    deletedCount++
                }
            }

            Timber.d("Cleared cache: $deletedCount files, ${freedBytes / 1024 / 1024} MB")
        }
    }

    /**
     * Get current cache size in bytes.
     */
    suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }

    /**
     * Get cache statistics.
     */
    suspend fun getStats(): CacheStats {
        return withContext(Dispatchers.IO) {
            val files = cacheDir.listFiles() ?: emptyArray()
            val totalSize = files.sumOf { it.length() }
            val fileCount = files.size

            CacheStats(
                fileCount = fileCount,
                totalSizeBytes = totalSize,
                maxSizeBytes = MAX_CACHE_SIZE_BYTES
            )
        }
    }

    /**
     * Generate cache key from remote path and modified time.
     */
    private fun generateCacheKey(remotePath: String, modifiedTime: Long): String {
        val input = "$remotePath|$modifiedTime"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Evict oldest files if cache exceeds size limit (LRU).
     */
    private fun evictIfNeeded() {
        val files = cacheDir.listFiles() ?: return
        val totalSize = files.sumOf { it.length() }

        if (totalSize <= MAX_CACHE_SIZE_BYTES) {
            return
        }

        Timber.d("Cache size exceeded: ${totalSize / 1024 / 1024} MB / ${MAX_CACHE_SIZE_MB} MB")

        // Sort by last modified (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }

        var freedSize = 0L
        var deletedCount = 0

        // Delete oldest files until we're under 80% of max size
        val targetSize = (MAX_CACHE_SIZE_BYTES * 0.8).toLong()

        for (file in sortedFiles) {
            if (totalSize - freedSize <= targetSize) {
                break
            }

            freedSize += file.length()
            if (file.delete()) {
                deletedCount++
            }
        }

        Timber.d("Evicted $deletedCount files, freed ${freedSize / 1024 / 1024} MB")
    }
}

/**
 * Cache statistics.
 */
data class CacheStats(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val maxSizeBytes: Long
) {
    val usagePercent: Int
        get() = ((totalSizeBytes.toDouble() / maxSizeBytes) * 100).toInt()

    val totalSizeMB: Double
        get() = totalSizeBytes / 1024.0 / 1024.0

    val maxSizeMB: Double
        get() = maxSizeBytes / 1024.0 / 1024.0
}
