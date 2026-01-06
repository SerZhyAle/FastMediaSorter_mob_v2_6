package com.sza.fastmediasorter.data.cloud

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for parsing and normalizing cloud storage paths.
 * 
 * Cloud path format: cloud://provider/folderId/fileId or cloud://provider/folderId/path/to/file.ext
 * 
 * Handles:
 * - Path normalization (cloud:/ → cloud://)
 * - Parsing provider, folderId, and fileId from paths
 * - Path validation
 */
@Singleton
class CloudPathParser @Inject constructor() {

    /**
     * Parse cloud path to extract provider, folderId, and fileId
     * @param path Cloud path (cloud://provider/folderId/fileId or cloud://provider/folderId/path/to/file.ext)
     * @return CloudPathInfo with parsed components, or null if invalid
     */
    fun parseCloudPath(path: String): CloudPathInfo? {
        try {
            val normalizedPath = normalizePath(path)
            if (!normalizedPath.startsWith("cloud://")) return null
            
            val withoutProtocol = normalizedPath.removePrefix("cloud://")
            val parts = withoutProtocol.split("/", limit = 3)
            
            if (parts.isEmpty()) return null
            
            val provider = CloudProvider.valueOf(parts[0].uppercase())
            val folderId = if (parts.size > 1) parts[1] else null
            val fileId = if (parts.size > 2) parts[2].substringBefore('/') else folderId
            
            return CloudPathInfo(provider, folderId, fileId ?: "")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing cloud path: $path")
            return null
        }
    }

    /**
     * Normalize cloud path by ensuring proper protocol format.
     * Converts cloud:/ to cloud://
     * @param path Cloud path to normalize
     * @return Normalized path with cloud://
     */
    fun normalizePath(path: String): String {
        return if (path.startsWith("cloud:/") && !path.startsWith("cloud://")) {
            path.replaceFirst("cloud:/", "cloud://")
        } else {
            path
        }
    }

    /**
     * Check if path is a valid cloud path
     * @param path Path to check
     * @return true if path starts with cloud:/ or cloud://
     */
    fun isCloudPath(path: String): Boolean {
        return path.startsWith("cloud:/") || path.startsWith("cloud://")
    }

    /**
     * Information extracted from cloud path
     * @property provider Cloud storage provider (GOOGLE_DRIVE, DROPBOX, ONEDRIVE)
     * @property folderId Parent folder ID, may be null for root
     * @property fileId File or folder ID
     */
    data class CloudPathInfo(
        val provider: CloudProvider,
        val folderId: String?,
        val fileId: String
    )
}
