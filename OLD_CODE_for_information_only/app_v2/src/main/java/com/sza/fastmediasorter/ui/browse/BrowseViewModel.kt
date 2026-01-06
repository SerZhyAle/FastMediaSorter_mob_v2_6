package com.sza.fastmediasorter.ui.browse

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.sza.fastmediasorter.R
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sza.fastmediasorter.core.cache.MediaFilesCacheManager
import com.sza.fastmediasorter.core.di.IoDispatcher
import com.sza.fastmediasorter.core.ui.BaseViewModel
import com.sza.fastmediasorter.data.cloud.CloudProvider
import com.sza.fastmediasorter.data.paging.MediaFilesPagingSource
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.sza.fastmediasorter.domain.model.FileFilter
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.UndoOperation
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.model.MediaExtensions
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.usecase.FileOperation
import com.sza.fastmediasorter.domain.usecase.FileOperationUseCase
import com.sza.fastmediasorter.data.observer.MediaFileObserver
import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.data.network.glide.NetworkFileDataFetcher
import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import com.sza.fastmediasorter.domain.usecase.GetResourcesUseCase
import com.sza.fastmediasorter.domain.usecase.MediaScannerFactory
import com.sza.fastmediasorter.domain.usecase.ScanProgressCallback
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import com.sza.fastmediasorter.domain.usecase.UpdateResourceUseCase
import com.sza.fastmediasorter.data.network.glide.NetworkVideoFrameDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class BrowseState(
    val resource: MediaResource? = null,
    val mediaFiles: List<MediaFile> = emptyList(),
    val usePagination: Boolean = false, // True if file count >= PAGINATION_THRESHOLD
    val totalFileCount: Int? = null, // Total count (null if not yet calculated)
    val selectedFiles: Set<String> = emptySet(),
    val lastSelectedPath: String? = null,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val displayMode: DisplayMode = DisplayMode.LIST,
    val filter: FileFilter? = null,
    val lastOperation: UndoOperation? = null,
    val undoOperationTimestamp: Long? = null, // Timestamp when undo operation was saved (for expiry check)
    val loadingProgress: Int = 0, // Number of files found during scan (0 = not scanning)
    val isCloudResource: Boolean = false, // True for cloud resources (to show animated dots)
    val isScanCancellable: Boolean = false, // True when scan runs >5 seconds, shows STOP button
    val showSmallControls: Boolean = false // True if "Small controls" setting is enabled
)

sealed class BrowseEvent {
    data class ShowError(val message: String, val details: String? = null, val exception: Throwable? = null) : BrowseEvent()
    data class ShowMessage(val message: String) : BrowseEvent()
    data class ShowUndoToast(val operationType: String) : BrowseEvent()
    data class NavigateToPlayer(val filePath: String, val fileIndex: Int) : BrowseEvent()
    object ShowCloudAuthenticationRequired : BrowseEvent()
    data class CloudAuthRequired(val provider: String, val message: String) : BrowseEvent()
    data class NoFilesFound(val message: String? = null, val messageResId: Int? = null) : BrowseEvent()  // Return to main screen with toast
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val mediaScannerFactory: MediaScannerFactory,
    private val settingsRepository: SettingsRepository,
    private val updateResourceUseCase: UpdateResourceUseCase,
    val fileOperationUseCase: FileOperationUseCase, // Public for RenameDialog
    private val smbClient: com.sza.fastmediasorter.data.network.SmbClient,
    private val cleanupTrashFoldersUseCase: com.sza.fastmediasorter.domain.usecase.CleanupTrashFoldersUseCase,
    private val googleDriveClient: com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient,
    private val credentialsRepository: com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository,
    private val favoritesUseCase: com.sza.fastmediasorter.domain.usecase.FavoritesUseCase,
    private val browseStateDataStore: com.sza.fastmediasorter.data.local.preferences.BrowseStateDataStore,
    private val unifiedCache: com.sza.fastmediasorter.core.cache.UnifiedFileCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<BrowseState, BrowseEvent>() {

    companion object {
        private const val PAGINATION_THRESHOLD = 500 // Use pagination for folders with 500+ files (reduced to improve initial load performance)
        private const val PAGE_SIZE = 50 // Load 50 files per page
    }

    private val resourceId: Long = savedStateHandle.get<Long>("resourceId") 
        ?: savedStateHandle.get<String>("resourceId")?.toLongOrNull() 
        ?: 0L
    
    private val skipAvailabilityCheck: Boolean = savedStateHandle.get<Boolean>("skipAvailabilityCheck") ?: false
    
    // Selection management
    private val selectionManager = com.sza.fastmediasorter.ui.browse.selection.BrowseSelectionManager()
    
    // Undo management
    private val undoManager = com.sza.fastmediasorter.ui.browse.undo.BrowseUndoManager(
        callbacks = object : com.sza.fastmediasorter.ui.browse.undo.BrowseUndoManager.UndoCallbacks {
            override suspend fun addFilesToList(files: List<MediaFile>) {
                addFiles(files)
            }
            override suspend fun reloadFileList() {
                loadResource()
            }
            override fun createMediaFileFromFile(file: java.io.File): MediaFile {
                return this@BrowseViewModel.createMediaFileFromFile(file)
            }
            override fun showMessage(message: String) {
                sendEvent(BrowseEvent.ShowMessage(message))
            }
            override fun showUndoToast(operationType: String) {
                sendEvent(BrowseEvent.ShowUndoToast(operationType))
            }
            override fun showError(message: String, details: String?, exception: Throwable?) {
                sendEvent(BrowseEvent.ShowError(message, details, exception))
            }
        }
    )
    
    // File list management
    private val fileListManager = com.sza.fastmediasorter.ui.browse.filelist.BrowseFileListManager(resourceId)
    
    // Metadata management
    private val metadataManager = com.sza.fastmediasorter.ui.browse.metadata.BrowseMetadataManager(
        updateResourceUseCase = updateResourceUseCase,
        ioDispatcher = ioDispatcher
    )
    
    // Loading management
    private val loadingManager = com.sza.fastmediasorter.ui.browse.loading.BrowseLoadingManager(
        mediaScannerFactory = mediaScannerFactory,
        getMediaFilesUseCase = getMediaFilesUseCase,
        favoritesUseCase = favoritesUseCase,
        resourceId = resourceId,
        viewModelScope = viewModelScope,
        ioDispatcher = ioDispatcher,
        pageSize = PAGE_SIZE,
        paginationThreshold = PAGINATION_THRESHOLD
    )
    
    // Cache management
    private val cacheManager = com.sza.fastmediasorter.ui.browse.cache.BrowseCacheManager(
        resourceId = resourceId,
        paginationThreshold = PAGINATION_THRESHOLD
    )
    
    private var fileObserver: MediaFileObserver? = null
    
    // Job for current file loading operation (to cancel on reload)
    private var loadFilesJob: Job? = null
    
    // Graceful stop flag: when true, scanner should stop and return partial results
    private val shouldStopScan = java.util.concurrent.atomic.AtomicBoolean(false)
    
    // Job for delayed STOP button visibility (10 seconds after scan start)
    private var stopButtonTimerJob: Job? = null
    
    // Track last emitted list to avoid redundant UI updates (survives Activity recreation)
    var lastEmittedMediaFiles: List<MediaFile>? = null
        private set
    
    fun markListAsSubmitted(list: List<MediaFile>) {
        lastEmittedMediaFiles = list
    }
    
    // PagingData flow for large datasets (used when usePagination = true)
    private val _pagingDataFlow = MutableStateFlow<Flow<PagingData<MediaFile>>?>(null)
    val pagingDataFlow: StateFlow<Flow<PagingData<MediaFile>>?> = _pagingDataFlow.asStateFlow()

    override fun getInitialState() = BrowseState()


    init {
        // Clear PDF thumbnail cache from previous Browse sessions
        // (in case app was killed without proper onCleared() call)
        clearPdfThumbnailCache()
        
        loadResource()
        loadSettings()
        restoreFilterState()
        observeSelectionChanges()
        observeUndoChanges()
    }
    
    private fun restoreFilterState() {
        viewModelScope.launch(ioDispatcher) {
            browseStateDataStore.filter.first()?.let { savedFilter ->
                if (!savedFilter.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        updateState { it.copy(filter = savedFilter) }
                        applyFilter()
                        
                        // Show hint about restored filter
                        val filterDesc = buildString {
                            if (!savedFilter.nameContains.isNullOrBlank()) append("Name")
                            if (savedFilter.minSizeMb != null) {
                                if (isNotEmpty()) append(", ")
                                append("Size")
                            }
                            if (savedFilter.minDate != null) {
                                if (isNotEmpty()) append(", ")
                                append("Date")
                            }
                        }
                        if (filterDesc.isNotEmpty()) {
                            sendEvent(BrowseEvent.ShowMessage("Restored last filter: $filterDesc"))
                        }
                    }
                }
            }
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            val settings = settingsRepository.getSettings().first()
            updateState { it.copy(showSmallControls = settings.showSmallControls) }
        }
    }
    
    /**
     * Get current settings from repository.
     * Used by Activity to access Safe Mode settings.
     */
    suspend fun getSettings() = settingsRepository.getSettings().first()
    
    /**
     * Observe selection changes from SelectionManager and sync to state.
     */
    private fun observeSelectionChanges() {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            selectionManager.selectionState.collect { selection ->
                updateState { state ->
                    state.copy(
                        selectedFiles = selection.selectedFiles,
                        lastSelectedPath = selection.lastSelectedPath
                    )
                }
            }
        }
    }
    
    /**
     * Observe undo state changes from UndoManager and sync to state.
     */
    private fun observeUndoChanges() {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            undoManager.undoState.collect { undoState ->
                updateState { state ->
                    state.copy(
                        lastOperation = undoState.lastOperation,
                        undoOperationTimestamp = undoState.undoOperationTimestamp
                    )
                }
            }
        }
    }
    
    /**
     * Cancel background thumbnail loading when navigating to PlayerActivity.
     * This frees network bandwidth for the player without affecting the adapter cache.
     */
    fun cancelBackgroundThumbnailLoading() {
        val resource = state.value.resource
        if (resource != null && resource.type != ResourceType.LOCAL) {
            val resourceKey = when (resource.type) {
                ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP -> {
                    val uri = java.net.URI(resource.path)
                    "${uri.scheme}://${uri.host}:${uri.port.takeIf { it != -1 } ?: 445}"
                }
                ResourceType.CLOUD -> "cloud://${resource.cloudProvider}/${resource.cloudFolderId}"
                else -> null
            }
            resourceKey?.let { 
                ConnectionThrottleManager.cancelAllForResource(it)
                Timber.d("BrowseViewModel.cancelBackgroundThumbnailLoading: Cancelled all operations for $it")
            }
        }
    }
    
    override fun onCleared() {
        // Save current filter state before clearing
        val currentFilter = state.value.filter
        viewModelScope.launch(ioDispatcher) {
            browseStateDataStore.saveFilter(currentFilter)
        }
        
        super.onCleared()
        
        // Cancel all active network operations for this resource to prevent conflicts
        val resource = state.value.resource
        if (resource != null && resource.type != ResourceType.LOCAL) {
            val resourceKey = when (resource.type) {
                ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP -> {
                    // Extract host:port from path (e.g., "smb://192.168.1.112:445/share")
                    val uri = java.net.URI(resource.path)
                    "${uri.scheme}://${uri.host}:${uri.port.takeIf { it != -1 } ?: 445}"
                }
                ResourceType.CLOUD -> "cloud://${resource.cloudProvider}/${resource.cloudFolderId}"
                else -> null
            }
            resourceKey?.let { 
                ConnectionThrottleManager.cancelAllForResource(it)
                Timber.d("BrowseViewModel.onCleared: Cancelled all operations for $it")
            }
        }
        
        // Explicitly cancel active jobs to stop network requests immediately
        // This ensures browsing stops but doesn't affect background copy/move operations
        // which run in their own scope (e.g. CopyToDialog scope)
        loadFilesJob?.cancel()
        stopButtonTimerJob?.cancel()
        cancelScan() // Set flag for graceful stop
        
        stopFileObserver()
        
        // Cleanup trash folders when leaving resource (background, maxAge=0 = delete all)
        if (resource != null) {
            viewModelScope.launch(ioDispatcher) {
                cleanupTrashOnBackground(resource, maxAgeMs = 0L)
            }
        }
        
        // Clear PDF thumbnail cache - full PDF files no longer needed after leaving Browse
        // Bitmap thumbnails remain in Glide disk cache for fast reload
        clearPdfThumbnailCache()
    }
    
    /**
     * Clear PDF thumbnail cache when leaving Browse screen.
     * Now uses UnifiedFileCache instead of legacy pdf_thumbnails directory.
     */
    private fun clearPdfThumbnailCache() {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Clear UnifiedFileCache (contains all network files)
                unifiedCache.clearAll()
                Timber.d("Cleared UnifiedFileCache")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear UnifiedFileCache")
            }
        }
    }

    /**
     * Remove specific files from the current list without full reload.
     * Used after Move and Delete operations to avoid unnecessary scanning.
     * 
     * @param filePaths Paths of files to remove from the list
     */
    fun removeFiles(filePaths: List<String>) {
        if (filePaths.isEmpty()) return
        
        // Notify selection manager about removed files
        selectionManager.onFilesRemoved(filePaths)
        
        updateState { state ->
            val updatedFiles = fileListManager.removeFiles(state.mediaFiles, filePaths)
            state.copy(mediaFiles = updatedFiles)
        }
    }
    
    /**
     * Add files to the list (e.g., after undo move/delete).
     * Files are added and list is re-sorted according to current sort mode.
     * 
     * @param newFiles List of MediaFile objects to add
     */
    fun addFiles(newFiles: List<MediaFile>) {
        if (newFiles.isEmpty()) return
        
        updateState { state ->
            val updatedFiles = fileListManager.addFiles(state.mediaFiles, newFiles, state.sortMode)
            state.copy(mediaFiles = updatedFiles)
        }
    }
    
    /**
     * Update specific file in the list (e.g., after rename).
     * 
     * @param oldPath Original file path
     * @param newFile Updated MediaFile object
     */
    fun updateFile(oldPath: String, newFile: MediaFile) {
        // Notify selection manager about path change
        selectionManager.onFilePathChanged(oldPath, newFile.path)
        
        updateState { state ->
            val updatedFiles = fileListManager.updateFile(state.mediaFiles, oldPath, newFile, state.sortMode)
            state.copy(mediaFiles = updatedFiles)
        }
    }

    fun reloadFiles() {
        // Clearing cache for resource
        MediaFilesCacheManager.clearCache(resourceId)
        // Clear video frame extraction cache to retry failed videos
        NetworkFileDataFetcher.clearFailedVideoCache()
        loadResource()
    }
    
    /**
     * Cancel currently running scan operation.
     * Saves partial file list to cache for user to work with.
     */
    fun cancelScan() {
        // Setting graceful stop flag
        // Set flag to signal scanner to stop gracefully and return partial results
        shouldStopScan.set(true)
        
        // Don't cancel the job - let it complete gracefully
        // loadFilesJob will finish and emit partial results
    }

    private fun loadResource() {
        Timber.d("BrowseViewModel.loadResource: START - resourceId=$resourceId")
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            setLoading(true)
            
            // Handle virtual Favorites resource
            if (resourceId == -100L) { // FAVORITES_RESOURCE_ID
                Timber.d("BrowseViewModel.loadResource: Loading Favorites resource")
                val favoritesResource = MediaResource(
                    id = -100L,
                    name = "Favorites", // Should be localized ideally
                    path = "Favorites",
                    type = ResourceType.LOCAL,
                    isAvailable = true,
                    fileCount = 0, // Will be updated when files load
                    isWritable = false // Favorites list itself is not writable (can't add files to it directly via file system)
                )
                
                updateState { 
                    it.copy(
                        resource = favoritesResource,
                        sortMode = SortMode.DATE_DESC, // Default to newest added
                        displayMode = DisplayMode.LIST,
                        isCloudResource = false,
                        filter = null
                    ) 
                }
                
                loadFavorites()
                return@launch
            }
            
            Timber.d("BrowseViewModel.loadResource: Fetching resource from database...")
            val resource = getResourcesUseCase.getById(resourceId)
            if (resource == null) {
                Timber.e("BrowseViewModel.loadResource: ERROR - Resource not found for id=$resourceId")
                sendEvent(BrowseEvent.ShowError("Resource not found"))
                setLoading(false)
                return@launch
            }
            
            Timber.d("BrowseViewModel.loadResource: Resource found - name='${resource.name}', type=${resource.type}, path='${resource.path}'")
            Timber.d("BrowseViewModel.loadResource: fileCount=${resource.fileCount}, isWritable=${resource.isWritable}, lastBrowse=${resource.lastBrowseDate}")
            
            // Note: Previously we called smbClient.resetClients() here, but that kills 
            // active background operations (copy/move) which share the singleton client.
            // Removed to allow background operations to continue when re-entering browse.
            
            // Check if resource is available (skip if already validated in MainActivity)
            // For network resources (SFTP/FTP/SMB), skip this check and try to load files directly
            // because fileCount might be 0 if initial scan failed, but connection might work
            val isNetworkResource = resource.type in setOf(
                com.sza.fastmediasorter.domain.model.ResourceType.SMB,
                com.sza.fastmediasorter.domain.model.ResourceType.SFTP,
                com.sza.fastmediasorter.domain.model.ResourceType.FTP
            )
            
            Timber.d("BrowseViewModel.loadResource: isNetworkResource=$isNetworkResource, skipAvailabilityCheck=$skipAvailabilityCheck")
            
            if (!skipAvailabilityCheck && !isNetworkResource && resource.fileCount == 0 && !resource.isWritable) {
                Timber.w("BrowseViewModel.loadResource: Resource unavailable - fileCount=0, isWritable=false")
                sendEvent(BrowseEvent.ShowError(
                    message = "Resource '${resource.name}' is unavailable. Check network connection or resource settings.",
                    details = "Resource ID: ${resource.id}\nType: ${resource.type}\nPath: ${resource.path}"
                ))
                setLoading(false)
                return@launch
            }
            
            // Determine if cloud resource for progress UI
            val isCloudResource = resource.type == ResourceType.CLOUD
            
            // Restore filter from resource.supportedMediaTypes if it's a subset
            // If supportedMediaTypes contains all 7 types (IVAGTPE), filter should be null (no filter)
            val restoredFilter = if (resource.supportedMediaTypes.size < 7) {
                FileFilter(
                    nameContains = null,
                    minDate = null,
                    maxDate = null,
                    minSizeMb = null,
                    maxSizeMb = null,
                    mediaTypes = resource.supportedMediaTypes
                )
            } else {
                null // All types = no filter
            }
            
            Timber.d("BrowseViewModel.loadResource: restoredFilter=$restoredFilter (supportedTypes=${resource.supportedMediaTypes.size})")
            
            updateState { 
                it.copy(
                    resource = resource,
                    sortMode = resource.sortMode,
                    displayMode = resource.displayMode,
                    isCloudResource = isCloudResource,
                    filter = restoredFilter
                ) 
            }
            
            Timber.d("BrowseViewModel.loadResource: State updated - displayMode=${resource.displayMode}, sortMode=${resource.sortMode}")
            Timber.d("BrowseViewModel.loadResource: Cloud resource details - cloudProvider=${resource.cloudProvider}, cloudFolderId=${resource.cloudFolderId}, path=${resource.path}")
            
            // For Google Drive resources, verify authentication by testing API access
            if (resource.type == ResourceType.CLOUD && resource.cloudProvider == CloudProvider.GOOGLE_DRIVE) {
                // Try to restore authentication from encrypted storage if not authenticated
                if (!googleDriveClient.isAuthenticated()) {
                    Timber.d("BrowseViewModel.loadResource: Client not authenticated, attempting automatic restoration")
                    val restored = googleDriveClient.tryRestoreFromStorage()
                    if (restored) {
                        Timber.d("BrowseViewModel.loadResource: Successfully restored Google Drive authentication")
                    } else {
                        Timber.d("BrowseViewModel.loadResource: Failed to restore authentication, will require interactive sign-in")
                    }
                }
                
                Timber.d("BrowseViewModel.loadResource: Verifying Google Drive access for folder: ${resource.cloudFolderId}")
                val testResult = googleDriveClient.listFiles(resource.cloudFolderId ?: "root")
                if (testResult is com.sza.fastmediasorter.data.cloud.CloudResult.Error) {
                    Timber.d("BrowseViewModel.loadResource: Google Drive access failed: ${testResult.message}")
                    // Check if error indicates authentication issue
                    val is401 = testResult.message.contains("401", ignoreCase = true)
                    val isUnauth = testResult.message.contains("unauthorized", ignoreCase = true)
                    val hasAuth = testResult.message.contains("authentication", ignoreCase = true)
                    val notAuth = testResult.message.contains("not authenticated", ignoreCase = true)
                    Timber.d("BrowseViewModel.loadResource: Auth error check: 401=$is401, unauthorized=$isUnauth, authentication=$hasAuth, not_authenticated=$notAuth")
                    
                    if (is401 || isUnauth || hasAuth || notAuth) {
                        Timber.d("BrowseViewModel.loadResource: Sending ShowCloudAuthenticationRequired event")
                        sendEvent(BrowseEvent.ShowCloudAuthenticationRequired)
                        setLoading(false)
                        return@launch
                    }
                    // Other errors will be handled by normal error flow
                    Timber.d("BrowseViewModel.loadResource: Error is not authentication-related, continuing normal flow")
                } else {
                    Timber.d("BrowseViewModel.loadResource: Google Drive access verified successfully")
                }
            }
            
            // Check cache using BrowseCacheManager
            Timber.d("BrowseViewModel.loadResource: Checking for cached files...")
            when (val cacheResult = cacheManager.checkCache(restoredFilter ?: FileFilter())) {
                is com.sza.fastmediasorter.ui.browse.cache.BrowseCacheManager.CacheCheckResult.UseCache -> {
                    // Update totalFileCount from actual cached files count
                    updateState { it.copy(mediaFiles = cacheResult.files, totalFileCount = cacheResult.files.size) }
                    setLoading(false)
                    
                    // Update resource metadata (lastBrowseDate) even when loading from cache
                    Timber.d("BrowseViewModel.loadResource: Cache hit, updating metadata for ${resource.name}")
                    updateResourceMetadataAfterBrowse(resource, cacheResult.files.size)
                    
                    return@launch
                }
                is com.sza.fastmediasorter.ui.browse.cache.BrowseCacheManager.CacheCheckResult.Rescan -> {
                    Timber.d("BrowseViewModel.loadResource: Cache rejected - ${cacheResult.reason}")
                    // Continue with fresh scan below
                }
            }
            
            // NOTE: Trash cleanup removed from loadResource() to avoid race condition
            // with ongoing delete operations. Cleanup now only runs on onCleared().
            // See commit: Fix delete race condition with trash cleanup
            
            Timber.d("BrowseViewModel.loadResource: Calling loadMediaFiles()...")
            loadMediaFiles()
        }
    }

    private fun loadFavorites() {
        val resource = state.value.resource ?: return
        Timber.d("BrowseViewModel.loadFavorites: START")
        
        loadFilesJob?.cancel()
        
        loadFilesJob = viewModelScope.launch(ioDispatcher + exceptionHandler) {
            setLoading(true)
            favoritesUseCase.getAllFavorites().collect { favorites ->
                val mediaFiles = favorites.map { entity ->
                    MediaFile(
                        path = entity.uri,
                        name = entity.displayName,
                        type = MediaType.entries.getOrElse(entity.mediaType) { MediaType.IMAGE },
                        size = entity.size,
                        createdDate = entity.dateModified,
                        isFavorite = true,
                        resourceId = entity.resourceId,
                        width = 0,
                        height = 0,
                        duration = 0
                    )
                }
                
                // Cache the list for PlayerActivity
                MediaFilesCacheManager.setCachedList(-100L, mediaFiles)
                
                updateState { it.copy(
                    mediaFiles = mediaFiles,
                    totalFileCount = mediaFiles.size,
                    loadingProgress = mediaFiles.size,
                    usePagination = false // No pagination for favorites for now
                ) }
                setLoading(false)
            }
        }
    }

    private fun loadMediaFiles() {
        val resource = state.value.resource ?: return
        
        if (resource.id == -100L) {
            loadFavorites()
            return
        }
        
        Timber.d("BrowseViewModel.loadMediaFiles: START - resource='${resource.name}' (id=${resource.id})")
        
        // Cancel previous load if still running (e.g., during manual refresh)
        if (loadFilesJob?.isActive == true) {
            Timber.d("BrowseViewModel.loadMediaFiles: Cancelling previous load job")
            loadFilesJob?.cancel()
        }
        
        // Starting new load
        
        // Don't clear current list to prevent UI flashing/empty state
        // updateState { it.copy(mediaFiles = emptyList(), loadingProgress = 0, isScanCancellable = false) }
        updateState { it.copy(loadingProgress = 0, isScanCancellable = false) }
        
        Timber.d("BrowseViewModel.loadMediaFiles: Starting progress timer")
        // Start progress timer for UI updates every 2 seconds
        var lastProgressUpdate = 0
        val progressJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                delay(2000) // Update every 2 seconds
                val currentProgress = state.value.loadingProgress
                if (currentProgress > 0 && currentProgress != lastProgressUpdate) {
                    Timber.d("BrowseViewModel.loadMediaFiles: Progress timer update - $currentProgress files found")
                    lastProgressUpdate = currentProgress
                }
            }
        }
        
        loadFilesJob = viewModelScope.launch(ioDispatcher + exceptionHandler) {
            setLoading(true)
            
            // Reset graceful stop flag at start of new scan
            shouldStopScan.set(false)
            
            // Start 5-second timer to show STOP button in the same coroutine context
            launch {
                delay(5_000L)
                if (loadFilesJob?.isActive == true) {
                    updateState { it.copy(isScanCancellable = true) }
                    Timber.d("BrowseViewModel.loadMediaFiles: Scan running >5s, showing STOP button")
                }
            }
            
            Timber.d("BrowseViewModel.loadMediaFiles: Getting size filter settings")
            // Get current settings for size filters and global media type restrictions
            val settings = settingsRepository.getSettings().first()
            val sizeFilter = SizeFilter(
                imageSizeMin = settings.imageSizeMin,
                imageSizeMax = settings.imageSizeMax,
                videoSizeMin = settings.videoSizeMin,
                videoSizeMax = settings.videoSizeMax,
                audioSizeMin = settings.audioSizeMin,
                audioSizeMax = settings.audioSizeMax
            )
            Timber.d("BrowseViewModel.loadMediaFiles: Size filter configured - imageMin=${settings.imageSizeMin}, videoMin=${settings.videoSizeMin}")
            
            // Apply global media type restrictions: intersect resource types with globally enabled types
            val globallyEnabled = settings.getGloballyEnabledMediaTypes()
            val effectiveMediaTypes = resource.supportedMediaTypes.intersect(globallyEnabled)
            val resourceWithGlobalFilter = if (effectiveMediaTypes != resource.supportedMediaTypes) {
                Timber.d("BrowseViewModel.loadMediaFiles: Global filter applied - resource types: ${resource.supportedMediaTypes}, global enabled: $globallyEnabled, effective: $effectiveMediaTypes")
                resource.copy(supportedMediaTypes = effectiveMediaTypes)
            } else {
                resource
            }
            
            try {
                // Use cached fileCount from resource metadata if available
                if (resource.fileCount > 0 && resource.lastBrowseDate != null) {
                    Timber.d("BrowseViewModel.loadMediaFiles: Using cached file count from resource: ${resource.fileCount}")
                    updateState { it.copy(totalFileCount = resource.fileCount) }
                } else {
                    Timber.d("BrowseViewModel.loadMediaFiles: No cached file count, will count during scan")
                }
                // If no cache, totalFileCount stays null (shows "counting..." in UI)
                
                // Apply 15-second timeout for network resources to prevent UI hanging
                val isNetworkResource = resourceWithGlobalFilter.type in setOf(
                    com.sza.fastmediasorter.domain.model.ResourceType.SMB,
                    com.sza.fastmediasorter.domain.model.ResourceType.SFTP,
                    com.sza.fastmediasorter.domain.model.ResourceType.FTP,
                    com.sza.fastmediasorter.domain.model.ResourceType.CLOUD
                )
                
                if (isNetworkResource) {
                    val result = withTimeoutOrNull(15_000L) {
                        loadMediaFilesStandard(resourceWithGlobalFilter, sizeFilter, progressJob)
                    }
                    if (result == null) {
                        Timber.w("BrowseViewModel.loadMediaFiles: Network resource timeout after 15 seconds")
                        progressJob.cancel()
                        setLoading(false)
                        sendEvent(BrowseEvent.ShowError(
                            message = "Resource '${resourceWithGlobalFilter.name}' is not responding.",
                            details = "Connection timed out after 15 seconds.\nCheck network connection or resource settings."
                        ))
                    }
                } else {
                    loadMediaFilesStandard(resourceWithGlobalFilter, sizeFilter, progressJob)
                }
            } catch (e: Exception) {
                Timber.e(e, "BrowseViewModel.loadMediaFiles: ERROR determining file count")
                progressJob.cancel()
                // Fallback to standard loading if count fails
                loadMediaFilesStandard(resourceWithGlobalFilter, sizeFilter, progressJob)
            }
        }
    }
    
    private suspend fun loadMediaFilesStandard(resource: MediaResource, sizeFilter: SizeFilter, progressJob: Job? = null) {
        // Delegate to BrowseLoadingManager with callbacks
        val callbacks = object : com.sza.fastmediasorter.ui.browse.loading.BrowseLoadingManager.LoadingCallbacks {
            override suspend fun updateLoadingProgress(progress: Int) {
                updateState { it.copy(loadingProgress = progress) }
            }
            
            override suspend fun updateState(mediaFiles: List<MediaFile>, usePagination: Boolean, loadingProgress: Int, totalFileCount: Int, isScanCancellable: Boolean) {
                // Note: Don't check for empty mediaFiles here and send NoFilesFound event
                // If there was a scan error, handleLoadingError() already showed the correct error message
                // Empty mediaFiles here could mean either:
                // 1. Scan error (already handled by handleLoadingError)
                // 2. Truly empty folder (user will see empty list, which is correct)
                
                this@BrowseViewModel.updateState { it.copy(
                    mediaFiles = mediaFiles,
                    usePagination = usePagination,
                    loadingProgress = loadingProgress,
                    totalFileCount = totalFileCount,
                    isScanCancellable = isScanCancellable
                ) }
            }
            
            override fun setLoading(loading: Boolean) {
                this@BrowseViewModel.setLoading(loading)
            }
            
            override suspend fun handleLoadingError(resource: MediaResource, error: Throwable) {
                this@BrowseViewModel.handleLoadingError(resource, error)
            }
            
            override suspend fun updateResourceMetadata(resource: MediaResource, fileCount: Int) {
                updateResourceMetadataAfterBrowse(resource, fileCount)
            }
            
            override fun startFileObserver() {
                this@BrowseViewModel.startFileObserver()
            }
            
            override fun sortFiles(files: List<MediaFile>, sortMode: SortMode, forceSort: Boolean): List<MediaFile> {
                return this@BrowseViewModel.sortFiles(files, sortMode, forceSort)
            }
        }
        
        loadingManager.loadFilesStandard(
            resource = resource,
            sortMode = state.value.sortMode,
            sizeFilter = sizeFilter,
            shouldStopScan = shouldStopScan,
            progressJob = progressJob,
            callbacks = callbacks
        )
    }
    
    private fun loadMediaFilesWithPagination(resource: MediaResource, sizeFilter: SizeFilter) {
        try {
            // Delegate pagination setup to BrowseLoadingManager
            val newFlow = loadingManager.setupPagination(
                resource = resource,
                sortMode = state.value.sortMode,
                sizeFilter = sizeFilter
            )
            
            _pagingDataFlow.value = newFlow
            updateState { it.copy(usePagination = true, mediaFiles = emptyList()) }
            setLoading(false)
            
            // Update resource metadata (fileCount from totalFileCount and lastBrowseDate)
            // Launch in viewModelScope since updateResourceMetadataAfterBrowse is now suspend
            viewModelScope.launch(ioDispatcher) {
                val actualFileCount = state.value.totalFileCount ?: 0
                updateResourceMetadataAfterBrowse(resource, actualFileCount)
            }
            
            Timber.d("Pagination enabled for ${resource.name}")
        } catch (e: Exception) {
            Timber.e(e, "Error setting up pagination")
            handleLoadingError(resource, e)
            setLoading(false)
        }
    }
    
    /**
     * Updates resource metadata (fileCount and lastBrowseDate) after successful file loading.
     * Delegated to BrowseMetadataManager.
     */
    private suspend fun updateResourceMetadataAfterBrowse(resource: MediaResource, actualFileCount: Int) {
        metadataManager.updateMetadata(resource, actualFileCount)
    }
    
    private fun handleLoadingError(resource: MediaResource, e: Throwable) {
        // Check if this is ConnectionThrottle cancellation (video player active)
        if (e is java.util.concurrent.CancellationException && 
            e.message?.contains("Video player priority", ignoreCase = true) == true) {
            Timber.d("Folder loading blocked by video player throttle")
            sendEvent(BrowseEvent.ShowError(
                message = "Cannot load folder while video is playing\n\n" +
                         "The video player has priority access to network resources.\n\n" +
                         "Please close the video player and try again.",
                details = "Resource: ${resource.name}\nPath: ${resource.path}\n\n" +
                         "This is a safety feature to prevent network congestion and ensure smooth video playback.",
                exception = e
            ))
            setLoading(false)
            return
        }
        
        // Check if this is Cloud authentication error - send special event
        if (resource.type == ResourceType.CLOUD && 
            (e.message?.contains("Google Drive authentication required", ignoreCase = true) == true ||
             e.message?.contains("Not authenticated", ignoreCase = true) == true)) {
            Timber.d("Cloud authentication error detected, sending ShowCloudAuthenticationRequired event")
            sendEvent(BrowseEvent.ShowCloudAuthenticationRequired)
            setLoading(false)
            return
        }
        
        // Update resource availability to false on connection errors
        viewModelScope.launch(ioDispatcher) {
            try {
                val isConnectionError = e.message?.contains("Connection", ignoreCase = true) == true ||
                    e.message?.contains("Network", ignoreCase = true) == true ||
                    e.message?.contains("Authentication", ignoreCase = true) == true ||
                    e.message?.contains("timed out", ignoreCase = true) == true
                
                if (isConnectionError && resource.isAvailable) {
                    Timber.d("Updating resource availability to false due to connection error")
                    val updatedResource = resource.copy(isAvailable = false)
                    updateResourceUseCase(updatedResource)
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to update resource availability")
            }
        }
        
        // Build detailed error information
        val errorTitle = when {
            e.message?.contains("Authentication failed", ignoreCase = true) == true ||
            e.message?.contains("LOGON_FAILURE", ignoreCase = true) == true -> {
                "Authentication Failed"
            }
            e.message?.contains("Connection error", ignoreCase = true) == true ||
            e.message?.contains("Network", ignoreCase = true) == true -> {
                "Network Connection Error"
            }
            e.message?.contains("Permission denied", ignoreCase = true) == true -> {
                "Permission Denied"
            }
            else -> {
                "Error Loading Files"
            }
        }
        
        val errorMessage = when {
            e.message?.contains("Authentication failed", ignoreCase = true) == true ||
            e.message?.contains("LOGON_FAILURE", ignoreCase = true) == true -> {
                "Invalid username or password.\n\n" +
                "Recommendations:\n" +
                "• Edit this resource and update credentials\n" +
                "• Verify username and password are correct\n" +
                "• Check if account is locked or expired"
            }
            e.message?.contains("Connection error", ignoreCase = true) == true ||
            e.message?.contains("Network", ignoreCase = true) == true -> {
                "Cannot connect to server.\n\n" +
                "Recommendations:\n" +
                "• Check your network connection\n" +
                "• Verify server address and port\n" +
                "• Check if server is online and accessible\n" +
                "• Try pinging the server from another device"
            }
            e.message?.contains("timed out", ignoreCase = true) == true ||
            e.message?.contains("SocketTimeoutException", ignoreCase = true) == true -> {
                "Connection timeout.\n\n" +
                "Possible causes:\n" +
                "• Server is slow or overloaded\n" +
                "• Network latency is too high\n" +
                "• Firewall blocking data connection (FTP passive mode)\n" +
                "• NAT/Router issues with FTP passive mode\n\n" +
                "Recommendations:\n" +
                "• Check server status and load\n" +
                "• Test connection from computer/laptop\n" +
                "• Configure firewall to allow FTP passive mode\n" +
                "• Check router port forwarding settings\n" +
                "• Contact server administrator if problem persists"
            }
            e.message?.contains("Permission denied", ignoreCase = true) == true -> {
                "No access to this folder.\n\n" +
                "Recommendations:\n" +
                "• Check folder permissions on server\n" +
                "• Verify your account has read access\n" +
                "• Try accessing from root directory first"
            }
            else -> {
                "Failed to load media files.\n\n" +
                "See error details below for more information."
            }
        }
        
        val errorDetails = buildString {
            append("Resource: ${resource.name}\n")
            append("Path: ${resource.path}\n")
            append("Type: ${resource.type}\n\n")
            append("Error: ${e.message ?: "Unknown error"}\n\n")
            append("Stack trace:\n${e.stackTraceToString()}")
        }
        
        sendEvent(BrowseEvent.ShowError(
            message = "$errorTitle\n\n$errorMessage",
            details = errorDetails,
            exception = e
        ))
        handleError(e)
    }

    fun setSortMode(sortMode: SortMode) {
        val resource = state.value.resource ?: return
        
        // Update state immediately for UI responsiveness
        updateState { it.copy(sortMode = sortMode) }
        
        // Save to database
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            updateResourceUseCase(resource.copy(sortMode = sortMode))
            // Saved sortMode for resource
        }
        
        // If we have cached list, re-sort it instead of rescanning
        val cachedFiles = MediaFilesCacheManager.getCachedList(resourceId)
        if (cachedFiles != null && cachedFiles.isNotEmpty()) {
            Timber.d("setSortMode: Applying sort to cached list (${cachedFiles.size} files)")
            // Apply current filter first, then sort
            val currentFilter = state.value.filter ?: FileFilter()
            val filteredFiles = applyFilterToList(cachedFiles, currentFilter)
            val sortedFiles = sortFiles(filteredFiles, sortMode, forceSort = true)  // User-requested sort - always apply
            // Update cache with full list (unfiltered) but sorted
            val sortedCache = sortFiles(cachedFiles, sortMode, forceSort = true)
            MediaFilesCacheManager.setCachedList(resourceId, sortedCache)
            updateState { it.copy(mediaFiles = sortedFiles) }
        } else {
            // No cache or empty cache - need to reload
            if (cachedFiles != null && cachedFiles.isEmpty()) {
                Timber.w("setSortMode: Found empty cache for resource $resourceId, clearing before reload")
                MediaFilesCacheManager.clearCache(resourceId)
            }
            loadMediaFiles()
        }
    }

    fun toggleDisplayMode() {
        val resource = state.value.resource ?: return
        
        val newMode = if (state.value.displayMode == DisplayMode.LIST) {
            DisplayMode.GRID
        } else {
            DisplayMode.LIST
        }
        
        // Update state immediately for UI responsiveness
        updateState { it.copy(displayMode = newMode) }
        
        // Save to database
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            updateResourceUseCase(resource.copy(displayMode = newMode))
            // Saved displayMode for resource
        }
    }

    fun selectFile(filePath: String) {
        selectionManager.toggleSelection(filePath)
    }
    
    fun selectFileRange(filePath: String) {
        val mediaFiles = state.value.mediaFiles
        selectionManager.selectRange(filePath, mediaFiles)
    }

    fun clearSelection() {
        selectionManager.clearSelection()
    }
    
    fun selectAll() {
        val mediaFiles = state.value.mediaFiles
        selectionManager.selectAll(mediaFiles)
    }

    fun openFile(file: MediaFile, approximatePosition: Int = 0) {
        val resource = state.value.resource ?: return
        
        // In pagination mode, pass approximatePosition to Player for smart chunk loading
        val index = if (state.value.usePagination) {
            Timber.d("openFile (pagination mode): ${file.name}, approximatePosition=$approximatePosition")
            approximatePosition
        } else {
            // Standard mode - find actual index in mediaFiles list
            val foundIndex = state.value.mediaFiles.indexOfFirst { it.path == file.path }
            if (foundIndex == -1) {
                Timber.w("openFile: Cache miss detected for ${file.path}, attempting fallback")
                if (!tryResolveMissingFile(file, approximatePosition)) {
                    sendEvent(BrowseEvent.ShowError("File not found: ${file.name}", null))
                }
                return
            }
            // Opening file in standard mode
            foundIndex
        }
        
        // Update state immediately for instant UI response
        val updatedResource = resource.copy(lastViewedFile = file.path)
        updateState { it.copy(resource = updatedResource) }
        
        // Save to database asynchronously
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            updateResourceUseCase(updatedResource)
            Timber.d("Saved lastViewedFile=${file.path} for resource: ${resource.name}")
        }
        
        sendEvent(BrowseEvent.NavigateToPlayer(file.path, index))
    }

    private fun tryResolveMissingFile(file: MediaFile, approximatePosition: Int): Boolean {
        val resource = state.value.resource ?: return false
        return when (resource.type) {
            ResourceType.SMB -> {
                resolveMissingSmbFile(resource, file, approximatePosition)
                true
            }
            else -> false
        }
    }

    private fun resolveMissingSmbFile(resource: MediaResource, file: MediaFile, approximatePosition: Int) {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            Timber.d("resolveMissingSmbFile: Refreshing metadata for ${file.path}")
            setLoading(true)
            try {
                val scanner = mediaScannerFactory.getScanner(resource.type)
                val smbScanner = scanner as? com.sza.fastmediasorter.data.network.SmbMediaScanner
                if (smbScanner == null) {
                    Timber.e("resolveMissingSmbFile: Unable to obtain SMB scanner for resource ${resource.name}")
                    sendEvent(BrowseEvent.ShowError("File not found: ${file.name}", file.path))
                    return@launch
                }

                val refreshed = smbScanner.getFileByPath(
                    path = file.path,
                    supportedTypes = resource.supportedMediaTypes,
                    credentialsId = resource.credentialsId
                )

                if (refreshed != null) {
                    mergeResolvedFileAndOpen(refreshed, approximatePosition)
                } else {
                    Timber.e("resolveMissingSmbFile: Remote SMB file still missing ${file.path}")
                    sendEvent(BrowseEvent.ShowError("File not found: ${file.name}", file.path))
                }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun mergeResolvedFileAndOpen(resolvedFile: MediaFile, approximatePosition: Int) {
        val resource = state.value.resource ?: return

        val updatedList = state.value.mediaFiles.toMutableList().apply {
            removeAll { it.path == resolvedFile.path }
            add(resolvedFile)
        }

        val sortedList = sortFiles(updatedList, state.value.sortMode, forceSort = true)

        MediaFilesCacheManager.setCachedList(resourceId, sortedList)
        updateState { current ->
            current.copy(
                mediaFiles = sortedList,
                totalFileCount = sortedList.size
            )
        }

        val targetIndex = sortedList.indexOfFirst { it.path == resolvedFile.path }
        val indexForPlayer = if (targetIndex >= 0) targetIndex else approximatePosition

        sendEvent(BrowseEvent.NavigateToPlayer(resolvedFile.path, indexForPlayer))
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            val selectedPaths = state.value.selectedFiles.toList()
            if (selectedPaths.isEmpty()) {
                sendEvent(BrowseEvent.ShowMessage("No files selected"))
                return@launch
            }
            
            setLoading(true)
            
            // Convert all paths to File objects (works for both local, network, and cloud)
            val filesToDelete = selectedPaths.map { path ->
                if (path.startsWith("smb://") || path.startsWith("sftp://") || path.startsWith("ftp://") || path.startsWith("cloud://")) {
                    // Network/cloud file - wrap in File object with original path
                    object : java.io.File(path) {
                        override fun getAbsolutePath(): String = path
                        override fun getPath(): String = path
                    }
                } else {
                    // Local file or SAF URI
                    java.io.File(path)
                }
            }
            
            // Determine if soft-delete is possible (only for local files, not SAF/network/cloud)
            val canUseSoftDelete = selectedPaths.all { path ->
                !path.startsWith("content:/") && !path.startsWith("smb://") && 
                !path.startsWith("sftp://") && !path.startsWith("ftp://") && 
                !path.startsWith("cloud://")
            }
            
            // Use FileOperationUseCase for delete (soft-delete for local, hard-delete for SAF/network/cloud)
            Timber.d("Deleting ${filesToDelete.size} files via FileOperationUseCase (softDelete=$canUseSoftDelete)")
            
            // Show progress message for large operations
            if (filesToDelete.size > 10) {
                sendEvent(BrowseEvent.ShowMessage(context.getString(R.string.deleting_n_files, filesToDelete.size)))
            }
            
            val deleteOperation = FileOperation.Delete(
                files = filesToDelete,
                softDelete = canUseSoftDelete // Use trash only for local files
            )
            
            when (val result = fileOperationUseCase.execute(deleteOperation)) {
                is com.sza.fastmediasorter.domain.usecase.FileOperationResult.Success -> {
                    val deletedCount = result.processedCount
                    Timber.i("Successfully deleted $deletedCount files (softDelete=$canUseSoftDelete)")
                    
                    // Save undo operation only for soft-delete (trash)
                    if (canUseSoftDelete) {
                        // result.copiedFilePaths format: [trashDirPath, originalPath1, originalPath2, ...]
                        val undoOp = UndoOperation(
                            type = com.sza.fastmediasorter.domain.model.FileOperationType.DELETE,
                            sourceFiles = selectedPaths,
                            destinationFolder = null,
                            copiedFiles = result.copiedFilePaths, // Contains trash dir + original paths
                            oldNames = null
                        )
                        saveUndoOperation(undoOp)
                    } else {
                        Timber.d("Hard delete used - no undo available for SAF/network/cloud files")
                    }
                    
                    // Clear selection and remove deleted files from list
                    clearSelection()
                    removeFiles(selectedPaths)
                    
                    sendEvent(BrowseEvent.ShowMessage(context.getString(R.string.deleted_n_files, deletedCount)))
                }
                is com.sza.fastmediasorter.domain.usecase.FileOperationResult.PartialSuccess -> {
                    val message = context.getString(R.string.deleted_n_of_m_files, result.processedCount, filesToDelete.size) + ". Errors: ${result.errors.take(3).joinToString(", ")}"
                    Timber.w(message)
                    
                    // PartialSuccess doesn't provide copiedFilePaths, can't create undo
                    // Remove successfully deleted files (first N from selected list)
                    clearSelection()
                    removeFiles(selectedPaths.take(result.processedCount))
                    
                    sendEvent(BrowseEvent.ShowMessage(message))
                }
                is com.sza.fastmediasorter.domain.usecase.FileOperationResult.Failure -> {
                    Timber.e("Failed to delete files: ${result.error}")
                    sendEvent(BrowseEvent.ShowError(
                        message = "Failed to delete files",
                        details = result.error
                    ))
                }
                is com.sza.fastmediasorter.domain.usecase.FileOperationResult.AuthenticationRequired -> {
                    Timber.w("Cloud authentication required: ${result.provider}")
                    sendEvent(BrowseEvent.CloudAuthRequired(result.provider, result.message))
                }
            }
            
            setLoading(false)
        }
    }
    
    fun saveUndoOperation(operation: UndoOperation) {
        undoManager.saveOperation(operation)
    }
    
    fun undoLastOperation() {
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            undoManager.undoLastOperation()
        }
    }
    
    /**
     * Clear undo operation if it has expired (older than 10 seconds).
     * Call this when activity resumes or when checking before showing undo button.
     */
    fun clearExpiredUndoOperation() {
        undoManager.clearIfExpired()
    }
    
    fun setFilter(filter: FileFilter?) {
        val resource = state.value.resource
        
        // IMPORTANT: Filter can only NARROW the selection, not expand it
        // If filter.mediaTypes is null, keep resource.supportedMediaTypes unchanged
        // If filter.mediaTypes is set, intersect with resource.supportedMediaTypes
        val newSupportedTypes = if (filter?.mediaTypes != null) {
            // Filter specifies types - use intersection with resource types
            resource?.supportedMediaTypes?.intersect(filter.mediaTypes) ?: filter.mediaTypes
        } else {
            // No filter types specified - keep resource types unchanged
            resource?.supportedMediaTypes ?: MediaType.entries.toSet()
        }
        
        // Update state immediately with both filter AND updated resource supportedMediaTypes
        val updatedResource = resource?.copy(supportedMediaTypes = newSupportedTypes)
        updateState { 
            it.copy(
                filter = filter,
                resource = updatedResource ?: it.resource
            )
        }
        
        // Save to database asynchronously
        if (updatedResource != null) {
            viewModelScope.launch(ioDispatcher + exceptionHandler) {
                updateResourceUseCase(updatedResource)
                Timber.d("Updated resource supportedMediaTypes in DB: $newSupportedTypes")
            }
        }
        
        applyFilter()
    }
    
    private fun applyFilter() {
        val resource = state.value.resource ?: return
        val filter = state.value.filter
        
        Timber.d("applyFilter: filter=$filter")
        
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            setLoading(true)
            
            // Get current settings for size filters
            val settings = settingsRepository.getSettings().first()
            val sizeFilter = SizeFilter(
                imageSizeMin = settings.imageSizeMin,
                imageSizeMax = settings.imageSizeMax,
                videoSizeMin = settings.videoSizeMin,
                videoSizeMax = settings.videoSizeMax,
                audioSizeMin = settings.audioSizeMin,
                audioSizeMax = settings.audioSizeMax
            )
            
            getMediaFilesUseCase(resource, state.value.sortMode, sizeFilter)
                .catch { e ->
                    Timber.e(e, "Error loading media files")
                    sendEvent(BrowseEvent.ShowError(
                        message = "Failed to load media files: ${e.message ?: "Unknown error"}",
                        details = e.stackTraceToString(),
                        exception = e
                    ))
                }
                .collect { files ->
                    Timber.d("applyFilter: loaded ${files.size} files")
                    
                    // Apply filter if exists
                    val filteredFiles = if (filter != null) {
                        Timber.d("applyFilter: applying filter to ${files.size} files")
                        applyFilterToList(files, filter).also {
                            Timber.d("applyFilter: filtered to ${it.size} files")
                        }
                    } else {
                        files
                    }
                    
                    updateState { 
                        it.copy(mediaFiles = filteredFiles) 
                    }
                    setLoading(false)
                }
        }
    }
    
    fun toggleFavorite(file: MediaFile) {
        val resource = state.value.resource ?: return
        
        // Optimistic UI update
        updateState { state ->
            val updatedFiles = state.mediaFiles.map { 
                if (it.path == file.path) it.copy(isFavorite = !it.isFavorite) else it 
            }
            state.copy(mediaFiles = updatedFiles)
        }
        
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            try {
                favoritesUseCase.toggleFavorite(file, resource.id)
                // If successful, UI is already correct.
            } catch (e: Exception) {
                Timber.e(e, "Error toggling favorite")
                // Revert UI on error
                updateState { state ->
                    val updatedFiles = state.mediaFiles.map { 
                        if (it.path == file.path) it.copy(isFavorite = !it.isFavorite) else it 
                    }
                    state.copy(mediaFiles = updatedFiles)
                }
                sendEvent(BrowseEvent.ShowError("Failed to update favorite status"))
            }
        }
    }

    private fun applyFilterToList(files: List<MediaFile>, filter: FileFilter): List<MediaFile> {
        return files.filter { file ->
            val matchesName = filter.nameContains == null || 
                file.name.contains(filter.nameContains, ignoreCase = true)
            
            val matchesMinDate = filter.minDate == null || 
                file.createdDate >= filter.minDate
            
            val matchesMaxDate = filter.maxDate == null || 
                file.createdDate <= filter.maxDate
            
            val fileSizeMb = file.size / (1024f * 1024f)
            val matchesMinSize = filter.minSizeMb == null || 
                fileSizeMb >= filter.minSizeMb
            
            val matchesMaxSize = filter.maxSizeMb == null || 
                fileSizeMb <= filter.maxSizeMb
            
            val matchesType = filter.mediaTypes == null || 
                filter.mediaTypes.contains(file.type)
            
            matchesName && matchesMinDate && matchesMaxDate && 
                matchesMinSize && matchesMaxSize && matchesType
        }
    }
    
    /**
     * Start FileObserver for local resources to detect external file changes
     */
    private fun startFileObserver() {
        val resource = state.value.resource ?: return
        
        // Only observe local folders
        if (resource.type != com.sza.fastmediasorter.domain.model.ResourceType.LOCAL) {
            return
        }
        
        // Stop previous observer if exists
        stopFileObserver()
        
        try {
            fileObserver = MediaFileObserver(
                path = resource.path,
                listener = object : MediaFileObserver.FileChangeListener {
                    override fun onFileDeleted(fileName: String) {
                        Timber.i("External file deleted: $fileName")
                        // Use targeted removal instead of full reload
                        val resource = state.value.resource ?: return
                        val fullPath = java.io.File(resource.path, fileName).absolutePath
                        removeFiles(listOf(fullPath))
                    }

                    override fun onFileCreated(fileName: String) {
                        Timber.i("External file created: $fileName")
                        // Reload files to reflect new file
                        reloadFiles()
                    }

                    override fun onFileMoved(fromName: String?, toName: String?) {
                        Timber.i("External file moved: from=$fromName, to=$toName")
                        // Reload files to reflect move
                        reloadFiles()
                    }

                    override fun onFileModified(fileName: String) {
                        Timber.d("External file modified: $fileName")
                        // Optionally reload to update file metadata
                        // For now, skip reload for modifications to avoid too many refreshes
                    }
                }
            )
            fileObserver?.startWatching()
            Timber.d("Started FileObserver for: ${resource.path}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start FileObserver for: ${resource.path}")
        }
    }
    
    /**
     * Stop FileObserver
     */
    private fun stopFileObserver() {
        fileObserver?.stopWatching()
        fileObserver = null
        Timber.d("Stopped FileObserver")
    }
    
    /**
     * Sorts files according to sort mode. Used for in-memory sorting without rescanning.
     */
    private fun sortFiles(files: List<MediaFile>, mode: SortMode, forceSort: Boolean = false): List<MediaFile> {
        // Skip sorting for large folders (> 500 files) ONLY during initial load
        // User-requested sorting (forceSort=true) always executes
        if (!forceSort && files.size > PAGINATION_THRESHOLD) {
            Timber.d("sortFiles: Large folder (${files.size} files), skipping auto-sort for performance")
            return files
        }
        
        return fileListManager.sortFiles(files, mode, forceSort)
    }
    
    /**
     * Save last viewed file path to resource for position restoration
     */
    fun saveLastViewedFile(filePath: String) {
        val resource = state.value.resource ?: return
        
        // Update state immediately for instant UI response
        val updatedResource = resource.copy(lastViewedFile = filePath)
        updateState { it.copy(resource = updatedResource) }
        
        // Save to database asynchronously
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            updateResourceUseCase(updatedResource)
            Timber.d("Saved lastViewedFile=$filePath for resource: ${resource.name}")
        }
    }
    
    /**
     * Save scroll position (first visible item) when leaving Browse screen.
     * Position is restored on next resource open.
     */
    fun saveScrollPosition(position: Int) {
        val resource = state.value.resource ?: return
        
        // Update state immediately
        val updatedResource = resource.copy(lastScrollPosition = position)
        updateState { it.copy(resource = updatedResource) }
        
        // Save to database asynchronously
        viewModelScope.launch(ioDispatcher + exceptionHandler) {
            updateResourceUseCase(updatedResource)
            Timber.d("Saved lastScrollPosition=$position for resource: ${resource.name}")
        }
    }
    
    /**
     * Create MediaFile object from java.io.File for undo operations
     */
    /**
     * Create MediaFile from java.io.File for instant list updates (e.g., after rename).
     * Used to avoid full resource reload when only single file metadata changes.
     */
    fun createMediaFileFromFile(file: java.io.File): MediaFile {
        val extension = file.extension.lowercase()
        val mediaType = MediaExtensions.getMediaType(extension)
        
        return MediaFile(
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            createdDate = file.lastModified(),
            type = mediaType,
            duration = null,
            width = null,
            height = null,
            exifOrientation = null,
            exifDateTime = null,
            exifLatitude = null,
            exifLongitude = null,
            videoCodec = null,
            videoBitrate = null,
            videoFrameRate = null,
            videoRotation = null
        )
    }
    
    /**
     * Cleanup trash folders in resource directory on background.
     * For LOCAL resources, cleans up .trash_* folders in resource path.
     * For network resources (SMB/FTP/SFTP), skips cleanup (not supported yet).
     * 
     * @param resource Resource to cleanup
     * @param maxAgeMs Maximum age of trash folders to keep (0 = delete all)
     */
    private fun cleanupTrashOnBackground(resource: MediaResource, maxAgeMs: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (resource.type) {
                    com.sza.fastmediasorter.domain.model.ResourceType.LOCAL -> {
                        val rootDir = java.io.File(resource.path)
                        if (!rootDir.exists() || !rootDir.isDirectory) {
                            return@launch
                        }
                        
                        val deletedCount = cleanupTrashFoldersUseCase.cleanup(rootDir, maxAgeMs)
                        if (deletedCount > 0) {
                            Timber.i("Cleaned up $deletedCount trash folders in ${resource.name}")
                        }
                    }
                    else -> {
                        // Network resource cleanup: SMB/SFTP/FTP/Cloud
                        // TODO: Implement network trash cleanup when needed
                        // Requires:
                        // 1. Scan for .trash folders via network clients
                        // 2. Delete each .trash folder recursively
                        // 3. Handle connection credentials properly
                        Timber.d("Network cleanup not yet implemented for resource type: ${resource.type}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup trash folders in ${resource.name}")
            }
        }
    }

    /**
     * Syncs current state with cache.
     * Called when returning from PlayerActivity to reflect changes (move/delete)
     * without full network reload.
     */
    fun syncWithCache() {
        val cachedFiles = MediaFilesCacheManager.getCachedList(resourceId)
        if (cachedFiles != null) {
            val currentFiles = state.value.mediaFiles
            
            // Always update state from cache to reflect deletions, moves, and renames
            // The size check was insufficient because renames don't change size, 
            // and sometimes cache updates might not be reflected if size happens to match
            Timber.d("syncWithCache: Syncing state with cache (${cachedFiles.size} files)")
             
            // Apply current filter to cached files before updating state
            val filter = state.value.filter
            val filteredFiles = if (filter != null) {
                applyFilterToList(cachedFiles, filter)
            } else {
                cachedFiles
            }
             
            Timber.d("syncWithCache: After filter applied - ${filteredFiles.size} files (was ${cachedFiles.size})")
            
            // Only update if list content actually changed to avoid unnecessary recompositions
            if (filteredFiles != currentFiles) {
                updateState { it.copy(mediaFiles = filteredFiles, totalFileCount = filteredFiles.size) }
                Timber.d("syncWithCache: State updated")
            } else {
                Timber.d("syncWithCache: State already up to date")
            }
        }
    }
    
    /**
     * Check if resource settings (supportedMediaTypes, scanSubdirectories) changed in database.
     * If changed, reload files to reflect new filter.
     * If not changed, sync with PlayerActivity cache for deleted/moved/renamed files.
     * Called from BrowseActivity.onResume() when returning from PlayerActivity or EditResourceActivity.
     */
    fun checkAndReloadIfResourceChanged() {
        val currentResource = state.value.resource ?: return
        
        // Skip for Favorites (virtual resource not in database)
        if (currentResource.id == -100L) {
            syncWithCache()
            return
        }
        
        viewModelScope.launch(ioDispatcher) {
            val updatedResource = getResourcesUseCase.getById(resourceId)
            if (updatedResource == null) {
                Timber.w("checkAndReloadIfResourceChanged: Resource not found in database")
                return@launch
            }
            
            // Compare supportedMediaTypes and scanSubdirectories
            val typesChanged = currentResource.supportedMediaTypes != updatedResource.supportedMediaTypes
            val subfoldersChanged = currentResource.scanSubdirectories != updatedResource.scanSubdirectories
            
            if (typesChanged || subfoldersChanged) {
                Timber.d("checkAndReloadIfResourceChanged: Resource settings changed, reloading files")
                Timber.d("  supportedMediaTypes: ${currentResource.supportedMediaTypes} -> ${updatedResource.supportedMediaTypes}")
                Timber.d("  scanSubdirectories: ${currentResource.scanSubdirectories} -> ${updatedResource.scanSubdirectories}")
                
                // Update resource in state before reloading
                updateState { it.copy(resource = updatedResource) }
                
                // Reload files with new filter
                reloadFiles()
            } else {
                Timber.d("checkAndReloadIfResourceChanged: No changes detected, syncing with cache")
                // No settings changed - just sync with PlayerActivity cache (deletions/moves/renames)
                syncWithCache()
            }
        }
    }
    
    /**
     * Remove deleted/moved files from the current list.
     * Called when returning from PlayerActivity with modified files list.
     * Much faster than reloading all files from network.
     */
    fun removeFilesFromList(paths: List<String>) {
        if (paths.isEmpty()) return
        
        val pathsSet = paths.toSet()
        val currentFiles = state.value.mediaFiles
        val updatedFiles = currentFiles.filterNot { it.path in pathsSet }
        
        if (updatedFiles.size != currentFiles.size) {
            Timber.d("removeFilesFromList: Removed ${currentFiles.size - updatedFiles.size} files from list")
            updateState { it.copy(mediaFiles = updatedFiles, totalFileCount = updatedFiles.size) }
            
            // Also update cache
            MediaFilesCacheManager.setCachedList(resourceId, updatedFiles)
        }
    }
}
