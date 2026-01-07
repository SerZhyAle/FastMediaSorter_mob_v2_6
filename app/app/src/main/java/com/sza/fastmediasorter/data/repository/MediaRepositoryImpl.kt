package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.scanner.FtpMediaScanner
import com.sza.fastmediasorter.data.scanner.LocalMediaScanner
import com.sza.fastmediasorter.data.scanner.SftpMediaScanner
import com.sza.fastmediasorter.data.scanner.SmbMediaScanner
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.MediaRepository
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaRepository.
 * Supports local file scanning and network scanning (SMB, SFTP, FTP).
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val credentialsRepository: NetworkCredentialsRepository,
    private val localMediaScanner: LocalMediaScanner,
    private val smbMediaScanner: SmbMediaScanner,
    private val sftpMediaScanner: SftpMediaScanner,
    private val ftpMediaScanner: FtpMediaScanner
) : MediaRepository {

    // In-memory cache for scanned files (per resource)
    private val fileCache = mutableMapOf<Long, List<MediaFile>>()

    override suspend fun getFilesForResource(resourceId: Long): List<MediaFile> {
        // Check cache first
        fileCache[resourceId]?.let { cached ->
            Timber.d("Returning cached files for resource $resourceId: ${cached.size} files")
            return cached
        }

        // Get resource info
        val resource = resourceRepository.getResourceById(resourceId)
        if (resource == null) {
            Timber.w("Resource not found: $resourceId")
            return emptyList()
        }

        return getMediaFiles(resource)
    }

    override suspend fun getMediaFiles(resource: Resource): List<MediaFile> {
        // Check cache first
        fileCache[resource.id]?.let { cached ->
            Timber.d("Returning cached files for resource ${resource.id}: ${cached.size} files")
            return cached
        }

        // Scan based on resource type
        val files = when (resource.type) {
            ResourceType.LOCAL -> {
                localMediaScanner.scanFolder(resource.path, recursive = false)
            }
            ResourceType.SMB -> scanSmbResource(resource)
            ResourceType.SFTP -> scanSftpResource(resource)
            ResourceType.FTP -> scanFtpResource(resource)
            ResourceType.GOOGLE_DRIVE, ResourceType.ONEDRIVE, ResourceType.DROPBOX -> {
                // TODO: Implement cloud scanning
                Timber.d("Cloud scanning not yet implemented for ${resource.type}")
                emptyList()
            }
        }

        // Cache the results
        fileCache[resource.id] = files
        Timber.d("Scanned and cached ${files.size} files for resource ${resource.id}")

        return files
    }

    private suspend fun scanSmbResource(resource: Resource): List<MediaFile> {
        val credentialsId = resource.credentialsId
        if (credentialsId == null) {
            Timber.e("No credentials configured for SMB resource: ${resource.id}")
            return emptyList()
        }

        return when (val result = credentialsRepository.getCredentials(credentialsId)) {
            is Result.Success -> {
                val creds = result.data
                smbMediaScanner.scanFolder(
                    server = creds.server,
                    port = creds.port,
                    shareName = creds.shareName ?: "",
                    path = resource.path,
                    username = creds.username,
                    password = creds.password,
                    domain = creds.domain
                )
            }
            is Result.Error -> {
                Timber.e("Failed to get credentials: ${result.message}")
                emptyList()
            }
            is Result.Loading -> emptyList()
        }
    }

    private suspend fun scanSftpResource(resource: Resource): List<MediaFile> {
        val credentialsId = resource.credentialsId
        if (credentialsId == null) {
            Timber.e("No credentials configured for SFTP resource: ${resource.id}")
            return emptyList()
        }

        return when (val result = credentialsRepository.getCredentials(credentialsId)) {
            is Result.Success -> {
                val creds = result.data
                // Read SSH key if configured
                val privateKey: String? = if (creds.useSshKey && !creds.sshKeyPath.isNullOrEmpty()) {
                    try {
                        java.io.File(creds.sshKeyPath).readText()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to read SSH key from ${creds.sshKeyPath}")
                        null
                    }
                } else {
                    null
                }
                
                sftpMediaScanner.scanFolder(
                    host = creds.server,
                    port = creds.port,
                    path = resource.path,
                    username = creds.username,
                    password = if (creds.useSshKey) "" else creds.password,
                    privateKey = privateKey,
                    passphrase = null // Passphrase for key not yet supported in credentials model
                )
            }
            is Result.Error -> {
                Timber.e("Failed to get credentials: ${result.message}")
                emptyList()
            }
            is Result.Loading -> emptyList()
        }
    }

    private suspend fun scanFtpResource(resource: Resource): List<MediaFile> {
        val credentialsId = resource.credentialsId
        if (credentialsId == null) {
            Timber.e("No credentials configured for FTP resource: ${resource.id}")
            return emptyList()
        }

        return when (val result = credentialsRepository.getCredentials(credentialsId)) {
            is Result.Success -> {
                val creds = result.data
                ftpMediaScanner.scanFolder(
                    host = creds.server,
                    port = creds.port,
                    remotePath = resource.path,
                    username = creds.username,
                    password = creds.password
                )
            }
            is Result.Error -> {
                Timber.e("Failed to get credentials: ${result.message}")
                emptyList()
            }
            is Result.Loading -> emptyList()
        }
    }

    override suspend fun scanResource(resourceId: Long, forceRefresh: Boolean) {
        if (forceRefresh) {
            fileCache.remove(resourceId)
        }
        // Trigger scan by getting files
        getFilesForResource(resourceId)
    }

    override suspend fun getFileByPath(path: String): MediaFile? {
        // Search in all caches
        fileCache.values.forEach { files ->
            files.find { it.path == path }?.let { return it }
        }
        return null
    }

    override suspend fun clearCacheForResource(resourceId: Long) {
        fileCache.remove(resourceId)
        Timber.d("Cleared cache for resource $resourceId")
    }
}
