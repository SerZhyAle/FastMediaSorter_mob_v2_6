package com.sza.fastmediasorter.ui.player.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import timber.log.Timber
import kotlin.math.abs

/**
 * Custom view for drawing translated text blocks in Google Lens style
 * Overlays translated text rectangles over original text positions
 * 
 * Language-agnostic design:
 * - Works equally well for any translation direction (ru→en, en→ru, uk→en, etc.)
 * - Conservative font sizing prevents oversized blocks on short text
 * - Intelligent text wrapping with expansion limits
 * 
 * Text rendering strategy:
 * 1. Calculate font size: 50% of box height (45% for small boxes < 40sp)
 * 2. Wrap text to multiple lines within bounding box width
 * 3. If still doesn't fit vertically, reduce font size (min 9sp)
 * 4. If still doesn't fit horizontally, expand box up to 150% width
 * 5. If still doesn't fit vertically, expand box up to 200% height
 * 6. Center text vertically within final box
 * 
 * Features:
 * - Tap on a block to bring it to front (raise z-order)
 * - Adaptive padding based on box size (2-4sp)
 * - Slight letter spacing for better readability
 * - Rounded corners with opaque white background
 * - Expansion limits prevent blocks from overlapping excessively
 */
class TranslationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Data class representing a translated text block with position
     */
    data class TranslatedBlock(
        val originalText: String,
        val translatedText: String,
        val boundingBox: Rect,
        val confidence: Float,
        var backgroundColor: Int = Color.parseColor("#F0FFFFFF"), // Adaptive background color
        var textColor: Int = Color.BLACK, // Contrast text color
        var customFontSize: Float? = null // Per-block font size override (6-72sp)
    )

    private val translatedBlocks = mutableListOf<TranslatedBlock>()
    
    // Text size range in SP (adaptive to match different document types)
    private val minTextSizeSp = 8f  // Raised minimum to prevent too small text
    private val maxTextSizeSp = 14f  // Moderate maximum for readability
    
    // Per-block custom font size range (user adjustable via gestures)
    private val perBlockMinFontSizeSp = 6f
    private val perBlockMaxFontSizeSp = 72f
    private val perBlockFontSizeStepSp = 2f
    
    // Source bitmap for color sampling
    private var sourceBitmap: Bitmap? = null
    
    // Font size multiplier for user-adjustable scaling (0.7x to 1.5x)
    private var fontSizeMultiplier: Float = 1.0f
    private val minFontSizeMultiplier = 0.7f
    private val maxFontSizeMultiplier = 1.5f
    private val fontSizeStep = 0.1f
    
    // Paint for background rectangles
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#F0FFFFFF") // More opaque white for readability
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // TextPaint for multiline text (extends Paint with text layout features)
    private val textPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 32f // Will be adjusted based on bounding box height
        isAntiAlias = true
        isFakeBoldText = false
        // Slight letter spacing for better readability of both Cyrillic and Latin
        letterSpacing = 0.02f
    }
    
    /**
     * Scale factor to convert OCR bitmap coordinates to view coordinates
     */
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    
    /**
     * Offset for image position within view (for letterboxing)
     * When image doesn't fill the entire view, we need to offset coordinates
     */
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    
    /**
     * Cached scaled rectangles for hit testing
     */
    private val scaledRects = mutableListOf<RectF>()
    
    /**
     * Gesture detector for swipe gestures and taps
     */
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            
            Timber.d("TranslationOverlay: onFling deltaX=$deltaX, deltaY=$deltaY")
            
            // Require primarily horizontal movement
            if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 50) {
                // Find which block was swiped
                for (i in translatedBlocks.indices.reversed()) {
                    if (i < scaledRects.size && scaledRects[i].contains(e1.x, e1.y)) {
                        val block = translatedBlocks[i]
                        val currentSize = block.customFontSize ?: (maxTextSizeSp * fontSizeMultiplier)
                        
                        val newSize = if (deltaX > 0) {
                            // Swipe right - increase
                            (currentSize + perBlockFontSizeStepSp).coerceAtMost(perBlockMaxFontSizeSp)
                        } else {
                            // Swipe left - decrease  
                            (currentSize - perBlockFontSizeStepSp).coerceAtLeast(perBlockMinFontSizeSp)
                        }
                        
                        block.customFontSize = newSize
                        Timber.d("Block font size changed: ${block.customFontSize}sp (was ${currentSize}sp)")
                        invalidate()
                        return true
                    }
                }
            }
            return false
        }
        
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            
            // Find which block was tapped (iterate in reverse to check top blocks first)
            for (i in translatedBlocks.indices.reversed()) {
                if (i < scaledRects.size && scaledRects[i].contains(x, y)) {
                    // Move tapped block to end of list (top of z-order)
                    val block = translatedBlocks.removeAt(i)
                    translatedBlocks.add(block)
                    scaledRects.clear() // Force recalculation
                    invalidate()
                    Timber.d("Translation block tapped: brought to front")
                    return true
                }
            }
            return false
        }
    })
    
    /**
     * Convert SP to pixels
     */
    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }
    
    /**
     * Load font size multiplier from SharedPreferences
     */
    init {
        val prefs = context.getSharedPreferences("translation_settings", Context.MODE_PRIVATE)
        fontSizeMultiplier = prefs.getFloat("font_size_multiplier", 1.0f)
            .coerceIn(minFontSizeMultiplier, maxFontSizeMultiplier)
    }
    
    /**
     * Increase font size for translation blocks
     */
    fun increaseFontSize() {
        fontSizeMultiplier = (fontSizeMultiplier + fontSizeStep)
            .coerceAtMost(maxFontSizeMultiplier)
        saveFontSize()
        invalidate() // Redraw with new font size
    }
    
    /**
     * Decrease font size for translation blocks
     */
    fun decreaseFontSize() {
        fontSizeMultiplier = (fontSizeMultiplier - fontSizeStep)
            .coerceAtLeast(minFontSizeMultiplier)
        saveFontSize()
        invalidate() // Redraw with new font size
    }
    
    /**
     * Get current font size multiplier
     */
    fun getFontSizeMultiplier(): Float = fontSizeMultiplier
    
    /**
     * Save font size multiplier to SharedPreferences
     */
    private fun saveFontSize() {
        val prefs = context.getSharedPreferences("translation_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("font_size_multiplier", fontSizeMultiplier).apply()
    }
    
    /**
     * Create StaticLayout for multiline text with word wrapping
     */
    @Suppress("DEPRECATION")
    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(
                text, paint, width,
                Layout.Alignment.ALIGN_NORMAL,
                1f, 0f, false
            )
        }
    }
    
    /**
     * Set the scale factor for coordinate conversion.
     * Calculates uniform scale (fit center) and offsets for letterboxing.
     * Call this when the bitmap size differs from the view size.
     */
    fun setScale(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Int, viewHeight: Int) {
        // Calculate scale factors for each dimension
        val scaleToFitWidth = viewWidth.toFloat() / bitmapWidth.toFloat()
        val scaleToFitHeight = viewHeight.toFloat() / bitmapHeight.toFloat()
        
        // Use uniform scale (min of both) to maintain aspect ratio (fit center behavior)
        val uniformScale = minOf(scaleToFitWidth, scaleToFitHeight)
        scaleX = uniformScale
        scaleY = uniformScale
        
        // Calculate the actual size of scaled image
        val scaledImageWidth = bitmapWidth * uniformScale
        val scaledImageHeight = bitmapHeight * uniformScale
        
        // Calculate offsets for centering (letterboxing)
        offsetX = (viewWidth - scaledImageWidth) / 2f
        offsetY = (viewHeight - scaledImageHeight) / 2f
    }
    
    /**
     * Set the source bitmap for color sampling
     */
    fun setSourceBitmap(bitmap: Bitmap?) {
        sourceBitmap = bitmap
    }
    
    /**
     * Sample background color from source image at top-left corner of bounding box
     */
    private fun sampleBackgroundColor(boundingBox: Rect): Int {
        val bitmap = sourceBitmap ?: return Color.parseColor("#F0FFFFFF")
        
        try {
            // Get coordinates (top-left corner of bounding box)
            val x = boundingBox.left.coerceIn(0, bitmap.width - 1)
            val y = boundingBox.top.coerceIn(0, bitmap.height - 1)
            
            // Sample pixel color
            val pixelColor = bitmap.getPixel(x, y)
            
            // Add slight opacity for better blending
            val alpha = 240 // ~94% opacity
            return Color.argb(
                alpha,
                Color.red(pixelColor),
                Color.green(pixelColor),
                Color.blue(pixelColor)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to sample background color")
            return Color.parseColor("#F0FFFFFF")
        }
    }
    
    /**
     * Calculate contrast text color (black or white) based on background brightness
     * Uses luminance formula: 0.299*R + 0.587*G + 0.114*B
     */
    private fun getContrastTextColor(backgroundColor: Int): Int {
        val r = Color.red(backgroundColor)
        val g = Color.green(backgroundColor)
        val b = Color.blue(backgroundColor)
        
        // Calculate perceived brightness (0-255)
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        
        // Threshold at 128 (mid-point)
        // Dark background → white text, Light background → black text
        return if (luminance < 128) Color.WHITE else Color.BLACK
    }
    
    /**
     * Update the translated blocks to display
     */
    fun setTranslatedBlocks(blocks: List<TranslatedBlock>) {
        translatedBlocks.clear()
        
        // Sample colors for each block
        for (block in blocks) {
            val bgColor = sampleBackgroundColor(block.boundingBox)
            block.backgroundColor = bgColor
            block.textColor = getContrastTextColor(bgColor)
        }
        
        translatedBlocks.addAll(blocks)
        scaledRects.clear() // Clear cached rects, will be recalculated on draw
        invalidate() // Trigger redraw
    }
    
    /**
     * Clear all translated blocks
     */
    fun clear() {
        translatedBlocks.clear()
        scaledRects.clear()
        invalidate()
    }
    
    /**
     * Handle touch events - delegate to GestureDetector for swipes and taps
     * CRITICAL: Always consume touch events when visible to prevent them from
     * passing through to the image/document underneath
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always pass ALL events to gesture detector (including DOWN, MOVE, UP)
        // This is critical for fling detection to work properly
        gestureDetector.onTouchEvent(event)
        
        // CRITICAL: Always consume touch events when overlay is visible
        // to prevent them from passing through to the image touch zones
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val minTextSizePx = spToPx(minTextSizeSp)
        val maxTextSizePx = spToPx(maxTextSizeSp)
        
        // Clear and rebuild scaled rects for hit testing
        scaledRects.clear()
        
        // Draw each translated block
        for (block in translatedBlocks) {
            // Use block-specific colors
            backgroundPaint.color = block.backgroundColor
            textPaint.color = block.textColor
            
            // Scale coordinates from bitmap to view and apply letterbox offset
            val baseLeft = block.boundingBox.left * scaleX + offsetX
            val baseTop = block.boundingBox.top * scaleY + offsetY
            val baseRight = block.boundingBox.right * scaleX + offsetX
            val baseBottom = block.boundingBox.bottom * scaleY + offsetY
            
            val boxHeight = baseBottom - baseTop
            val boxWidth = baseRight - baseLeft
            
            // Apply small offset: move blocks 3% left and 5% up for better alignment
            val offsetAdjustmentX = -boxWidth * 0.03f // 3% left
            val offsetAdjustmentY = -boxHeight * 0.05f // 5% up
            
            val scaledLeft = baseLeft + offsetAdjustmentX
            val scaledTop = baseTop + offsetAdjustmentY
            val scaledRight = baseRight + offsetAdjustmentX
            val scaledBottom = baseBottom + offsetAdjustmentY
            
            // Adaptive padding based on box size (2-4sp range)
            val padding = spToPx(2f + (boxHeight / 100f).coerceIn(0f, 2f))
            val availableWidth = (boxWidth - padding * 2).toInt().coerceAtLeast(1)
            
            // Use custom font size if set by user gesture, otherwise calculate automatically
            var textSize = if (block.customFontSize != null) {
                spToPx(block.customFontSize!!)
            } else {
                // Calculate optimal text size with adaptive approach:
                // Strategy: Analyze text density (chars per pixel) to determine appropriate size
                // - Dense text (many chars in small box) → smaller ratio
                // - Sparse text (few chars in large box) → larger ratio
                val textLength = block.translatedText.length
                val boxArea = boxWidth * boxHeight
                val charDensity = textLength / boxArea  // chars per square pixel
                
                // Adaptive ratio based on box height and character density
                val baseRatio = when {
                    // Very large box (> 80sp) with low density → use moderate ratio
                    boxHeight > spToPx(80f) && charDensity < 0.01f -> 0.38f
                    // Large box (50-80sp) → conservative ratio
                    boxHeight > spToPx(50f) -> 0.35f
                    // Medium box (30-50sp) → standard ratio
                    boxHeight > spToPx(30f) -> 0.40f
                    // Small box (< 30sp) → larger ratio to keep readable
                    else -> 0.45f
                }
                
                (boxHeight * baseRatio * fontSizeMultiplier).coerceIn(minTextSizePx * fontSizeMultiplier, maxTextSizePx * fontSizeMultiplier)
            }
            
            textPaint.textSize = textSize
            
            // Create StaticLayout for multiline text wrapping within box width
            var staticLayout = createStaticLayout(block.translatedText, textPaint, availableWidth)
            
            // If text takes more than 1 line and no custom size, try to reduce font size to fit in box height
            val adjustedMinTextSizePx = minTextSizePx * fontSizeMultiplier
            if (block.customFontSize == null && staticLayout.height > boxHeight - padding * 2 && staticLayout.lineCount > 1 && textSize > adjustedMinTextSizePx) {
                // Scale down to fit within box height with padding
                val targetHeight = boxHeight - padding * 2
                val scaleFactor = (targetHeight / staticLayout.height).coerceIn(0.6f, 1f)
                val newSize = (textSize * scaleFactor).coerceAtLeast(adjustedMinTextSizePx)
                textPaint.textSize = newSize
                staticLayout = createStaticLayout(block.translatedText, textPaint, availableWidth)
            }
            
            // Calculate final background dimensions
            // Width: use box width, unless single line is wider
            val textActualWidth = (0 until staticLayout.lineCount)
                .maxOfOrNull { staticLayout.getLineWidth(it) } ?: 0f
            
            // Limit horizontal expansion to 270% of original box width (improved readability)
            val maxAllowedWidth = boxWidth * 2.7f
            val actualRight = if (textActualWidth + padding * 2 > boxWidth) {
                // Text doesn't fit - expand right, but with limit
                val expandedWidth = textActualWidth + padding * 2
                scaledLeft + expandedWidth.coerceAtMost(maxAllowedWidth)
            } else {
                // Text fits or is shorter - use actual text width (shrink to fit)
                scaledLeft + textActualWidth + padding * 2
            }
            
            // Height: use box width, unless multiline text is taller
            val textHeightWithPadding = staticLayout.height + padding * 2
            // Limit vertical expansion to 270% of original box height (match width expansion)
            val maxAllowedHeight = boxHeight * 2.7f
            val actualBottom = if (textHeightWithPadding > boxHeight) {
                scaledTop + textHeightWithPadding.coerceAtMost(maxAllowedHeight)
            } else {
                // Text fits or is shorter - use actual text height (shrink to fit)
                scaledTop + textHeightWithPadding
            }
            
            // Calculate actual final box height (for proper vertical centering)
            val finalBoxHeight = actualBottom - scaledTop
            
            val backgroundRect = RectF(
                scaledLeft,
                scaledTop,
                actualRight,
                actualBottom
            )
            
            // Store rect for hit testing
            scaledRects.add(backgroundRect)
            
            // Draw background rectangle with adaptive color and subtle shadow effect
            canvas.drawRoundRect(backgroundRect, 6f, 6f, backgroundPaint)
            
            // Draw multiline text using StaticLayout with adaptive text color
            canvas.save()
            // Vertically center text within FINAL box height
            val textStartY = scaledTop + (finalBoxHeight - staticLayout.height) / 2
            canvas.translate(scaledLeft + padding, textStartY)
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }
}
