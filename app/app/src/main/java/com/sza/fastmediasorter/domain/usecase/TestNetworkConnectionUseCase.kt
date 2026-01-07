package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import javax.inject.Inject

/**
 * UseCase for testing network connection with credentials.
 * 
 * Validates credentials by attempting to connect to the server.
 */
class TestNetworkConnectionUseCase @Inject constructor(
    private val networkCredentialsRepository: NetworkCredentialsRepository
) {
    /**
     * Test connection with given credentials.
     * 
     * @param credentials NetworkCredentials to test
     * @return Result indicating success or error with details
     */
    suspend operator fun invoke(credentials: NetworkCredentials): Result<Unit> {
        return networkCredentialsRepository.testConnection(credentials)
    }
}
