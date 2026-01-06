package com.sza.fastmediasorter.domain.repository

/**
 * Repository interface for application settings.
 */
interface SettingsRepository {

    /**
     * Gets the global "work with all files" setting.
     * When true, all file types are shown, not just media.
     */
    suspend fun getWorkWithAllFiles(): Boolean

    /**
     * Sets the global "work with all files" setting.
     */
    suspend fun setWorkWithAllFiles(enabled: Boolean)

    /**
     * Gets the default sort mode for new resources.
     */
    suspend fun getDefaultSortMode(): String

    /**
     * Sets the default sort mode for new resources.
     */
    suspend fun setDefaultSortMode(sortMode: String)

    /**
     * Gets the default display mode for new resources.
     */
    suspend fun getDefaultDisplayMode(): String

    /**
     * Sets the default display mode for new resources.
     */
    suspend fun setDefaultDisplayMode(displayMode: String)

    /**
     * Gets whether to confirm before delete.
     */
    suspend fun getConfirmDelete(): Boolean

    /**
     * Sets whether to confirm before delete.
     */
    suspend fun setConfirmDelete(confirm: Boolean)

    /**
     * Gets the trash retention period in days.
     */
    suspend fun getTrashRetentionDays(): Int

    /**
     * Sets the trash retention period in days.
     */
    suspend fun setTrashRetentionDays(days: Int)

    /**
     * Gets whether dark mode is enabled.
     * null = follow system
     */
    suspend fun getDarkMode(): Boolean?

    /**
     * Sets dark mode preference.
     */
    suspend fun setDarkMode(enabled: Boolean?)

    /**
     * Gets the app language code (en, ru, uk).
     * null = follow system
     */
    suspend fun getLanguage(): String?

    /**
     * Sets the app language code.
     */
    suspend fun setLanguage(languageCode: String?)
}
