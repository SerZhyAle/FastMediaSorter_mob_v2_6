# 7. Settings Activity

## SettingsActivity
**Package:** `com.sza.fastmediasorter.ui.settings`  
**Purpose:** Global app configuration  
**ViewModel:** `SettingsViewModel`  
**Layout:** `activity_settings.xml`

**UI Elements:**

**Toolbar:**
- `toolbar` - MaterialToolbar
  - Title: "Settings"
  - Navigation icon: ic_arrow_back
  - Background: colorPrimary

**Tab Navigation:**
- `tabLayout` - TabLayout
  - 5 tabs: General | Media | Playback | Destinations | Network
  - Fixed mode with equal spacing
  - Indicator color: colorPrimary

**Content:**
- `viewPager` - ViewPager2
  - 5 fragments (see below)
  - No page transitions (instant switch via custom transformer)
  - OffscreenPageLimit: 1

**Tabs (ViewPager2):**

#### 7.1 General Settings Tab (`GeneralSettingsFragment`)
Layout: `fragment_settings_general.xml`

**Key UI Sections:**
- Language Selection (Spinner): English/Russian/Ukrainian
- Theme (RadioGroup): Light/Dark/System
- Display Mode (Spinner): Grid/List/Auto
- Grid Columns (Slider): 1-10 with live preview
- File Name Display (Switch): Full/Truncated
- Show Hidden Files (Switch)
- **Universal Access:**
  - Work With All Files (Switch): Shows non-media files (*.*) in browser
- Date Format (Spinner): Multiple formats
- **Sync Section:**
  - Enable Sync (Switch)
  - Sync Interval (Slider): 1-60 minutes
  - Sync Now (Button)
- **Behavior Section:**
  - Enable Favorites (Switch)
  - Prevent Sleep During Playback (Switch)
  - Confirm Delete (Switch)
  - Confirm Move (Switch)
- **Cache Section:**
  - Cache Size Limit (Spinner): 0.5GB-10GB/Unlimited
  - Auto Cache Management (Switch)
  - Calculate Optimal Size (Button)
  - Clear Cache Now (Button)
  - Current Cache Size Display (TextView)
- App Version Info (TextView)

**Layout Adaptation:**
- Portrait: Vertical stacking
- Landscape: Horizontal pairing (Sync+Interval, Delete+Move, etc.)

#### 7.2 Media Settings Tab (`MediaSettingsFragment`)
Layout: `fragment_settings_media_container.xml`

**Nested TabLayout + ViewPager2:**
- 5 sub-tabs: Images | Videos | Audio | Documents | Other

**Sub-fragments (via MediaCategoryPagerAdapter):**

**Images (`ImagesSettingsFragment`):**
- Thumbnail Quality (Radio): Low/Medium/High
- Load Full Resolution (Switch)
- Auto-Rotate Images (Switch)
- Show EXIF Data (Switch)
- JPEG Compression (Slider): 50-100%
- Image Cache Strategy (Spinner): Memory/Disk/Both

**Videos (`VideoSettingsFragment`):**
- Video Quality (Spinner): Auto/Low/Medium/High
- Hardware Acceleration (Switch)
- Auto-Play on Open (Switch)
- Show Video Controls (Switch)
- Seek Increment (Slider): 5-60 seconds
- Generate Thumbnails (Switch)
- Video Preview Duration (Spinner): 3s/5s/10s

**Audio (`AudioSettingsFragment`):**
- Show Waveform (Switch)
- Waveform Style (Spinner): Bar/Line/Filled
- Waveform Color (Color Picker)
- Background Playback (Switch)
- Audio Focus Handling (Switch)

**Documents (`DocumentsSettingsFragment`):**
- PDF Page Cache (Slider): 5-50 pages
- PDF Render Quality (Spinner): Low/Medium/High
- Text Encoding Detection (Switch)
- Default Encoding (Spinner): UTF-8/UTF-16/Windows-1251/etc.
- Syntax Highlighting (Switch)
- Text Zoom Level (Slider): 50-200%

**Other Media (`OtherMediaSettingsFragment`):**
- GIF Auto-Play (Switch)
- GIF Playback Speed (Slider): 0.25x-4x
- GIF Frame Rate Limit (Spinner): 15/30/60 FPS
- Support EPUB (Switch)
- EPUB Font (Spinner): Serif/Sans/Mono
- EPUB Font Size (Slider): 12-24sp

#### 7.3 Playback Settings Tab (`PlaybackSettingsFragment`)
Layout: `fragment_settings_playback.xml`

**Slideshow Section:**
- Default Interval (Slider): 1-60 seconds
- Random Order (Switch)
- Loop Mode (Radio): Once/Repeat/Shuffle
- Auto-Start on Browse (Switch)

**Touch Zones Section:**
- Enable Touch Zones (Switch)
- Touch Zone Sensitivity (Slider): 100-300ms hold time
- Show Zone Overlay (Switch)

**Command Panel Section:**
- Auto-Hide Delay (Slider): 0-10 seconds (0=never)
- Panel Position (Radio): Top/Bottom
- Show Thumbnails in Panel (Switch)

**Video Playback Section:**
- Repeat Mode (Radio): None/One/All
- Default Speed (Spinner): 0.25x to 2x
- Resume from Last Position (Switch)
- Skip Silence in Audio (Switch)

#### 7.4 Destinations Settings Tab (`DestinationsSettingsFragment`)
Layout: `fragment_settings_destinations.xml`

**Main Content:**
- `rvDestinations` - RecyclerView
  - Adapter: DestinationsAdapter (with drag-and-drop)
  - Shows up to 10 destination resources
  - Each item displays:
    - Drag handle icon (vertical dots)
    - Destination number badge (1-10)
    - Resource name (TextView)
    - Resource path (TextView, truncated)
    - Color marker (colored circle, clickable)
    - File count (TextView)
    - Free space (TextView)
    - Remove button (X icon)
  - Drag to reorder → Changes destination numbers
  - Click color marker → ColorPickerDialog
  - Click X → Remove destination marking

**Quick Actions:**
- Compact Display Mode (Switch): Smaller items
- Show Free Space (Switch)
- Show File Counts (Switch)

**Empty State:**
- Message: "No destinations configured"
- Button: "Add destination from resources" → Opens MainActivity

---

#### 7.5 Network Settings Tab (`NetworkSettingsFragment`)
**Package:** `com.sza.fastmediasorter.ui.settings`  
**Layout:** `fragment_settings_network.xml`

**UI Elements:**

**Network Sync Section Header:**
- TextView: "Network Sync Settings" (18sp, bold)

**Background Sync Control:**
- `switchEnableBackgroundSync` - SwitchMaterial
  - Label: "Enable background sync"
  - Default: checked (enabled)
  - Action: Enables/disables periodic WorkManager job
  - Position: Left side with label on right

**Description:**
- TextView: Background sync explanation (12sp, secondary color)
  - Text: "Automatically sync network resources in the background"

**Sync Interval Section:**
- Label TextView: "Sync interval" (14sp)
- `tvSyncIntervalValue` - TextView
  - Shows current value (e.g., "4 hours")
  - Size: 12sp, secondary color
  - Updates dynamically as slider moves

**Sync Interval Slider:**
- `sliderSyncInterval` - Material Slider
  - Range: 1-24 hours
  - Default: 4 hours
  - Step size: 1 hour
  - Shows value in plural format (e.g., "1 hour", "4 hours")
  - Action: Updates background sync schedule if enabled

**Manual Sync Button:**
- `btnSyncNow` - MaterialButton
  - Text: "Sync now"
  - Icon: ic_popup_sync (system refresh icon)
  - Action: Immediately triggers SyncNetworkResourcesUseCase
  - Disabled during sync operation

**Sync Status Display:**
- `tvSyncStatus` - TextView
  - Shows current sync state
  - Size: 12sp, secondary color, italic
  - States:
    - "Idle" (default)
    - "In progress..." (during sync)
    - "Completed: X resources synced" (success)
    - "Failed" (error)

**Features:**
- **Background Sync Management:**
  - Enable/disable periodic background sync
  - Uses WorkManager for reliable scheduling
  - Respects battery optimization settings
  - Automatically reschedules on interval change
- **Configurable Interval:**
  - Range: 1-24 hours
  - Persists selection across app restarts
  - Live updates: Changes take effect immediately if sync enabled
  - Plural-aware display (e.g., "1 hour" vs "2 hours")
- **Manual Sync Trigger:**
  - On-demand sync of all network resources (SMB/SFTP/FTP/Cloud)
  - Shows progress in status TextView
  - Disables button during operation to prevent duplicate requests
  - Success message shows count of synced resources
  - Error message displays failure reason
- **Status Feedback:**
  - Real-time status updates during sync
  - Snackbar notifications for actions:
    - "Background sync enabled"
    - "Background sync disabled"
    - "Sync completed successfully: X resources"
    - "Sync failed: [error message]"
  - Status persists after operation completes

**Functionality:**
- **Background Sync Scheduling:**
  - Switch ON: Calls `scheduleNetworkSyncUseCase(intervalHours)`
  - Switch OFF: Calls `scheduleNetworkSyncUseCase.cancel()`
  - WorkManager creates PeriodicWorkRequest with configured interval
  - Work runs in background even when app closed
- **Interval Updates:**
  - Slider change: Updates displayed value via `tvSyncIntervalValue`
  - If sync enabled: Automatically reschedules with new interval
  - Uses resource string plurals for correct grammar
- **Manual Sync Process:**
  1. Disables "Sync now" button
  2. Sets status to "In progress..."
  3. Calls `syncNetworkResourcesUseCase.syncAll()` on IO dispatcher
  4. On success: Shows count of synced resources
  5. On failure: Shows error message
  6. Re-enables button in finally block
- **Use Case Integration:**
  - `ScheduleNetworkSyncUseCase`: WorkManager scheduling
  - `SyncNetworkResourcesUseCase`: Performs actual sync operation
  - Both injected via Hilt
- **Analytics:**
  - Logs sync enable/disable events (Timber)
  - Tracks interval changes
  - Reports sync success/failure with counts
  - Monitors manual sync frequency

---

**Features:**
- **General Settings:**
  - Language: Switches app locale (en/ru/uk) via LocaleHelper
  - Theme: Light/Dark/System (persisted, applied via AppCompatDelegate)
  - Display Mode: Grid/List/Auto (affects BrowseActivity layout)
  - Grid Columns: 1-10 with live preview in MainActivity
  - File Name Display: Full names vs. truncated with ellipsis
  - Show Hidden Files: Includes files starting with . (dot)
  - Date Format: Multiple formats (DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD, etc.)
  - Sync Interval: Background resource refresh (1-60 minutes)
  - Sync Now: Manual immediate sync of all resources
  - Cache Management: Size limit selection (0.5GB-10GB/Unlimited)
  - Auto Cache: Glide automatic cache cleanup
  - Clear Cache: Manual cache purge with size calculation
- **Media Settings (Nested Tabs):**
  - **Images:**
    - Thumbnail Quality: Low (256x256), Medium (512x512), High (1024x1024)
    - Load Full Resolution: Downloads original size for zoom
    - Auto-Rotate: Applies EXIF orientation automatically
    - Show EXIF: Displays metadata in FileInfoDialog
    - JPEG Compression: 50-100% for edited images
    - Cache Strategy: Memory only, Disk only, or Both
  - **Videos:**
    - Video Quality: Auto (adaptive), Low (480p), Medium (720p), High (1080p+)
    - Hardware Acceleration: Uses GPU decoding (ExoPlayer)
    - Auto-Play: Starts playback immediately on open
    - Show Controls: Overlay play/pause/seek controls
    - Seek Increment: Arrow key/button jump (5-60 seconds)
    - Generate Thumbnails: Creates preview images for quick access
    - Preview Duration: Thumbnail generation length (3s/5s/10s)
  - **Audio:**
    - Show Waveform: Visualizes audio amplitude (ExoPlayer visualization)
    - Album Art Display: Shows cover art if embedded
    - Auto-Play Next: Continues to next track
    - Repeat Mode: None/One/All tracks
    - Shuffle: Random playback order
  - **Documents (PDF/Text/EPUB):**
    - Default Font Size: 12sp-24sp for text viewer
    - Default Encoding: UTF-8, CP1251, ISO-8859-1, Windows-1252
    - PDF Page Cache: Number of pages pre-rendered (1-10)
    - Syntax Highlighting: Enables code highlighting for text files
    - EPUB Font Family: Serif/Sans-serif
    - EPUB Line Height: 1.2x-2.0x
  - **Other (GIFs):**
    - GIF Auto-Play: Starts animation on open
    - GIF Loop: Continuous playback
    - Max GIF Size: Memory limit for large GIFs (10MB-100MB)
- **Playback Settings:**
  - Slideshow Interval: 1-60 seconds per file
  - Random Order: Shuffles playlist
  - Loop Mode: Once/Repeat/Shuffle entire playlist
  - Auto-Start: Begins slideshow on BrowseActivity open
  - Touch Zone Sensitivity: Hold time for zone detection (100-300ms)
  - Show Zone Overlay: Visual grid overlay for debugging
  - Auto-Hide Delay: Command panel fade-out (0-10s, 0=never)
  - Panel Position: Top or Bottom command bar
  - Show Thumbnails: Thumbnail previews in command panel
  - Repeat Video: None/One/All video files
  - Default Speed: Playback speed (0.25x-2x)
  - Resume Position: Continue from last viewed time
  - Skip Silence: Removes silent audio segments
- **Destinations Settings:**
  - Drag-and-drop reordering of 10 destinations
  - Color picker for each destination marker
  - Remove destination marking
  - Show/hide file counts and free space
  - Compact display mode for smaller items

**Functionality:**
- **Settings Persistence:**
  - All settings stored in SharedPreferences
  - ViewModel observes changes and applies immediately
  - Some settings require app restart (locale, theme)
  - Others applied in real-time (grid columns, cache size)
- **Tab Navigation:**
  - ViewPager2 with 5 fragments (General/Media/Playback/Destinations/Network)
  - TabLayout synchronized with ViewPager2
  - Keyboard navigation: Arrow keys, Page Up/Down
  - State preserved on configuration change
- **Cache Management:**
  - Calculate button: Scans disk to determine optimal cache size
  - Clear button: Deletes Glide cache + PDF thumbnail cache
  - Size display: Shows current cache usage in MB/GB
  - Auto-management: Glide LRU eviction when limit reached
- **Sync Configuration:**
  - Sync interval: Sets WorkManager periodic work request
  - Sync now: Triggers immediate background scan
  - Notification: Shows sync progress in status bar
- **Media Settings Application:**
  - Thumbnail quality: Changes Glide RequestOptions
  - Video quality: Updates ExoPlayer track selector
  - Font size: Applies to TextView in PlayerActivity
  - Encoding: Default for TextViewerManager
- **Destinations Management:**
  - RecyclerView with ItemTouchHelper for drag-and-drop
  - Drag handle: Press and hold vertical dots icon
  - Reorder: Changes destination numbers in database
  - Color picker: Opens ColorPickerDialog, saves to ResourceEntity
  - Remove: Clears isDestination flag in database
- **Live Preview:**
  - Grid columns: Updates MainActivity RecyclerView instantly
  - Theme: Recreates activity to apply new theme
  - Locale: Recreates all activities to update strings
- **Validation:**
  - Cache size: Min 0.5GB, warns if <1GB
  - Sync interval: Min 1 minute, max 60 minutes
  - Font size: Min 8sp, max 24sp
- **Analytics:**
  - Tracks most changed settings
  - Reports cache size distribution
  - Monitors theme preferences

---

## Detailed UI Behaviors and Interactions

### SettingsActivity - Main Activity

**Toolbar:**
- Navigation icon: ic_arrow_back
- Click → `finish()` (returns to previous activity)
- Title: "Settings" (static)

**Keyboard Navigation:**
- **DPAD_LEFT / PAGE_UP**: Previous tab (if currentItem > 0)
- **DPAD_RIGHT / PAGE_DOWN**: Next tab (if currentItem < itemCount - 1)
- **ESCAPE**: Exit settings (`finish()`)
- **TAB**: Next UI element (SHIFT+TAB for previous)
- **DPAD_DOWN**: Navigate to next focusable element
- **DPAD_UP**: Navigate to previous focusable element

**ViewPager2:**
- Page transitions: Disabled via custom PageTransformer (instant switch)
  - `translationX = 0f`, `alpha = 1f` (visible) or `0f` (hidden)
  - No animation between tabs
- offscreenPageLimit: 1 (pre-loads adjacent tab)
- Tab labels: General / Media / Playback / Destinations

---

### GeneralSettingsFragment - Detailed Behaviors

**Version Info (tvVersionInfo):**
- Format: `"VERSION_NAME | Build VERSION_CODE | sza@ukr.net"`
- Example: "2.51.2161.854 | Build 2161854 | sza@ukr.net"
- Non-interactive, informational only

**Language Spinner:**
- Options: English (0), Russian (1), Ukrainian (2)
- Selection → Shows restart dialog: "Change language to [Language Name]? App will restart."
- Positive button: "Restart" → Saves to SharedPreferences + DataStore → Calls `LocaleHelper.restartApp()`
- Negative button: "Cancel" → Reverts spinner to current active language
  - Uses `isUpdatingSpinner` flag to prevent loop
  - Reverts settings to current active language
- Restart required: Yes (recreates all activities with new locale)

**Safe Mode Section:**
- `switchEnableSafeMode`: Master toggle
- Checked → Shows sub-options:
  - `layoutConfirmDelete` (visibility: VISIBLE)
  - `layoutConfirmMove` (visibility: VISIBLE)
- Unchecked → Hides sub-options (visibility: GONE)
- Help icon (`iconHelpSafeMode`):
  - Click → Opens TooltipDialog with title/message
  - Content: "Safe Mode enables confirmation dialogs for destructive actions"

**Network Parallelism:**
- Type: AutoCompleteTextView (dropdown + manual input)
- Options: "1", "2", "4", "8", "12", "24"
- Manual input: Accepts 1-32, validates on focus loss
- Invalid input → Restores previous value
- Action: Updates `ConnectionThrottleManager.setUserNetworkLimit()` immediately
- No restart required

**Cache Size Limit:**
- Type: AutoCompleteTextView (dropdown + manual input)
- Options: "512", "1024", "2048", "4096", "8192", "16384" (MB)
- Manual input: Accepts 512-16384 MB
- Invalid input → Toast "Cache size must be between 512 and 16384 MB" + restores previous
- Selection → Shows restart dialog: "Change cache size to X MB? App will restart."
- Positive button: "Restart" → Saves to DataStore + SharedPreferences (`glide_config`) → Calls `LocaleHelper.restartApp()`
- Negative button: "Cancel" → Reverts to current value
- Restart required: Yes (Glide initialization reads SharedPreferences)
- Flag: `isCacheSizeUserModified` set to `true` when manually changed

**Auto-Calculate Cache (btnAutoCalculateCache):**
- Click → Calls `CalculateOptimalCacheSizeUseCase()`
- Shows dialog: "Optimal cache size: X MB (Y% of available storage)"
- Positive button: "Apply" → Shows restart dialog (sets `isCacheSizeUserModified = false`)
- Negative button: "Cancel" → Does nothing
- Suggestion logic: Only shown on first install if `!isCacheSizeUserModified`
- Dialog content includes storage info (total/available space)

**Clear Cache (btnClearCache):**
- Click → Shows confirmation dialog: "This will clear all cached thumbnails and file data"
- Positive button: "OK" → Executes 5 steps:
  1. Glide.get(context).clearDiskCache() (IO thread)
  2. UnifiedFileCache.clearAll() (all network files)
  3. Manual deletion: deleteRecursive(cacheDir) (all files in cache/ except cache/ itself)
  4. TranslationCacheManager.clearAll() (in-memory translation cache)
  5. PlaybackPositionRepository.deleteAllPositions() (saved playback times)
  6. Glide.get(context).clearMemory() (Main thread)
- Button text changes: "Clear Cache" → "Calculating..." (during operation)
- Button disabled during operation
- Toast: "Cache cleared" on success, "Cache clear failed" on error
- Updates cache size display after clearing

**Cache Size Display (tvCacheSize):**
- Format: "Current cache size: X.XX MB" (or GB)
- Calculation: Recursive scan of requireContext().cacheDir
- Updates: onCreate, onResume, after clear cache
- Runs on IO dispatcher to avoid blocking UI
- Shows "N/A" on error

**Background Sync:**
- `switchEnableBackgroundSync`: Master toggle
- Options: "5", "15", "60", "120", "300" minutes via AutoCompleteTextView
- Manual input: Accepts values ≥5 minutes
- Invalid input → Restores previous value + Toast
- Conversion: Minutes input → Hours storage (coerceAtLeast(1))
- Action: Reschedules WorkManager periodic work request immediately

**Sync Now (btnSyncNow):**
- Click → Toast "Manual sync triggered"
- TODO comment: Not implemented yet
- Intended: Triggers `SyncNetworkResourcesUseCase` immediately

**Default User/Password:**
- Two EditText fields: etDefaultUser, etDefaultPassword
- Save on focus loss (not on every keystroke)
- Compare with current settings before updating
- Used: As default credentials for SMB/SFTP/FTP connections

**User Guide (btnUserGuide):**
- Click → Opens browser with language-specific URL:
  - en: `https://serzhyale.github.io/FastMediaSorter_mob_v2/`
  - ru: `https://serzhyale.github.io/FastMediaSorter_mob_v2/index-ru.html`
  - uk: `https://serzhyale.github.io/FastMediaSorter_mob_v2/index-uk.html`
- Language determined by `LocaleHelper.getLanguage(context)` (current active locale)
- Fallback: Toast "No browser found" if ActivityNotFoundException

**Privacy Policy (btnPrivacyPolicy):**
- Click → Opens browser with language-specific URL:
  - en: `https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.html`
  - ru: `https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.ru.html`
  - uk: `https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.uk.html`
- Same browser fallback as User Guide

**Export Settings (btnExportSettings):**
- Click → Calls `ExportSettingsUseCase()`
- Success → Toast "Settings exported successfully"
- Failure → Toast "Export failed: [error message]"
- File location: External storage / app-specific directory
- Format: JSON with all AppSettings fields

**Import Settings (btnImportSettings):**
- Click → Calls `ImportSettingsUseCase()`
- Success → Shows restart dialog: "Settings imported. Restart now?"
  - Positive: "Restart now" → Relaunches app
  - Negative: "Restart later" → Dismisses dialog
- Failure → Toast:
  - "File not found" (specific error)
  - "Import failed: [error message]" (generic)
- File location: Same as export

**Permissions Buttons:**
- `btnLocalFilesPermission`: Request storage permissions
  - Android 11+ (API 30+): Launches `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`
  - Android 6-10 (API 23-29): Requests `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`
  - Android 5.x and below: Toast "Permissions granted" (granted at install)
  - Button state: Disabled (alpha 0.5f) if permission already granted
  - Toast: "Storage permissions granted" if already has permission
- `btnNetworkPermission`: Always disabled (alpha 0.5f)
  - Network permissions (INTERNET, ACCESS_NETWORK_STATE) are normal permissions (auto-granted)
  - Click → Toast "Network permissions are already granted automatically"
- Permission state checked: onCreate and onResume (updates when returning from system settings)

**Log Buttons:**
- `btnShowLog`: Show full application log
  - Click → Opens `DialogUtils.showScrollableDialog()` with last 512 lines
  - Execution: `Runtime.getRuntime().exec("logcat -d -v time")`
  - Format: "Last X lines of log:\n\n[log content]"
  - Error: "Error reading log: [exception message]"
- `btnShowSessionLog`: Show current session log
  - Click → Opens same dialog with filtered log
  - Filter: Lines containing package name OR "FastMediaSorter"
  - Format: "Current session log (X lines):\n\n[log content]"
  - Empty: "No log entries found for current session"

**Layout Adaptation (Landscape Optimization):**
- Orientation check: `resources.configuration.orientation`
- Containers dynamically switch between HORIZONTAL (landscape) and VERTICAL (portrait):
  - `containerSync`: Enable Sync + Sync Controls
  - `containerSleepFavorites`: Sleep + Favorites
  - `containerConfirm`: Confirm Delete + Confirm Move
  - `containerCache`: Cache controls + Cache actions
- Layout params updated: `width = 0` + `weight = 1f` (horizontal) vs `width = MATCH_PARENT` + `weight = 0f` (vertical)
- Applied: onCreate and onConfigurationChanged

**Settings Update Protection:**
- Flag: `isUpdatingSpinner` prevents infinite loops
- Set to `true`: Before programmatic UI updates from ViewModel
- Reset to `false`: After UI updates complete (often in `post {}`)
- Checked: In all listener lambdas (early return if true)
- Purpose: Prevents triggering change listeners when syncing UI with ViewModel state

---

### PlaybackSettingsFragment - Detailed Behaviors

**Sort Mode Dropdown:**
- Options: 8 sort modes (NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC, TYPE_ASC, TYPE_DESC)
- Type: AutoCompleteTextView
- Click item → Updates `defaultSortMode` immediately
- Localized: Uses string resources for display names

**Slideshow Interval:**
- Options: "1", "5", "10", "30", "60", "120", "300" seconds
- Type: AutoCompleteTextView (dropdown + manual input)
- Manual input: Accepts 1-3600 seconds, clamped to range
- Invalid input → Auto-corrects to clamped value
- Saves on focus loss

**Icon Size (Grid Thumbnails):**
- Options: "24", "32", "48", "64", "96", "128", "160", "192", "256", "320", "384", "512", "768", "1024" pixels
- Type: AutoCompleteTextView (dropdown + manual input)
- Manual input: Accepts 24-1024 pixels, clamped to range
- Invalid input → Auto-corrects to clamped value
- Affects: BrowseActivity grid thumbnail size
- Help icon: Opens TooltipDialog with explanation

**Switches (11 total):**
- `switchPlayToEnd`: Play files to completion in slideshow (vs skip after interval)
- `switchAllowRename`: Enable rename button in PlayerActivity
- `switchAllowDelete`: Enable delete button in PlayerActivity
- `switchConfirmDelete`: Show confirmation dialog before delete
- `switchGridMode`: Default view mode for BrowseActivity (Grid vs List)
- `switchHideGridActionButtons`: Hide quick action buttons in grid items
- `switchShowCommandPanel`: Show command panel by default in PlayerActivity
- `switchDetailedErrors`: Show ErrorDialog with full stack trace vs simple Toast
- `switchShowPlayerHint`: Show touch zone hint on first PlayerActivity open
- `switchAlwaysShowTouchZones`: Always display touch zone overlay (for debugging)

**Show Hint Now (btnShowHintNow):**
- Click → Calls `viewModel.resetPlayerFirstRun()`
- Resets: `showPlayerHintOnFirstRun` flag
- Toast: "Hint will be shown next time you open player"
- Purpose: Re-triggers first-run tutorial without clearing other settings

**Help Icons (3 total):**
- `iconHelpSlideshow`: Opens tooltip for slideshow settings
- `iconHelpTouchZones`: Opens tooltip for touch zone functionality
- `iconHelpGridSize`: Opens tooltip for icon size impact
- All use `TooltipDialog.show()` with title + message

**Settings Update Protection:**
- Flag: `isUpdatingFromSettings` prevents infinite loops
- Same pattern as GeneralSettingsFragment

---

### DestinationsSettingsFragment - Detailed Behaviors

**Copy Options Section:**
- `switchEnableCopying`: Master toggle for copy feature
  - Checked → Shows `layoutCopyOptions` and `layoutOverwriteCopyWrapper` (visibility: VISIBLE)
  - Unchecked → Hides both layouts (visibility: GONE)
- `switchGoToNextAfterCopy`: Auto-advance to next file after successful copy
- `switchOverwriteOnCopy`: Overwrite existing files without confirmation

**Move Options Section:**
- `switchEnableMoving`: Master toggle for move feature
- `switchOverwriteOnMove`: Overwrite existing files without confirmation
- Visibility: `layoutOverwriteMoveWrapper` controlled by master toggle

**Max Recipients (etMaxRecipients):**
- Type: AutoCompleteTextView (dropdown + manual input)
- Options: "5", "10", "15", "20", "25", "30"
- Manual input: Accepts 1-30, validates on focus loss
- Invalid input → Shows error in TextInputLayout: "Max recipients error"
  - Restores previous valid value
- IME action: DONE → Clears focus, hides keyboard
- Purpose: Limits number of share recipients in file share operations

**Destinations RecyclerView:**
- Adapter: `DestinationsAdapter` (ListAdapter with DiffUtil)
- Layout: GridLayoutManager
  - Portrait: 1 column
  - Landscape: 2 columns (via `onConfigurationChanged`)
- Each item shows:
  - Destination number badge (1-10)
  - Resource name (TextView)
  - Resource path (TextView, truncated)
  - Color indicator (colored circle, clickable)
  - File count (from database)
  - Free space (formatted)
  - Up/Down buttons (for reordering)
  - Delete button (X icon)

**Destination Item Actions:**
- **Color indicator click**:
  - Opens `ColorPickerDialog` with current color
  - Selection → Calls `viewModel.updateDestinationColor(resource, color)`
  - Updates `ResourceEntity.destinationColor` in database
- **Move Up button** (`btnMoveUp`):
  - Click → Calls `viewModel.moveDestination(resource, -1)`
  - Swaps order with previous destination (decrements position)
  - Disabled for first item
- **Move Down button** (`btnMoveDown`):
  - Click → Calls `viewModel.moveDestination(resource, +1)`
  - Swaps order with next destination (increments position)
  - Disabled for last item
- **Delete button** (`btnDelete`):
  - Click → Shows confirmation dialog: "Remove [ResourceName] from destinations?"
  - Positive: "Remove" → Calls `viewModel.removeDestination(resource)` + Toast "Removed"
  - Negative: "Cancel" → Dismisses

**Add Destination (btnAddDestination):**
- Click → Opens selection dialog with available resources
- Query: `viewModel.getWritableNonDestinationResources()`
  - Filters: `isWritable = true` AND `isDestination = false`
- Empty list → Toast "No writable resources available for destinations"
- Dialog: Lists resources as "[Name] ([Path])"
- Selection → Calls `viewModel.addDestination(resource)` + Toast "Destination added: [Name]"
- Button visibility:
  - Shown: If available resources exist
  - Hidden: If no writable non-destination resources
- Message (tvNoResourcesMessage):
  - Shown: If no destinations AND no available resources
  - Hidden: Otherwise

**Help Icon (iconHelpDestinations):**
- Click → Opens TooltipDialog
- Content: "Destinations are quick shortcuts for copy/move operations. Up to 10 destinations can be configured."

**Settings Update Protection:**
- Flag: `isUpdatingFromSettings` prevents infinite loops
- Applied to all switch listeners

**DiffUtil Callback:**
- `DestinationDiffCallback` compares MediaResource objects
- `areItemsTheSame`: Compares resource IDs
- `areContentsTheSame`: Deep equality check (includes color, name, paths)

---

### Common Patterns Across All Fragments

**Listener Protection:**
- All fragments use boolean flag (`isUpdatingSpinner`, `isUpdatingFromSettings`)
- Prevents: Infinite loops when syncing UI with ViewModel state
- Pattern: Set flag → Update UI → Reset flag (often in `post {}`)

**Settings Observation:**
- All fragments collect `viewModel.settings` Flow in `repeatOnLifecycle(STARTED)`
- Updates: Only if value differs from current UI state (prevents flicker)
- Scope: `viewLifecycleOwner.lifecycleScope`

**Restart Dialogs:**
- Common pattern for language and cache size changes
- Title: "Restart app?"
- Message: Explains what will change
- Positive button: Saves settings + calls `LocaleHelper.restartApp(activity)`
- Negative button: Reverts UI + updates settings to previous value
- `setCancelable(false)`: Forces user decision

**AutoCompleteTextView Pattern:**
- Used for: Dropdowns with manual input support
- Adapter: `ArrayAdapter` with `simple_dropdown_item_1line` layout
- `setAdapter()`: Sets predefined options
- `setText(value, false)`: Programmatically sets value (false = no filter)
- `setOnItemClickListener`: Handles dropdown selection
- `setOnFocusChangeListener`: Validates and saves manual input
- Validation: Checks range → Clamps → Updates or restores

**Help Icons:**
- Icon: `iconHelp[Topic]` (ImageView with "?" or info icon)
- Click → `TooltipDialog.show(context, titleRes, messageRes)`
- Pattern used: Safe Mode, Slideshow, Touch Zones, Grid Size, Destinations

**Configuration Changes:**
- `onConfigurationChanged()` overridden where needed
- Common actions: Re-setup layout managers (Grid span count, LinearLayout orientation)
- Fragments: GeneralSettingsFragment (layout containers), DestinationsSettingsFragment (RecyclerView span)

---

### MediaSettingsFragment - Container with Nested Tabs

**Structure:**
- Nested TabLayout + ViewPager2 with 5 sub-fragments
- Tab labels: Images / Video / Audio / Documents / Other
- Adapter: `MediaCategoryPagerAdapter` (FragmentStateAdapter)
- Pattern: Similar to SettingsActivity's main ViewPager2
- No direct settings controls in this fragment (pure navigation container)

**Tab Switching:**
- `TabLayoutMediator` synchronizes tabs with ViewPager2
- Tab click → Switches to corresponding media category fragment
- No keyboard navigation (handled by parent SettingsActivity)

---

### ImagesSettingsFragment - Detailed Behaviors

**Support Images Switch:**
- Master toggle for image file type support
- Checked: BrowseActivity shows image files (JPEG, PNG, BMP, WebP)
- Unchecked: Images hidden from file lists

**Support GIFs Switch:**
- Separate toggle for GIF support (distinct from static images)
- Checked: BrowseActivity shows GIF files
- Unchecked: GIFs hidden from file lists

**Load Full Size Images Switch:**
- Controls image loading strategy in PlayerActivity
- Checked: Loads original resolution (uses more memory)
- Unchecked: Loads downsampled version (Glide thumbnail strategy)
- Affects: Memory usage and loading time

**Image Size Limits:**
- Two EditText fields: Min / Max size in KB
- Type: `addTextChangedListener` (updates on every keystroke if not updating)
- Conversion: KB input → Bytes storage (multiplied by 1024)
- Purpose: Filter files in BrowseActivity by file size
- No validation: Accepts any positive long value
- Pattern: Updates settings immediately (no focus loss required)
- `isUpdatingFromSettings` flag prevents loops

**Settings Update Protection:**
- Flag: `isUpdatingFromSettings` prevents infinite loops
- Same pattern as all other fragments

---

### VideoSettingsFragment - Detailed Behaviors

**Support Videos Switch:**
- Master toggle for video file type support
- Checked: BrowseActivity shows video files (MP4, AVI, MKV, MOV, etc.)
- Unchecked: Videos hidden from file lists

**Show Video Thumbnails Switch:**
- Controls thumbnail generation in BrowseActivity
- Checked: Uses MediaMetadataRetriever to extract first frame (IO-intensive)
- Unchecked: Shows generic video icon (faster)
- Affects: Grid loading performance

**Video Size Limits:**
- Two EditText fields: Min / Max size in KB
- Type: `addTextChangedListener` (updates on every keystroke if not updating)
- Conversion: KB input → Bytes storage (multiplied by 1024)
- Purpose: Filter files in BrowseActivity by file size
- No validation: Accepts any positive long value
- Pattern: Updates settings immediately
- `isUpdatingFromSettings` flag prevents loops

---

### AudioSettingsFragment - Detailed Behaviors

**Support Audio Switch:**
- Master toggle for audio file type support
- Checked: BrowseActivity shows audio files (MP3, WAV, FLAC, OGG, etc.)
- Unchecked: Audio files hidden from file lists

**Search Audio Covers Online Switch:**
- Master toggle for online album cover fetching
- Checked: Shows sub-option `layoutSearchCoversOnlyWifi` (visibility: VISIBLE)
- Unchecked: Hides sub-option (visibility: GONE)
- Action: PlayerActivity fetches album art from Last.fm API or similar
- Fallback: Uses embedded ID3 tags if online fetch fails

**Search Covers Only on WiFi Switch:**
- Sub-option under "Search Audio Covers Online"
- Checked: Fetches covers only when connected to WiFi (saves mobile data)
- Unchecked: Fetches covers on any connection (WiFi or Mobile)
- Visibility: Controlled by parent switch `searchAudioCoversOnline`

**Audio Size Limits:**
- Two EditText fields: Min / Max size in MB (not KB like Images/Video)
- Type: `addTextChangedListener` (updates on every keystroke if not updating)
- Conversion: MB input → Bytes storage (multiplied by 1024 * 1024)
- Purpose: Filter files in BrowseActivity by file size
- No validation: Accepts any positive long value
- `isUpdatingFromSettings` flag prevents loops

---

### DocumentsSettingsFragment - Detailed Behaviors

**Support Text Switch:**
- Master toggle for text file type support (.txt, .log, .xml, .json, etc.)
- Checked: Shows sub-option `layoutShowTextLineNumbers` (visibility: VISIBLE)
- Unchecked: Hides sub-option (visibility: GONE)
- Action: BrowseActivity shows text files

**Show Text Line Numbers Switch:**
- Sub-option under "Support Text"
- Checked: TextViewerManager displays line numbers in left gutter
- Unchecked: No line numbers (plain text display)
- Visibility: Controlled by `supportText` switch

**Support PDF Switch:**
- Master toggle for PDF file type support
- Checked: Shows sub-option `layoutShowPdfThumbnails` (visibility: VISIBLE)
- Unchecked: Hides sub-option (visibility: GONE)
- Action: BrowseActivity shows PDF files

**Show PDF Thumbnails Switch:**
- Sub-option under "Support PDF"
- Checked: Renders first page of PDF as thumbnail (PdfRenderer, IO-intensive)
- Unchecked: Shows generic PDF icon (faster)
- Visibility: Controlled by `supportPdf` switch

**Support EPUB Switch:**
- Master toggle for EPUB e-book support
- Checked: BrowseActivity shows EPUB files
- Unchecked: EPUB files hidden
- Note: No sub-options (unlike Text and PDF)

**Settings Update Protection:**
- Flag: `isUpdatingFromSettings` prevents infinite loops
- Applied to all switch listeners

---

### OtherMediaSettingsFragment - Detailed Behaviors

**Translation Section:**
- `switchEnableTranslation`: Master toggle for in-app translation feature
- Checked: Shows `layoutTranslationLanguages` and `layoutTranslationLensStyle` (visibility: VISIBLE)
- Unchecked: Hides both layouts (visibility: GONE)

**Translation Language Spinners:**
- Two spinners: Source Language / Target Language
- Source languages (22 total): Auto-detect, English, Russian, Ukrainian, Spanish, French, German, Chinese, Japanese, Korean, Arabic, Portuguese, Hindi, Italian, Turkish, Polish, Dutch, Thai, Persian, Greek, Indonesian, Maltese
- Target languages (21 total): Same as source except "Auto-detect" removed
- Type: Standard Spinner with `ArrayAdapter` (simple_spinner_item layout)
- Mapping: Position → Language code (ISO 639-1)
  - Examples: en, ru, uk, es, fr, de, zh, ja, ko, ar, pt, hi, it, tr, pl, nl, th, fa, el, id, mt
  - Source "Auto-detect" → code "auto"
- Selection: Updates `translationSourceLanguage` or `translationTargetLanguage` immediately
- Helpers: `getLanguageCode(position, isSource)`, `getLanguagePosition(code, isSource)`

**Swap Languages Button (btnSwapLanguages):**
- Click: Swaps source and target language selections
- Disabled: If source is "auto" (cannot reverse auto-detect)
- Action: Reads current codes → Updates settings with reversed codes
- No Toast or feedback (silent operation)

**Translation Lens Style Switch:**
- Checked: Uses overlay-style translation UI (Google Lens-like)
- Unchecked: Uses inline replacement translation
- Visibility: Shown only when `enableTranslation = true`

**Google Lens Switch:**
- Enables/disables Google Lens integration
- Checked: Shows "Search with Google Lens" button in PlayerActivity for images
- Unchecked: Button hidden

**OCR Section:**
- `switchEnableOcr`: Master toggle for Optical Character Recognition
- Checked: Shows `layoutOcrFontSize` and `layoutOcrFontFamily` (visibility: VISIBLE)
- Unchecked: Hides both layouts (visibility: GONE)
- Action: Enables OCR button in PlayerActivity for text extraction from images

**OCR Font Size Spinner:**
- Options: Автоматический (AUTO), Минимальный (MINIMUM), Маленький (SMALL), Средний (MEDIUM), Крупный (LARGE), Большой (HUGE)
- Type: Standard Spinner with `ArrayAdapter`
- Mapping: Position → Font size enum string
- Selection: Updates `ocrDefaultFontSize` immediately
- Purpose: Default font size for OCR text display in PlayerActivity
- Visibility: Controlled by `enableOcr` switch

**OCR Font Family Spinner:**
- Options: По-умолчанию (DEFAULT), С засечками (SERIF), Моноширинный (MONOSPACE)
- Type: Standard Spinner with `ArrayAdapter`
- Mapping: Position → Font family enum string
- Selection: Updates `ocrDefaultFontFamily` immediately
- Purpose: Default font family for OCR text display in PlayerActivity
- Visibility: Controlled by `enableOcr` switch

**Settings Update Protection:**
- Flag: `isUpdatingFromSettings` prevents infinite loops
- Applied to all switch and spinner listeners

**Visibility Helpers:**
- `updateTranslationVisibility(enabled: Boolean)`: Shows/hides translation language controls
- `updateOcrVisibility(enabled: Boolean)`: Shows/hides OCR font controls

---

### NetworkSettingsFragment - Detailed Behaviors

**Background Sync Switch:**
- Master toggle for automatic network resource synchronization
- Checked: Calls `scheduleNetworkSyncUseCase(intervalHours)` → Schedules WorkManager PeriodicWorkRequest
- Unchecked: Calls `scheduleNetworkSyncUseCase.cancel()` → Cancels WorkManager task
- Toast feedback: "Background sync enabled" / "Background sync disabled"
- Timber log: "Background sync enabled (interval=X hours)" or "disabled"

**Sync Interval Slider:**
- Type: Material Slider (range: 1-24 hours, step: 1)
- Value display: TextView `tvSyncIntervalValue` with plurals resource
  - Format: "1 hour" / "4 hours" / "24 hours"
- `addOnChangeListener` with `fromUser` check (ignores programmatic changes)
- Action: If sync enabled → Reschedules WorkManager with new interval immediately
- Timber log: "Sync interval updated to X hours"
- No settings persistence (hardcoded default: 4 hours)

**Sync Now Button (btnSyncNow):**
- Click: Triggers manual synchronization
- Action sequence:
  1. Disables button (prevents duplicate clicks)
  2. Updates status TextView: "Sync in progress..."
  3. Calls `syncNetworkResourcesUseCase.syncAll()` on `Dispatchers.IO`
  4. Success: "Completed, synced X resources" + Snackbar + Timber.i
  5. Failure: "Sync failed" + Snackbar with error + Timber.e
  6. Always: Re-enables button in finally block
- Status TextView (`tvSyncStatus`):
  - "In progress..." → "Completed, synced X resources" or "Failed"
- Feedback: Snackbar for user confirmation

**Manual Sync Result:**
- Success: Shows count of synced resources (from `syncNetworkResourcesUseCase` return value)
- Failure: Shows error message from exception
- Snackbar duration: SHORT (2 seconds)

**Settings Observation:**
- `observeSettings()` launches coroutine but does NOT observe any settings currently
- Future extension: Store sync preferences (enabled state, interval) in AppSettings
- Current implementation: Uses hardcoded defaults (enabled=true, interval=4h)
- Slider default: Value set in XML layout (default: 4)

**Direct UseCase Injection:**
- `@Inject` `scheduleNetworkSyncUseCase`: WorkManager scheduling
- `@Inject` `syncNetworkResourcesUseCase`: Manual sync execution
- Fragment annotated with `@AndroidEntryPoint` (Hilt injection)

---

## Edge Cases and Validation Rules

### Input Validation
- **AutoCompleteTextView fields** (NetworkParallelism, CacheSize, SyncInterval, MaxRecipients):
  - Manual input validated on focus loss
  - Invalid values: Restore previous value + Toast error
  - Valid ranges:
    - NetworkParallelism: 1-32
    - CacheSize: 512-16384 MB
    - SyncInterval: ≥5 minutes
    - MaxRecipients: 5-30
    - IconSize: 24-1024 pixels
    - SlideshowInterval: 1-3600 seconds

### Restart Requirements
- **Language change**: Immediate restart required (recreates all activities with new locale)
- **Cache size change**: Immediate restart required (Glide initialization reads SharedPreferences)
- **Import settings**: Optional restart (user can defer to "Restart later")
- **Theme change**: Immediate activity recreation (not full restart, uses `recreate()`)

### Visibility Toggles
- **Master switches** control sub-option visibility:
  - Safe Mode → Confirm Delete, Confirm Move
  - Enable Copying → Copy sub-options (Go To Next, Overwrite)
  - Enable Moving → Move sub-options (Overwrite)
  - Background Sync → Sync interval dropdown (in GeneralSettingsFragment)
  - Search Audio Covers Online → WiFi-only option
  - Support Text → Show Line Numbers
  - Support PDF → Show PDF Thumbnails
  - Enable Translation → Language spinners, Lens Style
  - Enable OCR → Font size, Font family

### Permission Handling
- **Local Files Permission**:
  - Android 11+: Opens system settings for MANAGE_EXTERNAL_STORAGE
  - Android 6-10: Requests READ/WRITE_EXTERNAL_STORAGE at runtime
  - Android 5.x and below: Toast "Permissions granted" (no action, granted at install)
  - Button state: Disabled if already granted (checked in onCreate and onResume)
- **Network Permission**:
  - Always disabled (normal permission, auto-granted)
  - Click → Toast "Network permissions are already granted automatically"

### Cache Management
- **Clear cache steps** (order is critical):
  1. Glide disk cache (IO thread)
  2. UnifiedFileCache (all network files)
  3. Manual recursive deletion: cacheDir except cacheDir itself
  4. TranslationCache (in-memory)
  5. PlaybackPositionRepository (saved playback times)
  6. Glide memory cache (Main thread)
- **Cache size calculation**:
  - Recursive scan of `requireContext().cacheDir`
  - Updates: onCreate, onResume, after clear cache
  - Runs on IO dispatcher (avoids UI blocking)
- **Optimal cache suggestion**:
  - Shown on first install if `!isCacheSizeUserModified`
  - Uses `CalculateOptimalCacheSizeUseCase()` (analyzes available storage)
  - Dialog shows: Total storage, Available storage, Suggested cache size (% of available)

### Destinations Constraints
- **Max destinations**: 10 (enforced by ViewModel query filter)
- **Add button visibility**: Shown only if writable non-destination resources exist
- **Empty state message**: Shown if no destinations AND no available resources
- **Move up/down**: Disabled for first/last items respectively
- **Delete confirmation**: Required before removal
- **Color picker**: Opens with current color, updates immediately on selection

### Translation Constraints
- **Language swap**: Disabled if source is "auto" (cannot reverse auto-detect)
- **Source languages**: Includes "Auto-detect" (position 0, code "auto")
- **Target languages**: Excludes "Auto-detect" (starts from "English" at position 0)
- **Visibility**: All translation controls hidden when master switch off

### OCR Constraints
- **Font settings**: Only visible when OCR enabled
- **Default values**: AUTO (font size), DEFAULT (font family)
- **Purpose**: Applied to OCR text extraction results in PlayerActivity

---

## Dialog Components

### TooltipDialog (Simple AlertDialog)
**Purpose:** Context-sensitive help for complex settings

**Structure:**
- Simple `AlertDialog.Builder` with title + message
- Positive button: "OK" (dismisses dialog)
- Cancelable: `true` (back button dismisses)

**Usage:**
- Help icons in various fragments (Safe Mode, Slideshow, Touch Zones, Grid Size, Destinations)
- Static method: `TooltipDialog.show(context, title, message)`
- Overload: `show(context, titleResId, messageResId)` for string resources

**Examples:**
- Safe Mode: "Safe Mode enables confirmation dialogs for destructive actions"
- Destinations: "Destinations are quick shortcuts for copy/move operations. Up to 10 destinations can be configured."
- Slideshow: Explains slideshow interval behavior
- Touch Zones: Explains PlayerActivity touch zone layout

---

### ColorPickerDialog (MaterialAlertDialogBuilder + RecyclerView)
**Purpose:** Select destination colors in DestinationsSettingsFragment

**Structure:**
- `DialogFragment` with custom layout (`dialog_color_picker.xml`)
- Color preview: Shows selected color + color name
- RecyclerView: GridLayoutManager with 6 columns
- Adapter: `ColorAdapter` with `ItemColorBinding` (color circle + checkmark)
- Buttons: Cancel (dismisses) / OK (confirms selection)

**Color Palette:**
- Source: `ColorPalette.EXTENDED_PALETTE` (array of Int colors)
- Default: `ColorPalette.DEFAULT_COLORS[0]` (fallback)
- Display: Color circle with checkmark overlay for selected color
- Checkmark: Adaptive color (BLACK for light colors, WHITE for dark colors)
  - Luminance calculation: `0.299*R + 0.587*G + 0.114*B`
  - Threshold: 0.5 (above = BLACK, below = WHITE)

**Interaction:**
- Initial color: Passed as argument `ARG_INITIAL_COLOR`
- Click color circle: Updates `selectedColor` + Preview + Checkmark position
- Cancel: Dismisses without callback
- OK: Calls `onColorSelected(selectedColor)` + Dismisses
- Callback: Lambda passed to `newInstance(initialColor, onColorSelected)`

**Usage:**
- DestinationsSettingsFragment: Color indicator click in RecyclerView item
- Pattern: `ColorPickerDialog.newInstance(resource.destinationColor) { color -> viewModel.updateDestinationColor(resource, color) }`

---

### Restart Dialog (MaterialAlertDialogBuilder)
**Purpose:** Confirm app restart for Language or Cache Size changes

**Structure:**
- Title: "Restart app?" (generic for both cases)
- Message: Context-specific (e.g., "Change language to Russian? App will restart.")
- Positive button: "Restart" → Saves settings + Calls `LocaleHelper.restartApp(activity)`
- Negative button: "Cancel" → Reverts UI to previous value
- Cancelable: `false` (forces user decision)

**Trigger Conditions:**
1. **Language change** (GeneralSettingsFragment):
   - Message: "Change language to [Language Name]? App will restart."
   - Cancel action: Reverts spinner to current active language
   - Uses `isUpdatingSpinner` flag to prevent loop
2. **Cache size change** (GeneralSettingsFragment):
   - Message: "Change cache size to X MB? App will restart."
   - Cancel action: Reverts AutoCompleteTextView to current value
3. **Import settings** (GeneralSettingsFragment):
   - Message: "Settings imported. Restart now?"
   - Positive: "Restart now"
   - Negative: "Restart later" (defers restart)

**Restart Mechanism:**
- Method: `LocaleHelper.restartApp(activity)`
- Action: Finishes all activities + Relaunches MainActivity
- Effect: Full app restart (clears activity stack)

---

### Optimal Cache Dialog (Custom Dialog via DialogUtils)
**Purpose:** Suggest optimal cache size on first install

**Structure:**
- Title: "Optimal Cache Size"
- Message: Multi-line with storage info:
  - Total storage: X GB
  - Available storage: Y GB
  - Suggested cache size: Z MB (calculated as % of available)
- Positive button: "Apply" → Shows restart dialog (saves cache size)
- Negative button: "Cancel" → Dismisses
- Cancelable: `true`

**Trigger Condition:**
- Shown: On first app install if `!isCacheSizeUserModified`
- NOT shown: If user manually changed cache size in settings
- UseCase: `CalculateOptimalCacheSizeUseCase()` calculates suggestion

**Calculation Logic:**
- Reads: `Environment.getExternalStorageDirectory().totalSpace`, `.freeSpace`
- Suggestion: Typically 5-10% of available space (capped at 16384 MB max)
- Purpose: Balance between cache performance and disk space

---

### Log Dialog (Scrollable AlertDialog)
**Purpose:** Display application logs for debugging

**Structure:**
- Title: "Last X lines of log" (full log) or "Current session log (X lines)"
- Message: Scrollable TextView with monospace font
- Positive button: "OK" (dismisses)
- Cancelable: `true`

**Two Variants:**
1. **Show Full Log** (`btnShowLog`):
   - Command: `Runtime.getRuntime().exec("logcat -d -v time")`
   - Filters: Last 512 lines
   - Format: Timestamp + Priority + Tag + Message
2. **Show Session Log** (`btnShowSessionLog`):
   - Same command, filtered by package name OR "FastMediaSorter"
   - Shows: Only logs from current app session
   - Empty state: "No log entries found for current session"

**Error Handling:**
- IOException: Shows "Error reading log: [exception message]"
- SecurityException: Shows "Permission denied to read logs"
- Empty log: Shows placeholder message

**Usage:**
- GeneralSettingsFragment: Two buttons (Show Log / Show Session Log)
- Purpose: In-app debugging without ADB or terminal

---

### Confirmation Dialogs (Generic MaterialAlertDialogBuilder)
**Purpose:** Confirm destructive actions

**Examples:**
1. **Delete Destination** (DestinationsSettingsFragment):
   - Title: "Remove Destination?"
   - Message: "Remove [ResourceName] from destinations?"
   - Positive: "Remove" → Calls `viewModel.removeDestination(resource)` + Toast "Removed"
   - Negative: "Cancel" → Dismisses
2. **Clear Cache** (GeneralSettingsFragment):
   - Title: "Clear Cache?"
   - Message: "This will clear all cached thumbnails and file data"
   - Positive: "OK" → Executes 5-step cache clearing process
   - Negative: "Cancel" → Dismisses

**Pattern:**
- Always include context-specific message (what will be removed/cleared)
- Positive button: Action verb (Remove, Clear, Delete, etc.)
- Negative button: Always "Cancel"
- Cancelable: `true` (allows back button dismissal)

---

## Additional UI Elements

### Touch Zones Legend (PlaybackSettingsFragment - Static Display)

**Purpose:** Visual reference for PlayerActivity touch zone layout

**Structure:**
- ImageView: Shows touch zones scheme diagram (drawable resource)
- Legend: 9 labeled touch zones with numbered list
- Position: Below "Show Hint Now" button in PlaybackSettingsFragment
- Non-interactive: Informational display only (no click handlers)

**Touch Zones Mapping:**
1. Zone 1: Back (top-left corner)
2. Zone 2: Copy (top-center)
3. Zone 3: Rename (top-right corner)
4. Zone 4: Previous (middle-left)
5. Zone 5: Move (center)
6. Zone 6: Next (middle-right)
7. Zone 7: Command Panel (bottom-left)
8. Zone 8: Delete (bottom-center)
9. Zone 9: Slideshow (bottom-right)

**Relation to PlayerActivity:**
- Touch zones active when `alwaysShowTouchZones = true` OR first-run hint shown
- Zones overlay: Semi-transparent colored rectangles with labels
- Quick actions: Tap zone → Execute corresponding action (no confirmation)
- Debugging: Always visible when `alwaysShowTouchZones = true`

**Layout Details:**
- Title: "Touch Zones Scheme" (TextView)
- Image: `R.drawable.touch_zones_scheme` (aspect ratio preserved)
- Legend title: "Touch Zones Legend" (bold TextView)
- Legend items: 9 TextViews with numbered descriptions
- Spacing: Vertical LinearLayout with standard padding

---

## Summary

SettingsActivity provides comprehensive configuration for all app features across 9 fragments (1 main activity + 3 top-level fragments + 1 Media container + 5 Media category fragments). Total documented behaviors: **150+ detailed interactions** including:
- 85+ listener implementations (Click, CheckedChange, ItemSelected, SeekBar, FocusChange, TextChanged)
- 30+ switches with master/sub-option visibility toggles
- 15+ AutoCompleteTextView fields with manual input validation
- 10+ spinners with position-to-value mapping
- 6 dialog types (Tooltip, ColorPicker, Restart, OptimalCache, Log, Confirmation)
- 7 keyboard shortcuts (DPAD_LEFT/RIGHT, PAGE_UP/DOWN, ESCAPE, TAB, DPAD_UP/DOWN)
- 4 orientation adaptation containers (Portrait vs Landscape)
- 2 sync mechanisms (WorkManager + Manual)
- Full validation rules, restart flows, permission handling, and edge case documentation
