package com.sza.fastmediasorter.ui.browse.cache

import com.sza.fastmediasorter.core.cache.MediaFilesCacheManager
import com.sza.fastmediasorter.domain.model.FileFilter
import com.sza.fastmediasorter.domain.model.MediaFile
import timber.log.Timber

/**
 * Manages file cache operations for BrowseViewModel.
 * Handles cache checking, filtering, and cache invalidation logic.
 */
class BrowseCacheManager(
    private val resourceId: Long,
    private val paginationThreshold: Int
) {
    /**
     * Result of cache check operation.
     */
    sealed class CacheCheckResult {
        /**
         * Cache is valid and should be used.
         * @param files The cached files to display
         */
        data class UseCache(val files: List<MediaFile>) : CacheCheckResult()
        
        /**
         * Cache should not be used, need to perform fresh scan.
         * @param reason The reason why cache was rejected
         */
        data class Rescan(val reason: String) : CacheCheckResult()
    }
    
    /**
     * Checks if cached files should be used based on cache status and size.
     * Strategy: Use cache ONLY for large resources (>= paginationThreshold) to avoid stale data.
     * For small resources, always rescan to detect external changes.
     * 
     * @param filter Optional filter to apply to cached files before returning
     * @return CacheCheckResult indicating whether to use cache or rescan
     */
    fun checkCache(filter: FileFilter?): CacheCheckResult {
        Timber.d("BrowseCacheManager: Checking cache for resource $resourceId")
        
        val cachedFiles = MediaFilesCacheManager.getCachedList(resourceId)
        Timber.d("BrowseCacheManager: Cache check result - cachedFiles=${cachedFiles?.size ?: "null"}")
        
        return when {
            // No cache found - need to scan
            cachedFiles == null -> {
                Timber.d("BrowseCacheManager: No cache found, will perform fresh scan")
                CacheCheckResult.Rescan("No cache found")
            }
            
            // Cache exists but is empty (process killed and restored empty cache) - clear and rescan
            cachedFiles.isEmpty() -> {
                Timber.w("BrowseCacheManager: Found empty cache for resource $resourceId, clearing and rescanning")
                MediaFilesCacheManager.clearCache(resourceId)
                CacheCheckResult.Rescan("Empty cache")
            }
            
            // Small resource - always rescan to show fresh data
            cachedFiles.size < paginationThreshold -> {
                Timber.d("BrowseCacheManager: Ignoring cache (${cachedFiles.size} files) - small resource, rescanning")
                MediaFilesCacheManager.clearCache(resourceId)
                CacheCheckResult.Rescan("Small resource (${cachedFiles.size} files)")
            }
            
            // Large resource - use cache with optional filter
            else -> {
                Timber.d("BrowseCacheManager: Using cached list (${cachedFiles.size} files) - large resource, skipping scan")
                val filteredFiles = if (filter != null) {
                    applyFilter(cachedFiles, filter)
                } else {
                    cachedFiles
                }
                Timber.d("BrowseCacheManager: Filtered cached list ${cachedFiles.size} → ${filteredFiles.size} files")
                CacheCheckResult.UseCache(filteredFiles)
            }
        }
    }
    
    /**
     * Applies FileFilter to a list of MediaFiles.
     * Filters by name, date range, size range, and media types.
     * 
     * @param files The files to filter
     * @param filter The filter criteria
     * @return Filtered list of files
     */
    fun applyFilter(files: List<MediaFile>, filter: FileFilter): List<MediaFile> {
        return files.filter { file ->
            val matchesName = filter.nameContains == null || 
                file.name.contains(filter.nameContains, ignoreCase = true)
            
            val matchesMinDate = filter.minDate == null || 
                file.createdDate >= filter.minDate
            
            val matchesMaxDate = filter.maxDate == null || 
                file.createdDate <= filter.maxDate
            
            val fileSizeMb = file.size / (1024f * 1024f)
            val matchesMinSize = filter.minSizeMb == null || 
                fileSizeMb >= filter.minSizeMb
            
            val matchesMaxSize = filter.maxSizeMb == null || 
                fileSizeMb <= filter.maxSizeMb
            
            val matchesType = filter.mediaTypes == null || 
                filter.mediaTypes.contains(file.type)
            
            matchesName && matchesMinDate && matchesMaxDate && 
                matchesMinSize && matchesMaxSize && matchesType
        }
    }
}
