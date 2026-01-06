package com.sza.fastmediasorter.ui.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.databinding.FragmentSettingsDocumentsBinding
import com.sza.fastmediasorter.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

class DocumentsSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsDocumentsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by activityViewModels()
    private var isUpdatingFromSettings = false

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
        setupViews()
        observeData()
    }

    private fun setupViews() {
        // Support Text
        binding.switchSupportText.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(supportText = isChecked))
                binding.layoutShowTextLineNumbers.isVisible = isChecked
            }
        }

        // Show Text Line Numbers
        binding.switchShowTextLineNumbers.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(showTextLineNumbers = isChecked))
            }
        }

        // Support PDF
        binding.switchSupportPdf.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(supportPdf = isChecked))
                binding.layoutShowPdfThumbnails.isVisible = isChecked
            }
        }

        // Show PDF Thumbnails
        binding.switchShowPdfThumbnails.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(showPdfThumbnails = isChecked))
            }
        }

        // Support EPUB
        binding.switchSupportEpub.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSettings) {
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(supportEpub = isChecked))
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    isUpdatingFromSettings = true
                    
                    binding.switchSupportText.isChecked = settings.supportText
                    binding.switchShowTextLineNumbers.isChecked = settings.showTextLineNumbers
                    binding.switchSupportPdf.isChecked = settings.supportPdf
                    binding.switchSupportEpub.isChecked = settings.supportEpub
                    binding.switchShowPdfThumbnails.isChecked = settings.showPdfThumbnails
                    
                    binding.layoutShowTextLineNumbers.isVisible = settings.supportText
                    binding.layoutShowPdfThumbnails.isVisible = settings.supportPdf
                    
                    isUpdatingFromSettings = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
