package com.sza.fastmediasorter.data.transfer

import android.content.Context
import android.net.Uri
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.transfer.FileOperationError
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import com.sza.fastmediasorter.domain.usecase.FileOperation
import com.sza.fastmediasorter.domain.usecase.FileOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Base class for file operation handlers.
 * 
 * Provides common implementations for copy, move, and delete operations that are
 * protocol-agnostic. Subclasses provide protocol-specific strategy implementations.
 * 
 * This base class handles:
 * - Loop structure over multiple files
 * - Error collection and aggregation
 * - Result building (Success/PartialSuccess/Failure)
 * - Common utility methods (SAF deletion, path extraction, etc.)
 * 
 * Subclasses must provide:
 * - Protocol-specific strategies (SMB, SFTP, FTP, Cloud, Local)
 * - Path normalization logic
 * - Connection credential management
 */
abstract class BaseFileOperationHandler(
    protected val context: Context
) {
    
    /**
     * Get the list of strategies this handler supports.
     * Each strategy handles a specific protocol (e.g., SMB, SFTP).
     */
    protected abstract fun getStrategies(): List<FileOperationStrategy>
    
    /**
     * Find the appropriate strategy for a given file path.
     * 
     * @param path File path (may be protocol URL like smb://, sftp://, etc.)
     * @return The matching strategy, or null if no strategy supports the path
     */
    protected fun getStrategyForPath(path: String): FileOperationStrategy? {
        return getStrategies().firstOrNull { it.supportsProtocol(path) }
    }
    
    /**
     * Execute a copy operation.
     * 
     * Common implementation that iterates over source files, delegates to strategies,
     * collects errors, and builds the result.
     * 
     * Can be overridden by subclasses for custom behavior (e.g., cross-protocol transfers).
     */
    open suspend fun executeCopy(
        operation: FileOperation.Copy,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult = withContext(Dispatchers.IO) {
        val destinationPath = operation.destination.path
        Timber.d("executeCopy: Starting copy of ${operation.sources.size} files to $destinationPath")
        
        val errors = mutableListOf<String>()
        val copiedPaths = mutableListOf<String>()
        var successCount = 0
        
        operation.sources.forEachIndexed { index, source ->
            Timber.d("executeCopy: [${index + 1}/${operation.sources.size}] Processing ${source.name}")
            
            try {
                val sourcePath = source.path
                val fileName = extractFileName(sourcePath, source.name)
                val destPath = "$destinationPath/$fileName"
                
                // Delegate to protocol-specific copy logic
                val result = copyFile(sourcePath, destPath, operation.overwrite, progressCallback)
                
                result.fold(
                    onSuccess = { resultPath ->
                        copiedPaths.add(resultPath)
                        successCount++
                        Timber.i("executeCopy: SUCCESS - copied ${source.name}")
                    },
                    onFailure = { error ->
                        val errorMsg = FileOperationError.formatTransferError(
                            fileName = source.name,
                            sourcePath = sourcePath,
                            destinationPath = destPath,
                            errorMessage = error.message ?: "Unknown error"
                        )
                        Timber.e("executeCopy: FAILED - $errorMsg")
                        errors.add(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val error = FileOperationError.formatTransferError(
                    fileName = source.name,
                    sourcePath = source.path,
                    destinationPath = "$destinationPath/${source.name}",
                    errorMessage = FileOperationError.extractErrorMessage(e)
                )
                Timber.e(e, "executeCopy: ERROR - $error")
                errors.add(error)
            }
        }
        
        return@withContext buildCopyResult(successCount, operation, copiedPaths, errors)
    }
    
    /**
     * Execute a move operation.
     * 
     * Common implementation that iterates over source files, delegates to strategies,
     * collects errors, and builds the result.
     * 
     * Can be overridden by subclasses for custom behavior (e.g., cross-protocol transfers).
     */
    open suspend fun executeMove(
        operation: FileOperation.Move,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult = withContext(Dispatchers.IO) {
        val destinationPath = operation.destination.path
        Timber.d("executeMove: Starting move of ${operation.sources.size} files to $destinationPath")
        
        val errors = mutableListOf<String>()
        val movedPaths = mutableListOf<String>()
        var successCount = 0
        
        operation.sources.forEachIndexed { index, source ->
            Timber.d("executeMove: [${index + 1}/${operation.sources.size}] Processing ${source.name}")
            
            try {
                val sourcePath = source.path
                val fileName = extractFileName(sourcePath, source.name)
                val destPath = "$destinationPath/$fileName"
                
                // Delegate to protocol-specific move logic
                val result = moveFile(sourcePath, destPath, operation.overwrite, progressCallback)
                
                result.fold(
                    onSuccess = { resultPath ->
                        movedPaths.add(resultPath)
                        successCount++
                        Timber.i("executeMove: SUCCESS - moved ${source.name}")
                    },
                    onFailure = { error ->
                        val errorMsg = FileOperationError.formatTransferError(
                            fileName = source.name,
                            sourcePath = sourcePath,
                            destinationPath = destPath,
                            errorMessage = error.message ?: "Unknown error"
                        )
                        Timber.e("executeMove: FAILED - $errorMsg")
                        errors.add(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val error = FileOperationError.formatTransferError(
                    fileName = source.name,
                    sourcePath = source.path,
                    destinationPath = "$destinationPath/${source.name}",
                    errorMessage = FileOperationError.extractErrorMessage(e)
                )
                Timber.e(e, "executeMove: ERROR - $error")
                errors.add(error)
            }
        }
        
        return@withContext buildMoveResult(successCount, operation, movedPaths, errors)
    }
    
    /**
     * Execute a delete operation.
     * 
     * Common implementation that handles both soft delete (trash) and hard delete.
     * 
     * Can be overridden by subclasses for custom behavior.
     */
    open suspend fun executeDelete(
        operation: FileOperation.Delete
    ): FileOperationResult = withContext(Dispatchers.IO) {
        Timber.d("executeDelete: START - ${operation.files.size} files, softDelete=${operation.softDelete}")
        
        val errors = mutableListOf<String>()
        val deletedPaths = mutableListOf<String>()
        val trashedPaths = mutableListOf<String>()
        var successCount = 0
        
        // For soft delete, create trash folder
        var trashDirPath: String? = null
        if (operation.softDelete && operation.files.isNotEmpty()) {
            trashDirPath = createTrashFolder(operation.files.first().path)
            if (trashDirPath == null) {
                Timber.w("executeDelete: Failed to create trash folder, falling back to hard delete")
            }
        }
        
        operation.files.forEachIndexed { index, file ->
            Timber.d("executeDelete: Processing file [${index + 1}/${operation.files.size}]: ${file.name}")
            
            try {
                val filePath = file.path
                
                val result = if (operation.softDelete && trashDirPath != null) {
                    // Soft delete: move to trash
                    val fileName = extractFileName(filePath, file.name)
                    val trashFilePath = "$trashDirPath/$fileName"
                    moveToTrash(filePath, trashFilePath, fileName)
                } else {
                    // Hard delete: permanent
                    deleteFile(filePath)
                }
                
                result.fold(
                    onSuccess = {
                        if (operation.softDelete && trashDirPath != null) {
                            trashedPaths.add(filePath)
                            deletedPaths.add("$trashDirPath/${extractFileName(filePath, file.name)}")
                        } else {
                            deletedPaths.add(filePath)
                        }
                        successCount++
                        Timber.i("executeDelete: SUCCESS - deleted ${file.name}")
                    },
                    onFailure = { error ->
                        val errorMsg = FileOperationError.formatDeleteError(
                            fileName = file.name,
                            filePath = filePath,
                            errorMessage = error.message ?: "Unknown error"
                        )
                        Timber.e("executeDelete: FAILED - $errorMsg")
                        errors.add(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = FileOperationError.formatDeleteError(
                    fileName = file.name,
                    filePath = file.path,
                    errorMessage = FileOperationError.extractErrorMessage(e)
                )
                Timber.e(e, "executeDelete: EXCEPTION - $errorMsg")
                errors.add(errorMsg)
            }
        }
        
        // Return trash directory path in result for undo restoration
        val resultPaths = if (operation.softDelete && trashDirPath != null) {
            listOf(trashDirPath) + trashedPaths
        } else {
            deletedPaths
        }
        
        return@withContext buildDeleteResult(successCount, operation, resultPaths, errors)
    }
    
    // ==================== Protocol-Specific Methods (Delegated to Strategies) ====================
    
    /**
     * Copy a file using the appropriate strategy.
     * Subclasses should override to add protocol-specific logic like overwrite checking.
     */
    protected open suspend fun copyFile(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val sourceStrategy = getStrategyForPath(sourcePath)
        val destStrategy = getStrategyForPath(destPath)

        // If both strategies exist and are different, use cross-protocol bridging
        if (sourceStrategy != null && destStrategy != null && sourceStrategy != destStrategy) {
            return copyCrossProtocol(sourcePath, destPath, sourceStrategy, destStrategy, overwrite, progressCallback)
        }

        val strategy = sourceStrategy ?: destStrategy
        ?: return Result.failure(IllegalArgumentException("No strategy found for paths: $sourcePath -> $destPath"))
        
        return strategy.copyFile(sourcePath, destPath, overwrite, progressCallback)
    }

    private suspend fun copyCrossProtocol(
        sourcePath: String,
        destPath: String,
        sourceStrategy: FileOperationStrategy,
        destStrategy: FileOperationStrategy,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        Timber.d("copyCrossProtocol: Bridging transfer $sourcePath -> $destPath")

        // Create temp file
        val fileName = extractFileName(sourcePath, sourcePath.substringAfterLast('/'))
        val tempFile = File(context.cacheDir, "transfer_${System.currentTimeMillis()}_$fileName")

        try {
            // 1. Download source -> temp
            Timber.d("copyCrossProtocol: Step 1 - Download to temp ${tempFile.absolutePath}")
            val downloadResult = sourceStrategy.copyFile(
                sourcePath,
                tempFile.absolutePath,
                true, // Always overwrite temp
                progressCallback // Pass callback? Maybe split progress?
            )

            if (downloadResult.isFailure) {
                return Result.failure(Exception("Download failed: ${downloadResult.exceptionOrNull()?.message}"))
            }

            // 2. Upload temp -> dest
            Timber.d("copyCrossProtocol: Step 2 - Upload to dest $destPath")
            val uploadResult = destStrategy.copyFile(
                tempFile.absolutePath,
                destPath,
                overwrite,
                progressCallback // Pass callback again?
            )

            if (uploadResult.isFailure) {
                return Result.failure(Exception("Upload failed: ${uploadResult.exceptionOrNull()?.message}"))
            }

            return Result.success(destPath)

        } catch (e: Exception) {
            Timber.e(e, "copyCrossProtocol: Failed")
            return Result.failure(e)
        } finally {
            // Cleanup temp
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
    
    /**
     * Move a file using the appropriate strategy.
     * Default implementation: copy + delete.
     */
    protected open suspend fun moveFile(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        // Copy file
        val copyResult = copyFile(sourcePath, destPath, overwrite, progressCallback)
        if (copyResult.isFailure) {
            return copyResult
        }
        
        // Delete source
        val deleteResult = deleteFile(sourcePath)
        if (deleteResult.isFailure) {
            return Result.failure(
                Exception("File copied but failed to delete source: ${deleteResult.exceptionOrNull()?.message}")
            )
        }
        
        return copyResult
    }
    
    /**
     * Delete a file using the appropriate strategy.
     */
    protected open suspend fun deleteFile(filePath: String): Result<Unit> {
        val strategy = getStrategyForPath(filePath)
            ?: return Result.failure(IllegalArgumentException("No strategy found for path: $filePath"))
        
        return strategy.deleteFile(filePath)
    }
    
    /**
     * Move a file to trash (soft delete).
     * Default implementation uses moveFile.
     */
    protected open suspend fun moveToTrash(
        sourcePath: String,
        trashPath: String,
        fileName: String
    ): Result<Unit> {
        val strategy = getStrategyForPath(sourcePath)
            ?: return Result.failure(IllegalArgumentException("No strategy found for path: $sourcePath"))
        
        return strategy.moveFile(sourcePath, trashPath)
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Extract clean filename from a path.
     * Handles content:// URIs, network paths (smb://, sftp://, etc.), cloud paths, and local paths.
     */
    protected fun extractFileName(path: String, fallbackName: String): String {
        return when {
            // Cloud paths contain folder IDs, not filenames - use fallback (actual filename)
            path.startsWith("cloud://") || path.startsWith("cloud:/") -> fallbackName
            path.startsWith("content:/") -> {
                try {
                    val decoded = Uri.decode(path)
                    decoded.substringAfterLast("/").substringAfterLast("%2F")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to decode content URI, using fallback")
                    fallbackName
                }
            }
            path.contains("/") -> path.substringAfterLast("/")
            else -> fallbackName
        }
    }
    
    /**
     * Create a trash folder for soft delete operations.
     * Returns the trash folder path, or null if creation failed.
     */
    protected open suspend fun createTrashFolder(firstFilePath: String): String? {
        // Extract parent directory
        val parentDir = if (firstFilePath.contains("/")) {
            firstFilePath.substringBeforeLast('/')
        } else {
            return null
        }
        
        val trashDirPath = "$parentDir/.trash_${System.currentTimeMillis()}"
        
        val strategy = getStrategyForPath(trashDirPath) ?: return null
        
        // Create directory (strategies should implement this if needed)
        // For now, we'll try to use the exists check as a proxy
        // Subclasses can override this method for protocol-specific directory creation
        
        return trashDirPath
    }
    
    /**
     * Delete a content:// URI using SAF.
     */
    protected suspend fun deleteWithSaf(contentUri: String): Boolean {
        return com.sza.fastmediasorter.utils.SafHelper.deleteContentUri(
            context, contentUri, "BaseFileOperationHandler"
        )
    }
    
    // ==================== Result Building Methods ====================
    
    /**
     * Build the result for a copy operation.
     */
    protected fun buildCopyResult(
        successCount: Int,
        operation: FileOperation.Copy,
        copiedPaths: List<String>,
        errors: List<String>
    ): FileOperationResult {
        return when {
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
                // Fallback resolved string for compatibility, but provide resource ID for UI to resolve with correct context
                FileOperationResult.Failure(
                    error = context.getString(R.string.all_copy_operations_failed, errorMessage),
                    errorRes = R.string.all_copy_operations_failed,
                    formatArgs = listOf(errorMessage)
                )
            }
        }
    }
    
    /**
     * Build the result for a move operation.
     */
    protected fun buildMoveResult(
        successCount: Int,
        operation: FileOperation.Move,
        movedPaths: List<String>,
        errors: List<String>
    ): FileOperationResult {
        return when {
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
    }
    
    /**
     * Build the result for a delete operation.
     */
    protected fun buildDeleteResult(
        successCount: Int,
        operation: FileOperation.Delete,
        deletedPaths: List<String>,
        errors: List<String>
    ): FileOperationResult {
        return when {
            successCount == operation.files.size -> {
                Timber.i("executeDelete: All $successCount files deleted successfully")
                FileOperationResult.Success(successCount, operation, deletedPaths)
            }
            successCount > 0 -> {
                Timber.w("executeDelete: Partial success - $successCount/${operation.files.size} deleted. Errors: $errors")
                FileOperationResult.PartialSuccess(successCount, errors.size, errors)
            }
            else -> {
                Timber.e("executeDelete: All delete operations failed. Errors: $errors")
                val errorMessage = errors.joinToString("; ")
                FileOperationResult.Failure(
                    error = "All delete operations failed: $errorMessage",
                    errorRes = R.string.all_delete_operations_failed,
                    formatArgs = listOf(errorMessage)
                )
            }
        }
    }
}
