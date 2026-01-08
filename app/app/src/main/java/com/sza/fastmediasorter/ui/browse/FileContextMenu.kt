package com.sza.fastmediasorter.ui.browse

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType

/**
 * Context menu for file operations in BrowseActivity
 * Shows relevant options based on file type
 */
class FileContextMenu(
    private val context: Context,
    private val anchor: View,
    private val file: MediaFile,
    private val listener: OnMenuItemSelectedListener
) {
    
    interface OnMenuItemSelectedListener {
        fun onOpen(file: MediaFile)
        fun onOpenWith(file: MediaFile)
        fun onRename(file: MediaFile)
        fun onCopy(file: MediaFile)
        fun onMove(file: MediaFile)
        fun onDelete(file: MediaFile)
        fun onShare(file: MediaFile)
        fun onInfo(file: MediaFile)
        fun onAddToFavorites(file: MediaFile)
        fun onRemoveFromFavorites(file: MediaFile)
        fun onSetAsWallpaper(file: MediaFile)
        fun onRotate(file: MediaFile)
        fun onExtractAudio(file: MediaFile)
    }
    
    private val popup = PopupMenu(context, anchor)
    
    init {
        buildMenu()
    }
    
    private fun buildMenu() {
        popup.menuInflater.inflate(R.menu.context_menu_file, popup.menu)
        
        // Configure visibility based on file type
        val menu = popup.menu
        
        // Wallpaper option - only for images
        menu.findItem(R.id.action_set_wallpaper)?.isVisible = 
            file.type == MediaType.IMAGE
        
        // Rotate option - only for images and videos
        menu.findItem(R.id.action_rotate)?.isVisible = 
            file.type == MediaType.IMAGE || file.type == MediaType.VIDEO
        
        // Extract audio - only for videos
        menu.findItem(R.id.action_extract_audio)?.isVisible = 
            file.type == MediaType.VIDEO
        
        // Favorites - toggle based on current state
        menu.findItem(R.id.action_add_favorite)?.isVisible = !file.isFavorite
        menu.findItem(R.id.action_remove_favorite)?.isVisible = file.isFavorite
        
        // Set click listener
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open -> {
                    listener.onOpen(file)
                    true
                }
                R.id.action_open_with -> {
                    listener.onOpenWith(file)
                    true
                }
                R.id.action_rename -> {
                    listener.onRename(file)
                    true
                }
                R.id.action_copy -> {
                    listener.onCopy(file)
                    true
                }
                R.id.action_move -> {
                    listener.onMove(file)
                    true
                }
                R.id.action_delete -> {
                    listener.onDelete(file)
                    true
                }
                R.id.action_share -> {
                    listener.onShare(file)
                    true
                }
                R.id.action_info -> {
                    listener.onInfo(file)
                    true
                }
                R.id.action_add_favorite -> {
                    listener.onAddToFavorites(file)
                    true
                }
                R.id.action_remove_favorite -> {
                    listener.onRemoveFromFavorites(file)
                    true
                }
                R.id.action_set_wallpaper -> {
                    listener.onSetAsWallpaper(file)
                    true
                }
                R.id.action_rotate -> {
                    listener.onRotate(file)
                    true
                }
                R.id.action_extract_audio -> {
                    listener.onExtractAudio(file)
                    true
                }
                else -> false
            }
        }
    }
    
    fun show() {
        popup.show()
    }
    
    fun dismiss() {
        popup.dismiss()
    }
}
