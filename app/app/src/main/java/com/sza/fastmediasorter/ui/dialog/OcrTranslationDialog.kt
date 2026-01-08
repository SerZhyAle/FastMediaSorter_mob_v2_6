package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.ocr.OcrManager
import com.sza.fastmediasorter.translation.TranslationManager
import kotlinx.coroutines.launch

/**
 * Dialog that performs OCR on an image and optionally translates the text
 */
class OcrTranslationDialog : DialogFragment() {
    
    companion object {
        const val TAG = "OcrTranslationDialog"
        
        private var pendingBitmap: Bitmap? = null
        
        fun newInstance(bitmap: Bitmap): OcrTranslationDialog {
            pendingBitmap = bitmap
            return OcrTranslationDialog()
        }
    }
    
    private var bitmap: Bitmap? = null
    private var ocrManager: OcrManager? = null
    private var translationManager: TranslationManager? = null
    
    private var detectedText: String = ""
    private var translatedText: String = ""
    private var isTranslated: Boolean = false
    
    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var textContent: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnTranslate: Button
    private lateinit var btnToggle: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bitmap = pendingBitmap
        pendingBitmap = null
        
        ocrManager = OcrManager()
        translationManager = TranslationManager()
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_ocr_translation, null)
        
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        textContent = view.findViewById(R.id.textContent)
        btnCopy = view.findViewById(R.id.btnCopy)
        btnTranslate = view.findViewById(R.id.btnTranslate)
        btnToggle = view.findViewById(R.id.btnToggle)
        
        setupClickListeners()
        
        // Start OCR processing
        bitmap?.let { performOcr(it) }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ocr_title)
            .setView(view)
            .setPositiveButton(R.string.close, null)
            .create()
    }
    
    private fun setupClickListeners() {
        btnCopy.setOnClickListener {
            val textToCopy = if (isTranslated && translatedText.isNotEmpty()) {
                translatedText
            } else {
                detectedText
            }
            
            if (textToCopy.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OCR Text", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
            }
        }
        
        btnTranslate.setOnClickListener {
            if (detectedText.isNotEmpty()) {
                translateText(detectedText)
            }
        }
        
        btnToggle.setOnClickListener {
            if (translatedText.isNotEmpty()) {
                isTranslated = !isTranslated
                textContent.text = if (isTranslated) translatedText else detectedText
                updateToggleButton()
            }
        }
    }
    
    private fun performOcr(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressText.text = getString(R.string.ocr_processing)
        textContent.visibility = View.GONE
        btnCopy.visibility = View.GONE
        btnTranslate.visibility = View.GONE
        btnToggle.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val result = ocrManager?.recognizeText(bitmap)
                
                if (result != null && result.fullText.isNotEmpty()) {
                    detectedText = result.fullText
                    textContent.text = detectedText
                    textContent.visibility = View.VISIBLE
                    btnCopy.visibility = View.VISIBLE
                    btnTranslate.visibility = View.VISIBLE
                    
                    progressText.text = getString(R.string.ocr_complete, result.blocks.size, result.processingTimeMs)
                } else {
                    textContent.text = getString(R.string.ocr_no_text_found)
                    textContent.visibility = View.VISIBLE
                    progressText.text = getString(R.string.ocr_complete_no_text)
                }
            } catch (e: Exception) {
                textContent.text = getString(R.string.ocr_error, e.message)
                textContent.visibility = View.VISIBLE
                progressText.text = getString(R.string.ocr_failed)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun translateText(text: String) {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressText.text = getString(R.string.translating)
        btnTranslate.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Auto-detect source language and translate to English as default
                val detectedLang = translationManager?.detectLanguage(text)
                val targetLang = "en" // Default to English
                
                val translated = translationManager?.translate(text, detectedLang ?: "en", targetLang)
                
                if (translated != null && translated.isNotEmpty()) {
                    translatedText = translated
                    isTranslated = true
                    textContent.text = translatedText
                    btnToggle.visibility = View.VISIBLE
                    updateToggleButton()
                    
                    progressText.text = getString(R.string.translation_complete)
                } else {
                    progressText.text = getString(R.string.translation_failed)
                }
            } catch (e: Exception) {
                progressText.text = getString(R.string.translation_error, e.message)
            } finally {
                progressBar.visibility = View.GONE
                btnTranslate.isEnabled = true
            }
        }
    }
    
    private fun updateToggleButton() {
        btnToggle.text = if (isTranslated) {
            getString(R.string.show_original)
        } else {
            getString(R.string.show_translation)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ocrManager?.close()
        translationManager?.clearCache()
    }
}
