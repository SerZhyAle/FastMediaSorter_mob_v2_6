package com.sza.fastmediasorter.ui.settings.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sza.fastmediasorter.databinding.FragmentSettingsDocumentsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment for Document media types.
 * Controls: Text, PDF, EPUB support.
 */
@AndroidEntryPoint
class DocumentsSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsDocumentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.switchSupportText.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchSupportPdf.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
        
        binding.switchSupportEpub.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update settings
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
