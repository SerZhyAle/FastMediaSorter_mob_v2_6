package com.sza.fastmediasorter.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogRenameBinding

/**
 * Dialog for renaming files and folders.
 *
 * Features:
 * - Pre-filled with current name (without extension for files)
 * - Real-time validation
 * - Invalid character detection
 * - Conflict detection with existing files
 * - Extension preservation (optional)
 * - Keyboard auto-focus with text selection
 */
class RenameDialog : DialogFragment() {

    companion object {
        const val TAG = "RenameDialog"
        private const val ARG_CURRENT_NAME = "current_name"
        private const val ARG_EXTENSION = "extension"
        private const val ARG_IS_DIRECTORY = "is_directory"
        private const val ARG_EXISTING_NAMES = "existing_names"

        // Characters not allowed in file names
        private val INVALID_CHARACTERS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

        fun newInstance(
            currentName: String,
            extension: String? = null,
            isDirectory: Boolean = false,
            existingNames: List<String> = emptyList()
        ): RenameDialog {
            return RenameDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_NAME, currentName)
                    putString(ARG_EXTENSION, extension)
                    putBoolean(ARG_IS_DIRECTORY, isDirectory)
                    putStringArrayList(ARG_EXISTING_NAMES, ArrayList(existingNames))
                }
            }
        }
    }

    private var _binding: DialogRenameBinding? = null
    private val binding get() = _binding!!

    var onRename: ((newName: String) -> Unit)? = null

    private var currentName: String = ""
    private var extension: String? = null
    private var isDirectory: Boolean = false
    private var existingNames: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            currentName = args.getString(ARG_CURRENT_NAME, "")
            extension = args.getString(ARG_EXTENSION)
            isDirectory = args.getBoolean(ARG_IS_DIRECTORY, false)
            existingNames = args.getStringArrayList(ARG_EXISTING_NAMES)?.toSet() ?: emptySet()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogRenameBinding.inflate(LayoutInflater.from(requireContext()))

        setupViews()

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isDirectory) R.string.rename_folder else R.string.rename_file)
            .setView(binding.root)
            .setPositiveButton(R.string.rename, null) // Set null initially to override later
            .setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()

        // Override positive button to prevent dismiss on invalid input
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val newName = binding.etNewName.text?.toString()?.trim() ?: ""
                if (validateName(newName)) {
                    val fullName = if (extension != null && !isDirectory) {
                        "$newName.$extension"
                    } else {
                        newName
                    }
                    onRename?.invoke(fullName)
                    dismiss()
                }
            }

            // Initially validate to enable/disable button
            updateButtonState(binding.etNewName.text?.toString()?.trim() ?: "")
        }

        // Show keyboard
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return dialog
    }

    private fun setupViews() {
        // Set current name without extension
        val nameWithoutExtension = if (extension != null && !isDirectory) {
            currentName.removeSuffix(".$extension")
        } else {
            currentName
        }
        binding.etNewName.setText(nameWithoutExtension)

        // Select all text for easy replacement
        binding.etNewName.selectAll()

        // Show extension note if applicable
        if (extension != null && !isDirectory) {
            binding.tvExtensionNote.visibility = View.VISIBLE
            binding.tvExtensionNote.text = getString(R.string.rename_extension_note_format, extension)
        } else {
            binding.tvExtensionNote.visibility = View.GONE
        }

        // Real-time validation
        binding.etNewName.doAfterTextChanged { text ->
            val name = text?.toString()?.trim() ?: ""
            validateName(name)
            updateButtonState(name)
        }

        // Handle IME action
        binding.etNewName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = binding.etNewName.text?.toString()?.trim() ?: ""
                if (validateName(newName)) {
                    val fullName = if (extension != null && !isDirectory) {
                        "$newName.$extension"
                    } else {
                        newName
                    }
                    onRename?.invoke(fullName)
                    dismiss()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun validateName(name: String): Boolean {
        // Check empty
        if (name.isEmpty()) {
            binding.tilNewName.error = getString(R.string.error_name_empty)
            return false
        }

        // Check invalid characters
        val invalidChar = name.firstOrNull { it in INVALID_CHARACTERS }
        if (invalidChar != null) {
            binding.tilNewName.error = getString(R.string.error_invalid_character, invalidChar.toString())
            return false
        }

        // Check leading/trailing spaces
        if (name != name.trim()) {
            binding.tilNewName.error = getString(R.string.error_name_spaces)
            return false
        }

        // Check leading/trailing dots (not allowed on Windows)
        if (name.startsWith('.') || name.endsWith('.')) {
            binding.tilNewName.error = getString(R.string.error_name_dots)
            return false
        }

        // Check reserved names (Windows)
        val reservedNames = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )
        if (name.uppercase() in reservedNames) {
            binding.tilNewName.error = getString(R.string.error_reserved_name)
            return false
        }

        // Check name length (max 255 characters is typical limit)
        if (name.length > 255) {
            binding.tilNewName.error = getString(R.string.error_name_too_long)
            return false
        }

        // Build full name to check against existing files
        val fullName = if (extension != null && !isDirectory) {
            "$name.$extension"
        } else {
            name
        }

        // Check conflict with existing files (case-insensitive)
        if (existingNames.any { it.equals(fullName, ignoreCase = true) && !it.equals(currentName, ignoreCase = true) }) {
            binding.tilNewName.error = getString(R.string.error_name_exists)
            return false
        }

        // Clear error
        binding.tilNewName.error = null
        return true
    }

    private fun updateButtonState(name: String) {
        val dialog = dialog as? android.app.AlertDialog ?: return
        val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)

        // Enable button only if valid and name has changed
        val fullName = if (extension != null && !isDirectory) {
            "$name.$extension"
        } else {
            name
        }
        val isValid = binding.tilNewName.error == null && name.isNotEmpty()
        val hasChanged = !fullName.equals(currentName, ignoreCase = true)

        positiveButton?.isEnabled = isValid && hasChanged
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
