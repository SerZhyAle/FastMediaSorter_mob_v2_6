# 0. Overview & Architecture

## Activity Flow Diagram

```
WelcomeActivity (first launch only)
    ↓
MainActivity
    ├→ AddResourceActivity
    │   ├→ GoogleDriveFolderPickerActivity
    │   ├→ OneDriveFolderPickerActivity
    │   └→ DropboxFolderPickerActivity
    ├→ EditResourceActivity
    │   └→ ColorPickerDialog
    ├→ BrowseActivity
    │   ├→ PlayerActivity
    │   └→ Dialogs: Copy/Move/Delete/Rename/FileInfo/Filter/Sort
    ├→ PlayerActivity (random slideshow)
    │   └→ Dialogs: PlayerSettings/ImageEdit/GifEditor/Copy/Move/Delete/Rename/FileInfo
    └→ SettingsActivity
        └→ 4 Fragments (General/Media/Playback/Destinations)

Widget Flow (separate from main app):
    ResourceLaunchWidgetConfigActivity (launcher → widget setup)
        ↓ (selects resource)
    Widget on home screen → taps widget → BrowseActivity

Note: All activities extend BaseActivity<VB> which provides ViewBinding, locale support,
screen awake management, configuration change handling, and touch event logging.
```

---

## Activity Launch Modes

- **WelcomeActivity:** `singleTop` (launch once per install)
- **MainActivity:** `singleTask` (single instance, clear top)
- **BrowseActivity:** `standard` (multiple instances per resource)
- **PlayerActivity:** `standard` (multiple instances possible)
- **SettingsActivity:** `singleTop` (single instance)
- **AddResourceActivity:** `standard`
- **EditResourceActivity:** `standard`
- **Cloud Pickers:** `standard` (return result for folder selection)
- **ResourceLaunchWidgetConfigActivity:** `standard` (widget configuration, return result)
- **BaseActivity:** Abstract base class (not launched directly)

---

## Intent Extras Conventions

### MainActivity → BrowseActivity
```kotlin
EXTRA_RESOURCE_ID: Long // Resource database ID
```

### BrowseActivity → PlayerActivity
```kotlin
EXTRA_MEDIA_FILES: ArrayList<MediaFile> // All files in folder
EXTRA_INITIAL_POSITION: Int // Index to start at
EXTRA_RESOURCE: MediaResource // Current resource context
```

### PlayerActivity → BrowseActivity (result)
```kotlin
EXTRA_MODIFIED_FILES: ArrayList<String> // Paths of deleted/moved files
```

### AddResourceActivity → Cloud Pickers
```kotlin
// No extras needed
```

### Cloud Pickers → AddResourceActivity (result)
```kotlin
EXTRA_FOLDER_ID: String // Cloud folder ID
EXTRA_FOLDER_NAME: String // Cloud folder name
```

### Launcher → ResourceLaunchWidgetConfigActivity
```kotlin
AppWidgetManager.EXTRA_APPWIDGET_ID: Int // Widget instance ID
```

### ResourceLaunchWidgetConfigActivity → Launcher (result)
```kotlin
AppWidgetManager.EXTRA_APPWIDGET_ID: Int // Widget instance ID
// Result code: RESULT_OK or RESULT_CANCELED
// Configuration saved to SharedPreferences: "widget_prefs"
//   - resource_id_[appWidgetId]: Long
//   - resource_name_[appWidgetId]: String
```

---

## State Preservation

### Activities with SavedStateHandle
- `MainActivity`: Resource list scroll position, selected tab
- `BrowseActivity`: File list scroll position, filter/sort state, selected files
- `PlayerActivity`: Current file index, playback position (video/audio)
- `SettingsActivity`: Current tab

### Activities Without State Preservation
- `WelcomeActivity`: Always restart from page 0
- `AddResourceActivity`: Discard unsaved changes
- `EditResourceActivity`: Discard unsaved changes
- Cloud Pickers: Restart from root folder
- `ResourceLaunchWidgetConfigActivity`: No state needed (single action flow, uses Compose)
- `BaseActivity`: Abstract class (no state)

---

## Configuration Change Handling

All activities extend `BaseActivity` which properly handles:
- **Rotation:** Preserve ViewModels, recreate views
- **Language Change:** Recreate all activities with new locale
- **Theme Change:** Recreate all activities with new theme
- **Multi-window/Split-screen:** Adjust layouts, pause background tasks

Special handling:
- **PlayerActivity:** Pause video on rotation, preserve playback position
- **BrowseActivity:** Cancel file loading jobs, restart with same filters
- **Cloud activities:** Do NOT refresh token on rotation (check isChangingConfigurations)
