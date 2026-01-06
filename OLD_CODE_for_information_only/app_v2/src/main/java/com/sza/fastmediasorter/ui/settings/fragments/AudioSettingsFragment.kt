package com.sza.fastmediasorter.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.databinding.FragmentSettingsAudioBinding
import com.sza.fastmediasorter.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

class AudioSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsAudioBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by activityViewModels()
    private var isUpdatingFromSettings = false

    companion object {
        private const val MB_TO_BYTES = 1024L * 1024L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        // Support Audio
        binding.switchSupportAudio.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(supportAudio = isChecked))
            }
        }

        // Search audio covers online
        binding.switchSearchAudioCoversOnline.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(searchAudioCoversOnline = isChecked))
                binding.layoutSearchCoversOnlyWifi.isVisible = isChecked
            }
        }

        // Search covers only on WiFi
        binding.switchSearchCoversOnlyWifi.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(searchAudioCoversOnlyOnWifi = isChecked))
            }
        }

        // Audio size limits
        binding.etAudioSizeMin.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isUpdatingFromSettings && !s.isNullOrBlank()) {
                    val minMb = s.toString().toLongOrNull() ?: 0L
                    val current = viewModel.settings.value
                    viewModel.updateSettings(current.copy(audioSizeMin = minMb * MB_TO_BYTES))
                }
            }
        })

        binding.etAudioSizeMax.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isUpdatingFromSettings && !s.isNullOrBlank()) {
                    val maxMb = s.toString().toLongOrNull() ?: 0L
                    val current = viewModel.settings.value
                    viewModel.updateSettings(current.copy(audioSizeMax = maxMb * MB_TO_BYTES))
                }
            }
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    isUpdatingFromSettings = true
                    
                    binding.switchSupportAudio.isChecked = settings.supportAudio
                    binding.switchSearchAudioCoversOnline.isChecked = settings.searchAudioCoversOnline
                    binding.switchSearchCoversOnlyWifi.isChecked = settings.searchAudioCoversOnlyOnWifi
                    
                    binding.layoutSearchCoversOnlyWifi.isVisible = settings.searchAudioCoversOnline
                    
                    val minMb = settings.audioSizeMin / MB_TO_BYTES
                    val maxMb = settings.audioSizeMax / MB_TO_BYTES
                    
                    if (binding.etAudioSizeMin.text.toString() != minMb.toString()) {
                        binding.etAudioSizeMin.setText(minMb.toString())
                    }
                    if (binding.etAudioSizeMax.text.toString() != maxMb.toString()) {
                        binding.etAudioSizeMax.setText(maxMb.toString())
                    }
                    
                    isUpdatingFromSettings = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
