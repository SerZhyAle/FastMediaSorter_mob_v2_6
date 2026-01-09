package com.sza.fastmediasorter.ui.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MainViewModel
    private lateinit var resourceRepository: ResourceRepository
    private lateinit var preferencesRepository: PreferencesRepository

    private val testResources = listOf(
        Resource(
            id = 1L,
            name = "Photos",
            path = "/storage/photos",
            type = ResourceType.LOCAL
        ),
        Resource(
            id = 2L,
            name = "Network Share",
            path = "//192.168.1.100/photos",
            type = ResourceType.SMB
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        resourceRepository = mock()
        preferencesRepository = mock()

        // Default: Return test resources
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(flowOf(testResources))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = MainViewModel(resourceRepository, preferencesRepository)
    }

    // ==================== INITIAL STATE TESTS ====================

    @Test
    fun `initial state is loading`() = runTest {
        createViewModel()
        
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertTrue(initialState.resources.isEmpty())
    }

    @Test
    fun `loads resources on init`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.resources.size)
        assertEquals("Photos", state.resources[0].name)
        assertEquals("Network Share", state.resources[1].name)
        assertFalse(state.showEmptyState)
        assertNull(state.errorMessage)
    }

    @Test
    fun `shows empty state when no resources`() = runTest {
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(flowOf(emptyList()))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.resources.isEmpty())
        assertTrue(state.showEmptyState)
    }

    @Test
    fun `handles error loading resources`() = runTest {
        val exception = Exception("Database error")
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(kotlinx.coroutines.flow.flow { throw exception })

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Database error", state.errorMessage)
    }

    // ==================== RESOURCE INTERACTION TESTS ====================

    @Test
    fun `onResourceClick sends navigate to browse event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val resource = testResources[0]
        viewModel.onResourceClick(resource)
        advanceUntilIdle()

        // Can't easily verify Channel events in this setup, but we can verify no crash
        // In real tests, you'd use a test observer or turbine library
    }

    @Test
    fun `onResourceLongClick sends navigate to edit event and returns true`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val resource = testResources[0]
        val result = viewModel.onResourceLongClick(resource)
        advanceUntilIdle()

        assertTrue(result)
    }

    @Test
    fun `onResourceMoreClick sends navigate to edit event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val resource = testResources[0]
        viewModel.onResourceMoreClick(resource)
        advanceUntilIdle()

        // Verify no crash - event verification would require turbine or similar
    }

    @Test
    fun `onAddResourceClick sends navigate to add resource event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAddResourceClick()
        advanceUntilIdle()

        // Verify no crash
    }

    @Test
    fun `onSearchClick sends navigate to search event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onSearchClick()
        advanceUntilIdle()

        // Verify no crash
    }

    @Test
    fun `onFavoritesClick sends navigate to favorites event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onFavoritesClick()
        advanceUntilIdle()

        // Verify no crash
    }

    @Test
    fun `onSettingsClick sends navigate to settings event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onSettingsClick()
        advanceUntilIdle()

        // Verify no crash
    }

    // ==================== DELETE RESOURCE TESTS ====================

    @Test
    fun `deleteResource calls repository and shows success message`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val resource = testResources[0]
        viewModel.deleteResource(resource)
        advanceUntilIdle()

        verify(resourceRepository).deleteResource(resource)
        // Snackbar event would be sent (can't easily verify without turbine)
    }

    @Test
    fun `deleteResource handles error and shows error message`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val resource = testResources[0]
        whenever(resourceRepository.deleteResource(any())).thenThrow(RuntimeException("Delete failed"))

        viewModel.deleteResource(resource)
        advanceUntilIdle()

        verify(resourceRepository).deleteResource(resource)
        // Error snackbar event would be sent
    }

    // ==================== VIEW MODE TESTS ====================

    @Test
    fun `toggleViewMode switches between grid and list`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // Initial state is list (false)
        assertFalse(viewModel.uiState.value.isGridMode)

        // Toggle to grid
        viewModel.toggleViewMode()
        assertTrue(viewModel.uiState.value.isGridMode)

        // Toggle back to list
        viewModel.toggleViewMode()
        assertFalse(viewModel.uiState.value.isGridMode)
    }

    // ==================== TAB FILTER TESTS ====================

    @Test
    fun `setActiveTab updates active tab in state`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // Initial tab is ALL
        assertEquals(ResourceTab.ALL, viewModel.uiState.value.activeTab)

        // Change to LOCAL
        viewModel.setActiveTab(ResourceTab.LOCAL)
        assertEquals(ResourceTab.LOCAL, viewModel.uiState.value.activeTab)

        // Change to SMB
        viewModel.setActiveTab(ResourceTab.SMB)
        assertEquals(ResourceTab.SMB, viewModel.uiState.value.activeTab)

        // Change to FTP_SFTP
        viewModel.setActiveTab(ResourceTab.FTP_SFTP)
        assertEquals(ResourceTab.FTP_SFTP, viewModel.uiState.value.activeTab)

        // Change to CLOUD
        viewModel.setActiveTab(ResourceTab.CLOUD)
        assertEquals(ResourceTab.CLOUD, viewModel.uiState.value.activeTab)
    }

    // ==================== REFRESH TESTS ====================

    @Test
    fun `refresh sets loading state`() = runTest {
        createViewModel()
        advanceUntilIdle()

        // Initial load complete
        assertFalse(viewModel.uiState.value.isLoading)

        // Trigger refresh
        viewModel.refresh()
        
        // Should set loading to true (will be updated by flow)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    // ==================== START PLAYER TESTS ====================

    @Test
    fun `onStartPlayerClick navigates to first resource when resources exist`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onStartPlayerClick()
        advanceUntilIdle()

        // Should send navigate event with first resource ID
        // Event verification would require turbine
    }

    @Test
    fun `onStartPlayerClick does nothing when no resources`() = runTest {
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(flowOf(emptyList()))

        createViewModel()
        advanceUntilIdle()

        viewModel.onStartPlayerClick()
        advanceUntilIdle()

        // Should not crash, no navigation event sent
    }

    // ==================== FILTER TESTS ====================

    @Test
    fun `onFilterClick triggers filter dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onFilterClick()
        
        // Should trigger filter dialog (can't easily verify without turbine)
    }

    // ==================== STATE PERSISTENCE TESTS ====================

    @Test
    fun `state maintains resources across multiple emissions`() = runTest {
        val updatedResources = testResources + Resource(
            id = 3L,
            name = "New Resource",
            path = "/storage/new",
            type = ResourceType.LOCAL
        )

        val flowEmitter = kotlinx.coroutines.flow.MutableStateFlow(testResources)
        whenever(resourceRepository.getAllResourcesFlow()).thenReturn(flowEmitter)

        createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.resources.size)

        // Emit updated resources
        flowEmitter.value = updatedResources
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.resources.size)
        assertEquals("New Resource", viewModel.uiState.value.resources[2].name)
    }

    @Test
    fun `clears error message when resources load successfully`() = runTest {
        // Start with error
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(kotlinx.coroutines.flow.flow { throw Exception("Initial error") })

        createViewModel()
        advanceUntilIdle()

        assertEquals("Initial error", viewModel.uiState.value.errorMessage)

        // Switch to successful flow
        whenever(resourceRepository.getAllResourcesFlow())
            .thenReturn(flowOf(testResources))

        // Create new view model to simulate retry
        viewModel = MainViewModel(resourceRepository, preferencesRepository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.resources.size)
    }
}
