package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sza.fastmediasorter.databinding.FragmentSettingsDestinationsBinding
import com.sza.fastmediasorter.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Destinations settings fragment placeholder.
 * Will contain quick move/copy destination management.
 */
@AndroidEntryPoint
class DestinationsSettingsFragment : BaseFragment<FragmentSettingsDestinationsBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsDestinationsBinding =
        FragmentSettingsDestinationsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("DestinationsSettingsFragment created")
        
        // Placeholder - full implementation in future sprint
        binding.tvPlaceholder.text = "Destinations settings coming soon.\n\nWill include:\n• Quick destinations list\n• Drag-to-reorder\n• Color markers\n• Add from resources"
    }
}
