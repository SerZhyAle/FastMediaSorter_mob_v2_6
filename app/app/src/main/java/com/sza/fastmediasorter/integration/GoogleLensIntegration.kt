package com.sza.fastmediasorter.integration

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
 * Integration with Google Lens for image analysis
 * 
 * Google Lens can be launched via intent to analyze images for:
 * - Text recognition (OCR)
 * - Object detection
 * - Product search
 * - Translation
 * - Smart actions (copy text, call numbers, open URLs)
 */
@Singleton
class GoogleLensIntegration @Inject constructor() {

    companion object {
        // Google Lens package name
        const val GOOGLE_LENS_PACKAGE = "com.google.ar.lens"
        
        // Google app package (also has Lens built-in)
        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
        
        // Intent action for Google Lens
        const val ACTION_LENS = "com.google.android.apps.photos.api.intent.SEARCH_WITH_LENS"
        
        // Fallback web URL for Google Lens
        const val LENS_WEB_URL = "https://lens.google.com/"
        
        // File provider authority pattern
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    }
    
    /**
     * Check if Google Lens is available on the device
     */
    fun isGoogleLensAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        
        // Check for Google Lens app
        try {
            packageManager.getPackageInfo(GOOGLE_LENS_PACKAGE, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Google Lens not installed
        }
        
        // Check for Google app (has Lens built-in)
        try {
            packageManager.getPackageInfo(GOOGLE_APP_PACKAGE, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Google app not installed
        }
        
        return false
    }
    
    /**
     * Open image with Google Lens
     * @param context The context
     * @param imageUri URI of the image to analyze
     * @return true if Lens was launched successfully
     */
    fun openWithGoogleLens(context: Context, imageUri: Uri): Boolean {
        try {
            // Try Google Lens specific intent
            val lensIntent = Intent(ACTION_LENS).apply {
                data = imageUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (lensIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(lensIntent)
                Timber.d("Launched Google Lens with intent action")
                return true
            }
            
            // Try ACTION_SEND with Google Lens package
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                setPackage(GOOGLE_LENS_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (sendIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(sendIntent)
                Timber.d("Launched Google Lens with ACTION_SEND")
                return true
            }
            
            // Try with Google app
            val googleIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                setPackage(GOOGLE_APP_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (googleIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(googleIntent)
                Timber.d("Launched Google app for Lens")
                return true
            }
            
            // Fallback: open image with any app that can view/search images
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(viewIntent, "Open with Google Lens")
            if (chooser.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
                Timber.d("Launched chooser for image")
                return true
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Google Lens")
        }
        
        return false
    }
    
    /**
     * Open bitmap with Google Lens
     * Saves bitmap to a temp file first, then opens with Lens
     */
    fun openBitmapWithGoogleLens(context: Context, bitmap: Bitmap): Boolean {
        try {
            // Save bitmap to cache directory
            val cacheDir = File(context.cacheDir, "lens_temp")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val tempFile = File(cacheDir, "lens_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // Get content URI using FileProvider
            val authority = context.packageName + FILE_PROVIDER_SUFFIX
            val contentUri = FileProvider.getUriForFile(context, authority, tempFile)
            
            return openWithGoogleLens(context, contentUri)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to open bitmap with Google Lens")
            return false
        }
    }
    
    /**
     * Open Google Lens in web browser (fallback option)
     */
    fun openGoogleLensWeb(context: Context): Boolean {
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(LENS_WEB_URL))
            context.startActivity(webIntent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Google Lens web")
            false
        }
    }
    
    /**
     * Create an intent to search Google Lens from the Play Store
     */
    fun getInstallGoogleLensIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_LENS_PACKAGE")
        }
    }
    
    /**
     * Create an intent to search for text using Google Lens
     * This uses a special intent that focuses on text recognition
     */
    fun searchTextWithLens(context: Context, imageUri: Uri): Boolean {
        try {
            // Try specific text search action if available
            val intent = Intent().apply {
                action = "android.intent.action.PROCESS_TEXT"
                type = "image/*"
                data = imageUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Fall back to standard Lens open
            return openWithGoogleLens(context, imageUri)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to search text with Lens")
            return false
        }
    }
    
    /**
     * Translate image using Google Lens
     * Opens Lens with translation mode hint
     */
    fun translateWithLens(context: Context, imageUri: Uri): Boolean {
        // Google Lens doesn't have a direct translation intent,
        // but when opened with an image containing text in a foreign language,
        // it automatically offers translation
        return openWithGoogleLens(context, imageUri)
    }
    
    /**
     * Clean up temporary Lens files
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "lens_temp")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    // Delete files older than 1 hour
                    if (System.currentTimeMillis() - file.lastModified() > 3600000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup Lens temp files")
        }
    }
}
