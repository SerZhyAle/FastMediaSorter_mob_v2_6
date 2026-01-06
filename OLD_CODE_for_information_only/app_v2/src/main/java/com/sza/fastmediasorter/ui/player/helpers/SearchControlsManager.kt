package com.sza.fastmediasorter.ui.player.helpers

import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.domain.model.MediaType
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages search controls setup and search operations for PlayerActivity.
 * 
 * Responsibilities:
 * - Setup search button listeners (PDF/Text/EPUB)
 * - Show/hide search panel with keyboard management
 * - Perform search across different media types
 * - Navigate search results (next/previous match)
 * - Update search counter display
 * - Clear search state
 * 
 * Supports search for:
 * - PDF files (via PdfViewerManager)
 * - Text files (via TextViewerManager)
 * - EPUB files (via EpubViewerManager)
 */
class SearchControlsManager(
    private val binding: ActivityPlayerUnifiedBinding,
    private val textViewerManager: TextViewerManager,
    private val pdfViewerManager: PdfViewerManager,
    private val epubViewerManager: EpubViewerManager,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val inputMethodManager: InputMethodManager,
    private val callback: SearchControlsCallback
) {
    
    interface SearchControlsCallback {
        fun getCurrentMediaFile(): com.sza.fastmediasorter.domain.model.MediaFile?
        fun scheduleHideControls()
        fun onEpubTranslate()
        fun showTranslationSettingsDialog()
    }
    
    /**
     * Setup all search control listeners.
     * Called once during PlayerActivity initialization.
     */
    fun setupSearchControls() {
        // Search button click listeners (in command panel)
        binding.btnSearchPdfCmd.setOnClickListener {
            Timber.d("SearchControlsManager: btnSearchPdfCmd clicked")
            showSearchPanel()
            callback.scheduleHideControls()
        }
        
        binding.btnSearchTextCmd.setOnClickListener {
            Timber.d("SearchControlsManager: btnSearchTextCmd clicked")
            showSearchPanel()
            callback.scheduleHideControls()
        }
        
        // EPUB search in command panel
        binding.btnSearchEpubCmd.setOnClickListener {
            Timber.d("SearchControlsManager: btnSearchEpubCmd clicked")
            showSearchPanel()
            callback.scheduleHideControls()
        }
        
        // EPUB translate in command panel
        binding.btnTranslateEpubCmd.setOnClickListener {
            Timber.d("SearchControlsManager: EPUB translate button clicked")
            callback.onEpubTranslate()
            callback.scheduleHideControls()
        }
        binding.btnTranslateEpubCmd.setOnLongClickListener {
            Timber.d("SearchControlsManager: EPUB translate button LONG-CLICKED - showing settings dialog")
            try {
                callback.showTranslationSettingsDialog()
                Timber.d("SearchControlsManager: showTranslationSettingsDialog() called successfully")
            } catch (e: Exception) {
                Timber.e(e, "SearchControlsManager: Error calling showTranslationSettingsDialog()")
            }
            true
        }
        
        // Search panel controls
        binding.btnCloseSearch.setOnClickListener {
            Timber.d("SearchControlsManager: btnCloseSearch clicked")
            hideSearchPanel()
        }
        
        binding.btnSearchNext.setOnClickListener {
            Timber.d("SearchControlsManager: btnSearchNext clicked")
            performSearchNavigation(forward = true)
        }
        
        binding.btnSearchPrev.setOnClickListener {
            Timber.d("SearchControlsManager: btnSearchPrev clicked")
            performSearchNavigation(forward = false)
        }
        
        // Search query input
        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
        
        binding.etSearchQuery.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.isNullOrBlank()) {
                    clearSearch()
                } else {
                    performSearch()
                }
            }
        })
    }
    
    /**
     * Show search panel with keyboard.
     * Displays search input field and focuses it.
     */
    fun showSearchPanel() {
        binding.searchPanel.isVisible = true
        binding.etSearchQuery.requestFocus()
        // Show keyboard
        inputMethodManager.showSoftInput(binding.etSearchQuery, InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Hide search panel with keyboard.
     * Clears search query and hides search results.
     */
    fun hideSearchPanel() {
        binding.searchPanel.isVisible = false
        binding.etSearchQuery.text.clear()
        clearSearch()
        // Hide keyboard
        inputMethodManager.hideSoftInputFromWindow(binding.etSearchQuery.windowToken, 0)
    }
    
    /**
     * Perform search in current media file.
     * Searches in PDF/Text/EPUB and updates counter.
     */
    private fun performSearch() {
        val query = binding.etSearchQuery.text.toString()
        if (query.isBlank()) {
            binding.tvSearchCounter.isVisible = false
            return
        }
        
        val currentFile = callback.getCurrentMediaFile() ?: return
        
        lifecycleScope.launch {
            val matchCount = when (currentFile.type) {
                MediaType.TEXT -> {
                    textViewerManager.searchText(query)
                }
                MediaType.PDF -> {
                    pdfViewerManager.searchInPdf(query)
                }
                MediaType.EPUB -> {
                    var count = 0
                    epubViewerManager.searchInEpub(query) { count = it }
                    count
                }
                else -> 0
            }
            
            binding.tvSearchCounter.text = if (matchCount > 0) "1/$matchCount" else "0/0"
            binding.tvSearchCounter.isVisible = matchCount > 0
        }
    }
    
    /**
     * Navigate to next/previous search match.
     * 
     * @param forward true for next match, false for previous
     */
    private fun performSearchNavigation(forward: Boolean) {
        val currentFile = callback.getCurrentMediaFile() ?: return
        
        when (currentFile.type) {
            MediaType.TEXT -> {
                // TextView doesn't have built-in navigation, would need custom implementation
                // For now, just highlight matches
                val query = binding.etSearchQuery.text.toString()
                textViewerManager.highlightSearchMatch(query, 0)
            }
            MediaType.PDF -> {
                if (forward) {
                    pdfViewerManager.nextSearchResult()
                } else {
                    pdfViewerManager.previousSearchResult()
                }
                updateSearchCounter()
            }
            MediaType.EPUB -> {
                if (forward) {
                    epubViewerManager.nextSearchMatch()
                } else {
                    epubViewerManager.previousSearchMatch()
                }
            }
            else -> {}
        }
    }
    
    /**
     * Update search counter display.
     * Shows current match / total matches.
     */
    private fun updateSearchCounter() {
        val (current, total, _) = pdfViewerManager.getSearchState()
        binding.tvSearchCounter.text = "$current/$total"
        binding.tvSearchCounter.isVisible = total > 0
    }
    
    /**
     * Clear search state for current media file.
     * Removes highlights and hides counter.
     */
    fun clearSearch() {
        val currentFile = callback.getCurrentMediaFile() ?: return
        
        when (currentFile.type) {
            MediaType.TEXT -> {
                textViewerManager.clearSearch()
            }
            MediaType.EPUB -> {
                epubViewerManager.clearSearch()
            }
            else -> {}
        }
        
        binding.tvSearchCounter.isVisible = false
    }
}
