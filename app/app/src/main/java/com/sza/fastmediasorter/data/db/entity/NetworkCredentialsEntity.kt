package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for storing network credentials securely.
 * Used for SMB, SFTP, and FTP connections.
 * 
 * NOTE: Passwords should be encrypted using EncryptedSharedPreferences
 * and only the reference stored here.
 */
@Entity(tableName = "network_credentials")
data class NetworkCredentialsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Unique identifier for referencing from ResourceEntity */
    val credentialId: String = UUID.randomUUID().toString(),
    
    /** Type: SMB, SFTP, FTP */
    val type: String,
    
    /** Server hostname or IP */
    val server: String,
    
    /** Port number */
    val port: Int,
    
    /** Username for authentication */
    val username: String,
    
    /** 
     * Password or key reference.
     * For security, actual passwords stored in EncryptedSharedPreferences.
     * This field stores a reference key.
     */
    val passwordRef: String,
    
    /** Windows domain (for SMB) */
    val domain: String = "",
    
    /** Share name (for SMB) */
    val shareName: String? = null,
    
    /** SSH key path (for SFTP) */
    val sshKeyPath: String? = null,
    
    /** Whether to use SSH key authentication (for SFTP) */
    val useSshKey: Boolean = false,
    
    /** Timestamp of creation */
    val createdDate: Long = System.currentTimeMillis()
)
