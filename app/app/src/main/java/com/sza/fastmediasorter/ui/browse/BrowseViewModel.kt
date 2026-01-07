package com.sza.fastmediasorter.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.operation.TrashManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
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
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for BrowseActivity.
 * Manages file list state and user interactions.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val fileOperationStrategy: FileOperationStrategy,
    private val trashManager: TrashManager
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
                _events.emit(BrowseUiEvent.ShowDestinationPicker(selectedFiles, isMove = true))
            }
        }
    }

    fun onCopyClick() {
        viewModelScope.launch {
            val selectedFiles = _uiState.value.selectedFiles.toList()
            if (selectedFiles.isNotEmpty()) {
                _events.emit(BrowseUiEvent.ShowDestinationPicker(selectedFiles, isMove = false))
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
            _uiState.update { it.copy(isLoading = true) }
            
            val selectedPaths = _uiState.value.selectedFiles.toList()
            val selectedFiles = _uiState.value.files.filter { it.path in selectedPaths }
            
            var successCount = 0
            var failCount = 0
            
            for (file in selectedFiles) {
                when (trashManager.moveToTrash(file)) {
                    is Result.Success -> successCount++
                    is Result.Error -> {
                        failCount++
                        Timber.e("Failed to delete: ${file.path}")
                    }
                    is Result.Loading -> { /* Ignore */ }
                }
            }
            
            exitSelectionMode()
            refresh()
            
            // Show undo snackbar
            if (successCount > 0) {
                val message = if (failCount == 0) {
                    "$successCount file(s) deleted"
                } else {
                    "$successCount deleted, $failCount failed"
                }
                _events.emit(BrowseUiEvent.ShowUndoSnackbar(message, successCount))
            } else if (failCount > 0) {
                _events.emit(BrowseUiEvent.ShowSnackbar("Failed to delete files"))
            }
        }
    }

    fun undoLastDelete() {
        viewModelScope.launch {
            val lastDeleted = trashManager.getLastDeleted()
            if (lastDeleted != null) {
                when (val result = trashManager.restoreFromTrash(lastDeleted)) {
                    is Result.Success -> {
                        refresh()
                        _events.emit(BrowseUiEvent.ShowSnackbar("File restored"))
                    }
                    is Result.Error -> {
                        _events.emit(BrowseUiEvent.ShowSnackbar("Failed to restore: ${result.message}"))
                    }
                    is Result.Loading -> { /* Ignore */ }
                }
            }
        }
    }

    fun undoRecentDeletes(count: Int) {
        viewModelScope.launch {
            val recentlyDeleted = trashManager.getRecentlyDeleted().take(count)
            var restoredCount = 0
            
            for (trashedFile in recentlyDeleted) {
                when (trashManager.restoreFromTrash(trashedFile)) {
                    is Result.Success -> restoredCount++
                    is Result.Error -> Timber.e("Failed to restore: ${trashedFile.originalPath}")
                    is Result.Loading -> { /* Ignore */ }
                }
            }
            
            if (restoredCount > 0) {
                refresh()
                _events.emit(BrowseUiEvent.ShowSnackbar("$restoredCount file(s) restored"))
            }
        }
    }

    fun onSortClick() {
        viewModelScope.launch {
            _events.emit(BrowseUiEvent.ShowSortDialog(_uiState.value.sortMode))
        }
    }

    fun onSortModeSelected(sortMode: SortMode) {
        val currentFiles = _uiState.value.files
        val sortedFiles = sortFiles(currentFiles, sortMode)
        _uiState.update { 
            it.copy(
                sortMode = sortMode,
                files = sortedFiles
            )
        }
    }

    private fun sortFiles(files: List<MediaFile>, sortMode: SortMode): List<MediaFile> {
        return when (sortMode) {
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortMode.DATE_ASC -> files.sortedBy { it.date }
            SortMode.DATE_DESC -> files.sortedByDescending { it.date }
            SortMode.SIZE_ASC -> files.sortedBy { it.size }
            SortMode.SIZE_DESC -> files.sortedByDescending { it.size }
        }
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

    fun executeFileOperation(filePaths: List<String>, destinationDir: String, isMove: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            var successCount = 0
            var failCount = 0
            
            for (filePath in filePaths) {
                val fileName = File(filePath).name
                val destinationPath = "$destinationDir/$fileName"
                
                // Create a simple MediaFile for the operation
                val sourceFile = _uiState.value.files.find { it.path == filePath }
                if (sourceFile == null) {
                    failCount++
                    continue
                }
                
                val result = if (isMove) {
                    fileOperationStrategy.move(sourceFile, destinationPath)
                } else {
                    fileOperationStrategy.copy(sourceFile, destinationPath)
                }
                
                when (result) {
                    is Result.Success -> successCount++
                    is Result.Error -> {
                        failCount++
                        Timber.e(result.throwable, "Failed to ${if (isMove) "move" else "copy"} $filePath: ${result.message}")
                    }
                    is Result.Loading -> { /* Ignore loading state */ }
                }
            }
            
            // Exit selection mode and refresh
            exitSelectionMode()
            refresh()
            
            // Show result message
            val message = if (isMove) {
                if (failCount == 0) "$successCount file(s) moved"
                else "$successCount moved, $failCount failed"
            } else {
                if (failCount == 0) "$successCount file(s) copied"
                else "$successCount copied, $failCount failed"
            }
            _events.emit(BrowseUiEvent.ShowSnackbar(message))
        }
    }
}
