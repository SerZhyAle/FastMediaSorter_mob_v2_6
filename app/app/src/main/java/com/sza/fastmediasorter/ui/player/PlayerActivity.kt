package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityPlayerBinding
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
class PlayerActivity : BaseActivity<ActivityPlayerBinding>() {

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

    override fun getViewBinding() = ActivityPlayerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFullscreen()
        initializeVideoPlayer()
        setupViewPager()
        setupToolbar()
        setupControls()
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

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_favorite -> {
                    viewModel.onFavoriteClick()
                    true
                }
                R.id.action_share -> {
                    viewModel.onShareClick()
                    true
                }
                R.id.action_delete -> {
                    viewModel.onDeleteClick()
                    true
                }
                R.id.action_info -> {
                    viewModel.onInfoClick()
                    true
                }
                else -> false
            }
        }
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
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: PlayerUiState) {
        // Update toolbar title with current position
        binding.toolbar.title = state.currentFileName
        binding.toolbar.subtitle = if (state.totalCount > 0) {
            "${state.currentIndex + 1} / ${state.totalCount}"
        } else null

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
        binding.toolbar.visibility = if (uiVisible) View.VISIBLE else View.GONE
        binding.controlsContainer.visibility = if (uiVisible) View.VISIBLE else View.GONE

        // Update fullscreen mode
        toggleSystemUi(uiVisible)

        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Update favorite icon
        updateFavoriteIcon(state.isFavorite)
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        val menuItem = binding.toolbar.menu.findItem(R.id.action_favorite)
        menuItem?.setIcon(
            if (isFavorite) R.drawable.ic_favorite_filled
            else R.drawable.ic_favorite_outline
        )
    }

    private fun toggleSystemUi(visible: Boolean) {
        val controller = window.insetsController ?: return
        if (visible) {
            controller.show(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
        } else {
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = 
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            is PlayerUiEvent.NavigateBack -> {
                finish()
            }
        }
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
