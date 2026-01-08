package com.sza.fastmediasorter.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogErrorBinding

/**
 * Universal error dialog with expandable technical details.
 *
 * Features:
 * - User-friendly error message
 * - Expandable technical details (stack trace, etc.)
 * - Copy to clipboard functionality
 * - Optional action suggestion
 * - Optional action button
 */
class ErrorDialog : DialogFragment() {

    companion object {
        const val TAG = "ErrorDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_DETAILS = "details"
        private const val ARG_SUGGESTION = "suggestion"
        private const val ARG_ACTION_TEXT = "action_text"
        private const val ARG_SHOW_REPORT_BUTTON = "show_report_button"

        fun newInstance(
            title: String? = null,
            message: String,
            details: String? = null,
            suggestion: String? = null,
            actionText: String? = null,
            showReportButton: Boolean = false
        ): ErrorDialog {
            return ErrorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_DETAILS, details)
                    putString(ARG_SUGGESTION, suggestion)
                    putString(ARG_ACTION_TEXT, actionText)
                    putBoolean(ARG_SHOW_REPORT_BUTTON, showReportButton)
                }
            }
        }

        /**
         * Create error dialog from an exception
         */
        fun fromException(
            exception: Throwable,
            userMessage: String? = null,
            suggestion: String? = null
        ): ErrorDialog {
            val message = userMessage ?: exception.localizedMessage ?: exception.message ?: "Unknown error"
            val details = buildString {
                appendLine("Exception: ${exception::class.simpleName}")
                appendLine("Message: ${exception.message}")
                appendLine()
                appendLine("Stack trace:")
                exception.stackTraceToString().let { append(it) }
            }
            return newInstance(
                title = null,
                message = message,
                details = details,
                suggestion = suggestion
            )
        }
    }

    private var _binding: DialogErrorBinding? = null
    private val binding get() = _binding!!

    var onAction: (() -> Unit)? = null
    var onReportError: ((details: String) -> Unit)? = null

    private var isDetailsExpanded = false

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogErrorBinding.inflate(LayoutInflater.from(requireContext()))

        val args = arguments ?: Bundle()
        val title = args.getString(ARG_TITLE) ?: getString(R.string.error_title)
        val message = args.getString(ARG_MESSAGE) ?: ""
        val details = args.getString(ARG_DETAILS)
        val suggestion = args.getString(ARG_SUGGESTION)
        val actionText = args.getString(ARG_ACTION_TEXT)
        val showReportButton = args.getBoolean(ARG_SHOW_REPORT_BUTTON, false)

        setupViews(message, details, suggestion)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.ok, null)

        // Add action button if specified
        if (!actionText.isNullOrEmpty()) {
            builder.setNeutralButton(actionText) { _, _ ->
                onAction?.invoke()
            }
        }

        // Add report button if specified
        if (showReportButton && !details.isNullOrEmpty()) {
            builder.setNegativeButton(R.string.report_error) { _, _ ->
                onReportError?.invoke(details)
            }
        }

        return builder.create()
    }

    private fun setupViews(message: String, details: String?, suggestion: String?) {
        // Main error message
        binding.tvErrorMessage.text = message

        // Details section
        if (!details.isNullOrEmpty()) {
            binding.layoutDetailsContainer.visibility = View.VISIBLE
            binding.tvErrorDetails.text = details

            // Toggle details visibility
            binding.layoutDetailsHeader.setOnClickListener {
                toggleDetails()
            }

            // Copy to clipboard
            binding.btnCopyDetails.setOnClickListener {
                copyToClipboard(details)
            }
        } else {
            binding.layoutDetailsContainer.visibility = View.GONE
        }

        // Suggestion
        if (!suggestion.isNullOrEmpty()) {
            binding.tvSuggestion.visibility = View.VISIBLE
            binding.tvSuggestion.text = suggestion
        } else {
            binding.tvSuggestion.visibility = View.GONE
        }
    }

    private fun toggleDetails() {
        isDetailsExpanded = !isDetailsExpanded

        if (isDetailsExpanded) {
            binding.scrollDetails.visibility = View.VISIBLE
            binding.ivExpandIcon.setImageResource(R.drawable.ic_expand_less)

            // Animate expansion
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            binding.scrollDetails.startAnimation(animation)
        } else {
            binding.scrollDetails.visibility = View.GONE
            binding.ivExpandIcon.setImageResource(R.drawable.ic_expand_more)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Details", text)
        clipboard.setPrimaryClip(clip)

        // Show confirmation
        view?.let { view ->
            Snackbar.make(view, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Helper object for showing error dialogs
 */
object ErrorDialogHelper {

    /**
     * Show a simple error dialog with just a message
     */
    fun showError(
        fragmentManager: androidx.fragment.app.FragmentManager,
        message: String,
        title: String? = null
    ) {
        ErrorDialog.newInstance(
            title = title,
            message = message
        ).show(fragmentManager, ErrorDialog.TAG)
    }

    /**
     * Show an error dialog from an exception
     */
    fun showException(
        fragmentManager: androidx.fragment.app.FragmentManager,
        exception: Throwable,
        userMessage: String? = null,
        suggestion: String? = null
    ) {
        ErrorDialog.fromException(
            exception = exception,
            userMessage = userMessage,
            suggestion = suggestion
        ).show(fragmentManager, ErrorDialog.TAG)
    }

    /**
     * Show an error dialog with action button
     */
    fun showErrorWithAction(
        fragmentManager: androidx.fragment.app.FragmentManager,
        message: String,
        actionText: String,
        onAction: () -> Unit
    ) {
        ErrorDialog.newInstance(
            message = message,
            actionText = actionText
        ).apply {
            this.onAction = onAction
        }.show(fragmentManager, ErrorDialog.TAG)
    }
}
