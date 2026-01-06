# 6. Cloud Pickers

## GoogleDriveFolderPickerActivity
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** Navigate Google Drive folder hierarchy  
**ViewModel:** `GoogleDriveFolderPickerViewModel`  
**Layout:** `activity_google_drive_folder_picker.xml`

**UI Elements:**

**Toolbar:**
- `toolbar` - MaterialToolbar
  - Title: "Select Google Drive Folder"
  - Navigation icon: ic_arrow_back
  - Background: colorPrimary

**SwipeRefreshLayout:**
- `swipeRefresh` - SwipeRefreshLayout
  - Wraps all content below toolbar
  - Pull-to-refresh gesture
  - Action: Reload folder contents

**Loading State:**
- `progressBar` - ProgressBar (Circular)
  - Centered on screen
  - Visibility: gone (shown during loading)
  - ContentDescription: "Loading"

**Empty State:**
- `tvEmptyState` - TextView
  - Text: "No folders found"
  - Centered on screen
  - Visibility: gone (shown when folder is empty)
  - TextAppearance: bodyLarge
  - Padding: 32dp

**Configuration Options:**
- `llCheckboxes` - LinearLayout (Horizontal)
  - Background: colorSurface
  - Padding: 16dp horizontal, 8dp vertical
  - Position: Top of screen (below toolbar)
  - Contains:
    - `cbAddAsDestination` - MaterialCheckBox
      - Text: "Add as destination"
      - MarginEnd: 16dp
    - `cbScanSubdirectories` - MaterialCheckBox
      - Text: "Scan subdirectories"
      - Checked by default

**Main Content:**
- `rvFolders` - RecyclerView
  - LayoutManager: LinearLayoutManager
  - ClipToPadding: false
  - PaddingBottom: 16dp
  - Shows folders in current directory
  - Adapter: GoogleDriveFolderAdapter
  - Each item:
    - Folder icon (ic_folder)
    - Folder name (TextView)
    - Folder ID (TextView, small gray text)
    - Navigate button (arrow icon) → Go into folder
    - Select button → Choose this folder

**Features:**
- Breadcrumb navigation (My Drive → subfolder1 → subfolder2)
- Folder list with icons
- Select folder button (returns folder ID)
- Back navigation (folder stack)
- OAuth re-authentication if token expired
- Pull-to-refresh to reload folder contents
- "Add as destination" checkbox
- "Scan subdirectories" checkbox (checked by default)
- Empty state message when no folders found
- Loading indicator during API calls

**Functionality:**
- **OAuth Authentication:**
  - Checks for valid access token on launch
  - Re-authenticates if token expired (401 Unauthorized)
  - Stores refresh token in EncryptedSharedPreferences
  - Handles OAuth errors: Shows error dialog with retry
- **Folder Navigation:**
  - ViewModel maintains stack of visited folders
  - Back button pops stack to previous folder
  - Navigate button: Pushes folder to stack, loads children
  - Root folder: "My Drive" (ID: "root" for Google Drive)
- **Folder Loading:**
  - API call: List folders only (no files)
  - Pagination: 100 folders per page (for large directories)
  - Sorting: Alphabetical by folder name
  - Caching: Stores folder list in memory for back navigation
- **Folder Selection:**
  - Select button: Returns folder ID to AddResourceActivity
  - Result: Folder ID + Folder Name + Checkboxes state
  - Intent extras: EXTRA_FOLDER_ID, EXTRA_FOLDER_NAME, EXTRA_IS_DESTINATION, EXTRA_SCAN_SUBDIRECTORIES
- **Checkbox State:**
  - "Add as destination": Passed back to AddResourceActivity
  - "Scan subdirectories": Determines recursive scanning
  - State preserved on back navigation
- **Pull-to-Refresh:**
  - Swipe down gesture triggers refresh
  - Reloads current folder contents
  - Shows progress indicator during refresh
- **Error Handling:**
  - Network error: Shows retry button
  - Token expired: Re-authenticates automatically
  - Empty folder: Shows "No folders found" message
  - API rate limit: Shows error dialog with retry delay
- **Provider-Specific Differences:**
  - Google Drive: Uses Google Drive API v3, folder MIME type
  - OneDrive: Uses Microsoft Graph API, driveItem filter
  - Dropbox: Uses Dropbox API v2, list_folder endpoint
- **Analytics:**
  - Tracks folder selection frequency
  - Monitors API call latency
  - Reports token refresh rate

**Flow:** Launched from AddResourceActivity → Returns folder ID & name

---

## OneDriveFolderPickerActivity
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** Navigate OneDrive folder hierarchy  
**ViewModel:** `OneDriveFolderPickerViewModel`  
**Layout:** `activity_onedrive_folder_picker.xml`

**UI Elements:**
- `toolbar` - MaterialToolbar: Title "Select OneDrive Folder"
- `swipeRefresh` - SwipeRefreshLayout: Pull-to-refresh
- `progressBar` - ProgressBar: Loading indicator
- `tvEmptyState` - TextView: "No folders found"
- `llCheckboxes` - LinearLayout with:
  - `cbAddAsDestination` - MaterialCheckBox
  - `cbScanSubdirectories` - MaterialCheckBox (checked by default)
- `rvFolders` - RecyclerView with OneDriveFolderAdapter

**Features:** Identical to GoogleDriveFolderPickerActivity but for OneDrive REST API (Microsoft Graph)

**Functionality:** Same as GoogleDriveFolderPickerActivity (see above), with OneDrive-specific API endpoints and authentication via MSAL library

---

## DropboxFolderPickerActivity
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** Navigate Dropbox folder hierarchy  
**ViewModel:** `DropboxFolderPickerViewModel`  
**Layout:** `activity_dropbox_folder_picker.xml`

**UI Elements:**
- `toolbar` - MaterialToolbar: Title "Select Dropbox Folder"
- `swipeRefresh` - SwipeRefreshLayout: Pull-to-refresh
- `progressBar` - ProgressBar: Loading indicator
- `tvEmptyState` - TextView: "No folders found"
- `llCheckboxes` - LinearLayout with:
  - `cbAddAsDestination` - MaterialCheckBox
  - `cbScanSubdirectories` - MaterialCheckBox (checked by default)
- `rvFolders` - RecyclerView with DropboxFolderAdapter

**Features:** Identical to GoogleDriveFolderPickerActivity but for Dropbox OAuth2

**Functionality:** Same as GoogleDriveFolderPickerActivity (see above), with Dropbox-specific API v2 endpoints and authentication via Dropbox SDK
