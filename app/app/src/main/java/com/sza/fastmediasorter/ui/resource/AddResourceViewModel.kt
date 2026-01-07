package com.sza.fastmediasorter.ui.resource

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.usecase.AddResourceUseCase
import com.sza.fastmediasorter.domain.usecase.SaveNetworkCredentialsUseCase
import com.sza.fastmediasorter.domain.usecase.TestNetworkConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Events emitted by AddResourceViewModel.
 */
sealed class AddResourceEvent {
    data class ResourceAdded(val resourceId: Long) : AddResourceEvent()
    data class ShowError(val message: String) : AddResourceEvent()
    data class ShowSnackbar(val message: String) : AddResourceEvent()
    data class ConnectionTesting(val message: String) : AddResourceEvent()
    data class ConnectionSuccess(val message: String) : AddResourceEvent()
    data class ConnectionFailed(val message: String) : AddResourceEvent()
}

/**
 * ViewModel for AddResourceActivity.
 */
@HiltViewModel
class AddResourceViewModel @Inject constructor(
    private val addResourceUseCase: AddResourceUseCase,
    private val saveNetworkCredentialsUseCase: SaveNetworkCredentialsUseCase,
    private val testNetworkConnectionUseCase: TestNetworkConnectionUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<AddResourceEvent>()
    val events = _events.asSharedFlow()

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            // Get folder name from URI
            val folderName = extractFolderName(uri)
            val folderPath = uri.toString()

            Timber.d("Selected folder: $folderName at $folderPath")

            // Use the AddResourceUseCase
            val result = addResourceUseCase.addLocalFolder(
                name = folderName,
                path = folderPath
            )

            result
                .onSuccess { resourceId ->
                    Timber.d("Resource inserted with ID: $resourceId")
                    _events.emit(AddResourceEvent.ResourceAdded(resourceId))
                }
                .onError { message, _, _ ->
                    Timber.e("Failed to add resource: $message")
                    _events.emit(AddResourceEvent.ShowError("Failed to add folder: $message"))
                }
        }
    }

    private fun extractFolderName(uri: Uri): String {
        // Try to get a user-friendly name from the URI
        val path = uri.path ?: uri.toString()
        
        // Handle content:// URIs from SAF
        if (uri.scheme == "content") {
            // Try to extract folder name from document ID
            val documentId = uri.lastPathSegment ?: ""
            
            // Common patterns: "primary:DCIM" or "primary:Pictures/Screenshots"
            val parts = documentId.split(":")
            if (parts.size >= 2) {
                val folderPath = parts.last()
                return folderPath.substringAfterLast("/").ifEmpty { folderPath }
            }
        }
        
        // Fallback: use last path segment
        return path.substringAfterLast("/").ifEmpty { "Folder" }
    }

    fun onNetworkCredentialsEntered(
        credentialId: String,
        type: NetworkType,
        name: String,
        server: String,
        port: Int,
        username: String,
        password: String,
        domain: String,
        shareName: String?,
        useSshKey: Boolean,
        sshKeyPath: String?
    ) {
        viewModelScope.launch {
            // Create credentials object
            val credentials = NetworkCredentials(
                credentialId = credentialId,
                type = type,
                server = server,
                port = port,
                username = username,
                password = password,
                domain = domain,
                shareName = shareName,
                useSshKey = useSshKey,
                sshKeyPath = sshKeyPath
            )

            // Show testing message
            _events.emit(AddResourceEvent.ConnectionTesting("Testing connection..."))

            // Test connection first
            testNetworkConnectionUseCase(credentials)
                .onSuccess {
                    Timber.d("Connection test successful")
                    _events.emit(AddResourceEvent.ConnectionSuccess("Connection successful!"))

                    // Save credentials
                    saveNetworkCredentialsUseCase(credentials)
                        .onSuccess { savedCredentialId ->
                            Timber.d("Credentials saved: $savedCredentialId")

                            // Create resource path based on type
                            val resourcePath = when (type) {
                                NetworkType.SMB -> "smb://$server:$port/$shareName"
                                NetworkType.SFTP -> "sftp://$server:$port"
                                NetworkType.FTP -> "ftp://$server:$port"
                                else -> ""
                            }

                            // Convert NetworkType to ResourceType
                            val resourceType = when (type) {
                                NetworkType.SMB -> ResourceType.SMB
                                NetworkType.SFTP -> ResourceType.SFTP
                                NetworkType.FTP -> ResourceType.FTP
                                else -> return@onSuccess
                            }

                            // Add resource
                            addResourceUseCase(
                                name = name,
                                path = resourcePath,
                                type = resourceType,
                                credentialsId = savedCredentialId
                            ).onSuccess { resourceId ->
                                Timber.d("Network resource added: $resourceId")
                                _events.emit(AddResourceEvent.ResourceAdded(resourceId))
                            }.onError { message, _, _ ->
                                Timber.e("Failed to add network resource: $message")
                                _events.emit(AddResourceEvent.ShowError("Failed to add resource: $message"))
                            }
                        }
                        .onError { message, _, _ ->
                            Timber.e("Failed to save credentials: $message")
                            _events.emit(AddResourceEvent.ShowError("Failed to save credentials: $message"))
                        }
                }
                .onError { message, _, _ ->
                    Timber.e("Connection test failed: $message")
                    _events.emit(AddResourceEvent.ConnectionFailed("Connection failed: $message"))
                }
        }
    }

    fun onNetworkResourceSelected(type: ResourceType, host: String, path: String, name: String) {
        viewModelScope.launch {
            val result = addResourceUseCase(
                name = name,
                path = "$host$path",
                type = type
            )

            result
                .onSuccess { resourceId ->
                    _events.emit(AddResourceEvent.ResourceAdded(resourceId))
                }
                .onError { message, _, _ ->
                    Timber.e("Failed to add network resource: $message")
                    _events.emit(AddResourceEvent.ShowError("Failed to add resource: $message"))
                }
        }
    }
}
