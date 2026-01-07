package com.sza.fastmediasorter.ui.favorites

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityFavoritesBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.browse.MediaFileAdapter
import com.sza.fastmediasorter.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity for browsing favorite media files.
 * Displays all favorited files across all resources.
 */
@AndroidEntryPoint
class FavoritesActivity : BaseActivity<ActivityFavoritesBinding>() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, FavoritesActivity::class.java)
        }
    }

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: MediaFileAdapter

    override fun getViewBinding() = ActivityFavoritesBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        observeUiState()
        observeEvents()
        
        viewModel.loadFavorites()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.favorites)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = MediaFileAdapter(
            onItemClick = { file -> 
                Timber.d("File clicked: ${file.name}")
                viewModel.onFileClick(file)
            },
            onItemLongClick = { file ->
                Timber.d("File long clicked: ${file.name}")
                false // No selection mode in favorites for now
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@FavoritesActivity, 3)
            adapter = this@FavoritesActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    Timber.d("UI State update: loading=${state.isLoading}, files=${state.files.size}")
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: FavoritesUiState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Empty state
        binding.emptyView.visibility = if (state.showEmptyState && !state.isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (!state.showEmptyState && !state.isLoading) View.VISIBLE else View.GONE
        
        // Error state
        if (state.errorMessage != null) {
            Snackbar.make(binding.root, state.errorMessage, Snackbar.LENGTH_LONG).show()
        }
        
        // Update file list
        adapter.submitList(state.files)
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

    private fun handleEvent(event: FavoritesUiEvent) {
        when (event) {
            is FavoritesUiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is FavoritesUiEvent.NavigateToPlayer -> {
                Timber.d("Navigate to player: ${event.filePath}")
                val intent = PlayerActivity.createIntent(
                    context = this,
                    filePaths = event.files,
                    currentIndex = event.currentIndex
                )
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites in case user unfavorited something in player
        viewModel.loadFavorites()
    }
}
