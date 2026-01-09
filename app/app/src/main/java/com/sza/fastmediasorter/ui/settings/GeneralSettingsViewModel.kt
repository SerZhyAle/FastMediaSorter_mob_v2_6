package com.sza.fastmediasorter.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.data.cache.UnifiedFileCache
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.domain.usecase.debug.GenerateStressDataUseCase
import com.sza.fastmediasorter.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for General Settings.
 * Handles language, theme, display mode, and cache management.
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val unifiedFileCache: UnifiedFileCache,
    private val generateStressDataUseCase: GenerateStressDataUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("GeneralSettingsViewModel initialized")
        loadSettings()
        loadCacheSize()
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
                preferencesRepository.preventSleepDuringPlayback,
                preferencesRepository.defaultUsername,
                preferencesRepository.defaultPassword
            ) { values ->
                GeneralSettingsUiState(
                    language = values[0] as String,
                    theme = values[1] as String,
                    displayMode = values[2] as String,
                    showHiddenFiles = values[3] as Boolean,
                    confirmDelete = values[4] as Boolean,
                    confirmMove = values[5] as Boolean,
                    preventSleepDuringPlayback = values[6] as Boolean,
                    defaultUsername = values[7] as String,
                    defaultPassword = values[8] as String,
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
        // Apply locale change - this will automatically recreate activities
        LocaleHelper.setLocale(langCode)
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

    fun setDefaultUsername(username: String) {
        Timber.d("Setting default username: $username")
        viewModelScope.launch {
            preferencesRepository.setDefaultUsername(username)
        }
    }

    fun setDefaultPassword(password: String) {
        Timber.d("Setting default password: ***")
        viewModelScope.launch {
            preferencesRepository.setDefaultPassword(password)
        }
    }

    fun clearCache() {
        Timber.d("Clearing cache...")
        viewModelScope.launch {
            // Clear Glide cache (disk)
            withContext(Dispatchers.IO) {
                Glide.get(context).clearDiskCache()
            }
            // Clear Glide memory cache (must be on main thread)
            withContext(Dispatchers.Main) {
                Glide.get(context).clearMemory()
            }
            // Clear unified file cache
            unifiedFileCache.clearAll()

            Timber.d("Cache cleared successfully")
            _uiState.update { it.copy(cacheSizeDisplay = "0 MB") }
        }
    }

    private fun loadCacheSize() {
        viewModelScope.launch {
            val unifiedCacheSize = unifiedFileCache.getCacheSize()
            val totalSizeMB = unifiedCacheSize / 1024.0 / 1024.0
            val displaySize = if (totalSizeMB >= 1) {
                String.format("%.1f MB", totalSizeMB)
            } else {
                String.format("%.0f KB", unifiedCacheSize / 1024.0)
            }
            _uiState.update { it.copy(cacheSizeDisplay = displaySize) }
        }
    }

    fun generateStressTestData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingData = true) }
            val dir = context.getExternalFilesDir(null)?.resolve("StressTestResult")
            if (dir != null) {
                Timber.d("Generating stress test data in: ${dir.absolutePath}")
                val result = generateStressDataUseCase(dir.absolutePath, 10000) { progress ->
                    if (progress % 1000 == 0) Timber.d("Generated $progress files...")
                }
                result.onSuccess {
                    Timber.d("Stress test data generation complete")
                }
                result.onError { msg, e, _ ->
                    Timber.e(e, "Stress test data generation failed: $msg")
                }
            }
            _uiState.update { it.copy(isGeneratingData = false) }
        }
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
    val defaultUsername: String = "",
    val defaultPassword: String = "",
    val cacheSizeDisplay: String = "0 MB",
    val isGeneratingData: Boolean = false
)
