package com.sza.fastmediasorter.ui.player.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sza.fastmediasorter.ocr.OcrManager

/**
 * Overlay view that displays OCR text blocks on top of an image
 * Similar to Google Lens text detection overlay
 */
class OcrOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Paint for drawing text block backgrounds
    private val blockBackgroundPaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.FILL
    }
    
    // Paint for drawing text block borders
    private val blockBorderPaint = Paint().apply {
        color = Color.argb(255, 66, 133, 244) // Google Blue
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    // Paint for drawing selected block background
    private val selectedBlockPaint = Paint().apply {
        color = Color.argb(100, 66, 133, 244)
        style = Paint.Style.FILL
    }
    
    // Paint for drawing text
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
    }
    
    // Paint for drawing translated text
    private val translatedTextPaint = Paint().apply {
        color = Color.argb(255, 0, 100, 0) // Dark green for translated text
        textSize = 36f
        isAntiAlias = true
    }
    
    // Current text blocks
    private var textBlocks: List<OcrManager.TextBlock> = emptyList()
    
    // Translations mapped to original text
    private var translations: Map<String, String> = emptyMap()
    
    // Currently selected block index
    private var selectedBlockIndex: Int = -1
    
    // Scale factors for mapping coordinates from original image to view
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    
    // Original image dimensions
    private var originalImageWidth: Int = 0
    private var originalImageHeight: Int = 0
    
    // Whether to show translations or original text
    var showTranslations: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    // Whether overlay is visible
    var overlayVisible: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    
    // Callback when a text block is clicked
    var onBlockClickListener: ((OcrManager.TextBlock) -> Unit)? = null
    
    // Callback when a text block is long-clicked
    var onBlockLongClickListener: ((OcrManager.TextBlock) -> Unit)? = null
    
    /**
     * Set the detected text blocks
     */
    fun setTextBlocks(blocks: List<OcrManager.TextBlock>, imageWidth: Int, imageHeight: Int) {
        this.textBlocks = blocks
        this.originalImageWidth = imageWidth
        this.originalImageHeight = imageHeight
        calculateScaleFactors()
        invalidate()
    }
    
    /**
     * Set translations for the text blocks
     */
    fun setTranslations(translationMap: Map<String, String>) {
        this.translations = translationMap
        invalidate()
    }
    
    /**
     * Clear all text blocks and translations
     */
    fun clear() {
        textBlocks = emptyList()
        translations = emptyMap()
        selectedBlockIndex = -1
        invalidate()
    }
    
    /**
     * Calculate scale factors based on view and image dimensions
     */
    private fun calculateScaleFactors() {
        if (originalImageWidth == 0 || originalImageHeight == 0 || width == 0 || height == 0) {
            scaleX = 1f
            scaleY = 1f
            offsetX = 0f
            offsetY = 0f
            return
        }
        
        val viewAspect = width.toFloat() / height.toFloat()
        val imageAspect = originalImageWidth.toFloat() / originalImageHeight.toFloat()
        
        if (imageAspect > viewAspect) {
            // Image is wider than view - fit to width
            scaleX = width.toFloat() / originalImageWidth
            scaleY = scaleX
            offsetX = 0f
            offsetY = (height - originalImageHeight * scaleY) / 2
        } else {
            // Image is taller than view - fit to height
            scaleY = height.toFloat() / originalImageHeight
            scaleX = scaleY
            offsetX = (width - originalImageWidth * scaleX) / 2
            offsetY = 0f
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateScaleFactors()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!overlayVisible || textBlocks.isEmpty()) return
        
        textBlocks.forEachIndexed { index, block ->
            block.boundingBox?.let { originalBox ->
                // Transform bounding box to view coordinates
                val viewBox = transformRect(originalBox)
                
                // Draw background
                if (index == selectedBlockIndex) {
                    canvas.drawRoundRect(viewBox, 8f, 8f, selectedBlockPaint)
                } else {
                    canvas.drawRoundRect(viewBox, 8f, 8f, blockBackgroundPaint)
                }
                
                // Draw border
                canvas.drawRoundRect(viewBox, 8f, 8f, blockBorderPaint)
                
                // Draw text
                val textToDisplay = if (showTranslations && translations.containsKey(block.text)) {
                    translations[block.text] ?: block.text
                } else {
                    block.text
                }
                
                val paint = if (showTranslations && translations.containsKey(block.text)) {
                    translatedTextPaint
                } else {
                    textPaint
                }
                
                // Adjust text size to fit within the box
                val textSize = calculateTextSize(textToDisplay, viewBox.width() - 16, viewBox.height() - 8, paint)
                paint.textSize = textSize
                
                // Draw text centered in box
                val textWidth = paint.measureText(textToDisplay)
                val textX = viewBox.left + (viewBox.width() - textWidth) / 2
                val textY = viewBox.top + viewBox.height() / 2 + paint.textSize / 3
                
                canvas.drawText(textToDisplay, textX, textY, paint)
            }
        }
    }
    
    /**
     * Transform a rectangle from image coordinates to view coordinates
     */
    private fun transformRect(rect: RectF): RectF {
        return RectF(
            rect.left * scaleX + offsetX,
            rect.top * scaleY + offsetY,
            rect.right * scaleX + offsetX,
            rect.bottom * scaleY + offsetY
        )
    }
    
    /**
     * Transform view coordinates to image coordinates
     */
    private fun viewToImageCoordinates(viewX: Float, viewY: Float): Pair<Float, Float> {
        val imageX = (viewX - offsetX) / scaleX
        val imageY = (viewY - offsetY) / scaleY
        return Pair(imageX, imageY)
    }
    
    /**
     * Calculate appropriate text size to fit within bounds
     */
    private fun calculateTextSize(text: String, maxWidth: Float, maxHeight: Float, paint: Paint): Float {
        var textSize = 48f
        val minSize = 12f
        
        while (textSize > minSize) {
            paint.textSize = textSize
            val textWidth = paint.measureText(text)
            val textHeight = paint.textSize
            
            if (textWidth <= maxWidth && textHeight <= maxHeight) {
                return textSize
            }
            textSize -= 2
        }
        
        return minSize
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!overlayVisible || textBlocks.isEmpty()) {
            return super.onTouchEvent(event)
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchedBlock = findBlockAtPoint(event.x, event.y)
                if (touchedBlock != null) {
                    selectedBlockIndex = textBlocks.indexOf(touchedBlock)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                val touchedBlock = findBlockAtPoint(event.x, event.y)
                if (touchedBlock != null) {
                    onBlockClickListener?.invoke(touchedBlock)
                    // Deselect after click
                    selectedBlockIndex = -1
                    invalidate()
                    return true
                }
                selectedBlockIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                selectedBlockIndex = -1
                invalidate()
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * Find which text block contains the given view coordinates
     */
    private fun findBlockAtPoint(viewX: Float, viewY: Float): OcrManager.TextBlock? {
        textBlocks.forEach { block ->
            block.boundingBox?.let { originalBox ->
                val viewBox = transformRect(originalBox)
                if (viewBox.contains(viewX, viewY)) {
                    return block
                }
            }
        }
        return null
    }
    
    /**
     * Get all selected text (from selected block or all if none selected)
     */
    fun getSelectedText(): String {
        return if (selectedBlockIndex >= 0 && selectedBlockIndex < textBlocks.size) {
            textBlocks[selectedBlockIndex].text
        } else {
            textBlocks.joinToString("\n") { it.text }
        }
    }
    
    /**
     * Get all recognized text
     */
    fun getAllText(): String {
        return textBlocks.joinToString("\n") { it.text }
    }
    
    /**
     * Select a specific block by index
     */
    fun selectBlock(index: Int) {
        if (index in textBlocks.indices || index == -1) {
            selectedBlockIndex = index
            invalidate()
        }
    }
    
    /**
     * Get the count of text blocks
     */
    fun getBlockCount(): Int = textBlocks.size
}
