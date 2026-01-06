package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogCopyToBinding
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.FileOperationType
import com.sza.fastmediasorter.domain.model.UndoOperation
import com.sza.fastmediasorter.domain.usecase.FileOperation
import com.sza.fastmediasorter.domain.usecase.FileOperationResult
import com.sza.fastmediasorter.domain.usecase.FileOperationUseCase
import com.sza.fastmediasorter.domain.usecase.GetDestinationsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CopyToDialog(
    context: Context,
    private val sourceFiles: List<File>,
    private val sourceFolderName: String,
    private val currentResourceId: Long,
    private val currentBrowsePath: String?, // Current browsing path for network destinations
    private val sourceCredentialsId: String?, // Credentials ID for source resource
    private val fileOperationUseCase: FileOperationUseCase,
    private val getDestinationsUseCase: GetDestinationsUseCase,
    private val overwriteFiles: Boolean,
    private val onComplete: (UndoOperation?) -> Unit,
    private val onAuthRequest: ((String) -> Unit)? = null
) : Dialog(context) {
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val TAG = "CopyToDialog"
    }

    private lateinit var binding: DialogCopyToBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogCopyToBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set dialog width to 90% of screen width to accommodate buttons
        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        setupUI()
        loadDestinations()
    }

    private fun setupUI() {
        binding.apply {
            tvFileCount.text = context.getString(
                R.string.copying_n_files_from_folder,
                sourceFiles.size,
                sourceFolderName
            )
            
            btnCancel.setOnClickListener { dismiss() }
        }
    }

    private fun loadDestinations() {
        Log.d(TAG, "loadDestinations() called")
        scope.launch {
            try {
                val destinations = withContext(Dispatchers.IO) {
                    getDestinationsUseCase.getDestinationsExcluding(currentResourceId)
                }
                
                Log.d(TAG, "Loaded ${destinations.size} destinations")
                destinations.forEach { dest ->
                    Log.d(TAG, "Destination: ${dest.name}, order=${dest.destinationOrder}, color=${dest.destinationColor}")
                }
                
                if (destinations.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_destinations_available),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                } else {
                    createDestinationButtons(destinations)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading destinations", e)
                Toast.makeText(context, context.getString(R.string.toast_error_loading_destinations, e.message), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    /**
     * Create colored destination buttons in two rows (max 5 buttons per row)
     * Distribution: 1-5: single row, 6: 3+3, 7: 4+3, 8: 4+4, 9: 5+4, 10: 5+5
     */
    private fun createDestinationButtons(destinations: List<MediaResource>) {
        Log.d(TAG, "createDestinationButtons() called with ${destinations.size} destinations")
        val container = binding.layoutDestinations
        container.removeAllViews()
        
        val destinationsList = destinations.take(10)
        val count = destinationsList.size
        
        // Calculate button distribution across rows (max 5 per row)
        val distribution = when (count) {
            0, 1, 2, 3, 4, 5 -> listOf(count) // Single row
            6 -> listOf(3, 3)
            7 -> listOf(4, 3)
            8 -> listOf(4, 4)
            9 -> listOf(5, 4)
            10 -> listOf(5, 5)
            else -> listOf(5, 5) // Fallback
        }
        
        Log.d(TAG, "Button distribution: $distribution for $count destinations")
        
        // Small margins for spacing (4dp on each side = 8dp total between buttons)
        val marginSize = (4 * context.resources.displayMetrics.density).toInt()
        
        // Create rows
        var destIndex = 0
        distribution.forEach { rowCount ->
            if (rowCount > 0) {
                val buttonRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                
                repeat(rowCount) {
                    if (destIndex < destinationsList.size) {
                        val destination = destinationsList[destIndex]
                        val button = androidx.appcompat.widget.AppCompatButton(context).apply {
                            text = destination.name
                            setTextColor(Color.WHITE)
                            textSize = 16f
                            isAllCaps = false
                            setPadding(8, 32, 8, 32)
                            
                            // Equal weight for buttons in this row
                            layoutParams = LinearLayout.LayoutParams(
                                0, // width 0 with weight for equal distribution
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f // each button gets equal weight
                            ).apply {
                                setMargins(marginSize, 8, marginSize, 8)
                            }
                            
                            minimumWidth = 0
                            minimumHeight = resources.getDimensionPixelSize(R.dimen.destination_button_min_height)
                            elevation = 6f
                            
                            // Rounded corners background
                            background = android.graphics.drawable.GradientDrawable().apply {
                                setColor(destination.destinationColor)
                                cornerRadius = 12f
                            }
                            
                            setOnClickListener {
                                copyToDestination(destination)
                            }
                        }
                        
                        buttonRow.addView(button)
                        Log.d(TAG, "Added button for ${destination.name} at position $destIndex with color ${destination.destinationColor}")
                        destIndex++
                    }
                }
                
                container.addView(buttonRow)
            }
        }
        
        Log.d(TAG, "Finished creating $destIndex destination buttons in ${distribution.size} rows")
    }

    private fun copyToDestination(destination: MediaResource) {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutDestinations.isEnabled = false
        
        // Show start message
        val totalSize = sourceFiles.sumOf { it.length() }
        if (totalSize > 1024 * 1024) { // > 1MB
            Toast.makeText(
                context,
                context.getString(R.string.msg_copy_started, destination.name),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.msg_copy_started, destination.name),
                Toast.LENGTH_LONG
            ).show()
        }
        
        // Create cancellable job for copy operation
        scope.launch {
            // Show progress dialog immediately
            val progressDialog = FileOperationProgressDialog.show(
                context,
                context.getString(R.string.copying_files),
                onCancel = { 
                    cancel() // Cancel this coroutine job
                }
            )

            try {
                // Determine destination path: for network destinations, use currentBrowsePath if it matches the destination protocol
                val destinationPath = when {
                    // Network destination with current browse path from same protocol
                    destination.path.startsWith("smb://") && currentBrowsePath?.startsWith("smb://") == true -> currentBrowsePath
                    destination.path.startsWith("sftp://") && currentBrowsePath?.startsWith("sftp://") == true -> currentBrowsePath
                    destination.path.startsWith("ftp://") && currentBrowsePath?.startsWith("ftp://") == true -> currentBrowsePath
                    // Otherwise use destination base path
                    else -> destination.path
                }
                
                // Create File object that preserves network/cloud paths
                val destinationFolder = if (destinationPath.startsWith("smb://") || 
                                            destinationPath.startsWith("sftp://") || 
                                            destinationPath.startsWith("ftp://") ||
                                            destinationPath.startsWith("cloud://")) {
                    object : File(destinationPath) {
                        override fun getAbsolutePath(): String = destinationPath
                        override fun getPath(): String = destinationPath
                    }
                } else {
                    File(destinationPath)
                }
                
                val operation = FileOperation.Copy(
                    sources = sourceFiles,
                    destination = destinationFolder,
                    overwrite = overwriteFiles,
                    sourceCredentialsId = sourceCredentialsId
                )
                
                // Use executeWithProgress to get progress updates
                var completed = false
                withContext(Dispatchers.IO) {
                    fileOperationUseCase.executeWithProgress(operation).collect { progress ->
                        if (completed) return@collect
                        
                        // Update progress dialog on main thread
                        withContext(Dispatchers.Main) {
                            progressDialog.updateProgress(progress)
                            
                            // Handle completion
                            if (progress is com.sza.fastmediasorter.domain.usecase.FileOperationProgress.Completed) {
                                completed = true
                                progressDialog.dismiss()
                                handleCopyResult(progress.result, destinationFolder, destination)
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Operation cancelled by user
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.toast_copy_cancelled, Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.layoutDestinations.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                        context,
                        context.getString(R.string.error_copy),
                        e.message ?: "Unknown error",
                        e.stackTraceToString()
                    )
                    
                    binding.progressBar.visibility = View.GONE
                    binding.layoutDestinations.isEnabled = true
                }
            }
        }
    }
    
    private fun handleCopyResult(
        result: FileOperationResult, 
        destinationFolder: File,
        destinationResource: MediaResource? = null
    ) {
        when (result) {
            is FileOperationResult.Success -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_n_files, result.processedCount),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Create UndoOperation for copy
                val undoOp = UndoOperation(
                    type = FileOperationType.COPY,
                    sourceFiles = sourceFiles.map { it.absolutePath },
                    destinationFolder = destinationFolder.absolutePath,
                    copiedFiles = result.copiedFilePaths,
                    oldNames = null,
                    timestamp = System.currentTimeMillis()
                )
                
                onComplete(undoOp)
                dismiss()
            }
            is FileOperationResult.PartialSuccess -> {
                val message = buildString {
                    append(context.getString(
                        R.string.copied_n_of_m_files,
                        result.processedCount,
                        result.processedCount + result.failedCount
                    ))
                    append("\n\nErrors:\n")
                    result.errors.take(5).forEach { error ->
                        append("\n$error\n")
                    }
                    if (result.errors.size > 5) {
                        append("\n... and ${result.errors.size - 5} more errors")
                    }
                }
                
                com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                    context,
                    "Partial Copy Success",
                    message
                )
                
                onComplete(null) // Don't save partial operations for undo
                dismiss()
            }
            is FileOperationResult.Failure -> {
                // Check if this is a Cloud authentication error
                if (result.error.contains("Google Drive authentication required", ignoreCase = true) ||
                    result.error.contains("Not authenticated", ignoreCase = true) ||
                    result.error.contains("expired_access_token", ignoreCase = true)) {
                    showCloudAuthenticationError(result.error, destinationResource)
                } else {
                    com.sza.fastmediasorter.ui.dialog.ErrorDialog.show(
                        context,
                        "Copy Failed",
                        context.getString(R.string.copy_failed, result.error),
                        "Check logcat for detailed information (tag: FileOperation)"
                    )
                }
                
                binding.progressBar.visibility = View.GONE
                binding.layoutDestinations.isEnabled = true
            }
            is FileOperationResult.AuthenticationRequired -> {
                showCloudAuthenticationError(result.message, destinationResource)
                binding.progressBar.visibility = View.GONE
                binding.layoutDestinations.isEnabled = true
            }
        }
    }
    
    private fun showCloudAuthenticationError(errorMessage: String, destinationResource: MediaResource? = null) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.authentication_required))
            .setMessage(context.getString(R.string.cloud_auth_copy_error))
            .setNeutralButton(context.getString(R.string.copy_error)) { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Error", errorMessage)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)

        if (onAuthRequest != null) {
            // Try to determine provider from destination resource or error message
            val provider = when {
                destinationResource?.path?.startsWith("cloud://dropbox") == true -> "dropbox"
                destinationResource?.path?.startsWith("cloud://google_drive") == true -> "google_drive"
                destinationResource?.path?.startsWith("cloud://onedrive") == true -> "onedrive"
                errorMessage.contains("Dropbox", ignoreCase = true) -> "dropbox"
                errorMessage.contains("Google", ignoreCase = true) -> "google_drive"
                errorMessage.contains("OneDrive", ignoreCase = true) -> "onedrive"
                else -> null
            }
            
            if (provider != null) {
                builder.setPositiveButton(context.getString(R.string.sign_in)) { _, _ ->
                    onAuthRequest.invoke(provider)
                    dismiss()
                }
            } else {
                builder.setPositiveButton(context.getString(R.string.go_to_resources)) { _, _ ->
                    dismiss()
                }
            }
        } else {
            builder.setPositiveButton(context.getString(R.string.go_to_resources)) { _, _ ->
                dismiss()
            }
        }
            
        builder.show()
    }
}
