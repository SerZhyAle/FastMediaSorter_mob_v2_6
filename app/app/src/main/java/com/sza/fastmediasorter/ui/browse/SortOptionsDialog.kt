package com.sza.fastmediasorter.ui.browse

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.SortMode

/**
 * Dialog for selecting sort mode.
 */
class SortOptionsDialog : DialogFragment() {

    var onSortModeSelected: ((SortMode) -> Unit)? = null
    var currentSortMode: SortMode = SortMode.NAME_ASC

    companion object {
        const val TAG = "SortOptionsDialog"
        private const val ARG_CURRENT_SORT_MODE = "current_sort_mode"

        fun newInstance(currentSortMode: SortMode): SortOptionsDialog {
            return SortOptionsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_SORT_MODE, currentSortMode.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_CURRENT_SORT_MODE)?.let { name ->
            currentSortMode = SortMode.valueOf(name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sortModes = SortMode.entries.toTypedArray()
        val labels = sortModes.map { getSortModeLabel(it) }.toTypedArray()
        val currentIndex = sortModes.indexOf(currentSortMode)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                onSortModeSelected?.invoke(sortModes[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }

    private fun getSortModeLabel(sortMode: SortMode): String {
        return when (sortMode) {
            SortMode.NAME_ASC -> getString(R.string.sort_name_asc)
            SortMode.NAME_DESC -> getString(R.string.sort_name_desc)
            SortMode.DATE_ASC -> getString(R.string.sort_date_asc)
            SortMode.DATE_DESC -> getString(R.string.sort_date_desc)
            SortMode.SIZE_ASC -> getString(R.string.sort_size_asc)
            SortMode.SIZE_DESC -> getString(R.string.sort_size_desc)
        }
    }
}
