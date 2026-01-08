package com.sza.fastmediasorter.ui.cloudfolders

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.databinding.ActivityCloudFolderPickerBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base activity for cloud folder pickers.
 * Provides common UI setup and event handling.
 * 
 * Subclasses implement:
 * - getViewModel() - return provider-specific ViewModel
 * - getProviderName() - display name for UI
 * - startAuthentication() - launch provider-specific OAuth flow
 */
abstract class BaseCloudFolderPickerActivity : BaseActivity<ActivityCloudFolderPickerBinding>() {

    protected abstract fun getViewModel(): BaseCloudFolderPickerViewModel
    protected abstract fun getProviderName(): String
    protected abstract fun startAuthentication()

    private lateinit var adapter: CloudFolderAdapter

    protected var isDestination: Boolean = false
    protected var scanSubdirectories: Boolean = true
    
    companion object {
        const val EXTRA_IS_DESTINATION = "is_destination"
        const val RESULT_FOLDER_ID = "folder_id"
        const val RESULT_FOLDER_NAME = "folder_name"
        const val RESULT_FOLDER_PATH = "folder_path"
        const val RESULT_IS_DESTINATION = "is_destination"
        const val RESULT_SCAN_SUBDIRECTORIES = "scan_subdirectories"
    }

    override fun getViewBinding() = ActivityCloudFolderPickerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        isDestination = intent.getBooleanExtra(EXTRA_IS_DESTINATION, false)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getProviderName()
        binding.toolbar.setNavigationOnClickListener {
            if (!getViewModel().navigateBack()) {
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CloudFolderAdapter(
            onFolderClick = { folder ->
                // Navigate into folder on click
                getViewModel().navigateToFolder(folder)
            },
            onNavigateClick = { folder ->
                // Navigate into folder
                getViewModel().navigateToFolder(folder)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BaseCloudFolderPickerActivity)
            adapter = this@BaseCloudFolderPickerActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSelect.setOnClickListener {
            getViewModel().selectCurrentFolder(
                isDestination = isDestination,
                scanSubdirectories = binding.checkboxSubdirectories.isChecked
            )
        }

        binding.btnRetry.setOnClickListener {
            getViewModel().retry()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    getViewModel().uiState.collectLatest { state ->
                        updateUI(state)
                    }
                }
                
                launch {
                    getViewModel().events.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUI(state: CloudFolderUiState) {
        // Loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Error state
        binding.errorLayout.visibility = if (state.hasError) View.VISIBLE else View.GONE
        binding.tvError.text = state.error
        
        // Empty state
        binding.emptyLayout.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        
        // Content
        val showContent = !state.isLoading && !state.hasError && !state.isEmpty
        binding.recyclerView.visibility = if (showContent) View.VISIBLE else View.GONE
        binding.btnSelect.visibility = if (showContent || state.isEmpty) View.VISIBLE else View.GONE
        
        // Breadcrumbs
        updateBreadcrumbs(state)
        
        // Folder list
        adapter.submitList(state.folders)
        
        // Subdirectories checkbox
        binding.checkboxSubdirectories.visibility = if (!isDestination) View.VISIBLE else View.GONE
    }

    private fun updateBreadcrumbs(state: CloudFolderUiState) {
        val breadcrumbs = state.breadcrumbs
        if (breadcrumbs.isEmpty()) {
            binding.tvBreadcrumb.text = getProviderName()
        } else {
            binding.tvBreadcrumb.text = breadcrumbs.joinToString(" > ") { it.name }
        }
    }

    protected open fun handleEvent(event: CloudFolderEvent) {
        when (event) {
            is CloudFolderEvent.ShowError -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
            is CloudFolderEvent.FolderSelected -> {
                val resultIntent = Intent().apply {
                    putExtra(RESULT_FOLDER_ID, event.folderId)
                    putExtra(RESULT_FOLDER_NAME, event.folderName)
                    putExtra(RESULT_FOLDER_PATH, event.folderPath)
                    putExtra(RESULT_IS_DESTINATION, event.isDestination)
                    putExtra(RESULT_SCAN_SUBDIRECTORIES, event.scanSubdirectories)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            is CloudFolderEvent.AuthenticationRequired -> {
                startAuthentication()
            }
            is CloudFolderEvent.NavigateBack -> {
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!getViewModel().navigateBack()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
