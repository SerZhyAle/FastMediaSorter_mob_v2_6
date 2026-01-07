package com.sza.fastmediasorter.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sza.fastmediasorter.data.paging.MediaFilePagingSource
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.SortMode
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for retrieving paginated media files from a resource.
 * Provides efficient pagination for large file collections.
 */
class GetPaginatedMediaFilesUseCase @Inject constructor() {

    companion object {
        private const val PAGE_SIZE = 50
        private const val INITIAL_LOAD_SIZE = 100
        private const val PREFETCH_DISTANCE = 20
    }

    /**
     * Get paginated media files for a directory path.
     * 
     * @param directoryPath The directory path to scan
     * @param sortMode The sort mode to apply
     * @return Flow of PagingData containing MediaFile items
     */
    operator fun invoke(
        directoryPath: String,
        sortMode: SortMode = SortMode.NAME_ASC
    ): Flow<PagingData<MediaFile>> {
        Timber.d("GetPaginatedMediaFilesUseCase: Loading paginated files for path $directoryPath with sort mode $sortMode")
        
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MediaFilePagingSource(
                    directoryPath = directoryPath,
                    sortMode = sortMode
                )
            }
        ).flow
    }
}
