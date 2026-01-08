package com.sza.fastmediasorter.ui.dialog

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R

/**
 * Lightweight tooltip dialog for showing feature hints and tips.
 *
 * Features:
 * - Icon + message format
 * - "Don't show again" checkbox with persistence
 * - Single button (Got it!)
 * - Can be shown only once per feature
 */
class TooltipDialog : DialogFragment() {

    companion object {
        const val TAG = "TooltipDialog"
        private const val ARG_TIP_ID = "tip_id"
        private const val ARG_MESSAGE = "message"
        private const val ARG_ICON_RES = "icon_res"
        private const val ARG_TITLE = "title"

        private const val PREFS_NAME = "tooltip_prefs"
        private const val PREF_PREFIX_SHOWN = "shown_"

        fun newInstance(
            tipId: String,
            message: String,
            @DrawableRes iconRes: Int = R.drawable.ic_lightbulb,
            title: String? = null
        ): TooltipDialog {
            return TooltipDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIP_ID, tipId)
                    putString(ARG_MESSAGE, message)
                    putInt(ARG_ICON_RES, iconRes)
                    putString(ARG_TITLE, title)
                }
            }
        }

        /**
         * Show tooltip only if it hasn't been dismissed before
         */
        fun showIfNeeded(
            context: Context,
            fragmentManager: androidx.fragment.app.FragmentManager,
            tipId: String,
            message: String,
            @DrawableRes iconRes: Int = R.drawable.ic_lightbulb,
            title: String? = null
        ): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(PREF_PREFIX_SHOWN + tipId, false)) {
                return false // Already shown and dismissed
            }

            newInstance(tipId, message, iconRes, title).show(fragmentManager, TAG)
            return true
        }

        /**
         * Reset all tooltips to show again
         */
        fun resetAllTooltips(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                clear()
            }
        }

        /**
         * Reset a specific tooltip to show again
         */
        fun resetTooltip(context: Context, tipId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(PREF_PREFIX_SHOWN + tipId)
            }
        }

        // Predefined tip IDs
        object Tips {
            const val SWIPE_NAVIGATION = "tip_swipe_navigation"
            const val TOUCH_ZONES = "tip_touch_zones"
            const val LONG_PRESS_INFO = "tip_long_press_info"
            const val SLIDESHOW_MODE = "tip_slideshow_mode"
            const val FILTER_OPTIONS = "tip_filter_options"
            const val SORT_OPTIONS = "tip_sort_options"
            const val MULTI_SELECT = "tip_multi_select"
            const val NETWORK_SOURCES = "tip_network_sources"
        }
    }

    private var prefs: SharedPreferences? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val context = requireContext()
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val args = arguments ?: Bundle()
        val tipId = args.getString(ARG_TIP_ID) ?: ""
        val message = args.getString(ARG_MESSAGE) ?: ""
        val iconRes = args.getInt(ARG_ICON_RES, R.drawable.ic_lightbulb)
        val title = args.getString(ARG_TITLE)

        // Create custom view
        val contentView = createContentView(message, iconRes)

        val builder = MaterialAlertDialogBuilder(context)
            .setView(contentView)
            .setPositiveButton(R.string.got_it, null)

        if (title != null) {
            builder.setTitle(title)
        }

        val dialog = builder.create()

        // Handle dismiss with "don't show again"
        dialog.setOnDismissListener {
            val checkbox = contentView.findViewWithTag<MaterialCheckBox>("dont_show_checkbox")
            if (checkbox?.isChecked == true) {
                prefs?.edit {
                    putBoolean(PREF_PREFIX_SHOWN + tipId, true)
                }
            }
        }

        return dialog
    }

    private fun createContentView(message: String, @DrawableRes iconRes: Int): View {
        val context = requireContext()
        val padding = (24 * context.resources.displayMetrics.density).toInt()
        val iconSize = (48 * context.resources.displayMetrics.density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding / 2)

            // Icon and message row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                // Icon
                addView(ImageView(context).apply {
                    setImageResource(iconRes)
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        marginEnd = (16 * resources.displayMetrics.density).toInt()
                    }
                })

                // Message
                addView(TextView(context).apply {
                    text = message
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            })

            // Don't show again checkbox
            addView(MaterialCheckBox(context).apply {
                text = context.getString(R.string.dont_show_again)
                tag = "dont_show_checkbox"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
            })
        }
    }
}

/**
 * Simple confirmation dialog for generic confirmations
 */
class ConfirmationDialog : DialogFragment() {

    companion object {
        const val TAG = "ConfirmationDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_TEXT = "positive_text"
        private const val ARG_NEGATIVE_TEXT = "negative_text"
        private const val ARG_IS_DESTRUCTIVE = "is_destructive"

        fun newInstance(
            title: String? = null,
            message: String,
            positiveText: String? = null,
            negativeText: String? = null,
            isDestructive: Boolean = false
        ): ConfirmationDialog {
            return ConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_POSITIVE_TEXT, positiveText)
                    putString(ARG_NEGATIVE_TEXT, negativeText)
                    putBoolean(ARG_IS_DESTRUCTIVE, isDestructive)
                }
            }
        }
    }

    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val args = arguments ?: Bundle()
        val title = args.getString(ARG_TITLE)
        val message = args.getString(ARG_MESSAGE) ?: ""
        val positiveText = args.getString(ARG_POSITIVE_TEXT) ?: getString(R.string.ok)
        val negativeText = args.getString(ARG_NEGATIVE_TEXT) ?: getString(R.string.cancel)
        val isDestructive = args.getBoolean(ARG_IS_DESTRUCTIVE, false)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onConfirm?.invoke()
            }
            .setNegativeButton(negativeText) { _, _ ->
                onCancel?.invoke()
            }

        if (title != null) {
            builder.setTitle(title)
        }

        // Style destructive action button
        val dialog = builder.create()
        if (isDestructive) {
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                    requireContext().getColor(R.color.md_theme_error)
                )
            }
        }

        return dialog
    }
}
