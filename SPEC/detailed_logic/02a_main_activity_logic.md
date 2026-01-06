# 02a. MainActivity Logic

## Overview

`MainActivity` serves as the central hub for resource management. It uses a **Model-View-ViewModel (MVVM)** architecture with Unidirectional Data Flow (UDF).

## Components

### 1. View (MainActivity)

- **Role:** Renders state, captures user input, delegates navigation.
- **Key Delegates:**
  - `KeyboardNavigationHandler`: Hardware keyboard support (Arrow keys, Enter, Delete, Tab, Esc, Space).
  - `ResourcePasswordManager`: Secure PIN input handling for protected resources.
  - `ResourceAdapter`: Displays list/grid of resources with context actions.

### 2. ViewModel (MainViewModel)

- **Role:** Manages state, applies business rules, coordinates with UseCases.
- **Key Coordinators:**
  - `ResourceNavigationCoordinator`: Validates resource availability/permissions before navigation.
  - `ResourceScanCoordinator`: Orchestrates network scanning and database updates.
  - `ResourceFilterManager`: Applies in-memory filtering and sorting.
  - `ResourceOrderManager`: Handles manual reordering logic (Move Up/Down).

---

## State Management (`MainState`)

The UI is driven by a single state object:

```kotlin
data class MainState(
    val resources: List<MediaResource>, // Filtered & Sorted list
    val isResourceGridMode: Boolean,    // Layout View Mode (List vs Grid)
    val selectedResource: MediaResource?,
    val sortMode: SortMode,             // MANUAL, NAME, DATE, etc.
    val activeResourceTab: ResourceTab, // ALL, LOCAL, SMB, FTP, CLOUD
    val filterByType: Set<ResourceType>?,
    val filterByMediaType: Set<MediaType>?,
    val filterByName: String?,
    val previousTab: ResourceTab?       // For restoring context after returning from Favorites
)
```

**State Flow:**

1. `getResourcesUseCase()` emits DB updates.
2. `settingsRepository.getSettings()` emits Config updates.
3. `combine` operator merges sources.
4. `applyFiltersAndSorting()` processes the list.
5. `state` is updated -> UI Re-renders.

---

## Key Logic Flows

### 1. Resource Navigation (`validateAndOpenResource`)

**Trigger:** Click on Resource Item or "Start Player" button.
**Logic:**

1. **Check Availability:** Pings network resources (SMB/FTP) to ensure reachability.
2. **Check Permissions:** Verifies local storage access.
3. **Check PIN:** If resource is locked, returns `RequestPin` result.
4. **Result Handling:**
   - `Navigate`: Emits `NavigateToBrowse` or `NavigateToPlayerSlideshow`.
   - `RequestPin`: Triggers `showPasswordPrompt()`.
   - `Error`: Emits `ShowError` (respects `showDetailedErrors` setting).

### 2. Smart Player Start (`startPlayer`)

**Trigger:** Large Play Button.
**Logic:**

1. Check `selectedResource`.
2. If none, check `settingsRepository.getLastUsedResourceId()`.
3. If valid, open that resource.
4. Fallback: Open first available resource in list.

### 3. Layout Adaptation (`updateLayoutManagerForScreenSize`)

**Trigger:** `onCreate`, `onConfigurationChanged`, `state.isResourceGridMode` change.
**Logic:**

- **Grid Mode:**
  - Tablet/Landscape (>=600dp): 5 columns.
  - Phone Portrait: 3 columns.
- **List Mode:**
  - Tablet/Landscape: 2 columns (Compact Grid).
  - Phone Portrait: 1 column (Linear List).

### 4. Tab & Filtering Logic

**Tabs:** `ALL` | `LOCAL` | `SMB` | `FTP_SFTP` | `CLOUD`
**Logic:**

- Selecting a tab updates `activeResourceTab`.
- `ResourceFilterManager` applies type filtering based on the tab.
- **Favorites Handling:**
  - "Favorites" is a button (`btnFavorites`), NOT a tab.
  - Clicking it saves `currentTab`, opens `BrowseActivity` with `FAVORITES_RESOURCE_ID` (-100).
  - On return to MainActivity, `onResume` checks `state.previousTab != null` and restores it via `viewModel.restorePreviousTab()`.
  - Tab synchronization: If `activeResourceTab != FAVORITES`, TabLayout is synced to match ViewModel state.

**Filter Warning Display**:
- When filters are active (`filterByType`, `filterByMediaType`, or `filterByName` not null), `tvFilterWarning` becomes visible.
- Text format: "Filters: Type: LOCAL, SMB | Media: IMAGES, VIDEOS | Name: 'search term'"
- Color: `@android:color/holo_orange_dark` for visibility.

### 5. Scanning System (`scanAllResources`)

**Trigger:** Refresh Button.
**Logic:**

1. `smbClient.forceFullReset()`: Clears SMB session pool, forces re-authentication.
2. `NetworkFileDataFetcher.clearFailedVideoCache()`: Clears blacklist of video URLs that failed thumbnail extraction (allows retry).
3. `viewModel.scanAllResources()`:
   - Launches parallel coroutines for all resources.
   - Emits `ScanProgress` events with `(scannedCount, currentFileName)`.
   - Updates `lastScanDate` and `fileCount` in ResourceEntity via DB.
   - Returns summary (Success count, Failures).
   - Emits `ScanComplete` event to hide overlay.

**Progress UI**:
- `scanProgressLayout` becomes visible during scan.
- `tvScanProgress` shows current file name: "Scanning: IMG_1234.jpg"
- `tvScanDetail` shows count: "150 files scanned"
- `scanProgressBar` shows indeterminate animation.

---

## State Observation & UI Updates

### observeData() Coroutines

**state.collect**:
- `submitList(state.resources)`: Updates RecyclerView with filtered/sorted list.
- `setSelectedResource(state.selectedResource?.id)`: Highlights selected resource in adapter.
- `setViewMode(state.isResourceGridMode)`: Switches adapter between detailed and compact layouts.
- `updateLayoutManagerForScreenSize()`: Reconfigures RecyclerView LayoutManager (Linear vs Grid, span count).
- **Toggle button icon**: Updates between `ic_view_grid` and `ic_view_list`.
- **Toggle button visibility**: Shows if grid mode active OR resource count > 10.
- **Play button state**: Enabled only if resources list not empty.
- **Empty/Error state visibility**:
  - `errorStateView.isVisible = hasError && isEmpty`
  - `emptyStateView.isVisible = !hasError && isEmpty`
  - `rvResources.isVisible = !isEmpty`
  - Uses `state.resources.size` instead of `adapter.itemCount` (avoids async timing issues).
- **Filter warning**: Calls `updateFilterWarning(state)` to show/hide and update text.
- **Tab sync**: Updates TabLayout selection to match `state.activeResourceTab` (skips FAVORITES enum).

**loading.collect**:
- `progressBar.isVisible = isLoading`: Shows/hides circular progress indicator.

**error.collect**:
- Updates `errorStateView` visibility and `tvErrorMessage.text`.
- Coordinates with `state` to determine if error state should be shown (only when isEmpty).

**events.collect**:
- See "Event Handling" section above for complete event catalog.
- Each event triggers specific UI action (navigation, dialog, toast, etc.).

**settingsRepository.getSettings().collect**:
- `btnFavorites.visibility = if (settings.enableFavorites) VISIBLE else GONE`
- Dynamically shows/hides Favorites button based on user preference.

---

## Layout Adaptation Logic

### updateLayoutManagerForScreenSize()

**Called on**:
- Initial setup in `setupViews()`.
- Configuration change via `onLayoutConfigurationChanged(newConfig)`.
- State change when `isResourceGridMode` toggles.

**Decision Tree**:

```
screenWidthDp = resources.configuration.screenWidthDp
isWideScreen = screenWidthDp >= 600

IF isResourceGridMode == true:
    IF isWideScreen:
        GridLayoutManager(spanCount=5)  // Tablet/Landscape compact grid
    ELSE:
        GridLayoutManager(spanCount=3)  // Phone Portrait compact grid
ELSE:
    IF isWideScreen:
        GridLayoutManager(spanCount=2)  // Tablet/Landscape detailed grid
    ELSE:
        LinearLayoutManager()           // Phone Portrait detailed list
```

**Optimization**: Only creates new LayoutManager if current one differs (checks type and spanCount).

---

## Error Handling

### showError(message, details)

**Behavior**:
1. Reads `settingsRepository.getSettings().first()` to check `showDetailedErrors` flag.
2. **If true**: Shows `ErrorDialog` with:
   - Title: "Error"
   - Message: `message`
   - Details: `details` (copyable, scrollable)
   - Buttons: Copy, OK
3. **If false**: Shows simple Toast with `message` (LENGTH_LONG).

**Purpose**: Power users get full stack traces, casual users get simple messages.

### showInfo(message, details)

**Similar to showError**, but:
- Dialog title: "Information" instead of "Error".
- Toast duration: LENGTH_SHORT instead of LENGTH_LONG.
- Used for non-error info (e.g., "Folder is empty but resource is reachable").

---

## Version Logging

### App Version Check (in setupViews)

Launched in background IO coroutine:
```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val versionName = packageManager.getPackageInfo(packageName, 0).versionName
    val versionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode
    Timber.d("App version: $versionName (code: $versionCode)")
}
```

**Purpose**: Non-critical logging for debugging, doesn't block UI.

---

## Companion Object

### Constants

```kotlin
const val ACTION_START_SLIDESHOW = "com.sza.fastmediasorter.ACTION_START_SLIDESHOW"
```

**Usage**: Home screen widgets send this intent to MainActivity to trigger auto-play slideshow.

**Handling**: In `onCreate`, if `intent.action == ACTION_START_SLIDESHOW`, delayed call to `viewModel.startPlayer()` via `binding.root.post { }`.

---

## Complete Behavior Summary

### Initialization Flow

1. onCreate → Check savedInstanceState, fix cloud paths, check first launch.
2. If first launch → WelcomeActivity, finish.
3. Initialize keyboard/password handlers.
4. setupViews → Create adapter, configure RecyclerView, bind all button actions.
5. observeData → Start collecting state/events/loading/error flows.
6. viewModel.refreshResources → Load resources from DB, apply filters, render.

### User Interaction Flows

**Add Resource**:
1. Click btnAddResource or emptyStateView.
2. Event: NavigateToAddResource → Open AddResourceActivity.

**Open Resource**:
1. Click resource card in list.
2. Adapter: onItemClick → viewModel.selectResource → viewModel.openBrowse.
3. ViewModel validates availability/permissions/PIN.
4. Event: RequestPassword (if protected) OR NavigateToBrowse.

**Edit Resource**:
1. Long-press card OR click Edit button.
2. Check PIN protection.
3. If protected: ResourcePasswordManager validates PIN.
4. Event: NavigateToEditResource → Open EditResourceActivity.

**Delete Resource**:
1. Click Delete button in card.
2. Show AlertDialog confirmation.
3. Positive button → viewModel.deleteResource → DB delete → State update.

**Toggle View**:
1. Click btnToggleView.
2. viewModel.toggleResourceViewMode → `isResourceGridMode` flips.
3. State update triggers:
   - Adapter switches layout (detailed ↔ compact).
   - LayoutManager reconfigures (Linear ↔ Grid, span count).
   - Button icon updates.

**Filter Resources**:
1. Click btnFilter.
2. FilterResourceDialog shows with current state.
3. User adjusts filters, clicks Apply.
4. Dialog callback → viewModel setters (setSortMode, setFilterByType, etc.).
5. State update → ResourceFilterManager applies filters → UI re-renders with filtered list.
6. tvFilterWarning shows active filter summary.

**Scan Resources**:
1. Click btnRefresh.
2. Reset SMB client, clear video cache.
3. viewModel.scanAllResources → Parallel scans.
4. ScanProgress events → Update scan overlay.
5. ScanComplete event → Hide overlay.
6. State update with new fileCount/lastScanDate.

**Tab Navigation**:
1. Click tab (ALL, LOCAL, SMB, FTP_SFTP, CLOUD).
2. TabLayout.OnTabSelectedListener → viewModel.setActiveTab(ResourceTab).
3. State update → ResourceFilterManager applies type filter.
4. UI re-renders with filtered list.

**Favorites**:
1. Click btnFavorites.
2. viewModel.openFavorites → Saves current tab to `previousTab`.
3. Event: NavigateToFavorites → BrowseActivity with FAVORITES_RESOURCE_ID.
4. On return: onResume → restorePreviousTab → TabLayout syncs.

**Start Slideshow**:
1. Click btnStartPlayer.
2. viewModel.startPlayer → Smart selection (selected → last used → first).
3. Check availability/permissions/PIN.
4. Event: RequestPassword OR NavigateToPlayerSlideshow.

**Exit App**:
1. Click btnExit.
2. finishAffinity() + Process.killProcess().
3. onDestroy → clearAll() UnifiedFileCache.

---

## Event Handling (`MainEvent`)

Sealed class hierarchy for one-shot events from ViewModel to View:

**Navigation Events:**
- `NavigateToBrowse(resourceId, skipAvailabilityCheck)`: Open BrowseActivity with slide-in animation.
- `NavigateToFavorites`: Open BrowseActivity with FAVORITES_RESOURCE_ID (-100).
- `NavigateToPlayerSlideshow(resourceId)`: Start slideshow mode from resource.
- `NavigateToEditResource(resourceId)`: Open EditResourceActivity (checks PIN first).
- `NavigateToAddResource(preselectedTab)`: Open AddResourceActivity with optional tab pre-selection.
- `NavigateToAddResourceCopy(copyResourceId)`: Open AddResourceActivity in copy mode.
- `NavigateToSettings`: Open SettingsActivity.

**UI Feedback Events:**
- `ShowError(message, details)`: Displays ErrorDialog (if `showDetailedErrors=true`) or Toast.
- `ShowInfo(message, details)`: Similar to ShowError but with "Information" title.
- `ShowMessage(message)`: Simple Toast notification.
- `ShowResourceMessage(resId, args)`: Toast with string resource and formatting args.

**Security Events:**
- `RequestPassword(resource, forSlideshow)`: Handled by `ResourcePasswordManager`, triggers PIN input dialog.

**Progress Events:**
- `ScanProgress(scannedCount, currentFile)`: Updates scan progress overlay with file count and current filename.
- `ScanComplete`: Hides scan progress overlay.

---

## Lifecycle Management

### onCreate

1. **Configuration logging**: Logs `savedInstanceState` and `isChangingConfigurations` for debugging recreation issues.
2. **Cloud paths migration**: Calls `MediaFilesCacheManager.fixCloudPaths()` to fix legacy format (cloud:/ → cloud://).
3. **First launch check**: If `!welcomeViewModel.isWelcomeCompleted()`, redirect to WelcomeActivity and finish.
4. **Widget action handling**: If `intent.action == ACTION_START_SLIDESHOW`, post delayed call to `viewModel.startPlayer()`.
5. **Keyboard navigation init**: Create `KeyboardNavigationHandler` with callbacks for delete, add, settings, filter, exit.
6. **Password manager init**: Create `ResourcePasswordManager` for PIN-protected resources.
7. **Deferred UI setup**: Actual view setup happens in `setupViews()` called by BaseActivity.

### onResume

1. **Tab restoration**: If returning from Favorites Browse, call `viewModel.restorePreviousTab()`.
2. **Tab synchronization**: Sync TabLayout selection with ViewModel's `activeResourceTab` state (skip FAVORITES tab).
3. **Conditional refresh**: Only call `viewModel.refreshResources()` if `isReturningFromAnotherActivity` flag is true.

**Purpose**: Avoid redundant refresh on first launch, but ensure fresh data when returning from other activities.

### onPause

- Set `isReturningFromAnotherActivity = true` to trigger refresh in next onResume.

### onDestroy

1. **Configuration check**: Skip cleanup if `isChangingConfigurations` (rotation, theme change).
2. **Cache cleanup**: If `isFinishing && !isChangingConfigurations`:
   - Get cache stats from `unifiedCache.getCacheStats()`.
   - Call `unifiedCache.clearAll()` to clear network file cache.
   - Log cleared size in MB.
3. **Note**: Glide bitmap thumbnail cache is NOT cleared (persists across app restarts).

---

## ResourceAdapter Callbacks

The adapter provides 7 callback actions:

1. **onItemClick(resource)**: Simple click → Select resource → Open BrowseActivity.
2. **onItemLongClick(resource)**: Long press → Check PIN → Open EditResourceActivity.
3. **onEditClick(resource)**: Edit button → Check PIN → Open EditResourceActivity.
4. **onCopyFromClick(resource)**: Copy button → Select resource → Trigger copy mode in ViewModel.
5. **onDeleteClick(resource)**: Delete button → Show confirmation dialog → Delete resource.
6. **onMoveUpClick(resource)**: Up arrow → Call `viewModel.moveResourceUp(resource)`.
7. **onMoveDownClick(resource)**: Down arrow → Call `viewModel.moveResourceDown(resource)`.

**PIN Protection Logic**:
- If `resource.accessPin.isNotBlank()`, delegate to `passwordManager.checkResourcePinForEdit(resource)`.
- Otherwise, directly navigate to EditResourceActivity.

---

## Button Actions

### btnToggleView
- **Action**: Toggles between List Mode and Compact Grid Mode.
- **Visibility Logic**: 
  - Always visible when `isResourceGridMode == true` (allow return to list).
  - Visible when resource count > 10 (allow switch to grid for many resources).
  - Hidden otherwise.
- **Icon**: `ic_view_grid` (list mode) ↔ `ic_view_list` (grid mode).

### btnStartPlayer
- **Action**: Calls `viewModel.startPlayer()` → Smart selection logic (selected resource → last used → first available).
- **Enabled State**: Only enabled when `resources.isNotEmpty()`.

### btnAddResource
- **Action**: Calls `viewModel.addResource()` → Opens AddResourceActivity.

### btnSettings
- **Action**: Opens SettingsActivity.

### btnFilter
- **Action**: Shows `FilterResourceDialog` with current filter state (sortMode, filterByType, filterByMediaType, filterByName).
- **Callback**: Applies filters via ViewModel setters (`setSortMode`, `setFilterByType`, etc.).

### btnRefresh
- **Action**: Full scan refresh:
  1. `smbClient.forceFullReset()` - Clears SMB connection pool.
  2. `NetworkFileDataFetcher.clearFailedVideoCache()` - Retries previously failed video thumbnails.
  3. `viewModel.scanAllResources()` - Rescans all resources, updates DB.

### btnExit
- **Action**: Hard exit:
  1. `finishAffinity()` - Closes all activities.
  2. `android.os.Process.killProcess(android.os.Process.myPid())` - Terminates process.

### btnFavorites
- **Action**: Calls `viewModel.openFavorites()` → Opens BrowseActivity with FAVORITES_RESOURCE_ID (-100).
- **Visibility**: Controlled by `settingsRepository.getSettings().enableFavorites` setting.

### emptyStateView
- **Click**: Same as btnAddResource → Opens AddResourceActivity.

### btnRetry (Error State)
- **Action**: 
  1. `viewModel.clearError()` - Clears error message.
  2. `viewModel.scanAllResources()` - Retries scan operation.

---

## Tab Navigation Behavior

### Tab Setup (setupResourceTypeTabs)

Creates 5 tabs programmatically:
1. **ALL** (tab index 0): Icon `ic_view_list`, shows all resources.
2. **LOCAL** (tab index 1): Icon `ic_resource_local`, filters by LOCAL type.
3. **SMB** (tab index 2): Icon `ic_resource_smb`, filters by SMB type.
4. **FTP_SFTP** (tab index 3): Icon `ic_resource_ftp`, filters by FTP/SFTP types.
5. **CLOUD** (tab index 4): Icon `ic_resource_cloud`, filters by CLOUD types (Google Drive, OneDrive, Dropbox).

**Initial Selection**: Based on `viewModel.state.value.activeResourceTab`.

### Tab Selection Listener

**onTabSelected(tab)**:
- Maps tab position to ResourceTab enum and calls `viewModel.setActiveTab(...)`.

**onTabUnselected(tab)**:
- No action needed (state managed by ViewModel).

**onTabReselected(tab)**:
- **Position 5 (Favorites tab)**: Special case (unused in current implementation, commented out).
  - Would re-open Favorites Browse.
  - Would restore previous tab after navigation.

**Current Implementation Note**: Favorites is a button (`btnFavorites`), not a tab. The tab reselection code at position 5 is dead code.

---
