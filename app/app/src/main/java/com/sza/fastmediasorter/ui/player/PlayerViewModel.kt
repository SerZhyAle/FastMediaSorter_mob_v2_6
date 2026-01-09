package com.sza.fastmediasorter.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.operation.TrashManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.translation.TranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for PlayerActivity.
 * Manages media viewing state and user interactions.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val fileMetadataRepository: FileMetadataRepository,
    private val trashManager: TrashManager,
    val preferencesRepository: PreferencesRepository,
    private val translationManager: TranslationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState.Initial)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlayerUiEvent>()
    val events = _events.asSharedFlow()

    // Translation session settings
    private var lastSourceLanguage: String? = null
    private var lastTargetLanguage: String = "en" // Default to English
    private var pendingTranslationContent: String? = null

    // Slideshow controller
    private val slideshowController = SlideshowController(
        onAdvance = { advanceSlideshow() },
        onCountdownTick = { remaining -> onSlideshowCountdownTick(remaining) }
    )

    init {
        // Observe slideshow state changes
        viewModelScope.launch {
            slideshowController.state.collect { slideshowState ->
                _uiState.update {
                    it.copy(
                        isSlideshowActive = slideshowState.isActive,
                        isSlideshowPaused = slideshowState.isPaused,
                        slideshowRemainingSeconds = slideshowState.remainingSeconds,
                        showSlideshowCountdown = slideshowState.showCountdown,
                        slideshowInterval = slideshowState.intervalSeconds
                    )
                }
            }
        }
    }

    /**
     * Loads file list and sets initial position.
     */
    fun loadFiles(filePaths: List<String>, startIndex: Int) {
        if (filePaths.isEmpty()) {
            Timber.w("Empty file list provided")
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.NavigateBack)
            }
            return
        }

        val safeIndex = startIndex.coerceIn(0, filePaths.size - 1)
        val currentFile = File(filePaths[safeIndex])
        val mediaType = getMediaType(currentFile.extension)

        _uiState.update {
            it.copy(
                files = filePaths,
                currentIndex = safeIndex,
                currentFileName = currentFile.name,
                totalCount = filePaths.size,
                hasPrevious = safeIndex > 0,
                hasNext = safeIndex < filePaths.size - 1,
                isLoading = false,
                currentMediaType = mediaType
            )
        }

        // Load favorite status for initial file
        loadFavoriteStatus(filePaths[safeIndex])

        Timber.d("Loaded ${filePaths.size} files, starting at index $safeIndex, type: $mediaType")
    }

    /**
     * Called when user swipes to a new page.
     */
    fun onPageSelected(position: Int) {
        val files = _uiState.value.files
        if (position !in files.indices) return

        val currentFile = File(files[position])
        val mediaType = getMediaType(currentFile.extension)

        _uiState.update {
            it.copy(
                currentIndex = position,
                currentFileName = currentFile.name,
                hasPrevious = position > 0,
                hasNext = position < files.size - 1,
                isFavorite = false, // Reset until loaded
                currentMediaType = mediaType
            )
        }

        // Load favorite status for new file
        loadFavoriteStatus(files[position])

        Timber.d("Page selected: $position - ${currentFile.name}, type: $mediaType")
    }

    private fun loadFavoriteStatus(filePath: String) {
        viewModelScope.launch {
            try {
                val isFavorite = fileMetadataRepository.isFavorite(filePath)
                _uiState.update { it.copy(isFavorite = isFavorite) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load favorite status")
            }
        }
    }

    /**
     * Navigates to previous file.
     */
    fun onPreviousClick() {
        val currentIndex = _uiState.value.currentIndex
        if (currentIndex > 0) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.NavigateToPage(currentIndex - 1))
            }
        }
    }

    /**
     * Navigates to next file.
     */
    fun onNextClick() {
        val state = _uiState.value
        if (state.currentIndex < state.files.size - 1) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.NavigateToPage(state.currentIndex + 1))
            }
        }
    }

    /**
     * Toggles UI visibility when media is tapped.
     */
    fun onMediaClick() {
        _uiState.update {
            it.copy(isUiVisible = !it.isUiVisible)
        }
    }

    /**
     * Toggles UI visibility (controls, toolbar).
     * Alias for onMediaClick for clearer usage from touch zones.
     */
    fun toggleUiVisibility() {
        onMediaClick()
    }

    /**
     * Handles long click on media (shows context menu).
     */
    fun onMediaLongClick(): Boolean {
        Timber.d("Media long clicked")
        val state = _uiState.value
        if (state.files.isNotEmpty() && state.currentIndex in state.files.indices) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowContextMenu(state.files[state.currentIndex]))
            }
        }
        return true
    }

    /**
     * Toggles favorite status for current file.
     */
    fun onFavoriteClick() {
        val state = _uiState.value
        val currentPath = state.files.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            try {
                // Toggle in database (resourceId 0 for now, will be updated when known)
                val newFavoriteState = fileMetadataRepository.toggleFavorite(currentPath, 0)

                _uiState.update {
                    it.copy(isFavorite = newFavoriteState)
                }

                val message = if (newFavoriteState) "Added to favorites" else "Removed from favorites"
                _events.emit(PlayerUiEvent.ShowSnackbar(message))
                Timber.d("Favorite toggled: $newFavoriteState for $currentPath")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
                _events.emit(PlayerUiEvent.ShowSnackbar("Failed to update favorite"))
            }
        }
    }

    /**
     * Initiates file sharing.
     */
    fun onShareClick() {
        val state = _uiState.value
        if (state.files.isNotEmpty() && state.currentIndex in state.files.indices) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShareFile(state.files[state.currentIndex]))
            }
        }
    }

    /**
     * Shows delete confirmation dialog.
     */
    fun onDeleteClick() {
        val state = _uiState.value
        if (state.files.isNotEmpty() && state.currentIndex in state.files.indices) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowDeleteConfirmation(state.files[state.currentIndex]))
            }
        }
    }

    /**
     * Confirms deletion of the current file.
     */
    fun confirmDeleteCurrentFile() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch

            // Create a MediaFile for the trash manager
            val file = File(currentPath)
            val mediaFile = MediaFile(
                path = currentPath,
                name = file.name,
                size = file.length(),
                date = Date(file.lastModified()),
                type = getMediaType(file.extension)
            )

            when (trashManager.moveToTrash(mediaFile)) {
                is Result.Success -> {
                    _events.emit(PlayerUiEvent.ShowSnackbar("File deleted"))

                    // Remove file from list and navigate
                    val newFiles = state.files.toMutableList().apply { removeAt(state.currentIndex) }

                    if (newFiles.isEmpty()) {
                        // No more files, close player
                        _events.emit(PlayerUiEvent.NavigateBack)
                    } else {
                        // Update state with remaining files
                        val newIndex = state.currentIndex.coerceAtMost(newFiles.size - 1)
                        val newFile = File(newFiles[newIndex])
                        _uiState.update {
                            it.copy(
                                files = newFiles,
                                currentIndex = newIndex,
                                currentFileName = newFile.name,
                                totalCount = newFiles.size,
                                hasPrevious = newIndex > 0,
                                hasNext = newIndex < newFiles.size - 1
                            )
                        }
                        _events.emit(PlayerUiEvent.NavigateToPage(newIndex))
                    }
                }
                is Result.Error -> {
                    _events.emit(PlayerUiEvent.ShowSnackbar("Failed to delete file"))
                    Timber.e("Failed to delete: $currentPath")
                }
                is Result.Loading -> { /* Ignore */ }
            }
        }
    }

    private fun getMediaType(extension: String): MediaType {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "gif" -> MediaType.GIF
            "mp4", "mkv", "mov", "avi", "webm", "m4v", "3gp", "wmv", "flv" -> MediaType.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "opus" -> MediaType.AUDIO
            "txt", "log", "md", "json", "xml", "html", "css", "js", "kt", "java" -> MediaType.TXT
            "pdf" -> MediaType.PDF
            "epub" -> MediaType.EPUB
            else -> MediaType.OTHER
        }
    }

    /**
     * Shows file information dialog.
     */
    fun onInfoClick() {
        val state = _uiState.value
        if (state.files.isNotEmpty() && state.currentIndex in state.files.indices) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowFileInfo(state.files[state.currentIndex]))
            }
        }
    }

    // ===== Command Panel Actions =====

    /**
     * Search within text file.
     */
    fun onSearchTextClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch
            
            if (state.currentMediaType == MediaType.TXT) {
                _events.emit(PlayerUiEvent.ShowSearchDialog(
                    filePath = currentPath,
                    documentType = "TEXT",
                    content = null
                ))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("Search is only available for text files"))
            }
        }
    }

    /**
     * Translate current content.
     * Shows translation settings dialog to select languages, then performs translation.
     */
    fun onTranslateClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch

            // Get text content based on media type
            val textContent = getTextContentForTranslation(currentPath, state.currentMediaType)
            if (textContent.isNullOrBlank()) {
                _events.emit(PlayerUiEvent.ShowSnackbar("No text content to translate"))
                return@launch
            }

            // Store for later use after dialog
            pendingTranslationContent = textContent

            // Show translation settings dialog
            _events.emit(
                PlayerUiEvent.ShowTranslationDialog(
                    contentToTranslate = textContent.take(500), // Show preview
                    sourceLanguage = lastSourceLanguage,
                    targetLanguage = lastTargetLanguage
                )
            )
        }
    }

    /**
     * Get text content for translation based on media type.
     * For images, this would return OCR text if available.
     * For text files, this returns the file content.
     * For PDF/EPUB, this returns the current page/chapter text.
     */
    private fun getTextContentForTranslation(filePath: String, mediaType: MediaType?): String? {
        return try {
            when (mediaType) {
                MediaType.TXT -> {
                    // Read text file content
                    File(filePath).readText(Charsets.UTF_8).take(10000) // Limit to 10k chars
                }
                MediaType.PDF, MediaType.EPUB -> {
                    // For now, return a placeholder - OCR or text extraction would be needed
                    null
                }
                MediaType.IMAGE, MediaType.GIF -> {
                    // Would need OCR results - return null for now
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read text content for translation")
            null
        }
    }

    /**
     * Perform translation with selected languages.
     * Called from the activity after user confirms language selection.
     */
    fun performTranslation(sourceLanguage: String?, targetLanguage: String) {
        viewModelScope.launch {
            val content = pendingTranslationContent
            if (content.isNullOrBlank()) {
                _events.emit(PlayerUiEvent.ShowSnackbar("No content to translate"))
                return@launch
            }

            // Save language preferences for next time
            lastSourceLanguage = sourceLanguage
            lastTargetLanguage = targetLanguage

            // Show loading
            _events.emit(PlayerUiEvent.ShowTranslationProgress(true))

            try {
                val translatedText = translationManager.translate(
                    text = content,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )

                _events.emit(PlayerUiEvent.ShowTranslationProgress(false))

                if (translatedText != null) {
                    _events.emit(PlayerUiEvent.ShowTranslationResult(translatedText))
                    Timber.d("Translation successful: ${content.take(50)}... -> ${translatedText.take(50)}...")
                } else {
                    _events.emit(PlayerUiEvent.ShowSnackbar("Translation failed"))
                }
            } catch (e: Exception) {
                _events.emit(PlayerUiEvent.ShowTranslationProgress(false))
                _events.emit(PlayerUiEvent.ShowSnackbar("Translation error: ${e.message}"))
                Timber.e(e, "Translation failed")
            }
        }
    }

    /**
     * Set text content for translation from external source (e.g., OCR result).
     */
    fun setTextForTranslation(text: String) {
        pendingTranslationContent = text
    }

    /**
     * Show text/translation settings.
     */
    fun onTextSettingsClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Text settings: Not yet implemented"))
        }
    }

    /**
     * Search within PDF.
     */
    fun onSearchPdfClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch
            
            if (state.currentMediaType == MediaType.PDF) {
                // For PDF, we would need to extract text first
                // For now, show search dialog - text extraction can be added later
                _events.emit(PlayerUiEvent.ShowSearchDialog(
                    filePath = currentPath,
                    documentType = "PDF",
                    content = null
                ))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("PDF search is only available for PDF files"))
            }
        }
    }

    /**
     * Edit PDF (rotate, reorder pages).
     * Opens PDF Tools dialog with page thumbnails and manipulation options.
     */
    fun onEditPdfClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch

            if (state.currentMediaType == MediaType.PDF) {
                _events.emit(PlayerUiEvent.ShowPdfToolsDialog(currentPath))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("PDF tools are only available for PDF files"))
            }
        }
    }

    /**
     * Search within EPUB.
     */
    fun onSearchEpubClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch
            
            if (state.currentMediaType == MediaType.EPUB) {
                // For EPUB, we would need to extract chapter text
                // For now, show search dialog - text extraction can be added later
                _events.emit(PlayerUiEvent.ShowSearchDialog(
                    filePath = currentPath,
                    documentType = "EPUB",
                    content = null
                ))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("EPUB search is only available for EPUB files"))
            }
        }
    }

    /**
     * OCR recognition for current file.
     * Opens OCR dialog with current image/PDF page.
     */
    fun onOcrClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentPath = state.files.getOrNull(state.currentIndex) ?: return@launch

            // OCR is supported for images, PDFs, and EPUBs
            when (state.currentMediaType) {
                MediaType.IMAGE, MediaType.GIF, MediaType.PDF, MediaType.EPUB -> {
                    _events.emit(PlayerUiEvent.ShowOcrDialog(currentPath))
                }
                else -> {
                    _events.emit(PlayerUiEvent.ShowSnackbar("OCR is not available for this file type"))
                }
            }
        }
    }

    /**
     * Open Google Lens for current image/PDF.
     */
    fun onGoogleLensClick() {
        viewModelScope.launch {
            val currentFile = _uiState.value.files.getOrNull(_uiState.value.currentIndex)
            if (currentFile != null) {
                _events.emit(PlayerUiEvent.ShareToGoogleLens(currentFile))
            }
        }
    }

    /**
     * Show lyrics for audio file.
     */
    fun onLyricsClick() {
        val state = _uiState.value
        if (state.files.isEmpty() || state.currentIndex !in state.files.indices) {
            return
        }
        
        val currentFile = state.files[state.currentIndex]
        val extension = currentFile.substringAfterLast('.', "")
        val mediaType = getMediaType(extension)
        
        // Only show lyrics for audio files
        if (mediaType != MediaType.AUDIO) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowSnackbar("Lyrics are only available for audio files"))
            }
            return
        }
        
        // Extract title from file name
        val title = java.io.File(currentFile).nameWithoutExtension
        
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowLyricsDialog(currentFile, null, title))
        }
    }

    /**
     * Rename current file.
     */
    fun onRenameClick() {
        val state = _uiState.value
        if (state.files.isNotEmpty() && state.currentIndex in state.files.indices) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowRenameDialog(state.files[state.currentIndex]))
            }
        }
    }

    /**
     * Perform the actual file rename operation.
     * @param oldPath The current file path
     * @param newName The new file name (without extension)
     */
    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            try {
                val oldFile = File(oldPath)
                if (!oldFile.exists()) {
                    _events.emit(PlayerUiEvent.ShowSnackbar("File not found"))
                    return@launch
                }

                // Validate new name
                if (newName.isBlank()) {
                    _events.emit(PlayerUiEvent.ShowSnackbar("Please enter a filename"))
                    return@launch
                }

                // Check for invalid characters
                if (newName.contains(Regex("[\\\\/:*?\"<>|]"))) {
                    _events.emit(PlayerUiEvent.ShowSnackbar("Invalid filename characters"))
                    return@launch
                }

                // Preserve the original extension
                val extension = oldFile.extension
                val newFileName = if (extension.isNotEmpty()) "$newName.$extension" else newName
                val newFile = File(oldFile.parent, newFileName)

                // Check if target already exists
                if (newFile.exists() && newFile.canonicalPath != oldFile.canonicalPath) {
                    _events.emit(PlayerUiEvent.ShowSnackbar("A file with this name already exists"))
                    return@launch
                }

                // Perform the rename
                if (oldFile.renameTo(newFile)) {
                    // Update the file list with the new path
                    val state = _uiState.value
                    val newFiles = state.files.toMutableList()
                    val index = newFiles.indexOf(oldPath)
                    if (index >= 0) {
                        newFiles[index] = newFile.absolutePath
                        _uiState.update {
                            it.copy(
                                files = newFiles,
                                currentFileName = newFile.name
                            )
                        }
                    }
                    _events.emit(PlayerUiEvent.ShowSnackbar("File renamed successfully"))
                    Timber.d("Renamed: $oldPath -> ${newFile.absolutePath}")
                } else {
                    _events.emit(PlayerUiEvent.ShowSnackbar("Failed to rename file"))
                    Timber.e("Failed to rename: $oldPath")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error renaming file")
                _events.emit(PlayerUiEvent.ShowSnackbar("Error renaming file: ${e.message}"))
            }
        }
    }

    /**
     * Edit current image/GIF.
     */
    fun onEditClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Image editing: Not yet implemented"))
        }
    }

    /**
     * Copy text content to clipboard.
     */
    fun onCopyTextClick() {
        val state = _uiState.value
        val currentPath = state.files.getOrNull(state.currentIndex) ?: return
        
        viewModelScope.launch {
            try {
                val textContent = getTextContentForTranslation(currentPath, state.currentMediaType)
                if (textContent.isNullOrBlank()) {
                    _events.emit(PlayerUiEvent.ShowSnackbar("No text content to copy"))
                    return@launch
                }
                
                // Copy to clipboard - this will be handled by the Activity
                pendingCopyText = textContent
                _events.emit(PlayerUiEvent.CopyToClipboard(textContent))
            } catch (e: Exception) {
                Timber.e(e, "Error reading text content")
                _events.emit(PlayerUiEvent.ShowSnackbar("Error reading text: ${e.message}"))
            }
        }
    }
    
    // Store pending text for clipboard operation
    private var pendingCopyText: String? = null

    /**
     * Enter text editing mode.
     */
    fun onEditTextClick() {
        val state = _uiState.value
        val currentPath = state.files.getOrNull(state.currentIndex) ?: return
        
        // Only allow editing for text files
        if (state.currentMediaType != MediaType.TXT) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowSnackbar("Text editing is only available for text files"))
            }
            return
        }
        
        // Check if file is writable
        val file = File(currentPath)
        if (!file.canWrite()) {
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.ShowSnackbar("This file is read-only"))
            }
            return
        }
        
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowTextEditorDialog(currentPath))
        }
    }

    /**
     * Undo last operation.
     */
    fun onUndoClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Undo: No operations to undo"))
        }
    }

    /**
     * Toggle fullscreen mode.
     */
    fun toggleFullscreen() {
        _uiState.update {
            it.copy(isFullscreen = !it.isFullscreen)
        }
    }

    /**
     * Start/stop slideshow.
     */
    fun onSlideshowClick() {
        toggleSlideshow()
    }

    /**
     * Toggle slideshow mode.
     */
    fun toggleSlideshow() {
        slideshowController.toggle()
        
        val isActive = slideshowController.state.value.isActive
        viewModelScope.launch {
            if (isActive) {
                _events.emit(PlayerUiEvent.ShowSnackbar("Slideshow started"))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("Slideshow stopped"))
            }
        }
    }

    /**
     * Pause/resume slideshow.
     */
    fun toggleSlideshowPause() {
        if (!_uiState.value.isSlideshowActive) return
        
        slideshowController.togglePause()
        
        val isPaused = slideshowController.state.value.isPaused
        viewModelScope.launch {
            if (isPaused) {
                _events.emit(PlayerUiEvent.ShowSnackbar("Slideshow paused"))
            } else {
                _events.emit(PlayerUiEvent.ShowSnackbar("Slideshow resumed"))
            }
        }
    }

    /**
     * Set slideshow interval.
     */
    fun setSlideshowInterval(seconds: Int) {
        slideshowController.setInterval(seconds)
    }

    /**
     * Called when slideshow timer advances.
     */
    private fun advanceSlideshow() {
        val state = _uiState.value
        if (state.currentIndex < state.files.size - 1) {
            // Advance to next
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.NavigateToPage(state.currentIndex + 1))
            }
        } else {
            // Loop back to beginning
            viewModelScope.launch {
                _events.emit(PlayerUiEvent.NavigateToPage(0))
            }
        }
    }

    /**
     * Called on slideshow countdown tick.
     */
    private fun onSlideshowCountdownTick(remaining: Int) {
        // Countdown is already updated via state collection
        Timber.d("Slideshow countdown: $remaining")
    }

    /**
     * Notify slideshow of lifecycle events.
     */
    fun onSlideshowBackground() {
        slideshowController.onBackground()
    }

    fun onSlideshowForeground() {
        slideshowController.onForeground()
    }

    /**
     * Reset slideshow timer after manual navigation.
     */
    fun resetSlideshowTimer() {
        slideshowController.resetTimer()
    }

    /**
     * Rotate current image 90 degrees clockwise.
     */
    fun onRotateClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Rotate: Not yet implemented"))
        }
    }

    /**
     * Copy current file to first destination.
     */
    fun onCopyToFirstDestination() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Copy to destination: Not yet implemented"))
        }
    }

    /**
     * Move current file to first destination.
     */
    fun onMoveToFirstDestination() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Move to destination: Not yet implemented"))
        }
    }

    /**
     * Navigate to previous page (PDF/EPUB).
     */
    fun onPreviousPage() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Previous page: Not yet implemented"))
        }
    }

    /**
     * Navigate to next page (PDF/EPUB).
     */
    fun onNextPage() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Next page: Not yet implemented"))
        }
    }

    /**
     * Navigate to first page (PDF/EPUB).
     */
    fun onFirstPage() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("First page: Not yet implemented"))
        }
    }

    /**
     * Navigate to last page (PDF/EPUB).
     */
    fun onLastPage() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Last page: Not yet implemented"))
        }
    }

    /**
     * Save text file.
     */
    fun onSaveTextClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Save text: Not yet implemented"))
        }
    }

    /**
     * Handles back button press.
     * @return true if the back press was consumed, false otherwise.
     */
    fun onBackPressed(): Boolean {
        // If slideshow is active, stop it first
        if (_uiState.value.isSlideshowActive) {
            slideshowController.stop()
            return true
        }
        // If UI is hidden, show it first
        if (!_uiState.value.isUiVisible) {
            _uiState.update { it.copy(isUiVisible = true) }
            return true
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        slideshowController.release()
    }
}
