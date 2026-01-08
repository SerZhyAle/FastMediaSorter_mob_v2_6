package com.sza.fastmediasorter.integrations

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Google Lens integration.
 * Supports sharing images and PDF pages to Google Lens for visual search.
 */
@Singleton
class GoogleLensHelper @Inject constructor() {

    companion object {
        // Google Lens package name
        private const val GOOGLE_LENS_PACKAGE = "com.google.ar.lens"
        
        // Alternative: Google App with Lens
        private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
        
        // Play Store URL for Google Lens
        private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$GOOGLE_LENS_PACKAGE"
        
        // Cache directory for temporary images
        private const val LENS_CACHE_DIR = "lens_cache"
    }

    /**
     * Check if Google Lens is available on the device.
     * Returns true if either the standalone Lens app or Google app (with Lens) is installed.
     */
    fun isGoogleLensAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        
        return try {
            // Check for standalone Lens app
            packageManager.getPackageInfo(GOOGLE_LENS_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                // Check for Google app (which includes Lens)
                packageManager.getPackageInfo(GOOGLE_APP_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Share an image file to Google Lens.
     * 
     * @param context The context
     * @param imageFile The image file to share
     * @return true if the intent was launched successfully, false otherwise
     */
    fun shareToGoogleLens(context: Context, imageFile: File): Boolean {
        if (!imageFile.exists()) {
            Timber.e("Image file does not exist: ${imageFile.absolutePath}")
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            shareUriToGoogleLens(context, uri, getMimeType(imageFile.extension))
        } catch (e: Exception) {
            Timber.e(e, "Failed to share image to Google Lens")
            false
        }
    }

    /**
     * Share a bitmap to Google Lens.
     * The bitmap is saved to a temporary file first.
     * 
     * @param context The context
     * @param bitmap The bitmap to share
     * @param fileName Optional filename for the temporary file
     * @return true if the intent was launched successfully, false otherwise
     */
    fun shareToGoogleLens(context: Context, bitmap: Bitmap, fileName: String = "lens_image.jpg"): Boolean {
        return try {
            // Create cache directory if needed
            val cacheDir = File(context.cacheDir, LENS_CACHE_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Save bitmap to temporary file
            val tempFile = File(cacheDir, fileName)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Share the file
            shareToGoogleLens(context, tempFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to share bitmap to Google Lens")
            false
        }
    }

    /**
     * Share a content URI to Google Lens.
     * 
     * @param context The context
     * @param uri The content URI of the image
     * @param mimeType The MIME type of the image
     * @return true if the intent was launched successfully, false otherwise
     */
    fun shareUriToGoogleLens(context: Context, uri: Uri, mimeType: String = "image/*"): Boolean {
        return try {
            val intent = createLensIntent(uri, mimeType)
            
            // Grant URI permission
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            // Try to find an activity that can handle this
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Try alternative approach with chooser
                val chooser = Intent.createChooser(intent, "Open with Google Lens")
                if (chooser.resolveActivity(context.packageManager) != null) {
                    context.startActivity(chooser)
                    true
                } else {
                    Timber.w("No activity found to handle Google Lens intent")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to share URI to Google Lens")
            false
        }
    }

    /**
     * Create the intent for Google Lens.
     */
    private fun createLensIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            
            // Try to target Google Lens specifically
            setPackage(GOOGLE_LENS_PACKAGE)
        }
    }

    /**
     * Open Play Store to install Google Lens.
     */
    fun openPlayStoreForGoogleLens(context: Context) {
        try {
            // Try Play Store app first
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_LENS_PACKAGE"))
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(marketIntent)
            } else {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Play Store for Google Lens")
        }
    }

    /**
     * Clean up temporary files created for Lens sharing.
     */
    fun clearCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, LENS_CACHE_DIR)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Lens cache")
        }
    }

    /**
     * Get MIME type for image extension.
     */
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heif"
            else -> "image/*"
        }
    }
}
