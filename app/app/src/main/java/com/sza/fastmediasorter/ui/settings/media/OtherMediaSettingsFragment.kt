package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsOtherBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for Other media types.
 * Controls: GIF support, frame rate limit, Translation, Google Lens.
 */
@AndroidEntryPoint
class OtherMediaSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsOtherBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaSettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("OtherMediaSettingsFragment created")
        
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.switchSupportGif.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportGif(isChecked)
            binding.layoutGifOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.sliderGifFrameRate.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val fps = value.toInt()
                binding.tvGifFrameRateValue.text = if (fps == 0) {
                    getString(R.string.no_limit)
                } else {
                    getString(R.string.fps_format, fps)
                }
                viewModel.setGifFrameRateLimit(fps)
            }
        }
        
        binding.switchEnableTranslation.setOnCheckedChangeListener { _, isChecked ->
            // Translation is a feature toggle, not stored in preferences for now
            Timber.d("Enable translation: $isChecked")
        }
        
        binding.switchEnableGoogleLens.setOnCheckedChangeListener { _, isChecked ->
            // Google Lens is a feature toggle, not stored in preferences for now
            Timber.d("Enable Google Lens: $isChecked")
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
        binding.switchSupportGif.isChecked = state.supportGif
        binding.layoutGifOptions.visibility = if (state.supportGif) View.VISIBLE else View.GONE
        
        binding.sliderGifFrameRate.value = state.gifFrameRateLimit.toFloat()
        binding.tvGifFrameRateValue.text = if (state.gifFrameRateLimit == 0) {
            getString(R.string.no_limit)
        } else {
            getString(R.string.fps_format, state.gifFrameRateLimit)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
