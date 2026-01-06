# EditResourceActivity - observeData Complete Details

**Source**: Items 42-60 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `EditResourceActivity.kt` (719 lines)

---

## observeData State Collection

**Location**: `EditResourceActivity.observeData()`

**Main Structure**:
```kotlin
private fun observeData() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // State flow
            launch {
                viewModel.state.collect { state ->
                    updateUiFromState(state)
                }
            }
            
            // Loading flow
            launch {
                viewModel.loading.collect { isLoading ->
                    binding.progressBar.isVisible = isLoading
                }
            }
            
            // Events flow
            launch {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }
}
```

---

## State Updates (Items 42-60)

### 42. Resource Path Display

**Field**: `tvResourcePath` (read-only TextView)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Display resource path (read-only)
        binding.tvResourcePath.text = resource.path
        
        Timber.d("Resource path displayed: ${resource.path}")
    }
}
```

**Path Examples**:
- Local: `content://com.android.externalstorage.documents/tree/primary%3ADCIM`
- SMB: `smb://192.168.1.100/SharedFolder/Photos`
- SFTP: `sftp://192.168.1.50:22/home/user/media`
- FTP: `ftp://192.168.1.75:21/files`
- Cloud: `googledrive://folder/1A2B3C4D5E6F7G8H9I0J`

---

### 43. Created Date Formatting

**Field**: `tvCreatedDate`

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Format created date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(resource.createdDate))
        
        binding.tvCreatedDate.text = formattedDate
        
        Timber.d("Created date formatted: $formattedDate")
    }
}
```

**SimpleDateFormat Pattern**: "yyyy-MM-dd HH:mm:ss"
- Example: "2026-01-06 14:35:22"

---

### 44. File Count Display

**Field**: `tvFileCount`

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Display file count
        val fileCountText = if (resource.fileCount >= 1000) {
            ">1000"
        } else {
            resource.fileCount.toString()
        }
        
        binding.tvFileCount.text = fileCountText
        
        Timber.d("File count displayed: $fileCountText (actual: ${resource.fileCount})")
    }
}
```

**Logic**: If file count ≥ 1000, show ">1000" instead of exact number (avoids clutter, indicates large collection).

---

### 45. Last Browse Date Display

**Field**: `tvLastBrowseDate`

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Display last browse date
        val lastBrowsedText = if (resource.lastBrowsedDate != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.format(Date(resource.lastBrowsedDate))
        } else {
            getString(R.string.never_browsed)
        }
        
        binding.tvLastBrowseDate.text = lastBrowsedText
        
        Timber.d("Last browse date displayed: $lastBrowsedText")
    }
}
```

**Logic**: 
- If `lastBrowsedDate == null` → show "Never browsed"
- Otherwise → format date with SimpleDateFormat

---

### 46. Slideshow Interval Update

**Field**: `etSlideshowInterval` (AutoCompleteTextView)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Remove watchers before update
        removeTextWatchers()
        
        // Update slideshow interval
        // Use setText(text, false) to avoid showing dropdown
        binding.etSlideshowInterval.setText(
            resource.slideshowInterval.toString(),
            false // Don't show dropdown suggestions
        )
        
        // Re-add watchers
        addTextWatchers()
        
        Timber.d("Slideshow interval updated: ${resource.slideshowInterval} seconds")
    }
}
```

**AutoCompleteTextView.setText(text, filter)**:
- `setText(text, true)` - Shows dropdown suggestions
- `setText(text, false)` - Sets text without triggering dropdown (used here to avoid unwanted popup)

---

### 47-53. Media Type Checkboxes Updates (7 checkboxes)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Remove checkbox listeners before update
        removeCheckboxListeners()
        
        // Update media type checkboxes based on supportedMediaTypes Set
        binding.cbSupportImages.isChecked = MediaType.IMAGE in resource.supportedMediaTypes
        binding.cbSupportVideos.isChecked = MediaType.VIDEO in resource.supportedMediaTypes
        binding.cbSupportAudio.isChecked = MediaType.AUDIO in resource.supportedMediaTypes
        binding.cbSupportGif.isChecked = MediaType.GIF in resource.supportedMediaTypes
        binding.cbSupportText.isChecked = MediaType.TEXT in resource.supportedMediaTypes
        binding.cbSupportPdf.isChecked = MediaType.PDF in resource.supportedMediaTypes
        binding.cbSupportEpub.isChecked = MediaType.EPUB in resource.supportedMediaTypes
        
        // Re-add checkbox listeners
        addCheckboxListeners()
        
        Timber.d("Media types updated: ${resource.supportedMediaTypes.joinToString(", ")}")
    }
}
```

**Set Membership Check**: `MediaType.IMAGE in resource.supportedMediaTypes` checks if IMAGE is present in the Set.

**Listener Management**: Remove listeners before programmatic update to prevent triggering onChange → ViewModel call loop.

---

### 54-55. Media Types Layout Visibility

**Fields**: 
- `layoutMediaTypesTextPdf` (LinearLayout containing cbSupportText + cbSupportPdf)
- `layoutMediaTypesEpub` (LinearLayout containing cbSupportEpub)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    // Check global settings
    val settingsManager = SettingsManager(this)
    
    // Text/PDF layout visibility (controlled by settings)
    binding.layoutMediaTypesTextPdf.isVisible = settingsManager.isSupportTextPdf
    
    // EPUB layout visibility (controlled by settings)
    binding.layoutMediaTypesEpub.isVisible = settingsManager.isSupportEpub
    
    Timber.d("Media types layout visibility: textPdf=${settingsManager.isSupportTextPdf}, epub=${settingsManager.isSupportEpub}")
}
```

**Settings**:
```kotlin
// SettingsManager.kt
val isSupportTextPdf: Boolean
    get() = sharedPreferences.getBoolean(PREF_SUPPORT_TEXT_PDF, false)

val isSupportEpub: Boolean
    get() = sharedPreferences.getBoolean(PREF_SUPPORT_EPUB, false)
```

**Purpose**: Hides TEXT/PDF/EPUB checkboxes if features disabled in settings (reduces clutter, experimental features).

---

### 56-58. Flag Checkboxes Updates

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Remove checkbox listeners before update
        removeCheckboxListeners()
        
        // Update flag checkboxes
        binding.cbScanSubdirectories.isChecked = resource.scanSubdirectories
        binding.cbDisableThumbnails.isChecked = resource.disableThumbnails
        binding.cbReadOnlyMode.isChecked = resource.isReadOnly
        
        // Re-add checkbox listeners
        addCheckboxListeners()
        
        Timber.d("Flags updated: scanSubdirectories=${resource.scanSubdirectories}, disableThumbnails=${resource.disableThumbnails}, isReadOnly=${resource.isReadOnly}")
    }
}
```

---

### 59. cbReadOnlyMode Complex Update

**Purpose**: Temporarily remove listener, force checked state if resource is not writable, then re-attach listener.

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Remove listener
        binding.cbReadOnlyMode.setOnCheckedChangeListener(null)
        
        // Update checkbox
        binding.cbReadOnlyMode.isChecked = resource.isReadOnly
        
        // If resource is NOT writable (e.g., permissions lost), force read-only mode
        if (!resource.isWritable) {
            binding.cbReadOnlyMode.isChecked = true
            binding.cbReadOnlyMode.isEnabled = false // Disable checkbox (user cannot change)
            
            Timber.w("Resource is not writable, forcing read-only mode")
        } else {
            binding.cbReadOnlyMode.isEnabled = true
        }
        
        // Re-attach listener
        binding.cbReadOnlyMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateReadOnlyMode(isChecked)
        }
    }
}
```

**isWritable Check**:
```kotlin
// MediaResource.kt
val isWritable: Boolean
    get() = when (type) {
        ResourceType.LOCAL -> {
            // Check if SAF URI permissions still valid
            try {
                val uri = Uri.parse(path)
                context.contentResolver.persistedUriPermissions.any { 
                    it.uri == uri && it.isWritePermission 
                }
            } catch (e: Exception) {
                false
            }
        }
        ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP -> {
            // Network resources are writable unless explicitly read-only
            !isReadOnly
        }
        ResourceType.CLOUD -> {
            // Cloud resources are writable if OAuth token valid
            !isReadOnly
        }
    }
```

**Scenario**: Local folder loses write permissions (user revoked or app uninstalled/reinstalled). Checkbox forced to read-only and disabled.

---

### 60. switchIsDestination Complex Update

**Purpose**: Remove listener, update enabled/checked state based on validation rules, log visibility, re-attach listener.

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Remove listener
        binding.switchIsDestination.setOnCheckedChangeListener(null)
        
        // Update enabled state based on canBeDestination
        binding.switchIsDestination.isEnabled = state.canBeDestination || resource.isDestination
        
        // Update checked state
        binding.switchIsDestination.isChecked = resource.isDestination
        
        // Log visibility (detailed explanation)
        val reason = when {
            resource.isDestination -> "Currently destination"
            !state.canBeDestination && resource.isReadOnly -> "Read-only mode"
            !state.canBeDestination && state.destinationsCount >= 10 -> "Max destinations reached (10)"
            state.canBeDestination -> "Can be added"
            else -> "Unknown"
        }
        
        Timber.d("Destination switch updated: enabled=${state.canBeDestination || resource.isDestination}, checked=${resource.isDestination}, reason=$reason")
        
        // Re-attach listener
        binding.switchIsDestination.setOnCheckedChangeListener { _, isChecked ->
            userActionLogger.logAction(
                action = if (isChecked) "AddToDestinations" else "RemoveFromDestinations",
                screen = "EditResource",
                details = resource.name
            )
            
            // Validate if can be destination
            if (isChecked && !state.canBeDestination) {
                Toast.makeText(
                    this@EditResourceActivity,
                    R.string.cannot_add_destination_limit_reached,
                    Toast.LENGTH_SHORT
                ).show()
                
                viewModel.updateIsDestination(false)
                return@setOnCheckedChangeListener
            }
            
            viewModel.updateIsDestination(isChecked)
        }
    }
}
```

**Enabled State Rules**:
- If resource is already destination → switch enabled (can toggle off)
- If `canBeDestination == false` → switch disabled (cannot toggle on)
- Otherwise → switch enabled (can toggle on)

**Checked State**: Reflects `resource.isDestination` flag.

**Logging**: Logs detailed reason for enabled/disabled state (helpful for debugging).

---

## Credentials Section Visibility & Updates

### 61-62. Credentials Sections Visibility

**Layouts**:
- `layoutSmbCredentials` - SMB username/password/domain/port fields
- `layoutSftpCredentials` - SFTP/FTP username/password/SSH key fields

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Show/hide credentials section based on resource type
        when (resource.type) {
            ResourceType.SMB -> {
                binding.layoutSmbCredentials.isVisible = true
                binding.layoutSftpCredentials.isVisible = false
            }
            ResourceType.SFTP, ResourceType.FTP -> {
                binding.layoutSmbCredentials.isVisible = false
                binding.layoutSftpCredentials.isVisible = true
            }
            else -> {
                // Local/Cloud resources have no editable credentials
                binding.layoutSmbCredentials.isVisible = false
                binding.layoutSftpCredentials.isVisible = false
            }
        }
        
        Timber.d("Credentials sections visibility: SMB=${binding.layoutSmbCredentials.isVisible}, SFTP=${binding.layoutSftpCredentials.isVisible}")
    }
}
```

---

### 63. Credentials Title Update

**Field**: `tvCredentialsTitle`

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Update credentials section title
        val titleResId = when (resource.type) {
            ResourceType.SMB -> R.string.smb_credentials
            ResourceType.SFTP -> R.string.sftp_credentials
            ResourceType.FTP -> R.string.ftp_credentials
            else -> R.string.credentials // Generic title
        }
        
        binding.tvCredentialsTitle.setText(titleResId)
        
        Timber.d("Credentials title updated: ${getString(titleResId)}")
    }
}
```

**String Resources**:
- `R.string.smb_credentials` = "SMB Connection Settings"
- `R.string.sftp_credentials` = "SFTP Connection Settings"
- `R.string.ftp_credentials` = "FTP Connection Settings"
- `R.string.credentials` = "Connection Settings"

---

## Speed Test Results Card

### 64. Speed Test Results Card (Complex Logic)

**Purpose**: Displays network speed test results with detailed logging and visibility control.

**Layout**: `cardSpeedTestResults` contains:
- `tvResourceType` - Resource type label
- `tvReadSpeed` - Read speed in Mbps
- `tvWriteSpeed` - Write speed in Mbps
- `tvRecommendedThreads` - Recommended thread count for operations
- `tvLastTestDate` - Last test date formatted

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Check if speed test results available
        val hasSpeedTestResults = resource.readSpeedMbps != null && 
                                   resource.writeSpeedMbps != null &&
                                   resource.lastSpeedTestDate != null
        
        if (hasSpeedTestResults) {
            // Show card
            binding.cardSpeedTestResults.isVisible = true
            
            // Update resource type
            val typeText = when (resource.type) {
                ResourceType.SMB -> "SMB"
                ResourceType.SFTP -> "SFTP"
                ResourceType.FTP -> "FTP"
                ResourceType.CLOUD -> when (resource.cloudProvider) {
                    CloudProvider.GOOGLE_DRIVE -> "Google Drive"
                    CloudProvider.DROPBOX -> "Dropbox"
                    CloudProvider.ONEDRIVE -> "OneDrive"
                    else -> "Cloud"
                }
                else -> resource.type.name
            }
            binding.tvResourceType.text = typeText
            
            // Format speeds (%.2f for 2 decimal places)
            val readSpeedText = String.format("%.2f Mbps", resource.readSpeedMbps)
            val writeSpeedText = String.format("%.2f Mbps", resource.writeSpeedMbps)
            
            binding.tvReadSpeed.text = readSpeedText
            binding.tvWriteSpeed.text = writeSpeedText
            
            // Recommended threads
            binding.tvRecommendedThreads.text = resource.recommendedThreads.toString()
            
            // Last test date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val lastTestDateText = dateFormat.format(Date(resource.lastSpeedTestDate!!))
            binding.tvLastTestDate.text = lastTestDateText
            
            Timber.d("Speed test results displayed: type=$typeText, read=$readSpeedText, write=$writeSpeedText, threads=${resource.recommendedThreads}, date=$lastTestDateText")
        } else {
            // Hide card
            binding.cardSpeedTestResults.isVisible = false
            
            Timber.d("Speed test results card HIDDEN (no results available)")
        }
    }
}
```

**Visibility Logic**:
- Card visible ONLY if all 3 fields non-null: `readSpeedMbps`, `writeSpeedMbps`, `lastSpeedTestDate`
- Otherwise hidden (card not shown for resources without speed test data)

**Speed Formatting**: `String.format("%.2f Mbps", speed)` - 2 decimal places
- Example: "12.45 Mbps", "0.83 Mbps"

**Recommended Threads Calculation** (from NetworkSpeedTestUseCase):
```kotlin
private fun calculateThreads(readSpeedMbps: Double, writeSpeedMbps: Double): Int {
    val avgSpeed = (readSpeedMbps + writeSpeedMbps) / 2
    
    return when {
        avgSpeed >= 100.0 -> 8  // Very fast (100+ Mbps)
        avgSpeed >= 50.0 -> 6   // Fast (50-100 Mbps)
        avgSpeed >= 10.0 -> 4   // Medium (10-50 Mbps)
        avgSpeed >= 1.0 -> 2    // Slow (1-10 Mbps)
        else -> 1               // Very slow (<1 Mbps)
    }
}
```

**Date Format**: "yyyy-MM-dd HH:mm" (excludes seconds for compact display)
- Example: "2026-01-06 14:35"

---

## Clear Trash Button Visibility

### 65. Clear Trash Button Visibility

**Field**: `btnClearTrash`

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    // Show Clear Trash button only if resource has trash folders
    binding.btnClearTrash.isVisible = state.hasTrashFolders
    
    Timber.d("Clear Trash button visibility: ${state.hasTrashFolders}")
}
```

**hasTrashFolders Check** (from ViewModel):
```kotlin
// In EditResourceViewModel
init {
    viewModelScope.launch {
        // Check if resource has trash folders
        val resource = _state.value.currentResource
        if (resource != null) {
            val hasTrash = checkHasTrashFoldersUseCase(resource.id)
            _state.update { it.copy(hasTrashFolders = hasTrash) }
        }
    }
}
```

**checkHasTrashFoldersUseCase**:
```kotlin
class CheckHasTrashFoldersUseCase @Inject constructor(
    private val fileOperationsRepository: FileOperationsRepository
) {
    suspend operator fun invoke(resourceId: Long): Boolean = withContext(Dispatchers.IO) {
        // Check if .trash/ folder exists in resource root
        val trashFolderPath = when (val resource = resourceRepository.getResourceById(resourceId)) {
            null -> return@withContext false
            else -> "${resource.path}/.trash"
        }
        
        // Check folder existence
        return@withContext when (resource.type) {
            ResourceType.LOCAL -> File(trashFolderPath).exists()
            ResourceType.SMB -> smbOperationsUseCase.folderExists(trashFolderPath).getOrDefault(false)
            ResourceType.SFTP -> sftpOperationsUseCase.folderExists(trashFolderPath).getOrDefault(false)
            ResourceType.FTP -> ftpOperationsUseCase.folderExists(trashFolderPath).getOrDefault(false)
            ResourceType.CLOUD -> cloudOperationsUseCase.folderExists(trashFolderPath).getOrDefault(false)
        }
    }
}
```

**Purpose**: Button appears only if soft-deleted files exist (allows user to permanently delete or restore from trash).

---

## SMB Credentials Update

### 66. SMB Credentials Update (Detailed Logic)

**Fields** (6 total):
1. `etSmbServer` - Server hostname/IP
2. `etSmbShareName` - Share name
3. `etSmbUsername` - Username
4. `etSmbPassword` - Password
5. `etSmbDomain` - Domain (optional)
6. `etSmbPort` - Port number (default 445)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Only update if resource type is SMB
        if (resource.type != ResourceType.SMB) return@let
        
        // Remove listeners before update
        removeSmbListeners()
        
        // Get SMB credentials from state
        val smbUsername = state.smbUsername
        val smbPassword = state.smbPassword
        val smbDomain = state.smbDomain
        val smbPort = state.smbPort
        
        // Parse server and share from path: smb://server/shareName/subfolders
        val pathWithoutScheme = resource.path.removePrefix("smb://")
        val pathParts = pathWithoutScheme.split("/")
        val server = pathParts.getOrNull(0) ?: ""
        val shareName = pathParts.getOrNull(1) ?: ""
        
        // Update fields ONLY if text differs (prevents unnecessary updates)
        if (binding.etSmbServer.text.toString() != server) {
            binding.etSmbServer.setText(server)
        }
        
        if (binding.etSmbShareName.text.toString() != shareName) {
            binding.etSmbShareName.setText(shareName)
        }
        
        if (binding.etSmbUsername.text.toString() != smbUsername) {
            binding.etSmbUsername.setText(smbUsername)
        }
        
        if (binding.etSmbPassword.text.toString() != smbPassword) {
            binding.etSmbPassword.setText(smbPassword)
        }
        
        if (binding.etSmbDomain.text.toString() != smbDomain) {
            binding.etSmbDomain.setText(smbDomain)
        }
        
        val portString = smbPort.toString()
        if (binding.etSmbPort.text.toString() != portString) {
            binding.etSmbPort.setText(portString)
        }
        
        // Re-add listeners after update
        addSmbListeners()
        
        Timber.d("SMB credentials updated: server=$server, share=$shareName, username=$smbUsername, port=$smbPort")
    }
}
```

**Comparison Before Update**: Checks if text differs before calling `setText()` - prevents triggering TextWatcher unnecessarily.

**removeSmbListeners() / addSmbListeners()**:
```kotlin
private fun removeSmbListeners() {
    binding.etSmbServer.removeTextChangedListener(smbServerWatcher)
    binding.etSmbShareName.removeTextChangedListener(smbShareNameWatcher)
    binding.etSmbUsername.removeTextChangedListener(smbUsernameWatcher)
    binding.etSmbPassword.removeTextChangedListener(smbPasswordWatcher)
    binding.etSmbDomain.removeTextChangedListener(smbDomainWatcher)
    binding.etSmbPort.removeTextChangedListener(smbPortWatcher)
    
    Timber.d("SMB listeners removed")
}

private fun addSmbListeners() {
    binding.etSmbServer.addTextChangedListener(smbServerWatcher)
    binding.etSmbShareName.addTextChangedListener(smbShareNameWatcher)
    binding.etSmbUsername.addTextChangedListener(smbUsernameWatcher)
    binding.etSmbPassword.addTextChangedListener(smbPasswordWatcher)
    binding.etSmbDomain.addTextChangedListener(smbDomainWatcher)
    binding.etSmbPort.addTextChangedListener(smbPortWatcher)
    
    Timber.d("SMB listeners added")
}
```

---

## SFTP Credentials Update

### 67. SFTP Credentials Update (Detailed Logic)

**Fields** (5 total):
1. `etSftpHost` - Host hostname/IP
2. `etSftpPort` - Port number (default 22 for SFTP, 21 for FTP)
3. `etSftpUsername` - Username
4. `etSftpPassword` - Password (for password auth)
5. `etSftpPrivateKey` - SSH private key (for key auth)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    state.currentResource?.let { resource ->
        // Only update if resource type is SFTP or FTP
        if (resource.type !in listOf(ResourceType.SFTP, ResourceType.FTP)) return@let
        
        // Remove listeners before update
        removeSftpListeners()
        
        // Get SFTP credentials from state
        val sftpUsername = state.sftpUsername
        val sftpPassword = state.sftpPassword
        val sftpPrivateKey = state.sftpPrivateKey
        val sftpPort = state.sftpPort
        
        // Parse host and port from path: sftp://host:port/path or ftp://host:port/path
        val scheme = if (resource.type == ResourceType.SFTP) "sftp://" else "ftp://"
        val pathWithoutScheme = resource.path.removePrefix(scheme)
        val hostAndRest = pathWithoutScheme.substringBefore("/")
        
        val (host, port) = if (hostAndRest.contains(":")) {
            val parts = hostAndRest.split(":")
            parts[0] to (parts[1].toIntOrNull() ?: if (resource.type == ResourceType.SFTP) 22 else 21)
        } else {
            hostAndRest to if (resource.type == ResourceType.SFTP) 22 else 21
        }
        
        // Update fields ONLY if text differs
        if (binding.etSftpHost.text.toString() != host) {
            binding.etSftpHost.setText(host)
        }
        
        val portString = port.toString()
        if (binding.etSftpPort.text.toString() != portString) {
            binding.etSftpPort.setText(portString)
        }
        
        if (binding.etSftpUsername.text.toString() != sftpUsername) {
            binding.etSftpUsername.setText(sftpUsername)
        }
        
        if (binding.etSftpPassword.text.toString() != sftpPassword) {
            binding.etSftpPassword.setText(sftpPassword)
        }
        
        if (binding.etSftpPrivateKey.text.toString() != sftpPrivateKey) {
            binding.etSftpPrivateKey.setText(sftpPrivateKey)
        }
        
        // Re-add listeners after update
        addSftpListeners()
        
        Timber.d("SFTP/FTP credentials updated: host=$host, port=$port, username=$sftpUsername")
    }
}
```

**removeSftpListeners() / addSftpListeners()**:
```kotlin
private fun removeSftpListeners() {
    binding.etSftpHost.removeTextChangedListener(sftpHostWatcher)
    binding.etSftpPort.removeTextChangedListener(sftpPortWatcher)
    binding.etSftpUsername.removeTextChangedListener(sftpUsernameWatcher)
    binding.etSftpPassword.removeTextChangedListener(sftpPasswordWatcher)
    binding.etSftpPrivateKey.removeTextChangedListener(sftpPrivateKeyWatcher)
    
    Timber.d("SFTP listeners removed")
}

private fun addSftpListeners() {
    binding.etSftpHost.addTextChangedListener(sftpHostWatcher)
    binding.etSftpPort.addTextChangedListener(sftpPortWatcher)
    binding.etSftpUsername.addTextChangedListener(sftpUsernameWatcher)
    binding.etSftpPassword.addTextChangedListener(sftpPasswordWatcher)
    binding.etSftpPrivateKey.addTextChangedListener(sftpPrivateKeyWatcher)
    
    Timber.d("SFTP listeners added")
}
```

---

## Button Enable States

### 68. Button Enable States (btnSave / btnReset)

**Logic**:
```kotlin
private fun updateUiFromState(state: EditResourceState) {
    // Save button always enabled (can save even without changes to update lastModified timestamp)
    binding.btnSave.isEnabled = true
    
    // Reset button enabled only if changes exist
    binding.btnReset.isEnabled = state.hasChanges
    
    Timber.d("Button states: btnSave=true, btnReset=${state.hasChanges}")
}
```

**hasChanges Flag** (from ViewModel):
```kotlin
// In EditResourceViewModel
private fun checkHasChanges() {
    val original = _state.value.originalResource
    val current = _state.value.currentResource
    
    if (original == null || current == null) {
        _state.update { it.copy(hasChanges = false) }
        return
    }
    
    // Compare resources
    val hasResourceChanges = original != current
    
    // Compare credentials (by type)
    val hasCredentialsChanges = when (current.type) {
        ResourceType.SMB -> {
            val originalCreds = _state.value.originalSmbCredentials
            val currentCreds = SmbCredentials(
                username = _state.value.smbUsername,
                password = _state.value.smbPassword,
                domain = _state.value.smbDomain,
                port = _state.value.smbPort
            )
            originalCreds != currentCreds
        }
        ResourceType.SFTP, ResourceType.FTP -> {
            val originalCreds = _state.value.originalSftpCredentials
            val currentCreds = SftpCredentials(
                username = _state.value.sftpUsername,
                password = _state.value.sftpPassword,
                privateKey = _state.value.sftpPrivateKey,
                port = _state.value.sftpPort
            )
            originalCreds != currentCreds
        }
        else -> false
    }
    
    // Update flags
    _state.update { it.copy(
        hasResourceChanges = hasResourceChanges,
        hasSmbCredentialsChanges = current.type == ResourceType.SMB && hasCredentialsChanges,
        hasSftpCredentialsChanges = current.type in listOf(ResourceType.SFTP, ResourceType.FTP) && hasCredentialsChanges,
        hasChanges = hasResourceChanges || hasCredentialsChanges
    ) }
    
    Timber.d("Changes check: resource=$hasResourceChanges, credentials=$hasCredentialsChanges, total=${hasResourceChanges || hasCredentialsChanges}")
}
```

---

## Summary

**Items Documented**: 42-60 (19 behaviors)

**Key UI Updates**:
- Resource path display (read-only)
- Date formatting (created/last browsed/last speed test)
- File count with ">1000" logic
- Slideshow interval update (AutoCompleteTextView with false parameter)
- 7 media type checkboxes with listener management
- Media types layout visibility based on settings
- 3 flag checkboxes updates
- cbReadOnlyMode complex logic (forced if not writable)
- switchIsDestination complex logic (enabled/checked state + validation + logging)
- Credentials sections visibility and title update
- Speed test results card with detailed formatting and visibility
- Clear trash button visibility (based on hasTrashFolders)
- SMB credentials update (6 fields with comparison before setText)
- SFTP credentials update (5 fields with comparison before setText)
- Button enable states (Save always enabled, Reset only if hasChanges)

**Key Patterns**:
- **Listener Management**: Remove listeners before programmatic updates, re-add after
- **Comparison Before Update**: Check if text differs before calling setText() to prevent unnecessary listener triggers
- **Complex State Logic**: cbReadOnlyMode and switchIsDestination have multi-step update logic with validation
- **Visibility Control**: Multiple layouts controlled by resource type and settings
- **Date Formatting**: SimpleDateFormat with different patterns for different contexts
- **Speed Formatting**: String.format("%.2f Mbps", speed) for 2 decimal places
- **Logging**: Detailed Timber logs for every state update (helpful for debugging)
