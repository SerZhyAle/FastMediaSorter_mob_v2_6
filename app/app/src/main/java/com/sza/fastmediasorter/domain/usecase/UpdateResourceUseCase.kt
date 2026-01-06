package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for updating an existing resource.
 */
class UpdateResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) {

    /**
     * Update a resource.
     * 
     * @param resource The resource with updated values
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(resource: Resource): Result<Unit> {
        // Validate input
        if (resource.name.isBlank()) {
            return Result.Error(
                message = "Resource name cannot be empty",
                errorCode = ErrorCode.INVALID_INPUT
            )
        }

        return try {
            // Verify resource exists
            val existing = resourceRepository.getResourceById(resource.id)
            if (existing == null) {
                return Result.Error(
                    message = "Resource not found",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Update the resource
            resourceRepository.updateResource(resource)
            Timber.d("UpdateResourceUseCase: Updated resource ${resource.id} - ${resource.name}")
            
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating resource: ${resource.id}")
            Result.Error(
                message = e.message ?: "Failed to update resource",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }

    /**
     * Update resource sort mode.
     */
    suspend fun updateSortMode(resourceId: Long, sortMode: SortMode): Result<Unit> {
        return try {
            val resource = resourceRepository.getResourceById(resourceId)
                ?: return Result.Error(
                    message = "Resource not found",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )

            val updated = resource.copy(sortMode = sortMode)
            resourceRepository.updateResource(updated)
            Timber.d("Updated sort mode for resource $resourceId to $sortMode")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating sort mode for resource $resourceId")
            Result.Error(
                message = e.message ?: "Failed to update sort mode",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }

    /**
     * Update resource display mode.
     */
    suspend fun updateDisplayMode(resourceId: Long, displayMode: DisplayMode): Result<Unit> {
        return try {
            val resource = resourceRepository.getResourceById(resourceId)
                ?: return Result.Error(
                    message = "Resource not found",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )

            val updated = resource.copy(displayMode = displayMode)
            resourceRepository.updateResource(updated)
            Timber.d("Updated display mode for resource $resourceId to $displayMode")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating display mode for resource $resourceId")
            Result.Error(
                message = e.message ?: "Failed to update display mode",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }

    /**
     * Toggle destination status for a resource.
     */
    suspend fun toggleDestination(resourceId: Long): Result<Boolean> {
        return try {
            val resource = resourceRepository.getResourceById(resourceId)
                ?: return Result.Error(
                    message = "Resource not found",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )

            val newState = !resource.isDestination
            val updated = resource.copy(isDestination = newState)
            resourceRepository.updateResource(updated)
            Timber.d("Toggled destination for resource $resourceId to $newState")
            
            Result.Success(newState)
        } catch (e: Exception) {
            Timber.e(e, "Error toggling destination for resource $resourceId")
            Result.Error(
                message = e.message ?: "Failed to toggle destination",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }
}
