package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sza.fastmediasorter.databinding.FragmentSettingsVideoBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment for Video media type.
 * Controls: support video, size limits, thumbnails.
 */
@AndroidEntryPoint
class VideoSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsVideoBinding? = null
    private val binding get() = _binding!!

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
        setupListeners()
    }

    private fun setupListeners() {
        binding.switchSupportVideo.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchShowThumbnails.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
