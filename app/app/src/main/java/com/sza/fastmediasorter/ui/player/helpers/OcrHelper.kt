package com.sza.fastmediasorter.ui.player.helpers

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hybrid OCR implementation following SPEC.
 * Strategy: Try ML Kit first (fast), fallback to Tesseract for Cyrillic.
 * 
 * ML Kit works well for Latin scripts and some Cyrillic.
 * Tesseract provides better Cyrillic accuracy but is slower.
 */
@Singleton
class OcrHelper @Inject constructor(
    private val context: Context
) {
    
    private val mlKitRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val tesseractManager = TesseractManager(context)
    
    /**
     * Recognize text using hybrid strategy.
     * 
     * @param bitmap Image to process
     * @param preferTesseract Force Tesseract for Cyrillic-heavy content
     * @return Recognized text or null
     */
    suspend fun recognizeText(bitmap: Bitmap, preferTesseract: Boolean = false): String? {
        return try {
            if (preferTesseract) {
                // Use Tesseract directly for better Cyrillic
                recognizeWithTesseract(bitmap)
            } else {
                // Try ML Kit first (faster)
                val mlKitResult = recognizeWithMLKit(bitmap)
                
                // If ML Kit found text, use it
                if (!mlKitResult.isNullOrBlank()) {
                    mlKitResult
                } else {
                    // Fallback to Tesseract
                    Timber.d("ML Kit found no text, trying Tesseract")
                    recognizeWithTesseract(bitmap)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR failed")
            null
        }
    }
    
    /**
     * ML Kit text recognition (fast, on-device).
     */
    private suspend fun recognizeWithMLKit(bitmap: Bitmap): String? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = mlKitRecognizer.process(inputImage).await()
            result.text.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.e(e, "ML Kit recognition failed")
            null
        }
    }
    
    /**
     * Tesseract recognition (slower, better Cyrillic).
     * Uses Russian language model by default.
     */
    private suspend fun recognizeWithTesseract(bitmap: Bitmap): String? {
        return try {
            // Initialize for Russian (good for Cyrillic)
            val initialized = tesseractManager.initialize("rus")
            if (!initialized) {
                Timber.e("Tesseract initialization failed")
                return null
            }
            
            tesseractManager.recognizeText(bitmap)
        } catch (e: Exception) {
            Timber.e(e, "Tesseract recognition failed")
            null
        }
    }
    
    /**
     * Release resources when OCR is no longer needed.
     */
    fun release() {
        tesseractManager.release()
    }
}
