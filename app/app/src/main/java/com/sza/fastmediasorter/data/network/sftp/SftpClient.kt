package com.sza.fastmediasorter.data.network.sftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import timber.log.Timber
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SFTP client for secure file transfer over SSH using SSHJ library.
 * Supports password and key-based authentication.
 */
@Singleton
class SftpClient @Inject constructor() : Closeable {

    companion object {
        private const val DEFAULT_PORT = 22
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val CONNECTION_IDLE_TIMEOUT_MS = 30000L
    }

    // Connection pool: key = "host:port:username"
    private val connectionPool = ConcurrentHashMap<String, PooledConnection>()
    private val connectionMutex = Mutex()

    data class SftpConnectionInfo(
        val host: String,
        val port: Int = DEFAULT_PORT,
        val username: String,
        val password: String,
        val privateKey: String? = null, // Optional: path to private key file
        val passphrase: String? = null  // Optional: passphrase for private key
    )

    data class SftpFileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val permissions: String
    )

    sealed class SftpResult<out T> {
        data class Success<T>(val data: T) : SftpResult<T>()
        data class Error(val message: String, val cause: Throwable? = null) : SftpResult<Nothing>()
    }

    private data class PooledConnection(
        val sshClient: SSHClient,
        val sftpClient: SFTPClient,
        var lastUsed: Long = System.currentTimeMillis()
    ) : Closeable {
        override fun close() {
            try {
                sftpClient.close()
                sshClient.disconnect()
            } catch (e: Exception) {
                Timber.w(e, "Error closing SFTP connection")
            }
        }
    }

    /**
     * Lists files in a remote directory.
     */
    suspend fun listFiles(
        connectionInfo: SftpConnectionInfo,
        remotePath: String
    ): SftpResult<List<SftpFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            val path = if (remotePath.isEmpty()) "/" else remotePath
            
            val files = sftp.ls(path).mapNotNull { resource ->
                if (resource.name == "." || resource.name == "..") {
                    null
                } else {
                    resource.toSftpFileInfo(path)
                }
            }
            
            Timber.d("Listed ${files.size} files in $remotePath")
            SftpResult.Success(files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list files: $remotePath")
            SftpResult.Error("Failed to list files: ${e.message}", e)
        }
    }

    /**
     * Downloads a file from SFTP server.
     */
    suspend fun downloadFile(
        connectionInfo: SftpConnectionInfo,
        remotePath: String,
        outputStream: OutputStream,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): SftpResult<Long> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            
            // Get file size
            val fileSize = sftp.stat(remotePath).size
            
            val remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.READ))
            remoteFile.use { rf ->
                val inputStream = rf.RemoteFileInputStream()
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
                SftpResult.Success(totalBytesRead)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file: $remotePath")
            SftpResult.Error("Failed to download: ${e.message}", e)
        }
    }

    /**
     * Uploads a file to SFTP server.
     */
    suspend fun uploadFile(
        connectionInfo: SftpConnectionInfo,
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): SftpResult<Long> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            
            val remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))
            remoteFile.use { rf ->
                val outputStream = rf.RemoteFileOutputStream()
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
                SftpResult.Success(totalBytesWritten)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file: $remotePath")
            SftpResult.Error("Failed to upload: ${e.message}", e)
        }
    }

    /**
     * Deletes a file from SFTP server.
     */
    suspend fun deleteFile(
        connectionInfo: SftpConnectionInfo,
        remotePath: String
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            sftp.rm(remotePath)
            
            Timber.d("Deleted file: $remotePath")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file: $remotePath")
            SftpResult.Error("Failed to delete: ${e.message}", e)
        }
    }

    /**
     * Creates a directory on SFTP server.
     */
    suspend fun createDirectory(
        connectionInfo: SftpConnectionInfo,
        remotePath: String
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            sftp.mkdir(remotePath)
            
            Timber.d("Created directory: $remotePath")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create directory: $remotePath")
            SftpResult.Error("Failed to create directory: ${e.message}", e)
        }
    }

    /**
     * Renames/moves a file on SFTP server.
     */
    suspend fun rename(
        connectionInfo: SftpConnectionInfo,
        oldPath: String,
        newPath: String
    ): SftpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            sftp.rename(oldPath, newPath)
            
            Timber.d("Renamed $oldPath to $newPath")
            SftpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename: $oldPath -> $newPath")
            SftpResult.Error("Failed to rename: ${e.message}", e)
        }
    }

    /**
     * Gets file information.
     */
    suspend fun stat(
        connectionInfo: SftpConnectionInfo,
        remotePath: String
    ): SftpResult<SftpFileInfo> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateConnection(connectionInfo)
            val attrs = sftp.stat(remotePath)
            val name = remotePath.substringAfterLast('/')
            
            SftpResult.Success(
                SftpFileInfo(
                    name = name,
                    path = remotePath,
                    isDirectory = attrs.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY,
                    size = attrs.size,
                    lastModified = attrs.mtime * 1000L,
                    permissions = attrs.permissions?.toString() ?: ""
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to stat: $remotePath")
            SftpResult.Error("Failed to get file info: ${e.message}", e)
        }
    }

    /**
     * Tests connection to SFTP server.
     */
    suspend fun testConnection(connectionInfo: SftpConnectionInfo): SftpResult<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val sftp = getOrCreateConnection(connectionInfo)
                // Try to list root to verify connection works
                sftp.ls("/")
                SftpResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                SftpResult.Error("Connection failed: ${e.message}", e)
            }
        }

    private suspend fun getOrCreateConnection(info: SftpConnectionInfo): SFTPClient = 
        connectionMutex.withLock {
            val key = "${info.host}:${info.port}:${info.username}"
            
            // Check for existing valid connection
            connectionPool[key]?.let { pooled ->
                if (isConnectionValid(pooled)) {
                    pooled.lastUsed = System.currentTimeMillis()
                    return pooled.sftpClient
                } else {
                    pooled.close()
                    connectionPool.remove(key)
                }
            }
            
            // Create new connection
            val ssh = SSHClient()
            ssh.connectTimeout = CONNECTION_TIMEOUT_MS
            
            // Accept all host keys (in production, you'd want proper host key verification)
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            
            ssh.connect(info.host, info.port)
            
            // Authenticate
            if (info.privateKey != null) {
                // Key-based authentication
                val keys = if (info.passphrase != null) {
                    ssh.loadKeys(info.privateKey, info.passphrase)
                } else {
                    ssh.loadKeys(info.privateKey)
                }
                ssh.authPublickey(info.username, keys)
            } else {
                // Password authentication
                ssh.authPassword(info.username, object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray = 
                        info.password.toCharArray()
                    override fun shouldRetry(resource: Resource<*>?): Boolean = false
                })
            }
            
            val sftp = ssh.newSFTPClient()
            val pooled = PooledConnection(ssh, sftp)
            connectionPool[key] = pooled
            
            Timber.d("Created new SFTP connection to ${info.host}:${info.port}")
            sftp
        }

    private fun isConnectionValid(pooled: PooledConnection): Boolean {
        return try {
            pooled.sshClient.isConnected &&
            (System.currentTimeMillis() - pooled.lastUsed) < CONNECTION_IDLE_TIMEOUT_MS
        } catch (e: Exception) {
            false
        }
    }

    private fun RemoteResourceInfo.toSftpFileInfo(parentPath: String): SftpFileInfo {
        val fullPath = if (parentPath.endsWith("/")) {
            "$parentPath$name"
        } else {
            "$parentPath/$name"
        }
        
        return SftpFileInfo(
            name = name,
            path = fullPath,
            isDirectory = isDirectory,
            size = attributes.size,
            lastModified = attributes.mtime * 1000L,
            permissions = attributes.permissions?.toString() ?: ""
        )
    }

    /**
     * Closes all connections in the pool.
     */
    override fun close() {
        connectionPool.values.forEach { it.close() }
        connectionPool.clear()
        Timber.d("SFTP client closed")
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
