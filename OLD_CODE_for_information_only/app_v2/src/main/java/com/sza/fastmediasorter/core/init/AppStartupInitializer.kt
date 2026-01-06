package com.sza.fastmediasorter.core.init

import android.content.Context
import com.sza.fastmediasorter.BuildConfig
import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.domain.repository.PlaybackPositionRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.repository.ThumbnailCacheRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles all background initialization tasks for the application.
 * Extracted from Application class for better testability and separation of concerns.
 */
class AppStartupInitializer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val resourceRepository: ResourceRepository,
    private val playbackPositionRepository: PlaybackPositionRepository,
    private val thumbnailCacheRepository: ThumbnailCacheRepository,
    private val applicationScope: CoroutineScope
) {
    
    /**
     * Initialize all background tasks.
     * Should be called from Application.onCreate() after dependency injection.
     */
    fun initialize() {
        syncCacheSizeToSharedPreferences()
        fixCloudResourcesWritableFlag()
        cleanupPlaybackPositions()
        cleanupOldThumbnails()
        initializeConnectionThrottleManager()
    }
    
    /**
     * Sync cache size setting to SharedPreferences for Glide initialization.
     * Also logs all settings in DEBUG builds.
     */
    private fun syncCacheSizeToSharedPreferences() {
        applicationScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                context.getSharedPreferences("glide_config", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("cache_size_mb", settings.cacheSizeMb)
                    .apply()
                Timber.d("Synced cache size to SharedPreferences: ${settings.cacheSizeMb}MB")
                
                // Log all settings for debugging (DEBUG builds only)
                if (BuildConfig.DEBUG) {
                    logAllSettings(settings)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync cache size to SharedPreferences")
            }
        }
    }
    
    /**
     * Log all application settings for debugging in DEBUG builds.
     */
    private fun logAllSettings(settings: com.sza.fastmediasorter.domain.model.AppSettings) {
        Timber.i("========== APP SETTINGS AT STARTUP (DEBUG) ==========")
        Timber.i("UI STATE: isResourceGridMode=${settings.isResourceGridMode}")
        Timber.i("GENERAL: language=${settings.language}, preventSleep=${settings.preventSleep}, showSmallControls=${settings.showSmallControls}")
        Timber.i("CREDENTIALS: defaultUser=${settings.defaultUser.ifEmpty { "(empty)" }}, defaultPassword=${"*".repeat(settings.defaultPassword.length)}")
        Timber.i("NETWORK: parallelism=${settings.networkParallelism}, cacheSizeMb=${settings.cacheSizeMb}, cacheUserModified=${settings.isCacheSizeUserModified}")
        Timber.i("BACKGROUND SYNC: enabled=${settings.enableBackgroundSync}, intervalHours=${settings.backgroundSyncIntervalHours}")
        
        Timber.i("MEDIA IMAGES: enabled=${settings.supportImages}, sizeMin=${settings.imageSizeMin}B, sizeMax=${settings.imageSizeMax}B, loadFullSize=${settings.loadFullSizeImages}")
        Timber.i("MEDIA GIFS: enabled=${settings.supportGifs}")
        Timber.i("MEDIA VIDEOS: enabled=${settings.supportVideos}, sizeMin=${settings.videoSizeMin}B, sizeMax=${settings.videoSizeMax}B, showThumbnails=${settings.showVideoThumbnails}")
        Timber.i("MEDIA AUDIO: enabled=${settings.supportAudio}, sizeMin=${settings.audioSizeMin}B, sizeMax=${settings.audioSizeMax}B")
        Timber.i("AUDIO COVERS: searchOnline=${settings.searchAudioCoversOnline}, onlyOnWifi=${settings.searchAudioCoversOnlyOnWifi}")
        Timber.i("MEDIA TEXT: enabled=${settings.supportText}, sizeMax=${settings.textSizeMax}B")
        Timber.i("MEDIA PDF: enabled=${settings.supportPdf}, showThumbnails=${settings.showPdfThumbnails}")
        
        Timber.i("TRANSLATION: enabled=${settings.enableTranslation}, source=${settings.translationSourceLanguage}, target=${settings.translationTargetLanguage}, lensStyle=${settings.translationLensStyle}")
        
        Timber.i("PLAYBACK: sortMode=${settings.defaultSortMode}, slideshowInterval=${settings.slideshowInterval}s, playToEnd=${settings.playToEndInSlideshow}")
        Timber.i("PERMISSIONS: allowRename=${settings.allowRename}, allowDelete=${settings.allowDelete}")
        Timber.i("CONFIRMATIONS: safeMode=${settings.enableSafeMode}, confirmDelete=${settings.confirmDelete}, confirmMove=${settings.confirmMove}")
        Timber.i("VIEW MODES: gridMode=${settings.defaultGridMode}, iconSize=${settings.defaultIconSize}dp, showCommandPanel=${settings.defaultShowCommandPanel}")
        Timber.i("UI HINTS: showPlayerHint=${settings.showPlayerHintOnFirstRun}, alwaysShowTouchZones=${settings.alwaysShowTouchZonesOverlay}, detailedErrors=${settings.showDetailedErrors}")
        
        Timber.i("DESTINATIONS: copying=${settings.enableCopying}, goToNextAfterCopy=${settings.goToNextAfterCopy}, overwriteOnCopy=${settings.overwriteOnCopy}")
        Timber.i("DESTINATIONS: moving=${settings.enableMoving}, overwriteOnMove=${settings.overwriteOnMove}, undo=${settings.enableUndo}, maxRecipients=${settings.maxRecipients}")
        Timber.i("FAVORITES: enabled=${settings.enableFavorites}")
        
        Timber.i("PANEL STATE: copyPanelCollapsed=${settings.copyPanelCollapsed}, movePanelCollapsed=${settings.movePanelCollapsed}")
        Timber.i("LAST USED: resourceId=${settings.lastUsedResourceId}")
        Timber.i("====================================================")
    }
    
    /**
     * Fix cloud resources: set isWritable = true for existing CLOUD resources.
     * Migration fix for older app versions.
     */
    private fun fixCloudResourcesWritableFlag() {
        applicationScope.launch {
            try {
                val resources = resourceRepository.getAllResources().first()
                val cloudResources = resources.filter { 
                    it.type == com.sza.fastmediasorter.domain.model.ResourceType.CLOUD && !it.isWritable 
                }
                if (cloudResources.isNotEmpty()) {
                    cloudResources.forEach { resource ->
                        resourceRepository.updateResource(resource.copy(isWritable = true))
                    }
                    Timber.d("Fixed isWritable flag for ${cloudResources.size} cloud resources")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fix cloud resources isWritable flag")
            }
        }
    }
    
    /**
     * Cleanup playback positions by count limit on app start.
     */
    private fun cleanupPlaybackPositions() {
        applicationScope.launch {
            try {
                playbackPositionRepository.cleanupOldPositions()
                Timber.d("Checked playback positions count limit")
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup playback positions")
            }
        }
    }
    
    /**
     * Cleanup old thumbnail cache (>30 days) on app start.
     */
    private fun cleanupOldThumbnails() {
        applicationScope.launch {
            try {
                val deletedCount = thumbnailCacheRepository.cleanupOldThumbnails(30)
                Timber.d("Cleaned up $deletedCount old thumbnail cache entries")
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup old thumbnail cache")
            }
        }
    }
    
    /**
     * Initialize ConnectionThrottleManager with saved settings.
     * Observes settings changes and updates network limit dynamically.
     */
    private fun initializeConnectionThrottleManager() {
        applicationScope.launch {
            try {
                settingsRepository.getSettings()
                    .map { it.networkParallelism }
                    .distinctUntilChanged()
                    .collect { limit ->
                        ConnectionThrottleManager.setUserNetworkLimit(limit)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize ConnectionThrottleManager settings")
            }
        }
    }
}
