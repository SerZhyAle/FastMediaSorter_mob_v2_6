package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for adding a new resource.
 * Handles validation and duplicate checking.
 */
class AddResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) {

    /**
     * Add a new resource.
     * 
     * @param name Display name for the resource
     * @param path Path or URI to the resource
     * @param type Type of resource (LOCAL, SMB, etc.)
     * @param sortMode Initial sort mode
     * @param displayMode Initial display mode
     * @param workWithAllFiles Whether to include non-media files
     * @return Result containing the new resource ID or error
     */
    suspend operator fun invoke(
        name: String,
        path: String,
        type: ResourceType,
        sortMode: SortMode = SortMode.DATE_DESC,
        displayMode: DisplayMode = DisplayMode.GRID,
        workWithAllFiles: Boolean = false
    ): Result<Long> {
        // Validate input
        if (name.isBlank()) {
            return Result.Error(
                message = "Resource name cannot be empty",
                errorCode = ErrorCode.INVALID_INPUT
            )
        }

        if (path.isBlank()) {
            return Result.Error(
                message = "Resource path cannot be empty",
                errorCode = ErrorCode.INVALID_INPUT
            )
        }

        return try {
            // Check for duplicate path
            val existing = resourceRepository.getResourceByPath(path)
            if (existing != null) {
                return Result.Error(
                    message = "A resource with this path already exists",
                    errorCode = ErrorCode.FILE_EXISTS
                )
            }

            // Create and insert the resource
            val resource = Resource(
                id = 0, // Will be auto-generated
                name = name.trim(),
                path = path,
                type = type,
                sortMode = sortMode,
                displayMode = displayMode,
                workWithAllFiles = workWithAllFiles
            )

            val id = resourceRepository.insertResource(resource)
            Timber.d("AddResourceUseCase: Created resource $id - $name")
            
            Result.Success(id)
            
        } catch (e: Exception) {
            Timber.e(e, "Error adding resource: $name")
            Result.Error(
                message = e.message ?: "Failed to add resource",
                throwable = e,
                errorCode = ErrorCode.DATABASE_ERROR
            )
        }
    }

    /**
     * Add a local folder resource.
     * Convenience method for local folders.
     * 
     * @param name Display name
     * @param path File system path or content URI
     * @return Result containing the new resource ID
     */
    suspend fun addLocalFolder(name: String, path: String): Result<Long> {
        return invoke(
            name = name,
            path = path,
            type = ResourceType.LOCAL,
            sortMode = SortMode.DATE_DESC,
            displayMode = DisplayMode.GRID
        )
    }
}
