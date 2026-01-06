package com.sza.fastmediasorter.data.transfer.strategy

import android.content.Context
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.data.network.model.SmbConnectionInfo
import com.sza.fastmediasorter.data.network.model.SmbResult
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import com.sza.fastmediasorter.utils.SmbPathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Strategy for SMB (Server Message Block) file operations.
 * Handles smb:// protocol operations using SmbClient.
 */
class SmbOperationStrategy(
    private val context: Context,
    private val smbClient: SmbClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : FileOperationStrategy {
    
    override suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isSourceSmb = source.startsWith("smb://")
            val isDestSmb = destination.startsWith("smb://")
            
            when {
                isSourceSmb && isDestSmb -> {
                    // SMB to SMB: buffer transfer
                    copySmbToSmb(source, destination, overwrite, progressCallback)
                }
                isSourceSmb && !isDestSmb -> {
                    // SMB to Local: download
                    downloadFromSmb(source, destination, progressCallback)
                }
                !isSourceSmb && isDestSmb -> {
                    // Local to SMB: upload
                    uploadToSmb(source, destination, overwrite, progressCallback)
                }
                else -> {
                    Result.failure(IllegalArgumentException("At least one path must be SMB"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: Copy failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(source: String, destination: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isSourceSmb = source.startsWith("smb://")
            val isDestSmb = destination.startsWith("smb://")
            
            when {
                isSourceSmb && isDestSmb -> {
                    // Try server-side move if on same share
                    val sourceInfo = SmbPathUtils.parseSmbPath(source)
                    val destInfo = SmbPathUtils.parseSmbPath(destination)
                    
                    if (sourceInfo != null && destInfo != null &&
                        sourceInfo.connectionInfo.server == destInfo.connectionInfo.server &&
                        sourceInfo.connectionInfo.shareName == destInfo.connectionInfo.shareName
                    ) {
                        // Server-side move (rename)
                        val connectionInfo = getConnectionInfo(sourceInfo)
                        val fromPath = sourceInfo.remotePath
                        val toPath = destInfo.remotePath
                        
                        when (val result = smbClient.moveFile(connectionInfo, fromPath, toPath)) {
                            is SmbResult.Success -> Result.success(Unit)
                            is SmbResult.Error -> Result.failure(Exception(result.message))
                        }
                    } else {
                        // Different shares: copy + delete
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
            Timber.e(e, "SmbOperationStrategy: Move failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (path.startsWith("smb://")) {
                val pathInfo = SmbPathUtils.parseSmbPath(path)
                    ?: return@withContext Result.failure(Exception("Failed to parse SMB path: $path"))
                
                val connectionInfo = getConnectionInfo(pathInfo)
                
                when (val result = smbClient.deleteFile(connectionInfo, pathInfo.remotePath)) {
                    is SmbResult.Success -> Result.success(Unit)
                    is SmbResult.Error -> Result.failure(Exception(result.message))
                }
            } else {
                // Handle local file deletion (for Move operations: Local -> SMB)
                try {
                    val uri = parseAndFixUri(path)
                    if (uri.scheme == "content") {
                        val deleted = try {
                            context.contentResolver.delete(uri, null, null) > 0
                        } catch (e: Exception) {
                            // Try DocumentFile if direct delete fails or for verify
                            androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.delete() == true
                        }
                        if (deleted) Result.success(Unit) else Result.failure(Exception("Failed to delete content URI: $path"))
                    } else {
                        val file = File(if (uri.scheme == "file") uri.path ?: path else path)
                        if (file.delete()) Result.success(Unit) else Result.failure(Exception("Failed to delete local file: $path"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: Delete failed - $path")
            Result.failure(e)
        }
    }
    
    // ... (exists method unchanged) ...
    override suspend fun exists(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (path.startsWith("smb://")) {
                val pathInfo = SmbPathUtils.parseSmbPath(path)
                    ?: return@withContext Result.failure(Exception("Failed to parse SMB path: $path"))
                
                val connectionInfo = getConnectionInfo(pathInfo)
                
                when (val result = smbClient.exists(connectionInfo, pathInfo.remotePath)) {
                    is SmbResult.Success -> Result.success(result.data)
                    is SmbResult.Error -> Result.failure(Exception(result.message))
                }
            } else {
                // Check local existence logic if needed, currently limiting to SMB
                Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: Exists check failed - $path")
            Result.failure(e)
        }
    }

    override fun supportsProtocol(path: String): Boolean {
        // Strict check: Only claim support for SMB paths.
        // Mixed operations (Local<->SMB) are handled by the Handler (SmbFileOperationHandler)
        // explicitly calling this strategy, OR by BaseFileOperationHandler delegating correctly.
        return path.startsWith("smb://")
    }
    
    override fun getProtocolName(): String = "smb"
    
    // ==================== Helper Methods ====================
    
    /**
     * Parses URI string and ensures it has correct format (especially for content://)
     */
    private fun parseAndFixUri(path: String): android.net.Uri {
        return if (path.startsWith("content:") && !path.startsWith("content://")) {
            // Fix malformed content URI (replace "content:/" with "content://")
            android.net.Uri.parse(path.replaceFirst("content:/", "content://"))
        } else {
            android.net.Uri.parse(path)
        }
    }

    private suspend fun downloadFromSmb(
        smbPath: String,
        localPath: String,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val pathInfo = SmbPathUtils.parseSmbPath(smbPath)
            ?: return Result.failure(Exception("Failed to parse SMB path: $smbPath"))
        
        val connectionInfo = getConnectionInfo(pathInfo)
        
        // Handle destination (Local)
        val destUri = parseAndFixUri(localPath)
        val outputStream = try {
            if (destUri.scheme == "content") {
                context.contentResolver.openOutputStream(destUri) 
                    ?: return Result.failure(Exception("Failed to open output stream for content URI: $localPath"))
            } else {
                val localFile = File(if (destUri.scheme == "file") destUri.path ?: localPath else localPath)
                localFile.parentFile?.mkdirs()
                FileOutputStream(localFile)
            }
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to open local destination: ${e.message}"))
        }
        
        return try {
            outputStream.use { stream ->
                when (val result = smbClient.downloadFile(
                    connectionInfo,
                    pathInfo.remotePath,
                    stream,
                    fileSize = 0L, // Unknown size
                    progressCallback = progressCallback
                )) {
                    is SmbResult.Success -> {
                        Timber.d("SmbOperationStrategy: Downloaded to $localPath")
                        Result.success(localPath)
                    }
                    is SmbResult.Error -> Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: Download failed")
            Result.failure(e)
        }
    }
    
    private suspend fun uploadToSmb(
        localPath: String,
        smbPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val pathInfo = SmbPathUtils.parseSmbPath(smbPath)
            ?: return Result.failure(Exception("Failed to parse SMB path: $smbPath"))
        
        val connectionInfo = getConnectionInfo(pathInfo)
        
        // Check if destination exists and overwrite flag
        if (!overwrite) {
            when (val existsResult = smbClient.exists(connectionInfo, pathInfo.remotePath)) {
                is SmbResult.Success -> {
                    if (existsResult.data) {
                        return Result.failure(Exception("Destination file already exists: $smbPath"))
                    }
                }
                is SmbResult.Error -> {
                    Timber.w("Failed to check if destination exists: ${existsResult.message}")
                }
            }
        }
        
        val sourceUri = parseAndFixUri(localPath)
        
        return try {
            val (inputStream, size) = if (sourceUri.scheme == "content") {
                val stream = context.contentResolver.openInputStream(sourceUri) 
                    ?: return Result.failure(Exception("No content provider: $localPath"))
                
                // Try to get size
                val fileSize = try {
                    context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { it.statSize } ?: -1L
                } catch (e: Exception) { -1L }
                
                Pair(stream, fileSize)
            } else {
                val file = File(if (sourceUri.scheme == "file") sourceUri.path ?: localPath else localPath)
                if (!file.exists()) {
                    return Result.failure(Exception("Source file does not exist: $localPath"))
                }
                Pair(FileInputStream(file), file.length())
            }

            inputStream.use { stream ->
                when (val result = smbClient.uploadFile(
                    connectionInfo,
                    pathInfo.remotePath,
                    stream,
                    fileSize = size,
                    progressCallback = progressCallback
                )) {
                    is SmbResult.Success -> {
                        Timber.d("SmbOperationStrategy: Uploaded $localPath")
                        Result.success(smbPath)
                    }
                    is SmbResult.Error -> Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: Upload failed")
            Result.failure(e)
        }
    }
    
    private suspend fun copySmbToSmb(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val sourceInfo = SmbPathUtils.parseSmbPath(sourcePath)
            ?: return Result.failure(Exception("Failed to parse source SMB path: $sourcePath"))
        
        val destInfo = SmbPathUtils.parseSmbPath(destPath)
            ?: return Result.failure(Exception("Failed to parse destination SMB path: $destPath"))
        
        val sourceConnectionInfo = getConnectionInfo(sourceInfo)
        val destConnectionInfo = getConnectionInfo(destInfo)
        
        // Check if destination exists and overwrite flag
        if (!overwrite) {
            when (val existsResult = smbClient.exists(destConnectionInfo, destInfo.remotePath)) {
                is SmbResult.Success -> {
                    if (existsResult.data) {
                        return Result.failure(Exception("Destination file already exists: $destPath"))
                    }
                }
                is SmbResult.Error -> {
                    Timber.w("Failed to check if destination exists: ${existsResult.message}")
                }
            }
        }
        
        // Buffer transfer: download to memory → upload
        return try {
            val buffer = ByteArrayOutputStream()
            
            // Download to buffer
            when (val downloadResult = smbClient.downloadFile(
                sourceConnectionInfo,
                sourceInfo.remotePath,
                buffer,
                fileSize = 0L,
                progressCallback = null // No progress for first half
            )) {
                is SmbResult.Error -> return Result.failure(Exception("Download failed: ${downloadResult.message}"))
                is SmbResult.Success -> {
                    // Upload from buffer
                    val inputStream = ByteArrayInputStream(buffer.toByteArray())
                    when (val uploadResult = smbClient.uploadFile(
                        destConnectionInfo,
                        destInfo.remotePath,
                        inputStream,
                        fileSize = buffer.size().toLong(),
                        progressCallback = progressCallback
                    )) {
                        is SmbResult.Success -> {
                            Timber.d("SmbOperationStrategy: Copied SMB→SMB (${buffer.size()} bytes)")
                            Result.success(destPath)
                        }
                        is SmbResult.Error -> Result.failure(Exception("Upload failed: ${uploadResult.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SmbOperationStrategy: SMB→SMB copy failed")
            Result.failure(e)
        }
    }
    
    private suspend fun getConnectionInfo(pathInfo: SmbPathUtils.SmbPathInfo): SmbConnectionInfo {
        // Try to get credentials from repository
        val credentials = credentialsRepository.getByServerAndShare(
            pathInfo.connectionInfo.server,
            pathInfo.connectionInfo.shareName
        )
        
        // Return existing connectionInfo with credentials if found, or original if not
        return if (credentials != null) {
            pathInfo.connectionInfo.copy(
                username = credentials.username,
                password = credentials.password,
                domain = credentials.domain
            )
        } else {
            pathInfo.connectionInfo
        }
    }
}
