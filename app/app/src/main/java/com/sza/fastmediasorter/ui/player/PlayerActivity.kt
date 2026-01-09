package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.dialog.OcrTranslationDialog
import com.sza.fastmediasorter.ui.dialog.PdfToolsDialog
import com.sza.fastmediasorter.ui.dialog.TranslationSettingsDialog
import com.sza.fastmediasorter.integrations.GoogleLensHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for displaying and navigating through media files.
 * Uses ViewPager2 for horizontal swiping between files.
 *
 * Supports:
 * - Image viewing with Glide
 * - Video playback with ExoPlayer
 * - Audio playback (future)
 */
@AndroidEntryPoint
class PlayerActivity : BaseActivity<ActivityPlayerUnifiedBinding>() {

    companion object {
        private const val EXTRA_FILE_PATHS = "EXTRA_FILE_PATHS"
        private const val EXTRA_CURRENT_INDEX = "EXTRA_CURRENT_INDEX"

        /**
         * Creates an Intent to start PlayerActivity with given files.
         *
         * @param context Source context
         * @param filePaths List of file paths to display
         * @param currentIndex Starting index in the list
         */
        fun createIntent(
            context: Context,
            filePaths: List<String>,
            currentIndex: Int
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_FILE_PATHS, ArrayList(filePaths))
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            }
        }
    }

    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var pagerAdapter: MediaPagerAdapter
    private lateinit var keyboardHandler: PlayerKeyboardHandler
    private lateinit var audioManager: AudioManager

    @Inject
    lateinit var videoPlayerManager: VideoPlayerManager

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    @Inject
    lateinit var googleLensHelper: GoogleLensHelper

    @Inject
    lateinit var epubReaderManager: com.sza.fastmediasorter.epub.EpubReaderManager

    override fun getViewBinding() = ActivityPlayerUnifiedBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable fullscreen mode for player only
        setupFullscreen()

        // Initialize audio manager for volume control
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Initialize keyboard handler
        setupKeyboardHandler()

        initializeVideoPlayer()
        setupViewPager()
        setupCommandPanel()
        setupControls()
        setupTouchZones()
        observeUiState()
        observeEvents()

        // Load files from intent
        if (savedInstanceState == null) {
            val filePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS) ?: emptyList()
            val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
            viewModel.loadFiles(filePaths, currentIndex)
        }
    }

    private fun initializeVideoPlayer() {
        videoPlayerManager.initialize(this)
        audioPlayerManager.initialize(this)
    }

    /**
     * Set up the keyboard handler for hardware keyboard support.
     */
    private fun setupKeyboardHandler() {
        keyboardHandler = PlayerKeyboardHandler(object : PlayerKeyboardHandler.KeyboardActionListener {
            override fun getCurrentMediaType(): MediaType? = viewModel.uiState.value.currentMediaType

            // Navigation
            override fun onNavigatePrevious() = navigateToPrevious()
            override fun onNavigateNext() = navigateToNext()
            override fun onNavigateFirst() {
                binding.viewPager.setCurrentItem(0, true)
            }
            override fun onNavigateLast() {
                binding.viewPager.setCurrentItem(pagerAdapter.itemCount - 1, true)
            }

            // Playback control
            override fun onPlayPause() {
                if (videoPlayerManager.isPlaying()) {
                    videoPlayerManager.pause()
                } else {
                    videoPlayerManager.play()
                }
            }
            override fun onVolumeUp() {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            override fun onVolumeDown() {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            override fun onMute() {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_TOGGLE_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            override fun onSeekToPercent(percent: Int) {
                videoPlayerManager.seekToPercent(percent)
            }
            override fun onSeekStart() {
                videoPlayerManager.seekTo(0)
            }
            override fun onSeekEnd() {
                videoPlayerManager.getDuration()?.let { duration ->
                    videoPlayerManager.seekTo(duration - 1000) // 1 second before end
                }
            }

            // Image controls
            override fun onRotateImage() {
                viewModel.onRotateClick()
            }
            override fun onBrightnessUp() {
                // Brightness control could be implemented here
                Snackbar.make(binding.root, R.string.brightness_up, Snackbar.LENGTH_SHORT).show()
            }
            override fun onBrightnessDown() {
                // Brightness control could be implemented here
                Snackbar.make(binding.root, R.string.brightness_down, Snackbar.LENGTH_SHORT).show()
            }

            // Slideshow
            override fun onToggleSlideshow() {
                viewModel.toggleSlideshow()
            }
            override fun onStartSlideshow() {
                viewModel.onSlideshowClick()
            }

            // File operations
            override fun onShowRenameDialog() {
                viewModel.onRenameClick()
            }
            override fun onDeleteFile() {
                viewModel.onDeleteClick()
            }
            override fun onShowFileInfo() {
                viewModel.onInfoClick()
            }
            override fun onCopyToFirstDestination() {
                viewModel.onCopyToFirstDestination()
            }
            override fun onMoveToFirstDestination() {
                viewModel.onMoveToFirstDestination()
            }

            // UI control
            override fun onToggleFullscreen() {
                viewModel.toggleFullscreen()
            }
            override fun onExitPlayer() {
                finish()
            }
            override fun onCloseOverlay(): Boolean {
                // Try to close any visible overlay
                return viewModel.onBackPressed()
            }

            // PDF/EPUB navigation
            override fun onPreviousPage() {
                viewModel.onPreviousPage()
            }
            override fun onNextPage() {
                viewModel.onNextPage()
            }
            override fun onFirstPage() {
                viewModel.onFirstPage()
            }
            override fun onLastPage() {
                viewModel.onLastPage()
            }

            // Search and text
            override fun onShowSearch() {
                viewModel.onSearchTextClick()
            }
            override fun onCopyText() {
                viewModel.onCopyTextClick()
            }
            override fun onSaveText() {
                viewModel.onSaveTextClick()
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && keyboardHandler.onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun navigateToPrevious() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem > 0) {
            binding.viewPager.currentItem = currentItem - 1
        }
    }

    private fun navigateToNext() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem < pagerAdapter.itemCount - 1) {
            binding.viewPager.currentItem = currentItem + 1
        }
    }

    /**
     * Enable fullscreen edge-to-edge mode for the player.
     * This makes the content extend behind system bars.
     */
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupViewPager() {
        pagerAdapter = MediaPagerAdapter(
            onMediaClick = { viewModel.onMediaClick() },
            onMediaLongClick = { viewModel.onMediaLongClick() },
            videoPlayerManager = videoPlayerManager,
            audioPlayerManager = audioPlayerManager,
            onPreviousClick = { navigateToPrevious() },
            onNextClick = { navigateToNext() },
            epubReaderManager = epubReaderManager
        )

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.onPageSelected(position)
                    // Notify adapter about page change to manage video playback
                    pagerAdapter.onPageSelected(position)
                    // Reset slideshow timer on manual navigation
                    viewModel.resetSlideshowTimer()
                }
            })
        }
    }

    /**
     * Set up the top command panel with all action buttons.
     * Button visibility is controlled based on current media type.
     */
    private fun setupCommandPanel() {
        with(binding) {
            // Universal buttons
            btnBack.setOnClickListener { finish() }

            // Text file buttons
            btnSearchTextCmd.setOnClickListener { viewModel.onSearchTextClick() }
            btnTranslateTextCmd.setOnClickListener { viewModel.onTranslateClick() }
            btnTextSettingsCmd.setOnClickListener { viewModel.onTextSettingsClick() }

            // PDF buttons
            btnSearchPdfCmd.setOnClickListener { viewModel.onSearchPdfClick() }
            btnEditPdf.setOnClickListener { viewModel.onEditPdfClick() }
            btnTranslatePdfCmd.setOnClickListener { viewModel.onTranslateClick() }
            btnPdfTextSettingsCmd.setOnClickListener { viewModel.onTextSettingsClick() }
            btnOcrPdfCmd.setOnClickListener { viewModel.onOcrClick() }
            btnGoogleLensPdfCmd.setOnClickListener { viewModel.onGoogleLensClick() }

            // EPUB buttons
            btnSearchEpubCmd.setOnClickListener { viewModel.onSearchEpubClick() }
            btnTranslateEpubCmd.setOnClickListener { viewModel.onTranslateClick() }
            btnEpubTextSettingsCmd.setOnClickListener { viewModel.onTextSettingsClick() }
            btnOcrEpubCmd.setOnClickListener { viewModel.onOcrClick() }

            // Image buttons
            btnTranslateImageCmd.setOnClickListener { viewModel.onTranslateClick() }
            btnImageTextSettingsCmd.setOnClickListener { viewModel.onTextSettingsClick() }
            btnOcrImageCmd.setOnClickListener { viewModel.onOcrClick() }
            btnGoogleLensImageCmd.setOnClickListener { viewModel.onGoogleLensClick() }

            // Audio button
            btnLyricsCmd.setOnClickListener { viewModel.onLyricsClick() }

            // Common file operation buttons
            btnRenameCmd.setOnClickListener { viewModel.onRenameClick() }
            btnEditCmd.setOnClickListener { viewModel.onEditClick() }
            btnCopyTextCmd.setOnClickListener { viewModel.onCopyTextClick() }
            btnEditTextCmd.setOnClickListener { viewModel.onEditTextClick() }
            btnUndoCmd.setOnClickListener { viewModel.onUndoClick() }

            // Right-side buttons
            btnDeleteCmd.setOnClickListener { viewModel.onDeleteClick() }
            btnFavorite.setOnClickListener { viewModel.onFavoriteClick() }
            btnShareCmd.setOnClickListener { viewModel.onShareClick() }
            btnInfoCmd.setOnClickListener { viewModel.onInfoClick() }
            btnFullscreenCmd.setOnClickListener { viewModel.toggleFullscreen() }
            btnSlideshowCmd.setOnClickListener { viewModel.onSlideshowClick() }
            btnPreviousCmd.setOnClickListener { navigateToPrevious() }
            btnNextCmd.setOnClickListener { navigateToNext() }

            // Overflow menu for portrait mode
            btnOverflowMenu.setOnClickListener { showOverflowMenu() }
        }
        
        // Update overflow menu visibility based on orientation
        updateOverflowMenuVisibility()
    }

    /**
     * Show overflow menu with actions that don't fit in portrait mode.
     */
    private fun showOverflowMenu() {
        val popup = PopupMenu(this, binding.btnOverflowMenu)
        popup.menuInflater.inflate(R.menu.menu_player_overflow, popup.menu)

        // Update fullscreen menu item based on state
        val fullscreenItem = popup.menu.findItem(R.id.action_fullscreen)
        fullscreenItem?.setTitle(
            if (viewModel.uiState.value.isFullscreen) R.string.exit_fullscreen else R.string.action_fullscreen
        )

        // Show/hide slideshow based on media type
        val slideshowItem = popup.menu.findItem(R.id.action_slideshow)
        val mediaType = viewModel.uiState.value.currentMediaType
        slideshowItem?.isVisible = mediaType == MediaType.IMAGE || mediaType == MediaType.GIF

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    viewModel.onShareClick()
                    true
                }
                R.id.action_info -> {
                    viewModel.onInfoClick()
                    true
                }
                R.id.action_fullscreen -> {
                    viewModel.toggleFullscreen()
                    true
                }
                R.id.action_slideshow -> {
                    viewModel.onSlideshowClick()
                    true
                }
                R.id.action_previous -> {
                    navigateToPrevious()
                    true
                }
                R.id.action_next -> {
                    navigateToNext()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    /**
     * Update overflow menu visibility based on screen orientation.
     * In portrait mode, show overflow menu and hide some buttons.
     * In landscape mode, show all buttons and hide overflow menu.
     */
    private fun updateOverflowMenuVisibility() {
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        
        with(binding) {
            // Show overflow menu only in portrait
            btnOverflowMenu.isVisible = isPortrait
            
            // Hide these buttons in portrait (they go into overflow menu)
            btnShareCmd.isVisible = !isPortrait
            btnInfoCmd.isVisible = !isPortrait
            btnFullscreenCmd.isVisible = !isPortrait
            // Previous/Next command buttons always visible
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOverflowMenuVisibility()
    }

    /**
     * Update command panel button visibility based on current media type.
     * Also disables edit/delete/rename buttons when file is read-only.
     */
    private fun updateCommandPanelForMediaType(mediaType: MediaType?, isReadOnly: Boolean = false) {
        with(binding) {
            // Hide all type-specific buttons first
            btnSearchTextCmd.isVisible = false
            btnTranslateTextCmd.isVisible = false
            btnTextSettingsCmd.isVisible = false
            btnSearchPdfCmd.isVisible = false
            btnEditPdf.isVisible = false
            btnTranslatePdfCmd.isVisible = false
            btnPdfTextSettingsCmd.isVisible = false
            btnOcrPdfCmd.isVisible = false
            btnGoogleLensPdfCmd.isVisible = false
            btnSearchEpubCmd.isVisible = false
            btnTranslateEpubCmd.isVisible = false
            btnEpubTextSettingsCmd.isVisible = false
            btnOcrEpubCmd.isVisible = false
            btnTranslateImageCmd.isVisible = false
            btnImageTextSettingsCmd.isVisible = false
            btnOcrImageCmd.isVisible = false
            btnGoogleLensImageCmd.isVisible = false
            btnLyricsCmd.isVisible = false
            btnEditCmd.isVisible = false
            btnCopyTextCmd.isVisible = false
            btnEditTextCmd.isVisible = false
            btnSlideshowCmd.isVisible = false

            // Show buttons based on media type
            when (mediaType) {
                MediaType.IMAGE, MediaType.GIF -> {
                    btnTranslateImageCmd.isVisible = true
                    btnImageTextSettingsCmd.isVisible = true
                    btnOcrImageCmd.isVisible = true
                    btnGoogleLensImageCmd.isVisible = true
                    btnEditCmd.isVisible = true
                    btnSlideshowCmd.isVisible = true
                }
                MediaType.VIDEO -> {
                    // Video has minimal buttons, mainly playback controls
                }
                MediaType.AUDIO -> {
                    btnLyricsCmd.isVisible = true
                }
                MediaType.PDF -> {
                    btnSearchPdfCmd.isVisible = true
                    btnEditPdf.isVisible = true
                    btnTranslatePdfCmd.isVisible = true
                    btnPdfTextSettingsCmd.isVisible = true
                    btnOcrPdfCmd.isVisible = true
                    btnGoogleLensPdfCmd.isVisible = true
                    btnCopyTextCmd.isVisible = true
                }
                MediaType.EPUB -> {
                    btnSearchEpubCmd.isVisible = true
                    btnTranslateEpubCmd.isVisible = true
                    btnEpubTextSettingsCmd.isVisible = true
                    btnOcrEpubCmd.isVisible = true
                    btnCopyTextCmd.isVisible = true
                }
                MediaType.TXT -> {
                    btnSearchTextCmd.isVisible = true
                    btnTranslateTextCmd.isVisible = true
                    btnTextSettingsCmd.isVisible = true
                    btnCopyTextCmd.isVisible = true
                    btnEditTextCmd.isVisible = true
                }
                else -> {
                    // Generic file - minimal buttons
                }
            }

            // Disable modification buttons when file is read-only
            val canModify = !isReadOnly
            btnDeleteCmd.isEnabled = canModify
            btnDeleteCmd.alpha = if (canModify) 1.0f else 0.4f
            btnRenameCmd.isEnabled = canModify
            btnRenameCmd.alpha = if (canModify) 1.0f else 0.4f
            btnEditCmd.isEnabled = canModify
            btnEditCmd.alpha = if (canModify) 1.0f else 0.4f
            btnEditPdf.isEnabled = canModify
            btnEditPdf.alpha = if (canModify) 1.0f else 0.4f
            btnEditTextCmd.isEnabled = canModify
            btnEditTextCmd.alpha = if (canModify) 1.0f else 0.4f
        }
    }

    /**
     * Set up the touch zones overlay for gesture navigation.
     */
    private fun setupTouchZones() {
        binding.touchZonesOverlay.zoneTapListener = object : com.sza.fastmediasorter.ui.player.views.TouchZoneOverlayView.OnZoneTapListener {
            override fun onPreviousFile() {
                navigateToPrevious()
            }

            override fun onNextFile() {
                navigateToNext()
            }

            override fun onToggleUi() {
                viewModel.toggleUiVisibility()
            }

            override fun onSeekBackward() {
                videoPlayerManager.seekRelative(-10_000) // Seek back 10 seconds
            }

            override fun onSeekForward() {
                videoPlayerManager.seekRelative(10_000) // Seek forward 10 seconds
            }

            override fun onPlayPause() {
                if (videoPlayerManager.isPlaying()) {
                    videoPlayerManager.pause()
                } else {
                    videoPlayerManager.play()
                }
            }
        }

        // Set initial label visibility (could be controlled by settings)
        binding.touchZonesOverlay.showLabels = false
    }

    override fun onResume() {
        super.onResume()
        // Resume video playback if any
        videoPlayerManager.play()
        // Resume slideshow if active
        viewModel.onSlideshowForeground()
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback when activity goes to background
        videoPlayerManager.pause()
        // Pause slideshow when going to background
        viewModel.onSlideshowBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release player resources
        pagerAdapter.releaseVideo()
        pagerAdapter.releaseAudio()
        videoPlayerManager.release()
        audioPlayerManager.release()
    }

    private fun setupControls() {
        // Previous button
        binding.btnPrevious.setOnClickListener {
            viewModel.onPreviousClick()
        }

        // Next button
        binding.btnNext.setOnClickListener {
            viewModel.onNextClick()
        }

        // Play/Pause button for slideshow
        binding.btnPlayPause.setOnClickListener {
            viewModel.toggleSlideshowPause()
        }

        // Pause/Resume slideshow button in overlay
        binding.btnPauseSlideshow.setOnClickListener {
            viewModel.toggleSlideshowPause()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUi(state)
                }
            }
        }

        // Observe favorites preference
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.preferencesRepository.enableFavorites.collect { enabled ->
                    binding.btnFavorite.isVisible = enabled
                }
            }
        }
    }

    private fun updateUi(state: PlayerUiState) {
        // Update title bar
        binding.tvFileName.text = state.currentFileName
        binding.tvFilePosition.text = if (state.totalCount > 0) {
            "${state.currentIndex + 1} / ${state.totalCount}"
        } else ""

        // Update media list
        if (state.files.isNotEmpty()) {
            pagerAdapter.submitList(state.files)

            // Set current page if different
            if (binding.viewPager.currentItem != state.currentIndex) {
                binding.viewPager.setCurrentItem(state.currentIndex, false)
            }
        }

        // Update navigation buttons visibility
        binding.btnPrevious.visibility = if (state.hasPrevious) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (state.hasNext) View.VISIBLE else View.INVISIBLE

        // Toggle UI visibility
        val uiVisible = state.isUiVisible
        binding.topCommandPanel.visibility = if (uiVisible) View.VISIBLE else View.GONE
        binding.titleBar.visibility = if (uiVisible) View.VISIBLE else View.GONE
        binding.controlsContainer.visibility = if (uiVisible) View.VISIBLE else View.GONE

        // Update fullscreen mode
        toggleSystemUi(uiVisible)

        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Update favorite icon
        updateFavoriteIcon(state.isFavorite)

        // Update command panel buttons based on current media type
        updateCommandPanelForMediaType(state.currentMediaType, state.isCurrentFileReadOnly)

        // Update fullscreen icon
        binding.btnFullscreenCmd.setImageResource(
            if (state.isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )

        // Update slideshow UI
        updateSlideshowUi(state)
    }

    private fun updateSlideshowUi(state: PlayerUiState) {
        // Show/hide slideshow controls overlay
        binding.layoutSlideshowControls.isVisible = state.isSlideshowActive

        if (state.isSlideshowActive) {
            // Update countdown display
            if (state.showSlideshowCountdown) {
                binding.tvSlideshowCountdown.isVisible = true
                binding.tvSlideshowCountdown.text = state.slideshowRemainingSeconds.toString()
            } else {
                binding.tvSlideshowCountdown.isVisible = false
            }

            // Update progress bar
            val progress = if (state.slideshowInterval > 0) {
                ((state.slideshowInterval - state.slideshowRemainingSeconds).toFloat() / state.slideshowInterval * 100).toInt()
            } else 0
            binding.progressSlideshow.progress = progress

            // Update timer text
            if (state.isSlideshowPaused) {
                binding.tvSlideshowTimer.text = getString(R.string.slideshow_paused)
            } else {
                binding.tvSlideshowTimer.text = getString(R.string.slideshow_next_in, state.slideshowRemainingSeconds)
            }

            // Update pause/play button icon
            binding.btnPauseSlideshow.setImageResource(
                if (state.isSlideshowPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
            )

            // Show play/pause button in controls container
            binding.btnPlayPause.isVisible = true
            binding.btnPlayPause.setImageResource(
                if (state.isSlideshowPaused) R.drawable.ic_play_circle else R.drawable.ic_pause_circle
            )

            // Update slideshow button icon in command panel
            binding.btnSlideshowCmd.setImageResource(R.drawable.ic_stop)
        } else {
            // Hide slideshow controls
            binding.btnPlayPause.isVisible = false
            binding.btnSlideshowCmd.setImageResource(R.drawable.ic_slideshow)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        binding.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
    }

    /**
     * Toggle system UI (status bar and navigation bar) visibility.
     * When UI is hidden, the player enters immersive mode.
     */
    private fun toggleSystemUi(showUi: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (showUi) {
            // Show system bars
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        } else {
            // Hide system bars for immersive mode
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: PlayerUiEvent) {
        when (event) {
            is PlayerUiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is PlayerUiEvent.NavigateToPage -> {
                binding.viewPager.setCurrentItem(event.index, true)
            }
            is PlayerUiEvent.ShareFile -> {
                shareFile(event.filePath)
            }
            is PlayerUiEvent.ShowDeleteConfirmation -> {
                showDeleteConfirmationDialog(event.filePath)
            }
            is PlayerUiEvent.ShowFileInfo -> {
                showFileInfoDialog(event.filePath)
            }
            is PlayerUiEvent.ShowContextMenu -> {
                showContextMenu(event.filePath)
            }
            is PlayerUiEvent.ShowRenameDialog -> {
                showRenameDialog(event.filePath)
            }
            is PlayerUiEvent.NavigateBack -> {
                finish()
            }
            // Translation events
            is PlayerUiEvent.ShowTranslationDialog -> {
                showTranslationDialog(event.sourceLanguage, event.targetLanguage)
            }
            is PlayerUiEvent.ShowTranslationResult -> {
                showTranslationResult(event.translatedText)
            }
            is PlayerUiEvent.ShowTranslationProgress -> {
                showTranslationProgress(event.isLoading)
            }
            // OCR events
            is PlayerUiEvent.ShowOcrDialog -> {
                showOcrDialog(event.filePath)
            }
            // PDF events
            is PlayerUiEvent.ShowPdfToolsDialog -> {
                showPdfToolsDialog(event.filePath)
            }
            // Google Lens events
            is PlayerUiEvent.ShareToGoogleLens -> {
                shareToGoogleLens(event.filePath)
            }
            is PlayerUiEvent.ShowGoogleLensNotInstalled -> {
                showGoogleLensNotInstalledDialog()
            }
            // Text editing events
            is PlayerUiEvent.ShowTextEditorDialog -> {
                showTextEditorDialog(event.filePath)
            }
            is PlayerUiEvent.RefreshCurrentFile -> {
                refreshCurrentMediaPage()
            }
            is PlayerUiEvent.CopyToClipboard -> {
                copyTextToClipboard(event.text)
            }
            // Lyrics events
            is PlayerUiEvent.ShowLyricsDialog -> {
                showLyricsDialog(event.filePath, event.artist, event.title)
            }
            // Search events
            is PlayerUiEvent.ShowSearchDialog -> {
                showSearchDialog(event.filePath, event.documentType, event.content)
            }
            is PlayerUiEvent.ScrollToSearchResult -> {
                // Handle scroll to search result if needed
                Timber.d("Scroll to search result at line ${event.lineNumber}")
            }
        }
    }

    private fun showSearchDialog(filePath: String, documentType: String, content: String?) {
        val type = try {
            com.sza.fastmediasorter.ui.dialog.SearchDialog.DocumentType.valueOf(documentType)
        } catch (e: Exception) {
            com.sza.fastmediasorter.ui.dialog.SearchDialog.DocumentType.TEXT
        }
        
        val dialog = com.sza.fastmediasorter.ui.dialog.SearchDialog.newInstance(
            filePath = filePath,
            documentType = type,
            content = content
        )
        
        dialog.setOnResultSelectedListener { result ->
            // Notify the text viewer to scroll to this result
            Timber.d("Search result selected: line ${result.lineNumber}, pos ${result.startPosition}-${result.endPosition}")
            // The text viewer can observe this and highlight/scroll
        }
        
        dialog.show(supportFragmentManager, "search_dialog")
    }

    private fun showRenameDialog(filePath: String) {
        val file = java.io.File(filePath)
        val currentName = file.nameWithoutExtension

        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewName)
        val inputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilNewName)

        // Pre-fill with current name and select all
        editText.setText(currentName)
        editText.selectAll()

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_title)
            .setView(dialogView)
            .setPositiveButton(R.string.action_ok, null) // Set null initially to override later
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newName = editText.text?.toString()?.trim() ?: ""

                // Validate input
                when {
                    newName.isEmpty() -> {
                        inputLayout.error = getString(R.string.rename_error_empty)
                    }
                    newName.contains(Regex("[\\\\/:*?\"<>|]")) -> {
                        inputLayout.error = getString(R.string.rename_error_invalid)
                    }
                    else -> {
                        inputLayout.error = null
                        viewModel.renameFile(filePath, newName)
                        dialog.dismiss()
                    }
                }
            }

            // Focus on the EditText and show keyboard
            editText.requestFocus()
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        dialog.show()
    }

    private fun showContextMenu(filePath: String) {
        val menuItems = arrayOf(
            getString(R.string.share),
            getString(R.string.file_info),
            getString(R.string.action_delete),
            getString(R.string.open_with),
            getString(R.string.action_cancel)
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(java.io.File(filePath).name)
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> shareFile(filePath)  // Share
                    1 -> showFileInfoDialog(filePath)  // File Info
                    2 -> showDeleteConfirmationDialog(filePath)  // Delete
                    3 -> openWith(filePath)  // Open With
                    // Cancel - do nothing
                }
            }
            .show()
    }

    private fun openWith(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val mimeType = contentResolver.getType(uri) ?: "*/*"

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(intent, getString(R.string.open_with))
            startActivity(chooser)
        } catch (e: Exception) {
            Timber.e(e, "Error opening with external app")
            Snackbar.make(binding.root, getString(R.string.error_unknown), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showFileInfoDialog(filePath: String) {
        val dialog = com.sza.fastmediasorter.ui.dialog.FileInfoDialog(this, filePath)
        dialog.show()
    }

    private fun shareFile(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(file.extension.lowercase())

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to share file")
            Snackbar.make(binding.root, R.string.error_sharing_file, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            else -> "*/*"
        }
    }

    private fun showDeleteConfirmationDialog(filePath: String) {
        val fileName = java.io.File(filePath).name

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(getString(R.string.delete_single_file_message, fileName))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.confirmDeleteCurrentFile()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // ===== Translation methods =====

    /**
     * Show the translation settings dialog.
     */
    private fun showTranslationDialog(sourceLanguage: String?, targetLanguage: String?) {
        val dialog = TranslationSettingsDialog.newInstance(sourceLanguage, targetLanguage)
        dialog.setOnLanguagesSelectedListener { source, target ->
            viewModel.performTranslation(source, target)
        }
        dialog.show(supportFragmentManager, TranslationSettingsDialog.TAG)
    }

    /**
     * Show translation result in a dialog.
     */
    private fun showTranslationResult(translatedText: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.translation_complete)
            .setMessage(translatedText)
            .setPositiveButton(R.string.action_ok, null)
            .setNeutralButton(R.string.copy_to_clipboard) { _, _ ->
                copyToClipboard(translatedText)
            }
            .show()
    }

    /**
     * Show/hide translation progress indicator.
     */
    private fun showTranslationProgress(isLoading: Boolean) {
        // Show a simple snackbar for now - could be enhanced with a ProgressDialog
        if (isLoading) {
            Snackbar.make(binding.root, R.string.downloading_translation_model, Snackbar.LENGTH_INDEFINITE)
                .show()
        }
    }

    /**
     * Copy text to clipboard.
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Translation", text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
    }

    // ===== OCR methods =====

    /**
     * Show the OCR dialog with the current file's image.
     */
    private fun showOcrDialog(filePath: String) {
        lifecycleScope.launch {
            try {
                // Load bitmap from file
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    Snackbar.make(binding.root, R.string.error_file_not_found, Snackbar.LENGTH_SHORT).show()
                    return@launch
                }

                // Load bitmap based on file type
                val bitmap = loadBitmapForOcr(file)
                if (bitmap != null) {
                    val dialog = OcrTranslationDialog.newInstance(bitmap)
                    dialog.show(supportFragmentManager, OcrTranslationDialog.TAG)
                } else {
                    Snackbar.make(binding.root, R.string.ocr_load_failed, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to open OCR dialog")
                Snackbar.make(binding.root, R.string.ocr_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Load a bitmap from file for OCR processing.
     */
    private fun loadBitmapForOcr(file: java.io.File): android.graphics.Bitmap? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                // First decode just bounds to check size
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate sample size to reduce memory usage for very large images
            val maxSize = 2048 // Max dimension for OCR
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // Now decode with sample size
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap for OCR")
            null
        }
    }

    // ===== PDF methods =====

    /**
     * Show the PDF tools dialog.
     */
    private fun showPdfToolsDialog(filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                Snackbar.make(binding.root, R.string.error_file_not_found, Snackbar.LENGTH_SHORT).show()
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val dialog = PdfToolsDialog.newInstance(uri)
            dialog.show(supportFragmentManager, PdfToolsDialog.TAG)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open PDF tools dialog")
            Snackbar.make(binding.root, R.string.pdf_load_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    // ===== Google Lens methods =====

    /**
     * Share the current file to Google Lens for visual search.
     */
    private fun shareToGoogleLens(filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                Snackbar.make(binding.root, R.string.error_file_not_found, Snackbar.LENGTH_SHORT).show()
                return
            }

            // Check if Google Lens is available
            if (!googleLensHelper.isGoogleLensAvailable(this)) {
                showGoogleLensNotInstalledDialog()
                return
            }

            // Share to Google Lens
            val success = googleLensHelper.shareToGoogleLens(this, file)
            if (!success) {
                Snackbar.make(binding.root, R.string.google_lens_share_failed, Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to share to Google Lens")
            Snackbar.make(binding.root, R.string.google_lens_share_failed, Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Show dialog when Google Lens is not installed.
     */
    private fun showGoogleLensNotInstalledDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.google_lens_not_installed_title)
            .setMessage(R.string.google_lens_not_installed_message)
            .setPositiveButton(R.string.install) { _, _ ->
                googleLensHelper.openPlayStoreForGoogleLens(this)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show full-screen text editor dialog.
     */
    private fun showTextEditorDialog(filePath: String) {
        val dialog = com.sza.fastmediasorter.ui.dialog.TextEditorDialog.newInstance(filePath)
        dialog.setOnSaveListener { success ->
            if (success) {
                // Refresh the current page to show updated content
                refreshCurrentMediaPage()
            }
        }
        dialog.show(supportFragmentManager, "text_editor")
    }

    /**
     * Refresh the current media page (e.g., after editing a text file).
     */
    private fun refreshCurrentMediaPage() {
        val currentPosition = binding.viewPager.currentItem
        pagerAdapter.notifyItemChanged(currentPosition)
    }

    /**
     * Copy text to system clipboard.
     */
    private fun copyTextToClipboard(text: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, R.string.text_copied, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Show lyrics dialog for audio file.
     */
    private fun showLyricsDialog(filePath: String, artist: String?, title: String?) {
        val dialog = com.sza.fastmediasorter.ui.dialog.LyricsDialog.newInstance(filePath, artist, title)
        dialog.show(supportFragmentManager, "lyrics_dialog")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.onBackPressed()) {
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
