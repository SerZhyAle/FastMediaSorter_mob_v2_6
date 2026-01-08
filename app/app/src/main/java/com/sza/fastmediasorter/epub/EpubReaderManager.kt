package com.sza.fastmediasorter.epub

import android.content.Context
import android.net.Uri
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EPUB Reader Manager - Basic EPUB parsing and rendering
 * 
 * EPUB is essentially a ZIP file containing XHTML files and metadata
 * This implementation provides basic chapter extraction and HTML content
 * 
 * Features:
 * - Parse EPUB structure
 * - Extract chapter list from navigation
 * - Get chapter HTML content
 * - Support for font size and family customization
 */
@Singleton
class EpubReaderManager @Inject constructor() {
    
    /**
     * Data class for EPUB metadata
     */
    data class EpubInfo(
        val title: String,
        val author: String,
        val chapters: List<Chapter>,
        val isValid: Boolean
    )
    
    /**
     * Data class for chapter information
     */
    data class Chapter(
        val title: String,
        val href: String,
        val index: Int
    )
    
    /**
     * Result wrapper for operations
     */
    sealed class EpubResult<out T> {
        data class Success<T>(val data: T) : EpubResult<T>()
        data class Error(val message: String) : EpubResult<Nothing>()
    }
    
    /**
     * Parse EPUB file and extract metadata
     */
    fun parseEpub(context: Context, uri: Uri): EpubResult<EpubInfo> {
        return try {
            val chapters = mutableListOf<Chapter>()
            var title = "Unknown"
            var author = "Unknown"
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                
                while (entry != null) {
                    val name = entry.name
                    
                    // Look for navigation file (toc.ncx or nav.xhtml)
                    if (name.endsWith("toc.ncx") || name.endsWith("nav.xhtml")) {
                        val content = readZipEntry(zipInputStream)
                        // Basic parsing - in production, use XML parser
                        parseNavigation(content, chapters)
                    }
                    
                    // Look for metadata (content.opf)
                    if (name.endsWith("content.opf") || name.endsWith(".opf")) {
                        val content = readZipEntry(zipInputStream)
                        title = extractMetadata(content, "dc:title") ?: title
                        author = extractMetadata(content, "dc:creator") ?: author
                    }
                    
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
            
            if (chapters.isEmpty()) {
                // Fallback: create single chapter
                chapters.add(Chapter("Chapter 1", "content.html", 0))
            }
            
            EpubResult.Success(
                EpubInfo(
                    title = title,
                    author = author,
                    chapters = chapters,
                    isValid = true
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse EPUB")
            EpubResult.Error("Failed to parse EPUB: ${e.message}")
        }
    }
    
    /**
     * Get HTML content for a specific chapter
     */
    fun getChapterContent(
        context: Context,
        uri: Uri,
        chapterHref: String,
        fontSize: Int = 16,
        fontFamily: String = "serif"
    ): EpubResult<String> {
        return try {
            var chapterHtml: String? = null
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                
                while (entry != null) {
                    if (entry.name.endsWith(chapterHref) || entry.name.contains(chapterHref)) {
                        chapterHtml = readZipEntry(zipInputStream)
                        break
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
            
            if (chapterHtml != null) {
                // Inject custom CSS for font size and family
                val styledHtml = injectStyles(chapterHtml!!, fontSize, fontFamily)
                EpubResult.Success(styledHtml)
            } else {
                EpubResult.Error("Chapter not found: $chapterHref")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to read chapter")
            EpubResult.Error("Failed to read chapter: ${e.message}")
        }
    }
    
    /**
     * Get all chapter contents as HTML
     */
    fun getAllChaptersContent(
        context: Context,
        uri: Uri,
        fontSize: Int = 16,
        fontFamily: String = "serif"
    ): EpubResult<String> {
        return try {
            val epubInfo = when (val result = parseEpub(context, uri)) {
                is EpubResult.Success -> result.data
                is EpubResult.Error -> return result
            }
            
            val allHtml = StringBuilder()
            allHtml.append("<html><head>")
            allHtml.append("<style>")
            allHtml.append("body { font-size: ${fontSize}px; font-family: $fontFamily; padding: 16px; }")
            allHtml.append(".chapter { page-break-before: always; }")
            allHtml.append("</style>")
            allHtml.append("</head><body>")
            
            epubInfo.chapters.forEach { chapter ->
                when (val result = getChapterContent(context, uri, chapter.href, fontSize, fontFamily)) {
                    is EpubResult.Success -> {
                        allHtml.append("<div class='chapter'>")
                        allHtml.append("<h2>${chapter.title}</h2>")
                        allHtml.append(result.data)
                        allHtml.append("</div>")
                    }
                    is EpubResult.Error -> Timber.w("Failed to load chapter: ${chapter.title}")
                }
            }
            
            allHtml.append("</body></html>")
            EpubResult.Success(allHtml.toString())
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all chapters")
            EpubResult.Error("Failed to get chapters: ${e.message}")
        }
    }
    
    // Helper functions
    
    private fun readZipEntry(zipInputStream: ZipInputStream): String {
        val reader = BufferedReader(InputStreamReader(zipInputStream))
        return reader.readText()
    }
    
    private fun parseNavigation(content: String, chapters: MutableList<Chapter>) {
        // Simple regex-based parsing (in production, use XML parser like XmlPullParser)
        val navPointRegex = """<navPoint[^>]*>.*?<text>(.*?)</text>.*?<content src="([^"]+)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        navPointRegex.findAll(content).forEachIndexed { index, match ->
            val title = match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            val href = match.groupValues[2]
            chapters.add(Chapter(title, href, index))
        }
    }
    
    private fun extractMetadata(content: String, tagName: String): String? {
        val regex = """<$tagName[^>]*>(.*?)</$tagName>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return regex.find(content)?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim()
    }
    
    private fun injectStyles(html: String, fontSize: Int, fontFamily: String): String {
        val style = """
            <style>
                body {
                    font-size: ${fontSize}px;
                    font-family: $fontFamily;
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    background-color: #ffffff;
                    color: #333333;
                }
                p { margin-bottom: 1em; }
                img { max-width: 100%; height: auto; }
            </style>
        """.trimIndent()
        
        return if (html.contains("<head>", ignoreCase = true)) {
            html.replace("</head>", "$style</head>", ignoreCase = true)
        } else if (html.contains("<html>", ignoreCase = true)) {
            html.replace("<html>", "<html><head>$style</head>", ignoreCase = true)
        } else {
            "<html><head>$style</head><body>$html</body></html>"
        }
    }
    
    /**
     * Search for text across all chapters
     */
    fun searchInEpub(
        context: Context,
        uri: Uri,
        query: String
    ): EpubResult<List<SearchResult>> {
        return try {
            val results = mutableListOf<SearchResult>()
            val epubInfo = when (val result = parseEpub(context, uri)) {
                is EpubResult.Success -> result.data
                is EpubResult.Error -> return result
            }
            
            epubInfo.chapters.forEach { chapter ->
                when (val result = getChapterContent(context, uri, chapter.href)) {
                    is EpubResult.Success -> {
                        val content = result.data.replace(Regex("<[^>]*>"), " ")
                        if (content.contains(query, ignoreCase = true)) {
                            // Find context around match
                            val index = content.indexOf(query, ignoreCase = true)
                            val start = maxOf(0, index - 50)
                            val end = minOf(content.length, index + query.length + 50)
                            val snippet = content.substring(start, end)
                            
                            results.add(
                                SearchResult(
                                    chapterTitle = chapter.title,
                                    chapterIndex = chapter.index,
                                    snippet = snippet,
                                    position = index
                                )
                            )
                        }
                    }
                    is EpubResult.Error -> { /* Skip chapter on error */ }
                }
            }
            
            EpubResult.Success(results)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to search EPUB")
            EpubResult.Error("Search failed: ${e.message}")
        }
    }
    
    data class SearchResult(
        val chapterTitle: String,
        val chapterIndex: Int,
        val snippet: String,
        val position: Int
    )
}
