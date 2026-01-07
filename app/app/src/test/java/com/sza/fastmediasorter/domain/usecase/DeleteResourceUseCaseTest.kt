package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.data.cache.UnifiedFileCache
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for DeleteResourceUseCase.
 * Tests deletion logic and cleanup operations.
 */
class DeleteResourceUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var fileMetadataRepository: FileMetadataRepository
    private lateinit var networkCredentialsRepository: NetworkCredentialsRepository
    private lateinit var unifiedFileCache: UnifiedFileCache
    private lateinit var useCase: DeleteResourceUseCase

    @Before
    fun setup() {
        resourceRepository = mock()
        fileMetadataRepository = mock()
        networkCredentialsRepository = mock()
        unifiedFileCache = mock()
        useCase = DeleteResourceUseCase(
            resourceRepository = resourceRepository,
            fileMetadataRepository = fileMetadataRepository,
            credentialsRepository = networkCredentialsRepository,
            unifiedFileCache = unifiedFileCache
        )
    }

    @Test
    fun `invoke with existing resource deletes all related data`() = runTest {
        // Given
        val resourceId = 1L
        val testResource = Resource(
            id = resourceId,
            name = "Test Resource",
            path = "/test/path",
            type = ResourceType.LOCAL,
            sortMode = SortMode.DATE_DESC,
            displayMode = DisplayMode.GRID,
            workWithAllFiles = false
        )
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue(result is Result.Success)
        verify(fileMetadataRepository).deleteMetadataByResourcePath(testResource.path)
        verify(resourceRepository).deleteResource(testResource)
    }

    @Test
    fun `invoke with network resource also deletes credentials`() = runTest {
        // Given
        val resourceId = 1L
        val credentialsId = "cred_123"
        val testResource = Resource(
            id = resourceId,
            name = "Network Resource",
            path = "smb://192.168.1.100/share",
            type = ResourceType.SMB,
            sortMode = SortMode.DATE_DESC,
            displayMode = DisplayMode.GRID,
            workWithAllFiles = false,
            credentialsId = credentialsId
        )
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue(result is Result.Success)
        verify(networkCredentialsRepository).deleteCredentials(credentialsId)
        verify(resourceRepository).deleteResource(testResource)
    }

    @Test
    fun `invoke with non-existent resource returns error`() = runTest {
        // Given
        val resourceId = 999L
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(null)

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("not found"))
    }

    @Test
    fun `invoke when repository throws exception on delete returns error`() = runTest {
        // Given
        val resourceId = 1L
        val exception = RuntimeException("Database error")
        val testResource = Resource(
            id = resourceId,
            name = "Test Resource",
            path = "/test/path",
            type = ResourceType.LOCAL,
            sortMode = SortMode.DATE_DESC,
            displayMode = DisplayMode.GRID,
            workWithAllFiles = false
        )
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)
        whenever(resourceRepository.deleteResource(testResource)).thenThrow(exception)

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals(exception, result.throwable)
    }
}
