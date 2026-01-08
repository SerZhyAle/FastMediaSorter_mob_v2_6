package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentSettingsNetworkBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Network settings fragment.
 * Contains: Background sync toggle, sync interval, manual sync button.
 */
@AndroidEntryPoint
class NetworkSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsNetworkBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("NetworkSettingsFragment created")
        
        setupViews()
        observeSettings()
    }

    private fun setupViews() {
        // Background sync toggle
        binding.switchEnableBackgroundSync.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intervalHours = binding.sliderSyncInterval.value.toLong()
                Timber.i("NetworkSettingsFragment: Background sync enabled (interval=$intervalHours hours)")
                showMessage(getString(R.string.background_sync_enabled))
                // TODO: Call scheduleNetworkSyncUseCase(intervalHours = intervalHours)
            } else {
                Timber.i("NetworkSettingsFragment: Background sync disabled")
                showMessage(getString(R.string.background_sync_disabled))
                // TODO: Call scheduleNetworkSyncUseCase.cancel()
            }
        }
        
        // Sync interval slider
        binding.sliderSyncInterval.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val hours = value.toInt()
                binding.tvSyncIntervalValue.text = resources.getQuantityString(
                    R.plurals.sync_interval_hours,
                    hours,
                    hours
                )
                
                // Reschedule if sync is enabled
                if (binding.switchEnableBackgroundSync.isChecked) {
                    Timber.d("NetworkSettingsFragment: Sync interval updated to $hours hours")
                    // TODO: Call scheduleNetworkSyncUseCase(intervalHours = value.toLong())
                }
            }
        }
        
        // Initialize interval display
        val hours = binding.sliderSyncInterval.value.toInt()
        binding.tvSyncIntervalValue.text = resources.getQuantityString(
            R.plurals.sync_interval_hours,
            hours,
            hours
        )
        
        // Manual sync button
        binding.btnSyncNow.setOnClickListener {
            performManualSync()
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Settings can be extended in future to store sync preferences
                // For now, using hardcoded defaults: enabled=true, interval=4h
            }
        }
    }

    private fun performManualSync() {
        binding.btnSyncNow.isEnabled = false
        binding.tvSyncStatus.text = getString(R.string.sync_status_in_progress)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // TODO: Implement actual sync via SyncNetworkResourcesUseCase
                withContext(Dispatchers.IO) {
                    // Simulate sync delay
                    kotlinx.coroutines.delay(2000)
                }
                
                binding.tvSyncStatus.text = getString(R.string.sync_status_completed, 0)
                showMessage(getString(R.string.sync_completed_successfully, 0))
                Timber.i("NetworkSettingsFragment: Manual sync completed")
            } catch (e: Exception) {
                binding.tvSyncStatus.text = getString(R.string.sync_status_failed)
                showMessage(getString(R.string.sync_failed, e.message ?: "Unknown error"))
                Timber.e(e, "NetworkSettingsFragment: Manual sync failed")
            } finally {
                binding.btnSyncNow.isEnabled = true
            }
        }
    }

    private fun showMessage(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
