package com.sza.fastmediasorter.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sza.fastmediasorter.databinding.DialogFilterOptionsBinding
import com.sza.fastmediasorter.domain.model.MediaType

/**
 * Bottom sheet dialog for selecting media type filters.
 * Allows filtering browse results by media type (Image, Video, Audio, etc.)
 */
class FilterOptionsDialog : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "FilterOptionsDialog"
        private const val ARG_SELECTED_TYPES = "selected_types"

        fun newInstance(selectedTypes: Set<MediaType>): FilterOptionsDialog {
            return FilterOptionsDialog().apply {
                arguments = Bundle().apply {
                    putStringArray(ARG_SELECTED_TYPES, selectedTypes.map { it.name }.toTypedArray())
                }
            }
        }
    }

    private var _binding: DialogFilterOptionsBinding? = null
    private val binding get() = _binding!!

    var onFiltersApplied: ((Set<MediaType>) -> Unit)? = null

    private val selectedTypes = mutableSetOf<MediaType>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore selected types from arguments
        arguments?.getStringArray(ARG_SELECTED_TYPES)?.forEach { typeName ->
            try {
                selectedTypes.add(MediaType.valueOf(typeName))
            } catch (e: IllegalArgumentException) {
                // Ignore invalid type names
            }
        }

        setupCheckboxes()
        setupButtons()
    }

    private fun setupCheckboxes() {
        with(binding) {
            // Initialize checkbox states
            cbImages.isChecked = selectedTypes.contains(MediaType.IMAGE) || selectedTypes.isEmpty()
            cbVideos.isChecked = selectedTypes.contains(MediaType.VIDEO) || selectedTypes.isEmpty()
            cbAudio.isChecked = selectedTypes.contains(MediaType.AUDIO) || selectedTypes.isEmpty()
            cbGifs.isChecked = selectedTypes.contains(MediaType.GIF) || selectedTypes.isEmpty()
            cbPdf.isChecked = selectedTypes.contains(MediaType.PDF) || selectedTypes.isEmpty()
            cbText.isChecked = selectedTypes.contains(MediaType.TXT) || selectedTypes.isEmpty()
            cbEpub.isChecked = selectedTypes.contains(MediaType.EPUB) || selectedTypes.isEmpty()
            cbOther.isChecked = selectedTypes.contains(MediaType.OTHER) || selectedTypes.isEmpty()

            // Update selected types when checkboxes change
            cbImages.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.IMAGE, isChecked)
            }
            cbVideos.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.VIDEO, isChecked)
            }
            cbAudio.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.AUDIO, isChecked)
            }
            cbGifs.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.GIF, isChecked)
            }
            cbPdf.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.PDF, isChecked)
            }
            cbText.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.TXT, isChecked)
            }
            cbEpub.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.EPUB, isChecked)
            }
            cbOther.setOnCheckedChangeListener { _, isChecked ->
                updateSelection(MediaType.OTHER, isChecked)
            }
        }
    }

    private fun updateSelection(type: MediaType, isSelected: Boolean) {
        if (isSelected) {
            selectedTypes.add(type)
        } else {
            selectedTypes.remove(type)
        }
    }

    private fun setupButtons() {
        binding.btnSelectAll.setOnClickListener {
            setAllCheckboxes(true)
        }

        binding.btnSelectNone.setOnClickListener {
            setAllCheckboxes(false)
        }

        binding.btnApply.setOnClickListener {
            // If all are selected or none are selected, treat as "show all"
            val finalSelection = if (selectedTypes.size == MediaType.entries.size || selectedTypes.isEmpty()) {
                emptySet()
            } else {
                selectedTypes.toSet()
            }
            onFiltersApplied?.invoke(finalSelection)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setAllCheckboxes(checked: Boolean) {
        with(binding) {
            cbImages.isChecked = checked
            cbVideos.isChecked = checked
            cbAudio.isChecked = checked
            cbGifs.isChecked = checked
            cbPdf.isChecked = checked
            cbText.isChecked = checked
            cbEpub.isChecked = checked
            cbOther.isChecked = checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
