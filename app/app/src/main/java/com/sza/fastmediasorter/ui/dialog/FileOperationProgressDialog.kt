package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogFileOperationProgressBinding
import java.text.DecimalFormat

/**
 * Dialog showing progress of file operations (copy, move, delete).
 *
 * Features:
 * - Progress bar (0-100%)
 * - Current file being processed
 * - Files completed / total count
 * - Transfer speed (MB/s)
 * - Time remaining estimate
 * - Cancel button
 */
class FileOperationProgressDialog : DialogFragment() {

    companion object {
        const val TAG = "FileOperationProgressDialog"
        private const val ARG_OPERATION_TYPE = "operation_type"
        private const val ARG_TOTAL_FILES = "total_files"

        fun newInstance(operationType: OperationType, totalFiles: Int): FileOperationProgressDialog {
            return FileOperationProgressDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_OPERATION_TYPE, operationType.name)
                    putInt(ARG_TOTAL_FILES, totalFiles)
                }
            }
        }
    }

    enum class OperationType {
        COPY, MOVE, DELETE
    }

    private var _binding: DialogFileOperationProgressBinding? = null
    private val binding get() = _binding!!

    private var operationType: OperationType = OperationType.COPY
    private var totalFiles: Int = 0
    private var completedFiles: Int = 0
    private var startTimeMs: Long = 0
    private var totalBytesProcessed: Long = 0

    var onCancelRequested: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        operationType = arguments?.getString(ARG_OPERATION_TYPE)?.let {
            OperationType.valueOf(it)
        } ?: OperationType.COPY
        totalFiles = arguments?.getInt(ARG_TOTAL_FILES, 0) ?: 0
        startTimeMs = System.currentTimeMillis()
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFileOperationProgressBinding.inflate(LayoutInflater.from(requireContext()))

        binding.btnCancel.setOnClickListener {
            onCancelRequested?.invoke()
        }

        val title = when (operationType) {
            OperationType.COPY -> R.string.copying_files
            OperationType.MOVE -> R.string.moving_files
            OperationType.DELETE -> R.string.deleting_files
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    /**
     * Update the progress display.
     *
     * @param currentFile Name of the file currently being processed
     * @param completed Number of files completed
     * @param bytesProcessed Total bytes processed so far
     */
    fun updateProgress(currentFile: String, completed: Int, bytesProcessed: Long = 0) {
        if (_binding == null) return

        completedFiles = completed
        totalBytesProcessed = bytesProcessed

        val progressPercent = if (totalFiles > 0) {
            (completed * 100) / totalFiles
        } else {
            0
        }

        binding.progressBar.progress = progressPercent
        binding.tvProgressPercent.text = "$progressPercent%"
        binding.tvCurrentFile.text = currentFile
        binding.tvFileCount.text = getString(R.string.files_progress, completed, totalFiles)

        // Calculate speed
        val elapsedMs = System.currentTimeMillis() - startTimeMs
        if (elapsedMs > 1000 && bytesProcessed > 0) {
            val bytesPerSecond = (bytesProcessed * 1000) / elapsedMs
            binding.tvSpeed.text = getString(R.string.speed_value, formatSpeed(bytesPerSecond))

            // Estimate time remaining
            if (completed > 0) {
                val avgTimePerFile = elapsedMs / completed
                val remainingFiles = totalFiles - completed
                val estimatedRemainingMs = avgTimePerFile * remainingFiles
                binding.tvTimeRemaining.text = getString(
                    R.string.time_remaining_value,
                    formatDuration(estimatedRemainingMs)
                )
            }
        }
    }

    /**
     * Mark operation as complete.
     */
    fun setComplete() {
        if (_binding == null) return

        binding.progressBar.progress = 100
        binding.tvProgressPercent.text = "100%"
        binding.tvCurrentFile.text = getString(R.string.operation_complete)
        binding.btnCancel.text = getString(R.string.action_close)
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    /**
     * Show error state.
     */
    fun setError(message: String) {
        if (_binding == null) return

        binding.tvCurrentFile.text = message
        binding.tvCurrentFile.setTextColor(requireContext().getColor(R.color.error))
        binding.btnCancel.text = getString(R.string.action_close)
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000_000 -> {
                DecimalFormat("#.##").format(bytesPerSecond / 1_000_000_000.0) + " GB/s"
            }
            bytesPerSecond >= 1_000_000 -> {
                DecimalFormat("#.##").format(bytesPerSecond / 1_000_000.0) + " MB/s"
            }
            bytesPerSecond >= 1_000 -> {
                DecimalFormat("#.##").format(bytesPerSecond / 1_000.0) + " KB/s"
            }
            else -> "$bytesPerSecond B/s"
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        return when {
            seconds >= 3600 -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
            seconds >= 60 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                "${minutes}m ${secs}s"
            }
            else -> "${seconds}s"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
