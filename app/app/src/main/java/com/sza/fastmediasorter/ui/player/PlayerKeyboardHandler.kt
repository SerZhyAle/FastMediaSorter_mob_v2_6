package com.sza.fastmediasorter.ui.player

import android.view.KeyEvent
import com.sza.fastmediasorter.domain.model.MediaType
import timber.log.Timber

/**
 * Handles keyboard input for PlayerActivity.
 * Supports 40+ keyboard shortcuts for navigation, playback control, and file operations.
 *
 * Supported Keys:
 * - Arrow Left/Right: Previous/Next file
 * - Arrow Up/Down: Volume (video/audio) or Brightness (images)
 * - Space: Play/Pause or toggle slideshow
 * - Enter: Next page/chapter or start slideshow
 * - Delete: Delete current file
 * - R: Rotate image 90°
 * - F2: Rename file
 * - I: Show file info
 * - F: Toggle fullscreen
 * - S: Toggle slideshow
 * - C: Copy to first destination
 * - M: Move to first destination
 * - Escape: Close overlays or exit player
 * - 0-9: Jump to 0%-90% (video)
 * - Page Up/Down: Previous/Next page (PDF/EPUB)
 * - Home/End: First/Last page or Jump to start/end
 * - Ctrl+C: Copy text selection
 * - Ctrl+S: Save (text editor)
 * - Ctrl+F: Show search
 */
class PlayerKeyboardHandler(
    private val listener: KeyboardActionListener
) {

    /**
     * Interface for keyboard action callbacks.
     */
    interface KeyboardActionListener {
        fun getCurrentMediaType(): MediaType?
        
        // Navigation
        fun onNavigatePrevious()
        fun onNavigateNext()
        fun onNavigateFirst()
        fun onNavigateLast()
        
        // Playback control
        fun onPlayPause()
        fun onVolumeUp()
        fun onVolumeDown()
        fun onMute()
        fun onSeekToPercent(percent: Int)
        fun onSeekStart()
        fun onSeekEnd()
        
        // Image controls
        fun onRotateImage()
        fun onBrightnessUp()
        fun onBrightnessDown()
        
        // Slideshow
        fun onToggleSlideshow()
        fun onStartSlideshow()
        
        // File operations
        fun onShowRenameDialog()
        fun onDeleteFile()
        fun onShowFileInfo()
        fun onCopyToFirstDestination()
        fun onMoveToFirstDestination()
        
        // UI control
        fun onToggleFullscreen()
        fun onExitPlayer()
        fun onCloseOverlay(): Boolean // Returns true if an overlay was closed
        
        // PDF/EPUB navigation
        fun onPreviousPage()
        fun onNextPage()
        fun onFirstPage()
        fun onLastPage()
        
        // Search and text
        fun onShowSearch()
        fun onCopyText()
        fun onSaveText()
    }

    /**
     * Handle key down events.
     * @return true if the event was handled, false otherwise
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isCtrlPressed = event.isCtrlPressed
        val isShiftPressed = event.isShiftPressed
        val mediaType = listener.getCurrentMediaType()

        Timber.d("KeyEvent: keyCode=$keyCode, ctrl=$isCtrlPressed, shift=$isShiftPressed, mediaType=$mediaType")

        // Handle Ctrl+key combinations first
        if (isCtrlPressed) {
            return handleCtrlKeyCombo(keyCode)
        }

        // Handle regular keys
        return when (keyCode) {
            // Arrow navigation
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                listener.onNavigatePrevious()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                listener.onNavigateNext()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                when (mediaType) {
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onVolumeUp()
                        true
                    }
                    MediaType.IMAGE, MediaType.GIF -> {
                        listener.onBrightnessUp()
                        true
                    }
                    else -> false
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                when (mediaType) {
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onVolumeDown()
                        true
                    }
                    MediaType.IMAGE, MediaType.GIF -> {
                        listener.onBrightnessDown()
                        true
                    }
                    else -> false
                }
            }

            // Space - Play/Pause or Slideshow toggle
            KeyEvent.KEYCODE_SPACE -> {
                when (mediaType) {
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onPlayPause()
                        true
                    }
                    MediaType.IMAGE, MediaType.GIF -> {
                        listener.onToggleSlideshow()
                        true
                    }
                    else -> false
                }
            }

            // Enter - Next page/chapter or start slideshow
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                when (mediaType) {
                    MediaType.PDF -> {
                        listener.onNextPage()
                        true
                    }
                    MediaType.EPUB -> {
                        listener.onNextPage() // Next chapter
                        true
                    }
                    MediaType.IMAGE, MediaType.GIF -> {
                        listener.onStartSlideshow()
                        true
                    }
                    else -> false
                }
            }

            // Delete - Delete file
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                listener.onDeleteFile()
                true
            }

            // R - Rotate image
            KeyEvent.KEYCODE_R -> {
                if (mediaType == MediaType.IMAGE || mediaType == MediaType.GIF) {
                    listener.onRotateImage()
                    true
                } else false
            }

            // F2 - Rename
            KeyEvent.KEYCODE_F2 -> {
                listener.onShowRenameDialog()
                true
            }

            // I - File info
            KeyEvent.KEYCODE_I -> {
                listener.onShowFileInfo()
                true
            }

            // F - Toggle fullscreen
            KeyEvent.KEYCODE_F -> {
                listener.onToggleFullscreen()
                true
            }

            // S - Toggle slideshow
            KeyEvent.KEYCODE_S -> {
                if (mediaType == MediaType.IMAGE || mediaType == MediaType.GIF) {
                    listener.onToggleSlideshow()
                    true
                } else false
            }

            // C - Copy to first destination
            KeyEvent.KEYCODE_C -> {
                listener.onCopyToFirstDestination()
                true
            }

            // M - Move to first destination / Mute
            KeyEvent.KEYCODE_M -> {
                when (mediaType) {
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onMute()
                        true
                    }
                    else -> {
                        listener.onMoveToFirstDestination()
                        true
                    }
                }
            }

            // Escape - Close overlay or exit
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                if (listener.onCloseOverlay()) {
                    true
                } else {
                    listener.onExitPlayer()
                    true
                }
            }

            // Number keys 0-9 - Video seek to percentage
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> {
                if (mediaType == MediaType.VIDEO) {
                    listener.onSeekStart()
                    true
                } else false
            }
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> handleNumberKey(1, mediaType)
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> handleNumberKey(2, mediaType)
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> handleNumberKey(3, mediaType)
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> handleNumberKey(4, mediaType)
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> handleNumberKey(5, mediaType)
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> handleNumberKey(6, mediaType)
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> handleNumberKey(7, mediaType)
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> handleNumberKey(8, mediaType)
            KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> handleNumberKey(9, mediaType)

            // Page Up/Down - PDF/EPUB pages, or volume for audio/video
            KeyEvent.KEYCODE_PAGE_UP -> {
                when (mediaType) {
                    MediaType.PDF, MediaType.EPUB -> {
                        listener.onPreviousPage()
                        true
                    }
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onVolumeUp()
                        true
                    }
                    else -> false
                }
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                when (mediaType) {
                    MediaType.PDF, MediaType.EPUB -> {
                        listener.onNextPage()
                        true
                    }
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        listener.onVolumeDown()
                        true
                    }
                    else -> false
                }
            }

            // Home/End - First/Last page or start/end of video
            KeyEvent.KEYCODE_MOVE_HOME -> {
                when (mediaType) {
                    MediaType.PDF, MediaType.EPUB -> {
                        listener.onFirstPage()
                        true
                    }
                    MediaType.VIDEO -> {
                        listener.onSeekStart()
                        true
                    }
                    else -> {
                        listener.onNavigateFirst()
                        true
                    }
                }
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                when (mediaType) {
                    MediaType.PDF, MediaType.EPUB -> {
                        listener.onLastPage()
                        true
                    }
                    MediaType.VIDEO -> {
                        listener.onSeekEnd()
                        true
                    }
                    else -> {
                        listener.onNavigateLast()
                        true
                    }
                }
            }

            // F1 - Toggle slideshow
            KeyEvent.KEYCODE_F1 -> {
                if (mediaType == MediaType.IMAGE || mediaType == MediaType.GIF) {
                    listener.onToggleSlideshow()
                    true
                } else false
            }

            // F3 - Show search
            KeyEvent.KEYCODE_F3 -> {
                listener.onShowSearch()
                true
            }

            else -> false
        }
    }

    /**
     * Handle Ctrl+key combinations.
     */
    private fun handleCtrlKeyCombo(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_C -> {
                listener.onCopyText()
                true
            }
            KeyEvent.KEYCODE_S -> {
                listener.onSaveText()
                true
            }
            KeyEvent.KEYCODE_F -> {
                listener.onShowSearch()
                true
            }
            else -> false
        }
    }

    /**
     * Handle number key press for video seeking.
     */
    private fun handleNumberKey(number: Int, mediaType: MediaType?): Boolean {
        return if (mediaType == MediaType.VIDEO) {
            listener.onSeekToPercent(number * 10) // 1 = 10%, 2 = 20%, etc.
            true
        } else {
            false
        }
    }
}
