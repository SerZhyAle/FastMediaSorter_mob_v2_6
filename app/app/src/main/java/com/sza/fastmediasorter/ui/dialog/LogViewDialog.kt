package com.sza.fastmediasorter.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.sza.fastmediasorter.R
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for viewing debug logs with filtering, search, and export functionality.
 */
class DebugLogViewerDialog : DialogFragment() {

    companion object {
        const val TAG = "DebugLogViewerDialog"
        
        private const val ARG_LOG_CONTENT = "log_content"
        
        /**
         * Create a new instance of DebugLogViewerDialog.
         * 
         * @param logContent The log content to display
         */
        fun newInstance(logContent: String = ""): DebugLogViewerDialog {
            return DebugLogViewerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOG_CONTENT, logContent)
                }
            }
        }
    }

    private var allLogLines: List<LogLine> = emptyList()
    private var filteredLogLines: List<LogLine> = emptyList()
    private var currentFilter: LogLevel? = null
    private var searchQuery: String = ""
    
    private lateinit var logTextView: TextView

    /**
     * Log severity levels.
     */
    enum class LogLevel(val tag: String, val color: Int) {
        VERBOSE("V", android.R.color.darker_gray),
        DEBUG("D", android.R.color.holo_blue_dark),
        INFO("I", android.R.color.holo_green_dark),
        WARN("W", android.R.color.holo_orange_dark),
        ERROR("E", android.R.color.holo_red_dark)
    }

    /**
     * Represents a single log line with parsed level.
     */
    data class LogLine(
        val level: LogLevel,
        val timestamp: String,
        val tag: String,
        val message: String,
        val rawLine: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_Dialog_Fullscreen)
        
        val logContent = arguments?.getString(ARG_LOG_CONTENT) ?: getSampleLogs()
        allLogLines = parseLogContent(logContent)
        filteredLogLines = allLogLines
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Title bar
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(context).apply {
            text = getString(R.string.debug_logs)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(titleView)

        val closeButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.action_close)
            setOnClickListener { dismiss() }
        }
        titleBar.addView(closeButton)
        
        rootLayout.addView(titleBar)

        // Search bar
        val searchInput = EditText(context).apply {
            hint = getString(R.string.search_logs)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
            }
            doAfterTextChanged { text ->
                searchQuery = text?.toString() ?: ""
                applyFilters()
            }
        }
        rootLayout.addView(searchInput)

        // Filter chips
        val filterChipGroup = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding / 2
            }
        }
        
        val chipLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // All chip
        val allChip = Chip(context).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = true
            setOnClickListener {
                currentFilter = null
                applyFilters()
                updateChipSelection(this, chipLayout)
            }
        }
        chipLayout.addView(allChip)

        // Level chips
        LogLevel.entries.forEach { level ->
            val chip = Chip(context).apply {
                text = level.name
                isCheckable = true
                setOnClickListener {
                    currentFilter = level
                    applyFilters()
                    updateChipSelection(this, chipLayout)
                }
            }
            chipLayout.addView(chip)
        }

        filterChipGroup.addView(chipLayout)
        rootLayout.addView(filterChipGroup)

        // Log content area
        val logScrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = padding
            }
        }

        val horizontalScroll = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        logTextView = TextView(context).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            textSize = 10f
            setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
            setBackgroundColor(context.getColor(android.R.color.black))
        }
        
        horizontalScroll.addView(logTextView)
        logScrollView.addView(horizontalScroll)
        rootLayout.addView(logScrollView)

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

        val copyButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.copy_to_clipboard)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { copyLogsToClipboard() }
        }
        buttonLayout.addView(copyButton)

        val exportButton = MaterialButton(context).apply {
            text = getString(R.string.export_logs)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding / 2
            }
            setOnClickListener { exportLogs() }
        }
        buttonLayout.addView(exportButton)

        rootLayout.addView(buttonLayout)

        // Initial display
        displayLogs()

        return rootLayout
    }

    private fun updateChipSelection(selectedChip: Chip, chipLayout: LinearLayout) {
        for (i in 0 until chipLayout.childCount) {
            val chip = chipLayout.getChildAt(i) as? Chip
            chip?.isChecked = chip == selectedChip
        }
    }

    private fun applyFilters() {
        filteredLogLines = allLogLines.filter { logLine ->
            val matchesLevel = currentFilter == null || logLine.level == currentFilter
            val matchesSearch = searchQuery.isEmpty() || 
                logLine.rawLine.contains(searchQuery, ignoreCase = true)
            matchesLevel && matchesSearch
        }
        displayLogs()
    }

    private fun displayLogs() {
        val context = requireContext()
        val builder = SpannableStringBuilder()
        
        filteredLogLines.forEach { logLine ->
            val start = builder.length
            builder.append(logLine.rawLine)
            builder.append("\n")
            
            // Color based on log level
            val color = context.getColor(logLine.level.color)
            builder.setSpan(
                ForegroundColorSpan(color),
                start,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            // Highlight search matches
            if (searchQuery.isNotEmpty()) {
                var searchStart = logLine.rawLine.indexOf(searchQuery, ignoreCase = true)
                while (searchStart >= 0) {
                    builder.setSpan(
                        android.text.style.BackgroundColorSpan(
                            context.getColor(android.R.color.holo_orange_light)
                        ),
                        start + searchStart,
                        start + searchStart + searchQuery.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    searchStart = logLine.rawLine.indexOf(
                        searchQuery,
                        searchStart + 1,
                        ignoreCase = true
                    )
                }
            }
        }
        
        logTextView.text = builder
    }

    private fun parseLogContent(content: String): List<LogLine> {
        val logPattern = Regex("""(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEA])\s+([^:]+):\s*(.*)""")
        
        return content.lines().mapNotNull { line ->
            val match = logPattern.find(line)
            if (match != null) {
                val (timestamp, levelChar, tag, message) = match.destructured
                val level = LogLevel.entries.find { it.tag == levelChar } ?: LogLevel.DEBUG
                LogLine(level, timestamp, tag.trim(), message, line)
            } else if (line.isNotBlank()) {
                // Unmatched lines treated as DEBUG
                LogLine(LogLevel.DEBUG, "", "", line, line)
            } else {
                null
            }
        }
    }

    private fun getSampleLogs(): String {
        // Return sample logs for testing when no content provided
        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        return """
            $timestamp 12345 12345 I FastMediaSorter: Application started
            $timestamp 12345 12345 D MainActivity: onCreate called
            $timestamp 12345 12345 D BrowseActivity: Loading resources...
            $timestamp 12345 12345 I ResourceRepository: Found 42 resources
            $timestamp 12345 12345 W NetworkManager: Slow connection detected
            $timestamp 12345 12345 E FileManager: Failed to read file: permission denied
            $timestamp 12345 12345 D PlayerActivity: Video loaded successfully
        """.trimIndent()
    }

    private fun copyLogsToClipboard() {
        val context = requireContext()
        val clipboard = context.getSystemService<ClipboardManager>()
        val logText = filteredLogLines.joinToString("\n") { it.rawLine }
        val clip = ClipData.newPlainText("Debug Logs", logText)
        clipboard?.setPrimaryClip(clip)
        
        Timber.d("Copied ${filteredLogLines.size} log lines to clipboard")
    }

    private fun exportLogs() {
        try {
            val context = requireContext()
            val logText = filteredLogLines.joinToString("\n") { it.rawLine }
            
            // Create log file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val logFile = File(context.cacheDir, "fms_logs_$timestamp.txt")
            FileWriter(logFile).use { it.write(logText) }
            
            // Share via intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.export_logs)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to export logs")
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
