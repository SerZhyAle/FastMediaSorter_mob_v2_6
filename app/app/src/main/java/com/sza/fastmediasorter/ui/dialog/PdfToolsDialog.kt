package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.pdf.PdfEditManager
import com.sza.fastmediasorter.pdf.PdfToolsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog for PDF tools - page thumbnails, extraction, export to images, rotate, delete
 */
@AndroidEntryPoint
class PdfToolsDialog : DialogFragment() {
    
    companion object {
        const val TAG = "PdfToolsDialog"
        
        private var pendingUri: Uri? = null
        
        fun newInstance(pdfUri: Uri): PdfToolsDialog {
            pendingUri = pdfUri
            return PdfToolsDialog()
        }
    }
    
    private var pdfUri: Uri? = null
    private var pdfToolsManager: PdfToolsManager? = null
    
    @javax.inject.Inject
    lateinit var pdfEditManager: PdfEditManager
    
    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var infoText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnExtract: MaterialButton
    private lateinit var btnExportImages: MaterialButton
    private lateinit var btnRotateLeft: MaterialButton
    private lateinit var btnRotateRight: MaterialButton
    private lateinit var btnDelete: MaterialButton
    
    private val thumbnails = mutableListOf<Bitmap?>()
    private val selectedPages = mutableSetOf<Int>()
    private var adapter: PageThumbnailAdapter? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfUri = pendingUri
        pendingUri = null
        pdfToolsManager = PdfToolsManager()
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_pdf_tools, null)
        
        progressBar = view.findViewById(R.id.progressBar)
        infoText = view.findViewById(R.id.infoText)
        recyclerView = view.findViewById(R.id.recyclerView)
        btnExtract = view.findViewById(R.id.btnExtract)
        btnExportImages = view.findViewById(R.id.btnExportImages)
        btnRotateLeft = view.findViewById(R.id.btnRotateLeft)
        btnRotateRight = view.findViewById(R.id.btnRotateRight)
        btnDelete = view.findViewById(R.id.btnDelete)
        
        setupRecyclerView()
        setupClickListeners()
        
        // Load PDF info and thumbnails
        pdfUri?.let { loadPdfData(it) }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pdf_tools)
            .setView(view)
            .setNegativeButton(R.string.close, null)
            .create()
    }
    
    private fun setupRecyclerView() {
        adapter = PageThumbnailAdapter(
            thumbnails = thumbnails,
            selectedPages = selectedPages,
            onPageClick = { pageIndex ->
                togglePageSelection(pageIndex)
            },
            onPageLongClick = { pageIndex ->
                selectRange(pageIndex)
            }
        )
        
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnExtract.setOnClickListener {
            if (selectedPages.isEmpty()) {
                Toast.makeText(context, R.string.select_pages_first, Toast.LENGTH_SHORT).show()
            } else {
                extractSelectedPages()
            }
        }
        
        btnExportImages.setOnClickListener {
            val pages = if (selectedPages.isEmpty()) {
                (0 until thumbnails.size).toSet()
            } else {
                selectedPages
            }
            exportPagesAsImages(pages)
        }
        
        btnRotateLeft.setOnClickListener {
            if (selectedPages.isEmpty()) {
                Toast.makeText(context, R.string.select_pages_first, Toast.LENGTH_SHORT).show()
            } else {
                rotatePages(-90)
            }
        }
        
        btnRotateRight.setOnClickListener {
            if (selectedPages.isEmpty()) {
                Toast.makeText(context, R.string.select_pages_first, Toast.LENGTH_SHORT).show()
            } else {
                rotatePages(90)
            }
        }
        
        btnDelete.setOnClickListener {
            if (selectedPages.isEmpty()) {
                Toast.makeText(context, R.string.select_pages_first, Toast.LENGTH_SHORT).show()
            } else {
                confirmDeletePages()
            }
        }
    }
    
    private fun loadPdfData(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        infoText.text = getString(R.string.loading_pdf)
        
        lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) {
                pdfToolsManager?.getPdfInfo(requireContext(), uri)
            }
            
            if (info != null && info.isValid) {
                infoText.text = getString(R.string.pdf_info_format, info.fileName, info.pageCount)
                
                // Load thumbnails in background
                loadThumbnails(uri, info.pageCount)
            } else {
                progressBar.visibility = View.GONE
                infoText.text = getString(R.string.pdf_load_error)
            }
        }
    }
    
    private suspend fun loadThumbnails(uri: Uri, pageCount: Int) {
        thumbnails.clear()
        
        for (pageIndex in 0 until pageCount) {
            val thumbnail = withContext(Dispatchers.IO) {
                pdfToolsManager?.renderPage(requireContext(), uri, pageIndex, 200)
            }
            thumbnails.add(thumbnail)
            
            withContext(Dispatchers.Main) {
                adapter?.notifyItemInserted(thumbnails.size - 1)
            }
        }
        
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
        }
    }
    
    private fun togglePageSelection(pageIndex: Int) {
        if (selectedPages.contains(pageIndex)) {
            selectedPages.remove(pageIndex)
        } else {
            selectedPages.add(pageIndex)
        }
        adapter?.notifyItemChanged(pageIndex)
        updateButtonText()
    }
    
    private fun selectRange(toIndex: Int) {
        if (selectedPages.isNotEmpty()) {
            val fromIndex = selectedPages.max()
            val range = if (fromIndex < toIndex) {
                fromIndex..toIndex
            } else {
                toIndex..fromIndex
            }
            range.forEach { selectedPages.add(it) }
            adapter?.notifyDataSetChanged()
            updateButtonText()
        }
    }
    
    private fun updateButtonText() {
        val count = selectedPages.size
        btnExtract.text = if (count > 0) {
            getString(R.string.extract_pages_count, count)
        } else {
            getString(R.string.extract_pages)
        }
    }
    
    private fun extractSelectedPages() {
        Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        // Full implementation would require a document provider save location picker
    }
    
    private fun exportPagesAsImages(pages: Set<Int>) {
        Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        // Full implementation would require storage access framework
    }
    
    private fun rotatePages(degrees: Int) {
        val uri = pdfUri ?: return
        val pages = selectedPages.toList().sorted()
        
        progressBar.visibility = View.VISIBLE
        btnRotateLeft.isEnabled = false
        btnRotateRight.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Create temp output file
                val outputFile = withContext(Dispatchers.IO) {
                    val tempDir = requireContext().cacheDir
                    File.createTempFile("rotated_", ".pdf", tempDir)
                }
                
                // Rotate pages using PdfEditManager
                val result = withContext(Dispatchers.IO) {
                    val rotations = pages.associateWith { degrees }
                    pdfEditManager.rotatePages(
                        context = requireContext(),
                        sourceUri = uri,
                        outputFile = outputFile,
                        rotations = rotations
                    )
                }
                
                when (result) {
                    is PdfEditManager.PdfResult.Success -> {
                        Toast.makeText(context, R.string.pages_rotated, Toast.LENGTH_SHORT).show()
                        // Reload PDF with rotated pages
                        pdfUri = Uri.fromFile(result.data)
                        selectedPages.clear()
                        loadPdfData(pdfUri!!)
                    }
                    is PdfEditManager.PdfResult.Error -> {
                        Toast.makeText(
                            context,
                            getString(R.string.pdf_operation_failed, result.message),
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    getString(R.string.pdf_operation_failed, e.message),
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.GONE
            } finally {
                btnRotateLeft.isEnabled = true
                btnRotateRight.isEnabled = true
            }
        }
    }
    
    private fun confirmDeletePages() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_pages)
            .setMessage(getString(R.string.delete_pages_confirm, selectedPages.size))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deletePages()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deletePages() {
        val uri = pdfUri ?: return
        val pagesToDelete = selectedPages.toSet()
        
        progressBar.visibility = View.VISIBLE
        btnDelete.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Create temp output file
                val outputFile = withContext(Dispatchers.IO) {
                    val tempDir = requireContext().cacheDir
                    File.createTempFile("deleted_", ".pdf", tempDir)
                }
                
                // Delete pages using PdfEditManager
                val result = withContext(Dispatchers.IO) {
                    pdfEditManager.deletePages(
                        context = requireContext(),
                        sourceUri = uri,
                        outputFile = outputFile,
                        pagesToDelete = pagesToDelete
                    )
                }
                
                when (result) {
                    is PdfEditManager.PdfResult.Success -> {
                        Toast.makeText(context, R.string.pages_deleted, Toast.LENGTH_SHORT).show()
                        // Reload PDF with deleted pages removed
                        pdfUri = Uri.fromFile(result.data)
                        selectedPages.clear()
                        loadPdfData(pdfUri!!)
                    }
                    is PdfEditManager.PdfResult.Error -> {
                        Toast.makeText(
                            context,
                            getString(R.string.pdf_operation_failed, result.message),
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    getString(R.string.pdf_operation_failed, e.message),
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.GONE
            } finally {
                btnDelete.isEnabled = true
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Recycle bitmaps
        thumbnails.forEach { it?.recycle() }
        thumbnails.clear()
    }
    
    /**
     * Adapter for page thumbnails
     */
    private class PageThumbnailAdapter(
        private val thumbnails: List<Bitmap?>,
        private val selectedPages: Set<Int>,
        private val onPageClick: (Int) -> Unit,
        private val onPageLongClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PageThumbnailAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.thumbnailImage)
            val pageNumber: TextView = view.findViewById(R.id.pageNumber)
            val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_page_thumbnail, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val bitmap = thumbnails.getOrNull(position)
            
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap)
            } else {
                holder.imageView.setImageResource(R.drawable.ic_pdf)
            }
            
            holder.pageNumber.text = (position + 1).toString()
            holder.selectionOverlay.visibility = if (selectedPages.contains(position)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            holder.itemView.setOnClickListener { onPageClick(position) }
            holder.itemView.setOnLongClickListener {
                onPageLongClick(position)
                true
            }
        }
        
        override fun getItemCount(): Int = thumbnails.size
    }
}
