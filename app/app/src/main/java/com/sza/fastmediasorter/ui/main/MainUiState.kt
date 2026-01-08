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
    val showEmptyState: Boolean = false,
    
    /** Whether grid mode is active for resources */
    val isGridMode: Boolean = false,
    
    /** Active resource tab filter */
    val activeTab: ResourceTab = ResourceTab.ALL,
    
    /** Filter by resource types */
    val filterByType: List<String>? = null,
    
    /** Filter by name */
    val filterByName: String? = null
) {
    companion object {
        val Initial = MainUiState()
    }
}

/**
 * Resource tab filter enum.
 */
enum class ResourceTab {
    ALL,
    LOCAL,
    SMB,
    FTP_SFTP,
    CLOUD
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
    data object NavigateToSearch : MainUiEvent()
    
    // Scan progress events
    data class ScanProgress(val scannedCount: Int, val currentFile: String? = null) : MainUiEvent()
    data object ScanComplete : MainUiEvent()
}
