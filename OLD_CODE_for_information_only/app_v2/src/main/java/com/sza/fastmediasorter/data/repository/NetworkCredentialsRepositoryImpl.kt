package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.local.db.NetworkCredentialsDao
import com.sza.fastmediasorter.data.local.db.NetworkCredentialsEntity
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkCredentialsRepositoryImpl @Inject constructor(
    private val dao: NetworkCredentialsDao,
    private val settingsRepository: SettingsRepository
) : NetworkCredentialsRepository {

    override suspend fun insert(credentials: NetworkCredentialsEntity): Long {
        return dao.insert(credentials)
    }

    override suspend fun getById(id: Long): NetworkCredentialsEntity? {
        // DAO doesn't have getById, use getByTypeServerAndPort as workaround
        return null // TODO: Add getById to DAO if needed
    }

    override suspend fun getByCredentialId(credentialId: String): NetworkCredentialsEntity? {
        val entity = dao.getCredentialsById(credentialId)
        return applyDefaultCredentialsIfNeeded(entity)
    }

    override suspend fun getByTypeServerAndPort(
        type: String,
        server: String,
        port: Int
    ): NetworkCredentialsEntity? {
        val entity = dao.getByTypeServerAndPort(type, server, port)
        return applyDefaultCredentialsIfNeeded(entity)
    }

    override suspend fun getByServerAndShare(server: String, shareName: String): NetworkCredentialsEntity? {
        val entity = dao.getByServerAndShare(server, shareName)
        return applyDefaultCredentialsIfNeeded(entity)
    }

    override suspend fun getCredentialsByHost(host: String): NetworkCredentialsEntity? {
        val entity = dao.getCredentialsByHost(host)
        return applyDefaultCredentialsIfNeeded(entity)
    }

    override suspend fun update(credentials: NetworkCredentialsEntity) {
        dao.update(credentials)
    }

    override suspend fun delete(credentials: NetworkCredentialsEntity) {
        // DAO doesn't have delete by entity, use deleteByCredentialId
        dao.deleteByCredentialId(credentials.credentialId)
    }

    override fun getAllCredentials(): kotlinx.coroutines.flow.Flow<List<NetworkCredentialsEntity>> {
        return dao.getAllCredentials()
    }

    private suspend fun applyDefaultCredentialsIfNeeded(entity: NetworkCredentialsEntity?): NetworkCredentialsEntity? {
        if (entity == null) return null

        // If username and password are provided, use them
        if (entity.username.isNotEmpty() && entity.encryptedPassword.isNotEmpty()) {
            return entity
        }

        // If username or password is missing, try to use default credentials from settings
        val settings = settingsRepository.getSettings().first()
        val defaultUser = settings.defaultUser
        val defaultPassword = settings.defaultPassword

        // If default credentials are also empty, return original entity
        if (defaultUser.isEmpty() && defaultPassword.isEmpty()) {
            return entity
        }

        var newUsername = entity.username
        var newEncryptedPassword = entity.encryptedPassword

        // Use default username if entity's username is empty
        if (newUsername.isEmpty()) {
            newUsername = defaultUser
        }

        // Use default password if entity's password is empty
        // Note: entity.encryptedPassword stores the encrypted password.
        // If it's empty, it means no password was set.
        if (newEncryptedPassword.isEmpty() && defaultPassword.isNotEmpty()) {
            newEncryptedPassword = com.sza.fastmediasorter.data.local.db.CryptoHelper.encrypt(defaultPassword) ?: ""
        }

        return entity.copy(
            username = newUsername,
            encryptedPassword = newEncryptedPassword
        )
    }
}
