package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.MediaRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for retrieving media files from a resource.
 * Implements Clean Architecture by isolating business logic from UI.
 */
class GetMediaFilesUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val mediaRepository: MediaRepository
) {

    /**
     * Get media files for a resource.
     * 
     * @param resourceId The ID of the resource to scan
     * @return Result containing list of MediaFile or error
     */
    suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
        return try {
            Timber.d("GetMediaFilesUseCase: Loading files for resource $resourceId")
            
            // Get the resource first
            val resource = resourceRepository.getResourceById(resourceId)
                ?: return Result.Error(
                    message = "Resource not found",
                    errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.FILE_NOT_FOUND
                )
            
            // Get files from the media repository
            val files = mediaRepository.getMediaFiles(resource)
            
            Timber.d("GetMediaFilesUseCase: Loaded ${files.size} files")
            Result.Success(files)
            
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied accessing resource $resourceId")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading files for resource $resourceId")
            Result.Error(
                message = e.message ?: "Unknown error loading files",
                throwable = e
            )
        }
    }

    /**
     * Get media files as a Flow for reactive updates.
     * 
     * @param resourceId The ID of the resource to scan
     * @return Flow of Result containing list of MediaFile
     */
    fun asFlow(resourceId: Long): Flow<Result<List<MediaFile>>> = flow {
        emit(Result.Loading)
        emit(invoke(resourceId))
    }.catch { e ->
        Timber.e(e, "Flow error loading files for resource $resourceId")
        emit(Result.Error(
            message = e.message ?: "Unknown error",
            throwable = e
        ))
    }
}
