package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import javax.inject.Inject

/**
 * UseCase for deleting network credentials.
 * 
 * Removes stored credentials and encrypted password.
 */
class DeleteNetworkCredentialsUseCase @Inject constructor(
    private val networkCredentialsRepository: NetworkCredentialsRepository
) {
    /**
     * Delete credentials by credential ID.
     * 
     * @param credentialId Unique credential identifier
     * @return Result indicating success or error
     */
    suspend operator fun invoke(credentialId: String): Result<Unit> {
        return networkCredentialsRepository.deleteCredentials(credentialId)
    }
}
