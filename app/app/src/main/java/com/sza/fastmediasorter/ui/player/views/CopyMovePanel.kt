package com.sza.fastmediasorter.ui.player.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import timber.log.Timber

/**
 * A slide-up panel for copy/move destination selection.
 * 
 * Features:
 * - Collapsible header with expand/collapse toggle
 * - Grid of destination buttons (5x1, 5x2, or 5x3 based on count)
 * - Remembers last operation type (copy or move)
 * - Swipe to dismiss support
 * - Slide up/down animations
 */
class CopyMovePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /**
     * Represents a destination for copy/move operations.
     */
    data class Destination(
        val id: String,
        val name: String,
        val path: String,
        val iconRes: Int = R.drawable.ic_folder
    )

    /**
     * Operation type for the panel.
     */
    enum class OperationType {
        COPY, MOVE
    }

    interface OnDestinationClickListener {
        fun onDestinationClick(destination: Destination, operationType: OperationType)
        fun onDestinationLongClick(destination: Destination): Boolean
    }

    private var copyPanelView: View? = null
    private var movePanelView: View? = null
    private var copyDestinationsAdapter: DestinationsAdapter? = null
    private var moveDestinationsAdapter: DestinationsAdapter? = null
    
    private var isCopyPanelExpanded = true
    private var isMovePanelExpanded = true
    
    private var currentOperationType: OperationType = OperationType.COPY
    private var listener: OnDestinationClickListener? = null
    
    private val destinations = mutableListOf<Destination>()

    /**
     * Initialize the panel with the parent layout's views.
     * Call this after inflating the include_copy_move_panels.xml
     */
    fun init(
        copyPanel: View,
        movePanel: View,
        copyGrid: RecyclerView,
        moveGrid: RecyclerView,
        copyHeader: View,
        moveHeader: View,
        copyExpandIcon: ImageView,
        moveExpandIcon: ImageView
    ) {
        copyPanelView = copyPanel
        movePanelView = movePanel
        
        // Setup adapters
        copyDestinationsAdapter = DestinationsAdapter(OperationType.COPY) { dest, type ->
            listener?.onDestinationClick(dest, type)
        }
        moveDestinationsAdapter = DestinationsAdapter(OperationType.MOVE) { dest, type ->
            listener?.onDestinationClick(dest, type)
        }
        
        // Setup RecyclerViews with GridLayoutManager (5 columns)
        copyGrid.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = copyDestinationsAdapter
        }
        moveGrid.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = moveDestinationsAdapter
        }
        
        // Setup header click for expand/collapse
        copyHeader.setOnClickListener {
            toggleCopyPanel(copyGrid, copyExpandIcon)
        }
        moveHeader.setOnClickListener {
            toggleMovePanel(moveGrid, moveExpandIcon)
        }
    }

    /**
     * Set the listener for destination clicks.
     */
    fun setOnDestinationClickListener(listener: OnDestinationClickListener) {
        this.listener = listener
    }

    /**
     * Set the list of destinations to display.
     */
    fun setDestinations(destinations: List<Destination>) {
        this.destinations.clear()
        this.destinations.addAll(destinations)
        copyDestinationsAdapter?.submitList(destinations.toList())
        moveDestinationsAdapter?.submitList(destinations.toList())
    }

    /**
     * Show the panel with slide-up animation.
     */
    fun show(operationType: OperationType) {
        currentOperationType = operationType
        visibility = View.VISIBLE
        
        when (operationType) {
            OperationType.COPY -> {
                copyPanelView?.visibility = View.VISIBLE
                movePanelView?.visibility = View.GONE
            }
            OperationType.MOVE -> {
                copyPanelView?.visibility = View.GONE
                movePanelView?.visibility = View.VISIBLE
            }
        }
        
        // Animate slide up
        translationY = height.toFloat()
        animate()
            .translationY(0f)
            .setDuration(250)
            .start()
        
        Timber.d("Showing $operationType panel")
    }

    /**
     * Hide the panel with slide-down animation.
     */
    fun hide() {
        animate()
            .translationY(height.toFloat())
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationY = 0f
                }
            })
            .start()
        
        Timber.d("Hiding panel")
    }

    /**
     * Toggle between copy and move panels.
     */
    fun toggleOperationType() {
        currentOperationType = when (currentOperationType) {
            OperationType.COPY -> OperationType.MOVE
            OperationType.MOVE -> OperationType.COPY
        }
        show(currentOperationType)
    }

    /**
     * Get the current operation type.
     */
    fun getCurrentOperationType(): OperationType = currentOperationType

    private fun toggleCopyPanel(grid: RecyclerView, expandIcon: ImageView) {
        isCopyPanelExpanded = !isCopyPanelExpanded
        grid.visibility = if (isCopyPanelExpanded) View.VISIBLE else View.GONE
        expandIcon.setImageResource(
            if (isCopyPanelExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )
    }

    private fun toggleMovePanel(grid: RecyclerView, expandIcon: ImageView) {
        isMovePanelExpanded = !isMovePanelExpanded
        grid.visibility = if (isMovePanelExpanded) View.VISIBLE else View.GONE
        expandIcon.setImageResource(
            if (isMovePanelExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )
    }

    /**
     * Adapter for the destination grid.
     */
    private class DestinationsAdapter(
        private val operationType: OperationType,
        private val onClick: (Destination, OperationType) -> Unit
    ) : RecyclerView.Adapter<DestinationsAdapter.ViewHolder>() {

        private val items = mutableListOf<Destination>()

        fun submitList(destinations: List<Destination>) {
            items.clear()
            items.addAll(destinations)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_destination_button, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val destination = items[position]
            holder.bind(destination)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.destinationIcon)
            private val name: TextView = itemView.findViewById(R.id.destinationName)

            fun bind(destination: Destination) {
                icon.setImageResource(destination.iconRes)
                name.text = destination.name
                itemView.setOnClickListener {
                    onClick(destination, operationType)
                }
            }
        }
    }
}
