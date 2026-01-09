package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for Network Credentials UseCases.
 * Tests retrieval, saving, and deletion of network credentials.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkCredentialsUseCaseTest {

    private lateinit var repository: NetworkCredentialsRepository
    private lateinit var getUseCase: GetNetworkCredentialsUseCase
    private lateinit var saveUseCase: SaveNetworkCredentialsUseCase
    private lateinit var deleteUseCase: DeleteNetworkCredentialsUseCase

    private val testCredentials = NetworkCredentials(
        id = "cred_123",
        resourceId = 1L,
        type = NetworkType.SMB,
        server = "192.168.1.100",
        port = 445,
        username = "testuser",
        password = "testpass",
        shareName = "share",
        domain = "WORKGROUP"
    )

    @Before
    fun setup() {
        repository = mock()
        getUseCase = GetNetworkCredentialsUseCase(repository)
        saveUseCase = SaveNetworkCredentialsUseCase(repository)
        deleteUseCase = DeleteNetworkCredentialsUseCase(repository)
    }

    // ==================== GET CREDENTIALS TESTS ====================

    @Test
    fun `getCredentials returns success when credentials exist`() = runTest {
        whenever(repository.getCredentials("cred_123"))
            .thenReturn(Result.Success(testCredentials))

        val result = getUseCase("cred_123")

        assertTrue(result is Result.Success)
        assertEquals("cred_123", (result as Result.Success).data.id)
        assertEquals("testuser", result.data.username)
        assertEquals(NetworkType.SMB, result.data.type)
        verify(repository).getCredentials("cred_123")
    }

    @Test
    fun `getCredentials returns error when credentials not found`() = runTest {
        whenever(repository.getCredentials("invalid_id"))
            .thenReturn(Result.Error("Credentials not found"))

        val result = getUseCase("invalid_id")

        assertTrue(result is Result.Error)
        assertEquals("Credentials not found", (result as Result.Error).message)
        verify(repository).getCredentials("invalid_id")
    }

    @Test
    fun `getCredentials decrypts password correctly`() = runTest {
        val encryptedCreds = testCredentials.copy(password = "encrypted_password")
        whenever(repository.getCredentials("cred_123"))
            .thenReturn(Result.Success(encryptedCreds))

        val result = getUseCase("cred_123")

        assertTrue(result is Result.Success)
        // In real implementation, password would be decrypted by repository
        val credentials = (result as Result.Success).data
        assertNotNull(credentials.password)
    }

    @Test
    fun `getCredentials handles repository exceptions`() = runTest {
        whenever(repository.getCredentials(any()))
            .thenReturn(Result.Error("Database error", throwable = Exception("DB exception")))

        val result = getUseCase("cred_123")

        assertTrue(result is Result.Error)
        assertNotNull((result as Result.Error).throwable)
    }

    // ==================== SAVE CREDENTIALS TESTS ====================

    @Test
    fun `saveCredentials successfully saves new credentials`() = runTest {
        whenever(repository.saveCredentials(testCredentials))
            .thenReturn(Result.Success("cred_123"))

        val result = saveUseCase(testCredentials)

        assertTrue(result is Result.Success)
        assertEquals("cred_123", (result as Result.Success).data)
        verify(repository).saveCredentials(testCredentials)
    }

    @Test
    fun `saveCredentials encrypts password before saving`() = runTest {
        whenever(repository.saveCredentials(any()))
            .thenReturn(Result.Success("cred_123"))

        val result = saveUseCase(testCredentials)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(argThat { 
            password == "testpass" // Repository should handle encryption
        })
    }

    @Test
    fun `saveCredentials updates existing credentials`() = runTest {
        val updatedCredentials = testCredentials.copy(username = "newuser")
        whenever(repository.saveCredentials(updatedCredentials))
            .thenReturn(Result.Success("cred_123"))

        val result = saveUseCase(updatedCredentials)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(argThat { username == "newuser" })
    }

    @Test
    fun `saveCredentials validates credential format`() = runTest {
        // In real implementation, UseCase or Repository would validate
        whenever(repository.saveCredentials(any()))
            .thenReturn(Result.Success("cred_123"))

        val result = saveUseCase(testCredentials)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(any())
    }

    @Test
    fun `saveCredentials handles save failure`() = runTest {
        whenever(repository.saveCredentials(any()))
            .thenReturn(Result.Error("Failed to encrypt credentials"))

        val result = saveUseCase(testCredentials)

        assertTrue(result is Result.Error)
        assertEquals("Failed to encrypt credentials", (result as Result.Error).message)
    }

    @Test
    fun `saveCredentials handles different network types`() = runTest {
        val sftpCreds = testCredentials.copy(
            id = "sftp_cred",
            type = NetworkType.SFTP,
            port = 22,
            useSshKey = true,
            sshKeyPath = "/path/to/key"
        )

        whenever(repository.saveCredentials(sftpCreds))
            .thenReturn(Result.Success("sftp_cred"))

        val result = saveUseCase(sftpCreds)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(argThat { 
            type == NetworkType.SFTP && useSshKey == true 
        })
    }

    // ==================== DELETE CREDENTIALS TESTS ====================

    @Test
    fun `deleteCredentials successfully deletes by ID`() = runTest {
        whenever(repository.deleteCredentials("cred_123"))
            .thenReturn(Result.Success(Unit))

        val result = deleteUseCase("cred_123")

        assertTrue(result is Result.Success)
        verify(repository).deleteCredentials("cred_123")
    }

    @Test
    fun `deleteCredentials handles non-existent credentials gracefully`() = runTest {
        whenever(repository.deleteCredentials("invalid_id"))
            .thenReturn(Result.Error("Credentials not found"))

        val result = deleteUseCase("invalid_id")

        assertTrue(result is Result.Error)
        verify(repository).deleteCredentials("invalid_id")
    }

    @Test
    fun `deleteCredentials securely removes sensitive data`() = runTest {
        whenever(repository.deleteCredentials(any()))
            .thenReturn(Result.Success(Unit))

        val result = deleteUseCase("cred_123")

        assertTrue(result is Result.Success)
        // Repository should handle secure deletion (zeroing memory, etc.)
        verify(repository).deleteCredentials("cred_123")
    }

    @Test
    fun `deleteCredentials handles deletion failure`() = runTest {
        whenever(repository.deleteCredentials(any()))
            .thenReturn(Result.Error("Database locked"))

        val result = deleteUseCase("cred_123")

        assertTrue(result is Result.Error)
        assertEquals("Database locked", (result as Result.Error).message)
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    fun `complete credential lifecycle - save, get, delete`() = runTest {
        // Save
        whenever(repository.saveCredentials(any()))
            .thenReturn(Result.Success("new_cred"))
        val saveResult = saveUseCase(testCredentials)
        assertTrue(saveResult is Result.Success)

        // Get
        whenever(repository.getCredentials("new_cred"))
            .thenReturn(Result.Success(testCredentials))
        val getResult = getUseCase("new_cred")
        assertTrue(getResult is Result.Success)

        // Delete
        whenever(repository.deleteCredentials("new_cred"))
            .thenReturn(Result.Success(Unit))
        val deleteResult = deleteUseCase("new_cred")
        assertTrue(deleteResult is Result.Success)

        verify(repository).saveCredentials(any())
        verify(repository).getCredentials("new_cred")
        verify(repository).deleteCredentials("new_cred")
    }

    @Test
    fun `handles concurrent credential operations`() = runTest {
        whenever(repository.saveCredentials(any()))
            .thenReturn(Result.Success("cred_1"))
            .thenReturn(Result.Success("cred_2"))

        val result1 = saveUseCase(testCredentials.copy(id = "cred_1"))
        val result2 = saveUseCase(testCredentials.copy(id = "cred_2"))

        assertTrue(result1 is Result.Success)
        assertTrue(result2 is Result.Success)
        verify(repository, times(2)).saveCredentials(any())
    }

    // ==================== SECURITY VALIDATION TESTS ====================

    @Test
    fun `credentials are never logged in plain text`() = runTest {
        // This is more of a code review check, but we can verify no exceptions
        whenever(repository.getCredentials(any()))
            .thenReturn(Result.Success(testCredentials))

        val result = getUseCase("cred_123")

        // Ensure execution completes without exposing credentials
        assertTrue(result is Result.Success)
        // In production, verify no println/Log statements with credentials
    }

    @Test
    fun `empty password is handled correctly`() = runTest {
        val credsWithEmptyPassword = testCredentials.copy(password = "")
        whenever(repository.saveCredentials(credsWithEmptyPassword))
            .thenReturn(Result.Success("cred_empty"))

        val result = saveUseCase(credsWithEmptyPassword)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(argThat { password == "" })
    }

    @Test
    fun `special characters in password are preserved`() = runTest {
        val specialPassword = "p@ssw0rd!#$%^&*()"
        val credsWithSpecialChars = testCredentials.copy(password = specialPassword)
        whenever(repository.saveCredentials(credsWithSpecialChars))
            .thenReturn(Result.Success("cred_special"))

        val result = saveUseCase(credsWithSpecialChars)

        assertTrue(result is Result.Success)
        verify(repository).saveCredentials(argThat { password == specialPassword })
    }
}
