package com.sza.fastmediasorter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sza.fastmediasorter.data.db.entity.NetworkCredentialsEntity

/**
 * Data Access Object for NetworkCredentialsEntity.
 */
@Dao
interface NetworkCredentialsDao {

    @Query("SELECT * FROM network_credentials WHERE credentialId = :credentialId")
    suspend fun getByCredentialId(credentialId: String): NetworkCredentialsEntity?

    @Query("SELECT * FROM network_credentials WHERE id = :id")
    suspend fun getById(id: Long): NetworkCredentialsEntity?

    @Insert
    suspend fun insert(credentials: NetworkCredentialsEntity): Long

    @Update
    suspend fun update(credentials: NetworkCredentialsEntity)

    @Delete
    suspend fun delete(credentials: NetworkCredentialsEntity)

    @Query("DELETE FROM network_credentials WHERE credentialId = :credentialId")
    suspend fun deleteByCredentialId(credentialId: String)
}
