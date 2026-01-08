package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsMediaBinding
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Media settings fragment with nested tabs.
 * Contains 5 sub-tabs: Images, Video, Audio, Documents, Other.
 */
@AndroidEntryPoint
class MediaSettingsFragment : BaseFragment<FragmentSettingsMediaBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsMediaBinding =
        FragmentSettingsMediaBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MediaSettingsFragment created")
        
        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = MediaCategoryPagerAdapter(this)
        binding.viewPagerMedia.adapter = adapter
        
        TabLayoutMediator(binding.tabLayoutMedia, binding.viewPagerMedia) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.media_category_images)
                1 -> getString(R.string.media_category_video)
                2 -> getString(R.string.media_category_audio)
                3 -> getString(R.string.media_category_documents)
                4 -> getString(R.string.media_category_other)
                else -> ""
            }
        }.attach()
    }
}
