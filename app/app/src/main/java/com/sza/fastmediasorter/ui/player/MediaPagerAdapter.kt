package com.sza.fastmediasorter.ui.player

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemMediaPageBinding
import com.sza.fastmediasorter.databinding.ItemMediaPageVideoBinding
import timber.log.Timber
import java.io.File

/**
 * Adapter for ViewPager2 to display media files.
 * Supports images and videos with ExoPlayer integration.
 */
class MediaPagerAdapter(
    private val onMediaClick: () -> Unit,
    private val onMediaLongClick: () -> Boolean,
    private val videoPlayerManager: VideoPlayerManager? = null
) : ListAdapter<String, RecyclerView.ViewHolder>(MediaDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_VIDEO = 1
    }

    private var currentVideoHolder: VideoViewHolder? = null
    private var currentVisiblePosition = -1

    override fun getItemViewType(position: Int): Int {
        val filePath = getItem(position)
        val file = File(filePath)
        return if (isVideo(file)) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
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
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.stopPlayback()
        }
    }

    /**
     * Called when a page becomes visible.
     * Starts video playback if it's a video.
     */
    fun onPageSelected(position: Int) {
        currentVisiblePosition = position
        
        // Stop previous video if any
        currentVideoHolder?.stopPlayback()
        currentVideoHolder = null
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
     * Release all video resources.
     */
    fun releaseVideo() {
        currentVideoHolder?.stopPlayback()
        currentVideoHolder = null
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

    private fun isImage(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")
    }

    private fun isVideo(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")
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
