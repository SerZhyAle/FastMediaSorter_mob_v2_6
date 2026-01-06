package com.sza.fastmediasorter.ui.settings

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sza.fastmediasorter.ui.settings.fragments.AudioSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.DestinationsSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.DocumentsSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.ImagesSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.OtherMediaSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.PlaybackSettingsFragment
import com.sza.fastmediasorter.ui.settings.fragments.VideoSettingsFragment

class MediaCategoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ImagesSettingsFragment()
            1 -> VideoSettingsFragment()
            2 -> AudioSettingsFragment()
            3 -> DocumentsSettingsFragment()
            4 -> OtherMediaSettingsFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
