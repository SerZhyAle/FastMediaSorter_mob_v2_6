package com.sza.fastmediasorter.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * ViewModel for PlayerActivity.
 * Manages media viewing state and user interactions.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

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
                // Reset favorite status for new file (would be loaded from DB in full impl)
                isFavorite = false
            )
        }

        Timber.d("Page selected: $position - ${currentFile.name}")
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
     * Handles long click on media (future: context menu).
     */
    fun onMediaLongClick(): Boolean {
        Timber.d("Media long clicked")
        // TODO: Show context menu
        return true
    }

    /**
     * Toggles favorite status for current file.
     */
    fun onFavoriteClick() {
        val state = _uiState.value
        val newFavoriteState = !state.isFavorite

        _uiState.update {
            it.copy(isFavorite = newFavoriteState)
        }

        // TODO: Persist favorite status to database
        viewModelScope.launch {
            val message = if (newFavoriteState) "Added to favorites" else "Removed from favorites"
            _events.emit(PlayerUiEvent.ShowSnackbar(message))
        }

        Timber.d("Favorite toggled: $newFavoriteState")
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
