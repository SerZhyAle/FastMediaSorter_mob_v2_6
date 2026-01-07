package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for GetResourcesUseCase.
 * Tests resource retrieval and error handling.
 */
class GetResourcesUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var useCase: GetResourcesUseCase

    private val testResource1 = Resource(
        id = 1L,
        name = "Photos",
        path = "/storage/emulated/0/DCIM",
        type = ResourceType.LOCAL,
        sortMode = SortMode.DATE_DESC,
        displayMode = DisplayMode.GRID,
        workWithAllFiles = false
    )

    private val testResource2 = Resource(
        id = 2L,
        name = "Network Share",
        path = "smb://192.168.1.100/share",
        type = ResourceType.SMB,
        sortMode = SortMode.NAME_ASC,
        displayMode = DisplayMode.LIST,
        workWithAllFiles = true,
        credentialsId = "cred_123"
    )

    @Before
    fun setup() {
        resourceRepository = mock()
        useCase = GetResourcesUseCase(resourceRepository)
    }

    @Test
    fun `invoke returns flow with success result containing resources`() = runTest {
        // Given
        val resources = listOf(testResource1, testResource2)
        whenever(resourceRepository.getAllResourcesFlow()).thenReturn(flowOf(resources))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)
        assertEquals(testResource1, result.data[0])
        assertEquals(testResource2, result.data[1])
    }

    @Test
    fun `invoke returns flow with empty list when no resources exist`() = runTest {
        // Given
        whenever(resourceRepository.getAllResourcesFlow()).thenReturn(flowOf(emptyList()))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `invoke returns error when flow throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        whenever(resourceRepository.getAllResourcesFlow()).thenReturn(
            kotlinx.coroutines.flow.flow {
                throw exception
            }
        )

        // When
        val result = useCase().first()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals("Database error", result.message)
    }

    @Test
    fun `getAll returns success with resources`() = runTest {
        // Given
        val resources = listOf(testResource1, testResource2)
        whenever(resourceRepository.getAllResources()).thenReturn(resources)

        // When
        val result = useCase.getAll()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)
    }

    @Test
    fun `getAll returns success with empty list when no resources`() = runTest {
        // Given
        whenever(resourceRepository.getAllResources()).thenReturn(emptyList())

        // When
        val result = useCase.getAll()

        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `getAll returns error when repository throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        whenever(resourceRepository.getAllResources()).thenThrow(exception)

        // When
        val result = useCase.getAll()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals(exception, result.throwable)
    }

    @Test
    fun `getById returns success when resource exists`() = runTest {
        // Given
        val resourceId = 1L
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource1)

        // When
        val result = useCase.getById(resourceId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(testResource1, (result as Result.Success).data)
    }

    @Test
    fun `getById returns error when resource not found`() = runTest {
        // Given
        val resourceId = 999L
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(null)

        // When
        val result = useCase.getById(resourceId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("not found"))
    }

    @Test
    fun `getById returns error when repository throws exception`() = runTest {
        // Given
        val resourceId = 1L
        val exception = RuntimeException("Database error")
        whenever(resourceRepository.getResourceById(resourceId)).thenThrow(exception)

        // When
        val result = useCase.getById(resourceId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals(exception, result.throwable)
    }
}
