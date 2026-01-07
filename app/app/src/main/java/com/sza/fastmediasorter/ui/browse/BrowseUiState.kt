package com.sza.fastmediasorter.ui.browse

import com.sza.fastmediasorter.domain.model.MediaFile

/**
 * UI State for BrowseActivity.
 */
data class BrowseUiState(
    val isLoading: Boolean = false,
    val files: List<MediaFile> = emptyList(),
    val resourceName: String = "",
    val currentPath: String = "",
    val showEmptyState: Boolean = false,
    val isGridView: Boolean = true,
    val errorMessage: String? = null,
    
    // Selection mode state
    val isSelectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet()
) {
    companion object {
        val Initial = BrowseUiState()
    }

    val selectedCount: Int get() = selectedFiles.size
    val allSelected: Boolean get() = selectedFiles.size == files.size && files.isNotEmpty()
}

/**
 * Events emitted by BrowseViewModel for one-time actions.
 */
sealed class BrowseUiEvent {
    data class ShowSnackbar(val message: String) : BrowseUiEvent()
    data class NavigateToPlayer(val filePath: String, val files: List<String>, val currentIndex: Int) : BrowseUiEvent()
    data object NavigateBack : BrowseUiEvent()
    data class ShowDestinationPicker(val selectedFiles: List<String>) : BrowseUiEvent()
    data class ShowDeleteConfirmation(val count: Int) : BrowseUiEvent()
    data object ShowSortDialog : BrowseUiEvent()
}
