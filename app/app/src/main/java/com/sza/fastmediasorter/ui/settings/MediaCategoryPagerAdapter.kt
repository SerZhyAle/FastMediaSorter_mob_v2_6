package com.sza.fastmediasorter.ui.settings

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sza.fastmediasorter.ui.settings.media.AudioSettingsFragment
import com.sza.fastmediasorter.ui.settings.media.DocumentsSettingsFragment
import com.sza.fastmediasorter.ui.settings.media.ImagesSettingsFragment
import com.sza.fastmediasorter.ui.settings.media.OtherMediaSettingsFragment
import com.sza.fastmediasorter.ui.settings.media.VideoSettingsFragment

/**
 * Adapter for Media settings sub-tabs.
 * Contains 5 fragments: Images, Video, Audio, Documents, Other.
 */
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
