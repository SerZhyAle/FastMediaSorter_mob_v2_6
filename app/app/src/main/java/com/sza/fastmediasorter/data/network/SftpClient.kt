package com.sza.fastmediasorter.data.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemoryDestFile
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
                    ssh.loadKeys(privateKey, null as String?)
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
                    ssh.loadKeys(privateKey, null as String?)
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            
            var bytesWritten = 0L
            val dest = object : InMemoryDestFile() {
                override fun getOutputStream(): OutputStream = object : OutputStream() {
                    override fun write(b: Int) {
                        outputStream.write(b)
                        bytesWritten++
                    }
                    override fun write(b: ByteArray, off: Int, len: Int) {
                        outputStream.write(b, off, len)
                        bytesWritten += len
                    }
                }
                override fun getOutputStream(append: Boolean): OutputStream = getOutputStream()
                override fun getLength(): Long = bytesWritten
            }
            
            sftp.get(remotePath, dest)
            val bytesTransferred = dest.length
            
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
                    ssh.loadKeys(privateKey, null as String?)
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

    /**
     * Delete a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with success or error
     */
    suspend fun deleteFile(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Unit> {
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            sftp.rm(remotePath)
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP deleted file: $remotePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SFTP delete failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Rename a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param fromPath Source file path
     * @param toPath Destination file path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with success or error
     */
    suspend fun rename(
        host: String,
        port: Int,
        fromPath: String,
        toPath: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Unit> {
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            sftp.rename(fromPath, toPath)
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP renamed $fromPath to $toPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SFTP rename failed: $fromPath -> $toPath")
            Result.failure(e)
        }
    }

    /**
     * Create a directory.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote directory path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with success or error
     */
    suspend fun createDirectory(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Unit> {
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            sftp.mkdir(remotePath)
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP created directory: $remotePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SFTP mkdir failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Upload a file.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param inputStream Input stream to read from
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with success or error
     */
    suspend fun uploadFile(
        host: String,
        port: Int,
        remotePath: String,
        inputStream: java.io.InputStream,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Unit> {
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            
            // Use put with InMemorySourceFile
            val sourceFile = object : net.schmizz.sshj.xfer.InMemorySourceFile() {
                override fun getName(): String = remotePath.substringAfterLast("/")
                override fun getLength(): Long = -1 // Unknown
                override fun getInputStream(): java.io.InputStream = inputStream
            }
            sftp.put(sourceFile, remotePath)
            
            sftp.close()
            ssh.disconnect()
            
            Timber.d("SFTP uploaded file to $remotePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SFTP upload failed: $remotePath")
            Result.failure(e)
        }
    }

    /**
     * Check if a file exists.
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param remotePath Remote file path
     * @param username Username
     * @param password Password (if using password auth)
     * @param privateKey Private key content (if using key auth)
     * @param passphrase Passphrase for private key (optional)
     * @return Result with true if exists, false otherwise
     */
    suspend fun exists(
        host: String,
        port: Int,
        remotePath: String,
        username: String,
        password: String = "",
        privateKey: String? = null,
        passphrase: String? = null
    ): Result<Boolean> {
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
                    ssh.loadKeys(privateKey, null as String?)
                }
                ssh.authPublickey(username, keyProvider)
            } else {
                ssh.authPassword(username, password)
            }
            
            val sftp = ssh.newSFTPClient()
            val exists = try {
                sftp.stat(remotePath)
                true
            } catch (e: net.schmizz.sshj.sftp.SFTPException) {
                false
            }
            
            sftp.close()
            ssh.disconnect()
            
            Result.success(exists)
        } catch (e: Exception) {
            Timber.e(e, "SFTP exists check failed: $remotePath")
            Result.failure(e)
        }
    }
}
