package com.sza.fastmediasorter.ui.cloudfolders

import com.sza.fastmediasorter.data.cloud.CloudFile

/**
 * UI state for cloud folder picker activities
 */
data class CloudFolderUiState(
    val isLoading: Boolean = false,
    val folders: List<CloudFile> = emptyList(),
    val currentFolder: CloudFile? = null,
    val folderStack: List<CloudFile> = emptyList(),
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val accountName: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && folders.isEmpty() && error == null
    
    val hasError: Boolean
        get() = error != null
    
    val breadcrumbs: List<CloudFile>
        get() = folderStack + listOfNotNull(currentFolder)
}

/**
 * Events from cloud folder picker ViewModels
 */
sealed class CloudFolderEvent {
    data class ShowError(val message: String) : CloudFolderEvent()
    data class FolderSelected(
        val folderId: String,
        val folderName: String,
        val folderPath: String,
        val isDestination: Boolean,
        val scanSubdirectories: Boolean
    ) : CloudFolderEvent()
    object AuthenticationRequired : CloudFolderEvent()
    object NavigateBack : CloudFolderEvent()
}
