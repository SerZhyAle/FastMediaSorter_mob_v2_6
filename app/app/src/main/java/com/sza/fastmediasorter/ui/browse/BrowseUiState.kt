package com.sza.fastmediasorter.ui.browse

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.SortMode

/**
 * UI State for BrowseActivity.
 */
data class BrowseUiState(
    val isLoading: Boolean = false,
    val files: List<MediaFile> = emptyList(),
    val filteredFiles: List<MediaFile> = emptyList(),
    val resourceName: String = "",
    val currentPath: String = "",
    val showEmptyState: Boolean = false,
    val isGridView: Boolean = true,
    val errorMessage: String? = null,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val searchQuery: String = "",

    // Selection mode state
    val isSelectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet(),

    // Filter state
    val activeFilterCount: Int = 0,
    val filterDescription: String? = null,
    val mediaTypeFilters: Set<MediaType> = emptySet(),

    // Undo support
    val hasUndoStack: Boolean = false,

    // Scan progress
    val scannedCount: Int = 0
) {
    companion object {
        val Initial = BrowseUiState()
    }

    val selectedCount: Int get() = selectedFiles.size
    val allSelected: Boolean get() = selectedFiles.size == displayedFiles.size && displayedFiles.isNotEmpty()

    /**
     * Files to display - applies search filter and media type filters.
     */
    val displayedFiles: List<MediaFile> get() {
        var result = if (searchQuery.isBlank()) files else filteredFiles

        // Apply media type filter if active
        if (mediaTypeFilters.isNotEmpty()) {
            result = result.filter { it.type in mediaTypeFilters }
        }

        return result
    }

    /**
     * True if search is active but no results found.
     */
    val showNoSearchResults: Boolean get() =
        searchQuery.isNotBlank() && filteredFiles.isEmpty() && files.isNotEmpty()

    /**
     * True if media type filter is active.
     */
    val hasActiveFilter: Boolean get() = mediaTypeFilters.isNotEmpty()
}

/**
 * Events emitted by BrowseViewModel for one-time actions.
 */
sealed class BrowseUiEvent {
    data class ShowSnackbar(val message: String) : BrowseUiEvent()
    data class ShowUndoSnackbar(val message: String, val deletedCount: Int) : BrowseUiEvent()
    data class NavigateToPlayer(val filePath: String, val files: List<String>, val currentIndex: Int) : BrowseUiEvent()
    data object NavigateBack : BrowseUiEvent()
    data class ShowDestinationPicker(val selectedFiles: List<String>, val isMove: Boolean) : BrowseUiEvent()
    data class ShowDeleteConfirmation(val count: Int) : BrowseUiEvent()
    data class ShowSortDialog(val currentSortMode: SortMode) : BrowseUiEvent()
    data class ShowFileInfo(val filePath: String) : BrowseUiEvent()
    data class RecordResourceVisit(val resource: Resource) : BrowseUiEvent()
    data class ShowFilterDialog(val currentFilters: Set<MediaType>) : BrowseUiEvent()
}
