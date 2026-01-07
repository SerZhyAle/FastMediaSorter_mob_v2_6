package com.sza.fastmediasorter.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for performing OCR (Text Recognition) on images using ML Kit.
 */
@Singleton
class TextRecognitionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Process an image and extract text.
     *
     * @param bitmap The image bitmap to process
     * @return Result containing the extracted text
     */
    suspend fun processImage(bitmap: Bitmap): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            val text = visionText.text
            
            if (text.isBlank()) {
                Result.success("No text found in image")
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR failed")
            Result.failure(e)
        }
    }

    /**
     * Process an image from a URI.
     *
     * @param uri The image URI
     * @return Result containing the extracted text
     */
    suspend fun processImage(uri: Uri): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            val text = visionText.text
            
            if (text.isBlank()) {
                Result.success("No text found in image")
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR failed for URI: $uri")
            Result.failure(e)
        }
    }
}
