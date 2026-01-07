package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Use case for retrieving favorite files.
 */
class GetFavoriteFilesUseCase @Inject constructor(
    private val fileMetadataRepository: FileMetadataRepository
) {
    /**
     * Get favorites as a Flow.
     */
    fun observeFavorites(): Flow<Result<List<MediaFile>>> {
        return fileMetadataRepository.getFavoritesFlow().map { paths ->
            try {
                val mediaFiles = paths.mapNotNull { path ->
                    val file = File(path)
                    if (file.exists()) {
                        createMediaFileFromPath(path)
                    } else {
                        Timber.w("Favorite file no longer exists: $path")
                        null
                    }
                }
                Result.Success(mediaFiles)
            } catch (e: Exception) {
                Timber.e(e, "Error loading favorites")
                Result.Error(message = e.message ?: "Error loading favorites", throwable = e)
            }
        }
    }

    /**
     * Get favorites once.
     */
    suspend operator fun invoke(): Result<List<MediaFile>> {
        return try {
            val paths = fileMetadataRepository.getFavorites()
            val mediaFiles = paths.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    createMediaFileFromPath(path)
                } else {
                    Timber.w("Favorite file no longer exists: $path")
                    null
                }
            }
            Result.Success(mediaFiles)
        } catch (e: Exception) {
            Timber.e(e, "Error loading favorites")
            Result.Error(message = e.message ?: "Error loading favorites", throwable = e)
        }
    }

    private fun createMediaFileFromPath(path: String): MediaFile {
        val file = File(path)
        val extension = file.extension.lowercase()
        
        val mediaType = when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "m4v", "3gp", "wmv", "flv" -> MediaType.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "opus" -> MediaType.AUDIO
            else -> MediaType.OTHER
        }

        return MediaFile(
            path = path,
            name = file.name,
            size = file.length(),
            date = Date(file.lastModified()),
            type = mediaType,
            thumbnailUrl = null,
            duration = null,
            width = null,
            height = null,
            isFavorite = true
        )
    }
}
