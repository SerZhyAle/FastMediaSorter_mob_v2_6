package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.scanner.LocalMediaScanner
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.repository.MediaRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaRepository.
 * Currently supports local file scanning, with network support to come.
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val localMediaScanner: LocalMediaScanner
) : MediaRepository {

    // In-memory cache for scanned files (per resource)
    private val fileCache = mutableMapOf<Long, List<MediaFile>>()

    override suspend fun getFilesForResource(resourceId: Long): List<MediaFile> {
        // Check cache first
        fileCache[resourceId]?.let { cached ->
            Timber.d("Returning cached files for resource $resourceId: ${cached.size} files")
            return cached
        }

        // Get resource info
        val resource = resourceRepository.getResourceById(resourceId)
        if (resource == null) {
            Timber.w("Resource not found: $resourceId")
            return emptyList()
        }

        // Scan based on resource type
        val files = when (resource.type) {
            ResourceType.LOCAL -> {
                localMediaScanner.scanFolder(resource.path, recursive = false)
            }
            ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP -> {
                // TODO: Implement network scanning
                Timber.d("Network scanning not yet implemented for ${resource.type}")
                emptyList()
            }
            ResourceType.GOOGLE_DRIVE, ResourceType.ONEDRIVE, ResourceType.DROPBOX -> {
                // TODO: Implement cloud scanning
                Timber.d("Cloud scanning not yet implemented for ${resource.type}")
                emptyList()
            }
        }

        // Cache the results
        fileCache[resourceId] = files
        Timber.d("Scanned and cached ${files.size} files for resource $resourceId")

        return files
    }

    override suspend fun scanResource(resourceId: Long, forceRefresh: Boolean) {
        if (forceRefresh) {
            fileCache.remove(resourceId)
        }
        // Trigger scan by getting files
        getFilesForResource(resourceId)
    }

    override suspend fun getFileByPath(path: String): MediaFile? {
        // Search in all caches
        fileCache.values.forEach { files ->
            files.find { it.path == path }?.let { return it }
        }
        return null
    }

    override suspend fun clearCacheForResource(resourceId: Long) {
        fileCache.remove(resourceId)
        Timber.d("Cleared cache for resource $resourceId")
    }
}
