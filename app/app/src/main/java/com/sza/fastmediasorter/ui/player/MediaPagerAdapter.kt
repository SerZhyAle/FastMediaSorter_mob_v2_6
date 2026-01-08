package com.sza.fastmediasorter.ui.player

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemMediaPageBinding
import com.sza.fastmediasorter.databinding.ItemMediaPageVideoBinding
import com.sza.fastmediasorter.databinding.ItemMediaPageAudioBinding
import com.sza.fastmediasorter.databinding.ItemMediaPageTextBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Adapter for ViewPager2 to display media files.
 * Supports images, videos with ExoPlayer, audio with controls, and text files.
 */
class MediaPagerAdapter(
    private val onMediaClick: () -> Unit,
    private val onMediaLongClick: () -> Boolean,
    private val videoPlayerManager: VideoPlayerManager? = null,
    private val audioPlayerManager: AudioPlayerManager? = null,
    private val onPreviousClick: (() -> Unit)? = null,
    private val onNextClick: (() -> Unit)? = null
) : ListAdapter<String, RecyclerView.ViewHolder>(MediaDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_VIDEO = 1
        private const val VIEW_TYPE_AUDIO = 2
        private const val VIEW_TYPE_TEXT = 3
        private const val MAX_TEXT_FILE_SIZE = 1_000_000L // 1MB limit for text files
    }

    private var currentVideoHolder: VideoViewHolder? = null
    private var currentAudioHolder: AudioViewHolder? = null
    private var currentVisiblePosition = -1

    override fun getItemViewType(position: Int): Int {
        val filePath = getItem(position)
        val file = File(filePath)
        return when {
            isVideo(file) -> VIEW_TYPE_VIDEO
            isAudio(file) -> VIEW_TYPE_AUDIO
            isText(file) -> VIEW_TYPE_TEXT
            else -> VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VIDEO -> {
                val binding = ItemMediaPageVideoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                VideoViewHolder(binding)
            }
            VIEW_TYPE_AUDIO -> {
                val binding = ItemMediaPageAudioBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AudioViewHolder(binding)
            }
            VIEW_TYPE_TEXT -> {
                val binding = ItemMediaPageTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TextViewHolder(binding)
            }
            else -> {
                val binding = ItemMediaPageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ImageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val filePath = getItem(position)
        when (holder) {
            is ImageViewHolder -> holder.bind(filePath)
            is VideoViewHolder -> holder.bind(filePath)
            is AudioViewHolder -> holder.bind(filePath)
            is TextViewHolder -> holder.bind(filePath)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is VideoViewHolder -> holder.stopPlayback()
            is AudioViewHolder -> holder.stopPlayback()
            is TextViewHolder -> holder.cancelLoading()
        }
    }

    /**
     * Called when a page becomes visible.
     * Manages video and audio playback.
     */
    fun onPageSelected(position: Int) {
        currentVisiblePosition = position
        
        // Stop previous video if any
        currentVideoHolder?.stopPlayback()
        currentVideoHolder = null
        
        // Stop previous audio if any
        currentAudioHolder?.stopPlayback()
        currentAudioHolder = null
    }

    /**
     * Called to notify that a video holder is visible and ready.
     */
    fun onVideoHolderVisible(holder: VideoViewHolder, position: Int) {
        if (position == currentVisiblePosition) {
            currentVideoHolder = holder
        }
    }

    /**
     * Called to notify that an audio holder is visible and ready.
     */
    fun onAudioHolderVisible(holder: AudioViewHolder, position: Int) {
        if (position == currentVisiblePosition) {
            currentAudioHolder = holder
        }
    }

    /**
     * Release all video resources.
     */
    fun releaseVideo() {
        currentVideoHolder?.stopPlayback()
        currentVideoHolder = null
    }

    /**
     * Release all audio resources.
     */
    fun releaseAudio() {
        currentAudioHolder?.stopPlayback()
        currentAudioHolder = null
    }

    inner class ImageViewHolder(
        private val binding: ItemMediaPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(filePath: String) {
            loadImage(filePath)

            binding.root.setOnClickListener {
                onMediaClick()
            }

            binding.root.setOnLongClickListener {
                onMediaLongClick()
            }
        }

        private fun loadImage(filePath: String) {
            Timber.d("Loading image: $filePath")

            Glide.with(binding.mediaImage.context)
                .load(File(filePath))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.mediaImage)
        }
    }

    inner class VideoViewHolder(
        private val binding: ItemMediaPageVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentFilePath: String? = null
        private var isPlaying = false

        fun bind(filePath: String) {
            currentFilePath = filePath
            
            // Load thumbnail initially
            loadThumbnail(filePath)
            
            // Show play overlay
            binding.playOverlay.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.errorContainer.visibility = View.GONE

            // Click on play overlay to start video
            binding.playOverlay.setOnClickListener {
                startPlayback()
            }

            // Click on thumbnail to start video
            binding.imageView.setOnClickListener {
                if (!isPlaying) {
                    startPlayback()
                } else {
                    onMediaClick()
                }
            }

            // Player view click
            binding.playerView.setOnClickListener {
                onMediaClick()
            }

            binding.root.setOnLongClickListener {
                onMediaLongClick()
            }

            // Retry button
            binding.btnRetry.setOnClickListener {
                startPlayback()
            }

            // Notify adapter that this video holder is ready
            onVideoHolderVisible(this, bindingAdapterPosition)
        }

        private fun loadThumbnail(filePath: String) {
            Timber.d("Loading video thumbnail: $filePath")
            
            Glide.with(binding.imageView.context)
                .load(File(filePath))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_broken_image)
                .frame(1000000) // Grab frame at 1 second
                .into(binding.imageView)
        }

        fun startPlayback() {
            val filePath = currentFilePath ?: return
            val manager = videoPlayerManager ?: return

            Timber.d("Starting video playback: $filePath")

            // Show loading
            binding.progressBar.visibility = View.VISIBLE
            binding.playOverlay.visibility = View.GONE
            binding.errorContainer.visibility = View.GONE

            // Attach player to view
            manager.attachToView(binding.playerView)
            
            // Load and play video
            manager.loadVideo(filePath)
            manager.play()

            // Setup playback listener
            manager.setPlaybackListener(object : VideoPlayerManager.PlaybackListener {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@VideoViewHolder.isPlaying = isPlaying
                    if (isPlaying) {
                        binding.playerView.visibility = View.VISIBLE
                        binding.imageView.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                    }
                }

                override fun onError(message: String) {
                    Timber.e("Video playback error: $message")
                    binding.progressBar.visibility = View.GONE
                    binding.playerView.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE
                    binding.playOverlay.visibility = View.GONE
                    binding.errorContainer.visibility = View.VISIBLE
                }

                override fun onBuffering() {
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onReady() {
                    binding.progressBar.visibility = View.GONE
                }

                override fun onEnded() {
                    // Video ended - show thumbnail and play overlay again
                    resetToThumbnail()
                }
            })
        }

        fun stopPlayback() {
            videoPlayerManager?.pause()
            resetToThumbnail()
        }

        private fun resetToThumbnail() {
            isPlaying = false
            binding.playerView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.playOverlay.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.errorContainer.visibility = View.GONE
        }
    }

    inner class AudioViewHolder(
        private val binding: ItemMediaPageAudioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentFilePath: String? = null
        private var isPlaying = false
        private val progressHandler = Handler(Looper.getMainLooper())
        private var progressRunnable: Runnable? = null

        fun bind(filePath: String) {
            currentFilePath = filePath
            val file = File(filePath)
            
            // Set title
            binding.textTitle.text = file.nameWithoutExtension
            
            // Reset UI
            binding.progressBar.visibility = View.GONE
            binding.errorContainer.visibility = View.GONE
            binding.controlsContainer.visibility = View.VISIBLE
            binding.infoContainer.visibility = View.VISIBLE
            updatePlayPauseButton(false)
            
            // Setup click listeners
            binding.btnPlayPause.setOnClickListener {
                if (isPlaying) {
                    pausePlayback()
                } else {
                    startPlayback()
                }
            }
            
            binding.btnPrevious.setOnClickListener {
                onPreviousClick?.invoke()
            }
            
            binding.btnNext.setOnClickListener {
                onNextClick?.invoke()
            }
            
            binding.btnRewind.setOnClickListener {
                audioPlayerManager?.seekBackward(10_000)
            }
            
            binding.btnForward.setOnClickListener {
                audioPlayerManager?.seekForward(10_000)
            }
            
            binding.progressSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val duration = audioPlayerManager?.getDuration() ?: 0
                    val position = (value / 100f * duration).toLong()
                    audioPlayerManager?.seekTo(position)
                }
            }
            
            binding.root.setOnLongClickListener {
                onMediaLongClick()
            }
            
            binding.btnRetry.setOnClickListener {
                startPlayback()
            }

            // Notify adapter that this audio holder is ready
            onAudioHolderVisible(this, bindingAdapterPosition)
        }

        private fun startPlayback() {
            val filePath = currentFilePath ?: return
            val manager = audioPlayerManager ?: return

            Timber.d("Starting audio playback: $filePath")

            // Show loading
            binding.progressBar.visibility = View.VISIBLE

            // Load and play audio
            manager.loadAudio(filePath)
            manager.play()

            // Setup playback listener
            manager.setPlaybackListener(object : AudioPlayerManager.PlaybackListener {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@AudioViewHolder.isPlaying = isPlaying
                    updatePlayPauseButton(isPlaying)
                    if (isPlaying) {
                        binding.progressBar.visibility = View.GONE
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onError(message: String) {
                    Timber.e("Audio playback error: $message")
                    binding.progressBar.visibility = View.GONE
                    binding.errorContainer.visibility = View.VISIBLE
                    binding.textError.text = message
                }

                override fun onBuffering() {
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onReady() {
                    binding.progressBar.visibility = View.GONE
                    updateDuration()
                }

                override fun onEnded() {
                    isPlaying = false
                    updatePlayPauseButton(false)
                    stopProgressUpdates()
                    binding.progressSlider.value = 0f
                    binding.textCurrentTime.text = formatTime(0)
                }

                override fun onProgressUpdate(position: Long, duration: Long) {
                    // Not used - we handle our own progress updates
                }
            })
        }

        private fun pausePlayback() {
            audioPlayerManager?.pause()
            isPlaying = false
            updatePlayPauseButton(false)
            stopProgressUpdates()
        }

        fun stopPlayback() {
            audioPlayerManager?.pause()
            isPlaying = false
            updatePlayPauseButton(false)
            stopProgressUpdates()
        }

        private fun updatePlayPauseButton(playing: Boolean) {
            val iconRes = if (playing) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
            binding.btnPlayPause.setImageResource(iconRes)
        }

        private fun startProgressUpdates() {
            stopProgressUpdates()
            progressRunnable = object : Runnable {
                override fun run() {
                    val position = audioPlayerManager?.getCurrentPosition() ?: 0
                    val duration = audioPlayerManager?.getDuration() ?: 1
                    
                    if (duration > 0) {
                        val progress = (position.toFloat() / duration * 100f).coerceIn(0f, 100f)
                        binding.progressSlider.value = progress
                        binding.textCurrentTime.text = formatTime(position)
                    }
                    
                    progressHandler.postDelayed(this, 500)
                }
            }
            progressHandler.post(progressRunnable!!)
        }

        private fun stopProgressUpdates() {
            progressRunnable?.let { progressHandler.removeCallbacks(it) }
            progressRunnable = null
        }

        private fun updateDuration() {
            val duration = audioPlayerManager?.getDuration() ?: 0
            binding.textDuration.text = formatTime(duration)
        }

        private fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun isImage(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")
    }

    private fun isVideo(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")
    }

    private fun isAudio(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
    }

    private fun isText(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf(
            "txt", "log", "md", "json", "xml", "html", "css", "js", "ts",
            "kt", "java", "py", "rb", "c", "cpp", "h", "hpp", "cs", "go",
            "rs", "swift", "yaml", "yml", "toml", "ini", "conf", "cfg",
            "properties", "sh", "bash", "bat", "ps1", "sql", "csv"
        )
    }

    /**
     * ViewHolder for text files.
     */
    inner class TextViewHolder(
        private val binding: ItemMediaPageTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadJob: Job? = null

        fun bind(filePath: String) {
            // Reset state
            binding.progressBar.isVisible = true
            binding.textScrollView.isVisible = false
            binding.errorContainer.isVisible = false
            binding.truncationNotice.isVisible = false
            binding.textContent.text = ""

            // Set click listener
            binding.root.setOnClickListener {
                onMediaClick()
            }

            binding.root.setOnLongClickListener {
                onMediaLongClick()
            }

            // Load text content
            loadTextContent(filePath)
        }

        private fun loadTextContent(filePath: String) {
            loadJob?.cancel()
            loadJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val file = File(filePath)
                        if (!file.exists()) {
                            throw IllegalStateException("File not found")
                        }
                        if (!file.canRead()) {
                            throw IllegalStateException("Cannot read file")
                        }

                        val isTruncated = file.length() > MAX_TEXT_FILE_SIZE
                        
                        val content = if (isTruncated) {
                            // Read only first 1MB
                            file.inputStream().use { input ->
                                val bytes = ByteArray(MAX_TEXT_FILE_SIZE.toInt())
                                val read = input.read(bytes)
                                String(bytes, 0, read, Charsets.UTF_8)
                            }
                        } else {
                            file.readText(Charsets.UTF_8)
                        }
                        
                        Pair(content, isTruncated)
                    }

                    val (content, isTruncated) = result
                    
                    binding.progressBar.isVisible = false
                    binding.textScrollView.isVisible = true
                    binding.textContent.text = content
                    binding.truncationNotice.isVisible = isTruncated

                } catch (e: Exception) {
                    Timber.e(e, "Failed to load text file: $filePath")
                    showError(e.message ?: "Failed to load file")
                }
            }
        }

        private fun showError(message: String) {
            binding.progressBar.isVisible = false
            binding.textScrollView.isVisible = false
            binding.errorContainer.isVisible = true
            binding.errorMessage.text = message
            
            binding.btnRetry.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    bind(getItem(position))
                }
            }
        }

        fun cancelLoading() {
            loadJob?.cancel()
            loadJob = null
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
