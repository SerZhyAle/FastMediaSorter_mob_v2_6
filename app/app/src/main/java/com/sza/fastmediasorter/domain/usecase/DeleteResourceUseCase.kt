package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for deleting a resource.
 * Handles cleanup and validation.
 */
class DeleteResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
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
            
            // TODO: Clean up related data (cached files, metadata, etc.)
            
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
     * Delete a resource object.
     * 
     * @param resource The resource to delete
     * @return Result indicating success or failure
     */
    suspend fun delete(resource: Resource): Result<Unit> {
        return invoke(resource.id)
    }
}
