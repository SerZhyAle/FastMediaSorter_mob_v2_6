package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sza.fastmediasorter.databinding.FragmentSettingsOtherBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment for Other media types.
 * Controls: GIF support, Translation, Google Lens.
 */
@AndroidEntryPoint
class OtherMediaSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsOtherBinding? = null
    private val binding get() = _binding!!

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
        setupListeners()
    }

    private fun setupListeners() {
        binding.switchSupportGif.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchEnableTranslation.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchEnableGoogleLens.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
