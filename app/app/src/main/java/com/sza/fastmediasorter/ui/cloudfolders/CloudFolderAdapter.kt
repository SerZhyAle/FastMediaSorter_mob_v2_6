package com.sza.fastmediasorter.ui.cloudfolders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.data.cloud.CloudFile
import com.sza.fastmediasorter.databinding.ItemCloudFolderBinding

/**
 * Adapter for displaying cloud folders in a RecyclerView
 */
class CloudFolderAdapter(
    private val onFolderClick: (CloudFile) -> Unit,
    private val onNavigateClick: (CloudFile) -> Unit
) : ListAdapter<CloudFile, CloudFolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemCloudFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(
        private val binding: ItemCloudFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: CloudFile) {
            binding.tvFolderName.text = folder.name
            
            // Click on card selects folder
            binding.root.setOnClickListener {
                onFolderClick(folder)
            }

            // Click on navigate button goes into folder
            binding.btnNavigate.setOnClickListener {
                onNavigateClick(folder)
            }
        }
    }

    class FolderDiffCallback : DiffUtil.ItemCallback<CloudFile>() {
        override fun areItemsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CloudFile, newItem: CloudFile): Boolean {
            return oldItem == newItem
        }
    }
}
