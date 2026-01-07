package com.sza.fastmediasorter.ui.search

import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.SearchResult

/**
 * UI State for SearchActivity.
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val selectedFileTypes: Set<MediaType> = emptySet(),
    val showFilters: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        val Initial = SearchUiState()
    }
    
    val hasResults: Boolean get() = results.isNotEmpty()
    val showEmptyState: Boolean get() = !isLoading && results.isEmpty() && query.isNotBlank()
}

/**
 * Events emitted by SearchViewModel for one-time actions.
 */
sealed class SearchUiEvent {
    data class ShowSnackbar(val message: String) : SearchUiEvent()
    data class NavigateToFile(val filePath: String, val resourceId: Long) : SearchUiEvent()
    data class ShowFileInfo(val filePath: String) : SearchUiEvent()
}
