package com.sza.fastmediasorter.ui.cloudfolders

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.sza.fastmediasorter.BuildConfig
import com.sza.fastmediasorter.data.cloud.AuthResult
import com.sza.fastmediasorter.data.cloud.CloudFile
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import com.sza.fastmediasorter.data.cloud.DropboxClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dropbox folder picker.
 * Handles folder navigation, listing, and selection using Dropbox PKCE OAuth.
 */
@HiltViewModel
class DropboxFolderPickerViewModel @Inject constructor(
    private val dropboxClient: DropboxClient
) : BaseCloudFolderPickerViewModel() {

    override val cloudClient: CloudStorageClient
        get() = dropboxClient
    
    // Dropbox uses empty string for root folder
    override val rootFolderId: String = ""

    override fun getRootFolderName(): String = "Dropbox"

    override suspend fun listFolders(parentId: String): CloudResult<List<CloudFile>> {
        // Dropbox uses paths, not IDs - empty string is root
        return dropboxClient.listFolders(parentId.ifEmpty { null })
    }
    
    /**
     * Start OAuth flow - launches browser/Dropbox app
     */
    fun startOAuthFlow(activity: Activity) {
        val requestConfig = DbxRequestConfig.newBuilder("FastMediaSorter/2.6").build()
        
        Auth.startOAuth2PKCE(
            activity,
            BuildConfig.DROPBOX_KEY,
            requestConfig,
            listOf("files.metadata.read", "files.content.read", "files.content.write")
        )
    }
    
    /**
     * Complete OAuth flow after returning from browser/Dropbox app
     * Must be called from Activity's onResume()
     */
    fun finishOAuthFlow() {
        viewModelScope.launch {
            val result = dropboxClient.finishAuthentication()
            when (result) {
                is AuthResult.Success -> {
                    onAuthenticationSuccess(result.accountName, result.credentialsJson)
                }
                is AuthResult.Error -> {
                    _events.trySend(CloudFolderEvent.ShowError(result.message))
                }
                is AuthResult.Cancelled -> {
                    // User didn't complete OAuth, stay on screen
                }
            }
        }
    }
    
    /**
     * Try to restore session from stored credentials
     */
    fun tryRestoreSession() {
        viewModelScope.launch {
            val restored = dropboxClient.tryRestoreFromStorage()
            if (restored) {
                loadFolder(rootFolderId)
            } else {
                _events.trySend(CloudFolderEvent.AuthenticationRequired)
            }
        }
    }
}
