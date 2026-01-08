package com.sza.fastmediasorter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 * Manages resource list state and user interactions.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository,
    val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState.Initial)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<MainUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeResources()
    }

    /**
     * Observe resources from repository.
     */
    private fun observeResources() {
        resourceRepository.getAllResourcesFlow()
            .onEach { resources ->
                Timber.d("Resources updated: ${resources.size} items")
                _uiState.update { state ->
                    state.copy(
                        resources = resources,
                        isLoading = false,
                        showEmptyState = resources.isEmpty(),
                        errorMessage = null
                    )
                }
            }
            .catch { error ->
                Timber.e(error, "Error loading resources")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handle resource click - navigate to browse.
     */
    fun onResourceClick(resource: Resource) {
        Timber.d("Resource clicked: ${resource.name}")
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToBrowse(resource.id))
        }
    }

    /**
     * Handle resource long click - show context menu.
     */
    fun onResourceLongClick(resource: Resource): Boolean {
        Timber.d("Resource long clicked: ${resource.name}")
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToEditResource(resource.id))
        }
        return true
    }

    /**
     * Handle more button click on resource item.
     */
    fun onResourceMoreClick(resource: Resource) {
        Timber.d("Resource more clicked: ${resource.name}")
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToEditResource(resource.id))
        }
    }

    /**
     * Handle FAB click - add new resource.
     */
    fun onAddResourceClick() {
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToAddResource)
        }
    }

    /**
     * Handle search menu click.
     */
    fun onSearchClick() {
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToSearch)
        }
    }

    /**
     * Handle favorites menu click.
     */
    fun onFavoritesClick() {
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToFavorites)
        }
    }

    /**
     * Handle settings menu click.
     */
    fun onSettingsClick() {
        viewModelScope.launch {
            _events.send(MainUiEvent.NavigateToSettings)
        }
    }

    /**
     * Delete a resource.
     */
    fun deleteResource(resource: Resource) {
        viewModelScope.launch {
            try {
                resourceRepository.deleteResource(resource)
                _events.send(MainUiEvent.ShowSnackbar("Resource deleted"))
            } catch (e: Exception) {
                Timber.e(e, "Error deleting resource")
                _events.send(MainUiEvent.ShowSnackbar("Error deleting resource"))
            }
        }
    }

    /**
     * Refresh resources.
     */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        // Flow will automatically update when data changes
    }
}
