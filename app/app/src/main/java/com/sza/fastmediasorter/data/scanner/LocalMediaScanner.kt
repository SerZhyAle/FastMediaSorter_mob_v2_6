package com.sza.fastmediasorter.data.scanner

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for local file system media files.
 * Scans directories and returns list of media files with metadata.
 */
@Singleton
class LocalMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Supported media extensions
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "wma")
        private val GIF_EXTENSIONS = setOf("gif")
        private val PDF_EXTENSIONS = setOf("pdf")
        private val TXT_EXTENSIONS = setOf("txt", "log", "json", "xml", "md")
        private val EPUB_EXTENSIONS = setOf("epub")
    }
    
    /** Reusable MediaMetadataRetriever for extracting video/audio metadata */
    private val mediaRetriever = MediaMetadataRetriever()

    /**
     * Scan a folder and return list of media files.
     * @param folderPath Path to the folder to scan
     * @param recursive Whether to scan subdirectories
     * @return List of MediaFile objects sorted by date descending
     */
    suspend fun scanFolder(folderPath: String, recursive: Boolean = false): List<MediaFile> = 
        withContext(Dispatchers.IO) {
            val files = mutableListOf<MediaFile>()
            val folder = File(folderPath)

            if (!folder.exists()) {
                Timber.w("Folder does not exist: $folderPath")
                return@withContext emptyList()
            }

            if (!folder.isDirectory) {
                Timber.w("Path is not a directory: $folderPath")
                return@withContext emptyList()
            }

            if (!folder.canRead()) {
                Timber.w("Cannot read folder: $folderPath")
                return@withContext emptyList()
            }

            scanDirectory(folder, files, recursive)

            files.sortedByDescending { it.date }
        }

    private fun scanDirectory(directory: File, files: MutableList<MediaFile>, recursive: Boolean) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile -> {
                        val mediaFile = file.toMediaFileOrNull()
                        if (mediaFile != null) {
                            files.add(mediaFile)
                        }
                    }
                    file.isDirectory && recursive -> {
                        scanDirectory(file, files, recursive)
                    }
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception scanning directory: ${directory.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error scanning directory: ${directory.absolutePath}")
        }
    }

    private fun File.toMediaFileOrNull(): MediaFile? {
        val extension = extension.lowercase()
        val mediaType = detectMediaType(extension)
        
        // Filter out non-media files
        if (mediaType == MediaType.OTHER) {
            return null
        }

        var duration: Long? = null
        var width: Int? = null
        var height: Int? = null
        
        // Extract metadata based on media type
        when (mediaType) {
            MediaType.IMAGE, MediaType.GIF -> {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(absolutePath, options)
                    width = options.outWidth.takeIf { it > 0 }
                    height = options.outHeight.takeIf { it > 0 }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to extract image dimensions: $absolutePath")
                }
            }
            MediaType.VIDEO -> {
                try {
                    mediaRetriever.setDataSource(absolutePath)
                    duration = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                    width = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull()
                    height = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to extract video metadata: $absolutePath")
                }
            }
            MediaType.AUDIO -> {
                try {
                    mediaRetriever.setDataSource(absolutePath)
                    duration = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to extract audio metadata: $absolutePath")
                }
            }
            else -> { /* No metadata to extract for other types */ }
        }

        return MediaFile(
            path = absolutePath,
            name = name,
            size = length(),
            date = Date(lastModified()),
            type = mediaType,
            duration = duration,
            width = width,
            height = height
        )
    }

    private fun detectMediaType(extension: String): MediaType {
        return when {
            extension in IMAGE_EXTENSIONS -> MediaType.IMAGE
            extension in VIDEO_EXTENSIONS -> MediaType.VIDEO
            extension in AUDIO_EXTENSIONS -> MediaType.AUDIO
            extension in GIF_EXTENSIONS -> MediaType.GIF
            extension in PDF_EXTENSIONS -> MediaType.PDF
            extension in TXT_EXTENSIONS -> MediaType.TXT
            extension in EPUB_EXTENSIONS -> MediaType.EPUB
            else -> MediaType.OTHER
        }
    }

    /**
     * Check if a file is a supported media type.
     */
    fun isSupportedMediaFile(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return detectMediaType(extension) != MediaType.OTHER
    }

    /**
     * Get the media type for a file path.
     */
    fun getMediaType(filePath: String): MediaType {
        val extension = File(filePath).extension.lowercase()
        return detectMediaType(extension)
    }
}
