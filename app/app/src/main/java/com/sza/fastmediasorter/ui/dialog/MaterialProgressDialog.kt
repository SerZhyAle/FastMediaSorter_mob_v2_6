package com.sza.fastmediasorter.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.sza.fastmediasorter.R

/**
 * Simple indeterminate progress dialog for short operations.
 *
 * Use cases:
 * - Loading data
 * - Connecting to server
 * - Processing (when duration is unknown)
 *
 * Features:
 * - Circular indeterminate progress
 * - Customizable message
 * - Non-cancellable by default
 * - Can update message while showing
 */
class MaterialProgressDialog : DialogFragment() {

    companion object {
        const val TAG = "MaterialProgressDialog"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CANCELLABLE = "cancellable"

        fun newInstance(
            message: String? = null,
            cancellable: Boolean = false
        ): MaterialProgressDialog {
            return MaterialProgressDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    putBoolean(ARG_CANCELLABLE, cancellable)
                }
            }
        }

        /**
         * Show loading dialog with default message
         */
        fun showLoading(
            fragmentManager: androidx.fragment.app.FragmentManager,
            message: String? = null
        ): MaterialProgressDialog {
            val dialog = newInstance(message ?: "Loading...")
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }

    private var messageTextView: TextView? = null
    var onCancel: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cancellable = arguments?.getBoolean(ARG_CANCELLABLE, false) ?: false
        isCancelable = cancellable
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val message = arguments?.getString(ARG_MESSAGE)
        val cancellable = arguments?.getBoolean(ARG_CANCELLABLE, false) ?: false

        val contentView = createContentView(message)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(contentView)
            .setCancelable(cancellable)
            .create()

        if (cancellable) {
            dialog.setOnCancelListener {
                onCancel?.invoke()
            }
        }

        // Prevent dismiss on outside touch
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    private fun createContentView(message: String?): View {
        val context = requireContext()
        val padding = (24 * context.resources.displayMetrics.density).toInt()
        val progressSize = (48 * context.resources.displayMetrics.density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)

            // Circular progress indicator
            addView(CircularProgressIndicator(context).apply {
                isIndeterminate = true
                layoutParams = LinearLayout.LayoutParams(progressSize, progressSize)
            })

            // Message text
            if (message != null) {
                addView(TextView(context).apply {
                    text = message
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = (16 * resources.displayMetrics.density).toInt()
                    }
                    messageTextView = this
                })
            }
        }
    }

    /**
     * Update the message while dialog is showing
     */
    fun updateMessage(message: String) {
        messageTextView?.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageTextView = null
    }
}

/**
 * Progress dialog with determinate progress and percentage
 */
class DeterminateProgressDialog : DialogFragment() {

    companion object {
        const val TAG = "DeterminateProgressDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CANCELLABLE = "cancellable"

        fun newInstance(
            title: String? = null,
            message: String? = null,
            cancellable: Boolean = false
        ): DeterminateProgressDialog {
            return DeterminateProgressDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putBoolean(ARG_CANCELLABLE, cancellable)
                }
            }
        }
    }

    private var progressIndicator: com.google.android.material.progressindicator.LinearProgressIndicator? = null
    private var messageTextView: TextView? = null
    private var percentageTextView: TextView? = null

    var onCancel: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cancellable = arguments?.getBoolean(ARG_CANCELLABLE, false) ?: false
        isCancelable = cancellable
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val title = arguments?.getString(ARG_TITLE)
        val message = arguments?.getString(ARG_MESSAGE)
        val cancellable = arguments?.getBoolean(ARG_CANCELLABLE, false) ?: false

        val contentView = createContentView(message)

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(contentView)
            .setCancelable(cancellable)

        if (title != null) {
            builder.setTitle(title)
        }

        if (cancellable) {
            builder.setNegativeButton(R.string.cancel) { _, _ ->
                onCancel?.invoke()
            }
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    private fun createContentView(message: String?): View {
        val context = requireContext()
        val padding = (24 * context.resources.displayMetrics.density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)

            // Message text (optional)
            if (message != null) {
                addView(TextView(context).apply {
                    text = message
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    messageTextView = this
                })
            }

            // Progress bar
            addView(com.google.android.material.progressindicator.LinearProgressIndicator(context).apply {
                isIndeterminate = false
                progress = 0
                max = 100
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
                progressIndicator = this
            })

            // Percentage text
            addView(TextView(context).apply {
                text = "0%"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * resources.displayMetrics.density).toInt()
                }
                percentageTextView = this
            })
        }
    }

    /**
     * Update progress (0-100)
     */
    fun updateProgress(progress: Int) {
        progressIndicator?.progress = progress.coerceIn(0, 100)
        percentageTextView?.text = "$progress%"
    }

    /**
     * Update message while showing
     */
    fun updateMessage(message: String) {
        messageTextView?.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressIndicator = null
        messageTextView = null
        percentageTextView = null
    }
}
