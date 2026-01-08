package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.sza.fastmediasorter.databinding.ActivitySettingsBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings screen with tabbed navigation for different setting categories.
 *
 * Tabs:
 * - General: Language, theme, display preferences
 * - Media: Image/Video/Audio/Document settings
 * - Playback: Slideshow, touch zones, video playback
 * - Destinations: Quick move/copy targets
 */
@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModels()

    private val tabTitles = listOf("General", "Media", "Playback", "Destinations", "Network")

    override fun getViewBinding(): ActivitySettingsBinding =
        ActivitySettingsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("SettingsActivity created")
        
        setupToolbar()
        setupViewPager()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = SettingsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles.getOrElse(position) { "" }
        }.attach()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle any global settings state updates
                    Timber.d("Settings state updated: $state")
                }
            }
        }
    }
}
