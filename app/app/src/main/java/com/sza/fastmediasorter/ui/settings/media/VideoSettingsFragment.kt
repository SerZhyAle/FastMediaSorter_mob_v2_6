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
import com.sza.fastmediasorter.databinding.FragmentSettingsVideoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for Video media type.
 * Controls: support video, quality, hardware acceleration, seek increment.
 */
@AndroidEntryPoint
class VideoSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsVideoBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaSettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("VideoSettingsFragment created")
        
        setupDropdowns()
        setupListeners()
        observeState()
    }

    private fun setupDropdowns() {
        val qualities = arrayOf(
            getString(R.string.quality_auto),
            getString(R.string.quality_low),
            getString(R.string.quality_medium),
            getString(R.string.quality_high),
            getString(R.string.quality_best)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, qualities)
        binding.dropdownVideoQuality.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.switchSupportVideo.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportVideo(isChecked)
            updateDependentViewsVisibility(isChecked)
        }
        
        binding.dropdownVideoQuality.setOnItemClickListener { _, _, position, _ ->
            val quality = when (position) {
                0 -> "auto"
                1 -> "low"
                2 -> "medium"
                3 -> "high"
                4 -> "best"
                else -> "auto"
            }
            viewModel.setVideoQuality(quality)
        }
        
        binding.switchHardwareAcceleration.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setHardwareAcceleration(isChecked)
        }
        
        binding.sliderSeekIncrement.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val seconds = value.toInt()
                binding.tvSeekIncrementValue.text = getString(R.string.seconds_format, seconds)
                viewModel.setSeekIncrement(seconds)
            }
        }
        
        binding.sliderPreviewDuration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val seconds = value.toInt()
                binding.tvPreviewDurationValue.text = getString(R.string.seconds_format, seconds)
                viewModel.setPreviewDuration(seconds)
            }
        }
        
        binding.switchShowThumbnails.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowVideoThumbnails(isChecked)
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
        binding.switchSupportVideo.isChecked = state.supportVideo
        updateDependentViewsVisibility(state.supportVideo)
        
        val qualityPosition = when (state.videoQuality) {
            "auto" -> 0
            "low" -> 1
            "medium" -> 2
            "high" -> 3
            "best" -> 4
            else -> 0
        }
        if (binding.dropdownVideoQuality.adapter != null && qualityPosition < binding.dropdownVideoQuality.adapter.count) {
            binding.dropdownVideoQuality.setText(
                binding.dropdownVideoQuality.adapter.getItem(qualityPosition).toString(),
                false
            )
        }
        
        binding.switchHardwareAcceleration.isChecked = state.hardwareAcceleration
        binding.sliderSeekIncrement.value = state.seekIncrement.toFloat()
        binding.tvSeekIncrementValue.text = getString(R.string.seconds_format, state.seekIncrement)
        binding.sliderPreviewDuration.value = state.previewDuration.toFloat()
        binding.tvPreviewDurationValue.text = getString(R.string.seconds_format, state.previewDuration)
        binding.switchShowThumbnails.isChecked = state.showVideoThumbnails
    }

    private fun updateDependentViewsVisibility(supportVideo: Boolean) {
        val visibility = if (supportVideo) View.VISIBLE else View.GONE
        binding.layoutVideoOptions.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
