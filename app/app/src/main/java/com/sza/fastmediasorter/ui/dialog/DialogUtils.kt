package com.sza.fastmediasorter.ui.dialog

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.MediaItem

/**
 * Utility class for common dialog operations.
 * Provides convenient methods for showing standard dialogs.
 */
object DialogUtils {

    /**
     * Show a simple message dialog
     */
    fun showMessage(
        context: Context,
        title: String,
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> onDismiss?.invoke() }
            .show()
    }

    /**
     * Show a confirmation dialog
     */
    fun showConfirmation(
        context: Context,
        title: String?,
        message: String,
        positiveText: String? = null,
        negativeText: String? = null,
        isDestructive: Boolean = false,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(positiveText ?: context.getString(R.string.ok)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(negativeText ?: context.getString(R.string.cancel)) { _, _ ->
                onCancel?.invoke()
            }

        if (title != null) {
            builder.setTitle(title)
        }

        val dialog = builder.create()

        if (isDestructive) {
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                    context.getColor(R.color.md_theme_error)
                )
            }
        }

        dialog.show()
    }

    /**
     * Show a single-choice list dialog
     */
    fun <T> showSingleChoice(
        context: Context,
        title: String,
        items: List<T>,
        itemToString: (T) -> String = { it.toString() },
        selectedIndex: Int = -1,
        onSelect: (T) -> Unit
    ) {
        val itemStrings = items.map { itemToString(it) }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(itemStrings, selectedIndex) { dialog, which ->
                onSelect(items[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Show a multi-choice list dialog
     */
    fun <T> showMultiChoice(
        context: Context,
        title: String,
        items: List<T>,
        itemToString: (T) -> String = { it.toString() },
        selectedItems: Set<T> = emptySet(),
        onConfirm: (Set<T>) -> Unit
    ) {
        val itemStrings = items.map { itemToString(it) }.toTypedArray()
        val checkedItems = items.map { it in selectedItems }.toBooleanArray()
        val currentSelection = selectedItems.toMutableSet()

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMultiChoiceItems(itemStrings, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    currentSelection.add(items[which])
                } else {
                    currentSelection.remove(items[which])
                }
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                onConfirm(currentSelection)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Show an input dialog
     */
    fun showInput(
        context: Context,
        title: String,
        hint: String? = null,
        initialValue: String = "",
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT,
        onConfirm: (String) -> Unit
    ) {
        val editText = com.google.android.material.textfield.TextInputEditText(context).apply {
            setText(initialValue)
            this.hint = hint
            this.inputType = inputType
            selectAll()
        }

        val container = android.widget.FrameLayout(context).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(editText)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                onConfirm(editText.text?.toString() ?: "")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Show loading dialog
     */
    fun showLoading(
        fragmentManager: FragmentManager,
        message: String? = null
    ): MaterialProgressDialog {
        return MaterialProgressDialog.showLoading(fragmentManager, message)
    }

    /**
     * Show error dialog
     */
    fun showError(
        fragmentManager: FragmentManager,
        message: String,
        title: String? = null
    ) {
        ErrorDialogHelper.showError(fragmentManager, message, title)
    }

    /**
     * Show error dialog from exception
     */
    fun showException(
        fragmentManager: FragmentManager,
        exception: Throwable,
        userMessage: String? = null
    ) {
        ErrorDialogHelper.showException(fragmentManager, exception, userMessage)
    }
}

/**
 * Helper class for BrowseActivity dialogs.
 * Centralizes dialog creation and handling for the browse screen.
 */
class BrowseDialogHelper(
    private val fragmentManager: FragmentManager,
    private val context: Context
) {

    /**
     * Show sort options dialog
     */
    fun showSortOptions(
        currentSortBy: String,
        currentSortOrder: String,
        onSort: (sortBy: String, sortOrder: String) -> Unit
    ) {
        SortOptionsDialog.newInstance(currentSortBy, currentSortOrder).apply {
            onSortSelected = { sortBy, sortOrder ->
                onSort(sortBy, sortOrder)
            }
        }.show(fragmentManager, SortOptionsDialog.TAG)
    }

    /**
     * Show filter options dialog
     */
    fun showFilterOptions(
        showImages: Boolean,
        showVideos: Boolean,
        showGifs: Boolean,
        onFilter: (showImages: Boolean, showVideos: Boolean, showGifs: Boolean) -> Unit
    ) {
        FilterOptionsDialog.newInstance(showImages, showVideos, showGifs).apply {
            onFilterApplied = { images, videos, gifs ->
                onFilter(images, videos, gifs)
            }
        }.show(fragmentManager, FilterOptionsDialog.TAG)
    }

    /**
     * Show destination picker for copy/move operations
     */
    fun showDestinationPicker(
        operation: String,
        currentPath: String,
        onDestinationSelected: (String) -> Unit
    ) {
        DestinationPickerDialog.newInstance(
            title = if (operation == "copy") context.getString(R.string.copy_to)
                    else context.getString(R.string.move_to),
            currentPath = currentPath
        ).apply {
            onDestinationSelected = { path ->
                onDestinationSelected(path)
            }
        }.show(fragmentManager, DestinationPickerDialog.TAG)
    }

    /**
     * Show delete confirmation dialog
     */
    fun showDeleteConfirmation(
        itemCount: Int,
        onConfirm: (usePermanentDelete: Boolean, dontAskAgain: Boolean) -> Unit
    ) {
        DeleteConfirmationDialog.newInstance(itemCount).apply {
            onConfirm = { permanent, dontAsk ->
                onConfirm(permanent, dontAsk)
            }
        }.show(fragmentManager, DeleteConfirmationDialog.TAG)
    }

    /**
     * Show rename dialog
     */
    fun showRenameDialog(
        currentName: String,
        extension: String?,
        isDirectory: Boolean,
        existingNames: List<String>,
        onRename: (String) -> Unit
    ) {
        RenameDialog.newInstance(
            currentName = currentName,
            extension = extension,
            isDirectory = isDirectory,
            existingNames = existingNames
        ).apply {
            onRename = { newName ->
                onRename(newName)
            }
        }.show(fragmentManager, RenameDialog.TAG)
    }

    /**
     * Show file info dialog
     */
    fun showFileInfo(mediaItem: MediaItem) {
        FileInfoDialog.newInstance(mediaItem.uri).show(fragmentManager, FileInfoDialog.TAG)
    }

    /**
     * Show network credentials dialog
     */
    fun showNetworkCredentials(
        serverAddress: String?,
        onCredentialsEntered: (username: String, password: String, domain: String?) -> Unit
    ) {
        NetworkCredentialsDialog.newInstance(serverAddress).apply {
            onCredentialsEntered = { username, password, domain ->
                onCredentialsEntered(username, password, domain)
            }
        }.show(fragmentManager, NetworkCredentialsDialog.TAG)
    }

    /**
     * Show network discovery dialog
     */
    fun showNetworkDiscovery(
        onDeviceSelected: (NetworkDiscoveryDialog.NetworkDevice) -> Unit,
        onManualConnect: () -> Unit
    ) {
        NetworkDiscoveryDialog.newInstance().apply {
            onDeviceSelected = { device ->
                onDeviceSelected(device)
            }
            onManualConnect = {
                onManualConnect()
            }
        }.show(fragmentManager, NetworkDiscoveryDialog.TAG)
    }
}

/**
 * Helper class for PlayerActivity dialogs.
 * Centralizes dialog creation and handling for the player screen.
 */
class PlayerDialogHelper(
    private val fragmentManager: FragmentManager,
    private val context: Context
) {

    /**
     * Show player settings bottom sheet
     */
    fun showPlayerSettings(
        mediaType: com.sza.fastmediasorter.domain.model.MediaType?,
        slideshowInterval: Int,
        randomOrder: Boolean,
        touchZonesEnabled: Boolean,
        showZoneLabels: Boolean,
        videoSpeed: Float,
        repeatMode: PlayerSettingsDialog.RepeatMode,
        onSettingsChanged: PlayerSettingsListener
    ) {
        PlayerSettingsDialog.newInstance(
            mediaType = mediaType,
            slideshowInterval = slideshowInterval,
            randomOrder = randomOrder,
            touchZonesEnabled = touchZonesEnabled,
            showZoneLabels = showZoneLabels,
            videoSpeed = videoSpeed,
            repeatMode = repeatMode
        ).apply {
            onSlideshowIntervalChanged = onSettingsChanged::onSlideshowIntervalChanged
            onRandomOrderChanged = onSettingsChanged::onRandomOrderChanged
            onTouchZonesEnabledChanged = onSettingsChanged::onTouchZonesEnabledChanged
            onShowZoneLabelsChanged = onSettingsChanged::onShowZoneLabelsChanged
            onVideoSpeedChanged = onSettingsChanged::onVideoSpeedChanged
            onRepeatModeChanged = onSettingsChanged::onRepeatModeChanged
        }.show(fragmentManager, PlayerSettingsDialog.TAG)
    }

    /**
     * Show file info dialog
     */
    fun showFileInfo(uri: Uri) {
        FileInfoDialog.newInstance(uri).show(fragmentManager, FileInfoDialog.TAG)
    }

    /**
     * Show image edit dialog
     */
    fun showImageEditor(
        imageUri: Uri,
        onSave: (editedBitmap: android.graphics.Bitmap, saveAsCopy: Boolean) -> Unit
    ) {
        ImageEditDialog.newInstance(imageUri).apply {
            onSave = { bitmap, asCopy ->
                onSave(bitmap, asCopy)
            }
        }.show(fragmentManager, ImageEditDialog.TAG)
    }

    /**
     * Show GIF editor dialog
     */
    fun showGifEditor(
        gifUri: Uri,
        onSave: (GifEditorDialog.GifSettings) -> Unit
    ) {
        GifEditorDialog.newInstance(gifUri).apply {
            onSave = { settings ->
                onSave(settings)
            }
        }.show(fragmentManager, GifEditorDialog.TAG)
    }

    /**
     * Show destination picker for copy/move
     */
    fun showDestinationPicker(
        operation: String,
        currentPath: String,
        onDestinationSelected: (String) -> Unit
    ) {
        DestinationPickerDialog.newInstance(
            title = if (operation == "copy") context.getString(R.string.copy_to)
            else context.getString(R.string.move_to),
            currentPath = currentPath
        ).apply {
            onDestinationSelected = { path ->
                onDestinationSelected(path)
            }
        }.show(fragmentManager, DestinationPickerDialog.TAG)
    }

    /**
     * Show delete confirmation
     */
    fun showDeleteConfirmation(
        onConfirm: (usePermanentDelete: Boolean, dontAskAgain: Boolean) -> Unit
    ) {
        DeleteConfirmationDialog.newInstance(1).apply {
            onConfirm = { permanent, dontAsk ->
                onConfirm(permanent, dontAsk)
            }
        }.show(fragmentManager, DeleteConfirmationDialog.TAG)
    }

    /**
     * Show rename dialog
     */
    fun showRenameDialog(
        currentName: String,
        extension: String?,
        existingNames: List<String>,
        onRename: (String) -> Unit
    ) {
        RenameDialog.newInstance(
            currentName = currentName,
            extension = extension,
            isDirectory = false,
            existingNames = existingNames
        ).apply {
            onRename = { newName ->
                onRename(newName)
            }
        }.show(fragmentManager, RenameDialog.TAG)
    }

    interface PlayerSettingsListener {
        fun onSlideshowIntervalChanged(interval: Int)
        fun onRandomOrderChanged(random: Boolean)
        fun onTouchZonesEnabledChanged(enabled: Boolean)
        fun onShowZoneLabelsChanged(show: Boolean)
        fun onVideoSpeedChanged(speed: Float)
        fun onRepeatModeChanged(mode: PlayerSettingsDialog.RepeatMode)
    }
}
