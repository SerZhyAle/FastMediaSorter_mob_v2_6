package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsDestinationsBinding
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections

/**
 * Destinations settings fragment.
 * Manages quick move/copy destination folders.
 */
@AndroidEntryPoint
class DestinationsSettingsFragment : BaseFragment<FragmentSettingsDestinationsBinding>() {

    private val viewModel: DestinationsSettingsViewModel by viewModels()
    private lateinit var adapter: DestinationAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private val currentList = mutableListOf<Resource>()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsDestinationsBinding =
        FragmentSettingsDestinationsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("DestinationsSettingsFragment created")
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = DestinationAdapter(
            onRemoveClick = { resource -> confirmRemoveDestination(resource) },
            onColorClick = { resource -> showColorPicker(resource) },
            onStartDrag = { viewHolder -> itemTouchHelper?.startDrag(viewHolder) }
        )

        binding.recyclerDestinations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DestinationsSettingsFragment.adapter
        }

        // Setup drag and drop
        val touchCallback = DestinationItemTouchCallback(
            onMoved = { fromPos, toPos ->
                Collections.swap(currentList, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)
            },
            onDragEnded = {
                viewModel.updateDestinationOrder(currentList.toList())
            }
        )
        itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper?.attachToRecyclerView(binding.recyclerDestinations)
    }

    private fun setupFab() {
        binding.fabAddDestination.setOnClickListener {
            showAddDestinationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.destinations.collectLatest { destinations ->
                        currentList.clear()
                        currentList.addAll(destinations)
                        adapter.submitList(destinations.toList())
                        updateEmptyState(destinations.isEmpty())
                    }
                }

                launch {
                    viewModel.events.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerDestinations.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun handleEvent(event: DestinationsEvent) {
        when (event) {
            is DestinationsEvent.DestinationAdded -> {
                Snackbar.make(binding.root, getString(R.string.destination_added, event.name), Snackbar.LENGTH_SHORT).show()
            }
            is DestinationsEvent.DestinationRemoved -> {
                Snackbar.make(binding.root, getString(R.string.destination_removed, event.name), Snackbar.LENGTH_SHORT).show()
            }
            is DestinationsEvent.Error -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddDestinationDialog() {
        val available = viewModel.availableResources.value
        if (available.isEmpty()) {
            Snackbar.make(binding.root, R.string.no_resources_to_add, Snackbar.LENGTH_SHORT).show()
            return
        }

        val names = available.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_destination)
            .setItems(names) { _, which ->
                viewModel.addDestination(available[which])
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmRemoveDestination(resource: Resource) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remove_destination)
            .setMessage(getString(R.string.confirm_remove_destination, resource.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.removeDestination(resource)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showColorPicker(resource: Resource) {
        // Simple color selection for now - can be enhanced with a proper color picker
        val colors = intArrayOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFF44336.toInt(), // Red
            0xFFFF9800.toInt(), // Orange
            0xFF9C27B0.toInt(), // Purple
            0xFF00BCD4.toInt(), // Cyan
            0xFFE91E63.toInt(), // Pink
            0xFF795548.toInt()  // Brown
        )
        val colorNames = arrayOf("Green", "Blue", "Red", "Orange", "Purple", "Cyan", "Pink", "Brown")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_color)
            .setItems(colorNames) { _, which ->
                viewModel.updateDestinationColor(resource, colors[which])
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
