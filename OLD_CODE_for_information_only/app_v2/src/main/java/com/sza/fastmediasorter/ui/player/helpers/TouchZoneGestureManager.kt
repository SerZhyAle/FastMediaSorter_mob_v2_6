package com.sza.fastmediasorter.ui.player.helpers

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.isVisible
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.ui.player.PlayerViewModel
import com.sza.fastmediasorter.ui.player.TouchZone
import com.sza.fastmediasorter.ui.player.TouchZoneDetector
import com.sza.fastmediasorter.utils.UserActionLogger
import timber.log.Timber

/**
 * Manages touch zone gesture detection and handling for PlayerActivity.
 * 
 * Responsibilities:
 * - GestureDetector setup for images (9-zone and 2-zone modes)
 * - Touch zone detection and routing based on fullscreen/command panel mode
 * - Coordinate-based zone calculation with effective height adjustments
 * 
 * Zone layouts:
 * - Fullscreen mode: 9-zone grid (3x3) for full navigation control
 * - Command panel mode: 2-zone (left/right) for simple navigation
 * - Video/Audio: Upper portion only (75%/66%) to reserve space for player controls
 */
class TouchZoneGestureManager(
    private val binding: ActivityPlayerUnifiedBinding,
    private val viewModel: PlayerViewModel,
    private val touchZoneDetector: TouchZoneDetector,
    private val callback: TouchZoneCallback
) {
    
    interface TouchZoneCallback {
        fun isOverlayBlocking(): Boolean
        fun getTouchZonesEnabled(): Boolean
        fun getLoadFullSizeImages(): Boolean
        fun onBack()
        fun onCopy()
        fun onRename()
        fun onPrevious()
        fun onMove()
        fun onNext()
        fun onCommandPanelToggle()
        fun onDelete()
        fun onSlideshowToggle()
        fun showSlideshowEnabledMessage()
        fun updateSlideShowButton()
        fun updateSlideShow()
    }
    
    /**
     * GestureDetector for handling touch zones on ImageView/PhotoView.
     * Called from setOnTouchListener with low priority (after other UI elements).
     */
    fun createImageTouchGestureDetector(context: android.content.Context): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val currentFile = viewModel.state.value.currentFile
                val isInFullscreenMode = !viewModel.state.value.showCommandPanel
                val isImage = currentFile?.type == MediaType.IMAGE || currentFile?.type == MediaType.GIF
                
                Timber.w("TOUCH_ZONE_DEBUG: imageTouchGestureDetector.onSingleTapConfirmed - x=${e.x}, y=${e.y}, isImage=$isImage, fullscreen=$isInFullscreenMode, touchZones=${callback.getTouchZonesEnabled()}")
                
                // Don't handle touch zones when overlays (translation/OCR) are visible
                if (callback.isOverlayBlocking()) {
                    Timber.w("TOUCH_ZONE_DEBUG: -> overlay blocking, skipping touch zones")
                    return false
                }
                
                if (isImage) {
                    // In fullscreen mode: always use 9-zone grid (even if touchZones disabled)
                    // This is the ONLY way to exit fullscreen mode via touch
                    if (isInFullscreenMode) {
                        Timber.w("TOUCH_ZONE_DEBUG: IMAGE -> FULLSCREEN mode: calling handleTouchZone (9 zones) ✓✓✓")
                        handleTouchZone(e.x, e.y)
                    } else if (callback.getTouchZonesEnabled()) {
                        // Command panel mode: simplified 2-zone navigation (left/right)
                        // ONLY if touch zones are enabled by user
                        Timber.w("TOUCH_ZONE_DEBUG: IMAGE -> COMMAND PANEL mode: calling handleCommandPanelTouchZones (2 zones) ✓✓✓")
                        handleCommandPanelTouchZones(e.x, e.y)
                    } else {
                        Timber.w("TOUCH_ZONE_DEBUG: IMAGE -> COMMAND PANEL mode: touch zones DISABLED, ignoring ✗✗✗")
                    }
                    return true
                }
                return false
            }
        })
    }
    
    /**
     * GestureDetector for handling touch zones on PlayerView and general screen.
     * Handles PDF/EPUB/VIDEO/AUDIO/IMAGE/GIF with appropriate zone layouts.
     */
    fun createGestureDetector(context: android.content.Context): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val state = viewModel.state.value
                val currentFile = state.currentFile
                val showCommandPanel = state.showCommandPanel
                val isInFullscreenMode = !showCommandPanel
                
                UserActionLogger.logGesture("SingleTap", "x=${e.x.toInt()} y=${e.y.toInt()}", "PlayerActivity")
                Timber.w("TOUCH_ZONE_DEBUG: === onSingleTapConfirmed ===")
                Timber.w("TOUCH_ZONE_DEBUG: x=${e.x}, y=${e.y}")
                Timber.w("TOUCH_ZONE_DEBUG: state.showCommandPanel=$showCommandPanel")
                Timber.w("TOUCH_ZONE_DEBUG: isInFullscreenMode=$isInFullscreenMode (= !showCommandPanel)")
                Timber.w("TOUCH_ZONE_DEBUG: useTouchZones=${callback.getTouchZonesEnabled()}")
                Timber.w("TOUCH_ZONE_DEBUG: fileType=${currentFile?.type}")
                Timber.w("TOUCH_ZONE_DEBUG: condition (isInFullscreenMode && useTouchZones) = ${isInFullscreenMode && callback.getTouchZonesEnabled()}")
                
                // PDF/EPUB/TEXT: Handled by specialized managers
                val isPdfOrEpub = currentFile?.type == MediaType.PDF || currentFile?.type == MediaType.EPUB
                val isText = currentFile?.type == MediaType.TEXT
                
                if (isPdfOrEpub || isText) {
                    // These file types handle their own touch zones via specialized managers
                    return true
                }
                
                // For IMAGE/GIF/VIDEO/AUDIO:
                // - Fullscreen mode: ALWAYS use 9-zone grid (required to exit fullscreen)
                // - Command panel mode: 2-zone navigation (only if touchZones enabled)
                if (isInFullscreenMode) {
                    Timber.w("TOUCH_ZONE_DEBUG: ✓✓✓ FULLSCREEN mode detected - Using FULL 9 touch zones for ${currentFile?.type}")
                    handleTouchZone(e.x, e.y)
                } else if (callback.getTouchZonesEnabled() && 
                          (currentFile?.type == MediaType.VIDEO || 
                           currentFile?.type == MediaType.AUDIO ||
                           currentFile?.type == MediaType.IMAGE ||
                           currentFile?.type == MediaType.GIF)) {
                    // Command panel mode: simplified navigation touch zones
                    // Upper portion divided: left = previous, right = next
                    // Lower portion reserved for player controls (VIDEO/AUDIO) or command panel (IMAGE/GIF)
                    Timber.w("TOUCH_ZONE_DEBUG: ✓✓✓ COMMAND PANEL mode detected - Using SIMPLIFIED 2 zones (touchZones=${callback.getTouchZonesEnabled()})")
                    handleCommandPanelTouchZones(e.x, e.y)
                } else {
                    Timber.w("TOUCH_ZONE_DEBUG: ✗✗✗ NO TOUCH ZONE handling - fileType=${currentFile?.type}, touchZones=${callback.getTouchZonesEnabled()}, fullscreen=$isInFullscreenMode")
                }
                
                return true
            }
        })
    }
    
    /**
     * Handle touch zones for static images (3x3 grid)
     * For video: only upper 75% of screen is touch-sensitive (lower 25% reserved for ExoPlayer controls)
     * For audio: only upper 66% of screen is touch-sensitive (lower 34% reserved for ExoPlayer controls)
     */
    private fun handleTouchZone(x: Float, y: Float) {
        val currentFile = viewModel.state.value.currentFile
        val screenWidth = binding.root.width
        val screenHeight = binding.root.height
        
        Timber.d("TOUCH_ZONE_DEBUG: handleTouchZone CALLED - x=$x, y=$y, screen=${screenWidth}x${screenHeight}, fileType=${currentFile?.type}")
        
        // Safety check: if screen dimensions not ready yet (e.g., during rotation), ignore
        if (screenWidth <= 0 || screenHeight <= 0) {
            Timber.w("TOUCH_ZONE_DEBUG: Invalid screen dimensions (${screenWidth}x${screenHeight}) - layout not ready, ignoring touch")
            return
        }
        
        // For video/audio, limit touch zones to upper portion to leave space for ExoPlayer controls
        // Audio: 66% (upper two thirds), Video: 75% (upper three quarters)
        val effectiveHeight = when (currentFile?.type) {
            MediaType.AUDIO -> (screenHeight * 0.66f).toInt() // Upper 66% (2/3) for audio
            MediaType.VIDEO -> (screenHeight * 0.75f).toInt() // Upper 75% for video
            else -> screenHeight
        }
        
        Timber.d("TOUCH_ZONE_DEBUG: effectiveHeight=$effectiveHeight, y=$y, y>effectiveHeight=${y > effectiveHeight}")
        
        // If touch is below effective height, ignore (reserved for player controls)
        if (y > effectiveHeight) {
            Timber.d("TOUCH_ZONE_DEBUG: Touch below effectiveHeight - IGNORED")
            return
        }
        
        val zone = touchZoneDetector.detectZone(x, y, screenWidth, effectiveHeight)
        Timber.d("TOUCH_ZONE_DEBUG: Detected zone = $zone")
        
        // Stop slideshow on any touch zone except NEXT and SLIDESHOW (toggle handled separately)
        if (zone != TouchZone.NEXT && zone != TouchZone.SLIDESHOW && zone != TouchZone.NONE) {
            if (viewModel.state.value.isSlideShowActive) {
                viewModel.toggleSlideShow()
                callback.updateSlideShow()
            }
        }
        
        when (zone) {
            TouchZone.BACK -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("NAVIGATION: BACK triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: TOP-LEFT-CORNER (zone 1 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onBack()
            }
            TouchZone.COPY -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: COPY triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: TOP-CENTER (zone 2 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onCopy()
            }
            TouchZone.RENAME -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: RENAME triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: TOP-RIGHT-CORNER (zone 3 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onRename()
            }
            TouchZone.PREVIOUS -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("NAVIGATION: PREVIOUS triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: MIDDLE-LEFT (zone 4 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onPrevious()
            }
            TouchZone.MOVE -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: MOVE triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: MIDDLE-CENTER (zone 5 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onMove()
            }
            TouchZone.NEXT -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("NAVIGATION: NEXT triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: MIDDLE-RIGHT (zone 6 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("Slideshow active: ${viewModel.state.value.isSlideShowActive}")
                Timber.w("═════════════════════════════════════════")
                // Reset slideshow timer but keep slideshow running
                if (viewModel.state.value.isSlideShowActive) {
                    callback.updateSlideShow()
                }
                callback.onNext()
            }
            TouchZone.COMMAND_PANEL -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: COMMAND_PANEL toggle triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: BOTTOM-LEFT-CORNER (zone 7 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("Current state: showCommandPanel=${viewModel.state.value.showCommandPanel}")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onCommandPanelToggle()
            }
            TouchZone.DELETE -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: DELETE triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: BOTTOM-CENTER (zone 8 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                callback.onDelete()
            }
            TouchZone.SLIDESHOW -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("ACTION: SLIDESHOW toggle triggered")
                Timber.w("Source: 9-zone grid (handleTouchZone)")
                Timber.w("Zone: BOTTOM-RIGHT-CORNER (zone 9 in 3x3 grid)")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("Current state: isSlideShowActive=${viewModel.state.value.isSlideShowActive}")
                Timber.w("FileType: ${viewModel.state.value.currentFile?.type}")
                Timber.w("═════════════════════════════════════════")
                val wasActive = viewModel.state.value.isSlideShowActive
                callback.onSlideshowToggle()
                
                // Show popup when enabling slideshow
                if (!wasActive && viewModel.state.value.isSlideShowActive) {
                    callback.showSlideshowEnabledMessage()
                }
                
                callback.updateSlideShowButton()
                callback.updateSlideShow()
            }
            TouchZone.NONE -> {
                Timber.w("═════════════════════════════════════════")
                Timber.w("TOUCH_ZONE: NONE detected")
                Timber.w("Touch: x=$x, y=$y")
                Timber.w("Screen: ${binding.root.width}x${binding.root.height}, effectiveHeight=$effectiveHeight")
                Timber.w("Zone detection failed - no action")
                Timber.w("═════════════════════════════════════════")
                // No action
            }
        }
    }
    
    /**
     * Handle simplified touch zones for command panel mode
     * - PhotoView mode (loadFullSizeImages=true): 3 zones (20% left=Previous, 60% center=Gestures, 20% right=Next)
     * - Standard mode: 2 zones (50% left=Previous, 50% right=Next)
     * Upper area divided, toolbar and command panel areas excluded
     */
    private fun handleCommandPanelTouchZones(x: Float, y: Float) {
        val screenWidth = binding.root.width
        val screenHeight = binding.root.height
        
        // Safety check
        if (screenWidth <= 0 || screenHeight <= 0) {
            Timber.w("TouchZoneGestureManager.handleCommandPanelTouchZones: Invalid screen dimensions - ignoring touch")
            return
        }
        
        // Get top command panel bottom (content starts below it)
        val topPanelBottom = if (binding.topCommandPanel.isVisible) binding.topCommandPanel.bottom else 0
        
        // Get bottom panels top position (content ends above them)
        // Use copyToPanel or moveToPanel (whichever is visible and higher up)
        val bottomPanelTop = when {
            binding.copyToPanel.isVisible && binding.moveToPanel.isVisible -> 
                minOf(binding.copyToPanel.top, binding.moveToPanel.top)
            binding.copyToPanel.isVisible -> binding.copyToPanel.top
            binding.moveToPanel.isVisible -> binding.moveToPanel.top
            else -> screenHeight // No bottom panels, use full height
        }
        
        // If click in top panel area - ignore (let buttons handle it)
        if (y < topPanelBottom) {
            Timber.d("TouchZoneGestureManager.handleCommandPanelTouchZones: Click in top panel area (y=$y < topPanelBottom=$topPanelBottom), ignoring")
            return
        }
        
        // If click in bottom panel area - ignore (let buttons handle it)
        if (y > bottomPanelTop) {
            Timber.d("TouchZoneGestureManager.handleCommandPanelTouchZones: Click in bottom panel area (y=$y > bottomPanelTop=$bottomPanelTop), ignoring")
            return
        }
        
        // Determine if using PhotoView 3-zone mode or standard 2-zone mode
        val usePhotoView = callback.getLoadFullSizeImages()
        
        if (usePhotoView) {
            // PhotoView 3-zone mode: 20% left | 60% center (gestures) | 20% right
            val leftZoneEnd = screenWidth * 0.20f
            val rightZoneStart = screenWidth * 0.80f
            
            Timber.w("TouchZoneGestureManager.handleCommandPanelTouchZones: PhotoView 3-ZONE mode detected")
            Timber.w("Touch in content area (topPanel=$topPanelBottom, y=$y, bottomPanel=$bottomPanelTop), width=$screenWidth")
            Timber.w("Zone boundaries: LEFT=[0-$leftZoneEnd], CENTER=[$leftZoneEnd-$rightZoneStart], RIGHT=[$rightZoneStart-$screenWidth]")
            
            when {
                x < leftZoneEnd -> {
                    Timber.w("═════════════════════════════════════════")
                    Timber.w("NAVIGATION: PREVIOUS triggered")
                    Timber.w("Source: 3-zone PhotoView mode (handleCommandPanelTouchZones)")
                    Timber.w("Zone: LEFT 20% (x=$x < $leftZoneEnd)")
                    Timber.w("Touch: x=$x, y=$y")
                    Timber.w("Content area: topPanel=$topPanelBottom to bottomPanel=$bottomPanelTop")
                    Timber.w("═════════════════════════════════════════")
                    callback.onPrevious()
                }
                x > rightZoneStart -> {
                    Timber.w("═════════════════════════════════════════")
                    Timber.w("NAVIGATION: NEXT triggered")
                    Timber.w("Source: 3-zone PhotoView mode (handleCommandPanelTouchZones)")
                    Timber.w("Zone: RIGHT 20% (x=$x > $rightZoneStart)")
                    Timber.w("Touch: x=$x, y=$y")
                    Timber.w("Content area: topPanel=$topPanelBottom to bottomPanel=$bottomPanelTop")
                    Timber.w("═════════════════════════════════════════")
                    callback.onNext()
                }
                else -> {
                    Timber.w("TouchZoneGestureManager.handleCommandPanelTouchZones: CENTER zone (x=$x in [$leftZoneEnd-$rightZoneStart]) - PhotoView gestures (pinch/zoom/rotate)")
                    // Center zone - let PhotoView handle gestures (no navigation action)
                }
            }
        } else {
            // Standard 2-zone mode: 50% left | 50% right
            Timber.w("TouchZoneGestureManager.handleCommandPanelTouchZones: Standard 2-ZONE mode")
            Timber.w("Touch in content area (topPanel=$topPanelBottom, y=$y, bottomPanel=$bottomPanelTop), width=$screenWidth")
            
            val isLeftHalf = x < (screenWidth / 2f)
            
            if (isLeftHalf) {
                Timber.w("═════════════════════════════════════════")
                Timber.w("NAVIGATION: PREVIOUS triggered")
                Timber.w("Source: 2-zone mode (handleCommandPanelTouchZones)")
                Timber.w("Zone: LEFT HALF of content area")
                Timber.w("Touch: x=$x (< ${screenWidth/2f}), y=$y")
                Timber.w("Content area: topPanel=$topPanelBottom to bottomPanel=$bottomPanelTop")
                Timber.w("═════════════════════════════════════════")
                callback.onPrevious()
            } else {
                Timber.w("═════════════════════════════════════════")
                Timber.w("NAVIGATION: NEXT triggered")
                Timber.w("Source: 2-zone mode (handleCommandPanelTouchZones)")
                Timber.w("Zone: RIGHT HALF of content area")
                Timber.w("Touch: x=$x (>= ${screenWidth/2f}), y=$y")
                Timber.w("Content area: topPanel=$topPanelBottom to bottomPanel=$bottomPanelTop")
                Timber.w("═════════════════════════════════════════")
                callback.onNext()
            }
        }
    }
}
