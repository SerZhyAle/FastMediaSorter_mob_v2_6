# AddResourceActivity - Cloud Storage OAuth Behaviors

**Source**: Items 41-64 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `AddResourceActivity.kt` (lines 950-1175)

---

## Cloud Storage Status Management

### 41. updateCloudStorageStatus() Method

**Signature**: `private fun updateCloudStorageStatus()`

**Called By**: `showCloudStorageOptions()` when Cloud Storage section is shown

**Purpose**: Updates status TextViews for all 3 cloud providers (Google Drive, Dropbox, OneDrive)

**Logic**:
```kotlin
private fun updateCloudStorageStatus() {
    Timber.d("Updating cloud storage status")
    
    // Update Google Drive status
    updateGoogleDriveStatus()
    
    // Update Dropbox status (coroutine)
    lifecycleScope.launch {
        updateDropboxStatus()
    }
    
    // Update OneDrive status (coroutine)
    lifecycleScope.launch {
        updateOneDriveStatus()
    }
}
```

---

### 42. Google Drive Status Check

**Method**: `private fun updateGoogleDriveStatus()`

**Logic**:
```kotlin
private fun updateGoogleDriveStatus() {
    // Get last signed-in account (synchronous)
    val account = GoogleSignIn.getLastSignedInAccount(this)
    
    if (account != null) {
        // Connected
        val email = account.email ?: "Unknown"
        binding.tvGoogleDriveStatus.text = getString(R.string.connected_as, email)
        binding.tvGoogleDriveStatus.setTextColor(getColor(R.color.success_green))
        
        Timber.d("Google Drive status: Connected as $email")
    } else {
        // Not connected
        binding.tvGoogleDriveStatus.text = getString(R.string.not_connected)
        binding.tvGoogleDriveStatus.setTextColor(getColor(R.color.error_red))
        
        Timber.d("Google Drive status: Not connected")
    }
}
```

**String Resources**:
- `R.string.connected_as`: "Connected as %s"
- `R.string.not_connected`: "Not connected"

---

### 43. Dropbox Status Check (Coroutine)

**Method**: `private suspend fun updateDropboxStatus()`

**Logic**:
```kotlin
private suspend fun updateDropboxStatus() {
    try {
        // Try to restore from storage (checks for saved access token)
        val restoreResult = dropboxClient.tryRestoreFromStorage()
        
        if (restoreResult) {
            // Restoration successful, now test connection
            when (val testResult = dropboxClient.testConnection()) {
                is CloudResult.Success -> {
                    // Get email from metadata
                    val email = testResult.data as? String ?: "Unknown"
                    
                    withContext(Dispatchers.Main) {
                        binding.tvDropboxStatus.text = getString(R.string.connected_as, email)
                        binding.tvDropboxStatus.setTextColor(getColor(R.color.success_green))
                    }
                    
                    Timber.d("Dropbox status: Connected as $email")
                }
                is CloudResult.Error -> {
                    // Test failed (token expired or network error)
                    withContext(Dispatchers.Main) {
                        binding.tvDropboxStatus.text = getString(R.string.not_connected)
                        binding.tvDropboxStatus.setTextColor(getColor(R.color.error_red))
                    }
                    
                    Timber.w("Dropbox connection test failed: ${testResult.message}")
                }
            }
        } else {
            // Restoration failed (no saved token)
            withContext(Dispatchers.Main) {
                binding.tvDropboxStatus.text = getString(R.string.not_connected)
                binding.tvDropboxStatus.setTextColor(getColor(R.color.error_red))
            }
            
            Timber.d("Dropbox status: Not connected (no saved token)")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            binding.tvDropboxStatus.text = getString(R.string.not_connected)
            binding.tvDropboxStatus.setTextColor(getColor(R.color.error_red))
        }
        
        Timber.e(e, "Error checking Dropbox status")
    }
}
```

**DropboxClient Methods**:
- `tryRestoreFromStorage()`: Returns Boolean (success/failure)
- `testConnection()`: Returns `CloudResult<String>` with email on success

---

### 44. OneDrive Status Check (Coroutine)

**Method**: `private suspend fun updateOneDriveStatus()`

**Logic**:
```kotlin
private suspend fun updateOneDriveStatus() {
    try {
        // Check if authenticated (checks MSAL cache)
        val isAuthenticated = oneDriveClient.isAuthenticated()
        
        if (isAuthenticated) {
            // Test connection
            when (val testResult = oneDriveClient.testConnection()) {
                is CloudResult.Success -> {
                    // Get email from user info
                    val email = testResult.data as? String ?: "Unknown"
                    
                    withContext(Dispatchers.Main) {
                        binding.tvOneDriveStatus.text = getString(R.string.connected_as, email)
                        binding.tvOneDriveStatus.setTextColor(getColor(R.color.success_green))
                    }
                    
                    Timber.d("OneDrive status: Connected as $email")
                }
                is CloudResult.Error -> {
                    // Test failed
                    withContext(Dispatchers.Main) {
                        binding.tvOneDriveStatus.text = getString(R.string.not_connected)
                        binding.tvOneDriveStatus.setTextColor(getColor(R.color.error_red))
                    }
                    
                    Timber.w("OneDrive connection test failed: ${testResult.message}")
                }
            }
        } else {
            // Not authenticated
            withContext(Dispatchers.Main) {
                binding.tvOneDriveStatus.text = getString(R.string.not_connected)
                binding.tvOneDriveStatus.setTextColor(getColor(R.color.error_red))
            }
            
            Timber.d("OneDrive status: Not authenticated")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            binding.tvOneDriveStatus.text = getString(R.string.not_connected)
            binding.tvOneDriveStatus.setTextColor(getColor(R.color.error_red))
        }
        
        Timber.e(e, "Error checking OneDrive status")
    }
}
```

**OneDriveClient Methods**:
- `isAuthenticated()`: Returns Boolean
- `testConnection()`: Returns `CloudResult<String>` with email

---

## Google Drive Authentication

### 45. cardGoogleDrive Click Behavior

**Card**: `cardGoogleDrive` (MaterialCardView)

**Logic**:
```kotlin
binding.cardGoogleDrive.setOnClickListener {
    UserActionLogger.logCardClick("GoogleDriveCard", "AddResource")
    Timber.d("Google Drive card clicked")
    
    authenticateGoogleDrive()
}
```

---

### 46. authenticateGoogleDrive() Method

**Signature**: `private fun authenticateGoogleDrive()`

**Logic**:
```kotlin
private fun authenticateGoogleDrive() {
    // Get last signed-in account
    val account = GoogleSignIn.getLastSignedInAccount(this)
    
    if (account != null) {
        // Already signed in
        Timber.d("Google account already exists: ${account.email}")
        showGoogleDriveSignedInOptions(account)
    } else {
        // Not signed in, launch sign-in flow
        Timber.d("No Google account found, launching sign-in")
        launchGoogleSignIn()
    }
}
```

---

### 47. launchGoogleSignIn() Method

**Signature**: `private fun launchGoogleSignIn()`

**Logic**:
```kotlin
private fun launchGoogleSignIn() {
    lifecycleScope.launch {
        try {
            // Get sign-in intent from Google Drive client
            val signInIntent = googleDriveClient.getSignInIntent()
            
            // Launch via ActivityResultLauncher
            googleSignInLauncher.launch(signInIntent)
            
            Timber.d("Google Sign-In intent launched")
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.google_drive_authentication_failed,
                Toast.LENGTH_LONG
            ).show()
            
            Timber.e(e, "Failed to launch Google Sign-In")
        }
    }
}
```

**GoogleDriveClient.getSignInIntent()**:
```kotlin
suspend fun getSignInIntent(): Intent {
    return withContext(Dispatchers.IO) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
        googleSignInClient.signInIntent
    }
}
```

---

### 48. handleGoogleSignInResult() Callback

**ActivityResultLauncher**:
```kotlin
private val googleSignInLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        handleGoogleSignInResult(result.data)
    } else {
        Toast.makeText(this, R.string.google_sign_in_cancelled, Toast.LENGTH_SHORT).show()
        Timber.d("Google Sign-In cancelled by user")
    }
}
```

**Method**: `private fun handleGoogleSignInResult(data: Intent?)`

**Logic**:
```kotlin
private fun handleGoogleSignInResult(data: Intent?) {
    try {
        // Get signed-in account from intent
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        
        if (account != null) {
            // Store account in property
            googleDriveAccount = account
            
            // Update status
            updateGoogleDriveStatus()
            
            // Show success toast
            val email = account.email ?: "Unknown"
            Toast.makeText(
                this,
                getString(R.string.google_drive_signed_in, email),
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("Google Sign-In successful: $email")
            
            // Navigate to folder picker
            navigateToGoogleDriveFolderPicker()
        } else {
            Toast.makeText(
                this,
                R.string.google_drive_authentication_failed,
                Toast.LENGTH_LONG
            ).show()
            
            Timber.e("Google Sign-In returned null account")
        }
    } catch (e: ApiException) {
        // Handle API exception
        Toast.makeText(
            this,
            getString(R.string.google_sign_in_error, e.statusCode),
            Toast.LENGTH_LONG
        ).show()
        
        Timber.e("Google Sign-In failed: statusCode=${e.statusCode}, message=${e.message}")
    }
}
```

**String Resources**:
- `R.string.google_drive_signed_in`: "Signed in as %s"
- `R.string.google_sign_in_error`: "Sign-in error: %d"

---

### 49. showGoogleDriveSignedInOptions() Dialog

**Signature**: `private fun showGoogleDriveSignedInOptions(account: GoogleSignInAccount)`

**Logic**:
```kotlin
private fun showGoogleDriveSignedInOptions(account: GoogleSignInAccount) {
    val email = account.email ?: "Unknown"
    
    AlertDialog.Builder(this)
        .setTitle(R.string.google_drive)
        .setMessage(getString(R.string.connected_as, email))
        .setPositiveButton(R.string.google_drive_select_folder) { dialog, _ ->
            dialog.dismiss()
            navigateToGoogleDriveFolderPicker()
        }
        .setNegativeButton(R.string.google_drive_sign_out) { dialog, _ ->
            dialog.dismiss()
            signOutGoogleDrive()
        }
        .setNeutralButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
    
    Timber.d("Showing Google Drive signed-in options for $email")
}
```

**Dialog Buttons**:
- **Positive**: "Select Folder" → Navigate to GoogleDriveFolderPickerActivity
- **Negative**: "Sign Out" → Sign out from Google account
- **Neutral**: "Cancel" → Dismiss dialog

---

### 50. signOutGoogleDrive() Method

**Signature**: `private fun signOutGoogleDrive()`

**Logic**:
```kotlin
private fun signOutGoogleDrive() {
    lifecycleScope.launch {
        try {
            // Get GoogleSignInClient
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
                .build()
            
            val googleSignInClient = GoogleSignIn.getClient(this@AddResourceActivity, signInOptions)
            
            // Sign out (clears cached account)
            googleSignInClient.signOut().addOnCompleteListener {
                // Clear local reference
                googleDriveAccount = null
                
                // Update status
                updateGoogleDriveStatus()
                
                // Show toast
                Toast.makeText(
                    this@AddResourceActivity,
                    R.string.google_drive_signed_out,
                    Toast.LENGTH_SHORT
                ).show()
                
                Timber.d("Google Drive sign-out successful")
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.sign_out_error,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.e(e, "Google Drive sign-out failed")
        }
    }
}
```

---

## Dropbox Authentication

### 51. cardDropbox Click Behavior

**Card**: `cardDropbox` (MaterialCardView)

**Logic**:
```kotlin
binding.cardDropbox.setOnClickListener {
    UserActionLogger.logCardClick("DropboxCard", "AddResource")
    Timber.d("Dropbox card clicked")
    
    authenticateDropbox()
}
```

---

### 52. authenticateDropbox() Method

**Signature**: `private fun authenticateDropbox()`

**Logic**:
```kotlin
private fun authenticateDropbox() {
    lifecycleScope.launch {
        try {
            // Test if already connected
            when (val result = dropboxClient.testConnection()) {
                is CloudResult.Success -> {
                    // Already connected
                    Timber.d("Dropbox already connected")
                    showDropboxSignedInOptions()
                }
                is CloudResult.Error -> {
                    // Not connected, start OAuth
                    Timber.d("Dropbox not connected, starting OAuth")
                    startDropboxOAuth()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.dropbox_authentication_error,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.e(e, "Dropbox authentication check failed")
        }
    }
}
```

**Helper Method**: `private fun startDropboxOAuth()`

```kotlin
private fun startDropboxOAuth() {
    // Get Dropbox app key from resources
    val appKey = getString(R.string.dropbox_app_key)
    
    // Start OAuth2 authentication via Dropbox SDK
    Auth.startOAuth2Authentication(this, appKey)
    
    // Set flag for onResume handling
    isDropboxAuthenticated = true
    
    Timber.d("Dropbox OAuth2 authentication started")
}
```

**Note**: OAuth completion handled in `onResume()` (see item 4).

---

### 53. showDropboxSignedInOptions() Dialog

**Signature**: `private fun showDropboxSignedInOptions()`

**Logic**:
```kotlin
private fun showDropboxSignedInOptions() {
    AlertDialog.Builder(this)
        .setTitle(R.string.dropbox)
        .setMessage(R.string.msg_already_authenticated)
        .setPositiveButton(R.string.dropbox_select_folder) { dialog, _ ->
            dialog.dismiss()
            navigateToDropboxFolderPicker()
        }
        .setNegativeButton(R.string.dropbox_sign_out) { dialog, _ ->
            dialog.dismiss()
            signOutDropbox()
        }
        .setNeutralButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
    
    Timber.d("Showing Dropbox signed-in options")
}
```

---

### 54. signOutDropbox() Method

**Signature**: `private fun signOutDropbox()`

**Logic**:
```kotlin
private fun signOutDropbox() {
    lifecycleScope.launch {
        try {
            // Sign out via Dropbox client
            dropboxClient.signOut()
            
            // Update status
            updateDropboxStatus()
            
            // Show toast
            Toast.makeText(
                this@AddResourceActivity,
                R.string.dropbox_signed_out,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("Dropbox sign-out successful")
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.sign_out_error,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.e(e, "Dropbox sign-out failed")
        }
    }
}
```

**DropboxClient.signOut()**:
```kotlin
suspend fun signOut() {
    withContext(Dispatchers.IO) {
        // Clear stored access token
        prefs.edit().remove(PREF_DROPBOX_ACCESS_TOKEN).apply()
        
        // Clear in-memory client
        dbxClientV2 = null
    }
}
```

---

## OneDrive Authentication

### 55. cardOneDrive Click Behavior

**Card**: `cardOneDrive` (MaterialCardView)

**Logic**:
```kotlin
binding.cardOneDrive.setOnClickListener {
    UserActionLogger.logCardClick("OneDriveCard", "AddResource")
    Timber.d("OneDrive card clicked")
    
    authenticateOneDrive()
}
```

---

### 56. authenticateOneDrive() Method

**Signature**: `private fun authenticateOneDrive()`

**Logic**:
```kotlin
private fun authenticateOneDrive() {
    lifecycleScope.launch {
        try {
            // Test if already connected
            when (val result = oneDriveClient.testConnection()) {
                is CloudResult.Success -> {
                    // Already connected
                    Timber.d("OneDrive already connected")
                    showOneDriveSignedInOptions()
                }
                is CloudResult.Error -> {
                    // Check if error is "Interactive sign-in required"
                    if (result.message.contains("Interactive sign-in required", ignoreCase = true)) {
                        Timber.d("OneDrive requires interactive sign-in")
                        startOneDriveInteractiveSignIn()
                    } else {
                        // Try silent authentication first
                        Timber.d("OneDrive not connected, attempting silent auth")
                        attemptOneDriveSilentAuth()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.onedrive_authentication_error,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.e(e, "OneDrive authentication check failed")
        }
    }
}
```

**Helper Method 1**: `private suspend fun attemptOneDriveSilentAuth()`

```kotlin
private suspend fun attemptOneDriveSilentAuth() {
    when (val authResult = oneDriveClient.authenticate()) {
        is AuthResult.Success -> {
            // Silent auth successful
            Timber.d("OneDrive silent authentication successful")
            navigateToOneDriveFolderPicker()
        }
        is AuthResult.Error -> {
            // Silent auth failed, need interactive sign-in
            if (authResult.message.contains("Interactive sign-in required", ignoreCase = true)) {
                Timber.d("Silent auth failed, starting interactive sign-in")
                startOneDriveInteractiveSignIn()
            } else {
                Toast.makeText(
                    this@AddResourceActivity,
                    getString(R.string.onedrive_auth_error, authResult.message),
                    Toast.LENGTH_LONG
                ).show()
                
                Timber.e("OneDrive authentication error: ${authResult.message}")
            }
        }
        is AuthResult.Cancelled -> {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.msg_onedrive_auth_cancelled,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("OneDrive authentication cancelled")
        }
    }
}
```

**Helper Method 2**: `private fun startOneDriveInteractiveSignIn()`

```kotlin
private fun startOneDriveInteractiveSignIn() {
    // Sign in with callback (opens system browser or account picker)
    oneDriveClient.signIn(this) { result ->
        when (result) {
            is AuthResult.Success -> {
                // Interactive sign-in successful
                val accountName = result.data as? String ?: "Unknown"
                
                Toast.makeText(
                    this,
                    getString(R.string.onedrive_signed_in, accountName),
                    Toast.LENGTH_SHORT
                ).show()
                
                Timber.d("OneDrive interactive sign-in successful: $accountName")
                
                // Set flag for onResume
                isOneDriveAuthenticated = true
                
                // Navigate to folder picker
                navigateToOneDriveFolderPicker()
            }
            is AuthResult.Error -> {
                Toast.makeText(
                    this,
                    getString(R.string.onedrive_auth_error, result.message),
                    Toast.LENGTH_LONG
                ).show()
                
                Timber.e("OneDrive interactive sign-in error: ${result.message}")
            }
            is AuthResult.Cancelled -> {
                Toast.makeText(
                    this,
                    R.string.msg_onedrive_auth_cancelled,
                    Toast.LENGTH_SHORT
                ).show()
                
                Timber.d("OneDrive interactive sign-in cancelled")
            }
        }
    }
}
```

**OneDriveClient.signIn()**:
```kotlin
fun signIn(activity: Activity, callback: (AuthResult) -> Unit) {
    msalApp?.acquireToken(
        AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(SCOPES)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    callback(AuthResult.Success(authenticationResult.account.username))
                }
                
                override fun onError(exception: MsalException) {
                    callback(AuthResult.Error(exception.message ?: "Sign-in failed"))
                }
                
                override fun onCancel() {
                    callback(AuthResult.Cancelled)
                }
            })
            .build()
    )
}
```

---

### 57. showOneDriveSignedInOptions() Dialog

**Signature**: `private fun showOneDriveSignedInOptions()`

**Logic**:
```kotlin
private fun showOneDriveSignedInOptions() {
    AlertDialog.Builder(this)
        .setTitle(R.string.onedrive)
        .setMessage(R.string.msg_already_authenticated)
        .setPositiveButton(R.string.onedrive_select_folder) { dialog, _ ->
            dialog.dismiss()
            navigateToOneDriveFolderPicker()
        }
        .setNegativeButton(R.string.onedrive_sign_out) { dialog, _ ->
            dialog.dismiss()
            signOutOneDrive()
        }
        .setNeutralButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
    
    Timber.d("Showing OneDrive signed-in options")
}
```

---

### 58. signOutOneDrive() Method

**Signature**: `private fun signOutOneDrive()`

**Logic**:
```kotlin
private fun signOutOneDrive() {
    lifecycleScope.launch {
        try {
            // Sign out via OneDrive client (clears MSAL cache)
            oneDriveClient.signOut()
            
            // Update status
            updateOneDriveStatus()
            
            // Show toast
            Toast.makeText(
                this@AddResourceActivity,
                R.string.onedrive_signed_out,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("OneDrive sign-out successful")
        } catch (e: Exception) {
            Toast.makeText(
                this@AddResourceActivity,
                R.string.sign_out_error,
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.e(e, "OneDrive sign-out failed")
        }
    }
}
```

**OneDriveClient.signOut()**:
```kotlin
suspend fun signOut() {
    withContext(Dispatchers.IO) {
        val accounts = msalApp?.accounts ?: emptyList()
        
        for (account in accounts) {
            msalApp?.removeAccount(account)
        }
    }
}
```

---

## Copy Mode Pre-Fill (AddResourceHelper)

### 59. preFillResourceData() Method

**File**: `AddResourceHelper.kt`

**Signature**:
```kotlin
fun preFillResourceData(
    resource: MediaResource,
    username: String?,
    password: String?,
    domain: String?,
    sshKey: String?,
    sshPassphrase: String?
)
```

**Purpose**: Pre-fills form fields when copying existing resource (Copy Mode).

**Routes by Type**:
```kotlin
fun preFillResourceData(
    resource: MediaResource,
    username: String?,
    password: String?,
    domain: String?,
    sshKey: String?,
    sshPassphrase: String?
) {
    when (resource.type) {
        ResourceType.LOCAL -> preFillLocalResource(resource)
        ResourceType.SMB -> preFillSmbResource(resource, username, password, domain)
        ResourceType.SFTP -> preFillSftpResource(resource, username, password, sshKey, sshPassphrase)
        ResourceType.FTP -> preFillFtpResource(resource, username, password)
        ResourceType.CLOUD -> preFillCloudResource(resource)
    }
}
```

---

### 60. LOCAL Copy Pre-Fill

**Method**: `private fun preFillLocalResource(resource: MediaResource)`

**Logic**:
```kotlin
private fun preFillLocalResource(resource: MediaResource) {
    // Show local folder section
    activity.showLocalFolderOptions()
    
    // Cannot pre-select SAF URI (security restriction)
    Toast.makeText(
        activity,
        R.string.select_folder_copy_location,
        Toast.LENGTH_LONG
    ).show()
    
    Timber.d("LOCAL copy mode: User must manually select folder (SAF restriction)")
}
```

**Note**: SAF (Storage Access Framework) URIs cannot be pre-filled programmatically for security reasons.

---

### 61. SMB Copy Pre-Fill

**Method**: `private fun preFillSmbResource(resource: MediaResource, username: String?, password: String?, domain: String?)`

**Logic**:
```kotlin
private fun preFillSmbResource(
    resource: MediaResource,
    username: String?,
    password: String?,
    domain: String?
) {
    // Show SMB section
    activity.showSmbFolderOptions()
    
    // Parse path: smb://server/shareName/subfolders
    val pathWithoutProtocol = resource.path.removePrefix("smb://")
    val parts = pathWithoutProtocol.split("/", limit = 2)
    
    if (parts.isNotEmpty()) {
        // Server IP/hostname
        val server = parts[0]
        binding.etSmbServer.setText(server)
        
        // Share name + subfolders
        if (parts.size > 1) {
            val sharePath = parts[1]
            val shareAndSubfolders = sharePath.split("/", limit = 2)
            
            val shareName = shareAndSubfolders[0]
            binding.etSmbShareName.setText(shareName)
        }
        
        // Credentials
        username?.let { binding.etSmbUsername.setText(it) }
        password?.let { binding.etSmbPassword.setText(it) }
        domain?.let { binding.etSmbDomain.setText(it) }
        
        // Port (default 445)
        binding.etSmbPort.setText("445")
        
        // Comment
        binding.etSmbComment.setText(resource.comment)
    }
    
    // Show toast
    Toast.makeText(
        activity,
        R.string.review_smb_details,
        Toast.LENGTH_LONG
    ).show()
    
    Timber.d("SMB copy mode: Pre-filled server=$server, shareName=${parts.getOrNull(1)}")
}
```

---

### 62. SFTP Copy Pre-Fill

**Method**: `private fun preFillSftpResource(resource: MediaResource, username: String?, password: String?, sshKey: String?, sshPassphrase: String?)`

**Logic**:
```kotlin
private fun preFillSftpResource(
    resource: MediaResource,
    username: String?,
    password: String?,
    sshKey: String?,
    sshPassphrase: String?
) {
    // Show SFTP section
    activity.showSftpFolderOptions()
    
    // Check SFTP radio button
    binding.rbSftp.isChecked = true
    
    // Parse path: sftp://host:port/path
    val pathWithoutProtocol = resource.path.removePrefix("sftp://")
    val hostAndPath = pathWithoutProtocol.split("/", limit = 2)
    
    if (hostAndPath.isNotEmpty()) {
        // Host:Port
        val hostPort = hostAndPath[0]
        val hostParts = hostPort.split(":")
        
        val host = hostParts[0]
        binding.etSftpHost.setText(host)
        
        val port = hostParts.getOrNull(1)?.toIntOrNull() ?: 22
        binding.etSftpPort.setText(port.toString())
        
        // Remote path
        if (hostAndPath.size > 1) {
            val remotePath = "/" + hostAndPath[1]
            binding.etSftpPath.setText(remotePath)
        } else {
            binding.etSftpPath.setText("/")
        }
        
        // Username
        username?.let { binding.etSftpUsername.setText(it) }
        
        // Auth method
        if (sshKey != null) {
            // SSH Key auth
            binding.rbSftpSshKey.isChecked = true
            binding.etSftpPrivateKey.setText(sshKey)
            sshPassphrase?.let { binding.etSftpKeyPassphrase.setText(it) }
        } else {
            // Password auth
            binding.rbSftpPassword.isChecked = true
            password?.let { binding.etSftpPassword.setText(it) }
        }
        
        // Comment
        binding.etSftpComment.setText(resource.comment)
    }
    
    // Show toast
    Toast.makeText(
        activity,
        R.string.review_sftp_details,
        Toast.LENGTH_LONG
    ).show()
    
    Timber.d("SFTP copy mode: Pre-filled host=$host, port=$port")
}
```

---

### 63. FTP Copy Pre-Fill

**Method**: `private fun preFillFtpResource(resource: MediaResource, username: String?, password: String?)`

**Logic**:
```kotlin
private fun preFillFtpResource(
    resource: MediaResource,
    username: String?,
    password: String?
) {
    // Show SFTP section (same UI for FTP)
    activity.showSftpFolderOptions()
    
    // Check FTP radio button
    binding.rbFtp.isChecked = true
    
    // Parse path: ftp://host:port/path
    val pathWithoutProtocol = resource.path.removePrefix("ftp://")
    val hostAndPath = pathWithoutProtocol.split("/", limit = 2)
    
    if (hostAndPath.isNotEmpty()) {
        // Host:Port
        val hostPort = hostAndPath[0]
        val hostParts = hostPort.split(":")
        
        val host = hostParts[0]
        binding.etSftpHost.setText(host)
        
        val port = hostParts.getOrNull(1)?.toIntOrNull() ?: 21
        binding.etSftpPort.setText(port.toString())
        
        // Remote path
        if (hostAndPath.size > 1) {
            val remotePath = "/" + hostAndPath[1]
            binding.etSftpPath.setText(remotePath)
        } else {
            binding.etSftpPath.setText("/")
        }
        
        // Credentials
        username?.let { binding.etSftpUsername.setText(it) }
        password?.let { binding.etSftpPassword.setText(it) }
        
        // Comment
        binding.etSftpComment.setText(resource.comment)
    }
    
    // Show toast
    Toast.makeText(
        activity,
        R.string.review_ftp_details,
        Toast.LENGTH_LONG
    ).show()
    
    Timber.d("FTP copy mode: Pre-filled host=$host, port=$port")
}
```

---

### 64. CLOUD Copy Pre-Fill

**Method**: `private fun preFillCloudResource(resource: MediaResource)`

**Logic**:
```kotlin
private fun preFillCloudResource(resource: MediaResource) {
    // Show cloud storage section
    activity.showCloudStorageOptions()
    
    // User must re-authenticate (cannot reuse tokens for copy)
    Toast.makeText(
        activity,
        R.string.select_cloud_folder_copy,
        Toast.LENGTH_LONG
    ).show()
    
    Timber.d("CLOUD copy mode: User must re-authenticate and select folder")
}
```

**Note**: OAuth tokens are resource-specific and cannot be reused for copying. User must authenticate again and select folder.

---

## Summary

**Items Documented**: 41-64 (24 behaviors)

**Key Features**:
- Status checking for 3 cloud providers (Google/Dropbox/OneDrive)
- OAuth flows (Google Sign-In SDK, Dropbox Core SDK, Microsoft MSAL)
- Interactive vs silent authentication (OneDrive)
- Sign-out functionality for all providers
- Copy mode pre-fill logic for all 5 resource types
- Path parsing for SMB/SFTP/FTP
- Auth method routing (password vs SSH key for SFTP)

**Navigation Methods** (referenced but not detailed here):
- `navigateToGoogleDriveFolderPicker()`
- `navigateToDropboxFolderPicker()`
- `navigateToOneDriveFolderPicker()`
