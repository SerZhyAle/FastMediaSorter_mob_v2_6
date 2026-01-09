package com.sza.fastmediasorter.data.operation

import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Date

/**
 * Unit tests for LocalOperationStrategy.
 * Uses temporary file system for isolated testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalOperationStrategyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var strategy: LocalOperationStrategy
    private lateinit var testFile: File
    private lateinit var testMediaFile: MediaFile

    @Before
    fun setup() {
        strategy = LocalOperationStrategy()
        
        // Create a test file
        testFile = tempFolder.newFile("test_image.jpg")
        testFile.writeText("Test content")
        
        testMediaFile = MediaFile(
            path = testFile.absolutePath,
            name = testFile.name,
            size = testFile.length(),
            date = Date(testFile.lastModified()),
            type = MediaType.IMAGE,
            isDirectory = false
        )
    }

    @After
    fun cleanup() {
        // TemporaryFolder rule handles cleanup automatically
    }

    // ==================== COPY TESTS ====================

    @Test
    fun `copy file successfully`() = runTest {
        val destFile = File(tempFolder.root, "copy_test.jpg")
        
        val result = strategy.copy(testMediaFile, destFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(destFile.absolutePath, (result as Result.Success).data)
        assertTrue(destFile.exists())
        assertEquals("Test content", destFile.readText())
        assertTrue(testFile.exists()) // Original should still exist
    }

    @Test
    fun `copy file with progress callback`() = runTest {
        val destFile = File(tempFolder.root, "copy_with_progress.jpg")
        val progressValues = mutableListOf<Float>()
        
        val result = strategy.copy(testMediaFile, destFile.absolutePath) { progress ->
            progressValues.add(progress)
        }
        
        assertTrue(result is Result.Success)
        assertTrue(progressValues.isNotEmpty())
        assertTrue(progressValues.last() <= 1f)
    }

    @Test
    fun `copy fails when source does not exist`() = runTest {
        val nonExistentFile = MediaFile(
            path = "/non/existent/file.jpg",
            name = "file.jpg",
            size = 0,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        val destFile = File(tempFolder.root, "dest.jpg")
        
        val result = strategy.copy(nonExistentFile, destFile.absolutePath)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
        assertFalse(destFile.exists())
    }

    @Test
    fun `copy fails when destination already exists`() = runTest {
        val existingDest = tempFolder.newFile("existing.jpg")
        
        val result = strategy.copy(testMediaFile, existingDest.absolutePath)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_EXISTS, (result as Result.Error).errorCode)
    }

    @Test
    fun `copy creates destination directory if needed`() = runTest {
        val newDir = File(tempFolder.root, "new_dir")
        val destFile = File(newDir, "copied.jpg")
        
        assertFalse(newDir.exists())
        
        val result = strategy.copy(testMediaFile, destFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertTrue(newDir.exists())
        assertTrue(destFile.exists())
    }

    // ==================== MOVE TESTS ====================

    @Test
    fun `move file successfully using rename`() = runTest {
        val destFile = File(tempFolder.root, "moved.jpg")
        val originalPath = testFile.absolutePath
        
        val result = strategy.move(testMediaFile, destFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(destFile.absolutePath, (result as Result.Success).data)
        assertTrue(destFile.exists())
        assertFalse(File(originalPath).exists()) // Original should be gone
    }

    @Test
    fun `move file with progress callback`() = runTest {
        val destFile = File(tempFolder.root, "moved_with_progress.jpg")
        val progressValues = mutableListOf<Float>()
        
        val result = strategy.move(testMediaFile, destFile.absolutePath) { progress ->
            progressValues.add(progress)
        }
        
        assertTrue(result is Result.Success)
        // Progress should be called at least once (either from rename or copy)
    }

    @Test
    fun `move fails when source does not exist`() = runTest {
        val nonExistentFile = MediaFile(
            path = "/non/existent/file.jpg",
            name = "file.jpg",
            size = 0,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        val destFile = File(tempFolder.root, "dest.jpg")
        
        val result = strategy.move(nonExistentFile, destFile.absolutePath)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
    }

    @Test
    fun `move fails when destination already exists`() = runTest {
        val existingDest = tempFolder.newFile("existing.jpg")
        
        val result = strategy.move(testMediaFile, existingDest.absolutePath)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_EXISTS, (result as Result.Error).errorCode)
        assertTrue(testFile.exists()) // Original should still exist
    }

    @Test
    fun `move creates destination directory if needed`() = runTest {
        val newDir = File(tempFolder.root, "new_dir")
        val destFile = File(newDir, "moved.jpg")
        
        assertFalse(newDir.exists())
        
        val result = strategy.move(testMediaFile, destFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertTrue(newDir.exists())
        assertTrue(destFile.exists())
    }

    // ==================== DELETE TESTS ====================

    @Test
    fun `delete file permanently`() = runTest {
        val result = strategy.delete(testMediaFile, permanent = true)
        
        assertTrue(result is Result.Success)
        assertFalse(testFile.exists())
    }

    @Test
    fun `delete file to trash`() = runTest {
        val result = strategy.delete(testMediaFile, permanent = false)
        
        assertTrue(result is Result.Success)
        assertFalse(testFile.exists())
        
        // Check trash folder was created
        val trashDir = File(testFile.parentFile, ".trash")
        assertTrue(trashDir.exists())
        assertTrue(trashDir.isDirectory)
        
        // Check file exists in trash (with timestamp prefix)
        val trashedFiles = trashDir.listFiles()
        assertNotNull(trashedFiles)
        assertTrue(trashedFiles!!.any { it.name.endsWith("_${testFile.name}") })
    }

    @Test
    fun `delete fails when file does not exist`() = runTest {
        val nonExistentFile = MediaFile(
            path = "/non/existent/file.jpg",
            name = "file.jpg",
            size = 0,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        
        val result = strategy.delete(nonExistentFile, permanent = true)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
    }

    // ==================== RENAME TESTS ====================

    @Test
    fun `rename file successfully`() = runTest {
        val newName = "renamed_image.jpg"
        
        val result = strategy.rename(testMediaFile, newName)
        
        assertTrue(result is Result.Success)
        val newPath = (result as Result.Success).data
        assertTrue(File(newPath).exists())
        assertEquals(newName, File(newPath).name)
        assertFalse(testFile.exists()) // Old file should not exist
    }

    @Test
    fun `rename fails when source does not exist`() = runTest {
        val nonExistentFile = MediaFile(
            path = "/non/existent/file.jpg",
            name = "file.jpg",
            size = 0,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        
        val result = strategy.rename(nonExistentFile, "new_name.jpg")
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
    }

    @Test
    fun `rename fails with blank name`() = runTest {
        val result = strategy.rename(testMediaFile, "")
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
        assertTrue(testFile.exists()) // Original should still exist
    }

    @Test
    fun `rename fails with name containing path separator`() = runTest {
        val result = strategy.rename(testMediaFile, "invalid/name.jpg")
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_INPUT, (result as Result.Error).errorCode)
    }

    @Test
    fun `rename fails when destination name already exists`() = runTest {
        val existingFile = tempFolder.newFile("existing.jpg")
        
        val result = strategy.rename(testMediaFile, "existing.jpg")
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_EXISTS, (result as Result.Error).errorCode)
        assertTrue(testFile.exists()) // Original should still exist
    }

    // ==================== EXISTS TESTS ====================

    @Test
    fun `exists returns true for existing file`() = runTest {
        val exists = strategy.exists(testFile.absolutePath)
        
        assertTrue(exists)
    }

    @Test
    fun `exists returns false for non-existent file`() = runTest {
        val exists = strategy.exists("/non/existent/file.jpg")
        
        assertFalse(exists)
    }

    @Test
    fun `exists returns true for directory`() = runTest {
        val dir = tempFolder.newFolder("test_dir")
        
        val exists = strategy.exists(dir.absolutePath)
        
        assertTrue(exists)
    }

    // ==================== CREATE DIRECTORY TESTS ====================

    @Test
    fun `createDirectory creates new directory`() = runTest {
        val newDir = File(tempFolder.root, "new_directory")
        
        assertFalse(newDir.exists())
        
        val result = strategy.createDirectory(newDir.absolutePath)
        
        assertTrue(result is Result.Success)
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory)
    }

    @Test
    fun `createDirectory creates nested directories`() = runTest {
        val nestedDir = File(tempFolder.root, "parent/child/grandchild")
        
        assertFalse(nestedDir.exists())
        
        val result = strategy.createDirectory(nestedDir.absolutePath)
        
        assertTrue(result is Result.Success)
        assertTrue(nestedDir.exists())
        assertTrue(nestedDir.isDirectory)
    }

    @Test
    fun `createDirectory succeeds when directory already exists`() = runTest {
        val existingDir = tempFolder.newFolder("existing")
        
        val result = strategy.createDirectory(existingDir.absolutePath)
        
        assertTrue(result is Result.Success)
    }

    @Test
    fun `createDirectory fails when path exists but is not directory`() = runTest {
        val result = strategy.createDirectory(testFile.absolutePath)
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_EXISTS, (result as Result.Error).errorCode)
    }

    // ==================== GET FILE INFO TESTS ====================

    @Test
    fun `getFileInfo returns correct metadata for file`() = runTest {
        val result = strategy.getFileInfo(testFile.absolutePath)
        
        assertTrue(result is Result.Success)
        val fileInfo = (result as Result.Success).data
        assertEquals(testFile.absolutePath, fileInfo.path)
        assertEquals(testFile.name, fileInfo.name)
        assertEquals(testFile.length(), fileInfo.size)
        assertEquals(MediaType.IMAGE, fileInfo.type)
        assertFalse(fileInfo.isDirectory)
    }

    @Test
    fun `getFileInfo returns correct metadata for directory`() = runTest {
        val dir = tempFolder.newFolder("test_directory")
        
        val result = strategy.getFileInfo(dir.absolutePath)
        
        assertTrue(result is Result.Success)
        val fileInfo = (result as Result.Success).data
        assertEquals(dir.absolutePath, fileInfo.path)
        assertEquals(dir.name, fileInfo.name)
        assertTrue(fileInfo.isDirectory)
    }

    @Test
    fun `getFileInfo fails when file does not exist`() = runTest {
        val result = strategy.getFileInfo("/non/existent/file.jpg")
        
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_NOT_FOUND, (result as Result.Error).errorCode)
    }

    @Test
    fun `getFileInfo detects MediaType VIDEO correctly`() = runTest {
        val videoFile = tempFolder.newFile("video.mp4")
        
        val result = strategy.getFileInfo(videoFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.VIDEO, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType AUDIO correctly`() = runTest {
        val audioFile = tempFolder.newFile("audio.mp3")
        
        val result = strategy.getFileInfo(audioFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.AUDIO, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType PDF correctly`() = runTest {
        val pdfFile = tempFolder.newFile("document.pdf")
        
        val result = strategy.getFileInfo(pdfFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.PDF, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType EPUB correctly`() = runTest {
        val epubFile = tempFolder.newFile("book.epub")
        
        val result = strategy.getFileInfo(epubFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.EPUB, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType TXT correctly`() = runTest {
        val txtFile = tempFolder.newFile("notes.txt")
        
        val result = strategy.getFileInfo(txtFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.TXT, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType GIF correctly`() = runTest {
        val gifFile = tempFolder.newFile("animation.gif")
        
        val result = strategy.getFileInfo(gifFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.GIF, (result as Result.Success).data.type)
    }

    @Test
    fun `getFileInfo detects MediaType OTHER for unknown extension`() = runTest {
        val unknownFile = tempFolder.newFile("file.xyz")
        
        val result = strategy.getFileInfo(unknownFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(MediaType.OTHER, (result as Result.Success).data.type)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `copy handles large files correctly`() = runTest {
        // Create a larger file (1MB)
        val largeContent = ByteArray(1024 * 1024) { it.toByte() }
        testFile.writeBytes(largeContent)
        testMediaFile = testMediaFile.copy(size = testFile.length())
        
        val destFile = File(tempFolder.root, "large_copy.jpg")
        val result = strategy.copy(testMediaFile, destFile.absolutePath)
        
        assertTrue(result is Result.Success)
        assertEquals(largeContent.size.toLong(), destFile.length())
    }

    @Test
    fun `multiple sequential operations work correctly`() = runTest {
        // Copy
        val copy1 = File(tempFolder.root, "copy1.jpg")
        val copyResult = strategy.copy(testMediaFile, copy1.absolutePath)
        assertTrue(copyResult is Result.Success)
        
        // Move the copy
        val copy1MediaFile = MediaFile(
            path = copy1.absolutePath,
            name = copy1.name,
            size = copy1.length(),
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        val move1 = File(tempFolder.root, "moved1.jpg")
        val moveResult = strategy.move(copy1MediaFile, move1.absolutePath)
        assertTrue(moveResult is Result.Success)
        
        // Rename the moved file
        val move1MediaFile = MediaFile(
            path = move1.absolutePath,
            name = move1.name,
            size = move1.length(),
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )
        val renameResult = strategy.rename(move1MediaFile, "final.jpg")
        assertTrue(renameResult is Result.Success)
        
        // Verify final state
        val finalFile = File(tempFolder.root, "final.jpg")
        assertTrue(finalFile.exists())
        assertFalse(copy1.exists())
        assertFalse(move1.exists())
    }
}
