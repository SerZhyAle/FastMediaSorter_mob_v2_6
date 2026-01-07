package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.Result

/**
 * Repository interface for managing network credentials.
 * 
 * Provides secure storage and retrieval of credentials for SMB, SFTP, and FTP connections.
 * Passwords are encrypted using EncryptedSharedPreferences.
 */
interface NetworkCredentialsRepository {

    /**
     * Get credentials by credential ID.
     * 
     * @param credentialId Unique credential identifier
     * @return Result containing NetworkCredentials or error
     */
    suspend fun getCredentials(credentialId: String): Result<NetworkCredentials>

    /**
     * Save or update network credentials.
     * 
     * @param credentials NetworkCredentials to save
     * @return Result containing saved credential ID or error
     */
    suspend fun saveCredentials(credentials: NetworkCredentials): Result<String>

    /**
     * Delete credentials by credential ID.
     * 
     * @param credentialId Unique credential identifier
     * @return Result indicating success or error
     */
    suspend fun deleteCredentials(credentialId: String): Result<Unit>

    /**
     * Test connection with given credentials.
     * 
     * @param credentials NetworkCredentials to test
     * @return Result indicating connection success or error with details
     */
    suspend fun testConnection(credentials: NetworkCredentials): Result<Unit>
}
