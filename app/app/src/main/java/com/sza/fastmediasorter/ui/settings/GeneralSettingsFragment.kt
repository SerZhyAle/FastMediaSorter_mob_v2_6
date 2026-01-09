package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.BuildConfig
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsGeneralBinding
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * General settings fragment.
 * Contains: Language, Theme, Display Mode, Grid Columns, Cache management.
 */
@AndroidEntryPoint
class GeneralSettingsFragment : BaseFragment<FragmentSettingsGeneralBinding>() {

    private val viewModel: GeneralSettingsViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsGeneralBinding =
        FragmentSettingsGeneralBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("GeneralSettingsFragment created")

        setupLanguageDropdown()
        setupThemeRadioGroup()
        setupDisplayModeDropdown()
        setupSwitches()
        setupNetworkCredentials()

        setupCacheSection()
        setupDeveloperOptions()
        observeState()
    }

    private fun setupLanguageDropdown() {
        val languages = arrayOf("English", "Русский", "Українська")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languages)
        binding.dropdownLanguage.setAdapter(adapter)
        binding.dropdownLanguage.setOnItemClickListener { _, _, position, _ ->
            val langCode = when (position) {
                0 -> "en"
                1 -> "ru"
                2 -> "uk"
                else -> "en"
            }
            viewModel.setLanguage(langCode)
        }
    }

    private fun setupThemeRadioGroup() {
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioLight -> "light"
                R.id.radioDark -> "dark"
                R.id.radioSystem -> "system"
                else -> "system"
            }
            viewModel.setTheme(theme)
        }
    }

    private fun setupDisplayModeDropdown() {
        val displayModes = arrayOf("Grid", "List", "Auto")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayModes)
        binding.dropdownDisplayMode.setAdapter(adapter)
        binding.dropdownDisplayMode.setOnItemClickListener { _, _, position, _ ->
            val mode = when (position) {
                0 -> "grid"
                1 -> "list"
                2 -> "auto"
                else -> "grid"
            }
            viewModel.setDisplayMode(mode)
        }
    }

    private fun setupSwitches() {
        binding.switchShowHiddenFiles.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowHiddenFiles(isChecked)
        }

        binding.switchConfirmDelete.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setConfirmDelete(isChecked)
        }

        binding.switchConfirmMove.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setConfirmMove(isChecked)
        }

        binding.switchPreventSleep.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPreventSleepDuringPlayback(isChecked)
        }
    }

    private fun setupNetworkCredentials() {
        binding.etDefaultUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val username = binding.etDefaultUsername.text.toString()
                viewModel.setDefaultUsername(username)
            }
        }

        binding.etDefaultPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.etDefaultPassword.text.toString()
                viewModel.setDefaultPassword(password)
            }
        }
    }

    private fun setupCacheSection() {
        binding.btnClearCache.setOnClickListener {
            viewModel.clearCache()
        }
    }

    private fun setupDeveloperOptions() {
        if (BuildConfig.DEBUG) {
            binding.layoutDeveloperOptions.visibility = View.VISIBLE
            binding.btnGenerateStressData.setOnClickListener {
                viewModel.generateStressTestData()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: GeneralSettingsUiState) {
        // Update language dropdown
        val langPosition = when (state.language) {
            "en" -> 0
            "ru" -> 1
            "uk" -> 2
            else -> 0
        }
        binding.dropdownLanguage.setText(binding.dropdownLanguage.adapter.getItem(langPosition).toString(), false)

        // Update theme radio
        val themeId = when (state.theme) {
            "light" -> R.id.radioLight
            "dark" -> R.id.radioDark
            else -> R.id.radioSystem
        }
        binding.radioGroupTheme.check(themeId)

        // Update display mode dropdown
        val displayPosition = when (state.displayMode) {
            "grid" -> 0
            "list" -> 1
            "auto" -> 2
            else -> 0
        }
        binding.dropdownDisplayMode.setText(binding.dropdownDisplayMode.adapter.getItem(displayPosition).toString(), false)

        // Update switches
        binding.switchShowHiddenFiles.isChecked = state.showHiddenFiles
        binding.switchConfirmDelete.isChecked = state.confirmDelete
        binding.switchConfirmMove.isChecked = state.confirmMove
        binding.switchPreventSleep.isChecked = state.preventSleepDuringPlayback

        // Update network credentials
        if (binding.etDefaultUsername.text.toString() != state.defaultUsername) {
            binding.etDefaultUsername.setText(state.defaultUsername)
        }
        if (binding.etDefaultPassword.text.toString() != state.defaultPassword) {
            binding.etDefaultPassword.setText(state.defaultPassword)
        }

        // Update cache size display
        binding.tvCacheSize.text = state.cacheSizeDisplay

        // Update generator button state
        binding.btnGenerateStressData.isEnabled = !state.isGeneratingData
        binding.btnGenerateStressData.text = if (state.isGeneratingData) {
            "Generating..."
        } else {
            "Generate 10k Files (Stress Test)"
        }
    }
}
