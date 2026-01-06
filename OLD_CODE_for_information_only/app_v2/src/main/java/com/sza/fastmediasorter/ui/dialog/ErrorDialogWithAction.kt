package com.sza.fastmediasorter.ui.dialog

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.sza.fastmediasorter.R

/**
 * Error dialog with an action button (e.g., "Open in external player")
 * Extends ErrorDialog functionality with additional action capability
 */
object ErrorDialogWithAction {

    /**
     * Show error dialog with action button
     * @param context Context
     * @param title Dialog title
     * @param message Error message
     * @param actionButtonText Text for the action button
     * @param onActionClick Callback when action button is clicked
     * @param details Optional detailed error information
     */
    fun show(
        context: Context,
        title: String = context.getString(R.string.error),
        message: String,
        actionButtonText: String,
        onActionClick: () -> Unit,
        details: String? = null
    ) {
        val fullMessage = if (details != null) {
            "$message\n\nDetails:\n$details"
        } else {
            message
        }

        // Inflate custom view with scrollable text
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_log_view,
            null
        )
        val textView = dialogView.findViewById<TextView>(R.id.tvLogText)
        textView.text = fullMessage

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(actionButtonText) { dialog, _ ->
                dialog.dismiss()
                onActionClick()
            }
            .setNeutralButton(R.string.copy_to_clipboard) { _, _ ->
                copyToClipboard(context, fullMessage)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Details", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
