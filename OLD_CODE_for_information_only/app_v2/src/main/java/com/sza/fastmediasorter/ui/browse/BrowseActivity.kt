package com.sza.fastmediasorter.ui.browse

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.core.ui.BaseActivity
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.data.network.glide.NetworkFileDataFetcher
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
import com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient
import com.sza.fastmediasorter.databinding.ActivityBrowseBinding
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.FileFilter
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.model.UndoOperation
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.usecase.FileOperationUseCase
import com.sza.fastmediasorter.domain.usecase.GetDestinationsUseCase
import com.sza.fastmediasorter.ui.player.PlayerActivity
import com.sza.fastmediasorter.ui.browse.managers.BrowseDialogHelper
import com.sza.fastmediasorter.ui.browse.managers.BrowseMediaStoreObserver
import com.sza.fastmediasorter.ui.browse.managers.BrowseRecyclerViewManager
import com.sza.fastmediasorter.ui.browse.managers.KeyboardNavigationManager
import com.sza.fastmediasorter.utils.UserActionLogger
import dagger.hilt.android.AndroidEntryPoint
import com.sza.fastmediasorter.utils.setBadgeText
import com.sza.fastmediasorter.utils.clearBadge
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class BrowseActivity : BaseActivity<ActivityBrowseBinding>() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var mediaFileAdapter: MediaFileAdapter
    private lateinit var dialogHelper: BrowseDialogHelper
    private lateinit var mediaStoreObserver: BrowseMediaStoreObserver
    private lateinit var recyclerViewManager: BrowseRecyclerViewManager
    private lateinit var keyboardNavigationManager: KeyboardNavigationManager
    private lateinit var fileOperationsManager: com.sza.fastmediasorter.ui.browse.managers.BrowseFileOperationsManager
    private lateinit var smallControlsManager: com.sza.fastmediasorter.ui.browse.managers.BrowseSmallControlsManager
    private lateinit var cloudAuthManager: com.sza.fastmediasorter.ui.browse.managers.BrowseCloudAuthManager
    private lateinit var utilityManager: com.sza.fastmediasorter.ui.browse.managers.BrowseUtilityManager
    private lateinit var stateManager: com.sza.fastmediasorter.ui.browse.managers.BrowseStateManager
    
    @Inject
    lateinit var googleDriveClient: GoogleDriveRestClient
    
    @Inject
    lateinit var dropboxClient: com.sza.fastmediasorter.data.cloud.DropboxClient

    @Inject
    lateinit var oneDriveClient: com.sza.fastmediasorter.data.cloud.OneDriveRestClient
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result.data)
    }
    
    private val playerActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(PlayerActivity.EXTRA_MODIFIED_FILES)?.let { modifiedPaths ->
                // Remove deleted/moved files from adapter
                viewModel.removeFilesFromList(modifiedPaths)
            }
        }
    }
    
    // Flag to prevent duplicate file loading on first onResume after onCreate
    private var isFirstResume = true
    
    // Cache current display mode to avoid redundant updateDisplayMode() calls
    private var currentDisplayMode: DisplayMode? = null
    // Track last submitted sort mode to force adapter refresh when the user changes sorting
    private var lastSubmittedSortMode: SortMode? = null
    
    // Shared RecycledViewPool for optimizing ViewHolder reuse
    private val sharedViewPool = RecyclerView.RecycledViewPool().apply {
        // Set max recycled views for each view type
        // ViewType 0 = List item, ViewType 1 = Grid item
        setMaxRecycledViews(0, 30) // List view holders
        setMaxRecycledViews(1, 40) // Grid view holders (more needed for grid)
    }
    
    @Inject
    lateinit var fileOperationUseCase: FileOperationUseCase
    
    @Inject
    lateinit var getDestinationsUseCase: GetDestinationsUseCase
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var smbClient: SmbClient
    
    @Inject
    lateinit var sftpClient: SftpClient
    
    @Inject
    lateinit var ftpClient: FtpClient
    
    @Inject
    lateinit var credentialsRepository: com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository

    private var showVideoThumbnails = true // Cached setting value
    private var showPdfThumbnails = false // Cached PDF thumbnail setting
    private var shouldScrollToLastViewed = false // Flag for scroll restoration after PlayerActivity return

    override fun onDestroy() {
        // Log Glide cache statistics before destroying activity
        com.sza.fastmediasorter.utils.GlideCacheStats.logStats()
        stopMediaStoreObserver()
        super.onDestroy()
    }

    override fun getViewBinding(): ActivityBrowseBinding {
        return ActivityBrowseBinding.inflate(layoutInflater)
    }

    override fun setupViews() {
        // Reset Glide cache stats for this browsing session
        com.sza.fastmediasorter.utils.GlideCacheStats.reset()
        Timber.d("showVideoThumbnails initialized: $showVideoThumbnails")
        
        // Initialize managers
        dialogHelper = BrowseDialogHelper(this, object : BrowseDialogHelper.DialogCallbacks {
            override fun onFilterApplied(filter: FileFilter?) {
                viewModel.setFilter(filter)
                
                // Show toast only for user-defined filters (not resource type restrictions)
                val resource = viewModel.state.value.resource
                val isUserFilter = filter != null && !filter.isEmpty() && (
                    !filter.nameContains.isNullOrBlank() ||
                    filter.minDate != null ||
                    filter.maxDate != null ||
                    filter.minSizeMb != null ||
                    filter.maxSizeMb != null ||
                    (filter.mediaTypes != null && filter.mediaTypes != resource?.supportedMediaTypes)
                )
                
                if (isUserFilter) {
                    Toast.makeText(this@BrowseActivity, R.string.toast_filter_active, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onSortModeSelected(sortMode: SortMode) {
                viewModel.setSortMode(sortMode)
            }
            override fun onRenameConfirmed(oldName: String, newName: String) {
                // Not used - handled by RenameDialog directly
            }
            override fun onRenameMultipleConfirmed(files: List<Pair<String, String>>) {
                // Not used - handled internally in showRenameMultipleDialog
            }
            override fun onCopyDestinationSelected(destinationPath: String) {
                // Not used - handled by CopyToDialog
            }
            override fun onMoveDestinationSelected(destinationPath: String) {
                // Not used - handled by MoveToDialog
            }
            override fun onDeleteConfirmed(fileCount: Int) {
                viewModel.deleteSelectedFiles()
            }
            override fun onCloudSignInRequested() {
                launchGoogleSignIn()
            }
            override fun saveUndoOperation(undoOp: UndoOperation) {
                viewModel.saveUndoOperation(undoOp)
            }
            override fun reloadFiles() {
                viewModel.reloadFiles()
            }
            override fun updateFile(oldPath: String, newFile: MediaFile) {
                viewModel.updateFile(oldPath, newFile)
            }
            override fun createMediaFileFromFile(file: java.io.File): MediaFile {
                return viewModel.createMediaFileFromFile(file)
            }
            override fun getFileOperationUseCase(): com.sza.fastmediasorter.domain.usecase.FileOperationUseCase {
                return viewModel.fileOperationUseCase
            }
            override fun getResourceName(): String? {
                return viewModel.state.value.resource?.name
            }
            override fun getLifecycleOwner(): androidx.lifecycle.LifecycleOwner {
                return this@BrowseActivity
            }
        })
        
        mediaStoreObserver = BrowseMediaStoreObserver(this, object : BrowseMediaStoreObserver.MediaStoreCallbacks {
            override fun onMediaStoreChanged() {
                viewModel.reloadFiles()
            }
        })
        
        // Setup standard adapter (always used - no pagination) - MUST be initialized before recyclerViewManager
        mediaFileAdapter = MediaFileAdapter(
            onFileClick = { file ->
                UserActionLogger.logItemClick(file.name, context = "File click")
                viewModel.openFile(file)
            },
            onFileLongClick = { file ->
                UserActionLogger.logItemLongClick(file.name, context = "Range selection")
                // According to specification: long press selects range
                viewModel.selectFileRange(file.path)
            },
            onSelectionChanged = { file, selected ->
                UserActionLogger.logSelection(file.name, selected, context = "Checkbox click")
                viewModel.selectFile(file.path)
            },
            onSelectionRangeRequested = { file ->
                UserActionLogger.logItemLongClick(file.name, context = "Checkbox long click - range")
                // Long click on checkbox: select range from last selected file
                viewModel.selectFileRange(file.path)
            },
            onPlayClick = { file ->
                UserActionLogger.logButtonClick("Play", "File: ${file.name}")
                viewModel.openFile(file)
            },
            onFavoriteClick = { file ->
                UserActionLogger.logButtonClick("Favorite", "File: ${file.name}")
                viewModel.toggleFavorite(file)
            },
            onCopyClick = { file ->
                UserActionLogger.logButtonClick("Copy", "File: ${file.name}")
                viewModel.selectFile(file.path)
                showCopyDialog()
            },
            onMoveClick = { file ->
                UserActionLogger.logButtonClick("Move", "File: ${file.name}")
                viewModel.selectFile(file.path)
                showMoveDialog()
            },
            onRenameClick = { file ->
                UserActionLogger.logButtonClick("Rename", "File: ${file.name}")
                viewModel.selectFile(file.path)
                showRenameDialog()
            },
            onDeleteClick = { file ->
                UserActionLogger.logButtonClick("Delete", "File: ${file.name}")
                viewModel.selectFile(file.path)
                showDeleteConfirmation()
            },
            getShowVideoThumbnails = { showVideoThumbnails },
            getShowPdfThumbnails = { showPdfThumbnails }
        )
        
        recyclerViewManager = BrowseRecyclerViewManager(
            recyclerView = binding.rvMediaFiles,
            adapter = mediaFileAdapter,
            resources = resources,
            callbacks = object : BrowseRecyclerViewManager.RecyclerViewCallbacks {
                override fun onDisplayModeChanged(displayMode: DisplayMode) {
                    currentDisplayMode = displayMode
                }
                override fun updateToggleButtonIcon(iconResId: Int) {
                    binding.btnToggleView.setImageResource(iconResId)
                }
            }
        )
        
        keyboardNavigationManager = KeyboardNavigationManager(
            recyclerView = binding.rvMediaFiles,
            callbacks = object : KeyboardNavigationManager.KeyboardNavigationCallbacks {
                override fun getCurrentFocusPosition(): Int = this@BrowseActivity.getCurrentFocusPosition()
                override fun getMediaFilesCount(): Int = viewModel.state.value.mediaFiles.size
                override fun getSelectedFilesCount(): Int = viewModel.state.value.selectedFiles.size
                override fun toggleCurrentItemSelection(position: Int) = this@BrowseActivity.toggleCurrentItemSelection(position)
                override fun playCurrentOrSelected(position: Int) = this@BrowseActivity.playCurrentOrSelected(position)
                @Suppress("DEPRECATION")
                override fun onBackPressed() = this@BrowseActivity.onBackPressed()
                override fun showDeleteConfirmation() = this@BrowseActivity.showDeleteConfirmation()
                override fun showCopyDialog() = this@BrowseActivity.showCopyDialog()
                override fun showMoveDialog() = this@BrowseActivity.showMoveDialog()
                override fun performButtonClick(buttonId: Int) { /* Handled via direct button clicks */ }
            }
        )
        
        fileOperationsManager = com.sza.fastmediasorter.ui.browse.managers.BrowseFileOperationsManager(
            context = this,
            coroutineScope = lifecycleScope,
            fileOperationUseCase = fileOperationUseCase,
            getDestinationsUseCase = getDestinationsUseCase,
            smbClient = smbClient,
            sftpClient = sftpClient,
            ftpClient = ftpClient,
            credentialsRepository = credentialsRepository,
            callbacks = object : com.sza.fastmediasorter.ui.browse.managers.BrowseFileOperationsManager.FileOperationCallbacks {
                override fun onOperationCompleted() {
                    viewModel.reloadFiles()
                }
                override fun saveUndoOperation(undoOp: UndoOperation) {
                    viewModel.saveUndoOperation(undoOp)
                }
                override fun clearSelection() {
                    viewModel.clearSelection()
                }
                override fun getCacheDir(): File? = cacheDir
                override fun getExternalCacheDir(): File? = externalCacheDir
                override fun onAuthRequest(provider: String) {
                    when (provider) {
                        "dropbox" -> cloudAuthManager.launchDropboxSignIn()
                        "google_drive" -> cloudAuthManager.launchGoogleSignIn()
                        "onedrive" -> cloudAuthManager.launchOneDriveSignIn()
                        // Add other providers as needed
                    }
                }
            }
        )
        
        smallControlsManager = com.sza.fastmediasorter.ui.browse.managers.BrowseSmallControlsManager(binding)
        
        cloudAuthManager = com.sza.fastmediasorter.ui.browse.managers.BrowseCloudAuthManager(
            context = this,
            coroutineScope = lifecycleScope,
            googleDriveClient = googleDriveClient,
            dropboxClient = dropboxClient,
            oneDriveClient = oneDriveClient,
            googleSignInLauncher = googleSignInLauncher,
            callbacks = object : com.sza.fastmediasorter.ui.browse.managers.BrowseCloudAuthManager.CloudAuthCallbacks {
                override fun onAuthenticationSuccess() {
                    viewModel.reloadFiles()
                }
                override fun onAuthenticationFailure() {
                    // Error already shown in manager
                }
            }
        )
        
        utilityManager = com.sza.fastmediasorter.ui.browse.managers.BrowseUtilityManager(this)
        
        stateManager = com.sza.fastmediasorter.ui.browse.managers.BrowseStateManager(
            recyclerView = binding.rvMediaFiles,
            adapter = mediaFileAdapter,
            callbacks = object : com.sza.fastmediasorter.ui.browse.managers.BrowseStateManager.StateCallbacks {
                override fun saveLastViewedFile(filePath: String) {
                    viewModel.saveLastViewedFile(filePath)
                }
                override fun saveScrollPosition(position: Int) {
                    viewModel.saveScrollPosition(position)
                }
            }
        )
        
        binding.btnBack.setOnClickListener {
            UserActionLogger.logButtonClick("Back", "BrowseActivity")
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.rvMediaFiles.apply {
            // Always use standard adapter (pagination removed)
            adapter = mediaFileAdapter
            
            // Calculate optimal cache size based on screen size
            val displayMetrics = resources.displayMetrics
            val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
            // For list view: ~80dp per item, for grid: ~150dp per item
            // Cache 2 screens worth of items for smooth scrolling
            val optimalCacheSize = ((screenHeightDp / 80) * 2.0).toInt().coerceIn(20, 50)
            setItemViewCacheSize(optimalCacheSize)
            
            // Use shared RecycledViewPool for efficient ViewHolder reuse
            setRecycledViewPool(sharedViewPool)
            
            // Set fixed size for better performance (item size doesn't change)
            setHasFixedSize(true)
            
            // Enable prefetching for smooth scrolling, but limit to 1-2 rows ahead
            // Too aggressive prefetch causes network congestion with 8 parallel connections
            layoutManager?.isItemPrefetchEnabled = true
            (layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = 4 // ~1 row ahead
            (layoutManager as? GridLayoutManager)?.initialPrefetchItemCount = 6  // ~2 rows ahead (3 columns × 2 rows)
            
            // Add scroll listener for user action logging
            addOnScrollListener(UserActionLogger.createScrollListener("BrowseActivity"))
            
            Timber.d("RecyclerView optimizations: cacheSize=$optimalCacheSize, screenHeightDp=$screenHeightDp")
        }
        
        // Setup FastScroller for interactive scrollbar (can drag with finger/mouse)
        FastScrollerBuilder(binding.rvMediaFiles).useMd2Style().build()

        binding.btnSort.setOnClickListener {
            UserActionLogger.logButtonClick("Sort", "BrowseActivity")
            showSortDialog()
        }

        binding.btnFilter.setOnClickListener {
            UserActionLogger.logButtonClick("Filter", "BrowseActivity")
            showFilterDialog()
        }

        binding.btnRefresh.setOnClickListener {
            UserActionLogger.logButtonClick("Refresh", "BrowseActivity")
            Timber.d("Manual refresh requested")
            // Clear failed video thumbnail cache to retry previously failed videos
            NetworkFileDataFetcher.clearFailedVideoCache()
            mediaFileAdapter.incrementRefreshVersion() // Force thumbnail reload
            viewModel.reloadFiles()
        }
        
        binding.btnStopScan.setOnClickListener {
            UserActionLogger.logButtonClick("StopScan", "BrowseActivity")
            Timber.d("Stop scan requested by user")
            viewModel.cancelScan()
            val fileCount = viewModel.state.value.mediaFiles.size
            Toast.makeText(this, getString(R.string.scan_stopped, fileCount), Toast.LENGTH_SHORT).show()
        }

        binding.btnToggleView.setOnClickListener {
            UserActionLogger.logButtonClick("ToggleView", "BrowseActivity")
            viewModel.toggleDisplayMode()
        }
        
        binding.btnSelectAll.setOnClickListener {
            UserActionLogger.logButtonClick("SelectAll", "BrowseActivity")
            viewModel.selectAll()
        }
        
        binding.btnDeselectAll.setOnClickListener {
            UserActionLogger.logButtonClick("DeselectAll", "BrowseActivity")
            viewModel.clearSelection()
        }

        binding.btnCopy.setOnClickListener {
            UserActionLogger.logButtonClick("Copy", "BrowseActivity - Toolbar")
            showCopyDialog()
        }

        binding.btnMove.setOnClickListener {
            UserActionLogger.logButtonClick("Move", "BrowseActivity - Toolbar")
            showMoveDialog()
        }

        binding.btnRename.setOnClickListener {
            UserActionLogger.logButtonClick("Rename", "BrowseActivity - Toolbar")
            showRenameDialog()
        }

        binding.btnDelete.setOnClickListener {
            UserActionLogger.logButtonClick("Delete", "BrowseActivity - Toolbar")
            showDeleteConfirmation()
        }
        
        binding.btnUndo.setOnClickListener {
            UserActionLogger.logButtonClick("Undo", "BrowseActivity")
            viewModel.undoLastOperation()
        }
        
        binding.btnShare.setOnClickListener {
            UserActionLogger.logButtonClick("Share", "BrowseActivity")
            shareSelectedFiles()
        }

        binding.btnPlay.setOnClickListener {
            UserActionLogger.logButtonClick("Play", "BrowseActivity - Toolbar")
            startSlideshow()
        }
        
        binding.btnRetry.setOnClickListener {
            UserActionLogger.logButtonClick("Retry", "BrowseActivity")
            viewModel.clearError()
            viewModel.reloadFiles()
        }
        
        // Scroll to top button
        binding.fabScrollToTop.setOnClickListener {
            UserActionLogger.logButtonClick("ScrollToTop", "BrowseActivity")
            val layoutManager = binding.rvMediaFiles.layoutManager
            when (layoutManager) {
                is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(0, 0)
                is GridLayoutManager -> layoutManager.scrollToPositionWithOffset(0, 0)
                else -> binding.rvMediaFiles.scrollToPosition(0)
            }
            Timber.d("Scrolled to top (position 0)")
        }
        
        // Scroll to bottom button
        binding.fabScrollToBottom.setOnClickListener {
            UserActionLogger.logButtonClick("ScrollToBottom", "BrowseActivity")
            val itemCount = mediaFileAdapter.itemCount
            if (itemCount > 0) {
                val layoutManager = binding.rvMediaFiles.layoutManager
                when (layoutManager) {
                    is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(itemCount - 1, 0)
                    is GridLayoutManager -> layoutManager.scrollToPositionWithOffset(itemCount - 1, 0)
                    else -> binding.rvMediaFiles.scrollToPosition(itemCount - 1)
                }
                Timber.d("Scrolled to bottom (position ${itemCount - 1})")
            }
        }
    }

    /**
     * Handle configuration changes (screen rotation).
     * Recalculates grid layout based on new screen dimensions.
     */
    override fun onLayoutConfigurationChanged(newConfig: Configuration) {
        // Force display mode recalculation with new screen dimensions
        lifecycleScope.launch {
            currentDisplayMode?.let { mode ->
                // Reset cached mode to force recalculation
                currentDisplayMode = null
                updateDisplayMode(mode)
                Timber.d("onLayoutConfigurationChanged: Recalculated display mode for screenWidthDp=${newConfig.screenWidthDp}")
            }
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Don't log every collect - only when actually submitting list (reduces log spam)
                    
                    // Always use standard mode (no pagination)
                    // Only submit list if content actually changed
                    // Compare with last emitted list from ViewModel (survives Activity recreation)
                    val previousMediaFiles = viewModel.lastEmittedMediaFiles
                    val previousSize = previousMediaFiles?.size ?: -1
                    
                    val sortChanged = state.sortMode != lastSubmittedSortMode
                    val shouldSubmit = if (previousMediaFiles == null) {
                        true
                    } else if (sortChanged) {
                        true
                    } else if (state.mediaFiles === previousMediaFiles) {
                        false
                    } else {
                        // Lists are different instances - let DiffUtil determine changes
                        // This handles size changes, content changes, and property changes (like isFavorite)
                        true
                    }
                    
                    if (shouldSubmit) {
                        viewModel.markListAsSubmitted(state.mediaFiles)
                        lastSubmittedSortMode = state.sortMode
                        
                        // Standard mode - submit full list to MediaFileAdapter
                        val previousListSize = mediaFileAdapter.itemCount
                        
                        val reason = when {
                            previousMediaFiles == null -> "First load"
                            state.mediaFiles.size != previousSize -> "Size changed ($previousSize → ${state.mediaFiles.size})"
                            else -> "Content changed"
                        }
                        Timber.d("Submitting list: $reason, resource=${state.resource?.name}")
                        
                        // Enable deferred thumbnail loading
                        mediaFileAdapter.setSkipInitialThumbnailLoad(true)
                        
                        mediaFileAdapter.submitList(state.mediaFiles) {
                            Timber.d("=== submitList CALLBACK START ===")
                            Timber.d("Adapter list submitted successfully, current itemCount=${mediaFileAdapter.itemCount}")
                            Timber.d("skipInitialThumbnailLoad flag BEFORE check: ${mediaFileAdapter.getSkipInitialThumbnailLoad()}")
                            
                            // Trigger thumbnail loading after layout is complete (ONLY if we have items)
                            if (mediaFileAdapter.itemCount > 0) {
                                // Check if layout is already complete
                                Timber.d("RecyclerView.isLaidOut = ${binding.rvMediaFiles.isLaidOut}")
                                if (binding.rvMediaFiles.isLaidOut && binding.rvMediaFiles.childCount > 0) {
                                    // Layout already done AND children are bound - trigger immediately
                                    Timber.d("=== RecyclerView ALREADY LAID OUT WITH CHILDREN - triggering thumbnails immediately ===")
                                    val layoutManager = binding.rvMediaFiles.layoutManager
                                    Timber.d("LayoutManager type: ${layoutManager?.javaClass?.simpleName}")
                                    
                                    val firstVisible = when (layoutManager) {
                                        is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                                        is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                                        else -> 0
                                    }
                                    val lastVisible = when (layoutManager) {
                                        is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
                                        is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
                                        else -> minOf(20, mediaFileAdapter.itemCount - 1)
                                    }
                                    
                                    Timber.d("Visible range: first=$firstVisible, last=$lastVisible, itemCount=${mediaFileAdapter.itemCount}")
                                    
                                    if (firstVisible >= 0 && lastVisible >= firstVisible) {
                                        val visibleCount = lastVisible - firstVisible + 1
                                        Timber.d(">>> Calling notifyItemRangeChanged($firstVisible, $visibleCount, LOAD_THUMBNAILS)")
                                        mediaFileAdapter.notifyItemRangeChanged(firstVisible, visibleCount, "LOAD_THUMBNAILS")
                                        Timber.d("<<< notifyItemRangeChanged completed")
                                    } else {
                                        Timber.w("NOT calling notifyItemRangeChanged - invalid range: $firstVisible to $lastVisible")
                                        Timber.w("RV isAttachedToWindow=${binding.rvMediaFiles.isAttachedToWindow}, childCount=${binding.rvMediaFiles.childCount}")
                                    }
                                    
                                    // Reset flag
                                    Timber.d("Resetting skipInitialThumbnailLoad flag to false")
                                    mediaFileAdapter.setSkipInitialThumbnailLoad(false)
                                    Timber.d("skipInitialThumbnailLoad flag AFTER reset: ${mediaFileAdapter.getSkipInitialThumbnailLoad()}")
                                } else {
                                    // Layout ready but no children yet OR not laid out - use post {} to trigger after children are bound
                                    Timber.d("=== RecyclerView laid out but childCount=${binding.rvMediaFiles.childCount} - using post {} ===")
                                    binding.rvMediaFiles.post {
                                        Timber.d("=== POST EXECUTED - checking for visible items ===")
                                        val layoutManager = binding.rvMediaFiles.layoutManager
                                        Timber.d("LayoutManager type: ${layoutManager?.javaClass?.simpleName}, childCount=${binding.rvMediaFiles.childCount}")
                                        
                                        val firstVisible = when (layoutManager) {
                                            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                                            is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                                            else -> 0
                                        }
                                        val lastVisible = when (layoutManager) {
                                            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
                                            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
                                            else -> minOf(20, mediaFileAdapter.itemCount - 1)
                                        }
                                        
                                        Timber.d("Visible range: first=$firstVisible, last=$lastVisible, itemCount=${mediaFileAdapter.itemCount}")
                                        
                                        if (firstVisible >= 0 && lastVisible >= firstVisible) {
                                            val visibleCount = lastVisible - firstVisible + 1
                                            Timber.d(">>> Calling notifyItemRangeChanged($firstVisible, $visibleCount, LOAD_THUMBNAILS)")
                                            mediaFileAdapter.notifyItemRangeChanged(firstVisible, visibleCount, "LOAD_THUMBNAILS")
                                            Timber.d("<<< notifyItemRangeChanged completed")
                                        } else {
                                            Timber.w("NOT calling notifyItemRangeChanged in post - invalid range: $firstVisible to $lastVisible")
                                            Timber.w("RV isAttachedToWindow=${binding.rvMediaFiles.isAttachedToWindow}, childCount=${binding.rvMediaFiles.childCount}")
                                        }
                                        
                                        // Reset flag after post execution
                                        Timber.d("Resetting skipInitialThumbnailLoad flag to false (in post)")
                                        mediaFileAdapter.setSkipInitialThumbnailLoad(false)
                                        Timber.d("skipInitialThumbnailLoad flag AFTER reset: ${mediaFileAdapter.getSkipInitialThumbnailLoad()}")
                                    }
                                }
                            } else {
                                // Empty list - just reset flag
                                Timber.d("Empty list (itemCount=0), resetting skipInitialThumbnailLoad flag")
                                mediaFileAdapter.setSkipInitialThumbnailLoad(false)
                            }
                            
                            // Update empty state AFTER adapter updates itemCount
                            val itemCount = mediaFileAdapter.itemCount
                            
                            if (itemCount > 0) {
                                // Files loaded
                                Timber.d("Empty state: hidden (itemCount=$itemCount)")
                            } else {
                                // No items yet - check loading state
                                val isLoading = viewModel.loading.value
                                
                                if (isLoading) {
                                    // Loading in progress
                                    Timber.d("Empty state: hidden during loading (layoutProgress visible)")
                                } else {
                                    // Loading complete, no files
                                    Timber.d("Empty state: no files found (isLoading=false, itemCount=0)")
                                }
                            }
                            Timber.d("UI visibility: rvMediaFiles.isVisible=${binding.rvMediaFiles.isVisible}")
                            
                            // Restore scroll position after adapter updates
                            if (itemCount > 0) {
                                // Priority 1: Restore to lastViewedFile (return from PlayerActivity)
                                if (shouldScrollToLastViewed) {
                                    state.resource?.lastViewedFile?.let { lastViewedPath ->
                                        Timber.d("submitList callback: Restoring scroll to lastViewedFile: $lastViewedPath")
                                        
                                        val position = state.mediaFiles.indexOfFirst { it.path == lastViewedPath }
                                        Timber.d("submitList callback: Found position=$position for file: ${lastViewedPath.substringAfterLast('/')}")
                                        
                                        if (position >= 0) {
                                            binding.rvMediaFiles.post {
                                                val layoutManager = binding.rvMediaFiles.layoutManager
                                                when (layoutManager) {
                                                    is LinearLayoutManager -> {
                                                        layoutManager.scrollToPositionWithOffset(position, 0)
                                                        Timber.i("submitList callback: ✓ Scrolled to '${state.mediaFiles[position].name}' at position $position (LinearLayoutManager)")
                                                    }
                                                    is GridLayoutManager -> {
                                                        layoutManager.scrollToPositionWithOffset(position, 0)
                                                        Timber.i("submitList callback: ✓ Scrolled to '${state.mediaFiles[position].name}' at position $position (GridLayoutManager)")
                                                    }
                                                    else -> {
                                                        binding.rvMediaFiles.scrollToPosition(position)
                                                        Timber.w("submitList callback: Scrolled to position $position (fallback)")
                                                    }
                                                }
                                            }
                                        } else {
                                            Timber.w("submitList callback: File not found in list: $lastViewedPath")
                                        }
                                    } ?: Timber.w("submitList callback: lastViewedFile is null")
                                    shouldScrollToLastViewed = false
                                } 
                                // Priority 2: Restore to lastScrollPosition (first open or reopen after back button)
                                else if (isFirstResume && state.resource?.lastScrollPosition != null && state.resource.lastScrollPosition > 0) {
                                    val position = state.resource.lastScrollPosition
                                    // Validate position is within bounds
                                    if (position < itemCount) {
                                        binding.rvMediaFiles.post {
                                            val layoutManager = binding.rvMediaFiles.layoutManager
                                            when (layoutManager) {
                                                is LinearLayoutManager -> {
                                                    layoutManager.scrollToPositionWithOffset(position, 0)
                                                    Timber.i("submitList callback: ✓ Restored scroll to saved position $position (LinearLayoutManager)")
                                                }
                                                is GridLayoutManager -> {
                                                    layoutManager.scrollToPositionWithOffset(position, 0)
                                                    Timber.i("submitList callback: ✓ Restored scroll to saved position $position (GridLayoutManager)")
                                                }
                                                else -> {
                                                    binding.rvMediaFiles.scrollToPosition(position)
                                                    Timber.i("submitList callback: ✓ Restored scroll to saved position $position (fallback)")
                                                }
                                            }
                                        }
                                    } else {
                                        Timber.w("submitList callback: Saved position $position is out of bounds (itemCount=$itemCount)")
                                    }
                                }
                            }
                            
                            // Update scroll buttons visibility based on file count
                            updateScrollButtonsVisibility(itemCount)
                        }
                    }
                    // No log for skipped submitList - reduces log spam during large folder loading
                    
                    mediaFileAdapter.setSelectedPaths(state.selectedFiles)
                    state.resource?.let { resource ->
                        mediaFileAdapter.setCredentialsId(resource.credentialsId)
                        mediaFileAdapter.setDisableThumbnails(resource.disableThumbnails)
                        
                        // Update item operation buttons visibility based on resource permissions
                        lifecycleScope.launch {
                            val hasDestinations = getDestinationsUseCase.getDestinationsExcluding(resource.id).isNotEmpty()
                            mediaFileAdapter.setResourcePermissions(
                                hasDestinations = hasDestinations,
                                isWritable = resource.isWritable && !resource.isReadOnly
                            )
                        }
                    }

                    state.resource?.let { _ ->
                        binding.tvResourceInfo.text = buildResourceInfo(state)
                    }

                    // Show filter warning ONLY for user-defined filters (not resource type restrictions)
                    // Filter is considered user-defined if it has ANY criteria beyond mediaTypes
                    // OR if mediaTypes differ from resource.supportedMediaTypes
                    val filter = state.filter
                    val resource = state.resource
                    val isUserFilter = filter != null && !filter.isEmpty() && (
                        !filter.nameContains.isNullOrBlank() ||
                        filter.minDate != null ||
                        filter.maxDate != null ||
                        filter.minSizeMb != null ||
                        filter.maxSizeMb != null ||
                        (filter.mediaTypes != null && filter.mediaTypes != resource?.supportedMediaTypes)
                    )
                    
                    if (isUserFilter) {
                        // Show short toast instead of permanent warning line
                        // Toast already shown when filter is applied, no need to repeat
                        binding.tvFilterWarning.isVisible = false
                    } else {
                        binding.tvFilterWarning.isVisible = false
                    }
                    
                    // Update filter badge (show red circle ONLY for user-defined filters)
                    if (isUserFilter) {
                        val filterCount = state.filter?.activeFilterCount() ?: 0
                        binding.btnFilter.setBadgeText(filterCount.toString())
                    } else {
                        binding.btnFilter.clearBadge()
                    }

                    val hasSelection = state.selectedFiles.isNotEmpty()
                    // resource is already defined above
                    val isWritable = (resource?.isWritable ?: false) && (resource?.isReadOnly != true)
                    
                    // Show operations panel only when there are selected files or undo available
                    binding.layoutOperations.isVisible = hasSelection || state.lastOperation != null
                    
                    binding.btnCopy.isVisible = hasSelection
                    binding.btnMove.isVisible = hasSelection && isWritable
                    binding.btnRename.isVisible = hasSelection && isWritable
                    binding.btnDelete.isVisible = hasSelection && isWritable
                    binding.btnUndo.isVisible = state.lastOperation != null
                    binding.btnShare.isVisible = hasSelection

                    // Only update display mode if it actually changed
                    if (state.displayMode != currentDisplayMode) {
                        currentDisplayMode = state.displayMode
                        updateDisplayMode(state.displayMode)
                    }
                    
                    // Apply or restore small controls based on setting
                    if (state.showSmallControls) {
                        smallControlsManager.applySmallControlsIfNeeded()
                    } else {
                        smallControlsManager.restoreCommandButtonHeightsIfNeeded()
                    }
                }
            }
        }

        // Observe settings for favorite button visibility
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    settingsRepository.getSettings(),
                    viewModel.state
                ) { settings, state ->
                    // Show favorite button if:
                    // 1. enableFavorites setting is on, OR
                    // 2. Currently viewing Favorites resource (id = -100)
                    settings.enableFavorites || state.resource?.id == -100L
                }.collect { shouldShow ->
                    mediaFileAdapter.setShowFavoriteButton(shouldShow)
                }
            }
        }

        // Observe hideGridActionButtons setting
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.getSettings().collect { settings ->
                    mediaFileAdapter.setHideGridActionButtons(settings.hideGridActionButtons)
                }
            }
        }

        // Observe loading state and STOP button visibility together
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.loading, viewModel.state) { isLoading, state ->
                    Pair(isLoading, state)
                }.collect { (isLoading, state) ->
                    binding.layoutProgress.isVisible = isLoading
                    binding.btnStopScan.isVisible = state.isScanCancellable && isLoading
                    
                    // Debug logging for STOP button visibility
                    Timber.d("Progress UI update: isLoading=$isLoading, isScanCancellable=${state.isScanCancellable}, btnStopScan.visible=${state.isScanCancellable && isLoading}, progress=${state.loadingProgress}")
                    
                    // Update progress message
                    if (state.loadingProgress > 0) {
                        binding.tvProgressMessage.text = getString(R.string.loading) + " (${state.loadingProgress})"
                    } else {
                        binding.tvProgressMessage.text = getString(R.string.loading)
                    }
                }
            }
        }

        // Observe settings changes
        var lastIconSize = 96 // Track last known icon size
        var lastShowPdfThumbnails = showPdfThumbnails // Track PDF thumbnail setting changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.getSettings().collect { settings ->
                    Timber.d("PDF_THUMB_DEBUG: Settings loaded - showPdfThumbnails=${settings.showPdfThumbnails}, current=$showPdfThumbnails, itemCount=${mediaFileAdapter.itemCount}")
                    val pdfThumbnailsChanged = showPdfThumbnails != settings.showPdfThumbnails
                    showVideoThumbnails = settings.showVideoThumbnails
                    showPdfThumbnails = settings.showPdfThumbnails
                    Timber.d("PDF_THUMB_DEBUG: After update - showPdfThumbnails=$showPdfThumbnails, changed=$pdfThumbnailsChanged")
                    
                    // If PDF thumbnail setting changed, refresh visible items to load/hide thumbnails
                    if (pdfThumbnailsChanged && mediaFileAdapter.itemCount > 0) {
                        Timber.d("PDF_THUMB_DEBUG: Triggering notifyItemRangeChanged for ${mediaFileAdapter.itemCount} items")
                        mediaFileAdapter.notifyItemRangeChanged(0, mediaFileAdapter.itemCount, "LOAD_THUMBNAILS")
                    }
                    
                    // Update grid cell size when thumbnail size changes in settings
                    val currentResource = viewModel.state.value.resource
                    if (currentResource != null && 
                        currentResource.displayMode == DisplayMode.GRID && 
                        settings.defaultIconSize != lastIconSize) {
                        
                        lastIconSize = settings.defaultIconSize
                        Timber.d("Thumbnail size changed to ${settings.defaultIconSize}, updating grid layout")
                        updateDisplayMode(DisplayMode.GRID)
                    } else if (currentResource != null) {
                        lastIconSize = settings.defaultIconSize
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMessage ->
                    // Show error state if error occurred and no files loaded
                    val hasError = errorMessage != null
                    val isEmpty = mediaFileAdapter.itemCount == 0
                    
                    binding.errorStateView.isVisible = hasError && isEmpty
                    // Empty state removed per user request
                    
                    if (hasError && isEmpty) {
                        binding.tvErrorMessage.text = errorMessage
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is BrowseEvent.ShowError -> {
                            showError(event.message, event.details, event.exception)
                        }
                        is BrowseEvent.ShowMessage -> {
                            Toast.makeText(this@BrowseActivity, event.message, Toast.LENGTH_SHORT).show()
                        }
                        is BrowseEvent.ShowUndoToast -> {
                            // Replaced Toast with Snackbar for better UX
                            val operation = viewModel.state.value.lastOperation
                            if (operation != null) {
                                showUndoSnackbar(operation)
                            }
                        }
                        is BrowseEvent.NavigateToPlayer -> {
                            val resourceId = viewModel.state.value.resource?.id ?: 0L
                            // Pass skipAvailabilityCheck to prevent redundant checks
                            val skipCheck = intent.getBooleanExtra(EXTRA_SKIP_AVAILABILITY_CHECK, false)
                            val playerIntent = PlayerActivity.createIntent(
                                this@BrowseActivity,
                                resourceId,
                                event.fileIndex,
                                skipCheck,
                                event.filePath // Pass file path for pagination mode
                            )
                            playerActivityLauncher.launch(playerIntent)
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                        is BrowseEvent.ShowCloudAuthenticationRequired -> {
                            showCloudAuthenticationDialog("Google Drive authentication required")
                        }
                        is BrowseEvent.CloudAuthRequired -> {
                            showCloudAuthenticationDialog(event.message)
                        }
                        is BrowseEvent.NoFilesFound -> {
                            val msg = if (event.messageResId != null) {
                                getString(event.messageResId)
                            } else {
                                event.message ?: ""
                            }
                            Toast.makeText(this@BrowseActivity, msg, Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Show error message respecting showDetailedErrors setting
     * If showDetailedErrors=true: shows ErrorDialog with copyable text and detailed info
     * If showDetailedErrors=false: shows Toast (short notification)
     */
    private fun showError(message: String, details: String?, exception: Throwable? = null) {
        // Check if this is a Google Drive authentication error
        if (message.contains("Google Drive authentication required", ignoreCase = true) ||
            message.contains("Not authenticated", ignoreCase = true)) {
            showCloudAuthenticationDialog(message)
            return
        }
        
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first()
            Timber.d("showError: showDetailedErrors=${settings.showDetailedErrors}, message=$message, hasDetails=${details != null}, hasException=${exception != null}")
            
            if (settings.showDetailedErrors) {
                // Use ErrorDialog with full details
                if (exception != null) {
                    // Show exception with stack trace
                    com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                        context = this@BrowseActivity,
                        title = getString(R.string.error),
                        throwable = exception
                    )
                } else if (details != null) {
                    // Show message with details
                    com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                        context = this@BrowseActivity,
                        title = getString(R.string.error),
                        message = message,
                        details = details
                    )
                } else {
                    // Show only message
                    com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                        context = this@BrowseActivity,
                        title = getString(R.string.error),
                        message = message
                    )
                }
            } else {
                // Simple toast for users who don't want details
                Toast.makeText(this@BrowseActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Show Snackbar with operation description and Undo button
     */
    private fun showUndoSnackbar(operation: UndoOperation) {
        if (isFinishing || isDestroyed) {
            return
        }
        
        val count = operation.sourceFiles.size
        val description = when (operation.type) {
            com.sza.fastmediasorter.domain.model.FileOperationType.DELETE -> {
                getString(R.string.deleted_n_files, count)
            }
            com.sza.fastmediasorter.domain.model.FileOperationType.COPY -> {
                val destination = operation.destinationFolder?.substringAfterLast('/') ?: "destination"
                getString(R.string.msg_copy_success_count, count, destination)
            }
            com.sza.fastmediasorter.domain.model.FileOperationType.MOVE -> {
                val destination = operation.destinationFolder?.substringAfterLast('/') ?: "destination"
                getString(R.string.msg_move_success_count, count, destination)
            }
            com.sza.fastmediasorter.domain.model.FileOperationType.RENAME -> {
                getString(R.string.renamed_n_files, count)
            }
        }

        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            description,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
            .setAction(getString(R.string.undo).uppercase()) {
                viewModel.undoLastOperation()
            }
            .setAnchorView(binding.layoutOperations)
            .show()
    }
    
    @Deprecated("Use showError() instead - respects showDetailedErrors setting")
    private fun showErrorDialog(message: String, details: String?) {
        dialogHelper.showErrorDialog(message, details)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyboardNavigationManager.handleKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
    
    private fun getCurrentFocusPosition(): Int {
        return stateManager.getCurrentFocusPosition()
    }
    
    private fun toggleCurrentItemSelection(position: Int) {
        if (position in 0 until viewModel.state.value.mediaFiles.size) {
            val file = viewModel.state.value.mediaFiles[position]
            viewModel.selectFile(file.path)
            Timber.d("Toggled selection for position $position: ${file.name}")
        }
    }
    
    private fun playCurrentOrSelected(position: Int) {
        val state = viewModel.state.value
        if (state.selectedFiles.isNotEmpty()) {
            // Play first selected file
            val firstSelected = state.mediaFiles.firstOrNull { it.path in state.selectedFiles }
            if (firstSelected != null) {
                viewModel.openFile(firstSelected)
            }
        } else if (position in 0 until state.mediaFiles.size) {
            // Play file at current position
            val file = state.mediaFiles[position]
            viewModel.openFile(file)
        }
    }

    private fun buildResourceInfo(state: BrowseState): String {
        return utilityManager.buildResourceInfo(state)
    }

    private fun buildFilterDescription(filter: FileFilter): String {
        return utilityManager.buildFilterDescription(filter)
    }

    private suspend fun updateDisplayMode(mode: DisplayMode) {
        val settings = settingsRepository.getSettings().first()
        val currentResource = viewModel.state.value.resource
        val iconSize = if (currentResource?.disableThumbnails == true) 32 else settings.defaultIconSize
        
        recyclerViewManager.updateDisplayMode(mode, iconSize, showVideoThumbnails)
        currentDisplayMode = mode
    }

    private fun showFilterDialog() {
        val allowedTypes = viewModel.state.value.resource?.supportedMediaTypes
        dialogHelper.showFilterDialog(viewModel.state.value.filter, allowedTypes)
    }

    private fun showSortDialog() {
        dialogHelper.showSortDialog(viewModel.state.value.sortMode)
    }

    private fun showDeleteConfirmation() {
        val state = viewModel.state.value
        val resource = state.resource
        
        if (resource?.isReadOnly == true) {
            Toast.makeText(this, R.string.error_read_only, Toast.LENGTH_SHORT).show()
            return
        }
        
        val count = state.selectedFiles.size
        lifecycleScope.launch {
            val settings = viewModel.getSettings()
            dialogHelper.showDeleteConfirmation(count, settings)
        }
    }
    
    private fun showRenameDialog() {
        val state = viewModel.state.value
        if (state.resource?.isReadOnly == true) {
            Toast.makeText(this, R.string.error_read_only, Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedFiles = state.mediaFiles.filter { 
            it.path in viewModel.state.value.selectedFiles 
        }
        dialogHelper.showRenameDialog(selectedFiles)
    }
    
    private fun startSlideshow() {
        val state = viewModel.state.value
        val resource = state.resource
        
        // Try to find lastViewedFile
        val startIndex = if (resource?.lastViewedFile != null) {
            state.mediaFiles.indexOfFirst { it.path == resource.lastViewedFile }
        } else if (state.selectedFiles.isNotEmpty()) {
            state.mediaFiles.indexOfFirst { it.path == state.selectedFiles.first() }
        } else {
            -1
        }
        
        if (startIndex >= 0) {
            val file = state.mediaFiles[startIndex]
            val isDocument = file.type == MediaType.TEXT || 
                            file.type == MediaType.PDF || 
                            file.type == MediaType.EPUB
            
            val resourceId = resource?.id ?: 0L
            val skipCheck = intent.getBooleanExtra(EXTRA_SKIP_AVAILABILITY_CHECK, false)
            val intent = PlayerActivity.createIntent(this, resourceId, startIndex, skipCheck).apply {
                // Start slideshow only for media files (not documents)
                if (!isDocument) {
                    putExtra("slideshow_mode", true)
                }
            }
            startActivity(intent)
        } else if (resource?.lastViewedFile != null) {
            // File was set but not found (deleted or moved)
            Toast.makeText(this, R.string.toast_file_unavailable, Toast.LENGTH_LONG).show()
        } else {
            // No lastViewedFile, no selected files - use first file
            if (state.mediaFiles.isNotEmpty()) {
                val resourceId = resource?.id ?: 0L
                val skipCheck = intent.getBooleanExtra(EXTRA_SKIP_AVAILABILITY_CHECK, false)
                val intent = PlayerActivity.createIntent(this, resourceId, 0, skipCheck).apply {
                    putExtra("slideshow_mode", true)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.toast_no_files_to_play, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showCopyDialog() {
        val state = viewModel.state.value
        val resource = state.resource ?: run {
            Toast.makeText(this, R.string.toast_resource_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }
        fileOperationsManager.showCopyDialog(
            state.selectedFiles.toList(),
            state.mediaFiles,
            resource
        )
    }
    
    private fun showMoveDialog() {
        val state = viewModel.state.value
        val resource = state.resource ?: run {
            Toast.makeText(this, R.string.toast_resource_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (resource.isReadOnly) {
            Toast.makeText(this, R.string.error_read_only, Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val settings = viewModel.getSettings()
            fileOperationsManager.showMoveDialog(
                state.selectedFiles.toList(),
                state.mediaFiles,
                resource,
                settings
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Handle any pending cloud authentication results
        if (::cloudAuthManager.isInitialized) {
            cloudAuthManager.onResume()
        }
        
        // Adapter is no longer cleared in onPause - no need to restore
        // Memory cache (1GB) persists across PlayerActivity navigation
        
        // Skip reload on first onResume - files already loaded in ViewModel.init{}
        if (isFirstResume) {
            isFirstResume = false
            Timber.d("BrowseActivity.onResume: First resume, skipping reload (already loaded in init)")
        } else {
            Timber.d("BrowseActivity.onResume: Returned to BrowseActivity, checking for changes")
            // Check if resource settings changed (supportedMediaTypes, scanSubfolders)
            // If changed, reloads files automatically. If not, syncs with PlayerActivity cache.
            viewModel.checkAndReloadIfResourceChanged()
        }
        
        // Clear expired undo operations (older than 5 minutes)
        viewModel.clearExpiredUndoOperation()
        
        // Scroll restoration moved to submitList callback in state observer
        // This ensures RecyclerView adapter has updated before scrolling
        Timber.d("BrowseActivity.onResume: shouldScrollToLastViewed=$shouldScrollToLastViewed (will restore in submitList callback)")
        
        // Start MediaStore observer for local resources
        startMediaStoreObserver()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop MediaStore observer to avoid unnecessary updates
        stopMediaStoreObserver()
        
        // Save scroll position when leaving Browse (back button, home, etc.)
        stateManager.saveScrollPosition()
        
        // Set flag to restore scroll position on next resume (return from PlayerActivity)
        shouldScrollToLastViewed = true
        
        // Cancel background thumbnail loading to free bandwidth for PlayerActivity
        Timber.d("BrowseActivity.onPause: Cancelling background thumbnail operations")
        viewModel.cancelBackgroundThumbnailLoading()
        
        // Note: Adapter is NO LONGER cleared in onPause to preserve memory cache
        // Thumbnails persist across PlayerActivity navigation for instant reloading
        // Memory cache (up to 1GB) survives until BrowseActivity exits (onDestroy)
    }
    
    private fun startMediaStoreObserver() {
        if (!::mediaStoreObserver.isInitialized) {
            Timber.w("MediaStoreObserver not initialized, skipping start")
            return
        }
        val resource = viewModel.state.value.resource
        mediaStoreObserver.start(resource?.type)
    }
    
    private fun stopMediaStoreObserver() {
        if (!::mediaStoreObserver.isInitialized) {
            return
        }
        mediaStoreObserver.stop()
    }
    
    private fun saveLastViewedFile() {
        stateManager.saveLastViewedFile()
    }
    
    private fun shareSelectedFiles() {
        val state = viewModel.state.value
        val resource = state.resource ?: return
        val selectedFiles = state.mediaFiles.filter { it.path in state.selectedFiles }
        
        fileOperationsManager.shareSelectedFiles(selectedFiles, resource)
    }
    
    private fun showCloudAuthenticationDialog(errorMessage: String) {
        val resourceName = viewModel.state.value.resource?.name ?: "Cloud resource"
        dialogHelper.showCloudAuthenticationDialog(errorMessage, resourceName)
    }
    
    private fun launchGoogleSignIn() {
        cloudAuthManager.launchGoogleSignIn()
    }
    
    private fun handleGoogleSignInResult(data: Intent?) {
        cloudAuthManager.handleGoogleSignInResult(data)
    }
    
    /**
     * Update scroll buttons visibility based on file count
     * Buttons are visible only when there are more than 20 files
     */
    private fun updateScrollButtonsVisibility(fileCount: Int) {
        val shouldShow = fileCount > 20
        binding.fabScrollToTop.isVisible = shouldShow
        binding.fabScrollToBottom.isVisible = shouldShow
        Timber.d("Scroll buttons visibility: $shouldShow (fileCount=$fileCount)")
    }

    companion object {
        const val EXTRA_RESOURCE_ID = "resourceId"
        const val EXTRA_SKIP_AVAILABILITY_CHECK = "skipAvailabilityCheck"

        fun createIntent(context: Context, resourceId: Long, skipAvailabilityCheck: Boolean = false): Intent {
            return Intent(context, BrowseActivity::class.java).apply {
                putExtra(EXTRA_RESOURCE_ID, resourceId)
                putExtra(EXTRA_SKIP_AVAILABILITY_CHECK, skipAvailabilityCheck)
            }
        }
    }
}
