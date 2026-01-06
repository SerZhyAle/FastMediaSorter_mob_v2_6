package com.sza.fastmediasorter.data.cloud

import android.content.Context
import com.sza.fastmediasorter.data.network.ConnectionThrottleManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import com.sza.fastmediasorter.domain.usecase.MediaFilePage
import com.sza.fastmediasorter.domain.usecase.MediaScanner
import com.sza.fastmediasorter.domain.usecase.ScanProgressCallback
import com.sza.fastmediasorter.domain.usecase.SizeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

import com.sza.fastmediasorter.data.common.MediaTypeUtils

@Singleton
class CloudMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleDriveClient: GoogleDriveRestClient,
    private val dropboxClient: DropboxClient,
    private val oneDriveRestClient: OneDriveRestClient,
    private val resourceRepository: ResourceRepository
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
            // For cloud resources, path can be either:
            // 1. Full path like "cloud://google_drive/FOLDER_ID"
            // 2. Just the FOLDER_ID (for backward compatibility)
            val folderId = if (path.startsWith("cloud://")) {
                // Extract folder ID from full path: cloud://provider/FOLDER_ID
                path.substringAfterLast("/")
            } else {
                path
            }
            
            // Find resource by cloudFolderId to determine cloud provider
            val resources = resourceRepository.getAllResourcesSync()
            val resource = resources.find { it.cloudFolderId == folderId }
                ?: return@withContext emptyList()
            
            val client = getClient(resource.cloudProvider) ?: return@withContext emptyList()
            
            // Ensure authenticated
            ensureAuthenticated(client, resource.cloudProvider)
            
            // Scan folder (throttled to avoid network overload)
            val resourceKey = "cloud://${resource.cloudProvider}/${folderId}"
            
            val allCloudFiles = if (scanSubdirectories) {
                // Recursive scan: collect files from all subfolders
                listFilesRecursive(client, folderId, resourceKey)
            } else {
                // Single level scan
                when (val result = ConnectionThrottleManager.withThrottle(
                    protocol = ConnectionThrottleManager.ProtocolLimits.CLOUD,
                    resourceKey = resourceKey,
                    highPriority = false,
                    operation = {
                        client.listFiles(folderId)
                    }
                )) {
                    is CloudResult.Success -> result.data.first
                    is CloudResult.Error -> emptyList()
                }
            }
            
            // Filter and convert to MediaFile
            allCloudFiles.mapNotNull { cloudFile ->
                if (!cloudFile.isFolder) {
                    // Try MIME type first, then fallback to extension
                    val mediaType = MediaTypeUtils.getMediaTypeFromMime(cloudFile.mimeType) 
                        ?: MediaTypeUtils.getMediaType(cloudFile.name)
                    
                    if (mediaType != null && supportedTypes.contains(mediaType)) {
                        // Apply size filter
                        if (sizeFilter != null && !MediaTypeUtils.isFileSizeInRange(cloudFile.size, mediaType, sizeFilter)) {
                            return@mapNotNull null
                        }
                        
                        val provider = resource.cloudProvider?.name?.lowercase() ?: "unknown"
                        MediaFile(
                            name = cloudFile.name,
                            path = "cloud://$provider/${cloudFile.id}",
                            type = mediaType,
                            size = cloudFile.size,
                            createdDate = cloudFile.modifiedDate,
                            thumbnailUrl = cloudFile.thumbnailUrl,
                            webViewUrl = cloudFile.webViewUrl
                        )
                    } else null
                } else null
            }
        } catch (e: IllegalStateException) {
            // Re-throw authentication errors to be handled by ViewModel
            if (e.message?.contains("Interactive sign-in required", ignoreCase = true) == true ||
                e.message?.contains("Not authenticated", ignoreCase = true) == true ||
                e.message?.contains("Authentication cancelled", ignoreCase = true) == true) {
                throw e
            } else {
                Timber.e(e, "Error scanning cloud folder")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning cloud folder")
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
            val resourceId = path.toLongOrNull() ?: return@withContext MediaFilePage(emptyList(), false)
            val resource = resourceRepository.getResourceById(resourceId) ?: return@withContext MediaFilePage(emptyList(), false)
            
            val client = getClient(resource.cloudProvider) ?: return@withContext MediaFilePage(emptyList(), false)
            
            ensureAuthenticated(client, resource.cloudProvider)
            
            // Get all files first (cloud APIs don't support offset-based pagination natively)
            val allFiles = scanFolder(path, supportedTypes, sizeFilter, credentialsId)
            
            // Apply pagination
            val start = offset.coerceAtMost(allFiles.size)
            val end = (offset + limit).coerceAtMost(allFiles.size)
            val pageFiles = if (start < end) allFiles.subList(start, end) else emptyList()
            val hasMore = end < allFiles.size
            
            MediaFilePage(pageFiles, hasMore)
        } catch (e: IllegalStateException) {
            // Re-throw authentication errors to be handled by ViewModel
            if (e.message?.contains("Interactive sign-in required", ignoreCase = true) == true ||
                e.message?.contains("Not authenticated", ignoreCase = true) == true ||
                e.message?.contains("Authentication cancelled", ignoreCase = true) == true) {
                throw e
            } else {
                Timber.e(e, "Error scanning cloud folder paged")
                MediaFilePage(emptyList(), false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning cloud folder paged")
            MediaFilePage(emptyList(), false)
        }
    }

    override suspend fun getFileCount(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter?,
        credentialsId: String?,
        scanSubdirectories: Boolean
    ): Int {
        // Fast count: use paged scan with limit 1000
        val page = scanFolderPaged(path, supportedTypes, sizeFilter, offset = 0, limit = 1000, credentialsId, scanSubdirectories)
        // If we got exactly 1000 files, there are likely more (return 1000 to show ">1000")
        // If we got less, that's the actual count
        return page.files.size
    }

    override suspend fun isWritable(path: String, credentialsId: String?): Boolean {
        // Cloud storage is always writable if authenticated
        return true
    }

    private fun getClient(provider: CloudProvider?): CloudStorageClient? {
        return when (provider) {
            CloudProvider.GOOGLE_DRIVE -> googleDriveClient
            CloudProvider.DROPBOX -> dropboxClient
            CloudProvider.ONEDRIVE -> oneDriveRestClient
            null -> null
        }
    }

    private suspend fun ensureAuthenticated(client: CloudStorageClient, provider: CloudProvider?) {
        when (provider) {
            CloudProvider.GOOGLE_DRIVE -> {
                // Try to authenticate (will use cached account or fail)
                when (val result = client.authenticate()) {
                    is AuthResult.Success -> {
                        Timber.d("Cloud client authenticated: ${result.accountName}")
                    }
                    is AuthResult.Error -> {
                        Timber.e("Cloud authentication failed: ${result.message}")
                        // User-friendly message for re-authentication requirement
                        val message = if (result.message.contains("Interactive sign-in required")) {
                            "Authorization required. Please delete and re-add this cloud resource to update permissions."
                        } else {
                            "Not authenticated with $provider: ${result.message}"
                        }
                        throw IllegalStateException(message)
                    }
                    AuthResult.Cancelled -> {
                        Timber.w("Cloud authentication cancelled")
                        throw IllegalStateException("Authentication cancelled for $provider")
                    }
                }
            }
            CloudProvider.DROPBOX, CloudProvider.ONEDRIVE -> {
                // Other providers may need different handling
                when (val result = client.authenticate()) {
                    is AuthResult.Success -> {
                        Timber.d("Cloud client authenticated: ${result.accountName}")
                    }
                    is AuthResult.Error -> {
                        throw IllegalStateException("Not authenticated with $provider: ${result.message}")
                    }
                    AuthResult.Cancelled -> {
                        throw IllegalStateException("Authentication cancelled for $provider")
                    }
                }
            }
            null -> throw IllegalStateException("Cloud provider not specified")
        }
    }
    
    /**
     * Recursively list all files in folder and all subfolders
     */
    private suspend fun listFilesRecursive(
        client: CloudStorageClient,
        folderId: String,
        resourceKey: String
    ): List<CloudFile> {
        val allFiles = mutableListOf<CloudFile>()
        
        // Get items in current folder
        when (val result = ConnectionThrottleManager.withThrottle(
            protocol = ConnectionThrottleManager.ProtocolLimits.CLOUD,
            resourceKey = resourceKey,
            highPriority = false,
            operation = {
                client.listFiles(folderId)
            }
        )) {
            is CloudResult.Success -> {
                val (cloudFiles, _) = result.data
                
                cloudFiles.forEach { cloudFile ->
                    if (cloudFile.isFolder) {
                        // Recursively scan subfolder
                        val subfolderFiles = listFilesRecursive(client, cloudFile.id, resourceKey)
                        allFiles.addAll(subfolderFiles)
                    } else {
                        // Add file to results
                        allFiles.add(cloudFile)
                    }
                }
            }
            is CloudResult.Error -> {
                Timber.e("Failed to list files in folder $folderId: ${result.message}")
            }
        }
        
        return allFiles
    }
}
