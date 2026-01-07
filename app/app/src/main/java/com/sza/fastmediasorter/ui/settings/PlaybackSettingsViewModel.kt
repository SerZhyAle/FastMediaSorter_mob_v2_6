package com.sza.fastmediasorter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Playback Settings.
 * Handles slideshow, touch zones, and video playback preferences.
 */
@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackSettingsUiState())
    val uiState: StateFlow<PlaybackSettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("PlaybackSettingsViewModel initialized")
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                preferencesRepository.slideshowInterval,
                preferencesRepository.randomOrder,
                preferencesRepository.loopSlideshow,
                preferencesRepository.enableTouchZones,
                preferencesRepository.showZoneOverlay,
                preferencesRepository.resumeFromLastPosition,
                preferencesRepository.autoPlayVideos
            ) { values ->
                PlaybackSettingsUiState(
                    slideshowInterval = values[0] as Int,
                    randomOrder = values[1] as Boolean,
                    loopSlideshow = values[2] as Boolean,
                    enableTouchZones = values[3] as Boolean,
                    showZoneOverlay = values[4] as Boolean,
                    resumeFromLastPosition = values[5] as Boolean,
                    autoPlayVideos = values[6] as Boolean
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSlideshowInterval(seconds: Int) {
        Timber.d("Setting slideshow interval to: $seconds seconds")
        viewModelScope.launch {
            preferencesRepository.setSlideshowInterval(seconds)
        }
    }

    fun setRandomOrder(random: Boolean) {
        Timber.d("Setting random order: $random")
        viewModelScope.launch {
            preferencesRepository.setRandomOrder(random)
        }
    }

    fun setLoopSlideshow(loop: Boolean) {
        Timber.d("Setting loop slideshow: $loop")
        viewModelScope.launch {
            preferencesRepository.setLoopSlideshow(loop)
        }
    }

    fun setEnableTouchZones(enable: Boolean) {
        Timber.d("Setting enable touch zones: $enable")
        viewModelScope.launch {
            preferencesRepository.setEnableTouchZones(enable)
        }
    }

    fun setShowZoneOverlay(show: Boolean) {
        Timber.d("Setting show zone overlay: $show")
        viewModelScope.launch {
            preferencesRepository.setShowZoneOverlay(show)
        }
    }

    fun setResumeFromLastPosition(resume: Boolean) {
        Timber.d("Setting resume from last position: $resume")
        viewModelScope.launch {
            preferencesRepository.setResumeFromLastPosition(resume)
        }
    }

    fun setAutoPlayVideos(autoPlay: Boolean) {
        Timber.d("Setting auto play videos: $autoPlay")
        viewModelScope.launch {
            preferencesRepository.setAutoPlayVideos(autoPlay)
        }
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
