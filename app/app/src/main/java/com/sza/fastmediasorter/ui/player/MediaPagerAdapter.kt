package com.sza.fastmediasorter.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemMediaPageBinding
import timber.log.Timber
import java.io.File

/**
 * Adapter for ViewPager2 to display media files.
 * Currently supports images, will be extended for video/audio.
 */
class MediaPagerAdapter(
    private val onImageClick: () -> Unit,
    private val onImageLongClick: () -> Boolean
) : ListAdapter<String, MediaPagerAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(
        private val binding: ItemMediaPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(filePath: String) {
            val file = File(filePath)
            
            // Determine file type and load accordingly
            when {
                isImage(file) -> loadImage(filePath)
                isVideo(file) -> loadVideoThumbnail(filePath)
                else -> loadPlaceholder()
            }

            // Click listeners
            binding.root.setOnClickListener {
                onImageClick()
            }

            binding.root.setOnLongClickListener {
                onImageLongClick()
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

        private fun loadVideoThumbnail(filePath: String) {
            Timber.d("Loading video thumbnail: $filePath")
            
            // Load video thumbnail with Glide
            Glide.with(binding.mediaImage.context)
                .load(File(filePath))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_broken_image)
                .frame(1000000) // Grab frame at 1 second
                .into(binding.mediaImage)
        }

        private fun loadPlaceholder() {
            binding.mediaImage.setImageResource(R.drawable.ic_file_placeholder)
        }

        private fun isImage(file: File): Boolean {
            val extension = file.extension.lowercase()
            return extension in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")
        }

        private fun isVideo(file: File): Boolean {
            val extension = file.extension.lowercase()
            return extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")
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
