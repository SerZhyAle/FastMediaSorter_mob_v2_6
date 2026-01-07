package com.sza.fastmediasorter.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SearchFilter
import com.sza.fastmediasorter.domain.model.SearchResult
import com.sza.fastmediasorter.domain.usecase.GlobalSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for SearchActivity.
 * Manages global search state and executes searches across all resources.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val globalSearchUseCase: GlobalSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchUiEvent>()
    val events = _events.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Auto-search with debounce when query changes
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length >= 2) { // Minimum 2 characters
                        executeSearch()
                    } else if (query.isEmpty()) {
                        _uiState.update { it.copy(results = emptyList()) }
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchQueryFlow.value = query
    }

    fun onFileTypeToggled(fileType: MediaType) {
        val currentTypes = _uiState.value.selectedFileTypes.toMutableSet()
        if (currentTypes.contains(fileType)) {
            currentTypes.remove(fileType)
        } else {
            currentTypes.add(fileType)
        }
        _uiState.update { it.copy(selectedFileTypes = currentTypes) }
        
        // Re-execute search with new filters
        if (_uiState.value.query.length >= 2) {
            executeSearch()
        }
    }

    fun onToggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun onClearFilters() {
        _uiState.update { 
            it.copy(
                selectedFileTypes = emptySet(),
                showFilters = false
            ) 
        }
        
        // Re-execute search without filters
        if (_uiState.value.query.length >= 2) {
            executeSearch()
        }
    }

    fun onResultClick(result: SearchResult) {
        viewModelScope.launch {
            _events.emit(
                SearchUiEvent.NavigateToFile(
                    filePath = result.file.path,
                    resourceId = result.resource.id
                )
            )
        }
    }

    fun onResultLongClick(result: SearchResult) {
        viewModelScope.launch {
            _events.emit(SearchUiEvent.ShowFileInfo(result.file.path))
        }
    }

    private fun executeSearch() {
        val currentState = _uiState.value
        
        val filter = SearchFilter(
            query = currentState.query,
            fileTypes = currentState.selectedFileTypes
        )

        viewModelScope.launch {
            globalSearchUseCase(filter).collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                results = result.data,
                                errorMessage = null
                            )
                        }
                        Timber.d("Search completed: ${result.data.size} results")
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                        _events.emit(SearchUiEvent.ShowSnackbar(result.message ?: "Search failed"))
                    }
                }
            }
        }
    }
}
