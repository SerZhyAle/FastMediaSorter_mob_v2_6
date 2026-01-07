package com.sza.fastmediasorter.data.scanner

import com.sza.fastmediasorter.data.network.SftpClient
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for SFTP servers.
 * Scans remote directories via SFTP protocol and returns list of media files.
 */
@Singleton
class SftpMediaScanner @Inject constructor(
    private val sftpClient: SftpClient
) {

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "wma")
        private val GIF_EXTENSIONS = setOf("gif")
    }

    /**
     * Scan an SFTP folder and return list of media files.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param path Remote path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @param recursive Whether to scan subdirectories
     * @return List of MediaFile objects
     */
    suspend fun scanFolder(
        host: String,
        port: Int,
        path: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null,
        recursive: Boolean = false
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val result = sftpClient.listFiles(host, port, path, username, password, privateKey, passphrase)
            
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Failed to list SFTP files")
                return@withContext emptyList()
            }
            
            val fileNames = result.getOrNull() ?: emptyList()
            
            fileNames.mapNotNull { fileName ->
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val mediaType = detectMediaType(extension)
                
                if (mediaType == MediaType.OTHER) {
                    return@mapNotNull null
                }
                
                val fullPath = "sftp://$host:$port$path/$fileName"
                
                MediaFile(
                    path = fullPath,
                    name = fileName,
                    size = 0L, // Size will be fetched on demand
                    date = Date(), // Date will be fetched on demand
                    type = mediaType
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning SFTP folder")
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
