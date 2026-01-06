package com.sza.fastmediasorter.data.transfer.strategy

import android.content.Context
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Strategy for FTP (File Transfer Protocol) file operations.
 * Handles ftp:// protocol operations using FtpClient.
 * Note: FTP connections are stateful - each instance maintains a persistent connection.
 */
class FtpOperationStrategy(
    private val context: Context,
    private val ftpClient: FtpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : FileOperationStrategy {
    
    override suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isSourceFtp = source.startsWith("ftp://")
            val isDestFtp = destination.startsWith("ftp://")
            
            when {
                isSourceFtp && isDestFtp -> {
                    // FTP to FTP: buffer transfer
                    copyFtpToFtp(source, destination, overwrite, progressCallback)
                }
                isSourceFtp && !isDestFtp -> {
                    // FTP to Local: download
                    downloadFromFtp(source, destination, progressCallback)
                }
                !isSourceFtp && isDestFtp -> {
                    // Local to FTP: upload
                    uploadToFtp(source, destination, overwrite, progressCallback)
                }
                else -> {
                    Result.failure(IllegalArgumentException("At least one path must be FTP"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FtpOperationStrategy: Copy failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(source: String, destination: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isSourceFtp = source.startsWith("ftp://")
            val isDestFtp = destination.startsWith("ftp://")
            
            when {
                isSourceFtp && isDestFtp -> {
                    // Try server-side move if on same server
                    val sourceInfo = parseFtpPath(source)
                    val destInfo = parseFtpPath(destination)
                    
                    if (sourceInfo != null && destInfo != null &&
                        sourceInfo.host == destInfo.host &&
                        sourceInfo.port == destInfo.port &&
                        sourceInfo.username == destInfo.username
                    ) {
                        // Server-side move (rename)
                        ensureConnected(sourceInfo)
                        val fromPath = sourceInfo.remotePath
                        val toPath = destInfo.remotePath
                        
                        ftpClient.moveFile(fromPath, toPath)
                    } else {
                        // Different servers: copy + delete
                        val copyResult = copyFile(source, destination, overwrite = true, progressCallback = null)
                        if (copyResult.isFailure) {
                            return@withContext Result.failure(copyResult.exceptionOrNull() ?: Exception("Copy failed"))
                        }
                        deleteFile(source)
                    }
                }
                else -> {
                    // Cross-protocol move: copy + delete
                    val copyResult = copyFile(source, destination, overwrite = true, progressCallback = null)
                    if (copyResult.isFailure) {
                        return@withContext Result.failure(copyResult.exceptionOrNull() ?: Exception("Copy failed"))
                    }
                    deleteFile(source)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FtpOperationStrategy: Move failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!path.startsWith("ftp://")) {
                return@withContext Result.failure(IllegalArgumentException("Path must be FTP"))
            }
            
            val pathInfo = parseFtpPath(path)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid FTP path"))
            
            ensureConnected(pathInfo)
            ftpClient.deleteFile(pathInfo.remotePath)
        } catch (e: Exception) {
            Timber.e(e, "FtpOperationStrategy: Delete failed - $path")
            Result.failure(e)
        }
    }
    
    override suspend fun exists(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!path.startsWith("ftp://")) return@withContext Result.success(false)
            
            val pathInfo = parseFtpPath(path) ?: return@withContext Result.success(false)
            ensureConnected(pathInfo)
            
            // FTP doesn't have a direct exists() method, try stat instead
            val files = ftpClient.listFilesWithMetadata(pathInfo.remotePath, recursive = false)
            Result.success(files.isSuccess && files.getOrNull()?.isNotEmpty() == true)
        } catch (e: Exception) {
            Timber.e(e, "FtpOperationStrategy: exists check failed - $path")
            Result.failure(e)
        }
    }
    
    override fun supportsProtocol(path: String): Boolean {
        return path.startsWith("ftp://", ignoreCase = true)
    }
    
    override fun getProtocolName(): String = "FTP"
    
    // Private helper methods
    
    private data class FtpPathInfo(
        val host: String,
        val port: Int,
        val username: String,
        val remotePath: String
    )
    
    private fun parseFtpPath(path: String): FtpPathInfo? {
        try {
            if (!path.startsWith("ftp://")) return null
            
            val withoutProtocol = path.substringAfter("ftp://")
            val userHostPart = withoutProtocol.substringBefore("/")
            val remotePath = "/" + withoutProtocol.substringAfter("/", "")

            // Supported formats:
            // - ftp://host:port/path
            // - ftp://username@host:port/path
            val usernameFromUrl = userHostPart.substringBefore("@", missingDelimiterValue = "")
                .takeIf { userHostPart.contains("@") }
                ?.trim()

            val hostPortPart = if (userHostPart.contains("@")) {
                userHostPart.substringAfter("@")
            } else {
                userHostPart
            }
            
            val host: String
            val port: Int
            if (hostPortPart.contains(":")) {
                host = hostPortPart.substringBefore(":")
                port = hostPortPart.substringAfter(":").toIntOrNull() ?: 21
            } else {
                host = hostPortPart
                port = 21
            }

            val username = usernameFromUrl ?: ""

            return FtpPathInfo(host = host, port = port, username = username, remotePath = remotePath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse FTP path: $path")
            return null
        }
    }
    
    private suspend fun ensureConnected(pathInfo: FtpPathInfo) {
        // FTP client maintains connection, but we should ensure it's connected
        // In practice, the handler will connect before using the strategy
        // This is a safety check
        val credentials = credentialsRepository.getByTypeServerAndPort("FTP", pathInfo.host, pathInfo.port)
            ?: credentialsRepository.getCredentialsByHost(pathInfo.host)
        if (credentials == null) {
            throw IllegalStateException("No credentials found for ${pathInfo.host}:${pathInfo.port}")
        }

        val usernameToUse = pathInfo.username.takeIf { it.isNotBlank() } ?: credentials.username
        
        // Connect if needed (FtpClient handles reconnection internally)
        val result = ftpClient.connect(
            host = pathInfo.host,
            port = pathInfo.port,
            username = usernameToUse,
            password = credentials.password
        )
        
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("Connection failed")
        }
    }
    
    private suspend fun copyFtpToFtp(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val sourceInfo = parseFtpPath(source)
                ?: return Result.failure(IllegalArgumentException("Invalid source path"))
            val destInfo = parseFtpPath(destination)
                ?: return Result.failure(IllegalArgumentException("Invalid destination path"))
            
            // Check if destination exists
            if (!overwrite) {
                val existsResult = exists(destination)
                if (existsResult.isSuccess && existsResult.getOrNull() == true) {
                    return Result.failure(Exception("Destination file already exists"))
                }
            }
            
            // Download to buffer
            ensureConnected(sourceInfo)
            val buffer = ByteArrayOutputStream()
            val downloadResult = ftpClient.downloadFile(
                sourceInfo.remotePath,
                buffer,
                fileSize = 0L, // FTP doesn't provide file size easily
                progressCallback = progressCallback
            )
            
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            // Upload from buffer (may need to reconnect if different server)
            if (sourceInfo.host != destInfo.host || sourceInfo.port != destInfo.port) {
                ensureConnected(destInfo)
            }
            
            val uploadResult = ftpClient.uploadFile(
                destInfo.remotePath,
                ByteArrayInputStream(buffer.toByteArray()),
                fileSize = buffer.size().toLong(),
                progressCallback = null
            )
            
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
            
            return Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "FTP to FTP copy failed: $source -> $destination")
            return Result.failure(e)
        }
    }
    
    private suspend fun downloadFromFtp(
        source: String,
        destination: String,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val sourceInfo = parseFtpPath(source)
                ?: return Result.failure(IllegalArgumentException("Invalid source path"))
            
            val destFile = File(destination)
            destFile.parentFile?.mkdirs()
            
            ensureConnected(sourceInfo)
            
            FileOutputStream(destFile).use { outputStream ->
                val downloadResult = ftpClient.downloadFile(
                    sourceInfo.remotePath,
                    outputStream,
                    fileSize = 0L,
                    progressCallback = progressCallback
                )
                
                if (downloadResult.isFailure) {
                    return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
                }
            }
            
            return Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "FTP to Local download failed: $source -> $destination")
            return Result.failure(e)
        }
    }
    
    private suspend fun uploadToFtp(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val destInfo = parseFtpPath(destination)
                ?: return Result.failure(IllegalArgumentException("Invalid destination path"))
            
            // Check if destination exists
            if (!overwrite) {
                val existsResult = exists(destination)
                if (existsResult.isSuccess && existsResult.getOrNull() == true) {
                    return Result.failure(Exception("Destination file already exists"))
                }
            }
            
            val sourceFile = File(source)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file not found"))
            }
            
            ensureConnected(destInfo)
            
            FileInputStream(sourceFile).use { inputStream ->
                val uploadResult = ftpClient.uploadFile(
                    destInfo.remotePath,
                    inputStream,
                    sourceFile.length(),
                    progressCallback
                )
                
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
                }
            }
            
            return Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "Local to FTP upload failed: $source -> $destination")
            return Result.failure(e)
        }
    }
}
