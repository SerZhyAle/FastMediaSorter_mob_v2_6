package com.sza.fastmediasorter.ui.player

import timber.log.Timber

/**
 * Detects which touch zone was activated based on screen coordinates.
 * Supports both 3x3 (9-zone) and 2-zone layouts.
 */
class TouchZoneDetector {

    companion object {
        // Zone boundary percentages for 3x3 grid (30%, 70%)
        private const val LEFT_BOUNDARY = 0.3f
        private const val RIGHT_BOUNDARY = 0.7f
        private const val TOP_BOUNDARY = 0.3f
        private const val BOTTOM_BOUNDARY = 0.7f

        // Zone boundary for 2-zone layout
        private const val HALF_BOUNDARY = 0.5f
    }

    /**
     * Detect which 9-zone grid cell was touched.
     * 
     * @param x Touch X coordinate
     * @param y Touch Y coordinate
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels (or active area height)
     * @return TouchZone that was touched
     */
    fun detectZone9(x: Float, y: Float, screenWidth: Int, screenHeight: Int): TouchZone {
        val leftBoundary = screenWidth * LEFT_BOUNDARY
        val rightBoundary = screenWidth * RIGHT_BOUNDARY
        val topBoundary = screenHeight * TOP_BOUNDARY
        val bottomBoundary = screenHeight * BOTTOM_BOUNDARY

        // Determine row (top=0, middle=1, bottom=2)
        val row = when {
            y < topBoundary -> 0
            y < bottomBoundary -> 1
            else -> 2
        }

        // Determine column (left=0, center=1, right=2)
        val column = when {
            x < leftBoundary -> 0
            x < rightBoundary -> 1
            else -> 2
        }

        val zone = when (row) {
            0 -> when (column) {
                0 -> TouchZone.BACK
                1 -> TouchZone.COPY
                2 -> TouchZone.RENAME
                else -> TouchZone.NONE
            }
            1 -> when (column) {
                0 -> TouchZone.PREVIOUS
                1 -> TouchZone.MOVE
                2 -> TouchZone.NEXT
                else -> TouchZone.NONE
            }
            2 -> when (column) {
                0 -> TouchZone.COMMAND_PANEL
                1 -> TouchZone.DELETE
                2 -> TouchZone.SLIDESHOW
                else -> TouchZone.NONE
            }
            else -> TouchZone.NONE
        }

        Timber.d("TouchZoneDetector: x=$x, y=$y -> row=$row, col=$column -> zone=$zone")
        return zone
    }

    /**
     * Detect which 2-zone layout cell was touched (left/right split).
     * Used for video/PDF with controls at bottom.
     * 
     * @param x Touch X coordinate
     * @param screenWidth Screen width in pixels
     * @return TouchZone2 that was touched
     */
    fun detectZone2(x: Float, screenWidth: Int): TouchZone2 {
        val halfBoundary = screenWidth * HALF_BOUNDARY

        val zone = if (x < halfBoundary) TouchZone2.PREVIOUS else TouchZone2.NEXT

        Timber.d("TouchZoneDetector (2-zone): x=$x -> zone=$zone")
        return zone
    }

    /**
     * Detect 2-zone layout with optional top/bottom restriction.
     * Used when only part of the screen is active for touch zones.
     * 
     * @param x Touch X coordinate
     * @param y Touch Y coordinate
     * @param screenWidth Screen width in pixels
     * @param activeAreaTop Top of active area (0 for full screen)
     * @param activeAreaBottom Bottom of active area (screenHeight for full screen)
     * @return TouchZone2 if in active area, NONE otherwise
     */
    fun detectZone2WithBounds(
        x: Float,
        y: Float,
        screenWidth: Int,
        activeAreaTop: Float,
        activeAreaBottom: Float
    ): TouchZone2 {
        // Check if touch is within active area
        if (y < activeAreaTop || y > activeAreaBottom) {
            return TouchZone2.NONE
        }

        return detectZone2(x, screenWidth)
    }

    /**
     * Get screen coordinates for zone boundaries (for visual overlay).
     */
    fun getZoneBoundaries(screenWidth: Int, screenHeight: Int): ZoneBoundaries {
        return ZoneBoundaries(
            leftBoundary = screenWidth * LEFT_BOUNDARY,
            rightBoundary = screenWidth * RIGHT_BOUNDARY,
            topBoundary = screenHeight * TOP_BOUNDARY,
            bottomBoundary = screenHeight * BOTTOM_BOUNDARY
        )
    }

    /**
     * Data class holding zone boundary coordinates.
     */
    data class ZoneBoundaries(
        val leftBoundary: Float,
        val rightBoundary: Float,
        val topBoundary: Float,
        val bottomBoundary: Float
    )
}
