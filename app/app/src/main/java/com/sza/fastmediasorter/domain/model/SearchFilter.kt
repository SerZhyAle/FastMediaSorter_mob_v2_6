package com.sza.fastmediasorter.domain.model

/**
 * Search filter criteria for global search.
 */
data class SearchFilter(
    val query: String = "",
    val fileTypes: Set<MediaType> = emptySet(), // Empty = all types
    val minSize: Long? = null, // Bytes
    val maxSize: Long? = null, // Bytes
    val dateFrom: Long? = null, // Timestamp
    val dateTo: Long? = null // Timestamp
) {
    companion object {
        val EMPTY = SearchFilter()
    }
    
    /**
     * Returns true if any filter criteria is set.
     */
    val hasFilters: Boolean
        get() = query.isNotBlank() || fileTypes.isNotEmpty() || 
                minSize != null || maxSize != null || 
                dateFrom != null || dateTo != null
}
