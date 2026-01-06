package com.sza.fastmediasorter.data.remote.ftp

import com.sza.fastmediasorter.data.common.MediaTypeUtils
import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.usecase.MediaFilePage
import com.sza.fastmediasorter.domain.usecase.MediaScanner
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import com.sza.fastmediasorter.utils.FtpPathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaScanner implementation for FTP network resources.
 * Scans remote FTP servers for media files using FtpClient.
 */
@Singleton
class FtpMediaScanner @Inject constructor(
    private val ftpClient: FtpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : MediaScanner {

    override suspend fun scanFolder(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        credentialsId: String?,
        scanSubdirectories: Boolean,
        onProgress: com.sza.fastmediasorter.domain.usecase.ScanProgressCallback?
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            Timber.d("FTP scanFolder: path=$path, credentialsId=$credentialsId")
            
            // Parse path format: ftp://server:port/remotePath
            val connectionInfo = parseFtpPath(path, credentialsId) ?: run {
                Timber.w("Invalid FTP path format: $path")
                return@withContext emptyList()
            }

            Timber.d("FTP connection info: host=${connectionInfo.host}, port=${connectionInfo.port}, user=${connectionInfo.username}, remotePath=${connectionInfo.remotePath}")

            // Connect and list files with metadata
            val connectResult = ftpClient.connect(
                host = connectionInfo.host,
                port = connectionInfo.port,
                username = connectionInfo.username,
                password = connectionInfo.password
            )

            if (connectResult.isFailure) {
                Timber.e("Failed to connect to FTP: ${connectResult.exceptionOrNull()?.message}")
                return@withContext emptyList()
            }

            // List files with metadata (size, timestamp) in remote path (throttled to avoid network overload)
            val resourceKey = "ftp://${connectionInfo.host}:${connectionInfo.port}"
            val filesResult = ConnectionThrottleManager.withThrottle(
                protocol = ConnectionThrottleManager.ProtocolLimits.FTP,
                resourceKey = resourceKey,
                highPriority = false
            ) {
                ftpClient.listFilesWithMetadata(connectionInfo.remotePath, recursive = scanSubdirectories)
            }
            ftpClient.disconnect()

            if (filesResult.isFailure) {
                val exception = filesResult.exceptionOrNull() ?: IOException("Unknown FTP error")
                Timber.e("Failed to list FTP files: ${exception.message}")
                throw IOException("FTP connection error: ${exception.message}", exception)
            }

            // Filter and convert to MediaFile with real size/date from FTPFile
            filesResult.getOrNull()?.mapNotNull { ftpFile ->
                // Skip trash folders created by soft-delete
                if (ftpFile.name.startsWith(".trash")) {
                    return@mapNotNull null
                }
                
                val mediaType = getMediaType(ftpFile.name)
                if (mediaType != null && supportedTypes.contains(mediaType)) {
                    val fileSize = ftpFile.size
                    val timestamp = ftpFile.timestamp?.timeInMillis ?: 0L
                    
                    // Apply size filter if specified based on media type
                    if (sizeFilter != null) {
                        val minSize = when (mediaType) {
                            MediaType.IMAGE, MediaType.GIF -> sizeFilter.imageSizeMin
                            MediaType.VIDEO -> sizeFilter.videoSizeMin
                            MediaType.AUDIO -> sizeFilter.audioSizeMin
                            MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> 0L // No filter for TEXT/PDF/EPUB
                        }
                        val maxSize = when (mediaType) {
                            MediaType.IMAGE, MediaType.GIF -> sizeFilter.imageSizeMax
                            MediaType.VIDEO -> sizeFilter.videoSizeMax
                            MediaType.AUDIO -> sizeFilter.audioSizeMax
                            MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> Long.MAX_VALUE // No filter for TEXT/PDF/EPUB
                        }
                        
                        if (fileSize < minSize || fileSize > maxSize) {
                            return@mapNotNull null
                        }
                    }
                    
                    MediaFile(
                        name = ftpFile.name,
                        path = buildFullFtpPath(connectionInfo, ftpFile.name),
                        size = fileSize,
                        createdDate = timestamp,
                        type = mediaType
                    )
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error scanning FTP folder: $path")
            emptyList()
        }
    }

    override suspend fun scanFolderPaged(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        offset: Int,
        limit: Int,
        credentialsId: String?,
        scanSubdirectories: Boolean
    ): MediaFilePage = withContext(Dispatchers.IO) {
        try {
            // For simplicity, reuse scanFolder and apply offset/limit
            // TODO: optimize FTP client to support native pagination
            val allFiles = scanFolder(path, supportedTypes, sizeFilter, credentialsId)
            
            val pageFiles = allFiles.drop(offset).take(limit)
            val hasMore = offset + limit < allFiles.size
            
            Timber.d("FtpMediaScanner paged: offset=$offset, limit=$limit, returned=${pageFiles.size}, hasMore=$hasMore")
            MediaFilePage(pageFiles, hasMore)
        } catch (e: Exception) {
            Timber.e(e, "Error scanning FTP folder (paged): $path")
            MediaFilePage(emptyList(), false)
        }
    }

    override suspend fun getFileCount(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        credentialsId: String?,
        scanSubdirectories: Boolean
    ): Int = withContext(Dispatchers.IO) {
        try {
            Timber.d("FTP getFileCount: path=$path")
            
            val connectionInfo = parseFtpPath(path, credentialsId) ?: run {
                Timber.w("Invalid FTP path format: $path")
                return@withContext 0
            }

            val connectResult = ftpClient.connect(
                host = connectionInfo.host,
                port = connectionInfo.port,
                username = connectionInfo.username,
                password = connectionInfo.password
            )

            if (connectResult.isFailure) {
                Timber.e("Failed to connect to FTP for file count: ${connectResult.exceptionOrNull()?.message}")
                return@withContext 0
            }

            val resourceKey = "ftp://${connectionInfo.host}:${connectionInfo.port}"
            val filesResult = ConnectionThrottleManager.withThrottle(
                protocol = ConnectionThrottleManager.ProtocolLimits.FTP,
                resourceKey = resourceKey,
                highPriority = false
            ) {
                ftpClient.listFilesWithMetadata(connectionInfo.remotePath, recursive = scanSubdirectories)
            }
            ftpClient.disconnect()

            if (filesResult.isFailure) {
                Timber.e("Failed to list FTP files for count: ${filesResult.exceptionOrNull()?.message}")
                return@withContext 0
            }

            // Count only matching files without creating MediaFile objects
            val count = filesResult.getOrNull()?.count { ftpFile ->
                val mediaType = getMediaType(ftpFile.name)
                if (mediaType == null || !supportedTypes.contains(mediaType)) {
                    false
                } else if (sizeFilter == null) {
                    true
                } else {
                    val fileSize = ftpFile.size
                    val minSize = when (mediaType) {
                        MediaType.IMAGE, MediaType.GIF -> sizeFilter.imageSizeMin
                        MediaType.VIDEO -> sizeFilter.videoSizeMin
                        MediaType.AUDIO -> sizeFilter.audioSizeMin
                        MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> 0L
                    }
                    val maxSize = when (mediaType) {
                        MediaType.IMAGE, MediaType.GIF -> sizeFilter.imageSizeMax
                        MediaType.VIDEO -> sizeFilter.videoSizeMax
                        MediaType.AUDIO -> sizeFilter.audioSizeMax
                        MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> Long.MAX_VALUE
                    }
                    fileSize in minSize..maxSize
                }
            } ?: 0
            
            Timber.d("FTP getFileCount result: $count files")
            count
        } catch (e: Exception) {
            Timber.e(e, "Error counting FTP files in: $path")
            0
        }
    }

    override suspend fun isWritable(path: String, credentialsId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("FTP isWritable: path=$path, credentialsId=$credentialsId")
            
            val connectionInfo = parseFtpPath(path, credentialsId) ?: run {
                Timber.w("Invalid FTP path format for isWritable: $path")
                return@withContext false
            }

            val connectResult = ftpClient.connect(
                host = connectionInfo.host,
                port = connectionInfo.port,
                username = connectionInfo.username,
                password = connectionInfo.password
            )

            if (connectResult.isFailure) {
                Timber.e("Failed to connect to FTP for writable check")
                return@withContext false
            }

            // For FTP we can't easily check permissions without attempting write
            // Assume writable if connection succeeds
            ftpClient.disconnect()
            Timber.d("FTP isWritable result: true")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking FTP writable: $path")
            false
        }
    }

    private suspend fun parseFtpPath(path: String, credentialsId: String?): ConnectionInfo? {
        Timber.d("Parsing FTP path: $path, credentialsId=$credentialsId")
        
        // Parse path using utility
        val pathInfo = FtpPathUtils.parseFtpPath(path) ?: run {
            Timber.w("Failed to parse FTP path")
            return null
        }
        val (host, port, remotePath) = pathInfo
        
        Timber.d("Parsed FTP URL: host=$host, port=$port, remotePath=$remotePath")

        // Get credentials from database
        if (credentialsId == null) {
            Timber.w("No credentials ID provided for FTP connection")
            return null
        }

        val credentials = credentialsRepository.getByCredentialId(credentialsId)
        if (credentials == null) {
            Timber.w("Credentials not found for ID: $credentialsId")
            return null
        }
        
        Timber.d("Found credentials: type=${credentials.type}, username=${credentials.username}")

        return ConnectionInfo(
            host = host,
            port = port,
            username = credentials.username,
            password = credentials.password,
            remotePath = remotePath
        )
    }

    private fun buildFullFtpPath(connectionInfo: ConnectionInfo, fileName: String): String {
        val cleanRemotePath = connectionInfo.remotePath.trimEnd('/')
        val cleanFileName = fileName.trimStart('/')
        
        // If remotePath is empty (was "/"), don't add extra slash
        val fullPath = if (cleanRemotePath.isEmpty()) {
            "ftp://${connectionInfo.host}:${connectionInfo.port}/$cleanFileName"
        } else {
            "ftp://${connectionInfo.host}:${connectionInfo.port}$cleanRemotePath/$cleanFileName"
        }
        
        // Verbose logging - too noisy for large file lists
        // Timber.d("buildFullFtpPath: remotePath='${connectionInfo.remotePath}', fileName='$fileName' -> '$fullPath'")
        return fullPath
    }

    private fun getMediaType(fileName: String): MediaType? {
        return MediaTypeUtils.getMediaType(fileName)
    }

    private data class ConnectionInfo(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val remotePath: String
    )
}
