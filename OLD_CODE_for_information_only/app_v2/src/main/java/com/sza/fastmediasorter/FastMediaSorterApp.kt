package com.sza.fastmediasorter

import android.app.Application
import android.content.Context
import android.content.ComponentCallbacks2
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.core.init.AppStartupInitializer
import com.sza.fastmediasorter.core.logging.LoggingHelper
import com.sza.fastmediasorter.core.util.CacheStatusHelper
import com.sza.fastmediasorter.core.util.LocaleHelper
import com.sza.fastmediasorter.worker.WorkManagerScheduler
import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.data.network.glide.NetworkFileDataFetcher
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FastMediaSorterApp : Application(), Configuration.Provider {

    companion object {
        // Static context for Glide ModelLoader factory (needed for Hilt EntryPoint access)
        lateinit var appContext: Context
            private set
    }
    
    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var playbackPositionRepository: com.sza.fastmediasorter.domain.repository.PlaybackPositionRepository
    
    @Inject
    lateinit var thumbnailCacheRepository: com.sza.fastmediasorter.domain.repository.ThumbnailCacheRepository
    
    @Inject
    lateinit var resourceRepository: com.sza.fastmediasorter.domain.repository.ResourceRepository
    
    @Inject
    lateinit var unifiedCache: com.sza.fastmediasorter.core.cache.UnifiedFileCache
    
    // Application-scoped coroutine for background initialization
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize static context for Glide ModelLoader
        appContext = applicationContext
        
        // TEMPORARILY DISABLED: PDFBox initialization (BouncyCastle conflict)
        // TODO: Re-enable when Android 9 compatible alternative found
        // try {
        //     com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        //     Timber.d("PDFBox initialized successfully")
        // } catch (e: Exception) {
        //     Timber.e(e, "Failed to initialize PDFBox")
        // }
        
        // Apply saved locale (fast)
        // LocaleHelper.applyLocale(this) // Temporarily disabled for debugging
        // Note: logging initialized early in attachBaseContext to capture startup crashes
        
        // Clear failed video thumbnail cache on app start
        NetworkFileDataFetcher.clearFailedVideoCache()
        
        // Clear translation cache on app start
        com.sza.fastmediasorter.core.cache.TranslationCacheManager.clearAll()
        
        // Log Glide disk cache status at startup
        CacheStatusHelper.logGlideDiskCacheStatus(this)
        
        Timber.d("FastMediaSorter v2 initialized with locale: ${LocaleHelper.getLanguage(this)}")
        
        // Initialize all background tasks
        val startupInitializer = AppStartupInitializer(
            context = applicationContext,
            settingsRepository = settingsRepository,
            resourceRepository = resourceRepository,
            playbackPositionRepository = playbackPositionRepository,
            thumbnailCacheRepository = thumbnailCacheRepository,
            applicationScope = applicationScope
        )
        startupInitializer.initialize()
        
        // Trash cleanup now handled synchronously in BrowseViewModel (on resource open/close)
        // WorkManager periodic cleanup disabled - unnecessary with sync cleanup
        // Left for potential future background tasks (e.g., network resource sync)
        
        // Defer WorkManager scheduling to background with delay to avoid blocking app startup
        // WorkManager initialization is expensive (~100-200ms), defer until after UI is rendered
        // applicationScope.launch(Dispatchers.IO) {
        //     try {
        //         kotlinx.coroutines.delay(500) // Wait for UI to render first
        //         workManagerScheduler.scheduleTrashCleanup()
        //         Timber.d("Background initialization: WorkManager scheduled")
        //     } catch (e: Exception) {
        //         Timber.e(e, "Failed to schedule WorkManager in background")
        //     }
        // }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        // super.attachBaseContext(LocaleHelper.applyLocale(base)) // Temporarily disabled for debugging
        super.attachBaseContext(base)
        // Initialize logging as early as possible so file logging exists even if app
        // crashes during or before onCreate(). Fail-safe: don't throw if logging fails.
        try {
            LoggingHelper.initialize(base)
        } catch (e: Exception) {
            android.util.Log.e("FastMediaSorterApp", "Early logging init failed", e)
        }
    }
    
    /**
     * Handle system memory pressure events.
     * Clear image cache ONLY on critical memory pressure to preserve thumbnails.
     * Large cache is intentional for Browse workflow - don't clear on background.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is running but system is CRITICALLY low on memory - clear memory cache
                Timber.w("CRITICAL memory: level=$level, clearing Glide memory cache")
                Glide.get(this).clearMemory()
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // System is about to kill background processes
                // Clear ONLY memory cache, preserve disk cache for next launch!
                // Disk cache should persist for fast thumbnail loading on restart
                Timber.w("System killing processes: level=$level, clearing Glide MEMORY cache only (preserving disk)")
                Glide.get(this).clearMemory()
                // DO NOT clear disk cache here - it should persist between app launches
                // Disk cache is cleared: manually in settings, on file delete/move/rename, or by FIFO eviction
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                // Low priority memory pressure - DO NOT clear cache, let it persist
                // Large cache is intentional for instant thumbnail reloading in Browse
                Timber.d("Low priority memory pressure: level=$level, preserving cache")
            }
        }
    }
}
