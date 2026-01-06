package com.sza.fastmediasorter.data.network

import android.content.Context
import android.net.Uri
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
import com.sza.fastmediasorter.data.transfer.BaseFileOperationHandler
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.FtpOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.LocalOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.SftpOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.SmbOperationStrategy
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.data.network.model.SmbResult
import com.sza.fastmediasorter.data.network.model.SmbConnectionInfo
import com.sza.fastmediasorter.domain.transfer.FileOperationError
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import com.sza.fastmediasorter.domain.usecase.FileOperation
import com.sza.fastmediasorter.domain.usecase.FileOperationResult
import com.sza.fastmediasorter.utils.FtpPathUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for FTP file operations built on BaseFileOperationHandler.
 *
 * Key requirement: SAF (content:/) sources must be supported for Local→FTP operations.
 * Cross-protocol FTP→SFTP/SMB transfers are implemented via temp file (download + upload).
 */
@Singleton
class FtpFileOperationHandler @Inject constructor(
    @ApplicationContext context: Context,
    private val ftpClient: FtpClient,
    private val smbClient: SmbClient,
    private val sftpClient: SftpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : BaseFileOperationHandler(context) {

    private val ftpStrategy = FtpOperationStrategy(context, ftpClient, credentialsRepository)
    private val smbStrategy = SmbOperationStrategy(context, smbClient, credentialsRepository)
    private val sftpStrategy = SftpOperationStrategy(context, sftpClient, credentialsRepository)
    private val localStrategy = LocalOperationStrategy(context)

    override fun getStrategies(): List<FileOperationStrategy> {
        return listOf(ftpStrategy, smbStrategy, sftpStrategy, localStrategy)
    }

    internal data class FtpConnectionInfoWithPath(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val remotePath: String
    )

    override suspend fun executeMove(
        operation: FileOperation.Move,
        progressCallback: ByteProgressCallback?
    ): FileOperationResult {
        val destinationPath = operation.destination.path
        
        // Handle Local/SAF -> FTP move explictly
        if (destinationPath.startsWith("ftp://")) {
            Timber.d("FTP executeMove: Starting move of ${operation.sources.size} files to $destinationPath")
            val errors = mutableListOf<String>()
            val movedPaths = mutableListOf<String>()
            var successCount = 0

            operation.sources.forEachIndexed { index, source ->
                val sourcePath = source.path
                val fileName = extractFileName(sourcePath, source.name)
                // Ensure dest path ends with / before appending filename if needed, or handle in uploadToFtp
                // uploadToFtp expects full file path
                val destFilePath = if (destinationPath.endsWith("/")) "$destinationPath$fileName" else "$destinationPath/$fileName"
                
                Timber.d("FTP executeMove: [${index + 1}/${operation.sources.size}] Moving $fileName")
                
                // 1. Upload
                val uploadedPath = uploadToFtp(File(sourcePath), destFilePath, progressCallback)
                
                if (uploadedPath != null) {
                    // 2. Delete Source
                    val deleteSuccess = if (sourcePath.startsWith("content:/")) {
                        deleteWithSaf(sourcePath)
                    } else {
                        // Check if local file
                         if (sourcePath.startsWith("/") || sourcePath.matches(Regex("^[a-zA-Z]:.*"))) {
                             File(sourcePath).delete()
                         } else {
                             // Network source
                             deleteFile(sourcePath).isSuccess
                         }
                    }
                    
                    if (deleteSuccess) {
                        movedPaths.add(uploadedPath)
                        successCount++
                        Timber.i("FTP executeMove: SUCCESS - moved $fileName")
                    } else {
                        val error = "Uploaded $fileName but failed to delete source"
                        Timber.w("FTP executeMove: PARTIAL - $error")
                        movedPaths.add(uploadedPath)
                        successCount++ 
                    }
                } else {
                    val error = "Failed to upload $fileName to FTP"
                    errors.add(error)
                }
            }
            return buildMoveResult(successCount, operation, movedPaths, errors)
        }
        
        return super.executeMove(operation, progressCallback)
    }

    suspend fun executeRename(operation: FileOperation.Rename): FileOperationResult = withContext(Dispatchers.IO) {
        Timber.d("FTP executeRename: Renaming ${operation.file.name} to ${operation.newName}")
        
        try {
            // Use path to preserve FTP URL format for SAF URIs
            val ftpPath = operation.file.path
            
            if (!ftpPath.startsWith("ftp://")) {
                Timber.e("FTP executeRename: File is not FTP path: $ftpPath")
                return@withContext FileOperationResult.Failure("Not an FTP file: $ftpPath")
            }
            
            val connectionInfo = parseFtpPath(ftpPath)
            if (connectionInfo == null) {
                Timber.e("FTP executeRename: Failed to parse FTP path: $ftpPath")
                return@withContext FileOperationResult.Failure("Invalid FTP path: $ftpPath")
            }
            
            Timber.d("FTP executeRename: Parsed - host=${connectionInfo.host}:${connectionInfo.port}, remotePath=${connectionInfo.remotePath}")
            
            // Check if file with new name already exists
            val directory = connectionInfo.remotePath.substringBeforeLast('/')
            val newRemotePath = if (directory.isEmpty()) operation.newName else "$directory/${operation.newName}"
            val existsResult = ftpClient.existsWithNewConnection(
                connectionInfo.host,
                connectionInfo.port,
                connectionInfo.username,
                connectionInfo.password,
                newRemotePath
            )
            if (existsResult.getOrDefault(false)) {
                val error = "File with name '${operation.newName}' already exists"
                Timber.w("FTP executeRename: SKIPPED - $error")
                return@withContext FileOperationResult.Failure(error)
            }
            
            val renameResult = ftpClient.renameFileWithNewConnection(
                connectionInfo.host,
                connectionInfo.port,
                connectionInfo.username,
                connectionInfo.password,
                connectionInfo.remotePath,
                operation.newName
            )
            
            when {
                renameResult.isSuccess -> {
                    val directory = ftpPath.substringBeforeLast('/')
                    val newPath = "$directory/${operation.newName}"
                    Timber.i("FTP executeRename: SUCCESS - renamed to $newPath")
                    FileOperationResult.Success(1, operation, listOf(newPath))
                }
                else -> {
                    val error = "${operation.file.name}\n  New name: ${operation.newName}\n  Error: ${renameResult.exceptionOrNull()?.message ?: "Rename failed"}"
                    Timber.e("FTP executeRename: FAILED - $error")
                    FileOperationResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            val error = "${operation.file.name}\n  New name: ${operation.newName}\n  Error: ${FileOperationError.extractErrorMessage(e)}"
            Timber.e(e, "FTP executeRename: EXCEPTION - $error")
            FileOperationResult.Failure(error)
        }
    }

    override suspend fun copyFile(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val source = normalizeNetworkPath(sourcePath)
        val destination = normalizeNetworkPath(destPath)

        val isSourceFtp = source.startsWith("ftp://")
        val isDestFtp = destination.startsWith("ftp://")
        val isDestSftp = destination.startsWith("sftp://")
        val isDestSmb = destination.startsWith("smb://")

        if (!overwrite) {
            val existsResult = existsAtDestination(destination)
            if (existsResult.isFailure) return Result.failure(existsResult.exceptionOrNull() ?: Exception("Exists check failed"))
            if (existsResult.getOrDefault(false)) {
                return Result.failure(Exception("Destination file already exists"))
            }
        }

        return when {
            isSourceFtp && isDestSftp -> copyFtpToSftp(source, destination)
            isSourceFtp && isDestSmb -> copyFtpToSmb(source, destination, progressCallback)
            isSourceFtp && isDestFtp -> copyFtpToFtp(source, destination)?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to copy between FTP servers"))
            isSourceFtp && !isDestFtp -> {
                val localFile = File(destination)
                downloadFromFtp(source, localFile, progressCallback)?.let { Result.success(destination) }
                    ?: Result.failure(Exception("Failed to download from FTP"))
            }
            !isSourceFtp && isDestFtp -> uploadToFtp(File(source), destination, progressCallback)?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to upload to FTP"))
            else -> Result.failure(IllegalArgumentException("Unsupported operation: $source -> $destination"))
        }
    }

    override suspend fun deleteFile(filePath: String): Result<Unit> {
        val normalized = normalizeNetworkPath(filePath)

        return when {
            normalized.startsWith("ftp://") -> {
                if (deleteFromFtp(normalized)) Result.success(Unit)
                else Result.failure(Exception("Failed to delete from FTP"))
            }
            normalized.startsWith("content:/") -> {
                if (deleteWithSaf(normalized)) Result.success(Unit)
                else Result.failure(Exception("Failed to delete content URI"))
            }
            else -> super.deleteFile(normalized)
        }
    }

    override suspend fun createTrashFolder(firstFilePath: String): String? {
        val normalized = normalizeNetworkPath(firstFilePath)
        if (!normalized.startsWith("ftp://")) {
            return super.createTrashFolder(firstFilePath)
        }

        val parentDir = normalized.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentDir.isEmpty()) return null

        val trashDirPath = "$parentDir/.trash_${System.currentTimeMillis()}"
        val connectionInfo = parseFtpPath(trashDirPath) ?: return null

        val createResult = ftpClient.createDirectoryWithNewConnection(
            connectionInfo.host,
            connectionInfo.port,
            connectionInfo.username,
            connectionInfo.password,
            connectionInfo.remotePath
        )

        return if (createResult.isSuccess) trashDirPath else null
    }

    override suspend fun moveToTrash(sourcePath: String, trashPath: String, fileName: String): Result<Unit> {
        val source = normalizeNetworkPath(sourcePath)
        val trash = normalizeNetworkPath(trashPath)

        if (!source.startsWith("ftp://")) {
            return super.moveToTrash(sourcePath, trashPath, fileName)
        }

        val sourceInfo = parseFtpPath(source)
            ?: return Result.failure(IllegalArgumentException("Invalid FTP path: $source"))

        val trashDirName = trash.substringBeforeLast('/').substringAfterLast('/')
        if (trashDirName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid trash path: $trash"))
        }

        val relativeNewName = "$trashDirName/$fileName"
        return ftpClient.renameFileWithNewConnection(
            sourceInfo.host,
            sourceInfo.port,
            sourceInfo.username,
            sourceInfo.password,
            sourceInfo.remotePath,
            relativeNewName
        )
    }

    private suspend fun downloadFromFtp(
        ftpPath: String, 
        localFile: File,
        progressCallback: ByteProgressCallback? = null
    ): File? {
        Timber.d("downloadFromFtp: $ftpPath → ${localFile.absolutePath}")
        
        val connectionInfo = parseFtpPath(ftpPath)
        if (connectionInfo == null) {
            Timber.e("downloadFromFtp: Failed to parse FTP path: $ftpPath")
            return null
        }
        
        Timber.d("downloadFromFtp: Parsed - host=${connectionInfo.host}:${connectionInfo.port}")
        
        // Use file output stream directly to avoid OOM with large files
        // Use new connection to avoid blocking UI
        return try {
            val fileSize = 0L // FTP doesn't easily provide file size before download, pass 0L for now
            localFile.outputStream().use { outputStream ->
                val downloadResult = ftpClient.downloadFileWithNewConnection(
                    connectionInfo.host,
                    connectionInfo.port,
                    connectionInfo.username,
                    connectionInfo.password,
                    connectionInfo.remotePath,
                    outputStream
                )
                
                if (downloadResult.isSuccess) {
                    Timber.i("downloadFromFtp: SUCCESS - downloaded to ${localFile.name}")
                    localFile
                } else {
                    Timber.e("downloadFromFtp: FAILED - ${downloadResult.exceptionOrNull()?.message}")
                    // Clean up partial file
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "downloadFromFtp: Exception during download")
            if (localFile.exists()) {
                localFile.delete()
            }
            null
        }
    }

    private suspend fun uploadToFtp(
        source: File, 
        ftpPath: String,
        progressCallback: ByteProgressCallback? = null
    ): String? {
        Timber.d("uploadToFtp: ${source.path} → $ftpPath")
        
        // Handle SAF URIs (content:/ or content://) using ContentResolver
        val inputStream = if (source.path.startsWith("content:/")) {
            try {
                // Normalize content URI for parsing
                val normalizedUri = if (source.path.startsWith("content://")) source.path 
                                   else source.path.replaceFirst("content:/", "content://")
                val uri = Uri.parse(normalizedUri)
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                Timber.e(e, "uploadToFtp: Failed to open SAF URI: ${source.path}")
                return null
            }
        } else {
            // Handle regular file paths
            if (!source.exists()) {
                Timber.e("uploadToFtp: Local file does not exist: ${source.path}")
                return null
            }
            try {
                source.inputStream()
            } catch (e: Exception) {
                Timber.e(e, "uploadToFtp: Failed to open file: ${source.path}")
                return null
            }
        }
        
        if (inputStream == null) {
            Timber.e("uploadToFtp: Failed to get input stream for: ${source.path}")
            return null
        }
        
        val fileSize = if (source.path.startsWith("content:/")) {
            // For SAF, try to get size from DocumentFile or fallback to stream available()
            try {
                // Normalize content URI for parsing
                val normalizedUri = if (source.path.startsWith("content://")) source.path 
                                   else source.path.replaceFirst("content:/", "content://")
                val uri = Uri.parse(normalizedUri)
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: inputStream.available().toLong()
            } catch (e: Exception) {
                Timber.w(e, "uploadToFtp: Failed to get SAF file size, using available()")
                inputStream.available().toLong()
            }
        } else {
            source.length()
        }
        Timber.d("uploadToFtp: File size=$fileSize bytes")
        
        val connectionInfo = parseFtpPath(ftpPath)
        if (connectionInfo == null) {
            Timber.e("uploadToFtp: Failed to parse FTP path: $ftpPath")
            return null
        }
        
        Timber.d("uploadToFtp: Parsed - host=${connectionInfo.host}:${connectionInfo.port}")
        
        // Use new connection to avoid blocking UI
        return try {
            inputStream.use { stream ->
                val uploadResult = ftpClient.uploadFileWithNewConnection(
                    connectionInfo.host,
                    connectionInfo.port,
                    connectionInfo.username,
                    connectionInfo.password,
                    connectionInfo.remotePath,
                    stream,
                    fileSize,
                    progressCallback
                )

                if (uploadResult.isSuccess) {
                    Timber.i("uploadToFtp: SUCCESS - uploaded ${source.name}")
                    ftpPath
                } else {
                    Timber.e("uploadToFtp: FAILED - ${uploadResult.exceptionOrNull()?.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "uploadToFtp: Exception during upload")
            null
        }
    }

    private suspend fun deleteFromFtp(ftpPath: String): Boolean {
        Timber.d("deleteFromFtp: $ftpPath")
        
        val connectionInfo = parseFtpPath(ftpPath)
        if (connectionInfo == null) {
            Timber.e("deleteFromFtp: Failed to parse FTP path: $ftpPath")
            return false
        }
        
        Timber.d("deleteFromFtp: Parsed - host=${connectionInfo.host}:${connectionInfo.port}")

        val deleteResult = ftpClient.deleteFileWithNewConnection(
            connectionInfo.host,
            connectionInfo.port,
            connectionInfo.username,
            connectionInfo.password,
            connectionInfo.remotePath
        )
        
        return when {
            deleteResult.isSuccess -> {
                Timber.i("deleteFromFtp: SUCCESS")
                true
            }
            else -> {
                Timber.e("deleteFromFtp: FAILED - ${deleteResult.exceptionOrNull()?.message}")
                false
            }
        }
    }

    private suspend fun copyFtpToFtp(sourcePath: String, destPath: String): String? {
        Timber.d("copyFtpToFtp: $sourcePath → $destPath")
        
        // Download to temp file then upload to avoid OOM
        val sourceConnectionInfo = parseFtpPath(sourcePath)
        if (sourceConnectionInfo == null) {
            Timber.e("copyFtpToFtp: Failed to parse source FTP path: $sourcePath")
            return null
        }
        
        Timber.d("copyFtpToFtp: Source parsed - host=${sourceConnectionInfo.host}:${sourceConnectionInfo.port}")
        
        // Create temp file
        val tempFile = File.createTempFile("ftp_copy_", ".tmp")
        
        try {
            // Download from source using new connection
            val downloadResult = tempFile.outputStream().use { outputStream ->
                ftpClient.downloadFileWithNewConnection(
                    sourceConnectionInfo.host,
                    sourceConnectionInfo.port,
                    sourceConnectionInfo.username,
                    sourceConnectionInfo.password,
                    sourceConnectionInfo.remotePath,
                    outputStream
                )
            }
            
            if (downloadResult.isFailure) {
                Timber.e("copyFtpToFtp: Download FAILED - ${downloadResult.exceptionOrNull()?.message}")
                return null
            }
            
            Timber.d("copyFtpToFtp: Downloaded ${tempFile.length()} bytes to temp file")
            
            // Upload to destination
            val destConnectionInfo = parseFtpPath(destPath)
            if (destConnectionInfo == null) {
                Timber.e("copyFtpToFtp: Failed to parse dest FTP path: $destPath")
                return null
            }
            
            Timber.d("copyFtpToFtp: Dest parsed - host=${destConnectionInfo.host}:${destConnectionInfo.port}")
            
            // Upload to destination using new connection
            val uploadResult = tempFile.inputStream().use { inputStream ->
                ftpClient.uploadFileWithNewConnection(
                    destConnectionInfo.host,
                    destConnectionInfo.port,
                    destConnectionInfo.username,
                    destConnectionInfo.password,
                    destConnectionInfo.remotePath,
                    inputStream
                )
            }

            return when {
                uploadResult.isSuccess -> {
                    Timber.i("copyFtpToFtp: SUCCESS - copied ${tempFile.length()} bytes between FTP servers")
                    destPath
                }
                else -> {
                    Timber.e("copyFtpToFtp: Upload FAILED - ${uploadResult.exceptionOrNull()?.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "copyFtpToFtp: Exception during copy")
            return null
        } finally {
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Normalize FTP path: "ftp:/host" -> "ftp://host"
     * Handles malformed paths where single slash is used instead of double slash
     */
    internal suspend fun parseFtpPath(path: String): FtpConnectionInfoWithPath? {
        return try {
            // Use FtpPathUtils for path parsing
            val pathInfo = FtpPathUtils.parseFtpPath(path)
            if (pathInfo == null) {
                Timber.e("parseFtpPath: Failed to parse FTP path: $path")
                return null
            }
            
            val (host, port, remotePath) = pathInfo
            Timber.d("parseFtpPath: Extracted host=$host, port=$port, remotePath=$remotePath")
            
            // Get credentials from database
            // Try to get specific FTP credentials first
            var credentials = credentialsRepository.getByTypeServerAndPort("FTP", host, port)
            
            // Fallback to host-based lookup if not found
            if (credentials == null) {
                credentials = credentialsRepository.getCredentialsByHost(host)
            }

            if (credentials == null) {
                Timber.e("parseFtpPath: No credentials found for host: $host")
                return null
            }
            
            FtpConnectionInfoWithPath(
                host = host,
                port = port,
                username = credentials.username,
                password = credentials.password,
                remotePath = remotePath
            )
        } catch (e: Exception) {
            Timber.e(e, "parseFtpPath: Exception parsing path: $path")
            null
        }
    }

    private fun normalizeNetworkPath(path: String): String {
        val normalized = path.replace('\\', '/')

        return when {
            normalized.startsWith("ftp:", ignoreCase = true) -> FtpPathUtils.normalizeFtpPath(normalized)
            normalized.startsWith("sftp:/", ignoreCase = true) -> "sftp://" + normalized.substringAfter("sftp:/").trimStart('/')
            normalized.startsWith("smb:/", ignoreCase = true) -> "smb://" + normalized.substringAfter("smb:/").trimStart('/')
            else -> normalized
        }
    }

    private suspend fun existsAtDestination(destinationPath: String): Result<Boolean> {
        return when {
            destinationPath.startsWith("ftp://") -> {
                val connectionInfo = parseFtpPath(destinationPath)
                    ?: return Result.failure(IllegalArgumentException("Invalid FTP path: $destinationPath"))
                ftpClient.existsWithNewConnection(
                    connectionInfo.host,
                    connectionInfo.port,
                    connectionInfo.username,
                    connectionInfo.password,
                    connectionInfo.remotePath
                )
            }
            destinationPath.startsWith("sftp://") -> {
                val (connectionInfo, remotePath) = parseSftpDestination(destinationPath)
                    ?: return Result.failure(IllegalArgumentException("Invalid SFTP path: $destinationPath"))
                sftpClient.exists(connectionInfo, remotePath)
            }
            destinationPath.startsWith("smb://") -> {
                val (connectionInfo, remotePath) = parseSmbDestination(destinationPath)
                    ?: return Result.failure(IllegalArgumentException("Invalid SMB path: $destinationPath"))
                when (val existsResult = smbClient.exists(connectionInfo, remotePath)) {
                    is SmbResult.Success -> Result.success(existsResult.data)
                    is SmbResult.Error -> Result.failure(Exception(existsResult.message, existsResult.exception))
                }
            }
            else -> Result.success(File(destinationPath).exists())
        }
    }

    private suspend fun copyFtpToSftp(sourceFtpPath: String, destSftpPath: String): Result<String> {
        val ftpConnectionInfo = parseFtpPath(sourceFtpPath)
            ?: return Result.failure(IllegalArgumentException("Invalid FTP path: $sourceFtpPath"))

        val (sftpConnectionInfo, sftpRemotePath) = parseSftpDestination(destSftpPath)
            ?: return Result.failure(IllegalArgumentException("Invalid SFTP path: $destSftpPath"))

        val tempFile = File.createTempFile("ftp_sftp_copy_", ".tmp", context.cacheDir)
        return try {
            val downloadResult = tempFile.outputStream().use { outputStream ->
                ftpClient.downloadFileWithNewConnection(
                    ftpConnectionInfo.host,
                    ftpConnectionInfo.port,
                    ftpConnectionInfo.username,
                    ftpConnectionInfo.password,
                    ftpConnectionInfo.remotePath,
                    outputStream
                )
            }
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("FTP download failed"))
            }

            val uploadResult = tempFile.inputStream().use { inputStream ->
                sftpClient.uploadFile(
                    sftpConnectionInfo,
                    sftpRemotePath,
                    inputStream,
                    tempFile.length()
                )
            }

            if (uploadResult.isSuccess) Result.success(destSftpPath)
            else Result.failure(uploadResult.exceptionOrNull() ?: Exception("SFTP upload failed"))
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun copyFtpToSmb(
        sourceFtpPath: String,
        destSmbPath: String,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        val ftpConnectionInfo = parseFtpPath(sourceFtpPath)
            ?: return Result.failure(IllegalArgumentException("Invalid FTP path: $sourceFtpPath"))

        val (smbConnectionInfo, smbRemotePath) = parseSmbDestination(destSmbPath)
            ?: return Result.failure(IllegalArgumentException("Invalid SMB path: $destSmbPath"))

        val tempFile = File.createTempFile("ftp_smb_copy_", ".tmp", context.cacheDir)
        return try {
            val downloadResult = tempFile.outputStream().use { outputStream ->
                ftpClient.downloadFileWithNewConnection(
                    ftpConnectionInfo.host,
                    ftpConnectionInfo.port,
                    ftpConnectionInfo.username,
                    ftpConnectionInfo.password,
                    ftpConnectionInfo.remotePath,
                    outputStream
                )
            }
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("FTP download failed"))
            }

            val uploadResult = tempFile.inputStream().use { inputStream ->
                smbClient.uploadFile(
                    smbConnectionInfo,
                    smbRemotePath,
                    inputStream,
                    tempFile.length(),
                    progressCallback
                )
            }

            when (uploadResult) {
                is SmbResult.Success -> Result.success(destSmbPath)
                is SmbResult.Error -> Result.failure(Exception(uploadResult.message, uploadResult.exception))
            }
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun parseSftpDestination(path: String): Pair<SftpClient.SftpConnectionInfo, String>? {
        val parts = path.removePrefix("sftp://").split("/", limit = 2)
        if (parts.isEmpty() || parts[0].isBlank()) return null

        val hostPort = parts[0].split(":", limit = 2)
        val host = hostPort[0]
        val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 22 else 22
        val remotePath = if (parts.size > 1) "/" + parts[1] else "/"

        val credentials = credentialsRepository.getByTypeServerAndPort("SFTP", host, port)
            ?: credentialsRepository.getCredentialsByHost(host)
            ?: return null

        val connectionInfo = SftpClient.SftpConnectionInfo(
            host = host,
            port = port,
            username = credentials.username,
            password = credentials.password,
            privateKey = credentials.decryptedSshPrivateKey,
            passphrase = null
        )

        return connectionInfo to remotePath
    }

    private suspend fun parseSmbDestination(path: String): Pair<SmbConnectionInfo, String>? {
        val parts = path.removePrefix("smb://").split("/", limit = 3)
        if (parts.size < 2) return null

        val serverWithPort = parts[0]
        val shareName = parts[1]
        val remotePath = if (parts.size > 2) parts[2] else ""

        val server = if (serverWithPort.contains(':')) serverWithPort.substringBefore(':') else serverWithPort
        val port = if (serverWithPort.contains(':')) serverWithPort.substringAfter(':').toIntOrNull() ?: 445 else 445

        val credentials = credentialsRepository.getByServerAndShare(server, shareName)
            ?: credentialsRepository.getCredentialsByHost(server)
            ?: return null

        val connectionInfo = SmbConnectionInfo(
            server = server,
            shareName = shareName,
            username = credentials.username,
            password = credentials.password,
            domain = credentials.domain,
            port = port
        )

        return connectionInfo to remotePath
    }
}
