package com.sza.fastmediasorter.ui.resource

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityEditResourceBinding
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity for editing an existing resource's properties.
 * Allows changing name, display mode, sort mode, and destination status.
 */
@AndroidEntryPoint
class EditResourceActivity : BaseActivity<ActivityEditResourceBinding>() {

    companion object {
        const val EXTRA_RESOURCE_ID = "EXTRA_RESOURCE_ID"

        /**
         * Creates an Intent to start EditResourceActivity.
         *
         * @param context Source context
         * @param resourceId ID of the resource to edit
         */
        fun createIntent(context: Context, resourceId: Long): Intent {
            return Intent(context, EditResourceActivity::class.java).apply {
                putExtra(EXTRA_RESOURCE_ID, resourceId)
            }
        }
    }

    private val viewModel: EditResourceViewModel by viewModels()

    override fun getViewBinding() = ActivityEditResourceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupInputListeners()
        setupDropdowns()
        setupSwitches()
        observeUiState()
        observeEvents()

        // Load resource if ID passed via intent
        if (savedInstanceState == null) {
            val resourceId = intent.getLongExtra(EXTRA_RESOURCE_ID, -1)
            if (resourceId != -1L) {
                viewModel.loadResource(resourceId)
            } else {
                Timber.e("No resource ID provided")
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    viewModel.saveResource()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupInputListeners() {
        binding.editName.doAfterTextChanged { text ->
            viewModel.onNameChanged(text?.toString() ?: "")
        }
    }

    private fun setupDropdowns() {
        // Sort mode dropdown
        val sortModes = SortMode.entries.map { getSortModeLabel(it) }
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortModes)
        binding.dropdownSortMode.setAdapter(sortAdapter)
        binding.dropdownSortMode.setOnItemClickListener { _, _, position, _ ->
            viewModel.onSortModeChanged(SortMode.entries[position])
        }

        // Display mode dropdown
        val displayModes = DisplayMode.entries.map { getDisplayModeLabel(it) }
        val displayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, displayModes)
        binding.dropdownDisplayMode.setAdapter(displayAdapter)
        binding.dropdownDisplayMode.setOnItemClickListener { _, _, position, _ ->
            viewModel.onDisplayModeChanged(DisplayMode.entries[position])
        }
    }

    private fun setupSwitches() {
        binding.switchDestination.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onDestinationChanged(isChecked)
            // Show/hide destination options
            binding.destinationOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchWorkWithAllFiles.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onWorkWithAllFilesChanged(isChecked)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: EditResourceUiState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        // Only update fields if they haven't been edited by user
        if (!state.isEdited) {
            binding.editName.setText(state.name)
            binding.textPath.text = state.path
            binding.textResourceType.text = state.resourceTypeName
            
            // Set dropdown values
            binding.dropdownSortMode.setText(getSortModeLabel(state.sortMode), false)
            binding.dropdownDisplayMode.setText(getDisplayModeLabel(state.displayMode), false)
            
            // Set switches
            binding.switchDestination.isChecked = state.isDestination
            binding.switchWorkWithAllFiles.isChecked = state.workWithAllFiles
            binding.destinationOptionsLayout.visibility = 
                if (state.isDestination) View.VISIBLE else View.GONE
        }

        // Save button state
        binding.toolbar.menu.findItem(R.id.action_save)?.isEnabled = state.canSave

        // Error states
        binding.inputLayoutName.error = state.nameError
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: EditResourceEvent) {
        when (event) {
            is EditResourceEvent.ResourceSaved -> {
                Timber.d("Resource saved")
                setResult(Activity.RESULT_OK)
                finish()
            }
            is EditResourceEvent.ResourceDeleted -> {
                Timber.d("Resource deleted")
                setResult(Activity.RESULT_OK)
                finish()
            }
            is EditResourceEvent.ShowError -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
            is EditResourceEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is EditResourceEvent.NavigateBack -> {
                finish()
            }
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_resource_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteResource()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun getSortModeLabel(sortMode: SortMode): String {
        return when (sortMode) {
            SortMode.NAME_ASC -> getString(R.string.sort_name_asc)
            SortMode.NAME_DESC -> getString(R.string.sort_name_desc)
            SortMode.DATE_ASC -> getString(R.string.sort_date_asc)
            SortMode.DATE_DESC -> getString(R.string.sort_date_desc)
            SortMode.SIZE_ASC -> getString(R.string.sort_size_asc)
            SortMode.SIZE_DESC -> getString(R.string.sort_size_desc)
        }
    }

    private fun getDisplayModeLabel(displayMode: DisplayMode): String {
        return when (displayMode) {
            DisplayMode.LIST -> getString(R.string.view_list)
            DisplayMode.GRID -> getString(R.string.view_grid)
        }
    }
}
