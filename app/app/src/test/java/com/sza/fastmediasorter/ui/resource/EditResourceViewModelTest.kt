package com.sza.fastmediasorter.ui.resource

import androidx.lifecycle.SavedStateHandle
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.usecase.DeleteResourceUseCase
import com.sza.fastmediasorter.domain.usecase.GetResourcesUseCase
import com.sza.fastmediasorter.domain.usecase.UpdateResourceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EditResourceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getResourcesUseCase: GetResourcesUseCase
    private lateinit var updateResourceUseCase: UpdateResourceUseCase
    private lateinit var deleteResourceUseCase: DeleteResourceUseCase

    private lateinit var viewModel: EditResourceViewModel

    private val testResource = Resource(
        id = 1L,
        name = "Photos",
        path = "/storage/Photos",
        type = ResourceType.LOCAL,
        sortMode = SortMode.DATE_DESC,
        displayMode = DisplayMode.GRID,
        isDestination = false,
        workWithAllFiles = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getResourcesUseCase = mock()
        updateResourceUseCase = mock()
        deleteResourceUseCase = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadResource populates state on success`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn null
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )

        viewModel.loadResource(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testResource.id, state.resourceId)
        assertEquals(testResource.name, state.name)
        assertEquals(testResource.path, state.path)
        assertEquals(testResource.type, state.resourceType)
        assertFalse(state.isEdited)
        assertFalse(state.canSave)
    }

    @Test
    fun `loadResource emits error and navigates back on failure`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Error("not found"))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn null
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )

        val eventDeferred = async { viewModel.events.first() }

        viewModel.loadResource(1L)
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is EditResourceEvent.ShowError)
    }

    @Test
    fun `onNameChanged updates state and validates`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onNameChanged("New Name")

        val state = viewModel.uiState.value
        assertEquals("New Name", state.name)
        assertTrue(state.isEdited)
        assertTrue(state.canSave)
        assertNull(state.nameError)
    }

    @Test
    fun `onNameChanged sets error when blank`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onNameChanged("")

        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertNotNull(state.nameError)
        assertFalse(state.canSave)
    }

    @Test
    fun `onSortModeChanged updates state and sets edited flag`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onSortModeChanged(SortMode.NAME_ASC)

        val state = viewModel.uiState.value
        assertEquals(SortMode.NAME_ASC, state.sortMode)
        assertTrue(state.isEdited)
        assertTrue(state.canSave)
    }

    @Test
    fun `onDisplayModeChanged updates state`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onDisplayModeChanged(DisplayMode.LIST)

        val state = viewModel.uiState.value
        assertEquals(DisplayMode.LIST, state.displayMode)
        assertTrue(state.isEdited)
    }

    @Test
    fun `saveResource emits ResourceSaved on success`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))
        whenever(updateResourceUseCase.invoke(any())).thenReturn(Result.Success(Unit))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onNameChanged("Updated")
        val eventDeferred = async { viewModel.events.first() }

        viewModel.saveResource()
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is EditResourceEvent.ResourceSaved)
    }

    @Test
    fun `saveResource emits ShowError on failure`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))
        whenever(updateResourceUseCase.invoke(any())).thenReturn(Result.Error("update failed"))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        viewModel.onNameChanged("Updated")
        val eventDeferred = async { viewModel.events.first() }

        viewModel.saveResource()
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is EditResourceEvent.ShowError)
    }

    @Test
    fun `deleteResource emits ResourceDeleted on success`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))
        whenever(deleteResourceUseCase.invoke(1L)).thenReturn(Result.Success(Unit))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        val eventDeferred = async { viewModel.events.first() }

        viewModel.deleteResource()
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is EditResourceEvent.ResourceDeleted)
    }

    @Test
    fun `deleteResource emits ShowError on failure`() = testScope.runTest {
        whenever(getResourcesUseCase.getById(1L)).thenReturn(Result.Success(testResource))
        whenever(deleteResourceUseCase.invoke(1L)).thenReturn(Result.Error("delete failed"))

        savedStateHandle = mock {
            on { get<Long>("EXTRA_RESOURCE_ID") } doReturn 1L
        }

        viewModel = EditResourceViewModel(
            savedStateHandle,
            getResourcesUseCase,
            updateResourceUseCase,
            deleteResourceUseCase
        )
        advanceUntilIdle()

        val eventDeferred = async { viewModel.events.first() }

        viewModel.deleteResource()
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is EditResourceEvent.ShowError)
    }
}
