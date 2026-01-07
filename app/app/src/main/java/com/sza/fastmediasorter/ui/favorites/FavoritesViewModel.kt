package com.sza.fastmediasorter.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.usecase.GetFavoriteFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for FavoritesActivity.
 * Manages favorite files list state.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteFilesUseCase: GetFavoriteFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState.Initial)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FavoritesUiEvent>()
    val events = _events.asSharedFlow()

    private var loadingJob: Job? = null

    fun loadFavorites() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getFavoriteFilesUseCase()) {
                is Result.Success -> {
                    val files = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            files = files,
                            showEmptyState = files.isEmpty()
                        )
                    }
                    Timber.d("Loaded ${files.size} favorite files")
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    Timber.e(result.throwable, "Error loading favorites: ${result.message}")
                }
                is Result.Loading -> {
                    // Already showing loading state
                }
            }
        }
    }

    fun onFileClick(mediaFile: MediaFile) {
        viewModelScope.launch {
            val files = _uiState.value.files.map { it.path }
            val currentIndex = files.indexOf(mediaFile.path)
            _events.emit(FavoritesUiEvent.NavigateToPlayer(mediaFile.path, files, currentIndex))
        }
    }
}

/**
 * UI State for FavoritesActivity.
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val files: List<MediaFile> = emptyList(),
    val showEmptyState: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        val Initial = FavoritesUiState()
    }
}

/**
 * Events emitted by FavoritesViewModel.
 */
sealed class FavoritesUiEvent {
    data class ShowSnackbar(val message: String) : FavoritesUiEvent()
    data class NavigateToPlayer(val filePath: String, val files: List<String>, val currentIndex: Int) : FavoritesUiEvent()
}
