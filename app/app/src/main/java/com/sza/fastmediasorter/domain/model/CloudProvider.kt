package com.sza.fastmediasorter.domain.model

/**
 * Cloud storage provider types
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    ONEDRIVE,
    DROPBOX;
    
    companion object {
        fun fromString(value: String?): CloudProvider? {
            return when (value?.uppercase()) {
                "GOOGLE_DRIVE" -> GOOGLE_DRIVE
                "ONEDRIVE" -> ONEDRIVE
                "DROPBOX" -> DROPBOX
                else -> null
            }
        }
    }
}
