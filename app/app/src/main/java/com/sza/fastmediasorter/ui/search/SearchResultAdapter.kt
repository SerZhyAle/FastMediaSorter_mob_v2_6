package com.sza.fastmediasorter.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemSearchResultBinding
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.SearchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for displaying search results.
 */
class SearchResultAdapter(
    private val onItemClick: (SearchResult) -> Unit,
    private val onItemLongClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultAdapter.SearchResultViewHolder>(SearchResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResult) {
            binding.apply {
                // File name
                textFileName.text = result.file.name

                // Resource path
                textResourcePath.text = result.resource.name

                // File size and date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val sizeText = formatFileSize(result.file.size)
                val dateText = dateFormat.format(result.file.date)
                textFileInfo.text = root.context.getString(
                    R.string.search_result_info,
                    sizeText,
                    dateText
                )

                // Thumbnail
                when (result.file.type) {
                    MediaType.IMAGE, MediaType.VIDEO -> {
                        Glide.with(root.context)
                            .load(result.file.path)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_broken_image)
                            .centerCrop()
                            .into(imageThumbnail)
                    }
                    MediaType.AUDIO -> {
                        imageThumbnail.setImageResource(R.drawable.ic_audio)
                    }
                    MediaType.PDF, MediaType.TXT, MediaType.EPUB -> {
                        imageThumbnail.setImageResource(R.drawable.ic_file)
                    }
                    MediaType.GIF, MediaType.OTHER -> {
                        imageThumbnail.setImageResource(R.drawable.ic_file)
                    }
                }

                // Click handlers
                root.setOnClickListener { onItemClick(result) }
                root.setOnLongClickListener {
                    onItemLongClick(result)
                    true
                }
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }

    private class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.file.path == newItem.file.path
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
