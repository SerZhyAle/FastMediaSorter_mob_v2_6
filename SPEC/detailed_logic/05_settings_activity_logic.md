# SettingsActivity - Detailed Behavior Specification

**File:** `app_v2/src/main/java/com/sza/fastmediasorter/ui/settings/SettingsActivity.kt` (118 lines)  
**Package:** `com.sza.fastmediasorter.ui.settings`  
**Purpose:** Global app configuration with 4 tabs, keyboard navigation, and instant tab switching  
**Pattern:** MVVM + Fragment Container (4 settings fragments)  
**ViewModel:** `SettingsViewModel`  
**Layout:** `activity_settings.xml`

---

## 1. Overview

SettingsActivity is a container activity hosting 4 settings fragments via ViewPager2:
1. **GeneralSettingsFragment** (1025 lines) - Language, theme, cache, sync, behavior
2. **MediaSettingsFragment** - Nested ViewPager2 with 5 media type sub-tabs
3. **PlaybackSettingsFragment** - Slideshow, touch zones, command panel, video playback
4. **DestinationsSettingsFragment** (351 lines) - Manage copy/move destinations with drag-and-drop
5. **NetworkSettingsFragment** (145 lines) - Background sync scheduling and manual sync

**Key Characteristics:**
- **Instant Tab Switching:** Custom PageTransformer eliminates ViewPager2 animations
- **Keyboard Navigation:** Left/Right arrows, Page Up/Down, Escape to exit, Tab/Shift+Tab, Up/Down arrows
- **State Preservation:** All settings persisted in SharedPreferences via SettingsRepository
- **Activity-Scoped ViewModel:** Shared `SettingsViewModel` accessed by all fragments via `activityViewModels()`

---

## 2. Components

### ViewBinding
```kotlin
private lateinit var binding: ActivitySettingsBinding
```
**Elements:**
- `toolbar` - MaterialToolbar with back navigation
- `tabLayout` - TabLayout with 4 tabs (General | Media | Playback | Destinations)
- `viewPager` - ViewPager2 hosting 4 fragments

### ViewModel
```kotlin
private val viewModel: SettingsViewModel by viewModels()
```
**Scope:** Activity-level, shared with all fragments  
**State:**
- `settings: StateFlow<AppSettings>` - Global app settings
- `destinations: StateFlow<List<MediaResource>>` - Up to 10 destination resources

**Methods:**
- `updateSettings(AppSettings)` - Persist settings to SharedPreferences
- `resetToDefaults()` - Restore factory defaults
- `resetPlayerFirstRun()` - Re-enable PlayerActivity first-run tooltips
- `moveDestination(resource, direction)` - Reorder destinations by swapping destinationOrder
- `removeDestination(resource)` - Clear isDestination flag
- `updateDestinationColor(resource, color)` - Change destination color marker

### Adapter
```kotlin
SettingsPagerAdapter(activity: FragmentActivity)
```
**Fragments:**
- Position 0: `GeneralSettingsFragment`
- Position 1: `MediaSettingsFragment` (nested ViewPager2 inside)
- Position 2: `PlaybackSettingsFragment`
- Position 3: `DestinationsSettingsFragment`

---

## 3. Lifecycle Methods

### `setupViews()`

**Toolbar Navigation:**
```kotlin
binding.toolbar.setNavigationOnClickListener {
    finish()
}
```

**ViewPager2 Configuration:**
```kotlin
val adapter = SettingsPagerAdapter(this)
binding.viewPager.adapter = adapter

// Instant page transitions (no animation)
binding.viewPager.setPageTransformer { page, position ->
    page.translationX = 0f
    page.alpha = if (position == 0f) 1f else 0f
}
binding.viewPager.offscreenPageLimit = 1
```
**Effect:** Visible page has `alpha=1`, others have `alpha=0`. No slide animation, instant switch.

**TabLayout Synchronization:**
```kotlin
TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
    tab.text = when (position) {
        0 -> getString(R.string.settings_tab_general)
        1 -> getString(R.string.settings_tab_media)
        2 -> getString(R.string.settings_tab_playback)
        3 -> getString(R.string.settings_tab_destinations)
        else -> ""
    }
}.attach()
```

### `observeData()`

**Empty implementation** - Settings observation happens in individual fragments via `activityViewModels()`.

```kotlin
override fun observeData() {
    // Settings are observed in individual fragments
}
```

---

## 4. Key Behaviors

### 1. Instant Tab Switching (Custom PageTransformer)

**Implementation:**
```kotlin
binding.viewPager.setPageTransformer { page, position ->
    page.translationX = 0f // No horizontal slide
    page.alpha = if (position == 0f) 1f else 0f // Instant visibility toggle
}
```

**Why This Pattern:**
- ViewPager2 default animation: slide transition
- Settings tabs don't benefit from animation (no spatial relationship)
- Instant switching improves UX for power users with keyboard navigation
- Matches spec requirement: "No page transitions"

### 2. Keyboard Navigation (Custom `onKeyDown()`)

**Previous Tab:**
```kotlin
KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP -> {
    val currentPosition = binding.viewPager.currentItem
    if (currentPosition > 0) {
        binding.viewPager.currentItem = currentPosition - 1
    }
    return true
}
```

**Next Tab:**
```kotlin
KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN -> {
    val currentPosition = binding.viewPager.currentItem
    val adapter = binding.viewPager.adapter
    if (adapter != null && currentPosition < adapter.itemCount - 1) {
        binding.viewPager.currentItem = currentPosition + 1
    }
    return true
}
```

**Exit Settings:**
```kotlin
KeyEvent.KEYCODE_ESCAPE -> {
    finish()
    return true
}
```

**Tab/Shift+Tab (Element Navigation):**
```kotlin
KeyEvent.KEYCODE_TAB -> {
    if (event?.isShiftPressed == true) {
        // Shift+TAB: previous element
        val currentFocus = currentFocus
        currentFocus?.focusSearch(View.FOCUS_UP)?.requestFocus()
    } else {
        // TAB: next element
        val currentFocus = currentFocus
        currentFocus?.focusSearch(View.FOCUS_DOWN)?.requestFocus()
    }
    return true
}
```

**Up/Down Arrows (Element Navigation):**
```kotlin
KeyEvent.KEYCODE_DPAD_DOWN -> {
    val currentFocus = currentFocus
    val nextFocus = currentFocus?.focusSearch(View.FOCUS_DOWN)
    if (nextFocus != null && nextFocus != currentFocus) {
        nextFocus.requestFocus()
        return true
    }
}

KeyEvent.KEYCODE_DPAD_UP -> {
    val currentFocus = currentFocus
    val prevFocus = currentFocus?.focusSearch(View.FOCUS_UP)
    if (prevFocus != null && prevFocus != currentFocus) {
        prevFocus.requestFocus()
        return true
    }
}
```

**Supported Keys:**
- **Left Arrow / Page Up:** Previous tab
- **Right Arrow / Page Down:** Next tab
- **Escape:** Close settings activity
- **Tab:** Next UI element
- **Shift+Tab:** Previous UI element
- **Down Arrow:** Next UI element (if not already at bottom)
- **Up Arrow:** Previous UI element (if not already at top)

### 3. Activity-Scoped ViewModel Sharing

**Pattern:**
```kotlin
// SettingsActivity
private val viewModel: SettingsViewModel by viewModels()

// GeneralSettingsFragment
private val viewModel: SettingsViewModel by activityViewModels()

// DestinationsSettingsFragment
private val viewModel: SettingsViewModel by activityViewModels()

// NetworkSettingsFragment
private val viewModel: SettingsViewModel by activityViewModels()
```

**Why:**
- All fragments need access to same `AppSettings` state
- Destinations tab needs to modify same resources as other tabs
- Single source of truth for settings updates
- State survives configuration changes (e.g., screen rotation)

### 4. OffscreenPageLimit = 1

**Setting:**
```kotlin
binding.viewPager.offscreenPageLimit = 1
```

**Effect:**
- Pre-loads adjacent tab (left/right) for instant switching
- Balance between memory usage and UX
- Current tab + 1 adjacent = max 2 fragments in memory
- Reduces lag when switching tabs with keyboard navigation

---

## 5. Fragment Delegation Pattern

### GeneralSettingsFragment (1025 lines)

**Key Features:**
- **Language Switcher:** Spinner with en/ru/uk → Recreates activity via `LocaleHelper.setLocale()`
- **Theme Switcher:** RadioGroup (Light/Dark/System) → `AppCompatDelegate.setDefaultNightMode()`
- **Cache Management:**
  - Auto-suggest optimal cache size on first run via `CalculateOptimalCacheSizeUseCase`
  - "Calculate Optimal Size" button
  - "Clear Cache Now" button → Deletes Glide cache + PDF thumbnails, shows freed space
  - Cache size limit spinner (0.5GB-10GB/Unlimited)
- **Sync Configuration:**
  - Enable/disable background sync (WorkManager)
  - Sync interval slider (1-60 minutes)
  - "Sync Now" button → Manual immediate sync
- **Adaptive Layouts:**
  - Portrait: Vertical stacking of all controls
  - Landscape: Horizontal pairing (Sync+Interval, Delete+Move confirms, etc.)
  - Dynamic layout changes in `onConfigurationChanged()`

**Lifecycle:**
```kotlin
override fun onViewCreated() {
    setupVersionInfo() // Display "versionName | Build versionCode | email"
    setupViews() // Setup all spinners, switches, buttons, sliders
    observeData() // Collect settings StateFlow, update UI
    checkAndSuggestOptimalCacheSize() // First-run suggestion dialog
    setupGeneralLayouts() // Adaptive portrait/landscape layouts
}

override fun onConfigurationChanged() {
    setupGeneralLayouts() // Re-apply portrait/landscape layouts
}
```

**Key Behaviors:**
1. **Language Change:** Shows "Restart required" dialog, sets `isCacheSizeUserModified = true` to prevent re-suggestion
2. **Theme Change:** Immediately applies via `AppCompatDelegate`, recreates activity
3. **Cache Size Suggestion:**
   - Only shown on first install (when `isCacheSizeUserModified == false`)
   - Uses `CalculateOptimalCacheSizeUseCase()` to scan storage
   - Shows dialog: "Suggested: X MB based on Y MB free space"
   - Positive button: Applies and requires restart
   - Negative button: Marks `isCacheSizeUserModified = true` to never suggest again
4. **Clear Cache:**
   - Calculates current cache size in coroutine (Dispatchers.IO)
   - Shows progress dialog
   - Deletes Glide cache directory (`image_cache/`)
   - Deletes PDF thumbnail directory (`pdf_thumbnails/`)
   - Shows toast: "Cleared X MB"
   - Updates UI cache size display

**Flag to Prevent Infinite Loop:**
```kotlin
private var isUpdatingSpinner = false

binding.spinnerLanguage.setOnItemSelectedListener { ... position ->
    if (isUpdatingSpinner) return // Avoid triggering on programmatic update
    // User selection logic
}
```

### MediaSettingsFragment

**Structure:** Nested ViewPager2 with 5 sub-tabs via `MediaCategoryPagerAdapter`:
1. **ImagesSettingsFragment** - Thumbnail quality, auto-rotate, EXIF, JPEG compression
2. **VideoSettingsFragment** - Quality, hardware acceleration, seek increment
3. **AudioSettingsFragment** - Waveform display, background playback
4. **DocumentsSettingsFragment** - PDF cache, text encoding, syntax highlighting
5. **OtherMediaSettingsFragment** - GIF auto-play/speed, EPUB font/size

**Each Sub-Fragment Pattern:**
- `private val viewModel: SettingsViewModel by activityViewModels()`
- Switches/Spinners/Sliders update `viewModel.updateSettings()`
- `observeData()` collects `viewModel.settings` StateFlow, updates UI

### PlaybackSettingsFragment

**Key Settings:**
- Slideshow interval slider (1-60 seconds)
- Touch zone sensitivity slider (100-300ms hold time)
- Command panel auto-hide delay (0-10s, 0=never)
- Video playback speed (0.25x-2x)
- Repeat mode radio buttons (None/One/All)

**Validation:**
- Slideshow interval clamped to [1, 60]
- Touch zone sensitivity clamped to [100, 300]
- Auto-hide delay clamped to [0, 10]

### DestinationsSettingsFragment (351 lines)

**Key Features:**
- **RecyclerView with Drag-and-Drop:** `ItemTouchHelper` for reordering
- **Copy/Move Behavior Switches:**
  - `switchEnableCopying` → `settings.enableCopying`
  - `switchGoToNextAfterCopy` → `settings.goToNextAfterCopy`
  - `switchOverwriteOnCopy` → `settings.overwriteOnCopy`
  - `switchEnableMoving` → `settings.enableMoving`
  - `switchOverwriteOnMove` → `settings.overwriteOnMove`
- **Max Recipients Field:** AutoCompleteTextView with validation (1-30)
- **Destination Actions:**
  - Move Up/Down: Calls `viewModel.moveDestination(resource, direction)`
  - Delete: Calls `viewModel.removeDestination(resource)`
  - Color Picker: Opens `ColorPickerDialog`, calls `viewModel.updateDestinationColor(resource, color)`
- **Add Destination Button:** Opens dialog with list of all resources (filtered to non-destinations), select and call `viewModel.addDestination(resource)`

**Adapter Pattern:**
```kotlin
inner class DestinationsAdapter(
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onColorClick: (MediaResource) -> Unit
) : ListAdapter<MediaResource, ViewHolder>(ResourceDiffCallback)
```

**Drag-and-Drop Implementation:**
```kotlin
val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(...) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        // Swap in adapter list
        Collections.swap(destinationsList, fromPosition, toPosition)
        adapter.notifyItemMoved(fromPosition, toPosition)
        return true
    }
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used (no swipe-to-delete)
    }
    
    override fun clearView(...) {
        super.clearView(...)
        // After drag ends: update all destinationOrder in database
        destinationsList.forEachIndexed { index, resource ->
            viewModel.updateDestinationOrder(resource, index + 1)
        }
    }
})
itemTouchHelper.attachToRecyclerView(binding.rvDestinations)
```

**Layout Manager:**
```kotlin
private fun setupDestinationsLayoutManager() {
    val orientation = resources.configuration.orientation
    val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    binding.rvDestinations.layoutManager = GridLayoutManager(requireContext(), spanCount)
}
```
**Effect:** 2 columns in landscape, 1 column in portrait.

**Max Recipients Validation:**
```kotlin
binding.etMaxRecipients.setOnFocusChangeListener { _, hasFocus ->
    if (!hasFocus && !isUpdatingFromSettings) {
        val text = binding.etMaxRecipients.text.toString()
        val limit = text.toIntOrNull()
        if (limit != null && limit in 1..30) {
            viewModel.updateSettings(current.copy(maxRecipients = limit))
            binding.tilMaxRecipients.error = null
        } else {
            binding.tilMaxRecipients.error = getString(R.string.max_recipients_error)
            binding.etMaxRecipients.setText(viewModel.settings.value.maxRecipients.toString())
        }
    }
}
```

**Flag to Prevent Loop:**
```kotlin
private var isUpdatingFromSettings = false

binding.switchEnableCopying.setOnCheckedChangeListener { _, isChecked ->
    if (isUpdatingFromSettings) return@setOnCheckedChangeListener
    // Update settings
}
```

### NetworkSettingsFragment (145 lines)

**Key Features:**
- **Background Sync Toggle:** Enable/disable periodic WorkManager sync
- **Sync Interval Slider:** 1-24 hours (uses resource plurals for display)
- **Manual Sync Button:** Triggers `SyncNetworkResourcesUseCase.syncAll()`
- **Sync Status Display:** Real-time updates (Idle/In progress/Completed/Failed)

**Use Case Injection:**
```kotlin
@Inject
lateinit var scheduleNetworkSyncUseCase: ScheduleNetworkSyncUseCase

@Inject
lateinit var syncNetworkResourcesUseCase: SyncNetworkResourcesUseCase
```

**Background Sync Logic:**
```kotlin
binding.switchEnableBackgroundSync.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) {
        val intervalHours = binding.sliderSyncInterval.value.toLong()
        scheduleNetworkSyncUseCase(intervalHours = intervalHours)
        Timber.i("Background sync enabled (interval=$intervalHours hours)")
        showMessage(getString(R.string.background_sync_enabled))
    } else {
        scheduleNetworkSyncUseCase.cancel()
        Timber.i("Background sync disabled")
        showMessage(getString(R.string.background_sync_disabled))
    }
}
```

**Sync Interval Slider:**
```kotlin
binding.sliderSyncInterval.addOnChangeListener { _, value, fromUser ->
    if (fromUser) {
        val hours = value.toInt()
        binding.tvSyncIntervalValue.text = resources.getQuantityString(
            R.plurals.sync_interval_hours,
            hours,
            hours
        )
        
        // Reschedule if sync is enabled
        if (binding.switchEnableBackgroundSync.isChecked) {
            scheduleNetworkSyncUseCase(intervalHours = value.toLong())
            Timber.d("Sync interval updated to $hours hours")
        }
    }
}
```

**Manual Sync:**
```kotlin
private fun performManualSync() {
    binding.btnSyncNow.isEnabled = false
    binding.tvSyncStatus.text = getString(R.string.sync_status_in_progress)
    
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                syncNetworkResourcesUseCase.syncAll()
            }
            
            result.onSuccess { count ->
                binding.tvSyncStatus.text = getString(R.string.sync_status_completed, count)
                showMessage(getString(R.string.sync_completed_successfully, count))
                Timber.i("Manual sync completed, synced $count resources")
            }.onFailure { error ->
                binding.tvSyncStatus.text = getString(R.string.sync_status_failed)
                showMessage(getString(R.string.sync_failed, error.message ?: "Unknown error"))
                Timber.e(error, "Manual sync failed")
            }
        } finally {
            binding.btnSyncNow.isEnabled = true
        }
    }
}
```

---

## 6. ViewModel State & Events

### State

```kotlin
data class AppSettings(
    // General
    val languageCode: String = "en",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val displayMode: DisplayMode = DisplayMode.AUTO,
    val gridColumns: Int = 3,
    val showFullFileName: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val dateFormat: DateFormat = DateFormat.DD_MM_YYYY,
    val enableSync: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val cacheSizeMb: Int = 2048,
    val autoCacheManagement: Boolean = true,
    val isCacheSizeUserModified: Boolean = false,
    val enableFavorites: Boolean = true,
    val preventSleepDuringPlayback: Boolean = true,
    val confirmDelete: Boolean = true,
    val confirmMove: Boolean = true,
    
    // Media (Images/Video/Audio/Documents/Other)
    val thumbnailQuality: ThumbnailQuality = ThumbnailQuality.MEDIUM,
    val loadFullResolution: Boolean = false,
    val autoRotateImages: Boolean = true,
    val showExifData: Boolean = true,
    val jpegCompression: Int = 90,
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val hardwareAcceleration: Boolean = true,
    val autoPlayVideo: Boolean = false,
    val showVideoControls: Boolean = true,
    val seekIncrementSeconds: Int = 10,
    val generateThumbnails: Boolean = true,
    val showWaveform: Boolean = true,
    val backgroundAudioPlayback: Boolean = false,
    val pdfPageCache: Int = 10,
    val defaultEncoding: String = "UTF-8",
    val syntaxHighlighting: Boolean = true,
    val textZoomLevel: Int = 100,
    val gifAutoPlay: Boolean = true,
    val gifPlaybackSpeed: Float = 1.0f,
    val supportEpub: Boolean = true,
    val epubFont: String = "Serif",
    val epubFontSize: Int = 16,
    
    // Playback
    val defaultSlideshowInterval: Int = 3,
    val randomOrder: Boolean = false,
    val loopMode: LoopMode = LoopMode.REPEAT,
    val autoStartSlideshow: Boolean = false,
    val enableTouchZones: Boolean = true,
    val touchZoneSensitivityMs: Int = 200,
    val showZoneOverlay: Boolean = false,
    val commandPanelAutoHideSeconds: Int = 3,
    val commandPanelPosition: PanelPosition = PanelPosition.BOTTOM,
    val showThumbnailsInPanel: Boolean = true,
    val videoRepeatMode: RepeatMode = RepeatMode.NONE,
    val defaultPlaybackSpeed: Float = 1.0f,
    val resumeFromLastPosition: Boolean = true,
    val skipSilence: Boolean = false,
    
    // Destinations
    val enableCopying: Boolean = true,
    val goToNextAfterCopy: Boolean = true,
    val overwriteOnCopy: Boolean = false,
    val enableMoving: Boolean = true,
    val overwriteOnMove: Boolean = false,
    val maxRecipients: Int = 10
)
```

### ViewModel Methods

**Update Settings:**
```kotlin
fun updateSettings(settings: AppSettings) {
    viewModelScope.launch {
        settingsRepository.updateSettings(settings)
    }
}
```

**Reset to Factory Defaults:**
```kotlin
fun resetToDefaults() {
    viewModelScope.launch {
        settingsRepository.resetToDefaults()
    }
}
```

**Destination Management:**
```kotlin
fun moveDestination(resource: MediaResource, direction: Int) {
    // Swap destinationOrder with adjacent resource
    // Update both resources in database
}

fun removeDestination(resource: MediaResource) {
    viewModelScope.launch {
        val updated = resource.copy(isDestination = false, destinationOrder = null)
        updateResourceUseCase(updated)
    }
}

fun updateDestinationColor(resource: MediaResource, color: Int) {
    viewModelScope.launch {
        val updated = resource.copy(destinationColor = color)
        updateResourceUseCase(updated)
    }
}

fun addDestination(resource: MediaResource) {
    viewModelScope.launch {
        val maxOrder = destinations.value.maxOfOrNull { it.destinationOrder ?: 0 } ?: 0
        val updated = resource.copy(
            isDestination = true,
            destinationOrder = maxOrder + 1,
            destinationColor = DestinationColors.getNextColor()
        )
        updateResourceUseCase(updated)
    }
}
```

---

## 7. Complete User Workflows

### Workflow 1: Change Language

1. User opens SettingsActivity → General tab.
2. User clicks Language spinner → Dropdown shows: English | Русский | Українська.
3. User selects language → `onItemSelected()` triggered.
4. Check `isUpdatingSpinner` flag (if true, skip).
5. Map position to language code (0=en, 1=ru, 2=uk).
6. Check if different from current: `if (newLangCode != viewModel.settings.value.languageCode)`.
7. Call `LocaleHelper.setLocale(requireContext(), newLangCode)`.
8. Update settings: `viewModel.updateSettings(current.copy(languageCode = newLangCode))`.
9. Show "Restart required" dialog with "Restart now" / "Restart later" buttons.
10. If "Restart now": Call `requireActivity().recreate()` (recreates activity with new locale).

### Workflow 2: Clear Cache

1. User opens SettingsActivity → General tab.
2. User clicks "Clear Cache Now" button.
3. Disable button: `binding.btnClearCache.isEnabled = false`.
4. Launch coroutine on Dispatchers.IO:
   - Calculate current cache size: `calculateCacheSize()` (scans Glide cache + PDF thumbnails).
   - Delete Glide cache directory: `context.cacheDir.resolve("image_cache").deleteRecursively()`.
   - Delete PDF thumbnails directory: `context.filesDir.resolve("pdf_thumbnails").deleteRecursively()`.
5. Switch to Dispatchers.Main:
   - Show toast: "Cleared X MB".
   - Update UI: `binding.tvCurrentCacheSize.text = "0 MB"`.
   - Re-enable button: `binding.btnClearCache.isEnabled = true`.

### Workflow 3: Configure Background Sync

1. User opens SettingsActivity → Network tab (4th tab).
2. User toggles "Enable background sync" switch:
   - **If enabled:**
     - Get current interval: `val hours = binding.sliderSyncInterval.value.toLong()`.
     - Call `scheduleNetworkSyncUseCase(intervalHours = hours)`.
     - WorkManager creates `PeriodicWorkRequest` with interval.
     - Show Snackbar: "Background sync enabled".
     - Timber logs: "Background sync enabled (interval=X hours)".
   - **If disabled:**
     - Call `scheduleNetworkSyncUseCase.cancel()`.
     - WorkManager cancels all sync work requests.
     - Show Snackbar: "Background sync disabled".
     - Timber logs: "Background sync disabled".
3. User adjusts sync interval slider (1-24 hours):
   - Update displayed value: `binding.tvSyncIntervalValue.text = "X hours"` (plural-aware).
   - If sync enabled: Automatically reschedule with new interval.

### Workflow 4: Manual Sync

1. User opens SettingsActivity → Network tab.
2. User clicks "Sync now" button.
3. Disable button: `binding.btnSyncNow.isEnabled = false`.
4. Update status: `binding.tvSyncStatus.text = "In progress..."`.
5. Launch coroutine on Dispatchers.IO:
   - Call `syncNetworkResourcesUseCase.syncAll()` → Returns `Result<Int>`.
   - Scans all network resources (SMB/SFTP/FTP/Cloud).
   - Compares remote file lists with cached database entries.
   - Updates database with new files, removes deleted files.
6. Switch to Dispatchers.Main:
   - **On success:**
     - Update status: `binding.tvSyncStatus.text = "Completed: X resources synced"`.
     - Show Snackbar: "Sync completed successfully: X resources".
     - Timber logs: "Manual sync completed, synced X resources".
   - **On failure:**
     - Update status: `binding.tvSyncStatus.text = "Failed"`.
     - Show Snackbar: "Sync failed: [error message]".
     - Timber logs: "Manual sync failed: [exception]".
7. Re-enable button in `finally` block: `binding.btnSyncNow.isEnabled = true`.

### Workflow 5: Reorder Destinations

1. User opens SettingsActivity → Destinations tab (3rd tab).
2. RecyclerView displays up to 10 destinations with drag handles (vertical dots icon).
3. User long-presses drag handle → Item enters drag mode.
4. User drags item up/down → `ItemTouchHelper.onMove()` triggered:
   - Swap positions in adapter's list: `Collections.swap(destinationsList, fromPos, toPos)`.
   - Notify adapter: `adapter.notifyItemMoved(fromPos, toPos)`.
5. User releases item → `ItemTouchHelper.clearView()` triggered:
   - Update all `destinationOrder` values in database to match new positions.
   - Iterate through list: `destinationsList.forEachIndexed { index, resource -> viewModel.updateDestinationOrder(resource, index + 1) }`.
6. Destinations now reordered in database and UI.
7. BrowseActivity quick-copy panel reflects new order (buttons 1-10 correspond to destinationOrder).

### Workflow 6: Change Destination Color

1. User opens SettingsActivity → Destinations tab.
2. User clicks colored circle marker next to destination name.
3. `ColorPickerDialog` opens with predefined palette (10 colors).
4. User selects color → Dialog returns selected color via callback.
5. Call `viewModel.updateDestinationColor(resource, selectedColor)`.
6. ViewModel updates resource in database: `resource.copy(destinationColor = selectedColor)`.
7. Adapter updates item: `adapter.notifyItemChanged(position)`.
8. BrowseActivity quick-copy buttons reflect new color (button background tint).

### Workflow 7: Remove Destination

1. User opens SettingsActivity → Destinations tab.
2. User clicks "X" icon next to destination.
3. Call `viewModel.removeDestination(resource)`.
4. ViewModel updates resource: `resource.copy(isDestination = false, destinationOrder = null)`.
5. Adapter removes item: `adapter.submitList(newList)` (filtered to exclude non-destinations).
6. BrowseActivity quick-copy panel no longer shows this destination.

### Workflow 8: Add Destination

1. User opens SettingsActivity → Destinations tab.
2. User clicks "Add destination" button.
3. Dialog opens with list of all resources (filtered to `isDestination = false`).
4. User selects resource → Dialog closes.
5. Call `viewModel.addDestination(resource)`.
6. ViewModel:
   - Calculates next order: `maxOrder = destinations.value.maxOfOrNull { it.destinationOrder ?: 0 } ?: 0`.
   - Gets next available color: `DestinationColors.getNextColor()`.
   - Updates resource: `resource.copy(isDestination = true, destinationOrder = maxOrder + 1, destinationColor = color)`.
7. Adapter adds item: `adapter.submitList(newList)`.
8. BrowseActivity quick-copy panel shows new destination button.

### Workflow 9: Suggest Optimal Cache Size (First Run)

1. User installs app, opens SettingsActivity → General tab for first time.
2. Check: `if (!settings.isCacheSizeUserModified)` → True (factory default).
3. Call `calculateOptimalCacheSizeUseCase()` on background thread:
   - Scans device storage: `StatFs` for available space.
   - Calculates optimal size: 10% of free space, clamped to [512MB, 4GB].
4. Compare: `if (settings.cacheSizeMb != optimalSizeMb)` → True (likely different).
5. Show dialog:
   - Title: "Optimize Cache Size"
   - Message: "We recommend X MB cache based on Y MB free space. Apply?"
   - Positive button: "Apply" → Calls `viewModel.updateSettings(current.copy(cacheSizeMb = optimalSizeMb, isCacheSizeUserModified = false))`, shows restart dialog.
   - Negative button: "Keep current" → Calls `viewModel.updateSettings(current.copy(isCacheSizeUserModified = true))` (marks as user-modified to prevent future suggestions).
6. Dialog dismissed.

---

## 8. Testing Considerations

### Unit Tests

**SettingsViewModel:**
- `updateSettings()` persists to SettingsRepository
- `resetToDefaults()` restores factory settings
- `moveDestination()` swaps destinationOrder correctly
- `removeDestination()` clears isDestination flag
- `addDestination()` assigns next available order and color

**CalculateOptimalCacheSizeUseCase:**
- Returns value in range [512MB, 4GB]
- Handles low storage gracefully (recommends minimum)
- Handles high storage (caps at maximum)

### Integration Tests

**Fragment Interaction:**
- GeneralSettingsFragment updates `viewModel.settings` on language change
- DestinationsSettingsFragment calls `viewModel.moveDestination()` on drag-and-drop
- NetworkSettingsFragment calls `scheduleNetworkSyncUseCase()` on toggle

**Keyboard Navigation:**
- Left/Right arrows switch tabs
- Escape closes activity
- Tab/Shift+Tab navigates between UI elements
- Up/Down arrows navigate between elements

### UI Tests (Espresso)

**Tab Switching:**
- Click tab → Verify ViewPager2 shows correct fragment
- Press Right arrow → Verify next tab selected
- Press Left arrow → Verify previous tab selected
- Press Escape → Verify activity finishes

**Settings Persistence:**
- Change language → Restart activity → Verify language persisted
- Change theme → Verify theme applied immediately
- Clear cache → Verify cache size = 0 MB

**Destination Reordering:**
- Drag destination from position 2 to position 5
- Verify `destinationOrder` values updated in database
- Verify BrowseActivity quick-copy buttons reflect new order

---

## 9. Known Issues & Limitations

1. **Language Change Requires Restart:**
   - **Issue:** Locale change via `LocaleHelper.setLocale()` only affects new activities.
   - **Workaround:** Show "Restart required" dialog, call `recreate()`.
   - **Future:** Implement runtime locale switching without restart (Jetpack Compose?).

2. **Theme Change Recreates Activity:**
   - **Issue:** `AppCompatDelegate.setDefaultNightMode()` triggers `recreate()`.
   - **Impact:** Brief UI flash during theme switch.
   - **Future:** Pre-render both themes, use crossfade animation.

3. **Cache Size Calculation Slow (Large Caches):**
   - **Issue:** Scanning Glide cache (2GB+) can take 5-10 seconds.
   - **Workaround:** Launch on Dispatchers.IO, show progress dialog.
   - **Future:** Cache size tracking in database (update on file add/remove).

4. **Destination Drag-and-Drop No Haptic Feedback:**
   - **Issue:** No vibration on drag start/end.
   - **Future:** Add `performHapticFeedback(LONG_PRESS)` on drag start.

5. **Network Sync Notification Missing:**
   - **Issue:** Background sync runs silently, no user feedback.
   - **Future:** Show notification during sync with progress bar.

6. **No Settings Export/Import:**
   - **Issue:** Users can't backup settings or transfer to new device.
   - **Future:** Add "Export settings" button (JSON file), "Import settings" (file picker).

7. **Optimal Cache Size Suggestion Intrusive:**
   - **Issue:** Dialog blocks first-time user experience.
   - **Future:** Show as dismissible Snackbar instead of dialog.

8. **Sync Interval Limited to Hours:**
   - **Issue:** Minimum interval is 1 hour (WorkManager constraint).
   - **Future:** Allow minutes for testing (15/30/45 min intervals).

9. **No Per-Resource Cache Settings:**
   - **Issue:** Single global cache limit applies to all resources.
   - **Future:** Allow per-resource cache allocation (e.g., 500MB for SMB, 1GB for Cloud).

10. **Destinations Limited to 10:**
    - **Issue:** Hard limit of 10 destinations (UI constraint).
    - **Future:** Implement pagination or scrollable list for 20+ destinations.

---

## 10. Future Enhancements

1. **Settings Search:** Search bar at top to filter settings by keyword.
2. **Settings Categories:** Group related settings (e.g., "Performance", "Privacy").
3. **Advanced Mode Toggle:** Hide advanced settings by default, show with toggle.
4. **Settings Profiles:** Multiple profiles (e.g., "Work", "Home") with quick switch.
5. **Undo/Redo Settings:** Stack of previous settings states with undo button.
6. **Settings Cloud Sync:** Sync settings across devices via Firebase/Google Drive.
7. **Accessibility Settings:** Font size, contrast, screen reader optimizations.
8. **Developer Options:** Hidden menu (tap version 7 times) for debug settings.
9. **Settings Recommendations:** AI-driven suggestions based on usage patterns.
10. **Settings Shortcuts:** Android shortcuts for common settings (e.g., "Clear cache", "Toggle sync").

---

## 11. Cross-References

**Related Specifications:**
- [02a_main_activity_logic.md](02a_main_activity_logic.md) - MainActivity launches SettingsActivity
- [02b_browse_activity_logic.md](02b_browse_activity_logic.md) - BrowseActivity respects settings (grid columns, slideshow interval)
- [04a_player_logic.md](04a_player_logic.md) - PlayerActivity uses playback settings
- [22_cache_strategies.md](../22_cache_strategies.md) - Cache management implementation
- [03_resource_management.md](../03_resource_management.md) - Destination resource management

**Related Files:**
- `SettingsViewModel.kt` (183 lines) - Settings state management
- `SettingsRepository.kt` - SharedPreferences persistence
- `CalculateOptimalCacheSizeUseCase.kt` - Storage analysis
- `ScheduleNetworkSyncUseCase.kt` - WorkManager scheduling
- `SyncNetworkResourcesUseCase.kt` - Network resource synchronization

---

**Last Updated:** January 6, 2026  
**Specification Version:** 1.0  
**Corresponds to Code Version:** Build 25161854 (2025/12/16 18:54)
