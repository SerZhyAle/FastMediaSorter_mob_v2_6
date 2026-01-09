package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.scanner.FtpMediaScanner
import com.sza.fastmediasorter.data.scanner.LocalMediaScanner
import com.sza.fastmediasorter.data.scanner.SftpMediaScanner
import com.sza.fastmediasorter.data.scanner.SmbMediaScanner
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.NetworkCredentialsRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Date

/**
 * Unit tests for MediaRepositoryImpl.
 * Tests file scanning across different resource types and caching logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImplTest {

    private lateinit var repository: MediaRepositoryImpl
    private lateinit var resourceRepository: ResourceRepository
    private lateinit var credentialsRepository: NetworkCredentialsRepository
    private lateinit var localMediaScanner: LocalMediaScanner
    private lateinit var smbMediaScanner: SmbMediaScanner
    private lateinit var sftpMediaScanner: SftpMediaScanner
    private lateinit var ftpMediaScanner: FtpMediaScanner

    private val testMediaFiles = listOf(
        MediaFile(
            path = "/storage/test/image1.jpg",
            name = "image1.jpg",
            size = 1024L,
            date = Date(),
            type = MediaType.IMAGE,
            isDirectory = false
        ),
        MediaFile(
            path = "/storage/test/video1.mp4",
            name = "video1.mp4",
            size = 2048L,
            date = Date(),
            type = MediaType.VIDEO,
            isDirectory = false
        )
    )

    private val localResource = Resource(
        id = 1L,
        name = "Local Photos",
        path = "/storage/photos",
        type = ResourceType.LOCAL
    )

    private val smbResource = Resource(
        id = 2L,
        name = "Network Share",
        path = "/share/photos",
        type = ResourceType.SMB,
        credentialsId = 10L
    )

    private val smbCredentials = NetworkCredentials(
        id = 10L,
        resourceId = 2L,
        type = NetworkType.SMB,
        server = "192.168.1.100",
        port = 445,
        username = "user",
        password = "pass",
        shareName = "photos",
        domain = "WORKGROUP"
    )

    @Before
    fun setup() {
        resourceRepository = mock()
        credentialsRepository = mock()
        localMediaScanner = mock()
        smbMediaScanner = mock()
        sftpMediaScanner = mock()
        ftpMediaScanner = mock()

        repository = MediaRepositoryImpl(
            resourceRepository = resourceRepository,
            credentialsRepository = credentialsRepository,
            localMediaScanner = localMediaScanner,
            smbMediaScanner = smbMediaScanner,
            sftpMediaScanner = sftpMediaScanner,
            ftpMediaScanner = ftpMediaScanner
        )
    }

    // ==================== LOCAL RESOURCE TESTS ====================

    @Test
    fun `getFilesForResource scans local resource successfully`() = runTest {
        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(localMediaScanner.scanFolder("/storage/photos", recursive = false))
            .thenReturn(testMediaFiles)

        val result = repository.getFilesForResource(1L)

        assertEquals(2, result.size)
        assertEquals("image1.jpg", result[0].name)
        assertEquals("video1.mp4", result[1].name)
        verify(localMediaScanner).scanFolder("/storage/photos", recursive = false)
    }

    @Test
    fun `getFilesForResource returns empty list when resource not found`() = runTest {
        whenever(resourceRepository.getResourceById(999L)).thenReturn(null)

        val result = repository.getFilesForResource(999L)

        assertTrue(result.isEmpty())
        verifyNoInteractions(localMediaScanner)
    }

    @Test
    fun `getMediaFiles caches results after first scan`() = runTest {
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(testMediaFiles)

        // First call - should scan
        val result1 = repository.getMediaFiles(localResource)
        assertEquals(2, result1.size)

        // Second call - should return cached
        val result2 = repository.getMediaFiles(localResource)
        assertEquals(2, result2.size)

        // Scanner should only be called once
        verify(localMediaScanner, times(1)).scanFolder(any(), any())
    }

    @Test
    fun `clearCacheForResource removes cache entry`() = runTest {
        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(testMediaFiles)

        // First scan
        repository.getFilesForResource(1L)
        verify(localMediaScanner, times(1)).scanFolder(any(), any())

        // Clear cache
        repository.clearCacheForResource(1L)

        // Second scan should call scanner again
        repository.getFilesForResource(1L)
        verify(localMediaScanner, times(2)).scanFolder(any(), any())
    }

    @Test
    fun `scanResource with forceRefresh clears cache and rescans`() = runTest {
        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(testMediaFiles)

        // Initial scan
        repository.getFilesForResource(1L)
        verify(localMediaScanner, times(1)).scanFolder(any(), any())

        // Force refresh
        repository.scanResource(1L, forceRefresh = true)
        verify(localMediaScanner, times(2)).scanFolder(any(), any())
    }

    @Test
    fun `scanResource without forceRefresh uses cache`() = runTest {
        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(testMediaFiles)

        // Initial scan
        repository.getFilesForResource(1L)
        verify(localMediaScanner, times(1)).scanFolder(any(), any())

        // Scan without refresh
        repository.scanResource(1L, forceRefresh = false)

        // Scanner should still only be called once (uses cache)
        verify(localMediaScanner, times(1)).scanFolder(any(), any())
    }

    // ==================== SMB RESOURCE TESTS ====================

    @Test
    fun `getMediaFiles scans SMB resource with credentials`() = runTest {
        whenever(credentialsRepository.getCredentials(10L))
            .thenReturn(Result.Success(smbCredentials))
        whenever(smbMediaScanner.scanFolder(
            server = "192.168.1.100",
            port = 445,
            shareName = "photos",
            path = "/share/photos",
            username = "user",
            password = "pass",
            domain = "WORKGROUP"
        )).thenReturn(testMediaFiles)

        val result = repository.getMediaFiles(smbResource)

        assertEquals(2, result.size)
        verify(smbMediaScanner).scanFolder(
            server = "192.168.1.100",
            port = 445,
            shareName = "photos",
            path = "/share/photos",
            username = "user",
            password = "pass",
            domain = "WORKGROUP"
        )
    }

    @Test
    fun `getMediaFiles returns empty list when SMB credentials not found`() = runTest {
        whenever(credentialsRepository.getCredentials(10L))
            .thenReturn(Result.Error("Credentials not found"))

        val result = repository.getMediaFiles(smbResource)

        assertTrue(result.isEmpty())
        verifyNoInteractions(smbMediaScanner)
    }

    @Test
    fun `getMediaFiles returns empty list when SMB resource has no credentials ID`() = runTest {
        val resourceWithoutCreds = smbResource.copy(credentialsId = null)

        val result = repository.getMediaFiles(resourceWithoutCreds)

        assertTrue(result.isEmpty())
        verifyNoInteractions(smbMediaScanner)
        verifyNoInteractions(credentialsRepository)
    }

    // ==================== SFTP RESOURCE TESTS ====================

    @Test
    fun `getMediaFiles scans SFTP resource with password auth`() = runTest {
        val sftpResource = Resource(
            id = 3L,
            name = "SFTP Server",
            path = "/home/user/photos",
            type = ResourceType.SFTP,
            credentialsId = 11L
        )
        val sftpCredentials = NetworkCredentials(
            id = 11L,
            resourceId = 3L,
            type = NetworkType.SFTP,
            server = "sftp.example.com",
            port = 22,
            username = "sftpuser",
            password = "sftppass",
            useSshKey = false
        )

        whenever(credentialsRepository.getCredentials(11L))
            .thenReturn(Result.Success(sftpCredentials))
        whenever(sftpMediaScanner.scanFolder(
            host = "sftp.example.com",
            port = 22,
            path = "/home/user/photos",
            username = "sftpuser",
            password = "sftppass",
            privateKey = null,
            passphrase = null
        )).thenReturn(testMediaFiles)

        val result = repository.getMediaFiles(sftpResource)

        assertEquals(2, result.size)
        verify(sftpMediaScanner).scanFolder(
            host = "sftp.example.com",
            port = 22,
            path = "/home/user/photos",
            username = "sftpuser",
            password = "sftppass",
            privateKey = null,
            passphrase = null
        )
    }

    @Test
    fun `getMediaFiles returns empty list when SFTP credentials not found`() = runTest {
        val sftpResource = Resource(
            id = 3L,
            name = "SFTP Server",
            path = "/home/user/photos",
            type = ResourceType.SFTP,
            credentialsId = 11L
        )

        whenever(credentialsRepository.getCredentials(11L))
            .thenReturn(Result.Error("Credentials not found"))

        val result = repository.getMediaFiles(sftpResource)

        assertTrue(result.isEmpty())
        verifyNoInteractions(sftpMediaScanner)
    }

    // ==================== FTP RESOURCE TESTS ====================

    @Test
    fun `getMediaFiles scans FTP resource with credentials`() = runTest {
        val ftpResource = Resource(
            id = 4L,
            name = "FTP Server",
            path = "/public/photos",
            type = ResourceType.FTP,
            credentialsId = 12L
        )
        val ftpCredentials = NetworkCredentials(
            id = 12L,
            resourceId = 4L,
            type = NetworkType.FTP,
            server = "ftp.example.com",
            port = 21,
            username = "ftpuser",
            password = "ftppass"
        )

        whenever(credentialsRepository.getCredentials(12L))
            .thenReturn(Result.Success(ftpCredentials))
        whenever(ftpMediaScanner.scanFolder(
            host = "ftp.example.com",
            port = 21,
            remotePath = "/public/photos",
            username = "ftpuser",
            password = "ftppass"
        )).thenReturn(testMediaFiles)

        val result = repository.getMediaFiles(ftpResource)

        assertEquals(2, result.size)
        verify(ftpMediaScanner).scanFolder(
            host = "ftp.example.com",
            port = 21,
            remotePath = "/public/photos",
            username = "ftpuser",
            password = "ftppass"
        )
    }

    @Test
    fun `getMediaFiles returns empty list when FTP credentials not found`() = runTest {
        val ftpResource = Resource(
            id = 4L,
            name = "FTP Server",
            path = "/public/photos",
            type = ResourceType.FTP,
            credentialsId = 12L
        )

        whenever(credentialsRepository.getCredentials(12L))
            .thenReturn(Result.Error("Credentials not found"))

        val result = repository.getMediaFiles(ftpResource)

        assertTrue(result.isEmpty())
        verifyNoInteractions(ftpMediaScanner)
    }

    // ==================== CLOUD RESOURCE TESTS ====================

    @Test
    fun `getMediaFiles returns empty list for Google Drive not yet implemented`() = runTest {
        val cloudResource = Resource(
            id = 5L,
            name = "Google Drive",
            path = "/MyDrive/Photos",
            type = ResourceType.GOOGLE_DRIVE
        )

        val result = repository.getMediaFiles(cloudResource)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMediaFiles returns empty list for OneDrive not yet implemented`() = runTest {
        val cloudResource = Resource(
            id = 6L,
            name = "OneDrive",
            path = "/Photos",
            type = ResourceType.ONEDRIVE
        )

        val result = repository.getMediaFiles(cloudResource)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMediaFiles returns empty list for Dropbox not yet implemented`() = runTest {
        val cloudResource = Resource(
            id = 7L,
            name = "Dropbox",
            path = "/Photos",
            type = ResourceType.DROPBOX
        )

        val result = repository.getMediaFiles(cloudResource)

        assertTrue(result.isEmpty())
    }

    // ==================== CACHE TESTS ====================

    @Test
    fun `getFileByPath finds file in cache`() = runTest {
        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(testMediaFiles)

        // Populate cache
        repository.getFilesForResource(1L)

        // Search for file
        val file = repository.getFileByPath("/storage/test/image1.jpg")

        assertNotNull(file)
        assertEquals("image1.jpg", file?.name)
    }

    @Test
    fun `getFileByPath returns null when file not in cache`() = runTest {
        val file = repository.getFileByPath("/non/existent/file.jpg")

        assertNull(file)
    }

    @Test
    fun `cache is separate for different resources`() = runTest {
        val resource2 = localResource.copy(id = 2L, path = "/storage/videos")
        val videoFiles = listOf(
            MediaFile(
                path = "/storage/videos/video1.mp4",
                name = "video1.mp4",
                size = 5000L,
                date = Date(),
                type = MediaType.VIDEO,
                isDirectory = false
            )
        )

        whenever(resourceRepository.getResourceById(1L)).thenReturn(localResource)
        whenever(resourceRepository.getResourceById(2L)).thenReturn(resource2)
        whenever(localMediaScanner.scanFolder("/storage/photos", false))
            .thenReturn(testMediaFiles)
        whenever(localMediaScanner.scanFolder("/storage/videos", false))
            .thenReturn(videoFiles)

        // Scan both resources
        val files1 = repository.getFilesForResource(1L)
        val files2 = repository.getFilesForResource(2L)

        assertEquals(2, files1.size)
        assertEquals(1, files2.size)
        assertNotEquals(files1, files2)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `getMediaFiles handles empty scan results`() = runTest {
        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(emptyList())

        val result = repository.getMediaFiles(localResource)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMediaFiles handles large file lists`() = runTest {
        val largeFileList = (1..1000).map { index ->
            MediaFile(
                path = "/storage/test/file$index.jpg",
                name = "file$index.jpg",
                size = 1024L,
                date = Date(),
                type = MediaType.IMAGE,
                isDirectory = false
            )
        }

        whenever(localMediaScanner.scanFolder(any(), any())).thenReturn(largeFileList)

        val result = repository.getMediaFiles(localResource)

        assertEquals(1000, result.size)
    }
}
