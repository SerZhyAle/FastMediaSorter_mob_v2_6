# AddResourceActivity - SFTP/FTP Section Behaviors

**Source**: Items 30-40 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `AddResourceActivity.kt` (lines 750-950)

---

## Protocol Selection & Switching

### 30. Protocol RadioGroup Listener

**Component**: `rgProtocol` (RadioGroup with `rbSftp` and `rbFtp`)

**Logic**:
```kotlin
rgProtocol.setOnCheckedChangeListener { _, checkedId ->
    when (checkedId) {
        R.id.rbSftp -> {
            // Auto-adjust port when switching to SFTP
            val currentPort = binding.etSftpPort.text.toString()
            if (currentPort.isBlank() || currentPort == "21") {
                binding.etSftpPort.setText("22")
            }
            Timber.d("Protocol switched to SFTP, port adjusted to 22")
        }
        R.id.rbFtp -> {
            // Auto-adjust port when switching to FTP
            val currentPort = binding.etSftpPort.text.toString()
            if (currentPort.isBlank() || currentPort == "22") {
                binding.etSftpPort.setText("21")
            }
            Timber.d("Protocol switched to FTP, port adjusted to 21")
        }
    }
}
```

**Purpose**: Prevents user confusion by automatically updating default port when switching protocols.

---

### 31. Auth Method RadioGroup Listener

**Component**: `rgSftpAuthMethod` (RadioGroup with `rbSftpPassword` and `rbSftpSshKey`)

**Logic**:
```kotlin
rgSftpAuthMethod.setOnCheckedChangeListener { _, checkedId ->
    when (checkedId) {
        R.id.rbSftpPassword -> {
            // Show password fields, hide SSH key fields
            binding.layoutSftpPasswordAuth.isVisible = true
            binding.layoutSftpSshKeyAuth.isVisible = false
            Timber.d("Auth method changed to Password")
        }
        R.id.rbSftpSshKey -> {
            // Show SSH key fields, hide password fields
            binding.layoutSftpPasswordAuth.isVisible = false
            binding.layoutSftpSshKeyAuth.isVisible = true
            Timber.d("Auth method changed to SSH Key")
        }
    }
}
```

**Layouts**:
- `layoutSftpPasswordAuth`: Contains `etSftpUsername`, `etSftpPassword`
- `layoutSftpSshKeyAuth`: Contains `etSftpUsername`, `etSftpPrivateKey`, `etSftpKeyPassphrase`, `btnSftpLoadKey`

---

## SSH Key Loading

### 32. btnSftpLoadKey Click Behavior

**Button**: `btnSftpLoadKey` (Material Button)

**Logic**:
```kotlin
binding.btnSftpLoadKey.setOnClickListener {
    UserActionLogger.logButtonClick("LoadSshKey", "AddResource")
    Timber.d("Loading SSH key file")
    
    // Launch file picker for SSH private key
    sshKeyFilePickerLauncher.launch(arrayOf("*/*"))
}
```

**ActivityResultLauncher**:
```kotlin
private val sshKeyFilePickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { loadSshKeyFromFile(it) }
}
```

---

### 33. loadSshKeyFromFile Method

**Signature**: `private fun loadSshKeyFromFile(uri: Uri)`

**Logic**:
```kotlin
private fun loadSshKeyFromFile(uri: Uri) {
    try {
        // Open input stream from URI
        contentResolver.openInputStream(uri)?.use { inputStream ->
            // Read entire file content
            val keyContent = inputStream.bufferedReader().use { it.readText() }
            
            // Validate key format (basic check)
            if (keyContent.contains("BEGIN") && keyContent.contains("PRIVATE KEY")) {
                // Set key content to EditText
                binding.etSftpPrivateKey.setText(keyContent)
                
                // Show success toast
                Toast.makeText(
                    this,
                    R.string.ssh_key_loaded,
                    Toast.LENGTH_SHORT
                ).show()
                
                Timber.d("SSH key loaded successfully, length: ${keyContent.length} bytes")
            } else {
                // Invalid key format
                Toast.makeText(
                    this,
                    R.string.invalid_ssh_key_format,
                    Toast.LENGTH_LONG
                ).show()
                
                Timber.w("Invalid SSH key format detected")
            }
        } ?: run {
            // Failed to open input stream
            Toast.makeText(
                this,
                R.string.sftp_key_load_error,
                Toast.LENGTH_LONG
            ).show()
            
            Timber.e("Failed to open input stream for SSH key file")
        }
    } catch (e: Exception) {
        // Handle exceptions (IOException, SecurityException, etc.)
        Toast.makeText(
            this,
            R.string.sftp_key_load_error,
            Toast.LENGTH_LONG
        ).show()
        
        Timber.e(e, "Exception while loading SSH key from file")
    }
}
```

**Supported Key Formats**:
- OpenSSH private key (BEGIN OPENSSH PRIVATE KEY)
- RSA private key (BEGIN RSA PRIVATE KEY)
- DSA private key (BEGIN DSA PRIVATE KEY)
- EC private key (BEGIN EC PRIVATE KEY)

**MIME Types**: Accepts `*/*` (any file type) as SSH keys often have no extension or `.pem`, `.key`, `.ppk` extensions.

---

## Custom Widget: RemotePathEditText

### 34. RemotePathEditText.getNormalizedPath() Method

**Custom Widget**: `RemotePathEditText` extends `TextInputEditText`

**Purpose**: Automatically normalizes Unix-style paths for SFTP/FTP servers.

**Method Implementation**:
```kotlin
fun getNormalizedPath(): String {
    val path = text.toString().trim()
    
    if (path.isEmpty()) return "/"
    
    var normalized = path
    
    // Convert backslashes to forward slashes
    normalized = normalized.replace('\\', '/')
    
    // Ensure path starts with /
    if (!normalized.startsWith('/')) {
        normalized = "/$normalized"
    }
    
    // Remove trailing slash (except root)
    if (normalized.length > 1 && normalized.endsWith('/')) {
        normalized = normalized.dropLast(1)
    }
    
    // Remove duplicate slashes
    normalized = normalized.replace(Regex("/+"), "/")
    
    return normalized
}
```

**Usage**:
```kotlin
val remotePath = binding.etSftpPath.getNormalizedPath()
// Input: "\\home\\user\\"
// Output: "/home/user"
```

**Validation**:
```kotlin
// Optional validation method
fun isValid(): Boolean {
    val path = getNormalizedPath()
    // Valid if starts with / and contains only allowed chars
    return path.matches(Regex("^/[a-zA-Z0-9_/.\\-]*$"))
}
```

---

## Protocol Selection Helpers

### 35. getSelectedProtocol() Method

**Signature**: `private fun getSelectedProtocol(): ResourceType`

**Logic**:
```kotlin
private fun getSelectedProtocol(): ResourceType {
    return when (binding.rgProtocol.checkedRadioButtonId) {
        R.id.rbSftp -> {
            Timber.d("Selected protocol: SFTP")
            ResourceType.SFTP
        }
        R.id.rbFtp -> {
            Timber.d("Selected protocol: FTP")
            ResourceType.FTP
        }
        else -> {
            // Default fallback (should never happen)
            Timber.w("No protocol selected, defaulting to SFTP")
            ResourceType.SFTP
        }
    }
}
```

**Used In**:
- `btnSftpTest.setOnClickListener` - Determines which test method to call
- `btnSftpAddResource.setOnClickListener` - Determines resource type to create

---

## Connection Testing

### 36. btnSftpTest Click Behavior

**Button**: `btnSftpTest` (Material Button)

**Logic**:
```kotlin
binding.btnSftpTest.setOnClickListener {
    UserActionLogger.logButtonClick("SftpTest", "AddResource")
    Timber.d("SFTP/FTP test connection initiated")
    
    // Validate host field
    if (!binding.etSftpHost.isValid()) {
        Toast.makeText(
            this,
            R.string.invalid_host_address,
            Toast.LENGTH_SHORT
        ).show()
        binding.etSftpHost.requestFocus()
        Timber.w("Invalid host address for SFTP/FTP test")
        return@setOnClickListener
    }
    
    // Validate host not empty
    val host = binding.etSftpHost.text.toString().trim()
    if (host.isEmpty()) {
        Toast.makeText(
            this,
            R.string.host_required,
            Toast.LENGTH_SHORT
        ).show()
        binding.etSftpHost.requestFocus()
        return@setOnClickListener
    }
    
    // Get protocol type
    val protocolType = getSelectedProtocol()
    
    // Get port (default based on protocol)
    val port = binding.etSftpPort.text.toString().toIntOrNull() 
        ?: if (protocolType == ResourceType.SFTP) 22 else 21
    
    // Get username
    val username = binding.etSftpUsername.text.toString().trim()
    
    // Determine auth method (SFTP only)
    if (protocolType == ResourceType.SFTP) {
        when (binding.rgSftpAuthMethod.checkedRadioButtonId) {
            R.id.rbSftpSshKey -> {
                // SSH Key authentication
                val privateKey = binding.etSftpPrivateKey.text.toString()
                if (privateKey.isEmpty()) {
                    Toast.makeText(
                        this,
                        R.string.ssh_key_required,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.etSftpPrivateKey.requestFocus()
                    Timber.w("SSH key empty for SFTP test")
                    return@setOnClickListener
                }
                
                val keyPassphrase = binding.etSftpKeyPassphrase.text.toString()
                    .takeIf { it.isNotBlank() }
                
                Timber.d("Testing SFTP connection with SSH key auth")
                viewModel.testSftpConnectionWithKey(
                    host = host,
                    port = port,
                    username = username,
                    privateKey = privateKey,
                    keyPassphrase = keyPassphrase
                )
            }
            R.id.rbSftpPassword -> {
                // Password authentication
                val password = binding.etSftpPassword.text.toString()
                
                Timber.d("Testing SFTP connection with password auth")
                viewModel.testSftpFtpConnection(
                    protocolType = ResourceType.SFTP,
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )
            }
        }
    } else {
        // FTP always uses password
        val password = binding.etSftpPassword.text.toString()
        
        Timber.d("Testing FTP connection")
        viewModel.testSftpFtpConnection(
            protocolType = ResourceType.FTP,
            host = host,
            port = port,
            username = username,
            password = password
        )
    }
}
```

---

### 37. ViewModel Test Methods

#### testSftpConnectionWithKey (SFTP with SSH Key)

**Signature**:
```kotlin
fun testSftpConnectionWithKey(
    host: String,
    port: Int,
    username: String,
    privateKey: String,
    keyPassphrase: String?
)
```

**Implementation**:
```kotlin
fun testSftpConnectionWithKey(
    host: String,
    port: Int,
    username: String,
    privateKey: String,
    keyPassphrase: String?
) {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        setLoading(true)
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Create SSHClient (SSHJ library)
            val ssh = SSHClient()
            
            // Configure SSH client
            ssh.addHostKeyVerifier { _, _, _ -> true } // Accept all (for testing)
            
            // Set timeout
            ssh.connectTimeout = 15000 // 15 seconds
            
            Timber.d("Connecting to SFTP server: $host:$port with SSH key")
            
            // Connect
            ssh.connect(host, port)
            
            // Load SSH key
            val keyProvider = if (keyPassphrase != null) {
                ssh.loadKeys(privateKey, keyPassphrase)
            } else {
                ssh.loadKeys(privateKey, null as String?)
            }
            
            // Authenticate
            ssh.authPublickey(username, keyProvider)
            
            // Start SFTP session
            val sftp = ssh.newSFTPClient()
            
            // Get server info
            val rootFiles = sftp.ls("/")
            val fileCount = rootFiles.size
            
            // Close connections
            sftp.close()
            ssh.disconnect()
            
            val connectionTime = System.currentTimeMillis() - startTime
            
            // Success message
            val message = buildString {
                appendLine("✓ Connection successful")
                appendLine()
                appendLine("Connection time: ${connectionTime}ms")
                appendLine("Protocol: SFTP")
                appendLine("Authentication: SSH Key")
                appendLine("Root directory files: $fileCount")
            }
            
            Timber.d("SFTP connection test successful: $connectionTime ms")
            _events.send(AddResourceEvent.ShowTestResult(message, true))
            
        } catch (e: Exception) {
            Timber.e(e, "SFTP connection test failed")
            
            val errorMessage = buildString {
                appendLine("✗ Connection failed")
                appendLine()
                appendLine("Error: ${e.message}")
                
                // Add specific error hints
                when (e) {
                    is java.net.ConnectException -> {
                        appendLine()
                        appendLine("Hints:")
                        appendLine("• Check host address and port")
                        appendLine("• Verify SFTP service is running")
                        appendLine("• Check firewall settings")
                    }
                    is net.schmizz.sshj.userauth.UserAuthException -> {
                        appendLine()
                        appendLine("Hints:")
                        appendLine("• Verify SSH key is correct")
                        appendLine("• Check key passphrase")
                        appendLine("• Ensure key is authorized on server")
                    }
                }
            }
            
            _events.send(AddResourceEvent.ShowTestResult(errorMessage, false))
        } finally {
            setLoading(false)
        }
    }
}
```

#### testSftpFtpConnection (Password Authentication)

**Signature**:
```kotlin
fun testSftpFtpConnection(
    protocolType: ResourceType,
    host: String,
    port: Int,
    username: String,
    password: String
)
```

**Implementation**: Similar to above but uses password authentication for SFTP or Apache Commons Net for FTP.

**FTP Logic**:
```kotlin
if (protocolType == ResourceType.FTP) {
    val ftp = FTPClient()
    
    try {
        ftp.connectTimeout = 15000
        ftp.connect(host, port)
        
        val loginSuccess = ftp.login(username, password)
        if (!loginSuccess) {
            throw Exception("FTP login failed")
        }
        
        // Set passive mode (prevents firewall issues)
        ftp.enterLocalPassiveMode()
        
        // List root directory
        val files = ftp.listFiles("/")
        
        ftp.logout()
        ftp.disconnect()
        
        // Success message...
    } catch (e: Exception) {
        // Error handling...
    }
}
```

---

## Media Types Selection

### 38. getSftpSupportedTypes() Method

**Signature**: `private fun getSftpSupportedTypes(): Set<MediaType>`

**Logic**:
```kotlin
private fun getSftpSupportedTypes(): Set<MediaType> {
    val types = mutableSetOf<MediaType>()
    
    // Check each checkbox
    if (binding.cbSftpSupportImages.isChecked) types.add(MediaType.IMAGE)
    if (binding.cbSftpSupportVideos.isChecked) types.add(MediaType.VIDEO)
    if (binding.cbSftpSupportAudio.isChecked) types.add(MediaType.AUDIO)
    if (binding.cbSftpSupportGif.isChecked) types.add(MediaType.GIF)
    if (binding.cbSftpSupportText.isChecked) types.add(MediaType.TEXT)
    if (binding.cbSftpSupportPdf.isChecked) types.add(MediaType.PDF)
    if (binding.cbSftpSupportEpub.isChecked) types.add(MediaType.EPUB)
    
    Timber.d("SFTP supported types: $types")
    return types
}
```

**Called By**: `btnSftpAddResource.setOnClickListener` before adding resource.

---

## Resource Addition

### 39. btnSftpAddResource Click Behavior

**Button**: `btnSftpAddResource` (Material Button)

**Logic**:
```kotlin
binding.btnSftpAddResource.setOnClickListener {
    UserActionLogger.logButtonClick("AddSftp", "AddResource")
    Timber.d("Adding SFTP/FTP resource")
    
    // Validate host
    if (!binding.etSftpHost.isValid()) {
        Toast.makeText(this, R.string.invalid_host_address, Toast.LENGTH_SHORT).show()
        binding.etSftpHost.requestFocus()
        return@setOnClickListener
    }
    
    val host = binding.etSftpHost.text.toString().trim()
    if (host.isEmpty()) {
        Toast.makeText(this, R.string.host_required, Toast.LENGTH_SHORT).show()
        binding.etSftpHost.requestFocus()
        return@setOnClickListener
    }
    
    // Get protocol
    val protocolType = getSelectedProtocol()
    
    // Get port
    val port = binding.etSftpPort.text.toString().toIntOrNull()
        ?: if (protocolType == ResourceType.SFTP) 22 else 21
    
    // Get remote path (normalized)
    val remotePath = binding.etSftpPath.getNormalizedPath().ifEmpty { "/" }
    
    // Get resource name (optional, uses path basename if empty)
    val resourceName = binding.etSftpResourceName.text.toString().trim()
        .ifEmpty { remotePath.substringAfterLast('/').ifEmpty { host } }
    
    // Get comment (optional)
    val comment = binding.etSftpComment.text.toString().trim()
    
    // Get supported media types
    val supportedTypes = getSftpSupportedTypes()
    if (supportedTypes.isEmpty()) {
        Toast.makeText(
            this,
            R.string.select_at_least_one_media_type,
            Toast.LENGTH_SHORT
        ).show()
        return@setOnClickListener
    }
    
    // Get flags
    val addToDestinations = binding.cbSftpAddToDestinations.isChecked
    val scanSubdirectories = binding.cbSftpScanSubdirectories.isChecked
    val isReadOnly = binding.cbSftpReadOnlyMode.isChecked
    
    // Get username
    val username = binding.etSftpUsername.text.toString().trim()
    
    // Determine auth method
    if (protocolType == ResourceType.SFTP) {
        when (binding.rgSftpAuthMethod.checkedRadioButtonId) {
            R.id.rbSftpSshKey -> {
                // SSH Key auth
                val privateKey = binding.etSftpPrivateKey.text.toString()
                if (privateKey.isEmpty()) {
                    Toast.makeText(this, R.string.ssh_key_required, Toast.LENGTH_SHORT).show()
                    binding.etSftpPrivateKey.requestFocus()
                    return@setOnClickListener
                }
                
                val keyPassphrase = binding.etSftpKeyPassphrase.text.toString()
                    .takeIf { it.isNotBlank() }
                
                viewModel.addSftpResourceWithKey(
                    host = host,
                    port = port,
                    username = username,
                    privateKey = privateKey,
                    keyPassphrase = keyPassphrase,
                    remotePath = remotePath,
                    resourceName = resourceName,
                    comment = comment,
                    addToDestinations = addToDestinations,
                    scanSubdirectories = scanSubdirectories,
                    isReadOnly = isReadOnly,
                    supportedTypes = supportedTypes
                )
            }
            R.id.rbSftpPassword -> {
                // Password auth
                val password = binding.etSftpPassword.text.toString()
                
                viewModel.addSftpFtpResource(
                    protocolType = ResourceType.SFTP,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    remotePath = remotePath,
                    resourceName = resourceName,
                    comment = comment,
                    addToDestinations = addToDestinations,
                    scanSubdirectories = scanSubdirectories,
                    isReadOnly = isReadOnly,
                    supportedTypes = supportedTypes
                )
            }
        }
    } else {
        // FTP (password only)
        val password = binding.etSftpPassword.text.toString()
        
        viewModel.addSftpFtpResource(
            protocolType = ResourceType.FTP,
            host = host,
            port = port,
            username = username,
            password = password,
            remotePath = remotePath,
            resourceName = resourceName,
            comment = comment,
            addToDestinations = addToDestinations,
            scanSubdirectories = scanSubdirectories,
            isReadOnly = isReadOnly,
            supportedTypes = supportedTypes
        )
    }
}
```

---

### 40. ViewModel Add Methods

#### addSftpResourceWithKey (SSH Key Auth)

**Signature**:
```kotlin
fun addSftpResourceWithKey(
    host: String,
    port: Int,
    username: String,
    privateKey: String,
    keyPassphrase: String?,
    remotePath: String,
    resourceName: String,
    comment: String,
    addToDestinations: Boolean,
    scanSubdirectories: Boolean,
    isReadOnly: Boolean,
    supportedTypes: Set<MediaType>
)
```

**Implementation**:
```kotlin
viewModelScope.launch(ioDispatcher + exceptionHandler) {
    setLoading(true)
    
    try {
        // Create resource path
        val path = "sftp://$host:$port$remotePath"
        
        // Create MediaResource
        val resource = MediaResource(
            id = 0, // Auto-increment
            name = resourceName,
            path = path,
            type = ResourceType.SFTP,
            comment = comment,
            isDestination = addToDestinations && !isReadOnly,
            scanSubdirectories = scanSubdirectories,
            isReadOnly = isReadOnly,
            supportedMediaTypes = supportedTypes,
            createdDate = System.currentTimeMillis(),
            credentialsId = 0 // Will be assigned after credentials save
        )
        
        // Save resource + credentials via UseCase
        val savedResource = addResourceUseCase.addSftpWithKey(
            resource = resource,
            host = host,
            port = port,
            username = username,
            privateKey = privateKey,
            keyPassphrase = keyPassphrase
        )
        
        Timber.d("SFTP resource added successfully: ${savedResource.name}")
        
        // Trigger background speed test
        applicationScope.launch(ioDispatcher) {
            triggerSpeedTest(savedResource)
        }
        
        // Send success event
        _events.send(AddResourceEvent.ResourcesAdded)
        
    } catch (e: Exception) {
        Timber.e(e, "Failed to add SFTP resource")
        handleError(e)
    } finally {
        setLoading(false)
    }
}
```

#### addSftpFtpResource (Password Auth)

Similar implementation but saves password credentials instead of SSH key.

---

## Summary

**Items Documented**: 30-40 (11 behaviors)

**Key Features**:
- Protocol switching with auto-port adjustment
- SSH key file loading with validation
- Remote path normalization (custom widget)
- Dual authentication support (password + SSH key)
- Connection testing with detailed diagnostics
- Media type selection
- Resource creation with credential encryption
