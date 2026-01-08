package com.sza.fastmediasorter.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for lyrics handling and synchronization
 * 
 * Features:
 * - Extract embedded lyrics from audio files
 * - Load external .lrt and .txt lyrics files
 * - Parse LRC format with timestamps
 * - Sync lyrics with playback position
 */
@Singleton
class LyricsManager @Inject constructor() {
    
    /**
     * A single lyric line with optional timestamp
     */
    data class LyricLine(
        val text: String,
        val startTimeMs: Long?, // null for unsynchronized lyrics
        val endTimeMs: Long? = null // optional end time
    )
    
    /**
     * Full lyrics data
     */
    data class Lyrics(
        val lines: List<LyricLine>,
        val isSynchronized: Boolean,
        val title: String?,
        val artist: String?,
        val album: String?,
        val source: LyricsSource
    )
    
    /**
     * Source of lyrics
     */
    enum class LyricsSource {
        EMBEDDED, // From audio file metadata
        LRC_FILE, // From .lrc file
        TXT_FILE, // From .txt file
        ONLINE    // From online service (future)
    }
    
    companion object {
        // LRC timestamp pattern: [mm:ss.xx] or [mm:ss]
        private val LRC_TIMESTAMP_PATTERN = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]""")
        
        // LRC metadata patterns
        private val LRC_TITLE_PATTERN = Regex("""\[ti:(.+?)\]""", RegexOption.IGNORE_CASE)
        private val LRC_ARTIST_PATTERN = Regex("""\[ar:(.+?)\]""", RegexOption.IGNORE_CASE)
        private val LRC_ALBUM_PATTERN = Regex("""\[al:(.+?)\]""", RegexOption.IGNORE_CASE)
    }
    
    /**
     * Load lyrics for an audio file
     * Tries embedded lyrics first, then external files
     */
    fun loadLyrics(context: Context, audioUri: Uri): Lyrics? {
        // Try embedded lyrics first
        val embedded = loadEmbeddedLyrics(context, audioUri)
        if (embedded != null) {
            return embedded
        }
        
        // Try to find external .lrc or .txt file
        return loadExternalLyrics(context, audioUri)
    }
    
    /**
     * Load embedded lyrics from audio file metadata
     */
    fun loadEmbeddedLyrics(context: Context, audioUri: Uri): Lyrics? {
        return try {
            val retriever = MediaMetadataRetriever()
            
            try {
                retriever.setDataSource(context, audioUri)
                
                // Try to get lyrics from metadata
                // Note: MediaMetadataRetriever doesn't have a standard lyrics field
                // Some formats store it in different places
                
                // Get other metadata for context
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                
                // For ID3v2 tags, lyrics would be in USLT frame
                // Android's MediaMetadataRetriever doesn't expose this directly
                // A full implementation would need a library like JAudioTagger
                
                null // Return null for now - no standard way to get embedded lyrics
                
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load embedded lyrics")
            null
        }
    }
    
    /**
     * Load external lyrics file (.lrc or .txt) based on audio file path
     */
    fun loadExternalLyrics(context: Context, audioUri: Uri): Lyrics? {
        val path = audioUri.path ?: return null
        
        // Get the directory and base filename
        val audioFile = File(path)
        val directory = audioFile.parentFile ?: return null
        val baseName = audioFile.nameWithoutExtension
        
        // Try .lrc file first
        val lrcFile = File(directory, "$baseName.lrc")
        if (lrcFile.exists()) {
            return parseLrcFile(lrcFile)
        }
        
        // Try .txt file
        val txtFile = File(directory, "$baseName.txt")
        if (txtFile.exists()) {
            return parseTxtFile(txtFile)
        }
        
        return null
    }
    
    /**
     * Parse an LRC format lyrics file
     */
    fun parseLrcFile(file: File): Lyrics? {
        return try {
            val content = file.readText()
            parseLrcContent(content)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse LRC file")
            null
        }
    }
    
    /**
     * Parse LRC content string
     */
    fun parseLrcContent(content: String): Lyrics {
        val lines = mutableListOf<LyricLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            // Check for metadata
            LRC_TITLE_PATTERN.find(trimmedLine)?.let {
                title = it.groupValues[1]
                return@forEach
            }
            LRC_ARTIST_PATTERN.find(trimmedLine)?.let {
                artist = it.groupValues[1]
                return@forEach
            }
            LRC_ALBUM_PATTERN.find(trimmedLine)?.let {
                album = it.groupValues[1]
                return@forEach
            }
            
            // Parse timestamps and text
            val timestamps = mutableListOf<Long>()
            var text = trimmedLine
            
            LRC_TIMESTAMP_PATTERN.findAll(trimmedLine).forEach { match ->
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val centiseconds = match.groupValues.getOrNull(3)?.padEnd(3, '0')?.toIntOrNull() ?: 0
                
                val timeMs = (minutes * 60 * 1000L) + (seconds * 1000L) + centiseconds
                timestamps.add(timeMs)
                
                // Remove timestamp from text
                text = text.replace(match.value, "")
            }
            
            text = text.trim()
            
            if (text.isNotEmpty()) {
                if (timestamps.isNotEmpty()) {
                    // Add a line for each timestamp (for repeated lyrics)
                    timestamps.forEach { time ->
                        lines.add(LyricLine(text, time))
                    }
                } else {
                    // Unsynchronized line
                    lines.add(LyricLine(text, null))
                }
            }
        }
        
        // Sort by timestamp
        val sortedLines = lines.sortedBy { it.startTimeMs ?: Long.MAX_VALUE }
        
        return Lyrics(
            lines = sortedLines,
            isSynchronized = sortedLines.any { it.startTimeMs != null },
            title = title,
            artist = artist,
            album = album,
            source = LyricsSource.LRC_FILE
        )
    }
    
    /**
     * Parse a plain text lyrics file
     */
    fun parseTxtFile(file: File): Lyrics? {
        return try {
            val content = file.readText()
            val lines = content.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { LyricLine(it, null) }
            
            Lyrics(
                lines = lines,
                isSynchronized = false,
                title = null,
                artist = null,
                album = null,
                source = LyricsSource.TXT_FILE
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse TXT lyrics file")
            null
        }
    }
    
    /**
     * Parse lyrics content from a URI
     */
    fun parseLyricsFromUri(context: Context, uri: Uri): Lyrics? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val content = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            
            // Check if it's LRC format
            if (content.contains(LRC_TIMESTAMP_PATTERN) || 
                content.contains(LRC_TITLE_PATTERN) ||
                content.contains(LRC_ARTIST_PATTERN)) {
                parseLrcContent(content)
            } else {
                // Plain text
                val lines = content.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { LyricLine(it, null) }
                
                Lyrics(
                    lines = lines,
                    isSynchronized = false,
                    title = null,
                    artist = null,
                    album = null,
                    source = LyricsSource.TXT_FILE
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse lyrics from URI")
            null
        }
    }
    
    /**
     * Get the current lyric line based on playback position
     * @param lyrics The lyrics data
     * @param positionMs Current playback position in milliseconds
     * @return Index of the current line, or -1 if no line matches
     */
    fun getCurrentLineIndex(lyrics: Lyrics, positionMs: Long): Int {
        if (!lyrics.isSynchronized) return -1
        
        var currentIndex = -1
        
        for ((index, line) in lyrics.lines.withIndex()) {
            val startTime = line.startTimeMs ?: continue
            if (positionMs >= startTime) {
                currentIndex = index
            } else {
                break // Lines are sorted, so we can stop
            }
        }
        
        return currentIndex
    }
    
    /**
     * Get a window of lyric lines around the current position
     */
    fun getLyricsWindow(
        lyrics: Lyrics,
        positionMs: Long,
        linesBefore: Int = 3,
        linesAfter: Int = 3
    ): Pair<List<LyricLine>, Int> {
        val currentIndex = getCurrentLineIndex(lyrics, positionMs)
        
        if (currentIndex < 0) {
            // For unsynchronized lyrics, return all
            return Pair(lyrics.lines, -1)
        }
        
        val startIndex = maxOf(0, currentIndex - linesBefore)
        val endIndex = minOf(lyrics.lines.size, currentIndex + linesAfter + 1)
        
        val window = lyrics.lines.subList(startIndex, endIndex)
        val highlightIndex = currentIndex - startIndex
        
        return Pair(window, highlightIndex)
    }
    
    /**
     * Format lyrics as plain text
     */
    fun formatAsText(lyrics: Lyrics): String {
        return lyrics.lines.joinToString("\n") { it.text }
    }
    
    /**
     * Save lyrics to an LRC file
     */
    fun saveLrcFile(lyrics: Lyrics, file: File): Boolean {
        return try {
            val sb = StringBuilder()
            
            // Write metadata
            lyrics.title?.let { sb.appendLine("[ti:$it]") }
            lyrics.artist?.let { sb.appendLine("[ar:$it]") }
            lyrics.album?.let { sb.appendLine("[al:$it]") }
            
            sb.appendLine()
            
            // Write lyrics lines
            lyrics.lines.forEach { line ->
                val timeTag = line.startTimeMs?.let { time ->
                    val minutes = (time / 60000).toInt()
                    val seconds = ((time % 60000) / 1000).toInt()
                    val centiseconds = ((time % 1000) / 10).toInt()
                    "[%02d:%02d.%02d]".format(minutes, seconds, centiseconds)
                } ?: ""
                
                sb.appendLine("$timeTag${line.text}")
            }
            
            file.writeText(sb.toString())
            true
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to save LRC file")
            false
        }
    }
}
