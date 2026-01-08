package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsAudioBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for Audio media type.
 * Controls: support audio, waveform style, background playback, audio focus.
 */
@AndroidEntryPoint
class AudioSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsAudioBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaSettingsViewModel by activityViewModels()

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
        Timber.d("AudioSettingsFragment created")
        
        setupDropdowns()
        setupListeners()
        observeState()
    }

    private fun setupDropdowns() {
        // Waveform style dropdown
        val waveformStyles = arrayOf(
            getString(R.string.waveform_none),
            getString(R.string.waveform_bars),
            getString(R.string.waveform_line)
        )
        val waveformAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, waveformStyles)
        binding.dropdownWaveformStyle.setAdapter(waveformAdapter)
        
        // Audio focus handling dropdown
        val focusModes = arrayOf(
            getString(R.string.audio_focus_duck),
            getString(R.string.audio_focus_pause),
            getString(R.string.audio_focus_ignore)
        )
        val focusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, focusModes)
        binding.dropdownAudioFocus.setAdapter(focusAdapter)
    }

    private fun setupListeners() {
        binding.switchSupportAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportAudio(isChecked)
            updateDependentViewsVisibility(isChecked)
        }
        
        binding.dropdownWaveformStyle.setOnItemClickListener { _, _, position, _ ->
            val style = when (position) {
                0 -> "none"
                1 -> "bars"
                2 -> "line"
                else -> "bars"
            }
            viewModel.setWaveformStyle(style)
        }
        
        binding.switchBackgroundPlayback.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBackgroundPlayback(isChecked)
        }
        
        binding.dropdownAudioFocus.setOnItemClickListener { _, _, position, _ ->
            val mode = when (position) {
                0 -> "duck"
                1 -> "pause"
                2 -> "ignore"
                else -> "duck"
            }
            viewModel.setAudioFocusHandling(mode)
        }
        
        binding.switchSearchCovers.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSearchAlbumCovers(isChecked)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: MediaSettingsUiState) {
        binding.switchSupportAudio.isChecked = state.supportAudio
        updateDependentViewsVisibility(state.supportAudio)
        
        val waveformPosition = when (state.waveformStyle) {
            "none" -> 0
            "bars" -> 1
            "line" -> 2
            else -> 1
        }
        if (binding.dropdownWaveformStyle.adapter != null && waveformPosition < binding.dropdownWaveformStyle.adapter.count) {
            binding.dropdownWaveformStyle.setText(
                binding.dropdownWaveformStyle.adapter.getItem(waveformPosition).toString(),
                false
            )
        }
        
        binding.switchBackgroundPlayback.isChecked = state.backgroundPlayback
        
        val focusPosition = when (state.audioFocusHandling) {
            "duck" -> 0
            "pause" -> 1
            "ignore" -> 2
            else -> 0
        }
        if (binding.dropdownAudioFocus.adapter != null && focusPosition < binding.dropdownAudioFocus.adapter.count) {
            binding.dropdownAudioFocus.setText(
                binding.dropdownAudioFocus.adapter.getItem(focusPosition).toString(),
                false
            )
        }
        
        binding.switchSearchCovers.isChecked = state.searchAlbumCovers
    }

    private fun updateDependentViewsVisibility(supportAudio: Boolean) {
        val visibility = if (supportAudio) View.VISIBLE else View.GONE
        binding.layoutAudioOptions.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
