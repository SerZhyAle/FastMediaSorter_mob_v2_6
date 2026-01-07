package com.sza.fastmediasorter.data.operation

import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages soft-deleted files in the .trash folder.
 * Supports undo operations by tracking deleted files.
 */
@Singleton
class TrashManager @Inject constructor() {

    companion object {
        const val TRASH_FOLDER_NAME = ".trash"
        private const val MAX_TRASH_AGE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    /**
     * Represents a file that was moved to trash.
     */
    data class TrashedFile(
        val originalPath: String,
        val trashPath: String,
        val deletedAt: Long = System.currentTimeMillis()
    )

    // In-memory tracking of recently deleted files for undo
    private val recentlyDeleted = mutableListOf<TrashedFile>()

    /**
     * Move a file to trash and track it for undo.
     * Returns the trash path if successful.
     */
    suspend fun moveToTrash(file: MediaFile): Result<TrashedFile> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(file.path)
            
            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    message = "File not found: ${file.path}",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            val trashDir = File(sourceFile.parentFile, TRASH_FOLDER_NAME)
            if (!trashDir.exists()) {
                trashDir.mkdirs()
            }

            // Use timestamp prefix for unique name and sorting
            val timestamp = System.currentTimeMillis()
            val trashFile = File(trashDir, "${timestamp}_${sourceFile.name}")

            if (sourceFile.renameTo(trashFile)) {
                val trashedFile = TrashedFile(
                    originalPath = file.path,
                    trashPath = trashFile.absolutePath,
                    deletedAt = timestamp
                )
                recentlyDeleted.add(0, trashedFile)
                
                // Keep only last 50 items in memory
                while (recentlyDeleted.size > 50) {
                    recentlyDeleted.removeAt(recentlyDeleted.size - 1)
                }

                Timber.d("Moved to trash: ${file.path} -> ${trashFile.path}")
                Result.Success(trashedFile)
            } else {
                Result.Error(
                    message = "Failed to move file to trash",
                    errorCode = ErrorCode.PERMISSION_DENIED
                )
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied moving to trash: ${file.path}")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error moving to trash: ${file.path}")
            Result.Error(
                message = e.message ?: "Failed to delete",
                throwable = e
            )
        }
    }

    /**
     * Restore a file from trash to its original location.
     */
    suspend fun restoreFromTrash(trashedFile: TrashedFile): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashedFile.trashPath)
            val originalFile = File(trashedFile.originalPath)

            if (!trashFile.exists()) {
                return@withContext Result.Error(
                    message = "Trashed file no longer exists",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Check if original location is available
            if (originalFile.exists()) {
                // Find alternative name
                val parent = originalFile.parentFile
                val baseName = originalFile.nameWithoutExtension
                val extension = originalFile.extension
                var counter = 1
                var newFile = File(parent, "${baseName}_restored.$extension")
                while (newFile.exists()) {
                    newFile = File(parent, "${baseName}_restored_$counter.$extension")
                    counter++
                }
                
                if (trashFile.renameTo(newFile)) {
                    recentlyDeleted.removeAll { it.trashPath == trashedFile.trashPath }
                    Timber.d("Restored (renamed): ${trashFile.path} -> ${newFile.path}")
                    return@withContext Result.Success(newFile.absolutePath)
                }
            } else {
                // Restore to original location
                originalFile.parentFile?.mkdirs()
                if (trashFile.renameTo(originalFile)) {
                    recentlyDeleted.removeAll { it.trashPath == trashedFile.trashPath }
                    Timber.d("Restored: ${trashFile.path} -> ${originalFile.path}")
                    return@withContext Result.Success(originalFile.absolutePath)
                }
            }

            Result.Error(
                message = "Failed to restore file",
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error restoring from trash: ${trashedFile.trashPath}")
            Result.Error(
                message = e.message ?: "Failed to restore",
                throwable = e
            )
        }
    }

    /**
     * Get recently deleted files that can be undone.
     */
    fun getRecentlyDeleted(): List<TrashedFile> = recentlyDeleted.toList()

    /**
     * Get the most recently deleted file for quick undo.
     */
    fun getLastDeleted(): TrashedFile? = recentlyDeleted.firstOrNull()

    /**
     * Clear the in-memory undo history.
     */
    fun clearUndoHistory() {
        recentlyDeleted.clear()
    }

    /**
     * Permanently delete old files from trash.
     */
    suspend fun cleanupOldTrash(rootPath: String) = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(rootPath)
            val trashDir = File(rootDir, TRASH_FOLDER_NAME)
            
            if (!trashDir.exists() || !trashDir.isDirectory) return@withContext

            val now = System.currentTimeMillis()
            var deletedCount = 0

            trashDir.listFiles()?.forEach { file ->
                // Parse timestamp from filename
                val timestamp = file.name.substringBefore("_").toLongOrNull() ?: 0L
                if (now - timestamp > MAX_TRASH_AGE_MS) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            if (deletedCount > 0) {
                Timber.d("Cleaned up $deletedCount old files from trash in $rootPath")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up trash in $rootPath")
        }
    }

    /**
     * Get list of all files in trash for a given path.
     */
    suspend fun getTrashContents(rootPath: String): List<TrashedFile> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(rootPath)
            val trashDir = File(rootDir, TRASH_FOLDER_NAME)
            
            if (!trashDir.exists() || !trashDir.isDirectory) {
                return@withContext emptyList()
            }

            trashDir.listFiles()?.mapNotNull { file ->
                val timestamp = file.name.substringBefore("_").toLongOrNull() ?: 0L
                val originalName = file.name.substringAfter("_")
                TrashedFile(
                    originalPath = File(rootDir, originalName).absolutePath,
                    trashPath = file.absolutePath,
                    deletedAt = timestamp
                )
            }?.sortedByDescending { it.deletedAt } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error listing trash in $rootPath")
            emptyList()
        }
    }

    /**
     * Empty the trash folder.
     */
    suspend fun emptyTrash(rootPath: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(rootPath)
            val trashDir = File(rootDir, TRASH_FOLDER_NAME)
            
            if (!trashDir.exists() || !trashDir.isDirectory) {
                return@withContext Result.Success(0)
            }

            var deletedCount = 0
            trashDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }

            // Also clear from undo history
            recentlyDeleted.clear()

            Timber.d("Emptied trash: $deletedCount files deleted from $rootPath")
            Result.Success(deletedCount)
        } catch (e: Exception) {
            Timber.e(e, "Error emptying trash in $rootPath")
            Result.Error(
                message = e.message ?: "Failed to empty trash",
                throwable = e
            )
        }
    }
}
