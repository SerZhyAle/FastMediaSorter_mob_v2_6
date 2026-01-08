package com.sza.fastmediasorter.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF Tools Manager for PDF manipulation operations
 * 
 * Features:
 * - Page extraction
 * - PDF merging
 * - Page reordering
 * - PDF rendering to bitmap
 * - Page deletion
 * - PDF info retrieval
 */
@Singleton
class PdfToolsManager @Inject constructor() {
    
    /**
     * Data class for PDF document info
     */
    data class PdfInfo(
        val pageCount: Int,
        val fileSizeBytes: Long,
        val fileName: String,
        val isValid: Boolean
    )
    
    /**
     * Data class for page info
     */
    data class PageInfo(
        val pageIndex: Int,
        val width: Int,
        val height: Int
    )
    
    /**
     * Get PDF document information
     */
    fun getPdfInfo(context: Context, uri: Uri): PdfInfo? {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                renderer.close()
                
                // Get file size
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE) ?: -1
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
                
                var fileSize = 0L
                var fileName = "document.pdf"
                
                if (cursor?.moveToFirst() == true) {
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "document.pdf"
                    }
                }
                cursor?.close()
                
                PdfInfo(
                    pageCount = pageCount,
                    fileSizeBytes = fileSize,
                    fileName = fileName,
                    isValid = true
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get PDF info")
            PdfInfo(0, 0, "", false)
        }
    }
    
    /**
     * Render a PDF page to bitmap
     * @param context The context
     * @param uri The PDF file URI
     * @param pageIndex Zero-based page index
     * @param width Desired width (height will be calculated to maintain aspect ratio)
     * @return Bitmap of the rendered page or null
     */
    fun renderPage(context: Context, uri: Uri, pageIndex: Int, width: Int = 1024): Bitmap? {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    renderer.close()
                    return null
                }
                
                val page = renderer.openPage(pageIndex)
                
                // Calculate height to maintain aspect ratio
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val height = (width * aspectRatio).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                page.close()
                renderer.close()
                
                bitmap
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to render PDF page $pageIndex")
            null
        }
    }
    
    /**
     * Render all pages as thumbnails
     */
    fun renderThumbnails(context: Context, uri: Uri, thumbnailWidth: Int = 256): List<Bitmap?> {
        val info = getPdfInfo(context, uri) ?: return emptyList()
        
        return (0 until info.pageCount).map { pageIndex ->
            renderPage(context, uri, pageIndex, thumbnailWidth)
        }
    }
    
    /**
     * Get page info for a specific page
     */
    fun getPageInfo(context: Context, uri: Uri, pageIndex: Int): PageInfo? {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    renderer.close()
                    return null
                }
                
                val page = renderer.openPage(pageIndex)
                val info = PageInfo(
                    pageIndex = pageIndex,
                    width = page.width,
                    height = page.height
                )
                page.close()
                renderer.close()
                
                info
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get page info")
            null
        }
    }
    
    /**
     * Extract specific pages from a PDF and save to a new file
     * Note: This requires iText or PDFBox library for full functionality.
     * This is a placeholder that creates single-page PDFs from rendered bitmaps.
     * 
     * @param context The context
     * @param sourceUri Source PDF URI
     * @param pageIndices List of zero-based page indices to extract
     * @param outputFile Destination file
     * @return true if successful
     */
    fun extractPages(
        context: Context,
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFile: File
    ): Boolean {
        return try {
            // For full PDF manipulation, you'd need iText or PDFBox
            // This is a bitmap-based fallback that creates a simple PDF from rendered pages
            
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
                .build()
            
            val document = PrintedPdfDocument(context, printAttributes)
            
            pageIndices.forEachIndexed { idx, pageIndex ->
                val bitmap = renderPage(context, sourceUri, pageIndex, 2480) // A4 at 300dpi ≈ 2480px
                if (bitmap != null) {
                    val page = document.startPage(idx)
                    val canvas = page.canvas
                    
                    // Scale bitmap to fit page
                    val scaleX = page.info.contentRect.width().toFloat() / bitmap.width
                    val scaleY = page.info.contentRect.height().toFloat() / bitmap.height
                    val scale = minOf(scaleX, scaleY)
                    
                    canvas.save()
                    canvas.translate(
                        page.info.contentRect.left.toFloat(),
                        page.info.contentRect.top.toFloat()
                    )
                    canvas.scale(scale, scale)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.restore()
                    
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }
            
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            
            Timber.d("Extracted ${pageIndices.size} pages to ${outputFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract pages")
            false
        }
    }
    
    /**
     * Save a single page as an image
     */
    fun savePageAsImage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        outputFile: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        return try {
            val bitmap = renderPage(context, uri, pageIndex, 2048) ?: return false
            
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(format, quality, out)
            }
            
            bitmap.recycle()
            Timber.d("Saved page $pageIndex as image: ${outputFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to save page as image")
            false
        }
    }
    
    /**
     * Export all pages as images
     */
    fun exportAllPagesAsImages(
        context: Context,
        uri: Uri,
        outputDir: File,
        baseFileName: String = "page",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): List<File> {
        val exportedFiles = mutableListOf<File>()
        
        val info = getPdfInfo(context, uri) ?: return exportedFiles
        
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.JPEG -> "jpg"
            else -> "png"
        }
        
        for (pageIndex in 0 until info.pageCount) {
            val outputFile = File(outputDir, "${baseFileName}_${pageIndex + 1}.$extension")
            if (savePageAsImage(context, uri, pageIndex, outputFile, format)) {
                exportedFiles.add(outputFile)
            }
        }
        
        return exportedFiles
    }
    
    /**
     * Create a page order array for reordering
     * @param pageCount Total number of pages
     * @param moves List of pairs (fromIndex, toIndex) representing moves
     * @return New page order array
     */
    fun calculateReorderedPages(pageCount: Int, moves: List<Pair<Int, Int>>): List<Int> {
        val pages = (0 until pageCount).toMutableList()
        
        moves.forEach { (from, to) ->
            if (from in pages.indices && to in 0..pages.size) {
                val page = pages.removeAt(from)
                val adjustedTo = if (to > from) to - 1 else to
                pages.add(adjustedTo.coerceIn(0, pages.size), page)
            }
        }
        
        return pages
    }
    
    /**
     * Calculate pages to keep after deletion
     * @param pageCount Total number of pages
     * @param pagesToDelete Set of page indices to delete
     * @return List of page indices to keep
     */
    fun calculatePagesAfterDeletion(pageCount: Int, pagesToDelete: Set<Int>): List<Int> {
        return (0 until pageCount).filter { it !in pagesToDelete }
    }
}
