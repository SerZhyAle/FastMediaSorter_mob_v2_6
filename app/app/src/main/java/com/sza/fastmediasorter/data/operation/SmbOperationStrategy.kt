package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.sza.fastmediasorter.data.network.smb.SmbClient
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FileOperationStrategy for SMB/CIFS network shares.
 * Handles file operations on Windows network shares and NAS devices.
 * 
 * Note: This strategy handles operations between local and SMB, or SMB to SMB.
 * For local-only operations, use LocalOperationStrategy.
 */
@Singleton
class SmbOperationStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbClient: SmbClient
) : FileOperationStrategy {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val SMB_SCHEME = "smb://"
    }

    // Connection info for current SMB session
    // In a real app, this would come from saved credentials or user input
    private var connectionInfo: SmbClient.SmbConnectionInfo? = null

    /**
     * Configure SMB connection settings.
     * Must be called before using SMB operations.
     */
    fun configure(
        host: String,
        share: String,
        username: String,
        password: String,
        domain: String = ""
    ) {
        connectionInfo = SmbClient.SmbConnectionInfo(
            host = host,
            share = share,
            username = username,
            password = password,
            domain = domain
        )
    }

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "SMB not configured. Call configure() first.",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            val isSourceRemote = isRemotePath(source.path)
            val isDestRemote = isRemotePath(destinationPath)

            when {
                isSourceRemote && !isDestRemote -> {
                    // Download: SMB -> Local
                    downloadFile(info, source.path, destinationPath, onProgress)
                }
                !isSourceRemote && isDestRemote -> {
                    // Upload: Local -> SMB
                    uploadFile(info, source.path, destinationPath, onProgress)
                }
                isSourceRemote && isDestRemote -> {
                    // SMB -> SMB: Download then upload (or use remote copy if same server)
                    copyRemoteToRemote(info, source.path, destinationPath, onProgress)
                }
                else -> {
                    // Both local - shouldn't use this strategy
                    return@withContext Result.Error(
                        message = "Use LocalOperationStrategy for local files",
                        errorCode = ErrorCode.INVALID_OPERATION
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB copy failed: ${source.path} -> $destinationPath")
            Result.Error(
                message = "Copy failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun move(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        // Move = Copy + Delete
        val copyResult = copy(source, destinationPath, onProgress)
        when (copyResult) {
            is Result.Success -> {
                // Delete source after successful copy
                val deleteResult = delete(source, permanent = true)
                if (deleteResult is Result.Error) {
                    Timber.w("Move: copy succeeded but delete failed for ${source.path}")
                }
                // Copy succeeded, even if delete failed - return success
                copyResult
            }
            is Result.Error -> copyResult
            is Result.Loading -> copyResult
        }
    }

    override suspend fun delete(
        file: MediaFile,
        permanent: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "SMB not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            if (!isRemotePath(file.path)) {
                return@withContext Result.Error(
                    message = "Use LocalOperationStrategy for local files",
                    errorCode = ErrorCode.INVALID_OPERATION
                )
            }

            val remotePath = extractRemotePath(file.path)
            
            when (val result = smbClient.deleteFile(info, remotePath)) {
                is SmbClient.SmbResult.Success -> {
                    Timber.d("SMB deleted: ${file.path}")
                    Result.Success(Unit)
                }
                is SmbClient.SmbResult.Error -> {
                    Result.Error(
                        message = result.message,
                        throwable = result.cause,
                        errorCode = ErrorCode.NETWORK_ERROR
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB delete failed: ${file.path}")
            Result.Error(
                message = "Delete failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun rename(
        file: MediaFile,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "SMB not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            if (!isRemotePath(file.path)) {
                return@withContext Result.Error(
                    message = "Use LocalOperationStrategy for local files",
                    errorCode = ErrorCode.INVALID_OPERATION
                )
            }

            val remotePath = extractRemotePath(file.path)
            val parentPath = remotePath.substringBeforeLast('/')
            val newRemotePath = "$parentPath/$newName"
            
            // SMB rename is essentially move on same server
            // Most SMB servers don't have a direct rename - we copy and delete
            val sourceMediaFile = file
            val destFullPath = "${file.path.substringBeforeLast('/')}/$newName"
            
            val result = copy(sourceMediaFile, destFullPath)
            when (result) {
                is Result.Success -> {
                    delete(file, permanent = true)
                    Timber.d("SMB renamed: ${file.path} -> $destFullPath")
                    Result.Success(destFullPath)
                }
                is Result.Error -> result
                is Result.Loading -> result
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB rename failed: ${file.path}")
            Result.Error(
                message = "Rename failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        val info = connectionInfo ?: return@withContext false

        try {
            if (!isRemotePath(path)) {
                return@withContext File(path).exists()
            }

            val remotePath = extractRemotePath(path)
            smbClient.exists(info, remotePath)
        } catch (e: Exception) {
            Timber.e(e, "SMB exists check failed: $path")
            false
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "SMB not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            if (!isRemotePath(path)) {
                return@withContext Result.Error(
                    message = "Use LocalOperationStrategy for local paths",
                    errorCode = ErrorCode.INVALID_OPERATION
                )
            }

            val remotePath = extractRemotePath(path)
            
            when (val result = smbClient.createDirectory(info, remotePath)) {
                is SmbClient.SmbResult.Success -> Result.Success(Unit)
                is SmbClient.SmbResult.Error -> Result.Error(
                    message = result.message,
                    throwable = result.cause,
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB createDirectory failed: $path")
            Result.Error(
                message = "Create directory failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun getFileInfo(path: String): Result<MediaFile> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "SMB not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            if (!isRemotePath(path)) {
                return@withContext Result.Error(
                    message = "Use LocalOperationStrategy for local files",
                    errorCode = ErrorCode.INVALID_OPERATION
                )
            }

            val remotePath = extractRemotePath(path)
            val parentPath = remotePath.substringBeforeLast('/')
            
            when (val result = smbClient.listFiles(info, parentPath)) {
                is SmbClient.SmbResult.Success -> {
                    val fileName = remotePath.substringAfterLast('/')
                    val fileInfo = result.data.find { it.name == fileName }
                    
                    if (fileInfo != null) {
                        Result.Success(
                            MediaFile(
                                path = path,
                                name = fileInfo.name,
                                size = fileInfo.size,
                                date = Date(fileInfo.lastModified),
                                isDirectory = fileInfo.isDirectory,
                                type = getMediaType(fileInfo.name)
                            )
                        )
                    } else {
                        Result.Error(
                            message = "File not found: $path",
                            errorCode = ErrorCode.FILE_NOT_FOUND
                        )
                    }
                }
                is SmbClient.SmbResult.Error -> Result.Error(
                    message = result.message,
                    throwable = result.cause,
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB getFileInfo failed: $path")
            Result.Error(
                message = "Get file info failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    // Helper functions

    private fun isRemotePath(path: String): Boolean {
        return path.startsWith(SMB_SCHEME, ignoreCase = true)
    }

    private fun extractRemotePath(fullPath: String): String {
        // smb://host/share/path -> path
        val withoutScheme = fullPath.removePrefix(SMB_SCHEME)
        val parts = withoutScheme.split("/")
        // Skip host and share name
        return if (parts.size > 2) parts.drop(2).joinToString("/") else ""
    }

    private suspend fun downloadFile(
        info: SmbClient.SmbConnectionInfo,
        remotePath: String,
        localPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val localFile = File(localPath)
        localFile.parentFile?.mkdirs()

        val remoteFilePath = extractRemotePath(remotePath)
        
        FileOutputStream(localFile).use { outputStream ->
            when (val result = smbClient.downloadFile(
                info, 
                remoteFilePath, 
                outputStream
            ) { bytesRead, totalBytes ->
                if (totalBytes > 0) {
                    onProgress?.invoke(bytesRead.toFloat() / totalBytes)
                }
            }) {
                is SmbClient.SmbResult.Success -> {
                    Timber.d("SMB download complete: $remotePath -> $localPath")
                    return Result.Success(localPath)
                }
                is SmbClient.SmbResult.Error -> {
                    localFile.delete() // Clean up partial file
                    return Result.Error(
                        message = result.message,
                        throwable = result.cause,
                        errorCode = ErrorCode.NETWORK_ERROR
                    )
                }
            }
        }
    }

    private suspend fun uploadFile(
        info: SmbClient.SmbConnectionInfo,
        localPath: String,
        remotePath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val localFile = File(localPath)
        if (!localFile.exists()) {
            return Result.Error(
                message = "Source file not found: $localPath",
                errorCode = ErrorCode.FILE_NOT_FOUND
            )
        }

        val remoteFilePath = extractRemotePath(remotePath)
        val fileSize = localFile.length()

        FileInputStream(localFile).use { inputStream ->
            when (val result = smbClient.uploadFile(
                info,
                remoteFilePath,
                inputStream,
                fileSize
            ) { bytesWritten, totalBytes ->
                if (totalBytes > 0) {
                    onProgress?.invoke(bytesWritten.toFloat() / totalBytes)
                }
            }) {
                is SmbClient.SmbResult.Success -> {
                    Timber.d("SMB upload complete: $localPath -> $remotePath")
                    return Result.Success(remotePath)
                }
                is SmbClient.SmbResult.Error -> {
                    return Result.Error(
                        message = result.message,
                        throwable = result.cause,
                        errorCode = ErrorCode.NETWORK_ERROR
                    )
                }
            }
        }
    }

    private suspend fun copyRemoteToRemote(
        info: SmbClient.SmbConnectionInfo,
        sourcePath: String,
        destPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        // For remote-to-remote copy, we download to temp file then upload
        val tempFile = File.createTempFile("smb_copy_", ".tmp", context.cacheDir)
        
        try {
            // Download to temp
            val downloadResult = downloadFile(info, sourcePath, tempFile.absolutePath) { progress ->
                onProgress?.invoke(progress * 0.5f) // First half for download
            }
            
            if (downloadResult is Result.Error) {
                return downloadResult
            }
            
            // Upload from temp
            val uploadResult = uploadFile(info, tempFile.absolutePath, destPath) { progress ->
                onProgress?.invoke(0.5f + progress * 0.5f) // Second half for upload
            }
            
            return if (uploadResult is Result.Error) {
                uploadResult
            } else {
                Result.Success(destPath)
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun getMediaType(filename: String): MediaType {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "ts" -> MediaType.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "opus" -> MediaType.AUDIO
            else -> MediaType.OTHER
        }
    }
}
