# 33. Navigation Graph & Deep Links

**Last Updated**: January 6, 2026  
**Purpose**: Navigation structure, screen flow, deep link configuration, and Intent extras catalog for FastMediaSorter v2.

This document provides visual navigation graph, deep link specifications, back stack management, and Intent parameter reference.

---

## Overview

FastMediaSorter uses **Activity-based navigation** (not Jetpack Navigation):
- **6 Activities**: MainActivity, BrowseActivity, PlayerActivity, SettingsActivity, EditResourceActivity, InfoActivity
- **Standard Intent navigation**: `startActivity(Intent(...))`
- **Deep Links**: Support for opening specific screens from external apps/notifications

### Navigation Principles

1. **Clear hierarchy**: MainActivity → BrowseActivity → PlayerActivity
2. **Predictable back stack**: Back button returns to previous screen
3. **Deep link support**: Open specific resource/file from URL
4. **State restoration**: Handle process death, configuration changes

---

## Table of Contents

1. [Navigation Graph (Visual)](#1-navigation-graph-visual)
2. [Activity Descriptions](#2-activity-descriptions)
3. [Navigation Patterns](#3-navigation-patterns)
4. [Intent Extras Catalog](#4-intent-extras-catalog)
5. [Deep Links](#5-deep-links)
6. [Back Stack Management](#6-back-stack-management)
7. [State Restoration](#7-state-restoration)

---

## 1. Navigation Graph (Visual)

**Full diagram**: [diagrams/navigation_flow.md](diagrams/navigation_flow.md)

**Summary**:
- **MainActivity** (entry) → BrowseActivity (click resource) / SettingsActivity (overflow menu)
- **BrowseActivity** → PlayerActivity (click file) / EditResourceActivity (long-press)
- **PlayerActivity** → InfoActivity (overflow menu)

**Mermaid version**: [diagrams/navigation_mermaid.md](diagrams/navigation_mermaid.md)

---

## 2. Activity Descriptions

### MainActivity

**Purpose**: Entry point, displays all registered resources.

**Features**:
- RecyclerView with resource list
- FAB → AddResourceBottomSheet
- Long-press → EditResourceActivity
- Click → BrowseActivity
- Overflow menu → SettingsActivity, InfoActivity (about app)

**Lifecycle**:
- `onCreate`: Load resources from Room DB (Flow)
- `onResume`: Refresh list (detect external changes)

---

### BrowseActivity

**Purpose**: Display files in selected resource.

**Features**:
- RecyclerView with file list (or ViewPager2 in grid mode)
- Toolbar: Sort (name/date/size), Filter (images/videos/all), Display mode (list/grid)
- Long-press → Selection mode (multi-select)
- Click file → PlayerActivity
- FAB → Quick actions (copy to destinations)

**Intent Extras**:
- `EXTRA_RESOURCE_ID` (Long): Resource to browse

**Special Cases**:
- Pagination: Auto-enabled for 1000+ files
- Network resources: Show connection status

---

### PlayerActivity

**Purpose**: View/play media files with swipe navigation.

**Features**:
- ViewPager2: Swipe between files
- ExoPlayer: Video playback
- Zoom/Pan: Image viewer
- Bottom toolbar: Copy/Move/Delete/Share
- Overflow menu: Edit image, Rotate, Info

**Intent Extras**:
- `EXTRA_FILE_INDEX` (Int): Current file index
- `EXTRA_RESOURCE_ID` (Long): Parent resource ID

**Complex Interactions**:
- Undo system: Soft-delete to `.trash/` folder
- Network images: Download → edit → upload back

---

### EditResourceActivity

**Purpose**: Add/edit resource configuration.

**Features**:
- Name, Path, Type (Local/SMB/SFTP/FTP/Cloud)
- For network: Username, Password, Domain, Port
- For cloud: OAuth sign-in button
- Test Connection button
- Mark as Destination checkbox

**Intent Extras**:
- `EXTRA_RESOURCE_ID` (Long, optional): Edit existing resource (null = add new)

**Result**:
- `RESULT_OK`: Resource saved
- `RESULT_CANCELED`: User canceled

---

### SettingsActivity

**Purpose**: App preferences.

**Features**:
- PreferenceFragmentCompat
- Categories: Display, Cache, Network, About
- Cache size slider (500MB - 5GB)
- Clear cache button
- Auto-sync toggle

**No Intent Extras**

---

### InfoActivity

**Purpose**: Display file metadata (Exif, size, date).

**Features**:
- Image: Exif data (camera, GPS, date)
- Video: Duration, codec, resolution
- Audio: Artist, album, bitrate
- General: Size, modified date, permissions

**Intent Extras**:
- `EXTRA_FILE_PATH` (String): File to display info for

---

## 3. Navigation Patterns

### Pattern 1: Simple Navigation

```kotlin
// MainActivity → BrowseActivity
val intent = Intent(this, BrowseActivity::class.java).apply {
    putExtra(BrowseActivity.EXTRA_RESOURCE_ID, resource.id)
}
startActivity(intent)
```

---

### Pattern 2: Result Navigation

```kotlin
// MainActivity → EditResourceActivity (expect result)
val editResourceLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        // Resource saved, refresh list
        viewModel.refreshResources()
    }
}

val intent = Intent(this, EditResourceActivity::class.java).apply {
    putExtra(EditResourceActivity.EXTRA_RESOURCE_ID, resource.id)
}
editResourceLauncher.launch(intent)
```

---

### Pattern 3: Navigation with Animation

```kotlin
val intent = Intent(this, PlayerActivity::class.java)
val options = ActivityOptions.makeSceneTransitionAnimation(
    this,
    imageView,
    "shared_image"
).toBundle()
startActivity(intent, options)
```

---

### Pattern 4: Clear Top (Return to Main)

```kotlin
// From any activity → MainActivity (clear back stack)
val intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}
startActivity(intent)
finish()
```

---

## 4. Intent Extras Catalog

### Common Extras

| Extra Name | Type | Required | Description | Example |
|------------|------|----------|-------------|---------|
| `EXTRA_RESOURCE_ID` | Long | Yes (Browse/Player) | Resource ID from Room | `1L` |
| `EXTRA_FILE_INDEX` | Int | Yes (Player) | Index in file list | `5` |
| `EXTRA_FILE_PATH` | String | Yes (Info) | Absolute file path | `"/sdcard/DCIM/photo.jpg"` |

---

### BrowseActivity Extras

```kotlin
companion object {
    const val EXTRA_RESOURCE_ID = "resource_id" // Long
    const val EXTRA_INITIAL_PATH = "initial_path" // String (optional, subdirectory)
}

// Usage
val intent = Intent(context, BrowseActivity::class.java).apply {
    putExtra(EXTRA_RESOURCE_ID, 42L)
    putExtra(EXTRA_INITIAL_PATH, "/subfolder") // Open specific subfolder
}
startActivity(intent)
```

---

### PlayerActivity Extras

```kotlin
companion object {
    const val EXTRA_RESOURCE_ID = "resource_id" // Long
    const val EXTRA_FILE_INDEX = "file_index" // Int
    const val EXTRA_FILES_JSON = "files_json" // String (JSON array, optional)
}

// Usage 1: Pass file index (ViewModel reloads files)
val intent = Intent(context, PlayerActivity::class.java).apply {
    putExtra(EXTRA_RESOURCE_ID, resource.id)
    putExtra(EXTRA_FILE_INDEX, 3)
}

// Usage 2: Pass serialized files (avoid reload)
val filesJson = Json.encodeToString(ListSerializer(MediaFile.serializer()), files)
val intent = Intent(context, PlayerActivity::class.java).apply {
    putExtra(EXTRA_RESOURCE_ID, resource.id)
    putExtra(EXTRA_FILE_INDEX, 3)
    putExtra(EXTRA_FILES_JSON, filesJson) // Optional optimization
}
startActivity(intent)
```

---

### EditResourceActivity Extras

```kotlin
companion object {
    const val EXTRA_RESOURCE_ID = "resource_id" // Long (optional, null = add new)
}

// Add new resource
val intent = Intent(context, EditResourceActivity::class.java)
startActivityForResult(intent, REQUEST_ADD_RESOURCE)

// Edit existing resource
val intent = Intent(context, EditResourceActivity::class.java).apply {
    putExtra(EXTRA_RESOURCE_ID, resource.id)
}
startActivityForResult(intent, REQUEST_EDIT_RESOURCE)
```

---

### InfoActivity Extras

```kotlin
companion object {
    const val EXTRA_FILE_PATH = "file_path" // String
    const val EXTRA_FILE_URI = "file_uri" // String (alternative to path)
}

val intent = Intent(context, InfoActivity::class.java).apply {
    putExtra(EXTRA_FILE_PATH, file.path)
}
startActivity(intent)
```

---

## 5. Deep Links

### Deep Link Configuration

**AndroidManifest.xml**:
```xml
<activity android:name=".ui.main.MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link: fastmediasorter://open -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="fastmediasorter"
            android:host="open" />
    </intent-filter>
</activity>

<activity android:name=".ui.browse.BrowseActivity">
    <!-- Deep link: fastmediasorter://browse?resourceId=42 -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="fastmediasorter"
            android:host="browse" />
    </intent-filter>
</activity>

<activity android:name=".ui.player.PlayerActivity">
    <!-- Deep link: fastmediasorter://player?resourceId=42&fileIndex=5 -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="fastmediasorter"
            android:host="player" />
    </intent-filter>
</activity>
```

---

### Deep Link Handling

**MainActivity.onCreate()**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Handle deep link
    intent?.data?.let { uri ->
        handleDeepLink(uri)
    }
}

private fun handleDeepLink(uri: Uri) {
    when (uri.host) {
        "open" -> {
            // Already in MainActivity, do nothing
        }
        "browse" -> {
            val resourceId = uri.getQueryParameter("resourceId")?.toLongOrNull()
            if (resourceId != null) {
                navigateToBrowse(resourceId)
            }
        }
        "player" -> {
            val resourceId = uri.getQueryParameter("resourceId")?.toLongOrNull()
            val fileIndex = uri.getQueryParameter("fileIndex")?.toIntOrNull()
            if (resourceId != null && fileIndex != null) {
                navigateToPlayer(resourceId, fileIndex)
            }
        }
    }
}
```

---

### Deep Link Examples

| URL | Description | Result |
|-----|-------------|--------|
| `fastmediasorter://open` | Open app (MainActivity) | Launch app |
| `fastmediasorter://browse?resourceId=42` | Open specific resource | MainActivity → BrowseActivity (resource 42) |
| `fastmediasorter://player?resourceId=42&fileIndex=5` | Open specific file | MainActivity → BrowseActivity → PlayerActivity (file 5) |

**Test Deep Link (ADB)**:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "fastmediasorter://player?resourceId=42&fileIndex=5" com.apemax.fastmediasorter
```

---

### Web Links (App Links)

**Optional**: Associate website domain with app.

**File**: `https://fastmediasorter.com/.well-known/assetlinks.json`
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.apemax.fastmediasorter",
    "sha256_cert_fingerprints": ["AB:CD:EF:..."]
  }
}]
```

**AndroidManifest.xml**:
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="https"
        android:host="fastmediasorter.com"
        android:pathPrefix="/open" />
</intent-filter>
```

**Benefit**: Clicking `https://fastmediasorter.com/open/resource/42` opens app directly (no browser chooser).

---

## 6. Back Stack Management

### Default Back Stack

```
MainActivity → BrowseActivity → PlayerActivity
     ^              ^                 |
     |              |                 |
     +──────────────+─────────────────+
                 (Back button)
```

---

### Clear Top Pattern

**Scenario**: User in PlayerActivity, wants to return to MainActivity.

```kotlin
// ❌ BAD: Creates duplicate MainActivity
val intent = Intent(this, MainActivity::class.java)
startActivity(intent)

// ✅ GOOD: Clears intermediate activities
val intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}
startActivity(intent)
finish()
```

**Result**:
```
Before: MainActivity → BrowseActivity → PlayerActivity
After:  MainActivity (existing instance reused)
```

---

### Single Top Pattern

**Scenario**: Prevent duplicate MainActivity on back button.

```xml
<activity
    android:name=".ui.main.MainActivity"
    android:launchMode="singleTop" />
```

**Effect**: If MainActivity already at top, reuse instance instead of creating new one.

---

### Task Affinity (Advanced)

**Scenario**: Separate task for media playback.

```xml
<activity
    android:name=".ui.player.PlayerActivity"
    android:taskAffinity=".player"
    android:excludeFromRecents="true" />
```

**Effect**: PlayerActivity runs in separate task (appears as separate app in Recents).

---

## 7. State Restoration

### Save State (PlayerActivity)

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(STATE_CURRENT_INDEX, viewPager.currentItem)
    outState.putLong(STATE_PLAYBACK_POSITION, player.currentPosition)
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val currentIndex = savedInstanceState?.getInt(STATE_CURRENT_INDEX) ?: intent.getIntExtra(EXTRA_FILE_INDEX, 0)
    val playbackPosition = savedInstanceState?.getLong(STATE_PLAYBACK_POSITION) ?: 0L
    
    viewPager.setCurrentItem(currentIndex, false)
    player.seekTo(playbackPosition)
}
```

---

### Restore from Intent (Process Death)

**Problem**: Activity recreated after process death, no ViewModel state.

**Solution**: Always restore from Intent extras.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val resourceId = savedInstanceState?.getLong(STATE_RESOURCE_ID)
        ?: intent.getLongExtra(EXTRA_RESOURCE_ID, -1L)
    
    if (resourceId == -1L) {
        finish() // Invalid state
        return
    }
    
    viewModel.loadResource(resourceId)
}
```

---

### ViewModel SavedStateHandle

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel() {
    
    private val resourceId: Long = savedStateHandle["resource_id"] ?: -1L
    
    init {
        if (resourceId != -1L) {
            loadFiles()
        }
    }
    
    private fun loadFiles() {
        viewModelScope.launch {
            val resource = repository.getResourceById(resourceId)
            // ...
        }
    }
}
```

**Activity**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val resourceId = intent.getLongExtra(EXTRA_RESOURCE_ID, -1L)
    viewModel.handle.set("resource_id", resourceId) // Save to handle
}
```

---

## Navigation Testing

### UI Test: Navigation Flow

```kotlin
@Test
fun navigateToPlayer() {
    // Start MainActivity
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    
    // Click first resource
    onView(withId(R.id.recycler_resources))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    
    // Verify BrowseActivity opened
    onView(withId(R.id.recycler_files)).check(matches(isDisplayed()))
    
    // Click first file
    onView(withId(R.id.recycler_files))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    
    // Verify PlayerActivity opened
    onView(withId(R.id.viewpager_player)).check(matches(isDisplayed()))
}
```

---

### Test Deep Link

```kotlin
@Test
fun deepLinkToPlayer() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("fastmediasorter://player?resourceId=1&fileIndex=0")
    }
    
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    
    // Verify navigation occurred
    onView(withId(R.id.viewpager_player)).check(matches(isDisplayed()))
}
```

---

## Navigation Diagram (Mermaid)

**See**: [diagrams/navigation_mermaid.md](diagrams/navigation_mermaid.md) for interactive Mermaid flowchart.

---

## Reference Files

### Source Code
- **MainActivity**: `ui/main/MainActivity.kt`
- **BrowseActivity**: `ui/browse/BrowseActivity.kt`
- **PlayerActivity**: `ui/player/PlayerActivity.kt`
- **Deep Link Handler**: `ui/main/DeepLinkHandler.kt`

### Related Documents
- [28. State Management Strategy](28_state_management.md) - SavedStateHandle usage
- [30. Testing Strategy](30_testing_strategy.md) - Navigation UI tests

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
