package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.lyrics.LyricsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog for displaying lyrics synced with audio playback
 */
class LyricsViewerDialog : DialogFragment() {
    
    companion object {
        const val TAG = "LyricsViewerDialog"
        
        private var pendingUri: Uri? = null
        private var pendingPositionProvider: (() -> Long)? = null
        
        fun newInstance(
            audioUri: Uri,
            positionProvider: (() -> Long)? = null
        ): LyricsViewerDialog {
            pendingUri = audioUri
            pendingPositionProvider = positionProvider
            return LyricsViewerDialog()
        }
    }
    
    private var audioUri: Uri? = null
    private var positionProvider: (() -> Long)? = null
    private var lyricsManager: LyricsManager? = null
    private var currentLyrics: LyricsManager.Lyrics? = null
    
    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var infoText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var noLyricsText: TextView
    private lateinit var btnCopy: MaterialButton
    
    // Adapter
    private var adapter: LyricsAdapter? = null
    
    // Sync handler for updating current line
    private val handler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null
    private var lastHighlightedIndex = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioUri = pendingUri
        positionProvider = pendingPositionProvider
        pendingUri = null
        pendingPositionProvider = null
        
        lyricsManager = LyricsManager()
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_lyrics_viewer, null)
        
        progressBar = view.findViewById(R.id.progressBar)
        infoText = view.findViewById(R.id.infoText)
        recyclerView = view.findViewById(R.id.recyclerView)
        noLyricsText = view.findViewById(R.id.noLyricsText)
        btnCopy = view.findViewById(R.id.btnCopy)
        
        setupRecyclerView()
        setupClickListeners()
        
        // Load lyrics
        audioUri?.let { loadLyrics(it) }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.lyrics)
            .setView(view)
            .setNegativeButton(R.string.close, null)
            .create()
    }
    
    private fun setupRecyclerView() {
        adapter = LyricsAdapter(emptyList(), -1)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnCopy.setOnClickListener {
            currentLyrics?.let { lyrics ->
                val text = lyricsManager?.formatAsText(lyrics) ?: ""
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Lyrics", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.lyrics_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadLyrics(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        infoText.text = getString(R.string.loading_lyrics)
        noLyricsText.visibility = View.GONE
        recyclerView.visibility = View.GONE
        btnCopy.visibility = View.GONE
        
        lifecycleScope.launch {
            val lyrics = withContext(Dispatchers.IO) {
                lyricsManager?.loadLyrics(requireContext(), uri)
            }
            
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                
                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                    currentLyrics = lyrics
                    displayLyrics(lyrics)
                } else {
                    noLyricsText.visibility = View.VISIBLE
                    infoText.text = getString(R.string.lyrics_not_found)
                }
            }
        }
    }
    
    private fun displayLyrics(lyrics: LyricsManager.Lyrics) {
        // Update info text
        val sourceText = when (lyrics.source) {
            LyricsManager.LyricsSource.EMBEDDED -> getString(R.string.lyrics_source_embedded)
            LyricsManager.LyricsSource.LRC_FILE -> getString(R.string.lyrics_source_lrc)
            LyricsManager.LyricsSource.TXT_FILE -> getString(R.string.lyrics_source_txt)
            LyricsManager.LyricsSource.ONLINE -> getString(R.string.lyrics_source_online)
        }
        
        val syncText = if (lyrics.isSynchronized) {
            getString(R.string.lyrics_synced)
        } else {
            getString(R.string.lyrics_unsynced)
        }
        
        infoText.text = "$sourceText • $syncText"
        
        // Display lyrics
        adapter = LyricsAdapter(lyrics.lines, -1)
        recyclerView.adapter = adapter
        recyclerView.visibility = View.VISIBLE
        btnCopy.visibility = View.VISIBLE
        
        // Start sync if synchronized lyrics and position provider available
        if (lyrics.isSynchronized && positionProvider != null) {
            startLyricsSync()
        }
    }
    
    private fun startLyricsSync() {
        syncRunnable = object : Runnable {
            override fun run() {
                val lyrics = currentLyrics ?: return
                val position = positionProvider?.invoke() ?: return
                
                val currentIndex = lyricsManager?.getCurrentLineIndex(lyrics, position) ?: -1
                
                if (currentIndex != lastHighlightedIndex && currentIndex >= 0) {
                    lastHighlightedIndex = currentIndex
                    adapter?.highlightLine(currentIndex)
                    
                    // Scroll to current line
                    recyclerView.smoothScrollToPosition(currentIndex)
                }
                
                // Update every 100ms
                handler.postDelayed(this, 100)
            }
        }
        
        syncRunnable?.let { handler.post(it) }
    }
    
    private fun stopLyricsSync() {
        syncRunnable?.let { handler.removeCallbacks(it) }
        syncRunnable = null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopLyricsSync()
    }
    
    /**
     * Adapter for lyrics lines
     */
    private class LyricsAdapter(
        private val lines: List<LyricsManager.LyricLine>,
        private var highlightedIndex: Int
    ) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val lyricText: TextView = view.findViewById(R.id.lyricText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lyric_line, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val line = lines[position]
            holder.lyricText.text = line.text
            
            if (position == highlightedIndex) {
                holder.lyricText.setTypeface(null, Typeface.BOLD)
                holder.lyricText.alpha = 1.0f
                holder.lyricText.textSize = 18f
            } else {
                holder.lyricText.setTypeface(null, Typeface.NORMAL)
                holder.lyricText.alpha = 0.7f
                holder.lyricText.textSize = 16f
            }
        }
        
        override fun getItemCount(): Int = lines.size
        
        fun highlightLine(index: Int) {
            val oldIndex = highlightedIndex
            highlightedIndex = index
            
            if (oldIndex >= 0) {
                notifyItemChanged(oldIndex)
            }
            if (index >= 0 && index < lines.size) {
                notifyItemChanged(index)
            }
        }
    }
}
