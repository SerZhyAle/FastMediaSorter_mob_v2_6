package com.sza.fastmediasorter.data.transfer.strategy

import android.content.Context
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
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
 * Strategy for SFTP (SSH File Transfer Protocol) file operations.
 * Handles sftp:// protocol operations using SftpClient.
 */
class SftpOperationStrategy(
    private val context: Context,
    private val sftpClient: SftpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : FileOperationStrategy {
    
    override suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isSourceSftp = source.startsWith("sftp://")
            val isDestSftp = destination.startsWith("sftp://")
            
            when {
                isSourceSftp && isDestSftp -> {
                    // SFTP to SFTP: buffer transfer
                    copySftpToSftp(source, destination, overwrite, progressCallback)
                }
                isSourceSftp && !isDestSftp -> {
                    // SFTP to Local: download
                    downloadFromSftp(source, destination, progressCallback)
                }
                !isSourceSftp && isDestSftp -> {
                    // Local to SFTP: upload
                    uploadToSftp(source, destination, overwrite, progressCallback)
                }
                else -> {
                    Result.failure(IllegalArgumentException("At least one path must be SFTP"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SftpOperationStrategy: Copy failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(source: String, destination: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isSourceSftp = source.startsWith("sftp://")
            val isDestSftp = destination.startsWith("sftp://")
            
            when {
                isSourceSftp && isDestSftp -> {
                    // Try server-side move if on same server
                    val sourceInfo = parseSftpPath(source)
                    val destInfo = parseSftpPath(destination)
                    
                    if (sourceInfo != null && destInfo != null &&
                        sourceInfo.host == destInfo.host &&
                        sourceInfo.port == destInfo.port &&
                        sourceInfo.username == destInfo.username
                    ) {
                        // Server-side move (rename)
                        val connectionInfo = getConnectionInfo(sourceInfo)
                        val fromPath = sourceInfo.remotePath
                        val toPath = destInfo.remotePath
                        
                        sftpClient.rename(connectionInfo, fromPath, toPath)
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
            Timber.e(e, "SftpOperationStrategy: Move failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!path.startsWith("sftp://")) {
                return@withContext Result.failure(IllegalArgumentException("Path must be SFTP"))
            }
            
            val pathInfo = parseSftpPath(path)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid SFTP path"))
            
            val connectionInfo = getConnectionInfo(pathInfo)
            sftpClient.deleteFile(connectionInfo, pathInfo.remotePath)
        } catch (e: Exception) {
            Timber.e(e, "SftpOperationStrategy: Delete failed - $path")
            Result.failure(e)
        }
    }
    
    override suspend fun exists(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!path.startsWith("sftp://")) return@withContext Result.success(false)
            
            val pathInfo = parseSftpPath(path) ?: return@withContext Result.success(false)
            val connectionInfo = getConnectionInfo(pathInfo)
            
            sftpClient.exists(connectionInfo, pathInfo.remotePath)
        } catch (e: Exception) {
            Timber.e(e, "SftpOperationStrategy: exists check failed - $path")
            Result.failure(e)
        }
    }
    
    override fun supportsProtocol(path: String): Boolean {
        return path.startsWith("sftp://", ignoreCase = true)
    }
    
    override fun getProtocolName(): String = "SFTP"
    
    // Private helper methods
    
    private data class SftpPathInfo(
        val host: String,
        val port: Int,
        val username: String,
        val remotePath: String
    )
    
    private fun parseSftpPath(path: String): SftpPathInfo? {
        try {
            if (!path.startsWith("sftp://")) return null
            
            val withoutProtocol = path.substringAfter("sftp://")
            val userHostPart = withoutProtocol.substringBefore("/")
            val remotePath = "/" + withoutProtocol.substringAfter("/", "")

            // Supported formats:
            // - sftp://host:port/path
            // - sftp://username@host:port/path
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
                port = hostPortPart.substringAfter(":").toIntOrNull() ?: 22
            } else {
                host = hostPortPart
                port = 22
            }

            val username = usernameFromUrl ?: ""

            return SftpPathInfo(host = host, port = port, username = username, remotePath = remotePath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse SFTP path: $path")
            return null
        }
    }
    
    private suspend fun getConnectionInfo(pathInfo: SftpPathInfo): SftpClient.SftpConnectionInfo {
        val credentials = credentialsRepository.getByTypeServerAndPort("SFTP", pathInfo.host, pathInfo.port)
            ?: credentialsRepository.getCredentialsByHost(pathInfo.host)

        if (credentials == null) {
            throw IllegalStateException("No credentials found for ${pathInfo.host}:${pathInfo.port}")
        }

        val usernameToUse = pathInfo.username.takeIf { it.isNotBlank() } ?: credentials.username
        return SftpClient.SftpConnectionInfo(
            host = pathInfo.host,
            port = pathInfo.port,
            username = usernameToUse,
            password = credentials.password,
            privateKey = credentials.decryptedSshPrivateKey,
            passphrase = null // TODO: Add passphrase support to NetworkCredentialsEntity
        )
    }
    
    private suspend fun copySftpToSftp(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val sourceInfo = parseSftpPath(source)
                ?: return Result.failure(IllegalArgumentException("Invalid source path"))
            val destInfo = parseSftpPath(destination)
                ?: return Result.failure(IllegalArgumentException("Invalid destination path"))
            
            // Check if destination exists
            if (!overwrite) {
                val destConnectionInfo = getConnectionInfo(destInfo)
                val existsResult = sftpClient.exists(destConnectionInfo, destInfo.remotePath)
                if (existsResult.isSuccess && existsResult.getOrNull() == true) {
                    return Result.failure(Exception("Destination file already exists"))
                }
            }
            
            // Get source file size
            val sourceConnectionInfo = getConnectionInfo(sourceInfo)
            val statResult = sftpClient.stat(sourceConnectionInfo, sourceInfo.remotePath)
            if (statResult.isFailure) {
                return Result.failure(statResult.exceptionOrNull() ?: Exception("Failed to get source file size"))
            }
            val fileSize = statResult.getOrNull()?.size ?: 0L
            
            // Download to buffer
            val buffer = ByteArrayOutputStream()
            val downloadResult = sftpClient.downloadFile(
                sourceConnectionInfo,
                sourceInfo.remotePath,
                buffer,
                fileSize,
                progressCallback
            )
            
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            // Upload from buffer
            val destConnectionInfo = getConnectionInfo(destInfo)
            val uploadResult = sftpClient.uploadFile(
                destConnectionInfo,
                destInfo.remotePath,
                buffer.toByteArray()
            )
            
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
            
            return Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "SFTP to SFTP copy failed: $source -> $destination")
            return Result.failure(e)
        }
    }
    
    private suspend fun downloadFromSftp(
        source: String,
        destination: String,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val sourceInfo = parseSftpPath(source)
                ?: return Result.failure(IllegalArgumentException("Invalid source path"))
            
            val destFile = File(destination)
            destFile.parentFile?.mkdirs()
            
            // Get file size for progress
            val connectionInfo = getConnectionInfo(sourceInfo)
            val statResult = sftpClient.stat(connectionInfo, sourceInfo.remotePath)
            val fileSize = statResult.getOrNull()?.size ?: 0L
            
            FileOutputStream(destFile).use { outputStream ->
                val downloadResult = sftpClient.downloadFile(
                    connectionInfo,
                    sourceInfo.remotePath,
                    outputStream,
                    fileSize,
                    progressCallback
                )
                
                if (downloadResult.isFailure) {
                    return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
                }
            }
            
            return Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "SFTP to Local download failed: $source -> $destination")
            return Result.failure(e)
        }
    }
    
    private suspend fun uploadToSftp(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        try {
            val destInfo = parseSftpPath(destination)
                ?: return Result.failure(IllegalArgumentException("Invalid destination path"))
            
            // Check if destination exists
            if (!overwrite) {
                val connectionInfo = getConnectionInfo(destInfo)
                val existsResult = sftpClient.exists(connectionInfo, destInfo.remotePath)
                if (existsResult.isSuccess && existsResult.getOrNull() == true) {
                    return Result.failure(Exception("Destination file already exists"))
                }
            }
            
            val sourceFile = File(source)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file not found"))
            }
            
            val connectionInfo = getConnectionInfo(destInfo)
            FileInputStream(sourceFile).use { inputStream ->
                val uploadResult = sftpClient.uploadFile(
                    connectionInfo,
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
            Timber.e(e, "Local to SFTP upload failed: $source -> $destination")
            return Result.failure(e)
        }
    }
}
