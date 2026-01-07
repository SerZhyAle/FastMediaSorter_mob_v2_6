package com.sza.fastmediasorter.data.network.ftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import timber.log.Timber
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FTP client for file transfer using Apache Commons Net library.
 * Supports both active and passive mode connections.
 */
@Singleton
class FtpClient @Inject constructor() : Closeable {

    companion object {
        private const val DEFAULT_PORT = 21
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val DATA_TIMEOUT_MS = 60000
        private const val CONNECTION_IDLE_TIMEOUT_MS = 30000L
        private const val BUFFER_SIZE = 8192
    }

    // Connection pool: key = "host:port:username"
    private val connectionPool = ConcurrentHashMap<String, PooledConnection>()
    private val connectionMutex = Mutex()

    data class FtpConnectionInfo(
        val host: String,
        val port: Int = DEFAULT_PORT,
        val username: String = "anonymous",
        val password: String = "anonymous@",
        val usePassiveMode: Boolean = true,
        val useTLS: Boolean = false // For FTPS
    )

    data class FtpFileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    sealed class FtpResult<out T> {
        data class Success<T>(val data: T) : FtpResult<T>()
        data class Error(val message: String, val cause: Throwable? = null) : FtpResult<Nothing>()
    }

    private data class PooledConnection(
        val client: FTPClient,
        var lastUsed: Long = System.currentTimeMillis()
    ) : Closeable {
        override fun close() {
            try {
                if (client.isConnected) {
                    client.logout()
                    client.disconnect()
                }
            } catch (e: Exception) {
                Timber.w(e, "Error closing FTP connection")
            }
        }
    }

    /**
     * Lists files in a remote directory.
     */
    suspend fun listFiles(
        connectionInfo: FtpConnectionInfo,
        remotePath: String
    ): FtpResult<List<FtpFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            val path = if (remotePath.isEmpty()) "/" else remotePath
            
            val files = ftp.listFiles(path).mapNotNull { file ->
                if (file.name == "." || file.name == "..") {
                    null
                } else {
                    file.toFtpFileInfo(path)
                }
            }
            
            Timber.d("Listed ${files.size} files in $remotePath")
            FtpResult.Success(files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list files: $remotePath")
            FtpResult.Error("Failed to list files: ${e.message}", e)
        }
    }

    /**
     * Downloads a file from FTP server.
     */
    suspend fun downloadFile(
        connectionInfo: FtpConnectionInfo,
        remotePath: String,
        outputStream: OutputStream,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): FtpResult<Long> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            // Get file size
            val files = ftp.listFiles(remotePath)
            val fileSize = files.firstOrNull()?.size ?: 0L
            
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            
            val inputStream = ftp.retrieveFileStream(remotePath)
                ?: return@withContext FtpResult.Error("Failed to open remote file: ${ftp.replyString}")
            
            var totalBytesRead = 0L
            val buffer = ByteArray(BUFFER_SIZE)
            
            inputStream.use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    progressCallback?.invoke(totalBytesRead, fileSize)
                }
            }
            
            // Complete pending command
            if (!ftp.completePendingCommand()) {
                return@withContext FtpResult.Error("Failed to complete download: ${ftp.replyString}")
            }
            
            Timber.d("Downloaded $totalBytesRead bytes from $remotePath")
            FtpResult.Success(totalBytesRead)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file: $remotePath")
            FtpResult.Error("Failed to download: ${e.message}", e)
        }
    }

    /**
     * Uploads a file to FTP server.
     */
    suspend fun uploadFile(
        connectionInfo: FtpConnectionInfo,
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): FtpResult<Long> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            
            val outputStream = ftp.storeFileStream(remotePath)
                ?: return@withContext FtpResult.Error("Failed to create remote file: ${ftp.replyString}")
            
            var totalBytesWritten = 0L
            val buffer = ByteArray(BUFFER_SIZE)
            
            outputStream.use { output ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    progressCallback?.invoke(totalBytesWritten, fileSize)
                }
            }
            
            // Complete pending command
            if (!ftp.completePendingCommand()) {
                return@withContext FtpResult.Error("Failed to complete upload: ${ftp.replyString}")
            }
            
            Timber.d("Uploaded $totalBytesWritten bytes to $remotePath")
            FtpResult.Success(totalBytesWritten)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file: $remotePath")
            FtpResult.Error("Failed to upload: ${e.message}", e)
        }
    }

    /**
     * Deletes a file from FTP server.
     */
    suspend fun deleteFile(
        connectionInfo: FtpConnectionInfo,
        remotePath: String
    ): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            if (!ftp.deleteFile(remotePath)) {
                return@withContext FtpResult.Error("Failed to delete: ${ftp.replyString}")
            }
            
            Timber.d("Deleted file: $remotePath")
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file: $remotePath")
            FtpResult.Error("Failed to delete: ${e.message}", e)
        }
    }

    /**
     * Creates a directory on FTP server.
     */
    suspend fun createDirectory(
        connectionInfo: FtpConnectionInfo,
        remotePath: String
    ): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            if (!ftp.makeDirectory(remotePath)) {
                return@withContext FtpResult.Error("Failed to create directory: ${ftp.replyString}")
            }
            
            Timber.d("Created directory: $remotePath")
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create directory: $remotePath")
            FtpResult.Error("Failed to create directory: ${e.message}", e)
        }
    }

    /**
     * Renames/moves a file on FTP server.
     */
    suspend fun rename(
        connectionInfo: FtpConnectionInfo,
        oldPath: String,
        newPath: String
    ): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            if (!ftp.rename(oldPath, newPath)) {
                return@withContext FtpResult.Error("Failed to rename: ${ftp.replyString}")
            }
            
            Timber.d("Renamed $oldPath to $newPath")
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename: $oldPath -> $newPath")
            FtpResult.Error("Failed to rename: ${e.message}", e)
        }
    }

    /**
     * Gets the current working directory.
     */
    suspend fun getWorkingDirectory(
        connectionInfo: FtpConnectionInfo
    ): FtpResult<String> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            val pwd = ftp.printWorkingDirectory()
                ?: return@withContext FtpResult.Error("Failed to get working directory")
            
            FtpResult.Success(pwd)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get working directory")
            FtpResult.Error("Failed to get working directory: ${e.message}", e)
        }
    }

    /**
     * Changes the working directory.
     */
    suspend fun changeDirectory(
        connectionInfo: FtpConnectionInfo,
        remotePath: String
    ): FtpResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val ftp = getOrCreateConnection(connectionInfo)
            
            if (!ftp.changeWorkingDirectory(remotePath)) {
                return@withContext FtpResult.Error("Failed to change directory: ${ftp.replyString}")
            }
            
            FtpResult.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Failed to change directory: $remotePath")
            FtpResult.Error("Failed to change directory: ${e.message}", e)
        }
    }

    /**
     * Tests connection to FTP server.
     */
    suspend fun testConnection(connectionInfo: FtpConnectionInfo): FtpResult<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val ftp = getOrCreateConnection(connectionInfo)
                // Try to get working directory to verify connection works
                ftp.printWorkingDirectory()
                FtpResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                FtpResult.Error("Connection failed: ${e.message}", e)
            }
        }

    private suspend fun getOrCreateConnection(info: FtpConnectionInfo): FTPClient = 
        connectionMutex.withLock {
            val key = "${info.host}:${info.port}:${info.username}"
            
            // Check for existing valid connection
            connectionPool[key]?.let { pooled ->
                if (isConnectionValid(pooled)) {
                    pooled.lastUsed = System.currentTimeMillis()
                    return pooled.client
                } else {
                    pooled.close()
                    connectionPool.remove(key)
                }
            }
            
            // Create new connection
            val ftp = FTPClient().apply {
                connectTimeout = CONNECTION_TIMEOUT_MS
                defaultTimeout = DATA_TIMEOUT_MS
                bufferSize = BUFFER_SIZE
            }
            
            ftp.connect(info.host, info.port)
            
            val replyCode = ftp.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftp.disconnect()
                throw Exception("FTP server refused connection: $replyCode")
            }
            
            if (!ftp.login(info.username, info.password)) {
                ftp.disconnect()
                throw Exception("FTP login failed: ${ftp.replyString}")
            }
            
            // Set passive mode if requested (usually needed for NAT/firewall)
            if (info.usePassiveMode) {
                ftp.enterLocalPassiveMode()
            }
            
            // Set binary mode for file transfers
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            
            val pooled = PooledConnection(ftp)
            connectionPool[key] = pooled
            
            Timber.d("Created new FTP connection to ${info.host}:${info.port}")
            ftp
        }

    private fun isConnectionValid(pooled: PooledConnection): Boolean {
        return try {
            pooled.client.isConnected &&
            pooled.client.sendNoOp() &&
            (System.currentTimeMillis() - pooled.lastUsed) < CONNECTION_IDLE_TIMEOUT_MS
        } catch (e: Exception) {
            false
        }
    }

    private fun FTPFile.toFtpFileInfo(parentPath: String): FtpFileInfo {
        val fullPath = if (parentPath.endsWith("/")) {
            "$parentPath$name"
        } else {
            "$parentPath/$name"
        }
        
        return FtpFileInfo(
            name = name,
            path = fullPath,
            isDirectory = isDirectory,
            size = size,
            lastModified = timestamp?.timeInMillis ?: 0L
        )
    }

    /**
     * Closes all connections in the pool.
     */
    override fun close() {
        connectionPool.values.forEach { it.close() }
        connectionPool.clear()
        Timber.d("FTP client closed")
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
