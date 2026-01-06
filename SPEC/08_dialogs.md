# 8. Dialogs

## Reusable Dialogs

### Dialog Catalog

#### 1. CopyToDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** BrowseActivity, PlayerActivity  

**Features:**
- List of all destination resources
- Search/filter destinations
- Shows free space for each destination
- Multi-file operation support
- Replace if exists option

#### 2. MoveToDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** BrowseActivity, PlayerActivity  

**Features:**
- Identical to CopyToDialog
- Additional warning for destructive move
- Undo support after move

#### 3. DeleteDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** MainActivity, BrowseActivity, PlayerActivity  

**Features:**
- Confirmation message with file count
- "Move to trash" vs "Delete permanently" options
- Checkbox: "Don't ask again for this session"
- Undo support (if moved to trash)

#### 4. RenameDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** BrowseActivity, PlayerActivity  

**Features:**
- Text input with current file name
- Extension validation (can't change for some types)
- Name conflict detection
- Suggestions (if available)

#### 5. FileInfoDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** BrowseActivity, PlayerActivity  

**Features:**
- Full file path
- File size (bytes + human-readable)
- Creation/modification dates
- Media-specific metadata:
  - Images: Resolution, EXIF data (camera, GPS, etc.)
  - Videos: Duration, codec, bitrate, FPS
  - Audio: Duration, bitrate, artist/album tags
  - Documents: Page count, author
- Copy path to clipboard button
- Open location button

#### 6. ImageEditDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** PlayerActivity  

**Features:**
- Preview with zoom
- Operations:
  - Rotate (90°/180°/270°/Free)
  - Flip (H/V)
  - Crop (freeform/ratio presets)
  - Filters (brightness, contrast, saturation, hue)
- Apply/Cancel buttons
- Save as new file option

#### 7. GifEditorDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** PlayerActivity  

**Features:**
- Frame-by-frame preview
- Extract current frame as image
- Speed multiplier (0.25x-4x)
- Reverse frames order
- Apply/Cancel buttons

#### 8. PlayerSettingsDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** PlayerActivity  

**Features:**
- Quick settings overlay (bottom sheet)
- Slideshow interval slider
- Random order toggle
- Touch zones toggle
- Video speed (for videos)
- Video repeat mode (for videos)
- Immediate apply (no OK button)

#### 9. FileOperationProgressDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** BrowseActivity, PlayerActivity  

**Features:**
- Progress bar (0-100%)
- Current file being processed
- Files completed / total count
- Transfer speed (MB/s)
- Time remaining estimate
- Cancel button
- Background operation support

#### 10. MaterialProgressDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** All activities (generic progress)  

**Features:**
- Indeterminate circular progress
- Custom message
- Cancelable/non-cancelable variants
- Material Design styling

#### 11. ColorPickerDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** EditResourceActivity, SettingsActivity  

**Features:**
- Color wheel picker
- Predefined color palette (Material colors)
- Hex color input
- Preview of selected color
- OK/Cancel buttons

#### 12. FilterResourceDialog
**Package:** `com.sza.fastmediasorter.ui.main`  
**Usage:** MainActivity  

**Features:**
- Filter by resource type (Local/SMB/SFTP/FTP/Cloud)
- Filter by media type (Images/Videos/Audio/GIFs/Documents)
- Search by name
- Show only destinations toggle
- Reset filters button

#### 13. NetworkDiscoveryDialog
**Package:** `com.sza.fastmediasorter.ui.addresource`  
**Usage:** AddResourceActivity  

**Features:**
- Scan local network for SMB shares
- Progress bar during scan
- List of discovered servers with:
  - Hostname
  - IP address
  - Available shares
- Click to auto-fill connection details
- Requires WiFi connection

#### 14. TooltipDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** All activities (onboarding/help)  

**Features:**
- Semi-transparent overlay
- Highlight specific UI element
- Explanation text
- "Got it" / "Next" buttons
- "Don't show again" checkbox

#### 15. ErrorDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** All activities  

**Features:**
- Error icon
- Error message
- Technical details (expandable)
- Copy error to clipboard
- OK button

#### 16. ErrorDialogWithAction
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Usage:** All activities  

**Features:**
- Same as ErrorDialog
- Additional action button (e.g., "Retry", "Settings")
- Action callback

#### 17. TranslationSettingsDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Layout:** `dialog_translation_settings.xml`  
**Usage:** PlayerActivity (for text/PDF/EPUB translation)  

**Features:**
- Source language selection (Spinner)
- Target language selection (Spinner)
- Font size slider (for overlay text)
- Background opacity slider
- Translation service selection (Google/DeepL/Custom)
- Apply/Cancel buttons

#### 18. AccessPasswordDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Layout:** `dialog_access_password.xml`  
**Usage:** BrowseActivity, PlayerActivity (for password-protected resources)  

**Features:**
- PIN code input field (6 digits)
- Show/hide password toggle
- Remember password checkbox
- OK/Cancel buttons
- Used for network resources and encrypted containers

#### 19. ScrollableTextDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Layout:** `dialog_scrollable_text.xml`  
**Usage:** All activities (for displaying long text content)  

**Features:**
- ScrollView with TextView
- Monospace font option
- Selectable text
- Copy to clipboard button
- Close button
- Used for: Logs, error details, file contents preview

#### 20. LogViewDialog
**Package:** `com.sza.fastmediasorter.ui.dialog`  
**Layout:** `dialog_log_view.xml`  
**Usage:** SettingsActivity, debugging  

**Features:**
- RecyclerView with log entries
- Filter by log level (Verbose/Debug/Info/Warn/Error)
- Search by text
- Copy selected log entries
- Clear logs button
- Export logs button (saves to file)
- Auto-scroll toggle

## Dialog Helpers (Non-dialog Classes)

### DialogUtils
**Package:** `com.sza.fastmediasorter.ui.common`  
**Purpose:** Static utility methods for common dialogs  

**Methods:**
- `showConfirmationDialog()`: Yes/No dialog
- `showInputDialog()`: Single text input
- `showListDialog()`: Select from list
- `showMultiChoiceDialog()`: Multiple selection
- `showDatePicker()`: Date selection
- `showTimePicker()`: Time selection

### ErrorDialogHelper
**Package:** `com.sza.fastmediasorter.ui.common`  
**Purpose:** Centralized error handling and display  

**Features:**
- Maps exception types to user-friendly messages
- Formats network errors with retry suggestions
- Logs errors to Timber
- Creates appropriate ErrorDialog/ErrorDialogWithAction

### BrowseDialogHelper
**Package:** `com.sza.fastmediasorter.ui.browse.managers`  
**Purpose:** Factory for BrowseActivity dialogs  

**Methods:**
- `showCopyToDialog()`
- `showMoveToDialog()`
- `showDeleteDialog()`
- `showRenameDialog()`
- `showFileInfoDialog()`
- `showFilterDialog()`
- `showSortDialog()`

### PlayerDialogHelper
**Package:** `com.sza.fastmediasorter.ui.player`  
**Purpose:** Factory for PlayerActivity dialogs  

**Methods:**
- `showPlayerSettingsDialog()`
- `showImageEditDialog()`
- `showGifEditorDialog()`
- `showCopyToDialog()`
- `showMoveToDialog()`
- `showDeleteDialog()`
- `showRenameDialog()`
- `showFileInfoDialog()`
