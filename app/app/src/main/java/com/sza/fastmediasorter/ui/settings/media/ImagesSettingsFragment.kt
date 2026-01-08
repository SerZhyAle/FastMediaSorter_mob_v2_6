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
import com.sza.fastmediasorter.databinding.FragmentSettingsImagesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for Image media type.
 * Controls: support images, thumbnail quality, auto-rotate, EXIF, JPEG quality.
 */
@AndroidEntryPoint
class ImagesSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsImagesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaSettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("ImagesSettingsFragment created")
        
        setupDropdowns()
        setupListeners()
        observeState()
    }

    private fun setupDropdowns() {
        val qualities = arrayOf(
            getString(R.string.quality_low),
            getString(R.string.quality_medium),
            getString(R.string.quality_high)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, qualities)
        binding.dropdownThumbnailQuality.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.switchSupportImages.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportImages(isChecked)
            updateDependentViewsVisibility(isChecked)
        }
        
        binding.dropdownThumbnailQuality.setOnItemClickListener { _, _, position, _ ->
            val quality = when (position) {
                0 -> "low"
                1 -> "medium"
                2 -> "high"
                else -> "medium"
            }
            viewModel.setThumbnailQuality(quality)
        }
        
        binding.switchAutoRotate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoRotateImages(isChecked)
        }
        
        binding.switchShowExif.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowExifData(isChecked)
        }
        
        binding.sliderJpegQuality.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val quality = value.toInt()
                binding.tvJpegQualityValue.text = "$quality%"
                viewModel.setJpegQuality(quality)
            }
        }
        
        binding.switchLoadFullSize.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLoadFullSizeImages(isChecked)
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
        binding.switchSupportImages.isChecked = state.supportImages
        updateDependentViewsVisibility(state.supportImages)
        
        val qualityPosition = when (state.thumbnailQuality) {
            "low" -> 0
            "medium" -> 1
            "high" -> 2
            else -> 1
        }
        if (binding.dropdownThumbnailQuality.adapter != null && qualityPosition < binding.dropdownThumbnailQuality.adapter.count) {
            binding.dropdownThumbnailQuality.setText(
                binding.dropdownThumbnailQuality.adapter.getItem(qualityPosition).toString(),
                false
            )
        }
        
        binding.switchAutoRotate.isChecked = state.autoRotateImages
        binding.switchShowExif.isChecked = state.showExifData
        binding.sliderJpegQuality.value = state.jpegQuality.toFloat()
        binding.tvJpegQualityValue.text = "${state.jpegQuality}%"
        binding.switchLoadFullSize.isChecked = state.loadFullSizeImages
    }

    private fun updateDependentViewsVisibility(supportImages: Boolean) {
        val visibility = if (supportImages) View.VISIBLE else View.GONE
        binding.layoutImageOptions.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
