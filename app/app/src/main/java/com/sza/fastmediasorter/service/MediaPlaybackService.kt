package com.sza.fastmediasorter.service

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sza.fastmediasorter.data.repository.PlaybackPositionRepository
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Media playback service for background audio playback.
 * Extends MediaSessionService to provide:
 * - Background playback when app is minimized
 * - Lock screen controls
 * - Notification controls (play/pause, prev/next)
 * - Bluetooth/headphone button support
 * - Audio focus management
 */
@AndroidEntryPoint
@UnstableApi
class MediaPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackPositionRepository: PlaybackPositionRepository

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    
    private var currentFilePath: String? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("MediaPlaybackService created")
        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                addListener(playerListener)
            }

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(mediaSessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        intent?.let {
            when (it.action) {
                ACTION_PLAY -> {
                    val filePath = it.getStringExtra(EXTRA_FILE_PATH)
                    val title = it.getStringExtra(EXTRA_TITLE)
                    val artist = it.getStringExtra(EXTRA_ARTIST)
                    val position = it.getLongExtra(EXTRA_POSITION, 0L)
                    
                    if (filePath != null) {
                        loadAndPlay(filePath, title, artist, position)
                    }
                }
                ACTION_PAUSE -> pause()
                ACTION_RESUME -> resume()
                ACTION_STOP -> stopSelf()
            }
        }
        
        return START_STICKY
    }

    private fun loadAndPlay(
        filePath: String,
        title: String? = null,
        artist: String? = null,
        startPosition: Long = 0
    ) {
        currentFilePath = filePath
        
        val file = File(filePath)
        val mediaTitle = title ?: file.nameWithoutExtension
        
        val metadata = MediaMetadata.Builder()
            .setTitle(mediaTitle)
            .setArtist(artist ?: "Unknown Artist")
            .build()
        
        val mediaItem = MediaItem.Builder()
            .setUri(filePath)
            .setMediaMetadata(metadata)
            .build()
        
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (startPosition > 0) {
                seekTo(startPosition)
            }
            playWhenReady = true
        }
        
        Timber.d("Playing: $filePath at position $startPosition")
    }

    fun pause() {
        player?.pause()
        savePosition()
    }

    fun resume() {
        player?.play()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2.0f))
    }

    private fun savePosition() {
        val path = currentFilePath ?: return
        val player = player ?: return
        
        val position = player.currentPosition
        val duration = player.duration
        
        if (duration > 0) {
            playbackPositionRepository.savePosition(path, position, duration)
            Timber.d("Saved position: $position / $duration for $path")
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    Timber.d("Playback ended")
                    savePosition()
                }
                Player.STATE_READY -> {
                    Timber.d("Player ready")
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("Buffering...")
                }
                Player.STATE_IDLE -> {
                    Timber.d("Player idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("Playing state changed: $isPlaying")
            if (!isPlaying) {
                savePosition()
            }
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                )
                .build()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Save position before being killed
        savePosition()
        
        // Check if we should stop or continue playing
        val player = player
        if (player?.isPlaying != true) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Timber.d("MediaPlaybackService destroyed")
        savePosition()
        
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.sza.fastmediasorter.ACTION_PLAY"
        const val ACTION_PAUSE = "com.sza.fastmediasorter.ACTION_PAUSE"
        const val ACTION_RESUME = "com.sza.fastmediasorter.ACTION_RESUME"
        const val ACTION_STOP = "com.sza.fastmediasorter.ACTION_STOP"
        
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_POSITION = "extra_position"
    }
}
