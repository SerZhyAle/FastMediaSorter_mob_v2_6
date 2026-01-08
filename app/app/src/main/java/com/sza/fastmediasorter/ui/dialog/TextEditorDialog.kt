package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogTextEditorBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset

/**
 * Full-screen dialog for editing text files.
 * Supports encoding selection, undo/redo, and save functionality.
 */
class TextEditorDialog : DialogFragment() {

    companion object {
        private const val ARG_FILE_PATH = "file_path"
        private const val MAX_FILE_SIZE = 1_000_000L // 1MB limit

        /**
         * Supported text encodings.
         */
        val ENCODINGS = listOf(
            "UTF-8" to Charsets.UTF_8,
            "UTF-16" to Charsets.UTF_16,
            "UTF-16LE" to Charsets.UTF_16LE,
            "UTF-16BE" to Charsets.UTF_16BE,
            "ISO-8859-1" to Charsets.ISO_8859_1,
            "Windows-1251" to Charset.forName("Windows-1251"),
            "Windows-1252" to Charset.forName("Windows-1252"),
            "US-ASCII" to Charsets.US_ASCII
        )

        fun newInstance(filePath: String): TextEditorDialog {
            return TextEditorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                }
            }
        }
    }

    private var _binding: DialogTextEditorBinding? = null
    private val binding get() = _binding!!

    private var filePath: String = ""
    private var originalContent: String = ""
    private var currentEncoding: Charset = Charsets.UTF_8
    private var hasUnsavedChanges = false
    private var isTruncated = false

    private var loadJob: Job? = null
    private var saveJob: Job? = null

    private var onSaveListener: ((Boolean) -> Unit)? = null

    // Undo/Redo history
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var isHistoryChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_FastMediaSorter_FullScreenDialog)
        filePath = arguments?.getString(ARG_FILE_PATH) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTextEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadFileContent()
    }

    private fun setupViews() {
        // Set up toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener { handleBackPress() }
            setNavigationIcon(R.drawable.ic_close)
            title = File(filePath).name
        }

        // Set up encoding spinner
        val encodingNames = ENCODINGS.map { it.first }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            encodingNames
        )
        binding.spinnerEncoding.setAdapter(adapter)
        binding.spinnerEncoding.setText(encodingNames[0], false)
        binding.spinnerEncoding.setOnItemClickListener { _, _, position, _ ->
            val newEncoding = ENCODINGS[position].second
            if (newEncoding != currentEncoding) {
                currentEncoding = newEncoding
                reloadWithEncoding()
            }
        }

        // Set up buttons
        binding.btnUndo.setOnClickListener { undo() }
        binding.btnRedo.setOnClickListener { redo() }
        binding.btnSave.setOnClickListener { saveFile() }

        // Track text changes for undo/redo
        binding.editContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isHistoryChange && s != null) {
                    undoStack.add(s.toString())
                    redoStack.clear()
                    updateUndoRedoButtons()
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isHistoryChange) {
                    hasUnsavedChanges = s?.toString() != originalContent
                    updateSaveButton()
                }
            }
        })

        updateUndoRedoButtons()
        updateSaveButton()
    }

    private fun loadFileContent() {
        loadJob?.cancel()
        loadJob = CoroutineScope(Dispatchers.Main).launch {
            showLoading(true)
            try {
                val content = withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    if (!file.exists()) {
                        throw IllegalStateException("File not found")
                    }
                    if (!file.canRead()) {
                        throw IllegalStateException("Cannot read file")
                    }

                    isTruncated = file.length() > MAX_FILE_SIZE
                    
                    if (isTruncated) {
                        // Read only first 1MB
                        file.inputStream().use { input ->
                            val bytes = ByteArray(MAX_FILE_SIZE.toInt())
                            val read = input.read(bytes)
                            String(bytes, 0, read, currentEncoding)
                        }
                    } else {
                        file.readText(currentEncoding)
                    }
                }

                originalContent = content
                binding.editContent.setText(content)
                binding.editContent.setSelection(0)
                hasUnsavedChanges = false
                undoStack.clear()
                redoStack.clear()
                updateUndoRedoButtons()
                updateSaveButton()

                // Show truncation warning
                binding.tvTruncationNotice.isVisible = isTruncated

                showLoading(false)

            } catch (e: Exception) {
                Timber.e(e, "Failed to load text file: $filePath")
                showError(e.message ?: "Failed to load file")
            }
        }
    }

    private fun reloadWithEncoding() {
        if (hasUnsavedChanges) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discard_changes)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard) { _, _ ->
                    loadFileContent()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            loadFileContent()
        }
    }

    private fun saveFile() {
        if (isTruncated) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error_title)
                .setMessage(R.string.cannot_save_truncated)
                .setPositiveButton(R.string.action_ok, null)
                .show()
            return
        }

        saveJob?.cancel()
        saveJob = CoroutineScope(Dispatchers.Main).launch {
            showLoading(true)
            try {
                val content = binding.editContent.text?.toString() ?: ""
                
                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    if (!file.canWrite()) {
                        throw IllegalStateException("Cannot write to file")
                    }
                    file.writeText(content, currentEncoding)
                }

                originalContent = content
                hasUnsavedChanges = false
                updateSaveButton()
                showLoading(false)
                
                onSaveListener?.invoke(true)
                
                // Show success message
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    R.string.file_saved_successfully,
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Timber.e(e, "Failed to save text file: $filePath")
                showLoading(false)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_title)
                    .setMessage(getString(R.string.save_failed_message, e.message))
                    .setPositiveButton(R.string.action_ok, null)
                    .show()
            }
        }
    }

    private fun undo() {
        if (undoStack.isNotEmpty()) {
            isHistoryChange = true
            val currentText = binding.editContent.text?.toString() ?: ""
            redoStack.add(currentText)
            val previousText = undoStack.removeLast()
            binding.editContent.setText(previousText)
            binding.editContent.setSelection(previousText.length)
            isHistoryChange = false
            hasUnsavedChanges = previousText != originalContent
            updateUndoRedoButtons()
            updateSaveButton()
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            isHistoryChange = true
            val currentText = binding.editContent.text?.toString() ?: ""
            undoStack.add(currentText)
            val nextText = redoStack.removeLast()
            binding.editContent.setText(nextText)
            binding.editContent.setSelection(nextText.length)
            isHistoryChange = false
            hasUnsavedChanges = nextText != originalContent
            updateUndoRedoButtons()
            updateSaveButton()
        }
    }

    private fun updateUndoRedoButtons() {
        binding.btnUndo.isEnabled = undoStack.isNotEmpty()
        binding.btnUndo.alpha = if (undoStack.isNotEmpty()) 1.0f else 0.4f
        binding.btnRedo.isEnabled = redoStack.isNotEmpty()
        binding.btnRedo.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.4f
    }

    private fun updateSaveButton() {
        binding.btnSave.isEnabled = hasUnsavedChanges && !isTruncated
        binding.btnSave.alpha = if (hasUnsavedChanges && !isTruncated) 1.0f else 0.4f
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.editContent.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                dismiss()
            }
            .show()
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discard_changes)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard) { _, _ ->
                    dismiss()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            dismiss()
        }
    }

    fun setOnSaveListener(listener: (Boolean) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        saveJob?.cancel()
        _binding = null
    }
}
