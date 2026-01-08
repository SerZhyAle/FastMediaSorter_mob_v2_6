package com.sza.fastmediasorter.ui.cloudfolders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.cloud.CloudFile
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for cloud folder picker activities.
 * Provides common folder navigation, selection, and authentication handling.
 */
abstract class BaseCloudFolderPickerViewModel : ViewModel() {

    protected abstract val cloudClient: CloudStorageClient

    private val _uiState = MutableStateFlow(CloudFolderUiState())
    val uiState: StateFlow<CloudFolderUiState> = _uiState.asStateFlow()

    protected val _events = Channel<CloudFolderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val folderStack = mutableListOf<CloudFile>()
    
    // Root folder ID varies by provider
    protected abstract val rootFolderId: String

    /**
     * Initialize and check authentication status
     */
    fun initialize(credentialsJson: String?) {
        viewModelScope.launch {
            if (credentialsJson.isNullOrEmpty()) {
                _events.send(CloudFolderEvent.AuthenticationRequired)
                return@launch
            }

            val initialized = cloudClient.initialize(credentialsJson)
            if (!initialized) {
                _events.send(CloudFolderEvent.AuthenticationRequired)
                return@launch
            }

            _uiState.update { it.copy(isAuthenticated = true) }
            loadFolder(rootFolderId)
        }
    }

    /**
     * Called after successful authentication
     */
    fun onAuthenticationSuccess(accountName: String, credentialsJson: String) {
        viewModelScope.launch {
            cloudClient.initialize(credentialsJson)
            _uiState.update { 
                it.copy(
                    isAuthenticated = true,
                    accountName = accountName
                ) 
            }
            loadFolder(rootFolderId)
        }
    }

    /**
     * Load folders from the specified folder ID
     */
    fun loadFolder(folderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = listFolders(folderId)) {
                is CloudResult.Success -> {
                    val currentFolder = if (folderId == rootFolderId) {
                        null
                    } else {
                        CloudFile(
                            id = folderId,
                            name = _uiState.value.currentFolder?.name ?: "Folder",
                            path = buildPath(),
                            isFolder = true
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            folders = result.data,
                            currentFolder = currentFolder,
                            folderStack = folderStack.toList()
                        )
                    }
                }
                is CloudResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Navigate into a subfolder
     */
    fun navigateToFolder(folder: CloudFile) {
        _uiState.value.currentFolder?.let { current ->
            folderStack.add(current)
        }
        _uiState.update { it.copy(currentFolder = folder) }
        loadFolder(folder.id)
    }

    /**
     * Navigate back to parent folder
     */
    fun navigateBack(): Boolean {
        if (folderStack.isEmpty()) {
            return false // At root, cannot go back
        }

        val parent = folderStack.removeLastOrNull()
        if (parent != null) {
            _uiState.update { it.copy(currentFolder = parent) }
            loadFolder(parent.id)
        } else {
            _uiState.update { it.copy(currentFolder = null) }
            loadFolder(rootFolderId)
        }
        return true
    }

    /**
     * Navigate to a specific folder in the breadcrumb path
     */
    fun navigateToBreadcrumb(folder: CloudFile) {
        val index = folderStack.indexOfFirst { it.id == folder.id }
        if (index >= 0) {
            // Remove all folders after this one
            while (folderStack.size > index) {
                folderStack.removeLast()
            }
            _uiState.update { it.copy(currentFolder = folder) }
            loadFolder(folder.id)
        } else if (_uiState.value.currentFolder?.id == folder.id) {
            // Already at this folder, just refresh
            loadFolder(folder.id)
        }
    }

    /**
     * Navigate to root folder
     */
    fun navigateToRoot() {
        folderStack.clear()
        _uiState.update { it.copy(currentFolder = null) }
        loadFolder(rootFolderId)
    }

    /**
     * Select current folder and return result
     */
    fun selectCurrentFolder(isDestination: Boolean, scanSubdirectories: Boolean) {
        viewModelScope.launch {
            val currentFolder = _uiState.value.currentFolder
            val folderId = currentFolder?.id ?: rootFolderId
            val folderName = currentFolder?.name ?: getRootFolderName()
            val folderPath = buildPath()

            _events.send(
                CloudFolderEvent.FolderSelected(
                    folderId = folderId,
                    folderName = folderName,
                    folderPath = folderPath,
                    isDestination = isDestination,
                    scanSubdirectories = scanSubdirectories
                )
            )
        }
    }

    /**
     * Retry last failed operation
     */
    fun retry() {
        val folderId = _uiState.value.currentFolder?.id ?: rootFolderId
        loadFolder(folderId)
    }

    private fun buildPath(): String {
        val parts = mutableListOf<String>()
        parts.add(getRootFolderName())
        folderStack.forEach { parts.add(it.name) }
        _uiState.value.currentFolder?.let { parts.add(it.name) }
        return parts.joinToString("/")
    }

    /**
     * Get root folder display name for this provider
     */
    protected abstract fun getRootFolderName(): String

    /**
     * List folders in the specified parent folder
     */
    protected abstract suspend fun listFolders(parentId: String): CloudResult<List<CloudFile>>
}
