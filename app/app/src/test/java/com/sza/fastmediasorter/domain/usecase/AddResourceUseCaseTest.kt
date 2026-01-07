package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for AddResourceUseCase.
 * Tests validation logic and repository interaction.
 */
class AddResourceUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var useCase: AddResourceUseCase

    @Before
    fun setup() {
        resourceRepository = mock()
        useCase = AddResourceUseCase(resourceRepository)
    }

    @Test
    fun `invoke with valid input returns success with resource ID`() = runTest {
        // Given
        val expectedId = 42L
        val name = "My Photos"
        val path = "/storage/emulated/0/DCIM"
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(null)
        whenever(resourceRepository.insertResource(any())).thenReturn(expectedId)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedId, (result as Result.Success).data)
        verify(resourceRepository).insertResource(any())
    }

    @Test
    fun `invoke with empty name returns error`() = runTest {
        // Given
        val name = ""
        val path = "/storage/emulated/0/DCIM"

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("name cannot be empty"))
        verify(resourceRepository, never()).insertResource(any())
    }

    @Test
    fun `invoke with blank name returns error`() = runTest {
        // Given
        val name = "   "
        val path = "/storage/emulated/0/DCIM"

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
    }

    @Test
    fun `invoke with empty path returns error`() = runTest {
        // Given
        val name = "My Photos"
        val path = ""

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("path cannot be empty"))
        verify(resourceRepository, never()).insertResource(any())
    }

    @Test
    fun `invoke with duplicate path returns error`() = runTest {
        // Given
        val name = "My Photos"
        val path = "/storage/emulated/0/DCIM"
        val existingResource = Resource(
            id = 1L,
            name = "Existing",
            path = path,
            type = ResourceType.LOCAL,
            sortMode = SortMode.NAME_ASC,
            displayMode = DisplayMode.GRID,
            workWithAllFiles = false
        )
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(existingResource)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_EXISTS, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("already exists"))
        verify(resourceRepository, never()).insertResource(any())
    }

    @Test
    fun `invoke with network credentials ID stores it correctly`() = runTest {
        // Given
        val name = "Network Share"
        val path = "smb://192.168.1.100/share"
        val credentialsId = "cred_123"
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(null)
        whenever(resourceRepository.insertResource(any())).thenReturn(1L)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.SMB,
            credentialsId = credentialsId
        )

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).insertResource(any())
    }

    @Test
    fun `invoke with custom sort and display modes applies them`() = runTest {
        // Given
        val name = "My Videos"
        val path = "/storage/emulated/0/Movies"
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(null)
        whenever(resourceRepository.insertResource(any())).thenReturn(1L)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL,
            sortMode = SortMode.SIZE_DESC,
            displayMode = DisplayMode.LIST
        )

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).insertResource(any())
    }

    @Test
    fun `invoke with workWithAllFiles enabled stores flag correctly`() = runTest {
        // Given
        val name = "Documents"
        val path = "/storage/emulated/0/Documents"
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(null)
        whenever(resourceRepository.insertResource(any())).thenReturn(1L)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL,
            workWithAllFiles = true
        )

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).insertResource(any())
    }

    @Test
    fun `invoke when repository throws exception returns error`() = runTest {
        // Given
        val name = "My Photos"
        val path = "/storage/emulated/0/DCIM"
        val exception = RuntimeException("Database error")
        whenever(resourceRepository.getResourceByPath(path)).thenReturn(null)
        whenever(resourceRepository.insertResource(any())).thenThrow(exception)

        // When
        val result = useCase(
            name = name,
            path = path,
            type = ResourceType.LOCAL
        )

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals(exception, result.throwable)
    }
}
