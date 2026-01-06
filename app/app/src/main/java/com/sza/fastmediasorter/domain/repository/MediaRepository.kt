package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.MediaFile

/**
 * Repository interface for media file operations.
 * Abstracts data source details from domain layer.
 */
interface MediaRepository {
    
    /**
     * Get all media files for a specific resource.
     * @param resourceId The ID of the resource to get files for
     * @return List of MediaFile objects
     */
    suspend fun getFilesForResource(resourceId: Long): List<MediaFile>
    
    /**
     * Scan and cache files for a resource.
     * @param resourceId The ID of the resource
     * @param forceRefresh Force rescan even if cached
     */
    suspend fun scanResource(resourceId: Long, forceRefresh: Boolean = false)
    
    /**
     * Get a single media file by path.
     */
    suspend fun getFileByPath(path: String): MediaFile?
    
    /**
     * Delete cached metadata for a resource.
     */
    suspend fun clearCacheForResource(resourceId: Long)
}
