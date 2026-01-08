package com.sza.fastmediasorter.ui.cloudfolders

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.cloud.AuthResult
import com.sza.fastmediasorter.data.cloud.CloudFile
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import com.sza.fastmediasorter.data.cloud.OneDriveRestClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OneDrive folder picker.
 * Handles folder navigation, listing, and selection using MSAL authentication.
 */
@HiltViewModel
class OneDriveFolderPickerViewModel @Inject constructor(
    private val oneDriveClient: OneDriveRestClient
) : BaseCloudFolderPickerViewModel() {

    override val cloudClient: CloudStorageClient
        get() = oneDriveClient
    
    // OneDrive uses "root" as the root folder ID
    override val rootFolderId: String = "root"

    override fun getRootFolderName(): String = "OneDrive"

    override suspend fun listFolders(parentId: String): CloudResult<List<CloudFile>> {
        // Convert "root" to null for the API call (API expects null for root)
        val actualParentId = if (parentId == "root") null else parentId
        return oneDriveClient.listFolders(actualParentId)
    }
    
    /**
     * Start interactive sign-in flow
     * Must be called with Activity context
     */
    fun signIn(activity: Activity) {
        oneDriveClient.signIn(activity) { result ->
            viewModelScope.launch {
                when (result) {
                    is AuthResult.Success -> {
                        onAuthenticationSuccess(result.accountName, result.credentialsJson)
                    }
                    is AuthResult.Error -> {
                        _events.trySend(CloudFolderEvent.ShowError(result.message))
                    }
                    is AuthResult.Cancelled -> {
                        _events.trySend(CloudFolderEvent.NavigateBack)
                    }
                }
            }
        }
    }
    
    /**
     * Try to restore session from stored credentials
     */
    fun tryRestoreSession() {
        viewModelScope.launch {
            val result = oneDriveClient.authenticate()
            when (result) {
                is AuthResult.Success -> {
                    onAuthenticationSuccess(result.accountName, result.credentialsJson)
                }
                is AuthResult.Error -> {
                    // Silent auth failed, need interactive
                    _events.trySend(CloudFolderEvent.AuthenticationRequired)
                }
                is AuthResult.Cancelled -> {
                    _events.trySend(CloudFolderEvent.NavigateBack)
                }
            }
        }
    }
}
