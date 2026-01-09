package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.nl.translate.TranslateLanguage
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogTranslationSettingsBinding
import com.sza.fastmediasorter.translation.TranslationManager

/**
 * Dialog for selecting source and target languages for translation.
 * 
 * Features:
 * - Auto-detect source language option
 * - 22 supported target languages
 * - Font size selection (small/medium/large)
 * - Persistent settings storage
 * - Remember last selected languages
 */
class TranslationSettingsDialog : DialogFragment() {

    companion object {
        const val TAG = "TranslationSettingsDialog"
        private const val ARG_SOURCE_LANGUAGE = "source_language"
        private const val ARG_TARGET_LANGUAGE = "target_language"
        
        // Shared preferences keys
        private const val PREFS_NAME = "translation_settings"
        private const val PREF_SOURCE_LANGUAGE = "source_language"
        private const val PREF_TARGET_LANGUAGE = "target_language"
        private const val PREF_FONT_SIZE = "font_size"
        
        // Font size values
        const val FONT_SIZE_SMALL = 12f
        const val FONT_SIZE_MEDIUM = 14f
        const val FONT_SIZE_LARGE = 18f

        fun newInstance(
            sourceLanguage: String? = null,
            targetLanguage: String? = null
        ): TranslationSettingsDialog {
            return TranslationSettingsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_LANGUAGE, sourceLanguage)
                    putString(ARG_TARGET_LANGUAGE, targetLanguage)
                }
            }
        }
    }

    private var _binding: DialogTranslationSettingsBinding? = null
    private val binding get() = _binding!!

    private var onLanguagesSelected: ((sourceLanguage: String?, targetLanguage: String) -> Unit)? = null
    private var onSettingsChanged: ((sourceLanguage: String?, targetLanguage: String, fontSize: Float) -> Unit)? = null
    
    private var selectedSourceLanguage: String? = null
    private var selectedTargetLanguage: String = TranslateLanguage.ENGLISH
    private var selectedFontSize: Float = FONT_SIZE_MEDIUM

    // Language display data
    data class LanguageItem(
        val code: String?,
        val name: String,
        val isAutoDetect: Boolean = false
    )

    private val sourceLanguages = mutableListOf<LanguageItem>().apply {
        add(LanguageItem(null, "Auto-detect", isAutoDetect = true))
        TranslationManager.SUPPORTED_LANGUAGES.forEach { code ->
            add(LanguageItem(code, getLanguageName(code)))
        }
    }

    private val targetLanguages = TranslationManager.SUPPORTED_LANGUAGES.map { code ->
        LanguageItem(code, getLanguageName(code))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTranslationSettingsBinding.inflate(layoutInflater)

        // Load saved preferences first
        loadSavedSettings()
        
        // Override with arguments if provided
        arguments?.let { args ->
            args.getString(ARG_SOURCE_LANGUAGE)?.let { selectedSourceLanguage = it }
            args.getString(ARG_TARGET_LANGUAGE)?.let { selectedTargetLanguage = it }
        }

        setupSpinners()
        setupFontSizeToggle()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.translation_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.translate) { _, _ ->
                saveSettings()
                notifySelection()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnLanguagesSelectedListener(listener: (sourceLanguage: String?, targetLanguage: String) -> Unit) {
        onLanguagesSelected = listener
    }
    
    fun setOnSettingsChangedListener(listener: (sourceLanguage: String?, targetLanguage: String, fontSize: Float) -> Unit) {
        onSettingsChanged = listener
    }
    
    private fun loadSavedSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedSourceLanguage = prefs.getString(PREF_SOURCE_LANGUAGE, null)
        selectedTargetLanguage = prefs.getString(PREF_TARGET_LANGUAGE, TranslateLanguage.ENGLISH) ?: TranslateLanguage.ENGLISH
        selectedFontSize = prefs.getFloat(PREF_FONT_SIZE, FONT_SIZE_MEDIUM)
    }
    
    private fun saveSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (selectedSourceLanguage != null) {
                putString(PREF_SOURCE_LANGUAGE, selectedSourceLanguage)
            } else {
                remove(PREF_SOURCE_LANGUAGE)
            }
            putString(PREF_TARGET_LANGUAGE, selectedTargetLanguage)
            putFloat(PREF_FONT_SIZE, selectedFontSize)
            apply()
        }
    }
    
    private fun setupFontSizeToggle() {
        // Set initial selection based on saved font size
        val checkedId = when (selectedFontSize) {
            FONT_SIZE_SMALL -> R.id.btnFontSmall
            FONT_SIZE_LARGE -> R.id.btnFontLarge
            else -> R.id.btnFontMedium
        }
        binding.fontSizeToggleGroup.check(checkedId)
        
        // Listen for changes
        binding.fontSizeToggleGroup.addOnButtonCheckedListener { _, checkedButtonId, isChecked ->
            if (isChecked) {
                selectedFontSize = when (checkedButtonId) {
                    R.id.btnFontSmall -> FONT_SIZE_SMALL
                    R.id.btnFontLarge -> FONT_SIZE_LARGE
                    else -> FONT_SIZE_MEDIUM
                }
            }
        }
    }

    private fun setupSpinners() {
        // Source language spinner
        val sourceAdapter = LanguageAdapter(
            requireContext(),
            sourceLanguages
        )
        binding.spinnerSourceLanguage.adapter = sourceAdapter
        
        // Find and select current source language
        val sourceIndex = if (selectedSourceLanguage == null) {
            0 // Auto-detect
        } else {
            sourceLanguages.indexOfFirst { it.code == selectedSourceLanguage }.takeIf { it >= 0 } ?: 0
        }
        binding.spinnerSourceLanguage.setSelection(sourceIndex)

        // Target language spinner
        val targetAdapter = LanguageAdapter(
            requireContext(),
            targetLanguages
        )
        binding.spinnerTargetLanguage.adapter = targetAdapter
        
        // Find and select current target language
        val targetIndex = targetLanguages.indexOfFirst { it.code == selectedTargetLanguage }.takeIf { it >= 0 } ?: 0
        binding.spinnerTargetLanguage.setSelection(targetIndex)

        // Swap button
        binding.btnSwapLanguages.setOnClickListener {
            swapLanguages()
        }
    }

    private fun swapLanguages() {
        val sourcePos = binding.spinnerSourceLanguage.selectedItemPosition
        val targetPos = binding.spinnerTargetLanguage.selectedItemPosition

        if (sourcePos > 0) { // Can't swap if source is "Auto-detect"
            val sourceItem = sourceLanguages[sourcePos]
            val targetItem = targetLanguages[targetPos]

            // Find the positions after swap
            val newSourcePos = sourceLanguages.indexOfFirst { it.code == targetItem.code }
            val newTargetPos = targetLanguages.indexOfFirst { it.code == sourceItem.code }

            if (newSourcePos >= 0 && newTargetPos >= 0) {
                binding.spinnerSourceLanguage.setSelection(newSourcePos)
                binding.spinnerTargetLanguage.setSelection(newTargetPos)
            }
        }
    }

    private fun notifySelection() {
        val sourcePos = binding.spinnerSourceLanguage.selectedItemPosition
        val targetPos = binding.spinnerTargetLanguage.selectedItemPosition

        val source = sourceLanguages[sourcePos].code
        val target = targetLanguages[targetPos].code ?: TranslateLanguage.ENGLISH
        
        // Update selected values
        selectedSourceLanguage = source
        selectedTargetLanguage = target

        onLanguagesSelected?.invoke(source, target)
        onSettingsChanged?.invoke(source, target, selectedFontSize)
    }

    private fun getLanguageName(code: String): String {
        return when (code) {
            TranslateLanguage.ENGLISH -> "English"
            TranslateLanguage.UKRAINIAN -> "Українська"
            TranslateLanguage.RUSSIAN -> "Русский"
            TranslateLanguage.GERMAN -> "Deutsch"
            TranslateLanguage.FRENCH -> "Français"
            TranslateLanguage.SPANISH -> "Español"
            TranslateLanguage.ITALIAN -> "Italiano"
            TranslateLanguage.PORTUGUESE -> "Português"
            TranslateLanguage.POLISH -> "Polski"
            TranslateLanguage.DUTCH -> "Nederlands"
            TranslateLanguage.CZECH -> "Čeština"
            TranslateLanguage.SWEDISH -> "Svenska"
            TranslateLanguage.DANISH -> "Dansk"
            TranslateLanguage.NORWEGIAN -> "Norsk"
            TranslateLanguage.FINNISH -> "Suomi"
            TranslateLanguage.TURKISH -> "Türkçe"
            TranslateLanguage.ARABIC -> "العربية"
            TranslateLanguage.HEBREW -> "עברית"
            TranslateLanguage.CHINESE -> "中文"
            TranslateLanguage.JAPANESE -> "日本語"
            TranslateLanguage.KOREAN -> "한국어"
            TranslateLanguage.HINDI -> "हिन्दी"
            else -> code.uppercase()
        }
    }

    /**
     * Custom adapter for language spinner with native language names.
     */
    private class LanguageAdapter(
        context: android.content.Context,
        private val items: List<LanguageItem>
    ) : ArrayAdapter<LanguageItem>(context, android.R.layout.simple_spinner_item, items) {

        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            (view as? TextView)?.text = items[position].name
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            (view as? TextView)?.text = items[position].name
            return view
        }
    }
}
