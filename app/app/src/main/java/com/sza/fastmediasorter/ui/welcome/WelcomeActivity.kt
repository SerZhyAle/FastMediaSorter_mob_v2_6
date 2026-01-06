package com.sza.fastmediasorter.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityWelcomeBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.main.MainActivity
import com.sza.fastmediasorter.util.PermissionsHandler
import dagger.hilt.android.AndroidEntryPoint

/**
 * Welcome/Onboarding activity that handles permission requests.
 * Shows on first launch or when permissions are missing.
 */
@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    
    override fun getViewBinding() = ActivityWelcomeBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // If permissions already granted, skip to MainActivity
        if (PermissionsHandler.hasAllPermissions(this)) {
            navigateToMain()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.buttonGrantPermission.setOnClickListener {
            requestPermissions()
        }
    }
    
    private fun requestPermissions() {
        if (PermissionsHandler.shouldShowRationale(this)) {
            // Show rationale dialog
            showRationaleDialog()
        } else {
            PermissionsHandler.requestPermissions(this)
        }
    }
    
    private fun showRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                PermissionsHandler.requestPermissions(this)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied_title)
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Check permissions on resume (user may have granted in settings)
        if (PermissionsHandler.hasAllPermissions(this)) {
            navigateToMain()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (PermissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            navigateToMain()
        } else {
            // Check if user permanently denied
            if (!PermissionsHandler.shouldShowRationale(this)) {
                showSettingsDialog()
            }
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
