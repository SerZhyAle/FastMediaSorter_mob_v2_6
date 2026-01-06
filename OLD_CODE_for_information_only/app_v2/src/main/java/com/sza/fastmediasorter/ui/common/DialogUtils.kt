package com.sza.fastmediasorter.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R

object DialogUtils {

    fun showScrollableDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String? = context.getString(R.string.ok),
        onPositive: (() -> Unit)? = null,
        negativeButtonText: String? = null,
        onNegative: (() -> Unit)? = null,
        cancelable: Boolean = true
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_scrollable_text, null)
        val tvContent = view.findViewById<TextView>(R.id.tvDialogContent)
        val btnCopy = view.findViewById<MaterialButton>(R.id.btnCopyToClipboard)

        tvContent.text = message

        btnCopy.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Dialog Content", message)
            clipboard.setPrimaryClip(clip)
            // Use existing string resource for "Copied to clipboard"
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setCancelable(cancelable)

        if (positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText) { _, _ ->
                onPositive?.invoke()
            }
        }

        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText) { _, _ ->
                onNegative?.invoke()
            }
        }

        builder.show()
    }

    fun showScrollableDialog(
        context: Context,
        @StringRes titleRes: Int,
        message: String,
        @StringRes positiveButtonTextRes: Int = R.string.ok,
        onPositive: (() -> Unit)? = null
    ) {
        showScrollableDialog(context, context.getString(titleRes), message, context.getString(positiveButtonTextRes), onPositive)
    }
}
