package com.sza.fastmediasorter.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import timber.log.Timber

/**
 * Helper class for managing dynamic app shortcuts.
 * Tracks recently visited resources and creates launcher shortcuts.
 */
object ShortcutHelper {

    private const val MAX_SHORTCUTS = 4
    private const val PREFS_NAME = "shortcut_prefs"
    private const val KEY_RECENT_RESOURCES = "recent_resource_ids"

    /**
     * Records a resource visit and updates dynamic shortcuts.
     * Call this when a resource is opened in BrowseActivity.
     */
    fun recordResourceVisit(context: Context, resource: Resource) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return // Dynamic shortcuts require Android 7.1+
        }

        try {
            // Update recent resources list
            val recentIds = getRecentResourceIds(context).toMutableList()
            
            // Remove if already exists and add to front
            recentIds.remove(resource.id)
            recentIds.add(0, resource.id)
            
            // Keep only MAX_SHORTCUTS items
            val trimmedIds = recentIds.take(MAX_SHORTCUTS)
            
            // Save to preferences
            saveRecentResourceIds(context, trimmedIds)
            
            Timber.d("Recorded visit to resource: ${resource.name} (ID: ${resource.id})")
        } catch (e: Exception) {
            Timber.e(e, "Failed to record resource visit")
        }
    }

    /**
     * Updates dynamic shortcuts with recently visited resources.
     * Call this after recording a visit or on app startup.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun updateDynamicShortcuts(context: Context, resources: List<Resource>) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager == null) {
                Timber.w("ShortcutManager not available")
                return
            }

            val recentIds = getRecentResourceIds(context)
            val shortcuts = mutableListOf<ShortcutInfo>()

            // Create shortcuts for recent resources that still exist
            for (resourceId in recentIds) {
                val resource = resources.find { it.id == resourceId }
                if (resource != null) {
                    shortcuts.add(createShortcut(context, resource))
                }
                
                if (shortcuts.size >= MAX_SHORTCUTS) {
                    break
                }
            }

            shortcutManager.dynamicShortcuts = shortcuts
            Timber.d("Updated ${shortcuts.size} dynamic shortcuts")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update dynamic shortcuts")
        }
    }

    /**
     * Removes all dynamic shortcuts.
     * Call this on logout or when clearing app data.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun clearDynamicShortcuts(context: Context) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.removeAllDynamicShortcuts()
            
            // Clear preferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_RECENT_RESOURCES)
                .apply()
            
            Timber.d("Cleared all dynamic shortcuts")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear dynamic shortcuts")
        }
    }

    /**
     * Creates a ShortcutInfo for a resource.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createShortcut(context: Context, resource: Resource): ShortcutInfo {
        val intent = Intent(context, BrowseActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(BrowseActivity.EXTRA_RESOURCE_ID, resource.id)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return ShortcutInfo.Builder(context, "resource_${resource.id}")
            .setShortLabel(resource.name)
            .setLongLabel(resource.name)
            .setIcon(Icon.createWithResource(context, getIconForResourceType(resource.type)))
            .setIntent(intent)
            .setRank(0) // Higher rank = higher priority
            .build()
    }

    /**
     * Returns the appropriate icon resource for a resource type.
     */
    private fun getIconForResourceType(type: ResourceType): Int {
        return when (type) {
            ResourceType.LOCAL -> R.drawable.ic_folder
            ResourceType.SMB -> R.drawable.ic_smb
            ResourceType.SFTP -> R.drawable.ic_sftp
            ResourceType.FTP -> R.drawable.ic_ftp
            ResourceType.GOOGLE_DRIVE -> R.drawable.ic_google_drive
            ResourceType.ONEDRIVE -> R.drawable.ic_onedrive
            ResourceType.DROPBOX -> R.drawable.ic_dropbox
        }
    }

    /**
     * Gets the list of recent resource IDs from preferences.
     */
    private fun getRecentResourceIds(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idsString = prefs.getString(KEY_RECENT_RESOURCES, "") ?: ""
        
        return if (idsString.isEmpty()) {
            emptyList()
        } else {
            idsString.split(",").mapNotNull { it.toLongOrNull() }
        }
    }

    /**
     * Saves the list of recent resource IDs to preferences.
     */
    private fun saveRecentResourceIds(context: Context, ids: List<Long>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_RECENT_RESOURCES, ids.joinToString(","))
            .apply()
    }
}
