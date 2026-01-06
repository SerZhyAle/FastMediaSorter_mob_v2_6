# 04a. PlayerActivity Logic

## Overview

`PlayerActivity` is the unified media consumption interface. It supports Videos, Audio, Images, GIFs, PDFs, eBooks (EPUB), and Text files within a single Activity, dynamically switching ViewManagers based on the content type.

## Architecture: The "Manager Ecosystem"

Due to extreme complexity (150KB+ file), the Activity delegates almost all logic to specialized components:

### 1. View Managers (Content Rendering)

- **`VideoPlayerManager`**: Wraps `ExoPlayer` for Video/Audio. Handles network streaming, buffering, and retries.
- **`ImageLoadingManager`**: Wraps `Glide` for Images/GIFs. Manages preloading and high-res ZOOM.
- **`PdfViewerManager`**: Renders PDF pages using Android's native `PdfRenderer`.
- **`EpubViewerManager`**: HTML-based renderer for eBooks.
- **`TextViewerManager`**: Simple scrolling text view.

### 2. Interaction Controllers

- **`CommandPanelController`**: Manages the Overlay UI (Play/Pause, Timeline, Toolbar buttons).
- **`SlideshowController`**: Handles auto-advance logic and countdown timers.
- **`PlayerDialogHelper`**: Centralizes all Dialogs (Rename, Edit, Settings, Delete confirm).
- **`PlayerKeyboardHandler`**: Maps hardware keys (D-Pad/Keyboard) to player actions.
- **`TouchZoneGestureManager`**: Handles tap zones (Left=Prev, Right=Next, Center=UI) vs Gestures (Swipe).

### 3. Logic Handlers

- **`FileOperationsHandler`**: Coordinates Copy/Move/Delete operations and their callbacks.
- **`UndoOperationManager`**: Manages the local undo stack for accidental deletions.
- **`NetworkFileManager`**: Handles specific network protocols quirks (e.g. Google Drive auth).

---

## ViewModel Logic (`PlayerViewModel`)

**Key Responsibilities:**

- **State Source of Truth**: Holds the list of `MediaFiles` and `currentIndex`.
- **Navigation Logic**:
  - `nextFile()` / `previousFile()`: Handles circular navigation.
  - **Looping**: Last file -> First file.
  - **Filters**: Can `skipDocuments` during slideshows.
- **Loading Strategy**:
  1. Checks **Cache** (fastest).
  2. If miss, loads via `GetMediaFilesUseCase` or `GetResourcesUseCase`.
  3. Applies Resource filters (e.g., if Resource only allows Images).
- **Undo System**: Maintains `lastOperation` state and exposes `restore...` functions.

---

## Key Feature Implementations

### 1. Robust Network Streaming (`VideoPlayerManager`)

- **Engine**: `ExoPlayer` configured with custom `LoadControl`.
- **Buffering Strategy**:
  - **SMB/SFTP/FTP**: Min Buffer 50s.
  - **Cloud**: Min Buffer 60s (Higher latency tolerance).
- **Error Recovery**:
  - Catches `EOFException` (common in unstable streams).
  - Retries playback **3 times** initiated from the last known position.
  - Fallback logic: If `ExoPlayer` fails with format error, tries native `MediaPlayer`.

### 2. Editing Capabilities

The Player is not just a viewer, but an editor:

- **Images**:
  - **Rotate/Flip**: Physical file transformation (via `RotateImageUseCase`).
  - **Filters**: GPU-based filters (Grayscale, Sepia, etc.).
  - **Adjustments**: Brightness/Contrast modification.
- **GIFs**:
  - **Speed**: Change frame delays (via `ChangeGifSpeedUseCase`).
  - **Frame Extract**: Save frames as individual images.

### 3. Google Drive Integration

- **Auth Flow**: checks `isAuthenticated()` on load.
- **Lazy Auth**: If token invalid, triggers `CloudAuthRequired` event.
- **Activity Result**: `PlayerActivity` launches generic `GoogleSignIn` intent via `BrowseCloudAuthManager`.

### 4. Slideshow Logic

- **Controller**: `SlideshowController`.
- **Interval**: Configurable per-Resource (takes precedence) or Global.
- **Logic**:
  - Video/Audio: Waits for `STATE_ENDED`.
  - Image: Timer based (3s, 5s, etc.).
  - **Play to End**: Setting to disable timeout for videos (play full duration).

### 5. Trash & Undo

- **Delete Action**:
  - **Local**: Soft moves to `.trash` folder.
  - **Network**: Hard delete (Protocol limitation).
- **Undo**:
  - Reverses the Move operation.
  - Updates list and restores file at index.
