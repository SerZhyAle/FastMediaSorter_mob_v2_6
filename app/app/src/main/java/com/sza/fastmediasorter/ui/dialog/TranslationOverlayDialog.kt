package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.ocr.OcrManager
import com.sza.fastmediasorter.translation.TranslationManager
import com.sza.fastmediasorter.ui.view.TranslationOverlayView
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Dialog that provides Google Lens-style translation overlay on images.
 * 
 * Features:
 * - Shows image with detected text boxes overlaid
 * - Tap on any text box to translate it
 * - Copy translated text to clipboard
 * - Translate all detected text at once
 * - Configure source/target languages
 */
class TranslationOverlayDialog : DialogFragment() {

    companion object {
        const val TAG = "TranslationOverlayDialog"
        private const val ARG_FILE_PATH = "file_path"
        private const val ARG_SOURCE_LANGUAGE = "source_language"
        private const val ARG_TARGET_LANGUAGE = "target_language"
        private const val ARG_FONT_SIZE = "font_size"
        
        private var pendingBitmap: Bitmap? = null
        
        fun newInstance(
            filePath: String,
            sourceLanguage: String? = null,
            targetLanguage: String = "en",
            fontSize: Float = 14f
        ): TranslationOverlayDialog {
            return TranslationOverlayDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                    putString(ARG_SOURCE_LANGUAGE, sourceLanguage)
                    putString(ARG_TARGET_LANGUAGE, targetLanguage)
                    putFloat(ARG_FONT_SIZE, fontSize)
                }
            }
        }
        
        fun newInstance(
            bitmap: Bitmap,
            sourceLanguage: String? = null,
            targetLanguage: String = "en",
            fontSize: Float = 14f
        ): TranslationOverlayDialog {
            pendingBitmap = bitmap
            return TranslationOverlayDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_LANGUAGE, sourceLanguage)
                    putString(ARG_TARGET_LANGUAGE, targetLanguage)
                    putFloat(ARG_FONT_SIZE, fontSize)
                }
            }
        }
    }
    
    private var bitmap: Bitmap? = null
    private var filePath: String? = null
    
    private val ocrManager = OcrManager()
    private val translationManager = TranslationManager()
    
    private var sourceLanguage: String? = null
    private var targetLanguage: String = "en"
    private var fontSize: Float = 14f
    
    // Views
    private lateinit var imageView: ImageView
    private lateinit var overlayView: TranslationOverlayView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnTranslateAll: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnCopyAll: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        arguments?.let { args ->
            filePath = args.getString(ARG_FILE_PATH)
            sourceLanguage = args.getString(ARG_SOURCE_LANGUAGE)
            targetLanguage = args.getString(ARG_TARGET_LANGUAGE) ?: "en"
            fontSize = args.getFloat(ARG_FONT_SIZE, 14f)
        }
        
        // Get bitmap from pending or load from file
        bitmap = pendingBitmap ?: filePath?.let { loadBitmap(it) }
        pendingBitmap = null
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_translation_overlay, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imageView = view.findViewById(R.id.imageView)
        overlayView = view.findViewById(R.id.overlayView)
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        statusText = view.findViewById(R.id.statusText)
        btnTranslateAll = view.findViewById(R.id.btnTranslateAll)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnCopyAll = view.findViewById(R.id.btnCopyAll)
        
        // Close button
        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
        
        overlayView.translationFontSize = fontSize
        
        setupClickListeners()
        
        bitmap?.let { bmp ->
            imageView.setImageBitmap(bmp)
            performOcr(bmp)
        } ?: run {
            statusText.text = getString(R.string.error_loading_image)
            statusText.visibility = View.VISIBLE
        }
    }
    
    private fun setupClickListeners() {
        // Tap on text box to translate
        overlayView.onTextBoxTapped = { textBox, index ->
            if (textBox.translatedText == null && !textBox.isTranslating) {
                translateTextBox(textBox.originalText, index)
            } else {
                // Show translation in a popup or copy to clipboard
                textBox.translatedText?.let { showTranslationPopup(textBox.originalText, it) }
            }
        }
        
        // Long press to copy original text
        overlayView.onTextBoxLongPressed = { textBox, _ ->
            copyToClipboard(textBox.originalText, getString(R.string.original_text_copied))
        }
        
        // Translate all button
        btnTranslateAll.setOnClickListener {
            translateAllTextBoxes()
        }
        
        // Settings button - show language selection
        btnSettings.setOnClickListener {
            showLanguageSettings()
        }
        
        // Copy all translated text
        btnCopyAll.setOnClickListener {
            copyAllTranslatedText()
        }
    }
    
    private fun loadBitmap(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from $path")
            null
        }
    }
    
    private fun performOcr(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressText.text = getString(R.string.detecting_text)
        statusText.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val result = ocrManager.recognizeText(bitmap)
                
                if (result.blocks.isNotEmpty()) {
                    overlayView.setTextBlocks(
                        result.blocks,
                        bitmap.width.toFloat(),
                        bitmap.height.toFloat()
                    )
                    
                    statusText.text = getString(R.string.tap_text_to_translate, result.blocks.size)
                    statusText.visibility = View.VISIBLE
                    
                    // Enable buttons
                    btnTranslateAll.isEnabled = true
                    btnCopyAll.isEnabled = true
                } else {
                    statusText.text = getString(R.string.no_text_detected)
                    statusText.visibility = View.VISIBLE
                }
                
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
            } catch (e: Exception) {
                Timber.e(e, "OCR failed")
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                statusText.text = getString(R.string.ocr_error, e.message)
                statusText.visibility = View.VISIBLE
            }
        }
    }
    
    private fun translateTextBox(text: String, index: Int) {
        overlayView.setTranslating(index, true)
        
        lifecycleScope.launch {
            try {
                val translated = translationManager.translate(
                    text = text,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )
                
                overlayView.updateTranslation(index, translated)
                
                if (translated == null) {
                    Toast.makeText(context, R.string.translation_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Translation failed")
                overlayView.setTranslating(index, false)
                Toast.makeText(context, getString(R.string.translation_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun translateAllTextBoxes() {
        val textBoxes = overlayView.getTextBoxes()
        if (textBoxes.isEmpty()) return
        
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            var translated = 0
            val total = textBoxes.size
            
            textBoxes.forEachIndexed { index, textBox ->
                if (textBox.translatedText == null) {
                    progressText.text = getString(R.string.translating_progress, translated + 1, total)
                    overlayView.setTranslating(index, true)
                    
                    try {
                        val result = translationManager.translate(
                            text = textBox.originalText,
                            sourceLanguage = sourceLanguage,
                            targetLanguage = targetLanguage
                        )
                        overlayView.updateTranslation(index, result)
                    } catch (e: Exception) {
                        Timber.e(e, "Translation failed for block $index")
                        overlayView.setTranslating(index, false)
                    }
                }
                translated++
            }
            
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
            statusText.text = getString(R.string.all_text_translated)
        }
    }
    
    private fun showLanguageSettings() {
        val dialog = TranslationSettingsDialog.newInstance(sourceLanguage, targetLanguage)
        dialog.setOnLanguagesSelectedListener { source, target ->
            sourceLanguage = source
            targetLanguage = target
            
            // Re-translate any already translated boxes with new language
            val textBoxes = overlayView.getTextBoxes()
            if (textBoxes.any { it.translatedText != null }) {
                // Clear translations and re-translate
                textBoxes.forEachIndexed { index, _ ->
                    overlayView.updateTranslation(index, null)
                }
                translateAllTextBoxes()
            }
        }
        dialog.show(childFragmentManager, TranslationSettingsDialog.TAG)
    }
    
    private fun showTranslationPopup(original: String, translated: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.translation)
            .setMessage("$original\n\n→\n\n$translated")
            .setPositiveButton(R.string.copy_translation) { _, _ ->
                copyToClipboard(translated, getString(R.string.translation_copied))
            }
            .setNeutralButton(R.string.copy_original) { _, _ ->
                copyToClipboard(original, getString(R.string.original_text_copied))
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }
    
    private fun copyToClipboard(text: String, successMessage: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Translation", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
    }
    
    private fun copyAllTranslatedText() {
        val textBoxes = overlayView.getTextBoxes()
        val translatedTexts = textBoxes.mapNotNull { it.translatedText }
        
        if (translatedTexts.isEmpty()) {
            Toast.makeText(context, R.string.no_translations_to_copy, Toast.LENGTH_SHORT).show()
            return
        }
        
        val allText = translatedTexts.joinToString("\n\n")
        copyToClipboard(allText, getString(R.string.all_translations_copied))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ocrManager.close()
        translationManager.clearCache()
        bitmap = null
    }
}
