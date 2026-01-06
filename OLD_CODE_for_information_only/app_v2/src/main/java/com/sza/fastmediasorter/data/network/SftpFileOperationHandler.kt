package com.sza.fastmediasorter.data.network

import android.content.Context
import com.sza.fastmediasorter.data.transfer.BaseFileOperationHandler
import com.sza.fastmediasorter.data.transfer.FileOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.FtpOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.LocalOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.SftpOperationStrategy
import com.sza.fastmediasorter.data.transfer.strategy.SmbOperationStrategy
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.domain.transfer.FileOperationError
import com.sza.fastmediasorter.domain.usecase.ByteProgressCallback
import com.sza.fastmediasorter.domain.usecase.FileOperation
import com.sza.fastmediasorter.domain.usecase.FileOperationResult
import com.sza.fastmediasorter.utils.SftpPathUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for SFTP file operations built on BaseFileOperationHandler + strategies.
 */
@Singleton
class SftpFileOperationHandler @Inject constructor(
    @ApplicationContext context: Context,
    private val sftpClient: SftpClient,
    private val smbClient: SmbClient,
    private val ftpClient: FtpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : BaseFileOperationHandler(context) {

    private val sftpStrategy = SftpOperationStrategy(context, sftpClient, credentialsRepository)
    private val smbStrategy = SmbOperationStrategy(context, smbClient, credentialsRepository)
    private val ftpStrategy = FtpOperationStrategy(context, ftpClient, credentialsRepository)
    private val localStrategy = LocalOperationStrategy(context)

    override fun getStrategies(): List<FileOperationStrategy> {
        return listOf(sftpStrategy, smbStrategy, ftpStrategy, localStrategy)
    }

    override suspend fun executeCopy(
        operation: FileOperation.Copy,
        progressCallback: ByteProgressCallback?
    ): FileOperationResult = super.executeCopy(operation, progressCallback)

    override suspend fun executeMove(
        operation: FileOperation.Move,
        progressCallback: ByteProgressCallback?
    ): FileOperationResult {
        val destinationPath = operation.destination.path

        // Handle Local/SAF -> SFTP move explictly
        if (destinationPath.startsWith("sftp://")) {
            Timber.d("SFTP executeMove: Starting move of ${operation.sources.size} files to $destinationPath")
            val errors = mutableListOf<String>()
            val movedPaths = mutableListOf<String>()
            var successCount = 0

            operation.sources.forEachIndexed { index, source ->
                val sourcePath = source.path
                val fileName = extractFileName(sourcePath, source.name)
                val destFilePath = if (destinationPath.endsWith("/")) "$destinationPath$fileName" else "$destinationPath/$fileName"
                
                Timber.d("SFTP executeMove: [${index + 1}/${operation.sources.size}] Moving $fileName")
                
                // 1. Upload via copyFile (which we should enable to handle SAF if not already)
                // Need to ensure sftpStrategy.copyFile handles SAF or we do it here manually via sftpClient
                // sftpStrategy uses SftpClient.uploadFile which takes InputStream. 
                // We should check if sftpStrategy resolves SAF.
                // Assuming it might NOT, we should implement a helper here similar to Ftp.
                
                val uploadResult = copyFile(sourcePath, destFilePath, true, progressCallback)
                
                if (uploadResult.isSuccess) {
                    val uploadedPath = uploadResult.getOrNull() ?: destFilePath
                    
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
                        Timber.i("SFTP executeMove: SUCCESS - moved $fileName")
                    } else {
                        val error = "Uploaded $fileName but failed to delete source"
                        Timber.w("SFTP executeMove: PARTIAL - $error")
                        movedPaths.add(uploadedPath)
                        successCount++ 
                    }
                } else {
                    val error = "Failed to upload $fileName to SFTP: ${uploadResult.exceptionOrNull()?.message}"
                    errors.add(error)
                }
            }
            return buildMoveResult(successCount, operation, movedPaths, errors)
        }
        
        return super.executeMove(operation, progressCallback)
    }

    /**
     * Override copyFile to handle cross-protocol transfers via strategies.
     * Optimization: Explicitly handle Local <-> SFTP to use the optimized SftpOperationStrategy methods.
     */
    override suspend fun copyFile(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        // Optimization: If operation involves SFTP, use SFTP strategy directly
        if (sourcePath.startsWith("sftp://") || destPath.startsWith("sftp://")) {
            return sftpStrategy.copyFile(sourcePath, destPath, overwrite, progressCallback)
        }
        return super.copyFile(sourcePath, destPath, overwrite, progressCallback)
    }

    override suspend fun moveFile(
        sourcePath: String,
        destPath: String,
        overwrite: Boolean,
        progressCallback: ByteProgressCallback?
    ): Result<String> {
        // Optimization: If operation involves SFTP, use SFTP strategy directly
        if (sourcePath.startsWith("sftp://") || destPath.startsWith("sftp://")) {
            val result = sftpStrategy.moveFile(sourcePath, destPath)
            return result.map { destPath }
        }
        return super.moveFile(sourcePath, destPath, overwrite, progressCallback)
    }

    override suspend fun executeDelete(operation: FileOperation.Delete): FileOperationResult {
        return super.executeDelete(operation)
    }

    suspend fun executeRename(operation: FileOperation.Rename): FileOperationResult = withContext(Dispatchers.IO) {
        Timber.d("SFTP executeRename: Renaming ${operation.file.name} to ${operation.newName}")

        try {
            val sftpPath = operation.file.path
            if (!sftpPath.startsWith("sftp://")) {
                Timber.e("SFTP executeRename: File is not SFTP path: $sftpPath")
                return@withContext FileOperationResult.Failure("Not an SFTP file: $sftpPath")
            }

            val connectionInfo = parseSftpPath(sftpPath)
                ?: return@withContext FileOperationResult.Failure("Invalid SFTP path: $sftpPath")

            Timber.d(
                "SFTP executeRename: Parsed - host=${connectionInfo.host}:${connectionInfo.port}, remotePath=${connectionInfo.remotePath}"
            )

            val directory = connectionInfo.remotePath.substringBeforeLast('/')
            val newRemotePath = if (directory.isEmpty() || directory == connectionInfo.remotePath) {
                operation.newName
            } else {
                "$directory/${operation.newName}"
            }

            val existsResult = sftpClient.exists(connectionInfo.toClientInfo(), newRemotePath)
            if (existsResult.getOrDefault(false)) {
                val error = "File with name '${operation.newName}' already exists"
                Timber.w("SFTP executeRename: SKIPPED - $error")
                return@withContext FileOperationResult.Failure(error)
            }

            val renameResult = sftpClient.renameFile(
                connectionInfo.toClientInfo(),
                connectionInfo.remotePath,
                operation.newName
            )

            return@withContext when {
                renameResult.isSuccess -> {
                    val directoryPath = sftpPath.substringBeforeLast('/')
                    val newPath = "$directoryPath/${operation.newName}"
                    Timber.i("SFTP executeRename: SUCCESS - renamed to $newPath")
                    FileOperationResult.Success(1, operation, listOf(newPath))
                }

                else -> {
                    val error = "${operation.file.name}\n  New name: ${operation.newName}\n  Error: ${renameResult.exceptionOrNull()?.message ?: "Rename failed"}"
                    Timber.e("SFTP executeRename: FAILED - $error")
                    FileOperationResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            val error = "${operation.file.name}\n  New name: ${operation.newName}\n  Error: ${FileOperationError.extractErrorMessage(e)}"
            Timber.e(e, "SFTP executeRename: EXCEPTION - $error")
            FileOperationResult.Failure(error)
        }
    }

    internal data class SftpConnectionInfoWithPath(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val privateKey: String?,
        val remotePath: String
    ) {
        fun toClientInfo(): SftpClient.SftpConnectionInfo {
            return SftpClient.SftpConnectionInfo(
                host = host,
                port = port,
                username = username,
                password = password,
                privateKey = privateKey,
                passphrase = password.ifEmpty { null }
            )
        }
    }

    internal suspend fun parseSftpPath(path: String): SftpConnectionInfoWithPath? {
        return try {
            val pathInfo = SftpPathUtils.parseSftpPath(path)
            if (pathInfo == null) {
                Timber.e("parseSftpPath: Failed to parse SFTP path: $path")
                return null
            }

            val (host, port, remotePath) = pathInfo
            Timber.d("parseSftpPath: Extracted host=$host, port=$port, remotePath=$remotePath")

            var credentials = credentialsRepository.getByTypeServerAndPort("SFTP", host, port)
            if (credentials == null) {
                credentials = credentialsRepository.getCredentialsByHost(host)
            }

            if (credentials == null) {
                Timber.e("parseSftpPath: No credentials found for host: $host")
                return null
            }

            SftpConnectionInfoWithPath(
                host = host,
                port = port,
                username = credentials.username,
                password = credentials.password,
                privateKey = credentials.decryptedSshPrivateKey,
                remotePath = remotePath
            )
        } catch (e: Exception) {
            Timber.e(e, "parseSftpPath: Exception parsing path: $path")
            null
        }
    }
}
