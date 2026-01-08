package com.sza.fastmediasorter.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import com.sza.fastmediasorter.ui.resource.AddResourceActivity
import com.sza.fastmediasorter.ui.resource.EditResourceActivity
import com.sza.fastmediasorter.ui.search.SearchActivity
import com.sza.fastmediasorter.ui.settings.SettingsActivity
import com.sza.fastmediasorter.util.ShortcutHelper
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

        setupControlButtons()
        setupResourceTypeTabs()
        setupRecyclerView()
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

    private fun setupControlButtons() {
        with(binding) {
            // Exit button
            btnExit.setOnClickListener {
                finishAffinity()
            }
            
            // Add resource
            btnAddResource.setOnClickListener {
                viewModel.onAddResourceClick()
            }
            
            // Filter
            btnFilter.setOnClickListener {
                viewModel.onFilterClick()
            }
            
            // Refresh
            btnRefresh.setOnClickListener {
                viewModel.refresh()
                Toast.makeText(this@MainActivity, R.string.resources_refreshed, Toast.LENGTH_SHORT).show()
            }
            
            // Settings
            btnSettings.setOnClickListener {
                viewModel.onSettingsClick()
            }
            
            // Toggle view (grid/list)
            btnToggleView.setOnClickListener {
                viewModel.toggleViewMode()
            }
            
            // Favorites
            btnFavorites.setOnClickListener {
                viewModel.onFavoritesClick()
            }
            
            // Start player
            btnStartPlayer.setOnClickListener {
                viewModel.onStartPlayerClick()
            }
            
            // Empty state click -> add resource
            emptyStateView.setOnClickListener {
                viewModel.onAddResourceClick()
            }
            
            // Error retry
            btnRetry.setOnClickListener {
                viewModel.refresh()
            }
        }
    }

    private fun setupResourceTypeTabs() {
        with(binding.tabResourceTypes) {
            // Add tabs
            addTab(newTab().setText(R.string.tab_all))
            addTab(newTab().setText(R.string.tab_local))
            addTab(newTab().setText(R.string.tab_smb))
            addTab(newTab().setText(R.string.tab_ftp_sftp))
            addTab(newTab().setText(R.string.tab_cloud))
            
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val tabPosition = tab?.position ?: 0
                    val resourceTab = when (tabPosition) {
                        0 -> ResourceTab.ALL
                        1 -> ResourceTab.LOCAL
                        2 -> ResourceTab.SMB
                        3 -> ResourceTab.FTP_SFTP
                        4 -> ResourceTab.CLOUD
                        else -> ResourceTab.ALL
                    }
                    viewModel.setActiveTab(resourceTab)
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupRecyclerView() {
        resourceAdapter = ResourceAdapter(
            onItemClick = { resource -> viewModel.onResourceClick(resource) },
            onItemLongClick = { resource -> viewModel.onResourceLongClick(resource) },
            onMoreClick = { resource -> viewModel.onResourceMoreClick(resource) }
        )

        binding.rvResources.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = resourceAdapter
            setHasFixedSize(true)
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
                    binding.btnFavorites.isVisible = enabled
                }
            }
        }
    }

    private fun updateUi(state: MainUiState) {
        // Loading state
        binding.progressBar.isVisible = state.isLoading

        // Empty state
        val isEmpty = state.resources.isEmpty() && !state.isLoading
        val hasError = state.errorMessage != null
        
        binding.emptyStateView.isVisible = isEmpty && !hasError
        binding.errorStateView.isVisible = isEmpty && hasError
        binding.rvResources.isVisible = !isEmpty

        // Update error message if present
        state.errorMessage?.let { message ->
            binding.tvErrorMessage.text = message
        }

        // Update adapter
        resourceAdapter.submitList(state.resources)
        
        // Toggle view button visibility
        binding.btnToggleView.isVisible = state.resources.size > 10 || state.isGridMode
        
        // Update toggle view icon
        binding.btnToggleView.setImageResource(
            if (state.isGridMode) R.drawable.ic_view_list else R.drawable.ic_view_grid
        )
        
        // Update layout manager based on mode
        updateLayoutManager(state.isGridMode)
        
        // Start player enabled only when resources exist
        binding.btnStartPlayer.isEnabled = state.resources.isNotEmpty()
        
        // Update filter warning
        updateFilterWarning(state)
        
        // Update dynamic shortcuts
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutHelper.updateDynamicShortcuts(this, state.resources)
        }
    }
    
    private fun updateLayoutManager(isGridMode: Boolean) {
        val currentLayoutManager = binding.rvResources.layoutManager
        val screenWidthDp = resources.configuration.screenWidthDp
        val isWideScreen = screenWidthDp >= 600
        
        if (isGridMode) {
            val spanCount = if (isWideScreen) 5 else 3
            if (currentLayoutManager !is GridLayoutManager || 
                (currentLayoutManager as GridLayoutManager).spanCount != spanCount) {
                binding.rvResources.layoutManager = GridLayoutManager(this, spanCount)
            }
        } else {
            val spanCount = if (isWideScreen) 2 else 1
            if (spanCount == 1) {
                if (currentLayoutManager !is LinearLayoutManager || currentLayoutManager is GridLayoutManager) {
                    binding.rvResources.layoutManager = LinearLayoutManager(this)
                }
            } else {
                if (currentLayoutManager !is GridLayoutManager ||
                    (currentLayoutManager as GridLayoutManager).spanCount != spanCount) {
                    binding.rvResources.layoutManager = GridLayoutManager(this, spanCount)
                }
            }
        }
    }
    
    private fun updateFilterWarning(state: MainUiState) {
        val filterParts = mutableListOf<String>()
        
        state.filterByType?.let { types ->
            if (types.isNotEmpty()) {
                filterParts.add("Type: ${types.joinToString(", ")}")
            }
        }
        
        state.filterByName?.let { name ->
            if (name.isNotBlank()) {
                filterParts.add("Name: '$name'")
            }
        }
        
        binding.tvFilterWarning.isVisible = filterParts.isNotEmpty()
        if (filterParts.isNotEmpty()) {
            binding.tvFilterWarning.text = getString(R.string.filter) + ": " + filterParts.joinToString(" | ")
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
            is MainUiEvent.NavigateToSearch -> {
                Timber.d("Navigate to search")
                startActivity(SearchActivity.createIntent(this))
            }
            is MainUiEvent.ScanProgress -> {
                binding.scanProgressLayout.isVisible = true
                binding.tvScanProgress.text = getString(R.string.scanning_resources)
                binding.tvScanDetail.text = getString(R.string.files_scanned_count, event.scannedCount)
            }
            is MainUiEvent.ScanComplete -> {
                binding.scanProgressLayout.isVisible = false
            }
        }
    }
}
