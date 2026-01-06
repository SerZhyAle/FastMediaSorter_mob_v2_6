# 02b. BrowseActivity Logic

## Overview

`BrowseActivity` manages the file browsing experience for a specific `MediaResource`. It acts as a "Decomposed God Class," delegating specific responsibilities to specialized Manager classes while acting as the central coordination point.

**File Size**: 1363 lines (decomposed from original 3000+ line God Class)
**Architecture**: MVVM + Manager Delegation Pattern

## Components

### 1. Managers (Delegation Pattern)

To avoid a massive Activity class, logic is split into 9 specialized managers:

- **`BrowseDialogHelper`**: Manages all dialog interactions (Filter, Sort, Rename, Copy/Move, Delete). Provides unified callbacks interface.
- **`BrowseRecyclerViewManager`**: Handles LayoutManager switching (Grid/List), Scroll listeners, and Adapter configuration. Manages RecycledViewPool optimization.
- **`BrowseMediaStoreObserver`**: Local file system monitoring (Android MediaStore) for automatic refresh when files change.
- **`BrowseFileOperationsManager`**: Coordinates complex file ops (Copy/Move/Delete/Rename) via FileOperationUseCase. Handles network file download-edit-upload pattern.
- **`BrowseSmallControlsManager`**: Optional compact UI mode (reduces button heights from 48dp to 32dp for landscape/small screens).
- **`BrowseCloudAuthManager`**: Handles OAuth flows for Google Drive, OneDrive, Dropbox. Manages token refresh and re-authentication.
- **`BrowseUtilityManager`**: Helper functions (Share, MediaInfo display, Resource info formatting).
- **`BrowseStateManager`**: Scroll position persistence, Last viewed file tracking.
- **`KeyboardNavigationManager`**: Hardware keyboard shortcuts (Arrows, Enter, Space, Delete, Ctrl+A, F2, Escape).

**Manager Initialization**: All managers initialized in `setupViews()` with callback interfaces for bi-directional communication with Activity.

### 2. ViewModel (BrowseViewModel)

- **Role:** Holds `BrowseState`, survives configuration changes, communicates with Repository layer.
- **Key Logic:**
  - Resource Loading & Caching via `UnifiedFileCache`.
  - State aggregations (Filtering + Sorting).
  - Undo stack persistence (5-minute expiration).
  - Deduplication of file lists (prevents redundant submitList calls).
  - Background thumbnail cancellation (frees bandwidth for PlayerActivity).

---

## State Management (`BrowseState`)

```kotlin
data class BrowseState(
    val resource: MediaResource?,
    val mediaFiles: List<MediaFile>,
    val selectedFiles: Set<String>,       // Paths of selected files
    val sortMode: SortMode,               // NAME_ASC, DATE_DESC, SIZE_ASC, etc.
    val displayMode: DisplayMode,         // LIST vs GRID_2COL vs GRID_3COL vs GRID_4COL
    val filter: FileFilter?,              // Active filter criteria
    val lastOperation: UndoOperation?,    // For Undo button (5-minute expiration)
    val loadingProgress: Int,             // Files scanned so far (0-100%)
    val isScanCancellable: Boolean,       // Shows STOP button after 5s
    val showSmallControls: Boolean        // Compact UI mode (32dp buttons vs 48dp)
)
```

**State Flow:**

1. `onCreate` → `viewModel.init{}` triggers `GetMediaFilesUseCase`.
2. UseCases fetch files: Network/Local/Cache (UnifiedFileCache).
3. Files filtered (in-memory via FileFilter) → Sorted (SortMode) → Deduplicated.
4. State updated → `observeData()` in Activity collects changes.
5. `mediaFileAdapter.submitList(files)` renders UI (with diff calculation).

**Deduplication Logic**: ViewModel tracks `lastEmittedMediaFiles` to avoid redundant `submitList()` calls when state updates but file list unchanged (e.g., selection change, sort mode change without re-sorting).

---

## Key Logic Flows

### 1. Resource Loading Workflow

**Entry Point**: `onCreate` → `viewModel.init{ loadResource(resourceId) }`

**Steps**:

1. **Intent Extra Parsing**:
   - `resourceId` (Long): Primary identifier.
   - `skipAvailabilityCheck` (Boolean): If true, skip network ping (for Favorites).
   
2. **Resource Validation**:
   - Fetch `MediaResource` from DB via `GetResourceByIdUseCase`.
   - If null → Show error "Resource not found".
   - If network resource (SMB/SFTP/FTP/Cloud) AND !skipAvailabilityCheck:
     - Ping resource (15s timeout).
     - If unreachable → Show connection error + Retry button.

3. **Cache Check**:
   - Query `UnifiedFileCache.get(resourcePath)`.
   - If valid (not expired, file count matches DB) → Return Cached List (instant load).
   - If invalid/stale → Proceed to Network/Disk scan.

4. **File Scanning**:
   - **Local**: `File.listFiles()` via `LocalMediaScanner`.
   - **SMB**: `smbClient.listFiles(path)` with credentials.
   - **SFTP**: `sftpClient.listFiles(path)` with SSH key/password.
   - **FTP**: `ftpClient.listFiles(path)` with PASV/Active mode fallback.
   - **Cloud** (Google Drive/OneDrive/Dropbox):
     - Check `isAuthenticated()`.
     - If No → Emit `ShowCloudAuthenticationRequired` event.
     - If Yes → API call with OAuth token.
   - **Folders** (if `pref_show_folders` enabled):
     - Include `MediaType.FOLDER` items in scanning.
     - Derived from subdirectories in the current path.
   - **Favorites** (resourceId == -100L):
     - Query all files with `isFavorite = true` from MediaFilesCache table.
     - Aggregate from all resources.

5. **Progress Updates**:
   - Emit `loadingProgress` every 100ms (file count).
   - After 5 seconds → Set `isScanCancellable = true` (shows Stop button).

6. **Filtering & Sorting**:
   - Apply `FileFilter` (mediaTypes, nameContains, minDate, maxDate, minSize, maxSize).
   - Apply `SortMode` (comparator).
   - Emit final list to `state.mediaFiles`.

7. **Caching**:
   - Store result in `UnifiedFileCache` with 24h expiration.
   - Update `ResourceEntity.fileCount` and `lastScanDate` in DB.

8. **Completion**:
   - Set `isLoading = false`.
   - If files empty AND not filtered → Show "No files found" empty state.
   - If files empty AND filtered → Show "No files match filter" empty state.

### 2. Cloud Authentication (Google Drive Example)

**Trigger**: Opening Google Drive resource OR manual re-auth button.

**Flow**:

1. `viewModel.loadResource()` detects Cloud resource → Checks `googleDriveClient.isAuthenticated()`.
2. If No → Try `refreshAccessToken()` (silently).
3. If refresh fails → Emit `BrowseEvent.ShowCloudAuthenticationRequired`.
4. Activity receives event → `cloudAuthManager.launchGoogleSignIn()`.
5. `googleSignInLauncher` (ActivityResultContract) starts OAuth flow.
6. User selects account → Returns to app with auth code.
7. `handleGoogleSignInResult()` → Exchange code for access token → Store in `EncryptedSharedPreferences`.
8. Call `viewModel.reloadFiles()` to retry file listing.

**OneDrive/Dropbox**: Similar flow but using MSAL (OneDrive) or Dropbox SDK auth methods.

### 3. File Operations (Copy/Move/Delete/Rename)

**Trigger**: Toolbar buttons (btnCopy, btnMove, btnRename, btnDelete) or adapter context menu.

**Flow**:

1. **Selection**: User selects files (checkbox tap, long-press range selection, Select All button).
2. **Dialog**: Activity shows appropriate dialog:
   - Copy: `CopyToDialog` lists destination resources.
   - Move: `MoveToDialog` lists writable destinations.
   - Rename: `RenameDialog` (single) or `RenameMultipleDialog` (multiple).
   - Delete: `DeleteConfirmationDialog` with file count.

3. **Execution**:
   - **Copy/Move**: `fileOperationsManager` calls `FileOperationUseCase`.
     - Cross-protocol routing (Local→SMB, SMB→Cloud, etc.).
     - Progress notifications (indeterminate for network ops).
     - Result: Success → Show "X files copied/moved" toast.
   - **Delete**: 
     - Soft delete (default): Move to `.trash/` folder in same resource.
     - Store `UndoOperation(type=DELETE, files, trashPaths, timestamp)`.
     - Show Undo Snackbar (5-minute timeout).
   - **Rename**:
     - Single: `File.renameTo()` or network equivalent.
     - Multiple: Batch rename with pattern (prefix, suffix, numbering).

4. **Post-Operation**:
   - `viewModel.reloadFiles()` → Refresh file list.
   - `viewModel.clearSelection()` → Deselect all files.
   - Update adapter → Diff calculation → Smooth UI update.

**Network File Editing**: 
- For network resources (SMB/FTP/Cloud), editing requires:
  1. Download to temp file.
  2. Open in editor (external app or internal viewer).
  3. On save → Upload back to original location.
  4. Delete temp file.
- Pattern handled by `NetworkImageEditUseCase`.

### 4. Selection System

**Types**:

1. **Single Select**: Tap checkbox → Toggle selection for that file.
2. **Range Select**: 
   - Long-press file card → Selects that file + all files between it and last selected file.
   - Long-press checkbox → Same range selection behavior.
3. **Select All**: btnSelectAll → Selects all visible files (respects filter).
4. **Deselect All**: btnDeselectAll → Clears selection set.

**State Management**:
- `state.selectedFiles: Set<String>` contains file paths.
- Adapter receives `setSelectedPaths(Set<String>)` → Updates checkbox states.
- Selection count displayed in `tvResourceInfo` ("150 selected").

**Keyboard**:
- `Space`: Toggle current focused item.
- `Ctrl+A`: Select all.
- `Shift+Arrow`: Extend selection (range).

### 5. Interactive Scrolling & View Mode

**Display Modes** (stored in `DisplayMode` enum):

- `LIST`: LinearLayoutManager, full details (name, size, date, duration, thumbnail).
- `GRID_2COL`: GridLayoutManager(2), medium thumbnails + name.
- `GRID_3COL`: GridLayoutManager(3), small thumbnails + name.
- `GRID_4COL`: GridLayoutManager(4), tiny thumbnails only.

**Adaptive Logic**:
- Phone Portrait: LIST or GRID_3COL.
- Phone Landscape: GRID_2COL or GRID_4COL.
- Tablet: GRID_4COL or GRID_2COL.
- User can toggle manually via btnToggleView.

**Optimization**:
- **RecycledViewPool**: Shared between modes, max 40 holders for grid, 30 for list.
- **ItemViewCacheSize**: Calculated dynamically based on screen height (visible items + 5).
- **Scroll Restoration**:
  - On return from PlayerActivity: Scroll to `lastViewedFile`.
  - On return from background: Restore `lastScrollPosition` from `stateManager`.
  - Mechanism: Waits for `submitList` callback completion → `scrollToPosition()`.

**Scroll Buttons**:
- `fabScrollToTop`: Appears when scrolled past position 10.
- `fabScrollToBottom`: Appears when not at bottom (itemCount - 10).
- Action: `scrollToPositionWithOffset(0, 0)` for instant scroll to edge.

### 6. Filtering & Sorting

**FileFilter Fields**:
- `mediaTypes: Set<MediaType>?` (IMAGES, VIDEOS, AUDIO, GIF, TEXT, PDF, EPUB).
- `nameContains: String?` (case-insensitive substring).
- `minDate: Long?`, `maxDate: Long?` (Unix timestamp).
- `minSizeMb: Float?`, `maxSizeMb: Float?`.

**Application**:
- Applied in-memory via `files.filter { }` in ViewModel.
- User-defined filter (beyond resource's `supportedMediaTypes`) shows:
  - Badge on btnFilter with active filter count.
  - Toast "Filter active" on apply.
  - Warning message if no files match.

**SortMode**:
- `NAME_ASC`, `NAME_DESC`, `DATE_ASC`, `DATE_DESC`, `SIZE_ASC`, `SIZE_DESC`, `TYPE_ASC`, `DURATION_DESC`.
- Applied via `files.sortedWith(comparator)`.
- Force adapter refresh when sort changes (via `lastSubmittedSortMode` tracking).

### 7. Undo System

**Mechanism**: Soft-delete to `.trash/` folder.

**Flow**:

1. User deletes files → Files moved to `{resourcePath}/.trash/`.
2. `UndoOperation` stored:
   ```kotlin
   data class UndoOperation(
       val type: OperationType.DELETE,
       val originalPaths: List<String>,
       val trashPaths: List<String>,
       val timestamp: Long = System.currentTimeMillis()
   )
   ```
3. State: `lastOperation = undoOp` → btnUndo becomes visible.
4. User taps btnUndo within 5 minutes → Files moved from `.trash/` back to original locations.
5. If > 5 minutes → `clearExpiredUndoOperation()` removes from state, `.trash/` files auto-deleted on next scan.

**Limitations**:
- Only last operation undoable (no multi-level undo stack).
- Undo expires after 5 minutes.
- Network operations may fail if connection lost.

### 8. Favorites System

**Virtual Resource**: `resourceId == -100L` (FAVORITES_RESOURCE_ID).

**Logic**:

- Files marked `isFavorite = true` via `toggleFavorite()` button in adapter.
- Favorites Browse shows aggregated list from all resources.
- DB query: `SELECT * FROM media_files_cache WHERE isFavorite = 1`.
- **Read-only context**: Cannot move/delete favorites (operation would remove from source resource).
- Favorite toggle: Writes to `MediaFilesCacheEntity.isFavorite` column.

**UI**:
- Star button in adapter (gold when favorite, outline when not).
- Visibility controlled by `settings.enableFavorites` OR `currentResourceId == -100L`.

---

## Lifecycle Management

### onCreate

Deferred to `setupViews()` (called by BaseActivity):

1. **Reset Glide stats**: `GlideCacheStats.reset()`.
2. **Initialize managers**: 9 manager objects with callback interfaces.
3. **Setup adapter**: `MediaFileAdapter` with 11 callback lambdas.
4. **Button listeners**: 17 button click handlers (Back, Sort, Filter, Refresh, Toggle, Select All, Deselect, Copy, Move, Rename, Delete, Undo, Share, Play, Retry, ScrollToTop, ScrollToBottom).
5. **FastScroller**: Attach to RecyclerView for quick alphabet navigation.
6. **Load settings**: `showVideoThumbnails`, `showPdfThumbnails` from SettingsRepository.

**No file loading here** → Happens in ViewModel.init{} which runs on first ViewModel creation.

### onResume

1. **Cloud auth check**: `cloudAuthManager.onResume()` handles pending auth results.
2. **Conditional reload**:
   - `isFirstResume == true` → Skip (files already loaded in ViewModel init).
   - `isFirstResume == false` → Call `viewModel.checkAndReloadIfResourceChanged()`.
     - Checks if `resource.supportedMediaTypes` or `scanSubfolders` changed.
     - If changed → Full reload. If not → Sync with PlayerActivity cache.
3. **Undo expiration**: `clearExpiredUndoOperation()` removes operations older than 5 minutes.
4. **MediaStore observer**: Start monitoring for local resources.
5. **Scroll restoration**: Flag set for restoration in `submitList` callback.

### onPause

1. **Stop MediaStore observer**: Prevents unnecessary reloads when app in background.
2. **Save scroll position**: `stateManager.saveScrollPosition()`.
3. **Cancel thumbnails**: `viewModel.cancelBackgroundThumbnailLoading()` frees bandwidth.
4. **Set scroll flag**: `shouldScrollToLastViewed = true` for return from PlayerActivity.

**Note**: Adapter NO LONGER cleared in onPause (memory cache persists for instant reload).

### onDestroy

1. **Log Glide stats**: `GlideCacheStats.logStats()` for debugging.
2. **Stop observer**: Final cleanup of MediaStore observer.

### onConfigurationChanged

Recalculates display mode with new screen dimensions:
- Reset `currentDisplayMode = null` to force recalculation.
- Call `updateDisplayMode(mode)` → Adjusts GridLayoutManager spanCount.

---

## MediaFileAdapter Callbacks

The adapter provides 11 callback actions (defined in constructor):

1. **onFileClick(file)**: Single tap:
   - If `MediaType.FOLDER`: Navigate to same fragment with new `path`.
   - If File: Call `viewModel.openFile(file)` → Navigate to PlayerActivity.
2. **onFileLongClick(file)**: Long-press card → Range selection from last selected file to this file.
3. **onSelectionChanged(file, selected)**: Checkbox tap → Toggle selection for that file.
4. **onSelectionRangeRequested(file)**: Long-press checkbox → Same as onFileLongClick.
5. **onPlayClick(file)**: Play icon → Same as onFileClick (redundant for convenience).
6. **onFavoriteClick(file)**: Star button → Call `viewModel.toggleFavorite(file)`.
7. **onCopyClick(file)**: Copy icon → Select file → Show CopyToDialog.
8. **onMoveClick(file)**: Move icon → Select file → Show MoveToDialog.
9. **onRenameClick(file)**: Rename icon → Select file → Show RenameDialog.
10. **onDeleteClick(file)**: Delete icon → Select file → Show DeleteConfirmationDialog.
11. **getShowVideoThumbnails()**: Cached setting → Controls video thumbnail generation.
12. **getShowPdfThumbnails()**: Cached setting → Controls PDF first-page thumbnail rendering.

**Adapter Configuration Methods**:
- `setSelectedPaths(Set<String>)`: Updates checkbox states for selected files.
- `setCredentialsId(Long?)`: Passes credentials ID for authenticated network image loading (Glide NetworkFileModelLoader).
- `setDisableThumbnails(Boolean)`: Resource-level thumbnail disable (for slow networks).
- `setResourcePermissions(hasDestinations, isWritable)`: Shows/hides operation icons based on resource capabilities.
- `setShowFavoriteButton(Boolean)`: Controlled by `enableFavorites` setting OR viewing Favorites resource.

---

## Event Handling (`BrowseEvent`)

```kotlin
sealed class BrowseEvent {
    // Navigation
    data class NavigateToPlayer(
        val resourceId: Long,
        val fileIndex: Int,
        val fileList: List<MediaFile>  // Or cache key for large lists
    ) : BrowseEvent()
    
    data class OpenExternalFile(val file: MediaFile) : BrowseEvent()
    
    // Errors
    data class ShowConnectionError(val message: String, val retryable: Boolean) : BrowseEvent()
    data class ShowError(val message: String, val isFatal: Boolean = false) : BrowseEvent()
    
    // Cloud Authentication
    object ShowCloudAuthenticationRequired : BrowseEvent()
    data class CloudAuthSuccess(val provider: CloudProvider, val email: String) : BrowseEvent()
    data class CloudAuthFailed(val error: String) : BrowseEvent()
    
    // File Operations
    data class ShowUndoToast(val operation: UndoOperation, val fileCount: Int) : BrowseEvent()
    data class OperationSuccess(val type: String, val count: Int) : BrowseEvent() // "Copied 5 files"
    data class OperationFailed(val type: String, val error: String) : BrowseEvent()
    
    // Dialogs
    data class ShowCopyToDialog(val selectedFiles: List<MediaFile>) : BrowseEvent()
    data class ShowMoveToDialog(val selectedFiles: List<MediaFile>) : BrowseEvent()
    data class ShowRenameDialog(val files: List<MediaFile>) : BrowseEvent()
    data class ShowDeleteConfirmation(val count: Int) : BrowseEvent()
    
    // Empty States
    object NoFilesFound : BrowseEvent()           // Folder empty
    object NoMatchingFiles : BrowseEvent()        // Filter too restrictive
    
    // Settings
    data class ShowFilterApplied(val activeFilterCount: Int) : BrowseEvent()
}
```

**Event Handling Examples:**

```kotlin
// In BrowseActivity.observeData()
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.events.collect { event ->
        when (event) {
            is NavigateToPlayer -> playerActivityLauncher.launch(
                Intent(this@BrowseActivity, PlayerActivity::class.java).apply {
                    putExtra("resourceId", event.resourceId)
                    putExtra("fileIndex", event.fileIndex)
                    putParcelableArrayListExtra("files", ArrayList(event.fileList))
                }
            )
            
            is ShowCloudAuthenticationRequired -> {
                cloudAuthManager.launchGoogleSignIn() // Or OneDrive/Dropbox equivalent
            }
            
            is ShowConnectionError -> {
                errorStateView.show(event.message)
                btnRetry.isVisible = event.retryable
            }
            
            is ShowUndoToast -> {
                val message = "Deleted ${event.fileCount} files"
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("UNDO") { viewModel.undoLastOperation() }
                    .show()
            }
            
            is OperationSuccess -> {
                Toast.makeText(this@BrowseActivity, 
                    "${event.type} ${event.count} files", 
                    Toast.LENGTH_SHORT).show()
                viewModel.reloadFiles()
            }
            
            NoFilesFound -> {
                if (!viewModel.hasActiveFilter()) {
                    // Actually empty folder
                    emptyStateView.show("No files found")
                } else {
                    // Filter too restrictive
                    emptyStateView.show("No files match filter")
                    btnClearFilter.isVisible = true
                }
            }
        }
    }
}
```

---

## Pagination Support

**Trigger**: File count > `PAGINATION_THRESHOLD` (1000).

**Implementation**:
- ViewModel switches to `PagingMediaFileAdapter` (Paging3 library).
- Scanners implement `scanFolderPaged(offset, limit)` for incremental loading.
- UI shows "Loading more..." footer while paginating.
- **Limitation**: Filtering/sorting requires full list load (pagination bypassed).

**Adapter Switch Logic**:
```kotlin
if (state.mediaFiles.size > 1000 && !state.hasActiveFilter()) {
    // Use PagingMediaFileAdapter
    recyclerView.adapter = pagingAdapter
} else {
    // Use standard MediaFileAdapter
    recyclerView.adapter = standardAdapter
}
```

---

## Managers' Detailed Responsibilities

### 1. BrowseDialogHelper

**Purpose**: Centralized dialog creation/management.

**Methods**:
- `showSortDialog()`: Bottom sheet with 8 sort options.
- `showFilterDialog()`: Full-screen dialog with media type checkboxes, date range, size range, name search.
- `showCopyToDialog(files)`: List of writable destination resources.
- `showMoveToDialog(files)`: Similar to Copy but excludes current resource.
- `showRenameDialog(file)`: Single file rename with validation.
- `showRenameMultipleDialog(files)`: Batch rename with pattern (prefix/suffix/numbering).
- `showDeleteConfirmationDialog(count)`: "Delete X files?" with trash/permanent options.
- `showResourceUnavailableDialog(resourceName)`: Error dialog with Retry button.

**Implementation Notes**:
- All dialogs use Material3 theming.
- Callbacks passed via constructor (`onCopyTo: (ResourceId) -> Unit`, etc.).
- Dialog state persists across configuration changes (DialogFragment + ViewModel).

### 2. BrowseRecyclerViewManager

**Purpose**: Display mode switching, scroll management, item decoration.

**Methods**:
- `updateDisplayMode(mode)`: Switches LayoutManager (Linear/Grid 2/3/4 columns).
- `updateScrollButtonsVisibility(itemCount)`: Shows/hides FAB scroll buttons.
- `scrollToPosition(index)`: Smooth scroll with offset calculation.
- `scrollToPositionWithOffset(index, offset)`: Instant scroll.
- `getFirstVisibleItemPosition()`: For scroll position saving.
- `getLastVisibleItemPosition()`: For scroll buttons logic.

**Display Mode Configuration**:
```kotlin
when (mode) {
    DisplayMode.LIST -> {
        layoutManager = LinearLayoutManager(context)
        spanCount = 1
        itemDecoration = DividerItemDecoration(context, LinearLayout.VERTICAL)
    }
    DisplayMode.GRID_2COL -> {
        layoutManager = GridLayoutManager(context, 2)
        spanCount = 2
        itemDecoration = GridSpacingItemDecoration(2, 8.dp)
    }
    DisplayMode.GRID_3COL -> {
        layoutManager = GridLayoutManager(context, 3)
        spanCount = 3
        itemDecoration = GridSpacingItemDecoration(3, 4.dp)
    }
    DisplayMode.GRID_4COL -> {
        layoutManager = GridLayoutManager(context, 4)
        spanCount = 4
        itemDecoration = GridSpacingItemDecoration(4, 2.dp)
    }
}
recyclerView.layoutManager = layoutManager
recyclerView.addItemDecoration(itemDecoration)
```

### 3. BrowseMediaStoreObserver

**Purpose**: Monitors local file system changes.

**Logic**:
- Only active for local resources (`resource.type == ResourceType.LOCAL`).
- Uses `FileObserver` API to watch folder.
- Detects: `CREATE`, `DELETE`, `MODIFY`, `MOVED_FROM`, `MOVED_TO` events.
- On change → Debounce 500ms → Call `viewModel.reloadFiles()`.

**Lifecycle**:
- Started in `onResume()`.
- Stopped in `onPause()` (prevents background reloads).
- Fully released in `onDestroy()`.

### 4. BrowseFileOperationsManager

**Purpose**: Executes file operations (copy/move/delete/rename).

**Methods**:
- `copyFiles(files, destinationPath)`: Delegates to `FileOperationUseCase`.
- `moveFiles(files, destinationPath)`: Same as copy + delete source.
- `renameFile(file, newName)`: Single file rename.
- `renameFiles(files, pattern)`: Batch rename with numbering.
- `deleteFiles(files, permanent)`: Soft-delete to `.trash/` or permanent deletion.
- `shareFiles(files)`: Creates FileProvider URIs → System share sheet.

**Progress Tracking**:
- For operations > 5 files OR > 100MB total → Show foreground service notification.
- Notification shows progress bar (indeterminate for network ops).
- Cancellation support via `Job.cancel()`.

### 5. BrowseSmallControlsManager

**Purpose**: Manages compact UI mode (32dp buttons vs 48dp).

**Methods**:
- `applySmallControls()`: Reduces button sizes, padding, icon sizes.
- `restoreNormalControls()`: Restores default sizes.

**Applied To**:
- Top toolbar buttons (Back, Sort, Filter, Refresh, etc.).
- Bottom operations panel (Copy, Move, Delete, etc.).
- FAB scroll buttons.

**Setting**: `settings.enableSmallControls` (default: false).

### 6. BrowseCloudAuthManager

**Purpose**: Handles OAuth flows for Google Drive, OneDrive, Dropbox.

**Methods**:
- `launchGoogleSignIn()`: Uses `GoogleSignInClient` with scopes (Drive.FILE, Drive.METADATA).
- `launchOneDriveSignIn()`: Uses MSAL (Microsoft Authentication Library).
- `launchDropboxSignIn()`: Uses Dropbox SDK auth flow.
- `handleSignInResult(result)`: Processes auth code → Exchanges for token → Stores encrypted.
- `onResume()`: Checks for pending auth results (activity relaunch after auth).

**ActivityResultContracts**:
```kotlin
private val googleSignInLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    cloudAuthManager.handleGoogleSignInResult(result)
}
```

### 7. BrowseUtilityManager

**Purpose**: Miscellaneous helper methods (date formatting, size formatting, permission checks).

**Methods**:
- `formatFileSize(bytes)`: "1.5 MB", "350 KB", "2.1 GB".
- `formatDate(timestamp)`: "Today", "Yesterday", "Jan 15", "12/15/2024".
- `formatDuration(ms)`: "1:23", "45:67", "1:23:45".
- `checkStoragePermission()`: Android 10+ storage permission check.
- `checkNetworkAvailability()`: Pings resource to verify connectivity.
- `buildResourceInfo(state)`: Generates header text ("Resource • Path • X selected").

### 8. BrowseStateManager

**Purpose**: Saves/restores UI state across configuration changes and process death.

**State Persisted**:
- `lastScrollPosition: Int`.
- `lastViewedFilePath: String?`.
- `selectedFilePaths: Set<String>`.
- `sortMode: SortMode`.
- `displayMode: DisplayMode`.
- `activeFilter: FileFilter?`.

**Storage**: 
- ViewModel `SavedStateHandle` (survives process death).
- SharedPreferences for long-term preferences (sort mode, display mode).

**Methods**:
- `saveScrollPosition()`: Called in `onPause()`.
- `restoreScrollPosition()`: Called in `submitList` callback.
- `saveLastViewedFile(path)`: Called before opening PlayerActivity.
- `restoreLastViewedFile()`: Called on return from PlayerActivity.

### 9. KeyboardNavigationManager

**Purpose**: Handles keyboard shortcuts for desktop mode (tablets with keyboards, DeX, ChromeOS).

**Key Bindings**:
- **Arrow Up/Down**: Navigate RecyclerView (focus highlight).
- **Enter**: Open focused file.
- **Space**: Toggle selection.
- **Delete**: Delete focused file.
- **Ctrl+A**: Select all.
- **Ctrl+D**: Deselect all.
- **Ctrl+C**: Copy selected.
- **Ctrl+X**: Move selected.
- **Ctrl+Z**: Undo.
- **F2**: Rename focused file.
- **Escape**: Close activity.

**Focus Management**:
- Uses `View.requestFocus()` + custom focus indicator (border or background tint).
- Focus persists during scroll (tracked via `focusedPosition: Int`).
- Focus lost on orientation change (limitation: RecyclerView doesn't persist focus).

---

## Testing Considerations

### Unit Tests

- **ViewModel**: Test state updates, event emissions, UseCase interactions (mocked).
- **Managers**: Test individual manager logic (dialog callbacks, scroll calculations, auth flows).
- **Adapters**: Test DiffUtil callbacks, ViewHolder binding, selection state updates.

### Integration Tests

- **File Operations**: Test copy/move/delete across protocols (Local→SMB, SMB→Cloud, etc.).
- **Authentication**: Test OAuth flows with test accounts (Google/OneDrive/Dropbox sandboxes).
- **Pagination**: Test with > 1000 files, verify lazy loading.
- **Undo**: Test soft-delete to `.trash/`, verify restoration, verify expiration (5min).

### UI Tests

- **Espresso**: Test button clicks, dialog interactions, RecyclerView scrolling.
- **Screenshot Tests**: Capture display modes (LIST/GRID_2/3/4), error states, empty states.
- **A11y Tests**: Verify content descriptions, touch target sizes (48dp minimum), keyboard navigation.

### Performance Tests

- **Large Lists**: Test 5000+ files, verify smooth scrolling (60fps).
- **Network Latency**: Test with throttled network (slow SMB), verify timeout handling.
- **Memory**: Monitor heap during PlayerActivity navigation, verify no memory leaks.
- **Battery**: Monitor battery drain during cloud sync, verify Doze mode compatibility.

---

## Known Issues & Limitations

1. **FTP PASV Mode**: Timeouts on some routers → Fallback to active mode implemented.
2. **SMB Connection Pool**: Idle connections cleaned after 45s, but pool may not release all connections under high load.
3. **Undo Expiration**: 5-minute timeout is hardcoded → User setting planned for future.
4. **Pagination + Filter**: Filtering requires full list load (pagination disabled) → Performance impact for > 10k files.
5. **Focus Persistence**: RecyclerView doesn't preserve keyboard focus across configuration changes.
6. **Cloud Thumbnail Loading**: Slow on metered connections → Option to disable thumbnails per-resource.
7. **Network Image Editing**: Requires download → edit → upload cycle → Large files (> 50MB) may timeout.
8. **Multi-level Undo**: Only last operation undoable → Full undo stack planned for future.
9. **Sort Stability**: Quicksort used (not stable) → Files with identical sort keys may reorder unpredictably.
10. **Adapter Clear in onPause**: NO LONGER DONE (memory cache persists) → But may cause memory warnings on low-RAM devices.

---

## Future Enhancements

- **Batch Editing**: Edit multiple images with same adjustments (brightness, contrast, crop).
- **Cloud Sync**: Two-way sync between local and cloud (like Dropbox desktop app).
- **Advanced Filters**: Regex support, EXIF metadata filters (GPS, camera model, ISO).
- **Smart Collections**: Auto-generated collections (Recent, Frequently Viewed, Large Files).
- **AI Features**: Auto-tagging with ML Kit, duplicate detection via perceptual hashing.
- **External Storage**: Direct access to USB drives, SD cards (Storage Access Framework).
- **Archive Support**: Browse .zip, .rar, .7z files without extraction.
- **EPUB Support**: Built-in reader with progress tracking, bookmarks.
- **Folder Comparison**: Side-by-side view for syncing folders (like WinMerge).
- **Network Discovery**: Auto-detect SMB shares, DLNA servers on local network.
- `setDisableThumbnails(Boolean)`: Resource-level thumbnail disable (for slow networks).
- `setResourcePermissions(hasDestinations, isWritable)`: Shows/hides operation icons based on resource capabilities.
- `setShowFavoriteButton(Boolean)`: Controlled by `enableFavorites` setting OR viewing Favorites resource.

---

## Button Actions (17 buttons)

### Top Toolbar (9 buttons)

1. **btnBack**: `finish()` → Return to MainActivity with slide-out animation.
2. **btnSort**: Show `SortModeDialog` bottom sheet → User selects sort → `viewModel.setSortMode(sortMode)`.
3. **btnFilter**: Show `FilterDialog` → User sets criteria → `viewModel.setFilter(filter)` → Show badge with filter count.
4. **btnRefresh**: Force reload → `smbClient.forceFullReset()`, `NetworkFileDataFetcher.clearFailedVideoCache()`, `viewModel.reloadFiles()`.
5. **btnToggleView**: Cycle through display modes (LIST → GRID_2COL → GRID_3COL → GRID_4COL).
6. **btnSelectAll**: `viewModel.selectAllFiles()` → Selects all visible files (respects filter).
7. **btnDeselectAll**: `viewModel.clearSelection()` → Clears selection set.
8. **btnPlay**: `startSlideshow()` → Opens PlayerActivity in slideshow mode from first selected file or first file.
9. **btnStopScan** (inside layoutProgress): Cancels ongoing scan → `viewModel.cancelScan()` → Shows "Scan cancelled" toast.

### Bottom Operations Panel (6 buttons)

Visibility: `layoutOperations.isVisible = hasSelection || lastOperation != null`.

10. **btnCopy**: Show `CopyToDialog` → User selects destination → `fileOperationsManager` executes copy.
11. **btnMove**: Show `MoveToDialog` → User selects destination → `fileOperationsManager` executes move (only if resource `isWritable && !isReadOnly`).
12. **btnRename**: Show `RenameDialog` (single file) or `RenameMultipleDialog` (multiple files) → Execute rename → Reload.
13. **btnDelete**: Show `DeleteConfirmationDialog` → Confirm → Soft-delete to `.trash/` → Show Undo snackbar.
14. **btnUndo**: `viewModel.undoLastOperation()` → Restore files from `.trash/` → Clear undo state.
15. **btnShare**: `shareSelectedFiles()` → Android system share sheet with file URIs (uses FileProvider).

### Floating Buttons (2 buttons)

16. **fabScrollToTop**: `scrollToPositionWithOffset(0, 0)` → Instant scroll to first item.
17. **fabScrollToBottom**: `scrollToPositionWithOffset(itemCount-1, 0)` → Instant scroll to last item.

**Conditional Visibility**:
- fabScrollToTop: Visible when `firstVisiblePosition > 10`.
- fabScrollToBottom: Visible when `lastVisiblePosition < itemCount - 10`.
- Updated on scroll via `RecyclerView.OnScrollListener`.

---

## State Observation & UI Updates

### observeData() Coroutines

**state.collect** (main state observer):

1. **Deduplication check**:
   - Compare `state.mediaFiles` with `viewModel.lastEmittedMediaFiles`.
   - If identical (same size, same content hash) → Skip `submitList()`.
   - Purpose: Avoids redundant adapter updates when only selection/filter badge changes.

2. **Submit list**:
   - `mediaFileAdapter.submitList(state.mediaFiles) { callback }`.
   - Callback executes after DiffUtil completes → Safe to scroll.

3. **Scroll restoration** (in submitList callback):
   - If `shouldScrollToLastViewed == true`:
     - Get `lastViewedFilePath` from `stateManager`.
     - Find index in new list → `scrollToPosition(index)`.
     - Reset flag: `shouldScrollToLastViewed = false`.

4. **Update scroll buttons**: Call `updateScrollButtonsVisibility(itemCount)`.

5. **Selection sync**: `mediaFileAdapter.setSelectedPaths(state.selectedFiles)`.

6. **Adapter config updates**:
   - `setCredentialsId(resource.credentialsId)`.
   - `setDisableThumbnails(resource.disableThumbnails)`.
   - `setResourcePermissions(hasDestinations, isWritable)`.

7. **Resource info**: `tvResourceInfo.text = buildResourceInfo(state)` → Shows "Resource Name • Path • X selected".

8. **Filter warning**:
   - If user-defined filter active → Show badge on btnFilter with count.
   - If no user filter → Clear badge.
   - **Never show tvFilterWarning** (removed in favor of badge + toast).

9. **Operations panel visibility**:
   - `layoutOperations.isVisible = hasSelection || lastOperation != null`.
   - Individual button visibility:
     - btnCopy: Always visible when selection exists.
     - btnMove/Rename/Delete: Only if `isWritable && !isReadOnly`.
     - btnUndo: Only if `lastOperation != null`.
     - btnShare: Always visible when selection exists.

10. **Display mode update**:
    - If `state.displayMode != currentDisplayMode`:
      - Cache new mode: `currentDisplayMode = state.displayMode`.
      - Call `recyclerViewManager.updateDisplayMode(displayMode)`.

11. **Small controls**: Apply/restore based on `state.showSmallControls` setting.

**settings + state combine** (favorite button visibility):
- `settings.enableFavorites || resource.id == -100L` → Show favorite button in adapter.
- Purpose: Always show for Favorites browse, optionally show for other resources.

**loading.collect**: Show/hide `layoutProgress`.

**error.collect**: Show/hide `errorStateView` with error message + Retry button.

**events.collect**: Handle one-shot events (ShowCloudAuthenticationRequired, NoFilesFound, NavigateToPlayer).

---

## Keyboard Navigation

**Supported Keys** (handled by KeyboardNavigationManager):

- **Arrow Up/Down**: Move focus in RecyclerView (highlights item with selection background).
- **Arrow Left/Right**: Navigate grid columns (in grid mode).
- **Enter**: Open focused file in PlayerActivity.
- **Space**: Toggle selection for focused file.
- **Delete**: Delete focused file (shows confirmation dialog).
- **Ctrl+A**: Select all files.
- **Ctrl+D**: Deselect all files.
- **F2**: Rename focused file.
- **Escape**: Close activity (return to MainActivity).
- **Ctrl+C**: Copy selected files (shows CopyToDialog).
- **Ctrl+X**: Move selected files (shows MoveToDialog).
- **Ctrl+Z**: Undo last operation.

**Focus Management**:
- RecyclerView item gets focus highlight (border or background tint).
- Focus persists across scroll.
- Focus resets on orientation change (limitation of RecyclerView).

---

## Error Handling

**Error States**:

1. **Connection Failed**: Network resource unreachable (SMB/FTP/Cloud).
   - UI: Show `errorStateView` with message "Cannot connect to {resourceName}".
   - Action: btnRetry → Retry connection → `viewModel.reloadFiles()`.

2. **Authentication Required**: Cloud OAuth token expired/invalid.
   - Event: `ShowCloudAuthenticationRequired`.
   - UI: Dialog "Sign in to {provider} required" with Sign In button.
   - Action: Launch OAuth flow → On success → Retry file load.

3. **Permission Denied**: Local storage access denied (Android 10+).
   - UI: Error dialog with "Grant storage permission" button.
   - Action: Opens app settings → User grants permission → Return → Reload.

4. **No Files Found**: Folder empty or filter too restrictive.
   - UI: Empty state view with message "No files found".
   - Action: Clear filter button (if filtered) OR "Go back" button.

5. **Scan Timeout**: Network scan exceeds 15 seconds.
   - UI: Show timeout error + Retry button.
   - Action: User can increase timeout in Settings OR use different network.

**Error Display**:
- All errors respect `showDetailedErrors` setting (ErrorDialog with stack trace vs simple Toast).
- Critical errors (connection, auth) show ErrorDialog regardless of setting.

---

## Performance Optimizations

1. **RecycledViewPool**: Shared across display modes, max 40 holders (grid) / 30 (list).
2. **ItemViewCacheSize**: Dynamically calculated: `(screenHeight / itemHeight) + 5`.
3. **Glide Memory Cache**: Up to 1GB (40% of heap), persists across PlayerActivity navigation.
4. **UnifiedFileCache**: 24h expiration, stores file lists keyed by resource path.
5. **DiffUtil**: Automatic diff calculation for smooth list updates (only changed items rebound).
6. **Background Thumbnail Cancellation**: On onPause → Frees bandwidth for PlayerActivity.
7. **Deduplication**: Skips `submitList()` when file list unchanged (tracks hash).
8. **Lazy Thumbnail Loading**: Glide loads thumbnails on-demand as user scrolls.
9. **PDF Thumbnail Caching**: First page rendered to bitmap → Cached in `pdf_thumbnails/`.
10. **Connection Pooling**: SMB/SFTP clients reuse connections (max 5 per resource).

---

## Complete Behavior Summary

### Initialization Flow

1. Intent parsing → Extract resourceId, skipAvailabilityCheck.
2. ViewModel.init{} → loadResource(resourceId).
3. Fetch resource from DB → Validate → Ping if network.
4. Check UnifiedFileCache → If valid → Return cached.
5. Scan files (Local/SMB/SFTP/FTP/Cloud) → Apply filter → Sort.
6. Update state → observeData collects → submitList to adapter.
7. Render UI with thumbnails loaded lazily.

### User Interaction Flows

**Open File**:
1. Tap file card → `onFileClick(file)`.
2. ViewModel: `openFile(file)` → Save lastViewedFile.
3. Event: NavigateToPlayer(resourceId, fileIndex).
4. Launch PlayerActivity with `playerActivityLauncher`.
5. On return: Result contains modifiedFiles → Remove from list.
6. Scroll restoration: Scroll to lastViewedFile position.

**Select Files**:
1. Tap checkbox → `onSelectionChanged(file, true)`.
2. ViewModel: Add path to `selectedFiles` set.
3. State update → Adapter: Update checkbox states.
4. UI: Show operation buttons (Copy, Move, Rename, Delete, Share).

**Range Select**:
1. Long-press card → `onFileLongClick(file)`.
2. ViewModel: `selectFileRange(file.path)`.
3. Find lastSelectedFile → Select all files in between.
4. State update → Multiple checkboxes enabled simultaneously.

**Copy Files**:
1. Select files → Tap btnCopy.
2. CopyToDialog shows destinations.
3. User picks destination → Dialog callback.
4. `fileOperationsManager.copy(selectedFiles, destinationPath)`.
5. Progress notification (foreground service for large ops).
6. On complete: Toast "X files copied" → Reload → Clear selection.

**Delete Files**:
1. Select files → Tap btnDelete.
2. DeleteConfirmationDialog → User confirms.
3. `viewModel.deleteSelectedFiles()`:
   - Move to `.trash/` folder.
   - Store UndoOperation with timestamp.
4. Show Undo Snackbar (5min timeout).
5. Reload → Files disappear from list.

**Undo Delete**:
1. Tap btnUndo within 5 minutes.
2. `viewModel.undoLastOperation()`:
   - Move files from `.trash/` back to original paths.
   - Clear undo state.
3. Toast "X files restored".
4. Reload → Files reappear.

**Filter Files**:
1. Tap btnFilter → FilterDialog.
2. User sets criteria (media types, name, date, size).
3. Dialog callback: `viewModel.setFilter(filter)`.
4. ViewModel: Apply filter in-memory → Update state.
5. UI: Show badge on btnFilter with count → Toast "Filter active".
6. Adapter: Shows only matching files.

**Sort Files**:
1. Tap btnSort → SortModeDialog.
2. User selects mode (Name A-Z, Date Newest, Size Largest, etc.).
3. Dialog callback: `viewModel.setSortMode(sortMode)`.
4. ViewModel: Apply sort → Force adapter refresh (via `lastSubmittedSortMode` tracking).
5. UI: Files reordered instantly.

**Toggle View**:
1. Tap btnToggleView.
2. Cycle: LIST → GRID_2COL → GRID_3COL → GRID_4COL → LIST.
3. ViewModel: Update `displayMode`.
4. Activity: `recyclerViewManager.updateDisplayMode(mode)`.
5. RecyclerView: Switch LayoutManager → Rebind all views.
6. Icon update: btnToggleView shows next mode icon.

**Refresh**:
1. Tap btnRefresh.
2. Reset network clients: `smbClient.forceFullReset()`.
3. Clear caches: `NetworkFileDataFetcher.clearFailedVideoCache()`, `UnifiedFileCache.invalidate(resourcePath)`.
4. Reload: `viewModel.reloadFiles()` → Full scan.
5. Progress: Show scan progress (file count, Stop button after 5s).
6. Complete: Update file list → Show "Refreshed" toast.

**Start Slideshow**:
1. Tap btnPlay.
2. `startSlideshow()`:
   - If files selected → Start from first selected.
   - Else → Start from first file in list.
3. Launch PlayerActivity with `slideshowMode = true` extra.
4. PlayerActivity: Auto-advance every X seconds (setting).

**Cloud Re-Auth**:
1. Open Cloud resource → Token expired.
2. Event: ShowCloudAuthenticationRequired.
3. Activity: `cloudAuthManager.launchGoogleSignIn()`.
4. OAuth flow: User signs in → Returns auth code.
5. Exchange for token → Store encrypted.
6. Retry: `viewModel.reloadFiles()` → Success.

---
