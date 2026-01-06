package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sza.fastmediasorter.databinding.FragmentSettingsMediaBinding
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Media settings fragment placeholder.
 * Will contain nested tabs for Images/Videos/Audio/Documents/Other.
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
        
        // Placeholder - full implementation in future sprint
        binding.tvPlaceholder.text = "Media settings coming soon.\n\nWill include:\n• Image settings\n• Video settings\n• Audio settings\n• Document settings"
    }
}
