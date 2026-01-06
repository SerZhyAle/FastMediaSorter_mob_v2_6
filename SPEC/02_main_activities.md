# 2. Main Activities

## MainActivity
**Package:** `com.sza.fastmediasorter.ui.main`  
**Purpose:** Primary screen for resource management  
**ViewModel:** `MainViewModel`  
**Layout:** `activity_main.xml`

**UI Elements:**

**Root Container:**
- ConstraintLayout (match_parent × match_parent)
  - Background: default theme background
  - Contains all MainActivity UI elements

**Top Control Bar** (`layoutControlButtons` - LinearLayout):
- Width: 0dp (constrained start→end to parent)
- Height: wrap_content
- Orientation: horizontal
- Padding: 4dp all sides
- Position: Constrained to top of parent
- Contains: 10 action buttons with spacing

- `btnExit` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_close_clear_cancel
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/exit ("Exit")
  - Action: Exit app with finishAffinity() + System.exit()
  - Position: First button (leftmost)

- Space (large separator)
  - Width: 24dp
  - Height: 1dp
  - Purpose: Visual separation between Exit and Add buttons

- `btnAddResource` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_add
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/add ("Add")
  - Action: Open AddResourceActivity
  - Position: After Exit button

- Space (small separator #1)
  - Width: 8dp
  - Height: 1dp

- `btnFilter` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_search
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/filter ("Filter")
  - Action: Show FilterResourceDialog
  - Position: After Add button
  
- `btnRefresh` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_popup_sync
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/refresh ("Refresh")
  - Action: Rescan all resources, reload list
  - Position: After Filter button

- Space (small separator #2)
  - Width: 8dp
  - Height: 1dp

- `btnSettings` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_preferences
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/settings ("Settings")
  - Action: Open SettingsActivity
  - Position: After Refresh button

- Space (small separator #3)
  - Width: 8dp
  - Height: 1dp

- Space (small separator #4)
  - Width: 8dp
  - Height: 1dp
  - Note: Two consecutive 8dp spaces (16dp total)

- `btnToggleView` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_view_grid (toggles to ic_view_list)
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/toggle_view ("Toggle view")
  - Visibility: gone (shown based on settings)
  - Action: Switch between Grid/List LayoutManager
  - Position: After Settings button

- `btnFavorites` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_star_outline (filled when active)
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/tab_favorites ("Favorites")
  - Action: Toggle favorites filter
  - Position: After Toggle View button

- `btnStartPlayer` - ImageButton (double width)
  - Size: 96dp × 48dp (2x width of other buttons)
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_media_play
  - Tint: ?attr/colorControlNormal
  - ScaleType: fitCenter
  - ContentDescription: @string/start_player ("Start player")
  - Action: Start random slideshow from all resources
  - Position: Last button (rightmost)

**Tabs Section:**
- `tabResourceTypes` - TabLayout
  - Width: 0dp (constrained start→end)
  - Height: wrap_content
  - Position: Constrained top to bottom of layoutControlButtons
  - Background: ?attr/colorSurface
  - TabMode: fixed
  - TabGravity: fill
  - TabIconTint: ?attr/colorControlNormal
  - TabSelectedTextColor: ?attr/colorPrimary
  - TabTextColor: ?attr/colorControlNormal
  - TabIndicatorColor: ?attr/colorPrimary
  - Tabs (dynamically created):
    - "All Resources" (default, always present)
    - "Destinations" (shown only if destinations exist)
  - Action: Filters resources by tab selection

**Main Content:**
- `rvResources` - RecyclerView
  - Width: 0dp (constrained start→end)
  - Height: 0dp (constrained top→bottom)
  - Margins: 8dp all sides
  - Position: Between tabResourceTypes (top) and tvFilterWarning (bottom)
  - LayoutManager: LinearLayoutManager (default, switches to GridLayoutManager)
  - Scrollbars: vertical, always visible
  - ScrollbarThumbVertical: ?android:attr/colorControlNormal
  - FadeScrollbars: false
  - Adapter: ResourceAdapter
  - ListItem: @layout/item_resource
  - Shows: All resources or filtered subset based on active filters
  - ClipToPadding: true
  - Content: Resource cards with icon, name, path, file count, last scan date

**Empty State** (`emptyStateView` - LinearLayout):
- Width: wrap_content
- Height: wrap_content
- Orientation: vertical
- Gravity: center
- Padding: 32dp all sides
- Visibility: gone (shown when resources empty)
- Position: Centered in rvResources area (constrained to rvResources bounds)
- Contains:
  - ImageView
    - Size: 64dp × 64dp
    - Icon: @android:drawable/ic_input_add
    - Alpha: 0.6
    - ContentDescription: "Add resource"
  - TextView
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 16dp
    - Text: @string/empty_state_message ("No resources added yet")
    - TextSize: 16sp
    - TextAlignment: center
    - Gravity: center
    - Alpha: 0.6

**Error State** (`errorStateView` - LinearLayout):
- Width: wrap_content
- Height: wrap_content
- Orientation: vertical
- Gravity: center
- Padding: 32dp all sides
- Visibility: gone (shown on connection errors)
- Position: Centered in rvResources area
- Contains:
  - ImageView (error icon)
    - Size: 64dp × 64dp
    - Icon: @android:drawable/stat_notify_error
    - Tint: @android:color/holo_red_dark
    - ContentDescription: @string/error ("Error")
  - TextView (`tvErrorMessage`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 16dp
    - Text: @string/connection_failed (default, dynamic)
    - TextSize: 16sp
    - TextAlignment: center
    - Gravity: center
    - TextColor: @android:color/holo_red_dark
  - Button (`btnRetry`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 16dp
    - Text: @string/retry ("Retry")
    - Style: @style/Widget.Material3.Button.TextButton
    - Action: Retry failed operation

**Bottom Warning** (`tvFilterWarning` - TextView):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- MarginHorizontal: 8dp
- MarginBottom: 4dp
- Padding: 8dp all sides
- TextSize: 12sp
- TextColor: @android:color/holo_orange_dark
- Background: @android:color/darker_gray
- Visibility: gone (shown when filters active)
- Position: Constrained to bottom of parent
- Text example: "Filters: Type: LOCAL, NETWORK | Media: Images, Videos | Name: 'test'"
- Purpose: Shows active filter summary

**Loading Overlay:**
- `progressBar` - ProgressBar
  - Width: wrap_content
  - Height: wrap_content
  - Visibility: gone (shown during loading operations)
  - Style: Default circular indeterminate
  - Position: Centered on screen (all constraints to parent)
  - ContentDescription: @string/loading_indicator ("Loading")

**Scan Progress Overlay** (`scanProgressLayout` - LinearLayout):
- Width: wrap_content
- Height: wrap_content
- Orientation: vertical
- Padding: 24dp all sides
- Background: @android:drawable/dialog_holo_dark_frame
- Elevation: 8dp
- Visibility: gone (shown during background scan)
- Position: Centered on screen
- Contains:
  - TextView (`tvScanProgress`)
    - Width: wrap_content
    - Height: wrap_content
    - Text: "Scanning resources..."
    - TextSize: 16sp
    - TextStyle: bold
    - MarginBottom: 8dp
  - TextView (`tvScanDetail`)
    - Width: wrap_content
    - Height: wrap_content
    - Text: "0 files scanned" (dynamic count)
    - TextSize: 14sp
    - MarginBottom: 16dp
  - ProgressBar (`scanProgressBar`)
    - Width: 200dp
    - Height: wrap_content
    - Style: ?android:attr/progressBarStyleHorizontal
    - Indeterminate: true
    - ContentDescription: @string/scan_progress_indicator ("Scan progress")

**Features:**
- **Resource Management:** Grid/List layout, cards showing type icons, name, path, file count, dest# marker, last scan date.
- **Tab Navigation:** "All Resources", "Destinations" tabs. Favorites filter toggle.
- **Toolbar Actions:** Add, Filter, Refresh, Settings, View Toggle, Favorites, Start Player.
- **Background Scanning:** Auto-refresh, notification progress, stoppable scan.
- **Widget Support:** Handles ACTION_START_SLIDESHOW from home widget.
- **Cache Management:** Clears PDF thumbnails on exit. Preserves Glide cache.
- **Error Handling:** Retry logic, offline indicators for cloud.
- **State Persistence:** Saves scroll position, selected tab, filters.

**Functionality:**
- Add/Edit/Delete/Rescan resources via context menu or toolbar.
- Filter by type/media/name.
- Slideshow Quick Start (random shuffle all resources).
- Database integration with Room (ResourceEntity).
- Keyboard navigation fully supported.

---

## MainActivity - Detailed UI Behavior

### Lifecycle Management

**onCreate:**
- Logs: `savedInstanceState` and `isChangingConfigurations` flags
- Fixes old cloud paths: `MediaFilesCacheManager.fixCloudPaths()` (cloud:/ → cloud://)
- Welcome check: `welcomeViewModel.isWelcomeCompleted()` (fast check, no DB query)
  - If false → Launches WelcomeActivity + `finish()`
- Widget action: Checks `intent.action == ACTION_START_SLIDESHOW`
  - If true → Starts player via `binding.root.post { viewModel.startPlayer() }` (ensures UI ready)
- Initializes:
  - `KeyboardNavigationHandler` with 5 callbacks (delete, add, settings, filter, exit)
  - `ResourcePasswordManager` with context + layoutInflater
- Defers: `setupViews()` and `observeData()` to BaseActivity.onCreate()

**onResume:**
- Tab restoration: Checks `viewModel.state.value.previousTab != null`
  - Restores: Previous active tab after returning from Favorites Browse
  - Skips FAVORITES tab (action-only, not filter)
- Tab synchronization: Syncs TabLayout selection with ViewModel state
  - Maps: ResourceTab enum → Tab position (ALL=0, LOCAL=1, SMB=2, FTP_SFTP=3, CLOUD=4)
  - Updates: Only if TabLayout selection differs from ViewModel
- Refresh: Only if `isReturningFromAnotherActivity = true` (not on first launch)
  - Calls: `viewModel.refreshResources()`

**onPause:**
- Sets: `isReturningFromAnotherActivity = true` (flag for next onResume)

**onDestroy:**
- Logs: `isFinishing` and `isChangingConfigurations` flags
- Cache cleanup: Clears `UnifiedFileCache` (network file cache)
  - Condition: `isFinishing && !isChangingConfigurations` (skip on rotation)
  - Stats: Logs cache size before clearing (e.g., "X.XX MB")
  - Preserves: Glide bitmap thumbnails (separate disk cache)
- Error handling: Catches exceptions during cleanup, logs with Timber.e

**onLayoutConfigurationChanged:**
- Triggers: `updateLayoutManagerForScreenSize()` on screen rotation
- Recalculates: GridLayoutManager span count based on new screen dimensions

### Button Click Handlers

**btnExit** (Exit app):
- Action: `finishAffinity()` + `android.os.Process.killProcess(android.os.Process.myPid())`
- Effect: Terminates all activities + kills app process completely
- Use case: Fully exit app (not just return to launcher)

**btnAddResource** (Add resource):
- Action: `viewModel.addResource()`
- Result: Emits `NavigateToAddResource` event with preselectedTab
- Navigation: Opens AddResourceActivity

**btnSettings** (Open settings):
- Action: `startActivity(Intent(this, SettingsActivity::class.java))`
- Direct: Does not use ViewModel event

**btnFilter** (Filter resources):
- Action: Opens `FilterResourceDialog` (MaterialAlertDialogBuilder)
- Current state: Reads `viewModel.state.value` (sortMode, filterByType, filterByMediaType, filterByName)
- Callback: `onApply` → Calls 4 ViewModel methods:
  - `viewModel.setSortMode(sortMode)`
  - `viewModel.setFilterByType(filterByType)`
  - `viewModel.setFilterByMediaType(filterByMediaType)`
  - `viewModel.setFilterByName(filterByName)`
- Dialog: Shown with `supportFragmentManager` tag "FilterResourceDialog"

**btnRefresh** (Rescan all resources):
- Action 1: `smbClient.forceFullReset()` (resets SMB connection pool)
- Action 2: `NetworkFileDataFetcher.clearFailedVideoCache()` (retries failed video thumbnails)
- Action 3: `viewModel.scanAllResources()` (triggers background scan)
- Purpose: Force-reload all resources from network/local storage

**btnStartPlayer** (Start slideshow):
- Action: `viewModel.startPlayer()`
- Enabled: Only when `state.resources.isNotEmpty()`
- Logic: Selects last used resource OR first in list → Launches PlayerActivity with slideshow_mode=true

**btnToggleView** (Switch grid/list mode):
- Action: `viewModel.toggleResourceViewMode()`
- Effect: Toggles `isResourceGridMode` flag
- Updates: Layout manager + button icon
- Visibility: Shown when `isResourceGridMode OR resources.size > 10`

**btnFavorites** (Open favorites):
- Action: `viewModel.openFavorites()`
- Result: Emits `NavigateToFavorites` event
- Navigation: Opens BrowseActivity with FAVORITES_RESOURCE_ID=-100L
- Visibility: Controlled by `settings.enableFavorites` (observed via settingsRepository.getSettings())

**emptyStateView** (Empty state):
- Action: Click → `viewModel.addResource()`
- Same: As btnAddResource (quick add shortcut)

**btnRetry** (Error state retry):
- Action 1: `viewModel.clearError()` (clears error state)
- Action 2: `viewModel.scanAllResources()` (retries scan)

### ResourceAdapter Callbacks

**Adapter initialization:**
- **onItemClick**: Simple click → `viewModel.selectResource(resource)` + `viewModel.openBrowse()`
- **onItemLongClick**: Long click → Checks PIN → Opens EditResourceActivity
  - PIN check: `!resource.accessPin.isNullOrBlank()` → `passwordManager.checkResourcePinForEdit(resource)`
  - No PIN: Direct navigation to EditResourceActivity
- **onEditClick**: Same as onItemLongClick (shows edit icon in list item)
- **onCopyFromClick**: `viewModel.selectResource(resource)` + `viewModel.copySelectedResource()`
  - Opens: AddResourceActivity with copyResourceId
- **onDeleteClick**: `showDeleteConfirmation(resource)` (AlertDialog)
- **onMoveUpClick**: `viewModel.moveResourceUp(resource)` (reorders in database)
- **onMoveDownClick**: `viewModel.moveResourceDown(resource)` (reorders in database)

### Layout Manager Logic

**Screen size detection:**
- Reads: `resources.configuration.screenWidthDp`
- Threshold: 600dp (tablet vs phone)

**updateLayoutManagerForScreenSize():**
- Called: onCreate, onLayoutConfigurationChanged, observeData (on mode change)
- Logs: "screenWidthDp=X, isWideScreen=Y, isGridMode=Z"
- **Compact Grid Mode** (`isResourceGridMode = true`):
  - Phone portrait (< 600dp): 3 columns
  - Tablet/Landscape (≥ 600dp): 5 columns
  - Layout: GridLayoutManager
- **Detailed Mode** (`isResourceGridMode = false`):
  - Phone portrait (< 600dp): LinearLayoutManager (1 column list)
  - Tablet/Landscape (≥ 600dp): GridLayoutManager with 2 columns (detailed grid)
- Optimization: Only recreates LayoutManager if span count differs
  - Check: `currentLayoutManager !is GridLayoutManager || currentLayoutManager.spanCount != spanCount`

### Tab System (setupResourceTypeTabs)

**Tab creation:**
- 5 static tabs: ALL, LOCAL, SMB, FTP_SFTP, CLOUD
- Each tab: `.newTab()` with `.setText()` and `.setIcon()`
- Icons:
  - ALL: ic_view_list
  - LOCAL: ic_resource_local
  - SMB: ic_resource_smb
  - FTP_SFTP: ic_resource_ftp
  - CLOUD: ic_resource_cloud

**Tab selection listener:**
- `addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener)`
- **onTabSelected**: Maps position → `viewModel.setActiveTab(ResourceTab.XXXX)`
  - 0 → ALL, 1 → LOCAL, 2 → SMB, 3 → FTP_SFTP, 4 → CLOUD
- **onTabUnselected**: No action
- **onTabReselected**: Special handling for FAVORITES tab (position 5)
  - Action: `viewModel.openFavorites()`
  - Restoration: Uses `binding.tabResourceTypes.post {}` to restore previous tab after Favorites opens

**Initial selection:**
- Reads: `viewModel.state.value.activeResourceTab`
- Maps: ResourceTab → Tab index → Selects corresponding tab via `getTabAt(tabIndex)?.select()`

### State Observation (observeData)

**viewModel.state flow:**
- Scope: `repeatOnLifecycle(Lifecycle.State.STARTED)`
- Updates:
  1. `resourceAdapter.submitList(state.resources)` (updates RecyclerView)
  2. `resourceAdapter.setSelectedResource(state.selectedResource?.id)` (highlights selected)
  3. `resourceAdapter.setViewMode(state.isResourceGridMode)` (compact/detailed)
  4. `updateLayoutManagerForScreenSize()` (recalculates span count)
  5. Updates toggle button icon (ic_view_list vs ic_view_grid)
  6. Updates toggle button visibility (`isResourceGridMode OR resources.size > 10`)
  7. Enables/disables btnStartPlayer (`resources.isNotEmpty()`)
  8. Updates empty/error state visibility:
     - `isEmpty = state.resources.isEmpty()` (NOT adapter.itemCount - avoids async submitList issues)
     - `hasError = viewModel.error.value != null`
     - `errorStateView.isVisible = hasError && isEmpty`
     - `emptyStateView.isVisible = !hasError && isEmpty`
     - `rvResources.isVisible = !isEmpty`
  9. `updateFilterWarning(state)` (shows filter summary at bottom)
  10. Syncs TabLayout selection with `state.activeResourceTab` (skips FAVORITES tab)

**viewModel.loading flow:**
- Scope: `repeatOnLifecycle(Lifecycle.State.STARTED)`
- Updates: `binding.progressBar.isVisible = isLoading`

**viewModel.error flow:**
- Scope: `repeatOnLifecycle(Lifecycle.State.STARTED)`
- Updates: Error/empty state visibility + error message text

**viewModel.events flow:**
- Scope: `repeatOnLifecycle(Lifecycle.State.STARTED)`
- Handles 14 event types (see MainEvent sealed class):
  1. **ShowError**: Calls `showError(message, details)` (respects showDetailedErrors setting)
  2. **ShowInfo**: Calls `showInfo(message, details)` (respects showDetailedErrors setting)
  3. **ShowMessage**: Simple Toast.LENGTH_SHORT
  4. **ShowResourceMessage**: Toast with `getString(resId, *args)` (localized)
  5. **RequestPassword**: `passwordManager.checkResourcePassword(resource, forSlideshow, callback)`
  6. **NavigateToBrowse**: Opens BrowseActivity with `createIntent(resourceId, skipAvailabilityCheck)`
  7. **NavigateToFavorites**: Opens BrowseActivity with FAVORITES_RESOURCE_ID=-100L
  8. **NavigateToPlayerSlideshow**: Opens PlayerActivity with `slideshow_mode=true` extra
  9. **NavigateToEditResource**: Checks PIN → Opens EditResourceActivity
  10. **NavigateToAddResource**: Opens AddResourceActivity with preselectedTab
  11. **NavigateToAddResourceCopy**: Opens AddResourceActivity with copyResourceId
  12. **NavigateToSettings**: Opens SettingsActivity
  13. **ScanProgress**: Shows scanProgressLayout with "X files scanned" + current file name
  14. **ScanComplete**: Hides scanProgressLayout

**settingsRepository.getSettings() flow:**
- Scope: `repeatOnLifecycle(Lifecycle.State.STARTED)`
- Observes: `settings.enableFavorites`
- Updates: `binding.btnFavorites.visibility` (VISIBLE or GONE)

### Filter Warning (updateFilterWarning)

**Visibility logic:**
- Shown when: `filterByType != null OR filterByMediaType != null OR !filterByName.isNullOrBlank()`
- Hidden when: No filters active

**Content format:**
- Parts: Joined with " | " separator
- Examples:
  - "Type: LOCAL, NETWORK"
  - "Media: Images, Videos"
  - "Name: 'test'"
  - Combined: "Type: LOCAL | Media: Images | Name: 'test'"
- Resource: `getString(R.string.filters_active, parts.joinToString(" | "))`

### Scan Progress Overlay

**Visibility:**
- Shown: When `MainEvent.ScanProgress` emitted
- Hidden: When `MainEvent.ScanComplete` emitted

**Components:**
- `tvScanProgress`: Shows "Scanning [filename]..." (current file)
  - Format: `getString(R.string.scanning_progress, fileName)`
- `tvScanDetail`: Shows "X files scanned" (total count)
  - Format: `"${event.scannedCount} files scanned"`
- `scanProgressBar`: Indeterminate horizontal progress bar

**Purpose:**
- Provides feedback during background resource scanning
- Shows real-time progress without blocking UI

### Error/Info Dialogs

**showError(message, details):**
- Reads: `settingsRepository.getSettings().first().showDetailedErrors`
- If enabled:
  - Shows: `ErrorDialog.show(context, title, message, details)`
  - Features: Scrollable details, Copy button for text
- If disabled:
  - Shows: `Toast.makeText(message, Toast.LENGTH_LONG)`
- Logs: Timber.d with all parameters

**showInfo(message, details):**
- Same as showError but:
  - Title: `getString(R.string.information)` (not "Error")
  - Toast duration: LENGTH_SHORT (not LONG)
- Use case: Non-error notifications (empty folders, etc.)

### Delete Confirmation Dialog

**showDeleteConfirmation(resource):**
- Dialog: `AlertDialog.Builder(this)`
- Title: `R.string.delete_resource_title` ("Delete resource?")
- Message: `getString(R.string.delete_resource_message, resource.name)` ("Delete [Name]?")
- Positive button: `R.string.delete` → `viewModel.deleteResource(resource)`
- Negative button: `android.R.string.cancel` → Dismisses
- Type: Standard MaterialAlertDialogBuilder

### Keyboard Navigation (KeyboardNavigationHandler)

**Supported keys:**
- **Arrow keys** (DPAD_UP/DOWN/LEFT/RIGHT):
  - UP: List → Previous item, Grid → Up by spanCount
  - DOWN: List → Next item, Grid → Down by spanCount
  - LEFT: Grid only → Previous item
  - RIGHT: Grid only → Next item
- **Page navigation**:
  - PAGE_UP: Scroll up by visible page size
  - PAGE_DOWN: Scroll down by visible page size
  - MOVE_HOME: Scroll to position 0 (first item)
  - MOVE_END: Scroll to last item
- **Enter**: Browse selected resource (`viewModel.selectResource()` + `viewModel.openBrowse()`)
- **Delete keys** (DEL, FORWARD_DEL): Show delete confirmation for current resource
- **Escape**: Exit app completely (`finishAffinity()` + `killProcess()`)
- **Insert/+**: Add new resource (same as btnAddResource)
- **Function keys**:
  - F1: Start slideshow for selected/first resource
  - F2: Open Settings
  - F3: Open Filter dialog
  - F4: Refresh resources + Toast "Resources refreshed"
  - F5: Copy selected resource

**Grid-aware navigation:**
- Grid mode: Arrow keys move by spanCount (up/down) or single item (left/right)
- List mode: Arrow keys move by single item (left/right ignored)
- Uses: `layoutManager is GridLayoutManager` check

**Page scrolling:**
- Calculates: `pageSize = lastVisible - firstVisible`
- Page down: `lastVisible + pageSize` (clamped to max)
- Page up: `firstVisible - pageSize` (clamped to 0)

**Focus management:**
- Gets: `getCurrentFocusPosition()` from `findFirstVisibleItemPosition()`
- Scrolls: `recyclerView.scrollToPosition(position)`
- Selects: `viewModel.selectResource(resource)` after navigation

### Password Manager (ResourcePasswordManager)

**checkResourcePassword():**
- Use case: Access resource for Browse or Slideshow
- Parameters: `resource`, `forSlideshow`, `onPasswordValidated` callback
- Dialog: Shows PIN entry dialog with resource name as title
- Validation: Compares entered PIN with `resource.accessPin`
- Success: Calls callback with `(resourceId, forSlideshow)`
- Failure: Shows error in TextInputLayout (`tilPassword.error`)

**checkResourcePinForEdit():**
- Use case: Edit resource with PIN protection
- Dialog: Same PIN entry dialog
- Success: Opens EditResourceActivity with `resourceId` extra
- Logs: Timber.d on validation success

**showPinDialog() internals:**
- Layout: Inflates `dialog_access_password.xml`
- Fields: TextInputEditText (etPassword) + TextInputLayout (tilPassword)
- Positive button: Overridden to prevent auto-dismiss on error
  - Validates: `enteredPin == correctPin`
  - Correct → `dialog.dismiss()` + `onSuccess()`
  - Incorrect → `tilPassword.error = getString(R.string.pin_incorrect)` + Logs warning
- Keyboard: Auto-shows via `SOFT_INPUT_STATE_ALWAYS_VISIBLE`
- Focus: `etPassword.requestFocus()` on dialog show

### RecyclerView Optimization

**Layout Manager:**
- Phone: LinearLayoutManager (onCreate)
- Tablet (≥600dp): GridLayoutManager with 2 columns (onCreate)
- Dynamic: Recalculates on screen rotation and mode toggle

**Item Animator:**
- Type: DefaultItemAnimator
- Durations: 300ms for add/remove/move/change operations
- Purpose: Smooth animations when resources reordered

**Version Logging:**
- Runs: In background (Dispatchers.IO) during onCreate
- Retrieves: `packageManager.getPackageInfo(packageName, 0)`
- Logs: "App version: X.XX.XX (code: XXXXXX)" via Timber.d
- Non-critical: Catches and logs exceptions without crashing

### Navigation Animations

**All navigation events:**
- BrowseActivity: `overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)`
- PlayerActivity: Same slide animations
- Direction: Slides in from right (forward navigation feel)
- Deprecated: Uses `@Suppress("DEPRECATION")` (overridePendingTransition deprecated in API 34+)

### Widget Support

**ACTION_START_SLIDESHOW:**
- Intent extra: Passed from HomeWidgetProvider
- Handling: `intent?.action == ACTION_START_SLIDESHOW` in onCreate
- Delay: Uses `binding.root.post {}` to ensure UI and ViewModel ready
- Action: `viewModel.startPlayer()` (starts slideshow from last/first resource)
- Companion: `const val ACTION_START_SLIDESHOW = "com.sza.fastmediasorter.ACTION_START_SLIDESHOW"`

---

## BrowseActivity
**Package:** `com.sza.fastmediasorter.ui.browse`  
**Purpose:** File browser for a single resource  
**ViewModel:** `BrowseViewModel`  
**Layout:** `activity_browse.xml`

**UI Elements:**

**Root Container:**
- ConstraintLayout (match_parent × match_parent)
  - Background: default theme background
  - Contains all BrowseActivity UI elements

**Top Controls Bar** (`layoutControls` - LinearLayout):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- Orientation: horizontal
- Padding: 4dp all sides
- Position: Constrained to top of parent
- Contains: 9 action buttons with spacing

- `btnBack` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_arrow_back
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/back ("Back")
  - Action: Return to MainActivity
  - Position: First button (leftmost)

- Space (small separator #1)
  - Width: 8dp
  - Height: 1dp

- `btnSort` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_sort_by_size
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/sort ("Sort")
  - Action: Show sort options bottom sheet
  - Position: After Back button

- `btnFilter` - FrameLayout container (48dp × 48dp)
  - Contains:
    - ImageButton (`btnFilter`)
      - Size: match_parent × match_parent
      - Background: ?attr/selectableItemBackgroundBorderless
      - Icon: @android:drawable/ic_menu_search
      - Tint: ?attr/colorControlNormal
      - ContentDescription: @string/filter ("Filter")
      - Action: Show FilterDialog
    - TextView (`tvFilterBadge`)
      - Size: 18dp × 18dp
      - Position: layout_gravity="top|end"
      - MarginTop: 4dp, MarginEnd: 4dp
      - Background: @drawable/badge_filter_background
      - Gravity: center
      - TextColor: @android:color/white
      - TextSize: 10sp
      - TextStyle: bold
      - Visibility: gone (shown when filters active, displays filter count)

- `btnRefresh` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_popup_sync
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/refresh ("Refresh")
  - Action: Reload files from resource
  - Position: After Filter

- `btnToggleView` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_view
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/toggle_view ("Toggle view")
  - Action: Switch between Grid/List mode
  - Position: After Refresh

- Space (small separator #2)
  - Width: 8dp
  - Height: 1dp

- `btnSelectAll` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_select_all
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/select_all ("Select all")
  - Action: Select all visible files
  - Position: After Toggle View

- `btnDeselectAll` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_deselect_all
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/deselect_all ("Deselect all")
  - Action: Clear all selections
  - Position: After Select All

- Space (small separator #3)
  - Width: 8dp
  - Height: 1dp

- `btnPlay` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_media_play
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/play ("Play")
  - Action: Start slideshow from current folder
  - Position: Last button (rightmost)

**Resource Info Bar:**
- `tvResourceInfo` - TextView
  - Width: 0dp (constrained start→end)
  - Height: wrap_content
  - Padding: 8dp all sides
  - Position: Between layoutControls (top) and rvMediaFiles (bottom)
  - TextSize: 12sp
  - TextColor: ?android:textColorSecondary
  - Text example: "Downloads • /storage/emulated/0/Download • 0 selected"
  - Shows: Resource name, path, selection count

**Main Content:**
- `rvMediaFiles` - RecyclerView
  - Width: 0dp (constrained start→end)
  - Height: 0dp (constrained top→bottom)
  - Padding: 0dp
  - Position: Between tvResourceInfo (top) and layoutOperations (bottom)
  - LayoutManager: LinearLayoutManager (default, switches to GridLayoutManager)
  - Scrollbars: vertical, always visible
  - ScrollbarFadeDuration: 0 (no fade)
  - FadeScrollbars: false
  - ClipToPadding: false
  - Adapter: MediaFileAdapter or PagingMediaFileAdapter (pagination for >1000 files)
  - ListItem: @layout/item_media_file
  - Shows: Media files with thumbnails, names, metadata

**Floating Action Buttons:**
- `fabScrollToTop` - ImageButton
  - Size: 32dp × 32dp
  - Position: Top-right corner of rvMediaFiles
  - MarginEnd: 40dp, MarginTop: 8dp
  - Background: @drawable/bg_scroll_button
  - Icon: @drawable/ic_arrow_upward
  - Tint: @android:color/white
  - Padding: 6dp
  - ScaleType: centerInside
  - Visibility: gone (appears when scrolled down)
  - ContentDescription: @string/scroll_to_top ("Scroll to top")
  - Action: Smooth scroll to first item

- `fabScrollToBottom` - ImageButton
  - Size: 32dp × 32dp
  - Position: Bottom-right corner of rvMediaFiles (above layoutOperations)
  - MarginEnd: 40dp, MarginBottom: 8dp
  - Background: @drawable/bg_scroll_button
  - Icon: @drawable/ic_arrow_downward
  - Tint: @android:color/white
  - Padding: 6dp
  - ScaleType: centerInside
  - Visibility: gone (appears when scrolled up)
  - ContentDescription: @string/scroll_to_bottom ("Scroll to bottom")
  - Action: Smooth scroll to last item

**Bottom Operations Panel** (`layoutOperations` - LinearLayout):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- Orientation: horizontal
- Padding: 4dp all sides
- Background: ?attr/colorSurface
- Elevation: 4dp
- Visibility: visible (always present, buttons hidden individually)
- Position: Above tvFilterWarning, below rvMediaFiles
- Contains: 6 operation buttons (all initially gone, shown when files selected)

- `btnCopy` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_save
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/copy ("Copy")
  - Visibility: gone (shown when files selected)
  - Action: Open CopyToDialog

- `btnMove` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_revert
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/move ("Move")
  - Visibility: gone (shown when files selected)
  - Action: Open MoveToDialog

- `btnRename` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_edit
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/rename ("Rename")
  - Visibility: gone (shown when files selected)
  - Action: Open RenameDialog (single/multiple)

- `btnDelete` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_delete
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/delete ("Delete")
  - Visibility: gone (shown when files selected)
  - Action: Open DeleteDialog, move to .trash/ folder

- `btnUndo` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @android:drawable/ic_menu_revert
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/undo ("Undo")
  - Visibility: gone (shown when undo stack not empty)
  - Action: Restore last deleted/moved files from .trash/

- `btnShare` - ImageButton
  - Size: 48dp × 48dp
  - Background: ?attr/selectableItemBackgroundBorderless
  - Icon: @drawable/ic_share
  - Tint: ?attr/colorControlNormal
  - ContentDescription: @string/share ("Share")
  - Visibility: gone (shown when files selected)
  - Action: System share sheet with selected files

**Bottom Warning** (`tvFilterWarning` - TextView):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- Padding: 8dp all sides
- Background: @color/filter_warning_bg
- TextSize: 12sp
- TextColor: @color/filter_warning_text
- Visibility: gone (shown when filters active)
- Position: Constrained to bottom of parent
- Text example: "⚠ Filter active: name contains 'photo', created after 01.01.2024"
- Purpose: Shows active filter description

**Error State** (`errorStateView` - LinearLayout):
- Width: wrap_content
- Height: wrap_content
- Orientation: vertical
- Gravity: center
- Padding: 32dp all sides
- Visibility: gone (shown on connection/loading errors)
- Position: Centered on screen
- Contains:
  - ImageView (error icon)
    - Size: 64dp × 64dp
    - Icon: @android:drawable/stat_notify_error
    - Tint: @android:color/holo_red_dark
    - ContentDescription: @string/error ("Error")
  - TextView (`tvErrorMessage`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 16dp
    - Text: @string/connection_failed (default, dynamic)
    - TextSize: 16sp
    - TextAlignment: center
    - Gravity: center
    - TextColor: @android:color/holo_red_dark
  - Button (`btnRetry`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 16dp
    - Text: @string/retry ("Retry")
    - Style: @style/Widget.Material3.Button.TextButton
    - Action: Retry failed operation

**Progress Layout** (`layoutProgress` - LinearLayout):
- Width: wrap_content
- Height: wrap_content
- Orientation: horizontal
- Gravity: center
- Visibility: gone (shown during loading)
- Position: Centered on screen
- Contains:
  - ProgressBar (`progressBar`)
    - Size: 32dp × 32dp
    - Style: Default circular indeterminate
    - ContentDescription: @string/loading_indicator ("Loading")
  - TextView (`tvProgressMessage`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginStart: 12dp
    - TextSize: 16sp
    - TextColor: ?android:textColorPrimary
    - MaxLines: 1
    - MaxWidth: 250dp
    - Text example: "Loading (12345)" (shows file count)
  - MaterialButton (`btnStopScan`)
    - Width: wrap_content
    - Height: wrap_content
    - MarginStart: 16dp
    - Text: @string/stop ("Stop")
    - Style: ?attr/materialButtonOutlinedStyle
    - Visibility: gone (shown during long scans)
    - Action: Cancel ongoing scan operation

**Features:**
- **File Display:** Thumbnails (lazy loaded), names, metadata. Selection checkboxes.
- **Filtering System:** By media type, date range, name, file size.
- **Sorting Options:** Name, date, size, type, duration.
- **Selection System:** Long press, Select All, range selection.
- **File Operations:** Copy, Move, Rename, Delete, Share. Undo support (trash).
- **Pagination:** Auto-enabled for >1000 files.
- **Network Operations:** Progress notifications, cancellable.
- **Local File Monitoring:** MediaStore observer (Android 11+).

**Functionality:**
- **File Loading:** ViewModel triggers scanner -> Adapter. Error handling.
- **Selection State:** ViewModel tracks selected paths.
- **File Op Workflow:** Select -> Dialog -> UnifiedFileOperationHandler -> Result/Undo.
- **Filtering/Sorting:** Background thread processing, auto-refresh.
- **Pagination:** Paging3 integration.
- **Undo Mechanism:** Moves to .trash folder, restores on undo.
- **Network Optimization:** Thumbnail caching, connection pooling.
- **Error Handling:** Retry timeouts, permission guides.

**Dialogs Used:** CopyToDialog, MoveToDialog, DeleteDialog, RenameDialog, FileInfoDialog, Filter/Sort sheets.

**Keyboard Navigation:** Arrow keys, Enter (Open), Space (Select), Ctrl+A (Select All), Delete, F2 (Rename), Escape.

---

## BrowseActivity - Detailed UI Behavior

### Control Bar Buttons

**btnBack** (Back button):
- Action: Click → `finish()` with slide_in_left/slide_out_right animation
- Logs: `UserActionLogger.logButtonClick("Back", "BrowseActivity")`

**btnSort** (Sort button):
- Action: Click → Opens SortDialog with single-choice list
- Options: MANUAL, NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC, TYPE_ASC, TYPE_DESC, RANDOM
- Selected option → Triggers `viewModel.onSortModeSelected(sortMode)`
- Current sort mode highlighted in dialog

**btnFilter** (Filter button with badge):
- Action: Click → Opens FilterDialog (MaterialAlertDialogBuilder)
- Container: FrameLayout 48dp × 48dp (for badge overlay)
- Badge (`tvFilterBadge`):
  - Size: 18dp × 18dp
  - Position: top|end with 4dp margins
  - Background: @drawable/badge_filter_background
  - Visibility: gone by default, shown when user-defined filters active
  - Shows count of active filter criteria (e.g., "3")
  - Only shown for user filters (not resource type restrictions)
- Dialog sections:
  - **Name filter**: EditText for substring search (case-insensitive)
  - **Media types**: 7 checkboxes (IMAGE, VIDEO, AUDIO, GIF, TEXT, PDF, EPUB)
    - Only types allowed by resource are visible
    - All allowed types selected by default (= no filter)
  - **Date range**: Two EditText fields with DatePicker popups
    - Click → Opens Android DatePickerDialog
    - Format: dd.MM.yyyy (e.g., "01.01.2024")
    - Min/Max date validated
  - **Size range**: Two EditText fields for min/max size in MB
    - Accepts decimal values (e.g., "0.5" for 512KB)
- Buttons:
  - **Clear Filter**: Sets filter to null, shows all files
  - **Cancel**: Dismisses dialog without changes
  - **Apply Filter**: Validates input, creates FileFilter object
- Post-apply: Short toast with filter description (e.g., "Filter: Images, Videos • Size 1-10 MB")
- Badge update: Red circle with count visible after apply

**btnRefresh** (Refresh button):
- Action: Click → Reloads files from resource
- Side effects:
  1. Clears failed video thumbnail cache (`NetworkFileDataFetcher.clearFailedVideoCache()`)
  2. Increments adapter `refreshVersion` (forces Glide reload)
  3. Calls `viewModel.reloadFiles()`
- Logs: `UserActionLogger.logButtonClick("Refresh", "BrowseActivity")`

**btnStopScan** (Stop scan button):
- Visibility: `gone` by default
- Shows: When `isScanCancellable = true` AND `isLoading = true`
- Trigger: After 5 seconds of scanning (see BrowseViewModel)
- Action: Click → `viewModel.cancelScan()`
- Post-click: Toast "Scan stopped (X files loaded)"
- Position: Inside layoutProgress, to the right of tvProgressMessage
- Style: ?attr/materialButtonOutlinedStyle

**btnToggleView** (Display mode toggle):
- Action: Click → `viewModel.toggleDisplayMode()`
- Icon changes:
  - LIST mode → Shows ic_view_grid (offer switch to grid)
  - GRID mode → Shows ic_view_list (offer switch to list)
- Triggers: `BrowseRecyclerViewManager.updateDisplayMode()`
- Side effects:
  1. Saves scroll position before layout change
  2. Updates adapter grid/list mode
  3. Recalculates GridLayoutManager span count (based on screen width + icon size)
  4. Restores scroll position after layout change
- Grid span calculation: `spanCount = (screenWidthDp / (iconSize + 8dp)).coerceAtLeast(1)`

**btnSelectAll** (Select all button):
- Action: Click → `viewModel.selectAll()`
- Selects: All visible files (respects current filter)
- Updates: `tvResourceInfo` shows "X selected"
- Adapter: Checkbox states updated via `setSelectedPaths()`
- Logs: `UserActionLogger.logButtonClick("SelectAll", "BrowseActivity")`

**btnDeselectAll** (Deselect all button):
- Action: Click → `viewModel.clearSelection()`
- Clears: All selection state
- Updates: `tvResourceInfo` shows "0 selected"
- Adapter: All checkboxes cleared
- Logs: `UserActionLogger.logButtonClick("DeselectAll", "BrowseActivity")`

**btnPlay** (Start slideshow):
- Action: Click → `startSlideshow()`
- Logic:
  1. Find start position: lastViewedFile → first selected → first in list
  2. Launch PlayerActivity with `slideshow_mode = true` extra
  3. Slideshow only for media files (not TEXT/PDF/EPUB)
- Animation: slide_in_right/slide_out_left
- Toast: "No files to play" if list empty
- Toast: "File unavailable" if lastViewedFile not found
- Position: Rightmost button in control bar

### Operations Panel (layoutOperations)

**Visibility Rules:**
- Panel shown when: `hasSelection OR lastOperation != null`
- Individual buttons shown based on:
  - `hasSelection` (files selected)
  - `isWritable` (resource.isWritable && !resource.isReadOnly)
  - `hasDestinations` (from GetDestinationsUseCase)

**btnCopy** (Copy files):
- Visibility: `hasSelection`
- Action: Click → `showCopyDialog()`
- Dialog: Lists all destination resources (excluding current)
- Execution: `fileOperationsManager.showCopyDialog()`
- Post-copy: Toast "X files copied to [destination]"

**btnMove** (Move files):
- Visibility: `hasSelection AND isWritable`
- Action: Click → `showMoveDialog()`
- Dialog: Lists writable destination resources
- Check: Safe Mode setting (`confirmMove`)
- Execution: Calls FileOperationUseCase
- Post-move: Toast "X files moved to [destination]"
- Side effect: Creates UndoOperation

**btnRename** (Rename files):
- Visibility: `hasSelection AND isWritable`
- Action: Click → `showRenameDialog()`
- Single file: RenameDialog with EditText
- Multiple files: RecyclerView with EditText for each file
  - Auto-focus first field after 200ms delay
  - TextWatcher updates list on input
  - Validates: No blank names, no duplicates
- Post-rename: Toast "X files renamed"
- Side effect: Creates UndoOperation with oldNames list

**btnDelete** (Delete files):
- Visibility: `hasSelection AND isWritable`
- Action: Click → `showDeleteConfirmation()`
- Check: Safe Mode setting (`confirmDelete`)
- Confirmation dialog: "Delete X files?"
- Execution: Soft delete → moves to `.trash/` folder
- Post-delete: Shows Snackbar with Undo button (5-minute timeout)
- Side effect: Creates UndoOperation with trash paths

**btnUndo** (Undo last operation):
- Visibility: `lastOperation != null` (survives 5 minutes)
- Action: Click → `viewModel.undoLastOperation()`
- Supports: DELETE, COPY, MOVE, RENAME
- Execution:
  - DELETE: Moves files from `.trash/` back to original location
  - COPY: Deletes copied files
  - MOVE: Moves files back to source
  - RENAME: Renames files back to old names
- Post-undo: Toast "Undo successful"
- Expiration: `clearExpiredUndoOperation()` runs in onResume (>5 minutes)

**btnShare** (Share files):
- Visibility: `hasSelection`
- Action: Click → `shareSelectedFiles()`
- Execution: `fileOperationsManager.shareSelectedFiles()`
- Creates: Android Intent.ACTION_SEND_MULTIPLE
- Supports: Multiple files via ClipData
- Downloads: Network files to temp cache before sharing

### RecyclerView (rvMediaFiles)

**Optimization Features:**
- **Shared ViewPool**: 30 list holders + 40 grid holders pre-allocated
- **Item cache size**: Dynamically calculated `((screenHeightDp / 80) * 2).coerceIn(20, 50)`
- **Fixed size**: `setHasFixedSize(true)` for better performance
- **Prefetch**: 
  - LinearLayoutManager: 4 items ahead (~1 row)
  - GridLayoutManager: 6 items ahead (~2 rows for 3-column grid)
- **Scroll listener**: UserActionLogger tracks scroll events
- **FastScroller**: Interactive scrollbar (can drag with finger/mouse)

**Thumbnail Loading Strategy:**
- Initial load: `skipInitialThumbnailLoad = true` flag set before `submitList()`
- Deferred loading: After layout complete, triggers via `notifyItemRangeChanged(firstVisible, visibleCount, "LOAD_THUMBNAILS")`
- Trigger timing:
  1. If already laid out → Immediate trigger
  2. If not laid out → Uses `post {}` to wait for children
- Visible range calculation: Uses `findFirstVisibleItemPosition()` and `findLastVisibleItemPosition()`
- Reset: `skipInitialThumbnailLoad = false` after trigger
- Logs: Extensive debug logging for troubleshooting

**Scroll Position Restoration:**
- Priority 1: `lastViewedFile` (return from PlayerActivity)
  - Triggered by `shouldScrollToLastViewed` flag
  - Finds file position in list
  - Scrolls with `scrollToPositionWithOffset(position, 0)`
  - Logs: "Scrolled to 'filename.jpg' at position X"
- Priority 2: `lastScrollPosition` (first open or back from MainActivity)
  - Triggered by `isFirstResume` flag
  - Validates position < itemCount
  - Scrolls after `submitList()` callback
- Saved: In `onPause()` via `stateManager.saveScrollPosition()`

**Empty State:**
- Hidden when: `itemCount > 0` OR `isLoading = true`
- Shown when: `itemCount = 0` AND `isLoading = false`
- Text changes:
  - No filter: "No files found"
  - Filter active: "No files match filter"

### Floating Action Buttons (FABs)

**fabScrollToTop**:
- Size: 32dp × 32dp
- Position: Top-right corner, 40dp from end, 8dp from top
- Background: @drawable/bg_scroll_button
- Icon: ic_arrow_upward (white)
- Visibility: `fileCount > 20` (threshold)
- Action: Click → `scrollToPositionWithOffset(0, 0)` with appropriate LayoutManager
- Logs: "Scrolled to top (position 0)"

**fabScrollToBottom**:
- Size: 32dp × 32dp
- Position: Bottom-right corner, 40dp from end, 8dp from bottom (above layoutOperations)
- Background: @drawable/bg_scroll_button
- Icon: ic_arrow_downward (white)
- Visibility: `fileCount > 20` (threshold)
- Action: Click → `scrollToPositionWithOffset(itemCount - 1, 0)` with appropriate LayoutManager
- Logs: "Scrolled to bottom (position X)"

### MediaFileAdapter Interactions

**List ViewHolder** (item_media_file.xml):
- **Thumbnail size**: 64dp × 64dp (standard), 32dp × 32dp (disableThumbnails mode)
- **Checkbox** (`cbSelect`):
  - Click → Toggles selection for single file
  - Calls `onSelectionChanged(file, isChecked)`
- **Thumbnail** (`ivThumbnail`):
  - Click → Opens file in PlayerActivity
- **Card/Root**:
  - Click → Opens file in PlayerActivity
  - Long press → Calls `onFileLongClick(file)` (enables selection mode)
- **Background color**: Changes to R.color.item_selected when selected
- **File info**: Shows "size • date" (hides invalid FTP metadata: size=0 or date=1970)
- **Favorite button** (`btnFavorite`):
  - Visibility: Controlled by `showFavoriteButton` setting
  - Icon: ic_star_filled (favorite) / ic_star_outline (not favorite)
  - Click → Toggles favorite status
- **Operation buttons**: btnCopyItem, btnMoveItem, btnRenameItem, btnDeleteItem
  - Visibility: `hasDestinations` AND `isWritable` AND NOT (isGridMode AND hideGridActionButtons)
  - Click → Triggers operation for single file

**Grid ViewHolder** (item_media_file_grid.xml):
- **Thumbnail size**: Dynamic based on `thumbnailSize` setting (96dp default, 64dp when disabled)
- **Checkbox** (`cbSelect`):
  - Click → Toggles selection
  - Long press → Range selection from last selected file
  - Logs: Only for long press on unchecked checkbox
- **Thumbnail** (`ivThumbnail`):
  - Click → Opens file
- **Card/Root**:
  - Click → Opens file
  - Long press → Enables selection mode
- **CardView** (`cvCard`):
  - Background color: R.color.item_selected when selected
- **File name**: Width matches thumbnail size
- **Favorite button**: Same as List mode
- **Operation buttons**: Same visibility rules as List mode

**Thumbnail Loading Details:**
- **Disabled mode**: Shows extension icons (no Glide loading)
  - IMAGE/VIDEO/GIF: Static placeholder icons
  - AUDIO/TEXT/EPUB: Generated bitmap with extension text
  - PDF: Static placeholder icon
- **Network paths** (SMB/SFTP/FTP):
  - Uses `NetworkFileData` model loader
  - Failed thumbnails cached (`NetworkFileDataFetcher.markThumbnailAsFailed()`)
  - Video decoder failures detected and cached (`isVideoDecoderException()`)
  - Thumbnail size: Fixed CACHED_THUMBNAIL_SIZE (300px) for cache stability
- **Cloud paths** (Google Drive):
  - Uses `GoogleDriveThumbnailData` model loader
  - Loads from `file.thumbnailUrl` field
  - Extracts fileId from `cloud://googledrive/{fileId}` path
- **PDF thumbnails**:
  - Size limits based on "Large PDF Thumbnails" setting:
    - Enabled: SMB 50MB, Network 10MB
    - Disabled: SMB 3MB, Network 1MB
  - Over limit → Shows "PDF" extension bitmap
  - Uses PdfPageDecoder registered in GlideAppModule
- **EPUB covers**:
  - Size limits: SMB 50MB, Network 10MB
  - Over limit → Shows "EPUB" extension bitmap
  - Uses EpubCoverDecoder registered in GlideAppModule
- **Glide settings**:
  - Priority: HIGH for images, NORMAL for videos
  - DiskCacheStrategy: RESOURCE (network), DATA (local)
  - Override size: 300px fixed for all modes (cache stability)
  - CrossFade transition: 100ms
  - Placeholder/Error: Type-specific icons
- **Cache stats**: Recorded via `GlideCacheStats.recordLoad(dataSource)`
- **View recycling**: Explicitly clears Glide requests in `onViewRecycled()` to free ConnectionThrottleManager slots

### Progress and Error States

**layoutProgress** (Loading state):
- Visibility: `isLoading = true`
- Components:
  - ProgressBar: 32dp circular indeterminate
  - tvProgressMessage: "Loading" or "Loading (X)" with file count
  - btnStopScan: Shown after 5 seconds (`isScanCancellable`)
- Position: Centered on screen

**errorStateView** (Error state):
- Visibility: `errorMessage != null` AND `itemCount = 0`
- Components:
  - ImageView: 64dp error icon (@android:drawable/stat_notify_error)
  - tvErrorMessage: Error text (dynamic)
  - btnRetry: Retry button
- Action (btnRetry): Click → `viewModel.clearError()` + `viewModel.reloadFiles()`
- Position: Centered on screen

**tvFilterWarning** (Filter warning):
- Visibility: `gone` (removed per user request, now shows short toast instead)
- Previous behavior: Showed active filter description at bottom
- Current behavior: Filter notification via toast only

### Resource Info Bar (tvResourceInfo)

**Format:**
- Pattern: `"ResourceName • /path/to/resource • X selected"`
- Examples:
  - No selection: "Downloads • /storage/emulated/0/Download • 0 selected"
  - With selection: "SMB Server • //192.168.1.100/media • 5 selected"
- Updates: On selection change, resource load
- Text size: 12sp
- Text color: ?android:textColorSecondary
- Padding: 8dp all sides

### Cloud Authentication

**Trigger:**
- BrowseEvent.ShowCloudAuthenticationRequired emitted by ViewModel
- Triggered when: Google Drive resource opened without valid token
- Retry: Attempts silent token refresh first

**Dialog:**
- Title: "Authentication required"
- Message: "Cloud resource 'Google Drive' requires authentication. Sign in now?"
- Buttons:
  - "Sign in now" → Launches `cloudAuthManager.launchGoogleSignIn()`
  - "Cancel" → Returns to MainActivity
- Copy error button: Copies error message to clipboard

**Google Sign-In Flow:**
- Launcher: `googleSignInLauncher` (ActivityResultContract)
- Handler: `handleGoogleSignInResult(data)`
- Success → Stores token in EncryptedSharedPreferences
- Success → Calls `viewModel.reloadFiles()`
- Failure → Shows error dialog

### Small Controls Mode

**Trigger:**
- State flag: `showSmallControls` (from BrowseState)
- Manager: BrowseSmallControlsManager

**Behavior:**
- Scales all command panel buttons to 50% height (0.5f)
- Scales margins and paddings proportionally
- Applies to: All control bar buttons + operations panel buttons
- Original heights: Saved before first application
- Restoration: `restoreCommandButtonHeightsIfNeeded()` when disabled
- Flag: `smallControlsApplied` prevents double-application

**Note:**
- Toolbar scaling commented out (View ID not found in layout)

### MediaStore Observer

**Purpose:**
- Auto-refresh when local files change (Android 11+)
- Detects: File creation, deletion, modification

**Manager:**
- BrowseMediaStoreObserver
- Lifecycle: `start()` in onResume, `stop()` in onPause
- Filter: Only observes resource's media type (IMAGE/VIDEO/AUDIO)

**Behavior:**
- Change detected → Callback to BrowseActivity
- Activity → Calls `viewModel.reloadFiles()`
- Skipped: For network/cloud resources

### Settings Integration

**Observed Settings:**
- `enableFavorites`: Shows/hides favorite button
- `hideGridActionButtons`: Hides quick action buttons in grid mode
- `defaultIconSize`: Updates grid cell size on change
- `showVideoThumbnails`: Controls video thumbnail loading
- `showPdfThumbnails`: Controls PDF size limits (Large PDF Thumbnails)

**Setting Changes:**
- Triggers: `notifyDataSetChanged()` to update all items
- PDF setting change: Forces thumbnail reload via `notifyItemRangeChanged(0, itemCount, "LOAD_THUMBNAILS")`
- Icon size change: Updates GridLayoutManager span count

### Keyboard Navigation (KeyboardNavigationManager)

**Arrow Keys:**
- **DPAD_UP**: List mode → Previous item, Grid mode → Up by spanCount
- **DPAD_DOWN**: List mode → Next item, Grid mode → Down by spanCount
- **DPAD_LEFT**: Grid mode only → Previous item
- **DPAD_RIGHT**: Grid mode only → Next item

**Page Navigation:**
- **PAGE_UP**: Scroll up by visible page size
- **PAGE_DOWN**: Scroll down by visible page size
- **MOVE_HOME**: Scroll to position 0 (first item)
- **MOVE_END**: Scroll to last item

**Selection Actions:**
- **SPACE**: Toggle current item selection
- **ENTER**: Play current focused item OR first selected file

**Function Keys:**
- **ESCAPE**: Back to MainActivity
- **DEL / FORWARD_DEL**: Show delete confirmation (if files selected)

**Focus Handling:**
- `getCurrentFocusPosition()`: Returns current focused RecyclerView position
- `scrollToPosition(position)`: Scrolls and requests focus on ViewHolder
- `toggleCurrentItemSelection(position)`: Toggles selection and logs action

### Lifecycle Events

**onCreate:**
- Initializes 11 managers (Dialog, FileOperations, RecyclerView, Keyboard, SmallControls, CloudAuth, Utility, State, MediaStoreObserver, ActionBar, Selection)
- Sets up click listeners (19 total)
- Configures RecyclerView optimization (cache, prefetch, shared pool)
- Observes 5 StateFlow/SharedFlow from ViewModel

**onResume:**
- Skips reload on first resume (`isFirstResume` flag)
- Checks resource settings changed → Reloads if needed
- Clears expired undo operations (>5 minutes)
- Starts MediaStore observer for local resources
- Scroll restoration → Handled in submitList callback

**onPause:**
- Stops MediaStore observer
- Saves scroll position via `stateManager`
- Sets `shouldScrollToLastViewed = true` flag
- Cancels background thumbnail loading (frees bandwidth for PlayerActivity)
- Note: Adapter NO LONGER cleared (preserves memory cache up to 1GB)

**onDestroy:**
- Memory cache cleared
- Glide requests canceled
- Managers cleaned up

### Configuration Changes

**onLayoutConfigurationChanged:**
- Triggered on screen rotation
- Forces display mode recalculation with new screen dimensions
- Resets `currentDisplayMode` to null → Triggers updateDisplayMode()
- Logs: "Recalculated display mode for screenWidthDp=X"

### Error Handling

**showError() Method:**
- Checks: `showDetailedErrors` setting
- If enabled: Shows ErrorDialog with full details + Copy button
- If disabled: Shows simple Toast
- Special case: Google Drive auth errors → Launches CloudAuthenticationDialog
- Supports: Exception with stack trace OR message + details

### Snackbar (Undo Notification)

**Trigger:**
- BrowseEvent.ShowUndoToast emitted after file operation
- Replaces previous Toast implementation

**Content:**
- DELETE: "Deleted X files"
- COPY: "Copied X files to [destination]"
- MOVE: "Moved X files to [destination]"
- RENAME: "Renamed X files"

**Action Button:**
- Text: "UNDO" (uppercase)
- Action: Calls `viewModel.undoLastOperation()`
- Duration: LONG (5 seconds)
- Anchor: layoutOperations (positioned above operations panel)
