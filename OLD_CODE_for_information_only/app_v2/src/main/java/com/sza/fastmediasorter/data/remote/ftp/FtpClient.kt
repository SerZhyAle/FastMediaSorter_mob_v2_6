@file:Suppress("DEPRECATION")

package com.sza.fastmediasorter.data.remote.ftp

import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level FTP client wrapper using Apache Commons Net
 * Handles FTP connection, authentication and file operations with passive mode
 * 
 * Thread-safe: Uses mutex for synchronized access to FTPClient instance
 */
@Singleton
class FtpClient @Inject constructor() {

    private var ftpClient: FTPClient? = null
    private val mutex = Any() // Synchronization lock for FTPClient operations

    companion object {
        private const val CONNECT_TIMEOUT = 10000 // 10 seconds (reduced from 15s for faster error feedback)
        private const val SOCKET_TIMEOUT = 30000 // 30 seconds
        private const val KEEPALIVE_TIMEOUT = 15L // 15 seconds
    }

    /**
     * Connect to FTP server with password authentication
     * @param host Server hostname or IP address
     * @param port Server port (default 21)
     * @param username Username for authentication
     * @param password Password for authentication
     * @return Result with Unit on success or exception on failure
     */
    suspend fun connect(
        host: String,
        port: Int = 21,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect() // Ensure clean state
            
            val client = FTPClient()
            
            // Set connection and socket timeout to 30/60 seconds for unreliable networks
            client.connectTimeout = CONNECT_TIMEOUT
            client.defaultTimeout = SOCKET_TIMEOUT
            client.setDataTimeout(SOCKET_TIMEOUT)
            client.controlKeepAliveTimeout = Duration.ofSeconds(KEEPALIVE_TIMEOUT).seconds
            
            client.connect(host, port)
            
            val replyCode = client.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                client.disconnect()
                return@withContext Result.failure(
                    IOException("FTP server refused connection. Reply code: $replyCode")
                )
            }
            
            if (!client.login(username, password)) {
                client.disconnect()
                return@withContext Result.failure(
                    IOException("FTP authentication failed for user: $username")
                )
            }
            
            // Enable passive mode (required for most modern FTP servers behind NAT/firewall)
            client.enterLocalPassiveMode()
            
            // Set binary mode for file transfers
            client.setFileType(FTP.BINARY_FILE_TYPE)
            
            ftpClient = client
            
            Timber.d("FTP connected to $host:$port as $username (passive mode)")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP connection failed: $host:$port")
            disconnect()
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP connection error: $host:$port")
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * List files and directories in remote path
     * @param remotePath Remote directory path (default "/")
     * @return Result with list of file names (not full paths) or exception on failure
     */
    /**
     * List files with metadata (name, size, timestamp) in remote directory.
     * Returns FTPFile objects with full attributes.
     * @param recursive If true, scans all subdirectories recursively
     */
    suspend fun listFilesWithMetadata(
        remotePath: String = "/",
        recursive: Boolean = true
    ): Result<List<FTPFile>> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = ftpClient ?: return@withContext Result.failure(
                    IllegalStateException("Not connected. Call connect() first.")
                )
                
                val allFiles = mutableListOf<FTPFile>()
                
                if (recursive) {
                    listFilesWithMetadataRecursive(client, remotePath, allFiles)
                } else {
                    listFilesWithMetadataSingleLevel(client, remotePath, allFiles)
                }
                
                Timber.d("FTP listed ${allFiles.size} files with metadata in $remotePath (recursive=$recursive)")
                Result.success(allFiles)
            } catch (e: IOException) {
                Timber.e(e, "FTP list files with metadata failed: $remotePath")
                Result.failure(e)
            } catch (e: Exception) {
                Timber.e(e, "FTP list files with metadata error: $remotePath")
                Result.failure(e)
            }
        }
    }
    
    /**
     * List files with metadata in single directory level (non-recursive)
     */
    private fun listFilesWithMetadataSingleLevel(
        client: FTPClient,
        remotePath: String,
        results: MutableList<FTPFile>
    ) {
        // Try passive mode first, fallback to active mode on timeout
        val ftpFiles = try {
            Timber.d("FTP listing files with metadata in passive mode: $remotePath")
            client.listFiles(remotePath)
        } catch (e: SocketTimeoutException) {
            Timber.w(e, "FTP passive mode timeout, switching to active mode")
            
            // Switch to active mode and retry
            client.enterLocalActiveMode()
            Timber.d("FTP retrying listFiles in active mode: $remotePath")
            
            try {
                client.listFiles(remotePath)
            } finally {
                // Switch back to passive for future operations
                try { 
                    client.enterLocalPassiveMode() 
                    Timber.d("FTP switched back to passive mode")
                } catch (ignored: Exception) {
                    Timber.w(ignored, "Failed to switch back to passive mode")
                }
            }
        }
        
        // Filter out . and .. entries, add only files (not directories)
        ftpFiles.forEach { ftpFile ->
            if (ftpFile.name != "." && ftpFile.name != ".." && ftpFile.isFile) {
                results.add(ftpFile)
            }
        }
    }
    
    /**
     * List files with metadata recursively in all subdirectories
     */
    private fun listFilesWithMetadataRecursive(
        client: FTPClient,
        remotePath: String,
        results: MutableList<FTPFile>
    ) {
        // Try passive mode first, fallback to active mode on timeout
        val ftpFiles = try {
            Timber.d("FTP listing files with metadata recursively in passive mode: $remotePath")
            client.listFiles(remotePath)
        } catch (e: SocketTimeoutException) {
            Timber.w(e, "FTP passive mode timeout, switching to active mode")
            
            // Switch to active mode and retry
            client.enterLocalActiveMode()
            Timber.d("FTP retrying listFiles in active mode: $remotePath")
            
            try {
                client.listFiles(remotePath)
            } finally {
                // Switch back to passive for future operations
                try { 
                    client.enterLocalPassiveMode() 
                    Timber.d("FTP switched back to passive mode")
                } catch (ignored: Exception) {
                    Timber.w(ignored, "Failed to switch back to passive mode")
                }
            }
        }
        
        ftpFiles.forEach { ftpFile ->
            if (ftpFile.name != "." && ftpFile.name != "..") {
                val subPath = if (remotePath.endsWith("/")) {
                    remotePath + ftpFile.name
                } else {
                    "$remotePath/${ftpFile.name}"
                }
                
                if (ftpFile.isDirectory) {
                    // Recursively scan subdirectory
                    listFilesWithMetadataRecursive(client, subPath, results)
                } else if (ftpFile.isFile) {
                    // Add file to results
                    results.add(ftpFile)
                }
            }
        }
    }

    suspend fun listFiles(remotePath: String = "/"): Result<List<String>> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = ftpClient ?: return@withContext Result.failure(
                    IllegalStateException("Not connected. Call connect() first.")
                )
                
                // Try passive mode first, fallback to active mode on timeout
                val files = try {
                    Timber.d("FTP listing files in passive mode: $remotePath")
                    val ftpFiles = client.listFiles(remotePath)
                    
                    // Filter out . and .. entries
                    ftpFiles.mapNotNull { ftpFile ->
                        if (ftpFile.name == "." || ftpFile.name == "..") null else ftpFile.name
                    }
                } catch (e: SocketTimeoutException) {
                    Timber.w(e, "FTP passive mode timeout, switching to active mode")
                    
                    // Switch to active mode and retry
                    client.enterLocalActiveMode()
                    Timber.d("FTP retrying listFiles in active mode: $remotePath")
                    
                    val ftpFiles = try {
                        client.listFiles(remotePath)
                    } finally {
                        // Switch back to passive for future operations
                        try { 
                            client.enterLocalPassiveMode() 
                            Timber.d("FTP switched back to passive mode")
                        } catch (ignored: Exception) {
                            Timber.w(ignored, "Failed to switch back to passive mode")
                        }
                    }
                    
                    // Filter out . and .. entries
                    ftpFiles.mapNotNull { ftpFile ->
                        if (ftpFile.name == "." || ftpFile.name == "..") null else ftpFile.name
                    }
                }
                
                Timber.d("FTP listed ${files.size} files in $remotePath")
                Result.success(files)
            } catch (e: IOException) {
                Timber.e(e, "FTP list files failed: $remotePath")
                Result.failure(e)
            } catch (e: Exception) {
                Timber.e(e, "FTP list files error: $remotePath")
                Result.failure(e)
            }
        }
    }

    /**
     * Test connection to FTP server
     * @param host Server hostname or IP address
     * @param port Server port (default 21)
     * @param username Username for authentication
     * @param password Password for authentication
     * @return Result with true on success or exception on failure
     */
    suspend fun testConnection(
        host: String,
        port: Int = 21,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testClient = FTPClient()
            
            // Set connection and socket timeout to 30/60 seconds for unreliable networks
            testClient.connectTimeout = CONNECT_TIMEOUT
            testClient.defaultTimeout = SOCKET_TIMEOUT
            testClient.setDataTimeout(SOCKET_TIMEOUT)
            testClient.controlKeepAliveTimeout = Duration.ofSeconds(KEEPALIVE_TIMEOUT).seconds
            
            testClient.connect(host, port)
            
            val replyCode = testClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                testClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP server refused connection. Reply code: $replyCode")
                )
            }
            
            if (!testClient.login(username, password)) {
                testClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP authentication failed for user: $username")
                )
            }
            
            // Enable passive mode
            testClient.enterLocalPassiveMode()
            
            // Test listing root directory
            testClient.listFiles("/")
            
            testClient.logout()
            testClient.disconnect()
            
            Timber.d("FTP test connection successful: $host:$port")
            Result.success(true)
        } catch (e: IOException) {
            Timber.e(e, "FTP test connection failed: $host:$port")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP test connection error: $host:$port")
            Result.failure(e)
        }
    }

    /**
     * Read file bytes from FTP server (useful for thumbnails and image loading)
     * @param remotePath Full path to remote file
     * @param maxBytes Maximum bytes to read (default: read entire file)
     * @return Result with ByteArray or exception on failure
     */
    suspend fun readFileBytes(
        remotePath: String,
        maxBytes: Long = Long.MAX_VALUE
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            client.retrieveFileStream(remotePath)?.use { inputStream ->
                val bytes = if (maxBytes < Long.MAX_VALUE) {
                    inputStream.readNBytes(maxBytes.toInt())
                } else {
                    inputStream.readBytes()
                }
                
                // Must complete transfer
                if (!client.completePendingCommand()) {
                    return@withContext Result.failure(
                        IOException("FTP command failed after retrieving file")
                    )
                }
                
                Timber.d("FTP read ${bytes.size} bytes from $remotePath")
                Result.success(bytes)
            } ?: Result.failure(IOException("Failed to open file stream: $remotePath"))
        } catch (e: IOException) {
            Timber.e(e, "FTP read file bytes failed: $remotePath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP read file bytes error: $remotePath")
            Result.failure(e)
        }
    }
    
    /**
     * Read byte range from FTP file (for sparse video reading).
     * Uses REST command to resume from offset.
     */
    suspend fun readFileBytesRange(
        remotePath: String,
        offset: Long,
        length: Long
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            // Use REST command to start reading from offset
            client.setRestartOffset(offset)
            
            client.retrieveFileStream(remotePath)?.use { inputStream ->
                val buffer = ByteArray(length.toInt())
                var totalRead = 0
                
                while (totalRead < length) {
                    val read = inputStream.read(buffer, totalRead, (length - totalRead).toInt())
                    if (read == -1) break
                    totalRead += read
                }
                
                // Must complete transfer
                if (!client.completePendingCommand()) {
                    return@withContext Result.failure(
                        IOException("FTP command failed after retrieving range")
                    )
                }
                
                // Return only bytes read (may be less than requested if EOF)
                if (totalRead < length) {
                    Result.success(buffer.copyOf(totalRead))
                } else {
                    Result.success(buffer)
                }
            } ?: Result.failure(IOException("Failed to open file stream: $remotePath"))
        } catch (e: IOException) {
            Timber.e(e, "FTP read bytes range failed: $remotePath offset=$offset length=$length")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP read bytes range error: $remotePath offset=$offset length=$length")
            Result.failure(e)
        }
    }

    /**
     * Download file from FTP server to OutputStream
     * @param remotePath Full path to remote file
     * @param outputStream OutputStream to write downloaded data
     * @param fileSize Size of the file to download (for progress tracking), 0 if unknown
     * @param progressCallback Optional callback for tracking download progress
     * @return Result with Unit on success or exception on failure
     */
    suspend fun downloadFile(
        remotePath: String,
        outputStream: OutputStream,
        fileSize: Long = 0L,
        @Suppress("UNUSED_PARAMETER") progressCallback: ByteProgressCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = ftpClient ?: return@withContext Result.failure(
                    IllegalStateException("Not connected. Call connect() first.")
                )
                
                Timber.d("FTP downloading: $remotePath (size=$fileSize bytes)")
                
                // Try passive mode first, fallback to active mode on timeout
                val success = try {
                    client.retrieveFile(remotePath, outputStream)
                } catch (e: SocketTimeoutException) {
                    Timber.w(e, "FTP passive mode timeout, switching to active mode for download")
                    
                    // Switch to active mode and retry
                    client.enterLocalActiveMode()
                    Timber.d("FTP retrying download in active mode: $remotePath")
                    
                    try {
                        client.retrieveFile(remotePath, outputStream)
                    } finally {
                        // Switch back to passive for future operations
                        try {
                            client.enterLocalPassiveMode()
                            Timber.d("FTP switched back to passive mode")
                        } catch (ignored: Exception) {
                            Timber.w(ignored, "Failed to switch back to passive mode")
                        }
                    }
                } catch (e: Exception) {
                    // Handle any other exception during retrieveFile (including NPE from internal FTPClient issues)
                    Timber.e(e, "FTP download error during retrieveFile: $remotePath")
                    return@withContext Result.failure(
                        IOException("FTP download failed: ${e.message}", e)
                    )
                }
                
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP download failed: ${client.replyString}")
                    )
                }
                
                Timber.i("FTP download success: $remotePath")
                Result.success(Unit)
            } catch (e: IOException) {
                Timber.e(e, "FTP download failed: $remotePath")
                Result.failure(e)
            } catch (e: Exception) {
                Timber.e(e, "FTP download error: $remotePath")
                Result.failure(e)
            }
        }
    }

    /**
     * Upload file to FTP server from InputStream
     * @param remotePath Full path where file should be uploaded
     * @param inputStream InputStream to read data from
     * @param fileSize Size of the file to upload (for progress tracking), 0 if unknown
     * @param progressCallback Optional callback for tracking upload progress
     * @return Result with Unit on success or exception on failure
     */
    suspend fun uploadFile(
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long = 0L,
        @Suppress("UNUSED_PARAMETER") progressCallback: ByteProgressCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            Timber.d("FTP uploading: $remotePath (size=$fileSize bytes)")
            
            val success = client.storeFile(remotePath, inputStream)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP upload failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP upload success: $remotePath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP upload failed: $remotePath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP upload error: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Delete file on FTP server
     * @param remotePath Full path to file to delete
     * @return Result with Unit on success or exception on failure
     */
    suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            Timber.d("FTP deleting: $remotePath")
            
            val success = client.deleteFile(remotePath)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP delete failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP delete success: $remotePath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP delete failed: $remotePath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP delete error: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Delete directory recursively on FTP server
     * @param remotePath Full path to directory to delete
     * @return Result with Unit on success or exception on failure
     */
    suspend fun deleteDirectory(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            Timber.d("FTP deleting directory: $remotePath")
            
            // List directory contents
            val files = client.listFiles(remotePath)
            
            // Delete all files and subdirectories recursively
            files.forEach { file ->
                val fullPath = "$remotePath/${file.name}"
                if (file.isDirectory) {
                    deleteDirectory(fullPath).getOrThrow()
                } else {
                    deleteFile(fullPath).getOrThrow()
                }
            }
            
            // Delete the directory itself
            val success = client.removeDirectory(remotePath)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP remove directory failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP delete directory success: $remotePath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP delete directory failed: $remotePath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP delete directory error: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Rename file on FTP server
     * @param oldPath Current file path
     * @param newName New filename (without path)
     * @return Result with Unit on success or exception on failure
     */
    suspend fun renameFile(oldPath: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            // Extract directory and construct new path
            val directory = oldPath.substringBeforeLast('/', "")
            val newPath = if (directory.isEmpty()) "/$newName" else "$directory/$newName"
            
            Timber.d("FTP renaming: $oldPath → $newPath")
            
            val success = client.rename(oldPath, newPath)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP rename failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP rename success: $newPath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP rename failed: $oldPath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP rename error: $oldPath")
            Result.failure(e)
        }
    }
    
    /**
     * Move/rename file with full paths (server-side operation)
     * @param oldPath Full path to source file
     * @param newPath Full path to destination file
     * @return Result with Unit or exception on failure
     */
    suspend fun moveFile(oldPath: String, newPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            Timber.d("FTP moving: $oldPath → $newPath")
            
            val success = client.rename(oldPath, newPath)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP move failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP move success: $newPath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP move failed: $oldPath → $newPath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP move error: $oldPath → $newPath")
            Result.failure(e)
        }
    }
    
    /**
     * Create directory on FTP server
     * @param remotePath Full path to directory to create
     * @return Result with Unit or exception on failure
     */
    suspend fun createDirectory(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = ftpClient ?: return@withContext Result.failure(
                IllegalStateException("Not connected. Call connect() first.")
            )
            
            Timber.d("FTP creating directory: $remotePath")
            val success = client.makeDirectory(remotePath)
            if (!success) {
                return@withContext Result.failure(
                    IOException("FTP create directory failed: ${client.replyString}")
                )
            }
            
            Timber.i("FTP directory created: $remotePath")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "FTP create directory failed: $remotePath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "FTP create directory error: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Disconnect from FTP server and cleanup resources
     * Uses short timeout for logout to avoid SocketTimeoutException delays
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            ftpClient?.let { client ->
                if (client.isConnected) {
                    // Set short timeout for logout - don't wait for server response
                    val originalTimeout = client.soTimeout
                    try {
                        client.soTimeout = 1000 // 1 second instead of default 30
                        client.logout()
                    } catch (e: java.net.SocketTimeoutException) {
                        // Ignore logout timeout - server will close connection anyway
                        Timber.d("FTP logout timeout (ignored)")
                    } catch (e: Exception) {
                        // Other logout errors also non-critical
                        Timber.d(e, "FTP logout error (ignored)")
                    } finally {
                        // Restore original timeout before disconnect
                        client.soTimeout = originalTimeout
                    }
                    // Always close socket regardless of logout success
                    client.disconnect()
                }
            }
            Timber.d("FTP disconnected")
        } catch (e: Exception) {
            Timber.w(e, "FTP disconnect error (non-critical)")
        } finally {
            ftpClient = null
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return ftpClient?.isConnected == true
    }

    /**
     * Upload file using a temporary FTP connection (for parallel uploads)
     * Creates a new connection, uploads the file, and closes immediately
     * 
     * @param host Server hostname or IP address
     * @param port Server port
     * @param username Username for authentication
     * @param password Password for authentication
     * @param remotePath Full path where file should be uploaded
     * @param inputStream InputStream to read data from
     * @param fileSize Size of the file to upload (for progress tracking), 0 if unknown
     * @param progressCallback Optional callback for tracking upload progress
     * @return Result with Unit on success or exception on failure
     */
    suspend fun uploadFileWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        inputStream: InputStream,
        fileSize: Long = 0L,
        @Suppress("UNUSED_PARAMETER") progressCallback: ByteProgressCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tempClient = FTPClient()
        try {
            // Set connection and socket timeout
            tempClient.connectTimeout = CONNECT_TIMEOUT
            tempClient.defaultTimeout = SOCKET_TIMEOUT
            tempClient.setDataTimeout(SOCKET_TIMEOUT)
            tempClient.controlKeepAliveTimeout = Duration.ofSeconds(KEEPALIVE_TIMEOUT).seconds
            
            tempClient.connect(host, port)
            
            val replyCode = tempClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP server refused connection. Reply code: $replyCode")
                )
            }
            
            if (!tempClient.login(username, password)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP authentication failed for user: $username")
                )
            }
            
            // Enable passive mode and binary transfer
            tempClient.enterLocalPassiveMode()
            tempClient.setFileType(FTP.BINARY_FILE_TYPE)
            
            Timber.d("FTP temp connection: uploading $remotePath")
            
            val success = tempClient.storeFile(remotePath, inputStream)
            
            if (success) {
                Timber.i("FTP temp connection upload success: $remotePath")
                Result.success(Unit)
            } else {
                val message = "FTP storeFile returned false: $remotePath"
                Timber.e(message)
                Result.failure(IOException(message))
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP temp connection upload failed: $remotePath")
            Result.failure(e)
        } finally {
            try {
                if (tempClient.isConnected) {
                    tempClient.soTimeout = 1000
                    try {
                        tempClient.logout()
                    } catch (e: Exception) {
                        Timber.d(e, "FTP temp logout error (ignored)")
                    }
                    tempClient.disconnect()
                }
            } catch (e: Exception) {
                Timber.w(e, "FTP temp disconnect error (ignored)")
            }
        }
    }

    /**
     * Delete file using a temporary FTP connection
     */
    suspend fun deleteFileWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        executeWithNewConnection(host, port, username, password) { client ->
            Timber.d("FTP temp connection: deleting $remotePath")
            val success = client.deleteFile(remotePath)
            if (success) {
                Timber.i("FTP temp connection delete success: $remotePath")
                Result.success(Unit)
            } else {
                Result.failure(IOException("FTP delete failed: ${client.replyString}"))
            }
        }
    }

    /**
     * Rename file using a temporary FTP connection
     */
    suspend fun renameFileWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        oldPath: String,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        executeWithNewConnection(host, port, username, password) { client ->
            val directory = oldPath.substringBeforeLast('/', "")
            val newPath = if (directory.isEmpty()) "/$newName" else "$directory/$newName"
            
            Timber.d("FTP temp connection: renaming $oldPath → $newPath")
            val success = client.rename(oldPath, newPath)
            if (success) {
                Timber.i("FTP temp connection rename success: $newPath")
                Result.success(Unit)
            } else {
                Result.failure(IOException("FTP rename failed: ${client.replyString}"))
            }
        }
    }

    /**
     * Create directory using a temporary FTP connection
     */
    suspend fun createDirectoryWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        executeWithNewConnection(host, port, username, password) { client ->
            Timber.d("FTP temp connection: creating directory $remotePath")
            val success = client.makeDirectory(remotePath)
            if (success) {
                Timber.i("FTP temp connection create directory success: $remotePath")
                Result.success(Unit)
            } else {
                Result.failure(IOException("FTP create directory failed: ${client.replyString}"))
            }
        }
    }

    /**
     * Check if file exists using a temporary FTP connection
     */
    suspend fun existsWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        executeWithNewConnection(host, port, username, password) { client ->
            try {
                val files = client.listFiles(remotePath)
                Result.success(files != null && files.isNotEmpty())
            } catch (e: Exception) {
                Timber.w(e, "FTP exists check failed for $remotePath")
                Result.success(false)
            }
        }
    }

    /**
     * Helper to execute a block with a new FTP connection
     */
    private suspend fun <T> executeWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        block: (FTPClient) -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        val tempClient = FTPClient()
        try {
            tempClient.connectTimeout = CONNECT_TIMEOUT
            tempClient.defaultTimeout = SOCKET_TIMEOUT
            tempClient.setDataTimeout(SOCKET_TIMEOUT)
            tempClient.controlKeepAliveTimeout = Duration.ofSeconds(KEEPALIVE_TIMEOUT).seconds
            
            tempClient.connect(host, port)
            
            val replyCode = tempClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP server refused connection. Reply code: $replyCode")
                )
            }
            
            if (!tempClient.login(username, password)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP authentication failed for user: $username")
                )
            }
            
            tempClient.enterLocalPassiveMode()
            tempClient.setFileType(FTP.BINARY_FILE_TYPE)
            
            block(tempClient)
        } catch (e: Exception) {
            Timber.e(e, "FTP temp connection operation failed")
            Result.failure(e)
        } finally {
            try {
                if (tempClient.isConnected) {
                    tempClient.soTimeout = 1000
                    try { tempClient.logout() } catch (ignored: Exception) {}
                    tempClient.disconnect()
                }
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Download file with temporary connection (helper for backward compatibility)
     */
    suspend fun downloadFileWithNewConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String,
        outputStream: OutputStream,
        fileSize: Long = 0L,
        progressCallback: ByteProgressCallback? = null
    ): Result<Unit> {
        return executeWithNewConnection(host, port, username, password) { tempClient ->
            try {
                val success = tempClient.retrieveFile(remotePath, outputStream)
                if (!success) {
                    Result.failure(IOException("FTP download failed: ${tempClient.replyString}"))
                } else {
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Open InputStream for reading file from FTP.
     * Creates a NEW connection for the stream to ensure thread safety.
     * Caller is responsible for closing the stream.
     */
    suspend fun openInputStream(
        host: String,
        port: Int,
        username: String,
        password: String,
        remotePath: String
    ): Result<InputStream> = withContext(Dispatchers.IO) {
        val tempClient = FTPClient()
        try {
            tempClient.connectTimeout = CONNECT_TIMEOUT
            tempClient.defaultTimeout = SOCKET_TIMEOUT
            tempClient.setDataTimeout(SOCKET_TIMEOUT)
            tempClient.controlKeepAliveTimeout = Duration.ofSeconds(KEEPALIVE_TIMEOUT).seconds
            
            tempClient.connect(host, port)
            
            val replyCode = tempClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP server refused connection. Reply code: $replyCode")
                )
            }
            
            if (!tempClient.login(username, password)) {
                tempClient.disconnect()
                return@withContext Result.failure(
                    IOException("FTP authentication failed for user: $username")
                )
            }
            
            tempClient.enterLocalPassiveMode()
            tempClient.setFileType(FTP.BINARY_FILE_TYPE)
            
            val stream = tempClient.retrieveFileStream(remotePath)
            if (stream == null) {
                tempClient.disconnect()
                return@withContext Result.failure(IOException("Failed to open FTP stream: $remotePath"))
            }
            
            // Return wrapper that closes connection
            val wrapper = object : java.io.FilterInputStream(stream) {
                override fun close() {
                    try {
                        super.close()
                        if (!tempClient.completePendingCommand()) {
                            Timber.w("FTP completePendingCommand failed after stream close")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error closing FTP stream")
                    } finally {
                        try {
                            if (tempClient.isConnected) {
                                tempClient.logout()
                                tempClient.disconnect()
                            }
                        } catch (e: Exception) {
                            Timber.w("Error disconnecting FTP client")
                        }
                    }
                }
            }
            
            Result.success(wrapper)
        } catch (e: Exception) {
            try {
                if (tempClient.isConnected) tempClient.disconnect()
            } catch (ignored: Exception) {}
            Timber.e(e, "FTP openInputStream failed: $remotePath")
            Result.failure(e)
        }
    }
}
