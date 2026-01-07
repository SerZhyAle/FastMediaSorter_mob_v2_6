package com.sza.fastmediasorter.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import com.sza.fastmediasorter.domain.usecase.GetResourcesUseCase
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
 * ViewModel for BrowseActivity.
 * Manages file list state and user interactions.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel() {

    companion object {
        const val EXTRA_RESOURCE_ID = "EXTRA_RESOURCE_ID"
    }

    private val _uiState = MutableStateFlow(BrowseUiState.Initial)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BrowseUiEvent>()
    val events = _events.asSharedFlow()

    private var loadingJob: Job? = null
    private var currentResourceId: Long = -1L

    init {
        val resourceId = savedStateHandle.get<Long>(EXTRA_RESOURCE_ID) ?: -1L
        if (resourceId != -1L) {
            loadFiles(resourceId)
        }
    }

    fun loadFiles(resourceId: Long) {
        currentResourceId = resourceId
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load resource info
            when (val resourceResult = getResourcesUseCase.getById(resourceId)) {
                is Result.Success -> {
                    val resource = resourceResult.data
                    
                    // Load files from the resource
                    when (val filesResult = getMediaFilesUseCase(resourceId)) {
                        is Result.Success -> {
                            val files = filesResult.data
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    files = files,
                                    resourceName = resource.name,
                                    currentPath = resource.path,
                                    showEmptyState = files.isEmpty()
                                )
                            }
                            Timber.d("Loaded ${files.size} files for resource: ${resource.name}")
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = filesResult.message
                                )
                            }
                        }
                        is Result.Loading -> {
                            // Already showing loading state
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = resourceResult.message)
                    }
                }
                is Result.Loading -> {
                    // Already showing loading state
                }
            }
        }
    }

    fun refresh() {
        if (currentResourceId != -1L) {
            loadFiles(currentResourceId)
        }
    }

    fun onFileClick(mediaFile: MediaFile) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isSelectionMode) {
                toggleSelection(mediaFile)
            } else {
                val files = state.files.map { it.path }
                val currentIndex = files.indexOf(mediaFile.path)
                _events.emit(BrowseUiEvent.NavigateToPlayer(mediaFile.path, files, currentIndex))
            }
        }
    }

    fun onFileLongClick(mediaFile: MediaFile): Boolean {
        val state = _uiState.value
        if (!state.isSelectionMode) {
            // Enter selection mode and select this file
            _uiState.update { 
                it.copy(
                    isSelectionMode = true,
                    selectedFiles = setOf(mediaFile.path)
                )
            }
        } else {
            toggleSelection(mediaFile)
        }
        Timber.d("Long clicked: ${mediaFile.name}, selection mode: ${_uiState.value.isSelectionMode}")
        return true
    }

    private fun toggleSelection(mediaFile: MediaFile) {
        _uiState.update { state ->
            val newSelection = if (state.selectedFiles.contains(mediaFile.path)) {
                state.selectedFiles - mediaFile.path
            } else {
                state.selectedFiles + mediaFile.path
            }
            
            // Exit selection mode if nothing selected
            if (newSelection.isEmpty()) {
                state.copy(isSelectionMode = false, selectedFiles = emptySet())
            } else {
                state.copy(selectedFiles = newSelection)
            }
        }
    }

    fun exitSelectionMode() {
        _uiState.update { 
            it.copy(isSelectionMode = false, selectedFiles = emptySet())
        }
    }

    fun onSelectAllClick() {
        _uiState.update { state ->
            if (state.allSelected) {
                // Deselect all
                state.copy(selectedFiles = emptySet())
            } else {
                // Select all
                state.copy(
                    isSelectionMode = true,
                    selectedFiles = state.files.map { it.path }.toSet()
                )
            }
        }
        Timber.d("Select all clicked: ${_uiState.value.selectedCount} selected")
    }

    fun onMoveClick() {
        viewModelScope.launch {
            val selectedFiles = _uiState.value.selectedFiles.toList()
            if (selectedFiles.isNotEmpty()) {
                _events.emit(BrowseUiEvent.ShowDestinationPicker(selectedFiles))
            }
        }
    }

    fun onCopyClick() {
        viewModelScope.launch {
            val selectedFiles = _uiState.value.selectedFiles.toList()
            if (selectedFiles.isNotEmpty()) {
                _events.emit(BrowseUiEvent.ShowDestinationPicker(selectedFiles))
            }
        }
    }

    fun onDeleteClick() {
        viewModelScope.launch {
            val count = _uiState.value.selectedCount
            if (count > 0) {
                _events.emit(BrowseUiEvent.ShowDeleteConfirmation(count))
            }
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val selectedFiles = _uiState.value.selectedFiles.toList()
            // TODO: Implement actual file deletion via use case
            exitSelectionMode()
        }
    }

    fun onSortClick() {
        viewModelScope.launch {
            _events.emit(BrowseUiEvent.ShowSortDialog)
        }
    }

    fun onSortSelected(sortType: String) {
        // TODO: Update sort order and refresh file list
        _uiState.update { it.copy(/* sortOrder = sortType */) }
    }

    fun onViewModeClick() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            if (_uiState.value.isSelectionMode) {
                exitSelectionMode()
            } else {
                _events.emit(BrowseUiEvent.NavigateBack)
            }
        }
    }
}
