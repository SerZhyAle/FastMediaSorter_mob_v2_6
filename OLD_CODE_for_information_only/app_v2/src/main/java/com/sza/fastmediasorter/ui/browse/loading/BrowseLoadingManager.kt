package com.sza.fastmediasorter.ui.browse.loading

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sza.fastmediasorter.core.cache.MediaFilesCacheManager
import com.sza.fastmediasorter.data.paging.MediaFilesPagingSource
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import com.sza.fastmediasorter.domain.usecase.MediaScannerFactory
import com.sza.fastmediasorter.domain.usecase.ScanProgressCallback
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import com.sza.fastmediasorter.domain.usecase.FavoritesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages file loading operations for BrowseViewModel.
 * Handles both standard loading and pagination setup.
 */
class BrowseLoadingManager(
    private val mediaScannerFactory: MediaScannerFactory,
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val favoritesUseCase: FavoritesUseCase,
    private val resourceId: Long,
    private val viewModelScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val pageSize: Int = 50,
    private val paginationThreshold: Int = 500
) {
    /**
     * Callbacks for communication with ViewModel.
     */
    interface LoadingCallbacks {
        suspend fun updateLoadingProgress(progress: Int)
        suspend fun updateState(mediaFiles: List<MediaFile>, usePagination: Boolean, loadingProgress: Int, totalFileCount: Int, isScanCancellable: Boolean)
        fun setLoading(loading: Boolean)
        suspend fun handleLoadingError(resource: MediaResource, error: Throwable)
        suspend fun updateResourceMetadata(resource: MediaResource, fileCount: Int)
        fun startFileObserver()
        fun sortFiles(files: List<MediaFile>, sortMode: SortMode, forceSort: Boolean): List<MediaFile>
    }
    
    /**
     * Sets up pagination for large folders using Paging3 library.
     * Creates a Pager with MediaFilesPagingSource for efficient chunked loading.
     * 
     * @param resource The resource to load files from
     * @param sortMode Current sort mode for file ordering
     * @param sizeFilter Size filters for different media types
     * @return Flow<PagingData<MediaFile>> that emits paginated data
     */
    fun setupPagination(
        resource: MediaResource,
        sortMode: SortMode,
        sizeFilter: SizeFilter
    ): Flow<PagingData<MediaFile>> {
        Timber.d("BrowseLoadingManager: Setting up pagination for resource='${resource.name}', sortMode=$sortMode")
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = 15,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2 // Load 2 pages initially
            ),
            pagingSourceFactory = {
                MediaFilesPagingSource(
                    resource = resource,
                    sortMode = sortMode,
                    sizeFilter = sizeFilter,
                    mediaScannerFactory = mediaScannerFactory
                )
            }
        ).flow.cachedIn(viewModelScope)
    }
    
    /**
     * Loads media files using standard (non-paginated) approach.
     * Scans all files with progress tracking, sorts them, caches results.
     * 
     * @param resource The resource to load files from
     * @param sortMode Current sort mode for file ordering
     * @param sizeFilter Size filters for different media types
     * @param shouldStopScan Flag to gracefully stop scanning
     * @param progressJob Job to cancel when loading completes
     * @param callbacks Callbacks for communication with ViewModel
     */
    suspend fun loadFilesStandard(
        resource: MediaResource,
        sortMode: SortMode,
        sizeFilter: SizeFilter,
        shouldStopScan: AtomicBoolean,
        progressJob: Job?,
        callbacks: LoadingCallbacks
    ) {
        Timber.d("BrowseLoadingManager: START loading - resource='${resource.name}' (id=${resource.id}), type=${resource.type}")
        Timber.d("BrowseLoadingManager: supportedTypes=${resource.supportedMediaTypes.map { it.name }}, sortMode=$sortMode")
        
        // Progress callback to update UI every time scanner emits progress
        val progressCallback = object : ScanProgressCallback {
            override suspend fun onProgress(scannedCount: Int, currentFile: String?) {
                Timber.d("BrowseLoadingManager: Progress - $scannedCount files scanned, current='$currentFile'")
                callbacks.updateLoadingProgress(scannedCount)
            }
            
            override suspend fun onComplete(totalFiles: Int, durationMs: Long) {
                Timber.d("BrowseLoadingManager: Progress callback completed: $totalFiles files in ${durationMs}ms")
            }
            
            override fun shouldStop(): Boolean {
                return shouldStopScan.get()
            }
        }
        
        Timber.d("BrowseLoadingManager: Calling GetMediaFilesUseCase...")
        getMediaFilesUseCase(
            resource = resource,
            sortMode = sortMode,
            sizeFilter = sizeFilter,
            useChunkedLoading = false,
            maxFiles = Int.MAX_VALUE,
            onProgress = progressCallback
        )
            .catch { e ->
                Timber.e(e, "BrowseLoadingManager: ERROR in flow - Exception")
                progressJob?.cancel()
                callbacks.setLoading(false)
                callbacks.handleLoadingError(resource, e)
            }
            .collect { files ->
                Timber.d("BrowseLoadingManager: Flow COLLECTED ${files.size} files")
                
                // Apply sorting for large folders (GetMediaFilesUseCase skipped it for performance)
                val sortedFiles = if (files.size > paginationThreshold) {
                    Timber.d("BrowseLoadingManager: Large folder detected (${files.size} files), applying sort: $sortMode")
                    callbacks.sortFiles(files, sortMode, forceSort = true)
                } else {
                    Timber.d("BrowseLoadingManager: Small folder (${files.size} files), using pre-sorted list")
                    files  // Already sorted by GetMediaFilesUseCase
                }
                
                // Check favorite status for all files
                Timber.d("BrowseLoadingManager: Checking favorite status for ${sortedFiles.size} files")
                val finalFiles = withContext(ioDispatcher) {
                    sortedFiles.map { file ->
                        val isFav = favoritesUseCase.isFavoriteSync(file.path)
                        if (isFav) {
                            file.copy(isFavorite = true)
                        } else {
                            file
                        }
                    }
                }
                
                Timber.d("BrowseLoadingManager: Caching ${finalFiles.size} files for resource $resourceId")
                // Cache the sorted list for future use
                MediaFilesCacheManager.setCachedList(resourceId, finalFiles)
                
                Timber.d("BrowseLoadingManager: Updating UI state with ${finalFiles.size} files")
                callbacks.updateState(finalFiles, false, 0, finalFiles.size, false)
                progressJob?.cancel()
                callbacks.setLoading(false)
                
                Timber.d("BrowseLoadingManager: COMPLETE - ${finalFiles.size} files loaded and displayed")
                
                // Update resource metadata (fileCount and lastBrowseDate) after successful load
                callbacks.updateResourceMetadata(resource, files.size)
                
                // Start FileObserver for local resources
                callbacks.startFileObserver()
            }
    }
}
