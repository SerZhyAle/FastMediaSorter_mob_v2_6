package com.sza.fastmediasorter.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.db.AppDatabase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Service providing data for Favorites widget list
 */
class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesRemoteViewsFactory(applicationContext)
    }
}

class FavoritesRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private data class FavoriteItem(
        val uri: String,
        val displayName: String,
        val mediaType: Int,
        val resourceId: Long
    )

    private var favorites = listOf<FavoriteItem>()

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface FavoritesWidgetEntryPoint {
        fun database(): AppDatabase
    }

    override fun onCreate() {
        loadFavorites()
    }

    override fun onDataSetChanged() {
        loadFavorites()
    }

    private fun loadFavorites() {
        try {
            // TODO: Implement actual favorites loading when FavoritesDao is added to AppDatabase
            // For now, use empty list as widgets framework requires implementation
            favorites = emptyList()
            
            /* Future implementation:
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                FavoritesWidgetEntryPoint::class.java
            )
            val database = entryPoint.database()
            
            favorites = runBlocking {
                database.favoritesDao().getAllFavorites()
                    .first()
                    .take(10)
                    .map { entity ->
                        FavoriteItem(
                            uri = entity.uri,
                            displayName = entity.displayName,
                            mediaType = entity.mediaType,
                            resourceId = entity.resourceId
                        )
                    }
            }
            */
        } catch (e: Exception) {
            Timber.e(e, "Failed to load favorites for widget")
            favorites = emptyList()
        }
    }

    override fun onDestroy() {
        favorites = emptyList()
    }

    override fun getCount(): Int = favorites.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_favorites_item)
        
        if (position >= favorites.size) {
            return views
        }
        
        val favorite = favorites[position]
        
        // Set file name
        views.setTextViewText(R.id.widget_favorite_name, favorite.displayName)
        
        // Set icon based on media type
        val iconRes = when (favorite.mediaType) {
            0 -> R.drawable.ic_image // IMAGE
            1 -> R.drawable.ic_video // VIDEO
            2 -> R.drawable.ic_audio // AUDIO
            3 -> R.drawable.ic_gif   // GIF
            4 -> R.drawable.ic_image // TEXT (use image icon as fallback)
            5 -> R.drawable.ic_image // PDF (use image icon as fallback)
            6 -> R.drawable.ic_image // EPUB (use image icon as fallback)
            else -> R.drawable.ic_image
        }
        views.setImageViewResource(R.id.widget_favorite_icon, iconRes)
        
        // Set click intent matching PlayerActivity expectations
        val fillInIntent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initialFilePath", favorite.uri)
            putExtra("resourceId", favorite.resourceId)
            putExtra("skipAvailabilityCheck", true)
        }
        views.setOnClickFillInIntent(R.id.widget_favorite_item, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
