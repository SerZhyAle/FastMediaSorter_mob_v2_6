package com.sza.fastmediasorter.ui.browse.managers

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.core.util.PathUtils
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.data.network.model.SmbConnectionInfo
import com.sza.fastmediasorter.data.network.model.SmbResult
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
import com.sza.fastmediasorter.domain.model.AppSettings
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.UndoOperation
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.usecase.FileOperationUseCase
import com.sza.fastmediasorter.domain.usecase.GetDestinationsUseCase
import com.sza.fastmediasorter.ui.dialog.CopyToDialog
import com.sza.fastmediasorter.ui.dialog.MoveToDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Manages file operations (copy, move, delete, share) in BrowseActivity.
 * Coordinates with FileOperationUseCase and handles progress/result feedback.
 */
class BrowseFileOperationsManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val fileOperationUseCase: FileOperationUseCase,
    private val getDestinationsUseCase: GetDestinationsUseCase,
    private val smbClient: SmbClient,
    private val sftpClient: SftpClient,
    private val ftpClient: FtpClient,
    private val credentialsRepository: NetworkCredentialsRepository,
    private val callbacks: FileOperationCallbacks
) {
    
    interface FileOperationCallbacks {
        fun onOperationCompleted()
        fun saveUndoOperation(undoOp: UndoOperation)
        fun clearSelection()
        fun getCacheDir(): File?
        fun getExternalCacheDir(): File?
        fun onAuthRequest(provider: String)
    }
    
    fun showCopyDialog(
        selectedPaths: List<String>,
        mediaFiles: List<MediaFile>,
        resource: MediaResource
    ) {
        if (selectedPaths.isEmpty()) {
            Toast.makeText(context, R.string.no_files_selected, Toast.LENGTH_SHORT).show()
            return
        }
        
        val mediaFilesMap = mediaFiles.associateBy { it.path }
        
        // For network/cloud paths, create File with URI-compatible scheme
        val selectedFiles = selectedPaths.map { path ->
            val size = mediaFilesMap[path]?.size ?: 0L
            if (path.startsWith("smb://") || path.startsWith("sftp://") || 
                path.startsWith("ftp://") || path.startsWith("cloud://")) {
                object : File(path) {
                    override fun getAbsolutePath(): String = path
                    override fun getPath(): String = path
                    override fun length(): Long = size
                }
            } else {
                File(path)
            }
        }
        
        val currentBrowsePath = selectedPaths.firstOrNull()?.let { firstPath ->
            val lastSlashIndex = firstPath.lastIndexOf('/')
            if (lastSlashIndex > 0) firstPath.substring(0, lastSlashIndex + 1) else null
        }
        
        val dialog = CopyToDialog(
            context = context,
            sourceFiles = selectedFiles,
            sourceFolderName = resource.name,
            currentResourceId = resource.id,
            currentBrowsePath = currentBrowsePath,
            sourceCredentialsId = resource.credentialsId,
            fileOperationUseCase = fileOperationUseCase,
            getDestinationsUseCase = getDestinationsUseCase,
            overwriteFiles = false,
            onComplete = { undoOp ->
                undoOp?.let { callbacks.saveUndoOperation(it) }
                callbacks.clearSelection()
            },
            onAuthRequest = { provider ->
                callbacks.onAuthRequest(provider)
            }
        )
        dialog.show()
    }
    
    fun showMoveDialog(
        selectedPaths: List<String>,
        mediaFiles: List<MediaFile>,
        resource: MediaResource,
        settings: AppSettings
    ) {
        if (selectedPaths.isEmpty()) {
            Toast.makeText(context, R.string.no_files_selected, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check Safe Mode for move confirmation
        val shouldConfirmMove = settings.enableSafeMode && settings.confirmMove
        
        if (shouldConfirmMove) {
            // Show confirmation dialog first
            AlertDialog.Builder(context)
                .setTitle(R.string.confirm_move_title)
                .setMessage(context.getString(R.string.confirm_move_message, selectedPaths.size, resource.name))
                .setPositiveButton(R.string.move) { _, _ ->
                    showMoveDialogInternal(selectedPaths, mediaFiles, resource, settings)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            showMoveDialogInternal(selectedPaths, mediaFiles, resource, settings)
        }
    }
    
    private fun showMoveDialogInternal(
        selectedPaths: List<String>,
        mediaFiles: List<MediaFile>,
        resource: MediaResource,
        settings: AppSettings
    ) {
        
        val mediaFilesMap = mediaFiles.associateBy { it.path }
        
        val selectedFiles = selectedPaths.map { path ->
            val size = mediaFilesMap[path]?.size ?: 0L
            if (path.startsWith("smb://") || path.startsWith("sftp://") || 
                path.startsWith("ftp://") || path.startsWith("cloud://")) {
                object : File(path) {
                    override fun getAbsolutePath(): String = path
                    override fun getPath(): String = path
                    override fun length(): Long = size
                }
            } else {
                File(path)
            }
        }
        
        val currentBrowsePath = selectedPaths.firstOrNull()?.let { firstPath ->
            val lastSlashIndex = firstPath.lastIndexOf('/')
            if (lastSlashIndex > 0) firstPath.substring(0, lastSlashIndex + 1) else null
        }
        
        val dialog = MoveToDialog(
            context = context,
            sourceFiles = selectedFiles,
            sourceFolderName = resource.name,
            currentResourceId = resource.id,
            currentBrowsePath = currentBrowsePath,
            sourceCredentialsId = resource.credentialsId,
            fileOperationUseCase = fileOperationUseCase,
            getDestinationsUseCase = getDestinationsUseCase,
            overwriteFiles = settings.overwriteOnMove,
            onComplete = { undoOp ->
                undoOp?.let { callbacks.saveUndoOperation(it) }
                callbacks.clearSelection()
            },
            onAuthRequest = { provider ->
                callbacks.onAuthRequest(provider)
            }
        )
        dialog.show()
    }
    
    fun shareSelectedFiles(
        selectedFiles: List<MediaFile>,
        resource: MediaResource
    ) {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(context, R.string.no_files_selected, Toast.LENGTH_SHORT).show()
            return
        }
        
        coroutineScope.launch {
            try {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show()
                
                val uris = mutableListOf<Uri>()
                
                for (mediaFile in selectedFiles) {
                    val fileToShare: File? = when (resource.type) {
                        ResourceType.LOCAL -> File(mediaFile.path)
                        ResourceType.SMB, ResourceType.SFTP, ResourceType.FTP, ResourceType.CLOUD -> {
                            downloadNetworkFileToCache(mediaFile, resource)
                        }
                    }
                    
                    if (fileToShare != null && fileToShare.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            fileToShare
                        )
                        uris.add(uri)
                    }
                }
                
                if (uris.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val shareIntent = android.content.Intent().apply {
                    action = if (uris.size == 1) {
                        android.content.Intent.ACTION_SEND
                    } else {
                        android.content.Intent.ACTION_SEND_MULTIPLE
                    }
                    
                    if (uris.size == 1) {
                        putExtra(android.content.Intent.EXTRA_STREAM, uris[0])
                    } else {
                        putParcelableArrayListExtra(
                            android.content.Intent.EXTRA_STREAM,
                            ArrayList(uris)
                        )
                    }
                    
                    type = "*/*"
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                withContext(Dispatchers.Main) {
                    context.startActivity(
                        android.content.Intent.createChooser(shareIntent, context.getString(R.string.share))
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to share files")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_failed_to_share, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private suspend fun downloadNetworkFileToCache(mediaFile: MediaFile, resource: MediaResource): File? {
        return withContext(Dispatchers.IO) {
            val cacheDir = callbacks.getExternalCacheDir() ?: callbacks.getCacheDir() ?: return@withContext null
            val fileName = mediaFile.name
            val tempFile = File(cacheDir, "share_$fileName")
            
            val downloadSuccess = when (resource.type) {
                ResourceType.SMB -> downloadSmbFile(mediaFile.path, resource, tempFile)
                ResourceType.SFTP -> downloadSftpFile(mediaFile.path, resource, tempFile)
                ResourceType.FTP -> downloadFtpFile(mediaFile.path, resource, tempFile)
                else -> false
            }
            
            if (downloadSuccess && tempFile.exists()) tempFile else null
        }
    }
    
    private suspend fun downloadSmbFile(path: String, resource: MediaResource, tempFile: File): Boolean {
        return try {
            if (resource.credentialsId == null) return false
            
            val credentials = credentialsRepository.getByCredentialId(resource.credentialsId) ?: return false
            val uri = PathUtils.safeParseUri(path)
            val host = uri.host ?: return false
            val pathSegments = uri.pathSegments
            if (pathSegments == null || pathSegments.size < 2) return false
            
            val shareName = pathSegments[0]
            val filePath = "/" + pathSegments.drop(1).joinToString("/")
            
            tempFile.outputStream().use { outputStream ->
                val result = smbClient.downloadFile(
                    SmbConnectionInfo(
                        server = host,
                        shareName = shareName,
                        username = credentials.username,
                        password = credentials.password,
                        domain = credentials.domain,
                        port = if (uri.port > 0) uri.port else 445
                    ),
                    remotePath = filePath,
                    localOutputStream = outputStream
                )
                result is SmbResult.Success
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download SMB file")
            false
        }
    }
    
    private suspend fun downloadSftpFile(path: String, resource: MediaResource, tempFile: File): Boolean {
        return try {
            if (resource.credentialsId == null) return false
            
            val credentials = credentialsRepository.getByCredentialId(resource.credentialsId) ?: return false
            val uri = PathUtils.safeParseUri(path)
            val host = uri.host ?: return false
            val port = if (uri.port > 0) uri.port else 22
            val sftpPath = uri.path ?: return false
            
            tempFile.outputStream().use { outputStream ->
                val connectionInfo = SftpClient.SftpConnectionInfo(
                    host = host,
                    port = port,
                    username = credentials.username,
                    password = credentials.password
                )
                val result = sftpClient.downloadFile(connectionInfo, sftpPath, outputStream)
                result.isSuccess
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download SFTP file")
            false
        }
    }
    
    private suspend fun downloadFtpFile(path: String, resource: MediaResource, tempFile: File): Boolean {
        return try {
            if (resource.credentialsId == null) return false
            
            val credentials = credentialsRepository.getByCredentialId(resource.credentialsId) ?: return false
            val uri = PathUtils.safeParseUri(path)
            val host = uri.host ?: return false
            val port = if (uri.port > 0) uri.port else 21
            val ftpPath = uri.path ?: return false
            
            ftpClient.connect(host, port, credentials.username, credentials.password)
            try {
                tempFile.outputStream().use { outputStream ->
                    val result = ftpClient.downloadFile(ftpPath, outputStream)
                    result.isSuccess
                }
            } finally {
                ftpClient.disconnect()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download FTP file")
            false
        }
    }
    
    fun cleanup() {
        // Cancel any pending operations if needed
    }
}
