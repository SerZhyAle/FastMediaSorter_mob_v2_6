package com.sza.fastmediasorter.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityWelcomeBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.main.MainActivity
import com.sza.fastmediasorter.util.PermissionsHandler
import dagger.hilt.android.AndroidEntryPoint

/**
 * Welcome/Onboarding activity with 4-page tutorial.
 * Handles permission requests on the final step.
 */
@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    
    override fun getViewBinding() = ActivityWelcomeBinding.inflate(layoutInflater)
    
    private val adapter = WelcomePagerAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // If permissions already granted, skip to MainActivity
        if (PermissionsHandler.hasAllPermissions(this)) {
            navigateToMain()
            return
        }
        
        setupViewPager()
        setupButtons()
        setupIndicators()
        setCurrentIndicator(0)
    }
    
    private fun setupViewPager() {
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                updateButtons(position)
            }
        })
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < adapter.itemCount) {
                binding.viewPager.currentItem += 1
            }
        }
        
        binding.btnPrevious.setOnClickListener {
            if (binding.viewPager.currentItem - 1 >= 0) {
                binding.viewPager.currentItem -= 1
            }
        }

        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }

        binding.btnFinish.setOnClickListener {
            requestPermissions()
        }
    }

    private fun updateButtons(position: Int) {
        val lastPage = adapter.itemCount - 1
        
        // Previous button visibility
        binding.btnPrevious.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        
        // Next/Finish button logic
        if (position == lastPage) {
            binding.btnNext.visibility = View.GONE
            binding.btnFinish.visibility = View.VISIBLE
        } else {
            binding.btnNext.visibility = View.VISIBLE
            binding.btnFinish.visibility = View.GONE
        }
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(adapter.itemCount)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        
        for (i in indicators.indices) {
            indicators[i] = ImageView(this)
            indicators[i]?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
            )
            indicators[i]?.layoutParams = layoutParams
            binding.layoutIndicator.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = binding.layoutIndicator.childCount
        for (i in 0 until childCount) {
            val imageView = binding.layoutIndicator.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.indicator_active)
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
                )
            }
        }
    }
    
    private fun requestPermissions() {
        if (PermissionsHandler.shouldShowRationale(this)) {
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
        // Only if we are on the last page or user manually returns from settings
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
        // Here we could save a flag "is_welcome_completed" in SharedPrefs if needed for logic other than permissions
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
