package com.sza.fastmediasorter.ui.cloudfolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for picking folders from OneDrive.
 * Handles Microsoft MSAL authentication and folder navigation.
 */
@AndroidEntryPoint
class OneDriveFolderPickerActivity : BaseCloudFolderPickerActivity() {

    private val viewModel: OneDriveFolderPickerViewModel by viewModels()

    companion object {
        fun createIntent(context: Context, isDestination: Boolean = false): Intent {
            return Intent(context, OneDriveFolderPickerActivity::class.java).apply {
                putExtra(EXTRA_IS_DESTINATION, isDestination)
            }
        }
    }

    override fun getViewModel(): BaseCloudFolderPickerViewModel = viewModel
    
    override fun getProviderName(): String = "OneDrive"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Try to restore from stored credentials
        viewModel.tryRestoreSession()
    }

    override fun startAuthentication() {
        // MSAL handles the activity internally
        viewModel.signIn(this)
    }
}
