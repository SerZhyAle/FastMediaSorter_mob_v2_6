package com.sza.fastmediasorter.data.network.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File as SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMB/CIFS client for network file operations using SMBJ library.
 * Supports SMB2/SMB3 protocols with connection pooling.
 */
@Singleton
class SmbClient @Inject constructor() : Closeable {

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val READ_TIMEOUT_MS = 60000L
        private const val WRITE_TIMEOUT_MS = 60000L
        private const val CONNECTION_IDLE_TIMEOUT_MS = 30000L
    }

    private val config: SmbConfig by lazy {
        SmbConfig.builder()
            .withTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .withSoTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .withMultiProtocolNegotiate(true)
            .build()
    }

    private val client: SMBClient by lazy { SMBClient(config) }
    
    // Connection pool: key = "host:share"
    private val connectionPool = ConcurrentHashMap<String, PooledConnection>()
    private val connectionMutex = Mutex()

    data class SmbConnectionInfo(
        val host: String,
        val share: String,
        val username: String,
        val password: String,
        val domain: String = ""
    )

    data class SmbFileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    sealed class SmbResult<out T> {
        data class Success<T>(val data: T) : SmbResult<T>()
        data class Error(val message: String, val cause: Throwable? = null) : SmbResult<Nothing>()
    }

    private data class PooledConnection(
        val connection: Connection,
        val session: Session,
        val share: DiskShare,
        var lastUsed: Long = System.currentTimeMillis()
    ) : Closeable {
        override fun close() {
            try {
                share.close()
                session.close()
                connection.close()
            } catch (e: Exception) {
                Timber.w(e, "Error closing SMB connection")
            }
        }
    }

    /**
     * Lists files in a remote directory.
     */
    suspend fun listFiles(
        connectionInfo: SmbConnectionInfo,
        remotePath: String
    ): SmbResult<List<SmbFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            
            val files = share.list(path).mapNotNull { fileInfo ->
                if (fileInfo.fileName == "." || fileInfo.fileName == "..") {
                    null
                } else {
                    SmbFileInfo(
                        name = fileInfo.fileName,
                        path = if (path.isEmpty()) fileInfo.fileName else "$path/${fileInfo.fileName}",
                        isDirectory = fileInfo.fileAttributes.toInt() and 0x10 != 0, // FILE_ATTRIBUTE_DIRECTORY
                        size = fileInfo.endOfFile,
                        lastModified = fileInfo.lastWriteTime.toEpochMillis()
                    )
                }
            }
            
            Timber.d("Listed ${files.size} files in $remotePath")
            SmbResult.Success(files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list files: $remotePath")
            SmbResult.Error("Failed to list files: ${e.message}", e)
        }
    }

    /**
     * Downloads a file from SMB share.
     */
    suspend fun downloadFile(
        connectionInfo: SmbConnectionInfo,
        remotePath: String,
        outputStream: OutputStream,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): SmbResult<Long> = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            
            val file = share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )

            file.use { smbFile ->
                val inputStream = smbFile.inputStream
                val fileSize = smbFile.fileInformation.standardInformation.endOfFile
                var totalBytesRead = 0L
                val buffer = ByteArray(8192)

                inputStream.use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        progressCallback?.invoke(totalBytesRead, fileSize)
                    }
                }

                Timber.d("Downloaded $totalBytesRead bytes from $remotePath")
                SmbResult.Success(totalBytesRead)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file: $remotePath")
            SmbResult.Error("Failed to download: ${e.message}", e)
        }
    }

    /**
     * Uploads a file to SMB share.
     */
    suspend fun uploadFile(
        connectionInfo: SmbConnectionInfo,
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): SmbResult<Long> = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            
            val file = share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            )

            file.use { smbFile ->
                val outputStream = smbFile.outputStream
                var totalBytesWritten = 0L
                val buffer = ByteArray(8192)

                outputStream.use { output ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesWritten += bytesRead
                        progressCallback?.invoke(totalBytesWritten, fileSize)
                    }
                }

                Timber.d("Uploaded $totalBytesWritten bytes to $remotePath")
                SmbResult.Success(totalBytesWritten)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file: $remotePath")
            SmbResult.Error("Failed to upload: ${e.message}", e)
        }
    }

    /**
     * Deletes a file from SMB share.
     */
    suspend fun deleteFile(
        connectionInfo: SmbConnectionInfo,
        remotePath: String
    ): SmbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            
            share.rm(path)
            
            Timber.d("Deleted file: $remotePath")
            SmbResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file: $remotePath")
            SmbResult.Error("Failed to delete: ${e.message}", e)
        }
    }

    /**
     * Creates a directory on SMB share.
     */
    suspend fun createDirectory(
        connectionInfo: SmbConnectionInfo,
        remotePath: String
    ): SmbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            
            share.mkdir(path)
            
            Timber.d("Created directory: $remotePath")
            SmbResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create directory: $remotePath")
            SmbResult.Error("Failed to create directory: ${e.message}", e)
        }
    }

    /**
     * Checks if a file or directory exists.
     */
    suspend fun exists(
        connectionInfo: SmbConnectionInfo,
        remotePath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val share = getOrCreateConnection(connectionInfo)
            val path = remotePath.trimStart('/')
            share.fileExists(path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check existence: $remotePath")
            false
        }
    }

    /**
     * Tests connection to SMB share.
     */
    suspend fun testConnection(connectionInfo: SmbConnectionInfo): SmbResult<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val share = getOrCreateConnection(connectionInfo)
                // Try to list root to verify connection works
                share.list("")
                SmbResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                SmbResult.Error("Connection failed: ${e.message}", e)
            }
        }

    private suspend fun getOrCreateConnection(info: SmbConnectionInfo): DiskShare = 
        connectionMutex.withLock {
            val key = "${info.host}:${info.share}"
            
            // Check for existing valid connection
            connectionPool[key]?.let { pooled ->
                if (isConnectionValid(pooled)) {
                    pooled.lastUsed = System.currentTimeMillis()
                    return pooled.share
                } else {
                    pooled.close()
                    connectionPool.remove(key)
                }
            }
            
            // Create new connection
            val connection = client.connect(info.host)
            val authContext = AuthenticationContext(
                info.username,
                info.password.toCharArray(),
                info.domain
            )
            val session = connection.authenticate(authContext)
            val share = session.connectShare(info.share) as DiskShare
            
            val pooled = PooledConnection(connection, session, share)
            connectionPool[key] = pooled
            
            Timber.d("Created new SMB connection to ${info.host}/${info.share}")
            share
        }

    private fun isConnectionValid(pooled: PooledConnection): Boolean {
        return try {
            pooled.connection.isConnected &&
            (System.currentTimeMillis() - pooled.lastUsed) < CONNECTION_IDLE_TIMEOUT_MS
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Closes all connections in the pool.
     */
    override fun close() {
        connectionPool.values.forEach { it.close() }
        connectionPool.clear()
        client.close()
        Timber.d("SMB client closed")
    }

    /**
     * Closes idle connections older than the timeout.
     */
    fun cleanupIdleConnections() {
        val now = System.currentTimeMillis()
        connectionPool.entries.removeIf { (key, pooled) ->
            if (now - pooled.lastUsed > CONNECTION_IDLE_TIMEOUT_MS) {
                pooled.close()
                Timber.d("Closed idle connection: $key")
                true
            } else {
                false
            }
        }
    }
}
