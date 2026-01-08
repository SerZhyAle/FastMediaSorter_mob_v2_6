package com.sza.fastmediasorter.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogScrollableTextBinding

/**
 * Universal dialog for displaying long scrollable text content.
 *
 * Use cases:
 * - Privacy policy
 * - Terms of service
 * - License agreements
 * - Help text
 * - Log output
 * - Debug information
 *
 * Features:
 * - Scrollable text area
 * - Support for plain text, HTML, and Markdown
 * - Optional copy to clipboard button
 * - Selectable text
 * - Clickable links (for HTML content)
 */
class ScrollableTextDialog : DialogFragment() {

    companion object {
        const val TAG = "ScrollableTextDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"
        private const val ARG_CONTENT_TYPE = "content_type"
        private const val ARG_SHOW_COPY_BUTTON = "show_copy_button"
        private const val ARG_POSITIVE_BUTTON_TEXT = "positive_button_text"

        fun newInstance(
            title: String,
            content: String,
            contentType: ContentType = ContentType.PLAIN_TEXT,
            showCopyButton: Boolean = false,
            positiveButtonText: String? = null
        ): ScrollableTextDialog {
            return ScrollableTextDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_CONTENT, content)
                    putString(ARG_CONTENT_TYPE, contentType.name)
                    putBoolean(ARG_SHOW_COPY_BUTTON, showCopyButton)
                    putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText)
                }
            }
        }

        /**
         * Create a log viewer dialog
         */
        fun forLogViewer(title: String, logContent: String): ScrollableTextDialog {
            return newInstance(
                title = title,
                content = logContent,
                contentType = ContentType.MONOSPACE,
                showCopyButton = true
            )
        }

        /**
         * Create a privacy policy dialog
         */
        fun forPrivacyPolicy(content: String): ScrollableTextDialog {
            return newInstance(
                title = "Privacy Policy",
                content = content,
                contentType = ContentType.HTML,
                showCopyButton = false
            )
        }

        /**
         * Create a terms of service dialog
         */
        fun forTermsOfService(content: String): ScrollableTextDialog {
            return newInstance(
                title = "Terms of Service",
                content = content,
                contentType = ContentType.HTML,
                showCopyButton = false
            )
        }
    }

    enum class ContentType {
        PLAIN_TEXT,  // Regular text
        HTML,        // HTML formatted text
        MONOSPACE    // Monospace font (for logs, code)
    }

    private var _binding: DialogScrollableTextBinding? = null
    private val binding get() = _binding!!

    var onPositiveClick: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogScrollableTextBinding.inflate(LayoutInflater.from(requireContext()))

        val args = arguments ?: Bundle()
        val title = args.getString(ARG_TITLE) ?: ""
        val content = args.getString(ARG_CONTENT) ?: ""
        val contentType = args.getString(ARG_CONTENT_TYPE)?.let { ContentType.valueOf(it) } ?: ContentType.PLAIN_TEXT
        val showCopyButton = args.getBoolean(ARG_SHOW_COPY_BUTTON, false)
        val positiveButtonText = args.getString(ARG_POSITIVE_BUTTON_TEXT) ?: getString(R.string.ok)

        setupContent(content, contentType)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositiveClick?.invoke()
            }

        if (showCopyButton) {
            builder.setNeutralButton(R.string.copy_to_clipboard) { _, _ ->
                copyToClipboard(content)
            }
        }

        return builder.create()
    }

    private fun setupContent(content: String, contentType: ContentType) {
        when (contentType) {
            ContentType.PLAIN_TEXT -> {
                binding.tvContent.text = content
            }
            ContentType.HTML -> {
                binding.tvContent.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
                binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
            }
            ContentType.MONOSPACE -> {
                binding.tvContent.text = content
                binding.tvContent.typeface = android.graphics.Typeface.MONOSPACE
                binding.tvContent.setTextIsSelectable(true)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Content", text)
        clipboard.setPrimaryClip(clip)

        // Show confirmation via Snackbar on the parent activity
        activity?.let { activity ->
            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            Snackbar.make(rootView, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Dialog for viewing log files with additional features
 */
class LogViewDialog : DialogFragment() {

    companion object {
        const val TAG = "LogViewDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_LOG_CONTENT = "log_content"
        private const val ARG_AUTO_SCROLL = "auto_scroll"

        fun newInstance(
            title: String = "Log Viewer",
            logContent: String,
            autoScroll: Boolean = true
        ): LogViewDialog {
            return LogViewDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_LOG_CONTENT, logContent)
                    putBoolean(ARG_AUTO_SCROLL, autoScroll)
                }
            }
        }
    }

    private var _binding: DialogScrollableTextBinding? = null
    private val binding get() = _binding!!

    var onClear: (() -> Unit)? = null
    var onShare: ((String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogScrollableTextBinding.inflate(LayoutInflater.from(requireContext()))

        val args = arguments ?: Bundle()
        val title = args.getString(ARG_TITLE) ?: "Log Viewer"
        val logContent = args.getString(ARG_LOG_CONTENT) ?: ""
        val autoScroll = args.getBoolean(ARG_AUTO_SCROLL, true)

        // Setup monospace font for logs
        binding.tvContent.text = logContent
        binding.tvContent.typeface = android.graphics.Typeface.MONOSPACE
        binding.tvContent.setTextIsSelectable(true)

        // Auto-scroll to bottom if requested
        if (autoScroll) {
            binding.tvContent.post {
                val scrollView = binding.tvContent.parent as? androidx.core.widget.NestedScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.copy_to_clipboard) { _, _ ->
                copyToClipboard(logContent)
            }

        // Add share button if handler provided
        if (onShare != null) {
            builder.setNegativeButton(R.string.share) { _, _ ->
                onShare?.invoke(logContent)
            }
        }

        return builder.create()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Log Content", text)
        clipboard.setPrimaryClip(clip)

        activity?.let { activity ->
            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            Snackbar.make(rootView, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
