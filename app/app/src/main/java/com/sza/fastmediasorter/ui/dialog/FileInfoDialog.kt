package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import java.util.Date

/**
 * Dialog to display file information.
 */
class FileInfoDialog(
    context: Context,
    private val filePath: String
) : Dialog(context) {

    private val file = File(filePath)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val contentView = createContentView()
        setContentView(contentView)
        
        // Set dialog title
        setTitle(R.string.file_info)
        
        // Set dialog width to 90% of screen width
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createContentView(): View {
        val padding = context.resources.getDimensionPixelSize(R.dimen.spacing_medium)
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // File name
        container.addView(createInfoRow(
            context.getString(R.string.info_name), 
            file.name
        ))

        // File path
        container.addView(createInfoRow(
            context.getString(R.string.info_path), 
            file.parent ?: ""
        ))

        // File size
        container.addView(createInfoRow(
            context.getString(R.string.info_size), 
            formatFileSize(file.length())
        ))

        // File type
        container.addView(createInfoRow(
            context.getString(R.string.info_type), 
            getFileType()
        ))

        // Modified date
        container.addView(createInfoRow(
            context.getString(R.string.info_modified), 
            formatDate(file.lastModified())
        ))

        // Open in external app button
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
            }
        }

        val openButton = com.google.android.material.button.MaterialButton(context).apply {
            text = context.getString(R.string.open_with)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { openWithExternalApp() }
        }
        buttonLayout.addView(openButton)

        val closeButton = com.google.android.material.button.MaterialButton(
            context, 
            null, 
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = context.getString(R.string.action_close)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding / 2
            }
            setOnClickListener { dismiss() }
        }
        buttonLayout.addView(closeButton)

        container.addView(buttonLayout)

        return container
    }

    private fun createInfoRow(label: String, value: String): View {
        val rowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
        }

        val labelView = TextView(context).apply {
            text = label
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
            setTextColor(context.getColor(android.R.color.darker_gray))
        }
        rowLayout.addView(labelView)

        val valueView = TextView(context).apply {
            text = value
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextIsSelectable(true)
        }
        rowLayout.addView(valueView)

        return rowLayout
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = DateFormat.getMediumDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        val date = Date(timestamp)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    private fun getFileType(): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "JPEG Image"
            "png" -> "PNG Image"
            "gif" -> "GIF Image"
            "webp" -> "WebP Image"
            "bmp" -> "Bitmap Image"
            "heic", "heif" -> "HEIC Image"
            "mp4" -> "MP4 Video"
            "mkv" -> "MKV Video"
            "mov" -> "QuickTime Video"
            "avi" -> "AVI Video"
            "webm" -> "WebM Video"
            "mp3" -> "MP3 Audio"
            "wav" -> "WAV Audio"
            "flac" -> "FLAC Audio"
            "m4a" -> "M4A Audio"
            "aac" -> "AAC Audio"
            "ogg" -> "Ogg Audio"
            "pdf" -> "PDF Document"
            "txt" -> "Text File"
            else -> extension.uppercase().ifEmpty { "Unknown" }
        }
    }

    private fun openWithExternalApp() {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, context.getString(R.string.open_with))
            context.startActivity(chooser)
            dismiss()
        } catch (e: Exception) {
            Timber.e(e, "Failed to open file with external app")
        }
    }

    private fun getMimeType(): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}
