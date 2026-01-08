package com.sza.fastmediasorter.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager for ML Kit translation functionality.
 * 
 * Features:
 * - On-device translation with 22+ languages
 * - Language auto-detection
 * - Model download management with progress
 * - Translation caching for performance
 */
@Singleton
class TranslationManager @Inject constructor() {

    companion object {
        // Supported languages for translation
        val SUPPORTED_LANGUAGES = listOf(
            TranslateLanguage.ENGLISH,
            TranslateLanguage.UKRAINIAN,
            TranslateLanguage.RUSSIAN,
            TranslateLanguage.GERMAN,
            TranslateLanguage.FRENCH,
            TranslateLanguage.SPANISH,
            TranslateLanguage.ITALIAN,
            TranslateLanguage.PORTUGUESE,
            TranslateLanguage.POLISH,
            TranslateLanguage.DUTCH,
            TranslateLanguage.CZECH,
            TranslateLanguage.SWEDISH,
            TranslateLanguage.DANISH,
            TranslateLanguage.NORWEGIAN,
            TranslateLanguage.FINNISH,
            TranslateLanguage.TURKISH,
            TranslateLanguage.ARABIC,
            TranslateLanguage.HEBREW,
            TranslateLanguage.CHINESE,
            TranslateLanguage.JAPANESE,
            TranslateLanguage.KOREAN,
            TranslateLanguage.HINDI
        )

        // Language display names
        private val LANGUAGE_NAMES = mapOf(
            TranslateLanguage.ENGLISH to "English",
            TranslateLanguage.UKRAINIAN to "Українська",
            TranslateLanguage.RUSSIAN to "Русский",
            TranslateLanguage.GERMAN to "Deutsch",
            TranslateLanguage.FRENCH to "Français",
            TranslateLanguage.SPANISH to "Español",
            TranslateLanguage.ITALIAN to "Italiano",
            TranslateLanguage.PORTUGUESE to "Português",
            TranslateLanguage.POLISH to "Polski",
            TranslateLanguage.DUTCH to "Nederlands",
            TranslateLanguage.CZECH to "Čeština",
            TranslateLanguage.SWEDISH to "Svenska",
            TranslateLanguage.DANISH to "Dansk",
            TranslateLanguage.NORWEGIAN to "Norsk",
            TranslateLanguage.FINNISH to "Suomi",
            TranslateLanguage.TURKISH to "Türkçe",
            TranslateLanguage.ARABIC to "العربية",
            TranslateLanguage.HEBREW to "עברית",
            TranslateLanguage.CHINESE to "中文",
            TranslateLanguage.JAPANESE to "日本語",
            TranslateLanguage.KOREAN to "한국어",
            TranslateLanguage.HINDI to "हिन्दी"
        )
    }

    private val modelManager = RemoteModelManager.getInstance()
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    // Translation cache: key = "sourceText|sourceLang|targetLang"
    private val translationCache = mutableMapOf<String, String>()

    /**
     * Get the display name for a language code.
     */
    fun getLanguageName(languageCode: String): String {
        return LANGUAGE_NAMES[languageCode] ?: languageCode.uppercase()
    }

    /**
     * Detect the language of the given text.
     */
    suspend fun detectLanguage(text: String): String? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    val result = if (languageCode != "und") languageCode else null
                    Timber.d("Detected language: $result")
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Language detection failed")
                    continuation.resume(null)
                }
        }
    }

    /**
     * Translate text from source language to target language.
     * 
     * @param text The text to translate
     * @param sourceLanguage Source language code (or null for auto-detect)
     * @param targetLanguage Target language code
     * @return Translated text or null if translation failed
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String
    ): String? = withContext(Dispatchers.IO) {
        val actualSourceLang = sourceLanguage ?: detectLanguage(text) ?: TranslateLanguage.ENGLISH
        
        // Check cache first
        val cacheKey = "$text|$actualSourceLang|$targetLanguage"
        translationCache[cacheKey]?.let {
            Timber.d("Translation cache hit")
            return@withContext it
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(actualSourceLang)
            .setTargetLanguage(targetLanguage)
            .build()

        val translator = Translation.getClient(options)

        try {
            // Ensure model is downloaded
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            suspendCancellableCoroutine { continuation ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Timber.d("Model ready for $actualSourceLang -> $targetLanguage")
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Model download failed")
                        continuation.resumeWithException(e)
                    }
            }

            // Perform translation
            val result = suspendCancellableCoroutine<String> { continuation ->
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        Timber.d("Translation successful: ${text.take(50)} -> ${translated.take(50)}")
                        continuation.resume(translated)
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Translation failed")
                        continuation.resumeWithException(e)
                    }
            }

            // Cache the result
            translationCache[cacheKey] = result
            result
        } catch (e: Exception) {
            Timber.e(e, "Translation error")
            null
        } finally {
            translator.close()
        }
    }

    /**
     * Check if a language model is downloaded.
     */
    suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val model = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded ->
                    continuation.resume(isDownloaded)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to check model status")
                    continuation.resume(false)
                }
        }
    }

    /**
     * Download a language model.
     */
    suspend fun downloadModel(
        languageCode: String,
        requireWifi: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder()
            .apply { if (requireWifi) requireWifi() }
            .build()

        suspendCancellableCoroutine { continuation ->
            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    Timber.d("Model downloaded: $languageCode")
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Model download failed: $languageCode")
                    continuation.resume(false)
                }
        }
    }

    /**
     * Delete a downloaded language model.
     */
    suspend fun deleteModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        suspendCancellableCoroutine { continuation ->
            modelManager.deleteDownloadedModel(model)
                .addOnSuccessListener {
                    Timber.d("Model deleted: $languageCode")
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Model deletion failed: $languageCode")
                    continuation.resume(false)
                }
        }
    }

    /**
     * Get list of downloaded models.
     */
    suspend fun getDownloadedModels(): List<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val languages = models.map { it.language }
                    Timber.d("Downloaded models: $languages")
                    continuation.resume(languages)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to get downloaded models")
                    continuation.resume(emptyList())
                }
        }
    }

    /**
     * Clear the translation cache.
     */
    fun clearCache() {
        translationCache.clear()
        Timber.d("Translation cache cleared")
    }
}
