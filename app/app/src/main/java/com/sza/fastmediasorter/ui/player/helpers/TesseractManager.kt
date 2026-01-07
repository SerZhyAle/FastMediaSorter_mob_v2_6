package com.sza.fastmediasorter.ui.player.helpers

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Simple Tesseract OCR manager for offline text recognition.
 * Used as fallback for Cyrillic text when ML Kit fails.
 * Auto-downloads training data files from GitHub on first use.
 * 
 * Architecture: Clean, single-purpose helper following v2 principles.
 * No complex state management - just init, recognize, release.
 */
class TesseractManager(private val context: Context) {

    private var tessApi: TessBaseAPI? = null
    private var currentLanguage: String? = null

    companion object {
        private const val TESS_DATA_DIR = "tessdata"
        private const val GITHUB_URL = "https://github.com/tesseract-ocr/tessdata_fast/raw/main/"
    }

    /**
     * Initialize Tesseract for a specific language.
     * Downloads training data if not present.
     * 
     * @param language ISO language code (rus, eng, ukr, etc.)
     * @return true if ready, false if initialization failed
     */
    suspend fun initialize(language: String): Boolean = withContext(Dispatchers.IO) {
        // Reinitialize if language changed
        if (currentLanguage == language && tessApi != null) {
            return@withContext true
        }

        try {
            // Setup paths
            val dataPath = File(context.filesDir, "tesseract")
            val tessDataDir = File(dataPath, TESS_DATA_DIR)
            tessDataDir.mkdirs()

            // Ensure training data exists
            if (!downloadTrainingDataIfNeeded(tessDataDir, language)) {
                Timber.e("Failed to obtain training data for $language")
                return@withContext false
            }

            // Clean up old instance
            tessApi?.recycle()

            // Initialize new instance
            tessApi = TessBaseAPI().apply {
                val success = init(dataPath.absolutePath, language)
                if (!success) {
                    Timber.e("Tesseract init failed for $language")
                    return@withContext false
                }
            }

            currentLanguage = language
            Timber.d("Tesseract initialized for $language")
            true
        } catch (e: Exception) {
            Timber.e(e, "Tesseract initialization error")
            false
        }
    }

    /**
     * Recognize text from bitmap.
     * Simple UTF-8 text extraction.
     * 
     * @param bitmap Image to process
     * @return Recognized text or null on failure
     */
    suspend fun recognizeText(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        try {
            val api = tessApi ?: run {
                Timber.w("Tesseract not initialized")
                return@withContext null
            }

            api.setImage(bitmap)
            val text = api.utF8Text
            api.clear()
            
            text?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.e(e, "Text recognition failed")
            null
        }
    }

    /**
     * Download training data file if not present.
     * Uses tessdata_fast for smaller file size and faster processing.
     */
    private fun downloadTrainingDataIfNeeded(dir: File, language: String): Boolean {
        val file = File(dir, "$language.traineddata")
        
        // Already have it
        if (file.exists() && file.length() > 0) {
            return true
        }

        Timber.d("Downloading training data for $language...")
        
        return try {
            val url = URL("$GITHUB_URL$language.traineddata")
            url.openStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Timber.d("Training data downloaded: ${file.length()} bytes")
            true
        } catch (e: Exception) {
            Timber.e(e, "Download failed for $language")
            file.delete() // Clean up partial download
            false
        }
    }

    /**
     * Release resources.
     * Call when OCR is no longer needed.
     */
    fun release() {
        tessApi?.recycle()
        tessApi = null
        currentLanguage = null
    }
}
