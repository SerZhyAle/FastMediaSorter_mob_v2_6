package com.sza.fastmediasorter.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemResourceBinding
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType

/**
 * RecyclerView Adapter for displaying resources in MainActivity.
 */
class ResourceAdapter(
    private val onItemClick: (Resource) -> Unit,
    private val onItemLongClick: (Resource) -> Boolean,
    private val onMoreClick: (Resource) -> Unit
) : ListAdapter<Resource, ResourceAdapter.ResourceViewHolder>(ResourceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResourceViewHolder(
        private val binding: ItemResourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(resource: Resource) {
            binding.apply {
                // Set resource name
                textResourceName.text = resource.name

                // Set resource path/type
                textResourcePath.text = resource.path

                // Set icon based on type
                iconResource.setImageResource(getIconForType(resource.type))

                // Show destination badge if this is a destination
                destinationBadge.visibility = if (resource.isDestination) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Set destination badge color
                if (resource.isDestination) {
                    destinationBadge.background.setTint(resource.destinationColor)
                }

                // Click listeners
                root.setOnClickListener { onItemClick(resource) }
                root.setOnLongClickListener { onItemLongClick(resource) }
                iconMore.setOnClickListener { onMoreClick(resource) }
            }
        }

        private fun getIconForType(type: ResourceType): Int {
            return when (type) {
                ResourceType.LOCAL -> R.drawable.ic_folder
                ResourceType.SMB -> R.drawable.ic_smb
                ResourceType.SFTP -> R.drawable.ic_sftp
                ResourceType.FTP -> R.drawable.ic_ftp
                ResourceType.GOOGLE_DRIVE -> R.drawable.ic_google_drive
                ResourceType.ONEDRIVE -> R.drawable.ic_onedrive
                ResourceType.DROPBOX -> R.drawable.ic_dropbox
            }
        }
    }

    private class ResourceDiffCallback : DiffUtil.ItemCallback<Resource>() {
        override fun areItemsTheSame(oldItem: Resource, newItem: Resource): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Resource, newItem: Resource): Boolean {
            return oldItem == newItem
        }
    }
}
