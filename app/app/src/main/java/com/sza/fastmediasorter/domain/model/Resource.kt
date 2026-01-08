package com.sza.fastmediasorter.domain.model

/**
 * Domain model representing a media source/resource.
 * This is a pure Kotlin class with no Android dependencies.
 */
data class Resource(
    /** Unique identifier */
    val id: Long,
    
    /** User-defined display name */
    val name: String,
    
    /** Path or URI to the resource */
    val path: String,
    
    /** Resource type (local, network, cloud) */
    val type: ResourceType,
    
    /** Reference to network credentials (for network resources) */
    val credentialsId: String? = null,
    
    /** Sorting preference for files in this resource */
    val sortMode: SortMode = SortMode.NAME_ASC,
    
    /** Display preference (list or grid) */
    val displayMode: DisplayMode = DisplayMode.LIST,
    
    /** Whether this resource is a move/copy destination */
    val isDestination: Boolean = false,
    
    /** Order in the destination list (-1 if not a destination) */
    val destinationOrder: Int = -1,
    
    /** Color for destination badge */
    val destinationColor: Int = 0xFF4CAF50.toInt(),
    
    /** Whether to show all files, not just media */
    val workWithAllFiles: Boolean = false,

    /** Whether the resource is read-only (no modifications allowed) */
    val isReadOnly: Boolean = false
)

/**
 * Types of resources supported by the application.
 */
enum class ResourceType {
    LOCAL,        // Local file system folder
    SMB,          // Windows/Samba network share
    SFTP,         // SSH File Transfer Protocol
    FTP,          // File Transfer Protocol
    GOOGLE_DRIVE, // Google Drive cloud storage
    ONEDRIVE,     // Microsoft OneDrive
    DROPBOX       // Dropbox cloud storage
}

/**
 * Sorting modes for file lists.
 */
enum class SortMode {
    NAME_ASC,     // A-Z
    NAME_DESC,    // Z-A
    DATE_ASC,     // Oldest first
    DATE_DESC,    // Newest first
    SIZE_ASC,     // Smallest first
    SIZE_DESC     // Largest first
}

/**
 * Display modes for file browsing.
 */
enum class DisplayMode {
    LIST,   // Linear list with details
    GRID    // Grid of thumbnails
}
