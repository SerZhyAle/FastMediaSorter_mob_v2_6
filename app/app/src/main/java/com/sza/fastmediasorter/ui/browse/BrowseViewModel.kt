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
            val files = _uiState.value.files.map { it.path }
            val currentIndex = files.indexOf(mediaFile.path)
            _events.emit(BrowseUiEvent.NavigateToPlayer(mediaFile.path, files, currentIndex))
        }
    }

    fun onFileLongClick(mediaFile: MediaFile): Boolean {
        // TODO: Implement selection mode
        Timber.d("Long clicked: ${mediaFile.name}")
        return true
    }

    fun onSortClick() {
        // TODO: Implement sorting options dialog
        Timber.d("Sort clicked")
    }

    fun onViewModeClick() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun onSelectAllClick() {
        // TODO: Implement select all
        Timber.d("Select all clicked")
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _events.emit(BrowseUiEvent.NavigateBack)
        }
    }
}
