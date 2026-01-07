package com.sza.fastmediasorter.domain.model

/**
 * A search result containing a media file and its parent resource.
 */
data class SearchResult(
    val file: MediaFile,
    val resource: Resource
) {
    val displayPath: String
        get() = "${resource.name}/${file.name}"
}
