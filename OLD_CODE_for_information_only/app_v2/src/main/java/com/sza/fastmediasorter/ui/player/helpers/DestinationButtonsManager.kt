package com.sza.fastmediasorter.ui.player

import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.button.MaterialButton
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.usecase.GetDestinationsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages destination buttons in PlayerActivity:
 * - Populates Copy/Move grids with destination buttons
 * - Calculates button distribution (5x2, 4x3, 3x3 layouts)
 * - Handles panel collapse/expand toggle
 * - Persists collapsed state to settings
 */
class DestinationButtonsManager(
    private val binding: ActivityPlayerUnifiedBinding,
    private val settingsRepository: SettingsRepository,
    private val getDestinationsUseCase: GetDestinationsUseCase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val callback: DestinationButtonsCallback
) {
    
    // Cache settings to avoid repeated reads
    private var cachedCopyCollapsed: Boolean? = null
    private var cachedMoveCollapsed: Boolean? = null
    private var cachedMaxRecipients: Int? = null
    
    interface DestinationButtonsCallback {
        fun onCopyClicked(destination: MediaResource)
        fun onMoveClicked(destination: MediaResource)
        fun getCurrentResourceId(): Long
        fun onUpdateCommandAvailability()
    }
    
    /**
     * Populate destination buttons for Copy/Move panels
     * Excludes current resource, reads collapsed state from settings
     */
    fun populateDestinationButtons() {
        val resourceId = callback.getCurrentResourceId()
        
        lifecycleScope.launch {
            try {
                val destinations = getDestinationsUseCase().first()
                    .filter { it.id != resourceId } // Exclude current resource
                
                // Read collapsed state and max recipients from settings (cached)
                if (cachedCopyCollapsed == null || cachedMoveCollapsed == null || cachedMaxRecipients == null) {
                    val settings = settingsRepository.getSettings().first()
                    cachedCopyCollapsed = settings.copyPanelCollapsed
                    cachedMoveCollapsed = settings.movePanelCollapsed
                    cachedMaxRecipients = settings.maxRecipients
                    Timber.d("DestinationButtonsManager: READ and CACHED settings - copyPanelCollapsed=$cachedCopyCollapsed, movePanelCollapsed=$cachedMoveCollapsed, maxRecipients=$cachedMaxRecipients")
                }
                val copyCollapsed = cachedCopyCollapsed!!
                val moveCollapsed = cachedMoveCollapsed!!
                val maxRecipients = cachedMaxRecipients!!
                
                // Clear existing buttons
                binding.copyToButtonsGrid.removeAllViews()
                binding.moveToButtonsGrid.removeAllViews()
                
                val destinationsList = destinations.take(maxRecipients)
                val count = destinationsList.size
                
                // Calculate button distribution across rows
                // If limit > 10 (user explicitly increased it), allow up to 15 buttons per row
                // Otherwise keep the default behavior (max 5 per row) to preserve UI for majority
                val maxPerRow = if (maxRecipients > 10) 15 else 5
                
                val numRows = if (count > 0) kotlin.math.ceil(count.toFloat() / maxPerRow).toInt() else 1
                val basePerCol = if (numRows > 0) count / numRows else 0
                val remainder = if (numRows > 0) count % numRows else 0
                
                val distribution = mutableListOf<Int>()
                repeat(numRows) { i ->
                    val extra = if (i < remainder) 1 else 0
                    distribution.add(basePerCol + extra)
                }
                
                // Create rows for Copy panel
                var destIndex = 0
                distribution.forEach { rowCount ->
                    if (rowCount > 0) {
                        val rowLayout = createButtonRow()
                        repeat(rowCount) {
                            if (destIndex < destinationsList.size) {
                                val btn = createDestinationButton(destinationsList[destIndex], destIndex, true, rowCount)
                                rowLayout.addView(btn)
                                destIndex++
                            }
                        }
                        binding.copyToButtonsGrid.addView(rowLayout)
                    }
                }
                
                // Create rows for Move panel
                destIndex = 0
                distribution.forEach { rowCount ->
                    if (rowCount > 0) {
                        val rowLayout = createButtonRow()
                        repeat(rowCount) {
                            if (destIndex < destinationsList.size) {
                                val btn = createDestinationButton(destinationsList[destIndex], destIndex, false, rowCount)
                                rowLayout.addView(btn)
                                destIndex++
                            }
                        }
                        binding.moveToButtonsGrid.addView(rowLayout)
                    }
                }
                
                // Hide panels if no destination buttons created
                val hasDestinations = destinations.isNotEmpty()
                if (!hasDestinations) {
                    binding.copyToPanel.isVisible = false
                    binding.moveToPanel.isVisible = false
                } else {
                    // Restore collapsed state AFTER adding buttons
                    Timber.d("DestinationButtonsManager: RESTORING panel states - copy=$copyCollapsed, move=$moveCollapsed")
                    updateCopyPanelVisibility(copyCollapsed)
                    updateMovePanelVisibility(moveCollapsed)
                }
                
                // Update command availability to refresh panel visibility
                callback.onUpdateCommandAvailability()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load destinations")
                Toast.makeText(binding.root.context, R.string.toast_failed_to_load_destinations, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Create horizontal LinearLayout for button row
     */
    private fun createButtonRow(): LinearLayout {
        return LinearLayout(binding.root.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }
    }
    
    /**
     * Clear cached settings - call when settings change
     */
    fun clearSettingsCache() {
        cachedCopyCollapsed = null
        cachedMoveCollapsed = null
        cachedMaxRecipients = null
        Timber.d("DestinationButtonsManager: Settings cache cleared")
    }
    
    /**
     * Create destination button with short name, color, click handler
     */
    private fun createDestinationButton(
        destination: MediaResource,
        @Suppress("UNUSED_PARAMETER") index: Int,
        isCopy: Boolean,
        buttonsInRow: Int
    ): MaterialButton {
        return MaterialButton(binding.root.context).apply {
            // Short name - take first 8 characters or first word
            val shortName = when {
                destination.name.length <= 10 -> destination.name
                destination.name.contains(" ") -> destination.name.substringBefore(" ").take(10)
                else -> destination.name.take(8) + ".."
            }
            text = shortName
            
            // Compact text size for destination buttons
            textSize = 12f
            
            // Calculate brightness to determine text color
            val color = destination.destinationColor
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val brightness = kotlin.math.sqrt(
                0.299 * r * r +
                0.587 * g * g +
                0.114 * b * b
            )
            setTextColor(if (brightness > 130) Color.BLACK else Color.WHITE)
            
            // Set button color from destination
            setBackgroundColor(destination.destinationColor)
            
            // Rounded rectangle buttons with equal weight distribution
            val density = resources.displayMetrics.density
            val buttonHeight = (56 * density).toInt() // Standard Material height
            val cornerRadius = (12 * density).toInt() // Rounded corners
            
            // Apply rounded corners
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(cornerRadius.toFloat())
                .build()
            
            minWidth = 0 // Allow shrinking
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                buttonHeight,
                1f // Equal weight for all buttons in row
            ).apply {
                setMargins(2, 2, 2, 2)
            }
            
            // Center text and allow wrapping
            gravity = Gravity.CENTER
            maxLines = 2
            
            setOnClickListener {
                Timber.d("DestinationButtonsManager: Destination button clicked - ${destination.name}, isCopy=$isCopy")
                if (isCopy) {
                    callback.onCopyClicked(destination)
                } else {
                    callback.onMoveClicked(destination)
                }
            }
        }
    }
    
    /**
     * Toggle Copy to panel collapsed/expanded state
     */
    fun toggleCopyPanel() {
        lifecycleScope.launch {
            val currentSettings = settingsRepository.getSettings().first()
            val newCollapsedState = !currentSettings.copyPanelCollapsed
            
            // Save new state
            settingsRepository.updateSettings(currentSettings.copy(copyPanelCollapsed = newCollapsedState))
            
            // Clear cache to force reload on next populateDestinationButtons()
            cachedCopyCollapsed = newCollapsedState
            
            // Update UI
            updateCopyPanelVisibility(newCollapsedState)
        }
    }
    
    /**
     * Toggle Move to panel collapsed/expanded state
     */
    fun toggleMovePanel() {
        lifecycleScope.launch {
            val currentSettings = settingsRepository.getSettings().first()
            val newCollapsedState = !currentSettings.movePanelCollapsed
            
            // Save new state
            settingsRepository.updateSettings(currentSettings.copy(movePanelCollapsed = newCollapsedState))
            
            // Clear cache to force reload on next populateDestinationButtons()
            cachedMoveCollapsed = newCollapsedState
            
            // Update UI
            updateMovePanelVisibility(newCollapsedState)
        }
    }
    
    /**
     * Update Copy to panel buttons visibility and indicator
     */
    private fun updateCopyPanelVisibility(collapsed: Boolean) {
        binding.copyToButtonsGrid.isVisible = !collapsed
        binding.copyToPanelIndicator.text = if (collapsed) "▶" else "▼"
    }
    
    /**
     * Update Move to panel buttons visibility and indicator
     */
    private fun updateMovePanelVisibility(collapsed: Boolean) {
        binding.moveToButtonsGrid.isVisible = !collapsed
        binding.moveToPanelIndicator.text = if (collapsed) "▶" else "▼"
    }
}
