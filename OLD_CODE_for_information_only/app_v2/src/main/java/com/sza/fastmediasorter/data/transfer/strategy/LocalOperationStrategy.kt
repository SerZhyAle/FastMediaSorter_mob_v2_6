package com.sza.fastmediasorter.data.transfer.strategy

import android.content.Context
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Strategy for local file system operations.
 * Handles standard file:// and local path operations.
 */
class LocalOperationStrategy(
    private val context: Context
) : FileOperationStrategy {
    
    override suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source)
            val destFile = File(destination)
            
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist: $source"))
            }
            
            if (destFile.exists() && !overwrite) {
                return@withContext Result.failure(Exception("Destination file already exists: $destination"))
            }
            
            // Create parent directories if needed
            destFile.parentFile?.mkdirs()
            
            // Copy file with progress tracking
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    val fileSize = sourceFile.length()
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        progressCallback?.onProgress(totalBytes, fileSize, 0L)
                    }
                }
            }
            
            Timber.d("LocalOperationStrategy: Copied ${sourceFile.name} (${destFile.length()} bytes)")
            Result.success(destination)
        } catch (e: Exception) {
            Timber.e(e, "LocalOperationStrategy: Copy failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(source: String, destination: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source)
            val destFile = File(destination)
            
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist: $source"))
            }
            
            // Try atomic rename first (same filesystem)
            if (sourceFile.renameTo(destFile)) {
                Timber.d("LocalOperationStrategy: Moved ${sourceFile.name} via rename")
                return@withContext Result.success(Unit)
            }
            
            // Fallback: copy + delete
            val copyResult = copyFile(source, destination, overwrite = true, progressCallback = null)
            if (copyResult.isFailure) {
                return@withContext Result.failure(copyResult.exceptionOrNull() ?: Exception("Copy failed"))
            }
            
            if (!sourceFile.delete()) {
                return@withContext Result.failure(Exception("Copied but failed to delete source"))
            }
            
            Timber.d("LocalOperationStrategy: Moved ${sourceFile.name} via copy+delete")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "LocalOperationStrategy: Move failed - $source -> $destination")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            
            if (!file.exists()) {
                Timber.w("LocalOperationStrategy: File does not exist for deletion: $path")
                return@withContext Result.success(Unit) // Already deleted
            }
            
            if (file.delete()) {
                Timber.d("LocalOperationStrategy: Deleted ${file.name}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file: $path"))
            }
        } catch (e: Exception) {
            Timber.e(e, "LocalOperationStrategy: Delete failed - $path")
            Result.failure(e)
        }
    }
    
    override suspend fun exists(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            Result.success(file.exists())
        } catch (e: Exception) {
            Timber.e(e, "LocalOperationStrategy: Exists check failed - $path")
            Result.failure(e)
        }
    }
    
    override fun supportsProtocol(path: String): Boolean {
        // Supports local file paths (no protocol prefix or file:// prefix)
        return !path.startsWith("smb://") &&
               !path.startsWith("sftp://") &&
               !path.startsWith("ftp://") &&
               !path.startsWith("content:/") &&
               !path.contains("://") || path.startsWith("file://")
    }
    
    override fun getProtocolName(): String = "local"
}
