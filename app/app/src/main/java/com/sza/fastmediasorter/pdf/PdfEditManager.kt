package com.sza.fastmediasorter.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF Edit Manager for advanced PDF manipulation using Apache PDFBox-Android
 * 
 * Features:
 * - Rotate PDF pages (90°, 180°, 270°)
 * - Reorder PDF pages (drag & drop, custom order)
 * - Delete PDF pages
 * - Save modified PDF documents
 * - Merge PDFs
 * - Split PDFs
 */
@Singleton
class PdfEditManager @Inject constructor() {
    
    /**
     * Sealed class for PDF operation results
     */
    sealed class PdfResult<out T> {
        data class Success<T>(val data: T) : PdfResult<T>()
        data class Error(val message: String, val exception: Throwable? = null) : PdfResult<Nothing>()
    }
    
    /**
     * Initialize PDFBox (call once at app startup or before first use)
     */
    fun initialize(context: Context) {
        try {
            PDFBoxResourceLoader.init(context)
            Timber.d("PDFBox initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PDFBox")
        }
    }
    
    /**
     * Rotate a specific page in a PDF document
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputFile Output file to save modified PDF
     * @param pageIndex Zero-based page index to rotate
     * @param degrees Rotation angle (90, 180, 270, or -90, -180, -270)
     * @return PdfResult indicating success or failure
     */
    suspend fun rotatePage(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        pageIndex: Int,
        degrees: Int
    ): PdfResult<File> {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                
                if (pageIndex < 0 || pageIndex >= document.numberOfPages) {
                    document.close()
                    return PdfResult.Error("Invalid page index: $pageIndex")
                }
                
                val page = document.getPage(pageIndex)
                val currentRotation = page.rotation
                val newRotation = (currentRotation + degrees) % 360
                page.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                
                document.save(outputFile)
                document.close()
                
                Timber.d("Rotated page $pageIndex by $degrees degrees")
                PdfResult.Success(outputFile)
            } ?: PdfResult.Error("Failed to open PDF")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate PDF page")
            PdfResult.Error("Failed to rotate page: ${e.message}", e)
        }
    }
    
    /**
     * Rotate multiple pages in a PDF document
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputFile Output file to save modified PDF
     * @param rotations Map of page index to rotation degrees
     * @return PdfResult indicating success or failure
     */
    suspend fun rotatePages(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        rotations: Map<Int, Int>
    ): PdfResult<File> {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                
                rotations.forEach { (pageIndex, degrees) ->
                    if (pageIndex in 0 until document.numberOfPages) {
                        val page = document.getPage(pageIndex)
                        val currentRotation = page.rotation
                        val newRotation = (currentRotation + degrees) % 360
                        page.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                    }
                }
                
                document.save(outputFile)
                document.close()
                
                Timber.d("Rotated ${rotations.size} pages")
                PdfResult.Success(outputFile)
            } ?: PdfResult.Error("Failed to open PDF")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate PDF pages")
            PdfResult.Error("Failed to rotate pages: ${e.message}", e)
        }
    }
    
    /**
     * Reorder pages in a PDF document
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputFile Output file to save modified PDF
     * @param newOrder List of page indices in desired order (zero-based)
     * @return PdfResult indicating success or failure
     */
    suspend fun reorderPages(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        newOrder: List<Int>
    ): PdfResult<File> {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val sourceDoc = PDDocument.load(inputStream)
                val targetDoc = PDDocument()
                
                // Verify all indices are valid
                if (newOrder.any { it < 0 || it >= sourceDoc.numberOfPages }) {
                    sourceDoc.close()
                    targetDoc.close()
                    return PdfResult.Error("Invalid page index in new order")
                }
                
                // Add pages in new order
                newOrder.forEach { pageIndex ->
                    val page = sourceDoc.getPage(pageIndex)
                    targetDoc.addPage(page)
                }
                
                targetDoc.save(outputFile)
                targetDoc.close()
                sourceDoc.close()
                
                Timber.d("Reordered PDF with ${newOrder.size} pages")
                PdfResult.Success(outputFile)
            } ?: PdfResult.Error("Failed to open PDF")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to reorder PDF pages")
            PdfResult.Error("Failed to reorder pages: ${e.message}", e)
        }
    }
    
    /**
     * Delete specific pages from a PDF document
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputFile Output file to save modified PDF
     * @param pagesToDelete Set of zero-based page indices to delete
     * @return PdfResult indicating success or failure
     */
    suspend fun deletePages(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        pagesToDelete: Set<Int>
    ): PdfResult<File> {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                
                // Delete pages in reverse order to avoid index shifting
                pagesToDelete.sortedDescending().forEach { pageIndex ->
                    if (pageIndex in 0 until document.numberOfPages) {
                        document.removePage(pageIndex)
                    }
                }
                
                if (document.numberOfPages == 0) {
                    document.close()
                    return PdfResult.Error("Cannot delete all pages from PDF")
                }
                
                document.save(outputFile)
                document.close()
                
                Timber.d("Deleted ${pagesToDelete.size} pages from PDF")
                PdfResult.Success(outputFile)
            } ?: PdfResult.Error("Failed to open PDF")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete PDF pages")
            PdfResult.Error("Failed to delete pages: ${e.message}", e)
        }
    }
    
    /**
     * Extract specific pages to a new PDF
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputFile Output file to save extracted pages
     * @param pagesToExtract List of zero-based page indices to extract
     * @return PdfResult indicating success or failure
     */
    suspend fun extractPages(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        pagesToExtract: List<Int>
    ): PdfResult<File> {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val sourceDoc = PDDocument.load(inputStream)
                val targetDoc = PDDocument()
                
                pagesToExtract.forEach { pageIndex ->
                    if (pageIndex in 0 until sourceDoc.numberOfPages) {
                        val page = sourceDoc.getPage(pageIndex)
                        targetDoc.addPage(page)
                    }
                }
                
                if (targetDoc.numberOfPages == 0) {
                    sourceDoc.close()
                    targetDoc.close()
                    return PdfResult.Error("No valid pages to extract")
                }
                
                targetDoc.save(outputFile)
                targetDoc.close()
                sourceDoc.close()
                
                Timber.d("Extracted ${pagesToExtract.size} pages to new PDF")
                PdfResult.Success(outputFile)
            } ?: PdfResult.Error("Failed to open PDF")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract PDF pages")
            PdfResult.Error("Failed to extract pages: ${e.message}", e)
        }
    }
    
    /**
     * Merge multiple PDF documents into one
     * 
     * @param context Application context
     * @param sourceUris List of source PDF URIs to merge
     * @param outputFile Output file to save merged PDF
     * @return PdfResult indicating success or failure
     */
    suspend fun mergePdfs(
        context: Context,
        sourceUris: List<Uri>,
        outputFile: File
    ): PdfResult<File> {
        return try {
            val targetDoc = PDDocument()
            
            sourceUris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val sourceDoc = PDDocument.load(inputStream)
                    
                    for (i in 0 until sourceDoc.numberOfPages) {
                        val page = sourceDoc.getPage(i)
                        targetDoc.addPage(page)
                    }
                    
                    sourceDoc.close()
                }
            }
            
            if (targetDoc.numberOfPages == 0) {
                targetDoc.close()
                return PdfResult.Error("No pages to merge")
            }
            
            targetDoc.save(outputFile)
            targetDoc.close()
            
            Timber.d("Merged ${sourceUris.size} PDFs into ${outputFile.name}")
            PdfResult.Success(outputFile)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to merge PDFs")
            PdfResult.Error("Failed to merge PDFs: ${e.message}", e)
        }
    }
    
    /**
     * Split PDF into separate single-page PDFs
     * 
     * @param context Application context
     * @param sourceUri Source PDF URI
     * @param outputDir Directory to save split PDFs
     * @param baseFileName Base name for output files
     * @return PdfResult with list of created files
     */
    suspend fun splitPdf(
        context: Context,
        sourceUri: Uri,
        outputDir: File,
        baseFileName: String = "page"
    ): PdfResult<List<File>> {
        return try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFiles = mutableListOf<File>()
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val sourceDoc = PDDocument.load(inputStream)
                
                for (i in 0 until sourceDoc.numberOfPages) {
                    val singlePageDoc = PDDocument()
                    val page = sourceDoc.getPage(i)
                    singlePageDoc.addPage(page)
                    
                    val outputFile = File(outputDir, "${baseFileName}_${i + 1}.pdf")
                    singlePageDoc.save(outputFile)
                    singlePageDoc.close()
                    
                    outputFiles.add(outputFile)
                }
                
                sourceDoc.close()
            }
            
            Timber.d("Split PDF into ${outputFiles.size} files")
            PdfResult.Success(outputFiles)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to split PDF")
            PdfResult.Error("Failed to split PDF: ${e.message}", e)
        }
    }
    
    /**
     * Get PDF page count (lightweight operation)
     */
    fun getPageCount(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val count = document.numberOfPages
                document.close()
                count
            } ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get page count")
            0
        }
    }
}
