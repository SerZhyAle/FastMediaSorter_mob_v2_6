package com.sza.fastmediasorter.data.transfer.strategy

import android.content.Context
import android.net.Uri
import com.sza.fastmediasorter.data.cloud.CloudProvider
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import com.sza.fastmediasorter.data.cloud.DropboxClient
import com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient
import com.sza.fastmediasorter.data.cloud.OneDriveRestClient
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Strategy for cloud:// operations.
 *
 * Supported path forms (provider in URI host):
 * - cloud://google_drive/<idOrPath>
 * - cloud://googledrive/<idOrPath>
 * - cloud://dropbox/<path>
 * - cloud://onedrive/<idOrPath>
 *
 * Notes:
 * - This strategy focuses on Cloud<->Local and Cloud<->Cloud operations.
 * - Cross-protocol Cloud<->(SMB/SFTP/FTP) transfers are expected to be handled by the handler via temp files.
 */
class CloudOperationStrategy(
    private val context: Context,
    private val googleDriveClient: GoogleDriveRestClient,
    private val dropboxClient: DropboxClient,
    private val oneDriveClient: OneDriveRestClient
) : FileOperationStrategy {

    override suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isSourceCloud = supportsProtocol(source)
            val isDestCloud = supportsProtocol(destination)

            when {
                isSourceCloud && !isDestCloud -> downloadCloudToLocal(source, destination, progressCallback)
                !isSourceCloud && isDestCloud -> uploadLocalToCloud(source, destination, overwrite, progressCallback)
                isSourceCloud && isDestCloud -> copyCloudToCloud(source, destination, overwrite, progressCallback)
                else -> Result.failure(IllegalArgumentException("At least one path must be cloud://"))
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Copy failed - $source -> $destination")
            Result.failure(e)
        }
    }

    override suspend fun moveFile(source: String, destination: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isSourceCloud = supportsProtocol(source)
            val isDestCloud = supportsProtocol(destination)

            when {
                isSourceCloud && isDestCloud -> {
                    val src = parseCloudUri(source)
                        ?: return@withContext Result.failure(Exception("Failed to parse cloud source: $source"))
                    val dst = parseCloudUri(destination)
                        ?: return@withContext Result.failure(Exception("Failed to parse cloud destination: $destination"))

                    val client = getClientOrThrow(src.provider)

                    if (src.provider == dst.provider) {
                        val srcIdOrPath = src.idOrPath
                        val (dstParent, dstName) = splitParentAndName(dst)

                        val moved = client.moveFile(srcIdOrPath, dstParent)
                        when (moved) {
                            is CloudResult.Success -> {
                                if (dstName != null && moved.data.name != dstName) {
                                    client.renameFile(moved.data.id, dstName)
                                }
                                Result.success(Unit)
                            }
                            is CloudResult.Error -> Result.failure(Exception(moved.message, moved.cause))
                        }
                    } else {
                        // Cross-provider move: copy then delete
                        val copyResult = copyCloudToCloud(source, destination, overwrite = true, progressCallback = null)
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
            Timber.e(e, "CloudOperationStrategy: Move failed - $source -> $destination")
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!supportsProtocol(path)) {
                return@withContext Result.failure(IllegalArgumentException("Path must be cloud://: $path"))
            }

            val info = parseCloudUri(path)
                ?: return@withContext Result.failure(Exception("Failed to parse cloud path: $path"))

            val client = getClientOrThrow(info.provider)

            when (val result = client.deleteFile(info.idOrPath)) {
                is CloudResult.Success -> Result.success(Unit)
                is CloudResult.Error -> Result.failure(Exception(result.message, result.cause))
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Delete failed - $path")
            Result.failure(e)
        }
    }

    override suspend fun exists(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!supportsProtocol(path)) {
                return@withContext Result.success(false)
            }

            val info = parseCloudUri(path)
                ?: return@withContext Result.failure(Exception("Failed to parse cloud path: $path"))

            val client = getClientOrThrow(info.provider)

            // Try lightweight fileExists when we can infer (parent + name), otherwise fallback to metadata.
            val (parentId, name) = splitParentAndName(info)
            if (name != null) {
                when (val existsResult = client.fileExists(name, parentId)) {
                    is CloudResult.Success -> return@withContext Result.success(existsResult.data)
                    is CloudResult.Error -> Timber.w("CloudOperationStrategy: fileExists failed, falling back to metadata: ${existsResult.message}")
                }
            }

            when (val meta = client.getFileMetadata(info.idOrPath)) {
                is CloudResult.Success -> Result.success(true)
                is CloudResult.Error -> Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Exists check failed - $path")
            Result.failure(e)
        }
    }

    override fun supportsProtocol(path: String): Boolean {
        return path.startsWith("cloud://") || path.startsWith("cloud:/")
    }

    override fun getProtocolName(): String = "cloud"

    private suspend fun downloadCloudToLocal(
        cloudPath: String,
        localPath: String,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val info = parseCloudUri(cloudPath)
            ?: return Result.failure(Exception("Failed to parse cloud path: $cloudPath"))

        val client = getClientOrThrow(info.provider)
        val localFile = File(localPath)
        localFile.parentFile?.mkdirs()

        val progressScope = CoroutineScope(currentCoroutineContext())

        return try {
            localFile.outputStream().use { output ->
                when (val result = client.downloadFile(
                    fileId = info.fileIdForDownload,
                    outputStream = output,
                    progressCallback = { progress ->
                        progressScope.launch {
                            progressCallback?.onProgress(progress.bytesTransferred, progress.totalBytes, 0L)
                        }
                    }
                )) {
                    is CloudResult.Success -> Result.success(localPath)
                    is CloudResult.Error -> Result.failure(Exception(result.message, result.cause))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadLocalToCloud(
        localPath: String,
        cloudDestPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val info = parseCloudUri(cloudDestPath)
            ?: return Result.failure(Exception("Failed to parse cloud destination: $cloudDestPath"))

        val client = getClientOrThrow(info.provider)
        val localFile = File(localPath)
        if (!localFile.exists()) {
            return Result.failure(Exception("Source file does not exist: $localPath"))
        }

        val (parentId, fileName) = splitParentAndName(info)
        val targetName = fileName ?: localFile.name

        if (!overwrite) {
            when (val existsResult = client.fileExists(targetName, parentId)) {
                is CloudResult.Success -> if (existsResult.data) {
                    return Result.failure(Exception("Destination file already exists: $cloudDestPath"))
                }
                is CloudResult.Error -> Timber.w("CloudOperationStrategy: fileExists failed: ${existsResult.message}")
            }
        }

        val mimeType = guessMimeType(targetName)

        val progressScope = CoroutineScope(currentCoroutineContext())

        return try {
            localFile.inputStream().use { input ->
                when (val result = client.uploadFile(
                    inputStream = input,
                    fileName = targetName,
                    mimeType = mimeType,
                    parentFolderId = parentId.ifBlank { null },
                    progressCallback = { progress ->
                        progressScope.launch {
                            progressCallback?.onProgress(progress.bytesTransferred, progress.totalBytes, 0L)
                        }
                    }
                )) {
                    is CloudResult.Success -> Result.success(cloudDestPath)
                    is CloudResult.Error -> Result.failure(Exception(result.message, result.cause))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun copyCloudToCloud(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val src = parseCloudUri(source)
            ?: return Result.failure(Exception("Failed to parse cloud source: $source"))
        val dst = parseCloudUri(destination)
            ?: return Result.failure(Exception("Failed to parse cloud destination: $destination"))

        if (src.provider == dst.provider) {
            val client = getClientOrThrow(src.provider)
            val (dstParent, dstName) = splitParentAndName(dst)
            val nameForCheck = dstName

            if (!overwrite && nameForCheck != null) {
                when (val existsResult = client.fileExists(nameForCheck, dstParent)) {
                    is CloudResult.Success -> if (existsResult.data) {
                        return Result.failure(Exception("Destination file already exists: $destination"))
                    }
                    is CloudResult.Error -> Timber.w("CloudOperationStrategy: fileExists failed: ${existsResult.message}")
                }
            }

            return when (val copied = client.copyFile(src.idOrPath, dstParent, dstName)) {
                is CloudResult.Success -> Result.success(destination)
                is CloudResult.Error -> Result.failure(Exception(copied.message, copied.cause))
            }
        }

        // Cross-provider: download to temp, then upload.
        val tempFile = File.createTempFile("cloud_copy_", ".tmp", context.cacheDir)
        return try {
            val downloadResult = downloadCloudToLocal(source, tempFile.absolutePath, progressCallback)
            if (downloadResult.isFailure) {
                return downloadResult
            }
            uploadLocalToCloud(tempFile.absolutePath, destination, overwrite, progressCallback)
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private suspend fun getClientOrThrow(provider: CloudProvider): CloudStorageClient {
        return getClient(provider) ?: throw Exception("Not authenticated: ${provider.name}")
    }

    private suspend fun getClient(provider: CloudProvider): CloudStorageClient? {
        val client: CloudStorageClient = when (provider) {
            CloudProvider.GOOGLE_DRIVE -> googleDriveClient
            CloudProvider.DROPBOX -> dropboxClient
            CloudProvider.ONEDRIVE -> oneDriveClient
        }

        if (client.isAuthenticated()) return client

        val restored = when (provider) {
            CloudProvider.DROPBOX -> (client as? DropboxClient)?.tryRestoreFromStorage() == true
            CloudProvider.GOOGLE_DRIVE -> (client as? GoogleDriveRestClient)?.tryRestoreFromStorage() == true
            CloudProvider.ONEDRIVE -> false
        }

        return if (restored) client else null
    }

    private data class CloudUriInfo(
        val provider: CloudProvider,
        val idOrPath: String,
        val segments: List<String>
    ) {
        val fileIdForDownload: String
            get() = segments.lastOrNull() ?: idOrPath
    }

    private fun parseCloudUri(rawPath: String): CloudUriInfo? {
        return try {
            val normalized = if (rawPath.startsWith("cloud:/") && !rawPath.startsWith("cloud://")) {
                rawPath.replaceFirst("cloud:/", "cloud://")
            } else {
                rawPath
            }

            if (!normalized.startsWith("cloud://")) return null

            val uri = Uri.parse(normalized)
            val host = uri.host?.lowercase() ?: return null
            val provider = when (host) {
                "google_drive", "googledrive", "google" -> CloudProvider.GOOGLE_DRIVE
                "dropbox" -> CloudProvider.DROPBOX
                "onedrive" -> CloudProvider.ONEDRIVE
                else -> return null
            }

            val idOrPath = uri.path?.removePrefix("/") ?: ""
            if (idOrPath.isBlank()) return null

            return CloudUriInfo(provider, idOrPath, uri.pathSegments)
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Failed to parse cloud uri: $rawPath")
            null
        }
    }

    private fun splitParentAndName(info: CloudUriInfo): Pair<String, String?> {
        if (info.segments.isEmpty()) return "" to null
        if (info.segments.size == 1) {
            // Ambiguous (could be folderId or fileId). Treat as "no parent".
            return "" to null
        }

        val name = info.segments.last()
        val parentSegments = info.segments.dropLast(1)

        val parentIdOrPath = when (info.provider) {
            CloudProvider.DROPBOX -> "/" + parentSegments.joinToString("/")
            else -> parentSegments.joinToString("/")
        }

        return parentIdOrPath to name
    }

    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }
}
