package com.sza.fastmediasorter.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementation of PreferencesRepository using DataStore.
 * Provides persistent storage for app settings with reactive updates.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        // General Settings
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val CONFIRM_MOVE = booleanPreferencesKey("confirm_move")
        val PREVENT_SLEEP = booleanPreferencesKey("prevent_sleep_during_playback")
        val ENABLE_FAVORITES = booleanPreferencesKey("enable_favorites")
        val WORK_WITH_ALL_FILES = booleanPreferencesKey("work_with_all_files")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val FILE_NAME_DISPLAY = stringPreferencesKey("file_name_display")
        val DEFAULT_USERNAME = stringPreferencesKey("default_username")
        val DEFAULT_PASSWORD = stringPreferencesKey("default_password")

        // Playback Settings
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
        val RANDOM_ORDER = booleanPreferencesKey("random_order")
        val LOOP_SLIDESHOW = booleanPreferencesKey("loop_slideshow")
        val ENABLE_TOUCH_ZONES = booleanPreferencesKey("enable_touch_zones")
        val SHOW_ZONE_OVERLAY = booleanPreferencesKey("show_zone_overlay")
        val ZONE_SENSITIVITY_MS = intPreferencesKey("zone_sensitivity_ms")
        val RESUME_FROM_LAST_POSITION = booleanPreferencesKey("resume_from_last_position")
        val AUTO_PLAY_VIDEOS = booleanPreferencesKey("auto_play_videos")
        val PANEL_POSITION = stringPreferencesKey("panel_position")
        val AUTO_HIDE_DELAY = intPreferencesKey("auto_hide_delay")
        val VIDEO_REPEAT_MODE = stringPreferencesKey("video_repeat_mode")
        val DEFAULT_VIDEO_SPEED = floatPreferencesKey("default_video_speed")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val ICON_SIZE = intPreferencesKey("icon_size")
        val HIDE_GRID_ACTION_BUTTONS = booleanPreferencesKey("hide_grid_action_buttons")

        // Image Settings
        val SUPPORT_IMAGES = booleanPreferencesKey("support_images")
        val THUMBNAIL_QUALITY = stringPreferencesKey("thumbnail_quality")
        val AUTO_ROTATE_IMAGES = booleanPreferencesKey("auto_rotate_images")
        val SHOW_EXIF_DATA = booleanPreferencesKey("show_exif_data")
        val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        val LOAD_FULL_SIZE_IMAGES = booleanPreferencesKey("load_full_size_images")

        // Video Settings
        val SUPPORT_VIDEO = booleanPreferencesKey("support_video")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val SEEK_INCREMENT = intPreferencesKey("seek_increment")
        val PREVIEW_DURATION = intPreferencesKey("preview_duration")
        val SHOW_VIDEO_THUMBNAILS = booleanPreferencesKey("show_video_thumbnails")

        // Audio Settings
        val SUPPORT_AUDIO = booleanPreferencesKey("support_audio")
        val WAVEFORM_STYLE = stringPreferencesKey("waveform_style")
        val WAVEFORM_COLOR = stringPreferencesKey("waveform_color")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val AUDIO_FOCUS_HANDLING = stringPreferencesKey("audio_focus_handling")
        val SEARCH_ALBUM_COVERS = booleanPreferencesKey("search_album_covers")

        // Document Settings
        val SUPPORT_TEXT = booleanPreferencesKey("support_text")
        val SUPPORT_PDF = booleanPreferencesKey("support_pdf")
        val SUPPORT_EPUB = booleanPreferencesKey("support_epub")
        val PDF_PAGE_CACHE = intPreferencesKey("pdf_page_cache")
        val PDF_RENDER_QUALITY = stringPreferencesKey("pdf_render_quality")
        val TEXT_ENCODING = stringPreferencesKey("text_encoding")
        val EPUB_FONT_FAMILY = stringPreferencesKey("epub_font_family")
        val EPUB_FONT_SIZE = intPreferencesKey("epub_font_size")

        // Other Media Settings
        val SUPPORT_GIF = booleanPreferencesKey("support_gif")
        val GIF_FRAME_RATE_LIMIT = intPreferencesKey("gif_frame_rate_limit")

        // Destinations Settings
        val ENABLE_COPYING = booleanPreferencesKey("enable_copying")
        val ENABLE_MOVING = booleanPreferencesKey("enable_moving")
        val GO_TO_NEXT_AFTER_COPY = booleanPreferencesKey("go_to_next_after_copy")
        val OVERWRITE_MODE = stringPreferencesKey("overwrite_mode")
        val MAX_DESTINATIONS = intPreferencesKey("max_destinations")

        // Network Settings
        val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout")
        val RETRY_ATTEMPTS = intPreferencesKey("retry_attempts")
        val NETWORK_PARALLELISM = intPreferencesKey("network_parallelism")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")

        // Album Art Settings
        val ALBUM_ART_WIFI_ONLY = booleanPreferencesKey("album_art_wifi_only")
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")

        // Onboarding
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    // ==================== General Settings ====================

    override val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: "en"
    }

    override suspend fun setLanguage(langCode: String) {
        Timber.d("Setting language to: $langCode")
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = langCode
        }
    }

    override val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "system"
    }

    override suspend fun setTheme(theme: String) {
        Timber.d("Setting theme to: $theme")
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }

    override val displayMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISPLAY_MODE] ?: "grid"
    }

    override suspend fun setDisplayMode(mode: String) {
        Timber.d("Setting display mode to: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_MODE] = mode
        }
    }

    override val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.GRID_COLUMNS] ?: 3
    }

    override suspend fun setGridColumns(columns: Int) {
        Timber.d("Setting grid columns to: $columns")
        context.dataStore.edit { prefs ->
            prefs[Keys.GRID_COLUMNS] = columns.coerceIn(1, 10)
        }
    }

    override val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_HIDDEN_FILES] ?: false
    }

    override suspend fun setShowHiddenFiles(show: Boolean) {
        Timber.d("Setting show hidden files: $show")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_HIDDEN_FILES] = show
        }
    }

    override val confirmDelete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_DELETE] ?: true
    }

    override suspend fun setConfirmDelete(confirm: Boolean) {
        Timber.d("Setting confirm delete: $confirm")
        context.dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_DELETE] = confirm
        }
    }

    override val confirmMove: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_MOVE] ?: false
    }

    override suspend fun setConfirmMove(confirm: Boolean) {
        Timber.d("Setting confirm move: $confirm")
        context.dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_MOVE] = confirm
        }
    }

    override val preventSleepDuringPlayback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREVENT_SLEEP] ?: true
    }

    override suspend fun setPreventSleepDuringPlayback(prevent: Boolean) {
        Timber.d("Setting prevent sleep during playback: $prevent")
        context.dataStore.edit { prefs ->
            prefs[Keys.PREVENT_SLEEP] = prevent
        }
    }

    override val enableFavorites: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_FAVORITES] ?: false
    }

    override suspend fun setEnableFavorites(enable: Boolean) {
        Timber.d("Setting enable favorites: $enable")
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_FAVORITES] = enable
        }
    }

    override val workWithAllFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORK_WITH_ALL_FILES] ?: false
    }

    override suspend fun setWorkWithAllFiles(enabled: Boolean) {
        Timber.d("Setting work with all files: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.WORK_WITH_ALL_FILES] = enabled
        }
    }

    override val dateFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DATE_FORMAT] ?: "medium"
    }

    override suspend fun setDateFormat(format: String) {
        Timber.d("Setting date format: $format")
        context.dataStore.edit { prefs ->
            prefs[Keys.DATE_FORMAT] = format
        }
    }

    override val fileNameDisplay: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.FILE_NAME_DISPLAY] ?: "truncated"
    }

    override suspend fun setFileNameDisplay(mode: String) {
        Timber.d("Setting file name display: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.FILE_NAME_DISPLAY] = mode
        }
    }

    override val defaultUsername: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_USERNAME] ?: ""
    }

    override suspend fun setDefaultUsername(username: String) {
        Timber.d("Setting default username: $username")
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_USERNAME] = username
        }
    }

    override val defaultPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_PASSWORD] ?: ""
    }

    override suspend fun setDefaultPassword(password: String) {
        Timber.d("Setting default password: ***")
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PASSWORD] = password
        }
    }

    // ==================== Playback Settings ====================

    override val slideshowInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLIDESHOW_INTERVAL] ?: 5
    }

    override suspend fun setSlideshowInterval(seconds: Int) {
        Timber.d("Setting slideshow interval: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.SLIDESHOW_INTERVAL] = seconds
        }
    }

    override val randomOrder: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RANDOM_ORDER] ?: false
    }

    override suspend fun setRandomOrder(random: Boolean) {
        Timber.d("Setting random order: $random")
        context.dataStore.edit { prefs ->
            prefs[Keys.RANDOM_ORDER] = random
        }
    }

    override val loopSlideshow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOOP_SLIDESHOW] ?: true
    }

    override suspend fun setLoopSlideshow(loop: Boolean) {
        Timber.d("Setting loop slideshow: $loop")
        context.dataStore.edit { prefs ->
            prefs[Keys.LOOP_SLIDESHOW] = loop
        }
    }

    override val enableTouchZones: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_TOUCH_ZONES] ?: true
    }

    override suspend fun setEnableTouchZones(enable: Boolean) {
        Timber.d("Setting enable touch zones: $enable")
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_TOUCH_ZONES] = enable
        }
    }

    override val showZoneOverlay: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_ZONE_OVERLAY] ?: false
    }

    override suspend fun setShowZoneOverlay(show: Boolean) {
        Timber.d("Setting show zone overlay: $show")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_ZONE_OVERLAY] = show
        }
    }

    override val zoneSensitivityMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ZONE_SENSITIVITY_MS] ?: 200
    }

    override suspend fun setZoneSensitivityMs(ms: Int) {
        Timber.d("Setting zone sensitivity: $ms ms")
        context.dataStore.edit { prefs ->
            prefs[Keys.ZONE_SENSITIVITY_MS] = ms.coerceIn(100, 500)
        }
    }

    override val resumeFromLastPosition: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RESUME_FROM_LAST_POSITION] ?: true
    }

    override suspend fun setResumeFromLastPosition(resume: Boolean) {
        Timber.d("Setting resume from last position: $resume")
        context.dataStore.edit { prefs ->
            prefs[Keys.RESUME_FROM_LAST_POSITION] = resume
        }
    }

    override val autoPlayVideos: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PLAY_VIDEOS] ?: false
    }

    override suspend fun setAutoPlayVideos(autoPlay: Boolean) {
        Timber.d("Setting auto play videos: $autoPlay")
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY_VIDEOS] = autoPlay
        }
    }

    override val panelPosition: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.PANEL_POSITION] ?: "bottom"
    }

    override suspend fun setPanelPosition(position: String) {
        Timber.d("Setting panel position: $position")
        context.dataStore.edit { prefs ->
            prefs[Keys.PANEL_POSITION] = position
        }
    }

    override val autoHideDelay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_HIDE_DELAY] ?: 3
    }

    override suspend fun setAutoHideDelay(seconds: Int) {
        Timber.d("Setting auto hide delay: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_HIDE_DELAY] = seconds.coerceIn(0, 30)
        }
    }

    override val videoRepeatMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIDEO_REPEAT_MODE] ?: "none"
    }

    override suspend fun setVideoRepeatMode(mode: String) {
        Timber.d("Setting video repeat mode: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.VIDEO_REPEAT_MODE] = mode
        }
    }

    override val defaultVideoSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_VIDEO_SPEED] ?: 1.0f
    }

    override suspend fun setDefaultVideoSpeed(speed: Float) {
        Timber.d("Setting default video speed: $speed")
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_VIDEO_SPEED] = speed.coerceIn(0.25f, 2.0f)
        }
    }

    override val skipSilence: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SKIP_SILENCE] ?: false
    }

    override suspend fun setSkipSilence(enabled: Boolean) {
        Timber.d("Setting skip silence: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SKIP_SILENCE] = enabled
        }
    }

    override val iconSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ICON_SIZE] ?: 48
    }

    override suspend fun setIconSize(size: Int) {
        Timber.d("Setting icon size: $size dp")
        context.dataStore.edit { prefs ->
            prefs[Keys.ICON_SIZE] = size.coerceIn(24, 128)
        }
    }

    override val hideGridActionButtons: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIDE_GRID_ACTION_BUTTONS] ?: false
    }

    override suspend fun setHideGridActionButtons(hidden: Boolean) {
        Timber.d("Setting hide grid action buttons: $hidden")
        context.dataStore.edit { prefs ->
            prefs[Keys.HIDE_GRID_ACTION_BUTTONS] = hidden
        }
    }

    // ==================== Image Settings ====================

    override val supportImages: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_IMAGES] ?: true
    }

    override suspend fun setSupportImages(enabled: Boolean) {
        Timber.d("Setting support images: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_IMAGES] = enabled
        }
    }

    override val thumbnailQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THUMBNAIL_QUALITY] ?: "medium"
    }

    override suspend fun setThumbnailQuality(quality: String) {
        Timber.d("Setting thumbnail quality: $quality")
        context.dataStore.edit { prefs ->
            prefs[Keys.THUMBNAIL_QUALITY] = quality
        }
    }

    override val autoRotateImages: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_ROTATE_IMAGES] ?: true
    }

    override suspend fun setAutoRotateImages(enabled: Boolean) {
        Timber.d("Setting auto rotate images: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_ROTATE_IMAGES] = enabled
        }
    }

    override val showExifData: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_EXIF_DATA] ?: true
    }

    override suspend fun setShowExifData(enabled: Boolean) {
        Timber.d("Setting show EXIF data: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_EXIF_DATA] = enabled
        }
    }

    override val jpegQuality: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.JPEG_QUALITY] ?: 85
    }

    override suspend fun setJpegQuality(quality: Int) {
        Timber.d("Setting JPEG quality: $quality")
        context.dataStore.edit { prefs ->
            prefs[Keys.JPEG_QUALITY] = quality.coerceIn(1, 100)
        }
    }

    override val loadFullSizeImages: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOAD_FULL_SIZE_IMAGES] ?: false
    }

    override suspend fun setLoadFullSizeImages(enabled: Boolean) {
        Timber.d("Setting load full size images: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.LOAD_FULL_SIZE_IMAGES] = enabled
        }
    }

    // ==================== Video Settings ====================

    override val supportVideo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_VIDEO] ?: true
    }

    override suspend fun setSupportVideo(enabled: Boolean) {
        Timber.d("Setting support video: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_VIDEO] = enabled
        }
    }

    override val videoQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIDEO_QUALITY] ?: "auto"
    }

    override suspend fun setVideoQuality(quality: String) {
        Timber.d("Setting video quality: $quality")
        context.dataStore.edit { prefs ->
            prefs[Keys.VIDEO_QUALITY] = quality
        }
    }

    override val hardwareAcceleration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HARDWARE_ACCELERATION] ?: true
    }

    override suspend fun setHardwareAcceleration(enabled: Boolean) {
        Timber.d("Setting hardware acceleration: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.HARDWARE_ACCELERATION] = enabled
        }
    }

    override val seekIncrement: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEEK_INCREMENT] ?: 10
    }

    override suspend fun setSeekIncrement(seconds: Int) {
        Timber.d("Setting seek increment: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.SEEK_INCREMENT] = seconds.coerceIn(5, 60)
        }
    }

    override val previewDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREVIEW_DURATION] ?: 3
    }

    override suspend fun setPreviewDuration(seconds: Int) {
        Timber.d("Setting preview duration: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.PREVIEW_DURATION] = seconds.coerceIn(0, 10)
        }
    }

    override val showVideoThumbnails: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_VIDEO_THUMBNAILS] ?: true
    }

    override suspend fun setShowVideoThumbnails(enabled: Boolean) {
        Timber.d("Setting show video thumbnails: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_VIDEO_THUMBNAILS] = enabled
        }
    }

    // ==================== Audio Settings ====================

    override val supportAudio: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_AUDIO] ?: true
    }

    override suspend fun setSupportAudio(enabled: Boolean) {
        Timber.d("Setting support audio: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_AUDIO] = enabled
        }
    }

    override val waveformStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WAVEFORM_STYLE] ?: "bars"
    }

    override suspend fun setWaveformStyle(style: String) {
        Timber.d("Setting waveform style: $style")
        context.dataStore.edit { prefs ->
            prefs[Keys.WAVEFORM_STYLE] = style
        }
    }

    override val waveformColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WAVEFORM_COLOR] ?: "#03DAC5"
    }

    override suspend fun setWaveformColor(color: String) {
        Timber.d("Setting waveform color: $color")
        context.dataStore.edit { prefs ->
            prefs[Keys.WAVEFORM_COLOR] = color
        }
    }

    override val backgroundPlayback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BACKGROUND_PLAYBACK] ?: true
    }

    override suspend fun setBackgroundPlayback(enabled: Boolean) {
        Timber.d("Setting background playback: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_PLAYBACK] = enabled
        }
    }

    override val audioFocusHandling: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUDIO_FOCUS_HANDLING] ?: "duck"
    }

    override suspend fun setAudioFocusHandling(mode: String) {
        Timber.d("Setting audio focus handling: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.AUDIO_FOCUS_HANDLING] = mode
        }
    }

    override val searchAlbumCovers: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_ALBUM_COVERS] ?: false
    }

    override suspend fun setSearchAlbumCovers(enabled: Boolean) {
        Timber.d("Setting search album covers: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SEARCH_ALBUM_COVERS] = enabled
        }
    }

    // ==================== Document Settings ====================

    override val supportText: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_TEXT] ?: true
    }

    override suspend fun setSupportText(enabled: Boolean) {
        Timber.d("Setting support text: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_TEXT] = enabled
        }
    }

    override val supportPdf: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_PDF] ?: true
    }

    override suspend fun setSupportPdf(enabled: Boolean) {
        Timber.d("Setting support PDF: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_PDF] = enabled
        }
    }

    override val supportEpub: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_EPUB] ?: true
    }

    override suspend fun setSupportEpub(enabled: Boolean) {
        Timber.d("Setting support EPUB: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_EPUB] = enabled
        }
    }

    override val pdfPageCache: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PDF_PAGE_CACHE] ?: 5
    }

    override suspend fun setPdfPageCache(pages: Int) {
        Timber.d("Setting PDF page cache: $pages pages")
        context.dataStore.edit { prefs ->
            prefs[Keys.PDF_PAGE_CACHE] = pages.coerceIn(1, 20)
        }
    }

    override val pdfRenderQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.PDF_RENDER_QUALITY] ?: "high"
    }

    override suspend fun setPdfRenderQuality(quality: String) {
        Timber.d("Setting PDF render quality: $quality")
        context.dataStore.edit { prefs ->
            prefs[Keys.PDF_RENDER_QUALITY] = quality
        }
    }

    override val textEncoding: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TEXT_ENCODING] ?: "auto"
    }

    override suspend fun setTextEncoding(encoding: String) {
        Timber.d("Setting text encoding: $encoding")
        context.dataStore.edit { prefs ->
            prefs[Keys.TEXT_ENCODING] = encoding
        }
    }

    override val epubFontFamily: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.EPUB_FONT_FAMILY] ?: "serif"
    }

    override suspend fun setEpubFontFamily(family: String) {
        Timber.d("Setting EPUB font family: $family")
        context.dataStore.edit { prefs ->
            prefs[Keys.EPUB_FONT_FAMILY] = family
        }
    }

    override val epubFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.EPUB_FONT_SIZE] ?: 16
    }

    override suspend fun setEpubFontSize(size: Int) {
        Timber.d("Setting EPUB font size: $size")
        context.dataStore.edit { prefs ->
            prefs[Keys.EPUB_FONT_SIZE] = size.coerceIn(8, 48)
        }
    }

    // ==================== Other Media Settings ====================

    override val supportGif: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUPPORT_GIF] ?: true
    }

    override suspend fun setSupportGif(enabled: Boolean) {
        Timber.d("Setting support GIF: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPPORT_GIF] = enabled
        }
    }

    override val gifFrameRateLimit: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.GIF_FRAME_RATE_LIMIT] ?: 0
    }

    override suspend fun setGifFrameRateLimit(fps: Int) {
        Timber.d("Setting GIF frame rate limit: $fps fps")
        context.dataStore.edit { prefs ->
            prefs[Keys.GIF_FRAME_RATE_LIMIT] = fps.coerceIn(0, 60)
        }
    }

    // ==================== Destinations Settings ====================

    override val enableCopying: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_COPYING] ?: true
    }

    override suspend fun setEnableCopying(enabled: Boolean) {
        Timber.d("Setting enable copying: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_COPYING] = enabled
        }
    }

    override val enableMoving: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_MOVING] ?: true
    }

    override suspend fun setEnableMoving(enabled: Boolean) {
        Timber.d("Setting enable moving: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_MOVING] = enabled
        }
    }

    override val goToNextAfterCopy: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.GO_TO_NEXT_AFTER_COPY] ?: true
    }

    override suspend fun setGoToNextAfterCopy(enabled: Boolean) {
        Timber.d("Setting go to next after copy: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.GO_TO_NEXT_AFTER_COPY] = enabled
        }
    }

    override val overwriteMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.OVERWRITE_MODE] ?: "ask"
    }

    override suspend fun setOverwriteMode(mode: String) {
        Timber.d("Setting overwrite mode: $mode")
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERWRITE_MODE] = mode
        }
    }

    override val maxDestinations: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_DESTINATIONS] ?: 15
    }

    override suspend fun setMaxDestinations(count: Int) {
        Timber.d("Setting max destinations: $count")
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_DESTINATIONS] = count.coerceIn(1, 30)
        }
    }

    // ==================== Network Settings ====================

    override val connectionTimeout: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONNECTION_TIMEOUT] ?: 30
    }

    override suspend fun setConnectionTimeout(seconds: Int) {
        Timber.d("Setting connection timeout: $seconds seconds")
        context.dataStore.edit { prefs ->
            prefs[Keys.CONNECTION_TIMEOUT] = seconds.coerceIn(5, 120)
        }
    }

    override val retryAttempts: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.RETRY_ATTEMPTS] ?: 3
    }

    override suspend fun setRetryAttempts(count: Int) {
        Timber.d("Setting retry attempts: $count")
        context.dataStore.edit { prefs ->
            prefs[Keys.RETRY_ATTEMPTS] = count.coerceIn(0, 10)
        }
    }

    override val networkParallelism: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NETWORK_PARALLELISM] ?: 4
    }

    override suspend fun setNetworkParallelism(threads: Int) {
        Timber.d("Setting network parallelism: $threads threads")
        context.dataStore.edit { prefs ->
            prefs[Keys.NETWORK_PARALLELISM] = threads.coerceIn(1, 32)
        }
    }

    override val offlineMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_MODE] ?: false
    }

    override suspend fun setOfflineMode(enabled: Boolean) {
        Timber.d("Setting offline mode: $enabled")
        context.dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_MODE] = enabled
        }
    }

    // ==================== Album Art Settings ====================

    override val albumArtWifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALBUM_ART_WIFI_ONLY] ?: true
    }

    override suspend fun setAlbumArtWifiOnly(wifiOnly: Boolean) {
        Timber.d("Setting album art WiFi only: $wifiOnly")
        context.dataStore.edit { prefs ->
            prefs[Keys.ALBUM_ART_WIFI_ONLY] = wifiOnly
        }
    }

    override val showAlbumArt: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_ALBUM_ART] ?: true
    }

    override suspend fun setShowAlbumArt(show: Boolean) {
        Timber.d("Setting show album art: $show")
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_ALBUM_ART] = show
        }
    }

    // ==================== Onboarding ====================

    override val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        Timber.d("Setting has completed onboarding: $completed")
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
