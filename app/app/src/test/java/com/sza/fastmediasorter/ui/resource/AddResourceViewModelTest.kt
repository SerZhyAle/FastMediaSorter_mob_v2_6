package com.sza.fastmediasorter.ui.resource

import android.net.Uri
import com.sza.fastmediasorter.domain.model.NetworkCredentials
import com.sza.fastmediasorter.domain.model.NetworkType
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.usecase.AddResourceUseCase
import com.sza.fastmediasorter.domain.usecase.SaveNetworkCredentialsUseCase
import com.sza.fastmediasorter.domain.usecase.TestNetworkConnectionUseCase
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AddResourceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var addResourceUseCase: AddResourceUseCase
    private lateinit var saveNetworkCredentialsUseCase: SaveNetworkCredentialsUseCase
    private lateinit var testNetworkConnectionUseCase: TestNetworkConnectionUseCase

    private lateinit var viewModel: AddResourceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addResourceUseCase = mock()
        saveNetworkCredentialsUseCase = mock()
        testNetworkConnectionUseCase = mock()

        viewModel = AddResourceViewModel(
            addResourceUseCase,
            saveNetworkCredentialsUseCase,
            testNetworkConnectionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onFolderSelected emits ShowConfig`() = testScope.runTest {
        val uri = mock<Uri> {
            on { path } doReturn "/storage/Pictures"
            on { toString() } doReturn "file:///storage/Pictures"
            on { scheme } doReturn "file"
            on { lastPathSegment } doReturn "Pictures"
        }
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onFolderSelected(uri)
        advanceUntilIdle()

        val event = eventDeferred.await() as AddResourceEvent.ShowConfig
        assertEquals("Pictures", event.name)
        assertEquals(uri.toString(), event.path)
    }

    @Test
    fun `onConfigConfirmed emits ResourceAdded on success`() = testScope.runTest {
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Success(42L))

        val eventsDeferred = async { viewModel.events.take(1).toList() }

        viewModel.onConfigConfirmed(
            name = "Folder",
            path = "/storage/folder",
            isReadOnly = false,
            isDestination = true,
            workWithAllFiles = false,
            pinCode = null,
            supportedMediaTypes = 0
        )
        advanceUntilIdle()

        val event = eventsDeferred.await().first()
        assertTrue(event is AddResourceEvent.ResourceAdded)
        assertEquals(42L, (event as AddResourceEvent.ResourceAdded).resourceId)
    }

    @Test
    fun `onConfigConfirmed emits ShowError on failure`() = testScope.runTest {
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Error("duplicate"))

        val eventsDeferred = async { viewModel.events.take(1).toList() }

        viewModel.onConfigConfirmed(
            name = "Folder",
            path = "/storage/folder",
            isReadOnly = false,
            isDestination = true,
            workWithAllFiles = false
        )
        advanceUntilIdle()

        val event = eventsDeferred.await().first()
        assertTrue(event is AddResourceEvent.ShowError)
    }

    @Test
    fun `onNetworkCredentialsEntered happy path emits testing success and resource added`() = testScope.runTest {
        whenever(testNetworkConnectionUseCase.invoke(any())).thenReturn(Result.Success(Unit))
        whenever(saveNetworkCredentialsUseCase.invoke(any())).thenReturn(Result.Success("cred-1"))
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Success(100L))

        val eventsDeferred = async { viewModel.events.take(3).toList() }

        viewModel.onNetworkCredentialsEntered(
            credentialId = "cred-1",
            type = NetworkType.SMB,
            name = "NAS",
            server = "192.168.1.10",
            port = 445,
            username = "user",
            password = "pass",
            domain = "",
            shareName = "share",
            useSshKey = false,
            sshKeyPath = null
        )
        advanceUntilIdle()

        val events = eventsDeferred.await()
        assertTrue(events[0] is AddResourceEvent.ConnectionTesting)
        assertTrue(events[1] is AddResourceEvent.ConnectionSuccess)
        assertTrue(events[2] is AddResourceEvent.ResourceAdded)
    }

    @Test
    fun `onNetworkCredentialsEntered emits ConnectionFailed on connection error`() = testScope.runTest {
        whenever(testNetworkConnectionUseCase.invoke(any())).thenReturn(Result.Error("network fail"))

        val eventsDeferred = async { viewModel.events.take(2).toList() }

        viewModel.onNetworkCredentialsEntered(
            credentialId = "cred-1",
            type = NetworkType.SMB,
            name = "NAS",
            server = "192.168.1.10",
            port = 445,
            username = "user",
            password = "pass",
            domain = "",
            shareName = "share",
            useSshKey = false,
            sshKeyPath = null
        )
        advanceUntilIdle()

        val events = eventsDeferred.await()
        assertTrue(events[0] is AddResourceEvent.ConnectionTesting)
        assertTrue(events[1] is AddResourceEvent.ConnectionFailed)
    }

    @Test
    fun `onNetworkCredentialsEntered emits ShowError when saving credentials fails`() = testScope.runTest {
        whenever(testNetworkConnectionUseCase.invoke(any())).thenReturn(Result.Success(Unit))
        whenever(saveNetworkCredentialsUseCase.invoke(any())).thenReturn(Result.Error("save fail"))

        val eventsDeferred = async { viewModel.events.take(3).toList() }

        viewModel.onNetworkCredentialsEntered(
            credentialId = "cred-1",
            type = NetworkType.SMB,
            name = "NAS",
            server = "192.168.1.10",
            port = 445,
            username = "user",
            password = "pass",
            domain = "",
            shareName = "share",
            useSshKey = false,
            sshKeyPath = null
        )
        advanceUntilIdle()

        val events = eventsDeferred.await()
        assertTrue(events[0] is AddResourceEvent.ConnectionTesting)
        assertTrue(events[1] is AddResourceEvent.ConnectionSuccess)
        assertTrue(events[2] is AddResourceEvent.ShowError)
    }

    @Test
    fun `onNetworkCredentialsEntered emits ShowError when adding resource fails`() = testScope.runTest {
        whenever(testNetworkConnectionUseCase.invoke(any())).thenReturn(Result.Success(Unit))
        whenever(saveNetworkCredentialsUseCase.invoke(any())).thenReturn(Result.Success("cred-1"))
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Error("insert fail"))

        val eventsDeferred = async { viewModel.events.take(3).toList() }

        viewModel.onNetworkCredentialsEntered(
            credentialId = "cred-1",
            type = NetworkType.SMB,
            name = "NAS",
            server = "192.168.1.10",
            port = 445,
            username = "user",
            password = "pass",
            domain = "",
            shareName = "share",
            useSshKey = false,
            sshKeyPath = null
        )
        advanceUntilIdle()

        val events = eventsDeferred.await()
        assertTrue(events[0] is AddResourceEvent.ConnectionTesting)
        assertTrue(events[1] is AddResourceEvent.ConnectionSuccess)
        assertTrue(events[2] is AddResourceEvent.ShowError)
    }

    @Test
    fun `onNetworkResourceSelected emits ResourceAdded on success`() = testScope.runTest {
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Success(7L))

        val eventsDeferred = async { viewModel.events.take(1).toList() }

        viewModel.onNetworkResourceSelected(ResourceType.SMB, "smb://host", "/share", "NAS")
        advanceUntilIdle()

        val event = eventsDeferred.await().first()
        assertTrue(event is AddResourceEvent.ResourceAdded)
    }

    @Test
    fun `onCloudFolderSelected emits ResourceAdded on success`() = testScope.runTest {
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Success(88L))

        val eventsDeferred = async { viewModel.events.take(1).toList() }

        viewModel.onCloudFolderSelected(
            resourceType = ResourceType.GOOGLE_DRIVE,
            folderId = UUID.randomUUID().toString(),
            folderName = "Drive",
            folderPath = "/",
            isDestination = true,
            scanSubdirectories = true
        )
        advanceUntilIdle()

        val event = eventsDeferred.await().first()
        assertTrue(event is AddResourceEvent.ResourceAdded)
    }

    @Test
    fun `onCloudFolderSelected emits ShowError on failure`() = testScope.runTest {
        whenever(addResourceUseCase.invoke(
            name = any(),
            path = any(),
            type = any(),
            credentialsId = anyOrNull(),
            sortMode = any(),
            displayMode = any(),
            workWithAllFiles = any(),
            isDestination = any(),
            isReadOnly = any(),
            pinCode = anyOrNull(),
            supportedMediaTypes = any()
        )).thenReturn(Result.Error("cloud fail"))

        val eventsDeferred = async { viewModel.events.take(1).toList() }

        viewModel.onCloudFolderSelected(
            resourceType = ResourceType.GOOGLE_DRIVE,
            folderId = UUID.randomUUID().toString(),
            folderName = "Drive",
            folderPath = "/",
            isDestination = true,
            scanSubdirectories = true
        )
        advanceUntilIdle()

        val event = eventsDeferred.await().first()
        assertTrue(event is AddResourceEvent.ShowError)
    }
}
