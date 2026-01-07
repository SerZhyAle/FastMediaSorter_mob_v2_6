package com.sza.fastmediasorter.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import com.sza.fastmediasorter.ui.resource.AddResourceActivity
import com.sza.fastmediasorter.ui.resource.EditResourceActivity
import com.sza.fastmediasorter.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Activity - displays list of resources.
 * Entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var resourceAdapter: ResourceAdapter

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeUiState()
        observeEvents()
        
        // Handle shortcut actions
        handleShortcutAction(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShortcutAction(it) }
    }
    
    private fun handleShortcutAction(intent: Intent) {
        when (intent.getStringExtra("action")) {
            "add_resource" -> {
                Timber.d("Shortcut: add_resource")
                viewModel.onAddResourceClick()
            }
            "favorites" -> {
                Timber.d("Shortcut: favorites")
                viewModel.onFavoritesClick()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_favorites -> {
                    viewModel.onFavoritesClick()
                    true
                }
                R.id.action_settings -> {
                    viewModel.onSettingsClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        resourceAdapter = ResourceAdapter(
            onItemClick = { resource -> viewModel.onResourceClick(resource) },
            onItemLongClick = { resource -> viewModel.onResourceLongClick(resource) },
            onMoreClick = { resource -> viewModel.onResourceMoreClick(resource) }
        )

        binding.recyclerViewResources.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = resourceAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAddResource.setOnClickListener {
            viewModel.onAddResourceClick()
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

    private fun updateUi(state: MainUiState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Empty state
        binding.emptyStateLayout.visibility = if (state.showEmptyState) View.VISIBLE else View.GONE

        // RecyclerView
        binding.recyclerViewResources.visibility = 
            if (!state.isLoading && !state.showEmptyState) View.VISIBLE else View.GONE

        // Update adapter
        resourceAdapter.submitList(state.resources)

        // Error state
        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry) { viewModel.refresh() }
                .show()
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

    private fun handleEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is MainUiEvent.NavigateToBrowse -> {
                Timber.d("Navigate to browse: ${event.resourceId}")
                val intent = Intent(this, BrowseActivity::class.java).apply {
                    putExtra(BrowseActivity.EXTRA_RESOURCE_ID, event.resourceId)
                }
                startActivity(intent)
            }
            is MainUiEvent.NavigateToAddResource -> {
                Timber.d("Navigate to add resource")
                startActivity(Intent(this, AddResourceActivity::class.java))
            }
            is MainUiEvent.NavigateToEditResource -> {
                Timber.d("Navigate to edit resource: ${event.resourceId}")
                startActivity(EditResourceActivity.createIntent(this, event.resourceId))
            }
            is MainUiEvent.NavigateToSettings -> {
                Timber.d("Navigate to settings")
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            is MainUiEvent.NavigateToFavorites -> {
                Timber.d("Navigate to favorites")
                startActivity(com.sza.fastmediasorter.ui.favorites.FavoritesActivity.createIntent(this))
            }
        }
    }
}
