package com.sza.fastmediasorter.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.databinding.ItemDestinationBinding
import com.sza.fastmediasorter.domain.model.Resource

/**
 * Adapter for displaying and managing destinations in settings.
 */
class DestinationAdapter(
    private val onRemoveClick: (Resource) -> Unit,
    private val onColorClick: (Resource) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Resource, DestinationAdapter.DestinationViewHolder>(DestinationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val binding = ItemDestinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DestinationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DestinationViewHolder(
        private val binding: ItemDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(resource: Resource) {
            binding.textName.text = resource.name
            binding.textPath.text = resource.path

            // Set color badge
            val drawable = binding.colorBadge.background as? GradientDrawable
            drawable?.setColor(resource.destinationColor)

            // Remove button
            binding.btnRemove.setOnClickListener {
                onRemoveClick(resource)
            }

            // Color badge click
            binding.colorBadge.setOnClickListener {
                onColorClick(resource)
            }

            // Drag handle touch
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    class DestinationDiffCallback : DiffUtil.ItemCallback<Resource>() {
        override fun areItemsTheSame(oldItem: Resource, newItem: Resource): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Resource, newItem: Resource): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * ItemTouchHelper callback for drag-and-drop reordering.
 */
class DestinationItemTouchCallback(
    private val onMoved: (fromPos: Int, toPos: Int) -> Unit,
    private val onDragEnded: () -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        onMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onDragEnded()
    }
}
