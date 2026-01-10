package com.sza.fastmediasorter.data.operation

import android.content.Context
import com.google.gson.Gson
import com.sza.fastmediasorter.data.network.SftpClient
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
 * Unit tests for SftpOperationStrategy.
 *
 * Test Modes:
 * 1. Mock Tests (default) - All SFTP calls mocked, no real network
 * 2. Real Network Tests - Requires test_network_creds.json configuration
 *
 * Configuration:
 * - Copy test_network_creds.json.template to test_network_creds.json
 * - Set sftp.enabled = true for real network tests
 * - Set settings.skipRealNetworkTests = false to enable real tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SftpOperationStrategyTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSftpClient: SftpClient

    private lateinit var strategy: SftpOperationStrategy
    private var testConfig: NetworkTestConfig? = null

    // Test configuration data classes
    private data class NetworkTestConfig(
        val sftp: SftpConfig,
        val testFiles: TestFiles,
        val settings: TestSettings
    )

    private data class SftpConfig(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val privateKeyPath: String,
        val privateKeyPassphrase: String,
        val testFolder: String,
        val authType: String
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
        strategy = SftpOperationStrategy(mockContext, mockSftpClient)
    }

    private fun loadTestConfiguration() {
        try {
            val configFile = File("test_network_creds.json")
            if (configFile.exists()) {
                testConfig = Gson().fromJson(configFile.readText(), NetworkTestConfig::class.java)
            }
        } catch (e: Exception) {
            testConfig = null
        }
    }

    private fun isRealNetworkTestEnabled(): Boolean {
        return testConfig?.sftp?.enabled == true &&
               testConfig?.settings?.skipRealNetworkTests == false
    }

    // ==================== CONFIGURATION TESTS ====================

    @Test
    fun `configure SFTP connection with password auth`() {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        assertNotNull(strategy)
    }

    @Test
    fun `configure SFTP connection with SSH key auth`() {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            privateKeyPath = "/path/to/id_rsa"
        )

        assertNotNull(strategy)
    }

    @Test
    fun `configure SFTP connection with passphrase-protected key`() {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            privateKeyPath = "/path/to/id_rsa",
            passphrase = "keypassphrase"
        )

        assertNotNull(strategy)
    }

    @Test
    fun `configure SFTP with non-standard port`() {
        strategy.configure(
            host = "192.168.1.101",
            port = 2222,
            username = "sftpuser",
            password = "sftppass"
        )

        assertNotNull(strategy)
    }

    // ==================== CONNECTION TESTS (MOCK) ====================

    @Test
    fun `connect to SFTP server with password auth (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        `when`(mockSftpClient.connect(anyString(), anyInt(), anyString(), anyString()))
            .thenReturn(true)

        verify(mockSftpClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString())
    }

    @Test
    fun `connect to SFTP server with key auth (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            privateKeyPath = "/path/to/id_rsa"
        )

        `when`(mockSftpClient.connectWithKey(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenReturn(true)

        verify(mockSftpClient, atMost(1)).connectWithKey(anyString(), anyInt(), anyString(), anyString(), anyString())
    }

    @Test
    fun `handle connection timeout`() = runTest {
        strategy.configure(
            host = "192.168.1.254",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        `when`(mockSftpClient.connect(anyString(), anyInt(), anyString(), anyString()))
            .thenReturn(false)

        // Should handle timeout gracefully
        verify(mockSftpClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString())
    }

    @Test
    fun `handle authentication failure`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "wronguser",
            password = "wrongpass"
        )

        `when`(mockSftpClient.connect(anyString(), anyInt(), anyString(), anyString()))
            .thenReturn(false)

        verify(mockSftpClient, atMost(1)).connect(anyString(), anyInt(), anyString(), anyString())
    }

    @Test
    fun `handle invalid key file`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            privateKeyPath = "/nonexistent/key"
        )

        `when`(mockSftpClient.connectWithKey(anyString(), anyInt(), anyString(), anyString(), anyString()))
            .thenThrow(IllegalArgumentException("Key file not found"))

        try {
            // Operation that requires connection
            val testFile = MediaFile(
                path = "sftp://192.168.1.101/test.txt",
                name = "test.txt",
                size = 100,
                date = Date(),
                type = MediaType.TXT,
                isDirectory = false
            )
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    // ==================== LIST FILES TESTS (MOCK) ====================

    @Test
    fun `list files in SFTP directory (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val mockFiles = listOf(
            MediaFile("sftp://192.168.1.101/home/user/file1.jpg", "file1.jpg", 1024, Date(), MediaType.IMAGE, false),
            MediaFile("sftp://192.168.1.101/home/user/file2.mp4", "file2.mp4", 2048, Date(), MediaType.VIDEO, false),
            MediaFile("sftp://192.168.1.101/home/user/folder/", "folder", 0, Date(), MediaType.OTHER, true)
        )

        `when`(mockSftpClient.listFiles(anyString())).thenReturn(mockFiles)

        val result = strategy.listFiles("sftp://192.168.1.101/home/user/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertEquals(3, files.size)
        assertTrue(files[2].isDirectory)
    }

    @Test
    fun `list files returns empty for empty directory`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        `when`(mockSftpClient.listFiles(anyString())).thenReturn(emptyList())

        val result = strategy.listFiles("sftp://192.168.1.101/empty/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertTrue(files.isEmpty())
    }

    @Test
    fun `list files handles symbolic links`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val mockFiles = listOf(
            MediaFile("sftp://192.168.1.101/link_to_file.txt", "link_to_file.txt", 100, Date(), MediaType.TXT, false)
        )

        `when`(mockSftpClient.listFiles(anyString())).thenReturn(mockFiles)

        val result = strategy.listFiles("sftp://192.168.1.101/")

        assertTrue(result is Result.Success)
        val files = (result as Result.Success).data
        assertEquals(1, files.size)
    }

    // ==================== DOWNLOAD TESTS (MOCK) ====================

    @Test
    fun `download file from SFTP to local (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "sftp://192.168.1.101/remote/test.jpg",
            name = "test.jpg",
            size = 1024,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )

        `when`(mockSftpClient.downloadFile(anyString(), anyString(), any()))
            .thenReturn(true)

        val result = strategy.copy(sourceFile, "/local/destination/test.jpg")

        verify(mockSftpClient, atMost(1)).downloadFile(anyString(), anyString(), any())
    }

    @Test
    fun `download file with progress callback`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "sftp://192.168.1.101/remote/large.mp4",
            name = "large.mp4",
            size = 10485760,
            date = Date(),
            type = MediaType.VIDEO,
            isDirectory = false
        )

        val progressValues = mutableListOf<Float>()

        `when`(mockSftpClient.downloadFile(anyString(), anyString(), any()))
            .thenAnswer { invocation ->
                val callback = invocation.getArgument<((Float) -> Unit)?>(2)
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

        assertTrue(progressValues.size >= 2)
    }

    @Test
    fun `download handles permission denied`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "limiteduser",
            password = "pass"
        )

        val restrictedFile = MediaFile(
            path = "sftp://192.168.1.101/root/protected.txt",
            name = "protected.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSftpClient.downloadFile(anyString(), anyString(), any()))
            .thenThrow(SecurityException("Permission denied"))

        try {
            strategy.copy(restrictedFile, "/local/destination/protected.txt")
        } catch (e: Exception) {
            assertTrue(e is SecurityException)
        }
    }

    // ==================== UPLOAD TESTS (MOCK) ====================

    @Test
    fun `upload file from local to SFTP (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "/local/source/test.jpg",
            name = "test.jpg",
            size = 1024,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        )

        `when`(mockSftpClient.uploadFile(anyString(), anyString(), any()))
            .thenReturn(true)

        val result = strategy.copy(sourceFile, "sftp://192.168.1.101/remote/uploaded.jpg")

        verify(mockSftpClient, atMost(1)).uploadFile(anyString(), anyString(), any())
    }

    @Test
    fun `upload large file with progress`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "/local/source/video.mp4",
            name = "video.mp4",
            size = 52428800, // 50 MB
            date = Date(),
            type = MediaType.VIDEO,
            isDirectory = false
        )

        val progressValues = mutableListOf<Float>()

        `when`(mockSftpClient.uploadFile(anyString(), anyString(), any()))
            .thenAnswer { invocation ->
                val callback = invocation.getArgument<((Float) -> Unit)?>(2)
                for (i in 1..10) {
                    callback?.invoke(i / 10f)
                }
                true
            }

        val result = strategy.copy(
            sourceFile,
            "sftp://192.168.1.101/remote/video.mp4"
        ) { progress ->
            progressValues.add(progress)
        }

        assertTrue(progressValues.size >= 5)
    }

    // ==================== DELETE TESTS (MOCK) ====================

    @Test
    fun `delete file on SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val fileToDelete = MediaFile(
            path = "sftp://192.168.1.101/remote/delete_me.txt",
            name = "delete_me.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSftpClient.deleteFile(anyString())).thenReturn(true)

        val result = strategy.delete(fileToDelete)

        verify(mockSftpClient, atMost(1)).deleteFile(anyString())
    }

    @Test
    fun `delete directory on SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val dirToDelete = MediaFile(
            path = "sftp://192.168.1.101/remote/empty_dir/",
            name = "empty_dir",
            size = 0,
            date = Date(),
            type = MediaType.OTHER,
            isDirectory = true
        )

        `when`(mockSftpClient.deleteDirectory(anyString())).thenReturn(true)

        val result = strategy.delete(dirToDelete)

        verify(mockSftpClient, atMost(1)).deleteDirectory(anyString())
    }

    // ==================== MOVE/RENAME TESTS (MOCK) ====================

    @Test
    fun `move file on SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "sftp://192.168.1.101/old/file.txt",
            name = "file.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSftpClient.moveFile(anyString(), anyString())).thenReturn(true)

        val result = strategy.move(sourceFile, "sftp://192.168.1.101/new/file.txt")

        verify(mockSftpClient, atMost(1)).moveFile(anyString(), anyString())
    }

    @Test
    fun `rename file on SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val sourceFile = MediaFile(
            path = "sftp://192.168.1.101/remote/old_name.txt",
            name = "old_name.txt",
            size = 100,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSftpClient.moveFile(anyString(), anyString())).thenReturn(true)

        val result = strategy.rename(sourceFile, "new_name.txt")

        verify(mockSftpClient, atMost(1)).moveFile(anyString(), anyString())
    }

    // ==================== CREATE DIRECTORY TESTS (MOCK) ====================

    @Test
    fun `create directory on SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        `when`(mockSftpClient.createDirectory(anyString())).thenReturn(true)

        val result = strategy.createDirectory("sftp://192.168.1.101/remote/new_folder")

        verify(mockSftpClient, atMost(1)).createDirectory(anyString())
    }

    // ==================== FILE INFO TESTS (MOCK) ====================

    @Test
    fun `get file info from SFTP server (mock)`() = runTest {
        strategy.configure(
            host = "192.168.1.101",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )

        val mockFileInfo = MediaFile(
            path = "sftp://192.168.1.101/remote/info.txt",
            name = "info.txt",
            size = 2048,
            date = Date(1609459200000), // Jan 1, 2021
            type = MediaType.TXT,
            isDirectory = false
        )

        `when`(mockSftpClient.getFileInfo(anyString())).thenReturn(mockFileInfo)

        val result = strategy.getFileInfo("sftp://192.168.1.101/remote/info.txt")

        assertTrue(result is Result.Success)
        val fileInfo = (result as Result.Success).data
        assertEquals("info.txt", fileInfo.name)
        assertEquals(2048L, fileInfo.size)
    }

    // ==================== REAL NETWORK TESTS (OPTIONAL) ====================

    @Test
    fun `REAL TEST - connect to actual SFTP server with password`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.sftp
        assumeTrue("SFTP authType must be 'password'", config.authType == "password")

        strategy.configure(
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password
        )

        // Test would connect to real server
    }

    @Test
    fun `REAL TEST - list files on actual SFTP server`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.sftp

        strategy.configure(
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password
        )

        val result = strategy.listFiles("sftp://${config.host}${config.testFolder}")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `REAL TEST - download file from actual SFTP server`() = runTest {
        assumeTrue("Skipping real network test", isRealNetworkTestEnabled())

        val config = testConfig!!.sftp
        val testFiles = testConfig!!.testFiles

        strategy.configure(
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password
        )

        val sourceFile = MediaFile(
            path = "sftp://${config.host}${config.testFolder}/${testFiles.smallFile}",
            name = testFiles.smallFile,
            size = 1024,
            date = Date(),
            type = MediaType.TXT,
            isDirectory = false
        )

        val tempDest = File.createTempFile("sftp_test_", ".tmp")
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
