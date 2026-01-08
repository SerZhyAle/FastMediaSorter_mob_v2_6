package com.sza.fastmediasorter.ocr

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Manager using ML Kit Text Recognition
 * Provides text detection and extraction from images
 */
@Singleton
class OcrManager @Inject constructor() {
    
    private var textRecognizer: TextRecognizer? = null
    
    /**
     * Data class representing a detected text block
     */
    data class TextBlock(
        val text: String,
        val boundingBox: RectF?,
        val confidence: Float?,
        val lines: List<TextLine>
    )
    
    /**
     * Data class representing a line of text within a block
     */
    data class TextLine(
        val text: String,
        val boundingBox: RectF?,
        val confidence: Float?,
        val elements: List<TextElement>
    )
    
    /**
     * Data class representing a single word/element
     */
    data class TextElement(
        val text: String,
        val boundingBox: RectF?,
        val confidence: Float?
    )
    
    /**
     * Result of OCR processing
     */
    data class OcrResult(
        val fullText: String,
        val blocks: List<TextBlock>,
        val processingTimeMs: Long
    )
    
    /**
     * Initialize the text recognizer (lazy initialization)
     */
    private fun getRecognizer(): TextRecognizer {
        if (textRecognizer == null) {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
        return textRecognizer!!
    }
    
    /**
     * Perform OCR on a bitmap image
     * @param bitmap The image to process
     * @return OcrResult containing detected text and positions
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult {
        val startTime = System.currentTimeMillis()
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        val visionText = processImage(inputImage)
        
        val blocks = visionText.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox?.let { 
                    RectF(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
                },
                confidence = null, // ML Kit doesn't provide block-level confidence
                lines = block.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox?.let {
                            RectF(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
                        },
                        confidence = line.confidence,
                        elements = line.elements.map { element ->
                            TextElement(
                                text = element.text,
                                boundingBox = element.boundingBox?.let {
                                    RectF(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
                                },
                                confidence = element.confidence
                            )
                        }
                    )
                }
            )
        }
        
        val processingTime = System.currentTimeMillis() - startTime
        
        return OcrResult(
            fullText = visionText.text,
            blocks = blocks,
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Process image and get Vision Text result
     */
    private suspend fun processImage(inputImage: InputImage): Text {
        return suspendCancellableCoroutine { continuation ->
            getRecognizer().process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
    
    /**
     * Extract just the text from an image (simplified method)
     */
    suspend fun extractText(bitmap: Bitmap): String {
        return recognizeText(bitmap).fullText
    }
    
    /**
     * Get text blocks with their positions (for overlay display)
     */
    suspend fun getTextBlocksWithPositions(bitmap: Bitmap): List<TextBlock> {
        return recognizeText(bitmap).blocks
    }
    
    /**
     * Find text at a specific point in the image
     * @param bitmap The image
     * @param x X coordinate in the image
     * @param y Y coordinate in the image
     * @return The text element at that point, or null
     */
    suspend fun findTextAtPoint(bitmap: Bitmap, x: Float, y: Float): TextElement? {
        val result = recognizeText(bitmap)
        
        for (block in result.blocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    element.boundingBox?.let { box ->
                        if (box.contains(x, y)) {
                            return element
                        }
                    }
                }
            }
        }
        return null
    }
    
    /**
     * Find text within a rectangular region
     * @param bitmap The image
     * @param region The region to search
     * @return All text elements within the region
     */
    suspend fun findTextInRegion(bitmap: Bitmap, region: RectF): List<TextElement> {
        val result = recognizeText(bitmap)
        val foundElements = mutableListOf<TextElement>()
        
        for (block in result.blocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    element.boundingBox?.let { box ->
                        if (RectF.intersects(box, region)) {
                            foundElements.add(element)
                        }
                    }
                }
            }
        }
        return foundElements
    }
    
    /**
     * Get text blocks grouped by approximate line (for translation overlay)
     * Uses vertical position clustering to group text
     */
    suspend fun getTextByLines(bitmap: Bitmap): List<String> {
        val result = recognizeText(bitmap)
        return result.blocks.flatMap { block ->
            block.lines.map { it.text }
        }
    }
    
    /**
     * Clean up resources
     */
    fun close() {
        textRecognizer?.close()
        textRecognizer = null
    }
}
