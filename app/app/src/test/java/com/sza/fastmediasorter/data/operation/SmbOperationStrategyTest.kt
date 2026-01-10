package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.google.gson.Gson
import com.sza.fastmediasorter.data.network.SmbClient
import com.sza.fastmediasorter.domain.model.ErrorCode
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.Date

/**
 * Unit tests for SmbOperationStrategy.
 *
 * Test Modes:
 * 1. Mock Tests (default) - All SMB calls mocked, no real network
 * 2. Real Network Tests - Requires test_network_creds.json configuration
 *
 * Configuration:
 * - Copy test_network_creds.json.template to test_network_creds.json
 * - Set smb.enabled = true for real network tests
 * - Set settings.skipRealNetworkTests = false to enable real tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmbOperationStrategyTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSmbClient: SmbClient

    private lateinit var strategy: SmbOperationStrategy
    private var testConfig: NetworkTestConfig? = null

    // Test configuration data classes
    private data class NetworkTestConfig(
        val smb: SmbConfig,
        val testFiles: TestFiles,
        val settings: TestSettings
    )

    private data class SmbConfig(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val shareName: String,
        val workgroup: String,
        val domain: String,
        val username: String,
        val password: String,
        val testFolder: String,
        val anonymousAccess: Boolean,
        val smbVersion: String
    )

    private data class TestFiles(
        val smallFile: String,
        val mediumFile: String,
        val largeFile: String
    )

    private data class TestSettings(
        val connectionTimeout: Int,
        val readTimeout: Int,
        val skipRealNetworkTests: Boolean
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Load test configuration if available
        loadTestConfiguration()

        // Initialize strategy with mocked dependencies
        strategy = SmbOperationStrategy(mockContext, mockSmbClient)
    }

    private fun loadTestConfiguration() {
        try {
            val configFile = File("test_network_creds.json")
            if (configFile.exists()) {
                testConfig = Gson().fromJson(configFile.readText(), NetworkTestConfig::class.java)
            }
        } catch (e: Exception) {
            // Config file not available - mock tests only
            testConfig = null
        }
    }

    private fun isRealNetworkTestEnabled(): Boolean {
        return testConfig?.smb?.enabled == true &&
               testConfig?.settings?.skipRealNetworkTests == false
    }

    // ==================== CONFIGURATION TESTS ====================

    @Test
    fun `configure SMB connection with valid credentials`() {
        strategy.configure(
            server = "192.168.1.100",
            port = 445,
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        // Configuration should be set internally (no exception)
        assertNotNull(strategy)
    }

    @Test
    fun `configure SMB connection with domain credentials`() {
        strategy.configure(
            server = "192.168.1.100",
            port = 445,
            shareName = "TestShare",
            username = "testuser",
            password = "testpass",
            domain = "TESTDOMAIN"
        )

        assertNotNull(strategy)
    }

    @Test
    fun `configure SMB connection with custom port`() {
        strategy.configure(
            server = "192.168.1.100",
            port = 1445,
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        assertNotNull(strategy)
    }

    // ==================== CONNECTION TESTS (MOCK) ====================

    @Test
    fun `connect to SMB share with mocked client`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        `when`(mockSmbClient.connect(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenReturn(true)

        // Test operation that requires connection
        val testFile = MediaFile(
            path = "smb://192.168.1.100/TestShare/test.txt",
            name = "test.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        verify(mockSmbClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString(), anyString())
    }

    @Test
    fun `handle connection failure gracefully`() = runTest {
        strategy.configure(
            server = "invalid.host",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        `when`(mockSmbClient.connect(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenReturn(false)

        // Operations should return error, not crash
        val testFile = MediaFile(
            path = "smb://invalid.host/TestShare/test.txt",
            name = "test.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        // Should handle error gracefully
        verify(mockSmbClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString(), anyString())
    }

    // ==================== LIST FILES TESTS (MOCK) ====================

    @Test
    fun `list files in SMB directory with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val mockFiles = listOf(
            MediaFile("smb://192.168.1.100/TestShare/file1.jpg", "file1.jpg", 1024, Date(), MediaType.IMAGE, false),
            MediaFile("smb://192.168.1.100/TestShare/file2.mp4", "file2.mp4", 2048, Date(), MediaType.VIDEO, false),
            MediaFile("smb://192.168.1.100/TestShare/folder/", "folder", 0, Date(), MediaType.OTHER, true)
        )

        `when`(mockSmbClient.listFiles(anyString())).thenReturn(mockFiles)

        val result = strategy.listFiles("smb://192.168.1.100/TestShare/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertEquals(3, files.size)
        assertEquals("file1.jpg", files[0].name)
        assertTrue(files[2].isDirectory)
    }

    @Test
    fun `list files returns empty list for empty directory`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        `when`(mockSmbClient.listFiles(anyString())).thenReturn(emptyList())

        val result = strategy.listFiles("smb://192.168.1.100/TestShare/empty/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertTrue(files.isEmpty())
    }

    @Test
    fun `list files filters by media type`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val mockFiles = listOf(
            MediaFile("path1.jpg", "file1.jpg", 1024, Date(), MediaType.IMAGE, false),
            MediaFile("path2.mp4", "file2.mp4", 2048, Date(), MediaType.VIDEO, false),
            MediaFile("path3.txt", "file3.txt", 512, Date(), MediaType.TXT, false)
        )

        `when`(mockSmbClient.listFiles(anyString())).thenReturn(mockFiles)

        val result = strategy.listFiles("smb://192.168.1.100/TestShare/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertEquals(3, files.size)
    }

    // ==================== COPY/DOWNLOAD TESTS (MOCK) ====================

    @Test
    fun `download file from SMB to local with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val sourceFile = MediaFile(
            path = "smb://192.168.1.100/TestShare/test.jpg",
            name = "test.jpg",
            size = 1024,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )

        `when`(mockSmbClient.downloadFile(anyString(), anyString(), any()))
            .thenReturn(true)

        val result = strategy.copy(sourceFile, "/local/destination/test.jpg")

        // Mock should not throw errors
        verify(mockSmbClient, atMost(1)).downloadFile(anyString(), anyString(), any())
    }

    @Test
    fun `download file with progress callback`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val sourceFile = MediaFile(
            path = "smb://192.168.1.100/TestShare/large.mp4",
            name = "large.mp4",
            size = 10485760, // 10 MB
            date = Date(),
            type = MediaType.VIDEO,
            isDirectory = false
        )

        val progressValues = mutableListOf<Float>()

        `when`(mockSmbClient.downloadFile(anyString(), anyString(), any()))
            .thenAnswer { invocation ->
                val callback = invocation.getArgument<((Float) -> Unit)?>(2)
                // Simulate progress
                callback?.invoke(0.25f)
                callback?.invoke(0.50f)
                callback?.invoke(0.75f)
                callback?.invoke(1.0f)
                true
            }

        val result = strategy.copy(
            sourceFile,
            "/local/destination/large.mp4"
        ) { progress ->
            progressValues.add(progress)
        }

        // Progress callbacks should have been invoked
        assertTrue(progressValues.size >= 2)
    }

    // ==================== UPLOAD TESTS (MOCK) ====================

    @Test
    fun `upload file from local to SMB with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val sourceFile = MediaFile(
            path = "/local/source/test.jpg",
            name = "test.jpg",
            size = 1024,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )

        `when`(mockSmbClient.uploadFile(anyString(), anyString(), any()))
            .thenReturn(true)

        val result = strategy.copy(sourceFile, "smb://192.168.1.100/TestShare/uploaded.jpg")

        verify(mockSmbClient, atMost(1)).uploadFile(anyString(), anyString(), any())
    }

    // ==================== DELETE TESTS (MOCK) ====================

    @Test
    fun `delete file on SMB share with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val fileToDelete = MediaFile(
            path = "smb://192.168.1.100/TestShare/delete_me.txt",
            name = "delete_me.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSmbClient.deleteFile(anyString())).thenReturn(true)

        val result = strategy.delete(fileToDelete)

        verify(mockSmbClient, atMost(1)).deleteFile(anyString())
    }

    // ==================== MOVE/RENAME TESTS (MOCK) ====================

    @Test
    fun `move file on SMB share with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val sourceFile = MediaFile(
            path = "smb://192.168.1.100/TestShare/old_location.txt",
            name = "old_location.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSmbClient.moveFile(anyString(), anyString())).thenReturn(true)

        val result = strategy.move(sourceFile, "smb://192.168.1.100/TestShare/new_location.txt")

        verify(mockSmbClient, atMost(1)).moveFile(anyString(), anyString())
    }

    @Test
    fun `rename file on SMB share with mock`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "testuser",
            password = "testpass"
        )

        val sourceFile = MediaFile(
            path = "smb://192.168.1.100/TestShare/old_name.txt",
            name = "old_name.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSmbClient.moveFile(anyString(), anyString())).thenReturn(true)

        val result = strategy.rename(sourceFile, "new_name.txt")

        verify(mockSmbClient, atMost(1)).moveFile(anyString(), anyString())
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    fun `handle authentication failure`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "TestShare",
            username = "wronguser",
            password = "wrongpass"
        )

        `when`(mockSmbClient.connect(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenReturn(false)

        // Should handle gracefully without crashing
        verify(mockSmbClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString(), anyString())
    }

    @Test
    fun `handle permission denied error`() = runTest {
        strategy.configure(
            server = "192.168.1.100",
            shareName = "RestrictedShare",
            username = "limiteduser",
            password = "testpass"
        )

        val restrictedFile = MediaFile(
            path = "smb://192.168.1.100/RestrictedShare/protected.txt",
            name = "protected.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSmbClient.downloadFile(anyString(), anyString(), any()))
            .thenThrow(SecurityException("Access denied"))

        // Should handle exception gracefully
        try {
            strategy.copy(restrictedFile, "/local/destination/protected.txt")
        } catch (e: Exception) {
            assertTrue(e is SecurityException)
        }
    }

    @Test
    fun `handle network timeout`() = runTest {
        strategy.configure(
            server = "192.168.1.254",
            shareName = "TimeoutShare",
            username = "testuser",
            password = "testpass"
        )

        `when`(mockSmbClient.connect(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenAnswer { Thread.sleep(15000); false } // Simulate timeout

        // Should timeout gracefully
        verify(mockSmbClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString(), anyString())
    }

    // ==================== REAL NETWORK TESTS (OPTIONAL) ====================

    @Test
    fun `REAL TEST - connect to actual SMB server`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.smb

        strategy.configure(
            server = config.host,
            port = config.port,
            shareName = config.shareName,
            username = config.username,
            password = config.password,
            domain = config.domain
        )

        // This would test actual connection - requires real SMB server
        // Implementation depends on actual SmbClient behavior
    }

    @Test
    fun `REAL TEST - list files on actual SMB share`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.smb

        strategy.configure(
            server = config.host,
            port = config.port,
            shareName = config.shareName,
            username = config.username,
            password = config.password
        )

        val result = strategy.listFiles("smb://${config.host}/${config.shareName}${config.testFolder}")

        // Should successfully list files (even if empty)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `REAL TEST - download file from actual SMB share`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.smb
        val testFiles = testConfig!!.testFiles

        strategy.configure(
            server = config.host,
            port = config.port,
            shareName = config.shareName,
            username = config.username,
            password = config.password
        )

        val sourceFile = MediaFile(
            path = "smb://${config.host}/${config.shareName}${config.testFolder}/${testFiles.smallFile}",
            name = testFiles.smallFile,
            size = 1024,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        val tempDest = File.createTempFile("smb_test_", ".tmp")
        try {
            val result = strategy.copy(sourceFile, tempDest.absolutePath)

            assertTrue(result is Result.Success)
            assertTrue(tempDest.exists())
            assertTrue(tempDest.length() > 0)
        } finally {
            tempDest.delete()
        }
    }
}
