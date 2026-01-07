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
 * Unit tests for UpdateResourceUseCase.
 * Tests update validation and repository interaction.
 */
class UpdateResourceUseCaseTest {

    private lateinit var resourceRepository: ResourceRepository
    private lateinit var useCase: UpdateResourceUseCase

    private val testResource = Resource(
        id = 1L,
        name = "Photos",
        path = "/storage/emulated/0/DCIM",
        type = ResourceType.LOCAL,
        sortMode = SortMode.DATE_DESC,
        displayMode = DisplayMode.GRID,
        workWithAllFiles = false
    )

    @Before
    fun setup() {
        resourceRepository = mock()
        useCase = UpdateResourceUseCase(resourceRepository)
    }

    @Test
    fun `invoke with valid resource returns success`() = runTest {
        // Given
        whenever(resourceRepository.getResourceById(testResource.id)).thenReturn(testResource)

        val updatedResource = testResource.copy(name = "Updated Photos")

        // When
        val result = useCase(updatedResource)

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).updateResource(updatedResource)
    }

    @Test
    fun `invoke with empty name returns error`() = runTest {
        // Given
        val invalidResource = testResource.copy(name = "")

        // When
        val result = useCase(invalidResource)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("name cannot be empty"))
        verify(resourceRepository, never()).updateResource(any())
    }

    @Test
    fun `invoke with blank name returns error`() = runTest {
        // Given
        val invalidResource = testResource.copy(name = "   ")

        // When
        val result = useCase(invalidResource)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
    }

    @Test
    fun `invoke with non-existent resource returns error`() = runTest {
        // Given
        whenever(resourceRepository.getResourceById(testResource.id)).thenReturn(null)

        // When
        val result = useCase(testResource)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
        assertTrue(result.message.contains("not found"))
        verify(resourceRepository, never()).updateResource(any())
    }

    @Test
    fun `invoke when repository throws exception returns error`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        whenever(resourceRepository.getResourceById(testResource.id)).thenReturn(testResource)
        whenever(resourceRepository.updateResource(any())).thenThrow(exception)

        // When
        val result = useCase(testResource)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.DATABASE_ERROR, (result as Result.Error).errorCode)
        assertEquals(exception, result.throwable)
    }

    @Test
    fun `updateSortMode with valid ID returns success`() = runTest {
        // Given
        val resourceId = 1L
        val newSortMode = SortMode.NAME_ASC
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)

        // When
        val result = useCase.updateSortMode(resourceId, newSortMode)

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).updateResource(any())
    }

    @Test
    fun `updateSortMode with non-existent resource returns error`() = runTest {
        // Given
        val resourceId = 999L
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(null)

        // When
        val result = useCase.updateSortMode(resourceId, SortMode.NAME_ASC)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
        verify(resourceRepository, never()).updateResource(any())
    }

    @Test
    fun `updateDisplayMode with valid ID returns success`() = runTest {
        // Given
        val resourceId = 1L
        val newDisplayMode = DisplayMode.LIST
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)

        // When
        val result = useCase.updateDisplayMode(resourceId, newDisplayMode)

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).updateResource(any())
    }

    @Test
    fun `toggleDestination with valid ID returns success`() = runTest {
        // Given
        val resourceId = 1L
        whenever(resourceRepository.getResourceById(resourceId)).thenReturn(testResource)

        // When
        val result = useCase.toggleDestination(resourceId)

        // Then
        assertTrue(result is Result.Success)
        verify(resourceRepository).updateResource(any())
    }
}
