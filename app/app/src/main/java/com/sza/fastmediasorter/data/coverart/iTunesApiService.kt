package com.sza.fastmediasorter.data.coverart

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * iTunes Search API service for album art lookup
 * https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/
 */
interface iTunesApiService {
    
    @GET("search")
    suspend fun searchAlbum(
        @Query("term") searchTerm: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "album",
        @Query("limit") limit: Int = 10
    ): iTunesSearchResponse
}

data class iTunesSearchResponse(
    val resultCount: Int,
    val results: List<iTunesAlbumResult>
)

data class iTunesAlbumResult(
    val artistName: String?,
    val collectionName: String?,
    val artworkUrl30: String?,  // 30x30
    val artworkUrl60: String?,  // 60x60
    val artworkUrl100: String?, // 100x100
    val artworkUrl600: String?, // 600x600 (available by replacing '100' with '600')
    val collectionId: Long?,
    val primaryGenreName: String?
) {
    /**
     * Get high-resolution artwork URL (600x600 or larger)
     */
    fun getHighResArtworkUrl(): String? {
        return artworkUrl100?.replace("100x100", "600x600")
    }
}
