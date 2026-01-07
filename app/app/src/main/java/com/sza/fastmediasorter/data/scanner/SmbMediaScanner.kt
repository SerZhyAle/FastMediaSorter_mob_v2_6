package com.sza.fastmediasorter.data.scanner

import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for SMB/CIFS network shares.
 * Scans remote directories via SMB protocol and returns list of media files.
 */
@Singleton
class SmbMediaScanner @Inject constructor(
    private val smbClient: SmbClient
) {

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "wma")
        private val GIF_EXTENSIONS = setOf("gif")
    }

    /**
     * Scan an SMB share folder and return list of media files.
     * 
     * @param server Server hostname or IP
     * @param port Server port
     * @param shareName Share name
     * @param path Path within share
     * @param username Username
     * @param password Password
     * @param domain Domain (optional)
     * @param recursive Whether to scan subdirectories
     * @return List of MediaFile objects
     */
    suspend fun scanFolder(
        server: String,
        port: Int,
        shareName: String,
        path: String,
        username: String,
        password: String,
        domain: String = "",
        recursive: Boolean = false
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val result = smbClient.listFiles(server, port, shareName, path, username, password, domain)
            
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Failed to list SMB files")
                return@withContext emptyList()
            }
            
            val fileNames = result.getOrNull() ?: emptyList()
            
            fileNames.mapNotNull { fileName ->
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val mediaType = detectMediaType(extension)
                
                if (mediaType == MediaType.OTHER) {
                    return@mapNotNull null
                }
                
                val fullPath = if (path.isEmpty()) {
                    "smb://$server:$port/$shareName/$fileName"
                } else {
                    "smb://$server:$port/$shareName/$path/$fileName"
                }
                
                MediaFile(
                    path = fullPath,
                    name = fileName,
                    size = 0L, // Size will be fetched on demand
                    date = Date(), // Date will be fetched on demand
                    type = mediaType
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning SMB folder")
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
