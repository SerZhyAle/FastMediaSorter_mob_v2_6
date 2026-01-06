package com.sza.fastmediasorter.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.cloud.CloudFileOperationHandler
import com.sza.fastmediasorter.data.network.SmbFileOperationHandler
import com.sza.fastmediasorter.domain.transfer.FileOperationError
import com.sza.fastmediasorter.data.network.SftpFileOperationHandler
import com.sza.fastmediasorter.data.network.FtpFileOperationHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

sealed class FileOperation {
    data class Copy(val sources: List<File>, val destination: File, val overwrite: Boolean, val sourceCredentialsId: String? = null) : FileOperation()
    data class Move(val sources: List<File>, val destination: File, val overwrite: Boolean, val sourceCredentialsId: String? = null) : FileOperation()
    data class Rename(val file: File, val newName: String) : FileOperation()
    data class Delete(val files: List<File>, val softDelete: Boolean = true) : FileOperation() // softDelete: move to trash instead of permanent delete
}

sealed class FileOperationResult {
    data class Success(
        val processedCount: Int, 
        val operation: FileOperation,
        val copiedFilePaths: List<String> = emptyList() // Paths of destination files for undo
    ) : FileOperationResult()
    data class PartialSuccess(val processedCount: Int, val failedCount: Int, val errors: List<String>) : FileOperationResult()
    data class Failure(
        val error: String,
        val errorRes: Int? = null,
        val formatArgs: List<Any> = emptyList()
    ) : FileOperationResult()
    
    /**
     * Cloud provider requires re-authentication
     * UI should prompt user to re-authenticate via AddResourceActivity
     */
    data class AuthenticationRequired(val provider: String, val message: String) : FileOperationResult()
}

/**
 * Progress updates for file operations
 */
sealed class FileOperationProgress {
    data class Starting(val operation: FileOperation, val totalFiles: Int) : FileOperationProgress()
    data class Processing(
        val currentFile: String,
        val currentIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSecond: Long = 0L
    ) : FileOperationProgress()
    data class Completed(val result: FileOperationResult) : FileOperationProgress()
}

data class OperationHistory(
    val operation: FileOperation,
    val result: FileOperationResult,
    val timestamp: Long = System.currentTimeMillis()
)

class FileOperationUseCase @Inject constructor(
    private val context: Context,
    private val smbFileOperationHandler: SmbFileOperationHandler,
    private val sftpFileOperationHandler: SftpFileOperationHandler,
    private val ftpFileOperationHandler: FtpFileOperationHandler,
    private val cloudFileOperationHandler: CloudFileOperationHandler
) {
    
    private var lastOperation: OperationHistory? = null
    
    /**
     * Execute file operation with progress updates emitted via Flow
     * Use this method when you need to show progress UI during long operations
     * Supports cancellation via coroutine job cancellation
     */
    fun executeWithProgress(operation: FileOperation): Flow<FileOperationProgress> = channelFlow {
        Timber.d("FileOperation.executeWithProgress: Starting ${operation.javaClass.simpleName}")
        
        val totalFiles = when (operation) {
            is FileOperation.Copy -> operation.sources.size
            is FileOperation.Move -> operation.sources.size
            is FileOperation.Delete -> operation.files.size
            is FileOperation.Rename -> 1
        }
        
        send(FileOperationProgress.Starting(operation, totalFiles))
        
        // Update current file tracking based on operation type
        var currentFileIndex = 1
        var currentFileName = when (operation) {
            is FileOperation.Copy -> operation.sources.firstOrNull()?.name ?: ""
            is FileOperation.Move -> operation.sources.firstOrNull()?.name ?: ""
            is FileOperation.Delete -> operation.files.firstOrNull()?.name ?: ""
            is FileOperation.Rename -> operation.file.name
        }
        
        // Create progress callback that sends to channel (thread-safe)
        val progressCallback = object : ByteProgressCallback {
            override suspend fun onProgress(bytesTransferred: Long, totalBytes: Long, speedBytesPerSecond: Long) {
                // Use trySend to avoid blocking if channel is full
                trySend(FileOperationProgress.Processing(
                    currentFile = currentFileName,
                    currentIndex = currentFileIndex,
                    totalFiles = totalFiles,
                    bytesTransferred = bytesTransferred,
                    totalBytes = totalBytes,
                    speedBytesPerSecond = speedBytesPerSecond
                ))
            }
        }
        
        // Execute operation in separate coroutine to allow progress updates
        val resultDeferred = launch(Dispatchers.IO) {
            val result = executeInternal(operation, progressCallback)
            send(FileOperationProgress.Completed(result))
        }
        
        // Wait for completion
        resultDeferred.join()
    }
    
    /**
     * Internal execution without withContext (called from flow with flowOn)
     */
    private suspend fun executeInternal(
        operation: FileOperation,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult {
        Timber.d("FileOperation: Starting operation: ${operation.javaClass.simpleName}")
        
        try {
            // Helper to check if path is network resource (use path instead of absolutePath to avoid /prefix)
            fun File.isNetworkPath(protocol: String): Boolean {
                val pathStr = this.path
                val result = pathStr.startsWith("$protocol://") || 
                             pathStr.startsWith("/$protocol://") || 
                             pathStr.startsWith("/$protocol:/") ||
                             pathStr.startsWith("$protocol:/")  // Single colon case
                Timber.d("FileOperation.isNetworkPath: path='$pathStr', protocol='$protocol', result=$result")
                return result
            }
            
            // Check if operation involves SMB or SFTP paths
            val hasSmbPath = when (operation) {
                is FileOperation.Copy -> {
                    val sourceSmbCount = operation.sources.count { it.isNetworkPath("smb") }
                    val destIsSmb = operation.destination.isNetworkPath("smb")
                    Timber.d("FileOperation.Copy: sources=$sourceSmbCount/${operation.sources.size} SMB, dest=${if (destIsSmb) "SMB" else "Local"}")
                    sourceSmbCount > 0 || destIsSmb
                }
                is FileOperation.Move -> {
                    val sourceSmbCount = operation.sources.count { it.isNetworkPath("smb") }
                    val destIsSmb = operation.destination.isNetworkPath("smb")
                    Timber.d("FileOperation.Move: sources=$sourceSmbCount/${operation.sources.size} SMB, dest=${if (destIsSmb) "SMB" else "Local"}")
                    sourceSmbCount > 0 || destIsSmb
                }
                is FileOperation.Delete -> {
                    val smbCount = operation.files.count { it.isNetworkPath("smb") }
                    Timber.d("FileOperation.Delete: $smbCount/${operation.files.size} SMB files")
                    smbCount > 0
                }
                is FileOperation.Rename -> {
                    val isSmb = operation.file.isNetworkPath("smb")
                    Timber.d("FileOperation.Rename: file=${if (isSmb) "SMB" else "Local"}")
                    isSmb
                }
            }

            val hasSftpPath = when (operation) {
                is FileOperation.Copy -> {
                    val sourceSftpCount = operation.sources.count { it.isNetworkPath("sftp") }
                    val destIsSftp = operation.destination.isNetworkPath("sftp")
                    Timber.d("FileOperation.Copy: sources=$sourceSftpCount/${operation.sources.size} SFTP, dest=${if (destIsSftp) "SFTP" else "Local"}")
                    sourceSftpCount > 0 || destIsSftp
                }
                is FileOperation.Move -> {
                    val sourceSftpCount = operation.sources.count { it.isNetworkPath("sftp") }
                    val destIsSftp = operation.destination.isNetworkPath("sftp")
                    Timber.d("FileOperation.Move: sources=$sourceSftpCount/${operation.sources.size} SFTP, dest=${if (destIsSftp) "SFTP" else "Local"}")
                    sourceSftpCount > 0 || destIsSftp
                }
                is FileOperation.Delete -> {
                    val sftpCount = operation.files.count { it.isNetworkPath("sftp") }
                    Timber.d("FileOperation.Delete: $sftpCount/${operation.files.size} SFTP files")
                    sftpCount > 0
                }
                is FileOperation.Rename -> {
                    val isSftp = operation.file.isNetworkPath("sftp")
                    Timber.d("FileOperation.Rename: file=${if (isSftp) "SFTP" else "Local"}")
                    isSftp
                }
            }

            val hasFtpPath = when (operation) {
                is FileOperation.Copy -> {
                    val sourceFtpCount = operation.sources.count { it.isNetworkPath("ftp") }
                    val destIsFtp = operation.destination.isNetworkPath("ftp")
                    Timber.d("FileOperation.Copy: sources=$sourceFtpCount/${operation.sources.size} FTP, dest=${if (destIsFtp) "FTP" else "Local"}")
                    sourceFtpCount > 0 || destIsFtp
                }
                is FileOperation.Move -> {
                    val sourceFtpCount = operation.sources.count { it.isNetworkPath("ftp") }
                    val destIsFtp = operation.destination.isNetworkPath("ftp")
                    Timber.d("FileOperation.Move: sources=$sourceFtpCount/${operation.sources.size} FTP, dest=${if (destIsFtp) "FTP" else "Local"}")
                    sourceFtpCount > 0 || destIsFtp
                }
                is FileOperation.Delete -> {
                    val ftpCount = operation.files.count { it.isNetworkPath("ftp") }
                    Timber.d("FileOperation.Delete: $ftpCount/${operation.files.size} FTP files")
                    ftpCount > 0
                }
                is FileOperation.Rename -> {
                    val isFtp = operation.file.isNetworkPath("ftp")
                    Timber.d("FileOperation.Rename: file=${if (isFtp) "FTP" else "Local"}")
                    isFtp
                }
            }

            val hasCloudPath = when (operation) {
                is FileOperation.Copy -> {
                    val sourceCloudCount = operation.sources.count { it.isNetworkPath("cloud") }
                    val destIsCloud = operation.destination.isNetworkPath("cloud")
                    Timber.d("FileOperation.Copy: sources=$sourceCloudCount/${operation.sources.size} Cloud, dest=${if (destIsCloud) "Cloud" else "Local"}")
                    sourceCloudCount > 0 || destIsCloud
                }
                is FileOperation.Move -> {
                    val sourceCloudCount = operation.sources.count { it.isNetworkPath("cloud") }
                    val destIsCloud = operation.destination.isNetworkPath("cloud")
                    Timber.d("FileOperation.Move: sources=$sourceCloudCount/${operation.sources.size} Cloud, dest=${if (destIsCloud) "Cloud" else "Local"}")
                    sourceCloudCount > 0 || destIsCloud
                }
                is FileOperation.Delete -> {
                    val cloudCount = operation.files.count { it.isNetworkPath("cloud") }
                    Timber.d("FileOperation.Delete: $cloudCount/${operation.files.size} Cloud files")
                    cloudCount > 0
                }
                is FileOperation.Rename -> {
                    val isCloud = operation.file.isNetworkPath("cloud")
                    Timber.d("FileOperation.Rename: file=${if (isCloud) "Cloud" else "Local"}")
                    isCloud
                }
            }

            val result = when {
                hasCloudPath -> {
                    Timber.d("FileOperation: Using Cloud handler")
                    // Use Cloud handler for operations involving cloud paths
                    when (operation) {
                        is FileOperation.Copy -> cloudFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> cloudFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> cloudFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> cloudFileOperationHandler.executeRename(operation)
                    }
                }
                hasSmbPath && hasSftpPath -> {
                    // Mixed operation SMB↔SFTP: use destination protocol as priority
                    val useSmb = when (operation) {
                        is FileOperation.Copy -> operation.destination.isNetworkPath("smb")
                        is FileOperation.Move -> operation.destination.isNetworkPath("smb")
                        else -> hasSmbPath // For Delete/Rename, use first detected protocol
                    }
                    
                    if (useSmb) {
                        Timber.d("FileOperation: Mixed SMB↔SFTP - using SMB handler (dest=SMB)")
                        when (operation) {
                            is FileOperation.Copy -> smbFileOperationHandler.executeCopy(operation, progressCallback)
                            is FileOperation.Move -> smbFileOperationHandler.executeMove(operation, progressCallback)
                            is FileOperation.Delete -> smbFileOperationHandler.executeDelete(operation)
                            is FileOperation.Rename -> smbFileOperationHandler.executeRename(operation)
                        }
                    } else {
                        Timber.d("FileOperation: Mixed SMB↔SFTP - using SFTP handler (dest=SFTP)")
                        when (operation) {
                            is FileOperation.Copy -> sftpFileOperationHandler.executeCopy(operation, progressCallback)
                            is FileOperation.Move -> sftpFileOperationHandler.executeMove(operation, progressCallback)
                            is FileOperation.Delete -> sftpFileOperationHandler.executeDelete(operation)
                            is FileOperation.Rename -> sftpFileOperationHandler.executeRename(operation)
                        }
                    }
                }
                hasSmbPath && hasFtpPath -> {
                    // Mixed operation SMB↔FTP: FTP doesn't support cross-protocol, use SMB handler to download first
                    Timber.d("FileOperation: Mixed SMB↔FTP - using SMB handler (FTP can't handle cross-protocol)")
                    when (operation) {
                        is FileOperation.Copy -> smbFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> smbFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> smbFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> smbFileOperationHandler.executeRename(operation)
                    }
                }
                hasSftpPath && hasFtpPath -> {
                    // Mixed operation SFTP↔FTP: FTP doesn't support cross-protocol, use SFTP handler to download first
                    Timber.d("FileOperation: Mixed SFTP↔FTP - using SFTP handler (FTP can't handle cross-protocol)")
                    when (operation) {
                        is FileOperation.Copy -> sftpFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> sftpFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> sftpFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> sftpFileOperationHandler.executeRename(operation)
                    }
                }
                hasFtpPath -> {
                    Timber.d("FileOperation: Using FTP handler")
                    // Use FTP handler for operations involving FTP paths (local↔FTP or FTP↔FTP)
                    when (operation) {
                        is FileOperation.Copy -> ftpFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> ftpFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> ftpFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> ftpFileOperationHandler.executeRename(operation)
                    }
                }
                hasSmbPath -> {
                    Timber.d("FileOperation: Using SMB handler")
                    // Use SMB handler for operations involving SMB paths
                    when (operation) {
                        is FileOperation.Copy -> smbFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> smbFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> smbFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> smbFileOperationHandler.executeRename(operation)
                    }
                }
                hasSftpPath -> {
                    Timber.d("FileOperation: Using SFTP handler")
                    // Use SFTP handler for operations involving SFTP paths
                    when (operation) {
                        is FileOperation.Copy -> sftpFileOperationHandler.executeCopy(operation, progressCallback)
                        is FileOperation.Move -> sftpFileOperationHandler.executeMove(operation, progressCallback)
                        is FileOperation.Delete -> sftpFileOperationHandler.executeDelete(operation)
                        is FileOperation.Rename -> sftpFileOperationHandler.executeRename(operation)
                    }
                }
                else -> {
                    Timber.d("FileOperation: Using local file operations")
                    // Use local file operations
                    when (operation) {
                        is FileOperation.Copy -> executeCopy(operation)
                        is FileOperation.Move -> executeMove(operation)
                        is FileOperation.Rename -> executeRename(operation)
                        is FileOperation.Delete -> executeDelete(operation)
                    }
                }
            }
            
            when (result) {
                is FileOperationResult.Success -> Timber.i("FileOperation: SUCCESS - processed ${result.processedCount} files")
                is FileOperationResult.PartialSuccess -> Timber.w("FileOperation: PARTIAL SUCCESS - ${result.processedCount} ok, ${result.failedCount} failed. Errors: ${result.errors}")
                is FileOperationResult.Failure -> Timber.e("FileOperation: FAILURE - ${result.error}")
                is FileOperationResult.AuthenticationRequired -> Timber.w("FileOperation: AUTH REQUIRED - ${result.provider}: ${result.message}")
            }
            
            lastOperation = OperationHistory(operation, result)
            return result
            
        } catch (e: Exception) {
            Timber.e(e, "FileOperation: EXCEPTION in executeInternal()")
            return FileOperationResult.Failure("${e.javaClass.simpleName}: ${e.message}")
        }
    }
    
    suspend fun execute(
        operation: FileOperation,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult = withContext(Dispatchers.IO) {
        executeInternal(operation, progressCallback)
    }
    
    private fun executeCopy(operation: FileOperation.Copy): FileOperationResult {
        Timber.d("executeCopy: Starting local copy of ${operation.sources.size} files to ${operation.destination.absolutePath}")
        
        val errors = mutableListOf<String>()
        val copiedPaths = mutableListOf<String>()
        var successCount = 0
        
        operation.sources.forEachIndexed { index, source ->
            Timber.d("executeCopy: [${index + 1}/${operation.sources.size}] Processing ${source.name}")
            
            val sourcePath = source.path
            val isContentUri = sourcePath.startsWith("content:/")
            
            try {
                if (isContentUri) {
                    // Handle SAF source: copy via ContentResolver
                    val normalizedUri = if (sourcePath.startsWith("content://")) sourcePath 
                                       else sourcePath.replaceFirst("content:/", "content://")
                    val uri = Uri.parse(normalizedUri)
                    
                    // Extract clean filename from URI
                    val fileName = try {
                        val decoded = Uri.decode(sourcePath)
                        decoded.substringAfterLast("/").substringAfterLast("%2F")
                    } catch (e: Exception) {
                        source.name
                    }
                    
                    val destFile = File(operation.destination, fileName)
                    
                    if (destFile.exists() && !operation.overwrite) {
                        val destinationName = destFile.parentFile?.name ?: operation.destination.name
                        val error = context.getString(R.string.file_already_exists_in_folder, fileName, destinationName)
                        Timber.w("executeCopy: File already exists - $fileName in $destinationName")
                        errors.add(error)
                        return@forEachIndexed
                    }
                    
                    val startTime = System.currentTimeMillis()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IOException("Failed to open SAF URI")
                    
                    val duration = System.currentTimeMillis() - startTime
                    copiedPaths.add(destFile.absolutePath)
                    successCount++
                    Timber.i("executeCopy: SUCCESS - SAF $fileName copied in ${duration}ms")
                    return@forEachIndexed
                }
                
                // Regular file path handling
                val destFile = File(operation.destination, source.name)
                
                if (!source.exists()) {
                    val error = "${source.name}\n  Source: ${source.absolutePath}\n  Error: File not found"
                    Timber.e("executeCopy: $error")
                    errors.add(error)
                    return@forEachIndexed
                }
                
                Timber.d("executeCopy: Target: ${destFile.absolutePath}, size=${source.length()} bytes")
                
                if (destFile.exists() && !operation.overwrite) {
                    val destinationName = destFile.parentFile?.name ?: operation.destination.name
                    val error = context.getString(R.string.file_already_exists_in_folder, source.name, destinationName)
                    Timber.w("executeCopy: File already exists - ${source.name} in $destinationName")
                    errors.add(error)
                    return@forEachIndexed
                }
                
                val startTime = System.currentTimeMillis()
                source.copyTo(destFile, operation.overwrite)
                val duration = System.currentTimeMillis() - startTime
                
                copiedPaths.add(destFile.absolutePath)
                successCount++
                Timber.i("executeCopy: SUCCESS - ${source.name} copied in ${duration}ms")
                
            } catch (e: Exception) {
                val error = FileOperationError.formatTransferError(
                    source.name,
                    source.absolutePath,
                    operation.destination.absolutePath,
                    FileOperationError.extractErrorMessage(e)
                )
                Timber.e(e, "executeCopy: ERROR - $error")
                errors.add(error)
            }
        }
        
        val result = when {
            successCount == operation.sources.size -> {
                Timber.i("executeCopy: All $successCount files copied successfully")
                FileOperationResult.Success(successCount, operation, copiedPaths)
            }
            successCount > 0 -> {
                Timber.w("executeCopy: Partial success - $successCount/${operation.sources.size} files copied. Errors: $errors")
                FileOperationResult.PartialSuccess(successCount, errors.size, errors)
            }
            else -> {
                Timber.e("executeCopy: All copy operations failed. Errors: $errors")
                val errorMessage = errors.joinToString("\n")
                FileOperationResult.Failure(
                    error = context.getString(R.string.all_copy_operations_failed, errorMessage),
                    errorRes = R.string.all_copy_operations_failed,
                    formatArgs = listOf(errorMessage)
                )
            }
        }
        
        return result
    }
    
    private fun executeMove(operation: FileOperation.Move): FileOperationResult {
        Timber.d("executeMove: Starting local move of ${operation.sources.size} files to ${operation.destination.absolutePath}")
        
        val errors = mutableListOf<String>()
        val movedPaths = mutableListOf<String>()
        var successCount = 0
        
        operation.sources.forEachIndexed { index, source ->
            Timber.d("executeMove: [${index + 1}/${operation.sources.size}] Processing ${source.name}")
            
            val sourcePath = source.path
            val isContentUri = sourcePath.startsWith("content:/")
            
            try {
                if (isContentUri) {
                    // Handle SAF source: copy via ContentResolver then delete
                    val normalizedUri = if (sourcePath.startsWith("content://")) sourcePath 
                                       else sourcePath.replaceFirst("content:/", "content://")
                    val uri = Uri.parse(normalizedUri)
                    
                    // Extract clean filename from URI
                    val fileName = try {
                        val decoded = Uri.decode(sourcePath)
                        decoded.substringAfterLast("/").substringAfterLast("%2F")
                    } catch (e: Exception) {
                        source.name
                    }
                    
                    val destFile = File(operation.destination, fileName)
                    
                    if (destFile.exists() && !operation.overwrite) {
                        val destinationName = destFile.parentFile?.name ?: operation.destination.name
                        val error = context.getString(R.string.file_already_exists_in_folder, fileName, destinationName)
                        Timber.w("executeMove: File already exists - $fileName in $destinationName")
                        errors.add(error)
                        return@forEachIndexed
                    }
                    
                    val startTime = System.currentTimeMillis()
                    
                    // Copy from SAF to destination
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IOException("Failed to open SAF URI")
                    
                    val copyDuration = System.currentTimeMillis() - startTime
                    Timber.d("executeMove: SAF copy completed in ${copyDuration}ms, attempting delete")
                    
                    // Delete source SAF file
                    val deleted = try {
                        context.contentResolver.delete(uri, null, null) > 0 ||
                            DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } catch (e: Exception) {
                        Timber.w(e, "executeMove: Failed to delete SAF source")
                        false
                    }
                    
                    if (deleted) {
                        val totalDuration = System.currentTimeMillis() - startTime
                        movedPaths.add(destFile.absolutePath)
                        successCount++
                        Timber.i("executeMove: SUCCESS - SAF $fileName moved in ${totalDuration}ms")
                    } else {
                        // File was copied but not deleted - treat as partial success
                        val totalDuration = System.currentTimeMillis() - startTime
                        movedPaths.add(destFile.absolutePath)
                        successCount++
                        Timber.w("executeMove: SAF $fileName copied in ${totalDuration}ms but source delete failed - manual cleanup needed")
                    }
                    return@forEachIndexed
                }
                
                // Regular file path handling
                val destFile = File(operation.destination, source.name)
                
                if (!source.exists()) {
                    val error = "${source.name}\n  Source: ${source.absolutePath}\n  Error: File not found"
                    Timber.e("executeMove: $error")
                    errors.add(error)
                    return@forEachIndexed
                }
                
                Timber.d("executeMove: Moving ${source.absolutePath} to ${destFile.absolutePath}")
                
                if (destFile.exists() && !operation.overwrite) {
                    val destinationName = destFile.parentFile?.name ?: operation.destination.name
                    val error = context.getString(R.string.file_already_exists_in_folder, source.name, destinationName)
                    Timber.w("executeMove: File already exists - ${source.name} in $destinationName")
                    errors.add(error)
                    return@forEachIndexed
                }
                
                val startTime = System.currentTimeMillis()
                
                // Try rename first (faster for same filesystem)
                // CRITICAL: renameTo silently overwrites on Android, so check first
                if (!destFile.exists() && source.renameTo(destFile)) {
                    val duration = System.currentTimeMillis() - startTime
                    movedPaths.add(destFile.absolutePath)
                    successCount++
                    Timber.i("executeMove: SUCCESS via rename - ${source.name} moved in ${duration}ms")
                } else {
                    Timber.d("executeMove: Rename failed, trying copy+delete for ${source.name}")
                    
                    source.copyTo(destFile, operation.overwrite)
                    val copyDuration = System.currentTimeMillis() - startTime
                    Timber.d("executeMove: Copy completed in ${copyDuration}ms, attempting delete")
                    
                    if (source.delete()) {
                        val totalDuration = System.currentTimeMillis() - startTime
                        movedPaths.add(destFile.absolutePath)
                        successCount++
                        Timber.i("executeMove: SUCCESS via copy+delete - ${source.name} moved in ${totalDuration}ms")
                    } else {
                        val error = FileOperationError.formatTransferError(
                            source.name,
                            source.absolutePath,
                            destFile.absolutePath,
                            "Failed to delete source after copy"
                        )
                        Timber.e("executeMove: $error - copied file remains at ${destFile.absolutePath}")
                        errors.add(error)
                    }
                }
                
            } catch (e: Exception) {
                val error = FileOperationError.formatTransferError(
                    source.name,
                    source.absolutePath,
                    File(operation.destination, source.name).absolutePath,
                    FileOperationError.extractErrorMessage(e)
                )
                Timber.e(e, "executeMove: ERROR - $error")
                errors.add(error)
            }
        }
        
        val result = when {
            successCount == operation.sources.size -> {
                Timber.i("executeMove: All $successCount files moved successfully")
                FileOperationResult.Success(successCount, operation, movedPaths)
            }
            successCount > 0 -> {
                Timber.w("executeMove: Partial success - $successCount/${operation.sources.size} files moved. Errors: $errors")
                FileOperationResult.PartialSuccess(successCount, errors.size, errors)
            }
            else -> {
                Timber.e("executeMove: All move operations failed. Errors: $errors")
                val errorMessage = errors.joinToString("\n")
                FileOperationResult.Failure(
                    error = context.getString(R.string.all_move_operations_failed, errorMessage),
                    errorRes = R.string.all_move_operations_failed,
                    formatArgs = listOf(errorMessage)
                )
            }
        }
        
        return result
    }
    
    private fun executeRename(operation: FileOperation.Rename): FileOperationResult {
        try {
            val filePath = operation.file.path
            
            // Check if this is a SAF/content URI
            if (filePath.startsWith("content:/")) {
                val normalizedUri = if (filePath.startsWith("content://")) filePath 
                                   else filePath.replaceFirst("content:/", "content://")
                val uri = Uri.parse(normalizedUri)
                
                return try {
                    val newUri = DocumentsContract.renameDocument(context.contentResolver, uri, operation.newName)
                    if (newUri != null) {
                        FileOperationResult.Success(1, operation, listOf(newUri.toString()))
                    } else {
                        FileOperationResult.Failure("Failed to rename SAF document: ${operation.file.name}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "SAF rename failed")
                    FileOperationResult.Failure("SAF rename error: ${e.message}")
                }
            }
            
            // Regular file path handling
            if (!operation.file.exists()) {
                return FileOperationResult.Failure("File not found: ${operation.file.name}")
            }
            
            // For network paths (SMB/S/FTP), manually construct new path
            // filePath already declared above
            val newFile = if (filePath.startsWith("smb://") || filePath.startsWith("sftp://") || filePath.startsWith("ftp://")) {
                val lastSlashIndex = filePath.lastIndexOf('/')
                val parentPath = filePath.substring(0, lastSlashIndex)
                val newPath = "$parentPath/${operation.newName}"
                object : File(newPath) {
                    override fun getPath(): String = newPath
                    override fun getAbsolutePath(): String = newPath
                }
            } else {
                File(operation.file.parent, operation.newName)
            }
            
            if (newFile.exists()) {
                return FileOperationResult.Failure(context.getString(R.string.file_already_exists, operation.newName))
            }
            
            if (operation.file.renameTo(newFile)) {
                return FileOperationResult.Success(1, operation, listOf(newFile.absolutePath))
            } else {
                return FileOperationResult.Failure("Failed to rename ${operation.file.name}")
            }
            
        } catch (e: Exception) {
            return FileOperationResult.Failure("Rename error: ${e.message}")
        }
    }
    
    private suspend fun executeDelete(operation: FileOperation.Delete): FileOperationResult = withContext(Dispatchers.IO) {
        val errors = Collections.synchronizedList(mutableListOf<String>())
        val deletedPaths = Collections.synchronizedList(mutableListOf<String>())
        val trashedPaths = Collections.synchronizedList(mutableListOf<String>())
        val successCount = AtomicInteger(0)
        
        // CRITICAL: Soft delete only works for LOCAL files!
        // For Cloud/Network/SAF resources, trash folder creation via File() API is impossible.
        // Cloud paths like "cloud://google/fileId", network paths like "smb://server/share/file",
        // and SAF URIs like "content://..." cannot be used with File(parent, ".trash").
        if (operation.softDelete && operation.files.any { file ->
            val path = file.path
            path.startsWith("cloud://") || path.startsWith("smb://") || 
            path.startsWith("sftp://") || path.startsWith("ftp://") ||
            path.startsWith("content:/") // SAF URIs
        }) {
            Timber.e("Soft delete not supported for Cloud/Network/SAF resources")
            return@withContext FileOperationResult.Failure("Soft delete (trash) is only supported for local files. Use hard delete for Cloud/Network/SAF resources.")
        }
        
        // For soft delete, create a trash folder for each parent directory.
        // This avoids cross-directory/volume rename issues when files come from different folders.
        val trashDirs: Map<File?, File?> = if (operation.softDelete && operation.files.isNotEmpty()) {
            operation.files.map { it.parentFile }.distinct().associateWith { parent ->
                if (parent == null) return@associateWith null
                // Use unique timestamp per parent to avoid conflicts
                val trash = File(parent, ".trash_${System.nanoTime()}_${Thread.currentThread().id}")
                if (!trash.exists()) {
                    val created = trash.mkdirs()
                    if (!created) {
                        Timber.w("Failed to create trash directory: ${trash.absolutePath}")
                        null
                    } else {
                        Timber.d("Created trash directory: ${trash.absolutePath}")
                        trash
                    }
                } else {
                    trash
                }
            }
        } else {
            emptyMap()
        }
        
        // Validate trash directories for soft delete
        if (operation.softDelete && trashDirs.values.all { it == null } && operation.files.isNotEmpty()) {
            Timber.e("Soft delete requested but no trash directories could be created")
            return@withContext FileOperationResult.Failure("Failed to create trash directory(ies) for soft delete")
        }
        
        // Process files in smaller batches with delays to avoid overwhelming filesystem
        // Reduced from 20 to 5 for better stability, especially on SMB and older devices
        val batchSize = 5
        val totalFiles = operation.files.size
        
        operation.files.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            Timber.d("Processing delete batch ${batchIndex + 1}/${(totalFiles + batchSize - 1) / batchSize}, files: ${successCount.get()}/$totalFiles")
            
            batch.map { file ->
                async {
                    // pick trash dir specific to file's parent, fallback to null
                    val parent = file.parentFile
                    val fileTrash = trashDirs[parent]
                    deleteFileWithRetry(file, operation.softDelete, fileTrash, trashedPaths, deletedPaths, successCount, errors)
                }
            }.awaitAll()
            
            // Add delay between batches to let filesystem recover (except for last batch)
            if (batchIndex < operation.files.size / batchSize) {
                delay(150L) // 150ms pause between batches
            }
        }
        
        // Return trash directory paths (one per parent) in copiedFilePaths for undo restoration
        val trashPathsList = if (operation.softDelete) {
            trashDirs.values.filterNotNull().map { it.absolutePath }
        } else emptyList()

        val resultPaths = if (operation.softDelete && trashPathsList.isNotEmpty()) {
            trashPathsList + trashedPaths
        } else {
            deletedPaths
        }
        
        return@withContext when {
            successCount.get() == operation.files.size -> FileOperationResult.Success(successCount.get(), operation, resultPaths)
            successCount.get() > 0 -> FileOperationResult.PartialSuccess(successCount.get(), errors.size, errors)
            else -> {
                val errorMessage = errors.joinToString("; ")
                FileOperationResult.Failure(
                    error = "All delete operations failed: $errorMessage",
                    errorRes = R.string.all_delete_operations_failed,
                    formatArgs = listOf(errorMessage)
                )
            }
        }
    }
    
    private suspend fun deleteFileWithRetry(
        file: File,
        softDelete: Boolean,
        trashDir: File?,
        trashedPaths: MutableList<String>,
        deletedPaths: MutableList<String>,
        successCount: AtomicInteger,
        errors: MutableList<String>,
        maxRetries: Int = 3
    ) {
        var lastException: Exception? = null
        val filePath = file.path // Use path property which may contain content:// URI
        
        // Check if this is a SAF/content URI
        val isContentUri = filePath.startsWith("content:/")
        
        repeat(maxRetries) { attempt ->
            try {
                if (isContentUri) {
                    // Handle SAF URIs via ContentResolver
                    val normalizedUri = if (filePath.startsWith("content://")) filePath 
                                       else filePath.replaceFirst("content:/", "content://")
                    val uri = Uri.parse(normalizedUri)
                    
                    // Soft delete not supported for SAF - just do hard delete
                    if (softDelete) {
                        Timber.w("Soft delete not supported for SAF URIs, performing hard delete: $filePath")
                    }
                    
                    // Try DocumentsContract first (most reliable for SAF)
                    try {
                        val docDeleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                        if (docDeleted) {
                            deletedPaths.add(filePath)
                            successCount.incrementAndGet()
                            Timber.d("SAF delete: permanently deleted via DocumentsContract")
                            return // Success
                        }
                    } catch (e: Exception) {
                        Timber.w("DocumentsContract.deleteDocument failed: ${e.message}")
                    }

                    // Fallback to ContentResolver.delete
                    try {
                        val deleted = context.contentResolver.delete(uri, null, null) > 0
                        if (deleted) {
                            deletedPaths.add(filePath)
                            successCount.incrementAndGet()
                            Timber.d("SAF delete: permanently deleted via ContentResolver")
                            return // Success
                        }
                    } catch (e: Exception) {
                        Timber.w("ContentResolver.delete failed: ${e.message}")
                    }
                    
                    // Last resort: DocumentFile
                    try {
                        val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                        if (docFile != null && docFile.exists() && docFile.delete()) {
                            deletedPaths.add(filePath)
                            successCount.incrementAndGet()
                            Timber.d("SAF delete: permanently deleted via DocumentFile")
                            return // Success
                        }
                    } catch (e: Exception) {
                        Timber.w("DocumentFile.delete failed: ${e.message}")
                    }

                    throw IOException("Failed to delete SAF file: $filePath")
                }
                
                // Regular file path handling
                if (!file.exists()) {
                    if (attempt == 0) errors.add("File not found: ${file.name}")
                    return
                }
                
                if (softDelete && trashDir != null) {
                    // Soft delete: move to trash folder
                    val trashFile = File(trashDir, file.name)
                    
                    // Check if trash directory still exists and is writable
                    if (!trashDir.exists() || !trashDir.canWrite()) {
                        throw IOException("Trash directory not accessible: ${trashDir.absolutePath}")
                    }
                    
                    // Check if target file already exists in trash
                    if (trashFile.exists()) {
                        // Generate unique name
                        val baseName = file.nameWithoutExtension
                        val extension = file.extension
                        val uniqueName = "$baseName.${System.currentTimeMillis()}.$extension"
                        val uniqueTrashFile = File(trashDir, uniqueName)

                        if (file.renameTo(uniqueTrashFile)) {
                            trashedPaths.add(filePath)
                            deletedPaths.add(uniqueTrashFile.absolutePath)
                            successCount.incrementAndGet()
                            Timber.d("Soft delete: moved ${file.name} to trash as $uniqueName")
                            return // Success
                        } else {
                            // Fallback: try copy+delete to move across filesystems
                            try {
                                file.copyTo(uniqueTrashFile)
                                if (file.delete()) {
                                    trashedPaths.add(filePath)
                                    deletedPaths.add(uniqueTrashFile.absolutePath)
                                    successCount.incrementAndGet()
                                    Timber.d("Soft delete(fallback): copied ${file.name} to trash as $uniqueName and deleted original")
                                    return
                                } else {
                                    // cleanup copied file
                                    uniqueTrashFile.delete()
                                    throw IOException("Failed to delete original after copy to trash")
                                }
                            } catch (ex: Exception) {
                                throw IOException("Failed to move to trash (unique name) and fallback failed: ${ex.message}")
                            }
                        }
                    } else if (file.renameTo(trashFile)) {
                        trashedPaths.add(filePath)
                        deletedPaths.add(trashFile.absolutePath)
                        successCount.incrementAndGet()
                        Timber.d("Soft delete: moved ${file.name} to trash")
                        return // Success
                    } else {
                        // Fallback: try copy+delete to move across filesystems
                        try {
                            file.copyTo(trashFile)
                            if (file.delete()) {
                                trashedPaths.add(filePath)
                                deletedPaths.add(trashFile.absolutePath)
                                successCount.incrementAndGet()
                                Timber.d("Soft delete(fallback): copied ${file.name} to trash and deleted original")
                                return
                            } else {
                                trashFile.delete()
                                throw IOException("Failed to delete original after copy to trash")
                            }
                        } catch (ex: Exception) {
                            throw IOException("Failed to move to trash (rename failed) and fallback failed: ${ex.message}")
                        }
                    }
                } else {
                    // Hard delete: permanent deletion
                    if (file.delete()) {
                        deletedPaths.add(filePath)
                        successCount.incrementAndGet()
                        Timber.d("Hard delete: permanently deleted ${file.name}")
                        return // Success
                    } else {
                        throw IOException("Failed to delete")
                    }
                }
                
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Exponential backoff: 100ms, 200ms, 300ms
                    val delayMs = 100L * (attempt + 1)
                    delay(delayMs)
                    Timber.w("Delete retry ${attempt + 1}/$maxRetries for ${file.name} after ${delayMs}ms: ${e.message}")
                }
            }
        }
        
        // All retries failed
        errors.add("Delete error for ${file.name}: ${lastException?.message}")
        Timber.e(lastException, "Delete failed after $maxRetries attempts: ${file.name}")
    }
    
    fun getLastOperation(): OperationHistory? = lastOperation
    
    fun clearHistory() {
        lastOperation = null
    }
    
    suspend fun canUndo(): Boolean = withContext(Dispatchers.IO) {
        lastOperation != null
    }
    
    suspend fun undo(): FileOperationResult? = withContext(Dispatchers.IO) {
        val history = lastOperation ?: return@withContext null
        
        when (val op = history.operation) {
            is FileOperation.Copy -> {
                val filesToDelete = op.sources.map { File(op.destination, it.name) }
                execute(FileOperation.Delete(filesToDelete))
            }
            is FileOperation.Move -> {
                val filesToMoveBack = op.sources.mapNotNull { source ->
                    val parent = source.parentFile
                    if (parent != null) {
                        File(op.destination, source.name) to parent
                    } else {
                        null
                    }
                }.filter { it.first.exists() }
                
                if (filesToMoveBack.isEmpty()) return@withContext null
                
                execute(FileOperation.Move(
                    sources = filesToMoveBack.map { it.first },
                    destination = filesToMoveBack.first().second,
                    overwrite = true
                ))
            }
            is FileOperation.Delete -> null
            is FileOperation.Rename -> {
                // For network paths, manually construct new path
                val filePath = op.file.path
                val newFile = if (filePath.startsWith("smb://") || filePath.startsWith("sftp://") || filePath.startsWith("ftp://")) {
                    val lastSlashIndex = filePath.lastIndexOf('/')
                    val parentPath = filePath.substring(0, lastSlashIndex)
                    val newPath = "$parentPath/${op.newName}"
                    object : File(newPath) {
                        override fun getPath(): String = newPath
                        override fun getAbsolutePath(): String = newPath
                        override fun exists(): Boolean = true // Assume exists for undo
                    }
                } else {
                    File(op.file.parent, op.newName)
                }
                
                if (newFile.exists()) {
                    execute(FileOperation.Rename(newFile, op.file.name))
                } else {
                    null
                }
            }
        }
    }
}
