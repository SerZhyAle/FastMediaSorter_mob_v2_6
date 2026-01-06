package com.sza.fastmediasorter.ui.browse.filelist

import com.sza.fastmediasorter.core.cache.MediaFilesCacheManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.SortMode
import timber.log.Timber

/**
 * Manages file list manipulation operations for BrowseViewModel.
 * Handles adding, removing, updating files with proper sorting and deduplication.
 * 
 * Performance optimizations:
 * - Lazy sorting: Only sorts when needed
 * - Deduplication: O(n) using HashSet
 * - Cache synchronization: Automatic cache updates after mutations
 */
class BrowseFileListManager(
    private val resourceId: Long
) {
    
    /**
     * Remove files from list by paths.
     * Updates cache automatically.
     * 
     * @param currentList Current file list
     * @param pathsToRemove Paths to remove
     * @return Updated list without removed files
     */
    fun removeFiles(currentList: List<MediaFile>, pathsToRemove: List<String>): List<MediaFile> {
        if (pathsToRemove.isEmpty()) return currentList
        
        val pathSet = pathsToRemove.toSet() // O(1) lookup
        val updatedFiles = currentList.filterNot { it.path in pathSet }
        
        Timber.d("removeFiles: Removed ${pathsToRemove.size} files, ${updatedFiles.size} remaining")
        
        // Update cache
        MediaFilesCacheManager.setCachedList(resourceId, updatedFiles)
        
        return updatedFiles
    }
    
    /**
     * Add new files to list with deduplication and sorting.
     * Updates cache automatically.
     * 
     * @param currentList Current file list
     * @param newFiles Files to add
     * @param sortMode Current sort mode
     * @return Updated list with new files added and sorted
     */
    fun addFiles(currentList: List<MediaFile>, newFiles: List<MediaFile>, sortMode: SortMode): List<MediaFile> {
        if (newFiles.isEmpty()) return currentList
        
        // Combine and deduplicate by path (O(n) with HashSet)
        val allPaths = mutableSetOf<String>()
        val updatedFiles = (currentList + newFiles).filter { file ->
            if (file.path in allPaths) {
                false // Skip duplicate
            } else {
                allPaths.add(file.path)
                true
            }
        }
        
        // Sort according to current mode
        val sortedFiles = sortFiles(updatedFiles, sortMode, forceSort = true)
        
        Timber.d("addFiles: Added ${newFiles.size} files, ${sortedFiles.size} total after dedup and sort")
        
        // Update cache
        MediaFilesCacheManager.setCachedList(resourceId, sortedFiles)
        
        return sortedFiles
    }
    
    /**
     * Update single file in list (e.g., after rename).
     * Re-sorts list after update.
     * Updates cache automatically.
     * 
     * @param currentList Current file list
     * @param oldPath Original file path
     * @param newFile Updated MediaFile object
     * @param sortMode Current sort mode
     * @return Updated list with file replaced and re-sorted
     */
    fun updateFile(currentList: List<MediaFile>, oldPath: String, newFile: MediaFile, sortMode: SortMode): List<MediaFile> {
        val updatedFiles = currentList.map { file ->
            if (file.path == oldPath) newFile else file
        }
        
        Timber.d("updateFile: Updated file $oldPath -> ${newFile.path}")
        
        // Update cache
        MediaFilesCacheManager.setCachedList(resourceId, updatedFiles)
        
        // Re-sort if needed
        val sortedFiles = sortFiles(updatedFiles, sortMode, forceSort = true)
        
        return sortedFiles
    }
    
    /**
     * Sort files according to sort mode.
     * Optimized: Only sorts if forceSort=true or list is not already sorted.
     * 
     * @param files Files to sort
     * @param sortMode Sort mode to apply
     * @param forceSort Force sorting even if list might be already sorted
     * @return Sorted list
     */
    fun sortFiles(files: List<MediaFile>, sortMode: SortMode, forceSort: Boolean = false): List<MediaFile> {
        if (!forceSort && files.size < 2) return files // Optimization: single-item list is always sorted
        
        return when (sortMode) {
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortMode.DATE_ASC -> files.sortedBy { it.createdDate }
            SortMode.DATE_DESC -> files.sortedByDescending { it.createdDate }
            SortMode.SIZE_ASC -> files.sortedBy { it.size }
            SortMode.SIZE_DESC -> files.sortedByDescending { it.size }
            SortMode.TYPE_ASC -> files.sortedBy { it.type.ordinal }
            SortMode.TYPE_DESC -> files.sortedByDescending { it.type.ordinal }
            SortMode.MANUAL -> files // No sorting for manual mode
            SortMode.RANDOM -> files.shuffled()
        }
    }
    
    /**
     * Synchronize list with cache (used when cache updates externally).
     * 
     * @param currentList Current file list
     * @param cacheList List from cache
     * @return Cache list if different, current list otherwise
     */
    fun syncWithCache(currentList: List<MediaFile>, cacheList: List<MediaFile>?): List<MediaFile> {
        if (cacheList == null) return currentList
        
        // Check if lists are different by comparing sizes and first/last items
        if (cacheList.size != currentList.size) {
            Timber.d("syncWithCache: Size mismatch (cache=${cacheList.size}, current=${currentList.size}), using cache")
            return cacheList
        }
        
        if (cacheList.isNotEmpty() && currentList.isNotEmpty()) {
            if (cacheList.first().path != currentList.first().path || 
                cacheList.last().path != currentList.last().path) {
                Timber.d("syncWithCache: Content mismatch, using cache")
                return cacheList
            }
        }
        
        return currentList // Lists are the same
    }
}
