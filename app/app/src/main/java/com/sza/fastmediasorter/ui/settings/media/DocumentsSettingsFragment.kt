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
import com.sza.fastmediasorter.databinding.FragmentSettingsDocumentsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for Document media types.
 * Controls: Text, PDF, EPUB support, render quality, encoding, font settings.
 */
@AndroidEntryPoint
class DocumentsSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsDocumentsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaSettingsViewModel by activityViewModels()

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
        Timber.d("DocumentsSettingsFragment created")
        
        setupDropdowns()
        setupListeners()
        observeState()
    }

    private fun setupDropdowns() {
        // PDF render quality dropdown
        val pdfQualities = arrayOf(
            getString(R.string.quality_low),
            getString(R.string.quality_medium),
            getString(R.string.quality_high)
        )
        val pdfQualityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, pdfQualities)
        binding.dropdownPdfQuality.setAdapter(pdfQualityAdapter)
        
        // Text encoding dropdown
        val encodings = arrayOf(
            getString(R.string.encoding_auto),
            "UTF-8",
            "UTF-16",
            "CP1251",
            "ISO-8859-1"
        )
        val encodingAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, encodings)
        binding.dropdownTextEncoding.setAdapter(encodingAdapter)
        
        // EPUB font family dropdown
        val fontFamilies = arrayOf(
            getString(R.string.font_serif),
            getString(R.string.font_sans),
            getString(R.string.font_mono)
        )
        val fontAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fontFamilies)
        binding.dropdownEpubFont.setAdapter(fontAdapter)
    }

    private fun setupListeners() {
        binding.switchSupportText.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportText(isChecked)
            binding.layoutTextOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.switchSupportPdf.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportPdf(isChecked)
            binding.layoutPdfOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.switchSupportEpub.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSupportEpub(isChecked)
            binding.layoutEpubOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.sliderPdfPageCache.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pages = value.toInt()
                binding.tvPdfPageCacheValue.text = getString(R.string.pages_format, pages)
                viewModel.setPdfPageCache(pages)
            }
        }
        
        binding.dropdownPdfQuality.setOnItemClickListener { _, _, position, _ ->
            val quality = when (position) {
                0 -> "low"
                1 -> "medium"
                2 -> "high"
                else -> "high"
            }
            viewModel.setPdfRenderQuality(quality)
        }
        
        binding.dropdownTextEncoding.setOnItemClickListener { _, _, position, _ ->
            val encoding = when (position) {
                0 -> "auto"
                1 -> "utf-8"
                2 -> "utf-16"
                3 -> "cp1251"
                4 -> "iso-8859-1"
                else -> "auto"
            }
            viewModel.setTextEncoding(encoding)
        }
        
        binding.dropdownEpubFont.setOnItemClickListener { _, _, position, _ ->
            val family = when (position) {
                0 -> "serif"
                1 -> "sans"
                2 -> "mono"
                else -> "serif"
            }
            viewModel.setEpubFontFamily(family)
        }
        
        binding.sliderEpubFontSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val size = value.toInt()
                binding.tvEpubFontSizeValue.text = getString(R.string.pixels_format, size)
                viewModel.setEpubFontSize(size)
            }
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
        binding.switchSupportText.isChecked = state.supportText
        binding.layoutTextOptions.visibility = if (state.supportText) View.VISIBLE else View.GONE
        
        binding.switchSupportPdf.isChecked = state.supportPdf
        binding.layoutPdfOptions.visibility = if (state.supportPdf) View.VISIBLE else View.GONE
        
        binding.switchSupportEpub.isChecked = state.supportEpub
        binding.layoutEpubOptions.visibility = if (state.supportEpub) View.VISIBLE else View.GONE
        
        binding.sliderPdfPageCache.value = state.pdfPageCache.toFloat()
        binding.tvPdfPageCacheValue.text = getString(R.string.pages_format, state.pdfPageCache)
        
        val pdfQualityPosition = when (state.pdfRenderQuality) {
            "low" -> 0
            "medium" -> 1
            "high" -> 2
            else -> 2
        }
        if (binding.dropdownPdfQuality.adapter != null && pdfQualityPosition < binding.dropdownPdfQuality.adapter.count) {
            binding.dropdownPdfQuality.setText(
                binding.dropdownPdfQuality.adapter.getItem(pdfQualityPosition).toString(),
                false
            )
        }
        
        val encodingPosition = when (state.textEncoding) {
            "auto" -> 0
            "utf-8" -> 1
            "utf-16" -> 2
            "cp1251" -> 3
            "iso-8859-1" -> 4
            else -> 0
        }
        if (binding.dropdownTextEncoding.adapter != null && encodingPosition < binding.dropdownTextEncoding.adapter.count) {
            binding.dropdownTextEncoding.setText(
                binding.dropdownTextEncoding.adapter.getItem(encodingPosition).toString(),
                false
            )
        }
        
        val fontPosition = when (state.epubFontFamily) {
            "serif" -> 0
            "sans" -> 1
            "mono" -> 2
            else -> 0
        }
        if (binding.dropdownEpubFont.adapter != null && fontPosition < binding.dropdownEpubFont.adapter.count) {
            binding.dropdownEpubFont.setText(
                binding.dropdownEpubFont.adapter.getItem(fontPosition).toString(),
                false
            )
        }
        
        binding.sliderEpubFontSize.value = state.epubFontSize.toFloat()
        binding.tvEpubFontSizeValue.text = getString(R.string.pixels_format, state.epubFontSize)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
