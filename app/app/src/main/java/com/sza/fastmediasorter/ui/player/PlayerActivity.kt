package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
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

    @Inject
    lateinit var videoPlayerManager: VideoPlayerManager

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    override fun getViewBinding() = ActivityPlayerUnifiedBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable fullscreen mode for player only
        setupFullscreen()

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
            onNextClick = { navigateToNext() }
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
        }
    }

    /**
     * Update command panel button visibility based on current media type.
     */
    private fun updateCommandPanelForMediaType(mediaType: MediaType?) {
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
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback when activity goes to background
        videoPlayerManager.pause()
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
            viewModel.toggleSlideshow()
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
        updateCommandPanelForMediaType(state.currentMediaType)

        // Update fullscreen icon
        binding.btnFullscreenCmd.setImageResource(
            if (state.isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        binding.btnFavorite.setImageResource(
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
        }
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.onBackPressed()) {
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
