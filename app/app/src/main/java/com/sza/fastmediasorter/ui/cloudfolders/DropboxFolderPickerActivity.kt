package com.sza.fastmediasorter.ui.cloudfolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for picking folders from Dropbox.
 * Handles Dropbox OAuth PKCE authentication and folder navigation.
 */
@AndroidEntryPoint
class DropboxFolderPickerActivity : BaseCloudFolderPickerActivity() {

    private val viewModel: DropboxFolderPickerViewModel by viewModels()

    private var oAuthFlowInProgress = false

    companion object {
        fun createIntent(context: Context, isDestination: Boolean = false): Intent {
            return Intent(context, DropboxFolderPickerActivity::class.java).apply {
                putExtra(EXTRA_IS_DESTINATION, isDestination)
            }
        }
    }

    override fun getViewModel(): BaseCloudFolderPickerViewModel = viewModel
    
    override fun getProviderName(): String = "Dropbox"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Try to restore from stored credentials
        viewModel.tryRestoreSession()
    }

    override fun onResume() {
        super.onResume()
        
        // Check if returning from OAuth flow
        if (oAuthFlowInProgress) {
            oAuthFlowInProgress = false
            viewModel.finishOAuthFlow()
        }
    }

    override fun startAuthentication() {
        oAuthFlowInProgress = true
        viewModel.startOAuthFlow(this)
    }
}
