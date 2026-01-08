package com.sza.fastmediasorter.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for storing and retrieving playback positions for audiobooks/audio files.
 * Used to resume playback from last position.
 */
@Singleton
class PlaybackPositionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "playback_positions"
        private const val KEY_PREFIX_POSITION = "position_"
        private const val KEY_PREFIX_DURATION = "duration_"
        private const val KEY_PREFIX_TIMESTAMP = "timestamp_"
    }

    /**
     * Save playback position for a file.
     * @param filePath Absolute path of the media file
     * @param position Current playback position in milliseconds
     * @param duration Total duration in milliseconds
     */
    fun savePosition(filePath: String, position: Long, duration: Long) {
        val key = filePathToKey(filePath)
        
        prefs.edit()
            .putLong(KEY_PREFIX_POSITION + key, position)
            .putLong(KEY_PREFIX_DURATION + key, duration)
            .putLong(KEY_PREFIX_TIMESTAMP + key, System.currentTimeMillis())
            .apply()
        
        Timber.d("Saved playback position for $filePath: $position / $duration")
    }

    /**
     * Get saved playback position for a file.
     * Returns null if no position is saved.
     */
    fun getPosition(filePath: String): PlaybackPosition? {
        val key = filePathToKey(filePath)
        
        val position = prefs.getLong(KEY_PREFIX_POSITION + key, -1)
        if (position == -1L) {
            return null
        }
        
        val duration = prefs.getLong(KEY_PREFIX_DURATION + key, 0)
        val timestamp = prefs.getLong(KEY_PREFIX_TIMESTAMP + key, 0)
        
        return PlaybackPosition(position, duration, timestamp)
    }

    /**
     * Clear position for a file.
     */
    fun clearPosition(filePath: String) {
        val key = filePathToKey(filePath)
        
        prefs.edit()
            .remove(KEY_PREFIX_POSITION + key)
            .remove(KEY_PREFIX_DURATION + key)
            .remove(KEY_PREFIX_TIMESTAMP + key)
            .apply()
        
        Timber.d("Cleared playback position for $filePath")
    }

    /**
     * Clear all saved positions.
     */
    fun clearAllPositions() {
        prefs.edit().clear().apply()
        Timber.d("Cleared all playback positions")
    }

    /**
     * Get all files with saved positions.
     */
    fun getAllSavedFiles(): List<String> {
        val keys = prefs.all.keys.filter { it.startsWith(KEY_PREFIX_POSITION) }
        return keys.map { keyToFilePath(it.removePrefix(KEY_PREFIX_POSITION)) }
    }

    /**
     * Convert file path to storage key (hash to avoid special characters).
     */
    private fun filePathToKey(filePath: String): String {
        return filePath.hashCode().toString()
    }

    /**
     * Convert storage key back to file path (note: this is lossy, only for display).
     */
    private fun keyToFilePath(key: String): String {
        // Since we use hash, we can't reverse it perfectly
        // This is mainly for debugging/listing purposes
        return key
    }

    /**
     * Data class for playback position.
     */
    data class PlaybackPosition(
        val position: Long,
        val duration: Long,
        val timestamp: Long
    ) {
        /**
         * Check if this position should be resumed.
         * Returns false if:
         * - Position is near the end (>95%)
         * - Position is at the beginning (<1%)
         */
        fun shouldResume(): Boolean {
            if (duration <= 0) return false
            val percentage = (position.toFloat() / duration) * 100
            return percentage in 1.0..95.0
        }

        /**
         * Get progress percentage (0-100).
         */
        fun getProgressPercentage(): Float {
            if (duration <= 0) return 0f
            return (position.toFloat() / duration) * 100
        }
    }
}
