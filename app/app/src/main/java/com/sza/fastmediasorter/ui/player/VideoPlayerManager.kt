package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
     */
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
        
        val mediaItem = MediaItem.fromUri(uri)
        
        player.setMediaItem(mediaItem)
        player.seekTo(playbackPosition)
        player.prepare()
    }

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
     * Get the current playback position.
     */
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    /**
     * Get the total duration of the media.
     */
    fun getDuration(): Long = player?.duration ?: 0

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
