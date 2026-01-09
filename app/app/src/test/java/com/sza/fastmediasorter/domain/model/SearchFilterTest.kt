package com.sza.fastmediasorter.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SearchFilter data class.
 * Tests filter criteria and hasFilters property.
 */
class SearchFilterTest {

    @Test
    fun `default SearchFilter has no filters`() {
        // Given
        val filter = SearchFilter()

        // Then
        assertEquals("", filter.query)
        assertTrue("Expected empty fileTypes", filter.fileTypes.isEmpty())
        assertNull("Expected null minSize", filter.minSize)
        assertNull("Expected null maxSize", filter.maxSize)
        assertNull("Expected null dateFrom", filter.dateFrom)
        assertNull("Expected null dateTo", filter.dateTo)
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }

    @Test
    fun `EMPTY constant has no filters`() {
        // Given
        val filter = SearchFilter.EMPTY

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
        assertEquals("", filter.query)
    }

    @Test
    fun `hasFilters is true with non-blank query`() {
        // Given
        val filter = SearchFilter(query = "test")

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `hasFilters is false with blank query`() {
        // Given
        val filter = SearchFilter(query = "   ")

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }

    @Test
    fun `hasFilters is true with fileTypes`() {
        // Given
        val filter = SearchFilter(fileTypes = setOf(MediaType.IMAGE))

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `hasFilters is true with minSize`() {
        // Given
        val filter = SearchFilter(minSize = 1024L)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `hasFilters is true with maxSize`() {
        // Given
        val filter = SearchFilter(maxSize = 1048576L)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `hasFilters is true with dateFrom`() {
        // Given
        val filter = SearchFilter(dateFrom = System.currentTimeMillis())

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `hasFilters is true with dateTo`() {
        // Given
        val filter = SearchFilter(dateTo = System.currentTimeMillis())

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `SearchFilter with all criteria set`() {
        // Given
        val query = "vacation"
        val fileTypes = setOf(MediaType.IMAGE, MediaType.VIDEO)
        val minSize = 1024L
        val maxSize = 1048576L
        val dateFrom = 1000000L
        val dateTo = 2000000L

        // When
        val filter = SearchFilter(
            query = query,
            fileTypes = fileTypes,
            minSize = minSize,
            maxSize = maxSize,
            dateFrom = dateFrom,
            dateTo = dateTo
        )

        // Then
        assertEquals("Expected query to match", query, filter.query)
        assertEquals("Expected fileTypes to match", fileTypes, filter.fileTypes)
        assertEquals("Expected minSize to match", minSize, filter.minSize)
        assertEquals("Expected maxSize to match", maxSize, filter.maxSize)
        assertEquals("Expected dateFrom to match", dateFrom, filter.dateFrom)
        assertEquals("Expected dateTo to match", dateTo, filter.dateTo)
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
    }

    @Test
    fun `SearchFilter supports multiple MediaTypes`() {
        // Given
        val fileTypes = setOf(
            MediaType.IMAGE,
            MediaType.VIDEO,
            MediaType.AUDIO,
            MediaType.PDF
        )

        // When
        val filter = SearchFilter(fileTypes = fileTypes)

        // Then
        assertEquals("Expected 4 file types", 4, filter.fileTypes.size)
        assertTrue("Expected IMAGE", filter.fileTypes.contains(MediaType.IMAGE))
        assertTrue("Expected VIDEO", filter.fileTypes.contains(MediaType.VIDEO))
        assertTrue("Expected AUDIO", filter.fileTypes.contains(MediaType.AUDIO))
        assertTrue("Expected PDF", filter.fileTypes.contains(MediaType.PDF))
    }

    @Test
    fun `SearchFilter with only query has filters`() {
        // Given
        val filter = SearchFilter(query = "summer photos")

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("summer photos", filter.query)
        assertTrue("Expected empty fileTypes", filter.fileTypes.isEmpty())
    }

    @Test
    fun `SearchFilter with only fileTypes has filters`() {
        // Given
        val filter = SearchFilter(fileTypes = setOf(MediaType.VIDEO))

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("", filter.query)
        assertEquals("Expected 1 file type", 1, filter.fileTypes.size)
    }

    @Test
    fun `SearchFilter supports size range`() {
        // Given - files between 1MB and 10MB
        val filter = SearchFilter(
            minSize = 1048576L,
            maxSize = 10485760L
        )

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("Expected minSize 1MB", 1048576L, filter.minSize)
        assertEquals("Expected maxSize 10MB", 10485760L, filter.maxSize)
    }

    @Test
    fun `SearchFilter supports date range`() {
        // Given
        val dateFrom = 1609459200000L // 2021-01-01
        val dateTo = 1640995200000L   // 2022-01-01
        val filter = SearchFilter(
            dateFrom = dateFrom,
            dateTo = dateTo
        )

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("Expected dateFrom", dateFrom, filter.dateFrom)
        assertEquals("Expected dateTo", dateTo, filter.dateTo)
    }

    @Test
    fun `SearchFilter with only minSize has filters`() {
        // Given - files larger than 5MB
        val filter = SearchFilter(minSize = 5242880L)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("Expected minSize", 5242880L, filter.minSize)
        assertNull("Expected null maxSize", filter.maxSize)
    }

    @Test
    fun `SearchFilter with only maxSize has filters`() {
        // Given - files smaller than 1MB
        val filter = SearchFilter(maxSize = 1048576L)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertNull("Expected null minSize", filter.minSize)
        assertEquals("Expected maxSize", 1048576L, filter.maxSize)
    }

    @Test
    fun `SearchFilter with only dateFrom has filters`() {
        // Given - files newer than date
        val dateFrom = System.currentTimeMillis()
        val filter = SearchFilter(dateFrom = dateFrom)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertEquals("Expected dateFrom", dateFrom, filter.dateFrom)
        assertNull("Expected null dateTo", filter.dateTo)
    }

    @Test
    fun `SearchFilter with only dateTo has filters`() {
        // Given - files older than date
        val dateTo = System.currentTimeMillis()
        val filter = SearchFilter(dateTo = dateTo)

        // Then
        assertTrue("Expected hasFilters to be true", filter.hasFilters)
        assertNull("Expected null dateFrom", filter.dateFrom)
        assertEquals("Expected dateTo", dateTo, filter.dateTo)
    }

    @Test
    fun `SearchFilter copy function works correctly`() {
        // Given
        val original = SearchFilter(
            query = "test",
            fileTypes = setOf(MediaType.IMAGE)
        )

        // When
        val copied = original.copy(query = "modified")

        // Then
        assertEquals("modified", copied.query)
        assertEquals("Expected same fileTypes", original.fileTypes, copied.fileTypes)
        assertNotSame("Expected different instance", original, copied)
    }

    @Test
    fun `SearchFilter data class equality`() {
        // Given
        val filter1 = SearchFilter(query = "test", fileTypes = setOf(MediaType.IMAGE))
        val filter2 = SearchFilter(query = "test", fileTypes = setOf(MediaType.IMAGE))

        // Then
        assertEquals("Expected equality", filter1, filter2)
        assertEquals("Expected same hashCode", filter1.hashCode(), filter2.hashCode())
    }

    @Test
    fun `SearchFilter data class inequality`() {
        // Given
        val filter1 = SearchFilter(query = "test1")
        val filter2 = SearchFilter(query = "test2")

        // Then
        assertNotEquals("Expected inequality", filter1, filter2)
    }

    @Test
    fun `empty query string is not a filter`() {
        // Given
        val filter = SearchFilter(query = "")

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }

    @Test
    fun `whitespace-only query is not a filter`() {
        // Given
        val filter = SearchFilter(query = "    ")

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }

    @Test
    fun `tab-only query is not a filter`() {
        // Given
        val filter = SearchFilter(query = "\t\t")

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }

    @Test
    fun `newline-only query is not a filter`() {
        // Given
        val filter = SearchFilter(query = "\n\n")

        // Then
        assertFalse("Expected hasFilters to be false", filter.hasFilters)
    }
}
