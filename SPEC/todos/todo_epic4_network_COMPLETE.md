# Epic 4: Network Protocol Implementation - Technical Specification
*Derived from: [Tactical Plan: Epic 4](../00_strategy_epic4_network.md)*  
*Reference: [Protocol Implementations](../20_protocol_implementations.md), [Network Architecture](../detailed_logic/08a_network_protocols_logic.md), [Common Pitfalls](../21_common_pitfalls.md)*

**Purpose**: Design and implement SMB, SFTP, FTP protocols from scratch with proper connection pooling, credential security, and unified caching.

**Development Approach**: Clean implementation WITHOUT copying V1 code. V1 имеет критические проблемы (connection leaks, credential exposure, quadratic file operations).

**Estimated Time**: 8-10 days  
**Prerequisites**: Epic 1-3 completed (database, browsing, player)  
**Output**: Production-ready network file access with encrypted credentials and connection pooling

---

## 1. Network Credentials Management

### 1.1 NetworkCredentialsRepository Specification

**ANTI-PATTERN (V1)**: Пароли хранятся в Room Database (SQLite) - легко извлекаются с rooted device

**CORRECT APPROACH**: EncryptedSharedPreferences (AES256_GCM) для паролей, Room только для metadata

- [ ] Create `data/repository/NetworkCredentialsRepositoryImpl.kt`

**Required Dependencies**:
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

**Repository Interface Contract**:
```kotlin
interface NetworkCredentialsRepository {
    suspend fun saveCredentials(
        resourceId: Long,
        username: String,
        password: String,
        domain: String? = null
    )
    suspend fun getCredentials(resourceId: Long): NetworkCredentialsEntity?
    suspend fun getPassword(resourceId: Long): String?
    suspend fun deleteCredentials(resourceId: Long)
    suspend fun updateLastUsed(resourceId: Long)
}
```

**Implementation Requirements**:

1. **EncryptedSharedPreferences Setup**:
   ```kotlin
   val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
   val sharedPrefs = EncryptedSharedPreferences.create(
       "network_credentials_encrypted",
       masterKeyAlias,
       context,
       PrefKeyEncryptionScheme.AES256_SIV,
       PrefValueEncryptionScheme.AES256_GCM
   )
   ```

2. **Storage Strategy**:
   - **Room Database**: Store username, domain, lastUpdated (NOT password)
   - **EncryptedSharedPreferences**: Store password with key `"password_$resourceId"`
   - Rationale: Room for queries, EncryptedPrefs for sensitive data

3. **`saveCredentials()` Logic**:
   - Insert `NetworkCredentialsEntity(resourceId, username, domain, timestamp)` to Room
   - Store password to EncryptedSharedPreferences via `editor.putString("password_$resourceId", password)`
   - Use `Dispatchers.IO` for all operations

4. **`getPassword()` Logic**:
   - Retrieve from EncryptedSharedPreferences: `prefs.getString("password_$resourceId", null)`
   - Return `null` if not found (user must re-enter credentials)

5. **`deleteCredentials()` Logic**:
   - Delete from Room via `credentialsDao.deleteByResourceId(resourceId)`
   - Remove from EncryptedSharedPreferences via `editor.remove("password_$resourceId")`

6. **`updateLastUsed()` Logic**:
   - Update Room entity's `lastUpdated` timestamp
   - Used for credential expiration checks (optional)

**Error Handling**:
- Catch `GeneralSecurityException` при инициализации EncryptedSharedPreferences
- Fallback: Show error "Device security not supported" (rare on modern devices)
- Log errors with `Timber.e(exception, "Context")`

**Security Notes**:
- MasterKey хранится в Android Keystore (hardware-backed on modern devices)
- Пароли НЕ доступны через adb shell или file managers
- Root access может скомпрометировать, но это acceptable risk для enterprise apps

### 1.2 Credential Input Dialog
- [ ] Create `res/layout/dialog_network_credentials.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="@dimen/spacing_large">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/network_credentials"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginBottom="@dimen/spacing_medium"/>
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/server_address"
        android:layout_marginBottom="@dimen/spacing_normal">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etServerAddress"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textUri"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/username"
        android:layout_marginBottom="@dimen/spacing_normal">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etUsername"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPersonName"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/password"
        app:endIconMode="password_toggle"
        android:layout_marginBottom="@dimen/spacing_normal"
        xmlns:app="http://schemas.android.com/apk/res-auto">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etPassword"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPassword"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilDomain"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/domain_optional"
        android:visibility="gone"
        android:layout_marginBottom="@dimen/spacing_normal">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etDomain"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilPort"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/port"
        android:layout_marginBottom="@dimen/spacing_medium">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etPort"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="number"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end">
        
        <Button
            android:id="@+id/btnTestConnection"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/test_connection"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_marginEnd="@dimen/spacing_small"/>
        
        <Button
            android:id="@+id/btnCancel"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@android:string/cancel"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_marginEnd="@dimen/spacing_small"/>
        
        <Button
            android:id="@+id/btnSave"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/save"/>
    </LinearLayout>
    
</LinearLayout>
```

---

## 2. SMB Protocol (SMBJ)

### 2.1 SmbClient Interface Specification

**ANTI-PATTERN (V1)**: SMBJ 0.11.x + BouncyCastle 1.70 → native crash (libpenguin.so)

**CORRECT APPROACH**: SMBJ 0.12.1 + BouncyCastle 1.78.1 (проверено, стабильно)

- [ ] Create `data/network/smb/SmbClient.kt`

**Required Dependencies**:
```kotlin
implementation("com.hierynomus:smbj:0.12.1")
implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")  // CRITICAL version
```

**Result Sealed Class**:
```kotlin
sealed class SmbResult<out T> {
    data class Success<T>(val data: T) : SmbResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : SmbResult<Nothing>()
}
```

**SmbClient Interface Contract**:
```kotlin
class SmbClient @Inject constructor() {
    suspend fun connect(
        server: String,
        shareName: String,
        username: String,
        password: String,
        domain: String? = null,
        port: Int = 445
    ): SmbResult<DiskShare>
    
    suspend fun listFiles(share: DiskShare, remotePath: String): SmbResult<List<SmbFileInfo>>
    suspend fun downloadFile(share: DiskShare, remotePath: String): SmbResult<InputStream>
    suspend fun uploadFile(share: DiskShare, remotePath: String, inputStream: InputStream, fileSize: Long): SmbResult<Unit>
    suspend fun deleteFile(share: DiskShare, remotePath: String): SmbResult<Unit>
    suspend fun createDirectory(share: DiskShare, remotePath: String): SmbResult<Unit>
    suspend fun testConnection(...): SmbResult<Boolean>  // For credentials dialog
}

data class SmbFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)
```

**Implementation Requirements**:

1. **`connect()` Logic**:
   - Create `SMBClient` с config: timeout 30s, soTimeout 30s
   - Call `client.connect(server, port)`
   - Create `AuthenticationContext(username, password.toCharArray(), domain)`
   - Authenticate: `connection.authenticate(authContext)`
   - Connect to share: `session.connectShare(shareName) as DiskShare`
   - Return `SmbResult.Success(share)` или `SmbResult.Error` при exception

2. **`listFiles()` Logic**:
   - Call `share.list(remotePath)`
   - Map to `SmbFileInfo` objects
   - Extract: `fileName`, `standardInformation.endOfFile` (size), `isDirectory`, `lastWriteTime`
   - Filter out `.` и `..` entries
   - Return `SmbResult.Success(files)`

3. **`downloadFile()` Logic**:
   - Open file: `share.openFile(remotePath, AccessMask.GENERIC_READ, ...)`
   - Get `inputStream` from file handle
   - Return `SmbResult.Success(inputStream)`
   - ВАЖНО: Caller должен закрыть stream после использования

4. **`uploadFile()` Logic**:
   - Open file: `share.openFile(remotePath, AccessMask.GENERIC_WRITE, ..., FILE_OVERWRITE_IF, ...)`
   - Get `outputStream` from file handle
   - Copy: `inputStream.copyTo(outputStream)`
   - Close file handle
   - Return `SmbResult.Success(Unit)`

5. **`deleteFile()` Logic**:
   - Call `share.rm(remotePath)`
   - Return `SmbResult.Success(Unit)`

6. **`createDirectory()` Logic**:
   - Call `share.mkdir(remotePath)`
   - Return `SmbResult.Success(Unit)`

7. **`testConnection()` Logic**:
   - Call `connect()`, если Success → close share и return `SmbResult.Success(true)`
   - Used in credentials dialog "Test Connection" button

**Error Handling**:
- Catch `IOException`: network errors
- Catch `SMBApiException`: SMB protocol errors (access denied, file not found)
- Wrap all exceptions in `SmbResult.Error` с message
- Log с `Timber.e(exception, "SMB operation failed")`

**Thread Safety**:
- All methods используют `withContext(Dispatchers.IO)`
- DiskShare НЕ thread-safe → use connection pooling (next section)

**Known Issues**:
- SMBJ 0.11.x crashes при определенных серверных конфигурациях → USE 0.12.1
- BouncyCastle <1.78 имеет security vulnerabilities → USE 1.78.1
- Timeout issues → set soTimeout=30s в config
package com.sza.fastmediasorter.data.network.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class SmbResult<out T> {
    data class Success<T>(val data: T) : SmbResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : SmbResult<Nothing>()
}

@Singleton
class SmbClient @Inject constructor() {
    
    private val config = SmbConfig.builder()
        .withTimeout(30, TimeUnit.SECONDS)
        .withSoTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val client = SMBClient(config)
    
    /**
     * Connect to SMB server and return DiskShare
     */
    suspend fun connect(
        server: String,
        shareName: String,
        username: String,
        password: String,
        domain: String? = null,
        port: Int = 445
    ): SmbResult<DiskShare> = withContext(Dispatchers.IO) {
        try {
            val connection: Connection = client.connect(server, port)
            val authContext = AuthenticationContext(
                username,
                password.toCharArray(),
                domain
            )
            val session: Session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            Timber.d("SMB connected: $server/$shareName")
            SmbResult.Success(share)
            
        } catch (e: Exception) {
            Timber.e(e, "SMB connection failed")
            SmbResult.Error("Failed to connect to SMB: ${e.message}", e)
        }
    }
    
    /**
     * List files in directory
     */
    suspend fun listFiles(share: DiskShare, remotePath: String): SmbResult<List<SmbFileInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val files = mutableListOf<SmbFileInfo>()
                
                share.list(remotePath).forEach { fileInfo ->
                    files.add(
                        SmbFileInfo(
                            name = fileInfo.fileName,
                            path = "$remotePath/${fileInfo.fileName}",
                            size = fileInfo.standardInformation.endOfFile,
                            isDirectory = fileInfo.standardInformation.isDirectory,
                            lastModified = fileInfo.standardInformation.lastWriteTime.toEpochMillis()
                        )
                    )
                }
                
                Timber.d("Listed ${files.size} files from $remotePath")
                SmbResult.Success(files)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to list files")
                SmbResult.Error("Failed to list files: ${e.message}", e)
            }
        }
    
    /**
     * Download file to InputStream
     */
    suspend fun downloadFile(share: DiskShare, remotePath: String): SmbResult<InputStream> =
        withContext(Dispatchers.IO) {
            try {
                val file = share.openFile(
                    remotePath,
                    setOf(AccessMask.GENERIC_READ),
                    setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    setOf(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    emptySet()
                )
                
                val inputStream = file.inputStream
                Timber.d("Opened SMB file: $remotePath")
                SmbResult.Success(inputStream)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to download file")
                SmbResult.Error("Failed to download: ${e.message}", e)
            }
        }
    
    /**
     * Upload file from InputStream
     */
    suspend fun uploadFile(
        share: DiskShare,
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long
    ): SmbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = share.openFile(
                remotePath,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                emptySet(),
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                emptySet()
            )
            
            file.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.close()
            
            Timber.d("Uploaded file to $remotePath")
            SmbResult.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            SmbResult.Error("Failed to upload: ${e.message}", e)
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(share: DiskShare, remotePath: String): SmbResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                share.rm(remotePath)
                Timber.d("Deleted file: $remotePath")
                SmbResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete file")
                SmbResult.Error("Failed to delete: ${e.message}", e)
            }
        }
    
    /**
     * Create directory
     */
    suspend fun createDirectory(share: DiskShare, remotePath: String): SmbResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                share.mkdir(remotePath)
                Timber.d("Created directory: $remotePath")
                SmbResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create directory")
                SmbResult.Error("Failed to create directory: ${e.message}", e)
            }
        }
    
    /**
     * Test connection (for credentials dialog)
     */
    suspend fun testConnection(
        server: String,
        shareName: String,
        username: String,
        password: String,
        domain: String? = null,
        port: Int = 445
    ): SmbResult<Boolean> = withContext(Dispatchers.IO) {
        when (val result = connect(server, shareName, username, password, domain, port)) {
            is SmbResult.Success -> {
                result.data.close()
                SmbResult.Success(true)
            }
            is SmbResult.Error -> result
        }
    }
}

data class SmbFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)
```

### 2.2 SMB Connection Pool
- [ ] Create `data/network/smb/SmbConnectionPool.kt`:
```kotlin
package com.sza.fastmediasorter.data.network.smb

import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbConnectionPool @Inject constructor(
    private val smbClient: SmbClient
) {
    
    private val connections = ConcurrentHashMap<String, PooledConnection>()
    private val mutex = Mutex()
    
    data class PooledConnection(
        val share: DiskShare,
        var lastUsed: Long = System.currentTimeMillis(),
        var refCount: Int = 0
    )
    
    /**
     * Get or create connection
     */
    suspend fun getConnection(
        resourceId: Long,
        server: String,
        shareName: String,
        username: String,
        password: String,
        domain: String? = null,
        port: Int = 445
    ): SmbResult<DiskShare> = mutex.withLock {
        val key = "$server:$port/$shareName"
        
        val pooled = connections[key]
        if (pooled != null && pooled.share.isConnected) {
            pooled.refCount++
            pooled.lastUsed = System.currentTimeMillis()
            Timber.d("Reusing SMB connection: $key (refCount: ${pooled.refCount})")
            return@withLock SmbResult.Success(pooled.share)
        }
        
        // Create new connection
        when (val result = smbClient.connect(server, shareName, username, password, domain, port)) {
            is SmbResult.Success -> {
                connections[key] = PooledConnection(result.data, refCount = 1)
                Timber.d("Created new SMB connection: $key")
                result
            }
            is SmbResult.Error -> result
        }
    }
    
    /**
     * Release connection (decrease refCount)
     */
    suspend fun releaseConnection(server: String, shareName: String, port: Int = 445) = mutex.withLock {
        val key = "$server:$port/$shareName"
        connections[key]?.let { pooled ->
            pooled.refCount--
            Timber.d("Released SMB connection: $key (refCount: ${pooled.refCount})")
            
            if (pooled.refCount <= 0) {
                // Close if idle for 45 seconds
                if (System.currentTimeMillis() - pooled.lastUsed > 45_000) {
                    pooled.share.close()
                    connections.remove(key)
                    Timber.d("Closed idle SMB connection: $key")
                }
            }
        }
    }
    
    /**
     * Close all connections
     */
    suspend fun closeAll() = mutex.withLock {
        connections.forEach { (key, pooled) ->
            pooled.share.close()
            Timber.d("Closed SMB connection: $key")
        }
        connections.clear()
    }
}
```

---

## 3. SFTP Protocol (SSHJ)

### 3.1 SftpClient Implementation
- [ ] Create `data/network/sftp/SftpClient.kt`:
```kotlin
package com.sza.fastmediasorter.data.network.sftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class SftpResult<out T> {
    data class Success<T>(val data: T) : SftpResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : SftpResult<Nothing>()
}

@Singleton
class SftpClient @Inject constructor() {
    
    /**
     * Connect to SFTP server
     */
    suspend fun connect(
        server: String,
        username: String,
        password: String,
        port: Int = 22
    ): SftpResult<SFTPClient> = withContext(Dispatchers.IO) {
        try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier()) // TODO: Use proper verification
            ssh.connect(server, port)
            ssh.authPassword(username, password)
            
            val sftp = ssh.newSFTPClient()
            Timber.d("SFTP connected: $server:$port")
            SftpResult.Success(sftp)
            
        } catch (e: Exception) {
            Timber.e(e, "SFTP connection failed")
            SftpResult.Error("Failed to connect to SFTP: ${e.message}", e)
        }
    }
    
    /**
     * List files in directory
     */
    suspend fun listFiles(sftp: SFTPClient, remotePath: String): SftpResult<List<SftpFileInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val files = sftp.ls(remotePath).map { file ->
                    SftpFileInfo(
                        name = file.name,
                        path = "$remotePath/${file.name}",
                        size = file.attributes.size,
                        isDirectory = file.isDirectory,
                        lastModified = file.attributes.mtime * 1000L
                    )
                }
                
                Timber.d("Listed ${files.size} files from $remotePath")
                SftpResult.Success(files)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to list files")
                SftpResult.Error("Failed to list files: ${e.message}", e)
            }
        }
    
    /**
     * Download file to InputStream
     */
    suspend fun downloadFile(sftp: SFTPClient, remotePath: String): SftpResult<InputStream> =
        withContext(Dispatchers.IO) {
            try {
                val remoteFile: RemoteFile = sftp.open(remotePath)
                val inputStream = remoteFile.RemoteFileInputStream()
                
                Timber.d("Opened SFTP file: $remotePath")
                SftpResult.Success(inputStream)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to download file")
                SftpResult.Error("Failed to download: ${e.message}", e)
            }
        }
    
    /**
     * Upload file from InputStream
     */
    suspend fun uploadFile(
        sftp: SFTPClient,
        remotePath: String,
        inputStream: InputStream
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            sftp.put(inputStream, remotePath)
            Timber.d("Uploaded file to $remotePath")
            SftpResult.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            SftpResult.Error("Failed to upload: ${e.message}", e)
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(sftp: SFTPClient, remotePath: String): SftpResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                sftp.rm(remotePath)
                Timber.d("Deleted file: $remotePath")
                SftpResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete file")
                SftpResult.Error("Failed to delete: ${e.message}", e)
            }
        }
    
    /**
     * Create directory
     */
    suspend fun createDirectory(sftp: SFTPClient, remotePath: String): SftpResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                sftp.mkdir(remotePath)
                Timber.d("Created directory: $remotePath")
                SftpResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create directory")
                SftpResult.Error("Failed to create directory: ${e.message}", e)
            }
        }
    
    /**
     * Test connection
     */
    suspend fun testConnection(
        server: String,
        username: String,
        password: String,
        port: Int = 22
    ): SftpResult<Boolean> = withContext(Dispatchers.IO) {
        when (val result = connect(server, username, password, port)) {
            is SftpResult.Success -> {
                result.data.close()
                SftpResult.Success(true)
            }
            is SftpResult.Error -> result
        }
    }
}

data class SftpFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)
```

---

## 4. FTP Protocol (Apache Commons Net)

### 4.1 FtpClient Implementation
- [ ] Create `data/network/ftp/FtpClient.kt`:
```kotlin
package com.sza.fastmediasorter.data.network.ftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class FtpResult<out T> {
    data class Success<T>(val data: T) : FtpResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : FtpResult<Nothing>()
}

@Singleton
class FtpClient @Inject constructor() {
    
    /**
     * Connect to FTP server
     */
    suspend fun connect(
        server: String,
        username: String,
        password: String,
        port: Int = 21
    ): FtpResult<FTPClient> = withContext(Dispatchers.IO) {
        val ftp = FTPClient()
        try {
            ftp.connect(server, port)
            ftp.login(username, password)
            
            val replyCode = ftp.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftp.disconnect()
                return@withContext FtpResult.Error("FTP server refused connection")
            }
            
            ftp.enterLocalPassiveMode() // PASV mode
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            ftp.setControlKeepAliveTimeout(30)
            
            Timber.d("FTP connected: $server:$port")
            FtpResult.Success(ftp)
            
        } catch (e: Exception) {
            Timber.e(e, "FTP connection failed")
            try {
                ftp.disconnect()
            } catch (ignored: Exception) {}
            FtpResult.Error("Failed to connect to FTP: ${e.message}", e)
        }
    }
    
    /**
     * List files in directory
     */
    suspend fun listFiles(ftp: FTPClient, remotePath: String): FtpResult<List<FtpFileInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val files = ftp.listFiles(remotePath).map { file ->
                    FtpFileInfo(
                        name = file.name,
                        path = "$remotePath/${file.name}",
                        size = file.size,
                        isDirectory = file.isDirectory,
                        lastModified = file.timestamp.timeInMillis
                    )
                }
                
                Timber.d("Listed ${files.size} files from $remotePath")
                FtpResult.Success(files)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to list files")
                FtpResult.Error("Failed to list files: ${e.message}", e)
            }
        }
    
    /**
     * Download file to InputStream
     * CRITICAL: Do NOT call completePendingCommand() after exceptions!
     */
    suspend fun downloadFile(ftp: FTPClient, remotePath: String): FtpResult<InputStream> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = ftp.retrieveFileStream(remotePath)
                    ?: return@withContext FtpResult.Error("Failed to open FTP stream")
                
                Timber.d("Opened FTP file: $remotePath")
                FtpResult.Success(inputStream)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to download file")
                // DO NOT call ftp.completePendingCommand() here!
                FtpResult.Error("Failed to download: ${e.message}", e)
            }
        }
    
    /**
     * Upload file from InputStream
     */
    suspend fun uploadFile(
        ftp: FTPClient,
        remotePath: String,
        inputStream: InputStream
    ): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = ftp.storeFile(remotePath, inputStream)
            if (!success) {
                return@withContext FtpResult.Error("Upload failed: ${ftp.replyString}")
            }
            
            Timber.d("Uploaded file to $remotePath")
            FtpResult.Success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            FtpResult.Error("Failed to upload: ${e.message}", e)
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(ftp: FTPClient, remotePath: String): FtpResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val success = ftp.deleteFile(remotePath)
                if (!success) {
                    return@withContext FtpResult.Error("Delete failed: ${ftp.replyString}")
                }
                
                Timber.d("Deleted file: $remotePath")
                FtpResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete file")
                FtpResult.Error("Failed to delete: ${e.message}", e)
            }
        }
    
    /**
     * Create directory
     */
    suspend fun createDirectory(ftp: FTPClient, remotePath: String): FtpResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val success = ftp.makeDirectory(remotePath)
                if (!success) {
                    return@withContext FtpResult.Error("mkdir failed: ${ftp.replyString}")
                }
                
                Timber.d("Created directory: $remotePath")
                FtpResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create directory")
                FtpResult.Error("Failed to create directory: ${e.message}", e)
            }
        }
    
    /**
     * Test connection
     */
    suspend fun testConnection(
        server: String,
        username: String,
        password: String,
        port: Int = 21
    ): FtpResult<Boolean> = withContext(Dispatchers.IO) {
        when (val result = connect(server, username, password, port)) {
            is FtpResult.Success -> {
                result.data.disconnect()
                FtpResult.Success(true)
            }
            is FtpResult.Error -> result
        }
    }
}

data class FtpFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)
```

---

## 5. Unified File Cache Specification

### 5.1 UnifiedFileCache Architecture

**PURPOSE**: Временный кеш для network files во время просмотра (избегает повторных загрузок)

**ANTI-PATTERN (V1)**: Файлы кешировались в external storage → доступны всем приложениям

**CORRECT APPROACH**: Использовать `context.cacheDir` (автоматически очищается при нехватке места)

- [ ] Create `data/cache/UnifiedFileCache.kt`

**Interface Contract**:
```kotlin
class UnifiedFileCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun cacheFile(remotePath: String, inputStream: InputStream): String  // Returns local path
    fun isCached(remotePath: String): Boolean
    fun getCachedFilePath(remotePath: String): String?
    suspend fun clearCache()
    fun getCacheSize(): Long
}
```

**Cache Directory Structure**:
```
context.cacheDir/
  └── network_files/
      ├── server1_share_folder_file1.jpg  (filename sanitized)
      ├── server1_share_folder_file2.mp4
      └── server2_documents_report.pdf
```

**Implementation Requirements**:

1. **Path Sanitization**:
   - Remote path: `smb://server1/share/folder/file.jpg`
   - Cached filename: `server1_share_folder_file.jpg`
   - Replace `/`, `\`, `:` with `_`
   - Use MD5 hash если filename > 255 chars (filesystem limit)

2. **`cacheFile()` Logic**:
   - Create cache directory if not exists
   - Sanitize remotePath to filename
   - Create `File(cacheDir, filename)`
   - Copy `inputStream` to file: `inputStream.copyTo(fileOutputStream)`
   - Return `file.absolutePath`
   - Execute на `Dispatchers.IO`

3. **`isCached()` Logic**:
   - Sanitize remotePath
   - Check `File(cacheDir, filename).exists()`

4. **`getCachedFilePath()` Logic**:
   - Similar to `isCached()`, but return path or null

5. **`clearCache()` Logic**:
   - Delete all files: `cacheDir.listFiles()?.forEach { it.delete() }`
   - Called from Settings → "Clear cache"

6. **`getCacheSize()` Logic**:
   - Sum all file sizes: `cacheDir.listFiles()?.sumOf { it.length() } ?: 0L`
   - Display в Settings: "Cache size: 125.5 MB"

**Cache Lifecycle**:
- Files НЕ удаляются после использования (могут быть re-viewed)
- Android автоматически очищает `cacheDir` при low storage
- User может очистить вручную через Settings

**Cache Strategy Comparison**:

| Approach | Location | Auto-cleanup | Pros | Cons |
|----------|----------|--------------|------|------|
| **cacheDir** | `/data/data/app/cache` | Yes (OS) | Secure, automatic | Limited size (~50MB) |
| externalCache | `/sdcard/Android/data/app/cache` | Yes (OS) | Large size | Less secure |
| filesDir | `/data/data/app/files` | No | Persistent | Manual cleanup needed |

**Recommendation**: Use `cacheDir` для temporary viewing, не для long-term storage

**Performance Notes**:
- Cache miss: Download from network (~500ms for 5MB file)
- Cache hit: Read from disk (~50ms for 5MB file)
- 10x faster для repeated views

---

## 6. Network Media Scanners

### 6.1 SmbMediaScanner
- [ ] Create `data/scanner/SmbMediaScanner.kt`:
```kotlin
package com.sza.fastmediasorter.data.scanner

import com.sza.fastmediasorter.data.network.smb.SmbClient
import com.sza.fastmediasorter.data.network.smb.SmbConnectionPool
import com.sza.fastmediasorter.data.network.smb.SmbResult
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import timber.log.Timber
import javax.inject.Inject

class SmbMediaScanner @Inject constructor(
    private val smbConnectionPool: SmbConnectionPool,
    private val credentialsRepository: NetworkCredentialsRepository
) {
    
    suspend fun scanFolder(
        resourceId: Long,
        server: String,
        shareName: String,
        remotePath: String
    ): List<MediaFile> {
        val credentials = credentialsRepository.getCredentials(resourceId)
            ?: return emptyList()
        
        val password = credentialsRepository.getPassword(resourceId)
            ?: return emptyList()
        
        when (val connectResult = smbConnectionPool.getConnection(
            resourceId, server, shareName,
            credentials.username, password, credentials.domain
        )) {
            is SmbResult.Success -> {
                val share = connectResult.data
                
                return when (val listResult = smbClient.listFiles(share, remotePath)) {
                    is SmbResult.Success -> {
                        listResult.data
                            .filter { !it.isDirectory }
                            .mapNotNull { fileInfo ->
                                val type = detectMediaType(fileInfo.name)
                                if (type != MediaType.OTHER) {
                                    MediaFile(
                                        path = fileInfo.path,
                                        name = fileInfo.name,
                                        size = fileInfo.size,
                                        date = fileInfo.lastModified,
                                        type = type,
                                        duration = 0,
                                        thumbnailUrl = null
                                    )
                                } else null
                            }
                    }
                    is SmbResult.Error -> {
                        Timber.e("Failed to list SMB files: ${listResult.message}")
                        emptyList()
                    }
                }
            }
            is SmbResult.Error -> {
                Timber.e("Failed to connect to SMB: ${connectResult.message}")
                return emptyList()
            }
        }
    }
    
    private fun detectMediaType(fileName: String): MediaType {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            in listOf("jpg", "jpeg", "png", "webp", "bmp") -> MediaType.IMAGE
            "gif" -> MediaType.GIF
            in listOf("mp4", "mkv", "avi", "mov", "wmv") -> MediaType.VIDEO
            in listOf("mp3", "wav", "flac", "m4a", "ogg") -> MediaType.AUDIO
            "pdf" -> MediaType.PDF
            in listOf("txt", "log", "json", "xml") -> MediaType.TXT
            "epub" -> MediaType.EPUB
            else -> MediaType.OTHER
        }
    }
}
```

---

## 7. Dependencies (build.gradle.kts)

- [ ] Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // SMB (SMBJ + BouncyCastle)
    implementation("com.hierynomus:smbj:0.12.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    
    // SFTP (SSHJ)
    implementation("com.hierynomus:sshj:0.37.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
    
    // FTP (Apache Commons Net)
    implementation("commons-net:commons-net:3.10.0")
    
    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

---

## 8. Completion Checklist

**Credentials**:
- [ ] NetworkCredentialsRepository with EncryptedSharedPreferences
- [ ] Credentials dialog with server/username/password/domain/port fields
- [ ] Test connection button in dialog

**SMB**:
- [ ] SmbClient with SMBJ 0.12.1 + BouncyCastle 1.78.1
- [ ] Connection pooling with refCount and idle timeout
- [ ] Operations: list, download, upload, delete, mkdir
- [ ] SmbMediaScanner for file discovery

**SFTP**:
- [ ] SftpClient with SSHJ 0.37.0
- [ ] Password authentication (key auth optional for later)
- [ ] Operations: list, download, upload, delete, mkdir

**FTP**:
- [ ] FtpClient with Apache Commons Net 3.10.0
- [ ] PASV mode with active mode fallback
- [ ] CRITICAL: No completePendingCommand() after exceptions
- [ ] Operations: list, download, upload, delete, mkdir

**Caching**:
- [ ] UnifiedFileCache for network file caching
- [ ] Cache size monitoring
- [ ] Clear cache functionality in settings

**Success Criteria**: Successfully connect to SMB/SFTP/FTP servers, list files, download/upload files, connection pooling works, credentials stored securely.

**Next**: Epic 5 (Cloud Integration) for Google Drive/OneDrive/Dropbox OAuth.
