package com.sza.fastmediasorter.ui.cloudfolders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.sza.fastmediasorter.data.cloud.AuthResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Activity for picking folders from Google Drive.
 * Handles Google OAuth2 sign-in and folder navigation.
 */
@AndroidEntryPoint
class GoogleDriveFolderPickerActivity : BaseCloudFolderPickerActivity() {

    private val viewModel: GoogleDriveFolderPickerViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.resultCode, result.data)
    }

    companion object {
        fun createIntent(context: Context, isDestination: Boolean = false): Intent {
            return Intent(context, GoogleDriveFolderPickerActivity::class.java).apply {
                putExtra(EXTRA_IS_DESTINATION, isDestination)
            }
        }
    }

    override fun getViewModel(): BaseCloudFolderPickerViewModel = viewModel
    
    override fun getProviderName(): String = "Google Drive"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Try to restore from stored credentials
        viewModel.tryRestoreSession()
    }

    override fun startAuthentication() {
        signInLauncher.launch(viewModel.getSignInIntent())
    }

    private fun handleSignInResult(resultCode: Int, data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            // Handle sign-in result in coroutine
            runBlocking {
                when (val result = viewModel.handleSignInResult(account)) {
                    is AuthResult.Success -> {
                        viewModel.onAuthenticationSuccess(result.accountName, result.credentialsJson)
                    }
                    is AuthResult.Error -> {
                        handleEvent(CloudFolderEvent.ShowError(result.message))
                    }
                    is AuthResult.Cancelled -> {
                        // User cancelled, stay on screen
                    }
                }
            }
        } catch (e: ApiException) {
            Timber.e(e, "Google Sign-In failed with status code: ${e.statusCode}")
            handleEvent(CloudFolderEvent.ShowError("Google Sign-In failed: ${e.message}"))
        }
    }
}
