package com.sza.fastmediasorter.ui.player

import com.sza.fastmediasorter.data.operation.TrashManager
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.translation.TranslationManager
import com.sza.fastmediasorter.ui.player.PlayerUiEvent.NavigateBack
import com.sza.fastmediasorter.ui.player.PlayerUiEvent.NavigateToPage
import com.sza.fastmediasorter.ui.player.PlayerUiEvent.ShowDeleteConfirmation
import com.sza.fastmediasorter.ui.player.PlayerUiEvent.ShowSnackbar
import com.sza.fastmediasorter.ui.player.PlayerViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fileMetadataRepository: FileMetadataRepository
    private lateinit var trashManager: TrashManager
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var translationManager: TranslationManager

    private lateinit var viewModel: PlayerViewModel

    private val fileOne = File.createTempFile("one", ".jpg").apply { deleteOnExit() }
    private val fileTwo = File.createTempFile("two", ".png").apply { deleteOnExit() }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fileMetadataRepository = mock()
        trashManager = mock()
        preferencesRepository = mock()
        translationManager = mock()

        viewModel = PlayerViewModel(
            fileMetadataRepository,
            trashManager,
            preferencesRepository,
            translationManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFiles populates state and favorite`() = testScope.runTest {
        whenever(fileMetadataRepository.isFavorite(any())).thenReturn(true)

        viewModel.loadFiles(listOf(fileOne.absolutePath, fileTwo.absolutePath), startIndex = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.totalCount)
        assertEquals(1, state.currentIndex)
        assertEquals(fileTwo.name, state.currentFileName)
        assertTrue(state.hasPrevious)
        assertFalse(state.hasNext)
        assertEquals(MediaType.IMAGE, state.currentMediaType)
        assertTrue(state.isFavorite)
    }

    @Test
    fun `loadFiles with empty list navigates back`() = testScope.runTest {
        val eventDeferred = async { viewModel.events.filterIsInstance<PlayerUiEvent.NavigateBack>().first() }

        viewModel.loadFiles(emptyList(), startIndex = 0)
        advanceUntilIdle()

        assertEquals(PlayerUiEvent.NavigateBack, eventDeferred.await())
    }

    @Test
    fun `onPageSelected updates state and favorite`() = testScope.runTest {
        whenever(fileMetadataRepository.isFavorite(any())).thenReturn(true, false)

        viewModel.loadFiles(listOf(fileOne.absolutePath, fileTwo.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.onPageSelected(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertEquals(fileTwo.name, state.currentFileName)
        assertFalse(state.isFavorite)
    }

    @Test
    fun `onFavoriteClick toggles favorite and emits snackbar`() = testScope.runTest {
        whenever(fileMetadataRepository.isFavorite(any())).thenReturn(false)
        whenever(fileMetadataRepository.toggleFavorite(any(), any())).thenReturn(true)
        val eventDeferred = async { viewModel.events.filterIsInstance<PlayerUiEvent.ShowSnackbar>().first() }

        viewModel.loadFiles(listOf(fileOne.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.onFavoriteClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
        assertTrue(eventDeferred.await().message.contains("Added"))
    }

    @Test
    fun `onDeleteClick shows confirmation`() = testScope.runTest {
        val eventDeferred = async { viewModel.events.filterIsInstance<PlayerUiEvent.ShowDeleteConfirmation>().first() }
        viewModel.loadFiles(listOf(fileOne.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.onDeleteClick()
        advanceUntilIdle()

        assertEquals(fileOne.absolutePath, eventDeferred.await().filePath)
    }

    @Test
    fun `confirmDeleteCurrentFile removes file and navigates`() = testScope.runTest {
        whenever(fileMetadataRepository.isFavorite(any())).thenReturn(false)
        whenever(trashManager.moveToTrash(any())).thenReturn(Result.Success(TrashManager.TrashedFile("", "")))

        val eventsDeferred = async { viewModel.events.take(2).toList() }

        viewModel.loadFiles(listOf(fileOne.absolutePath, fileTwo.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.confirmDeleteCurrentFile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals(fileTwo.name, state.currentFileName)

        val events = eventsDeferred.await()
        assertTrue(events.any { it is PlayerUiEvent.ShowSnackbar })
        assertTrue(events.any { it is PlayerUiEvent.NavigateToPage && it.index == 0 })
    }

    @Test
    fun `confirmDeleteCurrentFile on last file navigates back`() = testScope.runTest {
        whenever(fileMetadataRepository.isFavorite(any())).thenReturn(false)
        whenever(trashManager.moveToTrash(any())).thenReturn(Result.Success(TrashManager.TrashedFile("", "")))

        val eventsDeferred = async { viewModel.events.take(2).toList() }

        viewModel.loadFiles(listOf(fileOne.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.confirmDeleteCurrentFile()
        advanceUntilIdle()

        val events = eventsDeferred.await()
        assertTrue(events.any { it is PlayerUiEvent.ShowSnackbar })
        assertTrue(events.any { it is PlayerUiEvent.NavigateBack })
    }

    @Test
    fun `renameFile validates and updates state`() = testScope.runTest {
        // Ensure file exists and in state
        val tempDir = fileOne.parentFile ?: File(".")
        val original = File(tempDir, "rename_me.txt").apply { writeText("content") }
        val newName = "renamed_file"

        try {
            viewModel.loadFiles(listOf(original.absolutePath), startIndex = 0)
            advanceUntilIdle()

            val eventsDeferred = async { viewModel.events.filterIsInstance<PlayerUiEvent.ShowSnackbar>().first() }

            viewModel.renameFile(original.absolutePath, newName)
            advanceUntilIdle()

            val renamedFile = File(tempDir, "$newName.txt")
            assertTrue(renamedFile.exists())
            assertEquals(renamedFile.name, viewModel.uiState.value.currentFileName)
            assertTrue(eventsDeferred.await().message.contains("successfully"))
        } finally {
            File(tempDir, "$newName.txt").delete()
            original.delete()
        }
    }

    @Test
    fun `onBackPressed shows UI when hidden`() = testScope.runTest {
        viewModel.loadFiles(listOf(fileOne.absolutePath), startIndex = 0)
        advanceUntilIdle()

        viewModel.onMediaClick() // hide
        assertFalse(viewModel.uiState.value.isUiVisible)

        val consumed = viewModel.onBackPressed()

        assertTrue(consumed)
        assertTrue(viewModel.uiState.value.isUiVisible)
    }
}
