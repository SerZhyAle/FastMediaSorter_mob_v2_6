package com.sza.fastmediasorter.ui.player.helpers

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.media3.exoplayer.ExoPlayer
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.ui.player.PlayerViewModel
import timber.log.Timber

/**
 * Handles keyboard input for PlayerActivity:
 * - Navigation keys (Left/Right, Page Up/Down)
 * - Delete key (Del, Forward Del)
 * - Media controls (Enter, Media Play/Pause)
 * - Function keys (F1-F7) for various actions
 * - Escape key to exit player
 */
class PlayerKeyboardHandler(
    private val viewModel: PlayerViewModel,
    private val callback: PlayerKeyboardCallback
) {
    
    interface PlayerKeyboardCallback {
        fun onDeleteFile()
        fun onExitPlayer()
        fun onToggleSlideshow()
        fun onShowRenameDialog()
        fun onShowFileInfo()
        fun onToggleCommandPanel()
        fun onToggleCopyPanel()
        fun onToggleMovePanel()
        fun onShowEditDialog()
        fun getExoPlayer(): ExoPlayer?
        fun getCurrentMediaType(): MediaType?
        fun onPdfNextPage()
        fun onPdfPreviousPage()
        fun onTextScrollDown()
        fun onTextScrollUp()
    }
    
    /**
     * Process keyboard event and return true if handled
     */
    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("PlayerKeyboardHandler: keyCode=$keyCode")
        
        val currentType = callback.getCurrentMediaType()
        
        when (keyCode) {
            // PageUp: PDF/TXT - previous page, others - previous file
            KeyEvent.KEYCODE_PAGE_UP -> {
                when (currentType) {
                    MediaType.PDF -> {
                        callback.onPdfPreviousPage()
                        return true
                    }
                    MediaType.TEXT -> {
                        callback.onTextScrollUp()
                        return true
                    }
                    else -> {
                        Timber.d("PlayerKeyboardHandler: PageUp key - navigating to previous file")
                        viewModel.previousFile()
                        return true
                    }
                }
            }
            
            // PageDown: PDF/TXT - next page, others - next file
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                when (currentType) {
                    MediaType.PDF -> {
                        callback.onPdfNextPage()
                        return true
                    }
                    MediaType.TEXT -> {
                        callback.onTextScrollDown()
                        return true
                    }
                    else -> {
                        Timber.d("PlayerKeyboardHandler: PageDown key - navigating to next file")
                        viewModel.nextFile()
                        return true
                    }
                }
            }
            
            // Arrow keys: always navigate files
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                Timber.d("PlayerKeyboardHandler: Left arrow key - navigating to previous file")
                viewModel.previousFile()
                return true
            }
            
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                Timber.d("PlayerKeyboardHandler: Right arrow key - navigating to next file")
                viewModel.nextFile()
                return true
            }
            
            // Delete current file
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                callback.onDeleteFile()
                return true
            }
            
            // Play/Pause video/audio
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                handlePlayPause()
                return true
            }
            
            // Exit player (back to browse)
            KeyEvent.KEYCODE_ESCAPE -> {
                callback.onExitPlayer()
                return true
            }
            
            // F1: Start/stop slideshow
            KeyEvent.KEYCODE_F1 -> {
                callback.onToggleSlideshow()
                return true
            }
            
            // F2: Rename file
            KeyEvent.KEYCODE_F2 -> {
                callback.onShowRenameDialog()
                return true
            }
            
            // F3: Show file info
            KeyEvent.KEYCODE_F3 -> {
                callback.onShowFileInfo()
                return true
            }
            
            // F4: Toggle fullscreen/command panel mode
            KeyEvent.KEYCODE_F4 -> {
                callback.onToggleCommandPanel()
                return true
            }
            
            // F5: Toggle Copy To panel
            KeyEvent.KEYCODE_F5 -> {
                callback.onToggleCopyPanel()
                return true
            }
            
            // F6: Toggle Move To panel
            KeyEvent.KEYCODE_F6 -> {
                callback.onToggleMovePanel()
                return true
            }
            
            // F7: Image edit dialog / Player settings for video
            KeyEvent.KEYCODE_F7 -> {
                callback.onShowEditDialog()
                return true
            }
        }
        
        return false // Not handled
    }
    
    /**
     * Handle Enter/Media Play/Pause for video/audio playback
     */
    private fun handlePlayPause() {
        val currentType = callback.getCurrentMediaType()
        if (currentType == MediaType.VIDEO || currentType == MediaType.AUDIO) {
            callback.getExoPlayer()?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Timber.d("PlayerKeyboardHandler: Paused playback")
                } else {
                    player.play()
                    Timber.d("PlayerKeyboardHandler: Resumed playback")
                }
            }
        }
    }
    
    /**
     * Handle mouse scroll events for PDF/TXT page navigation
     */
    fun handleGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action != MotionEvent.ACTION_SCROLL) return false
        
        val currentType = callback.getCurrentMediaType()
        val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        
        when (currentType) {
            MediaType.PDF -> {
                if (scrollY > 0) {
                    callback.onPdfPreviousPage()
                } else if (scrollY < 0) {
                    callback.onPdfNextPage()
                }
                return true
            }
            MediaType.TEXT -> {
                if (scrollY > 0) {
                    callback.onTextScrollUp()
                } else if (scrollY < 0) {
                    callback.onTextScrollDown()
                }
                return true
            }
            else -> return false
        }
    }
}
