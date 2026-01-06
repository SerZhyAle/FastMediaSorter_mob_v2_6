package com.sza.fastmediasorter.data.transfer

import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import java.io.File

/**
 * Strategy interface for protocol-specific file operations.
 * Each protocol (SMB, SFTP, FTP, Cloud) implements this interface.
 */
interface FileOperationStrategy {
    
    /**
     * Copy a file from source to destination.
     * 
     * @param source Source file path (can be protocol-specific URL or local path)
     * @param destination Destination file path (can be protocol-specific URL or local path)
     * @param overwrite Whether to overwrite existing files
     * @param progressCallback Optional progress callback for tracking transfer progress
     * @return Result containing the destination path on success, or error on failure
     */
    suspend fun copyFile(
        source: String,
        destination: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback? = null
    ): Result<String>
    
    /**
     * Move a file from source to destination.
     * This typically performs copy + delete.
     * 
     * @param source Source file path
     * @param destination Destination file path
     * @return Result indicating success or failure
     */
    suspend fun moveFile(
        source: String,
        destination: String
    ): Result<Unit>
    
    /**
     * Delete a file.
     * 
     * @param path File path to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(path: String): Result<Unit>
    
    /**
     * Check if a file exists.
     * 
     * @param path File path to check
     * @return Result containing true if file exists, false otherwise
     */
    suspend fun exists(path: String): Result<Boolean>
    
    /**
     * Check if this strategy supports the given path protocol.
     * 
     * @param path File path to check
     * @return true if this strategy can handle the path
     */
    fun supportsProtocol(path: String): Boolean
    
    /**
     * Get the protocol identifier (e.g., "smb", "sftp", "ftp", "cloud", "local")
     */
    fun getProtocolName(): String
}
