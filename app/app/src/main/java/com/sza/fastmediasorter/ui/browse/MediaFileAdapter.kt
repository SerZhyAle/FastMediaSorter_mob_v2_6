package com.sza.fastmediasorter.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemMediaFileBinding
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType

/**
 * Adapter for displaying media files in a grid or list.
 */
class MediaFileAdapter(
    private val onItemClick: (MediaFile) -> Unit,
    private val onItemLongClick: (MediaFile) -> Boolean = { false }
) : ListAdapter<MediaFile, MediaFileAdapter.ViewHolder>(MediaFileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMediaFileBinding,
        private val onItemClick: (MediaFile) -> Unit,
        private val onItemLongClick: (MediaFile) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaFile: MediaFile) {
            binding.textFileName.text = mediaFile.name

            // Set placeholder icon based on media type
            val iconRes = when (mediaFile.type) {
                MediaType.IMAGE -> R.drawable.ic_image
                MediaType.VIDEO -> R.drawable.ic_video
                MediaType.AUDIO -> R.drawable.ic_audio
                MediaType.GIF -> R.drawable.ic_gif
                else -> R.drawable.ic_file
            }
            binding.thumbnail.setImageResource(iconRes)

            // Show duration for video/audio
            if (mediaFile.type == MediaType.VIDEO || mediaFile.type == MediaType.AUDIO) {
                binding.textDuration.visibility = View.VISIBLE
                binding.textDuration.text = formatDuration(mediaFile.duration ?: 0)
            } else {
                binding.textDuration.visibility = View.GONE
            }

            // TODO: Load actual thumbnail using Coil/Glide

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
