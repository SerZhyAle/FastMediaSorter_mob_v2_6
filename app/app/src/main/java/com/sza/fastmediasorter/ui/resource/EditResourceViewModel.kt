package com.sza.fastmediasorter.ui.resource

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.usecase.DeleteResourceUseCase
import com.sza.fastmediasorter.domain.usecase.GetResourcesUseCase
import com.sza.fastmediasorter.domain.usecase.UpdateResourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * Events emitted by EditResourceViewModel.
 */
sealed class EditResourceEvent {
    data object ResourceSaved : EditResourceEvent()
    data object ResourceDeleted : EditResourceEvent()
    data class ShowError(val message: String) : EditResourceEvent()
    data class ShowSnackbar(val message: String) : EditResourceEvent()
    data object NavigateBack : EditResourceEvent()
}

/**
 * UI State for EditResourceActivity.
 */
data class EditResourceUiState(
    val resourceId: Long = -1,
    val name: String = "",
    val path: String = "",
    val resourceType: ResourceType = ResourceType.LOCAL,
    val resourceTypeName: String = "",
    val sortMode: SortMode = SortMode.NAME_ASC,
    val displayMode: DisplayMode = DisplayMode.LIST,
    val isDestination: Boolean = false,
    val destinationOrder: Int = -1,
    val destinationColor: Int = 0xFF4CAF50.toInt(),
    val workWithAllFiles: Boolean = false,
    val isLoading: Boolean = true,
    val isEdited: Boolean = false,
    val canSave: Boolean = false,
    val nameError: String? = null
) {
    companion object {
        val Initial = EditResourceUiState()
    }
}

/**
 * ViewModel for EditResourceActivity.
 * Manages resource editing state and operations.
 */
@HiltViewModel
class EditResourceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val updateResourceUseCase: UpdateResourceUseCase,
    private val deleteResourceUseCase: DeleteResourceUseCase
) : ViewModel() {

    companion object {
        private const val KEY_RESOURCE_ID = "EXTRA_RESOURCE_ID"
    }

    private val _uiState = MutableStateFlow(EditResourceUiState.Initial)
    val uiState: StateFlow<EditResourceUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditResourceEvent>()
    val events = _events.asSharedFlow()

    private var originalResource: Resource? = null

    init {
        val resourceId = savedStateHandle.get<Long>(KEY_RESOURCE_ID) ?: -1L
        if (resourceId != -1L) {
            loadResource(resourceId)
        }
    }

    /**
     * Load resource data from database.
     */
    fun loadResource(resourceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = getResourcesUseCase.getById(resourceId)) {
                is com.sza.fastmediasorter.domain.model.Result.Success -> {
                    val resource = result.data
                    originalResource = resource

                    _uiState.update {
                        it.copy(
                            resourceId = resource.id,
                            name = resource.name,
                            path = resource.path,
                            resourceType = resource.type,
                            resourceTypeName = getResourceTypeName(resource.type),
                            sortMode = resource.sortMode,
                            displayMode = resource.displayMode,
                            isDestination = resource.isDestination,
                            destinationOrder = resource.destinationOrder,
                            destinationColor = resource.destinationColor,
                            workWithAllFiles = resource.workWithAllFiles,
                            isLoading = false,
                            isEdited = false,
                            canSave = false
                        )
                    }

                    Timber.d("Loaded resource: ${resource.name}")
                }
                is com.sza.fastmediasorter.domain.model.Result.Error -> {
                    Timber.e("Failed to load resource: ${result.message}")
                    _events.emit(EditResourceEvent.ShowError("Failed to load resource"))
                    _events.emit(EditResourceEvent.NavigateBack)
                }
                is com.sza.fastmediasorter.domain.model.Result.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    /**
     * Handle name input change.
     */
    fun onNameChanged(name: String) {
        val nameError = if (name.isBlank()) "Name cannot be empty" else null
        _uiState.update {
            it.copy(
                name = name,
                nameError = nameError,
                isEdited = true,
                canSave = name.isNotBlank() && hasChanges(it.copy(name = name))
            )
        }
    }

    /**
     * Handle sort mode change.
     */
    fun onSortModeChanged(sortMode: SortMode) {
        _uiState.update {
            it.copy(
                sortMode = sortMode,
                isEdited = true,
                canSave = it.name.isNotBlank() && hasChanges(it.copy(sortMode = sortMode))
            )
        }
    }

    /**
     * Handle display mode change.
     */
    fun onDisplayModeChanged(displayMode: DisplayMode) {
        _uiState.update {
            it.copy(
                displayMode = displayMode,
                isEdited = true,
                canSave = it.name.isNotBlank() && hasChanges(it.copy(displayMode = displayMode))
            )
        }
    }

    /**
     * Handle destination toggle.
     */
    fun onDestinationChanged(isDestination: Boolean) {
        _uiState.update {
            it.copy(
                isDestination = isDestination,
                isEdited = true,
                canSave = it.name.isNotBlank() && hasChanges(it.copy(isDestination = isDestination))
            )
        }
    }

    /**
     * Handle work with all files toggle.
     */
    fun onWorkWithAllFilesChanged(workWithAllFiles: Boolean) {
        _uiState.update {
            it.copy(
                workWithAllFiles = workWithAllFiles,
                isEdited = true,
                canSave = it.name.isNotBlank() && hasChanges(it.copy(workWithAllFiles = workWithAllFiles))
            )
        }
    }

    /**
     * Check if there are unsaved changes.
     */
    private fun hasChanges(state: EditResourceUiState): Boolean {
        val original = originalResource ?: return false
        return state.name != original.name ||
                state.sortMode != original.sortMode ||
                state.displayMode != original.displayMode ||
                state.isDestination != original.isDestination ||
                state.workWithAllFiles != original.workWithAllFiles
    }

    /**
     * Save resource changes.
     */
    fun saveResource() {
        val state = _uiState.value
        val original = originalResource

        if (original == null) {
            Timber.e("Cannot save: original resource is null")
            return
        }

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val updatedResource = original.copy(
                name = state.name,
                sortMode = state.sortMode,
                displayMode = state.displayMode,
                isDestination = state.isDestination,
                workWithAllFiles = state.workWithAllFiles
            )

            when (val result = updateResourceUseCase(updatedResource)) {
                is com.sza.fastmediasorter.domain.model.Result.Success -> {
                    Timber.d("Resource updated successfully")
                    _events.emit(EditResourceEvent.ResourceSaved)
                }
                is com.sza.fastmediasorter.domain.model.Result.Error -> {
                    Timber.e("Failed to update resource: ${result.message}")
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(EditResourceEvent.ShowError("Failed to save: ${result.message}"))
                }
                is com.sza.fastmediasorter.domain.model.Result.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    /**
     * Delete the resource.
     */
    fun deleteResource() {
        val original = originalResource ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = deleteResourceUseCase(original.id)) {
                is com.sza.fastmediasorter.domain.model.Result.Success -> {
                    Timber.d("Resource deleted successfully")
                    _events.emit(EditResourceEvent.ResourceDeleted)
                }
                is com.sza.fastmediasorter.domain.model.Result.Error -> {
                    Timber.e("Failed to delete resource: ${result.message}")
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(EditResourceEvent.ShowError("Failed to delete: ${result.message}"))
                }
                is com.sza.fastmediasorter.domain.model.Result.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    private fun getResourceTypeName(type: ResourceType): String {
        return when (type) {
            ResourceType.LOCAL -> "Local Folder"
            ResourceType.SMB -> "SMB/Windows Share"
            ResourceType.SFTP -> "SFTP"
            ResourceType.FTP -> "FTP"
            ResourceType.GOOGLE_DRIVE -> "Google Drive"
            ResourceType.ONEDRIVE -> "OneDrive"
            ResourceType.DROPBOX -> "Dropbox"
        }
    }
}
