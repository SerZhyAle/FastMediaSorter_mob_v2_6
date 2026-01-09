package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.db.dao.ResourceDao
import com.sza.fastmediasorter.data.db.entity.ResourceEntity
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.SortMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceRepositoryImplTest {

    private lateinit var resourceDao: ResourceDao
    private lateinit var repository: ResourceRepositoryImpl

    private val entity1 = ResourceEntity(
        id = 1L,
        name = "Photos",
        path = "/storage/Photos",
        type = "LOCAL",
        sortMode = "DATE_DESC",
        displayMode = "GRID",
        isDestination = false,
        workWithAllFiles = false
    )

    private val entity2 = ResourceEntity(
        id = 2L,
        name = "Videos",
        path = "/storage/Videos",
        type = "LOCAL",
        sortMode = "NAME_ASC",
        displayMode = "LIST",
        isDestination = true,
        workWithAllFiles = false
    )

    @Before
    fun setup() {
        resourceDao = mock()
        repository = ResourceRepositoryImpl(resourceDao)
    }

    @Test
    fun `getAllResourcesFlow returns mapped domain models`() = runTest {
        whenever(resourceDao.getAllFlow()).thenReturn(flowOf(listOf(entity1, entity2)))

        val result = repository.getAllResourcesFlow().first()

        assertEquals(2, result.size)
        assertEquals("Photos", result[0].name)
        assertEquals(ResourceType.LOCAL, result[0].type)
        assertEquals("Videos", result[1].name)
    }

    @Test
    fun `getAllResources returns mapped list`() = runTest {
        whenever(resourceDao.getAll()).thenReturn(listOf(entity1, entity2))

        val result = repository.getAllResources()

        assertEquals(2, result.size)
        assertEquals("Photos", result[0].name)
    }

    @Test
    fun `getResourceById returns domain model when found`() = runTest {
        whenever(resourceDao.getById(1L)).thenReturn(entity1)

        val result = repository.getResourceById(1L)

        assertNotNull(result)
        assertEquals("Photos", result?.name)
        assertEquals(ResourceType.LOCAL, result?.type)
    }

    @Test
    fun `getResourceById returns null when not found`() = runTest {
        whenever(resourceDao.getById(999L)).thenReturn(null)

        val result = repository.getResourceById(999L)

        assertNull(result)
    }

    @Test
    fun `getResourceByPath returns domain model when found`() = runTest {
        whenever(resourceDao.getByPath("/storage/Photos")).thenReturn(entity1)

        val result = repository.getResourceByPath("/storage/Photos")

        assertNotNull(result)
        assertEquals("Photos", result?.name)
    }

    @Test
    fun `insertResource assigns display order and returns id`() = runTest {
        val resource = Resource(
            id = 0L,
            name = "New Folder",
            path = "/storage/New",
            type = ResourceType.LOCAL
        )
        whenever(resourceDao.getMaxDisplayOrder()).thenReturn(5)
        whenever(resourceDao.insert(any())).thenReturn(10L)

        val id = repository.insertResource(resource)

        assertEquals(10L, id)
        verify(resourceDao).insert(any())
    }

    @Test
    fun `updateResource delegates to dao`() = runTest {
        val resource = Resource(
            id = 1L,
            name = "Updated",
            path = "/storage/Updated",
            type = ResourceType.LOCAL
        )

        repository.updateResource(resource)

        verify(resourceDao).update(any())
    }

    @Test
    fun `deleteResource delegates to dao`() = runTest {
        val resource = Resource(
            id = 1L,
            name = "Photos",
            path = "/storage/Photos",
            type = ResourceType.LOCAL
        )

        repository.deleteResource(resource)

        verify(resourceDao).deleteById(1L)
    }

    @Test
    fun `getDestinationsFlow returns only destinations`() = runTest {
        whenever(resourceDao.getDestinationsFlow()).thenReturn(flowOf(listOf(entity2)))

        val result = repository.getDestinationsFlow().first()

        assertEquals(1, result.size)
        assertEquals("Videos", result[0].name)
        assertEquals(true, result[0].isDestination)
    }

    @Test
    fun `getDestinations returns destination list`() = runTest {
        whenever(resourceDao.getDestinations()).thenReturn(listOf(entity2))

        val result = repository.getDestinations()

        assertEquals(1, result.size)
        assertEquals(true, result[0].isDestination)
    }
}
