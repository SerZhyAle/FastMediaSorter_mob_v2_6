package com.sza.fastmediasorter.data.operation

import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FileOperationStrategy for local file system.
 * Handles copy, move, delete, rename operations on local files.
 */
@Singleton
class LocalOperationStrategy @Inject constructor() : FileOperationStrategy {

    companion object {
        private const val TRASH_FOLDER_NAME = ".trash"
        private const val BUFFER_SIZE = 8192
    }

    override suspend fun copy(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source.path)
            val destFile = File(destinationPath)

            // Validate source exists
            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    message = "Source file not found: ${source.path}",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Check destination doesn't exist
            if (destFile.exists()) {
                return@withContext Result.Error(
                    message = "Destination file already exists: $destinationPath",
                    errorCode = ErrorCode.FILE_EXISTS
                )
            }

            // Create destination directory if needed
            destFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            // Copy with progress
            copyWithProgress(sourceFile, destFile, onProgress)

            Timber.d("Copied ${source.path} to $destinationPath")
            Result.Success(destinationPath)

        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied copying ${source.path}")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error copying ${source.path}")
            Result.Error(
                message = e.message ?: "Copy failed",
                throwable = e
            )
        }
    }

    override suspend fun move(
        source: MediaFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source.path)
            val destFile = File(destinationPath)

            // Validate source exists
            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    message = "Source file not found: ${source.path}",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Check destination doesn't exist
            if (destFile.exists()) {
                return@withContext Result.Error(
                    message = "Destination file already exists: $destinationPath",
                    errorCode = ErrorCode.FILE_EXISTS
                )
            }

            // Create destination directory if needed
            destFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            // Try atomic rename first (fast if same filesystem)
            if (sourceFile.renameTo(destFile)) {
                onProgress?.invoke(1f)
                Timber.d("Moved (renamed) ${source.path} to $destinationPath")
                return@withContext Result.Success(destinationPath)
            }

            // Fallback: copy then delete
            copyWithProgress(sourceFile, destFile, onProgress)
            
            if (!sourceFile.delete()) {
                Timber.w("Could not delete source after move: ${source.path}")
            }

            Timber.d("Moved ${source.path} to $destinationPath")
            Result.Success(destinationPath)

        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied moving ${source.path}")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error moving ${source.path}")
            Result.Error(
                message = e.message ?: "Move failed",
                throwable = e
            )
        }
    }

    override suspend fun delete(
        file: MediaFile,
        permanent: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(file.path)

            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    message = "File not found: ${file.path}",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            if (permanent) {
                // Permanent delete
                if (sourceFile.delete()) {
                    Timber.d("Permanently deleted: ${file.path}")
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        message = "Failed to delete file",
                        errorCode = ErrorCode.PERMISSION_DENIED
                    )
                }
            } else {
                // Move to trash
                val trashDir = File(sourceFile.parentFile, TRASH_FOLDER_NAME)
                if (!trashDir.exists()) {
                    trashDir.mkdirs()
                }

                val trashFile = File(trashDir, "${System.currentTimeMillis()}_${sourceFile.name}")
                if (sourceFile.renameTo(trashFile)) {
                    Timber.d("Moved to trash: ${file.path} -> ${trashFile.path}")
                    Result.Success(Unit)
                } else {
                    // Fallback to permanent delete
                    if (sourceFile.delete()) {
                        Timber.d("Deleted (trash failed): ${file.path}")
                        Result.Success(Unit)
                    } else {
                        Result.Error(
                            message = "Failed to delete file",
                            errorCode = ErrorCode.PERMISSION_DENIED
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied deleting ${file.path}")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error deleting ${file.path}")
            Result.Error(
                message = e.message ?: "Delete failed",
                throwable = e
            )
        }
    }

    override suspend fun rename(
        file: MediaFile,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(file.path)

            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    message = "File not found: ${file.path}",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            // Validate new name
            if (newName.isBlank() || newName.contains(File.separator)) {
                return@withContext Result.Error(
                    message = "Invalid filename",
                    errorCode = ErrorCode.INVALID_INPUT
                )
            }

            val destFile = File(sourceFile.parentFile, newName)

            if (destFile.exists()) {
                return@withContext Result.Error(
                    message = "A file with that name already exists",
                    errorCode = ErrorCode.FILE_EXISTS
                )
            }

            if (sourceFile.renameTo(destFile)) {
                Timber.d("Renamed ${file.path} to ${destFile.path}")
                Result.Success(destFile.path)
            } else {
                Result.Error(
                    message = "Rename failed",
                    errorCode = ErrorCode.PERMISSION_DENIED
                )
            }

        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied renaming ${file.path}")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error renaming ${file.path}")
            Result.Error(
                message = e.message ?: "Rename failed",
                throwable = e
            )
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists()) {
                return@withContext if (dir.isDirectory) {
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        message = "Path exists but is not a directory",
                        errorCode = ErrorCode.FILE_EXISTS
                    )
                }
            }

            if (dir.mkdirs()) {
                Timber.d("Created directory: $path")
                Result.Success(Unit)
            } else {
                Result.Error(
                    message = "Failed to create directory",
                    errorCode = ErrorCode.PERMISSION_DENIED
                )
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied creating directory $path")
            Result.Error(
                message = "Permission denied",
                throwable = e,
                errorCode = ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating directory $path")
            Result.Error(
                message = e.message ?: "Failed to create directory",
                throwable = e
            )
        }
    }

    override suspend fun getFileInfo(path: String): Result<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.Error(
                    message = "File not found: $path",
                    errorCode = ErrorCode.FILE_NOT_FOUND
                )
            }

            val mediaFile = MediaFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                date = Date(file.lastModified()),
                type = detectMediaType(file.extension.lowercase()),
                isDirectory = file.isDirectory
            )

            Result.Success(mediaFile)

        } catch (e: Exception) {
            Timber.e(e, "Error getting file info for $path")
            Result.Error(
                message = e.message ?: "Failed to get file info",
                throwable = e
            )
        }
    }

    // Private helper methods

    private fun copyWithProgress(source: File, dest: File, onProgress: ((Float) -> Unit)?) {
        val totalBytes = source.length()
        var bytesCopied = 0L

        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    if (totalBytes > 0 && onProgress != null) {
                        onProgress(bytesCopied.toFloat() / totalBytes)
                    }
                }
            }
        }
    }

    private fun detectMediaType(extension: String): MediaType {
        return when (extension) {
            "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp" -> MediaType.VIDEO
            "mp3", "m4a", "wav", "flac", "aac", "ogg", "wma" -> MediaType.AUDIO
            "gif" -> MediaType.GIF
            "pdf" -> MediaType.PDF
            "txt", "log", "json", "xml", "md" -> MediaType.TXT
            "epub" -> MediaType.EPUB
            else -> MediaType.OTHER
        }
    }
}
