# 8. Base Classes & Widget

## BaseActivity
**Package:** `com.sza.fastmediasorter.core.ui`  
**Purpose:** Abstract base class for all activities.
**Features:**
- ViewBinding abstraction (`getViewBinding()`)
- Locale application (`attachBaseContext`)
- Screen awake retention (`shouldKeepScreenAwake()`)
- Touch event logging (UserActionLogger)
- Configuration change handling

**BaseActivity (Abstract Base Class)**
**Package:** `com.sza.fastmediasorter.core.ui`  
**Purpose:** Base class providing common functionality for all activities  
**Type:** Abstract Class  
**Generic:** `<VB : ViewBinding>`

**Functionality:**
- **ViewBinding Lifecycle:**
  - Creates ViewBinding via `getViewBinding()` in `onCreate()`
  - Sets content view with `binding.root`
  - Nullifies binding in `onDestroy()` to prevent memory leaks
  - Subclasses access UI via `binding.element`
- **Locale Application:**
  - Applies user-selected locale in `attachBaseContext()`
  - Uses `LocaleHelper.applyLocale()` to wrap context
  - Ensures all text strings use correct language
  - Persists locale selection across app launches
- **Screen Awake Management:**
  - Adds `FLAG_KEEP_SCREEN_ON` in `onCreate()` if `shouldKeepScreenAwake()` returns true
  - PlayerActivity: Returns true (keeps screen on during playback)
  - SettingsActivity: Returns false (allows screen timeout)
  - Removes flag in `onDestroy()` to restore default behavior
- **Configuration Change Handling:**
  - `onConfigurationChanged()` detects orientation/screen size changes
  - Calls `onLayoutConfigurationChanged(newConfig)` for subclass customization
  - Uses `binding.root.post { }` to defer layout recalculations
  - BrowseActivity: Adjusts grid columns based on width
  - PlayerActivity: Repositions command panels and touch zones
- **Touch Event Logging:**
  - `dispatchTouchEvent()` intercepts all touch events
  - Logs ACTION_DOWN (finger down) and ACTION_UP (finger lift)
  - Records X/Y coordinates and activity class name
  - Useful for debugging gesture conflicts and touch zone issues
- **Lifecycle Optimization:**
  - `onCreate()` sets content view immediately
  - Defers `setupViews()` and `observeData()` to post-layout via `binding.root.post { }`
  - Allows first frame to render without blocking
  - Improves perceived startup performance
- **Timber Logging:**
  - Logs activity lifecycle events: onCreate, onStart, onResume, onPause, onStop, onDestroy
  - Includes activity class name for debugging
  - Format: "MainActivity.onCreate() called"
- **Error Handling:**
  - ViewBinding creation failures logged with Timber.e
  - Graceful degradation if locale application fails
- **Inheritance Benefits:**
  - Eliminates repetitive code in all activities
  - Consistent locale handling across app
  - Centralized touch event monitoring
  - Uniform configuration change behavior

**Features:**
- **ViewBinding Lifecycle Management:**
  - Automatic ViewBinding creation and cleanup
  - Protected `binding` property accessible to subclasses
  - Null safety with lifecycle-aware binding
  
- **Locale Support:**
  - Applies user-selected locale via `LocaleHelper.applyLocale()`
  - Overrides `attachBaseContext()` for locale injection
  
- **Screen Awake Management:**
  - Configurable "keep screen awake" functionality
  - `shouldKeepScreenAwake()`: Override to control behavior (default: true)
  - Automatically applies `FLAG_KEEP_SCREEN_ON` during `onCreate()`
  
- **Configuration Change Handling:**
  - Handles screen rotation and orientation changes
  - `onLayoutConfigurationChanged(newConfig)`: Override for layout recalculations
  - Supports landscape mode on phones (width > height treated as landscape)
  - Defers layout updates via `binding.root.post` for smooth transitions
  
- **Touch Event Logging:**
  - Logs all touch events (ACTION_DOWN, ACTION_UP) via `UserActionLogger`
  - Captures X/Y coordinates and activity context
  - Useful for debugging gesture interactions
  
- **Lifecycle Optimization:**
  - Defers heavy initialization (`setupViews()`, `observeData()`) to post-layout
  - Allows first frame to render quickly
  - Timber logging for onCreate/onDestroy events

**Abstract Methods (must be implemented by subclasses):**
```kotlin
abstract fun getViewBinding(): VB
abstract fun setupViews()
abstract fun observeData()
```

**Open Methods (can be overridden):**
```kotlin
protected open fun shouldKeepScreenAwake(): Boolean = true
protected open fun onLayoutConfigurationChanged(newConfig: Configuration)
```

**Usage Example:**
```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun getViewBinding() = ActivityMainBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        // Initialize UI elements
    }
    
    override fun observeData() {
        // Observe ViewModel LiveData/Flow
    }
    
    override fun shouldKeepScreenAwake() = true // Keep screen on
}
```

**All Activities Extending BaseActivity:**
- `WelcomeActivity`
- `MainActivity`
- `AddResourceActivity`
- `EditResourceActivity`
- `BrowseActivity`
- `PlayerActivity`
- `SettingsActivity`
- `GoogleDriveFolderPickerActivity`
- `OneDriveFolderPickerActivity`
- `DropboxFolderPickerActivity`


## BaseFragment
**Package:** `com.sza.fastmediasorter.core.ui`  
**Purpose:** Abstract base class for all fragments.
**Features:**
- ViewBinding management (auto-clearing in `onDestroyView`)
- Lifecycle-aware data observation
- Safe ViewBinding cleanup (nullify in onDestroyView)

---

## ResourceLaunchWidgetConfigActivity
**Package:** `com.sza.fastmediasorter.widget`  
**Purpose:** Configuration screen for home screen widget  
**Type:** ComponentActivity (Jetpack Compose)  
**Layout:** Compose UI (no XML layout)

**UI Elements (Compose):**

**Top App Bar:**
- Title: "Select Resource" (localized via `R.string.widget_select_resource`)
- Navigation Icon: Close button (X icon)
  - Action: Cancel configuration and close activity

**Main Content:**
- **Loading State:**
  - Circular progress indicator centered on screen
  - Shown while fetching resources from database
  
- **Empty State:**
  - Text: "No resources available" (localized via `R.string.no_resources_available`)
  - Centered on screen
  - Shown when user has no configured resources
  
- **Resource List (LazyColumn):**
  - Vertical scrollable list of all available resources
  - Padding: 16dp around list, 8dp spacing between items
  - Each item is a Material3 Card with elevation
  
**Resource Item Card (Composable):**
- **Resource Name** (TitleMedium typography)
  - Primary text, bold
  
- **Resource Path** (BodySmall typography)
  - Secondary text in gray color
  - Shows full path (e.g., "/storage/emulated/0/Pictures")
  
- **Resource Type Badge** (LabelSmall typography)
  - Colored label showing resource type:
    - "Local Storage" → `R.string.resource_type_local`
    - "SMB Network" → `R.string.resource_type_smb`
    - "SFTP" → `R.string.resource_type_sftp`
    - "FTP" → `R.string.resource_type_ftp`
    - "Cloud Storage" → `R.string.resource_type_cloud`
  - Color: Primary theme color
  
- **Click Action:**
  - Saves widget configuration to SharedPreferences
  - Updates widget appearance on home screen
  - Returns RESULT_OK to launcher
  - Closes activity

**Features:**
- **Resource Selection:**
  - Displays all resources from Room database
  - Shows resource name, path, and type badge
  - Click to select resource for widget
- **Widget Configuration:**
  - Saves selected resource ID to SharedPreferences with appWidgetId key
  - Updates widget appearance on home screen immediately
  - Sets result to RESULT_OK with appWidgetId in intent
- **Type Badges:**
  - Color-coded labels: Local (green), SMB (blue), SFTP (orange), FTP (purple), Cloud (pink)
  - Localized type names via string resources
- **Empty State:**
  - Shows message if no resources configured
  - Guides user to add resources in main app
- **Close Button:**
  - Cancels configuration without creating widget
  - Returns RESULT_CANCELED to launcher

**Functionality:**
- **Widget Setup Flow:**
  - Launcher invokes activity with ACTION_APPWIDGET_CONFIGURE
  - Activity receives appWidgetId from intent extras
  - User selects resource from list
  - Activity saves configuration: `prefs.edit().putLong(\"widget_$appWidgetId\", resourceId).apply()`
  - Updates widget via `AppWidgetManager.updateAppWidget()`
  - Returns result: `setResult(RESULT_OK, Intent().putExtra(EXTRA_APPWIDGET_ID, appWidgetId))`
  - Finishes activity
- **Resource Loading:**
  - ViewModel queries Room: `resourceDao.getAllResources()`
  - Observes LiveData for real-time updates
  - Filters only non-empty resources (file count > 0)
  - Sorts by name (alphabetical)
- **Compose UI:**
  - Material3 theme with dynamic colors (Android 12+)
  - Scaffold with TopAppBar + LazyColumn
  - Loading: CircularProgressIndicator
  - Empty: Text with padding
  - List: Card items with onClick handlers
- **Widget Update:**
  - Creates RemoteViews with resource name and icon
  - Sets PendingIntent for widget click: Launch MainActivity with resource filter
  - Updates widget background color based on resource type
  - Sends broadcast to ResourceLaunchWidget provider
- **Configuration Persistence:**
  - Stores in default SharedPreferences: \"widget_config\"
  - Key format: \"widget_<appWidgetId>\" → resourceId (Long)
  - Used by ResourceLaunchWidget to determine which resource to open
- **Cancel Handling:**
  - Close button or back press: Returns RESULT_CANCELED
  - Launcher deletes widget if configuration cancelled
  - No configuration saved to preferences
- **Error Handling:**
  - Database error: Shows error Snackbar
  - Invalid appWidgetId: Logs error and finishes immediately
- **Analytics:**
  - Tracks widget creation count
  - Logs selected resource types
  - Reports configuration completion rate
  
**Widget Configuration Details:**
  - User selects a resource to associate with widget
  - Configuration saved to `widget_prefs` SharedPreferences
  - Keys: `resource_id_[widgetId]`, `resource_name_[widgetId]`
  
- **Widget Update:**
  - Calls `ResourceLaunchWidgetProvider.updateAppWidget()`
  - Widget displays selected resource name/icon
  - Tapping widget opens BrowseActivity for that resource
  
- **Dependency Injection:**
  - Uses Hilt `@AndroidEntryPoint`
  - Custom EntryPoint to access `AppDatabase` in Compose
  - `EntryPointAccessors.fromApplication()` retrieves database
  
- **Compose Integration:**
  - Material3 theme
  - Reactive UI with `mutableStateOf`
  - Coroutine scope for database queries (`rememberCoroutineScope`)
  - `LaunchedEffect` for initial data loading
  
- **Flow:**
  1. User adds widget to home screen
  2. Launcher opens ResourceLaunchWidgetConfigActivity
  3. Activity displays list of configured resources
  4. User selects a resource
  5. Configuration saved, widget updated
  6. Activity returns RESULT_OK and closes
  7. Widget now shows selected resource and opens it on tap

**Intent Extras (Received):**
```kotlin
AppWidgetManager.EXTRA_APPWIDGET_ID: Int // Widget instance ID
```

**Intent Extras (Returned):**
```kotlin
AppWidgetManager.EXTRA_APPWIDGET_ID: Int // Widget instance ID
// Result code: RESULT_OK or RESULT_CANCELED
```

**SharedPreferences Keys:**
- Preferences file: `"widget_prefs"`
- `resource_id_[appWidgetId]`: Long (resource database ID)
- `resource_name_[appWidgetId]`: String (resource display name)

**Related Components:**
- `ResourceLaunchWidgetProvider`: BroadcastReceiver handling widget updates
- `AppDatabase`: Provides resource data
- Material3 Compose theme
