package com.sza.fastmediasorter.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for DestinationPickerDialog.
 * Loads destination resources from the database.
 */
@HiltViewModel
class DestinationPickerViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _destinations = MutableStateFlow<List<Resource>>(emptyList())
    val destinations: StateFlow<List<Resource>> = _destinations.asStateFlow()

    init {
        loadDestinations()
    }

    private fun loadDestinations() {
        viewModelScope.launch {
            resourceRepository.getDestinationsFlow().collect { resources ->
                _destinations.value = resources
            }
        }
    }
}
