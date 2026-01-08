package com.sza.fastmediasorter.ui.browse

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        
        setupControlButtons()
        setupRecyclerView()
        setupOperationsPanel()
        setupScrollButtons()
        setupBackPressHandler()
        observeUiState()
        observeEvents()

        // Load files if resource ID passed via intent
        val resourceId = intent.getLongExtra(EXTRA_RESOURCE_ID, -1)
        if (resourceId != -1L) {
            viewModel.loadFiles(resourceId)
        }
    }

    private fun setupControlButtons() {
        with(binding) {
            // Back
            btnBack.setOnClickListener {
                viewModel.onBackPressed()
            }
            
            // Sort
            btnSort.setOnClickListener {
                viewModel.onSortClick()
            }
            
            // Filter
            btnFilter.setOnClickListener {
                viewModel.onFilterClick()
            }
            
            // Refresh
            btnRefresh.setOnClickListener {
                viewModel.refresh()
                Toast.makeText(this@BrowseActivity, R.string.refresh, Toast.LENGTH_SHORT).show()
            }
            
            // Toggle View
            btnToggleView.setOnClickListener {
                viewModel.onViewModeClick()
            }
            
            // Select All
            btnSelectAll.setOnClickListener {
                viewModel.onSelectAllClick()
            }
            
            // Deselect All
            btnDeselectAll.setOnClickListener {
                viewModel.onDeselectAllClick()
            }
            
            // Play
            btnPlay.setOnClickListener {
                viewModel.onPlayClick()
            }
            
            // Error Retry
            btnRetry.setOnClickListener {
                viewModel.refresh()
            }
            
            // Stop scan
            btnStopScan.setOnClickListener {
                viewModel.stopScan()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MediaFileAdapter(
            onItemClick = { mediaFile -> viewModel.onFileClick(mediaFile) },
            onItemLongClick = { mediaFile -> viewModel.onFileLongClick(mediaFile) }
        )

        binding.rvMediaFiles.apply {
            layoutManager = GridLayoutManager(this@BrowseActivity, 3)
            adapter = this@BrowseActivity.adapter
            setHasFixedSize(true)
            
            // Scroll listener for scroll buttons visibility
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateScrollButtonsVisibility()
                }
            })
        }
    }

    private fun setupOperationsPanel() {
        with(binding) {
            btnCopy.setOnClickListener { viewModel.onCopyClick() }
            btnMove.setOnClickListener { viewModel.onMoveClick() }
            btnRename.setOnClickListener { viewModel.onRenameClick() }
            btnDelete.setOnClickListener { viewModel.onDeleteClick() }
            btnUndo.setOnClickListener { viewModel.onUndoClick() }
            btnShare.setOnClickListener { viewModel.onShareClick() }
        }
    }

    private fun setupScrollButtons() {
        binding.fabScrollToTop.setOnClickListener {
            binding.rvMediaFiles.smoothScrollToPosition(0)
        }
        binding.fabScrollToBottom.setOnClickListener {
            val lastPosition = adapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.rvMediaFiles.smoothScrollToPosition(lastPosition)
            }
        }
    }

    private fun updateScrollButtonsVisibility() {
        val layoutManager = binding.rvMediaFiles.layoutManager
        val itemCount = adapter.itemCount
        
        // Only show buttons if more than 20 items
        if (itemCount <= 20) {
            binding.fabScrollToTop.isVisible = false
            binding.fabScrollToBottom.isVisible = false
            return
        }
        
        val firstVisiblePosition = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            else -> 0
        }
        val lastVisiblePosition = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            else -> itemCount - 1
        }
        
        binding.fabScrollToTop.isVisible = firstVisiblePosition > 10
        binding.fabScrollToBottom.isVisible = lastVisiblePosition < itemCount - 10
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
        // Update resource info
        val selectionInfo = if (state.selectedCount > 0) 
            " • ${state.selectedCount} selected" 
        else ""
        binding.tvResourceInfo.text = "${state.resourceName}${selectionInfo}"
        
        // Filter badge
        val filterCount = state.activeFilterCount
        binding.tvFilterBadge.isVisible = filterCount > 0
        if (filterCount > 0) {
            binding.tvFilterBadge.text = filterCount.toString()
        }
        
        // Filter warning
        binding.tvFilterWarning.isVisible = state.filterDescription != null
        state.filterDescription?.let { binding.tvFilterWarning.text = "⚠ $it" }
        
        // Operations panel - show buttons when files selected
        val hasSelection = state.selectedCount > 0
        binding.btnCopy.isVisible = hasSelection
        binding.btnMove.isVisible = hasSelection
        binding.btnRename.isVisible = hasSelection
        binding.btnDelete.isVisible = hasSelection
        binding.btnShare.isVisible = hasSelection
        
        // Undo button
        binding.btnUndo.isVisible = state.hasUndoStack
        
        // Loading/Progress state
        binding.layoutProgress.isVisible = state.isLoading
        if (state.isLoading) {
            binding.tvProgressMessage.text = getString(R.string.loading) + 
                if (state.scannedCount > 0) " (${state.scannedCount})" else ""
            binding.btnStopScan.isVisible = state.scannedCount > 1000
        }
        
        // Empty state
        binding.emptyStateView.isVisible = state.showEmptyState && !state.isLoading
        
        // Error state
        binding.errorStateView.isVisible = state.errorMessage != null && !state.isLoading
        state.errorMessage?.let { binding.tvErrorMessage.text = it }
        
        // RecyclerView visibility
        binding.rvMediaFiles.isVisible = !state.isLoading && !state.showEmptyState && state.errorMessage == null
        
        // Layout manager based on view mode
        updateLayoutManager(state.isGridView)
        
        // Toggle view button icon
        binding.btnToggleView.setImageResource(
            if (state.isGridView) R.drawable.ic_view_list else R.drawable.ic_view_grid
        )

        // Update adapter
        adapter.setSelectionMode(state.isSelectionMode, state.selectedFiles)
        adapter.submitList(state.displayedFiles)
        
        // Update scroll buttons after list update
        updateScrollButtonsVisibility()
    }
    
    private fun updateLayoutManager(isGridView: Boolean) {
        val currentLayoutManager = binding.rvMediaFiles.layoutManager
        val screenWidthDp = resources.configuration.screenWidthDp
        val isWideScreen = screenWidthDp >= 600
        
        if (isGridView) {
            val spanCount = if (isWideScreen) 5 else 3
            if (currentLayoutManager !is GridLayoutManager ||
                (currentLayoutManager as GridLayoutManager).spanCount != spanCount) {
                binding.rvMediaFiles.layoutManager = GridLayoutManager(this, spanCount)
            }
        } else {
            if (currentLayoutManager !is LinearLayoutManager || currentLayoutManager is GridLayoutManager) {
                binding.rvMediaFiles.layoutManager = LinearLayoutManager(this)
            }
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
                ShortcutHelper.recordResourceVisit(this, event.resource)
            }
            is BrowseUiEvent.ShowFilterDialog -> {
                showFilterDialog()
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(getString(R.string.delete_confirmation_message, count))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.confirmDelete()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
    
    private fun showFilterDialog() {
        // TODO: Implement FilterDialog
        Timber.d("Show filter dialog")
    }
}
