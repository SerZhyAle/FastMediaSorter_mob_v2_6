package com.sza.fastmediasorter.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementation of PreferencesRepository using DataStore.
 * Provides persistent storage for app settings with reactive updates.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        // General Settings
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val CONFIRM_MOVE = booleanPreferencesKey("confirm_move")
        val PREVENT_SLEEP = booleanPreferencesKey("prevent_sleep_during_playback")

        // Playback Settings
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
        val RANDOM_ORDER = booleanPreferencesKey("random_order")
        val LOOP_SLIDESHOW = booleanPreferencesKey("loop_slideshow")
        val ENABLE_TOUCH_ZONES = booleanPreferencesKey("enable_touch_zones")
        val SHOW_ZONE_OVERLAY = booleanPreferencesKey("show_zone_overlay")
        val RESUME_FROM_LAST_POSITION = booleanPreferencesKey("resume_from_last_position")
        val AUTO_PLAY_VIDEOS = booleanPreferencesKey("auto_play_videos")

        // Onboarding
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    // ==================== General Settings ====================

    override val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: "en"
    }

    override suspend fun setLanguage(langCode: String) {
        Timber.d("Setting language to: $langCode")
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = langCode
        }
    }

    override val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "system"
    }

    override suspend fun setTheme(theme: String) {
        Timber.d("Setting theme to: $theme")
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }

    override val displayMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISPLAY_MODE] ?: "grid"
    }

    override suspend fun setDisplayMode(mode: String) {
        Timber.d("Setting display mode to: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_MODE] = mode
        }
    }

    override val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_HIDDEN_FILES] ?: false
    }

    override suspend fun setShowHiddenFiles(show: Boolean) {
        Timber.d("Setting show hidden files: $show")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_HIDDEN_FILES] = show
        }
    }

    override val confirmDelete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_DELETE] ?: true
    }

    override suspend fun setConfirmDelete(confirm: Boolean) {
        Timber.d("Setting confirm delete: $confirm")
        context.dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_DELETE] = confirm
        }
    }

    override val confirmMove: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_MOVE] ?: false
    }

    override suspend fun setConfirmMove(confirm: Boolean) {
        Timber.d("Setting confirm move: $confirm")
        context.dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_MOVE] = confirm
        }
    }

    override val preventSleepDuringPlayback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREVENT_SLEEP] ?: true
    }

    override suspend fun setPreventSleepDuringPlayback(prevent: Boolean) {
        Timber.d("Setting prevent sleep during playback: $prevent")
        context.dataStore.edit { prefs ->
            prefs[Keys.PREVENT_SLEEP] = prevent
        }
    }

    // ==================== Playback Settings ====================

    override val slideshowInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLIDESHOW_INTERVAL] ?: 5
    }

    override suspend fun setSlideshowInterval(seconds: Int) {
        Timber.d("Setting slideshow interval: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.SLIDESHOW_INTERVAL] = seconds
        }
    }

    override val randomOrder: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RANDOM_ORDER] ?: false
    }

    override suspend fun setRandomOrder(random: Boolean) {
        Timber.d("Setting random order: $random")
        context.dataStore.edit { prefs ->
            prefs[Keys.RANDOM_ORDER] = random
        }
    }

    override val loopSlideshow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOOP_SLIDESHOW] ?: true
    }

    override suspend fun setLoopSlideshow(loop: Boolean) {
        Timber.d("Setting loop slideshow: $loop")
        context.dataStore.edit { prefs ->
            prefs[Keys.LOOP_SLIDESHOW] = loop
        }
    }

    override val enableTouchZones: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_TOUCH_ZONES] ?: true
    }

    override suspend fun setEnableTouchZones(enable: Boolean) {
        Timber.d("Setting enable touch zones: $enable")
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_TOUCH_ZONES] = enable
        }
    }

    override val showZoneOverlay: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_ZONE_OVERLAY] ?: false
    }

    override suspend fun setShowZoneOverlay(show: Boolean) {
        Timber.d("Setting show zone overlay: $show")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_ZONE_OVERLAY] = show
        }
    }

    override val resumeFromLastPosition: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RESUME_FROM_LAST_POSITION] ?: true
    }

    override suspend fun setResumeFromLastPosition(resume: Boolean) {
        Timber.d("Setting resume from last position: $resume")
        context.dataStore.edit { prefs ->
            prefs[Keys.RESUME_FROM_LAST_POSITION] = resume
        }
    }

    override val autoPlayVideos: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PLAY_VIDEOS] ?: false
    }

    override suspend fun setAutoPlayVideos(autoPlay: Boolean) {
        Timber.d("Setting auto play videos: $autoPlay")
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY_VIDEOS] = autoPlay
        }
    }

    // ==================== Onboarding ====================

    override val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        Timber.d("Setting has completed onboarding: $completed")
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
