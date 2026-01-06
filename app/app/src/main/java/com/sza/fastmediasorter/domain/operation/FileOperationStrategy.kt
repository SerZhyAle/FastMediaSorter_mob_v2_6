package com.sza.fastmediasorter.domain.operation

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result

/**
 * Strategy interface for file operations.
 * Implements Strategy Pattern to support different protocols (Local, SMB, SFTP, etc.)
 * 
 * Each protocol implements this interface with its own logic for file operations.
 */
interface FileOperationStrategy {

    /**
     * Copy a file to a destination path.
     * 
     * @param source The source MediaFile
     * @param destinationPath The full destination path including filename
     * @param onProgress Optional progress callback (0.0 to 1.0)
     * @return Result with the new file path or error
     */
    suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>

    /**
     * Move a file to a destination path.
     * 
     * @param source The source MediaFile
     * @param destinationPath The full destination path including filename
     * @param onProgress Optional progress callback (0.0 to 1.0)
     * @return Result with the new file path or error
     */
    suspend fun move(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>

    /**
     * Delete a file (move to trash or permanent delete).
     * 
     * @param file The MediaFile to delete
     * @param permanent If true, skip trash and delete permanently
     * @return Result indicating success or error
     */
    suspend fun delete(
        file: MediaFile,
        permanent: Boolean = false
    ): Result<Unit>

    /**
     * Rename a file.
     * 
     * @param file The MediaFile to rename
     * @param newName The new filename (without path)
     * @return Result with the new file path or error
     */
    suspend fun rename(
        file: MediaFile,
        newName: String
    ): Result<String>

    /**
     * Check if a file exists.
     * 
     * @param path The full path to check
     * @return True if file exists
     */
    suspend fun exists(path: String): Boolean

    /**
     * Create a directory.
     * 
     * @param path The full path for the new directory
     * @return Result indicating success or error
     */
    suspend fun createDirectory(path: String): Result<Unit>

    /**
     * Get file metadata.
     * 
     * @param path The full path to the file
     * @return Result with MediaFile or error if not found
     */
    suspend fun getFileInfo(path: String): Result<MediaFile>
}

/**
 * Enum for file operation types.
 * Used for logging and history tracking.
 */
enum class FileOperationType {
    COPY,
    MOVE,
    DELETE,
    RENAME,
    CREATE_DIRECTORY
}

/**
 * Data class for operation progress updates.
 */
data class OperationProgress(
    val operationType: FileOperationType,
    val sourcePath: String,
    val destinationPath: String? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)
