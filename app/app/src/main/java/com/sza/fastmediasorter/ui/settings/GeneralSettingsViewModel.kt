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
 * ViewModel for General Settings.
 * Handles language, theme, display mode, and cache management.
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("GeneralSettingsViewModel initialized")
        loadSettings()
    }

    private fun loadSettings() {
        // TODO: Load from PreferencesRepository
        _uiState.update { 
            it.copy(
                language = "en",
                theme = "system",
                displayMode = "grid",
                showHiddenFiles = false,
                confirmDelete = true,
                confirmMove = false,
                preventSleepDuringPlayback = true,
                cacheSizeDisplay = "Calculating..."
            )
        }
    }

    fun setLanguage(langCode: String) {
        Timber.d("Setting language to: $langCode")
        _uiState.update { it.copy(language = langCode) }
        // TODO: Apply locale change via LocaleHelper and recreate activities
    }

    fun setTheme(theme: String) {
        Timber.d("Setting theme to: $theme")
        _uiState.update { it.copy(theme = theme) }
        // TODO: Apply theme via AppCompatDelegate
    }

    fun setDisplayMode(mode: String) {
        Timber.d("Setting display mode to: $mode")
        _uiState.update { it.copy(displayMode = mode) }
        // TODO: Save to preferences
    }

    fun setShowHiddenFiles(show: Boolean) {
        Timber.d("Setting show hidden files: $show")
        _uiState.update { it.copy(showHiddenFiles = show) }
        // TODO: Save to preferences
    }

    fun setConfirmDelete(confirm: Boolean) {
        Timber.d("Setting confirm delete: $confirm")
        _uiState.update { it.copy(confirmDelete = confirm) }
        // TODO: Save to preferences
    }

    fun setConfirmMove(confirm: Boolean) {
        Timber.d("Setting confirm move: $confirm")
        _uiState.update { it.copy(confirmMove = confirm) }
        // TODO: Save to preferences
    }

    fun setPreventSleepDuringPlayback(prevent: Boolean) {
        Timber.d("Setting prevent sleep during playback: $prevent")
        _uiState.update { it.copy(preventSleepDuringPlayback = prevent) }
        // TODO: Save to preferences
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
