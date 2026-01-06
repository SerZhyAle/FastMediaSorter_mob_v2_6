# 31. Security Requirements

**Last Updated**: January 6, 2026  
**Purpose**: Comprehensive security requirements for FastMediaSorter v2.

This document defines encryption strategies, network security, ProGuard rules, permission handling, and secure coding practices.

---

## Overview

FastMediaSorter handles sensitive data requiring protection:
- **Network Credentials**: SMB/SFTP/FTP/Cloud passwords, OAuth tokens
- **User Files**: Personal photos, videos, documents (no access without permission)
- **Network Traffic**: SMB/SFTP/FTP/HTTPS communications
- **Local Storage**: Database, cache, temporary files

### Security Principles

1. **Defense in Depth**: Multiple layers of security
2. **Least Privilege**: Minimum permissions required
3. **Data Encryption**: Credentials encrypted at rest, traffic encrypted in transit
4. **Secure Defaults**: Encrypted protocols preferred (SFTP > FTP, HTTPS > HTTP)
5. **No Hardcoded Secrets**: API keys, signing keys never in source code

---

## Table of Contents

1. [Credential Encryption](#1-credential-encryption)
2. [Network Security](#2-network-security)
3. [ProGuard Rules](#3-proguard-rules)
4. [Permission Handling](#4-permission-handling)
5. [Secure Storage](#5-secure-storage)
6. [OAuth & Token Management](#6-oauth--token-management)
7. [Code Obfuscation](#7-code-obfuscation)
8. [Vulnerability Mitigation](#8-vulnerability-mitigation)

---

## 1. Credential Encryption

### Android Keystore (AES-256-GCM)

**Goal**: Store network passwords securely using hardware-backed encryption.

**Implementation**:
```kotlin
object CredentialEncryption {
    
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "fastmediasorter_credentials"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // GCM recommended IV size
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
    
    /**
     * Generate or retrieve AES key from Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // No biometric for background operations
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
    
    /**
     * Encrypt password using AES-256-GCM
     * @return Base64-encoded IV + ciphertext
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Prepend IV to ciphertext (IV is public, no need to encrypt)
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypt password
     * @param encrypted Base64-encoded IV + ciphertext
     */
    fun decrypt(encrypted: String): String {
        val combined = Base64.decode(encrypted, Base64.NO_WRAP)
        
        // Extract IV and ciphertext
        val iv = combined.sliceArray(0 until IV_SIZE)
        val ciphertext = combined.sliceArray(IV_SIZE until combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}
```

**Usage in Repository**:
```kotlin
class NetworkCredentialsRepositoryImpl(
    private val credentialsDao: NetworkCredentialsDao
) : NetworkCredentialsRepository {
    
    override suspend fun saveCredentials(credentials: NetworkCredentials) {
        val encryptedPassword = CredentialEncryption.encrypt(credentials.password)
        val entity = NetworkCredentialsEntity(
            resourceId = credentials.resourceId,
            username = credentials.username,
            password = encryptedPassword, // Encrypted
            domain = credentials.domain
        )
        credentialsDao.insert(entity)
    }
    
    override suspend fun getCredentials(resourceId: Long): NetworkCredentials? {
        val entity = credentialsDao.getByResourceId(resourceId) ?: return null
        val decryptedPassword = CredentialEncryption.decrypt(entity.password)
        return NetworkCredentials(
            resourceId = entity.resourceId,
            username = entity.username,
            password = decryptedPassword, // Decrypted
            domain = entity.domain
        )
    }
}
```

**Security Properties**:
- **Hardware-backed**: Keys stored in Secure Element (TEE) on supported devices
- **Non-exportable**: Keys cannot be extracted from Keystore
- **AES-256-GCM**: Authenticated encryption (prevents tampering)
- **Per-device**: Keys unique to each device (backup won't work on other devices)

---

### Fallback for Android < 6.0 (API < 23)

**Issue**: Android Keystore AES support added in API 23.

**Solution**: Use EncryptedSharedPreferences (Jetpack Security library).

```kotlin
dependencies {
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
}

object LegacyCredentialEncryption {
    
    fun getEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            "encrypted_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun savePassword(context: Context, resourceId: Long, password: String) {
        getEncryptedPreferences(context).edit {
            putString("password_$resourceId", password)
        }
    }
    
    fun getPassword(context: Context, resourceId: Long): String? {
        return getEncryptedPreferences(context).getString("password_$resourceId", null)
    }
}
```

---

## 2. Network Security

### Network Security Config

**File**: `res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Production: Force HTTPS for all cloud APIs -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">googleapis.com</domain>
        <domain includeSubdomains="true">graph.microsoft.com</domain>
        <domain includeSubdomains="true">api.dropboxapi.com</domain>
    </domain-config>
    
    <!-- Development: Allow cleartext for local testing -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain> <!-- Android emulator host -->
        <domain includeSubdomains="true">192.168.0.0</domain> <!-- Local network -->
    </domain-config>
    
    <!-- Certificate pinning for critical APIs (optional) -->
    <domain-config>
        <domain includeSubdomains="true">googleapis.com</domain>
        <pin-set expiration="2027-01-01">
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <!-- Get actual hash from: openssl s_client -connect googleapis.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64 -->
        </pin-set>
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml**:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

### SSL/TLS for Network Protocols

#### SFTP (SSH)
```kotlin
class SftpClient {
    fun connect(host: String, port: Int, username: String, password: String) {
        val config = DefaultConfig()
        val sshClient = SSHClient(config).apply {
            // Load known host keys (optional, prevents MITM)
            loadKnownHosts()
            
            // Strict host key checking (production)
            addHostKeyVerifier { _, _, _ -> true } // FIXME: Verify fingerprint
            
            connect(host, port)
            authPassword(username, password)
        }
    }
}
```

**Known Hosts Verification** (advanced):
```kotlin
val knownHostsFile = File(context.filesDir, "known_hosts")
if (knownHostsFile.exists()) {
    sshClient.loadKnownHosts(knownHostsFile)
} else {
    // First connection: Save fingerprint
    sshClient.addHostKeyVerifier(PromptHostKeyVerifier { hostname, pubKey ->
        // Show dialog: "Accept fingerprint XYZ for host?"
        // If yes: Save to known_hosts
    })
}
```

---

#### SMB (SMBJ)
```kotlin
class SmbClient {
    fun connect(server: String, shareName: String, username: String, password: String) {
        val config = SmbConfig.builder()
            .withEncryptData(true) // SMB 3.x encryption
            .withSigningRequired(true) // Prevent tampering
            .withTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val connection = SMBClient(config).connect(server)
        val session = connection.authenticate(AuthenticationContext(username, password.toCharArray(), ""))
        val share = session.connectShare(shareName)
    }
}
```

---

## 3. ProGuard Rules

### Production ProGuard Configuration

**File**: `app_v2/proguard-rules.pro`

```proguard
# ========== General ==========
-dontoptimize
-dontpreverify
-keepattributes *Annotation*,Signature,Exception,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable # For crash reports

# ========== Kotlin ==========
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.coroutines.** { *; }

# ========== Hilt ==========
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Inject class * { *; }

# ========== Room Database ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ========== Gson (for JSON serialization) ==========
-keep class com.apemax.fastmediasorter.domain.model.** { *; }
-keep class com.apemax.fastmediasorter.data.local.db.entity.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ========== SMBJ (SMB Client) ==========
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.mssmb2.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# ========== SSHJ (SFTP Client) ==========
-keep class net.schmizz.sshj.** { *; }
-dontwarn net.i2p.crypto.eddsa.**
-keep class net.i2p.crypto.eddsa.** { *; }

# ========== Apache Commons Net (FTP) ==========
-keep class org.apache.commons.net.** { *; }

# ========== Google Drive REST Client ==========
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.client.**

# ========== Glide ==========
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ========== ExoPlayer ==========
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ========== Crash Reporting (Remove in debug) ==========
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

---

### R8 Full Mode (Aggressive Optimization)

**gradle.properties**:
```properties
android.enableR8.fullMode=true
```

**Risks**:
- More aggressive obfuscation
- Can break reflection-based code

**Mitigation**: Test release build thoroughly before publishing.

---

## 4. Permission Handling

### Runtime Permissions (Android 6.0+)

**Required Permissions**:
```xml
<manifest>
    <!-- Storage (required for local file access) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                     android:maxSdkVersion="29" />
    
    <!-- Android 13+ Granular Media Permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    
    <!-- Android 11+ Manage External Storage - REMOVED for Play Store Compliance -->
    <!-- Usage of Storage Access Framework (SAF) is enforced instead -->
    
    <!-- Network (implicit, no declaration needed) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
</manifest>
```

---

### Permission Request Pattern

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            onStoragePermissionGranted()
        } else {
            showPermissionRationale()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!hasStoragePermission()) {
            requestStoragePermission()
        }
    }
    
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            Environment.isExternalStorageManager() ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6-10
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            storagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ))
        } else {
            // Android 6-12
            storagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }
    
    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_rationale_storage)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}
```

---

### Permission Rationale (User Trust)

**strings.xml**:
```xml
<string name="permission_rationale_storage">
    FastMediaSorter needs storage access to:
    • Browse your photos and videos
    • Copy/move files between folders
    • Edit images and videos
    
    Your files stay on your device and are never uploaded without your permission.
</string>
```

---

## 5. Secure Storage

### Database Encryption (SQLCipher)

**Optional**: Encrypt entire Room database.

**Dependencies**:
```kotlin
dependencies {
    implementation 'net.zetetic:android-database-sqlcipher:4.5.4'
}
```

**Implementation**:
```kotlin
val passphrase = SQLiteDatabase.getBytes("your-secret-passphrase".toCharArray())
val factory = SupportFactory(passphrase)

val database = Room.databaseBuilder(context, AppDatabase::class.java, "app_database.db")
    .openHelperFactory(factory)
    .build()
```

**Trade-offs**:
- ✅ Full database encryption
- ❌ Performance overhead (~5-10%)
- ❌ Passphrase storage complexity

**Recommendation**: Use only if storing highly sensitive data (e.g., health records). For FastMediaSorter, encrypting credentials only (Android Keystore) is sufficient.

---

### Cache Encryption (Glide Disk Cache)

**Issue**: Thumbnails cached in plaintext.

**Mitigation**:
- Cache stored in app-private directory (`/data/data/com.apemax.fastmediasorter/cache/`)
- Not accessible to other apps
- Cleared on uninstall

**Optional**: Encrypt cache (complex, performance cost).

---

## 6. OAuth & Token Management

### Google Drive OAuth Flow

**Security Considerations**:
1. **Client ID**: Store in `local.properties` (not in git)
2. **Token Storage**: Encrypted SharedPreferences
3. **Token Refresh**: Automatic background refresh before expiration

**Implementation**:
```kotlin
class GoogleDriveAuthManager(
    private val context: Context
) {
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "oauth_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(accessToken: String, refreshToken: String, expiresAt: Long) {
        encryptedPrefs.edit {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putLong("expires_at", expiresAt)
        }
    }
    
    fun getAccessToken(): String? {
        val token = encryptedPrefs.getString("access_token", null) ?: return null
        val expiresAt = encryptedPrefs.getLong("expires_at", 0)
        
        // Check if token expired
        if (System.currentTimeMillis() > expiresAt - 60_000) { // Refresh 1 min before expiry
            refreshToken()
        }
        
        return encryptedPrefs.getString("access_token", null)
    }
    
    private fun refreshToken() {
        val refreshToken = encryptedPrefs.getString("refresh_token", null) ?: return
        
        // Call Google token endpoint
        // POST https://oauth2.googleapis.com/token
        // grant_type=refresh_token&refresh_token=...&client_id=...
        
        // Save new access token
    }
}
```

---

### Token Revocation (Sign Out)

```kotlin
suspend fun signOut(provider: CloudProvider) {
    when (provider) {
        CloudProvider.GOOGLE_DRIVE -> {
            val token = authManager.getAccessToken()
            // Revoke token
            httpClient.post("https://oauth2.googleapis.com/revoke") {
                parameter("token", token)
            }
        }
        // ... other providers
    }
    
    // Clear local tokens
    authManager.clearTokens()
}
```

---

## 7. Code Obfuscation

### R8 Optimization Flags

**build.gradle.kts**:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**Effects**:
- **Minification**: Shorter class/method names (reduces APK size)
- **Shrinking**: Removes unused code
- **Optimization**: Inlines methods, removes dead code

---

### Obfuscation Dictionary (Advanced)

**proguard-rules.pro**:
```proguard
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt
```

**dictionary.txt** (custom names):
```
a
b
c
aa
ab
ac
...
```

**Benefit**: Makes reverse engineering harder (non-default names).

---

## 8. Vulnerability Mitigation

### SQL Injection Prevention

**Room (Safe by Default)**:
```kotlin
@Query("SELECT * FROM resources WHERE name = :name") // Safe (parameterized)
fun getByName(name: String): List<ResourceEntity>
```

**Raw SQL (Dangerous)**:
```kotlin
// ❌ VULNERABLE
val query = "SELECT * FROM resources WHERE name = '$name'" // SQL injection!
database.query(query)

// ✅ SAFE
val query = "SELECT * FROM resources WHERE name = ?"
database.query(query, arrayOf(name))
```

**Recommendation**: Always use Room `@Query` with parameters.

---

### Path Traversal Prevention

**Issue**: User-provided paths can escape sandbox.

```kotlin
// ❌ VULNERABLE
fun readFile(filename: String) {
    val file = File("/data/data/com.apemax.fastmediasorter/files/$filename")
    file.readText() // User inputs "../../../../etc/passwd"
}

// ✅ SAFE
fun readFile(filename: String) {
    val sanitized = filename.replace("..", "") // Remove path traversal
    val file = File(context.filesDir, sanitized)
    
    // Verify file is within allowed directory
    if (!file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
        throw SecurityException("Path traversal detected")
    }
    
    file.readText()
}
```

---

### Dependency Vulnerabilities (Dependabot)

**GitHub Dependabot Configuration**:

**File**: `.github/dependabot.yml`
```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    reviewers:
      - "your-username"
    labels:
      - "dependencies"
      - "security"
```

**Benefits**:
- Automatic PRs for dependency updates
- Security vulnerability alerts

---

### Regular Security Audits

**Manual Checks**:
1. **Hardcoded Secrets**: `grep -r "password\|api_key\|secret" app_v2/src/`
2. **Logging Sensitive Data**: `grep -r "Timber.d.*password\|Log.d.*token" app_v2/src/`
3. **Cleartext Traffic**: `grep -r "http://" app_v2/src/` (should be https://)
4. **Weak Crypto**: `grep -r "MD5\|SHA1\|DES\|RC4" app_v2/src/`

**Automated Tools**:
- **Android Lint**: Built-in security checks
- **OWASP Dependency-Check**: Scan for vulnerable libraries

---

## Best Practices Summary

### ✅ DO

1. **Encrypt Credentials**
```kotlin
val encrypted = CredentialEncryption.encrypt(password)
```

2. **Use HTTPS for All Cloud APIs**
```xml
<domain-config cleartextTrafficPermitted="false">
```

3. **Enable ProGuard in Release**
```kotlin
isMinifyEnabled = true
```

4. **Request Permissions with Rationale**
```kotlin
showPermissionRationale("We need storage access to browse your photos")
```

5. **Validate User Inputs**
```kotlin
val sanitized = input.replace("..", "")
```

---

### ❌ DON'T

1. **Don't Store Passwords in Plaintext**
```kotlin
// ❌ BAD
database.insert(CredentialsEntity(password = "plaintext123"))
```

2. **Don't Log Sensitive Data**
```kotlin
// ❌ BAD
Timber.d("Login: username=$username, password=$password")
```

3. **Don't Use HTTP for Cloud APIs**
```kotlin
// ❌ BAD
val url = "http://api.example.com/user" // Use https://
```

4. **Don't Hardcode API Keys**
```kotlin
// ❌ BAD
const val API_KEY = "sk_live_1234567890"
```

5. **Don't Ignore Security Warnings**
```kotlin
// ❌ BAD
@SuppressLint("TrustAllCertificates") // NEVER IGNORE
```

---

## Security Checklist (Pre-Release)

- [ ] **Credentials encrypted** with Android Keystore
- [ ] **Network Security Config** enabled (no cleartext for production APIs)
- [ ] **ProGuard rules** applied, release build tested
- [ ] **OAuth tokens** stored in EncryptedSharedPreferences
- [ ] **Permissions** requested with clear rationale
- [ ] **No hardcoded secrets** (checked with grep)
- [ ] **No sensitive logging** in release build
- [ ] **Dependencies up-to-date** (Dependabot enabled)
- [ ] **SQL injection** prevented (Room parameterized queries)
- [ ] **Path traversal** prevented (input validation)

---

## Reference Files

### Source Code
- **Encryption**: `data/security/CredentialEncryption.kt`
- **Network Config**: `res/xml/network_security_config.xml`
- **ProGuard**: `app_v2/proguard-rules.pro`
- **Permissions**: `ui/main/PermissionManager.kt`

### Related Documents
- [26. Database Schema & Migrations](26_database_schema.md) - Encryption considerations
- [29. Error Handling Strategy](29_error_handling.md) - Security exception handling
- [24. Dependencies Reference](24_dependencies.md) - Security libraries (Jetpack Security, BouncyCastle)

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
