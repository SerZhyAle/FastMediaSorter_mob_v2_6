package com.sza.fastmediasorter.ui.cloudfolders

import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.sza.fastmediasorter.data.cloud.AuthResult
import com.sza.fastmediasorter.data.cloud.CloudFile
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Google Drive folder picker.
 * Handles folder navigation, listing, and selection.
 */
@HiltViewModel
class GoogleDriveFolderPickerViewModel @Inject constructor(
    private val googleDriveClient: GoogleDriveRestClient
) : BaseCloudFolderPickerViewModel() {

    override val cloudClient: CloudStorageClient
        get() = googleDriveClient
    
    // Google Drive uses "root" as the root folder ID
    override val rootFolderId: String = "root"

    override fun getRootFolderName(): String = "My Drive"

    override suspend fun listFolders(parentId: String): CloudResult<List<CloudFile>> {
        // Convert "root" to null for the API call (API expects null for root)
        val actualParentId = if (parentId == "root") null else parentId
        return googleDriveClient.listFolders(actualParentId)
    }
    
    /**
     * Try to restore session from stored credentials
     */
    fun tryRestoreSession() {
        viewModelScope.launch {
            val restored = googleDriveClient.tryRestoreFromStorage()
            if (restored) {
                loadFolder(rootFolderId)
            }
        }
    }
    
    /**
     * Get the sign-in intent for launching authentication
     */
    fun getSignInIntent() = googleDriveClient.getSignInIntent()
    
    /**
     * Handle sign-in result from activity
     */
    suspend fun handleSignInResult(account: GoogleSignInAccount?): AuthResult {
        return googleDriveClient.handleSignInResult(account)
    }
}
