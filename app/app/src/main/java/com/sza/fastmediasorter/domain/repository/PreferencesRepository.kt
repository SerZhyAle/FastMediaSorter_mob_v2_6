package com.sza.fastmediasorter.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app preferences/settings.
 * Provides access to user preferences with reactive updates.
 */
interface PreferencesRepository {

    // ==================== General Settings ====================

    /**
     * Get the current language code (en, ru, uk).
     */
    val language: Flow<String>
    suspend fun setLanguage(langCode: String)

    /**
     * Get the current theme (light, dark, system).
     */
    val theme: Flow<String>
    suspend fun setTheme(theme: String)

    /**
     * Get the default display mode (grid, list, auto).
     */
    val displayMode: Flow<String>
    suspend fun setDisplayMode(mode: String)

    /**
     * Whether to show hidden files (files starting with .).
     */
    val showHiddenFiles: Flow<Boolean>
    suspend fun setShowHiddenFiles(show: Boolean)

    /**
     * Whether to confirm before deleting files.
     */
    val confirmDelete: Flow<Boolean>
    suspend fun setConfirmDelete(confirm: Boolean)

    /**
     * Whether to confirm before moving files.
     */
    val confirmMove: Flow<Boolean>
    suspend fun setConfirmMove(confirm: Boolean)

    /**
     * Whether to prevent screen sleep during media playback.
     */
    val preventSleepDuringPlayback: Flow<Boolean>
    suspend fun setPreventSleepDuringPlayback(prevent: Boolean)

    // ==================== Playback Settings ====================

    /**
     * Slideshow interval in seconds.
     */
    val slideshowInterval: Flow<Int>
    suspend fun setSlideshowInterval(seconds: Int)

    /**
     * Whether to play slideshow in random order.
     */
    val randomOrder: Flow<Boolean>
    suspend fun setRandomOrder(random: Boolean)

    /**
     * Whether to loop the slideshow.
     */
    val loopSlideshow: Flow<Boolean>
    suspend fun setLoopSlideshow(loop: Boolean)

    /**
     * Whether touch zones are enabled for navigation.
     */
    val enableTouchZones: Flow<Boolean>
    suspend fun setEnableTouchZones(enable: Boolean)

    /**
     * Whether to show touch zone overlay indicators.
     */
    val showZoneOverlay: Flow<Boolean>
    suspend fun setShowZoneOverlay(show: Boolean)

    /**
     * Whether to resume video from last position.
     */
    val resumeFromLastPosition: Flow<Boolean>
    suspend fun setResumeFromLastPosition(resume: Boolean)

    /**
     * Whether to auto-play videos when opened.
     */
    val autoPlayVideos: Flow<Boolean>
    suspend fun setAutoPlayVideos(autoPlay: Boolean)

    // ==================== Onboarding ====================

    /**
     * Whether the user has completed onboarding.
     */
    val hasCompletedOnboarding: Flow<Boolean>
    suspend fun setHasCompletedOnboarding(completed: Boolean)
}
