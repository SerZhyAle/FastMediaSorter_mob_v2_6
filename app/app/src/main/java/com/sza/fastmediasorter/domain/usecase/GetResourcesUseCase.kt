package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for retrieving all resources.
 * Provides proper error handling and logging.
 */
class GetResourcesUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) {

    /**
     * Get all resources as a Flow.
     * Updates automatically when data changes.
     * 
     * @return Flow of Result containing list of Resources
     */
    operator fun invoke(): Flow<Result<List<Resource>>> {
        return resourceRepository.getAllResourcesFlow()
            .map<List<Resource>, Result<List<Resource>>> { resources ->
                Timber.d("GetResourcesUseCase: ${resources.size} resources loaded")
                Result.Success(resources)
            }
            .catch { e ->
                Timber.e(e, "Error loading resources")
                emit(Result.Error(
                    message = e.message ?: "Unknown error loading resources",
                    throwable = e,
                    errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.DATABASE_ERROR
                ))
            }
    }

    /**
     * Get all resources synchronously.
     * 
     * @return Result containing list of Resources
     */
    suspend fun getAll(): Result<List<Resource>> {
        return try {
            val resources = resourceRepository.getAllResources()
            Timber.d("GetResourcesUseCase.getAll: ${resources.size} resources")
            Result.Success(resources)
        } catch (e: Exception) {
            Timber.e(e, "Error loading resources")
            Result.Error(
                message = e.message ?: "Unknown error",
                throwable = e,
                errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.DATABASE_ERROR
            )
        }
    }

    /**
     * Get a single resource by ID.
     * 
     * @param id The resource ID
     * @return Result containing the Resource or error if not found
     */
    suspend fun getById(id: Long): Result<Resource> {
        return try {
            val resource = resourceRepository.getResourceById(id)
            if (resource != null) {
                Result.Success(resource)
            } else {
                Result.Error(
                    message = "Resource not found",
                    errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.FILE_NOT_FOUND
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading resource $id")
            Result.Error(
                message = e.message ?: "Unknown error",
                throwable = e,
                errorCode = com.sza.fastmediasorter.domain.model.ErrorCode.DATABASE_ERROR
            )
        }
    }
}
