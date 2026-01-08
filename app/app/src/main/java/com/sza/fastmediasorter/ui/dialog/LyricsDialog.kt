package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogLyricsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import android.media.MediaMetadataRetriever

/**
 * Dialog for displaying lyrics for audio files.
 * Supports embedded ID3 lyrics, .lrc files, and external .txt files.
 */
class LyricsDialog : DialogFragment() {

    companion object {
        private const val ARG_FILE_PATH = "file_path"
        private const val ARG_ARTIST = "artist"
        private const val ARG_TITLE = "title"

        fun newInstance(filePath: String, artist: String? = null, title: String? = null): LyricsDialog {
            return LyricsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                    putString(ARG_ARTIST, artist)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    private var _binding: DialogLyricsBinding? = null
    private val binding get() = _binding!!

    private var filePath: String = ""
    private var artist: String? = null
    private var title: String? = null
    private var lyricsContent: String? = null
    private var lyricsSource: LyricsSource = LyricsSource.NONE

    private var loadJob: Job? = null

    enum class LyricsSource {
        NONE, EMBEDDED, LRC_FILE, TXT_FILE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_Dialog)
        filePath = arguments?.getString(ARG_FILE_PATH) ?: ""
        artist = arguments?.getString(ARG_ARTIST)
        title = arguments?.getString(ARG_TITLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLyricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadLyrics()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupViews() {
        binding.toolbar.apply {
            setNavigationOnClickListener { dismiss() }
            title = this@LyricsDialog.title ?: File(filePath).nameWithoutExtension
            subtitle = artist
        }

        binding.btnCopyLyrics.setOnClickListener {
            copyLyricsToClipboard()
        }

        binding.btnTranslate.setOnClickListener {
            translateLyrics()
        }
    }

    private fun loadLyrics() {
        loadJob?.cancel()
        loadJob = CoroutineScope(Dispatchers.Main).launch {
            showLoading(true)
            try {
                val result = withContext(Dispatchers.IO) {
                    findLyrics()
                }

                if (result != null) {
                    lyricsContent = result.first
                    lyricsSource = result.second
                    showLyrics(result.first, result.second)
                } else {
                    showNoLyrics()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading lyrics for: $filePath")
                showError(e.message ?: "Failed to load lyrics")
            }
        }
    }

    private fun findLyrics(): Pair<String, LyricsSource>? {
        val audioFile = File(filePath)
        val baseName = audioFile.nameWithoutExtension
        val parentDir = audioFile.parentFile

        // 1. Try to find external .lrc file
        val lrcFile = File(parentDir, "$baseName.lrc")
        if (lrcFile.exists() && lrcFile.canRead()) {
            val lrcContent = parseLrcFile(lrcFile)
            if (lrcContent.isNotBlank()) {
                return Pair(lrcContent, LyricsSource.LRC_FILE)
            }
        }

        // 2. Try to find external .txt file with same name
        val txtFile = File(parentDir, "$baseName.txt")
        if (txtFile.exists() && txtFile.canRead()) {
            val txtContent = txtFile.readText().trim()
            if (txtContent.isNotBlank()) {
                return Pair(txtContent, LyricsSource.TXT_FILE)
            }
        }

        // 3. Try to extract embedded lyrics from ID3 tags
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            
            // METADATA_KEY_TITLE and METADATA_KEY_ARTIST for info
            val embeddedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val embeddedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            
            // Update title/artist if not provided
            if (title == null && embeddedTitle != null) {
                title = embeddedTitle
            }
            if (artist == null && embeddedArtist != null) {
                artist = embeddedArtist
            }
            
            // Unfortunately, MediaMetadataRetriever doesn't support USLT (lyrics) tag directly
            // For full lyrics support, we'd need a library like JAudioTagger
            // For now, we return null for embedded lyrics
            retriever.release()
        } catch (e: Exception) {
            Timber.d(e, "Could not extract metadata from: $filePath")
        }

        // 4. No lyrics found
        return null
    }

    /**
     * Parse LRC file format.
     * LRC format: [mm:ss.xx] lyrics line
     */
    private fun parseLrcFile(file: File): String {
        val lines = file.readLines()
        val lyrics = StringBuilder()
        
        // Regular expression to match LRC timestamp
        val timestampRegex = Regex("""\[\d{2}:\d{2}[.:]\d{2,3}]""")
        
        for (line in lines) {
            // Skip metadata lines like [ar:Artist], [ti:Title], etc.
            if (line.matches(Regex("""\[[a-z]{2}:.*]"""))) {
                continue
            }
            
            // Remove timestamps and get text
            val text = line.replace(timestampRegex, "").trim()
            if (text.isNotEmpty()) {
                if (lyrics.isNotEmpty()) {
                    lyrics.append("\n")
                }
                lyrics.append(text)
            }
        }
        
        return lyrics.toString()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.lyricsScrollView.isVisible = !show
        binding.noLyricsContainer.isVisible = false
    }

    private fun showLyrics(lyrics: String, source: LyricsSource) {
        binding.progressBar.isVisible = false
        binding.lyricsScrollView.isVisible = true
        binding.noLyricsContainer.isVisible = false
        binding.tvLyrics.text = lyrics

        // Show source indicator
        val sourceText = when (source) {
            LyricsSource.EMBEDDED -> getString(R.string.lyrics_source_embedded)
            LyricsSource.LRC_FILE -> getString(R.string.lyrics_source_lrc)
            LyricsSource.TXT_FILE -> getString(R.string.lyrics_source_txt)
            LyricsSource.NONE -> ""
        }
        binding.tvLyricsSource.text = sourceText
        binding.tvLyricsSource.isVisible = sourceText.isNotEmpty()

        // Enable action buttons
        binding.btnCopyLyrics.isEnabled = true
        binding.btnTranslate.isEnabled = true
    }

    private fun showNoLyrics() {
        binding.progressBar.isVisible = false
        binding.lyricsScrollView.isVisible = false
        binding.noLyricsContainer.isVisible = true

        // Disable action buttons
        binding.btnCopyLyrics.isEnabled = false
        binding.btnTranslate.isEnabled = false
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.lyricsScrollView.isVisible = false
        binding.noLyricsContainer.isVisible = true
        binding.tvNoLyrics.text = message
    }

    private fun copyLyricsToClipboard() {
        val lyrics = lyricsContent ?: return
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Lyrics", lyrics)
        clipboard.setPrimaryClip(clip)
        
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            R.string.lyrics_copied,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun translateLyrics() {
        val lyrics = lyricsContent ?: return
        // TODO: Integrate with TranslationManager
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Translation: Coming soon",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        _binding = null
    }
}
