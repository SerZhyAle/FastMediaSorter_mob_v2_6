package com.sza.fastmediasorter.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivitySearchBinding
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import com.sza.fastmediasorter.ui.dialog.FileInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity for searching across all resources.
 * Provides global search with filters for file type, size, and date.
 */
@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SearchActivity::class.java)
        }
    }

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter

    override fun getViewBinding() = ActivitySearchBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupSearchView()
        setupRecyclerView()
        setupFilterChips()
        setupFab()
        observeUiState()
        observeEvents()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onQueryChanged(newText ?: "")
                return true
            }
        })

        // Request focus on search view
        binding.searchView.requestFocus()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(
            onItemClick = { result -> viewModel.onResultClick(result) },
            onItemLongClick = { result -> viewModel.onResultLongClick(result) }
        )

        binding.resultsList.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupFilterChips() {
        binding.chipImages.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.onFileTypeToggled(MediaType.IMAGE)
            else viewModel.onFileTypeToggled(MediaType.IMAGE)
        }

        binding.chipVideos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.onFileTypeToggled(MediaType.VIDEO)
            else viewModel.onFileTypeToggled(MediaType.VIDEO)
        }

        binding.chipAudio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.onFileTypeToggled(MediaType.AUDIO)
            else viewModel.onFileTypeToggled(MediaType.AUDIO)
        }

        binding.chipOther.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.onFileTypeToggled(MediaType.OTHER)
            else viewModel.onFileTypeToggled(MediaType.OTHER)
        }
    }

    private fun setupFab() {
        binding.fabFilter.setOnClickListener {
            viewModel.onToggleFilters()
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

    private fun updateUi(state: SearchUiState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Filter chips visibility
        binding.filterChipsContainer.visibility = 
            if (state.showFilters) View.VISIBLE else View.GONE

        // Update chip states
        binding.chipImages.isChecked = MediaType.IMAGE in state.selectedFileTypes
        binding.chipVideos.isChecked = MediaType.VIDEO in state.selectedFileTypes
        binding.chipAudio.isChecked = MediaType.AUDIO in state.selectedFileTypes
        binding.chipOther.isChecked = MediaType.OTHER in state.selectedFileTypes

        // Results
        adapter.submitList(state.results)

        // Show/hide states
        when {
            state.isLoading -> {
                binding.resultsList.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
                binding.initialStateLayout.visibility = View.GONE
            }
            state.query.isBlank() -> {
                binding.resultsList.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
                binding.initialStateLayout.visibility = View.VISIBLE
            }
            state.showEmptyState -> {
                binding.resultsList.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.initialStateLayout.visibility = View.GONE
            }
            state.hasResults -> {
                binding.resultsList.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                binding.initialStateLayout.visibility = View.GONE
            }
        }

        // Error state
        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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

    private fun handleEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is SearchUiEvent.NavigateToFile -> {
                Timber.d("Navigate to file: ${event.filePath} in resource ${event.resourceId}")
                val intent = Intent(this, BrowseActivity::class.java).apply {
                    putExtra(BrowseActivity.EXTRA_RESOURCE_ID, event.resourceId)
                }
                startActivity(intent)
            }
            is SearchUiEvent.ShowFileInfo -> {
                val dialog = FileInfoDialog(this, event.filePath)
                dialog.show()
            }
        }
    }
}
