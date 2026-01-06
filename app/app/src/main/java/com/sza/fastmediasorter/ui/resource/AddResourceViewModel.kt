package com.sza.fastmediasorter.ui.resource

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.repository.ResourceRepository
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
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<AddResourceEvent>()
    val events = _events.asSharedFlow()

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // Get folder name from URI
                val folderName = extractFolderName(uri)
                val folderPath = uri.toString()

                Timber.d("Selected folder: $folderName at $folderPath")

                // Create resource
                val resource = Resource(
                    id = 0, // Auto-generated
                    name = folderName,
                    path = folderPath,
                    type = ResourceType.LOCAL,
                    isDestination = false,
                    destinationOrder = 0
                )

                val resourceId = resourceRepository.insertResource(resource)
                Timber.d("Resource inserted with ID: $resourceId")

                _events.emit(AddResourceEvent.ResourceAdded(resourceId))
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to add resource")
                _events.emit(AddResourceEvent.ShowError("Failed to add folder: ${e.message}"))
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
            try {
                val resource = Resource(
                    id = 0,
                    name = name,
                    path = "$host$path",
                    type = type,
                    isDestination = false,
                    destinationOrder = 0
                )

                val resourceId = resourceRepository.insertResource(resource)
                _events.emit(AddResourceEvent.ResourceAdded(resourceId))
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to add network resource")
                _events.emit(AddResourceEvent.ShowError("Failed to add resource: ${e.message}"))
            }
        }
    }
}
