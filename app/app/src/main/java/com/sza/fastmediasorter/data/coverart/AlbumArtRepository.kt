package com.sza.fastmediasorter.data.coverart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for album art retrieval and caching
 * Supports:
 * - Embedded ID3 cover art extraction
 * - iTunes API search
 * - WiFi-only download option
 * - Fallback cover generation
 * - Cover art caching
 */
@Singleton
class AlbumArtRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    
    private val iTunesApi: iTunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(iTunesApiService::class.java)
    }
    
    private val coverArtCache = mutableMapOf<String, String>()
    
    /**
     * Get album art for audio file
     * Priority:
     * 1. Embedded ID3 cover art
     * 2. Cached cover from previous searches
     * 3. iTunes API search (if WiFi or setting allows)
     * 4. Generated fallback cover
     */
    suspend fun getAlbumArt(
        filePath: String,
        artist: String? = null,
        album: String? = null
    ): AlbumArtResult = withContext(Dispatchers.IO) {
        try {
            // Try embedded cover art first
            val embeddedCover = extractEmbeddedCoverArt(filePath)
            if (embeddedCover != null) {
                return@withContext AlbumArtResult.EmbeddedArt(embeddedCover)
            }
            
            // Check cache
            val cacheKey = getCacheKey(artist, album)
            coverArtCache[cacheKey]?.let {
                return@withContext AlbumArtResult.CachedUrl(it)
            }
            
            // Search iTunes API if network allows
            if (shouldDownloadCoverArt() && !artist.isNullOrBlank() && !album.isNullOrBlank()) {
                val iTunesResult = searchITunesAlbumArt(artist, album)
                if (iTunesResult != null) {
                    coverArtCache[cacheKey] = iTunesResult
                    return@withContext AlbumArtResult.iTunesUrl(iTunesResult)
                }
            }
            
            // Generate fallback cover
            val fallbackBitmap = generateFallbackCover(artist, album)
            AlbumArtResult.FallbackGenerated(fallbackBitmap)
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting album art for $filePath")
            val fallbackBitmap = generateFallbackCover(artist, album)
            AlbumArtResult.FallbackGenerated(fallbackBitmap)
        }
    }
    
    /**
     * Extract embedded cover art from audio file's ID3 tags
     */
    private fun extractEmbeddedCoverArt(filePath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val embeddedPicture = retriever.embeddedPicture
            retriever.release()
            
            embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting embedded cover art")
            null
        }
    }
    
    /**
     * Search iTunes API for album art
     */
    private suspend fun searchITunesAlbumArt(artist: String, album: String): String? {
        return try {
            val searchTerm = "$artist $album"
            val response = iTunesApi.searchAlbum(searchTerm)
            
            if (response.resultCount > 0) {
                // Return high-res artwork URL from first result
                response.results.firstOrNull()?.getHighResArtworkUrl()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching iTunes for album art")
            null
        }
    }
    
    /**
     * Check if cover art download is allowed based on network and settings
     */
    private suspend fun shouldDownloadCoverArt(): Boolean {
        // Get WiFi-only setting
        val wifiOnly = preferencesRepository.albumArtWifiOnly.first { true }
        
        if (!wifiOnly) {
            return true // Download on any network
        }
        
        // Check if connected to WiFi
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Generate fallback cover art with colored background and text
     */
    private fun generateFallbackCover(artist: String?, album: String?): Bitmap {
        val size = 600
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Generate color from artist/album name
        val colorSeed = (artist ?: album ?: "Unknown").hashCode()
        val hue = (colorSeed % 360).toFloat()
        val backgroundColor = Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f))
        
        // Draw background
        canvas.drawColor(backgroundColor)
        
        // Draw text
        val paint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, Color.argb(128, 0, 0, 0))
        }
        
        val centerX = size / 2f
        var centerY = size / 2f
        
        // Draw artist name
        if (!artist.isNullOrBlank()) {
            paint.textSize = 48f
            val artistBounds = Rect()
            paint.getTextBounds(artist, 0, artist.length, artistBounds)
            
            if (album.isNullOrBlank()) {
                centerY = size / 2f + artistBounds.height() / 2f
            } else {
                centerY = size / 2f - 40f
            }
            
            canvas.drawText(artist, centerX, centerY, paint)
        }
        
        // Draw album name
        if (!album.isNullOrBlank()) {
            paint.textSize = 36f
            val albumBounds = Rect()
            paint.getTextBounds(album, 0, album.length, albumBounds)
            
            centerY = if (artist.isNullOrBlank()) {
                size / 2f + albumBounds.height() / 2f
            } else {
                size / 2f + 40f + albumBounds.height()
            }
            
            canvas.drawText(album, centerX, centerY, paint)
        }
        
        // Draw default text if both are null
        if (artist.isNullOrBlank() && album.isNullOrBlank()) {
            paint.textSize = 72f
            canvas.drawText("♪", centerX, size / 2f + 30f, paint)
        }
        
        return bitmap
    }
    
    /**
     * Save cover art to audio file
     */
    suspend fun saveCoverArtToFile(filePath: String, coverArtBitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Save bitmap to cache directory
                val cacheFile = File(context.cacheDir, "cover_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { out ->
                    coverArtBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                
                // TODO: Embed cover art into audio file using ID3 tags
                // This requires a library like JAudioTagger or similar
                // For now, just return success for saving to cache
                
                Timber.d("Saved cover art to cache: ${cacheFile.absolutePath}")
                true
            } catch (e: Exception) {
                Timber.e(e, "Error saving cover art to file")
                false
            }
        }
    }
    
    /**
     * Load and cache cover art from URL using Glide
     */
    suspend fun downloadAndCacheCoverArt(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .submit()
                    .get()
            } catch (e: Exception) {
                Timber.e(e, "Error downloading cover art from $url")
                null
            }
        }
    }
    
    private fun getCacheKey(artist: String?, album: String?): String {
        return "${artist ?: "unknown"}_${album ?: "unknown"}".lowercase()
    }
}

/**
 * Result types for album art retrieval
 */
sealed class AlbumArtResult {
    data class EmbeddedArt(val bitmap: Bitmap) : AlbumArtResult()
    data class iTunesUrl(val url: String) : AlbumArtResult()
    data class CachedUrl(val url: String) : AlbumArtResult()
    data class FallbackGenerated(val bitmap: Bitmap) : AlbumArtResult()
}
