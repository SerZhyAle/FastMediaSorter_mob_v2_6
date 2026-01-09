package com.sza.fastmediasorter.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for PlaybackPositionRepository.
 * Tests saving, retrieving, and clearing playback positions stored in SharedPreferences.
 */
class PlaybackPositionRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: PlaybackPositionRepository
    
    private val savedData = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        mockContext = mock()
        mockPrefs = mock()
        mockEditor = mock()

        // Setup SharedPreferences mock to use in-memory map
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        
        // Setup editor to capture saved values
        whenever(mockEditor.putLong(any(), any())).thenAnswer {
            val key = it.getArgument<String>(0)
            val value = it.getArgument<Long>(1)
            savedData[key] = value
            mockEditor
        }
        whenever(mockEditor.remove(any())).thenAnswer {
            val key = it.getArgument<String>(0)
            savedData.remove(key)
            mockEditor
        }
        whenever(mockEditor.clear()).thenAnswer {
            savedData.clear()
            mockEditor
        }
        whenever(mockEditor.apply()).then { }
        
        // Setup prefs.getLong to retrieve from savedData
        whenever(mockPrefs.getLong(any(), any())).thenAnswer {
            val key = it.getArgument<String>(0)
            val default = it.getArgument<Long>(1)
            savedData[key] as? Long ?: default
        }
        
        // Setup prefs.all to return savedData keys
        whenever(mockPrefs.all).thenAnswer {
            savedData.mapValues { it.value }
        }

        repository = PlaybackPositionRepository(mockContext)
    }

    @Test
    fun `savePosition stores position duration and timestamp`() {
        // Given
        val filePath = "/storage/music/song.mp3"
        val position = 45000L
        val duration = 180000L

        // When
        repository.savePosition(filePath, position, duration)

        // Then
        val key = filePath.hashCode().toString()
        verify(mockEditor).putLong(eq("position_$key"), eq(position))
        verify(mockEditor).putLong(eq("duration_$key"), eq(duration))
        verify(mockEditor).putLong(eq("timestamp_$key"), any())
        verify(mockEditor).apply()
    }

    @Test
    fun `getPosition returns saved position`() {
        // Given
        val filePath = "/storage/music/song.mp3"
        val position = 45000L
        val duration = 180000L
        repository.savePosition(filePath, position, duration)

        // When
        val result = repository.getPosition(filePath)

        // Then
        assertNotNull("Expected non-null position", result)
        assertEquals("Position mismatch", position, result!!.position)
        assertEquals("Duration mismatch", duration, result.duration)
        assertTrue("Timestamp should be positive", result.timestamp > 0)
    }

    @Test
    fun `getPosition returns null for unsaved file`() {
        // Given
        val filePath = "/storage/music/nonexistent.mp3"

        // When
        val result = repository.getPosition(filePath)

        // Then
        assertNull("Expected null for unsaved file", result)
    }

    @Test
    fun `clearPosition removes all position data for file`() {
        // Given
        val filePath = "/storage/music/song.mp3"
        repository.savePosition(filePath, 45000L, 180000L)

        // When
        repository.clearPosition(filePath)

        // Then
        val key = filePath.hashCode().toString()
        verify(mockEditor).remove("position_$key")
        verify(mockEditor).remove("duration_$key")
        verify(mockEditor).remove("timestamp_$key")
        verify(mockEditor, times(2)).apply() // once for save, once for clear
        
        // Verify position is gone
        val result = repository.getPosition(filePath)
        assertNull("Expected null after clearing", result)
    }

    @Test
    fun `clearAllPositions removes all saved data`() {
        // Given
        repository.savePosition("/file1.mp3", 1000L, 5000L)
        repository.savePosition("/file2.mp3", 2000L, 6000L)

        // When
        repository.clearAllPositions()

        // Then
        verify(mockEditor).clear()
        verify(mockEditor, times(3)).apply() // twice for saves, once for clear
        assertTrue("Expected empty savedData", savedData.isEmpty())
    }

    @Test
    fun `getAllSavedFiles returns list of files with saved positions`() {
        // Given
        val file1 = "/storage/music/song1.mp3"
        val file2 = "/storage/music/song2.mp3"
        repository.savePosition(file1, 1000L, 5000L)
        repository.savePosition(file2, 2000L, 6000L)

        // When
        val result = repository.getAllSavedFiles()

        // Then
        assertEquals("Expected 2 saved files", 2, result.size)
        // Note: keys are hashed, so we check by count not exact values
        assertTrue("Expected non-empty list", result.isNotEmpty())
    }

    @Test
    fun `getAllSavedFiles returns empty list when no positions saved`() {
        // When
        val result = repository.getAllSavedFiles()

        // Then
        assertTrue("Expected empty list", result.isEmpty())
    }

    @Test
    fun `PlaybackPosition shouldResume returns true for mid-playback position`() {
        // Given
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 50000L,
            duration = 100000L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.shouldResume()

        // Then
        assertTrue("Expected shouldResume true for 50% progress", result)
    }

    @Test
    fun `PlaybackPosition shouldResume returns false at beginning`() {
        // Given (0.5% progress)
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 500L,
            duration = 100000L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.shouldResume()

        // Then
        assertFalse("Expected shouldResume false at beginning", result)
    }

    @Test
    fun `PlaybackPosition shouldResume returns false near end`() {
        // Given (96% progress)
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 96000L,
            duration = 100000L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.shouldResume()

        // Then
        assertFalse("Expected shouldResume false near end", result)
    }

    @Test
    fun `PlaybackPosition shouldResume returns false for zero duration`() {
        // Given
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 5000L,
            duration = 0L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.shouldResume()

        // Then
        assertFalse("Expected shouldResume false for zero duration", result)
    }

    @Test
    fun `PlaybackPosition getProgressPercentage returns correct value`() {
        // Given
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 45000L,
            duration = 180000L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.getProgressPercentage()

        // Then
        assertEquals("Expected 25% progress", 25f, result, 0.1f)
    }

    @Test
    fun `PlaybackPosition getProgressPercentage returns zero for zero duration`() {
        // Given
        val position = PlaybackPositionRepository.PlaybackPosition(
            position = 5000L,
            duration = 0L,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = position.getProgressPercentage()

        // Then
        assertEquals("Expected 0% for zero duration", 0f, result, 0.1f)
    }

    @Test
    fun `savePosition handles special characters in file path`() {
        // Given
        val filePath = "/storage/music/Song with spaces & special (chars).mp3"
        val position = 10000L
        val duration = 60000L

        // When
        repository.savePosition(filePath, position, duration)
        val result = repository.getPosition(filePath)

        // Then
        assertNotNull("Expected position to be saved", result)
        assertEquals("Position mismatch", position, result!!.position)
    }

    @Test
    fun `savePosition overwrites previous position for same file`() {
        // Given
        val filePath = "/storage/music/song.mp3"
        repository.savePosition(filePath, 10000L, 60000L)

        // When - save new position
        val newPosition = 20000L
        val newDuration = 60000L
        repository.savePosition(filePath, newPosition, newDuration)
        val result = repository.getPosition(filePath)

        // Then
        assertNotNull("Expected position to be saved", result)
        assertEquals("Expected updated position", newPosition, result!!.position)
        assertEquals("Expected same duration", newDuration, result.duration)
    }
}
