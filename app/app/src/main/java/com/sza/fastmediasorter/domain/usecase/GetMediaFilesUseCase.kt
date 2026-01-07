package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for retrieving media files from a resource.
 * Simple non-paginated version for compatibility.
 */
class GetMediaFilesUseCase @Inject constructor() {

    /**
     * Get all media files for a resource by ID.
     * 
     * @param resourceId The resource ID to load files from
     * @return Result containing list of MediaFile items
     */
    suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
        Timber.d("GetMediaFilesUseCase: Loading files for resource ID $resourceId")
        
        // TODO: Implement actual file loading logic
        // For now, return empty list as this is just for build compatibility
        return Result.Success(emptyList())
    }
}
