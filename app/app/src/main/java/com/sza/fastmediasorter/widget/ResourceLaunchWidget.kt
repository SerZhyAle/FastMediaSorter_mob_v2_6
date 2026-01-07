package com.sza.fastmediasorter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import timber.log.Timber

/**
 * App Widget provider for Resource Launch Widget.
 * Allows users to quickly open a configured resource folder.
 */
class ResourceLaunchWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(keyResourceId(appWidgetId))
            editor.remove(keyResourceName(appWidgetId))
            editor.remove(keyResourceType(appWidgetId))
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "com.sza.fastmediasorter.widget.ResourceLaunchWidget"
        
        fun keyResourceId(appWidgetId: Int) = "resource_id_$appWidgetId"
        fun keyResourceName(appWidgetId: Int) = "resource_name_$appWidgetId"
        fun keyResourceType(appWidgetId: Int) = "resource_type_$appWidgetId"

        /**
         * Save widget configuration to SharedPreferences.
         */
        fun saveWidgetConfig(
            context: Context,
            appWidgetId: Int,
            resourceId: Long,
            resourceName: String,
            resourceType: ResourceType
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(keyResourceId(appWidgetId), resourceId)
                .putString(keyResourceName(appWidgetId), resourceName)
                .putString(keyResourceType(appWidgetId), resourceType.name)
                .apply()
        }

        /**
         * Load widget configuration from SharedPreferences.
         */
        fun loadWidgetConfig(context: Context, appWidgetId: Int): WidgetConfig? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val resourceId = prefs.getLong(keyResourceId(appWidgetId), -1)
            val resourceName = prefs.getString(keyResourceName(appWidgetId), null)
            val resourceTypeName = prefs.getString(keyResourceType(appWidgetId), null)
            
            if (resourceId == -1L || resourceName == null || resourceTypeName == null) {
                return null
            }
            
            val resourceType = try {
                ResourceType.valueOf(resourceTypeName)
            } catch (e: IllegalArgumentException) {
                ResourceType.LOCAL
            }
            
            return WidgetConfig(resourceId, resourceName, resourceType)
        }

        /**
         * Update a single app widget.
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val config = loadWidgetConfig(context, appWidgetId)
            
            val views = RemoteViews(context.packageName, R.layout.widget_resource_launch)
            
            if (config != null) {
                views.setTextViewText(R.id.widgetTitle, config.name)
                views.setImageViewResource(R.id.widgetIcon, getIconForType(config.type))
                
                // Create intent to open BrowseActivity with resource ID
                val intent = Intent(context, BrowseActivity::class.java).apply {
                    putExtra(BrowseActivity.EXTRA_RESOURCE_ID, config.resourceId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            } else {
                views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_not_configured))
                views.setImageViewResource(R.id.widgetIcon, R.drawable.ic_folder)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Timber.d("Updated widget $appWidgetId with config: $config")
        }

        /**
         * Get appropriate icon for resource type.
         */
        private fun getIconForType(type: ResourceType): Int {
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
    }

    /**
     * Data class for widget configuration.
     */
    data class WidgetConfig(
        val resourceId: Long,
        val name: String,
        val type: ResourceType
    )
}
