package com.sza.fastmediasorter.ui.browse

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Dialog for selecting a destination folder for move/copy operations.
 */
@AndroidEntryPoint
class DestinationPickerDialog : DialogFragment() {

    private val viewModel: DestinationPickerViewModel by viewModels()

    var onDestinationSelected: ((Resource) -> Unit)? = null
    var isMoveOperation: Boolean = true

    companion object {
        const val TAG = "DestinationPickerDialog"
        private const val ARG_IS_MOVE = "is_move"
        private const val ARG_SELECTED_FILES = "selected_files"

        fun newInstance(selectedFiles: List<String>, isMove: Boolean): DestinationPickerDialog {
            return DestinationPickerDialog().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_SELECTED_FILES, ArrayList(selectedFiles))
                    putBoolean(ARG_IS_MOVE, isMove)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isMoveOperation = arguments?.getBoolean(ARG_IS_MOVE, true) ?: true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = DestinationListAdapter { resource ->
            onDestinationSelected?.invoke(resource)
            dismiss()
        }

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_destination_picker, null)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.destinationList)
        val emptyView = view.findViewById<TextView>(R.id.emptyView)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destinations.collect { destinations ->
                    adapter.submitList(destinations)
                    emptyView.visibility = if (destinations.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (destinations.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        val title = if (isMoveOperation) R.string.select_destination_move else R.string.select_destination_copy

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(view)
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }
}

/**
 * Adapter for displaying destination resources.
 */
class DestinationListAdapter(
    private val onItemClick: (Resource) -> Unit
) : RecyclerView.Adapter<DestinationListAdapter.ViewHolder>() {

    private var items: List<Resource> = emptyList()

    fun submitList(list: List<Resource>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination_picker, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(
        itemView: View,
        private val onItemClick: (Resource) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.destinationName)
        private val pathView: TextView = itemView.findViewById(R.id.destinationPath)
        private val colorView: View = itemView.findViewById(R.id.destinationColor)

        fun bind(resource: Resource) {
            nameView.text = resource.name
            pathView.text = resource.path
            
            // Use destination color (stored as Int)
            val color = resource.destinationColor
            if (color != 0) {
                colorView.setBackgroundColor(color)
            } else {
                colorView.setBackgroundColor(android.graphics.Color.GRAY)
            }

            itemView.setOnClickListener { onItemClick(resource) }
        }
    }
}
