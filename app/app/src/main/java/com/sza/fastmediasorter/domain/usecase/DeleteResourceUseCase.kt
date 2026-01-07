package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.data.cache.UnifiedFileCache
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for deleting a resource.
 * Handles cleanup and validation.
 */
class DeleteResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val fileMetadataRepository: FileMetadataRepository,
    private val credentialsRepository: NetworkCredentialsRepository,
    private val unifiedFileCache: UnifiedFileCache
) {

    /**
     * Delete a resource by ID.
     * 
     * @param resourceId The ID of the resource to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(resourceId: Long): Result<Unit> {
        return try {
            // Get the resource first
            val resource = resourceRepository.getResourceById(resourceId)
            if (resource == null) {
                Timber.w("DeleteResourceUseCase: Resource $resourceId not found")
                return Result.Error(
                    message = "Resource not found",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Delete the resource
            resourceRepository.deleteResource(resource)
            Timber.d("DeleteResourceUseCase: Deleted resource $resourceId - ${resource.name}")
            
            // Clean up related data
            cleanupResourceData(resource)
            
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Error deleting resource $resourceId")
            Result.Error(
                message = e.message ?: "Failed to delete resource",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }
    
    /**
     * Clean up all data associated with a deleted resource.
     */
    private suspend fun cleanupResourceData(resource: Resource) {
        try {
            // Delete file metadata for files from this resource
            fileMetadataRepository.deleteMetadataByResourcePath(resource.path)
            Timber.d("Cleaned up file metadata for resource: ${resource.name}")
            
            // Delete network credentials if this was a network resource
            if (resource.type != ResourceType.LOCAL && resource.credentialsId != null) {
                credentialsRepository.deleteCredentials(resource.credentialsId)
                Timber.d("Deleted network credentials for resource: ${resource.name}")
            }
            
            // Clear network file cache for this resource path
            // Note: UnifiedFileCache uses hash-based keys, so we can't easily clear by path
            // The cache will naturally expire (24h TTL) or be evicted by LRU
            Timber.d("Resource cleanup complete: ${resource.name}")
            
        } catch (e: Exception) {
            // Log but don't fail the deletion if cleanup fails
            Timber.w(e, "Error during resource cleanup: ${resource.name}")
        }
    }

    /**
     * Delete a resource object.
     * 
     * @param resource The resource to delete
     * @return Result indicating success or failure
     */
    suspend fun delete(resource: Resource): Result<Unit> {
        return invoke(resource.id)
    }
}
