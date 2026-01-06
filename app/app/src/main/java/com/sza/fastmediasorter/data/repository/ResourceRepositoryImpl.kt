package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.db.dao.ResourceDao
import com.sza.fastmediasorter.data.db.entity.ResourceEntity
import com.sza.fastmediasorter.domain.model.DisplayMode
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.model.SortMode
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ResourceRepository.
 * Bridges the data layer (Room) with the domain layer.
 */
@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val resourceDao: ResourceDao
) : ResourceRepository {

    override fun getAllResourcesFlow(): Flow<List<Resource>> {
        return resourceDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllResources(): List<Resource> {
        return resourceDao.getAll().map { it.toDomain() }
    }

    override suspend fun getResourceById(id: Long): Resource? {
        return resourceDao.getById(id)?.toDomain()
    }

    override suspend fun getResourceByPath(path: String): Resource? {
        return resourceDao.getByPath(path)?.toDomain()
    }

    override suspend fun insertResource(resource: Resource): Long {
        val maxOrder = resourceDao.getMaxDisplayOrder() ?: 0
        val entity = resource.toEntity().copy(displayOrder = maxOrder + 1)
        return resourceDao.insert(entity)
    }

    override suspend fun updateResource(resource: Resource) {
        resourceDao.update(resource.toEntity())
    }

    override suspend fun deleteResource(resource: Resource) {
        resourceDao.deleteById(resource.id)
    }

    override fun getDestinationsFlow(): Flow<List<Resource>> {
        return resourceDao.getDestinationsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDestinations(): List<Resource> {
        return resourceDao.getDestinations().map { it.toDomain() }
    }

    // Extension functions for mapping between Entity and Domain models

    private fun ResourceEntity.toDomain(): Resource {
        return Resource(
            id = id,
            name = name,
            path = path,
            type = ResourceType.valueOf(type),
            sortMode = SortMode.valueOf(sortMode),
            displayMode = DisplayMode.valueOf(displayMode),
            isDestination = isDestination,
            destinationOrder = destinationOrder,
            destinationColor = destinationColor,
            workWithAllFiles = workWithAllFiles
        )
    }

    private fun Resource.toEntity(): ResourceEntity {
        return ResourceEntity(
            id = id,
            name = name,
            path = path,
            type = type.name,
            sortMode = sortMode.name,
            displayMode = displayMode.name,
            isDestination = isDestination,
            destinationOrder = destinationOrder,
            destinationColor = destinationColor,
            workWithAllFiles = workWithAllFiles
        )
    }
}
