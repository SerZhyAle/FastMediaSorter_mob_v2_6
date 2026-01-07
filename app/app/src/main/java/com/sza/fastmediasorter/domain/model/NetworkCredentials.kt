package com.sza.fastmediasorter.domain.model

/**
 * Domain model representing network credentials.
 * 
 * Used for SMB, SFTP, and FTP connections.
 */
data class NetworkCredentials(
    /** Unique credential identifier (UUID) */
    val credentialId: String,
    
    /** Network type: SMB, SFTP, FTP */
    val type: NetworkType,
    
    /** Server hostname or IP address */
    val server: String,
    
    /** Port number */
    val port: Int,
    
    /** Username for authentication */
    val username: String,
    
    /** Password (encrypted in storage) */
    val password: String,
    
    /** Windows domain (for SMB, empty if not applicable) */
    val domain: String = "",
    
    /** Share name (for SMB, null if not applicable) */
    val shareName: String? = null,
    
    /** SSH private key path (for SFTP, null if password auth) */
    val sshKeyPath: String? = null,
    
    /** Whether to use SSH key authentication (for SFTP) */
    val useSshKey: Boolean = false
) {
    /**
     * Get default port based on network type.
     */
    fun getDefaultPort(): Int = when (type) {
        NetworkType.SMB -> 445
        NetworkType.SFTP -> 22
        NetworkType.FTP -> 21
        else -> port
    }

    /**
     * Format server address with port.
     */
    fun getServerAddress(): String {
        return if (port == getDefaultPort()) {
            server
        } else {
            "$server:$port"
        }
    }
}

/**
 * Enum representing network protocol types.
 */
enum class NetworkType {
    LOCAL,
    SMB,
    SFTP,
    FTP,
    GOOGLE_DRIVE,
    ONEDRIVE,
    DROPBOX
}
