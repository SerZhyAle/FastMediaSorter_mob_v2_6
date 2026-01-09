package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetMediaFilesUseCase.
 * Tests the current implementation which returns empty list as a placeholder.
 */
class GetMediaFilesUseCaseTest {

    private lateinit var useCase: GetMediaFilesUseCase

    @Before
    fun setup() {
        useCase = GetMediaFilesUseCase()
    }

    @Test
    fun `invoke with valid resourceId returns success with empty list`() = runTest {
        // Given
        val resourceId = 1L

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue("Expected Success result", result is Result.Success)
        val successResult = result as Result.Success
        assertTrue("Expected empty list", successResult.data.isEmpty())
    }

    @Test
    fun `invoke with different resourceId returns success with empty list`() = runTest {
        // Given
        val resourceId = 999L

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue("Expected Success result", result is Result.Success)
        val successResult = result as Result.Success
        assertEquals("Expected empty list", emptyList<Any>(), successResult.data)
    }

    @Test
    fun `invoke with zero resourceId returns success with empty list`() = runTest {
        // Given
        val resourceId = 0L

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue("Expected Success result", result is Result.Success)
        val successResult = result as Result.Success
        assertTrue("Expected empty list", successResult.data.isEmpty())
    }

    @Test
    fun `invoke with negative resourceId returns success with empty list`() = runTest {
        // Given
        val resourceId = -1L

        // When
        val result = useCase(resourceId)

        // Then
        assertTrue("Expected Success result", result is Result.Success)
        val successResult = result as Result.Success
        assertTrue("Expected empty list", successResult.data.isEmpty())
    }
}
