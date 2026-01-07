package com.sza.fastmediasorter.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for General Settings.
 * Handles language, theme, display mode, and cache management.
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("GeneralSettingsViewModel initialized")
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                preferencesRepository.language,
                preferencesRepository.theme,
                preferencesRepository.displayMode,
                preferencesRepository.showHiddenFiles,
                preferencesRepository.confirmDelete,
                preferencesRepository.confirmMove,
                preferencesRepository.preventSleepDuringPlayback
            ) { values ->
                GeneralSettingsUiState(
                    language = values[0] as String,
                    theme = values[1] as String,
                    displayMode = values[2] as String,
                    showHiddenFiles = values[3] as Boolean,
                    confirmDelete = values[4] as Boolean,
                    confirmMove = values[5] as Boolean,
                    preventSleepDuringPlayback = values[6] as Boolean,
                    cacheSizeDisplay = "Calculating..."
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setLanguage(langCode: String) {
        Timber.d("Setting language to: $langCode")
        viewModelScope.launch {
            preferencesRepository.setLanguage(langCode)
        }
        // TODO: Apply locale change via LocaleHelper and recreate activities
    }

    fun setTheme(theme: String) {
        Timber.d("Setting theme to: $theme")
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
        // Apply theme immediately
        applyTheme(theme)
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setDisplayMode(mode: String) {
        Timber.d("Setting display mode to: $mode")
        viewModelScope.launch {
            preferencesRepository.setDisplayMode(mode)
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        Timber.d("Setting show hidden files: $show")
        viewModelScope.launch {
            preferencesRepository.setShowHiddenFiles(show)
        }
    }

    fun setConfirmDelete(confirm: Boolean) {
        Timber.d("Setting confirm delete: $confirm")
        viewModelScope.launch {
            preferencesRepository.setConfirmDelete(confirm)
        }
    }

    fun setConfirmMove(confirm: Boolean) {
        Timber.d("Setting confirm move: $confirm")
        viewModelScope.launch {
            preferencesRepository.setConfirmMove(confirm)
        }
    }

    fun setPreventSleepDuringPlayback(prevent: Boolean) {
        Timber.d("Setting prevent sleep during playback: $prevent")
        viewModelScope.launch {
            preferencesRepository.setPreventSleepDuringPlayback(prevent)
        }
    }

    fun clearCache() {
        Timber.d("Clearing cache...")
        // TODO: Implement cache clearing via CacheManager
        _uiState.update { it.copy(cacheSizeDisplay = "0 MB") }
    }
}

/**
 * UI state for General Settings.
 */
data class GeneralSettingsUiState(
    val language: String = "en",
    val theme: String = "system",
    val displayMode: String = "grid",
    val showHiddenFiles: Boolean = false,
    val confirmDelete: Boolean = true,
    val confirmMove: Boolean = false,
    val preventSleepDuringPlayback: Boolean = true,
    val cacheSizeDisplay: String = "0 MB"
)
