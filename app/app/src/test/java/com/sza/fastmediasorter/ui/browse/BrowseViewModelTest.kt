package com.sza.fastmediasorter.ui.browse

import androidx.lifecycle.SavedStateHandle
import com.sza.fastmediasorter.data.operation.TrashManager
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import com.sza.fastmediasorter.domain.usecase.GetResourcesUseCase
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getResourcesUseCase: GetResourcesUseCase
    private lateinit var getMediaFilesUseCase: GetMediaFilesUseCase
    private lateinit var fileOperationStrategy: FileOperationStrategy
    private lateinit var trashManager: TrashManager

    private lateinit var viewModel: BrowseViewModel

    private val resourceId = 1L
    private val testResource = Resource(
        id = resourceId,
        name = "Photos",
        path = "/storage/Photos",
        type = ResourceType.LOCAL
    )
    private val mediaFiles = listOf(
        MediaFile(
            path = "/storage/Photos/A.jpg",
            name = "A.jpg",
            size = 10,
            date = Date(1_000),
            type = MediaType.IMAGE
        ),
        MediaFile(
            path = "/storage/Photos/B.jpg",
            name = "B.jpg",
            size = 20,
            date = Date(2_000),
            type = MediaType.IMAGE
        ),
        MediaFile(
            path = "/storage/Photos/C.jpg",
            name = "C.jpg",
            size = 30,
            date = Date(3_000),
            type = MediaType.IMAGE
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = mock {
            on { get<Long>(BrowseViewModel.EXTRA_RESOURCE_ID) } doReturn null
        }
        getResourcesUseCase = mock()
        getMediaFilesUseCase = mock()
        fileOperationStrategy = mock()
        trashManager = mock()

        viewModel = BrowseViewModel(
            savedStateHandle,
            getResourcesUseCase,
            getMediaFilesUseCase,
            fileOperationStrategy,
            trashManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFiles sets state and records visit`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Success(testResource))
        whenever(getMediaFilesUseCase.invoke(resourceId)).thenReturn(Result.Success(mediaFiles))

        val eventDeferred = async { viewModel.events.filterIsInstance<BrowseUiEvent.RecordResourceVisit>().first() }

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mediaFiles, state.files)
        assertEquals(testResource.name, state.resourceName)

        assertEquals(testResource, eventDeferred.await().resource)
    }

    @Test
    fun `loadFiles handles resource error`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Error("not found"))

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("not found", state.errorMessage)
    }

    @Test
    fun `search filters files by query`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Success(testResource))
        whenever(getMediaFilesUseCase.invoke(resourceId)).thenReturn(Result.Success(mediaFiles))

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("b")

        val state = viewModel.uiState.value
        assertEquals(listOf(mediaFiles[1]), state.filteredFiles)
        assertEquals(listOf(mediaFiles[1]), state.displayedFiles)
    }

    @Test
    fun `sort updates order`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Success(testResource))
        whenever(getMediaFilesUseCase.invoke(resourceId)).thenReturn(Result.Success(mediaFiles))

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        viewModel.onSortModeSelected(SortMode.NAME_DESC)

        val state = viewModel.uiState.value
        assertEquals(listOf(mediaFiles[2], mediaFiles[1], mediaFiles[0]), state.files)
    }

    @Test
    fun `executeFileOperation emits snackbar`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Success(testResource))
        whenever(getMediaFilesUseCase.invoke(resourceId)).thenReturn(Result.Success(mediaFiles))
        whenever(fileOperationStrategy.copy(any(), any(), anyOrNull<((Float) -> Unit)>())).thenReturn(Result.Success("/dest/A.jpg"))

        val eventDeferred = async { viewModel.events.filterIsInstance<BrowseUiEvent.ShowSnackbar>().first() }

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        viewModel.onFileLongClick(mediaFiles[0])
        viewModel.onCopyClick()
        advanceUntilIdle()

        viewModel.executeFileOperation(listOf(mediaFiles[0].path), "/dest", isMove = false)
        advanceUntilIdle()

        assertTrue(eventDeferred.await().message.contains("copied"))
    }

    @Test
    fun `confirmDelete moves selected files to trash and shows undo`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(resourceId)).thenReturn(Result.Success(testResource))
        whenever(getMediaFilesUseCase.invoke(resourceId)).thenReturn(Result.Success(mediaFiles))
        whenever(trashManager.moveToTrash(any())).thenReturn(Result.Success(TrashManager.TrashedFile("", "")))

        val eventDeferred = async { viewModel.events.filterIsInstance<BrowseUiEvent.ShowUndoSnackbar>().first() }

        viewModel.loadFiles(resourceId)
        advanceUntilIdle()

        viewModel.onSelectAllClick()
        viewModel.confirmDelete()
        advanceUntilIdle()

        verify(trashManager).moveToTrash(mediaFiles[0])

        val event = eventDeferred.await()
        assertTrue(event.message.contains("deleted"))
        assertEquals(mediaFiles.size, event.deletedCount)
    }

    @Test
    fun `undoLastDelete restores file and refreshes`() = testScope.runTest {
        val trashed = TrashManager.TrashedFile(originalPath = "path", trashPath = "trash")
        whenever(trashManager.getLastDeleted()).thenReturn(trashed)
        whenever(trashManager.restoreFromTrash(trashed)).thenReturn(Result.Success("path"))

        viewModel.undoLastDelete()
        advanceUntilIdle()

        verify(trashManager).restoreFromTrash(trashed)
    }
}
