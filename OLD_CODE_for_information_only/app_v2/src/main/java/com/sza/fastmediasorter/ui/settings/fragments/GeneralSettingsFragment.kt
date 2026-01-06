package com.sza.fastmediasorter.ui.settings.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.core.util.LocaleHelper
import com.sza.fastmediasorter.databinding.FragmentSettingsGeneralBinding
import com.sza.fastmediasorter.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GeneralSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsGeneralBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by activityViewModels()
    
    // Lazy initialization of CalculateOptimalCacheSizeUseCase
    private val calculateOptimalCacheSizeUseCase by lazy {
        com.sza.fastmediasorter.domain.usecase.CalculateOptimalCacheSizeUseCase()
    }
    
    // Flag to prevent infinite loop when programmatically updating spinner
    private var isUpdatingSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsGeneralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVersionInfo()
        setupViews()
        observeData()
        checkAndSuggestOptimalCacheSize()
        setupGeneralLayouts()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        setupGeneralLayouts()
    }

    private fun setupGeneralLayouts() {
        val orientation = resources.configuration.orientation
        val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        // Helper to update layout params
        fun updateLayoutParams(view: android.view.View, isHorizontal: Boolean) {
            val params = view.layoutParams as android.widget.LinearLayout.LayoutParams
            if (isHorizontal) {
                params.width = 0
                params.weight = 1f
            } else {
                params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                params.weight = 0f
            }
            view.layoutParams = params
        }

        // Sync Container: Enable Sync + Interval + Sync Now
        binding.containerSync.orientation = if (isLandscape) android.widget.LinearLayout.HORIZONTAL else android.widget.LinearLayout.VERTICAL
        updateLayoutParams(binding.layoutEnableSync, isLandscape)
        updateLayoutParams(binding.layoutSyncControls, isLandscape)

        // Sleep + Favorites Container
        binding.containerSleepFavorites.orientation = if (isLandscape) android.widget.LinearLayout.HORIZONTAL else android.widget.LinearLayout.VERTICAL
        updateLayoutParams(binding.layoutEnableFavorites, isLandscape)
        updateLayoutParams(binding.layoutPreventSleep, isLandscape)

        // Confirm Delete + Move Container
        binding.containerConfirm.orientation = if (isLandscape) android.widget.LinearLayout.HORIZONTAL else android.widget.LinearLayout.VERTICAL
        updateLayoutParams(binding.layoutConfirmDelete, isLandscape)
        updateLayoutParams(binding.layoutConfirmMove, isLandscape)

        // Cache Container: Size limit + Auto + Clear + Size info
        binding.containerCache.orientation = if (isLandscape) android.widget.LinearLayout.HORIZONTAL else android.widget.LinearLayout.VERTICAL
        updateLayoutParams(binding.layoutCacheControls, isLandscape)
        updateLayoutParams(binding.layoutCacheActions, isLandscape)
    }
    
    private fun checkAndSuggestOptimalCacheSize() {
        // Only suggest on first install (when user hasn't modified cache size manually)
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = viewModel.settings.value
            if (!settings.isCacheSizeUserModified) {
                val optimalSizeMb = calculateOptimalCacheSizeUseCase()
                // Only show suggestion if current size differs from optimal
                if (settings.cacheSizeMb != optimalSizeMb) {
                    showOptimalCacheSizeSuggestion(optimalSizeMb)
                }
            }
        }
    }
    
    private fun showOptimalCacheSizeSuggestion(optimalSizeMb: Int) {
        val storageInfo = calculateOptimalCacheSizeUseCase.getStorageInfo()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.optimize_cache_title)
            .setMessage(getString(R.string.optimize_cache_message, optimalSizeMb, storageInfo))
            .setPositiveButton(R.string.apply) { _, _ ->
                showCacheSizeRestartDialog(optimalSizeMb, isUserModified = false)
            }
            .setNegativeButton(R.string.keep_current) { _, _ ->
                // User declined - mark as user-modified to prevent future suggestions
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(isCacheSizeUserModified = true))
            }
            .setCancelable(false)
            .show()
    }
    
    private fun setupVersionInfo() {
        val versionInfo = "${com.sza.fastmediasorter.BuildConfig.VERSION_NAME} | Build ${com.sza.fastmediasorter.BuildConfig.VERSION_CODE} | sza@ukr.net"
        binding.tvVersionInfo.text = versionInfo
    }

    private fun setupViews() {
        // Language Spinner
        val languages = resources.getStringArray(com.sza.fastmediasorter.R.array.languages)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
        
        binding.spinnerLanguage.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Skip if we're programmatically updating the spinner
                if (isUpdatingSpinner) return
                
                val newLanguageCode = when (position) {
                    0 -> "en"
                    1 -> "ru"
                    2 -> "uk"
                    else -> "en"
                }
                
                // Check if language actually changed compared to current settings
                val currentSettings = viewModel.settings.value
                if (newLanguageCode != currentSettings.language) {
                    // Update settings
                    viewModel.updateSettings(currentSettings.copy(language = newLanguageCode))
                    
                    // Show restart dialog
                    showRestartDialog(newLanguageCode)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        
        // Prevent Sleep
        binding.switchPreventSleep.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(preventSleep = isChecked))
        }

        // Enable Favorites
        binding.switchEnableFavorites.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(enableFavorites = isChecked))
        }
        
        // Small Controls
        binding.switchSmallControls.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(showSmallControls = isChecked))
        }
        
        // Safe Mode (Phase 2.1)
        binding.switchEnableSafeMode.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(enableSafeMode = isChecked))
            // Show/hide sub-options
            binding.layoutConfirmDelete.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.layoutConfirmMove.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.switchConfirmDelete.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(confirmDelete = isChecked))
        }
        
        binding.iconHelpSafeMode.setOnClickListener {
            com.sza.fastmediasorter.ui.dialog.TooltipDialog.show(
                requireContext(),
                R.string.tooltip_safe_mode_title,
                R.string.tooltip_safe_mode_message
            )
        }
        
        binding.switchConfirmMove.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(confirmMove = isChecked))
        }
        
        // Network Parallelism
        val parallelismOptions = arrayOf("1", "2", "4", "8", "12", "24")
        val parallelismAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, parallelismOptions)
        binding.actvNetworkParallelism.setAdapter(parallelismAdapter)
        
        binding.actvNetworkParallelism.setText(viewModel.settings.value.networkParallelism.toString(), false)
        
        binding.actvNetworkParallelism.setOnItemClickListener { _, _, position, _ ->
            if (isUpdatingSpinner) return@setOnItemClickListener
            val limit = parallelismOptions[position].toInt()
            val current = viewModel.settings.value
            if (current.networkParallelism != limit) {
                viewModel.updateSettings(current.copy(networkParallelism = limit))
                // Update manager immediately
                com.sza.fastmediasorter.data.network.ConnectionThrottleManager.setUserNetworkLimit(limit)
            }
        }
        
        // Handle manual input
        binding.actvNetworkParallelism.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isUpdatingSpinner) {
                val text = binding.actvNetworkParallelism.text.toString()
                val limit = text.toIntOrNull()
                if (limit != null && limit in 1..32) {
                    val current = viewModel.settings.value
                    if (current.networkParallelism != limit) {
                        viewModel.updateSettings(current.copy(networkParallelism = limit))
                        com.sza.fastmediasorter.data.network.ConnectionThrottleManager.setUserNetworkLimit(limit)
                    }
                } else {
                    // Invalid input, restore previous value
                    binding.actvNetworkParallelism.setText(viewModel.settings.value.networkParallelism.toString(), false)
                }
            }
        }
        
        // Cache Size Limit
        val cacheSizeOptions = arrayOf("512", "1024", "2048", "4096", "8192", "16384")
        val cacheSizeAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cacheSizeOptions)
        binding.actvCacheSizeLimit.setAdapter(cacheSizeAdapter)
        
        binding.actvCacheSizeLimit.setText(viewModel.settings.value.cacheSizeMb.toString(), false)
        
        binding.actvCacheSizeLimit.setOnItemClickListener { _, _, position, _ ->
            if (isUpdatingSpinner) return@setOnItemClickListener
            val sizeMb = cacheSizeOptions[position].toInt()
            val current = viewModel.settings.value
            if (current.cacheSizeMb != sizeMb) {
                // Show restart dialog
                showCacheSizeRestartDialog(sizeMb)
            }
        }
        
        // Handle manual input for cache size
        binding.actvCacheSizeLimit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isUpdatingSpinner) {
                val text = binding.actvCacheSizeLimit.text.toString()
                val sizeMb = text.toIntOrNull()
                if (sizeMb != null && sizeMb in 512..16384) {
                    val current = viewModel.settings.value
                    if (current.cacheSizeMb != sizeMb) {
                        // Show restart dialog
                        showCacheSizeRestartDialog(sizeMb)
                    }
                } else {
                    // Invalid input, restore previous value
                    binding.actvCacheSizeLimit.setText(viewModel.settings.value.cacheSizeMb.toString(), false)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Cache size must be between 512 and 16384 MB",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        // Network sync settings
        binding.switchEnableBackgroundSync.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSpinner) return@setOnCheckedChangeListener
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(enableBackgroundSync = isChecked))
        }
        
        // Sync interval dropdown
        val syncIntervalOptions = arrayOf("5", "15", "60", "120", "300")
        val syncAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, syncIntervalOptions)
        binding.actvSyncInterval.setAdapter(syncAdapter)
        
        // Convert hours to minutes for initial display
        val currentMinutes = viewModel.settings.value.backgroundSyncIntervalHours * 60
        binding.actvSyncInterval.setText(currentMinutes.toString(), false)
        
        binding.actvSyncInterval.setOnItemClickListener { _, _, position, _ ->
            if (isUpdatingSpinner) return@setOnItemClickListener
            val minutes = syncIntervalOptions[position].toInt()
            val hours = (minutes / 60.0).toInt().coerceAtLeast(1) // At least 1 hour
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(backgroundSyncIntervalHours = hours))
        }
        
        // Handle manual input
        binding.actvSyncInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isUpdatingSpinner) {
                val text = binding.actvSyncInterval.text.toString()
                val minutes = text.toIntOrNull()
                if (minutes != null && minutes >= 5) {
                    val hours = (minutes / 60.0).toInt().coerceAtLeast(1) // At least 1 hour
                    val current = viewModel.settings.value
                    viewModel.updateSettings(current.copy(backgroundSyncIntervalHours = hours))
                } else {
                    // Invalid input, restore previous value
                    val previousMinutes = viewModel.settings.value.backgroundSyncIntervalHours * 60
                    binding.actvSyncInterval.setText(previousMinutes.toString(), false)
                    android.widget.Toast.makeText(requireContext(), R.string.slide_interval_error, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.btnSyncNow.setOnClickListener {
            // TODO: Trigger manual sync
            android.widget.Toast.makeText(requireContext(), R.string.manual_sync_triggered, android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // Default User
        binding.etDefaultUser.setText(viewModel.settings.value.defaultUser)
        binding.etDefaultUser.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val current = viewModel.settings.value
                val newUser = binding.etDefaultUser.text.toString()
                if (current.defaultUser != newUser) {
                    viewModel.updateSettings(current.copy(defaultUser = newUser))
                }
            }
        }
        
        // Default Password
        binding.etDefaultPassword.setText(viewModel.settings.value.defaultPassword)
        binding.etDefaultPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val current = viewModel.settings.value
                val newPassword = binding.etDefaultPassword.text.toString()
                if (current.defaultPassword != newPassword) {
                    viewModel.updateSettings(current.copy(defaultPassword = newPassword))
                }
            }
        }
        
        // User Guide Button
        binding.btnUserGuide.setOnClickListener {
            try {
                // Get current language from LocaleHelper (active language)
                val currentLanguage = LocaleHelper.getLanguage(requireContext())
                
                // Determine User Guide URL based on language
                val guideUrl = when (currentLanguage) {
                    "ru" -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/index-ru.html"
                    "uk" -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/index-uk.html"
                    else -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/"
                }
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(guideUrl)
                }
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "No browser found to open documentation",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        binding.btnPrivacyPolicy.setOnClickListener {
            try {
                // Get current language from LocaleHelper (active language)
                val currentLanguage = LocaleHelper.getLanguage(requireContext())
                
                // Determine Privacy Policy URL based on language
                val privacyUrl = when (currentLanguage) {
                    "ru" -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.ru.html"
                    "uk" -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.uk.html"
                    else -> "https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.html"
                }
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(privacyUrl)
                }
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "No browser found to open Privacy Policy",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        // Export/Import Settings
        binding.btnExportSettings.setOnClickListener {
            exportSettings()
        }
        
        binding.btnImportSettings.setOnClickListener {
            importSettings()
        }
        
        // Permissions Buttons - enable only if permissions not granted
        updatePermissionButtonsState()
        
        binding.btnLocalFilesPermission.setOnClickListener {
            requestStoragePermissions()
        }
        
        binding.btnNetworkPermission.setOnClickListener {
            // Network permissions (INTERNET, ACCESS_NETWORK_STATE) are granted automatically
            // They are declared in AndroidManifest.xml and don't require runtime permissions
            Toast.makeText(
                requireContext(), 
                "Network permissions are already granted automatically", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Log Buttons
        binding.btnShowLog.setOnClickListener {
            showLogDialog(fullLog = true)
        }
        
        binding.btnShowSessionLog.setOnClickListener {
            showLogDialog(fullLog = false)
        }
        
        // Cache Management
        binding.btnAutoCalculateCache.setOnClickListener {
            autoCalculateCacheSize()
        }
        
        binding.btnClearCache.setOnClickListener {
            clearCache()
        }
        
        // Update cache size on view creation
        updateCacheSize()
    }

    override fun onResume() {
        super.onResume()
        // Update permission buttons state when returning from system settings
        updatePermissionButtonsState()
        // Update cache size when returning to fragment
        updateCacheSize()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    // Update language spinner
                    val languagePosition = when (settings.language) {
                        "en" -> 0
                        "ru" -> 1
                        "uk" -> 2
                        else -> 0
                    }
                    if (binding.spinnerLanguage.selectedItemPosition != languagePosition) {
                        // Set flag to prevent triggering onItemSelected
                        isUpdatingSpinner = true
                        binding.spinnerLanguage.setSelection(languagePosition, false)
                        // Reset flag after a short delay to allow UI to settle
                        binding.spinnerLanguage.post {
                            isUpdatingSpinner = false
                        }
                    }
                    
                    // Update switches (with protection from triggering listeners)
                    isUpdatingSpinner = true
                    
                    if (binding.switchPreventSleep.isChecked != settings.preventSleep) {
                        binding.switchPreventSleep.isChecked = settings.preventSleep
                    }
                    if (binding.switchEnableFavorites.isChecked != settings.enableFavorites) {
                        binding.switchEnableFavorites.isChecked = settings.enableFavorites
                    }
                    if (binding.switchSmallControls.isChecked != settings.showSmallControls) {
                        binding.switchSmallControls.isChecked = settings.showSmallControls
                    }
                    
                    // Safe Mode (Phase 2.1)
                    if (binding.switchEnableSafeMode.isChecked != settings.enableSafeMode) {
                        binding.switchEnableSafeMode.isChecked = settings.enableSafeMode
                    }
                    // Show/hide sub-options based on enableSafeMode
                    binding.layoutConfirmDelete.visibility = if (settings.enableSafeMode) View.VISIBLE else View.GONE
                    binding.layoutConfirmMove.visibility = if (settings.enableSafeMode) View.VISIBLE else View.GONE
                    
                    if (binding.switchConfirmDelete.isChecked != settings.confirmDelete) {
                        binding.switchConfirmDelete.isChecked = settings.confirmDelete
                    }
                    if (binding.switchConfirmMove.isChecked != settings.confirmMove) {
                        binding.switchConfirmMove.isChecked = settings.confirmMove
                    }
                    
                    isUpdatingSpinner = false
                    
                    if (binding.switchEnableBackgroundSync.isChecked != settings.enableBackgroundSync) {
                        binding.switchEnableBackgroundSync.isChecked = settings.enableBackgroundSync
                    }
                    
                    // Update network parallelism
                    val currentParallelism = binding.actvNetworkParallelism.text.toString().toIntOrNull()
                    if (currentParallelism != settings.networkParallelism) {
                        binding.actvNetworkParallelism.setText(settings.networkParallelism.toString(), false)
                    }
                    
                    // Update cache size limit
                    val currentCacheSize = binding.actvCacheSizeLimit.text.toString().toIntOrNull()
                    if (currentCacheSize != settings.cacheSizeMb) {
                        binding.actvCacheSizeLimit.setText(settings.cacheSizeMb.toString(), false)
                    }
                    
                    // Update sync interval (convert hours to minutes)
                    val syncMinutes = settings.backgroundSyncIntervalHours * 60
                    val displayedText = binding.actvSyncInterval.text.toString()
                    if (displayedText != syncMinutes.toString()) {
                        binding.actvSyncInterval.setText(syncMinutes.toString(), false)
                    }
                }
            }
        }
    }

    private fun showLogDialog(fullLog: Boolean) {
        val logText = if (fullLog) {
            getFullLog()
        } else {
            getSessionLog()
        }
        
        com.sza.fastmediasorter.ui.common.DialogUtils.showScrollableDialog(
            requireContext(),
            if (fullLog) "Application Log" else "Current Session Log",
            logText,
            "Close" // Positive button (acting as Close/OK)
        )
    }

    private fun getFullLog(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val bufferedReader = java.io.BufferedReader(
                java.io.InputStreamReader(process.inputStream)
            )
            
            val log = StringBuilder()
            var lineCount = 0
            val maxLines = 512
            
            // Read last 512 lines
            val lines = bufferedReader.readLines()
            val startIndex = maxOf(0, lines.size - maxLines)
            
            for (i in startIndex until lines.size) {
                log.append(lines[i]).append("\n")
                lineCount++
            }
            
            bufferedReader.close()
            
            if (log.isEmpty()) {
                "No log entries found"
            } else {
                "Last $lineCount lines of log:\n\n$log"
            }
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    private fun getSessionLog(): String {
        return try {
            val packageName = requireContext().packageName
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val bufferedReader = java.io.BufferedReader(
                java.io.InputStreamReader(process.inputStream)
            )
            
            val log = StringBuilder()
            var lineCount = 0
            
            bufferedReader.forEachLine { line ->
                // Filter only lines from current app
                if (line.contains(packageName, ignoreCase = true) || 
                    line.contains("FastMediaSorter", ignoreCase = true)) {
                    log.append(line).append("\n")
                    lineCount++
                }
            }
            
            bufferedReader.close()
            
            if (log.isEmpty()) {
                "No log entries found for current session"
            } else {
                "Current session log ($lineCount lines):\n\n$log"
            }
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.log_copied, Toast.LENGTH_SHORT).show()
    }

    private fun showRestartDialog(languageCode: String) {
        val languageName = LocaleHelper.getLanguageName(languageCode)
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.restart_app_title)
            .setMessage(getString(R.string.restart_app_message, languageName))
            .setPositiveButton(R.string.restart) { _, _ ->
                // Language already saved in DataStore by updateSettings()
                // Sync to SharedPreferences for app restart
                LocaleHelper.saveLanguage(requireActivity(), languageCode)
                // Restart app
                LocaleHelper.restartApp(requireActivity())
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                // User declined restart - revert spinner to current active language
                // (the language that was loaded at app start, not the new selection)
                val currentLanguage = LocaleHelper.getLanguage(requireContext())
                val currentPosition = when (currentLanguage) {
                    "en" -> 0
                    "ru" -> 1
                    "uk" -> 2
                    else -> 0
                }
                isUpdatingSpinner = true
                binding.spinnerLanguage.setSelection(currentPosition, false)
                binding.spinnerLanguage.post { isUpdatingSpinner = false }
                
                // Revert settings to current active language
                val currentSettings = viewModel.settings.value
                viewModel.updateSettings(currentSettings.copy(language = currentLanguage))
                
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showCacheSizeRestartDialog(newCacheSizeMb: Int, isUserModified: Boolean = true) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.restart_app_title)
            .setMessage(getString(R.string.restart_app_cache_message, newCacheSizeMb))
            .setPositiveButton(R.string.restart) { _, _ ->
                // Save to DataStore with user-modified flag
                val current = viewModel.settings.value
                viewModel.updateSettings(current.copy(
                    cacheSizeMb = newCacheSizeMb,
                    isCacheSizeUserModified = isUserModified
                ))
                
                // Save to SharedPreferences for Glide initialization
                requireContext().getSharedPreferences("glide_config", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("cache_size_mb", newCacheSizeMb)
                    .apply()
                
                // Save current language to ensure it persists after restart
                LocaleHelper.saveLanguage(requireContext(), viewModel.settings.value.language)
                
                // Restart app
                LocaleHelper.restartApp(requireActivity())
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                // User declined restart - revert to current cache size
                val currentCacheSize = viewModel.settings.value.cacheSizeMb
                isUpdatingSpinner = true
                binding.actvCacheSizeLimit.setText(currentCacheSize.toString(), false)
                binding.actvCacheSizeLimit.post { isUpdatingSpinner = false }
                
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun updatePermissionButtonsState() {
        // Local Files Permission - enable only if not granted
        val hasStoragePermission = com.sza.fastmediasorter.core.util.PermissionHelper.hasStoragePermission(requireContext())
        binding.btnLocalFilesPermission.isEnabled = !hasStoragePermission
        binding.btnLocalFilesPermission.alpha = if (hasStoragePermission) 0.5f else 1.0f
        
        // Network Permission - always granted (normal permission), so always disabled
        val hasNetworkPermission = com.sza.fastmediasorter.core.util.PermissionHelper.hasInternetPermission(requireContext())
        binding.btnNetworkPermission.isEnabled = !hasNetworkPermission
        binding.btnNetworkPermission.alpha = if (hasNetworkPermission) 0.5f else 1.0f
    }
    
    private fun requestStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): Request MANAGE_EXTERNAL_STORAGE
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general settings
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(requireContext(), R.string.storage_permissions_granted, Toast.LENGTH_SHORT).show()
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android 6-10 (API 23-29): Request READ/WRITE_EXTERNAL_STORAGE
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            val needsPermission = permissions.any { 
                androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), it) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED 
            }
            
            if (needsPermission) {
                @Suppress("DEPRECATION")
                requestPermissions(permissions, 100)
            } else {
                Toast.makeText(requireContext(), R.string.storage_permissions_granted, Toast.LENGTH_SHORT).show()
            }
        } else {
            // Android 5.x and below: permissions granted at install time
            Toast.makeText(requireContext(), R.string.storage_permissions_granted, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = viewModel.exportSettingsUseCase()
                
                result.onSuccess { _ ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.export_success),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.export_failed, error.message ?: "Unknown error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Export settings failed")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.export_failed, e.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun importSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = viewModel.importSettingsUseCase()
                
                result.onSuccess {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.import_success),
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Show restart dialog
                    showRestartAfterImportDialog()
                }.onFailure { error ->
                    val message = if (error.message == "File not found") {
                        getString(R.string.import_file_not_found)
                    } else {
                        getString(R.string.import_failed, error.message ?: "Unknown error")
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Import settings failed")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.import_failed, e.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showRestartAfterImportDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.restart_required_title)
            .setMessage(R.string.restart_required_message)
            .setPositiveButton(R.string.restart_now) { _, _ ->
                // Restart app
                val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
                intent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                    requireActivity().finish()
                }
            }
            .setNegativeButton(R.string.restart_later) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun updateCacheSize() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = requireContext().cacheDir
                val totalSize = calculateDirectorySize(cacheDir)
                val formattedSize = formatFileSize(totalSize)
                
                withContext(Dispatchers.Main) {
                    binding.tvCacheSize.text = getString(R.string.cache_size_format, formattedSize)
                    // Reset button text to static "Clear Cache" in case passed from "calculating..." state
                    binding.btnClearCache.text = getString(R.string.clear_cache)
                    binding.btnClearCache.isEnabled = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to calculate cache size")
                withContext(Dispatchers.Main) {
                    binding.tvCacheSize.text = getString(R.string.cache_size_format, "N/A")
                    binding.btnClearCache.text = getString(R.string.clear_cache)
                    binding.btnClearCache.isEnabled = true
                }
            }
        }
    }
    
    private fun calculateDirectorySize(directory: java.io.File): Long {
        var size = 0L
        try {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating directory size: ${directory.absolutePath}")
        }
        return size
    }
    
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    
    private fun autoCalculateCacheSize() {
        val optimalSizeMb = calculateOptimalCacheSizeUseCase()
        val storageInfo = calculateOptimalCacheSizeUseCase.getStorageInfo()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.auto_calculate_cache_title)
            .setMessage(getString(R.string.auto_calculate_cache_message, optimalSizeMb, storageInfo))
            .setPositiveButton(R.string.apply) { _, _ ->
                val current = viewModel.settings.value
                if (current.cacheSizeMb != optimalSizeMb) {
                    // Update settings and show restart dialog (NOT marked as user-modified)
                    showCacheSizeRestartDialog(optimalSizeMb, isUserModified = false)
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.cache_size_already_optimal,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun clearCache() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_cache)
            .setMessage(R.string.clear_cache_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                // Show loading state
                binding.btnClearCache.isEnabled = false
                binding.btnClearCache.text = getString(R.string.cache_size_calculating)
                
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 1. Clear Glide Disk Cache
                        try {
                            com.bumptech.glide.Glide.get(requireContext()).clearDiskCache()
                        } catch (e: Exception) {
                            Timber.e(e, "Glide clearDiskCache failed")
                        }
                        
                        // 2. Clear UnifiedFileCache (all network files)
                        try {
                            val app = requireActivity().application as com.sza.fastmediasorter.FastMediaSorterApp
                            val stats = app.unifiedCache.getCacheStats()
                            app.unifiedCache.clearAll()
                            Timber.d("Cleared UnifiedFileCache: ${stats.totalSizeMB} MB")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to clear UnifiedFileCache")
                        }
                        
                        // 3. Clear other cache files manually (including translation cache in memory)
                        val cacheDir = requireContext().cacheDir
                        deleteRecursive(cacheDir)
                        
                        // Clear global translation cache
                        com.sza.fastmediasorter.core.cache.TranslationCacheManager.clearAll()
                        
                        // 4. Clear playback positions
                        try {
                            val app = requireActivity().application as com.sza.fastmediasorter.FastMediaSorterApp
                            app.playbackPositionRepository.deleteAllPositions()
                            Timber.d("Cleared all playback positions")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to clear playback positions")
                        }
                        
                        withContext(Dispatchers.Main) {
                            // 5. Clear Glide Memory Cache
                            try {
                                com.bumptech.glide.Glide.get(requireContext()).clearMemory()
                            } catch (e: Exception) {
                                Timber.e(e, "Glide clearMemory failed")
                            }
                            
                            Toast.makeText(
                                requireContext(),
                                R.string.cache_cleared,
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            binding.btnClearCache.isEnabled = true
                            updateCacheSize()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to clear cache")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                R.string.cache_clear_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.btnClearCache.isEnabled = true
                            updateCacheSize()
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun deleteRecursive(fileOrDirectory: java.io.File): Boolean {
        return try {
            if (fileOrDirectory.isDirectory) {
                fileOrDirectory.listFiles()?.forEach { child ->
                    deleteRecursive(child)
                }
            }
            // Delete files inside cache dir but not the cache dir itself
            if (fileOrDirectory != requireContext().cacheDir) {
                fileOrDirectory.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting: ${fileOrDirectory.absolutePath}")
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
