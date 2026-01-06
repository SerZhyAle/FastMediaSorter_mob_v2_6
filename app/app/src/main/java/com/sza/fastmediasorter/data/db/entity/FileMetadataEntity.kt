package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing metadata about individual files.
 * Tracks favorites, viewing state, and cached information.
 */
@Entity(
    tableName = "file_metadata",
    foreignKeys = [
        ForeignKey(
            entity = ResourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["resourceId"]),
        Index(value = ["path"], unique = true)
    ]
)
data class FileMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Reference to the parent resource */
    val resourceId: Long,
    
    /** Full path to the file */
    val path: String,
    
    /** File name */
    val name: String,
    
    /** Whether this file is marked as favorite */
    val isFavorite: Boolean = false,
    
    /** Last position for video/audio (in milliseconds) */
    val lastPlaybackPosition: Long = 0,
    
    /** Number of times this file has been viewed/played */
    val viewCount: Int = 0,
    
    /** Last viewed timestamp */
    val lastViewedDate: Long? = null,
    
    /** Cached thumbnail path (for remote files) */
    val cachedThumbnailPath: String? = null,
    
    /** User-assigned rating (1-5, 0 = not rated) */
    val rating: Int = 0
)
