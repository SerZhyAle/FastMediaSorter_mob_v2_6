package com.sza.fastmediasorter.data.operation

import android.content.Context
import android.net.Uri
import com.sza.fastmediasorter.data.cloud.CloudProvider
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.CloudStorageClient
import com.sza.fastmediasorter.data.cloud.DropboxClient
import com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient
import com.sza.fastmediasorter.data.cloud.OneDriveRestClient
import com.sza.fastmediasorter.domain.model.MediaExtensions
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FileOperationStrategy implementation for cloud:// protocol.
 *
 * Supported path forms (provider in URI host):
 * - cloud://google_drive/<idOrPath>
 * - cloud://googledrive/<idOrPath>
 * - cloud://dropbox/<path>
 * - cloud://onedrive/<idOrPath>
 */
@Singleton
class CloudOperationStrategy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleDriveClient: GoogleDriveRestClient,
    private val dropboxClient: DropboxClient,
    private val oneDriveClient: OneDriveRestClient
) : FileOperationStrategy {

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourcePath = source.path
            val isSourceCloud = supportsProtocol(sourcePath)
            val isDestCloud = supportsProtocol(destinationPath)

            when {
                isSourceCloud && !isDestCloud -> downloadCloudToLocal(sourcePath, destinationPath, onProgress)
                !isSourceCloud && isDestCloud -> uploadLocalToCloud(sourcePath, destinationPath, source, onProgress)
                isSourceCloud && isDestCloud -> copyCloudToCloud(sourcePath, destinationPath, onProgress)
                else -> Result.Error("At least one path must be cloud://")
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Copy failed - ${source.path} -> $destinationPath")
            Result.Error("Copy failed: ${e.message}", e)
        }
    }

    override suspend fun move(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourcePath = source.path
            val isSourceCloud = supportsProtocol(sourcePath)
            val isDestCloud = supportsProtocol(destinationPath)

            when {
                isSourceCloud && isDestCloud -> {
                    val src = parseCloudUri(sourcePath) ?: return@withContext Result.Error("Failed to parse cloud source: $sourcePath")
                    val dst = parseCloudUri(destinationPath) ?: return@withContext Result.Error("Failed to parse cloud destination: $destinationPath")

                    val client = getClientOrNull(src.provider) ?: return@withContext Result.Error("Not authenticated: ${src.provider}")

                    if (src.provider == dst.provider) {
                        val (dstParent, dstName) = splitParentAndName(dst)
                        when (val moved = client.moveFile(src.idOrPath, dstParent)) {
                            is CloudResult.Success -> {
                                if (dstName != null && moved.data.name != dstName) {
                                    when (client.renameFile(moved.data.id, dstName)) {
                                        is CloudResult.Success -> Result.Success(destinationPath)
                                        is CloudResult.Error -> Result.Error("Move succeeded but rename failed")
                                    }
                                } else {
                                    Result.Success(destinationPath)
                                }
                            }
                            is CloudResult.Error -> Result.Error("Move failed: ${moved.message}", moved.cause)
                        }
                    } else {
                        when (val copyResult = copy(source, destinationPath, onProgress)) {
                            is Result.Success -> {
                                when (delete(source, false)) {
                                    is Result.Success -> Result.Success(destinationPath)
                                    is Result.Error -> Result.Error("Copied but failed to delete source")
                                    else -> Result.Error("Unexpected state")
                                }
                            }
                            is Result.Error -> Result.Error("Copy failed during move: ${copyResult.message}", copyResult.throwable)
                            Result.Loading -> Result.Error("Unexpected Loading state")
                        }
                    }
                }
                else -> {
                    when (val copyResult = copy(source, destinationPath, onProgress)) {
                        is Result.Success -> {
                            when (delete(source, false)) {
                                is Result.Success -> Result.Success(destinationPath)
                                is Result.Error -> Result.Error("Copied but failed to delete source")
                                else -> Result.Error("Unexpected state")
                            }
                        }
                        is Result.Error -> Result.Error("Copy failed during move: ${copyResult.message}", copyResult.throwable)
                        Result.Loading -> Result.Error("Unexpected Loading state")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Move failed - ${source.path} -> $destinationPath")
            Result.Error("Move failed: ${e.message}", e)
        }
    }

    override suspend fun delete(file: MediaFile, permanent: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = file.path
            if (!supportsProtocol(path)) {
                return@withContext Result.Error("Path must be cloud://: $path")
            }

            val info = parseCloudUri(path) ?: return@withContext Result.Error("Failed to parse cloud path: $path")
            val client = getClientOrNull(info.provider) ?: return@withContext Result.Error("Not authenticated: ${info.provider}")

            when (val result = client.deleteFile(info.idOrPath)) {
                is CloudResult.Success -> Result.Success(Unit)
                is CloudResult.Error -> Result.Error("Delete failed: ${result.message}", result.cause)
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Delete failed - ${file.path}")
            Result.Error("Delete failed: ${e.message}", e)
        }
    }

    override suspend fun rename(file: MediaFile, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val path = file.path
            if (!supportsProtocol(path)) {
                return@withContext Result.Error("Path must be cloud://: $path")
            }

            val info = parseCloudUri(path) ?: return@withContext Result.Error("Failed to parse cloud path: $path")
            val client = getClientOrNull(info.provider) ?: return@withContext Result.Error("Not authenticated: ${info.provider}")

            when (val result = client.renameFile(info.idOrPath, newName)) {
                is CloudResult.Success -> {
                    val segments = info.segments.dropLast(1) + newName
                    val newPath = "cloud://${info.provider.name.lowercase()}/" + segments.joinToString("/")
                    Result.Success(newPath)
                }
                is CloudResult.Error -> Result.Error("Rename failed: ${result.message}", result.cause)
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudOperationStrategy: Rename failed - ${file.path} -> $newName")
            Result.Error("Rename failed: ${e.message}", e)
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!supportsProtocol(path)) return@withContext false
            val info = parseCloudUri(path) ?: return@withContext false
            val client = getClientOrNull(info.provider) ?: return@withContext false

            val (parentId, name) = splitParentAndName(info)
            if (name != null) {
                when (val existsResult = client.fileExists(name, parentId)) {
                    is CloudResult.Success -> return@withContext existsResult.data
                    is CloudResult.Error -> Timber.w("fileExists failed, falling back to metadata")
                }
            }

            when (client.getFileMetadata(info.idOrPath)) {
                is CloudResult.Success -> true
                is CloudResult.Error -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "Exists check failed - $path")
            false
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!supportsProtocol(path)) return@withContext Result.Error("Path must be cloud://: $path")
            val info = parseCloudUri(path) ?: return@withContext Result.Error("Failed to parse cloud path: $path")
            val client = getClientOrNull(info.provider) ?: return@withContext Result.Error("Not authenticated: ${info.provider}")

            val (parentId, folderName) = splitParentAndName(info)
            if (folderName == null) return@withContext Result.Error("Cannot infer folder name from path: $path")

            when (val result = client.createFolder(folderName, parentId.ifBlank { null })) {
                is CloudResult.Success -> Result.Success(Unit)
                is CloudResult.Error -> Result.Error("Create folder failed: ${result.message}", result.cause)
            }
        } catch (e: Exception) {
            Timber.e(e, "Create directory failed - $path")
            Result.Error("Create directory failed: ${e.message}", e)
        }
    }

    override suspend fun getFileInfo(path: String): Result<MediaFile> = withContext(Dispatchers.IO) {
        try {
            if (!supportsProtocol(path)) return@withContext Result.Error("Path must be cloud://: $path")
            val info = parseCloudUri(path) ?: return@withContext Result.Error("Failed to parse cloud path: $path")
            val client = getClientOrNull(info.provider) ?: return@withContext Result.Error("Not authenticated: ${info.provider}")

            when (val result = client.getFileMetadata(info.idOrPath)) {
                is CloudResult.Success -> {
                    val cloudFile = result.data
                    val mediaType = when {
                        cloudFile.isFolder -> MediaType.OTHER
                        cloudFile.mimeType?.startsWith("image/") == true -> MediaType.IMAGE
                        cloudFile.mimeType?.startsWith("video/") == true -> MediaType.VIDEO
                        cloudFile.mimeType?.startsWith("audio/") == true -> MediaType.AUDIO
                        else -> MediaType.OTHER
                    }

                    Result.Success(
                        MediaFile(
                            path = path,
                            name = cloudFile.name,
                            size = cloudFile.size,
                            date = Date(cloudFile.modifiedDate),
                            type = mediaType,
                            thumbnailUrl = cloudFile.thumbnailUrl
                        )
                    )
                }
                is CloudResult.Error -> Result.Error("Get file info failed: ${result.message}", result.cause)
            }
        } catch (e: Exception) {
            Timber.e(e, "Get file info failed - $path")
            Result.Error("Get file info failed: ${e.message}", e)
        }
    }

    fun supportsProtocol(path: String): Boolean {
        return path.startsWith("cloud://") || path.startsWith("cloud:/")
    }

    private suspend fun downloadCloudToLocal(
        cloudPath: String,
        localPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val info = parseCloudUri(cloudPath) ?: return Result.Error("Failed to parse cloud path: $cloudPath")
        val client = getClientOrNull(info.provider) ?: return Result.Error("Not authenticated: ${info.provider}")
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
                            val percent = if (progress.totalBytes > 0) {
                                progress.bytesTransferred.toFloat() / progress.totalBytes
                            } else 0f
                            onProgress?.invoke(percent)
                        }
                    }
                )) {
                    is CloudResult.Success -> Result.Success(localPath)
                    is CloudResult.Error -> Result.Error("Download failed: ${result.message}", result.cause)
                }
            }
        } catch (e: Exception) {
            Result.Error("Download failed: ${e.message}", e)
        }
    }

    private suspend fun uploadLocalToCloud(
        localPath: String,
        cloudDestPath: String,
        sourceFile: MediaFile,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val info = parseCloudUri(cloudDestPath) ?: return Result.Error("Failed to parse cloud destination: $cloudDestPath")
        val client = getClientOrNull(info.provider) ?: return Result.Error("Not authenticated: ${info.provider}")
        val localFile = File(localPath)
        if (!localFile.exists()) return Result.Error("Source file does not exist: $localPath")

        val (parentId, fileName) = splitParentAndName(info)
        val targetName = fileName ?: localFile.name

        val mimeType = when {
            sourceFile.type == MediaType.IMAGE -> guessMimeType(targetName, "image/")
            sourceFile.type == MediaType.VIDEO -> guessMimeType(targetName, "video/")
            sourceFile.type == MediaType.AUDIO -> guessMimeType(targetName, "audio/")
            else -> guessMimeType(targetName, "")
        }

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
                            val percent = if (progress.totalBytes > 0) {
                                progress.bytesTransferred.toFloat() / progress.totalBytes
                            } else 0f
                            onProgress?.invoke(percent)
                        }
                    }
                )) {
                    is CloudResult.Success -> Result.Success(cloudDestPath)
                    is CloudResult.Error -> Result.Error("Upload failed: ${result.message}", result.cause)
                }
            }
        } catch (e: Exception) {
            Result.Error("Upload failed: ${e.message}", e)
        }
    }

    private suspend fun copyCloudToCloud(
        source: String,
        destination: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val src = parseCloudUri(source) ?: return Result.Error("Failed to parse cloud source: $source")
        val dst = parseCloudUri(destination) ?: return Result.Error("Failed to parse cloud destination: $destination")

        if (src.provider == dst.provider) {
            val client = getClientOrNull(src.provider) ?: return Result.Error("Not authenticated: ${src.provider}")
            val (dstParent, dstName) = splitParentAndName(dst)

            return when (val copied = client.copyFile(src.idOrPath, dstParent, dstName)) {
                is CloudResult.Success -> Result.Success(destination)
                is CloudResult.Error -> Result.Error("Cloud copy failed: ${copied.message}", copied.cause)
            }
        }

        val tempFile = File.createTempFile("cloud_copy_", ".tmp", context.cacheDir)
        return try {
            when (val downloadResult = downloadCloudToLocal(source, tempFile.absolutePath, onProgress)) {
                is Result.Success -> {
                    val tempMediaFile = MediaFile(
                        path = tempFile.absolutePath,
                        name = tempFile.name,
                        size = tempFile.length(),
                        date = Date(tempFile.lastModified()),
                        type = MediaType.OTHER
                    )
                    uploadLocalToCloud(tempFile.absolutePath, destination, tempMediaFile, onProgress)
                }
                is Result.Error -> downloadResult
                Result.Loading -> Result.Error("Unexpected Loading state")
            }
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private suspend fun getClientOrNull(provider: CloudProvider): CloudStorageClient? {
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
        val fileIdForDownload: String get() = segments.lastOrNull() ?: idOrPath
    }

    private fun parseCloudUri(rawPath: String): CloudUriInfo? {
        return try {
            val normalized = if (rawPath.startsWith("cloud:/") && !rawPath.startsWith("cloud://")) {
                rawPath.replaceFirst("cloud:/", "cloud://")
            } else rawPath

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

            CloudUriInfo(provider, idOrPath, uri.pathSegments)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse cloud uri: $rawPath")
            null
        }
    }

    private fun splitParentAndName(info: CloudUriInfo): Pair<String, String?> {
        if (info.segments.isEmpty()) return "" to null
        if (info.segments.size == 1) return "" to null

        val name = info.segments.last()
        val parentSegments = info.segments.dropLast(1)

        val parentIdOrPath = when (info.provider) {
            CloudProvider.DROPBOX -> "/" + parentSegments.joinToString("/")
            else -> parentSegments.joinToString("/")
        }

        return parentIdOrPath to name
    }

    private fun guessMimeType(fileName: String, typeHint: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when {
            typeHint.startsWith("image/") || MediaExtensions.isImage(extension) -> {
                when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    "bmp" -> "image/bmp"
                    "heic" -> "image/heic"
                    else -> "image/jpeg"
                }
            }
            typeHint.startsWith("video/") || MediaExtensions.isVideo(extension) -> {
                when (extension) {
                    "mp4" -> "video/mp4"
                    "webm" -> "video/webm"
                    "mkv" -> "video/x-matroska"
                    "avi" -> "video/x-msvideo"
                    "mov" -> "video/quicktime"
                    else -> "video/mp4"
                }
            }
            typeHint.startsWith("audio/") || MediaExtensions.isAudio(extension) -> {
                when (extension) {
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "ogg" -> "audio/ogg"
                    "flac" -> "audio/flac"
                    "m4a" -> "audio/mp4"
                    else -> "audio/mpeg"
                }
            }
            else -> "application/octet-stream"
        }
    }
}
