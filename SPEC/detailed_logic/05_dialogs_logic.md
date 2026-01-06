# 05. Dialog Logic & Helpers

## Overview

The application uses a **Helper Pattern** to decouple Dialog creation and logic from Activities.

- **Goals**: Reduce Activity bloat, centralize dialog logic (validation, network handling), and promote reuse.
- **Pattern**: `Activity` initializes a `Helper` (e.g., `BrowseDialogHelper`), passing a `Callback` interface. The Helper handles UI inflation, validation, and calls back with clean data (e.g., `onRenameConfirmed`).

## 1. BrowseDialogHelper

Located in `ui.browse.managers`. Focuses on list manipulation and filtering.

### Key Dialogs

- **Filter Dialog** (`DialogFilterBinding`):
  - **Custom UI**: Non-standard AlertDialog. Contains DateRange pickers, Size range inputs, and a grid of Checkboxes for MediaTypes.
  - **Logic**: Pre-fills existing filter. Validates that only `supportedMediaTypes` of the Resource are selectable.
- **Sort Dialog**: Single-choice list (Name A-Z, Size, Date, etc.).
- **Rename Multiple** (`DialogRenameMultipleBinding`):
  - **UI**: RecyclerView list of editable text fields.
  - **Logic**: Bulk rename. Validates collisions locally before executing. Returns a list of changes for Undo.
- **Delete Confirmation**: Checks `AppSettings.safeMode` before showing.

### 2. PlayerDialogHelper

Located in `ui.player`. Focuses on file operations and content editing.

### Key Dialogs

- **Copy / Move to** (`CopyToDialog`, `MoveToDialog`):
  - **Complex Logic**: These are full-screen like dialogs allowing directory navigation to select destination.
  - **Navigation**: Uses `GetDestinationsUseCase` to browse folder structures within the dialog.
- **Image Editing** (`ImageEditDialog`):
  - Wraps use cases: `Rotate`, `Flip`, `Filter`, `Adjust`.
  - **Callback**: `onImageEditComplete` triggers a reload of the image in the Player.
- **GIF Editor** (`GifEditorDialog`):
  - Capabilities: functionality to Extract frames, change playback speed.

## 3. Shared Dialog Components

These are standalone classes in `ui.dialog` used by both helpers.

- **`RenameDialog`**:
  - Handles single file rename.
  - **Network Smarts**: Aware of SMB/SFTP paths. Can rename files on remote servers if the protocol supports it.
- **`FileInfoDialog`**:
  - Displays extensive metadata: Path, Size, Modified Date, Resolution (if image/video), Codec info.
  - **Async Loading**: Fetches remote metadata on background threads for network files.

## 4. Network Path Handling (Critical Logic)

Standard Android `File` API fails with `smb://` or `sftp://` paths.
Both Helpers implement a **Proxy File Strategy** before passing files to Dialogs:

```kotlin
// Example from PlayerDialogHelper
val sourceFile = if (path.startsWith("smb://") ...) {
    object : File(path) {
        override fun getAbsolutePath() = path
        override fun getPath() = path
        // ... overrides to prevent disk access attempts
    }
} else {
    File(path)
}
```

This ensures that UI components (like a File Name TextView) display the correct logical path, while the underlying File Operations (handled by UseCases, not the Dialog itself) parse the URI scheme correctly.
