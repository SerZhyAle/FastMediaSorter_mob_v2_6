package com.sza.fastmediasorter.ui.resource

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityAddResourceBinding
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity for adding new resources (local folders, network shares, cloud storage).
 */
@AndroidEntryPoint
class AddResourceActivity : BaseActivity<ActivityAddResourceBinding>() {

    private val viewModel: AddResourceViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            viewModel.onFolderSelected(uri)
        }
    }

    override fun getViewBinding() = ActivityAddResourceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupToolbar()
        setupClickListeners()
        observeEvents()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // Local folder
        binding.cardLocalFolder.setOnClickListener {
            openFolderPicker()
        }

        // Network shares (not yet implemented)
        binding.cardSmb.setOnClickListener {
            showNotYetImplemented(ResourceType.SMB)
        }
        
        binding.cardSftp.setOnClickListener {
            showNotYetImplemented(ResourceType.SFTP)
        }
        
        binding.cardFtp.setOnClickListener {
            showNotYetImplemented(ResourceType.FTP)
        }

        // Cloud storage (not yet implemented)
        binding.cardGoogleDrive.setOnClickListener {
            showNotYetImplemented(ResourceType.GOOGLE_DRIVE)
        }
        
        binding.cardOneDrive.setOnClickListener {
            showNotYetImplemented(ResourceType.ONEDRIVE)
        }
        
        binding.cardDropbox.setOnClickListener {
            showNotYetImplemented(ResourceType.DROPBOX)
        }
    }

    private fun openFolderPicker() {
        try {
            folderPickerLauncher.launch(null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open folder picker")
            Snackbar.make(binding.root, R.string.error_opening_folder_picker, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showNotYetImplemented(type: ResourceType) {
        val message = getString(R.string.feature_not_yet_implemented, type.name)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
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

    private fun handleEvent(event: AddResourceEvent) {
        when (event) {
            is AddResourceEvent.ResourceAdded -> {
                Timber.d("Resource added: ${event.resourceId}")
                setResult(Activity.RESULT_OK)
                finish()
            }
            is AddResourceEvent.ShowError -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
            is AddResourceEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
