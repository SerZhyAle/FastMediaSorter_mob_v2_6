package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import javax.inject.Inject

/**
 * UseCase for saving network credentials.
 * 
 * Encrypts and stores credentials for SMB, SFTP, and FTP connections.
 */
class SaveNetworkCredentialsUseCase @Inject constructor(
    private val networkCredentialsRepository: NetworkCredentialsRepository
) {
    /**
     * Save or update network credentials.
     * 
     * @param credentials NetworkCredentials to save
     * @return Result containing credential ID or error
     */
    suspend operator fun invoke(credentials: NetworkCredentials): Result<String> {
        return networkCredentialsRepository.saveCredentials(credentials)
    }
}
