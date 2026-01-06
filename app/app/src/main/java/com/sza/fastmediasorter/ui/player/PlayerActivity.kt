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

/**
 * Activity for displaying and navigating through media files.
 * Uses ViewPager2 for horizontal swiping between files.
 * 
 * Phase 1: Image viewing with Glide
 * Phase 2: Video playback with ExoPlayer (future)
 * Phase 3: Audio playback (future)
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

    override fun getViewBinding() = ActivityPlayerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFullscreen()
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

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupViewPager() {
        pagerAdapter = MediaPagerAdapter(
            onImageClick = { viewModel.onMediaClick() },
            onImageLongClick = { viewModel.onMediaLongClick() }
        )

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.onPageSelected(position)
                }
            })
        }
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
                // TODO: Implement file sharing
                Timber.d("Share file: ${event.filePath}")
            }
            is PlayerUiEvent.ShowDeleteConfirmation -> {
                // TODO: Show delete confirmation dialog
                Timber.d("Delete confirmation for: ${event.filePath}")
            }
            is PlayerUiEvent.ShowFileInfo -> {
                // TODO: Show file info dialog
                Timber.d("Show info for: ${event.filePath}")
            }
            is PlayerUiEvent.NavigateBack -> {
                finish()
            }
        }
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
