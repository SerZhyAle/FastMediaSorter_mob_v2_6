package com.sza.fastmediasorter.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber

/**
 * Helper utility for Storage Access Framework (SAF) operations.
 * Provides consistent handling of content:// URIs across the application.
 */
object SafHelper {

    /**
     * Delete a file via SAF (Storage Access Framework).
     * Tries multiple methods in order: ContentResolver.delete, DocumentsContract, DocumentFile.
     *
     * @param context Application context
     * @param contentUri The content:// URI of the file to delete
     * @param tag Optional tag for logging (e.g., "SMB executeMove")
     * @return true if deletion was successful, false otherwise
     */
    fun deleteContentUri(context: Context, contentUri: String, tag: String = "SafHelper"): Boolean {
        return try {
            // Normalize content URI for deletion (ensure content://)
            val normalizedUri = if (contentUri.startsWith("content://")) {
                contentUri
            } else {
                contentUri.replaceFirst("content:/", "content://")
            }
            val uri = Uri.parse(normalizedUri)

            // Try multiple deletion methods for SAF
            var deleted = false

            // Method 1: ContentResolver.delete
            if (!deleted) {
                try {
                    deleted = context.contentResolver.delete(uri, null, null) > 0
                } catch (e: Exception) {
                    Timber.w("$tag: ContentResolver.delete failed: ${e.message}")
                }
            }

            // Method 2: DocumentsContract.deleteDocument
            if (!deleted) {
                try {
                    deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                } catch (e: Exception) {
                    Timber.w("$tag: DocumentsContract.deleteDocument failed: ${e.message}")
                }
            }

            // Method 3: DocumentFile (wrapper)
            if (!deleted) {
                try {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    if (docFile != null && docFile.exists()) {
                        deleted = docFile.delete()
                    }
                } catch (e: Exception) {
                    Timber.w("$tag: DocumentFile.delete failed: ${e.message}")
                }
            }

            if (!deleted) {
                Timber.e("$tag: All deletion methods failed for URI: $contentUri")
            }

            deleted
        } catch (e: Exception) {
            Timber.e(e, "$tag: Failed to delete SAF URI: $contentUri")
            false
        }
    }

    /**
     * Check if a path is a content URI (SAF).
     *
     * @param path The path to check
     * @return true if path starts with content:/ or content://
     */
    fun isContentUri(path: String): Boolean {
        return path.startsWith("content:/")
    }

    /**
     * Normalize a content URI to ensure it has the proper content:// prefix.
     *
     * @param path The content path to normalize
     * @return Normalized URI string with content:// prefix
     */
    fun normalizeContentUri(path: String): String {
        return if (path.startsWith("content://")) {
            path
        } else {
            path.replaceFirst("content:/", "content://")
        }
    }

    /**
     * Parse URI string with automatic sanitization for malformed content:// URIs.
     * This is the recommended way to parse URIs that may come from SAF or user input.
     *
     * @param uriString The URI string to parse
     * @return Parsed and sanitized Uri object
     */
    fun parseUri(uriString: String): Uri {
        val normalized = if (uriString.startsWith("content:") && !uriString.startsWith("content://")) {
            uriString.replaceFirst("content:/", "content://")
        } else {
            uriString
        }
        return Uri.parse(normalized)
    }

    /**
     * Get the display name of a file from a content URI.
     *
     * @param context Application context
     * @param contentUri The content:// URI
     * @return The display name or null if not found
     */
    fun getDisplayName(context: Context, contentUri: String): String? {
        return try {
            val uri = Uri.parse(normalizeContentUri(contentUri))
            val docFile = DocumentFile.fromSingleUri(context, uri)
            docFile?.name
        } catch (e: Exception) {
            Timber.w(e, "SafHelper: Failed to get display name for: $contentUri")
            null
        }
    }

    /**
     * Get the file size from a content URI.
     *
     * @param context Application context
     * @param contentUri The content:// URI
     * @return File size in bytes, or -1 if not available
     */
    fun getFileSize(context: Context, contentUri: String): Long {
        return try {
            val uri = Uri.parse(normalizeContentUri(contentUri))
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                fd.length
            } ?: -1L
        } catch (e: Exception) {
            Timber.w(e, "SafHelper: Failed to get file size for: $contentUri")
            -1L
        }
    }

    /**
     * Get DocumentFile from URI string.
     * Tries detection if it's a Tree URI or Single URI.
     * 
     * @param context Application context
     * @param uri The URI to wrap
     * @return DocumentFile or null if invalid
     */
    fun getDocumentFileFromUri(context: Context, uri: Uri): DocumentFile? {
        return try {
            if (DocumentFile.isDocumentUri(context, uri)) {
                DocumentFile.fromSingleUri(context, uri)
            } else {
                DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
            }
        } catch (e: Exception) {
            Timber.w(e, "SafHelper: Failed to get DocumentFile for: $uri")
            null
        }
    }
}
