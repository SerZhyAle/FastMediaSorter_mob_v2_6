package com.sza.fastmediasorter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sza.fastmediasorter.data.db.entity.FileOperationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FileOperationHistoryEntity.
 */
@Dao
interface FileOperationHistoryDao {

    @Query("SELECT * FROM file_operation_history WHERE canUndo = 1 AND isUndone = 0 ORDER BY timestamp DESC")
    fun getUndoableOperationsFlow(): Flow<List<FileOperationHistoryEntity>>

    @Query("SELECT * FROM file_operation_history WHERE canUndo = 1 AND isUndone = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastUndoableOperation(): FileOperationHistoryEntity?

    @Query("SELECT * FROM file_operation_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOperations(limit: Int = 50): List<FileOperationHistoryEntity>

    @Insert
    suspend fun insert(operation: FileOperationHistoryEntity): Long

    @Update
    suspend fun update(operation: FileOperationHistoryEntity)

    @Delete
    suspend fun delete(operation: FileOperationHistoryEntity)

    @Query("UPDATE file_operation_history SET isUndone = 1 WHERE id = :id")
    suspend fun markAsUndone(id: Long)

    @Query("DELETE FROM file_operation_history WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM file_operation_history WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
