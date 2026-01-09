package com.sza.fastmediasorter.ui.player.helpers

import timber.log.Timber
import java.io.File
import java.util.regex.Pattern

/**
 * Result of a search operation.
 */
data class SearchResult(
    val text: String,
    val startPosition: Int,
    val endPosition: Int,
    val lineNumber: Int,
    val lineContent: String,
    val context: String
)

/**
 * Base interface for document search.
 */
interface DocumentSearchHelper {
    fun search(query: String, ignoreCase: Boolean = true): List<SearchResult>
    fun clear()
}

/**
 * Search helper for text files.
 * Searches within text content and returns results with line context.
 */
class TextSearchHelper : DocumentSearchHelper {

    private var content: String = ""
    private var lines: List<String> = emptyList()
    private var currentResults: List<SearchResult> = emptyList()
    private var currentResultIndex: Int = -1

    /**
     * Load text content from file.
     */
    fun loadFromFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                return false
            }
            content = file.readText(Charsets.UTF_8)
            lines = content.lines()
            Timber.d("Loaded ${lines.size} lines for search from $filePath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load content for search: $filePath")
            false
        }
    }

    /**
     * Load content directly from string.
     */
    fun loadFromString(text: String) {
        content = text
        lines = content.lines()
    }

    override fun search(query: String, ignoreCase: Boolean): List<SearchResult> {
        if (query.isBlank()) {
            currentResults = emptyList()
            currentResultIndex = -1
            return currentResults
        }

        val results = mutableListOf<SearchResult>()
        val pattern = if (ignoreCase) {
            Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile(Pattern.quote(query))
        }

        var position = 0
        lines.forEachIndexed { lineIndex, line ->
            val matcher = pattern.matcher(line)
            while (matcher.find()) {
                val globalStart = position + matcher.start()
                val globalEnd = position + matcher.end()
                
                // Build context (50 chars before and after)
                val contextStart = maxOf(0, matcher.start() - 50)
                val contextEnd = minOf(line.length, matcher.end() + 50)
                val context = buildString {
                    if (contextStart > 0) append("...")
                    append(line.substring(contextStart, contextEnd))
                    if (contextEnd < line.length) append("...")
                }

                results.add(
                    SearchResult(
                        text = matcher.group(),
                        startPosition = globalStart,
                        endPosition = globalEnd,
                        lineNumber = lineIndex + 1,
                        lineContent = line,
                        context = context
                    )
                )
            }
            position += line.length + 1 // +1 for newline
        }

        currentResults = results
        currentResultIndex = if (results.isNotEmpty()) 0 else -1
        
        Timber.d("Found ${results.size} matches for '$query'")
        return currentResults
    }

    override fun clear() {
        currentResults = emptyList()
        currentResultIndex = -1
    }

    /**
     * Get current result index (0-based).
     */
    fun getCurrentIndex(): Int = currentResultIndex

    /**
     * Get total result count.
     */
    fun getResultCount(): Int = currentResults.size

    /**
     * Get current result.
     */
    fun getCurrentResult(): SearchResult? {
        return if (currentResultIndex in currentResults.indices) {
            currentResults[currentResultIndex]
        } else null
    }

    /**
     * Navigate to next result.
     * Returns the result or null if no results.
     */
    fun nextResult(): SearchResult? {
        if (currentResults.isEmpty()) return null
        currentResultIndex = (currentResultIndex + 1) % currentResults.size
        return currentResults[currentResultIndex]
    }

    /**
     * Navigate to previous result.
     * Returns the result or null if no results.
     */
    fun previousResult(): SearchResult? {
        if (currentResults.isEmpty()) return null
        currentResultIndex = if (currentResultIndex <= 0) {
            currentResults.size - 1
        } else {
            currentResultIndex - 1
        }
        return currentResults[currentResultIndex]
    }

    /**
     * Get all results.
     */
    fun getAllResults(): List<SearchResult> = currentResults
}
