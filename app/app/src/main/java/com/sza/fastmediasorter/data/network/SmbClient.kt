package com.sza.fastmediasorter.data.network

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMB/CIFS client for network file operations using SMBJ library.
 * Provides connection management, authentication, file listing, and data transfer.
 * 
 * Supports SMB2/SMB3 protocols with connection pooling.
 */
@Singleton
class SmbClient @Inject constructor() {
    
    companion object {
        private const val CONNECTION_TIMEOUT_MS = 5000L
        private const val READ_TIMEOUT_MS = 60000L
        private const val WRITE_TIMEOUT_MS = 60000L
    }
    
    private val config by lazy {
        SmbConfig.builder()
            .withTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .withSoTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .withMultiProtocolNegotiate(true)
            .build()
    }

    @Volatile
    private var client: SMBClient? = null
    
    private fun getClient(): SMBClient {
        return client ?: synchronized(this) {
            client ?: SMBClient(config).also { client = it }
        }
    }

    /**
     * Test connection to SMB server.
     * 
     * @param server Server hostname or IP
     * @param port Server port (default 445)
     * @param shareName Share name to test
     * @param username Username for authentication
     * @param password Password for authentication
     * @param domain Domain (optional)
     * @return Result with success message or error
     */
    suspend fun testConnection(
        server: String,
        port: Int = 445,
        shareName: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<String> {
        return try {
            Timber.d("SMB testConnection to $server:$port/$shareName (user: $username)")
            
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            
            val share = session.connectShare(shareName) as? DiskShare
                ?: return Result.failure(IOException("Failed to connect to share: $shareName"))
            
            // Test listing root directory
            val files = share.list("")
            
            share.close()
            session.close()
            connection.close()
            
            Result.success("Connection successful. Found ${files.size} items in root.")
        } catch (e: Exception) {
            Timber.e(e, "SMB connection failed")
            Result.failure(e)
        }
    }

    /**
     * List files in a directory.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param path Path within share (empty for root)
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with list of file names
     */
    suspend fun listFiles(
        server: String,
        port: Int,
        shareName: String,
        path: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<List<String>> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = path.trimStart('/')
            val fileInfoList = share.list(cleanPath)
            val fileNames = fileInfoList
                .filter { !it.fileName.startsWith(".") && it.fileName != "." && it.fileName != ".." }
                .map { it.fileName }
            
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB listed ${fileNames.size} files in $cleanPath")
            Result.success(fileNames)
        } catch (e: Exception) {
            Timber.e(e, "SMB listFiles failed: $path")
            Result.failure(e)
        }
    }

    /**
     * Download a file.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote file path
     * @param outputStream Output stream to write to
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with bytes transferred
     */
    suspend fun downloadFile(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        outputStream: OutputStream,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Long> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            val file: File = share.openFile(
                cleanPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            
            val inputStream: InputStream = file.inputStream
            val bytesTransferred = inputStream.copyTo(outputStream)
            
            inputStream.close()
            file.close()
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB downloaded $bytesTransferred bytes from $cleanPath")
            Result.success(bytesTransferred)
        } catch (e: Exception) {
            Timber.e(e, "SMB download failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Get file size.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with file size in bytes
     */
    suspend fun getFileSize(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Long> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            val fileInfo = share.getFileInformation(cleanPath)
            val size = fileInfo.standardInformation.endOfFile
            
            share.close()
            session.close()
            connection.close()
            
            Result.success(size)
        } catch (e: Exception) {
            Timber.e(e, "SMB getFileSize failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Delete a file.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with success or error
     */
    suspend fun deleteFile(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Unit> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            share.rm(cleanPath)
            
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB deleted file: $cleanPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SMB delete failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Rename/move a file.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param fromPath Source file path
     * @param toPath Destination file path
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with success or error
     */
    suspend fun rename(
        server: String,
        port: Int,
        shareName: String,
        fromPath: String,
        toPath: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Unit> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanFromPath = fromPath.trimStart('/')
            val cleanToPath = toPath.trimStart('/')
            
            // Open file for rename
            val file = share.openFile(
                cleanFromPath,
                EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            
            // SMB rename using file renaming
            file.rename(cleanToPath)
            
            file.close()
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB renamed $cleanFromPath to $cleanToPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SMB rename failed: $fromPath -> $toPath")
            Result.failure(e)
        }
    }

    /**
     * Create a directory.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote directory path
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with success or error
     */
    suspend fun createDirectory(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Unit> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            share.mkdir(cleanPath)
            
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB created directory: $cleanPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SMB mkdir failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Upload a file.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote file path
     * @param inputStream Input stream to read from
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with success or error
     */
    suspend fun uploadFile(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        inputStream: InputStream,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Unit> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            val file: File = share.openFile(
                cleanPath,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            )
            
            val outputStream: OutputStream = file.outputStream
            inputStream.copyTo(outputStream)
            
            outputStream.close()
            file.close()
            share.close()
            session.close()
            connection.close()
            
            Timber.d("SMB uploaded file to $cleanPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SMB upload failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Check if a file or directory exists.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @return Result with true if exists, false otherwise
     */
    suspend fun exists(
        server: String,
        port: Int,
        shareName: String,
        remotePath: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<Boolean> {
        return try {
            val connection = getClient().connect(server, port)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            
            val cleanPath = remotePath.trimStart('/')
            val exists = share.fileExists(cleanPath)
            
            share.close()
            session.close()
            connection.close()
            
            Result.success(exists)
        } catch (e: Exception) {
            Timber.e(e, "SMB exists check failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Release resources.
     */
    fun release() {
        try {
            client?.close()
            client = null
        } catch (e: Exception) {
            Timber.e(e, "Error releasing SMB client")
        }
    }
}
