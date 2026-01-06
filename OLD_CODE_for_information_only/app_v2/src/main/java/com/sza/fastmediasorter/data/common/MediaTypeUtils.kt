package com.sza.fastmediasorter.data.common

import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import java.util.Locale

object MediaTypeUtils {
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "avif")
    val GIF_EXTENSIONS = setOf("gif")
    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "webm", "3gp", "flv", "wmv", "m4v", "avi", "mpg", "mpeg",
        "ts", "m2ts", "vob", "ogv", "divx", "m2v", "mts"
    )
    val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "flac", "aac", "ogg", "wma", "opus",
        "amr", "awb", "ac3", "ec3", "ac4", "adts", "thd", "mka", "oga", "caf", "alac", "mia", "mid", "midi"
    )
    val TEXT_EXTENSIONS = setOf("txt", "md", "log", "json", "xml", "csv", "conf", "ini", "properties", "yml", "yaml")
    val PDF_EXTENSIONS = setOf("pdf")
    val EPUB_EXTENSIONS = setOf("epub")

    fun getMediaType(fileName: String): MediaType? {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            IMAGE_EXTENSIONS.contains(extension) -> MediaType.IMAGE
            GIF_EXTENSIONS.contains(extension) -> MediaType.GIF
            VIDEO_EXTENSIONS.contains(extension) -> MediaType.VIDEO
            VIDEO_EXTENSIONS.contains(extension) -> MediaType.VIDEO
            AUDIO_EXTENSIONS.contains(extension) -> MediaType.AUDIO
            TEXT_EXTENSIONS.contains(extension) -> MediaType.TEXT
            PDF_EXTENSIONS.contains(extension) -> MediaType.PDF
            EPUB_EXTENSIONS.contains(extension) -> MediaType.EPUB
            else -> null
        }
    }

    fun getMediaTypeFromMime(mimeType: String?): MediaType? {
        if (mimeType == null) return null
        return when {
            mimeType == "image/gif" -> MediaType.GIF
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("audio/") -> MediaType.AUDIO
            mimeType == "text/plain" || mimeType == "application/json" || mimeType == "text/xml" -> MediaType.TEXT
            mimeType == "application/pdf" -> MediaType.PDF
            mimeType == "application/epub+zip" -> MediaType.EPUB
            else -> null
        }
    }

    fun isFileSizeInRange(size: Long, mediaType: MediaType, filter: SizeFilter): Boolean {
        return when (mediaType) {
            MediaType.IMAGE, MediaType.GIF -> size in filter.imageSizeMin..filter.imageSizeMax
            MediaType.IMAGE, MediaType.GIF -> size in filter.imageSizeMin..filter.imageSizeMax
            MediaType.VIDEO -> size in filter.videoSizeMin..filter.videoSizeMax
            MediaType.AUDIO -> size in filter.audioSizeMin..filter.audioSizeMax
            MediaType.TEXT -> true // No size filtering for now
            MediaType.PDF -> true // No size filtering for now
            MediaType.EPUB -> true // No size filtering for now
        }
    }

    fun buildExtensionsSet(supportedTypes: Set<MediaType>): Set<String> {
        val extensions = mutableSetOf<String>()
        supportedTypes.forEach { type ->
            when (type) {
                MediaType.IMAGE -> extensions.addAll(IMAGE_EXTENSIONS)
                MediaType.GIF -> extensions.addAll(GIF_EXTENSIONS)
                MediaType.VIDEO -> extensions.addAll(VIDEO_EXTENSIONS)
                MediaType.AUDIO -> extensions.addAll(AUDIO_EXTENSIONS)
                MediaType.TEXT -> extensions.addAll(TEXT_EXTENSIONS)
                MediaType.PDF -> extensions.addAll(PDF_EXTENSIONS)
                MediaType.EPUB -> extensions.addAll(EPUB_EXTENSIONS)
            }
        }
        return extensions
    }
}
