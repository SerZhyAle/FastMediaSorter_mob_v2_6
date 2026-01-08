package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sza.fastmediasorter.databinding.FragmentSettingsAudioBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment for Audio media type.
 * Controls: support audio, size limits, album covers search.
 */
@AndroidEntryPoint
class AudioSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsAudioBinding? = null
    private val binding get() = _binding!!

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
        setupListeners()
    }

    private fun setupListeners() {
        binding.switchSupportAudio.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchSearchCovers.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
