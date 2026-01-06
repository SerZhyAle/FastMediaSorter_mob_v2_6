package com.sza.fastmediasorter.ui.browse.managers

import android.view.KeyEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

/**
 * Manages keyboard navigation in BrowseActivity.
 * Handles arrow keys, page navigation, selection shortcuts, and function keys.
 */
class KeyboardNavigationManager(
    private val recyclerView: RecyclerView,
    private val callbacks: KeyboardNavigationCallbacks
) {
    
    interface KeyboardNavigationCallbacks {
        fun getCurrentFocusPosition(): Int
        fun getMediaFilesCount(): Int
        fun getSelectedFilesCount(): Int
        fun toggleCurrentItemSelection(position: Int)
        fun playCurrentOrSelected(position: Int)
        fun onBackPressed()
        fun showDeleteConfirmation()
        fun showCopyDialog()
        fun showMoveDialog()
        fun performButtonClick(buttonId: Int)
    }
    
    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val layoutManager = recyclerView.layoutManager
        val currentPosition = callbacks.getCurrentFocusPosition()
        
        return when (keyCode) {
            // Arrow navigation
            KeyEvent.KEYCODE_DPAD_UP -> {
                navigateUp(currentPosition, layoutManager)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                navigateDown(currentPosition, layoutManager)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                navigateLeft(currentPosition, layoutManager)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                navigateRight(currentPosition, layoutManager)
                true
            }
            
            // Page navigation
            KeyEvent.KEYCODE_PAGE_UP -> {
                scrollPage(-1)
                true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                scrollPage(1)
                true
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                scrollToPosition(0)
                true
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                val lastPosition = callbacks.getMediaFilesCount() - 1
                scrollToPosition(lastPosition)
                true
            }
            
            // Selection actions
            KeyEvent.KEYCODE_SPACE -> {
                callbacks.toggleCurrentItemSelection(currentPosition)
                true
            }
            
            // Play action
            KeyEvent.KEYCODE_ENTER -> {
                callbacks.playCurrentOrSelected(currentPosition)
                true
            }
            
            // Back action
            KeyEvent.KEYCODE_ESCAPE -> {
                callbacks.onBackPressed()
                true
            }
            
            // Delete action
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                if (callbacks.getSelectedFilesCount() > 0) {
                    callbacks.showDeleteConfirmation()
                }
                true
            }
            
            // Function keys handled via button clicks
            else -> false
        }
    }
    
    private fun navigateUp(currentPosition: Int, layoutManager: RecyclerView.LayoutManager?) {
        when (layoutManager) {
            is LinearLayoutManager -> {
                val targetPosition = (currentPosition - 1).coerceAtLeast(0)
                scrollToPosition(targetPosition)
            }
            is GridLayoutManager -> {
                val spanCount = layoutManager.spanCount
                val targetPosition = (currentPosition - spanCount).coerceAtLeast(0)
                scrollToPosition(targetPosition)
            }
        }
    }
    
    private fun navigateDown(currentPosition: Int, layoutManager: RecyclerView.LayoutManager?) {
        val itemCount = callbacks.getMediaFilesCount()
        when (layoutManager) {
            is LinearLayoutManager -> {
                val targetPosition = (currentPosition + 1).coerceAtMost(itemCount - 1)
                scrollToPosition(targetPosition)
            }
            is GridLayoutManager -> {
                val spanCount = layoutManager.spanCount
                val targetPosition = (currentPosition + spanCount).coerceAtMost(itemCount - 1)
                scrollToPosition(targetPosition)
            }
        }
    }
    
    private fun navigateLeft(currentPosition: Int, layoutManager: RecyclerView.LayoutManager?) {
        if (layoutManager !is GridLayoutManager) return
        val targetPosition = (currentPosition - 1).coerceAtLeast(0)
        scrollToPosition(targetPosition)
    }
    
    private fun navigateRight(currentPosition: Int, layoutManager: RecyclerView.LayoutManager?) {
        if (layoutManager !is GridLayoutManager) return
        val itemCount = callbacks.getMediaFilesCount()
        val targetPosition = (currentPosition + 1).coerceAtMost(itemCount - 1)
        scrollToPosition(targetPosition)
    }
    
    private fun scrollToPosition(position: Int) {
        if (position < 0) return
        recyclerView.scrollToPosition(position)
        recyclerView.post { recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
    }
    
    private fun scrollPage(direction: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        val pageSize = lastVisible - firstVisible
        
        val targetPosition = when {
            direction < 0 -> (firstVisible - pageSize).coerceAtLeast(0)
            else -> (lastVisible + pageSize).coerceAtMost(callbacks.getMediaFilesCount() - 1)
        }
        
        scrollToPosition(targetPosition)
    }
}
