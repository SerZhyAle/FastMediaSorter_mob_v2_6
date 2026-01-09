package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogSearchBinding
import com.sza.fastmediasorter.ui.player.helpers.SearchResult
import com.sza.fastmediasorter.ui.player.helpers.TextSearchHelper
import timber.log.Timber
import java.io.File

/**
 * Dialog for searching within documents.
 * Supports text files, PDFs, and EPUBs.
 */
class SearchDialog : DialogFragment() {

    private var _binding: DialogSearchBinding? = null
    private val binding get() = _binding!!

    private val searchHelper = TextSearchHelper()
    private var onResultSelected: ((SearchResult) -> Unit)? = null
    private var documentType: DocumentType = DocumentType.TEXT

    enum class DocumentType {
        TEXT, PDF, EPUB
    }

    companion object {
        private const val ARG_FILE_PATH = "file_path"
        private const val ARG_DOCUMENT_TYPE = "document_type"
        private const val ARG_CONTENT = "content"

        fun newInstance(
            filePath: String,
            documentType: DocumentType = DocumentType.TEXT,
            content: String? = null
        ): SearchDialog {
            return SearchDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                    putString(ARG_DOCUMENT_TYPE, documentType.name)
                    if (content != null) {
                        putString(ARG_CONTENT, content)
                    }
                }
            }
        }
    }

    fun setOnResultSelectedListener(listener: (SearchResult) -> Unit) {
        onResultSelected = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSearchBinding.inflate(LayoutInflater.from(requireContext()))

        val filePath = arguments?.getString(ARG_FILE_PATH) ?: ""
        documentType = try {
            DocumentType.valueOf(arguments?.getString(ARG_DOCUMENT_TYPE) ?: "TEXT")
        } catch (e: Exception) {
            DocumentType.TEXT
        }
        val content = arguments?.getString(ARG_CONTENT)

        // Load content
        if (content != null) {
            searchHelper.loadFromString(content)
        } else if (filePath.isNotEmpty()) {
            searchHelper.loadFromFile(filePath)
        }

        setupSearch()
        setupNavigation()

        val title = when (documentType) {
            DocumentType.TEXT -> R.string.search_in_text
            DocumentType.PDF -> R.string.search_in_pdf
            DocumentType.EPUB -> R.string.search_in_epub
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun setupSearch() {
        binding.etSearchQuery.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            performSearch(query)
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch(binding.etSearchQuery.text?.toString() ?: "")
                true
            } else {
                false
            }
        }

        binding.cbCaseSensitive.setOnCheckedChangeListener { _, _ ->
            performSearch(binding.etSearchQuery.text?.toString() ?: "")
        }
    }

    private fun setupNavigation() {
        binding.btnPrevResult.setOnClickListener {
            val result = searchHelper.previousResult()
            if (result != null) {
                updateCurrentResult(result)
                onResultSelected?.invoke(result)
            }
        }

        binding.btnNextResult.setOnClickListener {
            val result = searchHelper.nextResult()
            if (result != null) {
                updateCurrentResult(result)
                onResultSelected?.invoke(result)
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            hideResults()
            return
        }

        val ignoreCase = !binding.cbCaseSensitive.isChecked
        val results = searchHelper.search(query, ignoreCase)

        if (results.isEmpty()) {
            showNoResults()
        } else {
            showResults()
            val currentResult = searchHelper.getCurrentResult()
            if (currentResult != null) {
                updateCurrentResult(currentResult)
                onResultSelected?.invoke(currentResult)
            }
        }
    }

    private fun showResults() {
        binding.layoutResults.isVisible = true
        binding.tvResultContext.isVisible = true
        binding.tvNoResults.isVisible = false
        updateResultCounter()
    }

    private fun hideResults() {
        binding.layoutResults.isVisible = false
        binding.tvResultContext.isVisible = false
        binding.tvNoResults.isVisible = false
    }

    private fun showNoResults() {
        binding.layoutResults.isVisible = false
        binding.tvResultContext.isVisible = false
        binding.tvNoResults.isVisible = true
    }

    private fun updateResultCounter() {
        val current = searchHelper.getCurrentIndex() + 1
        val total = searchHelper.getResultCount()
        binding.tvResultCounter.text = getString(R.string.search_result_counter, current, total)
    }

    private fun updateCurrentResult(result: SearchResult) {
        updateResultCounter()
        
        // Highlight the match in context
        val context = result.context
        val query = binding.etSearchQuery.text?.toString() ?: ""
        
        if (query.isNotEmpty()) {
            val spannable = SpannableString(context)
            val ignoreCase = !binding.cbCaseSensitive.isChecked
            var startIndex = if (ignoreCase) {
                context.lowercase().indexOf(query.lowercase())
            } else {
                context.indexOf(query)
            }
            
            while (startIndex >= 0) {
                val endIndex = startIndex + query.length
                spannable.setSpan(
                    BackgroundColorSpan(0xFFFFEB3B.toInt()), // Yellow highlight
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(0xFF000000.toInt()), // Black text
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                startIndex = if (ignoreCase) {
                    context.lowercase().indexOf(query.lowercase(), endIndex)
                } else {
                    context.indexOf(query, endIndex)
                }
            }
            
            binding.tvResultContext.text = spannable
        } else {
            binding.tvResultContext.text = context
        }
        
        Timber.d("Showing result at line ${result.lineNumber}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
