package com.sza.fastmediasorter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Destinations settings management.
 */
@HiltViewModel
class DestinationsSettingsViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _destinations = MutableStateFlow<List<Resource>>(emptyList())
    val destinations: StateFlow<List<Resource>> = _destinations.asStateFlow()

    private val _availableResources = MutableStateFlow<List<Resource>>(emptyList())
    val availableResources: StateFlow<List<Resource>> = _availableResources.asStateFlow()

    private val _events = MutableSharedFlow<DestinationsEvent>()
    val events: SharedFlow<DestinationsEvent> = _events.asSharedFlow()

    init {
        loadDestinations()
        loadAvailableResources()
    }

    private fun loadDestinations() {
        viewModelScope.launch {
            resourceRepository.getDestinationsFlow().collect { resources ->
                _destinations.value = resources
                Timber.d("Loaded ${resources.size} destinations")
            }
        }
    }

    private fun loadAvailableResources() {
        viewModelScope.launch {
            resourceRepository.getAllResourcesFlow().collect { resources ->
                // Filter out resources that are already destinations
                _availableResources.value = resources.filter { !it.isDestination }
            }
        }
    }

    fun addDestination(resource: Resource) {
        viewModelScope.launch {
            val maxOrder = _destinations.value.maxOfOrNull { it.destinationOrder } ?: -1
            val updatedResource = resource.copy(
                isDestination = true,
                destinationOrder = maxOrder + 1
            )
            resourceRepository.updateResource(updatedResource)
            _events.emit(DestinationsEvent.DestinationAdded(resource.name))
            Timber.d("Added destination: ${resource.name}")
        }
    }

    fun removeDestination(resource: Resource) {
        viewModelScope.launch {
            val updatedResource = resource.copy(
                isDestination = false,
                destinationOrder = -1
            )
            resourceRepository.updateResource(updatedResource)
            _events.emit(DestinationsEvent.DestinationRemoved(resource.name))
            Timber.d("Removed destination: ${resource.name}")
        }
    }

    fun updateDestinationOrder(destinations: List<Resource>) {
        viewModelScope.launch {
            destinations.forEachIndexed { index, resource ->
                val updated = resource.copy(destinationOrder = index)
                resourceRepository.updateResource(updated)
            }
            Timber.d("Updated destination order")
        }
    }

    fun updateDestinationColor(resource: Resource, color: Int) {
        viewModelScope.launch {
            val updatedResource = resource.copy(destinationColor = color)
            resourceRepository.updateResource(updatedResource)
            Timber.d("Updated destination color for: ${resource.name}")
        }
    }
}

sealed class DestinationsEvent {
    data class DestinationAdded(val name: String) : DestinationsEvent()
    data class DestinationRemoved(val name: String) : DestinationsEvent()
    data class Error(val message: String) : DestinationsEvent()
}
