package com.sza.fastmediasorter.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemMediaFileBinding
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import java.io.File

/**
 * Paginated adapter for displaying media files in a grid or list.
 * Uses Paging3 for efficient loading of large file collections.
 * Supports selection mode with checkboxes.
 * Uses Glide for thumbnail loading.
 */
class MediaFilePagingAdapter(
    private val onItemClick: (MediaFile) -> Unit,
    private val onItemLongClick: (MediaFile) -> Boolean = { false }
) : PagingDataAdapter<MediaFile, MediaFilePagingAdapter.ViewHolder>(MediaFileDiffCallback()) {

    private var isSelectionMode = false
    private var selectedPaths = emptySet<String>()

    fun setSelectionMode(enabled: Boolean, selectedFiles: Set<String>) {
        val changed = isSelectionMode != enabled || selectedPaths != selectedFiles
        isSelectionMode = enabled
        selectedPaths = selectedFiles
        if (changed) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item, isSelectionMode, selectedPaths)
        }
    }

    class ViewHolder(
        private val binding: ItemMediaFileBinding,
        private val onItemClick: (MediaFile) -> Unit,
        private val onItemLongClick: (MediaFile) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaFile: MediaFile, isSelectionMode: Boolean, selectedPaths: Set<String>) {
            binding.textFileName.text = mediaFile.name

            // Get placeholder icon based on media type
            val placeholderRes = when (mediaFile.type) {
                MediaType.IMAGE -> R.drawable.ic_image
                MediaType.VIDEO -> R.drawable.ic_video
                MediaType.AUDIO -> R.drawable.ic_audio
                MediaType.GIF -> R.drawable.ic_gif
                else -> R.drawable.ic_file
            }

            // Load thumbnail using Glide
            when (mediaFile.type) {
                MediaType.IMAGE, MediaType.GIF -> {
                    // Load image/GIF thumbnail directly
                    Glide.with(binding.root.context)
                        .load(File(mediaFile.path))
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.thumbnail)
                }
                MediaType.VIDEO -> {
                    // Load video thumbnail (frame from video)
                    Glide.with(binding.root.context)
                        .load(File(mediaFile.path))
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.thumbnail)
                }
                MediaType.AUDIO -> {
                    // For audio, just show placeholder icon
                    binding.thumbnail.setImageResource(placeholderRes)
                }
                else -> {
                    binding.thumbnail.setImageResource(placeholderRes)
                }
            }

            // Show duration for video/audio
            if (mediaFile.type == MediaType.VIDEO || mediaFile.type == MediaType.AUDIO) {
                binding.textDuration.visibility = View.VISIBLE
                binding.textDuration.text = formatDuration(mediaFile.duration ?: 0)
            } else {
                binding.textDuration.visibility = View.GONE
            }

            // Selection mode checkbox
            binding.checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.checkbox.isChecked = selectedPaths.contains(mediaFile.path)

            binding.root.setOnClickListener { onItemClick(mediaFile) }
            binding.root.setOnLongClickListener { onItemLongClick(mediaFile) }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val mins = minutes % 60
                String.format("%d:%02d:%02d", hours, mins, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    }

    class MediaFileDiffCallback : DiffUtil.ItemCallback<MediaFile>() {
        override fun areItemsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
            return oldItem == newItem
        }
    }
}
