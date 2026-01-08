package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogDeleteConfirmationBinding

/**
 * Dialog for confirming file deletion with options for trash vs permanent delete.
 *
 * Features:
 * - Shows file count in message
 * - Move to trash vs delete permanently options
 * - "Don't ask again for this session" checkbox
 * - Undo support when moving to trash
 */
class DeleteConfirmationDialog : DialogFragment() {

    companion object {
        const val TAG = "DeleteConfirmationDialog"
        private const val ARG_FILE_COUNT = "file_count"
        private const val ARG_FILE_NAMES = "file_names"
        private const val ARG_ALLOW_TRASH = "allow_trash"

        fun newInstance(
            fileCount: Int,
            fileNames: List<String> = emptyList(),
            allowTrash: Boolean = true
        ): DeleteConfirmationDialog {
            return DeleteConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FILE_COUNT, fileCount)
                    putStringArrayList(ARG_FILE_NAMES, ArrayList(fileNames.take(5)))
                    putBoolean(ARG_ALLOW_TRASH, allowTrash)
                }
            }
        }
    }

    private var _binding: DialogDeleteConfirmationBinding? = null
    private val binding get() = _binding!!

    private var fileCount: Int = 0
    private var allowTrash: Boolean = true

    /**
     * Callback when user confirms deletion.
     * @param usePermanentDelete true if permanent delete selected, false for trash
     * @param dontAskAgain true if user checked "don't ask again"
     */
    var onConfirm: ((usePermanentDelete: Boolean, dontAskAgain: Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileCount = arguments?.getInt(ARG_FILE_COUNT, 1) ?: 1
        allowTrash = arguments?.getBoolean(ARG_ALLOW_TRASH, true) ?: true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDeleteConfirmationBinding.inflate(LayoutInflater.from(requireContext()))

        setupUI()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_confirmation_title)
            .setView(binding.root)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val usePermanentDelete = binding.rbDeletePermanently.isChecked
                val dontAskAgain = binding.cbDontAskAgain.isChecked
                onConfirm?.invoke(usePermanentDelete, dontAskAgain)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }

    private fun setupUI() {
        // Set message based on file count
        val message = if (fileCount == 1) {
            getString(R.string.delete_confirmation_single)
        } else {
            getString(R.string.delete_confirmation_message, fileCount)
        }
        binding.tvDeleteMessage.text = message

        // Hide trash option for network files (no trash support)
        if (!allowTrash) {
            binding.rbMoveToTrash.isEnabled = false
            binding.rbDeletePermanently.isChecked = true
            binding.rbMoveToTrash.text = getString(R.string.delete_trash_not_available)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
