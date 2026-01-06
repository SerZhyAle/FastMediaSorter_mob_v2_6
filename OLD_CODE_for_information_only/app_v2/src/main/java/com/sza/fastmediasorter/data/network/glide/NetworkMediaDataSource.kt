package com.sza.fastmediasorter.data.network.glide

import android.media.MediaDataSource
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.data.remote.ftp.FtpClient
import com.sza.fastmediasorter.data.remote.sftp.SftpClient
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.data.network.model.SmbResult
import com.sza.fastmediasorter.data.network.model.SmbConnectionInfo
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException

/**
 * MediaDataSource implementation for network files (SMB/SFTP/FTP).
 * Provides direct byte access to MediaMetadataRetriever without temporary files.
 *
 * OPTIMIZATION: Eliminates disk IO for video thumbnail extraction by streaming
 * bytes directly from network protocols to MediaMetadataRetriever.
 */
class NetworkMediaDataSource(
    val path: String,
    private val fileSize: Long,
    private val credentialsId: String?,
    private val smbClient: SmbClient,
    private val sftpClient: SftpClient,
    private val ftpClient: FtpClient,
    private val credentialsRepository: NetworkCredentialsRepository
) : MediaDataSource() {

    private var isClosed = false

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (isClosed) {
            throw IOException("DataSource is closed")
        }

        if (position >= fileSize) {
            return -1 // EOF
        }

        val bytesToRead = minOf(size.toLong(), fileSize - position).toInt()

        return try {
            val bytes = readBytesFromNetwork(position, bytesToRead.toLong())
            if (bytes.isEmpty()) {
                -1 // EOF or error
            } else {
                bytes.copyInto(buffer, offset, 0, bytes.size)
                bytes.size
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading from network at position $position, size $size")
            -1
        }
    }

    override fun getSize(): Long = fileSize

    override fun close() {
        isClosed = true
    }

    private fun readBytesFromNetwork(offset: Long, length: Long): ByteArray {
        return when {
            path.startsWith("smb://") -> readFromSmb(offset, length)
            path.startsWith("sftp://") -> readFromSftp(offset, length)
            path.startsWith("ftp://") -> readFromFtp(offset, length)
            else -> throw IOException("Unsupported protocol: $path")
        }
    }

    private fun readFromSmb(offset: Long, length: Long): ByteArray = runBlocking {
        val uri = path.removePrefix("smb://")
        val parts = uri.split("/", limit = 2)
        if (parts.isEmpty()) throw IOException("Invalid SMB path")

        val serverPort = parts[0]
        val pathParts = if (parts.size > 1) parts[1] else ""

        val server: String
        val port: Int
        if (serverPort.contains(":")) {
            val sp = serverPort.split(":")
            server = sp[0]
            port = sp[1].toIntOrNull() ?: 445
        } else {
            server = serverPort
            port = 445
        }

        val credentials = if (credentialsId != null) {
            credentialsRepository.getByCredentialId(credentialsId)
        } else {
            credentialsRepository.getByTypeServerAndPort("SMB", server, port)
        } ?: throw IOException("No credentials found for SMB")

        val shareAndPath = pathParts.split("/", limit = 2)
        val shareName = if (shareAndPath.isNotEmpty()) shareAndPath[0] else (credentials.shareName ?: "")
        val remotePath = if (shareAndPath.size > 1) shareAndPath[1] else ""

        if (shareName.isEmpty()) throw IOException("No share name")

        val connectionInfo = SmbConnectionInfo(
            server = server,
            port = port,
            shareName = shareName,
            username = credentials.username,
            password = credentials.password,
            domain = credentials.domain
        )

        when (val result = smbClient.readFileBytesRange(connectionInfo, remotePath, offset, length)) {
            is SmbResult.Success -> result.data
            else -> throw IOException("SMB read failed: ${result}")
        }
    }

    private fun readFromSftp(offset: Long, length: Long): ByteArray = runBlocking {
        val uri = path.removePrefix("sftp://")
        val parts = uri.split("/", limit = 2)
        if (parts.isEmpty()) throw IOException("Invalid SFTP path")

        val serverPort = parts[0]
        val remotePath = if (parts.size > 1) "/${parts[1]}" else "/"

        val server: String
        val port: Int
        if (serverPort.contains(":")) {
            val sp = serverPort.split(":")
            server = sp[0]
            port = sp[1].toIntOrNull() ?: 22
        } else {
            server = serverPort
            port = 22
        }

        val credentials = if (credentialsId != null) {
            credentialsRepository.getByCredentialId(credentialsId)
        } else {
            credentialsRepository.getByTypeServerAndPort("SFTP", server, port)
        } ?: throw IOException("No credentials found for SFTP")

        val connectionInfo = SftpClient.SftpConnectionInfo(
            host = server,
            port = port,
            username = credentials.username,
            password = credentials.password,
            privateKey = credentials.sshPrivateKey
        )

        val result = sftpClient.readFileBytesRange(connectionInfo, remotePath, offset, length)
        result.getOrNull() ?: throw IOException("SFTP read failed")
    }

    private fun readFromFtp(offset: Long, length: Long): ByteArray = runBlocking {
        val uri = path.removePrefix("ftp://")
        val parts = uri.split("/", limit = 2)
        if (parts.isEmpty()) throw IOException("Invalid FTP path")

        val serverPort = parts[0]
        val remotePath = if (parts.size > 1) "/${parts[1]}" else "/"

        val server: String
        val port: Int
        if (serverPort.contains(":")) {
            val sp = serverPort.split(":")
            server = sp[0]
            port = sp[1].toIntOrNull() ?: 21
        } else {
            server = serverPort
            port = 21
        }

        val credentials = if (credentialsId != null) {
            credentialsRepository.getByCredentialId(credentialsId)
        } else {
            credentialsRepository.getByTypeServerAndPort("FTP", server, port)
        } ?: throw IOException("No credentials found for FTP")

        try {
            ftpClient.connect(server, port, credentials.username, credentials.password)
            if (!ftpClient.isConnected()) throw IOException("FTP connection failed")

            val result = ftpClient.readFileBytesRange(remotePath, offset, length)
            result.getOrNull() ?: throw IOException("FTP read failed")
        } finally {
            ftpClient.disconnect()
        }
    }
}
