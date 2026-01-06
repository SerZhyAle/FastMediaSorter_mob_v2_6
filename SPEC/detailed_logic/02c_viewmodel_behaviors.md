# AddResourceActivity - ViewModel Methods & observeData

**Source**: Items 65-79 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `AddResourceViewModel.kt` (1435 lines)

---

## ViewModel Core Methods

### 65. loadResourceForCopy() Method

**Signature**:
```kotlin
fun loadResourceForCopy(resourceId: Long)
```

**Purpose**: Loads existing resource with decrypted credentials for Copy Mode.

**Implementation**:
```kotlin
fun loadResourceForCopy(resourceId: Long) {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        setLoading(true)
        
        try {
            // Get resource by ID
            val resource = resourceRepository.getResourceById(resourceId)
            
            if (resource == null) {
                Timber.e("Resource not found for copy: $resourceId")
                _events.send(AddResourceEvent.ShowError("Resource not found"))
                return@launch
            }
            
            Timber.d("Loaded resource for copy: ${resource.name}, type: ${resource.type}")
            
            // Update state
            _state.update { it.copy(copyFromResource = resource) }
            
            // Fetch credentials based on type
            val (username, password, domain, sshKey, sshPassphrase) = when (resource.type) {
                ResourceType.SMB -> {
                    // Get SMB credentials
                    when (val result = smbOperationsUseCase.getConnectionInfo(resource.credentialsId)) {
                        is Result.Success -> {
                            val info = result.data
                            Timber.d("SMB credentials loaded for resource ${resource.name}")
                            
                            // Return tuple
                            Tuple5(
                                username = info.username,
                                password = info.password,
                                domain = info.domain,
                                sshKey = null,
                                sshPassphrase = null
                            )
                        }
                        is Result.Error -> {
                            Timber.e("Failed to load SMB credentials: ${result.exception.message}")
                            Tuple5(null, null, null, null, null)
                        }
                    }
                }
                
                ResourceType.SFTP -> {
                    // Get SFTP credentials
                    when (val result = getSftpCredentials(resource.credentialsId)) {
                        is Result.Success -> {
                            val creds = result.data
                            Timber.d("SFTP credentials loaded")
                            
                            Tuple5(
                                username = creds.username,
                                password = creds.password,
                                domain = null,
                                sshKey = creds.privateKey,
                                sshPassphrase = creds.keyPassphrase
                            )
                        }
                        is Result.Error -> {
                            Timber.e("Failed to load SFTP credentials")
                            Tuple5(null, null, null, null, null)
                        }
                    }
                }
                
                ResourceType.FTP -> {
                    // Get FTP credentials
                    when (val result = getFtpCredentials(resource.credentialsId)) {
                        is Result.Success -> {
                            val creds = result.data
                            Timber.d("FTP credentials loaded")
                            
                            Tuple5(
                                username = creds.username,
                                password = creds.password,
                                domain = null,
                                sshKey = null,
                                sshPassphrase = null
                            )
                        }
                        is Result.Error -> {
                            Timber.e("Failed to load FTP credentials")
                            Tuple5(null, null, null, null, null)
                        }
                    }
                }
                
                ResourceType.LOCAL, ResourceType.CLOUD -> {
                    // No credentials for local/cloud
                    Tuple5(null, null, null, null, null)
                }
            }
            
            // Send event to activity
            _events.send(
                AddResourceEvent.LoadResourceForCopy(
                    resource = resource,
                    username = username,
                    password = password,
                    domain = domain,
                    sshKey = sshKey,
                    sshPassphrase = sshPassphrase
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to load resource for copy")
            handleError(e)
        } finally {
            setLoading(false)
        }
    }
}
```

**Tuple Helper**:
```kotlin
private data class Tuple5<A, B, C, D, E>(
    val username: A,
    val password: B,
    val domain: C,
    val sshKey: D,
    val sshPassphrase: E
)
```

---

### 66. scanLocalFolders() Method

**Signature**: `fun scanLocalFolders()`

**Purpose**: Scans device storage for media folders (DCIM, Pictures, Movies, etc.).

**Implementation**:
```kotlin
fun scanLocalFolders() {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        _state.update { it.copy(isScanning = true) }
        setLoading(true)
        
        try {
            Timber.d("Scanning local folders")
            
            // Execute scan via UseCase
            val resources = scanLocalFoldersUseCase()
            
            // Update state
            _state.update { it.copy(
                resourcesToAdd = resources,
                isScanning = false
            ) }
            
            // Send message
            _events.send(
                AddResourceEvent.ShowMessage("Found ${resources.size} folders")
            )
            
            Timber.d("Scan completed: ${resources.size} folders found")
            
        } catch (e: Exception) {
            Timber.e(e, "Local folder scan failed")
            handleError(e)
        } finally {
            _state.update { it.copy(isScanning = false) }
            setLoading(false)
        }
    }
}
```

**scanLocalFoldersUseCase**:
```kotlin
class ScanLocalFoldersUseCase @Inject constructor(
    private val context: Context
) {
    suspend operator fun invoke(): List<ResourceToAdd> = withContext(Dispatchers.IO) {
        val resources = mutableListOf<ResourceToAdd>()
        
        // Get external storage volumes
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes
        
        for (volume in volumes) {
            // Standard media directories
            val dirs = listOf("DCIM", "Pictures", "Movies", "Download", "Documents")
            
            for (dir in dirs) {
                val path = "${volume.directory}/$dir"
                val file = File(path)
                
                if (file.exists() && file.isDirectory) {
                    resources.add(ResourceToAdd(
                        path = file.absolutePath,
                        name = file.name,
                        type = ResourceType.LOCAL,
                        fileCount = file.listFiles()?.size ?: 0
                    ))
                }
            }
        }
        
        resources
    }
}
```

---

### 67. scanNetwork() Method

**Signature**: `fun scanNetwork()`

**Purpose**: Discovers network hosts on local subnet (Flow emitter pattern).

**Implementation**:
```kotlin
fun scanNetwork() {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        _state.update { it.copy(
            isScanning = true,
            foundNetworkHosts = emptyList()
        ) }
        
        try {
            Timber.d("Starting network scan")
            
            // Execute scan (returns Flow)
            discoverNetworkResourcesUseCase.execute()
                .catch { e ->
                    Timber.e(e, "Network scan error")
                    _events.send(AddResourceEvent.ShowError(e.message ?: "Network scan failed"))
                }
                .collect { host ->
                    // Add each discovered host dynamically
                    _state.update { state ->
                        state.copy(
                            foundNetworkHosts = state.foundNetworkHosts + host
                        )
                    }
                    
                    Timber.d("Found host: ${host.hostname} (${host.ip})")
                }
            
            Timber.d("Network scan completed: ${_state.value.foundNetworkHosts.size} hosts found")
            
        } catch (e: Exception) {
            Timber.e(e, "Network scan failed")
            handleError(e)
        } finally {
            _state.update { it.copy(isScanning = false) }
        }
    }
}
```

**discoverNetworkResourcesUseCase**:
```kotlin
class DiscoverNetworkResourcesUseCase @Inject constructor() {
    fun execute(): Flow<NetworkHost> = flow {
        // Get device IP
        val localIp = NetworkUtils.getLocalIpAddress()
        val subnet = localIp.substringBeforeLast(".") + "."
        
        // Scan subnet (1-254)
        for (i in 1..254) {
            val ip = subnet + i
            
            // Check if host is reachable (ping)
            if (InetAddress.getByName(ip).isReachable(500)) {
                // Scan common ports
                val openPorts = scanPorts(ip, listOf(445, 139, 21, 22))
                
                if (openPorts.isNotEmpty()) {
                    // Get hostname
                    val hostname = try {
                        InetAddress.getByName(ip).hostName
                    } catch (e: Exception) {
                        ip
                    }
                    
                    // Emit host
                    emit(NetworkHost(
                        ip = ip,
                        hostname = hostname,
                        ports = openPorts
                    ))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
```

---

### 68. addSelectedResources() Method

**Signature**: `fun addSelectedResources()`

**Purpose**: Adds selected resources to database, triggers background speed tests.

**Implementation**:
```kotlin
fun addSelectedResources() {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        setLoading(true)
        
        try {
            // Filter selected resources
            val selectedPaths = _state.value.selectedPaths
            val selectedResources = _state.value.resourcesToAdd.filter { 
                it.path in selectedPaths 
            }
            
            // Validate
            if (selectedResources.isEmpty()) {
                _events.send(AddResourceEvent.ShowMessage("No resources selected"))
                return@launch
            }
            
            Timber.d("Adding ${selectedResources.size} selected resources")
            
            // Convert to MediaResource list
            val mediaResources = selectedResources.map { resourceToAdd ->
                MediaResource(
                    id = 0,
                    name = resourceToAdd.name,
                    path = resourceToAdd.path,
                    type = resourceToAdd.type,
                    isDestination = resourceToAdd.isDestination,
                    scanSubdirectories = resourceToAdd.scanSubdirectories,
                    isReadOnly = resourceToAdd.isReadOnly,
                    supportedMediaTypes = resourceToAdd.supportedMediaTypes,
                    createdDate = System.currentTimeMillis()
                )
            }
            
            // Add via UseCase
            addResourceUseCase.addMultiple(mediaResources)
            
            Timber.d("Added ${mediaResources.size} resources")
            
            // Trigger background speed tests (non-blocking)
            applicationScope.launch(ioDispatcher) {
                // Get all resources (to find IDs of newly inserted)
                val allResources = resourceRepository.getAllResources()
                
                // Find inserted resources by path matching
                val insertedResources = allResources.filter { resource ->
                    mediaResources.any { it.path == resource.path }
                }
                
                // Trigger speed test for each network resource
                insertedResources.forEach { resource ->
                    if (resource.type in listOf(
                        ResourceType.SMB,
                        ResourceType.SFTP,
                        ResourceType.FTP,
                        ResourceType.CLOUD
                    )) {
                        triggerSpeedTest(resource)
                    }
                }
            }
            
            // Send success event
            _events.send(AddResourceEvent.ResourcesAdded)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to add selected resources")
            handleError(e)
        } finally {
            setLoading(false)
        }
    }
}
```

---

### 69. triggerSpeedTest() Method

**Signature**: `private fun triggerSpeedTest(resource: MediaResource)`

**Purpose**: Launches non-blocking background speed test (survives activity destruction).

**Implementation**:
```kotlin
private fun triggerSpeedTest(resource: MediaResource) {
    applicationScope.launch(ioDispatcher) {
        try {
            Timber.d("Starting speed test for resource: ${resource.name}")
            
            // Execute speed test via UseCase
            val result = networkSpeedTestUseCase.execute(resource)
            
            when (result) {
                is Result.Success -> {
                    val metrics = result.data
                    
                    // Update resource with metrics
                    val updatedResource = resource.copy(
                        readSpeedMbps = metrics.readSpeedMbps,
                        writeSpeedMbps = metrics.writeSpeedMbps,
                        recommendedThreads = metrics.recommendedThreads,
                        lastSpeedTestDate = System.currentTimeMillis()
                    )
                    
                    // Save to database
                    resourceRepository.updateResource(updatedResource)
                    
                    Timber.d("Speed test completed: read=${metrics.readSpeedMbps} Mbps, write=${metrics.writeSpeedMbps} Mbps")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Speed test failed for ${resource.name}")
                    // Do not block resource addition
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Speed test exception for ${resource.name}")
            // Silent failure (does not block resource addition)
        }
    }
}
```

**networkSpeedTestUseCase**:
```kotlin
class NetworkSpeedTestUseCase @Inject constructor(
    private val smbOperationsUseCase: SmbOperationsUseCase,
    private val sftpOperationsUseCase: SftpOperationsUseCase
    // ... other operation usecases
) {
    suspend fun execute(resource: MediaResource): Result<SpeedTestMetrics> {
        return withContext(Dispatchers.IO) {
            try {
                // Test read speed (download 10MB test file)
                val readStartTime = System.currentTimeMillis()
                // ... download operation ...
                val readTime = System.currentTimeMillis() - readStartTime
                val readSpeedMbps = (10.0 * 8) / (readTime / 1000.0) // Convert to Mbps
                
                // Test write speed (upload 10MB test file)
                val writeStartTime = System.currentTimeMillis()
                // ... upload operation ...
                val writeTime = System.currentTimeMillis() - writeStartTime
                val writeSpeedMbps = (10.0 * 8) / (writeTime / 1000.0)
                
                // Calculate recommended threads based on speed
                val recommendedThreads = calculateThreads(readSpeedMbps, writeSpeedMbps)
                
                Result.Success(SpeedTestMetrics(
                    readSpeedMbps = readSpeedMbps,
                    writeSpeedMbps = writeSpeedMbps,
                    recommendedThreads = recommendedThreads
                ))
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
}
```

---

## State Update Methods (Toggle/Update)

### 70. toggleResourceSelection() Method

**Signature**: `fun toggleResourceSelection(resource: ResourceToAdd, selected: Boolean)`

**Implementation**:
```kotlin
fun toggleResourceSelection(resource: ResourceToAdd, selected: Boolean) {
    _state.update { state ->
        val updatedPaths = if (selected) {
            state.selectedPaths + resource.path
        } else {
            state.selectedPaths - resource.path
        }
        
        state.copy(selectedPaths = updatedPaths)
    }
    
    Timber.d("Resource selection toggled: ${resource.name}, selected=$selected")
}
```

---

### 71. updateResourceName() Method

**Signature**: `fun updateResourceName(resource: ResourceToAdd, newName: String)`

**Implementation**:
```kotlin
fun updateResourceName(resource: ResourceToAdd, newName: String) {
    _state.update { state ->
        val updatedResources = state.resourcesToAdd.map { r ->
            if (r.path == resource.path) {
                r.copy(name = newName)
            } else {
                r
            }
        }
        
        state.copy(resourcesToAdd = updatedResources)
    }
    
    Timber.d("Resource name updated: ${resource.path} -> $newName")
}
```

---

### 72. toggleDestination() Method

**Signature**: `fun toggleDestination(resource: ResourceToAdd, isDestination: Boolean)`

**Implementation**:
```kotlin
fun toggleDestination(resource: ResourceToAdd, isDestination: Boolean) {
    _state.update { state ->
        val updatedResources = state.resourcesToAdd.map { r ->
            if (r.path == resource.path) {
                r.copy(isDestination = isDestination)
            } else {
                r
            }
        }
        
        state.copy(resourcesToAdd = updatedResources)
    }
    
    Timber.d("Resource destination toggled: ${resource.name}, isDestination=$isDestination")
}
```

---

### 73. toggleScanSubdirectories() Method

**Signature**: `fun toggleScanSubdirectories(resource: ResourceToAdd, scanSubdirectories: Boolean)`

**Implementation**:
```kotlin
fun toggleScanSubdirectories(resource: ResourceToAdd, scanSubdirectories: Boolean) {
    _state.update { state ->
        val updatedResources = state.resourcesToAdd.map { r ->
            if (r.path == resource.path) {
                r.copy(scanSubdirectories = scanSubdirectories)
            } else {
                r
            }
        }
        
        state.copy(resourcesToAdd = updatedResources)
    }
    
    Timber.d("Resource scanSubdirectories toggled: ${resource.name}, scan=$scanSubdirectories")
}
```

---

### 74. toggleReadOnlyMode() Method

**Signature**: `fun toggleReadOnlyMode(resource: ResourceToAdd, isReadOnly: Boolean)`

**Purpose**: Toggle read-only mode. Forces `isDestination = false` when read-only enabled.

**Implementation**:
```kotlin
fun toggleReadOnlyMode(resource: ResourceToAdd, isReadOnly: Boolean) {
    _state.update { state ->
        val updatedResources = state.resourcesToAdd.map { r ->
            if (r.path == resource.path) {
                r.copy(
                    isReadOnly = isReadOnly,
                    // Force isDestination = false if read-only
                    isDestination = if (isReadOnly) false else r.isDestination
                )
            } else {
                r
            }
        }
        
        state.copy(resourcesToAdd = updatedResources)
    }
    
    Timber.d("Resource read-only toggled: ${resource.name}, isReadOnly=$isReadOnly, forced isDestination=false")
}
```

**Mutual Exclusion**: Read-only resources cannot be destinations (copy/move targets).

---

### 75. toggleMediaType() Method

**Signature**: `fun toggleMediaType(resource: ResourceToAdd, type: MediaType)`

**Purpose**: Add or remove media type from resource's supported types set.

**Implementation**:
```kotlin
fun toggleMediaType(resource: ResourceToAdd, type: MediaType) {
    _state.update { state ->
        val updatedResources = state.resourcesToAdd.map { r ->
            if (r.path == resource.path) {
                val currentTypes = r.supportedMediaTypes
                val updatedTypes = if (type in currentTypes) {
                    // Remove type
                    currentTypes - type
                } else {
                    // Add type
                    currentTypes + type
                }
                
                r.copy(supportedMediaTypes = updatedTypes)
            } else {
                r
            }
        }
        
        state.copy(resourcesToAdd = updatedResources)
    }
    
    Timber.d("Resource media type toggled: ${resource.name}, type=$type")
}
```

---

## observeData State Collection (Activity)

### 76. State Flow Collection

**Location**: `AddResourceActivity.observeData()`

**Logic**:
```kotlin
private fun observeData() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Collect state flow
            viewModel.state.collect { state ->
                // Filter resources by type
                val localResources = state.resourcesToAdd.filter { 
                    it.type == ResourceType.LOCAL 
                }
                val smbResources = state.resourcesToAdd.filter { 
                    it.type == ResourceType.SMB 
                }
                
                // Update adapters
                resourceToAddAdapter.submitList(localResources)
                resourceToAddAdapter.setSelectedPaths(state.selectedPaths)
                
                smbResourceToAddAdapter.submitList(smbResources)
                smbResourceToAddAdapter.setSelectedPaths(state.selectedPaths)
                
                // Visibility logic
                binding.tvResourcesToAdd.isVisible = localResources.isNotEmpty()
                binding.rvResourcesToAdd.isVisible = localResources.isNotEmpty()
                binding.btnAddToResources.isVisible = localResources.isNotEmpty()
                
                binding.tvSmbResourcesToAdd.isVisible = smbResources.isNotEmpty()
                binding.rvSmbResourcesToAdd.isVisible = smbResources.isNotEmpty()
                binding.btnSmbAddToResources.isVisible = smbResources.isNotEmpty()
            }
        }
    }
}
```

---

### 77. Visibility Control

**Purpose**: Show/hide RecyclerViews and buttons based on scan results.

**Logic**:
```kotlin
// Local section visible only if local resources found
binding.tvResourcesToAdd.isVisible = localResources.isNotEmpty()
binding.rvResourcesToAdd.isVisible = localResources.isNotEmpty()
binding.btnAddToResources.isVisible = localResources.isNotEmpty()

// SMB section visible only if SMB resources found
binding.tvSmbResourcesToAdd.isVisible = smbResources.isNotEmpty()
binding.rvSmbResourcesToAdd.isVisible = smbResources.isNotEmpty()
binding.btnSmbAddToResources.isVisible = smbResources.isNotEmpty()
```

---

### 78. Loading Flow Collection

**Location**: `AddResourceActivity.observeData()`

**Logic**:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.loading.collect { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }
}
```

---

### 79. Events Flow Collection

**Location**: `AddResourceActivity.observeData()`

**Logic**:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
            when (event) {
                is AddResourceEvent.ShowError -> {
                    showError(event.message)
                }
                is AddResourceEvent.ShowMessage -> {
                    Toast.makeText(
                        this@AddResourceActivity,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AddResourceEvent.ShowTestResult -> {
                    showTestResultDialog(event.message, event.isSuccess)
                }
                is AddResourceEvent.LoadResourceForCopy -> {
                    // Call helper to pre-fill form
                    helper.preFillResourceData(
                        resource = event.resource,
                        username = event.username,
                        password = event.password,
                        domain = event.domain,
                        sshKey = event.sshKey,
                        sshPassphrase = event.sshPassphrase
                    )
                }
                is AddResourceEvent.ResourcesAdded -> {
                    // Finish activity
                    finish()
                }
            }
        }
    }
}
```

**Event Types**:
1. `ShowError(message: String)` - Toast or dialog based on settings
2. `ShowMessage(message: String)` - Simple toast
3. `ShowTestResult(message: String, isSuccess: Boolean)` - Scrollable dialog with copy button
4. `LoadResourceForCopy(...)` - Pre-fills form for copy mode
5. `ResourcesAdded` - Finishes activity, returns to MainActivity

---

## Summary

**Items Documented**: 65-79 (15 behaviors)

**Key ViewModel Features**:
- Credential decryption for copy mode (item 65)
- Local folder scanning (item 66)
- Network scanning with Flow emitter (item 67)
- Resource addition with background speed tests (items 68-69)
- 6 state update methods for ResourceToAdd modifications (items 70-75)
- Activity observeData pattern with 3 flow collections (items 76-79)

**Key Patterns**:
- `applicationScope` for background jobs (survives activity)
- `viewModelScope` for UI-related coroutines
- `Flow` emitter for dynamic network scan
- State updates via `_state.update { }` immutable copy pattern
- Events via SharedFlow for one-time UI actions
