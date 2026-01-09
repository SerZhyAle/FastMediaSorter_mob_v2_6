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

    /** Whether slideshow is paused */
    val isSlideshowPaused: Boolean = false,

    /** Slideshow remaining seconds (for countdown display) */
    val slideshowRemainingSeconds: Int = 0,

    /** Whether to show countdown (last 3 seconds) */
    val showSlideshowCountdown: Boolean = false,

    /** Slideshow interval in seconds */
    val slideshowInterval: Int = 10,

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

    // Translation events
    data class ShowTranslationDialog(
        val contentToTranslate: String,
        val sourceLanguage: String? = null,
        val targetLanguage: String? = null
    ) : PlayerUiEvent()
    data class ShowTranslationResult(val translatedText: String) : PlayerUiEvent()
    data class ShowTranslationProgress(val isLoading: Boolean) : PlayerUiEvent()
    
    // Translation overlay events (for images)
    data class ShowTranslationOverlay(
        val filePath: String,
        val sourceLanguage: String? = null,
        val targetLanguage: String = "en",
        val fontSize: Float = 14f
    ) : PlayerUiEvent()

    // OCR events
    data class ShowOcrDialog(val filePath: String) : PlayerUiEvent()

    // PDF events
    data class ShowPdfToolsDialog(val filePath: String) : PlayerUiEvent()

    // Google Lens events
    data class ShareToGoogleLens(val filePath: String) : PlayerUiEvent()
    data object ShowGoogleLensNotInstalled : PlayerUiEvent()

    // Text editing events
    data class ShowTextEditorDialog(val filePath: String) : PlayerUiEvent()
    data object RefreshCurrentFile : PlayerUiEvent()
    data class CopyToClipboard(val text: String) : PlayerUiEvent()

    // Lyrics events
    data class ShowLyricsDialog(
        val filePath: String,
        val artist: String? = null,
        val title: String? = null
    ) : PlayerUiEvent()

    // Search events
    data class ShowSearchDialog(
        val filePath: String,
        val documentType: String, // "TEXT", "PDF", "EPUB"
        val content: String? = null
    ) : PlayerUiEvent()
    data class ScrollToSearchResult(
        val lineNumber: Int,
        val startPosition: Int,
        val endPosition: Int
    ) : PlayerUiEvent()
}
