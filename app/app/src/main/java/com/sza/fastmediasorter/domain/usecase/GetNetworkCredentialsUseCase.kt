package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import javax.inject.Inject

/**
 * UseCase for retrieving network credentials.
 * 
 * Decrypts and loads credentials for SMB, SFTP, and FTP connections.
 */
class GetNetworkCredentialsUseCase @Inject constructor(
    private val networkCredentialsRepository: NetworkCredentialsRepository
) {
    /**
     * Get credentials by credential ID.
     * 
     * @param credentialId Unique credential identifier
     * @return Result containing NetworkCredentials or error
     */
    suspend operator fun invoke(credentialId: String): Result<NetworkCredentials> {
        return networkCredentialsRepository.getCredentials(credentialId)
    }
}
