package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sza.fastmediasorter.databinding.FragmentSettingsImagesBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment for Image media type.
 * Controls: support images, size limits, full size loading.
 */
@AndroidEntryPoint
class ImagesSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsImagesBinding? = null
    private val binding get() = _binding!!

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
        setupListeners()
    }

    private fun setupListeners() {
        binding.switchSupportImages.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchLoadFullSize.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
