package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a media source/resource.
 * Can be a local folder, network share (SMB/SFTP/FTP), or cloud storage.
 */
@Entity(
    tableName = "resources",
    indices = [Index(value = ["path"], unique = true)]
)
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** User-defined display name */
    val name: String,
    
    /** Path or URI to the resource */
    val path: String,
    
    /** Resource type: LOCAL, SMB, SFTP, FTP, GOOGLE_DRIVE, ONEDRIVE, DROPBOX */
    val type: String,
    
    /** Reference to network_credentials table (for network resources) */
    val credentialsId: String? = null,
    
    /** Sorting mode: NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC */
    val sortMode: String = "NAME_ASC",
    
    /** Display mode: LIST, GRID */
    val displayMode: String = "LIST",
    
    /** Order in the resource list */
    val displayOrder: Int = 0,
    
    /** Whether this resource is a move/copy destination */
    val isDestination: Boolean = false,
    
    /** Order in the destination list (-1 if not a destination) */
    val destinationOrder: Int = -1,
    
    /** Color for destination badge (Material color int) */
    val destinationColor: Int = 0xFF4CAF50.toInt(),
    
    /** Work with all files (not just media) */
    val workWithAllFiles: Boolean = false,
    
    /** Timestamp of creation */
    val createdDate: Long = System.currentTimeMillis(),
    
    /** Timestamp of last access */
    val lastAccessedDate: Long = System.currentTimeMillis(),

    /** Whether the resource is read-only */
    val isReadOnly: Boolean = false
)
