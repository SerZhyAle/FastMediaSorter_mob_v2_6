# 0. Overview & Architecture

## Activity Flow Diagram

```
WelcomeActivity (first launch only)
    ↓
MainActivity
    ├→ AddResourceActivity
    │   ├→ GoogleDriveFolderPickerActivity ✅
    │   ├→ OneDriveFolderPickerActivity ✅
    │   └→ DropboxFolderPickerActivity ✅
    ├→ EditResourceActivity ✅
    │   └→ ColorPickerDialog ✅
    ├→ BrowseActivity ✅
    │   ├→ PlayerActivity ✅
    │   └→ Dialogs: Copy/Move/Delete/Rename/FileInfo/Filter/Sort ✅
    ├→ FavoritesActivity ✅
    ├→ SearchActivity ✅
    ├→ PlayerActivity (random slideshow) ✅
    │   └→ Dialogs: PlayerSettings/ImageEdit/GifEditor/TextEditor/Lyrics/PdfTools/OcrTranslation ✅
    └→ SettingsActivity ✅
        └→ 5 Fragments (General/Playback/Images/Video/Audio/Documents/Destinations/Network) ✅

Widget Flow (separate from main app):
    ResourceLaunchWidgetConfigActivity ✅ (launcher → widget setup)
        ↓ (selects resource)
    Widget on home screen → taps widget → BrowseActivity ✅
    
    FavoritesWidget ✅ (home screen → shows favorites list → PlayerActivity)
    ContinueReadingWidget ✅ (home screen → launches slideshow)

Note: All activities extend BaseActivity<VB> which provides ViewBinding, locale support,
screen awake management, configuration change handling, and touch event logging.
```

---

## ⚠️ Remaining Implementation Items (Deferred/Low Priority)

### 1. AddResourceActivity Enhancements (Step 4 - Partial)
- [ ] PIN code protection for resources (6-digit validation)
- [ ] Media type checkboxes grid (IMAGE/VIDEO/AUDIO/GIF/TEXT/PDF/EPUB filter)
- [ ] Batch resource adding with RecyclerView
- [ ] Preselected tab routing (EXTRA_PRESELECTED_TAB)

### 2. EPUB Advanced Features (Step 9 - Deferred)
**Status**: Basic WebView-based EPUB viewing works. Advanced features require epub4j library.
- [ ] Chapter navigation (Previous/Next chapter buttons)
- [ ] Table of Contents dialog with chapter list
- [ ] Font size control slider (6-144px)
- [ ] Font family selection (Serif/Sans/Mono)
- [ ] Chapter position save/restore
- [ ] Cross-chapter full-text search

### 3. In-Document Search (Step 10 - Deferred)
**Status**: Search buttons wired but functionality not implemented. Requires PDF text extraction.
- [ ] PDF text extraction and search
- [ ] EPUB cross-chapter search
- [ ] Text file search with highlighting
- [ ] Search result highlighting (yellow background)
- [ ] Prev/Next navigation between results
- [ ] Search result counter (e.g., "3 of 15")

### 4. Text File Syntax Highlighting (Step 13 - Partial)
- [ ] Syntax highlighting for common file types (JSON, XML, Kotlin, Java, Python, etc.)

### 5. Widget Enhancements (Step 17 - Partial)
**Status**: Core widget functionality complete.
- [ ] Widget configuration activity for Favorites
- [ ] Widget update logic when favorites change (requires FavoritesDao integration)
- [ ] Widget preview images for home screen picker

### 6. Audiobook Mode Enhancements (Step 18 - Partial)
**Status**: Core audiobook features complete (position save/restore, speed control).
- [ ] Sleep timer for audiobooks
- [ ] Audiobook-specific command panel layout
- [ ] Bookmarks functionality

### 7. Album Art Enhancements (Step 19 - Partial)
**Status**: Core album art features complete (iTunes API, embedded ID3, fallback generation).
- [ ] Manual cover art search dialog
- [ ] Display cover art in audio player UI
- [ ] Display cover art in browse grid thumbnails
- [ ] Cover art download progress indicator

### 8. WelcomeActivity Enhancements (Step 20 - Partial)
**Status**: Version display complete.
- [ ] Visual resource types grid on Page 2 (icons for Local/SMB/SFTP/FTP/Cloud)
- [ ] Interactive touch zones demonstration on Page 3 (show 3x3 grid with actions)
- [ ] Enhanced Page 4 with destination setup instructions (visual guide)
- [ ] Animated transitions between pages
- [ ] "Don't show again" checkbox
- [ ] Landscape orientation layouts

### 9. Cloud Integration Polish (Epic 5 - Requires API Keys)
**Status**: OAuth flows and folder pickers complete. Needs API key configuration.
- [ ] Configure API keys in Google/Microsoft/Dropbox developer consoles
- [ ] Test cloud file operations end-to-end
- [ ] Add cloud-specific error handling

### 10. Release Engineering (Epic 8 - In Progress)
**Status**: Production hardening complete (R8, ProGuard, signing).
- [ ] Capture Play Store screenshots (8 required)
- [ ] Test signed APK on multiple devices
- [ ] Host Privacy Policy HTML files online

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
