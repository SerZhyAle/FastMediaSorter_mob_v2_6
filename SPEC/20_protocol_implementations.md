# 20. Protocol Implementations

## Overview

FastMediaSorter v2 supports 5 network/cloud protocols plus local file system operations. This document provides implementation patterns and code examples for each protocol.

---

## SMB (Server Message Block)

### Dependencies

```kotlin
implementation("com.hierynomus:smbj:0.12.1")
implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") // CRITICAL
implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
```

**⚠️ Critical**: BouncyCastle 1.78.1 required to prevent `libpenguin.so` native crash on ARM devices.

### Connection Pattern

```kotlin
class SmbClient @Inject constructor() {
    private val client = SMBClient(
        SmbConfig.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withSoTimeout(30, TimeUnit.SECONDS)
            .build()
    )
    
    suspend fun connect(
        server: String,
        shareName: String,
        username: String,
        password: String,
        domain: String = "",
        port: Int = 445
    ): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            val connection = client.connect(server, port)
            val session = connection.authenticate(
                AuthenticationContext(username, password.toCharArray(), domain)
            )
            val share = session.connectShare(shareName) as DiskShare
            
            Result.success(connection)
        } catch (e: Exception) {
            Timber.e(e, "SMB connection failed: $server")
            Result.failure(e)
        }
    }
}
```

### File Operations

```kotlin
// List files
suspend fun listFiles(remotePath: String): Result<List<MediaFile>> {
    return try {
        val share = getShare() // From connection pool
        val files = share.list(remotePath)
            .filter { !it.fileName.startsWith(".") } // Hide hidden files
            .map { fileInfo ->
                MediaFile(
                    path = "$remotePath/${fileInfo.fileName}",
                    name = fileInfo.fileName,
                    size = fileInfo.standardInformation.endOfFile,
                    date = fileInfo.lastWriteTime.toEpochMilli(),
                    type = detectMediaType(fileInfo.fileName)
                )
            }
        Result.success(files)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Download file
suspend fun downloadFile(
    remotePath: String, 
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    return try {
        val share = getShare()
        val file = share.openFile(
            remotePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
        
        val inputStream = file.inputStream
        val totalSize = file.fileInformation.standardInformation.endOfFile
        
        inputStream.copyToWithProgress(outputStream, totalSize, progressCallback)
        
        file.close()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Upload file
suspend fun uploadFile(
    localFile: File,
    remotePath: String,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    return try {
        val share = getShare()
        val file = share.openFile(
            remotePath,
            EnumSet.of(AccessMask.GENERIC_WRITE),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OVERWRITE_IF,
            null
        )
        
        val outputStream = file.outputStream
        localFile.inputStream().use { input ->
            input.copyToWithProgress(outputStream, localFile.length(), progressCallback)
        }
        
        file.close()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Connection Pooling

```kotlin
private val connectionPool = ConcurrentHashMap<String, PooledSmbConnection>()
private val mutex = Mutex()

private suspend fun getShare(): DiskShare = mutex.withLock {
    val key = "$server:$shareName:$username"
    val pooled = connectionPool[key]
    
    if (pooled != null && pooled.isValid()) {
        pooled.updateLastUsed()
        return pooled.share
    }
    
    // Create new connection
    val connection = connect(server, shareName, username, password, domain, port)
    val share = connection.session.connectShare(shareName) as DiskShare
    
    connectionPool[key] = PooledSmbConnection(share, System.currentTimeMillis())
    scheduleCleanup()
    
    return share
}

private fun scheduleCleanup() {
    // Remove connections idle > 45 seconds
    viewModelScope.launch {
        delay(45_000)
        connectionPool.entries.removeIf { (_, pooled) ->
            System.currentTimeMillis() - pooled.lastUsed > 45_000
        }
    }
}
```

---

## SFTP (SSH File Transfer Protocol)

### Dependencies

```kotlin
implementation("com.hierynomus:sshj:0.37.0")
implementation("net.i2p.crypto:eddsa:0.3.0") // Curve25519 support
```

### Connection Pattern

```kotlin
class SftpClient @Inject constructor() {
    private val ssh = SSHClient()
    
    init {
        ssh.addHostKeyVerifier(PromiscuousVerifier()) // Trust all hosts
        // In production: Use known_hosts file
    }
    
    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<SFTPClient> = withContext(Dispatchers.IO) {
        try {
            ssh.connect(host, port)
            ssh.authPassword(username, password)
            
            val sftp = ssh.newSFTPClient()
            Result.success(sftp)
        } catch (e: Exception) {
            Timber.e(e, "SFTP connection failed: $host")
            Result.failure(e)
        }
    }
    
    suspend fun connectWithKey(
        host: String,
        port: Int,
        username: String,
        privateKeyPath: String
    ): Result<SFTPClient> = withContext(Dispatchers.IO) {
        try {
            ssh.connect(host, port)
            ssh.authPublickey(username, ssh.loadKeys(privateKeyPath))
            
            val sftp = ssh.newSFTPClient()
            Result.success(sftp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### File Operations

```kotlin
// List files
suspend fun listFiles(remotePath: String): Result<List<MediaFile>> {
    return try {
        val sftp = getClient()
        val files = sftp.ls(remotePath)
            .filter { !it.name.startsWith(".") }
            .map { fileEntry ->
                MediaFile(
                    path = "$remotePath/${fileEntry.name}",
                    name = fileEntry.name,
                    size = fileEntry.attributes.size,
                    date = fileEntry.attributes.mtime * 1000L, // Unix timestamp to millis
                    type = detectMediaType(fileEntry.name)
                )
            }
        Result.success(files)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Download file
suspend fun downloadFile(
    remotePath: String,
    localFile: File,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    return try {
        val sftp = getClient()
        val remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.READ))
        
        localFile.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024) // 64KB buffer
            var bytesRead: Int
            var totalRead = 0L
            val totalSize = remoteFile.attributes.size
            
            while (remoteFile.read().also { bytesRead = it.size } > 0) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                progressCallback(totalRead, totalSize)
            }
        }
        
        remoteFile.close()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## FTP (File Transfer Protocol)

### Dependencies

```kotlin
implementation("commons-net:commons-net:3.10.0")
```

### Connection Pattern

```kotlin
class FtpClient @Inject constructor() {
    private val ftp = FTPClient()
    
    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ftp.connect(host, port)
            ftp.login(username, password)
            
            ftp.enterLocalPassiveMode() // PASV mode (default)
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            ftp.setBufferSize(64 * 1024) // 64KB buffer
            
            if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
                throw IOException("FTP connection refused: ${ftp.replyString}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### PASV Mode Timeout Handling

**Problem**: Firewall blocks PASV data connection ports (random high ports)

**Solution**: Active mode fallback

```kotlin
suspend fun downloadFile(
    remotePath: String,
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    return try {
        // Try PASV mode first
        ftp.enterLocalPassiveMode()
        
        try {
            downloadWithProgress(remotePath, outputStream, progressCallback)
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w("PASV mode failed, trying active mode")
            
            // Fallback to active mode
            ftp.enterLocalActiveMode()
            downloadWithProgress(remotePath, outputStream, progressCallback)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun downloadWithProgress(
    remotePath: String,
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
) {
    val totalSize = ftp.mlistFile(remotePath)?.size ?: 0L
    
    ftp.retrieveFile(remotePath, object : OutputStream() {
        private var bytesWritten = 0L
        
        override fun write(b: Int) {
            outputStream.write(b)
            bytesWritten++
            progressCallback(bytesWritten, totalSize)
        }
        
        override fun write(b: ByteArray, off: Int, len: Int) {
            outputStream.write(b, off, len)
            bytesWritten += len
            progressCallback(bytesWritten, totalSize)
        }
    })
    
    // CRITICAL: Do NOT call completePendingCommand() after exceptions
}
```

---

## Google Drive (REST API)

### Dependencies

```kotlin
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.api-client:google-api-client-android:2.2.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20231212-2.0.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0") // For REST calls
```

### OAuth Authentication

```kotlin
class GoogleDriveClient @Inject constructor(
    private val context: Context
) {
    suspend fun authenticate(activity: Activity): AuthResult {
        return withContext(Dispatchers.Main) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@withContext AuthResult.Error("Not signed in")
                
                val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_FILE)
                )
                googleAccountCredential.selectedAccount = account.account
                
                AuthResult.Success(account.email ?: "Unknown")
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    sealed class AuthResult {
        data class Success(val email: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
        object Cancelled : AuthResult()
    }
}
```

### File Listing

```kotlin
suspend fun listFiles(folderId: String): Result<List<MediaFile>> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files?" +
                "q='$folderId'+in+parents+and+trashed=false" +
                "&fields=files(id,name,size,modifiedTime,mimeType,thumbnailLink)" +
                "&pageSize=1000"
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${getAccessToken()}")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val filesArray = json.getJSONArray("files")
            
            val files = (0 until filesArray.length()).map { i ->
                val file = filesArray.getJSONObject(i)
                MediaFile(
                    path = file.getString("id"), // Drive uses ID as path
                    name = file.getString("name"),
                    size = file.optLong("size", 0),
                    date = parseRFC3339(file.getString("modifiedTime")),
                    type = detectMediaType(file.getString("name")),
                    thumbnailUrl = file.optString("thumbnailLink")
                )
            }
            
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### File Download

```kotlin
suspend fun downloadFile(
    fileId: String,
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${getAccessToken()}")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val totalSize = response.body?.contentLength() ?: 0
            
            response.body?.byteStream()?.use { input ->
                input.copyToWithProgress(outputStream, totalSize, progressCallback)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Pagination (Paging3)

### Threshold Detection

```kotlin
// In BrowseViewModel
private val paginationThreshold = 1000

fun loadMediaFiles(resourceId: Long) {
    viewModelScope.launch {
        val fileCount = getFileCountUseCase(resourceId).getOrNull() ?: 0
        
        if (fileCount > paginationThreshold) {
            // Use Paging3
            _mediaFilesPaging.value = Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                    prefetchDistance = 10
                ),
                pagingSourceFactory = { MediaFilePagingSource(resourceId, scanner) }
            ).flow.cachedIn(viewModelScope)
            
            _usePagination.value = true
        } else {
            // Standard loading
            val files = getMediaFilesUseCase(resourceId).getOrNull() ?: emptyList()
            _mediaFiles.value = files
            _usePagination.value = false
        }
    }
}
```

### PagingSource Implementation

```kotlin
class MediaFilePagingSource(
    private val resourceId: Long,
    private val scanner: MediaScanner
) : PagingSource<Int, MediaFile>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaFile> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        
        return try {
            val files = scanner.scanFolderPaged(
                resourcePath = getResourcePath(resourceId),
                offset = offset,
                limit = limit
            )
            
            LoadResult.Page(
                data = files,
                prevKey = if (offset == 0) null else offset - limit,
                nextKey = if (files.size < limit) null else offset + limit
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, MediaFile>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(state.config.pageSize)
        }
    }
}
```

### Adapter Switching

```kotlin
// In BrowseActivity
private fun setupRecyclerView() {
    lifecycleScope.launch {
        viewModel.usePagination.collect { usePaging ->
            if (usePaging) {
                // Switch to PagingDataAdapter
                val pagingAdapter = PagingMediaFileAdapter(onClick = { file -> /* ... */ })
                binding.recyclerView.adapter = pagingAdapter
                
                viewModel.mediaFilesPaging.collect { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            } else {
                // Standard adapter
                val adapter = MediaFileAdapter(onClick = { file -> /* ... */ })
                binding.recyclerView.adapter = adapter
                
                viewModel.mediaFiles.collect { files ->
                    adapter.submitList(files)
                }
            }
        }
    }
}
```

---

## Related Documentation

- [14. Network Operations](14_network_operations.md) - Detailed network client implementations
- [18. Development Workflows](18_development_workflows.md) - Testing protocols
- [21. Common Pitfalls](21_common_pitfalls.md) - Protocol-specific issues
- [24. Dependencies](24_dependencies.md) - Library versions and compatibility notes
