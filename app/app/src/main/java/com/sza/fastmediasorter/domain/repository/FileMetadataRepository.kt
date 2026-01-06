package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing file metadata (favorites, playback state, etc.).
 */
interface FileMetadataRepository {

    /**
     * Observes favorite files as a Flow.
     * @return Flow of favorite MediaFile paths
     */
    fun getFavoritesFlow(): Flow<List<String>>

    /**
     * Gets all favorite file paths.
     * @return List of favorite file paths
     */
    suspend fun getFavorites(): List<String>

    /**
     * Checks if a file is marked as favorite.
     * @param path The file path
     * @return True if favorite
     */
    suspend fun isFavorite(path: String): Boolean

    /**
     * Toggles the favorite status of a file.
     * @param path The file path
     * @param resourceId The parent resource ID
     * @return The new favorite status
     */
    suspend fun toggleFavorite(path: String, resourceId: Long): Boolean

    /**
     * Gets the last playback position for a media file.
     * @param path The file path
     * @return Position in milliseconds, or 0 if not found
     */
    suspend fun getPlaybackPosition(path: String): Long

    /**
     * Saves the playback position for a media file.
     * @param path The file path
     * @param position Position in milliseconds
     */
    suspend fun savePlaybackPosition(path: String, position: Long)

    /**
     * Records that a file has been viewed.
     * @param path The file path
     */
    suspend fun recordView(path: String)
}
