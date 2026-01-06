package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for tracking file operations history.
 * Used for undo functionality and operation logging.
 */
@Entity(
    tableName = "file_operation_history",
    indices = [Index(value = ["timestamp"])]
)
data class FileOperationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Operation type: COPY, MOVE, DELETE, RENAME */
    val operationType: String,
    
    /** Source path before operation */
    val sourcePath: String,
    
    /** Destination path after operation (null for DELETE) */
    val destinationPath: String? = null,
    
    /** Original file name (for RENAME operations) */
    val originalName: String? = null,
    
    /** New file name (for RENAME operations) */
    val newName: String? = null,
    
    /** Trash path (for soft-delete undo) */
    val trashPath: String? = null,
    
    /** Whether this operation can be undone */
    val canUndo: Boolean = true,
    
    /** Whether this operation has been undone */
    val isUndone: Boolean = false,
    
    /** Timestamp of the operation */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Expiration timestamp for trash (auto-delete after 30 days) */
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
)
