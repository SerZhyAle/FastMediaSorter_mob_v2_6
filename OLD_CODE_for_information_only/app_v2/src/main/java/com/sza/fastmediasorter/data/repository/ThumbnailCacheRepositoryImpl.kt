package com.sza.fastmediasorter.data.repository

import android.content.Context
import com.sza.fastmediasorter.data.local.db.ThumbnailCacheDao
import com.sza.fastmediasorter.data.local.db.ThumbnailCacheEntity
import com.sza.fastmediasorter.domain.repository.CacheStats
import com.sza.fastmediasorter.domain.repository.ThumbnailCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ThumbnailCacheRepository.
 * Stores thumbnails in app cache directory with Room database index.
 * 
 * Cache directory: context.cacheDir/thumbnails/
 * Database: thumbnail_cache table
 */
@Singleton
class ThumbnailCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thumbnailCacheDao: ThumbnailCacheDao
) : ThumbnailCacheRepository {
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, "thumbnails").apply {
            if (!exists()) {
                mkdirs()
                Timber.d("ThumbnailCache: Created cache directory: $absolutePath")
            }
        }
    }
    
    override suspend fun getCachedThumbnail(filePath: String): File? {
        return try {
            val cacheEntry = thumbnailCacheDao.getThumbnail(filePath) ?: return null
            val thumbnailFile = File(cacheEntry.thumbnailPath)
            
            if (!thumbnailFile.exists()) {
                // File deleted but database entry exists - clean up
                Timber.w("ThumbnailCache: Cached file missing, deleting entry: $filePath")
                thumbnailCacheDao.deleteThumbnail(filePath)
                return null
            }
            
            // Update access timestamp
            thumbnailCacheDao.updateAccessTime(filePath, System.currentTimeMillis())
            
            Timber.d("ThumbnailCache: Cache HIT for $filePath")
            thumbnailFile
        } catch (e: Exception) {
            Timber.e(e, "ThumbnailCache: Error getting cached thumbnail for $filePath")
            null
        }
    }
    
    override suspend fun saveThumbnail(filePath: String, thumbnailFile: File) {
        try {
            if (!thumbnailFile.exists()) {
                Timber.w("ThumbnailCache: Cannot save non-existent file: ${thumbnailFile.absolutePath}")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            val cacheEntry = ThumbnailCacheEntity(
                filePath = filePath,
                thumbnailPath = thumbnailFile.absolutePath,
                fileSize = thumbnailFile.length(),
                createdAt = currentTime,
                lastAccessedAt = currentTime
            )
            
            thumbnailCacheDao.insertThumbnail(cacheEntry)
            Timber.d("ThumbnailCache: Saved thumbnail for $filePath (${thumbnailFile.length()} bytes)")
        } catch (e: Exception) {
            Timber.e(e, "ThumbnailCache: Error saving thumbnail for $filePath")
        }
    }
    
    override suspend fun deleteThumbnail(filePath: String) {
        try {
            val cacheEntry = thumbnailCacheDao.getThumbnail(filePath)
            if (cacheEntry != null) {
                // Delete physical file
                val thumbnailFile = File(cacheEntry.thumbnailPath)
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                    Timber.d("ThumbnailCache: Deleted physical file: ${thumbnailFile.absolutePath}")
                }
                
                // Delete database entry
                thumbnailCacheDao.deleteThumbnail(filePath)
                Timber.d("ThumbnailCache: Deleted cache entry for $filePath")
            }
        } catch (e: Exception) {
            Timber.e(e, "ThumbnailCache: Error deleting thumbnail for $filePath")
        }
    }
    
    override suspend fun cleanupOldThumbnails(days: Int): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
            
            // Get old entries to delete physical files
            val oldEntries = thumbnailCacheDao.getThumbnailsOlderThan(cutoffTime)
            
            // Delete physical files
            var deletedFiles = 0
            oldEntries.forEach { entry ->
                val file = File(entry.thumbnailPath)
                if (file.exists() && file.delete()) {
                    deletedFiles++
                }
            }
            
            // Delete database entries
            val deletedEntries = thumbnailCacheDao.deleteOldThumbnails(cutoffTime)
            
            Timber.i("ThumbnailCache: Cleanup completed - deleted $deletedEntries entries, $deletedFiles files (older than $days days)")
            deletedEntries
        } catch (e: Exception) {
            Timber.e(e, "ThumbnailCache: Error during cleanup")
            0
        }
    }
    
    override suspend fun getCacheStats(): CacheStats {
        return try {
            val count = thumbnailCacheDao.getCacheCount()
            val size = thumbnailCacheDao.getTotalCacheSize()
            CacheStats(count, size)
        } catch (e: Exception) {
            Timber.e(e, "ThumbnailCache: Error getting cache stats")
            CacheStats(0, 0)
        }
    }
}
