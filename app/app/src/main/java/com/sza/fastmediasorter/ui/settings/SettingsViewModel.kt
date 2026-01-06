package com.sza.fastmediasorter.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity.
 * Manages global settings state and coordinates with PreferencesRepository.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("SettingsViewModel initialized")
    }

    // Future: Add methods for saving settings, theme changes, etc.
}

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val currentTab: Int = 0
)
