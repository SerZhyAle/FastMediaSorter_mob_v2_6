package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.sza.fastmediasorter.data.network.SftpClient
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
 * Implementation of FileOperationStrategy for SFTP (SSH File Transfer Protocol).
 * Handles file operations on SFTP servers with password or key-based authentication.
 */
@Singleton
class SftpOperationStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sftpClient: SftpClient
) : FileOperationStrategy {

    companion object {
        private const val SFTP_SCHEME = "sftp://"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Connection configuration for SFTP operations.
     */
    data class SftpConfig(
        val host: String,
        val port: Int = 22,
        val username: String,
        val password: String = "",
        val privateKey: String? = null,
        val passphrase: String? = null
    )

    // Connection config for current SFTP session
    private var config: SftpConfig? = null

    /**
     * Configure SFTP connection settings.
     * Must be called before using SFTP operations.
     */
    fun configure(
        host: String,
        port: Int = 22,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ) {
        config = SftpConfig(host, port, username, password, privateKey, passphrase)
    }

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured. Call configure() first.",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        try {
            val isSourceRemote = isRemotePath(source.path)
            val isDestRemote = isRemotePath(destinationPath)

            when {
                isSourceRemote && !isDestRemote -> {
                    // Download: SFTP -> Local
                    downloadFile(cfg, source.path, destinationPath, onProgress)
                }
                !isSourceRemote && isDestRemote -> {
                    // Upload: Local -> SFTP
                    uploadFile(cfg, source.path, destinationPath, onProgress)
                }
                isSourceRemote && isDestRemote -> {
                    // SFTP -> SFTP: Download then upload
                    copyRemoteToRemote(cfg, source.path, destinationPath, onProgress)
                }
                else -> {
                    Result.Error(
                        message = "Use LocalOperationStrategy for local files",
                        errorCode = ErrorCode.INVALID_OPERATION
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SFTP copy failed: ${source.path} -> $destinationPath")
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
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        // Check if both paths are on the same remote server - use rename
        if (isRemotePath(source.path) && isRemotePath(destinationPath)) {
            val sourceRemote = extractRemotePath(source.path)
            val destRemote = extractRemotePath(destinationPath)
            
            val renameResult = sftpClient.rename(
                cfg.host, cfg.port, sourceRemote, destRemote, 
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (renameResult.isSuccess) {
                Timber.d("SFTP moved via rename: ${source.path} -> $destinationPath")
                return@withContext Result.Success(destinationPath)
            } else {
                Timber.w("SFTP rename failed, falling back to copy+delete")
                // Fall through to copy+delete
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
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured",
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
            
            val deleteResult = sftpClient.deleteFile(
                cfg.host, cfg.port, remotePath,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (deleteResult.isSuccess) {
                Timber.d("SFTP deleted: ${file.path}")
                Result.Success(Unit)
            } else {
                Result.Error(
                    message = deleteResult.exceptionOrNull()?.message ?: "Delete failed",
                    throwable = deleteResult.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "SFTP delete failed: ${file.path}")
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
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured",
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
            
            val renameResult = sftpClient.rename(
                cfg.host, cfg.port, remotePath, newRemotePath,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (renameResult.isSuccess) {
                val newFullPath = "${file.path.substringBeforeLast('/')}/$newName"
                Timber.d("SFTP renamed: ${file.path} -> $newFullPath")
                Result.Success(newFullPath)
            } else {
                Result.Error(
                    message = renameResult.exceptionOrNull()?.message ?: "Rename failed",
                    throwable = renameResult.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "SFTP rename failed: ${file.path}")
            Result.Error(
                message = "Rename failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        val cfg = config ?: return@withContext false

        try {
            if (!isRemotePath(path)) {
                return@withContext File(path).exists()
            }

            val remotePath = extractRemotePath(path)
            val existsResult = sftpClient.exists(
                cfg.host, cfg.port, remotePath,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            existsResult.getOrDefault(false)
        } catch (e: Exception) {
            Timber.e(e, "SFTP exists check failed: $path")
            false
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured",
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
            
            val mkdirResult = sftpClient.createDirectory(
                cfg.host, cfg.port, remotePath,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (mkdirResult.isSuccess) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    message = mkdirResult.exceptionOrNull()?.message ?: "Create directory failed",
                    throwable = mkdirResult.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "SFTP createDirectory failed: $path")
            Result.Error(
                message = "Create directory failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun getFileInfo(path: String): Result<MediaFile> = withContext(Dispatchers.IO) {
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SFTP not configured",
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
            val fileName = remotePath.substringAfterLast('/')
            
            // Get file size
            val sizeResult = sftpClient.getFileSize(
                cfg.host, cfg.port, remotePath,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            val size = sizeResult.getOrDefault(-1L)
            
            val mediaFile = MediaFile(
                path = path,
                name = fileName,
                size = if (size >= 0) size else 0,
                date = Date(),
                type = getMediaType(fileName),
                isDirectory = false
            )
            
            Result.Success(mediaFile)
        } catch (e: Exception) {
            Timber.e(e, "SFTP getFileInfo failed: $path")
            Result.Error(
                message = "Get file info failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    // Private helper methods

    private fun isRemotePath(path: String): Boolean {
        return path.startsWith(SFTP_SCHEME)
    }

    private fun extractRemotePath(fullPath: String): String {
        // sftp://host:port/path -> /path
        if (!fullPath.startsWith(SFTP_SCHEME)) return fullPath
        
        val withoutScheme = fullPath.removePrefix(SFTP_SCHEME)
        val pathStart = withoutScheme.indexOf('/')
        return if (pathStart >= 0) withoutScheme.substring(pathStart) else "/"
    }

    private suspend fun downloadFile(
        cfg: SftpConfig,
        remotePath: String,
        localPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val localFile = File(localPath)
        localFile.parentFile?.mkdirs()

        val remoteFilePath = extractRemotePath(remotePath)
        
        FileOutputStream(localFile).use { outputStream ->
            val result = sftpClient.downloadFile(
                cfg.host, cfg.port, remoteFilePath, outputStream,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (result.isSuccess) {
                onProgress?.invoke(1f)
                Timber.d("SFTP download complete: $remotePath -> $localPath")
                return Result.Success(localPath)
            } else {
                localFile.delete()
                return Result.Error(
                    message = result.exceptionOrNull()?.message ?: "Download failed",
                    throwable = result.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        }
    }

    private suspend fun uploadFile(
        cfg: SftpConfig,
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

        FileInputStream(localFile).use { inputStream ->
            val result = sftpClient.uploadFile(
                cfg.host, cfg.port, remoteFilePath, inputStream,
                cfg.username, cfg.password, cfg.privateKey, cfg.passphrase
            )
            
            if (result.isSuccess) {
                onProgress?.invoke(1f)
                Timber.d("SFTP upload complete: $localPath -> $remotePath")
                return Result.Success(remotePath)
            } else {
                return Result.Error(
                    message = result.exceptionOrNull()?.message ?: "Upload failed",
                    throwable = result.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
            }
        }
    }

    private suspend fun copyRemoteToRemote(
        cfg: SftpConfig,
        sourcePath: String,
        destPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val tempFile = File.createTempFile("sftp_copy_", ".tmp", context.cacheDir)
        
        try {
            val downloadResult = downloadFile(cfg, sourcePath, tempFile.absolutePath) { progress ->
                onProgress?.invoke(progress * 0.5f)
            }
            
            if (downloadResult is Result.Error) {
                return downloadResult
            }
            
            val uploadResult = uploadFile(cfg, tempFile.absolutePath, destPath) { progress ->
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
