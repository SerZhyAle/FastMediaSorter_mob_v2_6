package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.databinding.FragmentSettingsPlaybackBinding
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Playback settings fragment.
 * Contains: Slideshow, Touch Zones, Video Playback settings.
 */
@AndroidEntryPoint
class PlaybackSettingsFragment : BaseFragment<FragmentSettingsPlaybackBinding>() {

    private val viewModel: PlaybackSettingsViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsPlaybackBinding =
        FragmentSettingsPlaybackBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("PlaybackSettingsFragment created")
        
        setupSlideshowSection()
        setupTouchZonesSection()
        setupVideoPlaybackSection()
        observeState()
    }

    private fun setupSlideshowSection() {
        binding.sliderSlideshowInterval.addOnChangeListener { _, value, _ ->
            viewModel.setSlideshowInterval(value.toInt())
            binding.tvSlideshowIntervalValue.text = "${value.toInt()} sec"
        }
        
        binding.switchRandomOrder.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setRandomOrder(isChecked)
        }
        
        binding.switchLoopSlideshow.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLoopSlideshow(isChecked)
        }
    }

    private fun setupTouchZonesSection() {
        binding.switchEnableTouchZones.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEnableTouchZones(isChecked)
            binding.layoutTouchZoneOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.switchShowZoneOverlay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowZoneOverlay(isChecked)
        }
    }

    private fun setupVideoPlaybackSection() {
        binding.switchResumePlayback.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setResumeFromLastPosition(isChecked)
        }
        
        binding.switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoPlayVideos(isChecked)
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

    private fun updateUi(state: PlaybackSettingsUiState) {
        // Slideshow settings
        binding.sliderSlideshowInterval.value = state.slideshowInterval.toFloat()
        binding.tvSlideshowIntervalValue.text = "${state.slideshowInterval} sec"
        binding.switchRandomOrder.isChecked = state.randomOrder
        binding.switchLoopSlideshow.isChecked = state.loopSlideshow

        // Touch zones
        binding.switchEnableTouchZones.isChecked = state.enableTouchZones
        binding.layoutTouchZoneOptions.visibility = if (state.enableTouchZones) View.VISIBLE else View.GONE
        binding.switchShowZoneOverlay.isChecked = state.showZoneOverlay

        // Video playback
        binding.switchResumePlayback.isChecked = state.resumeFromLastPosition
        binding.switchAutoPlay.isChecked = state.autoPlayVideos
    }
}
