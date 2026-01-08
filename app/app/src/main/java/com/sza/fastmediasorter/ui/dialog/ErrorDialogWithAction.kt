package com.sza.fastmediasorter.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.sza.fastmediasorter.R
import timber.log.Timber

/**
 * Dialog for displaying errors with custom action buttons.
 * Supports expandable error details and copy-to-clipboard functionality.
 */
class ErrorDialogWithAction : DialogFragment() {

    companion object {
        const val TAG = "ErrorDialogWithAction"
        
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_DETAILS = "details"
        private const val ARG_ACTION_TEXT = "action_text"
        private const val ARG_SHOW_REPORT = "show_report"
        
        /**
         * Create a new instance of ErrorDialogWithAction.
         * 
         * @param title The error title
         * @param message The error message to display
         * @param details Optional detailed error information (stack trace, etc.)
         * @param actionText Optional text for the action button
         * @param showReportButton Whether to show a "Report" button
         */
        fun newInstance(
            title: String = "Error",
            message: String,
            details: String? = null,
            actionText: String? = null,
            showReportButton: Boolean = false
        ): ErrorDialogWithAction {
            return ErrorDialogWithAction().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    details?.let { putString(ARG_DETAILS, it) }
                    actionText?.let { putString(ARG_ACTION_TEXT, it) }
                    putBoolean(ARG_SHOW_REPORT, showReportButton)
                }
            }
        }
    }

    private var actionClickListener: (() -> Unit)? = null
    private var reportClickListener: (() -> Unit)? = null
    private var isDetailsExpanded = false

    /**
     * Set a listener for the action button click.
     */
    fun setOnActionClickListener(listener: () -> Unit) {
        this.actionClickListener = listener
    }

    /**
     * Set a listener for the report button click.
     */
    fun setOnReportClickListener(listener: () -> Unit) {
        this.reportClickListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_large)
        
        val args = requireArguments()
        val title = args.getString(ARG_TITLE) ?: getString(R.string.error_title)
        val message = args.getString(ARG_MESSAGE) ?: ""
        val details = args.getString(ARG_DETAILS)
        val actionText = args.getString(ARG_ACTION_TEXT)
        val showReportButton = args.getBoolean(ARG_SHOW_REPORT, false)

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Error icon and title
        val titleLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding
            }
        }

        val errorIcon = TextView(context).apply {
            text = "⚠️"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = padding / 2
            }
        }
        titleLayout.addView(errorIcon)

        val titleView = TextView(context).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setTextColor(context.getColor(android.R.color.holo_red_dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleLayout.addView(titleView)
        
        rootLayout.addView(titleLayout)

        // Error message
        val messageView = TextView(context).apply {
            text = message
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding
            }
        }
        rootLayout.addView(messageView)

        // Expandable details section (if provided)
        if (!details.isNullOrEmpty()) {
            val detailsSection = createDetailsSection(context, details, padding)
            rootLayout.addView(detailsSection)
        }

        // Action buttons
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
            }
        }

        // Dismiss button (always shown)
        val dismissButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.action_dismiss)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { dismiss() }
        }
        buttonLayout.addView(dismissButton)

        // Report button (optional)
        if (showReportButton) {
            val reportButton = MaterialButton(
                context,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = getString(R.string.report_error)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = padding / 2
                }
                setOnClickListener {
                    reportClickListener?.invoke()
                }
            }
            buttonLayout.addView(reportButton)
        }

        // Action button (optional)
        if (!actionText.isNullOrEmpty()) {
            val actionButton = MaterialButton(context).apply {
                text = actionText
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = padding / 2
                }
                setOnClickListener {
                    actionClickListener?.invoke()
                    dismiss()
                }
            }
            buttonLayout.addView(actionButton)
        }

        rootLayout.addView(buttonLayout)

        return rootLayout
    }

    private fun createDetailsSection(context: android.content.Context, details: String, padding: Int): View {
        val detailsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Expand/collapse header
        val expandHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding / 2
            }
        }

        val expandArrow = TextView(context).apply {
            text = "▶"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = padding / 4
            }
        }
        expandHeader.addView(expandArrow)

        val expandLabel = TextView(context).apply {
            text = getString(R.string.show_details)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
            setTextColor(context.getColor(R.color.colorPrimary))
        }
        expandHeader.addView(expandLabel)

        detailsContainer.addView(expandHeader)

        // Details content (hidden initially)
        val detailsScrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.error_details_max_height)
            )
            visibility = View.GONE
        }

        val detailsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.getColor(android.R.color.darker_gray) and 0x33FFFFFF)
            setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
        }

        val detailsText = TextView(context).apply {
            text = details
            setTextIsSelectable(true)
            textSize = 10f
            setTypeface(android.graphics.Typeface.MONOSPACE)
        }
        detailsLayout.addView(detailsText)

        // Copy details button
        val copyButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.copy_details)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding / 2
            }
            setOnClickListener {
                copyToClipboard(details)
            }
        }
        detailsLayout.addView(copyButton)

        detailsScrollView.addView(detailsLayout)
        detailsContainer.addView(detailsScrollView)

        // Toggle expansion
        expandHeader.setOnClickListener {
            isDetailsExpanded = !isDetailsExpanded
            detailsScrollView.isVisible = isDetailsExpanded
            expandArrow.text = if (isDetailsExpanded) "▼" else "▶"
            expandLabel.text = getString(
                if (isDetailsExpanded) R.string.hide_details else R.string.show_details
            )
        }

        return detailsContainer
    }

    private fun copyToClipboard(text: String) {
        val context = requireContext()
        val clipboard = context.getSystemService<ClipboardManager>()
        val clip = ClipData.newPlainText("Error Details", text)
        clipboard?.setPrimaryClip(clip)
        
        Timber.d("Copied error details to clipboard")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
