package com.sza.fastmediasorter.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.operation.TrashManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
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
    val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState.Initial)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlayerUiEvent>()
    val events = _events.asSharedFlow()

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
            _events.emit(PlayerUiEvent.ShowSnackbar("Search in text: Not yet implemented"))
        }
    }

    /**
     * Translate current content.
     */
    fun onTranslateClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Translation: Not yet implemented"))
        }
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
            _events.emit(PlayerUiEvent.ShowSnackbar("Search in PDF: Not yet implemented"))
        }
    }

    /**
     * Edit PDF (rotate, reorder pages).
     */
    fun onEditPdfClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("PDF editing: Not yet implemented"))
        }
    }

    /**
     * Search within EPUB.
     */
    fun onSearchEpubClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Search in EPUB: Not yet implemented"))
        }
    }

    /**
     * OCR recognition for current file.
     */
    fun onOcrClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("OCR: Not yet implemented"))
        }
    }

    /**
     * Open Google Lens for current image/PDF.
     */
    fun onGoogleLensClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Google Lens: Not yet implemented"))
        }
    }

    /**
     * Show lyrics for audio file.
     */
    fun onLyricsClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Lyrics: Not yet implemented"))
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
     * Copy text to clipboard.
     */
    fun onCopyTextClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Copy text: Not yet implemented"))
        }
    }

    /**
     * Enter text editing mode.
     */
    fun onEditTextClick() {
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Text editing: Not yet implemented"))
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
        viewModelScope.launch {
            _events.emit(PlayerUiEvent.ShowSnackbar("Slideshow: Not yet implemented"))
        }
    }

    /**
     * Handles back button press.
     * @return true if the back press was consumed, false otherwise.
     */
    fun onBackPressed(): Boolean {
        // If UI is hidden, show it first
        if (!_uiState.value.isUiVisible) {
            _uiState.update { it.copy(isUiVisible = true) }
            return true
        }
        return false
    }
}
