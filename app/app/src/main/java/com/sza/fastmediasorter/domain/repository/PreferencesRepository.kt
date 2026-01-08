package com.sza.fastmediasorter.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app preferences/settings.
 * Provides access to user preferences with reactive updates.
 */
interface PreferencesRepository {

    // ==================== General Settings ====================

    /**
     * Get the current language code (en, ru, uk).
     */
    val language: Flow<String>
    suspend fun setLanguage(langCode: String)

    /**
     * Get the current theme (light, dark, system).
     */
    val theme: Flow<String>
    suspend fun setTheme(theme: String)

    /**
     * Get the default display mode (grid, list, auto).
     */
    val displayMode: Flow<String>
    suspend fun setDisplayMode(mode: String)

    /**
     * Grid columns for browse view (1-10).
     */
    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    /**
     * Whether to show hidden files (files starting with .).
     */
    val showHiddenFiles: Flow<Boolean>
    suspend fun setShowHiddenFiles(show: Boolean)

    /**
     * Whether to confirm before deleting files.
     */
    val confirmDelete: Flow<Boolean>
    suspend fun setConfirmDelete(confirm: Boolean)

    /**
     * Whether to confirm before moving files.
     */
    val confirmMove: Flow<Boolean>
    suspend fun setConfirmMove(confirm: Boolean)

    /**
     * Whether to prevent screen sleep during media playback.
     */
    val preventSleepDuringPlayback: Flow<Boolean>
    suspend fun setPreventSleepDuringPlayback(prevent: Boolean)

    /**
     * Whether favorites feature is enabled.
     */
    val enableFavorites: Flow<Boolean>
    suspend fun setEnableFavorites(enable: Boolean)

    /**
     * Whether to work with all file types (not just media).
     */
    val workWithAllFiles: Flow<Boolean>
    suspend fun setWorkWithAllFiles(enabled: Boolean)

    /**
     * Date format for file dates (short, medium, long).
     */
    val dateFormat: Flow<String>
    suspend fun setDateFormat(format: String)

    /**
     * File name display mode (full, truncated).
     */
    val fileNameDisplay: Flow<String>
    suspend fun setFileNameDisplay(mode: String)

    // ==================== Playback Settings ====================

    /**
     * Slideshow interval in seconds.
     */
    val slideshowInterval: Flow<Int>
    suspend fun setSlideshowInterval(seconds: Int)

    /**
     * Whether to play slideshow in random order.
     */
    val randomOrder: Flow<Boolean>
    suspend fun setRandomOrder(random: Boolean)

    /**
     * Whether to loop the slideshow.
     */
    val loopSlideshow: Flow<Boolean>
    suspend fun setLoopSlideshow(loop: Boolean)

    /**
     * Whether touch zones are enabled for navigation.
     */
    val enableTouchZones: Flow<Boolean>
    suspend fun setEnableTouchZones(enable: Boolean)

    /**
     * Whether to show touch zone overlay indicators.
     */
    val showZoneOverlay: Flow<Boolean>
    suspend fun setShowZoneOverlay(show: Boolean)

    /**
     * Touch zone sensitivity in milliseconds (long-press delay).
     * Range: 100-500ms, default: 200ms.
     */
    val zoneSensitivityMs: Flow<Int>
    suspend fun setZoneSensitivityMs(ms: Int)

    /**
     * Whether to resume video from last position.
     */
    val resumeFromLastPosition: Flow<Boolean>
    suspend fun setResumeFromLastPosition(resume: Boolean)

    /**
     * Whether to auto-play videos when opened.
     */
    val autoPlayVideos: Flow<Boolean>
    suspend fun setAutoPlayVideos(autoPlay: Boolean)

    /**
     * Command panel position (top, bottom).
     */
    val panelPosition: Flow<String>
    suspend fun setPanelPosition(position: String)

    /**
     * Auto-hide delay for controls in seconds (0 = never hide).
     */
    val autoHideDelay: Flow<Int>
    suspend fun setAutoHideDelay(seconds: Int)

    /**
     * Video repeat mode (none, one, all).
     */
    val videoRepeatMode: Flow<String>
    suspend fun setVideoRepeatMode(mode: String)

    /**
     * Default video playback speed (0.25x - 2.0x).
     */
    val defaultVideoSpeed: Flow<Float>
    suspend fun setDefaultVideoSpeed(speed: Float)

    /**
     * Whether to skip silence in audio/video playback.
     */
    val skipSilence: Flow<Boolean>
    suspend fun setSkipSilence(enabled: Boolean)

    /**
     * Icon size for command panel buttons (24-128 dp).
     */
    val iconSize: Flow<Int>
    suspend fun setIconSize(size: Int)

    /**
     * Whether to hide grid action buttons overlay.
     */
    val hideGridActionButtons: Flow<Boolean>
    suspend fun setHideGridActionButtons(hidden: Boolean)

    // ==================== Image Settings ====================

    /**
     * Whether image files are supported.
     */
    val supportImages: Flow<Boolean>
    suspend fun setSupportImages(enabled: Boolean)

    /**
     * Thumbnail quality (low, medium, high).
     */
    val thumbnailQuality: Flow<String>
    suspend fun setThumbnailQuality(quality: String)

    /**
     * Whether to auto-rotate images based on EXIF.
     */
    val autoRotateImages: Flow<Boolean>
    suspend fun setAutoRotateImages(enabled: Boolean)

    /**
     * Whether to show EXIF data in file info.
     */
    val showExifData: Flow<Boolean>
    suspend fun setShowExifData(enabled: Boolean)

    /**
     * JPEG compression quality (1-100).
     */
    val jpegQuality: Flow<Int>
    suspend fun setJpegQuality(quality: Int)

    /**
     * Whether to load full-size images in player.
     */
    val loadFullSizeImages: Flow<Boolean>
    suspend fun setLoadFullSizeImages(enabled: Boolean)

    // ==================== Video Settings ====================

    /**
     * Whether video files are supported.
     */
    val supportVideo: Flow<Boolean>
    suspend fun setSupportVideo(enabled: Boolean)

    /**
     * Video quality preference (auto, low, medium, high, best).
     */
    val videoQuality: Flow<String>
    suspend fun setVideoQuality(quality: String)

    /**
     * Whether to use hardware acceleration for video.
     */
    val hardwareAcceleration: Flow<Boolean>
    suspend fun setHardwareAcceleration(enabled: Boolean)

    /**
     * Seek increment in seconds for forward/rewind.
     */
    val seekIncrement: Flow<Int>
    suspend fun setSeekIncrement(seconds: Int)

    /**
     * Video preview thumbnail duration in grid (seconds).
     */
    val previewDuration: Flow<Int>
    suspend fun setPreviewDuration(seconds: Int)

    /**
     * Whether to show video thumbnails in browse grid.
     */
    val showVideoThumbnails: Flow<Boolean>
    suspend fun setShowVideoThumbnails(enabled: Boolean)

    // ==================== Audio Settings ====================

    /**
     * Whether audio files are supported.
     */
    val supportAudio: Flow<Boolean>
    suspend fun setSupportAudio(enabled: Boolean)

    /**
     * Waveform display style (none, bars, line).
     */
    val waveformStyle: Flow<String>
    suspend fun setWaveformStyle(style: String)

    /**
     * Waveform color in hex (#RRGGBB).
     */
    val waveformColor: Flow<String>
    suspend fun setWaveformColor(color: String)

    /**
     * Whether audio can play in background.
     */
    val backgroundPlayback: Flow<Boolean>
    suspend fun setBackgroundPlayback(enabled: Boolean)

    /**
     * Audio focus handling (duck, pause, ignore).
     */
    val audioFocusHandling: Flow<String>
    suspend fun setAudioFocusHandling(mode: String)

    /**
     * Whether to search for album cover art online.
     */
    val searchAlbumCovers: Flow<Boolean>
    suspend fun setSearchAlbumCovers(enabled: Boolean)

    // ==================== Document Settings ====================

    /**
     * Whether text files are supported.
     */
    val supportText: Flow<Boolean>
    suspend fun setSupportText(enabled: Boolean)

    /**
     * Whether PDF files are supported.
     */
    val supportPdf: Flow<Boolean>
    suspend fun setSupportPdf(enabled: Boolean)

    /**
     * Whether EPUB files are supported.
     */
    val supportEpub: Flow<Boolean>
    suspend fun setSupportEpub(enabled: Boolean)

    /**
     * PDF page cache size (number of pages to keep in memory).
     */
    val pdfPageCache: Flow<Int>
    suspend fun setPdfPageCache(pages: Int)

    /**
     * PDF render quality (low, medium, high).
     */
    val pdfRenderQuality: Flow<String>
    suspend fun setPdfRenderQuality(quality: String)

    /**
     * Text file encoding (auto, utf-8, utf-16, cp1251, iso-8859-1).
     */
    val textEncoding: Flow<String>
    suspend fun setTextEncoding(encoding: String)

    /**
     * EPUB font family (serif, sans, mono).
     */
    val epubFontFamily: Flow<String>
    suspend fun setEpubFontFamily(family: String)

    /**
     * EPUB font size (8-48).
     */
    val epubFontSize: Flow<Int>
    suspend fun setEpubFontSize(size: Int)

    // ==================== Other Media Settings ====================

    /**
     * Whether GIF files are supported.
     */
    val supportGif: Flow<Boolean>
    suspend fun setSupportGif(enabled: Boolean)

    /**
     * GIF frame rate limit (0 = no limit, 1-60 fps).
     */
    val gifFrameRateLimit: Flow<Int>
    suspend fun setGifFrameRateLimit(fps: Int)

    // ==================== Destinations Settings ====================

    /**
     * Whether copying to destinations is enabled.
     */
    val enableCopying: Flow<Boolean>
    suspend fun setEnableCopying(enabled: Boolean)

    /**
     * Whether moving to destinations is enabled.
     */
    val enableMoving: Flow<Boolean>
    suspend fun setEnableMoving(enabled: Boolean)

    /**
     * Whether to go to next file after copy/move.
     */
    val goToNextAfterCopy: Flow<Boolean>
    suspend fun setGoToNextAfterCopy(enabled: Boolean)

    /**
     * Overwrite mode (ask, skip, replace).
     */
    val overwriteMode: Flow<String>
    suspend fun setOverwriteMode(mode: String)

    /**
     * Maximum number of destinations (1-30).
     */
    val maxDestinations: Flow<Int>
    suspend fun setMaxDestinations(count: Int)

    // ==================== Network Settings ====================

    /**
     * Connection timeout in seconds.
     */
    val connectionTimeout: Flow<Int>
    suspend fun setConnectionTimeout(seconds: Int)

    /**
     * Number of retry attempts for network operations.
     */
    val retryAttempts: Flow<Int>
    suspend fun setRetryAttempts(count: Int)

    /**
     * Network parallelism (concurrent connections, 1-32).
     */
    val networkParallelism: Flow<Int>
    suspend fun setNetworkParallelism(threads: Int)

    /**
     * Whether to operate in offline mode.
     */
    val offlineMode: Flow<Boolean>
    suspend fun setOfflineMode(enabled: Boolean)

    // ==================== Onboarding ====================

    /**
     * Whether the user has completed onboarding.
     */
    val hasCompletedOnboarding: Flow<Boolean>
    suspend fun setHasCompletedOnboarding(completed: Boolean)
}
