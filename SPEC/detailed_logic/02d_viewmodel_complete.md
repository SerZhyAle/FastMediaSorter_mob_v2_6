# EditResourceActivity - ViewModel Complete Methods

**Source**: Items 77-88 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `EditResourceViewModel.kt` (1089 lines)

---

## Update Methods (13 methods)

### 77. updateName() Method

**Signature**: `fun updateName(name: String)`

**Implementation**:
```kotlin
fun updateName(name: String) {
    val current = _state.value.currentResource ?: return
    
    // Update resource
    updateCurrentResource(current.copy(name = name))
    
    Timber.d("Resource name updated: $name")
}
```

---

### 78. updateComment() Method

**Signature**: `fun updateComment(comment: String)`

**Implementation**:
```kotlin
fun updateComment(comment: String) {
    val current = _state.value.currentResource ?: return
    
    // Update resource
    updateCurrentResource(current.copy(comment = comment))
    
    Timber.d("Resource comment updated: $comment")
}
```

---

### 79. updateAccessPin() Method

**Signature**: `fun updateAccessPin(pin: String)`

**Implementation**:
```kotlin
fun updateAccessPin(pin: String) {
    val current = _state.value.currentResource ?: return
    
    // Update resource
    updateCurrentResource(current.copy(accessPin = pin))
    
    Timber.d("Resource access PIN updated: ${if (pin.isNotEmpty()) "*".repeat(pin.length) else "empty"}")
}
```

**Logging**: Masks PIN in logs (shows asterisks instead of actual value).

---

### 80. updateSlideshowInterval() Method

**Signature**: `fun updateSlideshowInterval(interval: Int)`

**Implementation**:
```kotlin
fun updateSlideshowInterval(interval: Int) {
    val current = _state.value.currentResource ?: return
    
    // Clamp to valid range: 1-3600 seconds (1 sec to 1 hour)
    val clamped = interval.coerceIn(1, 3600)
    
    // Update resource
    updateCurrentResource(current.copy(slideshowInterval = clamped))
    
    Timber.d("Slideshow interval updated: $clamped seconds")
}
```

**Clamping**: Ensures interval is between 1-3600 seconds.

---

### 81. updateSupportedMediaTypes() Method

**Signature**: `fun updateSupportedMediaTypes(type: MediaType, added: Boolean)`

**Purpose**: Add or remove a single media type from the supported types set.

**Implementation**:
```kotlin
fun updateSupportedMediaTypes(type: MediaType, added: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // Add or remove type from Set
    val updatedTypes = if (added) {
        current.supportedMediaTypes + type
    } else {
        current.supportedMediaTypes - type
    }
    
    // Update resource
    updateCurrentResource(current.copy(supportedMediaTypes = updatedTypes))
    
    Timber.d("Media type ${if (added) "added" else "removed"}: $type, total=${updatedTypes.size}")
}
```

**Set Operations**:
- `set + element` - Adds element to set
- `set - element` - Removes element from set

---

### 82. updateScanSubdirectories() Method

**Signature**: `fun updateScanSubdirectories(scan: Boolean)`

**Implementation**:
```kotlin
fun updateScanSubdirectories(scan: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // Update resource
    updateCurrentResource(current.copy(scanSubdirectories = scan))
    
    Timber.d("Scan subdirectories updated: $scan")
}
```

---

### 83. updateDisableThumbnails() Method

**Signature**: `fun updateDisableThumbnails(disable: Boolean)`

**Implementation**:
```kotlin
fun updateDisableThumbnails(disable: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // Update resource
    updateCurrentResource(current.copy(disableThumbnails = disable))
    
    Timber.d("Disable thumbnails updated: $disable")
}
```

---

### 84. updateReadOnlyMode() Method

**Signature**: `fun updateReadOnlyMode(readOnly: Boolean)`

**Purpose**: Update read-only mode. Forces `isDestination = false` when enabling read-only.

**Implementation**:
```kotlin
fun updateReadOnlyMode(readOnly: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // If enabling read-only, force isDestination = false
    val updated = if (readOnly) {
        current.copy(
            isReadOnly = true,
            isDestination = false
        )
    } else {
        current.copy(isReadOnly = false)
    }
    
    // Update resource
    updateCurrentResource(updated)
    
    Timber.d("Read-only mode updated: $readOnly${if (readOnly) ", forced isDestination=false" else ""}")
}
```

**Mutual Exclusion**: Read-only resources cannot be destinations (copy/move targets).

---

### 85. updateIsDestination() Method

**Signature**: `fun updateIsDestination(isDestination: Boolean)`

**Purpose**: Update destination flag with validation.

**Implementation**:
```kotlin
fun updateIsDestination(isDestination: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // Validate if can be destination
    if (isDestination) {
        // Check if can add as destination
        if (!_state.value.canBeDestination) {
            Timber.w("Cannot set as destination: canBeDestination=false")
            return
        }
        
        // Check if read-only
        if (current.isReadOnly) {
            Timber.w("Cannot set as destination: resource is read-only")
            return
        }
    }
    
    // Update resource
    updateCurrentResource(current.copy(isDestination = isDestination))
    
    Timber.d("IsDestination updated: $isDestination")
}
```

**Validation Rules**:
1. If already destination → can always remove (toggle off)
2. If `canBeDestination == false` → cannot add (max 10 destinations reached)
3. If read-only → cannot add as destination
4. Otherwise → can add as destination

---

## Core Private Methods

### 86. updateCurrentResource() Private Method

**Signature**: `private fun updateCurrentResource(resource: MediaResource)`

**Purpose**: Updates current resource, triggers change detection, recomputes canBeDestination.

**Implementation**:
```kotlin
private fun updateCurrentResource(resource: MediaResource) {
    // Update state
    _state.update { it.copy(currentResource = resource) }
    
    // Check for changes
    checkHasChanges()
    
    // Recompute canBeDestination
    computeCanBeDestination()
    
    Timber.d("Current resource updated: ${resource.name}, hasChanges=${_state.value.hasChanges}")
}
```

**Call Chain**: All update methods → `updateCurrentResource()` → `checkHasChanges()` + `computeCanBeDestination()`

---

### 87. checkHasChanges() Private Method

**Signature**: `private fun checkHasChanges()`

**Purpose**: Compares current resource/credentials with original to detect changes.

**Implementation**:
```kotlin
private fun checkHasChanges() {
    val original = _state.value.originalResource
    val current = _state.value.currentResource
    
    // Check if both exist
    if (original == null || current == null) {
        _state.update { it.copy(
            hasChanges = false,
            hasResourceChanges = false,
            hasSmbCredentialsChanges = false,
            hasSftpCredentialsChanges = false
        ) }
        return
    }
    
    // Compare resources (all fields except ID)
    val hasResourceChanges = original != current
    
    // Compare credentials based on type
    val hasSmbCredentialsChanges = if (current.type == ResourceType.SMB) {
        val originalCreds = _state.value.originalSmbCredentials
        val currentCreds = SmbCredentials(
            username = _state.value.smbUsername,
            password = _state.value.smbPassword,
            domain = _state.value.smbDomain,
            port = _state.value.smbPort
        )
        
        originalCreds != currentCreds
    } else {
        false
    }
    
    val hasSftpCredentialsChanges = if (current.type in listOf(ResourceType.SFTP, ResourceType.FTP)) {
        val originalCreds = _state.value.originalSftpCredentials
        val currentCreds = SftpCredentials(
            username = _state.value.sftpUsername,
            password = _state.value.sftpPassword,
            privateKey = _state.value.sftpPrivateKey,
            keyPassphrase = _state.value.sftpKeyPassphrase,
            port = _state.value.sftpPort
        )
        
        originalCreds != currentCreds
    } else {
        false
    }
    
    // Update flags
    val hasChanges = hasResourceChanges || hasSmbCredentialsChanges || hasSftpCredentialsChanges
    
    _state.update { it.copy(
        hasChanges = hasChanges,
        hasResourceChanges = hasResourceChanges,
        hasSmbCredentialsChanges = hasSmbCredentialsChanges,
        hasSftpCredentialsChanges = hasSftpCredentialsChanges
    ) }
    
    Timber.d("Changes check: resource=$hasResourceChanges, smb=$hasSmbCredentialsChanges, sftp=$hasSftpCredentialsChanges, total=$hasChanges")
}
```

**Comparison Logic**:
- **Resource comparison**: Uses data class equality (checks all fields)
- **Credentials comparison**: Constructs current credentials object from state, compares with original
- **Flags**: 3 separate flags (resource/SMB/SFTP) + combined flag (hasChanges)

---

### 88. computeCanBeDestination() Private Method

**Signature**: `private fun computeCanBeDestination()`

**Purpose**: Calculates if resource can be added as destination based on rules.

**Implementation**:
```kotlin
private fun computeCanBeDestination() {
    val current = _state.value.currentResource ?: return
    
    // Rule 1: If already destination, can always toggle off (return true)
    if (current.isDestination) {
        _state.update { it.copy(canBeDestination = true) }
        Timber.d("Can be destination: true (already destination)")
        return
    }
    
    // Rule 2: If read-only, cannot be destination
    if (current.isReadOnly) {
        _state.update { it.copy(canBeDestination = false) }
        Timber.d("Can be destination: false (read-only mode)")
        return
    }
    
    // Rule 3: If max destinations reached (10), cannot add as destination
    val destinationsCount = _state.value.destinationsCount
    val maxDestinations = 10 // Hardcoded max
    
    if (destinationsCount >= maxDestinations) {
        _state.update { it.copy(canBeDestination = false) }
        Timber.d("Can be destination: false (max destinations reached: $destinationsCount/$maxDestinations)")
        return
    }
    
    // Otherwise, can be destination
    _state.update { it.copy(canBeDestination = true) }
    Timber.d("Can be destination: true (passed all checks)")
}
```

**Rules** (evaluated in order):
1. If `isDestination == true` → `canBeDestination = true` (can toggle off)
2. If `isReadOnly == true` → `canBeDestination = false` (read-only cannot be destination)
3. If `destinationsCount >= 10` → `canBeDestination = false` (max limit reached)
4. Otherwise → `canBeDestination = true` (can add as destination)

**destinationsCount Calculation** (in `loadResource()`):
```kotlin
// Count existing destinations (excluding current resource)
val destinationsCount = resourceRepository.getAllResources()
    .count { it.isDestination && it.id != resourceId }

_state.update { it.copy(destinationsCount = destinationsCount) }
```

---

## Save & Reset Methods

### 89. saveChanges() Method

**Signature**: `fun saveChanges()`

**Purpose**: Validates and saves resource + credentials to database.

**Implementation**:
```kotlin
fun saveChanges() {
    viewModelScope.launch(ioDispatcher + exceptionHandler) {
        setLoading(true)
        
        try {
            // Validate current resource exists
            val current = _state.value.currentResource
            if (current == null) {
                _events.send(EditResourceEvent.ShowError("Resource not found"))
                return@launch
            }
            
            // Validate at least one media type selected
            if (current.supportedMediaTypes.isEmpty()) {
                _events.send(EditResourceEvent.ShowError("Select at least one media type"))
                return@launch
            }
            
            Timber.d("Saving resource: ${current.name}")
            
            // Update resource in database
            updateResourceUseCase.update(current)
            
            // Save credentials if changed
            if (_state.value.hasSmbCredentialsChanges) {
                // Save SMB credentials
                val credentials = SmbCredentials(
                    username = _state.value.smbUsername,
                    password = _state.value.smbPassword,
                    domain = _state.value.smbDomain,
                    port = _state.value.smbPort
                )
                
                // Encrypt and save
                saveSmbCredentials(current.credentialsId, credentials)
                
                Timber.d("SMB credentials saved")
            }
            
            if (_state.value.hasSftpCredentialsChanges) {
                // Save SFTP/FTP credentials
                val credentials = SftpCredentials(
                    username = _state.value.sftpUsername,
                    password = _state.value.sftpPassword,
                    privateKey = _state.value.sftpPrivateKey,
                    keyPassphrase = _state.value.sftpKeyPassphrase,
                    port = _state.value.sftpPort
                )
                
                // Encrypt and save
                saveSftpCredentials(current.credentialsId, credentials)
                
                Timber.d("SFTP/FTP credentials saved")
            }
            
            // Send success event
            _events.send(EditResourceEvent.ResourceUpdated)
            
            Timber.d("Resource saved successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to save resource")
            handleError(e)
        } finally {
            setLoading(false)
        }
    }
}
```

**saveSmbCredentials() Helper**:
```kotlin
private suspend fun saveSmbCredentials(credentialsId: Long?, credentials: SmbCredentials) {
    // Encrypt credentials
    val encryptedUsername = credentialsEncryptor.encrypt(credentials.username)
    val encryptedPassword = credentialsEncryptor.encrypt(credentials.password)
    val encryptedDomain = credentials.domain?.let { credentialsEncryptor.encrypt(it) }
    
    // Create CredentialsEntity
    val credentialsEntity = CredentialsEntity(
        id = credentialsId ?: 0,
        username = encryptedUsername,
        password = encryptedPassword,
        domain = encryptedDomain,
        port = credentials.port
    )
    
    // Save to database
    if (credentialsId == null) {
        // Insert new
        credentialsRepository.insert(credentialsEntity)
    } else {
        // Update existing
        credentialsRepository.update(credentialsEntity)
    }
}
```

**saveSftpCredentials() Helper**:
```kotlin
private suspend fun saveSftpCredentials(credentialsId: Long?, credentials: SftpCredentials) {
    // Encrypt credentials
    val encryptedUsername = credentialsEncryptor.encrypt(credentials.username)
    val encryptedPassword = credentials.password?.let { credentialsEncryptor.encrypt(it) }
    val encryptedPrivateKey = credentials.privateKey?.let { credentialsEncryptor.encrypt(it) }
    val encryptedKeyPassphrase = credentials.keyPassphrase?.let { credentialsEncryptor.encrypt(it) }
    
    // Create CredentialsEntity
    val credentialsEntity = CredentialsEntity(
        id = credentialsId ?: 0,
        username = encryptedUsername,
        password = encryptedPassword,
        privateKey = encryptedPrivateKey,
        keyPassphrase = encryptedKeyPassphrase,
        port = credentials.port
    )
    
    // Save to database
    if (credentialsId == null) {
        // Insert new
        credentialsRepository.insert(credentialsEntity)
    } else {
        // Update existing
        credentialsRepository.update(credentialsEntity)
    }
}
```

**Validation**:
1. Resource must exist
2. At least one media type must be selected

**Encryption**: All credentials encrypted with Android Keystore AES-256-GCM before saving.

---

### 90. resetToOriginal() Method

**Signature**: `fun resetToOriginal()`

**Purpose**: Reverts all changes by copying original resource/credentials to current.

**Implementation**:
```kotlin
fun resetToOriginal() {
    val original = _state.value.originalResource ?: return
    
    // Copy original resource to current
    _state.update { state ->
        state.copy(
            currentResource = original.copy(), // Deep copy
            
            // Reset SMB credentials
            smbUsername = state.originalSmbCredentials?.username ?: "",
            smbPassword = state.originalSmbCredentials?.password ?: "",
            smbDomain = state.originalSmbCredentials?.domain ?: "",
            smbPort = state.originalSmbCredentials?.port ?: 445,
            
            // Reset SFTP credentials
            sftpUsername = state.originalSftpCredentials?.username ?: "",
            sftpPassword = state.originalSftpCredentials?.password ?: "",
            sftpPrivateKey = state.originalSftpCredentials?.privateKey ?: "",
            sftpKeyPassphrase = state.originalSftpCredentials?.keyPassphrase ?: "",
            sftpPort = state.originalSftpCredentials?.port ?: 22
        )
    }
    
    // Recheck changes (should all be false now)
    checkHasChanges()
    
    Timber.d("Reset to original: hasChanges=${_state.value.hasChanges}")
}
```

**After Reset**: `checkHasChanges()` will set all flags to false (current == original).

---

## SMB Credentials Update Methods

### 91. updateSmbUsername() Method

**Signature**: `fun updateSmbUsername(username: String)`

**Implementation**:
```kotlin
fun updateSmbUsername(username: String) {
    _state.update { it.copy(smbUsername = username) }
    checkHasChanges()
    
    Timber.d("SMB username updated")
}
```

---

### 92. updateSmbPassword() Method

**Signature**: `fun updateSmbPassword(password: String)`

**Implementation**:
```kotlin
fun updateSmbPassword(password: String) {
    _state.update { it.copy(smbPassword = password) }
    checkHasChanges()
    
    Timber.d("SMB password updated: ${if (password.isNotEmpty()) "***" else "empty"}")
}
```

---

### 93. updateSmbDomain() Method

**Signature**: `fun updateSmbDomain(domain: String)`

**Implementation**:
```kotlin
fun updateSmbDomain(domain: String) {
    _state.update { it.copy(smbDomain = domain) }
    checkHasChanges()
    
    Timber.d("SMB domain updated: $domain")
}
```

---

### 94. updateSmbPort() Method

**Signature**: `fun updateSmbPort(port: Int)`

**Implementation**:
```kotlin
fun updateSmbPort(port: Int) {
    _state.update { it.copy(smbPort = port) }
    checkHasChanges()
    
    Timber.d("SMB port updated: $port")
}
```

---

## SFTP Credentials Update Methods

### 95. updateSftpUsername() Method

**Signature**: `fun updateSftpUsername(username: String)`

**Implementation**:
```kotlin
fun updateSftpUsername(username: String) {
    _state.update { it.copy(sftpUsername = username) }
    checkHasChanges()
    
    Timber.d("SFTP username updated")
}
```

---

### 96. updateSftpPassword() Method

**Signature**: `fun updateSftpPassword(password: String)`

**Implementation**:
```kotlin
fun updateSftpPassword(password: String) {
    _state.update { it.copy(sftpPassword = password) }
    checkHasChanges()
    
    Timber.d("SFTP password updated: ${if (password.isNotEmpty()) "***" else "empty"}")
}
```

---

### 97. updateSftpPrivateKey() Method

**Signature**: `fun updateSftpPrivateKey(privateKey: String)`

**Implementation**:
```kotlin
fun updateSftpPrivateKey(privateKey: String) {
    _state.update { it.copy(sftpPrivateKey = privateKey) }
    checkHasChanges()
    
    Timber.d("SFTP private key updated: ${privateKey.length} chars")
}
```

---

### 98. updateSftpKeyPassphrase() Method

**Signature**: `fun updateSftpKeyPassphrase(passphrase: String)`

**Implementation**:
```kotlin
fun updateSftpKeyPassphrase(passphrase: String) {
    _state.update { it.copy(sftpKeyPassphrase = passphrase) }
    checkHasChanges()
    
    Timber.d("SFTP key passphrase updated: ${if (passphrase.isNotEmpty()) "***" else "empty"}")
}
```

---

### 99. updateSftpPort() Method

**Signature**: `fun updateSftpPort(port: Int)`

**Implementation**:
```kotlin
fun updateSftpPort(port: Int) {
    _state.update { it.copy(sftpPort = port) }
    checkHasChanges()
    
    Timber.d("SFTP port updated: $port")
}
```

---

## Summary

**Items Documented**: 77-88 (12 methods) + 9 credentials update methods (total 21)

**Core Methods**:
- **13 Update Methods** (items 77-85): updateName, updateComment, updateAccessPin, updateSlideshowInterval, updateSupportedMediaTypes, updateScanSubdirectories, updateDisableThumbnails, updateReadOnlyMode, updateIsDestination
- **3 Private Methods** (items 86-88): updateCurrentResource (calls checkHasChanges + computeCanBeDestination), checkHasChanges (compares resource + credentials, sets 4 flags), computeCanBeDestination (3 validation rules)
- **2 Action Methods** (items 89-90): saveChanges (validates, encrypts credentials, saves to DB, sends event), resetToOriginal (copies original to current, rechecks changes)

**Credentials Methods** (91-99):
- **4 SMB Methods**: updateSmbUsername, updateSmbPassword, updateSmbDomain, updateSmbPort
- **5 SFTP Methods**: updateSftpUsername, updateSftpPassword, updateSftpPrivateKey, updateSftpKeyPassphrase, updateSftpPort

**Key Patterns**:
- **State Updates**: All update methods → `updateCurrentResource()` → `checkHasChanges()` + `computeCanBeDestination()`
- **Credentials Updates**: Direct state update → `checkHasChanges()`
- **Validation**: saveChanges() validates before saving (resource exists, at least one media type)
- **Encryption**: All credentials encrypted with Android Keystore before saving
- **Change Detection**: Compares current vs original for both resource and credentials
- **Flags**: 4 separate flags (hasChanges, hasResourceChanges, hasSmbCredentialsChanges, hasSftpCredentialsChanges)
- **Mutual Exclusion**: Read-only mode forces isDestination=false
- **Destination Validation**: 3-rule check (already destination, read-only, max limit)
- **Logging**: Masks passwords/PINs in logs (shows asterisks)

**Total ViewModel Methods**: 21 documented (13 core + 9 credentials)
