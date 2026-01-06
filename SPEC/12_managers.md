# 12. Managers

## 1. PlayerActivity Managers

Extracted to decompose the complex `PlayerActivity`.

### Core Playback

- **`VideoPlayerManager`**: Encapsulates `ExoPlayer`, handles video/audio/network streams.
- **`MediaDisplayCoordinator`**: Manages the central viewing area (SurfaceView, ImageView, PdfView switch).
- **`SlideshowController`**: Handles the _logic_ of auto-advancing (timer, shuffle, loop).
- **`SlideshowManager`**: (Deprecated/Legacy) Older implementation, mostly superseded by Controller. _Note: Check if should be removed._

### UI & Interaction

- **`CommandPanelController`**: Top/Bottom bars, auto-hide logic.
- **`PlayerDialogHelper`**: All dialogs (Rename, Properties, Settings).
- **`PlayerGestureHelper`**: Touch zone logic (Left/Right tap, swipes).
- **`PlayerKeyboardHandler`**: D-Pad and hardware key mapping.
- **`ImageLoadingManager`**: Glide wrapper for high-performance image loading including encryption support.

### Feature Specific

- **`PdfViewerManager`**: Native PDF rendering.
- **`TextViewerManager`**: Plain text/code viewing.
- **`TranslationManager`**: OCR and ML Kit translation overlays.
- **`UndoOperationManager`**: Local history stack for undoing specific operations.
- **`FileOperationsHandler`**: Executes the actual Move/Copy/Delete logic and updates UI.

---

## 2. BrowseActivity Managers

Delegates for `BrowseActivity` to keep it clean.

### `BrowseDialogHelper`

- **Role:** Dialog factory.
- **Key Dialogs:** Filter (Custom complex dialog), Sort, Rename Multiple.

### `BrowseCloudAuthManager`

- **Role:** Handles OAuth flows for Google Drive, OneDrive, Dropbox.
- **Logic:** Launches generic auth Intents and handles results.

### `BrowseFileOperationsManager`

- **Role:** Execution of Copy/Move/Delete/Rename.
- **Logic:** Interfaces with `FileOperationUseCase` and updates the list via ViewModel.

### `BrowseRecyclerViewManager`

- **Role:** Manages the `RecyclerView` adapter, layout manager (Grid/List switching), and scroll position saving/restoring.

### `BrowseSmallControlsManager`

- **Role:** Handles the "Small Controls" bottom bar (Quick actions for selected files).

### `KeyboardNavigationManager`

- **Role:** Hardware keyboard navigation support for the file list.
- **Location (BrowseActivity)**: Handles Arrow keys, Enter, Delete for file browsing.
- **Location (PlayerActivity)**: Delegates to `PlayerKeyboardHandler`.

---

## 2.1 MainActivity Helpers

Located in `com.sza.fastmediasorter.ui.main.helpers`.

### `KeyboardNavigationHandler`

**Role:** Hardware keyboard support for MainActivity resource list.

**Supported Keys:**

- **Arrow Up/Down**: Navigate through resource list (moves focus in RecyclerView).
- **Enter**: Open selected resource in BrowseActivity.
- **Delete**: Show delete confirmation for selected resource.
- **Tab**: Cycle focus through action buttons (Add, Filter, Settings).
- **Ctrl+A**: Trigger Add Resource action.
- **Ctrl+F**: Trigger Filter dialog.
- **Ctrl+S**: Open Settings.
- **Escape**: Exit application (calls `finishAffinity()` + `killProcess()`).
- **Space**: Toggle resource selection (for multi-select future feature).

**Constructor Parameters:**

```kotlin
KeyboardNavigationHandler(
    context: Context,
    recyclerView: RecyclerView,
    viewModel: MainViewModel,
    onDeleteConfirmation: (MediaResource) -> Unit,
    onAddResourceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFilterClick: () -> Unit,
    onExit: () -> Unit
)
```

**Method:** `handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean`
- Returns `true` if key was consumed, `false` to pass to default handler.
- Integrates with RecyclerView focus management to highlight current item.

### `ResourcePasswordManager`

**Role:** Secure PIN input for password-protected resources.

**Methods:**

1. **checkResourcePassword(resource, forSlideshow, onPasswordValidated)**
   - Shows PIN input dialog for resource access.
   - Validates entered PIN against `resource.accessPin`.
   - On success: Calls `onPasswordValidated(resourceId, forSlideshow)`.
   - On failure: Shows error toast, allows retry.
   
2. **checkResourcePinForEdit(resource)**
   - Shows PIN input dialog for editing resource settings.
   - Validates entered PIN.
   - On success: Opens EditResourceActivity with `resourceId` extra.
   - On failure: Shows error toast.

**Constructor Parameters:**

```kotlin
ResourcePasswordManager(
    context: Context,
    layoutInflater: LayoutInflater
)
```

**Dialog Design:**
- Title: "Enter PIN" or "Enter password"
- Input: `EditText` with `inputType = numberPassword` (for numeric PINs) or `textPassword`.
- Buttons: OK (validates), Cancel (dismisses).
- Error feedback: Toast message "Incorrect PIN. Try again."

---

## 3. Global Helpers

Shared across activities.

### `PermissionHelper`

Wraps Android permission requests (Storage, Notifications).

### `ConnectionThrottleManager`

Limits concurrent network operations to prevent connection pool exhaustion on SMB/FTP.
