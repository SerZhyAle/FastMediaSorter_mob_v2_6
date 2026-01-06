package com.sza.fastmediasorter.data.network

import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.data.common.MediaTypeUtils
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.usecase.MediaFilePage
import com.sza.fastmediasorter.domain.usecase.MediaScanner
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import com.sza.fastmediasorter.utils.SmbPathUtils
import com.sza.fastmediasorter.data.network.model.SmbResult
import com.sza.fastmediasorter.data.network.model.SmbConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaScanner implementation for SMB network resources.
 * Scans remote SMB shares for media files using SmbClient.
 */
@Singleton
class SmbMediaScanner @Inject constructor(
    private val smbClient: SmbClient,
    private val credentialsRepository: NetworkCredentialsRepository
    // Metadata extraction removed - loaded on-demand
) : MediaScanner {

    companion object {
        // Extensions moved to MediaTypeUtils
    }

    override suspend fun scanFolder(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        credentialsId: String?,
        scanSubdirectories: Boolean,
        onProgress: com.sza.fastmediasorter.domain.usecase.ScanProgressCallback?
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        scanFolderWithProgress(path, supportedTypes, sizeFilter, credentialsId, scanSubdirectories, onProgress)
    }
    
    /**
     * Scan folder with progress callback support
     */
    suspend fun scanFolderWithProgress(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        credentialsId: String?,
        scanSubdirectories: Boolean,
        progressCallback: com.sza.fastmediasorter.domain.usecase.ScanProgressCallback?
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            // Parse path format: smb://server:port/share/path
            val connectionInfo = parseSmbPath(path, credentialsId) ?: run {
                Timber.w("Invalid SMB path format: $path")
                return@withContext emptyList()
            }

            // Get all supported extensions
            val extensions = MediaTypeUtils.buildExtensionsSet(supportedTypes)

            // Scan SMB folder with progress callback (throttled to avoid network overload)
            val resourceKey = "smb://${connectionInfo.connectionInfo.server}:${connectionInfo.connectionInfo.port}"
            
            when (val result = ConnectionThrottleManager.withThrottle(
                protocol = ConnectionThrottleManager.ProtocolLimits.SMB,
                resourceKey = resourceKey,
                highPriority = false
            ) {
                smbClient.scanMediaFiles(
                    connectionInfo = connectionInfo.connectionInfo,
                    remotePath = connectionInfo.remotePath,
                    extensions = extensions,
                    scanSubdirectories = scanSubdirectories,
                    progressCallback = progressCallback
                )
            }) {
                is SmbResult.Success -> {
                    // Convert SmbFileInfo to MediaFile
                    result.data.mapNotNull { fileInfo ->
                        val mediaType = MediaTypeUtils.getMediaType(fileInfo.name)
                        if (mediaType != null && supportedTypes.contains(mediaType)) {
                            // Apply size filter if provided
                            if (sizeFilter != null && !MediaTypeUtils.isFileSizeInRange(fileInfo.size, mediaType, sizeFilter)) {
                                return@mapNotNull null
                            }

                            // TODO: Extract EXIF from SMB files (requires downloading file header)
                            // For now, EXIF extraction is skipped for network files to avoid slow scanning
                            // EXIF can be extracted on-demand during image viewing
                            
                            // TODO: Extract video metadata from SMB files (requires downloading file or partial read)
                            // For now, video metadata extraction is skipped for network files to avoid slow scanning
                            // Video metadata can be extracted on-demand during video viewing
                            
                            MediaFile(
                                name = fileInfo.name,
                                path = buildFullSmbPath(connectionInfo, fileInfo.path),
                                size = fileInfo.size,
                                createdDate = fileInfo.lastModified,
                                type = mediaType
                            )
                        } else null
                    }
                }
                is SmbResult.Error -> {
                    Timber.e("Error scanning SMB folder: ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning SMB folder: $path")
            emptyList()
        }
    }

    /**
     * Fetch single file metadata directly from SMB share without full folder rescan.
     */
    suspend fun getFileByPath(
        path: String,
        supportedTypes: Set<MediaType>,
        credentialsId: String?
    ): MediaFile? = withContext(Dispatchers.IO) {
        try {
            val connectionInfo = parseSmbPath(path, credentialsId) ?: run {
                Timber.w("getFileByPath: Invalid SMB path: $path")
                return@withContext null
            }

            if (connectionInfo.remotePath.isEmpty()) {
                Timber.w("getFileByPath: Remote path is empty for $path")
                return@withContext null
            }

            val fileResult = smbClient.getFileInfo(
                connectionInfo = connectionInfo.connectionInfo,
                remotePath = connectionInfo.remotePath
            )

            when (fileResult) {
                is SmbResult.Success -> {
                    val smbFile = fileResult.data
                    val mediaType = MediaTypeUtils.getMediaType(smbFile.name)
                    if (mediaType == null || !supportedTypes.contains(mediaType)) {
                        Timber.d("getFileByPath: Unsupported media type for ${smbFile.name}")
                        null
                    } else {
                        MediaFile(
                            name = smbFile.name,
                            path = buildFullSmbPath(connectionInfo, smbFile.path),
                            size = smbFile.size,
                            createdDate = smbFile.lastModified,
                            type = mediaType
                        )
                    }
                }
                is SmbResult.Error -> {
                    Timber.w("getFileByPath: ${fileResult.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "getFileByPath: Error resolving SMB file: $path")
            null
        }
    }

    /**
     * Scan folder with limit (for lazy loading initial batch)
     * Returns first maxFiles files quickly
     */
    suspend fun scanFolderChunked(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter? = null,
        maxFiles: Int = 100,
        credentialsId: String? = null,
        scanSubdirectories: Boolean = true
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        try {
            Timber.d("scanFolderChunked: START - path=$path, maxFiles=$maxFiles, scanSubdirectories=$scanSubdirectories, credentialsId=$credentialsId")
            
            val connectionInfo = parseSmbPath(path, credentialsId) ?: run {
                Timber.w("Invalid SMB path format: $path")
                return@withContext emptyList()
            }

            Timber.d("scanFolderChunked: Parsed path - share=${connectionInfo.connectionInfo.shareName}, remotePath=${connectionInfo.remotePath}")

            val extensions = MediaTypeUtils.buildExtensionsSet(supportedTypes)
            
            Timber.d("scanFolderChunked: Extensions=$extensions")

            // Use chunked scan method
            when (val result = smbClient.scanMediaFilesChunked(
                connectionInfo = connectionInfo.connectionInfo,
                remotePath = connectionInfo.remotePath,
                extensions = extensions,
                maxFiles = maxFiles,
                scanSubdirectories = scanSubdirectories
            )) {
                is SmbResult.Success -> {
                    Timber.d("scanFolderChunked: Got ${result.data.size} files from smbClient")
                    
                    val mediaFiles = result.data.mapNotNull { fileInfo ->
                        val mediaType = MediaTypeUtils.getMediaType(fileInfo.name)
                        if (mediaType != null && supportedTypes.contains(mediaType)) {
                            if (sizeFilter != null && !MediaTypeUtils.isFileSizeInRange(fileInfo.size, mediaType, sizeFilter)) {
                                return@mapNotNull null
                            }

                            MediaFile(
                                name = fileInfo.name,
                                path = buildFullSmbPath(connectionInfo, fileInfo.path),
                                size = fileInfo.size,
                                createdDate = fileInfo.lastModified,
                                type = mediaType
                            )
                        } else null
                    }
                    
                    Timber.d("scanFolderChunked: Returning ${mediaFiles.size} MediaFile objects")
                    mediaFiles
                }
                is SmbResult.Error -> {
                    Timber.e("Error scanning SMB folder (chunked): ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning SMB folder (chunked): $path")
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
            val connectionInfo = parseSmbPath(path, credentialsId) ?: run {
                Timber.w("Invalid SMB path format: $path")
                return@withContext MediaFilePage(emptyList(), false)
            }

            val extensions = MediaTypeUtils.buildExtensionsSet(supportedTypes)

            // Use optimized paged scan with native offset/limit support
            when (val result = smbClient.scanMediaFilesPaged(
                connectionInfo = connectionInfo.connectionInfo,
                remotePath = connectionInfo.remotePath,
                extensions = extensions,
                offset = offset,
                limit = limit,
                scanSubdirectories = scanSubdirectories
            )) {
                is SmbResult.Success -> {
                    // Convert to MediaFile list with optional size filtering
                    val mediaFiles = result.data.mapNotNull { fileInfo ->
                        val mediaType = MediaTypeUtils.getMediaType(fileInfo.name)
                        if (mediaType != null && supportedTypes.contains(mediaType)) {
                            if (sizeFilter != null && !MediaTypeUtils.isFileSizeInRange(fileInfo.size, mediaType, sizeFilter)) {
                                return@mapNotNull null
                            }

                            MediaFile(
                                name = fileInfo.name,
                                path = buildFullSmbPath(connectionInfo, fileInfo.path),
                                size = fileInfo.size,
                                createdDate = fileInfo.lastModified,
                                type = mediaType
                            )
                        } else null
                    }
                    
                    // If we got fewer files than requested, no more pages
                    val hasMore = mediaFiles.size >= limit
                    
                    Timber.d("SmbMediaScanner paged: offset=$offset, limit=$limit, returned=${mediaFiles.size}, hasMore=$hasMore")
                    MediaFilePage(mediaFiles, hasMore)
                }
                is SmbResult.Error -> {
                    Timber.e("Error scanning SMB folder (paged): ${result.message}")
                    MediaFilePage(emptyList(), false)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning SMB folder (paged): $path")
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
            val connectionInfo = parseSmbPath(path, credentialsId) ?: run {
                Timber.w("Invalid SMB path format for count: $path")
                return@withContext 0
            }

            // Get all supported extensions
            val extensions = MediaTypeUtils.buildExtensionsSet(supportedTypes)

            // Use optimized count method (no SmbFileInfo objects created)
            when (val result = smbClient.countMediaFiles(
                connectionInfo = connectionInfo.connectionInfo,
                remotePath = connectionInfo.remotePath,
                extensions = extensions
            )) {
                is SmbResult.Success -> {
                    // Note: sizeFilter is ignored for counting (would require fetching size for each file)
                    result.data
                }
                is SmbResult.Error -> {
                    Timber.e("Error counting SMB files: ${result.message}")
                    0
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error counting SMB files in: $path")
            0
        }
    }

    override suspend fun isWritable(path: String, credentialsId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val pathInfo = parseSmbPath(path, credentialsId) ?: return@withContext false

            // Test actual write permission by creating a test file
            when (val result = smbClient.checkWritePermission(pathInfo.connectionInfo, pathInfo.remotePath)) {
                is SmbResult.Success -> result.data
                is SmbResult.Error -> {
                    Timber.w("SMB path not writable: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking SMB write access for: $path")
            false
        }
    }

    /**
     * Parse SMB path format: smb://server:port/share/path
     */
    private suspend fun parseSmbPath(path: String, credentialsId: String?): SmbConnectionInfoWithPath? {
        return try {
            // Parse path using utility
            val pathInfo = SmbPathUtils.parseSmbPath(path) ?: return null
            val server = pathInfo.connectionInfo.server
            val share = pathInfo.connectionInfo.shareName
            val port = pathInfo.connectionInfo.port
            val remotePath = pathInfo.remotePath
            
            // Try to get credentials from database using credentialsId first
            val credentials = if (credentialsId != null) {
                credentialsRepository.getByCredentialId(credentialsId)
            } else {
                // Fallback to old behavior for backward compatibility
                credentialsRepository.getByServerAndShare(server, share)
            }
            
            SmbConnectionInfoWithPath(
                connectionInfo = SmbConnectionInfo(
                    server = server,
                    shareName = share,
                    username = credentials?.username ?: "",
                    password = credentials?.password ?: "",
                    domain = credentials?.domain ?: "",
                    port = port
                ),
                remotePath = remotePath
            )
        } catch (e: Exception) {
            Timber.e(e, "Error parsing SMB path: $path")
            null
        }
    }

    private fun buildFullSmbPath(connectionInfo: SmbConnectionInfoWithPath, filePath: String): String {
        return "smb://${connectionInfo.connectionInfo.server}:${connectionInfo.connectionInfo.port}/${connectionInfo.connectionInfo.shareName}/$filePath"
    }

    private data class SmbConnectionInfoWithPath(
        val connectionInfo: SmbConnectionInfo,
        val remotePath: String
    )
}
