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
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.cloudfolders.BaseCloudFolderPickerActivity
import com.sza.fastmediasorter.ui.cloudfolders.DropboxFolderPickerActivity
import com.sza.fastmediasorter.ui.cloudfolders.GoogleDriveFolderPickerActivity
import com.sza.fastmediasorter.ui.cloudfolders.OneDriveFolderPickerActivity
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

    private val googleDriveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { handleCloudFolderResult(it, ResourceType.GOOGLE_DRIVE) }
        }
    }

    private val oneDriveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { handleCloudFolderResult(it, ResourceType.ONEDRIVE) }
        }
    }

    private val dropboxLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { handleCloudFolderResult(it, ResourceType.DROPBOX) }
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
            showNetworkCredentialsDialog(ResourceType.SMB)
        }
        
        binding.cardSftp.setOnClickListener {
            showNetworkCredentialsDialog(ResourceType.SFTP)
        }
        
        binding.cardFtp.setOnClickListener {
            showNetworkCredentialsDialog(ResourceType.FTP)
        }

        // Cloud storage
        binding.cardGoogleDrive.setOnClickListener {
            launchGoogleDriveFolderPicker()
        }
        
        binding.cardOneDrive.setOnClickListener {
            launchOneDriveFolderPicker()
        }
        
        binding.cardDropbox.setOnClickListener {
            launchDropboxFolderPicker()
        }
    }

    private fun launchGoogleDriveFolderPicker() {
        val intent = GoogleDriveFolderPickerActivity.createIntent(this, isDestination = false)
        googleDriveLauncher.launch(intent)
    }

    private fun launchOneDriveFolderPicker() {
        val intent = OneDriveFolderPickerActivity.createIntent(this, isDestination = false)
        oneDriveLauncher.launch(intent)
    }

    private fun launchDropboxFolderPicker() {
        val intent = DropboxFolderPickerActivity.createIntent(this, isDestination = false)
        dropboxLauncher.launch(intent)
    }

    private fun handleCloudFolderResult(data: Intent, resourceType: ResourceType) {
        val folderId = data.getStringExtra(BaseCloudFolderPickerActivity.RESULT_FOLDER_ID) ?: return
        val folderName = data.getStringExtra(BaseCloudFolderPickerActivity.RESULT_FOLDER_NAME) ?: "Unknown"
        val folderPath = data.getStringExtra(BaseCloudFolderPickerActivity.RESULT_FOLDER_PATH) ?: ""
        val isDestination = data.getBooleanExtra(BaseCloudFolderPickerActivity.RESULT_IS_DESTINATION, false)
        val scanSubdirectories = data.getBooleanExtra(BaseCloudFolderPickerActivity.RESULT_SCAN_SUBDIRECTORIES, true)

        Timber.d("Cloud folder selected: type=$resourceType, id=$folderId, name=$folderName, path=$folderPath")
        
        viewModel.onCloudFolderSelected(
            resourceType = resourceType,
            folderId = folderId,
            folderName = folderName,
            folderPath = folderPath,
            isDestination = isDestination,
            scanSubdirectories = scanSubdirectories
        )
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

    private fun showNetworkCredentialsDialog(resourceType: ResourceType) {
        val networkType = when (resourceType) {
            ResourceType.SMB -> NetworkType.SMB
            ResourceType.SFTP -> NetworkType.SFTP
            ResourceType.FTP -> NetworkType.FTP
            else -> return
        }

        val dialog = NetworkCredentialsDialog.newInstance(networkType)
        dialog.setOnCredentialsSubmittedListener { credentialId, type, name, server, port, 
            username, password, domain, shareName, useSshKey, sshKeyPath ->
            
            viewModel.onNetworkCredentialsEntered(
                credentialId = credentialId,
                type = type,
                name = name,
                server = server,
                port = port,
                username = username,
                password = password,
                domain = domain,
                shareName = shareName,
                useSshKey = useSshKey,
                sshKeyPath = sshKeyPath
            )
            
            dialog.dismiss()
        }
        dialog.show(supportFragmentManager, "NetworkCredentialsDialog")
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
            is AddResourceEvent.ConnectionTesting -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is AddResourceEvent.ConnectionSuccess -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            is AddResourceEvent.ConnectionFailed -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
            }
            is AddResourceEvent.ShowConfig -> {
                showConfigDialog(event.name, event.path)
            }
        }
    }
    
    private fun showConfigDialog(name: String, path: String) {
        val dialog = ResourceConfigBottomSheet.newInstance(name, path)
        dialog.setOnConfirmListener { isReadOnly, isDestination, scanAll, pinCode, supportedMediaTypes ->
            viewModel.onConfigConfirmed(
                name = name,
                path = path,
                isReadOnly = isReadOnly,
                isDestination = isDestination,
                workWithAllFiles = scanAll,
                pinCode = pinCode,
                supportedMediaTypes = supportedMediaTypes
            )
        }
        dialog.show(supportFragmentManager, "ResourceConfigBottomSheet")
    }
}
