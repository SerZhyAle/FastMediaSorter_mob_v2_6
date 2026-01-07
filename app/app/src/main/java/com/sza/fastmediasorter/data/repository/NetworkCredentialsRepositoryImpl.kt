package com.sza.fastmediasorter.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sza.fastmediasorter.data.db.dao.NetworkCredentialsDao
import com.sza.fastmediasorter.data.db.entity.NetworkCredentialsEntity
import com.sza.fastmediasorter.data.network.FtpClient
import com.sza.fastmediasorter.data.network.SftpClient
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkCredentialsRepository using Room and EncryptedSharedPreferences.
 * 
 * Passwords are encrypted using EncryptedSharedPreferences (AES-256 encryption).
 * Database stores metadata and reference to encrypted password.
 */
@Singleton
class NetworkCredentialsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsDao: NetworkCredentialsDao,
    private val smbClient: SmbClient,
    private val sftpClient: SftpClient,
    private val ftpClient: FtpClient
) : NetworkCredentialsRepository {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "network_credentials_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun getCredentials(credentialId: String): Result<NetworkCredentials> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = credentialsDao.getByCredentialId(credentialId)
                    ?: return@withContext Result.Error("Credentials not found: $credentialId")

                // Retrieve encrypted password
                val password = encryptedPrefs.getString(entity.passwordRef, null)
                    ?: return@withContext Result.Error("Password not found in secure storage")

                val credentials = entity.toNetworkCredentials(password)
                Result.Success(credentials)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get credentials: $credentialId")
                Result.Error("Failed to retrieve credentials: ${e.message}")
            }
        }
    }

    override suspend fun saveCredentials(credentials: NetworkCredentials): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Generate password reference key
                val passwordRef = "pwd_${credentials.credentialId}"

                // Save encrypted password
                encryptedPrefs.edit().putString(passwordRef, credentials.password).apply()

                // Check if credentials already exist
                val existing = credentialsDao.getByCredentialId(credentials.credentialId)

                if (existing != null) {
                    // Update existing credentials
                    val updated = credentials.toEntity(passwordRef).copy(id = existing.id)
                    credentialsDao.update(updated)
                    Timber.d("Updated credentials: ${credentials.credentialId}")
                } else {
                    // Insert new credentials
                    val entity = credentials.toEntity(passwordRef)
                    credentialsDao.insert(entity)
                    Timber.d("Saved new credentials: ${credentials.credentialId}")
                }

                Result.Success(credentials.credentialId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save credentials")
                Result.Error("Failed to save credentials: ${e.message}")
            }
        }
    }

    override suspend fun deleteCredentials(credentialId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = credentialsDao.getByCredentialId(credentialId)
                if (entity != null) {
                    // Delete encrypted password
                    encryptedPrefs.edit().remove(entity.passwordRef).apply()

                    // Delete database entry
                    credentialsDao.deleteByCredentialId(credentialId)
                    Timber.d("Deleted credentials: $credentialId")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete credentials: $credentialId")
                Result.Error("Failed to delete credentials: ${e.message}")
            }
        }
    }

    override suspend fun testConnection(credentials: NetworkCredentials): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                when (credentials.type) {
                    NetworkType.SMB -> {
                        smbClient.testConnection(
                            server = credentials.server,
                            port = credentials.port,
                            username = credentials.username,
                            password = credentials.password,
                            domain = credentials.domain,
                            shareName = credentials.shareName ?: ""
                        )
                    }
                    NetworkType.SFTP -> {
                        if (credentials.useSshKey) {
                            sftpClient.testConnection(
                                host = credentials.server,
                                port = credentials.port,
                                username = credentials.username,
                                privateKey = credentials.sshKeyPath ?: ""
                            )
                        } else {
                            sftpClient.testConnection(
                                host = credentials.server,
                                port = credentials.port,
                                username = credentials.username,
                                password = credentials.password
                            )
                        }
                    }
                    NetworkType.FTP -> {
                        ftpClient.testConnection(
                            host = credentials.server,
                            port = credentials.port,
                            username = credentials.username,
                            password = credentials.password
                        )
                    }
                    else -> {
                        return@withContext Result.Error("Unsupported network type: ${credentials.type}")
                    }
                }

                Timber.d("Connection test successful: ${credentials.type} ${credentials.server}")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                Result.Error("Connection test failed: ${e.message}")
            }
        }
    }

    /**
     * Convert NetworkCredentialsEntity to NetworkCredentials domain model.
     */
    private fun NetworkCredentialsEntity.toNetworkCredentials(password: String): NetworkCredentials {
        return NetworkCredentials(
            credentialId = this.credentialId,
            type = NetworkType.valueOf(this.type),
            server = this.server,
            port = this.port,
            username = this.username,
            password = password,
            domain = this.domain,
            shareName = this.shareName,
            sshKeyPath = this.sshKeyPath,
            useSshKey = this.useSshKey
        )
    }

    /**
     * Convert NetworkCredentials domain model to NetworkCredentialsEntity.
     */
    private fun NetworkCredentials.toEntity(passwordRef: String): NetworkCredentialsEntity {
        return NetworkCredentialsEntity(
            credentialId = this.credentialId,
            type = this.type.name,
            server = this.server,
            port = this.port,
            username = this.username,
            passwordRef = passwordRef,
            domain = this.domain,
            shareName = this.shareName,
            sshKeyPath = this.sshKeyPath,
            useSshKey = this.useSshKey
        )
    }
}
