# AddResourceActivity - Error Handling, Logging & Security

**Source**: Items 80-95 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**Files**: `AddResourceActivity.kt` (1175 lines), `AddResourceViewModel.kt` (1435 lines)

---

## Error Handling

### 80. showError() Method

**Signature**: `private fun showError(message: String)`

**Purpose**: Shows detailed errors in scrollable dialog or simple toast based on user settings.

**Implementation**:
```kotlin
private fun showError(message: String) {
    if (settingsManager.showDetailedErrors) {
        // Show scrollable dialog for long error messages
        DialogUtils.showScrollableDialog(
            context = this,
            title = getString(R.string.error),
            message = message,
            positiveButton = getString(R.string.ok),
            onPositiveClick = { dialog -> dialog.dismiss() }
        )
        
        Timber.e("Error shown in dialog: $message")
    } else {
        // Show simple toast
        Toast.makeText(
            this,
            getString(R.string.operation_failed),
            Toast.LENGTH_SHORT
        ).show()
        
        Timber.e("Error shown in toast (details hidden): $message")
    }
}
```

**Settings Check**:
```kotlin
// SettingsManager.kt
val showDetailedErrors: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_DETAILED_ERRORS, true)
```

**DialogUtils.showScrollableDialog**:
```kotlin
object DialogUtils {
    fun showScrollableDialog(
        context: Context,
        title: String,
        message: String,
        positiveButton: String,
        onPositiveClick: (dialog: AlertDialog) -> Unit
    ) {
        // Create ScrollView with TextView
        val scrollView = ScrollView(context).apply {
            val textView = TextView(context).apply {
                text = message
                textSize = 14f
                setPadding(16.dp, 16.dp, 16.dp, 16.dp)
                setTextIsSelectable(true) // Allow text selection
            }
            addView(textView)
        }
        
        // Show dialog
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton(positiveButton) { dialog, _ -> 
                onPositiveClick(dialog as AlertDialog)
            }
            .show()
    }
}
```

---

### 81. showTestResultDialog() Method

**Signature**: `private fun showTestResultDialog(message: String, isSuccess: Boolean)`

**Purpose**: Shows test result (success/error) with copy-to-clipboard button.

**Implementation**:
```kotlin
private fun showTestResultDialog(message: String, isSuccess: Boolean) {
    // Create TextView with selectable text
    val textView = TextView(this).apply {
        text = message
        textSize = 14f
        setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        setTextIsSelectable(true)
    }
    
    // Create ScrollView wrapper
    val scrollView = ScrollView(this).apply {
        addView(textView)
    }
    
    // Show dialog
    AlertDialog.Builder(this)
        .setTitle(
            if (isSuccess) 
                getString(R.string.test_successful) 
            else 
                getString(R.string.test_failed)
        )
        .setView(scrollView)
        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        .setNeutralButton(R.string.copy) { _, _ ->
            // Copy to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Test Result", message)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(
                this,
                R.string.copied_to_clipboard,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("Test result copied to clipboard")
        }
        .show()
    
    Timber.d("Test result dialog shown: isSuccess=$isSuccess")
}
```

**Extension Property** (dp conversion):
```kotlin
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
```

---

## Logging (Timber)

### 82. UserActionLogger Calls

**Location**: Throughout `AddResourceActivity.kt`

**14 Events with screen="AddResource"**:

```kotlin
// 1. Local Folder Selection
binding.cardLocalFolder.setOnClickListener {
    userActionLogger.logAction("LocalFolderCard", "AddResource")
    // ...
}

// 2. Folder Selected
folderPickerLauncher.launch(intent) { result ->
    userActionLogger.logAction("FolderSelected", "AddResource", result.data?.uri?.toString())
    // ...
}

// 3. SMB Network Card
binding.cardSmbNetwork.setOnClickListener {
    userActionLogger.logAction("SmbNetworkCard", "AddResource")
    // ...
}

// 4. Network Scan Button
binding.btnScanNetwork.setOnClickListener {
    userActionLogger.logAction("ScanNetwork", "AddResource")
    // ...
}

// 5. SFTP/FTP Protocol Card
binding.cardSftpFtp.setOnClickListener {
    userActionLogger.logAction("SftpFtpCard", "AddResource")
    // ...
}

// 6. SSH Key Loading
binding.btnSftpLoadKey.setOnClickListener {
    userActionLogger.logAction("LoadSshKey", "AddResource")
    // ...
}

// 7. SFTP Test Connection
binding.btnSftpTest.setOnClickListener {
    userActionLogger.logAction("SftpTestConnection", "AddResource")
    // ...
}

// 8. SFTP Add Resource
binding.btnSftpAddResource.setOnClickListener {
    userActionLogger.logAction("SftpAddResource", "AddResource")
    // ...
}

// 9. Google Drive Card
binding.cardGoogleDrive.setOnClickListener {
    userActionLogger.logAction("GoogleDriveCard", "AddResource")
    // ...
}

// 10. Dropbox Card
binding.cardDropbox.setOnClickListener {
    userActionLogger.logAction("DropboxCard", "AddResource")
    // ...
}

// 11. OneDrive Card
binding.cardOneDrive.setOnClickListener {
    userActionLogger.logAction("OneDriveCard", "AddResource")
    // ...
}

// 12. SMB Test Connection
binding.btnTestSmbConnection.setOnClickListener {
    userActionLogger.logAction("SmbTestConnection", "AddResource")
    // ...
}

// 13. SMB Add Resource
binding.btnSmbAddResource.setOnClickListener {
    userActionLogger.logAction("SmbAddResource", "AddResource")
    // ...
}

// 14. Add Selected Resources (from scan)
binding.btnAddToResources.setOnClickListener {
    userActionLogger.logAction("AddSelectedResources", "AddResource")
    // ...
}
```

**UserActionLogger Implementation**:
```kotlin
class UserActionLogger @Inject constructor(
    private val database: AppDatabase
) {
    fun logAction(
        action: String,
        screen: String,
        details: String? = null
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val log = ActionLog(
                action = action,
                screen = screen,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            
            database.actionLogDao().insert(log)
        }
    }
}
```

---

### 83. Timber Debug Logs (12+ Messages)

**Key Debug Messages**:

```kotlin
// 1. Folder Selected
Timber.d("Folder selected: $uri")

// 2. Persistable Permissions Taken
Timber.d("Persistable URI permissions taken for: $uri")

// 3. Resource Added (Local)
Timber.d("Local resource added: $name at $uri")

// 4. SMB Connection Test Started
Timber.d("Testing SMB connection to: $server/$shareName")

// 5. SMB Connection Successful
Timber.d("SMB connection successful: $server/$shareName")

// 6. SMB Resource Added
Timber.d("SMB resource added: $name at smb://$server/$shareName")

// 7. SFTP Connection Test Started
Timber.d("Testing SFTP connection to: $host:$port")

// 8. SFTP Connection Successful
Timber.d("SFTP connection successful: $host:$port, auth=$authMethod")

// 9. SFTP Resource Added
Timber.d("SFTP resource added: $name at sftp://$host:$port/$remotePath")

// 10. Google OAuth Success
Timber.d("Google Drive authentication successful: ${account.email}")

// 11. Dropbox OAuth Success
Timber.d("Dropbox authentication successful, token saved")

// 12. OneDrive OAuth Success
Timber.d("OneDrive authentication successful: ${result.account.username}")

// 13. Network Scan Started
Timber.d("Starting network scan")

// 14. Network Host Found
Timber.d("Found host: ${host.hostname} (${host.ip})")

// 15. Resources Added (Multiple)
Timber.d("Added ${mediaResources.size} resources")

// 16. Speed Test Completed
Timber.d("Speed test completed: read=${metrics.readSpeedMbps} Mbps, write=${metrics.writeSpeedMbps} Mbps")
```

---

### 84. Timber Warning Logs

**Fallback to Temporary Permissions**:
```kotlin
try {
    // Try to get persistable permissions
    contentResolver.takePersistableUriPermission(uri, takeFlags)
    Timber.d("Persistable URI permissions taken for: $uri")
} catch (e: SecurityException) {
    // Fallback: Use temporary permissions (valid until app restart)
    Timber.w("Could not take persistable permissions, using temporary: ${e.message}")
}
```

**Context**: Android 11+ restricts persistable permissions for some URIs. App falls back to temporary permissions which work until process death.

---

### 85. Timber Error Logs

**Permission Failed**:
```kotlin
catch (e: SecurityException) {
    Timber.e(e, "Failed to take URI permissions for: $uri")
    showError(getString(R.string.folder_permission_failed))
}
```

**Resource Not Found for Copy**:
```kotlin
if (resource == null) {
    Timber.e("Resource not found for copy: $resourceId")
    _events.send(AddResourceEvent.ShowError("Resource not found"))
    return@launch
}
```

**SMB Connection Failed**:
```kotlin
catch (e: Exception) {
    Timber.e(e, "SMB connection test failed")
    _events.send(AddResourceEvent.ShowTestResult(
        message = "Connection failed: ${e.message}",
        isSuccess = false
    ))
}
```

**SFTP Connection Failed**:
```kotlin
catch (e: Exception) {
    Timber.e(e, "SFTP connection test failed")
    _events.send(AddResourceEvent.ShowTestResult(
        message = "Connection failed: ${e.message}",
        isSuccess = false
    ))
}
```

**Network Scan Failed**:
```kotlin
catch (e: Exception) {
    Timber.e(e, "Network scan failed")
    handleError(e)
}
```

**Resource Addition Failed**:
```kotlin
catch (e: Exception) {
    Timber.e(e, "Failed to add resource")
    handleError(e)
}
```

---

## Intent & Dependencies

### 86. createIntent() Method (Companion Object)

**Signature**: `fun createIntent(context: Context, copyResourceId: Long? = null, preselectedTab: Int = 0): Intent`

**Purpose**: Factory method for creating Intent with extras.

**Implementation**:
```kotlin
companion object {
    // Intent extras
    const val EXTRA_COPY_RESOURCE_ID = "extra_copy_resource_id"
    const val EXTRA_PRESELECTED_TAB = "extra_preselected_tab"
    
    // Tab indices
    const val TAB_LOCAL = 0
    const val TAB_SMB = 1
    const val TAB_SFTP_FTP = 2
    const val TAB_CLOUD = 3
    
    /**
     * Creates Intent for AddResourceActivity.
     * 
     * @param context Context for Intent creation
     * @param copyResourceId Optional resource ID to copy from
     * @param preselectedTab Tab index to show on launch (0=Local, 1=SMB, 2=SFTP/FTP, 3=Cloud)
     * @return Intent with extras
     */
    fun createIntent(
        context: Context,
        copyResourceId: Long? = null,
        preselectedTab: Int = TAB_LOCAL
    ): Intent {
        return Intent(context, AddResourceActivity::class.java).apply {
            copyResourceId?.let {
                putExtra(EXTRA_COPY_RESOURCE_ID, it)
            }
            putExtra(EXTRA_PRESELECTED_TAB, preselectedTab)
        }
    }
}
```

**Usage**:
```kotlin
// From ResourcesAdapter (Copy button)
val intent = AddResourceActivity.createIntent(
    context = context,
    copyResourceId = resource.id,
    preselectedTab = when (resource.type) {
        ResourceType.LOCAL -> AddResourceActivity.TAB_LOCAL
        ResourceType.SMB -> AddResourceActivity.TAB_SMB
        ResourceType.SFTP, ResourceType.FTP -> AddResourceActivity.TAB_SFTP_FTP
        ResourceType.CLOUD -> AddResourceActivity.TAB_CLOUD
    }
)
context.startActivity(intent)
```

---

### 87. Extracting Intent Extras (onCreate)

**Location**: `AddResourceActivity.onCreate()`

**Logic**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ...
    
    // Extract intent extras
    val copyResourceId = intent.getLongExtra(EXTRA_COPY_RESOURCE_ID, -1L)
    val preselectedTab = intent.getIntExtra(EXTRA_PRESELECTED_TAB, TAB_LOCAL)
    
    // Check if Copy Mode
    if (copyResourceId != -1L) {
        // Load resource for copy
        viewModel.loadResourceForCopy(copyResourceId)
        Timber.d("Copy mode: loading resource $copyResourceId")
    }
    
    // Set preselected tab
    if (preselectedTab in 0..3) {
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(preselectedTab))
        Timber.d("Preselected tab: $preselectedTab")
    }
}
```

---

### 88. Hilt Injection (@AndroidEntryPoint)

**Annotation**:
```kotlin
@AndroidEntryPoint
class AddResourceActivity : AppCompatActivity() {
    // ...
}
```

**Purpose**: Enables Hilt dependency injection for Activity.

---

### 89. @Inject lateinit var (ViewModel)

**ViewModel Injection**:
```kotlin
@Inject
lateinit var viewModelFactory: AddResourceViewModelFactory

private val viewModel: AddResourceViewModel by viewModels { viewModelFactory }
```

**Note**: Starting from Hilt 2.44, can simplify to:
```kotlin
private val viewModel: AddResourceViewModel by viewModels()
```

---

### 90. @Inject lateinit var (Cloud Clients)

**Cloud SDK Injection**:
```kotlin
@Inject
lateinit var googleDriveClient: GoogleDriveClient

@Inject
lateinit var dropboxClient: DropboxClient

@Inject
lateinit var oneDriveClient: OneDriveClient
```

**Hilt Module**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CloudModule {
    
    @Provides
    @Singleton
    fun provideGoogleDriveClient(
        context: Context
    ): GoogleDriveClient {
        return GoogleDriveClient(context)
    }
    
    @Provides
    @Singleton
    fun provideDropboxClient(
        context: Context,
        sharedPreferences: SharedPreferences
    ): DropboxClient {
        return DropboxClient(context, sharedPreferences)
    }
    
    @Provides
    @Singleton
    fun provideOneDriveClient(
        context: Context,
        coroutineDispatcher: CoroutineDispatcher
    ): OneDriveClient {
        return OneDriveClient(context, coroutineDispatcher)
    }
}
```

---

### 91. ActivityResultContracts

**Three Contracts**:

**1. Folder Picker (OpenDocumentTree)**:
```kotlin
private val folderPickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
) { uri: Uri? ->
    uri?.let {
        handleFolderSelected(it)
    }
}
```

**2. SSH Key File Picker (OpenDocument)**:
```kotlin
private val sshKeyPickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    uri?.let {
        loadSshKeyFromFile(it)
    }
}
```

**3. Google Sign-In (StartActivityForResult)**:
```kotlin
private val googleSignInLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        handleGoogleSignInResult(result.data)
    } else {
        Timber.w("Google Sign-In cancelled: resultCode=${result.resultCode}")
    }
}
```

---

### 92. Library Dependencies

**build.gradle.kts (app_v2/)**:
```kotlin
dependencies {
    // Google Sign-In SDK
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    
    // Dropbox Core SDK
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
    
    // Microsoft MSAL (OneDrive authentication)
    implementation("com.microsoft.identity.client:msal:4.9.0")
    
    // SMBJ (SMB client)
    implementation("com.hierynomus:smbj:0.12.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") // BouncyCastle for encryption
    
    // SSHJ (SFTP client)
    implementation("com.hierynomus:sshj:0.37.0")
    implementation("net.i2p.crypto:eddsa:0.3.0") // EdDSA for Curve25519 keys
    
    // Apache Commons Net (FTP client)
    implementation("commons-net:commons-net:3.10.0")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Timber Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Activity KTX (viewModels delegate)
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Lifecycle KTX (repeatOnLifecycle)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

---

## Security

### 93. Android Keystore Encryption (AES-256-GCM)

**CredentialsEncryptor Implementation**:
```kotlin
class CredentialsEncryptor @Inject constructor() {
    
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    companion object {
        private const val KEY_ALIAS = "FastMediaSorterCredentialsKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
    
    /**
     * Encrypts sensitive data using Android Keystore AES-256-GCM.
     * Returns Base64-encoded string: <IV>:<ciphertext>
     */
    fun encrypt(plainText: String): String {
        // Get or create key
        val secretKey = getOrCreateSecretKey()
        
        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        // Encrypt
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Encode IV and ciphertext
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        
        return "$ivBase64:$cipherBase64"
    }
    
    /**
     * Decrypts data encrypted with encrypt().
     */
    fun decrypt(encryptedData: String): String {
        // Parse IV and ciphertext
        val parts = encryptedData.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }
        
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
        
        // Get secret key
        val secretKey = getOrCreateSecretKey()
        val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        // Decrypt
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val plainText = cipher.doFinal(cipherText)
        
        return String(plainText, Charsets.UTF_8)
    }
    
    /**
     * Gets existing key or creates new one in Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        // Check if key exists
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null)
        if (existingEntry is KeyStore.SecretKeyEntry) {
            return existingEntry.secretKey
        }
        
        // Create new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256) // AES-256
            .setUserAuthenticationRequired(false) // No biometric prompt
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
```

**Usage**:
```kotlin
// Encrypt password before saving to database
val encryptedPassword = credentialsEncryptor.encrypt(plainPassword)
credentialsEntity.password = encryptedPassword

// Decrypt password when loading
val plainPassword = credentialsEncryptor.decrypt(credentialsEntity.password)
```

---

### 94. Separate CredentialsEntity Table

**Database Schema**:
```kotlin
@Entity(tableName = "credentials")
data class CredentialsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Encrypted fields (all AES-256-GCM encrypted)
    val username: String? = null,
    val password: String? = null, // Base64 <IV>:<ciphertext>
    val domain: String? = null,
    val privateKey: String? = null, // SSH key (encrypted)
    val keyPassphrase: String? = null, // SSH key passphrase (encrypted)
    val accessToken: String? = null, // OAuth token (encrypted)
    val refreshToken: String? = null // OAuth refresh token (encrypted)
)

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // ...
    
    // Foreign key to CredentialsEntity
    val credentialsId: Long? = null
)
```

**Why Separate Table**:
1. Security: Credentials isolated from main table (reduces exposure in logs/debug)
2. Performance: Resources table smaller, faster queries
3. Atomicity: Can update credentials without touching resource data
4. Reusability: Multiple resources can share same credentials (future feature)

---

### 95. Persistable URI Permissions (Android SAF)

**Purpose**: Grant long-term access to user-selected folders (survives app restart).

**Implementation**:
```kotlin
private fun handleFolderSelected(uri: Uri) {
    try {
        // Define permission flags
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                             Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        
        // Take persistable permissions
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        
        Timber.d("Persistable URI permissions taken for: $uri")
        
        // Save resource to database with URI
        val resource = MediaResource(
            path = uri.toString(), // content://...
            // ...
        )
        
        viewModel.addLocalResource(resource)
        
    } catch (e: SecurityException) {
        // Fallback: Use temporary permissions (valid until app restart)
        Timber.w("Could not take persistable permissions, using temporary: ${e.message}")
        
        // Still add resource (will require re-selection after app restart)
        val resource = MediaResource(
            path = uri.toString(),
            // ...
        )
        
        viewModel.addLocalResource(resource)
    }
}
```

**Checking Existing Permissions**:
```kotlin
private fun hasUriPermission(uri: Uri): Boolean {
    val persistedUris = contentResolver.persistedUriPermissions
    return persistedUris.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
}
```

**Releasing Permissions** (when resource deleted):
```kotlin
fun releaseUriPermission(uri: Uri) {
    try {
        contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        
        Timber.d("Released URI permissions for: $uri")
    } catch (e: Exception) {
        Timber.e(e, "Failed to release URI permissions")
    }
}
```

**Context**: Android 11+ (API 30) restricts broad storage access. Apps must use Storage Access Framework (SAF) with persistable URI permissions for long-term folder access.

---

## Summary

**Items Documented**: 80-95 (16 behaviors)

**Key Security Features**:
- Android Keystore AES-256-GCM encryption (items 93)
- Separate credentials table (item 94)
- Persistable URI permissions (item 95)
- No credentials in logs (only encrypted strings)

**Error Handling**:
- Scrollable dialog for detailed errors (item 80)
- Test result dialog with copy button (item 81)
- Settings-based error verbosity

**Logging**:
- 14 UserActionLogger events (item 82)
- 16+ Timber debug messages (item 83)
- Warning logs for fallback scenarios (item 84)
- Error logs with exception details (item 85)

**Dependencies**:
- createIntent factory method (item 86)
- Intent extras extraction (item 87)
- Hilt injection (items 88-90)
- ActivityResultContracts (item 91)
- 10+ library dependencies (item 92)
