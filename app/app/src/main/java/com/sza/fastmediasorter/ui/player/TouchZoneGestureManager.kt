package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import timber.log.Timber
import kotlin.math.abs

/**
 * Manages touch zone gesture detection including tap, long-press, and swipe.
 * Supports both 9-zone (3x3) and 2-zone layouts.
 */
class TouchZoneGestureManager(
    private val context: Context,
    private val listener: TouchZoneListener
) {

    companion object {
        // Default long-press delay (configurable via settings)
        private const val DEFAULT_LONG_PRESS_DELAY_MS = 200L
        
        // Swipe thresholds
        private const val SWIPE_MIN_DISTANCE = 100f
        private const val SWIPE_VELOCITY_THRESHOLD = 100f
        
        // Brightness/Volume swipe
        private const val VERTICAL_SWIPE_MIN_DISTANCE = 50f
    }

    /**
     * Listener for touch zone events.
     */
    interface TouchZoneListener {
        // 9-zone tap events
        fun onZoneTap(zone: TouchZone)
        fun onZoneLongPress(zone: TouchZone)

        // 2-zone tap events
        fun onZone2Tap(zone: TouchZone2)

        // Swipe events
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onSwipeUpLeft()      // Brightness increase
        fun onSwipeDownLeft()    // Brightness decrease
        fun onSwipeUpRight()     // Volume increase
        fun onSwipeDownRight()   // Volume decrease

        // Double tap (for zoom/play toggle)
        fun onDoubleTap(x: Float, y: Float)
    }

    // Current layout mode
    var isNineZoneMode: Boolean = true

    // Sensitivity setting (long-press delay in ms)
    var longPressDelayMs: Long = DEFAULT_LONG_PRESS_DELAY_MS

    // Whether gestures are enabled
    var isEnabled: Boolean = true

    // Screen dimensions (set by the view)
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Active area bounds for 2-zone mode (e.g., excluding player controls)
    private var activeAreaTop: Float = 0f
    private var activeAreaBottom: Float = Float.MAX_VALUE

    // Detector instance
    private val touchZoneDetector = TouchZoneDetector()

    // Long-press handling
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressTriggered = false
    private var touchDownZone: TouchZone? = null

    // Swipe tracking
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L

    // Gesture detector for complex gestures
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isEnabled) return false
            listener.onDoubleTap(e.x, e.y)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (!isEnabled) return false
            if (e1 == null) return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            val absX = abs(diffX)
            val absY = abs(diffY)

            // Horizontal swipe (next/previous file)
            if (absX > absY && absX > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    Timber.d("TouchZoneGestureManager: Swipe right (previous)")
                    listener.onSwipeRight()
                } else {
                    Timber.d("TouchZoneGestureManager: Swipe left (next)")
                    listener.onSwipeLeft()
                }
                return true
            }

            // Vertical swipe (brightness/volume)
            if (absY > absX && absY > VERTICAL_SWIPE_MIN_DISTANCE) {
                val isLeftHalf = e1.x < screenWidth / 2f

                if (diffY < 0) {
                    // Swipe up
                    if (isLeftHalf) {
                        Timber.d("TouchZoneGestureManager: Swipe up (left half) - brightness increase")
                        listener.onSwipeUpLeft()
                    } else {
                        Timber.d("TouchZoneGestureManager: Swipe up (right half) - volume increase")
                        listener.onSwipeUpRight()
                    }
                } else {
                    // Swipe down
                    if (isLeftHalf) {
                        Timber.d("TouchZoneGestureManager: Swipe down (left half) - brightness decrease")
                        listener.onSwipeDownLeft()
                    } else {
                        Timber.d("TouchZoneGestureManager: Swipe down (right half) - volume decrease")
                        listener.onSwipeDownRight()
                    }
                }
                return true
            }

            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isEnabled || isLongPressTriggered) return false

            if (isNineZoneMode) {
                val zone = touchZoneDetector.detectZone9(e.x, e.y, screenWidth, screenHeight)
                if (zone != TouchZone.NONE) {
                    Timber.d("TouchZoneGestureManager: 9-zone tap -> $zone")
                    listener.onZoneTap(zone)
                    return true
                }
            } else {
                val zone = touchZoneDetector.detectZone2WithBounds(
                    e.x, e.y, screenWidth, activeAreaTop, activeAreaBottom
                )
                if (zone != TouchZone2.NONE) {
                    Timber.d("TouchZoneGestureManager: 2-zone tap -> $zone")
                    listener.onZone2Tap(zone)
                    return true
                }
            }

            return false
        }
    })

    /**
     * Set screen dimensions for zone calculations.
     */
    fun setScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    /**
     * Set active area for 2-zone mode (vertical bounds).
     */
    fun setActiveArea(top: Float, bottom: Float) {
        activeAreaTop = top
        activeAreaBottom = bottom
    }

    /**
     * Handle touch event from a view.
     * @return true if event was consumed
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                touchDownTime = System.currentTimeMillis()
                isLongPressTriggered = false

                // Schedule long-press detection for 9-zone mode
                if (isNineZoneMode) {
                    touchDownZone = touchZoneDetector.detectZone9(
                        event.x, event.y, screenWidth, screenHeight
                    )
                    
                    if (touchDownZone != TouchZone.NONE) {
                        scheduleLongPress(touchDownZone!!)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Cancel long-press if finger moved too much
                val diffX = abs(event.x - touchDownX)
                val diffY = abs(event.y - touchDownY)
                if (diffX > 20 || diffY > 20) {
                    cancelLongPress()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
            }
        }

        return gestureDetector.onTouchEvent(event)
    }

    /**
     * Attach to a view as touch listener.
     */
    fun attachToView(view: View) {
        view.setOnTouchListener { _, event ->
            onTouchEvent(event)
        }

        // Set dimensions when view is laid out
        view.post {
            setScreenDimensions(view.width, view.height)
        }
    }

    private fun scheduleLongPress(zone: TouchZone) {
        cancelLongPress()
        
        longPressRunnable = Runnable {
            if (!isLongPressTriggered) {
                isLongPressTriggered = true
                Timber.d("TouchZoneGestureManager: Long-press on $zone")
                listener.onZoneLongPress(zone)
            }
        }
        
        longPressHandler.postDelayed(longPressRunnable!!, longPressDelayMs)
    }

    private fun cancelLongPress() {
        longPressRunnable?.let {
            longPressHandler.removeCallbacks(it)
            longPressRunnable = null
        }
    }

    /**
     * Call when the view/activity is destroyed.
     */
    fun onDestroy() {
        cancelLongPress()
    }
}
