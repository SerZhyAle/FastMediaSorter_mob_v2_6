package com.sza.fastmediasorter.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FTP client wrapper using Apache Commons Net.
 * Handles FTP connection, authentication and file operations with passive mode.
 * 
 * Thread-safe with synchronized access to FTPClient instance.
 */
@Singleton
class FtpClient @Inject constructor() {

    private var ftpClient: FTPClient? = null
    private val mutex = Any()

    companion object {
        private const val CONNECT_TIMEOUT = 10000 // 10 seconds
        private const val SOCKET_TIMEOUT = 30000 // 30 seconds
    }

    /**
     * Test connection to FTP server.
     * 
     * @param host Server hostname or IP
     * @param port Server port (default 21)
     * @param username Username for authentication
     * @param password Password for authentication
     * @return Result with success message or error
     */
    suspend fun testConnection(
        host: String,
        port: Int = 21,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("FTP testConnection to $host:$port (user: $username)")
            
            val client = FTPClient()
            client.connectTimeout = CONNECT_TIMEOUT
            client.defaultTimeout = SOCKET_TIMEOUT
            client.setDataTimeout(SOCKET_TIMEOUT)
            
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
            
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            
            val files = client.listFiles("/")
            
            client.logout()
            client.disconnect()
            
            Result.success("Connection successful. Found ${files.size} items in root.")
        } catch (e: Exception) {
            Timber.e(e, "FTP connection failed")
            Result.failure(e)
        }
    }

    /**
     * List files in a directory.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote directory path
     * @param username Username
     * @param password Password
     * @return Result with list of file names
     */
    suspend fun listFiles(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val ftpFiles = client.listFiles(remotePath)
                val fileNames = ftpFiles
                    .filter { !it.name.startsWith(".") && it.name != "." && it.name != ".." && it.isFile }
                    .map { it.name }
                
                Timber.d("FTP listed ${fileNames.size} files in $remotePath")
                Result.success(fileNames)
            } catch (e: Exception) {
                Timber.e(e, "FTP listFiles failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Download a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param outputStream Output stream to write to
     * @param username Username
     * @param password Password
     * @return Result with bytes transferred
     */
    suspend fun downloadFile(
        host: String,
        port: Int,
        remotePath: String,
        outputStream: OutputStream,
        username: String,
        password: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val success = client.retrieveFile(remotePath, outputStream)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP download failed: ${client.replyString}")
                    )
                }
                
                // FTP doesn't provide bytes transferred, return 0
                Timber.d("FTP downloaded file from $remotePath")
                Result.success(0L)
            } catch (e: Exception) {
                Timber.e(e, "FTP download failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Get file size.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @return Result with file size in bytes
     */
    suspend fun getFileSize(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val ftpFile = client.mlistFile(remotePath)
                if (ftpFile == null) {
                    return@withContext Result.failure(
                        IOException("File not found: $remotePath")
                    )
                }
                
                Result.success(ftpFile.size)
            } catch (e: Exception) {
                Timber.e(e, "FTP getFileSize failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @return Result with success message or error
     */
    suspend fun deleteFile(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val success = client.deleteFile(remotePath)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP delete failed: ${client.replyString}")
                    )
                }
                
                Timber.d("FTP deleted file: $remotePath")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "FTP delete failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Rename a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param fromPath Source file path
     * @param toPath Destination file path
     * @param username Username
     * @param password Password
     * @return Result with success or error
     */
    suspend fun rename(
        host: String,
        port: Int,
        fromPath: String,
        toPath: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val success = client.rename(fromPath, toPath)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP rename failed: ${client.replyString}")
                    )
                }
                
                Timber.d("FTP renamed $fromPath to $toPath")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "FTP rename failed: $fromPath -> $toPath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Create a directory.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote directory path
     * @param username Username
     * @param password Password
     * @return Result with success or error
     */
    suspend fun createDirectory(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val success = client.makeDirectory(remotePath)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP mkdir failed: ${client.replyString}")
                    )
                }
                
                Timber.d("FTP created directory: $remotePath")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "FTP mkdir failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Upload a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param inputStream Input stream to read from
     * @param username Username
     * @param password Password
     * @return Result with success or error
     */
    suspend fun uploadFile(
        host: String,
        port: Int,
        remotePath: String,
        inputStream: java.io.InputStream,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val success = client.storeFile(remotePath, inputStream)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("FTP upload failed: ${client.replyString}")
                    )
                }
                
                Timber.d("FTP uploaded file to $remotePath")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "FTP upload failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    /**
     * Check if a file exists.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @return Result with true if exists, false otherwise
     */
    suspend fun exists(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        synchronized(mutex) {
            try {
                val client = getOrCreateClient(host, port, username, password)
                    ?: return@withContext Result.failure(IOException("Failed to connect"))
                
                val ftpFile = client.mlistFile(remotePath)
                Result.success(ftpFile != null)
            } catch (e: Exception) {
                Timber.e(e, "FTP exists check failed: $remotePath")
                disconnect()
                Result.failure(e)
            }
        }
    }

    private fun getOrCreateClient(
        host: String,
        port: Int,
        username: String,
        password: String
    ): FTPClient? {
        val existing = ftpClient
        if (existing != null && existing.isConnected) {
            return existing
        }
        
        disconnect()
        
        return try {
            val client = FTPClient()
            client.connectTimeout = CONNECT_TIMEOUT
            client.defaultTimeout = SOCKET_TIMEOUT
            client.setDataTimeout(SOCKET_TIMEOUT)
            
            client.connect(host, port)
            
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                client.disconnect()
                return null
            }
            
            if (!client.login(username, password)) {
                client.disconnect()
                return null
            }
            
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            
            ftpClient = client
            Timber.d("FTP connected to $host:$port")
            client
        } catch (e: Exception) {
            Timber.e(e, "FTP connection failed")
            null
        }
    }

    private fun disconnect() {
        try {
            ftpClient?.let {
                if (it.isConnected) {
                    it.logout()
                    it.disconnect()
                }
            }
            ftpClient = null
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting FTP")
        }
    }

    /**
     * Release resources.
     */
    fun release() {
        disconnect()
    }
}
