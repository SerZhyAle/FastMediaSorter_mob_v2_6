package com.sza.fastmediasorter.ui.resource

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.usecase.AddResourceUseCase
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
}

/**
 * ViewModel for AddResourceActivity.
 */
@HiltViewModel
class AddResourceViewModel @Inject constructor(
    private val addResourceUseCase: AddResourceUseCase
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
