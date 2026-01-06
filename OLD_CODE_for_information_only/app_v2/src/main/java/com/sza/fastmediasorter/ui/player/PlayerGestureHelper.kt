package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import timber.log.Timber

/**
 * Helper class for handling gesture detection and touch zones in PlayerActivity.
 * Manages swipe gestures, double taps, long press, and touch zone detection.
 * 
 * Responsibilities:
 * - Touch zone detection (left/center/right, command panel zones)
 * - Swipe gesture recognition (left/right/up/down)
 * - Double tap and long press handling
 * - First-run hint overlay display
 */
class PlayerGestureHelper(
    private val context: Context,
    private val gestureCallback: GestureCallback
) {
    
    /**
     * Callback interface for gesture events
     */
    interface GestureCallback {
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onSwipeUp()
        fun onSwipeDown()
        fun onDoubleTap()
        fun onLongPress()
        fun onTouchZone(zone: TouchZone)
    }
    
    /**
     * Touch zones for different areas of the screen
     */
    enum class TouchZone {
        LEFT,           // Left third of screen
        CENTER,         // Center third of screen
        RIGHT,          // Right third of screen
        COPY_PANEL,     // Command panel - copy button area
        MOVE_PANEL,     // Command panel - move button area
        DELETE          // Command panel - delete button area
    }
    
    companion object {
        // Touch zone thresholds
        private const val LEFT_ZONE_THRESHOLD = 0.25f
        private const val RIGHT_ZONE_THRESHOLD = 0.75f
        
        // Swipe thresholds
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        
        // First-run hint preferences
        private const val PREF_FIRST_RUN_HINT_SHOWN = "first_run_hint_shown"
    }
    
    private var gestureDetector: GestureDetector? = null
    
    /**
     * Setup gesture detector with custom listener
     */
    fun setupGestureDetector(): GestureDetector {
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            gestureCallback.onSwipeRight()
                        } else {
                            gestureCallback.onSwipeLeft()
                        }
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            gestureCallback.onSwipeDown()
                        } else {
                            gestureCallback.onSwipeUp()
                        }
                        return true
                    }
                }
                
                return false
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                gestureCallback.onDoubleTap()
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                gestureCallback.onLongPress()
            }
        })
        
        gestureDetector = detector
        return detector
    }
    
    /**
     * Handle touch event and determine touch zone
     * @return true if touch was handled
     */
    fun handleTouch(x: Float, y: Float, viewWidth: Int, viewHeight: Int): Boolean {
        val zone = detectTouchZone(x, y, viewWidth, viewHeight)
        gestureCallback.onTouchZone(zone)
        return true
    }
    
    /**
     * Detect which touch zone was touched
     */
    private fun detectTouchZone(x: Float, y: Float, viewWidth: Int, viewHeight: Int): TouchZone {
        val relativeX = x / viewWidth
        
        return when {
            relativeX < LEFT_ZONE_THRESHOLD -> TouchZone.LEFT
            relativeX > RIGHT_ZONE_THRESHOLD -> TouchZone.RIGHT
            else -> TouchZone.CENTER
        }
    }
    
    /**
     * Handle command panel touch zones
     * @return TouchZone.COPY_PANEL, TouchZone.MOVE_PANEL, or TouchZone.DELETE, or null if outside panel
     */
    fun handleCommandPanelTouch(
        x: Float, 
        y: Float, 
        viewWidth: Int, 
        viewHeight: Int,
        commandPanelHeight: Int
    ): TouchZone? {
        // Check if touch is in command panel area (bottom of screen)
        if (y < viewHeight - commandPanelHeight) {
            return null
        }
        
        // Divide panel into 3 equal zones
        val zoneWidth = viewWidth / 3f
        
        return when {
            x < zoneWidth -> TouchZone.COPY_PANEL
            x < zoneWidth * 2 -> TouchZone.MOVE_PANEL
            else -> TouchZone.DELETE
        }
    }
    
    /**
     * Show first-run hint overlay if not shown before
     */
    fun showFirstRunHintOverlay(rootView: View, onDismiss: () -> Unit) {
        val prefs = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_FIRST_RUN_HINT_SHOWN, false)) {
            return
        }
        
        // TODO: Implement hint overlay UI
        Timber.d("PlayerGestureHelper: First-run hint would be shown here")
        
        // Mark as shown
        prefs.edit().putBoolean(PREF_FIRST_RUN_HINT_SHOWN, true).apply()
        onDismiss()
    }
    
    /**
     * Release resources
     */
    fun cleanup() {
        gestureDetector = null
    }
}
