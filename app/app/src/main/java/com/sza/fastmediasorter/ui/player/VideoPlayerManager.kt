package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to handle ExoPlayer lifecycle and operations.
 * Encapsulates all video playback logic separate from Activity.
 *
 * Injected as singleton but player is initialized/released per activity lifecycle.
 */
@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var player: ExoPlayer? = null
    private var currentPlayerView: PlayerView? = null
    private var currentMediaPath: String? = null
    private var subtitlesEnabled: Boolean = true

    private var playbackPosition: Long = 0
    private var playWhenReady: Boolean = true
    private var wasPlaying: Boolean = false
    private var playbackListener: PlaybackListener? = null

    /**
     * Listener for playback events.
     */
    interface PlaybackListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onBuffering()
        fun onReady()
        fun onEnded()
        fun onError(message: String)
    }

    fun setPlaybackListener(listener: PlaybackListener?) {
        this.playbackListener = listener
    }

    /**
     * Initialize ExoPlayer instance.
     * Should be called in Activity onCreate with the activity context.
     */
    fun initialize(context: Context) {
        if (player == null) {
            Timber.d("Initializing ExoPlayer")
            player = ExoPlayer.Builder(context)
                .build()
                .apply {
                    addListener(playerListener)
                    playWhenReady = this@VideoPlayerManager.playWhenReady
                }
        }
    }

    /**
     * Attach the player to a PlayerView.
     */
    @OptIn(UnstableApi::class)
    fun attachToView(playerView: PlayerView) {
        Timber.d("Attaching player to view")
        currentPlayerView = playerView
        playerView.player = player
        playerView.useController = true
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
    }

    /**
     * Detach the player from the current view.
     */
    fun detachFromView() {
        Timber.d("Detaching player from view")
        currentPlayerView?.player = null
        currentPlayerView = null
    }

    /**
     * Load and prepare a video file for playback.
     * Automatically detects and adds subtitle files (.srt, .vtt) with the same base name.
     */
    @OptIn(UnstableApi::class)
    fun loadVideo(filePath: String, startPosition: Long = 0) {
        Timber.d("Loading video: $filePath at position: $startPosition")

        val player = player ?: run {
            Timber.w("Player not initialized")
            return
        }

        // If same video, just seek
        if (filePath == currentMediaPath) {
            player.seekTo(startPosition)
            return
        }

        currentMediaPath = filePath
        playbackPosition = startPosition

        // Convert file path to URI
        val uri = if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
            Uri.parse(filePath)
        } else {
            Uri.fromFile(File(filePath))
        }

        // Build MediaItem with subtitle tracks if found
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)

        // Detect and add subtitle files
        val subtitleConfigs = detectSubtitleFiles(filePath)
        if (subtitleConfigs.isNotEmpty()) {
            Timber.d("Found ${subtitleConfigs.size} subtitle file(s)")
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()

        player.setMediaItem(mediaItem)
        player.seekTo(playbackPosition)
        player.prepare()
    }

    /**
     * Detect subtitle files (.srt, .vtt) with the same base name as the video file.
     * Returns a list of SubtitleConfiguration for ExoPlayer.
     */
    @OptIn(UnstableApi::class)
    private fun detectSubtitleFiles(videoPath: String): List<MediaItem.SubtitleConfiguration> {
        val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
        
        try {
            val videoFile = File(videoPath)
            val parentDir = videoFile.parentFile ?: return emptyList()
            val baseName = videoFile.nameWithoutExtension
            
            // Check for .srt files
            val srtFile = File(parentDir, "$baseName.srt")
            if (srtFile.exists() && srtFile.canRead()) {
                Timber.d("Found SRT subtitle: ${srtFile.absolutePath}")
                subtitleConfigs.add(
                    MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(srtFile))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("und") // Unknown language
                        .setLabel("Subtitles (SRT)")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            }
            
            // Check for .vtt files
            val vttFile = File(parentDir, "$baseName.vtt")
            if (vttFile.exists() && vttFile.canRead()) {
                Timber.d("Found VTT subtitle: ${vttFile.absolutePath}")
                subtitleConfigs.add(
                    MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(vttFile))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("und")
                        .setLabel("Subtitles (VTT)")
                        .setSelectionFlags(if (srtFile.exists()) 0 else C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            }
            
            // Check for language-specific subtitle files (e.g., video.en.srt, video.ru.srt)
            val languageCodes = listOf("en", "ru", "uk", "es", "de", "fr", "it", "pt", "ja", "ko", "zh")
            for (langCode in languageCodes) {
                val langSrtFile = File(parentDir, "$baseName.$langCode.srt")
                if (langSrtFile.exists() && langSrtFile.canRead()) {
                    Timber.d("Found language-specific SRT subtitle: ${langSrtFile.absolutePath}")
                    subtitleConfigs.add(
                        MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(langSrtFile))
                            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                            .setLanguage(langCode)
                            .setLabel(getLanguageLabel(langCode))
                            .build()
                    )
                }
                
                val langVttFile = File(parentDir, "$baseName.$langCode.vtt")
                if (langVttFile.exists() && langVttFile.canRead()) {
                    Timber.d("Found language-specific VTT subtitle: ${langVttFile.absolutePath}")
                    subtitleConfigs.add(
                        MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(langVttFile))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage(langCode)
                            .setLabel(getLanguageLabel(langCode))
                            .build()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error detecting subtitle files")
        }
        
        return subtitleConfigs
    }

    /**
     * Get a human-readable label for a language code.
     */
    private fun getLanguageLabel(langCode: String): String {
        return when (langCode) {
            "en" -> "English"
            "ru" -> "Русский"
            "uk" -> "Українська"
            "es" -> "Español"
            "de" -> "Deutsch"
            "fr" -> "Français"
            "it" -> "Italiano"
            "pt" -> "Português"
            "ja" -> "日本語"
            "ko" -> "한국어"
            "zh" -> "中文"
            else -> langCode.uppercase()
        }
    }

    /**
     * Enable or disable subtitles.
     */
    @OptIn(UnstableApi::class)
    fun setSubtitlesEnabled(enabled: Boolean) {
        subtitlesEnabled = enabled
        val player = player ?: return
        
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        
        Timber.d("Subtitles enabled: $enabled")
    }

    /**
     * Check if subtitles are currently enabled.
     */
    fun areSubtitlesEnabled(): Boolean = subtitlesEnabled

    /**
     * Get available subtitle track count.
     */
    @OptIn(UnstableApi::class)
    fun getSubtitleTrackCount(): Int {
        val player = player ?: return 0
        val tracks = player.currentTracks
        
        return tracks.groups.count { group ->
            group.type == C.TRACK_TYPE_TEXT && group.length > 0
        }
    }

    /**
     * Check if the current video has subtitle tracks available.
     */
    fun hasSubtitles(): Boolean = getSubtitleTrackCount() > 0

    /**
     * Start or resume playback.
     */
    fun play() {
        Timber.d("Play requested")
        player?.play()
    }

    /**
     * Pause playback.
     */
    fun pause() {
        Timber.d("Pause requested")
        player?.pause()
    }

    /**
     * Toggle play/pause state.
     */
    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) pause() else play()
        }
    }

    /**
     * Seek to a specific position.
     */
    fun seekTo(positionMs: Long) {
        Timber.d("Seeking to: $positionMs ms")
        player?.seekTo(positionMs)
    }

    /**
     * Seek forward by a given amount.
     */
    fun seekForward(amountMs: Long = 10_000) {
        player?.let {
            val newPosition = (it.currentPosition + amountMs).coerceAtMost(it.duration)
            seekTo(newPosition)
        }
    }

    /**
     * Seek backward by a given amount.
     */
    fun seekBackward(amountMs: Long = 10_000) {
        player?.let {
            val newPosition = (it.currentPosition - amountMs).coerceAtLeast(0)
            seekTo(newPosition)
        }
    }

    /**
     * Seek relative to current position (positive for forward, negative for backward).
     */
    fun seekRelative(amountMs: Long) {
        if (amountMs >= 0) {
            seekForward(amountMs)
        } else {
            seekBackward(-amountMs)
        }
    }

    /**
     * Get the current playback position.
     */
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    /**
     * Get the total duration of the media.
     */
    fun getDuration(): Long = player?.duration ?: 0

    /**
     * Seek to a percentage of the total duration.
     * @param percent 0-100
     */
    fun seekToPercent(percent: Int) {
        player?.let {
            val duration = it.duration
            if (duration > 0) {
                val position = (duration * percent.coerceIn(0, 100)) / 100
                Timber.d("Seeking to $percent% ($position ms of $duration ms)")
                seekTo(position)
            }
        }
    }

    /**
     * Check if the player is currently playing.
     */
    fun isPlaying(): Boolean = player?.isPlaying == true

    /**
     * Save the current playback state (for configuration changes).
     */
    fun saveState() {
        player?.let {
            playbackPosition = it.currentPosition
            wasPlaying = it.isPlaying
            playWhenReady = it.playWhenReady
        }
        Timber.d("State saved: position=$playbackPosition, wasPlaying=$wasPlaying")
    }

    /**
     * Restore playback state after configuration change.
     */
    fun restoreState() {
        player?.let {
            it.seekTo(playbackPosition)
            if (wasPlaying) {
                it.play()
            }
        }
        Timber.d("State restored: position=$playbackPosition, wasPlaying=$wasPlaying")
    }

    /**
     * Release player resources.
     */
    fun release() {
        Timber.d("Releasing ExoPlayer")
        saveState()
        player?.run {
            removeListener(playerListener)
            release()
        }
        player = null
        currentPlayerView = null
        currentMediaPath = null
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    Timber.d("Playback ready, duration: ${player?.duration}")
                    playbackListener?.onReady()
                }
                Player.STATE_ENDED -> {
                    Timber.d("Playback ended")
                    playbackListener?.onEnded()
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("Buffering...")
                    playbackListener?.onBuffering()
                }
                Player.STATE_IDLE -> {
                    Timber.d("Player idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("isPlaying changed: $isPlaying")
            playbackListener?.onPlaybackStateChanged(isPlaying)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Player error: ${error.message}")
            playbackListener?.onError(error.message ?: "Playback error")
        }
    }
}
