# 3. Resource Management

## AddResourceActivity
**Package:** `com.sza.fastmediasorter.ui.addresource`  
**Purpose:** Add new media sources (supports Local/SMB/SFTP/FTP/Cloud)  
**ViewModel:** `AddResourceViewModel`  
**Layout:** `activity_add_resource.xml`  
**Helper:** `AddResourceHelper` (copy mode pre-fill logic)

**Activity Lifecycle & Initialization:**

**onCreate Behavior:**
1. **Copy Mode Detection:**
   - Reads `EXTRA_COPY_RESOURCE_ID` extra (Long, -1L if absent)
   - If valid: Sets `copyResourceId`, updates toolbar title to R.string.copy_resource_title
   - If invalid: Sets toolbar title to R.string.add_resource_title
   - Copy load triggered in `observeData()` after event subscriptions to avoid race conditions

2. **Preselected Tab Routing:**
   - Reads `EXTRA_PRESELECTED_TAB` extra (String, ResourceTab enum name)
   - Converts to `ResourceTab` enum via `valueOf()`, catches `IllegalArgumentException` for invalid values
   - Defers UI updates until after `setupViews()` completes using `binding.root.post { }`
   - Routes to section based on tab:
     - `LOCAL` → `showLocalFolderOptions()`
     - `SMB` → `showSmbFolderOptions()`
     - `FTP_SFTP` → `showSftpFolderOptions()`
     - `CLOUD` → `showCloudStorageOptions()`
     - `ALL`, `FAVORITES` → Shows all options (default)

**onResume Behavior:**
1. **Dropbox Authentication Completion:**
   - Checks `isDropboxAuthenticated` flag (set to true when OAuth started)
   - Calls `dropboxClient.finishAuthentication()` coroutine
   - Handles `AuthResult` variants:
     - `Success` → Shows toast "Signed in as {email}", navigates to `DropboxFolderPickerActivity`
     - `Error` → Shows toast with error message
     - `Cancelled` → Shows R.string.msg_dropbox_auth_cancelled toast
   - Resets `isDropboxAuthenticated = false` after handling

2. **OneDrive Authentication Completion:**
   - Checks `isOneDriveAuthenticated` flag (set to true when OAuth started)
   - Calls `oneDriveClient.testConnection()` coroutine
   - Handles `CloudResult` variants:
     - `Success` → Shows R.string.msg_onedrive_auth_success, navigates to `OneDriveFolderPickerActivity`
     - `Error` → Shows toast with error message
   - Resets `isOneDriveAuthenticated = false` after handling

**Configuration Changes:**
- `updateResourceTypeGridColumns()` called in `setupViews()`
- Landscape mode: Sets `layoutResourceTypes.columnCount = 2`
- Portrait mode: Sets `layoutResourceTypes.columnCount = 1`
- Uses `resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE`

**Navigation & Action Buttons:**

**Toolbar Navigation:**
- `toolbar.setNavigationOnClickListener` → `finish()`
- Logs: UserActionLogger.logButtonClick("ToolbarBack", "AddResource")

**Resource Type Selection Cards (4 cards):**

**cardLocalFolder Click:**
- Logs: UserActionLogger.logButtonClick("LocalFolderCard", "AddResource")
- Calls: `showLocalFolderOptions()`
- Effect: Hides `layoutResourceTypes`, shows `layoutLocalFolder`, updates title to R.string.add_local_folder

**cardNetworkFolder Click:**
- Logs: UserActionLogger.logButtonClick("NetworkFolderCard", "AddResource")
- Calls: `showSmbFolderOptions()`
- Effect: Hides `layoutResourceTypes`, shows `layoutSmbFolder`, updates title to R.string.add_network_folder
- Initializes media type checkboxes from `viewModel.getSupportedMediaTypes()` (coroutine)
- Sets checkbox `isChecked` and `isVisible` for each MediaType (IMAGE/VIDEO/AUDIO/GIF/TEXT/PDF/EPUB)

**cardSftpFolder Click:**
- Logs: UserActionLogger.logButtonClick("SftpFolderCard", "AddResource")
- Calls: `showSftpFolderOptions()`
- Effect: Hides layouts, shows `layoutSftpFolder`, updates title to R.string.add_sftp_ftp_title
- Sets default port: If `etSftpPort` blank → sets "22"
- Checks `rbSftp` radio button (SFTP selected by default)
- Initializes media type checkboxes (same as SMB)

**cardCloudStorage Click:**
- Logs: UserActionLogger.logButtonClick("CloudStorageCard", "AddResource")
- Calls: `showCloudStorageOptions()`
- Effect: Hides layouts, shows `layoutCloudStorage`, updates title to R.string.cloud_storage
- Triggers: `updateCloudStorageStatus()` (checks Google/Dropbox/OneDrive auth states)

**UI Elements:**

**Root Container:**
- ConstraintLayout (match_parent × match_parent)
  - Background: default theme background
  - Contains toolbar and all form sections

**Toolbar:**
- `toolbar` - MaterialToolbar
  - Width: match_parent
  - Height: ?attr/actionBarSize
  - Background: ?attr/colorPrimary
  - Elevation: 4dp
  - Title: "Add Resource"
  - NavigationIcon: @drawable/ic_arrow_back
  - NavigationIconTint: ?attr/colorOnPrimary
  - TitleTextColor: ?attr/colorOnPrimary

**Title Section:**
- `tvTitle` - TextView
  - Width: wrap_content
  - Height: wrap_content
  - Text: @string/select_resource_type ("Select resource type")
  - TextSize: 18sp
  - TextStyle: bold
  - Margin: 16dp all sides

**Resource Type Selection Cards:**
- `layoutResourceTypes` - GridLayout
  - Width: 0dp (constrained start→end)
  - Height: wrap_content
  - ColumnCount: 1
  - Padding: 16dp
  - UseDefaultMargins: true
  - Contains: 4 MaterialCardView items

**Card 1: Local Folder**
- `cardLocalFolder` - MaterialCardView
  - Width: 0dp (layout_columnWeight=1)
  - Height: wrap_content
  - CornerRadius: 8dp
  - Elevation: 2dp
  - Clickable: true, Focusable: true
  - Foreground: ?attr/selectableItemBackground
  - Contains:
    - LinearLayout (horizontal, padding 16dp)
      - ImageView: 32×32dp, ic_resource_local icon
      - LinearLayout (vertical, layout_weight=1):
        - TextView: "Local folder" (16sp, bold)
        - TextView: "Add folders from device storage" (14sp, secondary color)

**Card 2-4:** Similar structure for Network, SFTP, Cloud

**1.  Local Folder Section** (`layoutLocalFolder` - LinearLayout):
- Width: 0dp (constrained start→end)
- Height: 0dp (constrained top→bottom)
- Orientation: vertical
- Padding: 16dp
- Visibility: gone (shown when Local selected)
- Contains:

  **Action Buttons Row:**
  - LinearLayout (horizontal):
    - `btnScan` - MaterialButton
      - Width: 0dp (layout_weight=1)
      - Height: wrap_content
      - Text: @string/scan_button ("SCAN")
      - Icon: @android:drawable/ic_menu_search
      - Style: Widget.MaterialComponents.Button.Icon
    - `btnAddManually` - MaterialButton
      - Width: 0dp (layout_weight=1)
      - Height: wrap_content
      - MarginStart: 8dp
      - Text: @string/add_manually_button ("Add manually")
      - Icon: @android:drawable/ic_input_add

  **Resources List:**
  - `tvResourcesToAdd` - TextView
    - Text: @string/resources_to_add ("Resources to add")
    - TextSize: 16sp, TextStyle: bold
    - MarginTop: 8dp
    - Visibility: gone (shown after scan/add)
  
  - `rvResourcesToAdd` - RecyclerView
    - Width: match_parent
    - Height: 0dp (layout_weight=1)
    - MarginTop: 4dp
    - LayoutManager: LinearLayoutManager
    - Visibility: gone
    - ListItem: @layout/item_resource_to_add

  **Options Row:**
  - `layoutLocalOptions` - LinearLayout
    - Orientation: horizontal
    - Visibility: gone
    - Contains:
      - `cbLocalScanSubdirectories` - MaterialCheckBox
        - Width: 0dp (layout_weight=1)
        - Checked: true (default)
        - Text: @string/scan_subdirectories ("Scan subdirectories")
      - `cbLocalWorkWithAllFiles` - MaterialCheckBox
        - Width: 0dp (layout_weight=1)
        - Checked: false (default, follows global setting)
        - Text: @string/work_with_all_files ("All files *.*")
      - `cbLocalAddToDestinations` - MaterialCheckBox
        - Width: 0dp (layout_weight=1)
        - Checked: false
        - Text: @string/add_to_destinations ("Add to destinations")

  **Read-Only Mode:**
  - `cbLocalReadOnlyMode` - MaterialCheckBox
    - Width: wrap_content
    - Height: wrap_content
    - MarginTop: 4dp
    - Checked: false
    - Text: @string/read_only_mode ("Read-only mode")
    - Visibility: gone

  **PIN Code Field:**
  - `tilLocalPinCode` - TextInputLayout
    - Width: 120dp
    - Height: wrap_content
    - Hint: @string/resource_pin_code ("PIN Code")
    - HelperText: @string/resource_pin_code_hint ("Optional 6-digit code")
    - EndIconMode: password_toggle
    - MarginTop: 4dp
    - Visibility: gone
    - Contains:
      - `etLocalPinCode` - TextInputEditText
        - InputType: numberPassword
        - MaxLength: 6
        - MaxLines: 1

  **Add Button:**
  - `btnAddToResources` - MaterialButton
    - Width: match_parent
    - Height: wrap_content
    - Text: @string/add_to_resources_button ("Add to resources")
    - Visibility: gone
    - MarginTop: 8dp

**2. SMB Network Section** (`layoutSmbFolder` - ScrollView):
- Width: 0dp (constrained start→end)
- Height: 0dp (constrained top→bottom)
- Padding: 16dp
- Visibility: gone (shown when Network selected)
- Contains LinearLayout (vertical) with:

  **Network Scan + Server IP Row:**
  - LinearLayout (horizontal):
    - `btnScanNetwork` - MaterialButton
      - Width: wrap_content
      - Height: wrap_content
      - Text: @string/scan_network_button ("Scan Network")
      - Style: Widget.MaterialComponents.Button.OutlinedButton
      - MarginEnd: 8dp
    
    - `tilSmbServer` - TextInputLayout
      - Width: 180dp
      - Height: wrap_content
      - Hint: @string/smb_server ("Server IP")
      - HelperText: @string/smb_server_hint ("e.g., 192.168.1.100")
      - Contains:
        - `etSmbServer` - IpAddressEditText
          - Custom widget for IP input with auto-formatting

  **Username + Password Row:**
  - LinearLayout (horizontal):
    - `tilSmbUsername` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/smb_username ("Username")
      - Contains:
        - `etSmbUsername` - TextInputEditText
          - InputType: text
          - MaxLines: 1
    
    - `tilSmbPassword` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginStart: 4dp
      - Hint: @string/smb_password ("Password")
      - EndIconMode: password_toggle
      - Contains:
        - `etSmbPassword` - TextInputEditText
          - InputType: textPassword
          - MaxLines: 1

  **Test Connection:**
  - `btnSmbTest` - MaterialButton
    - Width: match_parent
    - Height: wrap_content
    - Text: @string/smb_test_connection ("Test Connection")
    - Icon: @android:drawable/ic_menu_info_details
    - Style: Widget.MaterialComponents.Button.Icon
    - MarginBottom: 8dp

  **Share Name + Resource Name Row:**
  - LinearLayout (horizontal):
    - `tilSmbShareName` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/smb_share_name ("Share Name")
      - HelperText: @string/smb_share_name_hint ("e.g., Media")
      - Contains: etSmbShareName
    
    - `tilSmbResourceName` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginStart: 4dp
      - Hint: @string/resource_name ("Resource Name")
      - HelperText: @string/resource_name_hint ("Display name")
      - Contains: etSmbResourceName

  **Comment + PIN Code Row:**
  - LinearLayout (horizontal):
    - `tilSmbComment` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/resource_comment ("Comment")
      - Contains: etSmbComment
    
    - `tilSmbPinCode` - TextInputLayout
      - Width: 240dp
      - MarginStart: 4dp
      - Hint: @string/resource_pin_code ("PIN Code")
      - EndIconMode: password_toggle
      - Contains:
        - `etSmbPinCode` - TextInputEditText
          - InputType: numberPassword
          - MaxLength: 6

  **Checkboxes Row 1:**
  - LinearLayout (horizontal):
    - `cbSmbAddToDestinations` - MaterialCheckBox
      - Width: 0dp (layout_weight=1)
      - Checked: false
      - Text: @string/register_as_destination ("Register as destination")

    - `cbSmbWorkWithAllFiles` - MaterialCheckBox
      - Width: 0dp (layout_weight=1)
      - Checked: false
      - Text: @string/work_with_all_files ("All files *.*")
    
    - `cbSmbReadOnlyMode` - MaterialCheckBox
      - Width: 0dp (layout_weight=1)
      - Checked: false
      - Text: @string/read_only_mode ("Read-only mode")

  **Checkbox Row 2:**
  - `cbSmbScanSubdirectories` - MaterialCheckBox
    - Width: wrap_content
    - Checked: true (default)
    - Text: @string/scan_subdirectories ("Scan subdirectories")
    - MarginBottom: 8dp

  **Media Types Selection:**
  - TextView
    - Text: @string/supported_media_types ("Supported media types")
    - TextAppearance: ?attr/textAppearanceBody2
    - TextStyle: bold
    - MarginTop: 8dp, MarginBottom: 4dp

  - MaterialCardView (container for media types):
    - Width: match_parent
    - Height: wrap_content
    - CardElevation: 0dp
    - CardBackgroundColor: ?attr/colorSurfaceVariant
    - StrokeWidth: 1dp
    - StrokeColor: ?attr/colorOutlineVariant
    - MarginBottom: 8dp
    - Contains LinearLayout (vertical, padding 8dp) with:
      
      **Row 1: I V A G** (Images, Videos, Audio, GIFs)
      - LinearLayout (horizontal, 4 items):
        - For each: LinearLayout (layout_weight=1) containing:
          - MaterialCheckBox (id: cbSmbSupportImage/Video/Audio/Gif)
          - TextView (text: @string/images_i, videos_v, audio_a, gifs_g)
          - TextSize: 13sp
          - MinHeight: 40dp
      
      **Row 2: T P E** (Text, PDF, EPUB)
      - LinearLayout (horizontal, 3 items + 1 spacer):
        - Similar structure for cbSmbSupportText, cbSmbSupportPdf, cbSmbSupportEpub
        - Space (layout_weight=1) for alignment

  **Add Resource Button:**
  - `btnSmbAddManually` - MaterialButton
    - Width: match_parent
    - Height: wrap_content
    - Text: @string/smb_add_manual_resource ("Add Resource")
    - Icon: @android:drawable/ic_input_add
    - Style: Widget.MaterialComponents.Button
    - MarginBottom: 8dp

  **Domain + Port Row (Optional):**
  - LinearLayout (horizontal):
    - `tilSmbDomain` - TextInputLayout (width 0dp, layout_weight=1)
    - `tilSmbPort` - TextInputLayout (width 120dp, default "445")

  **Resources List (from Network Scan):**
  - `tvSmbResourcesToAdd` - TextView (visibility: gone)
  - `rvSmbResourcesToAdd` - RecyclerView (visibility: gone)

**3. SFTP/FTP Section** (`layoutSftpFolder` - ScrollView):
- Similar structure to SMB with:
  - Protocol selector (SFTP/FTP ChipGroup)
  - Host, Port, Username, Password fields
  - Remote Path, Resource Name
  - FTP Mode selection (Passive/Active)
  - All shared options (media types, PIN, read-only, etc.)

**4. Cloud Storage Section** (`layoutCloudStorage` - ScrollView):
- Provider selection (Google Drive, OneDrive, Dropbox chips)
- Authenticate button with status indicator
- Select Folder button (opens cloud picker)
- Selected folder display (path + ID)
- Resource Name field
- Shared options

**Activity Modes:**
1. **Add Mode** (default): Create new resources from scratch
2. **Copy Mode**: Intent extra `EXTRA_COPY_RESOURCE_ID` → Pre-fills form via `AddResourceHelper.preFillResourceData()`
3. **Preselected Tab Mode**: Intent extra `EXTRA_PRESELECTED_TAB` → Auto-opens section (LOCAL/SMB/FTP_SFTP/CLOUD)

**Critical Lifecycle Behaviors:**

**onCreate:**
- Copy mode detection: Reads `EXTRA_COPY_RESOURCE_ID` (-1L if absent), updates toolbar title
- Preselected tab routing: Defers navigation via `binding.root.post { }` to avoid race conditions
- Routes to: `showLocalFolderOptions()`, `showSmbFolderOptions()`, `showSftpFolderOptions()`, `showCloudStorageOptions()`
- Copy load deferred to `observeData()` after event subscriptions

**onResume:**
- **Dropbox OAuth completion**: Checks `isDropboxAuthenticated` flag → Calls `dropboxClient.finishAuthentication()`
  - AuthResult.Success → Toast + navigate to `DropboxFolderPickerActivity`
  - AuthResult.Error/Cancelled → Toast error message
  - Resets flag after handling
- **OneDrive OAuth completion**: Checks `isOneDriveAuthenticated` flag → Calls `oneDriveClient.testConnection()`
  - CloudResult.Success → Toast + navigate to `OneDriveFolderPickerActivity`
  - Resets flag after handling

**Configuration Changes:**
- `updateResourceTypeGridColumns()`: Landscape = 2 columns, Portrait = 1 column
- Called in `setupViews()` for initial setup

**Common Features:**
- **Resource Types:** Local (SAF), SMB (SMBJ 0.12.1), SFTP (SSHJ 0.37.0), FTP (Apache Commons Net 3.10.0), Cloud (OAuth2)
- **Network Discovery (SMB):** `NetworkDiscoveryDialog` with dynamic host list, maps ports to services (445→SMB, 21→FTP, 22→SFTP)
- **OAuth Flow (Cloud):** Handles Google/Dropbox/OneDrive auth via external browser/SDK, stores refresh tokens
- **Credential Encryption:** Android Keystore AES-256-GCM encryption for passwords, domains, SSH keys
- **Input Validation:** Custom widgets (`IpAddressEditText`, `RemotePathEditText`) with inline validation
- **Connection Testing:** 15-second timeout, detailed diagnostics (connection time, protocol version, file count)
- **Background Ops:** Speed tests launched in `applicationScope` (survives activity destruction)
- **Media Types (IVAGTPE):** Images, Videos, Audio, GIFs, Text, PDF, EPUB (initialized from global settings)

**Local Folder Critical Behaviors:**

**btnScan Click:**
- Logs: UserActionLogger.logButtonClick("ScanLocal", "AddResource")
- Calls `viewModel.scanLocalFolders()` → `scanLocalFoldersUseCase()`
- Shows `tvResourcesToAdd`, `rvResourcesToAdd`, `btnAddToResources` if resources found
- Sends message "Found X folders" via event

**btnAddManually Click:**
- Logs: "AddLocalManually"
- Launches `folderPickerLauncher` (ActivityResultContracts.OpenDocumentTree)

**folderPickerLauncher Callback:**
- Takes persistable URI permissions: `FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION`
- Try-catch SecurityException: Fallback to READ-only if WRITE fails
- Logs: "Selected folder: $uri", "Persistable URI permission taken for: $uri"
- Calls `viewModel.addManualFolder(uri)`

**ResourceToAddAdapter (6 callbacks):**
- `onSelectionChanged` → `viewModel.toggleResourceSelection(resource, selected)` (updates `selectedPaths` Set)
- `onNameChanged` → `viewModel.updateResourceName(resource, newName)`
- `onDestinationChanged` → `viewModel.toggleDestination(resource, isDestination)`
- `onScanSubdirectoriesChanged` → `viewModel.toggleScanSubdirectories(resource, scanSubdirectories)`
- `onReadOnlyChanged` → `viewModel.toggleReadOnlyMode(resource, isReadOnly)` (forces `isDestination = false`)
- `onMediaTypeToggled` → `viewModel.toggleMediaType(resource, type)` (add/remove from Set)

**Checkbox Interactions:**
- `setupCheckboxInteractions()`: When `cbLocalReadOnlyMode` checked → Unchecks + disables `cbLocalAddToDestinations`
- Same logic for `cbSmbReadOnlyMode` and `cbSftpReadOnlyMode`
- Mutual exclusion: Read-only resources cannot be destinations
- **Universal Access Logic:**
  - `cbWorkWithAllFiles` checked:
    - Disables all "Supported media types" checkboxes (sets enabled=false)
    - Visually checks all of them (or greys them out while implying "ALL")
    - Scanner uses `*.*` filter instead of specific extensions
  - `cbWorkWithAllFiles` unchecked:
    - Restores previous state of media type checkboxes
    - Re-enables interactions

**btnAddToResources Click:**
- Logs: "AddSelectedLocal"
- Filters resources by `selectedPaths` Set
- Validates: Shows toast "No resources selected" if empty
- Calls `viewModel.addSelectedResources()` → `addResourceUseCase.addMultiple()`
- Triggers speed tests in background (`applicationScope.launch`) for network resources
- Sends `ResourcesAdded` event → `finish()`

**SMB Network Critical Behaviors:**

**setupIpAddressField Auto-Fill:**
- Gets device IP via `NetworkUtils.getLocalIpAddress(context)`
- Extracts subnet: `deviceIp.substringBeforeLast(".") + "."`
- Sets `etSmbServer.setText(subnet)`, positions cursor: `setSelection(subnet.length)`

**IpAddressEditText Custom Widget:**
- Input filter: `IpAddressInputFilter` (allows digits + dots, replaces comma with dot)
- Validates: Blocks 4th dot (limits to 4 octets), validates each octet 0-255
- Method: `isValid()` returns false if any octet invalid
- Shows inline error if invalid

**btnScanNetwork Click:**
- Logs: "ScanNetwork"
- Shows `NetworkDiscoveryDialog` via `supportFragmentManager`
- Dialog callback: `onHostSelected = { host -> etSmbServer.setText(host.ip); toast with hostname + ports }`

**NetworkDiscoveryDialog:**
- `onStart()`: Calls `viewModel.scanNetwork()` if hosts empty AND not scanning
- Observes `viewModel.state.foundNetworkHosts` in `repeatOnLifecycle`
- Updates RecyclerView dynamically as hosts found (Flow emitter)
- Shows ProgressBar while scanning, empty state TextView if no hosts
- NetworkHostAdapter: Maps ports to services (445→SMB, 21→FTP, 22→SFTP)

**btnSmbTest Click:**
- Validates: `etSmbServer.isValid()` (custom widget method)
- Shows toast R.string.invalid_server_address if invalid, requests focus
- Reads shareName, username, password, domain, port (default 445)
- Calls `viewModel.testSmbConnection(server, shareName, username, password, domain, port)`
- ViewModel logic: If shareName empty → Tests server + lists available shares
- Timeout: 15 seconds, detailed diagnostics (connection time, protocol version, shares list)

**Media Types Checkbox Initialization:**
- Called in `showSmbFolderOptions()` via `lifecycleScope.launch`
- Gets supported types from `viewModel.getSupportedMediaTypes()` (checks app settings)
- Sets each checkbox: `isChecked = MediaType.X in supportedTypes` AND `isVisible = MediaType.X in supportedTypes`
- Hidden types (e.g., PDF if disabled globally) remain invisible

**SFTP/FTP Critical Behaviors:**

**Protocol RadioGroup Listener:**
- When SFTP selected: If port blank OR port == "21" → Sets "22"
- When FTP selected: If port blank OR port == "22" → Sets "21"
- Prevents user confusion when switching protocols

**Auth Method RadioGroup Listener:**
- Password selected: Shows `layoutSftpPasswordAuth`, hides `layoutSftpSshKeyAuth`
- SSH Key selected: Hides password layout, shows SSH key layout

**btnSftpLoadKey Click:**
- Logs: "LoadSshKey"
- Launches `sshKeyFilePickerLauncher` with mimeTypes `["*/*"]`
- Callback: Reads file content, sets `etSftpPrivateKey.setText(keyContent)`, shows toast

**RemotePathEditText Custom Widget:**
- Method: `getNormalizedPath()` auto-adds leading /, removes trailing /, converts backslashes to forward slashes

**btnSftpTest Click:**
- Validates host via `etSftpHost.isValid()`
- Determines auth method: SSH Key vs Password
- Validates SSH key not empty if SSH Key method selected
- Calls `viewModel.testSftpConnectionWithKey(...)` OR `viewModel.testSftpFtpConnection(...)`
- Uses SSHJ 0.37.0 for SFTP, Apache Commons Net 3.10.0 for FTP

**Cloud Storage Critical Behaviors:**

**updateCloudStorageStatus():**
- Called in `showCloudStorageOptions()`
- **Google Drive**: Gets `GoogleSignIn.getLastSignedInAccount(this)`, sets status TextView
- **Dropbox** (coroutine): Tries `dropboxClient.tryRestoreFromStorage()`, tests connection, sets status
- **OneDrive** (coroutine): Checks `isAuthenticated()`, tests connection, sets status

**Google Drive Authentication:**
- If account exists: Shows dialog (Select Folder / Sign Out / Cancel)
- If account null: Launches `googleSignInLauncher` → `handleGoogleSignInResult()`
- Callback: Stores account, updates status, navigates to `GoogleDriveFolderPickerActivity`
- Catch ApiException: Logs statusCode, shows toast

**Dropbox Authentication:**
- Tests connection: If success → Shows dialog (already authenticated)
- Else: Starts OAuth via `Auth.startOAuth2Authentication(this, appKey)`
- Sets `isDropboxAuthenticated = true` for onResume handling

**OneDrive Authentication:**
- Tests connection: If success → Shows dialog
- Else: Calls `authenticate()` (silent auth), handles interactive sign-in if needed
- Uses MSAL library with callback pattern

**Copy Mode (AddResourceHelper):**

**preFillResourceData by Type:**
- **LOCAL**: Shows toast R.string.select_folder_copy_location (cannot pre-select SAF URI)
- **SMB**: Parses `smb://server/shareName/subfolders`, pre-fills server, shareName, username, password, domain, port "445", comment
- **SFTP**: Parses `sftp://host:port/path`, pre-fills host, port (fallback 22), path, username, auth method (SSH Key OR Password), comment
- **FTP**: Parses `ftp://host:port/path`, pre-fills host, port (fallback 21), path, username, password, comment
- **CLOUD**: Shows toast R.string.select_cloud_folder_copy (user must re-authenticate)

**Functionality:**
- **Discovery:** `discoverNetworkResourcesUseCase.execute()` (Flow emitter), dynamic host list in dialog
- **OAuth:** Google Sign-In SDK / Microsoft MSAL / Dropbox Core SDK with browser-based flows
- **Database:** Atomic save to Room `ResourceEntity`, credentials encrypted in separate `CredentialsEntity` table
- **Error Handling:** `showError()` checks `settings.showDetailedErrors` (dialog vs toast), `showTestResultDialog()` with copy button
- **Dynamic Visibility:** Sections show/hide via `showLocalFolderOptions()`, `showSmbFolderOptions()`, `showSftpFolderOptions()`, `showCloudStorageOptions()`
- **Speed Tests:** Launched in `applicationScope` on IO dispatcher (non-blocking background jobs)
- **Logging:** UserActionLogger (14 events: LocalFolderCard, NetworkFolderCard, SftpFolderCard, CloudStorageCard, GoogleDriveCard, DropboxCard, OneDriveCard, ScanLocal, AddLocalManually, AddSelectedLocal, SmbTest, ScanNetwork, AddSelectedSmb, AddSmbManually, SftpTest, AddSftp, LoadSshKey)
- **Timber Logs:** Debug (folder selected, permissions taken, resources added), Warning (permission fallback), Error (permission failed, resource not found)

---

## EditResourceActivity
**Package:** `com.sza.fastmediasorter.ui.editresource`  
**Purpose:** Modify existing resource configuration  
**ViewModel:** `EditResourceViewModel`  
**Layout:** `activity_edit_resource.xml`

**Root Container:**
- NestedScrollView (match_parent × match_parent)
  - FillViewport: true
  - Contains: ConstraintLayout with all elements

**UI Elements:**

**Toolbar:**
- `toolbar` - MaterialToolbar
  - Width: match_parent
  - Height: ?attr/actionBarSize
  - Background: ?attr/colorPrimary
  - Elevation: 4dp
  - NavigationIcon: @drawable/ic_arrow_back
  - Includes: toolbar_edit_resource layout

**Top Action Buttons** (`layoutButtons` - LinearLayout):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- Orientation: horizontal
- Padding: 16dp horizontal, 4dp vertical
- Background: ?attr/colorSurface
- Elevation: 2dp
- Contains:
  - `btnTest` - MaterialButton
    - Width: 0dp (layout_weight=1)
    - Text: @string/test ("Test")
    - Style: Widget.MaterialComponents.Button.OutlinedButton
  - `btnReset` - MaterialButton
    - Width: 0dp (layout_weight=1)
    - MarginStart: 8dp
    - Text: @string/reset ("Reset")
    - Style: OutlinedButton
  - `btnSave` - MaterialButton
    - Width: 0dp (layout_weight=1)
    - MarginStart: 8dp
    - Text: @string/save ("Save")

**Main Form Section** (`layoutContent` - LinearLayout):
- Width: 0dp (constrained start→end)
- Height: wrap_content
- Orientation: vertical
- Padding: 16dp horizontal, 8dp top, 16dp bottom

**Common Fields:**

  **Resource Name:**
  - `tilResourceName` - TextInputLayout
    - Width: match_parent
    - Hint: @string/resource_name ("Resource Name")
    - BoxBackgroundMode: outline
    - Contains:
      - `etResourceName` - TextInputEditText
        - InputType: text
        - MaxLines: 1

  **Comment + PIN Code Row:**
  - LinearLayout (horizontal):
    - `tilResourceComment` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/comment ("Comment")
      - BoxBackgroundMode: outline
      - Contains:
        - `etResourceComment` - TextInputEditText
          - InputType: textMultiLine
          - MinLines: 1, MaxLines: 3
    
    - `tilAccessPassword` - TextInputLayout
      - Width: 240dp
      - MarginStart: 4dp
      - Hint: @string/resource_pin_code ("PIN Code")
      - BoxBackgroundMode: outline
      - EndIconMode: password_toggle
      - Contains:
        - `etAccessPassword` - TextInputEditText
          - InputType: numberPassword
          - MaxLines: 1

**SMB Credentials Section** (`layoutSmbCredentials` - LinearLayout):
- Width: match_parent
- Orientation: vertical
- MarginTop: 8dp
- Visibility: gone (shown for SMB resources)

  **Server IP + Share Name Row:**
  - LinearLayout (horizontal):
    - `tilSmbServerEdit` - TextInputLayout
      - Width: 180dp
      - MarginEnd: 4dp
      - Hint: @string/smb_server ("Server IP")
      - HelperText: @string/smb_server_hint
      - BoxBackgroundMode: outline
      - Contains: etSmbServerEdit
    
    - `tilSmbShareNameEdit` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginStart: 4dp
      - Hint: @string/smb_share_name ("Share Name")
      - BoxBackgroundMode: outline
      - Contains: etSmbShareNameEdit

  **Username + Password Row:**
  - LinearLayout (horizontal):
    - `tilSmbUsernameEdit` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/smb_username ("Username")
      - BoxBackgroundMode: outline
      - Contains: etSmbUsernameEdit
    
    - `tilSmbPasswordEdit` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginStart: 4dp
      - Hint: @string/smb_password ("Password")
      - EndIconMode: password_toggle
      - BoxBackgroundMode: outline
      - Contains:
        - `etSmbPasswordEdit` - TextInputEditText
          - InputType: textPassword

  **Domain + Port Row:**
  - LinearLayout (horizontal):
    - `tilSmbDomainEdit` - TextInputLayout
      - Width: 0dp (layout_weight=1)
      - MarginEnd: 4dp
      - Hint: @string/smb_domain ("Domain")
      - BoxBackgroundMode: outline
      - Contains: etSmbDomainEdit
    
    - `tilSmbPortEdit` - TextInputLayout
      - Width: 120dp
      - MarginStart: 4dp
      - Hint: @string/smb_port ("Port")
      - BoxBackgroundMode: outline
      - Contains:
        - `etSmbPortEdit` - TextInputEditText
          - InputType: number
          - Default text: "445"

**SFTP Credentials Section** (`layoutSftpCredentials` - LinearLayout):
- Width: match_parent
- Orientation: vertical
- MarginTop: 12dp
- Visibility: gone (shown for SFTP/FTP resources)

  **Title:**
  - TextView
    - Text: "S/FTP Credentials"
    - TextSize: 16sp, TextStyle: bold

  **Host + Port Row:**
  - LinearLayout (horizontal):
    - `tilSftpHostEdit` - TextInputLayout
      - Width: 180dp
      - MarginEnd: 4dp
      - Hint: @string/sftp_host ("Host")
      - HelperText: @string/sftp_host_hint
      - BoxBackgroundMode: outline
      - Contains: etSftpHostEdit
    
    - `tilSftpPortEdit` - TextInputLayout
      - Width: 120dp
      - MarginStart: 4dp
      - Hint: @string/sftp_port ("Port")
      - BoxBackgroundMode: outline
      - Contains:
        - `etSftpPortEdit` - TextInputEditText
          - InputType: number
          - Default text: "22"

  **Username + Password Row:**
  - Linear Layout (horizontal, similar to SMB):
    - `tilSftpUsernameEdit` + `tilSftpPasswordEdit`

  **Remote Path:**
  - `tilSftpPathEdit` - TextInputLayout
    - Width: match_parent
    - MarginTop: 4dp
    - Hint: @string/sftp_remote_path ("Remote Path")
    - HelperText: @string/sftp_remote_path_hint
    - BoxBackgroundMode: outline
    - Contains: etSftpPathEdit

**Resource Path + Slideshow Interval Row:**
- LinearLayout (horizontal):
  - `tilResourcePath` - TextInputLayout
    - Width: 0dp (layout_weight=1)
    - MarginEnd: 4dp
    - Hint: @string/resource_path ("Resource Path")
    - BoxBackgroundMode: outline
    - Contains:
      - `etResourcePath` - TextInputEditText
        - InputType: none
        - Focusable: false (read-only)
        - TextSize: 12sp
  
  - `tilSlideshowIntervalForResource` - TextInputLayout
    - Width: 120dp
    - MarginStart: 4dp
    - Hint: @string/slideshow_interval_for_resource ("Slideshow (s)")
    - Style: Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu
    - Contains:
      - `etSlideshowInterval` - AutoCompleteTextView
        - InputType: number
        - MaxLength: 5
        - Default: "5"

**Media Types Selection:**
- `tvMediaTypesLabel` - TextView
  - Text: @string/supported_media_types ("Supported media types")
  - TextSize: 16sp, TextStyle: bold
  - MarginTop: 8dp

- LinearLayout (vertical):
  **Row 1: Images + Video**
  - LinearLayout (horizontal, minHeight 48dp):
    - `cbSupportImages` - MaterialCheckBox (layout_weight=1)
    - `cbSupportVideo` - MaterialCheckBox (layout_weight=1)
  
  **Row 2: Audio + GIF**
  - LinearLayout (horizontal, minHeight 48dp):
    - `cbSupportAudio` - MaterialCheckBox (layout_weight=1)
    - `cbSupportGif` - MaterialCheckBox (layout_weight=1)
  
  **Row 3: Text + PDF** (`layoutMediaTypesTextPdf`):
  - LinearLayout (horizontal, minHeight 48dp, visibility: gone):
    - `cbSupportText` - MaterialCheckBox (layout_weight=1)
    - `cbSupportPdf` - MaterialCheckBox (layout_weight=1)
  
  **Row 4: EPUB** (`layoutMediaTypesEpub`):
  - LinearLayout (horizontal, minHeight 48dp):
    - `cbSupportEpub` - MaterialCheckBox (layout_weight=1)
    - Empty View (layout_weight=1 for alignment)

**Options Row:**
- LinearLayout (horizontal, minHeight 48dp):
  - `cbScanSubdirectories` - MaterialCheckBox
    - Width: 0dp (layout_weight=1)
    - Checked: true (default)
    - Text: @string/scan_subdirectories
  
  - `cbDisableThumbnails` - MaterialCheckBox
    - Width: 0dp (layout_weight=1)
    - Text: @string/disable_thumbnails

**Read-Only Mode:**
- LinearLayout (horizontal, gravity: center_vertical):
  - `cbReadOnlyMode` - MaterialCheckBox
    - Width: wrap_content
  
  - LinearLayout (vertical, layout_weight=1):
    - TextView: @string/read_only_mode (16sp)
    - TextView: @string/read_only_mode_hint (12sp, secondary color)

**Destination Toggle:**
- LinearLayout (horizontal, gravity: center_vertical):
  - `switchIsDestination` - SwitchMaterial
    - Width: wrap_content
    - MarginEnd: 12dp
  
  - TextView
    - Text: @string/use_as_destination ("Use as destination")
    - TextSize: 16sp
    - Width: 0dp (layout_weight=1)

**Speed Test Results Card** (`cardSpeedResults`):
- MaterialCardView
  - Width: match_parent
  - MarginTop: 12dp
  - Visibility: gone (shown when speed test data available)
  - CornerRadius: 8dp
  - Elevation: 2dp
  - StrokeColor: ?attr/colorOutline
  - StrokeWidth: 1dp
  - Contains LinearLayout (vertical, padding 12dp):
    
    **Title:**
    - TextView: @string/network_speed ("Network Speed")
      - TextSize: 14sp, TextStyle: bold
    
    **Speed Metrics Row:**
    - LinearLayout (horizontal):
      - LinearLayout (vertical, layout_weight=1):
        - TextView: @string/read_speed ("Read Speed") - 12sp, secondary
        - `tvReadSpeed` - TextView: "0.0 Mbps" - 14sp, bold
      
      - LinearLayout (vertical, layout_weight=1):
        - TextView: @string/write_speed ("Write Speed") - 12sp, secondary
        - `tvWriteSpeed` - TextView: "0.0 Mbps" - 14sp, bold
      
      - LinearLayout (vertical, layout_weight=1):
        - TextView: @string/rec_threads ("Rec. Threads") - 12sp, secondary
        - `tvRecThreads` - TextView: "-" - 14sp, bold
    
    **Last Test Date:**
    - `tvLastTestDate` - TextView
      - Text: "Last test: -"
      - TextSize: 11sp
      - TextColor: secondary
      - MarginTop: 8dp

**Clear Trash Button:**
- `btnClearTrash` - MaterialButton
  - Width: match_parent
  - MarginTop: 12dp
  - Text: @string/clear_trash ("Clear Trash")
  - TextColor: ?attr/colorError
  - StrokeColor: ?attr/colorError
  - Icon: @android:drawable/ic_menu_delete
  - IconTint: ?attr/colorError
  - Style: Widget.MaterialComponents.Button.OutlinedButton
  - Visibility: gone (shown when trash folders exist)

**Critical Initialization Behaviors:**

**ResourceId Extraction (init block):**
- Reads from `savedStateHandle.get<Long>("resourceId")`
- Fallback: `savedStateHandle.get<String>("resourceId")?.toLongOrNull()`
- Default: 0L if neither exists
- Immediately calls `loadResource()` after state initialization

**loadResource Private Method:**
- Coroutine: `viewModelScope.launch(ioDispatcher + exceptionHandler)`
- Gets resource via `getResourcesUseCase.getById(resourceId)`
- Validates null: Sends error event, returns early
- Gets global settings: `settingsRepository.getSettings().first()` (textEnabled, pdfEnabled, epubEnabled)

**Credential Loading by Type:**
- **SMB**: `smbOperationsUseCase.getConnectionInfo(credentialsId).onSuccess { info -> ... }`
  - Parses path: Normalizes `replace('\\', '/')`, removes `smb://` prefix, extracts share + subfolders
  - Sets: `smbServer`, `smbShareName`, `smbUsername`, `smbPassword`, `smbDomain`, `smbPort`
- **SFTP**: `getSftpCredentials(credentialsId).onSuccess { creds -> ... }`
  - Parses path: Removes `sftp://`, extracts path (keeps leading /)
  - Sets: `sftpHost`, `sftpPort`, `sftpUsername`, `sftpPassword`, `sftpPath`
- **FTP**: `getFtpCredentials(credentialsId).onSuccess { creds -> ... }`
  - Parses path: Removes `ftp://`, extracts path
  - Same fields as SFTP

**State Update (Single Operation):**
- Updates state ONCE with all loaded data to prevent UI flickering
- Includes: `originalResource`, `currentResource`, credentials (SMB/SFTP), global settings

**Destinations Count Calculation:**
- Gets all resources: `resourceRepository.getFilteredResources()`
- Counts destinations: `allResources.count { it.isDestination }`
- Computes `canBeDestination` via helper:
  - If resource already destination: true (can toggle off)
  - If read-only: false
  - If destinationsCount >= maxDestinations: false
  - Else: true

**Toolbar & Header Critical Behaviors:**

**Toolbar Title with Resource Type:**
- Gets type label: R.string.resource_type_local/smb/sftp/ftp/cloud
- Sets title: R.string.edit_resource_with_type + label (e.g., "Edit Resource (SMB)")

**Toolbar Icon by Resource Type:**
- LOCAL → ic_resource_local
- SMB → ic_resource_smb
- SFTP → ic_resource_sftp
- FTP → ic_resource_ftp
- CLOUD → Provider-specific (ic_provider_google_drive / ic_provider_onedrive / ic_provider_dropbox) or default ic_resource_cloud

**Form Fields Critical Behaviors:**

**Text Field Listeners (3 fields):**
- `etResourceName`: Calls `viewModel.updateName(s?.toString() ?: "")` on afterTextChanged
- `etResourceComment`: Calls `viewModel.updateComment(s?.toString() ?: "")`
- `etAccessPassword`: Calls `viewModel.updateAccessPin(s?.toString()?.takeIf { it.isNotBlank() })` (null if blank)
- **Only updates if text differs** from current to avoid cursor position issues

**etSlideshowInterval (AutoCompleteTextView):**
- Dropdown options: ["1", "5", "10", "30", "60", "120", "300"] seconds
- Item click: Calls `viewModel.updateSlideshowInterval(seconds.toInt())`
- OnFocusChange: When focus lost → Reads text, clamps value `coerceIn(1, 3600)`, updates field if changed

**Media Type Checkboxes (7 checkboxes):**
- Each checkbox calls `updateMediaTypes()` on CheckedChangeListener
- `updateMediaTypes()`: Reads all checkbox states, creates `Set<MediaType>`, calls `viewModel.updateSupportedMediaTypes(types)`

**Credentials Sections (SMB/SFTP):**

**addSmbListeners Method:**
- Creates 6 TextWatchers: server, shareName, username, password, domain, port
- Stores references in properties: `smbServerWatcher`, `smbShareNameWatcher`, etc.
- Attaches to EditTexts, sets `IpAddressInputFilter` on server field

**removeSmbListeners Method:**
- Removes all 6 TextWatchers using stored references
- Called BEFORE updating fields in observeData to avoid re-trigger loops

**SMB TextWatchers:**
- `smbServerWatcher` → `viewModel.updateSmbServer(s?.toString() ?: "")`
- `smbShareNameWatcher` → `viewModel.updateSmbShareName(...)`
- `smbUsernameWatcher` → `viewModel.updateSmbUsername(...)`
- `smbPasswordWatcher` → `viewModel.updateSmbPassword(...)`
- `smbDomainWatcher` → `viewModel.updateSmbDomain(...)`
- `smbPortWatcher` → Parses to int (fallback 445), calls `viewModel.updateSmbPort(port)`

**SFTP Similar Pattern:**
- addSftpListeners: 5 watchers (host, port, username, password, path)
- removeSftpListeners: Removes all before state update
- Sets `IpAddressInputFilter` on host field

**Action Buttons Critical Behaviors:**

**btnReset Click:**
- Logs: "Button RESET clicked"
- Calls `viewModel.resetToOriginal()`
- **Enabled only when**: `hasResourceChanges OR hasSmbCredentialsChanges OR hasSftpCredentialsChanges`

**btnTest Click:**
- Logs: "Button TEST clicked"
- Validates resource exists: `viewModel.state.value.currentResource ?: return@setOnClickListener`
- Calls `viewModel.testConnection()` (combined connection + speed test)

**btnSave Click:**
- Logs: "Button SAVE clicked"
- Calls `viewModel.saveChanges()`
- **Always enabled** (validates inside ViewModel)

**btnClearTrash Click:**
- Logs: "Button CLEAR_TRASH clicked"
- Calls `viewModel.requestClearTrash()`
- **Visible only when**: `state.hasTrashFolders = true`

**observeData Critical Updates:**

**Resource Path Display:**
- Sets `etResourcePath.setText(resource.path)` (read-only field: `focusable = false`, `inputType = none`)

**Date Formatting:**
- Uses `SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())`
- `tvCreatedDate`: Formats `resource.createdDate` (Long timestamp)
- `tvLastBrowseDate`: Formats `resource.lastBrowseDate` or R.string.never_browsed

**File Count Display:**
- If `resource.fileCount >= 1000` → Text ">1000"
- Else: `fileCount.toString()`

**Media Types Layout Visibility:**
- `layoutMediaTypesTextPdf.isVisible = state.isGlobalTextSupportEnabled OR state.isGlobalPdfSupportEnabled`
- `cbSupportText.isVisible = state.isGlobalTextSupportEnabled`
- `cbSupportPdf.isVisible = state.isGlobalPdfSupportEnabled`
- `layoutMediaTypesEpub.isVisible = state.isGlobalEpubSupportEnabled`

**cbReadOnlyMode Complex Update:**
- Temporarily removes listener
- If `resource.isWritable = false`: Sets `isChecked = true` (force read-only)
- Else: Sets `isChecked = resource.isReadOnly` (respects user preference)
- Always `isEnabled = true` (user can toggle)
- Re-attaches listener

**switchIsDestination Complex Update:**
- Temporarily removes listener
- Sets `isEnabled = state.canBeDestination`
- Sets `isChecked = resource.isDestination AND state.canBeDestination`
- Re-attaches listener

**Speed Test Results Card:**
- Logs: "resource.type=${resource.type}, readSpeed=${resource.readSpeedMbps}, writeSpeed=${resource.writeSpeedMbps}"
- **Visible if**: Resource type is SMB/SFTP/FTP/CLOUD AND (readSpeedMbps != null OR writeSpeedMbps != null)
- Sets `tvReadSpeed.text = "%.2f Mbps".format(readSpeedMbps)` or "-"
- Sets `tvWriteSpeed.text = "%.2f Mbps".format(writeSpeedMbps)` or "-"
- Sets `tvRecThreads.text = recommendedThreads.toString()` or "-"
- Sets `tvLastTestDate.text` with formatted date or R.string.never_tested
- Logs: "Speed test card VISIBLE" or "HIDDEN"

**Credentials Section Update (with listener removal):**
- **SMB**: Calls `removeSmbListeners()`, updates 6 fields (only if text differs), calls `addSmbListeners()`
- **SFTP**: Calls `removeSftpListeners()`, updates 5 fields (only if text differs), calls `addSftpListeners()`

**Button Enable States:**
- `btnSave.isEnabled = true` (always)
- `btnReset.isEnabled = state.hasResourceChanges OR state.hasSmbCredentialsChanges OR state.hasSftpCredentialsChanges`

**Loading & Progress Critical Behaviors:**

**Loading Flow Collection:**
- If NOT testing speed: Shows `layoutProgress` + `progressBar`, message R.string.loading

**Speed Test Progress:**
- If `state.isTestingSpeed = true`: Shows `layoutProgress`, sets `tvProgressMessage.text = state.speedTestStatus` or fallback R.string.analyzing_speed
- Else if not loading: Hides `layoutProgress`

**Events Critical Behaviors:**

**TestResult Event:**
- Shows `showTestResultDialog(message, success)` with AlertDialog + copy button
- **If success AND credentials changed**: Shows dialog R.string.save_credentials_title, R.string.save_credentials_message
  - Positive button: R.string.save_now → `viewModel.saveChanges()`
  - Negative button: R.string.later (dismisses)

**ConfirmClearTrash Event:**
- Shows AlertDialog: R.string.clear_trash_confirm_title, R.string.clear_trash_confirm_message with count
- Positive button: R.string.clear_trash → `viewModel.clearTrash()`

**RequestCloudReAuthentication Event:**
- Shows AlertDialog: R.string.restore_connection, R.string.cloud_auth_dialog_message
- Positive button: R.string.sign_in_now → Dismisses, calls `launchGoogleSignIn()`

**ViewModel Critical Methods:**

**updateCurrentResource Private:**
- Updates `currentResource` in state
- Calls `checkHasChanges()` to update change flags
- Recomputes `canBeDestination`

**checkHasChanges Private:**
- Compares `currentResource` with `originalResource`
- Sets `hasResourceChanges = true` if any field differs (name, comment, PIN, slideshow, mediaTypes, flags)
- Compares credentials (SMB/SFTP) if applicable
- Sets `hasSmbCredentialsChanges` or `hasSftpCredentialsChanges`
- Updates `hasChanges` flag

**saveChanges:**
- Validates current resource exists
- Validates at least one media type selected
- Saves resource via `updateResourceUseCase.update(currentResource)`
- Saves credentials if changed (SMB or SFTP)
- Sends `ResourceUpdated` event → Activity finishes

**Features:**
- **Pre-Population:** Loads ResourceEntity via `getResourcesUseCase.getById()`, decrypts credentials by type
- **Modification:** Updates 13 fields (name, comment, PIN, slideshow, mediaTypes, flags, credentials)
- **Destinations:** Toggle via switch with `canBeDestination` validation (checks count < maxDestinations)
- **Statistics:** Speed test card (conditional visibility, formatted speeds, recommended threads, last test date)
- **Connection Testing:** Combined connection + speed test in single button
- **Re-Authentication:** `RequestCloudReAuthentication` event triggers Google Sign-In flow
- **Trash Management:** `btnClearTrash` with confirmation dialog, shows count of files to delete
- **Dynamic Visibility:** Credential sections (SMB/SFTP) show based on `resource.type`
- **Change Tracking:** 3 flags (`hasResourceChanges`, `hasSmbCredentialsChanges`, `hasSftpCredentialsChanges`) control Reset button state
- **Listener Management:** `addSmbListeners()` / `removeSmbListeners()`, `addSftpListeners()` / `removeSftpListeners()` prevent re-trigger loops

**Functionality:**
- **Validation:** Same as AddResourceActivity.
- **Credential Update:** Re-encrypts only on change.
- **Speed Test Data:** Queries ResourceEntity for readSpeedMbps, writeSpeedMbps, recommendedThreads.
- **Atomic Save/Reset:** Rollback on error.
- **Dynamic Sections:** SMB credentials visible only for SMB, SFTP only for SFTP/FTP.
- **Read-Only Path:** Resource path field is non-editable (displays URI/path).

**Dialogs:** ColorPickerDialog (for destination color), Delete Confirmation, Re-auth sheets.
