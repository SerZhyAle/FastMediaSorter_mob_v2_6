package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to handle audio playback with ExoPlayer and MediaSession.
 * Provides notification controls for background playback.
 * 
 * Injected as singleton but player is initialized/released per activity lifecycle.
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
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
        fun onProgressUpdate(position: Long, duration: Long)
    }

    fun setPlaybackListener(listener: PlaybackListener?) {
        this.playbackListener = listener
    }

    /**
     * Initialize ExoPlayer and MediaSession.
     * Should be called in Activity onCreate with the activity context.
     */
    @OptIn(UnstableApi::class)
    fun initialize(context: Context) {
        if (player == null) {
            Timber.d("Initializing Audio Player with MediaSession")
            
            player = ExoPlayer.Builder(context)
                .build()
                .apply {
                    addListener(playerListener)
                    playWhenReady = this@AudioPlayerManager.playWhenReady
                }
            
            // Create MediaSession for notification controls
            mediaSession = MediaSession.Builder(context, player!!)
                .build()
        }
    }

    /**
     * Load and prepare an audio file for playback.
     */
    fun loadAudio(filePath: String, title: String? = null, artist: String? = null, startPosition: Long = 0) {
        Timber.d("Loading audio: $filePath at position: $startPosition")
        
        val player = player ?: run {
            Timber.w("Player not initialized")
            return
        }

        // If same audio, just seek
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
        
        // Create media item with metadata
        val fileName = File(filePath).nameWithoutExtension
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title ?: fileName)
            .setArtist(artist ?: "Unknown Artist")
            .build()
        
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()
        
        player.setMediaItem(mediaItem)
        player.seekTo(playbackPosition)
        player.prepare()
    }

    /**
     * Start or resume playback.
     */
    fun play() {
        Timber.d("Audio play requested")
        player?.play()
    }

    /**
     * Pause playback.
     */
    fun pause() {
        Timber.d("Audio pause requested")
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
        Timber.d("Audio seeking to: $positionMs ms")
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
     * Get the current media path.
     */
    fun getCurrentMediaPath(): String? = currentMediaPath

    /**
     * Save the current playback state (for configuration changes).
     */
    fun saveState() {
        player?.let {
            playbackPosition = it.currentPosition
            wasPlaying = it.isPlaying
            playWhenReady = it.playWhenReady
        }
        Timber.d("Audio state saved: position=$playbackPosition, wasPlaying=$wasPlaying")
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
        Timber.d("Audio state restored: position=$playbackPosition, wasPlaying=$wasPlaying")
    }

    /**
     * Release player resources.
     */
    fun release() {
        Timber.d("Releasing Audio Player and MediaSession")
        saveState()
        
        mediaSession?.release()
        mediaSession = null
        
        player?.run {
            removeListener(playerListener)
            release()
        }
        player = null
        currentMediaPath = null
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    Timber.d("Audio ready, duration: ${player?.duration}")
                    playbackListener?.onReady()
                }
                Player.STATE_ENDED -> {
                    Timber.d("Audio playback ended")
                    playbackListener?.onEnded()
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("Audio buffering...")
                    playbackListener?.onBuffering()
                }
                Player.STATE_IDLE -> {
                    Timber.d("Audio player idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("Audio isPlaying changed: $isPlaying")
            playbackListener?.onPlaybackStateChanged(isPlaying)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Audio player error: ${error.message}")
            playbackListener?.onError(error.message ?: "Audio playback error")
        }
    }
}
