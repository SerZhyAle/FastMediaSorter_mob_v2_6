package com.sza.fastmediasorter.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Playback Settings.
 * Handles slideshow, touch zones, and video playback preferences.
 */
@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackSettingsUiState())
    val uiState: StateFlow<PlaybackSettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("PlaybackSettingsViewModel initialized")
        loadSettings()
    }

    private fun loadSettings() {
        // TODO: Load from PreferencesRepository
        _uiState.update {
            it.copy(
                slideshowInterval = 5,
                randomOrder = false,
                loopSlideshow = true,
                enableTouchZones = true,
                showZoneOverlay = false,
                resumeFromLastPosition = true,
                autoPlayVideos = false
            )
        }
    }

    fun setSlideshowInterval(seconds: Int) {
        Timber.d("Setting slideshow interval to: $seconds seconds")
        _uiState.update { it.copy(slideshowInterval = seconds) }
        // TODO: Save to preferences
    }

    fun setRandomOrder(random: Boolean) {
        Timber.d("Setting random order: $random")
        _uiState.update { it.copy(randomOrder = random) }
        // TODO: Save to preferences
    }

    fun setLoopSlideshow(loop: Boolean) {
        Timber.d("Setting loop slideshow: $loop")
        _uiState.update { it.copy(loopSlideshow = loop) }
        // TODO: Save to preferences
    }

    fun setEnableTouchZones(enable: Boolean) {
        Timber.d("Setting enable touch zones: $enable")
        _uiState.update { it.copy(enableTouchZones = enable) }
        // TODO: Save to preferences
    }

    fun setShowZoneOverlay(show: Boolean) {
        Timber.d("Setting show zone overlay: $show")
        _uiState.update { it.copy(showZoneOverlay = show) }
        // TODO: Save to preferences
    }

    fun setResumeFromLastPosition(resume: Boolean) {
        Timber.d("Setting resume from last position: $resume")
        _uiState.update { it.copy(resumeFromLastPosition = resume) }
        // TODO: Save to preferences
    }

    fun setAutoPlayVideos(autoPlay: Boolean) {
        Timber.d("Setting auto play videos: $autoPlay")
        _uiState.update { it.copy(autoPlayVideos = autoPlay) }
        // TODO: Save to preferences
    }
}

/**
 * UI state for Playback Settings.
 */
data class PlaybackSettingsUiState(
    val slideshowInterval: Int = 5,
    val randomOrder: Boolean = false,
    val loopSlideshow: Boolean = true,
    val enableTouchZones: Boolean = true,
    val showZoneOverlay: Boolean = false,
    val resumeFromLastPosition: Boolean = true,
    val autoPlayVideos: Boolean = false
)
