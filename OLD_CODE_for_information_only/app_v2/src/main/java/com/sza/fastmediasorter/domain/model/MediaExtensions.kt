package com.sza.fastmediasorter.domain.model

object MediaExtensions {
    val IMAGE = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "avif")
    val VIDEO = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpg", "mpeg", 
        "ts", "m2ts", "vob", "ogv", "divx", "m2v", "mts"
    )
    val AUDIO = setOf(
        "mp3", "flac", "aac", "ogg", "m4a", "wma", "opus", 
        "amr", "awb", "ac3", "ec3", "ac4", "adts", "thd", "mka", "oga", "caf", "alac", "mia", "mid", "midi"
    )
    val TEXT = setOf("txt", "md", "log", "json", "xml", "csv", "conf", "ini", "properties", "yml", "yaml")
    val PDF = setOf("pdf")
    
    fun isImage(extension: String): Boolean = extension.lowercase() in IMAGE
    fun isVideo(extension: String): Boolean = extension.lowercase() in VIDEO
    fun isAudio(extension: String): Boolean = extension.lowercase() in AUDIO
    fun isText(extension: String): Boolean = extension.lowercase() in TEXT
    fun isPdf(extension: String): Boolean = extension.lowercase() in PDF
    
    fun getMediaType(extension: String): MediaType {
        val lowerExt = extension.lowercase()
        return when {
            lowerExt in IMAGE -> MediaType.IMAGE
            lowerExt in VIDEO -> MediaType.VIDEO
            lowerExt in AUDIO -> MediaType.AUDIO
            lowerExt in TEXT -> MediaType.TEXT
            lowerExt in PDF -> MediaType.PDF
            else -> MediaType.IMAGE // Default fallback
        }
    }
}
