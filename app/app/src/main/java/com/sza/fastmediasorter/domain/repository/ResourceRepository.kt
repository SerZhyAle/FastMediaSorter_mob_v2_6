package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing resources.
 * Implements the Repository pattern from Clean Architecture.
 */
interface ResourceRepository {

    /**
     * Observes all resources as a Flow.
     * @return Flow of resource list, ordered by display order
     */
    fun getAllResourcesFlow(): Flow<List<Resource>>

    /**
     * Gets all resources synchronously.
     * @return List of all resources
     */
    suspend fun getAllResources(): List<Resource>

    /**
     * Gets a resource by its ID.
     * @param id The resource ID
     * @return The resource or null if not found
     */
    suspend fun getResourceById(id: Long): Resource?

    /**
     * Gets a resource by its path.
     * @param path The resource path
     * @return The resource or null if not found
     */
    suspend fun getResourceByPath(path: String): Resource?

    /**
     * Inserts a new resource.
     * @param resource The resource to insert
     * @return The generated ID
     */
    suspend fun insertResource(resource: Resource): Long

    /**
     * Updates an existing resource.
     * @param resource The resource to update
     */
    suspend fun updateResource(resource: Resource)

    /**
     * Deletes a resource.
     * @param resource The resource to delete
     */
    suspend fun deleteResource(resource: Resource)

    /**
     * Observes destinations (resources marked for move/copy) as a Flow.
     * @return Flow of destination resources
     */
    fun getDestinationsFlow(): Flow<List<Resource>>

    /**
     * Gets all destination resources synchronously.
     * @return List of destination resources
     */
    suspend fun getDestinations(): List<Resource>
}
