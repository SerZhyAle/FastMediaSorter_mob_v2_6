package com.sza.fastmediasorter.data.scanner

import com.sza.fastmediasorter.data.network.FtpClient
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for FTP servers.
 * Scans remote directories via FTP protocol and returns list of media files.
 */
@Singleton
class FtpMediaScanner @Inject constructor(
    private val ftpClient: FtpClient
) {

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "wma")
        private val GIF_EXTENSIONS = setOf("gif")
    }

    /**
     * Scan an FTP folder and return list of media files.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote path
     * @param username Username
     * @param password Password
     * @param recursive Whether to scan subdirectories
     * @return List of MediaFile objects
     */
    suspend fun scanFolder(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String,
        recursive: Boolean = false
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val result = ftpClient.listFiles(host, port, remotePath, username, password)
            
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Failed to list FTP files")
                return@withContext emptyList()
            }
            
            val fileNames = result.getOrNull() ?: emptyList()
            
            fileNames.mapNotNull { fileName ->
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val mediaType = detectMediaType(extension)
                
                if (mediaType == MediaType.OTHER) {
                    return@mapNotNull null
                }
                
                val fullPath = "ftp://$host:$port$remotePath/$fileName"
                
                MediaFile(
                    path = fullPath,
                    name = fileName,
                    size = 0L, // Size will be fetched on demand
                    date = Date(), // Date will be fetched on demand
                    type = mediaType
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning FTP folder")
            emptyList()
        }
    }

    private fun detectMediaType(extension: String): MediaType {
        return when (extension) {
            in GIF_EXTENSIONS -> MediaType.GIF
            in IMAGE_EXTENSIONS -> MediaType.IMAGE
            in VIDEO_EXTENSIONS -> MediaType.VIDEO
            in AUDIO_EXTENSIONS -> MediaType.AUDIO
            else -> MediaType.OTHER
        }
    }
}
