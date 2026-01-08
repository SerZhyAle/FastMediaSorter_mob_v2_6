package com.sza.fastmediasorter.ui.player

/**
 * Touch zones for static image/audio in full-screen mode (3x3 grid).
 * Each zone maps to a specific action in the player.
 * 
 * Zone Layout:
 * ```
 * +----------+----------+----------+
 * |   BACK   |   COPY   |  RENAME  |
 * +----------+----------+----------+
 * | PREVIOUS |   MOVE   |   NEXT   |
 * +----------+----------+----------+
 * |  PANEL   |  DELETE  | SLIDESHOW|
 * +----------+----------+----------+
 * ```
 */
enum class TouchZone {
    BACK,           // Top-left 30x30% - Navigate back
    COPY,           // Top-center 40x30% - Copy to destination
    RENAME,         // Top-right 30x30% - Rename file
    PREVIOUS,       // Middle-left 30x40% - Previous file
    MOVE,           // Center 40x40% - Move to destination
    NEXT,           // Middle-right 30x40% - Next file
    COMMAND_PANEL,  // Bottom-left 30x30% - Toggle command panel
    DELETE,         // Bottom-center 40x30% - Delete file
    SLIDESHOW,      // Bottom-right 30x30% - Toggle slideshow
    NONE;           // Outside zones or disabled

    companion object {
        /**
         * Get user-friendly description for zone action.
         */
        fun getDescription(zone: TouchZone): String = when (zone) {
            BACK -> "Back to Browse"
            COPY -> "Copy file"
            RENAME -> "Rename file"
            PREVIOUS -> "Previous file"
            MOVE -> "Move file"
            NEXT -> "Next file"
            COMMAND_PANEL -> "Toggle command panel"
            DELETE -> "Delete file"
            SLIDESHOW -> "Toggle slideshow"
            NONE -> "No action"
        }

        /**
         * Check if zone performs a destructive action that may require confirmation.
         */
        fun isDestructive(zone: TouchZone): Boolean = 
            zone == DELETE || zone == MOVE
    }
}

/**
 * Alternative zone layout for video/PDF with controls at bottom.
 * Uses 2-zone horizontal layout for left (previous) and right (next).
 */
enum class TouchZone2 {
    PREVIOUS,  // Left half - Previous file
    NEXT,      // Right half - Next file
    NONE;      // Disabled

    companion object {
        fun getDescription(zone: TouchZone2): String = when (zone) {
            PREVIOUS -> "Previous file"
            NEXT -> "Next file"
            NONE -> "No action"
        }
    }
}
