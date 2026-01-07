package com.sza.fastmediasorter.data.network

import com.hierynomus.sshj.SSHClient
import com.hierynomus.sshj.sftp.SFTPClient
import com.hierynomus.sshj.transport.verification.PromiscuousVerifier
import com.hierynomus.sshj.xfer.InMemoryDestFile
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SFTP client wrapper using SSHJ library.
 * Provides secure file transfer over SSH with password and key-based authentication.
 * 
 * SECURITY NOTE: Currently uses PromiscuousVerifier (no host key verification).
 * This is acceptable for trusted local networks but NOT for public networks.
 * Future: Implement Trust-on-First-Use (TOFU) pattern.
 */
@Singleton
class SftpClient @Inject constructor() {

    companion object {
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val SOCKET_TIMEOUT = 30000 // 30 seconds
    }

    /**
     * Test connection to SFTP server.
     * 
     * @param host Server hostname or IP
     * @param port Server port (default 22)
     * @param username Username for authentication
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with success message or error
     */
    suspend fun testConnection(
        host: String,
        port: Int = 22,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<String> {
        return try {
            Timber.d("SFTP testConnection to $host:$port (user: $username)")
            
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = CONNECTION_TIMEOUT
            ssh.timeout = SOCKET_TIMEOUT
            
            ssh.connect(host, port)
            
            if (privateKey != null) {
                // Key-based authentication
                val keyProvider = if (passphrase != null) {
                    ssh.loadKeys(privateKey, passphrase)
                } else {
                    ssh.loadKeys(privateKey, null)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                // Password authentication
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            val files = sftp.ls("/")
            
            sftp.close()
            ssh.disconnect()
            
            Result.success("Connection successful. Found ${files.size} items in root.")
        } catch (e: Exception) {
            Timber.e(e, "SFTP connection failed")
            Result.failure(e)
        }
    }

    /**
     * List files in a directory.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param path Remote directory path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with list of file names
     */
    suspend fun listFiles(
        host: String,
        port: Int,
        path: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<List<String>> {
        return try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = CONNECTION_TIMEOUT
            ssh.timeout = SOCKET_TIMEOUT
            
            ssh.connect(host, port)
            
            if (privateKey != null) {
                val keyProvider = if (passphrase != null) {
                    ssh.loadKeys(privateKey, passphrase)
                } else {
                    ssh.loadKeys(privateKey, null)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            val files = sftp.ls(path)
                .filter { !it.name.startsWith(".") && it.name != "." && it.name != ".." }
                .map { it.name }
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP listed ${files.size} files in $path")
            Result.success(files)
        } catch (e: Exception) {
            Timber.e(e, "SFTP listFiles failed: $path")
            Result.failure(e)
        }
    }

    /**
     * Download a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param outputStream Output stream to write to
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with bytes transferred
     */
    suspend fun downloadFile(
        host: String,
        port: Int,
        remotePath: String,
        outputStream: OutputStream,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Long> {
        return try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = CONNECTION_TIMEOUT
            ssh.timeout = SOCKET_TIMEOUT
            
            ssh.connect(host, port)
            
            if (privateKey != null) {
                val keyProvider = if (passphrase != null) {
                    ssh.loadKeys(privateKey, passphrase)
                } else {
                    ssh.loadKeys(privateKey, null)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            
            val dest = object : InMemoryDestFile() {
                override fun getOutputStream(): OutputStream = outputStream
            }
            
            sftp.get(remotePath, dest)
            val bytesTransferred = dest.length ?: 0L
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP downloaded $bytesTransferred bytes from $remotePath")
            Result.success(bytesTransferred)
        } catch (e: Exception) {
            Timber.e(e, "SFTP download failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Get file size.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with file size in bytes
     */
    suspend fun getFileSize(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Long> {
        return try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = CONNECTION_TIMEOUT
            ssh.timeout = SOCKET_TIMEOUT
            
            ssh.connect(host, port)
            
            if (privateKey != null) {
                val keyProvider = if (passphrase != null) {
                    ssh.loadKeys(privateKey, passphrase)
                } else {
                    ssh.loadKeys(privateKey, null)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            val attrs = sftp.stat(remotePath)
            val size = attrs.size
            
            sftp.close()
            ssh.disconnect()
            
            Result.success(size)
        } catch (e: Exception) {
            Timber.e(e, "SFTP getFileSize failed: $remotePath")
            Result.failure(e)
        }
    }
}
