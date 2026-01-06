package com.sza.fastmediasorter.ui.main

import com.sza.fastmediasorter.domain.model.Resource

/**
 * UI State for MainActivity.
 * Follows Unidirectional Data Flow pattern.
 */
data class MainUiState(
    /** List of resources to display */
    val resources: List<Resource> = emptyList(),
    
    /** Whether data is currently loading */
    val isLoading: Boolean = true,
    
    /** Error message to display (null if no error) */
    val errorMessage: String? = null,
    
    /** Whether to show empty state */
    val showEmptyState: Boolean = false
) {
    companion object {
        val Initial = MainUiState()
    }
}

/**
 * One-time events from ViewModel to UI.
 */
sealed class MainUiEvent {
    data class ShowSnackbar(val message: String) : MainUiEvent()
    data class NavigateToBrowse(val resourceId: Long) : MainUiEvent()
    data class NavigateToEditResource(val resourceId: Long) : MainUiEvent()
    data object NavigateToAddResource : MainUiEvent()
    data object NavigateToSettings : MainUiEvent()
    data object NavigateToFavorites : MainUiEvent()
}
