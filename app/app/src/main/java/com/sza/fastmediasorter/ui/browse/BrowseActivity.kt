package com.sza.fastmediasorter.ui.browse

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityBrowseBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.dialog.FileInfoDialog
import com.sza.fastmediasorter.ui.player.PlayerActivity
import com.sza.fastmediasorter.util.ShortcutHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity for browsing media files within a resource.
 * Displays files in a grid or list view with multi-select support.
 */
@AndroidEntryPoint
class BrowseActivity : BaseActivity<ActivityBrowseBinding>() {

    companion object {
        const val EXTRA_RESOURCE_ID = "EXTRA_RESOURCE_ID"
    }

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: MediaFileAdapter

    override fun getViewBinding() = ActivityBrowseBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSelectionToolbar()
        setupBackPressHandler()
        observeUiState()
        observeEvents()

        // Load files if resource ID passed via intent (for non-SavedStateHandle use)
        val resourceId = intent.getLongExtra(EXTRA_RESOURCE_ID, -1)
        if (resourceId != -1L) {
            viewModel.loadFiles(resourceId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            viewModel.onBackPressed()
        }

        // Setup SearchView
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.apply {
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.onSearchQueryChanged(newText ?: "")
                    return true
                }
            })
            setOnCloseListener {
                viewModel.clearSearch()
                false
            }
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sort -> {
                    viewModel.onSortClick()
                    true
                }
                R.id.action_view_mode -> {
                    viewModel.onViewModeClick()
                    true
                }
                R.id.action_select_all -> {
                    viewModel.onSelectAllClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MediaFileAdapter(
            onItemClick = { mediaFile -> viewModel.onFileClick(mediaFile) },
            onItemLongClick = { mediaFile -> viewModel.onFileLongClick(mediaFile) }
        )

        binding.fileList.apply {
            layoutManager = GridLayoutManager(this@BrowseActivity, 3)
            adapter = this@BrowseActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSelectionToolbar() {
        binding.btnInfo.setOnClickListener { viewModel.onInfoClick() }
        binding.btnMove.setOnClickListener { viewModel.onMoveClick() }
        binding.btnCopy.setOnClickListener { viewModel.onCopyClick() }
        binding.btnDelete.setOnClickListener { viewModel.onDeleteClick() }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onBackPressed()
            }
        })
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

    private fun updateUi(state: BrowseUiState) {
        // Update toolbar title based on selection mode
        if (state.isSelectionMode) {
            binding.toolbar.title = getString(R.string.selected_count, state.selectedCount)
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        } else {
            binding.toolbar.title = state.resourceName.ifEmpty { getString(R.string.files) }
            binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        }

        // Selection toolbar visibility
        binding.selectionBottomBar.visibility = if (state.isSelectionMode) View.VISIBLE else View.GONE
        binding.selectionCount.text = getString(R.string.selected_count, state.selectedCount)
        
        // Info button only enabled when exactly one file selected
        binding.btnInfo.isEnabled = state.selectedCount == 1
        binding.btnInfo.alpha = if (state.selectedCount == 1) 1.0f else 0.5f

        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Empty state
        binding.emptyStateLayout.visibility = if (state.showEmptyState) View.VISIBLE else View.GONE

        // RecyclerView
        binding.fileList.visibility = 
            if (!state.isLoading && !state.showEmptyState) View.VISIBLE else View.GONE

        // Update layout manager based on view mode
        val layoutManager = if (state.isGridView) {
            GridLayoutManager(this, 3)
        } else {
            LinearLayoutManager(this)
        }
        if (binding.fileList.layoutManager?.javaClass != layoutManager.javaClass) {
            binding.fileList.layoutManager = layoutManager
        }

        // Update adapter with selection state and displayed files (filtered if search active)
        adapter.setSelectionMode(state.isSelectionMode, state.selectedFiles)
        adapter.submitList(state.displayedFiles)
        
        // Show "no search results" message when search is active but no matches
        if (state.showNoSearchResults) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.fileList.visibility = View.GONE
        }

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

    private fun handleEvent(event: BrowseUiEvent) {
        when (event) {
            is BrowseUiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is BrowseUiEvent.ShowUndoSnackbar -> {
                showUndoSnackbar(event.message, event.deletedCount)
            }
            is BrowseUiEvent.NavigateToPlayer -> {
                Timber.d("Navigate to player: ${event.filePath}")
                val intent = PlayerActivity.createIntent(
                    context = this,
                    filePaths = event.files,
                    currentIndex = event.currentIndex
                )
                startActivity(intent)
            }
            is BrowseUiEvent.NavigateBack -> {
                finish()
            }
            is BrowseUiEvent.ShowDestinationPicker -> {
                showDestinationPicker(event.selectedFiles, event.isMove)
            }
            is BrowseUiEvent.ShowDeleteConfirmation -> {
                showDeleteConfirmationDialog(event.count)
            }
            is BrowseUiEvent.ShowSortDialog -> {
                showSortDialog(event.currentSortMode)
            }
            is BrowseUiEvent.ShowFileInfo -> {
                showFileInfoDialog(event.filePath)
            }
            is BrowseUiEvent.RecordResourceVisit -> {
                // Record resource visit for dynamic shortcuts
                ShortcutHelper.recordResourceVisit(this, event.resource)
            }
        }
    }

    private fun showFileInfoDialog(filePath: String) {
        val dialog = FileInfoDialog(this, filePath)
        dialog.show()
    }

    private fun showUndoSnackbar(message: String, deletedCount: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewModel.undoRecentDeletes(deletedCount)
            }
            .show()
    }

    private fun showSortDialog(currentSortMode: com.sza.fastmediasorter.domain.model.SortMode) {
        val dialog = SortOptionsDialog.newInstance(currentSortMode)
        dialog.onSortModeSelected = { sortMode ->
            viewModel.onSortModeSelected(sortMode)
        }
        dialog.show(supportFragmentManager, SortOptionsDialog.TAG)
    }

    private fun showDestinationPicker(selectedFiles: List<String>, isMove: Boolean) {
        val dialog = DestinationPickerDialog.newInstance(selectedFiles, isMove)
        dialog.onDestinationSelected = { resource ->
            viewModel.executeFileOperation(selectedFiles, resource.path, isMove)
        }
        dialog.show(supportFragmentManager, DestinationPickerDialog.TAG)
    }

    private fun showDeleteConfirmationDialog(count: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(getString(R.string.delete_confirmation_message, count))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.confirmDelete()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
