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

        _uiState.update {
            it.copy(
                files = filePaths,
                currentIndex = safeIndex,
                currentFileName = currentFile.name,
                totalCount = filePaths.size,
                hasPrevious = safeIndex > 0,
                hasNext = safeIndex < filePaths.size - 1,
                isLoading = false
            )
        }

        // Load favorite status for initial file
        loadFavoriteStatus(filePaths[safeIndex])

        Timber.d("Loaded ${filePaths.size} files, starting at index $safeIndex")
    }

    /**
     * Called when user swipes to a new page.
     */
    fun onPageSelected(position: Int) {
        val files = _uiState.value.files
        if (position !in files.indices) return

        val currentFile = File(files[position])

        _uiState.update {
            it.copy(
                currentIndex = position,
                currentFileName = currentFile.name,
                hasPrevious = position > 0,
                hasNext = position < files.size - 1,
                isFavorite = false // Reset until loaded
            )
        }

        // Load favorite status for new file
        loadFavoriteStatus(files[position])

        Timber.d("Page selected: $position - ${currentFile.name}")
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
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "m4v", "3gp", "wmv", "flv" -> MediaType.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "opus" -> MediaType.AUDIO
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
