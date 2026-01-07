package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.sza.fastmediasorter.data.network.FtpClient
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
 * Implementation of FileOperationStrategy for FTP protocol.
 * Handles file operations on FTP servers.
 */
@Singleton
class FtpOperationStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ftpClient: FtpClient
) : FileOperationStrategy {

    companion object {
        private const val FTP_SCHEME = "ftp://"
    }

    // Connection info for current FTP session
    private var connectionInfo: FtpClient.FtpConnectionInfo? = null

    /**
     * Configure FTP connection settings.
     * Must be called before using FTP operations.
     */
    fun configure(
        host: String,
        port: Int = 21,
        username: String = "anonymous",
        password: String = "anonymous@",
        usePassiveMode: Boolean = true
    ) {
        connectionInfo = FtpClient.FtpConnectionInfo(
            host = host,
            port = port,
            username = username,
            password = password,
            usePassiveMode = usePassiveMode
        )
    }

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "FTP not configured. Call configure() first.",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            val isSourceRemote = isRemotePath(source.path)
            val isDestRemote = isRemotePath(destinationPath)

            when {
                isSourceRemote && !isDestRemote -> {
                    // Download: FTP -> Local
                    downloadFile(info, source.path, destinationPath, onProgress)
                }
                !isSourceRemote && isDestRemote -> {
                    // Upload: Local -> FTP
                    uploadFile(info, source.path, destinationPath, onProgress)
                }
                isSourceRemote && isDestRemote -> {
                    // FTP -> FTP: Download then upload
                    copyRemoteToRemote(info, source.path, destinationPath, onProgress)
                }
                else -> {
                    Result.Error(
                        message = "Use LocalOperationStrategy for local files",
                        errorCode = ErrorCode.INVALID_OPERATION
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP copy failed: ${source.path} -> $destinationPath")
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
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "FTP not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        // Check if both paths are on the same remote server - use rename
        if (isRemotePath(source.path) && isRemotePath(destinationPath)) {
            val sourceRemote = extractRemotePath(source.path)
            val destRemote = extractRemotePath(destinationPath)
            
            when (val result = ftpClient.rename(info, sourceRemote, destRemote)) {
                is FtpClient.FtpResult.Success -> {
                    Timber.d("FTP moved via rename: ${source.path} -> $destinationPath")
                    return@withContext Result.Success(destinationPath)
                }
                is FtpClient.FtpResult.Error -> {
                    Timber.w("FTP rename failed, falling back to copy+delete")
                    // Fall through to copy+delete
                }
            }
        }

        // Fallback: Copy + Delete
        val copyResult = copy(source, destinationPath, onProgress)
        when (copyResult) {
            is Result.Success -> {
                delete(source, permanent = true)
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
                message = "FTP not configured",
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
            
            when (val result = ftpClient.deleteFile(info, remotePath)) {
                is FtpClient.FtpResult.Success -> {
                    Timber.d("FTP deleted: ${file.path}")
                    Result.Success(Unit)
                }
                is FtpClient.FtpResult.Error -> {
                    Result.Error(
                        message = result.message,
                        throwable = result.cause,
                        errorCode = ErrorCode.NETWORK_ERROR
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP delete failed: ${file.path}")
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
                message = "FTP not configured",
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
            
            when (val result = ftpClient.rename(info, remotePath, newRemotePath)) {
                is FtpClient.FtpResult.Success -> {
                    val newFullPath = "${file.path.substringBeforeLast('/')}/$newName"
                    Timber.d("FTP renamed: ${file.path} -> $newFullPath")
                    Result.Success(newFullPath)
                }
                is FtpClient.FtpResult.Error -> {
                    Result.Error(
                        message = result.message,
                        throwable = result.cause,
                        errorCode = ErrorCode.NETWORK_ERROR
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP rename failed: ${file.path}")
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
            val parentPath = remotePath.substringBeforeLast('/')
            val fileName = remotePath.substringAfterLast('/')
            
            when (val result = ftpClient.listFiles(info, parentPath)) {
                is FtpClient.FtpResult.Success -> {
                    result.data.any { it.name == fileName }
                }
                is FtpClient.FtpResult.Error -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP exists check failed: $path")
            false
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val info = connectionInfo
            ?: return@withContext Result.Error(
                message = "FTP not configured",
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
            
            when (val result = ftpClient.createDirectory(info, remotePath)) {
                is FtpClient.FtpResult.Success -> Result.Success(Unit)
                is FtpClient.FtpResult.Error -> Result.Error(
                    message = result.message,
                    throwable = result.cause,
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP createDirectory failed: $path")
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
                message = "FTP not configured",
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
            val fileName = remotePath.substringAfterLast('/')
            
            when (val result = ftpClient.listFiles(info, parentPath)) {
                is FtpClient.FtpResult.Success -> {
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
                is FtpClient.FtpResult.Error -> Result.Error(
                    message = result.message,
                    throwable = result.cause,
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "FTP getFileInfo failed: $path")
            Result.Error(
                message = "Get file info failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    // Helper functions

    private fun isRemotePath(path: String): Boolean {
        return path.startsWith(FTP_SCHEME, ignoreCase = true)
    }

    private fun extractRemotePath(fullPath: String): String {
        // ftp://host:port/path -> /path
        val withoutScheme = fullPath.removePrefix(FTP_SCHEME)
        val slashIndex = withoutScheme.indexOf('/')
        return if (slashIndex >= 0) withoutScheme.substring(slashIndex) else "/"
    }

    private suspend fun downloadFile(
        info: FtpClient.FtpConnectionInfo,
        remotePath: String,
        localPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val localFile = File(localPath)
        localFile.parentFile?.mkdirs()

        val remoteFilePath = extractRemotePath(remotePath)
        
        FileOutputStream(localFile).use { outputStream ->
            when (val result = ftpClient.downloadFile(
                info, 
                remoteFilePath, 
                outputStream
            ) { bytesRead, totalBytes ->
                if (totalBytes > 0) {
                    onProgress?.invoke(bytesRead.toFloat() / totalBytes)
                }
            }) {
                is FtpClient.FtpResult.Success -> {
                    Timber.d("FTP download complete: $remotePath -> $localPath")
                    return Result.Success(localPath)
                }
                is FtpClient.FtpResult.Error -> {
                    localFile.delete()
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
        info: FtpClient.FtpConnectionInfo,
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
            when (val result = ftpClient.uploadFile(
                info,
                remoteFilePath,
                inputStream,
                fileSize
            ) { bytesWritten, totalBytes ->
                if (totalBytes > 0) {
                    onProgress?.invoke(bytesWritten.toFloat() / totalBytes)
                }
            }) {
                is FtpClient.FtpResult.Success -> {
                    Timber.d("FTP upload complete: $localPath -> $remotePath")
                    return Result.Success(remotePath)
                }
                is FtpClient.FtpResult.Error -> {
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
        info: FtpClient.FtpConnectionInfo,
        sourcePath: String,
        destPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val tempFile = File.createTempFile("ftp_copy_", ".tmp", context.cacheDir)
        
        try {
            val downloadResult = downloadFile(info, sourcePath, tempFile.absolutePath) { progress ->
                onProgress?.invoke(progress * 0.5f)
            }
            
            if (downloadResult is Result.Error) {
                return downloadResult
            }
            
            val uploadResult = uploadFile(info, tempFile.absolutePath, destPath) { progress ->
                onProgress?.invoke(0.5f + progress * 0.5f)
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
