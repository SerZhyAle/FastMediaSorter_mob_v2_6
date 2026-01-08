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
     * @param credentialsId Reference to network credentials (for network resources)
     * @param sortMode Initial sort mode
     * @param displayMode Initial display mode
     * @param workWithAllFiles Whether to include non-media files
     * @param pinCode PIN code for resource protection (4-12 digits, null if not protected)
     * @param supportedMediaTypes Bitmask of supported media types (0 = all types)
     * @return Result containing the new resource ID or error
     */
    suspend operator fun invoke(
        name: String,
        path: String,
        type: ResourceType,
        credentialsId: String? = null,
        sortMode: SortMode = SortMode.DATE_DESC,
        displayMode: DisplayMode = DisplayMode.GRID,
        workWithAllFiles: Boolean = false,
        isDestination: Boolean = false,
        isReadOnly: Boolean = false,
        pinCode: String? = null,
        supportedMediaTypes: Int = 0
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
                credentialsId = credentialsId,
                sortMode = sortMode,
                displayMode = displayMode,
                workWithAllFiles = workWithAllFiles,
                isDestination = isDestination,
                isReadOnly = isReadOnly,
                pinCode = pinCode,
                supportedMediaTypes = supportedMediaTypes
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
            displayMode = DisplayMode.GRID,
            workWithAllFiles = false,
            isDestination = false,
            isReadOnly = false
        )
    }
}
