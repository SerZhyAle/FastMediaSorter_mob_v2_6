package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.sza.fastmediasorter.data.network.SmbClient
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
 * Implementation of FileOperationStrategy for SMB/CIFS protocol.
 * Handles file operations on Windows network shares.
 */
@Singleton
class SmbOperationStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbClient: SmbClient
) : FileOperationStrategy {

    companion object {
        private const val SMB_SCHEME = "smb://"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Connection configuration for SMB operations.
     */
    data class SmbConfig(
        val server: String,
        val port: Int = 445,
        val shareName: String,
        val username: String,
        val password: String,
        val domain: String = ""
    )

    // Connection config for current SMB session
    private var config: SmbConfig? = null

    /**
     * Configure SMB connection settings.
     * Must be called before using SMB operations.
     */
    fun configure(
        server: String,
        port: Int = 445,
        shareName: String,
        username: String,
        password: String,
        domain: String = ""
    ) {
        config = SmbConfig(server, port, shareName, username, password, domain)
    }

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        val cfg = config
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
                    downloadFile(cfg, source.path, destinationPath, onProgress)
                }
                !isSourceRemote && isDestRemote -> {
                    // Upload: Local -> SMB
                    uploadFile(cfg, source.path, destinationPath, onProgress)
                }
                isSourceRemote && isDestRemote -> {
                    // SMB -> SMB: Download then upload
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
        val cfg = config
            ?: return@withContext Result.Error(
                message = "SMB not configured",
                errorCode = ErrorCode.NETWORK_ERROR
            )

        // Check if both paths are on the same remote server - use rename
        if (isRemotePath(source.path) && isRemotePath(destinationPath)) {
            val sourceRemote = extractRemotePath(source.path)
            val destRemote = extractRemotePath(destinationPath)
            
            val renameResult = smbClient.rename(
                cfg.server, cfg.port, cfg.shareName, sourceRemote, destRemote,
                cfg.username, cfg.password, cfg.domain
            )
            
            if (renameResult.isSuccess) {
                Timber.d("SMB moved via rename: ${source.path} -> $destinationPath")
                return@withContext Result.Success(destinationPath)
            } else {
                Timber.w("SMB rename failed, falling back to copy+delete")
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
            
            val deleteResult = smbClient.deleteFile(
                cfg.server, cfg.port, cfg.shareName, remotePath,
                cfg.username, cfg.password, cfg.domain
            )
            
            if (deleteResult.isSuccess) {
                Timber.d("SMB deleted: ${file.path}")
                Result.Success(Unit)
            } else {
                Result.Error(
                    message = deleteResult.exceptionOrNull()?.message ?: "Delete failed",
                    throwable = deleteResult.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
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
        val cfg = config
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
            val newRemotePath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"
            
            val renameResult = smbClient.rename(
                cfg.server, cfg.port, cfg.shareName, remotePath, newRemotePath,
                cfg.username, cfg.password, cfg.domain
            )
            
            if (renameResult.isSuccess) {
                val newFullPath = "${file.path.substringBeforeLast('/')}/$newName"
                Timber.d("SMB renamed: ${file.path} -> $newFullPath")
                Result.Success(newFullPath)
            } else {
                Result.Error(
                    message = renameResult.exceptionOrNull()?.message ?: "Rename failed",
                    throwable = renameResult.exceptionOrNull(),
                    errorCode = ErrorCode.NETWORK_ERROR
                )
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
        val cfg = config ?: return@withContext false

        try {
            if (!isRemotePath(path)) {
                return@withContext File(path).exists()
            }

            val remotePath = extractRemotePath(path)
            val existsResult = smbClient.exists(
                cfg.server, cfg.port, cfg.shareName, remotePath,
                cfg.username, cfg.password, cfg.domain
            )
            
            existsResult.getOrDefault(false)
        } catch (e: Exception) {
            Timber.e(e, "SMB exists check failed: $path")
            false
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cfg = config
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
            
            val mkdirResult = smbClient.createDirectory(
                cfg.server, cfg.port, cfg.shareName, remotePath,
                cfg.username, cfg.password, cfg.domain
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
            Timber.e(e, "SMB createDirectory failed: $path")
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
            val fileName = remotePath.substringAfterLast('/')
            
            // Get file size
            val sizeResult = smbClient.getFileSize(
                cfg.server, cfg.port, cfg.shareName, remotePath,
                cfg.username, cfg.password, cfg.domain
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
            Timber.e(e, "SMB getFileInfo failed: $path")
            Result.Error(
                message = "Get file info failed: ${e.message}",
                throwable = e,
                errorCode = ErrorCode.NETWORK_ERROR
            )
        }
    }

    // Private helper methods

    private fun isRemotePath(path: String): Boolean {
        return path.startsWith(SMB_SCHEME)
    }

    private fun extractRemotePath(fullPath: String): String {
        // smb://server:port/share/path -> /path
        if (!fullPath.startsWith(SMB_SCHEME)) return fullPath
        
        val withoutScheme = fullPath.removePrefix(SMB_SCHEME)
        // Skip server:port/share
        val parts = withoutScheme.split("/", limit = 3)
        return if (parts.size > 2) "/" + parts[2] else "/"
    }

    private suspend fun downloadFile(
        cfg: SmbConfig,
        remotePath: String,
        localPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val localFile = File(localPath)
        localFile.parentFile?.mkdirs()

        val remoteFilePath = extractRemotePath(remotePath)
        
        FileOutputStream(localFile).use { outputStream ->
            val result = smbClient.downloadFile(
                cfg.server, cfg.port, cfg.shareName, remoteFilePath, outputStream,
                cfg.username, cfg.password, cfg.domain
            )
            
            if (result.isSuccess) {
                onProgress?.invoke(1f)
                Timber.d("SMB download complete: $remotePath -> $localPath")
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
        cfg: SmbConfig,
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
            val result = smbClient.uploadFile(
                cfg.server, cfg.port, cfg.shareName, remoteFilePath, inputStream,
                cfg.username, cfg.password, cfg.domain
            )
            
            if (result.isSuccess) {
                onProgress?.invoke(1f)
                Timber.d("SMB upload complete: $localPath -> $remotePath")
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
        cfg: SmbConfig,
        sourcePath: String,
        destPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val tempFile = File.createTempFile("smb_copy_", ".tmp", context.cacheDir)
        
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
