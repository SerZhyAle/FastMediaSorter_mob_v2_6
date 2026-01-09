package com.sza.fastmediasorter.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sza.fastmediasorter.ocr.OcrManager

/**
 * Custom view that overlays detected text boxes on an image.
 * Provides Google Lens-style tap-to-translate functionality.
 * 
 * Features:
 * - Draws semi-transparent boxes around detected text
 * - Highlights tapped text box
 * - Supports pinch-to-zoom and pan with the underlying image
 * - Translates tapped text and shows result overlay
 */
class TranslationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Represents a text box with its translation
     */
    data class TextBoxData(
        val originalText: String,
        val translatedText: String?,
        val boundingBox: RectF,
        val isTranslating: Boolean = false,
        val isSelected: Boolean = false
    )

    private val textBoxes = mutableListOf<TextBoxData>()
    private var selectedBoxIndex: Int = -1
    
    // Image dimensions for coordinate mapping
    private var imageWidth: Float = 0f
    private var imageHeight: Float = 0f
    private var displayMatrix: Matrix = Matrix()
    private val inverseMatrix: Matrix = Matrix()
    
    // Paint objects
    private val boxPaint = Paint().apply {
        color = Color.argb(60, 0, 150, 255) // Semi-transparent blue
        style = Paint.Style.FILL
    }
    
    private val boxStrokePaint = Paint().apply {
        color = Color.argb(180, 0, 150, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val selectedBoxPaint = Paint().apply {
        color = Color.argb(100, 255, 165, 0) // Semi-transparent orange
        style = Paint.Style.FILL
    }
    
    private val selectedStrokePaint = Paint().apply {
        color = Color.argb(220, 255, 165, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val translatedTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 14f * resources.displayMetrics.density
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    
    private val translatedBgPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }
    
    private val loadingPaint = Paint().apply {
        color = Color.argb(150, 128, 128, 128)
        style = Paint.Style.FILL
    }
    
    // Font size setting (in sp)
    var translationFontSize: Float = 14f
        set(value) {
            field = value.coerceIn(10f, 32f)
            translatedTextPaint.textSize = field * resources.displayMetrics.density
            invalidate()
        }
    
    // Callback for text box tap
    var onTextBoxTapped: ((TextBoxData, Int) -> Unit)? = null
    var onTextBoxLongPressed: ((TextBoxData, Int) -> Unit)? = null

    /**
     * Set OCR results to display
     */
    fun setTextBlocks(blocks: List<OcrManager.TextBlock>, imgWidth: Float, imgHeight: Float) {
        textBoxes.clear()
        imageWidth = imgWidth
        imageHeight = imgHeight
        
        for (block in blocks) {
            block.boundingBox?.let { box ->
                textBoxes.add(
                    TextBoxData(
                        originalText = block.text,
                        translatedText = null,
                        boundingBox = box,
                        isTranslating = false,
                        isSelected = false
                    )
                )
            }
        }
        invalidate()
    }
    
    /**
     * Clear all text boxes
     */
    fun clearTextBoxes() {
        textBoxes.clear()
        selectedBoxIndex = -1
        invalidate()
    }
    
    /**
     * Update translation for a specific text box
     */
    fun updateTranslation(index: Int, translatedText: String?) {
        if (index in textBoxes.indices) {
            textBoxes[index] = textBoxes[index].copy(
                translatedText = translatedText,
                isTranslating = false
            )
            invalidate()
        }
    }
    
    /**
     * Mark a box as translating
     */
    fun setTranslating(index: Int, isTranslating: Boolean) {
        if (index in textBoxes.indices) {
            textBoxes[index] = textBoxes[index].copy(isTranslating = isTranslating)
            invalidate()
        }
    }
    
    /**
     * Select a text box
     */
    fun selectBox(index: Int) {
        selectedBoxIndex = index
        textBoxes.forEachIndexed { i, box ->
            textBoxes[i] = box.copy(isSelected = i == index)
        }
        invalidate()
    }
    
    /**
     * Set the display matrix for coordinate transformation
     * Call this when the underlying image is scaled/translated
     */
    fun setDisplayMatrix(matrix: Matrix) {
        displayMatrix.set(matrix)
        displayMatrix.invert(inverseMatrix)
        invalidate()
    }
    
    /**
     * Get all text boxes
     */
    fun getTextBoxes(): List<TextBoxData> = textBoxes.toList()
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (textBoxes.isEmpty() || imageWidth == 0f || imageHeight == 0f) return
        
        // Calculate scale factors to map image coordinates to view coordinates
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (width - imageWidth * scale) / 2
        val offsetY = (height - imageHeight * scale) / 2
        
        for ((index, box) in textBoxes.withIndex()) {
            val screenBox = RectF(
                box.boundingBox.left * scale + offsetX,
                box.boundingBox.top * scale + offsetY,
                box.boundingBox.right * scale + offsetX,
                box.boundingBox.bottom * scale + offsetY
            )
            
            // Draw box background
            when {
                box.isTranslating -> {
                    canvas.drawRect(screenBox, loadingPaint)
                    canvas.drawRect(screenBox, boxStrokePaint)
                }
                box.isSelected || index == selectedBoxIndex -> {
                    canvas.drawRect(screenBox, selectedBoxPaint)
                    canvas.drawRect(screenBox, selectedStrokePaint)
                }
                box.translatedText != null -> {
                    // Box with translation - draw with highlighted color
                    canvas.drawRect(screenBox, selectedBoxPaint)
                    canvas.drawRect(screenBox, selectedStrokePaint)
                }
                else -> {
                    canvas.drawRect(screenBox, boxPaint)
                    canvas.drawRect(screenBox, boxStrokePaint)
                }
            }
            
            // Draw translated text overlay
            box.translatedText?.let { translated ->
                drawTranslatedText(canvas, translated, screenBox)
            }
        }
    }
    
    private fun drawTranslatedText(canvas: Canvas, text: String, box: RectF) {
        val padding = 4f * resources.displayMetrics.density
        val textHeight = translatedTextPaint.textSize
        
        // Calculate text width (may need to wrap)
        val maxWidth = box.width() - padding * 2
        val lines = wrapText(text, maxWidth)
        
        val totalHeight = lines.size * (textHeight + padding / 2)
        
        // Draw background below the box
        val bgTop = box.bottom + padding / 2
        val bgBottom = bgTop + totalHeight + padding * 2
        val bgRect = RectF(box.left, bgTop, box.right, bgBottom)
        
        canvas.drawRoundRect(bgRect, 4f, 4f, translatedBgPaint)
        
        // Draw text lines
        var y = bgTop + padding + textHeight
        for (line in lines) {
            canvas.drawText(line, box.left + padding, y, translatedTextPaint)
            y += textHeight + padding / 2
        }
    }
    
    private fun wrapText(text: String, maxWidth: Float): List<String> {
        if (maxWidth <= 0) return listOf(text)
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val testWidth = translatedTextPaint.measureText(testLine)
            
            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines.ifEmpty { listOf(text) }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val tappedIndex = findBoxAtPoint(event.x, event.y)
                if (tappedIndex >= 0) {
                    selectBox(tappedIndex)
                    onTextBoxTapped?.invoke(textBoxes[tappedIndex], tappedIndex)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun findBoxAtPoint(x: Float, y: Float): Int {
        if (imageWidth == 0f || imageHeight == 0f) return -1
        
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (width - imageWidth * scale) / 2
        val offsetY = (height - imageHeight * scale) / 2
        
        for ((index, box) in textBoxes.withIndex()) {
            val screenBox = RectF(
                box.boundingBox.left * scale + offsetX,
                box.boundingBox.top * scale + offsetY,
                box.boundingBox.right * scale + offsetX,
                box.boundingBox.bottom * scale + offsetY
            )
            
            if (screenBox.contains(x, y)) {
                return index
            }
        }
        return -1
    }
}
