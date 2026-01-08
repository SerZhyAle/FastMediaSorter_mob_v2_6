package com.sza.fastmediasorter.ui.player

import com.sza.fastmediasorter.domain.model.MediaType

/**
 * UI state for PlayerActivity.
 */
data class PlayerUiState(
    /** List of file paths being displayed */
    val files: List<String> = emptyList(),

    /** Current file index in the list */
    val currentIndex: Int = 0,

    /** Current file name for display */
    val currentFileName: String = "",

    /** Total number of files */
    val totalCount: Int = 0,

    /** Whether the current file is marked as favorite */
    val isFavorite: Boolean = false,

    /** Whether previous navigation is available */
    val hasPrevious: Boolean = false,

    /** Whether next navigation is available */
    val hasNext: Boolean = false,

    /** Whether toolbar and controls are visible */
    val isUiVisible: Boolean = true,

    /** Whether loading is in progress */
    val isLoading: Boolean = true,

    /** Error message if any */
    val errorMessage: String? = null,

    /** Current media type for command panel button visibility */
    val currentMediaType: MediaType? = null,

    /** Whether fullscreen mode is active */
    val isFullscreen: Boolean = false,

    /** Whether slideshow mode is active */
    val isSlideshowActive: Boolean = false,

    /** Whether current file is from a read-only resource (disables edit, rename, delete) */
    val isCurrentFileReadOnly: Boolean = false
) {
    companion object {
        val Initial = PlayerUiState()
    }
}

/**
 * One-time events from PlayerViewModel.
 */
sealed class PlayerUiEvent {
    data class ShowSnackbar(val message: String) : PlayerUiEvent()
    data class NavigateToPage(val index: Int) : PlayerUiEvent()
    data class ShareFile(val filePath: String) : PlayerUiEvent()
    data class ShowDeleteConfirmation(val filePath: String) : PlayerUiEvent()
    data class ShowFileInfo(val filePath: String) : PlayerUiEvent()
    data class ShowContextMenu(val filePath: String) : PlayerUiEvent()
    data class ShowRenameDialog(val filePath: String) : PlayerUiEvent()
    data object NavigateBack : PlayerUiEvent()
}
