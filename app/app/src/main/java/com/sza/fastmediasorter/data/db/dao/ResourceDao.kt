package com.sza.fastmediasorter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sza.fastmediasorter.data.db.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ResourceEntity.
 */
@Dao
interface ResourceDao {

    @Query("SELECT * FROM resources ORDER BY displayOrder, name")
    fun getAllFlow(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources ORDER BY displayOrder, name")
    suspend fun getAll(): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getById(id: Long): ResourceEntity?

    @Query("SELECT * FROM resources WHERE path = :path")
    suspend fun getByPath(path: String): ResourceEntity?

    @Insert
    suspend fun insert(resource: ResourceEntity): Long

    @Update
    suspend fun update(resource: ResourceEntity)

    @Delete
    suspend fun delete(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM resources WHERE isDestination = 1 ORDER BY destinationOrder")
    fun getDestinationsFlow(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE isDestination = 1 ORDER BY destinationOrder")
    suspend fun getDestinations(): List<ResourceEntity>

    @Query("UPDATE resources SET lastAccessedDate = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT MAX(displayOrder) FROM resources")
    suspend fun getMaxDisplayOrder(): Int?

    @Query("SELECT MAX(destinationOrder) FROM resources WHERE isDestination = 1")
    suspend fun getMaxDestinationOrder(): Int?
}
