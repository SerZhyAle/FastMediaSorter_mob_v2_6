package com.sza.fastmediasorter.ui.player.callbacks

import android.view.View
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.ui.player.PlayerGestureHelper
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.github.chrisbanes.photoview.PhotoView
import com.sza.fastmediasorter.ui.player.PlayerActivity
import com.sza.fastmediasorter.ui.player.PlayerViewModel

import com.sza.fastmediasorter.ui.player.helpers.EpubViewerManager
import com.sza.fastmediasorter.ui.player.helpers.PdfViewerManager
import timber.log.Timber

class PlayerGestureCallbackImpl(
    private val activity: PlayerActivity,
    private val viewModel: PlayerViewModel,
    private val binding: ActivityPlayerUnifiedBinding,
    private val pdfViewerManager: PdfViewerManager,
    private val epubViewerManager: EpubViewerManager
) : PlayerGestureHelper.GestureCallback {

    override fun onSwipeLeft() {
        val currentFile = viewModel.state.value.currentFile
        when (currentFile?.type) {
            MediaType.PDF -> {
                // PDF: Swipe left = next page
                pdfViewerManager.showNextPage()
            }
            MediaType.EPUB -> {
                // EPUB: Swipe left = next chapter
                epubViewerManager.showNextChapter()
            }
            else -> {
                // Other files: Swipe left = next file
                Timber.tag("TOUCH_ZONE_DEBUG").w("NEXT triggered by: Swipe LEFT (GestureHelper)")
                viewModel.nextFile()
            }
        }
    }

    override fun onSwipeRight() {
        val currentFile = viewModel.state.value.currentFile
        when (currentFile?.type) {
            MediaType.PDF -> {
                // PDF: Swipe right = previous page
                pdfViewerManager.showPreviousPage()
            }
            MediaType.EPUB -> {
                // EPUB: Swipe right = previous chapter
                epubViewerManager.showPreviousChapter()
            }
            else -> {
                // Other files: Swipe right = previous file
                Timber.tag("TOUCH_ZONE_DEBUG").w("PREVIOUS triggered by: Swipe RIGHT (GestureHelper)")
                viewModel.previousFile()
            }
        }
    }

    override fun onSwipeUp() {
        val currentFile = viewModel.state.value.currentFile
        if (currentFile?.type == MediaType.PDF) {
            // PDF: Swipe up = zoom out (decrease scale)
            val currentScale = binding.photoView.scale
            if (currentScale > binding.photoView.minimumScale) {
                binding.photoView.setScale(currentScale - 0.5f, true)
            }
        } else {
            if (!viewModel.state.value.showCommandPanel) {
                // Already in fullscreen, do nothing
            } else {
                viewModel.enterFullscreenMode()
            }
        }
    }

    override fun onSwipeDown() {
        val currentFile = viewModel.state.value.currentFile
        if (currentFile?.type == MediaType.PDF) {
            // PDF: Swipe down = zoom in (increase scale)
            val currentScale = binding.photoView.scale
            if (currentScale < binding.photoView.maximumScale) {
                binding.photoView.setScale(currentScale + 0.5f, true)
            }
        } else {
            if (viewModel.state.value.showCommandPanel) {
                // Already in command panel mode, do nothing
            } else {
                viewModel.enterCommandPanelMode()
            }
        }
    }

    override fun onDoubleTap() {
        viewModel.togglePause()
        if (viewModel.state.value.currentFile?.type == MediaType.IMAGE) {
            activity.updateSlideShow()
        }
    }

    override fun onLongPress() {
        // Long press could show quick actions menu
        activity.showFileInfo()
    }

    override fun onTouchZone(zone: PlayerGestureHelper.TouchZone) {
        when (zone) {
            PlayerGestureHelper.TouchZone.LEFT -> {
                Timber.tag("TOUCH_ZONE_DEBUG").w("PREVIOUS triggered by: 2-zone LEFT touch (GestureHelper)")
                viewModel.previousFile()
            }
            PlayerGestureHelper.TouchZone.RIGHT -> {
                Timber.tag("TOUCH_ZONE_DEBUG").w("NEXT triggered by: 2-zone RIGHT touch (GestureHelper)")
                viewModel.nextFile()
            }
            PlayerGestureHelper.TouchZone.CENTER -> {
                viewModel.togglePause()
                if (viewModel.state.value.currentFile?.type == MediaType.IMAGE) {
                    activity.updateSlideShow()
                }
            }
            PlayerGestureHelper.TouchZone.COPY_PANEL -> activity.showCopyDialog()
            PlayerGestureHelper.TouchZone.MOVE_PANEL -> activity.showMoveDialog()
            PlayerGestureHelper.TouchZone.DELETE -> activity.deleteCurrentFile()
        }
    }
}
