package com.sza.fastmediasorter.ui.player.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.sza.fastmediasorter.R

/**
 * Custom overlay view that displays a 3x3 grid of touch zones for gesture navigation.
 * Used in PlayerActivity for intuitive media navigation.
 *
 * Zone Layout:
 * ```
 * +----------+----------+----------+
 * | Previous |  Toggle  |   Next   |
 * | File     |   UI     |   File   |
 * +----------+----------+----------+
 * | Seek     |  Play/   |   Seek   |
 * | Back 10s |  Pause   |  Fwd 10s |
 * +----------+----------+----------+
 * | Previous |  Toggle  |   Next   |
 * | File     |   UI     |   File   |
 * +----------+----------+----------+
 * ```
 */
class TouchZoneOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Listener for zone tap events.
     */
    interface OnZoneTapListener {
        fun onPreviousFile()
        fun onNextFile()
        fun onToggleUi()
        fun onSeekBackward()
        fun onSeekForward()
        fun onPlayPause()
    }

    var zoneTapListener: OnZoneTapListener? = null

    // Whether to show zone labels (visual guide)
    var showLabels: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Whether the overlay is enabled for gestures
    var isGestureEnabled: Boolean = true

    // Paints for drawing
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20FFFFFF")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.FILL
    }

    // Zone labels
    private val zoneLabels = arrayOf(
        arrayOf(context.getString(R.string.touch_zone_top_left), context.getString(R.string.touch_zone_top_center), context.getString(R.string.touch_zone_top_right)),
        arrayOf(context.getString(R.string.touch_zone_middle_left), context.getString(R.string.touch_zone_center), context.getString(R.string.touch_zone_middle_right)),
        arrayOf(context.getString(R.string.touch_zone_bottom_left), context.getString(R.string.touch_zone_bottom_center), context.getString(R.string.touch_zone_bottom_right))
    )

    // Current zone being touched (for visual feedback)
    private var currentTouchedZone: Pair<Int, Int>? = null

    // Zone rectangles for hit testing
    private val zoneRects = Array(3) { Array(3) { RectF() } }

    // Gesture detector for detecting single taps vs double taps
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isGestureEnabled) return false

            val zone = getZoneAt(e.x, e.y)
            zone?.let { (row, col) ->
                handleZoneTap(row, col)
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isGestureEnabled) return false

            val zone = getZoneAt(e.x, e.y)
            zone?.let { (row, col) ->
                // Double tap in middle zones = play/pause
                if (col == 1) {
                    zoneTapListener?.onPlayPause()
                    return true
                }
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateZoneRects()
    }

    private fun updateZoneRects() {
        val zoneWidth = width / 3f
        val zoneHeight = height / 3f

        for (row in 0..2) {
            for (col in 0..2) {
                zoneRects[row][col] = RectF(
                    col * zoneWidth,
                    row * zoneHeight,
                    (col + 1) * zoneWidth,
                    (row + 1) * zoneHeight
                )
            }
        }
    }

    private fun getZoneAt(x: Float, y: Float): Pair<Int, Int>? {
        for (row in 0..2) {
            for (col in 0..2) {
                if (zoneRects[row][col].contains(x, y)) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }

    private fun handleZoneTap(row: Int, col: Int) {
        when {
            // Left column = Previous file
            col == 0 -> zoneTapListener?.onPreviousFile()
            // Right column = Next file
            col == 2 -> zoneTapListener?.onNextFile()
            // Middle column
            col == 1 -> {
                when (row) {
                    0, 2 -> zoneTapListener?.onToggleUi()  // Top/bottom center = Toggle UI
                    1 -> zoneTapListener?.onPlayPause()   // Center = Play/Pause
                }
            }
        }

        // For video: Left-middle = seek back, Right-middle = seek forward
        if (row == 1) {
            when (col) {
                0 -> zoneTapListener?.onSeekBackward()
                2 -> zoneTapListener?.onSeekForward()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showLabels && currentTouchedZone == null) return

        val zoneWidth = width / 3f
        val zoneHeight = height / 3f

        // Draw grid lines
        if (showLabels) {
            // Vertical lines
            canvas.drawLine(zoneWidth, 0f, zoneWidth, height.toFloat(), gridPaint)
            canvas.drawLine(2 * zoneWidth, 0f, 2 * zoneWidth, height.toFloat(), gridPaint)
            // Horizontal lines
            canvas.drawLine(0f, zoneHeight, width.toFloat(), zoneHeight, gridPaint)
            canvas.drawLine(0f, 2 * zoneHeight, width.toFloat(), 2 * zoneHeight, gridPaint)

            // Draw labels
            for (row in 0..2) {
                for (col in 0..2) {
                    val centerX = (col + 0.5f) * zoneWidth
                    val centerY = (row + 0.5f) * zoneHeight + labelPaint.textSize / 3

                    canvas.drawText(zoneLabels[row][col], centerX, centerY, labelPaint)
                }
            }
        }

        // Highlight current touched zone
        currentTouchedZone?.let { (row, col) ->
            canvas.drawRect(zoneRects[row][col], highlightPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGestureEnabled) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentTouchedZone = getZoneAt(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentTouchedZone = null
                invalidate()
            }
        }

        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
