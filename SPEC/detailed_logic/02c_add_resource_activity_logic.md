# AddResourceActivity - Complete Behavior Specification

**File**: `ui/addresource/AddResourceActivity.kt` (1175 lines)
**Architecture**: MVVM + Helper Delegation
**Purpose**: Unified activity for adding/copying resources (Local folders, SMB, SFTP, FTP, Cloud storage)

---

## Overview

AddResourceActivity handles all resource creation workflows:
- **Local folders**: Scan device storage OR manual folder picker (DocumentTree API)
- **SMB shares**: Manual entry OR network scan with credentials
- **SFTP**: Password OR SSH key authentication
- **FTP**: Standard password authentication
- **Cloud storage**: OAuth2 flows for Google Drive, OneDrive, Dropbox

**Modes**:
1. **Add Mode** (default): Create new resources from scratch
2. **Copy Mode**: Clone existing resource with pre-filled data (intent extra: `EXTRA_COPY_RESOURCE_ID`)
3. **Preselect Mode**: Auto-expand specific section based on intent extra `EXTRA_PRESELECTED_TAB` (LOCAL, SMB, FTP_SFTP, CLOUD)

**File Size**: 1175 lines decomposed with `AddResourceHelper` (UI show/hide logic ~150 lines extracted)

---

## Components

### 1. ViewBinding & Injected Dependencies

```kotlin
private val viewModel: AddResourceViewModel by viewModels()
private lateinit var binding: ActivityAddResourceBinding

@Inject lateinit var googleDriveClient: GoogleDriveRestClient
@Inject lateinit var dropboxClient: DropboxClient
@Inject lateinit var oneDriveClient: OneDriveRestClient

private lateinit var helper: AddResourceHelper
```

**Two Adapters** (for Local and SMB scan results):
- `resourceToAddAdapter`: Local folders from scan
- `smbResourceToAddAdapter`: SMB shares from network scan

### 2. ActivityResultLaunchers (4 launchers)

1. **folderPickerLauncher** (`OpenDocumentTree`): 
   - For manual local folder selection.
   - **Callback Logic**:
     - Takes persistable URI permissions: `FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION`
     - Try-catch SecurityException: If write permission fails, fallback to read-only with warning log
     - Logs: `Timber.d("Selected folder: $uri")`, `Timber.d("Persistable URI permission taken for: $uri")`
     - On write permission failure: `Timber.w("Could not take write permission, fallback to read-only: ${e.message}")`
     - On critical failure: `Timber.e("Failed to take persistable URI permission: ${e.message}")`
   - Calls `viewModel.addManualFolder(uri)` on success.

2. **googleSignInLauncher** (`StartActivityForResult`):
   - Launches Google Sign-In intent.
   - Processes result in `handleGoogleSignInResult()`.

3. **sshKeyFilePickerLauncher** (`OpenDocument`):
   - Picks SSH private key file (any MIME type: `*/*`).
   - Reads file content → Sets to `etSftpPrivateKey`.

4. **Implicit launchers** (Dropbox/OneDrive):
   - Dropbox uses `Auth.startOAuth2Authentication()` (SDK handles activity return).
   - OneDrive uses MSAL `signIn()` callback pattern.

### 3. State Flags

- `copyResourceId: Long?`: If set, activity is in Copy Mode.
- `googleDriveAccount: GoogleSignInAccount?`: Cached Google account.
- `isDropboxAuthenticated: Boolean`: Flag for `onResume()` processing.
- `isOneDriveAuthenticated: Boolean`: Flag for `onResume()` processing.

---

## Lifecycle Methods

### onCreate

1. **Parse Intent Extras**:
   - `EXTRA_COPY_RESOURCE_ID` (-1 if absent) → Store in `copyResourceId`.
   - `EXTRA_PRESELECTED_TAB` (String) → Parse to `ResourceTab` enum.

2. **Title Update**:
   - Copy mode: "Copy Resource" (R.string.copy_resource_title).
   - Add mode: "Add Resource" (R.string.add_resource_title).

3. **Auto-Expand Section** (deferred to `binding.root.post{}`):
   - `LOCAL` → `showLocalFolderOptions()`
   - `SMB` → `showSmbFolderOptions()`
   - `FTP_SFTP` → `showSftpFolderOptions()`
   - `CLOUD` → `showCloudStorageOptions()`
   - `ALL`/`FAVORITES` → Show default view (resource type cards)

**Load for Copy Mode**: Deferred to `observeData()` after event subscriptions ready.

### onResume

**Dropbox OAuth Completion**:
1. Checks `isDropboxAuthenticated` flag
2. If true: Calls `dropboxClient.finishAuthentication()`
3. Result handling:
   - `AuthResult.Success` → Toast success, navigate to `DropboxFolderPickerActivity`
   - `AuthResult.Error(message)` → Toast error message
   - `AuthResult.Cancelled` → Toast "Authentication cancelled"
4. Resets `isDropboxAuthenticated = false` after handling

**OneDrive OAuth Completion**:
1. Checks `isOneDriveAuthenticated` flag
2. If true: Calls `oneDriveClient.testConnection()` in coroutine
3. Result handling:
   - `CloudResult.Success` → Toast success, navigate to `OneDriveFolderPickerActivity`
   - `CloudResult.Error(message)` → Toast error message
4. Resets `isOneDriveAuthenticated = false` after handling

**Purpose**: OAuth flows redirect to external browser/app, return to activity in onResume. Flags ensure completion handled once.

### onConfigurationChanged

**Grid Column Update**:
- Calls `updateResourceTypeGridColumns()` to adjust layout
- Landscape: `gridLayoutManager.spanCount = 2` (2x2 grid for resource type cards)
- Portrait: `gridLayoutManager.spanCount = 1` (vertical stack)

### setupViews

**Toolbar**:
- Navigation icon click → `finish()` with slide-out animation.

**Grid Columns**:
- Landscape: 2 columns (resource type cards in 2x2 grid).
- Portrait: 1 column (vertical stack).
- Method: `updateResourceTypeGridColumns()` checks `Configuration.ORIENTATION_LANDSCAPE`.

**Helper Initialization**:
- `helper = AddResourceHelper(this, binding)` (handles show/hide UI section logic).

**IP Address Field Setup**:
- `setupIpAddressField()` auto-fills SMB server field with device IP subnet:
  - Gets device IP: `NetworkUtils.getLocalIpAddress(context)`
  - Extracts subnet: `deviceIp.substringBeforeLast(".") + "."` (e.g., "192.168.1.")
  - Sets text: `etSmbServer.setText(subnet)`
  - Positions cursor: `etSmbServer.setSelection(subnet.length)` (ready for user to type host)
  - Logs: `Timber.d("Auto-filled SMB server subnet: $subnet")`
- Custom widget `IpAddressEditText` provides validation:
  - Input filter: `IpAddressInputFilter` (allows digits + dots, replaces comma with dot)
  - Validates: Blocks 4th dot (limits to 4 octets), validates each octet 0-255
  - Method `isValid()`: Returns false if any octet invalid
  - Shows inline error if invalid

**Adapters** (2 adapters with 6 callbacks each):

```kotlin
ResourceToAddAdapter(
    onSelectionChanged = { resource, selected -> viewModel.toggleResourceSelection(resource, selected) },
    onNameChanged = { resource, newName -> viewModel.updateResourceName(resource, newName) },
    onDestinationChanged = { resource, isDest -> viewModel.toggleDestination(resource, isDest) },
    onScanSubdirectoriesChanged = { resource, scan -> viewModel.toggleScanSubdirectories(resource, scan) },
    onReadOnlyChanged = { resource, readOnly -> viewModel.toggleReadOnlyMode(resource, readOnly) },
    onMediaTypeToggled = { resource, type -> viewModel.toggleMediaType(resource, type) }
)
```

Assigned to:
- `rvResourcesToAdd` → Local folders
- `rvSmbResourcesToAdd` → SMB shares

**Click Listeners** (19 buttons + 3 radio groups):

**Resource Type Cards (4 cards)**:
1. `cardLocalFolder` → Logs `UserActionLogger.logCardClick("LocalFolderCard", "AddResource")` → `showLocalFolderOptions()`
2. `cardNetworkFolder` → Logs `UserActionLogger.logCardClick("NetworkFolderCard", "AddResource")` → `showSmbFolderOptions()`
3. `cardSftpFolder` → Logs `UserActionLogger.logCardClick("SftpFolderCard", "AddResource")` → `showSftpFolderOptions()`
4. `cardCloudStorage` → Logs `UserActionLogger.logCardClick("CloudStorageCard", "AddResource")` → `showCloudStorageOptions()`

**Cloud Provider Cards (3 cards)**:
5. `cardGoogleDrive` → Logs `UserActionLogger.logCardClick("GoogleDriveCard", "AddResource")` → `authenticateGoogleDrive()`
6. `cardDropbox` → Logs `UserActionLogger.logCardClick("DropboxCard", "AddResource")` → `authenticateDropbox()`
7. `cardOneDrive` → Logs `UserActionLogger.logCardClick("OneDriveCard", "AddResource")` → `authenticateOneDrive()`

**Local Buttons (3 buttons)**:
8. `btnScan` → Logs `UserActionLogger.logButtonClick("ScanLocal", "AddResource")` → `viewModel.scanLocalFolders()`
   - Shows `tvResourcesToAdd`, `rvResourcesToAdd`, `btnAddToResources` if resources found
   - Sends `ShowMessage` event: "Found X folders"
9. `btnAddManually` → Logs `UserActionLogger.logButtonClick("AddLocalManually", "AddResource")` → `folderPickerLauncher.launch(null)`
10. `btnAddToResources` → Logs `UserActionLogger.logButtonClick("AddSelectedLocal", "AddResource")` → `viewModel.addSelectedResources()`
   - Validates: Shows toast "No resources selected" if `selectedPaths.isEmpty()`
   - Filters resources by `selectedPaths` Set
   - Calls `addResourceUseCase.addMultiple()`
   - Triggers speed tests in background (`applicationScope.launch`) for network resources
   - Sends `ResourcesAdded` event → Activity calls `finish()`

**SMB Buttons (4 buttons)**:
11. `btnSmbTest` → Logs `UserActionLogger.logButtonClick("SmbTest", "AddResource")` → `testSmbConnection()`
12. `btnScanNetwork` → Logs `UserActionLogger.logButtonClick("ScanNetwork", "AddResource")` → Opens `NetworkDiscoveryDialog`
   - **Dialog Lifecycle**:
     - `onStart()`: Calls `viewModel.scanNetwork()` if `foundNetworkHosts.isEmpty() && !isScanning`
     - Observes `viewModel.state.foundNetworkHosts` in `repeatOnLifecycle(STARTED)`
     - Updates RecyclerView dynamically as hosts discovered (Flow emitter pattern)
   - **NetworkHostAdapter**:
     - Uses DiffUtil for efficient list updates
     - Maps ports to services: 445→SMB, 21→FTP, 22→SFTP
     - Item click callback: `onHostSelected = { host -> etSmbServer.setText(host.ip); toast with hostname + ports }`
   - **Empty State**: Shows "No devices found" TextView if scan completes with empty list
   - **Progress**: Shows ProgressBar while `isScanning = true`
13. `btnSmbAddToResources` → Logs `UserActionLogger.logButtonClick("AddSelectedSmb", "AddResource")` → `viewModel.addSelectedResources()` (add selected SMB shares)
14. `btnSmbAddManually` → Logs `UserActionLogger.logButtonClick("AddSmbManually", "AddResource")` → `addSmbResourceManually(isReadOnly)`

**SFTP Buttons (3 buttons)**:
15. `btnSftpTest` → `testSftpConnection()` (validates SFTP/FTP connection)
16. `btnSftpAddResource` → `addSftpResource()` (add SFTP/FTP resource)
17. `btnSftpLoadKey` → `sshKeyFilePickerLauncher.launch(arrayOf("*/*"))` (load SSH private key)

**Radio Groups (3 groups)**:

1. **rgProtocol** (SFTP vs FTP):
   - `rbSftp` selected → Set port to 22 (if empty or 21).
   - `rbFtp` selected → Set port to 21 (if empty or 22).

2. **rgSftpAuthMethod** (Password vs SSH Key):
   - `rbSftpPassword` → Show `layoutSftpPasswordAuth`, hide `layoutSftpSshKeyAuth`.
   - `rbSftpSshKey` → Show `layoutSftpSshKeyAuth`, hide `layoutSftpPasswordAuth`.

**Checkbox Interactions** (3 pairs):
- `cbLocalReadOnlyMode` checked → Disable + uncheck `cbLocalAddToDestinations`.
- `cbSmbReadOnlyMode` checked → Disable + uncheck `cbSmbAddToDestinations`.
- `cbSftpReadOnlyMode` checked → Disable + uncheck `cbSftpAddToDestinations`.

### observeData

**Load Resource for Copy** (deferred from onCreate):
```kotlin
copyResourceId?.let { resourceId ->
    viewModel.loadResourceForCopy(resourceId)
}
```

**State Collection** (3 coroutines):

1. **`state.collect`** (main state observer):
   - **Filter resources by type**:
     - Local: `type == ResourceType.LOCAL`
     - SMB: `type == ResourceType.SMB`
   - **Update adapters**:
     - `resourceToAddAdapter.submitList(localResources)` + `setSelectedPaths(selectedPaths)`
     - `smbResourceToAddAdapter.submitList(smbResources)` + `setSelectedPaths(selectedPaths)`
   - **Visibility logic**:
     - Local section visible if `localResources.isNotEmpty()`
     - SMB section visible if `smbResources.isNotEmpty()`

2. **`loading.collect`**: Show/hide `progressBar`.

3. **`events.collect`** (6 event types):
   - `ShowError(message)`: Call `showError()` (dialog OR toast based on settings).
   - `ShowMessage(message)`: Toast message.
   - `ShowTestResult(message, isSuccess)`: Call `showTestResultDialog()` with scrollable content.
   - `LoadResourceForCopy(resource, username, password, domain, sshKey, sshPassphrase)`:
     - Calls `helper.preFillResourceData()` to populate all fields.
   - `ResourcesAdded`: `finish()` (return to MainActivity).

### onResume

**Dropbox Auth Check**:
```kotlin
if (isDropboxAuthenticated) {
    val result = dropboxClient.finishAuthentication()
    when (result) {
        is AuthResult.Success -> Toast + navigateToDropboxFolderPicker()
        is AuthResult.Error -> Toast error
        is AuthResult.Cancelled -> Toast "Auth cancelled"
    }
    isDropboxAuthenticated = false
}
```

**OneDrive Auth Check**:
```kotlin
if (isOneDriveAuthenticated) {
    val result = oneDriveClient.testConnection()
    when (result) {
        is CloudResult.Success -> Toast + navigateToOneDriveFolderPicker()
        is CloudResult.Error -> Toast error
    }
    isOneDriveAuthenticated = false
}
```

**Note**: Google Drive uses `ActivityResultLauncher` so doesn't need onResume handling.

---

## UI Section Switching Logic

**Default View**: Resource type selection (4 cards: Local, Network, SFTP, Cloud).

**Section Switching** (handled by AddResourceHelper):

### showLocalFolderOptions()
- Hide `layoutResourceTypes`.
- Update title: "Add Local Folder".
- Show `layoutLocalFolder`.

### showSmbFolderOptions()
- Hide `layoutResourceTypes`.
- Update title: "Add Network Folder".
- Show `layoutSmbFolder`.
- Initialize media type checkboxes from `viewModel.getSupportedMediaTypes()`.
- Set visibility based on global settings (e.g., hide EPUB if disabled).

### showSftpFolderOptions()
- Hide `layoutResourceTypes`, `layoutSmbFolder`.
- Update title: "Add SFTP/FTP".
- Show `layoutSftpFolder`.
- Set default port: 22 (SFTP).
- Select `rbSftp` radio button.
- Initialize media type checkboxes from settings.

### showCloudStorageOptions()
- Hide `layoutResourceTypes`, `layoutSmbFolder`, `layoutSftpFolder`.
- Update title: "Cloud Storage".
- Show `layoutCloudStorage`.
- Call `updateCloudStorageStatus()`.

---

## Cloud Storage Authentication

### Google Drive

**Flow**:
1. User taps `cardGoogleDrive` → `authenticateGoogleDrive()`.
2. Check `GoogleSignIn.getLastSignedInAccount(this)`:
   - If account exists → `showGoogleDriveSignedInOptions(account)` (dialog with "Select Folder" or "Sign Out").
   - If null → `launchGoogleSignIn()`.
3. `googleSignInLauncher` launches `googleDriveClient.getSignInIntent()`.
4. User signs in → Returns to app → `handleGoogleSignInResult(data)`.
5. Parse account from `GoogleSignIn.getSignedInAccountFromIntent(data)`.
6. Store in `googleDriveAccount` → Toast "Signed in as {email}".
7. Navigate to `GoogleDriveFolderPickerActivity` for folder selection.

**Dialog Options** (when already signed in):
- "Select Folder" → `navigateToGoogleDriveFolderPicker()`
- "Sign Out" → `signOutGoogleDrive()` (clears account, updates status)
- "Cancel" → Dismiss dialog

### Dropbox

**Flow**:
1. User taps `cardDropbox` → `authenticateDropbox()`.
2. Check `dropboxClient.testConnection()`:
   - If Success → `showDropboxSignedInOptions()` (dialog).
   - If Error → Start OAuth2: `Auth.startOAuth2Authentication(this, APP_KEY)`.
3. Set `isDropboxAuthenticated = true` (flag for onResume).
4. Dropbox SDK opens browser → User signs in → Returns to app.
5. `onResume()` detects flag → Calls `dropboxClient.finishAuthentication()`.
6. If Success → Toast "Signed in" → `navigateToDropboxFolderPicker()`.

**Dialog Options** (when already signed in):
- "Select Folder" → `navigateToDropboxFolderPicker()`
- "Sign Out" → `signOutDropbox()` (clears tokens, updates status)
- "Cancel" → Dismiss dialog

### OneDrive

**Flow**:
1. User taps `cardOneDrive` → `authenticateOneDrive()`.
2. Check `oneDriveClient.testConnection()`:
   - If Success → `showOneDriveSignedInOptions()` (dialog).
   - If Error with "Interactive sign-in required":
     - Call `oneDriveClient.signIn(this) { result }`.
     - MSAL opens browser/system account picker → User signs in.
     - Callback receives `AuthResult` → If Success → `navigateToOneDriveFolderPicker()`.
3. If other error → Toast error message.

**Dialog Options** (when already signed in):
- "Select Folder" → `navigateToOneDriveFolderPicker()`
- "Sign Out" → `signOutOneDrive()` (clears MSAL cache, updates status)
- "Cancel" → Dismiss dialog

**Status Display** (`updateCloudStorageStatus()`):
- Google Drive: Shows email OR "Not connected".
- Dropbox: Restores from storage → Tests connection → Shows email OR "Not connected".
- OneDrive: Checks authentication → Tests connection → Shows email OR "Not connected".

---

## Local Folder Addition

### Scan Device Storage

**Trigger**: `btnScan` click.

**Logic**:
1. `viewModel.scanLocalFolders()` → UseCases enumerate external storage dirs.
2. Returns list of media folders (DCIM, Pictures, Movies, Downloads, etc.).
3. State update → `resourcesToAdd` contains `ResourceToAdd` objects.
4. Adapter displays list with:
   - Checkbox (selection state).
   - Editable name field.
   - "Add to Destinations" checkbox.
   - "Scan Subdirectories" checkbox.
   - "Read-Only Mode" checkbox.
   - Media type toggles (7 checkboxes: Image, Video, Audio, GIF, Text, PDF, EPUB).

**User Actions**:
- Toggle checkboxes → Callbacks update ViewModel state.
- Edit name → `onNameChanged` callback updates ViewModel.
- Click `btnAddToResources` → `viewModel.addSelectedResources()` → Saves to DB → `finish()`.

### Manual Folder Selection

**Trigger**: `btnAddManually` click.

**Logic**:
1. `folderPickerLauncher.launch(null)` → Opens system DocumentTree picker.
2. User selects folder → Returns URI (e.g., `content://com.android.externalstorage.documents/tree/primary:DCIM`).
3. Activity takes persistable URI permission:
   - Try read + write: `FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION`.
   - If SecurityException → Fallback to read-only: `FLAG_GRANT_READ_URI_PERMISSION`.
4. `viewModel.addManualFolder(uri)` → Creates `ResourceToAdd` with DocumentFile path.
5. Adapter displays newly added folder (same UI as scanned folders).
6. User configures → Clicks `btnAddToResources` → Saves to DB.

---

## SMB Share Addition

### Manual Entry

**Fields**:
- `etSmbServer`: IP/hostname (custom `IpAddressEditText` widget with validation).
- `etSmbShareName`: Share name (optional for testing, required for adding).
- `etSmbUsername`: Username (optional for guest access).
- `etSmbPassword`: Password.
- `etSmbDomain`: Windows domain (optional).
- `etSmbPort`: Port (default 445).
- `etSmbResourceName`: Custom resource name (optional, uses shareName if blank).
- `etSmbComment`: User comment (optional).
- `etSmbPinCode`: PIN for edit protection (optional).

**Media Type Checkboxes**: 7 checkboxes (Image, Video, Audio, GIF, Text, PDF, EPUB) initialized from global settings.

**Options**:
- `cbSmbAddToDestinations`: Add to quick copy/move destinations.
- `cbSmbReadOnlyMode`: Enable read-only mode (disables "Add to Destinations").

**Actions**:

1. **Test Connection** (`btnSmbTest` → `testSmbConnection()`):
   - Validate `etSmbServer` with `isValid()` method.
   - If server empty → Toast "Server address required".
   - If shareName empty → Tests server + lists all shares (in test result dialog).
   - If shareName provided → Tests specific share access.
   - Calls `viewModel.testSmbConnection(server, shareName, username, password, domain, port)`.
   - Result → `ShowTestResult` event → Dialog with scrollable message.

2. **Network Scan** (`btnScanNetwork` → Opens `NetworkDiscoveryDialog`):
   - Dialog scans LAN for SMB servers (ports 139, 445).
   - User selects discovered host → Auto-fills `etSmbServer`.
   - Toast shows hostname + open ports.

3. **Scan Shares** (`scanSmbShares()` - method exists but button removed):
   - Connects to server → Lists all visible shares.
   - Adds shares to `resourcesToAdd` list.
   - Adapter displays shares with selection checkboxes.

4. **Add Manually** (`btnSmbAddManually` → `addSmbResourceManually(isReadOnly)`):
   - Reads all fields.
   - Validates server address (custom widget validation).
   - Gets supported media types from checkboxes: `getSmbSupportedTypes()`.
   - Calls `viewModel.addSmbResourceManually(server, shareName, username, password, domain, port, resourceName, comment, addToDestinations, supportedTypes, isReadOnly)`.
   - ViewModel saves → Emits `ResourcesAdded` event → Activity finishes.

5. **Add Selected** (`btnSmbAddToResources` → `viewModel.addSelectedResources()`):
   - Adds all selected shares from scan results (if network scan was used).

### IP Address Auto-Fill

**Logic** (`setupIpAddressField()`):
1. Get device IP: `NetworkUtils.getLocalIpAddress(this)`.
2. Extract subnet: `deviceIp.substringBeforeLast(".") + "."` (e.g., "192.168.1.").
3. Pre-fill `etSmbServer` with subnet.
4. Set cursor to end of text for easy typing.

**Custom Widget Features** (`IpAddressEditText`):
- Input filter: Allow only digits and dots.
- Comma→dot replacement (for EU keyboards).
- Block 4th dot (max 3 dots).
- Block 4-digit numbers (max 255 per octet).
- Validation method: `isValid()` checks each octet in range [0-255].

---

## SFTP/FTP Addition

### Protocol Selection

**RadioGroup**: `rgProtocol` with 2 options:
- `rbSftp` (default): SFTP on port 22.
- `rbFtp`: FTP on port 21.

**Auto Port Update**:
- Switch to SFTP → Set port to 22 if empty or 21.
- Switch to FTP → Set port to 21 if empty or 22.

### Authentication Methods (SFTP only)

**RadioGroup**: `rgSftpAuthMethod` with 2 options:

1. **Password Auth** (`rbSftpPassword`):
   - Show `layoutSftpPasswordAuth` (username + password fields).
   - Hide `layoutSftpSshKeyAuth`.

2. **SSH Key Auth** (`rbSftpSshKey`):
   - Show `layoutSftpSshKeyAuth` (username + private key + passphrase fields).
   - Hide `layoutSftpPasswordAuth`.
   - User can:
     - Paste key manually into `etSftpPrivateKey`.
     - Load key file via `btnSftpLoadKey` → Opens file picker → Reads content.

**FTP**: Always uses password auth (SSH key UI hidden).

### Fields

**Common**:
- `etSftpHost`: IP/hostname (custom `IpAddressEditText` widget).
- `etSftpPort`: Port (default 22 for SFTP, 21 for FTP).
- `etSftpUsername`: Username.
- `etSftpPath`: Remote path (custom `UnixPathEditText` widget with auto-correction).
- `etSftpResourceName`: Custom name (optional).
- `etSftpComment`: User comment (optional).

**Password Auth**:
- `etSftpPassword`: Password.

**SSH Key Auth**:
- `etSftpPrivateKey`: Private key content (PEM format, multi-line).
- `etSftpKeyPassphrase`: Optional passphrase.

**Media Type Checkboxes**: 7 checkboxes initialized from global settings.

**Options**:
- `cbSftpAddToDestinations`: Add to destinations.
- `cbSftpReadOnlyMode`: Enable read-only mode (disables destinations).

### Actions

1. **Test Connection** (`btnSftpTest` → `testSftpConnection()`):
   - Validate `etSftpHost` with `isValid()` method.
   - If host empty → Toast "Host required".
   - Get protocol type from `rgProtocol`.
   - For SFTP:
     - If SSH key auth → Validate key not empty → `viewModel.testSftpConnectionWithKey(host, port, username, privateKey, passphrase)`.
     - If password auth → `viewModel.testSftpFtpConnection(SFTP, host, port, username, password)`.
   - For FTP:
     - `viewModel.testSftpFtpConnection(FTP, host, port, username, password)`.
   - Result → `ShowTestResult` event → Scrollable dialog.

2. **Add Resource** (`btnSftpAddResource` → `addSftpResource()`):
   - Validate host with custom widget.
   - Get normalized path from `etSftpPath.getNormalizedPath()` (auto-corrects backslashes, adds leading slash).
   - Get supported media types: `getSftpSupportedTypes()`.
   - Validate at least 1 media type selected.
   - For SFTP:
     - If SSH key auth → `viewModel.addSftpResourceWithKey(host, port, username, privateKey, passphrase, remotePath, resourceName, comment, supportedTypes)`.
     - If password auth → `viewModel.addSftpFtpResource(SFTP, host, port, username, password, remotePath, resourceName, comment, supportedTypes)`.
   - For FTP:
     - `viewModel.addSftpFtpResource(FTP, host, port, username, password, remotePath, resourceName, comment, supportedTypes)`.
   - ViewModel saves credentials → Saves resource → Emits `ResourcesAdded` event → Activity finishes.

3. **Load SSH Key** (`btnSftpLoadKey` → `sshKeyFilePickerLauncher`):
   - Opens file picker with `*/*` MIME type.
   - User selects key file → `loadSshKeyFromFile(uri)`.
   - Reads file content with `contentResolver.openInputStream()`.
   - Sets to `etSftpPrivateKey.setText(keyContent)`.
   - Toast "SSH key loaded".
   - If exception → Toast "Failed to load SSH key".

### Custom Widgets

**IpAddressEditText** (for host field):
- Same features as SMB server field (validation, comma→dot, max 3 dots, octet range [0-255]).

**UnixPathEditText** (for remote path):
- Input filter: Block backslashes (replace with forward slash).
- `getNormalizedPath()` method:
  - Ensures path starts with `/` (unless empty).
  - Converts `\` → `/`.
  - Removes duplicate slashes: `//` → `/`.
  - Returns normalized path OR `/` if empty.

---

## Copy Mode Behavior

**Trigger**: Intent extra `EXTRA_COPY_RESOURCE_ID` set.

**Flow**:
1. `onCreate` stores `copyResourceId` → Updates toolbar title to "Copy Resource".
2. `observeData` calls `viewModel.loadResourceForCopy(resourceId)`.
3. ViewModel:
   - Loads resource from DB.
   - Loads credentials from `NetworkCredentialsRepository`.
   - Emits `LoadResourceForCopy(resource, username, password, domain, sshKey, sshPassphrase)` event.
4. Activity receives event → Calls `helper.preFillResourceData(resource, username, password, domain, sshKey, sshPassphrase)`.
5. Helper auto-expands correct section (SMB/SFTP/Local) and fills all fields:
   - SMB: server, shareName, username, password, domain, port, resourceName, comment, checkboxes.
   - SFTP: host, port, username, password OR privateKey + passphrase, remotePath, resourceName, comment, checkboxes.
   - Local: path, resourceName, checkboxes.
6. User modifies fields as needed.
7. User clicks Add button → New resource created (original resource unchanged).

**Pre-Fill Details** (handled by AddResourceHelper):
- All text fields populated.
- Checkboxes set based on original resource:
  - Media type checkboxes (7 types).
  - "Add to Destinations" (if was destination).
  - "Scan Subdirectories" (if was scanning).
  - "Read-Only Mode" (if was read-only).
- Protocol radio button selected (SFTP vs FTP).
- Auth method radio button selected (Password vs SSH Key).

---

## ViewModel State & Events

### AddResourceState

```kotlin
data class AddResourceState(
    val resourcesToAdd: List<ResourceToAdd>,   // From scan OR manual add
    val selectedPaths: Set<String>             // User-selected items
)
```

### AddResourceEvent

```kotlin
sealed class AddResourceEvent {
    data class ShowError(val message: String) : AddResourceEvent()
    data class ShowMessage(val message: String) : AddResourceEvent()
    data class ShowTestResult(val message: String, val isSuccess: Boolean) : AddResourceEvent()
    data class LoadResourceForCopy(
        val resource: MediaResource,
        val username: String?,
        val password: String?,
        val domain: String?,
        val sshKey: String?,
        val sshPassphrase: String?
    ) : AddResourceEvent()
    object ResourcesAdded : AddResourceEvent()
}
```

---

## Adapter Callbacks (ResourceToAddAdapter)

**6 callbacks per adapter** (used by both Local and SMB adapters):

1. **onSelectionChanged(resource, selected)**: Toggle checkbox → Update `selectedPaths` in state.
2. **onNameChanged(resource, newName)**: Edit name field → Update resource name in state.
3. **onDestinationChanged(resource, isDestination)**: Toggle "Add to Destinations" → Update resource flag.
4. **onScanSubdirectoriesChanged(resource, scan)**: Toggle "Scan Subdirectories" → Update resource flag.
5. **onReadOnlyChanged(resource, readOnly)**: Toggle "Read-Only Mode" → Update resource flag (disables destinations checkbox).
6. **onMediaTypeToggled(resource, type)**: Toggle media type checkbox (IMAGE/VIDEO/AUDIO/GIF/TEXT/PDF/EPUB) → Update resource `supportedMediaTypes`.

**Adapter Methods**:
- `submitList(resources)`: Displays list with DiffUtil.
- `setSelectedPaths(paths)`: Updates checkbox states.

---

## Error Handling

### showError(message)

**Logic**:
1. Get settings: `viewModel.getSettings()`.
2. If `showDetailedErrors == true`:
   - Show scrollable dialog with full error message (using `DialogUtils.showScrollableDialog()`).
3. Else:
   - Show Toast (LENGTH_LONG).

**Error Sources**:
- Connection tests (SMB/SFTP/FTP/Cloud).
- Credential validation failures.
- Network timeouts.
- Authentication failures.

### showTestResultDialog(message, isSuccess)

**UI**:
- Title: "Connection Test Success" OR "Connection Test Failed".
- Content: Scrollable message (can be multi-line output from test).
- Button: "OK" (dismisses dialog).

**Called For**:
- SMB test results (including share lists).
- SFTP/FTP test results.
- SSH key validation results.

---

## Media Type Selection

### Global Settings Initialization

**When opening SMB/SFTP section**:
1. `viewModel.getSupportedMediaTypes()` retrieves global enabled types from settings.
2. Initialize checkboxes:
   - `cbSmbSupportImage.isChecked = IMAGE in supportedTypes`
   - `cbSmbSupportImage.isVisible = IMAGE in supportedTypes` (hide if disabled globally)
   - Repeat for all 7 media types (IMAGE, VIDEO, AUDIO, GIF, TEXT, PDF, EPUB).

**User Modifications**:
- User can uncheck any globally enabled type (to limit resource scope).
- User cannot check globally disabled types (checkbox invisible).

**Validation**:
- When adding resource: `getSmbSupportedTypes()` or `getSftpSupportedTypes()` reads checked states.
- If result empty → Toast "At least one media type required" → Abort add operation.

---

## Network Discovery Dialog

**Trigger**: `btnScanNetwork` click.

**Flow**:
1. Opens `NetworkDiscoveryDialog` (DialogFragment).
2. Dialog scans local network for SMB servers (ports 139, 445).
3. Displays list of discovered hosts with:
   - IP address.
   - Hostname (if resolvable).
   - Open ports.
4. User selects host → Dialog closes with callback.
5. Callback sets `etSmbServer.setText(host.ip)`.
6. Toast shows "Selected {hostname} (ports {ports})".

**Implementation**: Uses Java NIO sockets with timeout (500ms per IP).

---

## Key Behaviors & Edge Cases

### 1. Checkbox Interdependencies

**Read-Only Mode Disables Destinations**:
```kotlin
cbSmbReadOnlyMode.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) {
        cbSmbAddToDestinations.isChecked = false
        cbSmbAddToDestinations.isEnabled = false
    } else {
        cbSmbAddToDestinations.isEnabled = true
    }
}
```
Applied to Local, SMB, SFTP sections.

### 2. Port Auto-Update on Protocol Switch

```kotlin
binding.rgProtocol.setOnCheckedChangeListener { _, checkedId ->
    val currentPort = binding.etSftpPort.text.toString()
    when (checkedId) {
        binding.rbSftp.id -> {
            if (currentPort.isBlank() || currentPort == "21") {
                binding.etSftpPort.setText("22")
            }
        }
        binding.rbFtp.id -> {
            if (currentPort.isBlank() || currentPort == "22") {
                binding.etSftpPort.setText("21")
            }
        }
    }
}
```

### 3. DocumentTree Persistable Permissions

**Critical for Android 10+ scoped storage**:
```kotlin
try {
    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                   Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, takeFlags)
} catch (e: SecurityException) {
    // Fallback to read-only
    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
```
Without persistable permissions, URI becomes invalid after app restart.

### 4. Cloud Auth State Restoration

**Google Drive**:
- Uses `GoogleSignIn.getLastSignedInAccount(this)` (cached by Google Play Services).
- No manual restoration needed.

**Dropbox**:
- Uses `dropboxClient.tryRestoreFromStorage()` in `updateCloudStorageStatus()`.
- Reads OAuth2 token from EncryptedSharedPreferences.
- Tests connection to validate token not expired.

**OneDrive**:
- Uses `oneDriveClient.isAuthenticated()` (checks MSAL token cache).
- MSAL handles token refresh automatically.

### 5. SSH Key Format Support

**Supported Formats**:
- OpenSSH PEM (traditional `-----BEGIN RSA PRIVATE KEY-----`)
- OpenSSH new format (`-----BEGIN OPENSSH PRIVATE KEY-----`)
- PuTTY PPK (requires conversion to OpenSSH format externally)

**Passphrase Handling**:
- Optional field `etSftpKeyPassphrase`.
- If key is encrypted and passphrase empty → Connection test will fail.

### 6. Remote Path Normalization

**UnixPathEditText.getNormalizedPath()**:
```kotlin
fun getNormalizedPath(): String {
    val path = text.toString().trim()
        .replace('\\', '/')         // Backslash to forward slash
        .replace(Regex("/+"), "/")  // Remove duplicate slashes
    
    return when {
        path.isEmpty() -> "/"
        path.startsWith("/") -> path
        else -> "/$path"
    }
}
```

**Auto-Correction Examples**:
- User input: `\home\user\files` → Normalized: `/home/user/files`
- User input: `//share//folder//` → Normalized: `/share/folder/`
- User input: `folder` → Normalized: `/folder`
- User input: `` (empty) → Normalized: `/`

### 7. IP Address Validation

**Custom IpAddressEditText Widget**:
- **Input Filters**:
  - Allow only: `[0-9.]`
  - Replace comma with dot: `,` → `.`
  - Max 3 dots (blocks 4th dot input).
  - Max 3 digits per segment (blocks 4th digit).

- **Validation Method** (`isValid()`):
  ```kotlin
  fun isValid(): Boolean {
      val text = text.toString()
      if (text.isBlank()) return false
      
      val parts = text.split('.')
      if (parts.size != 4) return false
      
      return parts.all { part ->
          val num = part.toIntOrNull()
          num != null && num in 0..255
      }
  }
  ```

- **Visual Feedback**:
  - Invalid: Red border + error icon.
  - Valid: Normal border.

### 8. Orientation Change Handling

**Grid Layout Recalculation**:
```kotlin
private fun updateResourceTypeGridColumns() {
    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    binding.layoutResourceTypes.columnCount = if (isLandscape) 2 else 1
}
```

**Called**:
- In `setupViews()` during initialization.
- Automatically handled by configuration change (activity recreates with new orientation).

### 9. Test Connection Results

**SMB Test Without Share Name**:
- Tests server connectivity.
- Lists all visible shares with types (DISK, PRINTER, IPC, etc.).
- Result dialog shows multi-line list:
  ```
  Connection successful!
  
  Available shares:
  - MyShare (DISK)
  - Public (DISK)
  - IPC$ (IPC)
  - ADMIN$ (DISK)
  ```

**SMB Test With Share Name**:
- Tests specific share access.
- Result: "Successfully connected to \\\\server\\shareName"

**SFTP/FTP Test**:
- Connects to host.
- Lists files in root directory OR specified path.
- Result: "Connection successful! Files: [file1, file2, file3]"

---

## Complete User Workflows

### Add Local Folder (Scan Method)

1. Open AddResourceActivity (from MainActivity "+" button).
2. Tap `cardLocalFolder` → Local section expands.
3. Tap `btnScan` → ViewModel scans external storage.
4. RecyclerView displays found folders (e.g., DCIM, Pictures, Downloads).
5. User:
   - Checks desired folders.
   - Edits names (optional).
   - Enables "Add to Destinations" for some folders.
   - Toggles "Scan Subdirectories" (default: true).
   - Selects media types (default: all checked).
6. Tap `btnAddToResources` → ViewModel saves to DB.
7. Toast "X resources added" → Activity finishes → Return to MainActivity.
8. MainActivity reloads resources → New folders appear in list.

### Add Local Folder (Manual Method)

1. Tap `cardLocalFolder` → Local section expands.
2. Tap `btnAddManually` → System DocumentTree picker opens.
3. User navigates to desired folder → Selects folder.
4. Activity takes persistable URI permission.
5. Folder appears in RecyclerView (same UI as scanned folders).
6. User configures options (name, destination, media types).
7. Tap `btnAddToResources` → Saves → Finishes.

### Add SMB Share (Manual Entry)

1. Tap `cardNetworkFolder` → SMB section expands.
2. IP field pre-filled with device subnet (e.g., "192.168.1.").
3. User enters:
   - Server: `192.168.1.10`
   - Share name: `Media`
   - Username: `user` (optional for guest)
   - Password: `pass123`
4. Tap `btnSmbTest` → ViewModel tests connection.
5. Dialog shows "Connection successful!" OR error message.
6. User enters resource name (optional, defaults to share name).
7. User enables "Add to Destinations" (for quick copy target).
8. User selects media types (uncheck AUDIO if share is photos-only).
9. Tap `btnSmbAddManually` → ViewModel saves credentials + resource → Finishes.

### Add SMB Share (Network Scan)

1. Tap `cardNetworkFolder` → SMB section expands.
2. Tap `btnScanNetwork` → NetworkDiscoveryDialog opens.
3. Dialog scans LAN (192.168.1.1-255) for open SMB ports.
4. Found hosts displayed (IP + hostname + ports).
5. User taps host (e.g., "192.168.1.10 (NAS-Server)") → Dialog closes.
6. Server field auto-filled: `192.168.1.10`.
7. User enters credentials.
8. User can test connection OR scan shares (if button available).
9. If scanning shares: List of shares appears in RecyclerView.
10. User checks desired shares → Configures each → Taps `btnSmbAddToResources`.

### Add SFTP with Password

1. Tap `cardSftpFolder` → SFTP section expands.
2. Default: SFTP protocol (port 22), Password auth.
3. User enters:
   - Host: `192.168.1.20`
   - Username: `sftpuser`
   - Password: `securepass`
   - Remote path: `/data/media` (custom widget normalizes)
4. Tap `btnSftpTest` → ViewModel tests connection.
5. Dialog shows success + file list OR error.
6. User selects media types (e.g., only IMAGE and VIDEO).
7. Tap `btnSftpAddResource` → Saves → Finishes.

### Add SFTP with SSH Key

1. SFTP section expanded.
2. User selects `rbSftpSshKey` radio button.
3. Password field hidden, SSH key field shown.
4. User taps `btnSftpLoadKey` → File picker opens.
5. User selects private key file (e.g., `id_rsa`).
6. Key content loaded into `etSftpPrivateKey` (multi-line).
7. If key encrypted → User enters passphrase in `etSftpKeyPassphrase`.
8. User enters host, username, remote path.
9. Tap `btnSftpTest` → Tests with key authentication.
10. If success → User taps `btnSftpAddResource` → Saves key + passphrase encrypted.

### Add FTP Resource

1. Tap `cardSftpFolder` → SFTP section expands.
2. User selects `rbFtp` radio button → Port changes to 21.
3. SSH key auth hidden (FTP uses only password).
4. User enters:
   - Host: `ftp.example.com`
   - Port: `21`
   - Username: `ftpuser`
   - Password: `ftppass`
   - Remote path: `/public_html`
5. Tap `btnSftpTest` → Tests FTP connection (PASV mode, fallback to active mode).
6. Dialog shows success OR error (common: "PASV mode timeout, trying active mode").
7. Tap `btnSftpAddResource` → Saves as FTP resource.

### Google Drive Authentication & Folder Selection

1. Tap `cardCloudStorage` → Cloud section expands.
2. Status shows "Google Drive: Not connected".
3. Tap `cardGoogleDrive` → Google Sign-In activity opens.
4. User selects Google account → Grants permissions (Drive.FILE, Drive.METADATA).
5. Returns to AddResourceActivity → Toast "Signed in as user@gmail.com".
6. Status updated: "Connected as user@gmail.com".
7. Activity navigates to `GoogleDriveFolderPickerActivity`.
8. User browses Google Drive folders → Selects folder (e.g., "Photos").
9. Picker activity returns to MainActivity with folder metadata.
10. MainActivity creates resource record → User returns to MainActivity.

### Dropbox Authentication & Folder Selection

1. Cloud section expanded → Status: "Dropbox: Not connected".
2. Tap `cardDropbox` → Dropbox OAuth2 flow starts (browser opens).
3. User signs in → Grants app permissions → Browser redirects to app.
4. Activity resumes → `onResume()` calls `dropboxClient.finishAuthentication()`.
5. Toast "Signed in" → Activity navigates to `DropboxFolderPickerActivity`.
6. User browses Dropbox folders → Selects folder.
7. Picker returns to MainActivity → Resource created.

### OneDrive Authentication & Folder Selection

1. Cloud section expanded → Status: "OneDrive: Not connected".
2. Tap `cardOneDrive` → MSAL sign-in opens (system account picker OR browser).
3. User signs in with Microsoft account → Grants permissions.
4. Callback receives `AuthResult.Success` → Navigates to `OneDriveFolderPickerActivity`.
5. User browses OneDrive folders → Selects folder.
6. Picker returns to MainActivity → Resource created.

### Copy Existing Resource

1. In MainActivity, user long-presses resource card → Context menu → "Copy Resource".
2. Intent launched: `createIntent(context, copyResourceId = resource.id)`.
3. AddResourceActivity opens with title "Copy Resource".
4. ViewModel loads resource + credentials → Emits `LoadResourceForCopy` event.
5. Helper pre-fills all fields (section auto-expanded):
   - SMB: server, share, username, password, domain, port, name, comment, all checkboxes.
   - SFTP: host, port, username, password OR key, path, name, comment, checkboxes.
6. User modifies fields as needed (e.g., change resource name to "Copy of Media").
7. User taps Add button → New resource created (original unchanged).
8. Activity finishes → Return to MainActivity with 2 resources (original + copy).

---

## Testing Considerations

### Unit Tests

- **ViewModel**: Test state updates, event emissions, UseCase interactions (mocked).
- **Validation**: Test `IpAddressEditText.isValid()`, `UnixPathEditText.getNormalizedPath()`.
- **Media Type Logic**: Test `getSmbSupportedTypes()`, `getSftpSupportedTypes()` with various checkbox states.

### Integration Tests

- **SMB Connection**: Test with real SMB server (guest + authenticated access).
- **SFTP Connection**: Test password auth + SSH key auth (RSA, ECDSA, Ed25519 keys).
- **FTP Connection**: Test PASV + active mode (mock server).
- **Cloud Auth**: Test with sandbox accounts (Google, Dropbox, OneDrive test apps).

### UI Tests

- **Espresso**: Test section switching, button clicks, field validation, dialog interactions.
- **Screenshot Tests**: Capture all sections (Local, SMB, SFTP, Cloud) in portrait + landscape.
- **A11y Tests**: Verify content descriptions, touch target sizes (48dp minimum).

### Edge Cases

- **Empty Fields**: Verify validation prevents adding with missing required fields.
- **Invalid IP/Hostname**: Verify custom widget blocks invalid input, shows error state.
- **Connection Timeouts**: Mock slow network, verify 15s timeout with cancellation.
- **SSH Key Formats**: Test OpenSSH PEM, new format, encrypted keys with passphrase.
- **Persistable URI Loss**: Test behavior if URI permissions revoked by system.
- **Cloud Token Expiration**: Test refresh token flow when access token expires.

---

## Known Issues & Limitations

1. **Network Scan Speed**: LAN scan can take 30-60 seconds for full /24 subnet (256 IPs).
2. **SMB Guest Access**: Some servers block guest access, requires credentials even for public shares.
3. **FTP PASV Mode**: Timeouts common on home routers with NAT, active mode fallback implemented.
4. **SSH Key Formats**: PuTTY PPK not supported, user must convert to OpenSSH format.
5. **DocumentTree Picker**: System picker UI varies by device manufacturer, confusing for some users.
6. **Cloud Folder Depth**: Google Drive picker limited to 5 levels deep (API limitation).
7. **Orientation Change**: RecyclerView scroll position not preserved across orientation changes.
8. **Read-Only Detection**: SMB share permissions not auto-detected, user must manually enable read-only mode.

---

## Future Enhancements

- **Batch Edit**: Edit multiple resources' settings simultaneously.
- **Import/Export**: Export resource list + credentials to encrypted backup file.
- **QR Code Scan**: Scan QR code with SMB/SFTP credentials (for easy setup).
- **Auto-Discovery**: Continuous LAN monitoring for new SMB servers (background service).
- **Credential Templates**: Save username/password templates for reuse across resources.
- **Resource Groups**: Organize resources into folders/tags.
- **Advanced SMB Options**: Domain controller selection, SMB dialect version, DFS support.
- **SFTP Tunneling**: SSH tunneling for firewall traversal.
- **Cloud Quota Display**: Show remaining storage quota for cloud providers.
- **Resource Health Checks**: Periodic background ping to detect offline resources.

---
