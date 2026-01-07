package com.sza.fastmediasorter.domain.model

/**
 * Media file extensions categorized by type.
 * Used for file type detection and filtering.
 */
object MediaExtensions {
    
    val IMAGE = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif",
        "tiff", "tif", "ico", "svg", "raw", "cr2", "nef", "arw", "dng"
    )
    
    val VIDEO = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
        "mpg", "mpeg", "3gp", "3g2", "mts", "m2ts", "vob", "ogv"
    )
    
    val AUDIO = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus",
        "oga", "mid", "midi", "ape", "mka"
    )
    
    val ALL_MEDIA = IMAGE + VIDEO + AUDIO
    
    /**
     * Check if extension is an image format
     */
    fun isImage(extension: String): Boolean {
        return IMAGE.contains(extension.lowercase())
    }
    
    /**
     * Check if extension is a video format
     */
    fun isVideo(extension: String): Boolean {
        return VIDEO.contains(extension.lowercase())
    }
    
    /**
     * Check if extension is an audio format
     */
    fun isAudio(extension: String): Boolean {
        return AUDIO.contains(extension.lowercase())
    }
    
    /**
     * Check if extension is any media format
     */
    fun isMedia(extension: String): Boolean {
        return ALL_MEDIA.contains(extension.lowercase())
    }
    
    /**
     * Get media type category for extension
     */
    fun getMediaType(extension: String): String? {
        val ext = extension.lowercase()
        return when {
            IMAGE.contains(ext) -> "image"
            VIDEO.contains(ext) -> "video"
            AUDIO.contains(ext) -> "audio"
            else -> null
        }
    }
}
