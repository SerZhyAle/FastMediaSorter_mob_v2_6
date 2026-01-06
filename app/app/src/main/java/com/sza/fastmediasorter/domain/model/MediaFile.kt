package com.sza.fastmediasorter.domain.model

import java.util.Date

/**
 * Domain model representing a media file.
 * This is a pure Kotlin class with no Android dependencies.
 */
data class MediaFile(
    /** Full path to the file */
    val path: String,
    
    /** File name with extension */
    val name: String,
    
    /** File size in bytes */
    val size: Long,
    
    /** Last modified date */
    val date: Date,
    
    /** Media type classification */
    val type: MediaType,
    
    /** Duration in milliseconds (for video/audio) */
    val duration: Long? = null,
    
    /** Width in pixels (for images/videos) */
    val width: Int? = null,
    
    /** Height in pixels (for images/videos) */
    val height: Int? = null,
    
    /** Thumbnail URL (for cloud files or cached thumbnails) */
    val thumbnailUrl: String? = null,
    
    /** Whether this file is marked as favorite */
    val isFavorite: Boolean = false,
    
    /** Whether this is a directory */
    val isDirectory: Boolean = false
)

/**
 * Classification of media file types.
 */
enum class MediaType {
    IMAGE,    // JPG, PNG, WEBP, BMP, HEIC
    VIDEO,    // MP4, MKV, AVI, MOV, WEBM
    AUDIO,    // MP3, FLAC, WAV, M4A, OGG
    GIF,      // Animated GIF
    PDF,      // PDF documents
    TXT,      // Plain text files
    EPUB,     // E-books
    OTHER     // Any other file type (when "work with all files" is enabled)
}
