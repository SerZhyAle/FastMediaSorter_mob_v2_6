# EditResourceActivity - Complete Behavior Specification

**File**: `ui/editresource/EditResourceActivity.kt` (719 lines)
**Architecture**: MVVM + TextWatcher Delegation
**Purpose**: Edit existing resource settings, credentials, and configuration

---

## Overview

EditResourceActivity provides comprehensive resource editing:
- **Resource Settings**: Name, comment, PIN protection, slideshow interval
- **Media Type Selection**: Toggle supported types (IMAGE/VIDEO/AUDIO/GIF/TEXT/PDF/EPUB)
- **Flags**: Scan subdirectories, disable thumbnails, read-only mode, destination status
- **Credentials Editing**: Update SMB/SFTP/FTP credentials with live validation
- **Connection Testing**: Verify credentials work before saving
- **Speed Testing**: Measure read/write speeds, display results with recommendations
- **Cloud Re-authentication**: Restore expired OAuth tokens (Google Drive only)
- **Trash Management**: View and clear `.trash/` folder contents

**File Size**: 719 lines with TextWatcher pattern for real-time updates

---

## Components

### 1. ViewBinding & Dependencies

```kotlin
private val viewModel: EditResourceViewModel by viewModels()
private lateinit var binding: ActivityEditResourceBinding

@Inject lateinit var googleDriveRestClient: GoogleDriveRestClient
```

**Only Google Drive client injected** - OneDrive/Dropbox re-auth not yet implemented in EditResourceActivity.

---

## Initialization

### init Block (ViewModel)

**ResourceId Extraction** (from SavedStateHandle):
1. Primary attempt: `savedStateHandle.get<Long>("resourceId")`
2. Fallback: `savedStateHandle.get<String>("resourceId")?.toLongOrNull()`
3. Default: `0L` if neither exists
4. **Immediate action**: Calls `loadResource()` after state initialization
5. Logs: `Timber.d("EditResourceViewModel initialized with resourceId: $resourceId")`

### loadResource Private Method

**Coroutine Setup**:
- `viewModelScope.launch(ioDispatcher + exceptionHandler)`
- Shows loading: `_loading.value = true`

**Resource Loading**:
1. Gets resource: `getResourcesUseCase.getById(resourceId)`
2. Validates: If null → Sends `ShowError` event, returns early
3. Logs: `Timber.d("Loaded resource: ${resource.name}, type: ${resource.type}")`

**Global Settings Loading**:
- `settingsRepository.getSettings().first()`
- Extracts: `textEnabled`, `pdfEnabled`, `epubEnabled`

**Credential Loading by Type**:
- **SMB**:
  - Calls `smbOperationsUseCase.getConnectionInfo(credentialsId).onSuccess { info -> ... }`
  - Path parsing:
    - Normalizes: `resourcePath.replace('\\', '/')`
    - Removes prefix: `.substring(6)` (removes "smb://")
    - Extracts share + subfolders: `substring(firstSlash + 1)`
  - Sets: `smbServer`, `smbShareName`, `smbUsername`, `smbPassword`, `smbDomain`, `smbPort`
  - Logs: `Timber.d("SMB credentials loaded for resource ${resource.name}")`
- **SFTP**:
  - Calls `getSftpCredentials(credentialsId).onSuccess { creds -> ... }`
  - Path parsing:
    - Removes prefix: `sftp://` or `ftp://`
    - Extracts path: Keeps leading `/`
  - Sets: `sftpHost`, `sftpPort`, `sftpUsername`, `sftpPassword`, `sftpPath`
  - Logs: `Timber.d("SFTP credentials loaded")`
- **FTP**: Same as SFTP

**Single State Update**:
- Updates state ONCE with all loaded data to prevent UI flickering
- Includes: `originalResource`, `currentResource`, credentials (SMB/SFTP), global settings
- Logs: `Timber.d("Resource state updated with all data")`

**Destinations Count Calculation**:
- Gets all resources: `resourceRepository.getFilteredResources()`
- Counts destinations: `allResources.count { it.isDestination }`
- Computes `canBeDestination` via helper:
  - If resource already destination: true (can toggle off)
  - If read-only: false
  - If destinationsCount >= maxDestinations: false
  - Else: true
- Logs: `Timber.d("Destinations count: $destinationsCount, canBeDestination: $canBeDestination")`

**Loading Complete**:
- `_loading.value = false`
- Error handling: Logs exception with `Timber.e()`

### 2. TextWatcher References (11 watchers)

**Purpose**: Store references for temporary removal during programmatic updates (prevents recursive triggering).

**SMB Credentials** (6 watchers):
- `smbServerWatcher`: Server IP/hostname → Calls `viewModel.updateSmbServer(s?.toString() ?: "")`
- `smbShareNameWatcher`: Share name → Calls `viewModel.updateSmbShareName(...)`
- `smbUsernameWatcher`: Username → Calls `viewModel.updateSmbUsername(...)`
- `smbPasswordWatcher`: Password → Calls `viewModel.updateSmbPassword(...)`
- `smbDomainWatcher`: Domain (optional) → Calls `viewModel.updateSmbDomain(...)`
- `smbPortWatcher`: Port (default 445) → Parses to int (fallback 445), calls `viewModel.updateSmbPort(port)`

**SFTP/FTP Credentials** (5 watchers):
- `sftpHostWatcher`: Host IP/hostname → Calls `viewModel.updateSftpHost(...)`
- `sftpPortWatcher`: Port (default 22 for SFTP, 21 for FTP) → Parses to int, calls `viewModel.updateSftpPort(port)`
- `sftpUsernameWatcher`: Username → Calls `viewModel.updateSftpUsername(...)`
- `sftpPasswordWatcher`: Password → Calls `viewModel.updateSftpPassword(...)`
- `sftpPathWatcher`: Remote path → Calls `viewModel.updateSftpPath(...)`

**addSmbListeners Method**:
1. Creates 6 TextWatchers
2. Stores references in properties
3. Attaches to EditTexts
4. Sets `IpAddressInputFilter` on server field

**removeSmbListeners Method**:
1. Removes all 6 TextWatchers using stored references
2. Called BEFORE updating fields in observeData to avoid re-trigger loops

**addSftpListeners / removeSftpListeners**: Same pattern with 5 watchers

### 3. ActivityResultLauncher

**googleSignInLauncher** (`StartActivityForResult`):
- Launches Google Sign-In for re-authentication.
- Processes result in `handleGoogleSignInResult()`.
- Used when cloud token expired (Google Drive only).

---

## Lifecycle Methods

### setupViews

**Toolbar**:
- Navigation icon click → `finish()` with slide-right animation.

**Slideshow Interval** (`etSlideshowInterval` - AutoCompleteTextView):
- Dropdown options: 1, 5, 10, 30, 60, 120, 300 seconds.
- **Item click**: `viewModel.updateSlideshowInterval(seconds)`.
- **Focus loss**: Validate manual input → Clamp to [1, 3600] → Update ViewModel.
- **Validation**: If out of range (< 1 or > 3600), auto-correct to clamped value.

**Media Type Checkboxes** (7 checkboxes):
- Each checkbox triggers `updateMediaTypes()` on change.
- `updateMediaTypes()`:
  - Collects checked states into `Set<MediaType>`.
  - Calls `viewModel.updateSupportedMediaTypes(types)`.

**Scan Subdirectories Checkbox**:
- Checked change → `viewModel.updateScanSubdirectories(isChecked)`.

**Disable Thumbnails Checkbox**:
- Checked change → `viewModel.updateDisableThumbnails(isChecked)`.
- Purpose: Improve performance on slow networks by skipping thumbnail generation.

**Read-Only Mode Checkbox**:
- Checked change → `viewModel.updateReadOnlyMode(isChecked)`.
- **Always enabled** (user can toggle even if resource not writable).
- If resource `!isWritable`, defaults to checked but user can still uncheck (it's a preference, not capability).

**Resource Name** (`etResourceName`):
- TextWatcher → `viewModel.updateName(text)` on afterTextChanged.

**Resource Comment** (`etResourceComment`):
- TextWatcher → `viewModel.updateComment(text)` on afterTextChanged.

**Access PIN** (`etAccessPassword`):
- TextWatcher → `viewModel.updateAccessPin(text)` on afterTextChanged.
- Null if blank (removes PIN).

**Is Destination Switch** (`switchIsDestination`):
- Checked change → `viewModel.updateIsDestination(isChecked)`.
- Enabled only if `state.canBeDestination` (not read-only, destination slots available).

**Clear Trash Button** (`btnClearTrash`):
- Click → `viewModel.requestClearTrash()`.
- Visible only if `state.hasTrashFolders` (resource has `.trash/` folder with files).

**Reset Button** (`btnReset`):
- Click → `viewModel.resetToOriginal()`.
- Reverts all changes to original resource state.
- Enabled only if `hasResourceChanges || hasSmbCredentialsChanges || hasSftpCredentialsChanges`.

**Test Button** (`btnTest`):
- Click → `viewModel.testConnection()`.
- Tests connection with current credentials (SMB/SFTP/FTP/Cloud).
- **Combined functionality**: Also runs speed test for network resources.

**Save Button** (`btnSave`):
- Click → `viewModel.saveChanges()`.
- Saves all changes to DB (resource + credentials).
- **Always enabled** (ViewModel validates before saving).

**SMB Listeners** (`addSmbListeners()`):
- Attach 6 TextWatchers to SMB credential fields.
- Each triggers `viewModel.update*()` method on afterTextChanged.
- `etSmbServerEdit`: Apply `IpAddressInputFilter()` (comma→dot, max 3 dots, 0-255 octets).

**SFTP Listeners** (`addSftpListeners()`):
- Attach 5 TextWatchers to SFTP/FTP credential fields.
- Each triggers `viewModel.update*()` method on afterTextChanged.
- `etSftpHostEdit`: Apply `IpAddressInputFilter()`.

### observeData

**State Collection** (3 coroutines):

#### 1. Main State Observer (`state.collect`)

**Toolbar Update**:
- Resource type label: Local/SMB/SFTP/FTP/Cloud (from R.string).
- **Toolbar icon by resource type**:
  - `LOCAL` → `ic_resource_local`
  - `SMB` → `ic_resource_smb`
  - `SFTP` → `ic_resource_sftp`
  - `FTP` → `ic_resource_ftp`
  - `CLOUD` → Provider-specific icon:
    - `CloudProvider.GOOGLE_DRIVE` → `ic_provider_google_drive`
    - `CloudProvider.ONEDRIVE` → `ic_provider_onedrive`
    - `CloudProvider.DROPBOX` → `ic_provider_dropbox`
    - Fallback: `ic_resource_cloud`
- Title: "Edit {Type} Resource" (e.g., "Edit SMB Resource", "Edit Google Drive Resource")

**Field Population** (with cursor position preservation):

All text fields use conditional update pattern:
```kotlin
if (binding.etResourceName.text.toString() != resource.name) {
    binding.etResourceName.setText(resource.name)
}
```
**Purpose**: Avoid resetting cursor position during user typing.

**Fields Updated**:
- `etResourceName`: Resource name
- `etResourceComment`: Comment
- `etAccessPassword`: PIN code (empty if null)
- `etResourcePath`: Path (read-only display)
- `tvCreatedDate`: Created date, formatted with `SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())`
- `tvFileCount`: 
  - If `resource.fileCount >= 1000` → Text ">1000"
  - Else: `fileCount.toString()`
- `tvLastBrowseDate`: 
  - Formats `resource.lastBrowseDate` with `SimpleDateFormat`
  - OR displays R.string.never_browsed if null
- `etSlideshowInterval`: Slideshow interval (seconds)

**Media Type Checkboxes**:
- Set checked state for each of 7 types based on `resource.supportedMediaTypes`.
- **Visibility by Layout**:
  - `layoutMediaTypesTextPdf.isVisible = state.isGlobalTextSupportEnabled OR state.isGlobalPdfSupportEnabled`
  - `cbSupportText.isVisible = state.isGlobalTextSupportEnabled`
  - `cbSupportPdf.isVisible = state.isGlobalPdfSupportEnabled`
  - `layoutMediaTypesEpub.isVisible = state.isGlobalEpubSupportEnabled`
- **Purpose**: Hide disabled types to avoid confusion (global settings control availability)

**Flags**:
- `cbScanSubdirectories.isChecked = resource.scanSubdirectories`
- `cbDisableThumbnails.isChecked = resource.disableThumbnails`
- `cbReadOnlyMode` **Complex Update**:
  - Temporarily removes listener
  - If `resource.isWritable = false`: Sets `isChecked = true` (force read-only, resource capability)
  - Else: Sets `isChecked = resource.isReadOnly` (respects user preference)
  - Always `isEnabled = true` (user can toggle, it's a preference)
  - Re-attaches listener
  - Logs: `Timber.d("Read-only mode checkbox updated: isChecked=${isChecked}, isEnabled=true")`

**Is Destination Switch** (Complex Update):
- Temporarily removes listener
- Sets `isEnabled = state.canBeDestination`
- Sets `isChecked = resource.isDestination AND state.canBeDestination`
- **Logic**: If cannot be destination (slots full, read-only) → Force unchecked + disabled
- Re-attaches listener
- Logs: `Timber.d("Destination switch updated: isChecked=${isChecked}, isEnabled=${isEnabled}")`

**Credentials Sections Visibility**:
- `layoutSmbCredentials.isVisible = resource.type == SMB`
- `layoutSftpCredentials.isVisible = resource.type == SFTP || resource.type == FTP`
- Credentials title:
  - SFTP: "SFTP Credentials"
  - FTP: "FTP Credentials"
  - SMB: "Credentials"

**Test Button Text**:
- Always: "Test Connection" (speed test combined into single button).

**Speed Test Results Card** (`cardSpeedResults`):
- Logs: `Timber.d("resource.type=${resource.type}, readSpeed=${resource.readSpeedMbps}, writeSpeed=${resource.writeSpeedMbps}")`
- **Visible if**: Network resource (`SMB/SFTP/FTP/CLOUD`) AND has speed test results (`readSpeedMbps != null || writeSpeedMbps != null`).
- Display:
  - `tvReadSpeed`: Read speed formatted `"%.2f Mbps".format(readSpeedMbps)` OR "-" if null
  - `tvWriteSpeed`: Write speed formatted `"%.2f Mbps".format(writeSpeedMbps)` OR "-" if null
  - `tvRecThreads`: `recommendedThreads.toString()` OR "-" if null
  - `tvLastTestDate`: "Last tested: {formatted date}" OR R.string.never_tested if null
- **Hidden if**: Local resource OR no speed test data.
- Logs: `Timber.d("Speed test card ${if (isVisible) "VISIBLE" else "HIDDEN"}")`

**Clear Trash Button Visibility**:
- `btnClearTrash.isVisible = state.hasTrashFolders`
- True if resource has `.trash/` folder with files.

**SMB Credentials Update** (with TextWatcher pause):
1. `removeSmbListeners()` - Detach all 6 watchers.
2. Update fields with conditional check (cursor preservation).
3. `addSmbListeners()` - Re-attach watchers.

**SFTP/FTP Credentials Update** (with TextWatcher pause):
1. `removeSftpListeners()` - Detach all 5 watchers.
2. Update fields with conditional check.
3. `addSftpListeners()` - Re-attach watchers.

**Button States**:
- `btnSave.isEnabled = true` (always enabled, ViewModel validates)
- `btnReset.isEnabled = hasResourceChanges || hasSmbCredentialsChanges || hasSftpCredentialsChanges`

#### 2. Loading State Observer (`loading.collect`)

**Progress Layout Visibility**:
- Show if `isLoading && !isTestingSpeed`.
- `layoutProgress.isVisible = isLoading`
- `progressBar.isVisible = true`
- `tvProgressMessage.text = "Loading..."`

#### 3. Speed Test State Observer (`state.collect` for speed test)

**Speed Test Progress**:
- Show if `state.isTestingSpeed`.
- `layoutProgress.isVisible = true`
- `progressBar.isVisible = true`
- `tvProgressMessage.text = state.speedTestStatus` OR "Analyzing speed..."
- Hide if `!isTestingSpeed && !loading.value`.

#### 4. Events Observer (`events.collect`)

**8 Event Types**:

1. **ShowError(message)**:
   - Toast error message (LENGTH_SHORT).

2. **ShowMessage(message)**:
   - Toast success message (LENGTH_SHORT).

3. **ShowMessageRes(messageResId, args)**:
   - Toast formatted string resource with arguments.
   - Example: `getString(R.string.resource_updated, resourceName)`

4. **ResourceUpdated**:
   - Toast "Resource updated".
   - `finish()` - Return to MainActivity.

5. **TestResult(message, success)**:
   - Call `showTestResultDialog(message, success)`.
   - AlertDialog with scrollable message + Copy button (copies to clipboard)
   - **If test successful AND credentials changed**:
     - Shows second dialog: R.string.save_credentials_title, R.string.save_credentials_message
     - Positive button: R.string.save_now → `viewModel.saveChanges()`
     - Negative button: R.string.later → Dismisses dialog
     - Purpose: Prompt user to save working credentials immediately to avoid re-entry
   - Logs: `Timber.d("Test result dialog shown: success=$success")`

6. **ConfirmClearTrash(count)**:
   - Call `showClearTrashConfirmDialog(count)`.
   - AlertDialog: R.string.clear_trash_confirm_title, R.string.clear_trash_confirm_message with file count
   - Positive button: R.string.clear_trash → `viewModel.clearTrash()`
   - Negative button: R.string.cancel → Dismisses
   - Shows count: "Delete {count} files from trash?"
   - Logs: `Timber.d("Clear trash confirmation dialog shown for $count files")`

7. **TrashCleared(count)**:
   - Toast "Trash cleared: {count} files".

8. **RequestCloudReAuthentication**:
   - Call `showCloudReAuthenticationDialog()`.
   - AlertDialog: R.string.restore_connection, R.string.cloud_auth_dialog_message
   - Positive button: R.string.sign_in_now → Dismisses dialog, calls `launchGoogleSignIn()`
   - Negative button: R.string.cancel → Dismisses
   - **Google Drive Only**: Re-authentication via `googleSignInLauncher`
   - OneDrive/Dropbox: Not yet implemented in EditResourceActivity
   - Logs: `Timber.d("Cloud re-authentication dialog shown")`

---

## Key Behaviors

### 1. TextWatcher Pattern (Cursor Position Preservation)

**Problem**: Programmatic `setText()` during user typing resets cursor to end.

**Solution**:
1. Store TextWatcher references.
2. Remove watchers before programmatic updates.
3. Use conditional update: `if (currentText != newText) { setText(newText) }`.
4. Re-add watchers after update.

**Example**:
```kotlin
removeSmbListeners()
if (binding.etSmbServerEdit.text.toString() != state.smbServer) {
    binding.etSmbServerEdit.setText(state.smbServer)
}
addSmbListeners()
```

### 2. Slideshow Interval Validation

**Constraints**: [1, 3600] seconds (1 second to 1 hour).

**Input Methods**:
- **Dropdown selection**: Predefined values (1, 5, 10, 30, 60, 120, 300).
- **Manual input**: Typed by user.

**Validation on Focus Loss**:
```kotlin
binding.etSlideshowInterval.setOnFocusChangeListener { _, hasFocus ->
    if (!hasFocus) {
        val text = binding.etSlideshowInterval.text.toString()
        val seconds = text.toIntOrNull() ?: 5
        val clampedSeconds = seconds.coerceIn(1, 3600)
        if (seconds != clampedSeconds) {
            binding.etSlideshowInterval.setText(clampedSeconds.toString(), false)
        }
        viewModel.updateSlideshowInterval(clampedSeconds)
    }
}
```

**Auto-Correction**:
- Input: "-5" → Clamped: "1"
- Input: "5000" → Clamped: "3600"
- Input: "abc" → Parsed: null → Default: "5"

### 3. Media Type Selection with Global Settings

**Initialization**:
- Checkboxes visible only if type enabled globally in settings.
- Example: If PDF support disabled in settings → `cbSupportPdf.isVisible = false`.

**Update Logic**:
```kotlin
private fun updateMediaTypes() {
    val types = mutableSetOf<MediaType>()
    if (binding.cbSupportImages.isChecked) types.add(MediaType.IMAGE)
    if (binding.cbSupportVideo.isChecked) types.add(MediaType.VIDEO)
    // ... repeat for all 7 types
    viewModel.updateSupportedMediaTypes(types)
}
```

**Validation**: ViewModel enforces at least 1 type selected (prevents empty set).

### 4. Read-Only Mode Logic

**Checkbox Behavior**:
- **Always enabled**: User can toggle even for non-writable resources.
- **Default State**:
  - If `resource.isWritable == false` → Checked (read-only enforced).
  - Else → Uses `resource.isReadOnly` value.

**Purpose**: Allow user to manually restrict write operations even if resource is writable.

**Interaction with Destination Switch**:
- When read-only checked → Destination switch disabled + unchecked.
- Reason: Read-only resources cannot be copy/move targets.

### 5. Destination Slot Management

**Constraint**: Max 10 destination resources.

**Logic**:
```kotlin
binding.switchIsDestination.isEnabled = state.canBeDestination
binding.switchIsDestination.isChecked = resource.isDestination && state.canBeDestination
```

**State Calculation** (in ViewModel):
- `canBeDestination = !isReadOnly && (isDestination || destinationCount < 10)`
- If already destination → Can uncheck (doesn't consume slot).
- If not destination → Can check only if slots available.

### 6. Connection Testing

**Trigger**: `btnTest` click → `viewModel.testConnection()`.

**Protocols Tested**:
- **SMB**: Connects with credentials → Lists shares OR tests specific share access.
- **SFTP**: Connects with password/key → Lists root directory files.
- **FTP**: Connects with password → Lists root directory files (PASV mode, fallback to active).
- **Cloud**: Tests OAuth token validity → Lists folder (requires re-auth if expired).

**Result Display**:
- `showTestResultDialog(message, success)`:
  - Title: "Connection Test Success" OR "Connection Test Failed".
  - Message: Scrollable text (multi-line output).
  - Buttons: "OK", "Copy" (copies message to clipboard).

**Speed Test Integration**:
- For network resources: Test also measures read/write speeds.
- Creates temp test file (1MB) → Uploads → Downloads → Deletes.
- Updates resource record with `readSpeedMbps`, `writeSpeedMbps`, `recommendedThreads`, `lastSpeedTestDate`.
- Results displayed in `cardSpeedResults`.

**Credentials Auto-Save Prompt**:
- If test successful AND credentials changed → Show dialog:
  - "Credentials updated. Save now?"
  - Buttons: "Save Now" (calls `viewModel.saveChanges()`), "Later".

### 7. Cloud Re-Authentication (Google Drive Only)

**Trigger**: `RequestCloudReAuthentication` event.

**Flow**:
1. Dialog: "Sign in to restore connection?"
2. User taps "Sign In Now" → `launchGoogleSignIn()`.
3. `googleSignInLauncher` launches `googleDriveRestClient.getSignInIntent()`.
4. User signs in → Returns to app → `handleGoogleSignInResult(data)`.
5. Parse account from `GoogleSignIn.getSignedInAccountFromIntent(data)`.
6. Call `googleDriveRestClient.handleSignInResult(account)`.
7. Result:
   - `AuthResult.Success`: Toast "Authentication successful: {email}".
   - `AuthResult.Error`: Toast "Authentication failed: {message}".
   - `AuthResult.Cancelled`: Toast "Cancelled".

**Limitation**: OneDrive/Dropbox re-auth not yet implemented in EditResourceActivity.

### 8. Trash Management

**Clear Trash Button**:
- Visible if `state.hasTrashFolders` (resource has `.trash/` folder with files).
- Click → `viewModel.requestClearTrash()` → Emits `ConfirmClearTrash(count)` event.

**Confirmation Dialog**:
- Title: "Clear Trash?"
- Message: "Delete {count} files from trash? This action cannot be undone."
- Buttons: "Clear Trash" (calls `viewModel.clearTrash()`), "Cancel".

**Execution**:
- ViewModel: Delete all files in `.trash/` folder.
- Event: `TrashCleared(count)` → Toast "Trash cleared: {count} files".

**Use Case**: Free space after many soft-deletes (undo operations create `.trash/` files).

### 9. Credentials Update Pattern

**SMB Credentials** (6 fields):
- Server, Share Name, Username, Password, Domain, Port.
- Each field has TextWatcher → Updates ViewModel state.
- ViewModel stores in `EditResourceState`:
  ```kotlin
  data class EditResourceState(
      val smbServer: String,
      val smbShareName: String,
      val smbUsername: String,
      val smbPassword: String,
      val smbDomain: String,
      val smbPort: Int,
      val hasSmbCredentialsChanges: Boolean
  )
  ```
- `hasSmbCredentialsChanges`: True if any field differs from original credentials.

**SFTP/FTP Credentials** (5 fields):
- Host, Port, Username, Password, Path.
- Same pattern as SMB.
- `hasSftpCredentialsChanges`: True if any field differs from original.

**Save Logic**:
- `btnSave` enabled always.
- Click → `viewModel.saveChanges()`:
  - Validate: At least 1 media type selected.
  - Update resource in DB.
  - If credentials changed → Update `NetworkCredentialsRepository`.
  - Emit `ResourceUpdated` event → Activity finishes.

### 10. Reset Functionality

**Trigger**: `btnReset` click → `viewModel.resetToOriginal()`.

**Behavior**:
- Reload original resource from DB.
- Reset all fields to original values:
  - Resource settings (name, comment, PIN, slideshow, media types, flags).
  - SMB credentials (server, share, username, password, domain, port).
  - SFTP credentials (host, port, username, password, path).
- Clear change flags: `hasResourceChanges = false`, `hasSmbCredentialsChanges = false`, `hasSftpCredentialsChanges = false`.
- Disable Reset button (no changes to revert).

**Button State**:
- `btnReset.isEnabled = hasResourceChanges || hasSmbCredentialsChanges || hasSftpCredentialsChanges`
- Enabled only when there are unsaved changes.

### 11. IP Address Input Filtering

**Applied To**:
- `etSmbServerEdit` (SMB server field).
- `etSftpHostEdit` (SFTP/FTP host field).

**Filter**: `IpAddressInputFilter()` (custom InputFilter).

**Features**:
- Allow only: `[0-9.]`
- Replace comma with dot: `,` → `.`
- Block 4th dot (max 3 dots).
- Block 4-digit numbers (max 255 per octet).

**Purpose**: Prevent invalid IP addresses during typing.

---

## ViewModel State & Events

### EditResourceState

```kotlin
data class EditResourceState(
    val currentResource: MediaResource?,
    
    // Change flags
    val hasResourceChanges: Boolean,
    val hasSmbCredentialsChanges: Boolean,
    val hasSftpCredentialsChanges: Boolean,
    
    // Flags
    val canBeDestination: Boolean,
    val hasTrashFolders: Boolean,
    val isGlobalTextSupportEnabled: Boolean,
    val isGlobalPdfSupportEnabled: Boolean,
    val isGlobalEpubSupportEnabled: Boolean,
    
    // Speed test
    val isTestingSpeed: Boolean,
    val speedTestStatus: String,
    
    // SMB credentials
    val smbServer: String,
    val smbShareName: String,
    val smbUsername: String,
    val smbPassword: String,
    val smbDomain: String,
    val smbPort: Int,
    
    // SFTP/FTP credentials
    val sftpHost: String,
    val sftpPort: Int,
    val sftpUsername: String,
    val sftpPassword: String,
    val sftpPath: String
)
```

### EditResourceEvent

```kotlin
sealed class EditResourceEvent {
    data class ShowError(val message: String) : EditResourceEvent()
    data class ShowMessage(val message: String) : EditResourceEvent()
    data class ShowMessageRes(val messageResId: Int, val args: List<Any> = emptyList()) : EditResourceEvent()
    object ResourceUpdated : EditResourceEvent()
    data class TestResult(val message: String, val success: Boolean) : EditResourceEvent()
    data class ConfirmClearTrash(val count: Int) : EditResourceEvent()
    data class TrashCleared(val count: Int) : EditResourceEvent()
    object RequestCloudReAuthentication : EditResourceEvent()
}
```

---

## ViewModel Methods

### updateCurrentResource (Private)

**Purpose**: Central method for all resource updates.

**Logic**:
1. Updates `currentResource` in state with new value
2. Calls `checkHasChanges()` to update change flags
3. Recomputes `canBeDestination` via helper method
4. Single state emission with all updates
5. Logs: `Timber.d("Current resource updated, hasChanges=$hasChanges")`

**Called By**: All 13 update methods (`updateName`, `updateComment`, `updateAccessPin`, etc.)

### checkHasChanges (Private)

**Purpose**: Compares current state with original resource to determine if save needed.

**Resource Comparison**:
- Compares: `name`, `comment`, `accessPassword`, `slideshowInterval`, `supportedMediaTypes`
- Flags: `scanSubdirectories`, `disableThumbnails`, `isReadOnly`, `isDestination`
- Sets `hasResourceChanges = true` if any field differs

**Credentials Comparison**:
- **SMB**: Compares `server`, `shareName`, `username`, `password`, `domain`, `port`
  - Gets original via `smbOperationsUseCase.getConnectionInfo(credentialsId)`
  - Sets `hasSmbCredentialsChanges = true` if differs
- **SFTP/FTP**: Compares `host`, `port`, `username`, `password`, `path`
  - Gets original via `getSftpCredentials(credentialsId)` or `getFtpCredentials(credentialsId)`
  - Sets `hasSftpCredentialsChanges = true` if differs

**State Update**:
- Updates `hasChanges` flag (OR of all 3 change flags)
- Single emission with all flags updated
- Logs: `Timber.d("Changes detected: resource=$hasResourceChanges, smb=$hasSmbCredentialsChanges, sftp=$hasSftpCredentialsChanges")`

### computeCanBeDestination (Private)

**Purpose**: Determines if resource can be toggled as destination.

**Logic**:
1. If `currentResource.isDestination = true` → Returns `true` (can toggle off)
2. If `currentResource.isReadOnly = true` → Returns `false` (read-only cannot be destination)
3. Gets all resources: `resourceRepository.getFilteredResources()`
4. Counts destinations: `allResources.count { it.isDestination }`
5. If `destinationsCount >= maxDestinations` (10) → Returns `false` (slots full)
6. Else → Returns `true`
7. Logs: `Timber.d("canBeDestination computed: $result (destinations: $destinationsCount/$maxDestinations)")`

**Called By**: `updateCurrentResource` after every change

### saveChanges

**Purpose**: Saves all changes to database atomically.

**Validation**:
1. Checks `currentResource` exists (returns early if null)
2. Validates at least one media type selected:
   - If `supportedMediaTypes.isEmpty()` → Sends `ShowError("Select at least one media type")`, returns

**Save Resource**:
- Calls `updateResourceUseCase.update(currentResource)`
- Updates: All fields (name, comment, PIN, slideshow, mediaTypes, flags)
- Logs: `Timber.d("Resource saved: ${currentResource.name}")`

**Save Credentials** (if changed):
- **SMB** (if `hasSmbCredentialsChanges`):
  - Creates `ConnectionInfo` with current credentials
  - Calls `smbOperationsUseCase.saveConnectionInfo(credentialsId, connectionInfo)`
  - Encrypts via Android Keystore AES-256-GCM
  - Logs: `Timber.d("SMB credentials saved")`
- **SFTP** (if `hasSftpCredentialsChanges`):
  - Creates `SftpCredentials` object
  - Calls `saveSftpCredentials(credentialsId, credentials)`
  - Logs: `Timber.d("SFTP credentials saved")`
- **FTP**: Same as SFTP with `FtpCredentials`

**Completion**:
- Sends `ResourceUpdated` event → Activity finishes
- Logs: `Timber.d("Save completed successfully")`

**Error Handling**:
- Catches exceptions in `exceptionHandler`
- Sends `ShowError` event with message
- Logs: `Timber.e(exception, "Failed to save resource changes")`

### resetToOriginal

**Purpose**: Reverts all changes to original resource state.

**Logic**:
1. Copies `originalResource` to `currentResource`
2. Resets all credentials to original values (SMB/SFTP)
3. Calls `checkHasChanges()` → Sets all change flags to `false`
4. Single state update with reverted data
5. Logs: `Timber.d("Resource reset to original state")`

**UI Effect**: Disables `btnReset` (no changes detected)

---

## Complete User Workflows

### Edit Resource Name & Comment

1. Open EditResourceActivity (from MainActivity → Long press resource → Edit).
2. Change `etResourceName`: "My Photos" → "Family Photos".
3. Add `etResourceComment`: "Summer vacation 2025".
4. `btnReset` becomes enabled (has changes).
5. Tap `btnSave` → ViewModel validates → Updates DB.
6. Toast "Resource updated" → Activity finishes → Return to MainActivity.
7. MainActivity reloads resources → Name changed in list.

### Change Media Types

1. EditResourceActivity open with SMB resource.
2. Currently supports: IMAGE, VIDEO, AUDIO, GIF.
3. User unchecks AUDIO (only photos/videos needed).
4. Checkbox change → `updateMediaTypes()` → ViewModel updates.
5. Tap `btnSave` → Saves → Finishes.
6. Next browse: Only images, videos, GIFs shown.

### Update SMB Credentials

1. EditResourceActivity open with SMB resource.
2. SMB credentials section visible.
3. User updates password: "oldpass" → "newpass123".
4. TextWatcher → `viewModel.updateSmbPassword("newpass123")`.
5. `hasSmbCredentialsChanges = true` → `btnReset` enabled.
6. User taps `btnTest` → Tests with new password.
7. Dialog: "Connection Test Success".
8. Dialog prompt: "Save credentials now?" → User taps "Save Now".
9. ViewModel saves resource + credentials → Finishes.

### Test Connection with Auto-Save Prompt

1. EditResourceActivity open with SFTP resource.
2. User changes username: "user1" → "user2".
3. User changes password: "pass1" → "pass2".
4. User taps `btnTest` → ViewModel tests connection.
5. Progress: "Testing connection...".
6. Result: "Connection Test Success!" (with file list).
7. Dialog appears: "Credentials updated. Save now?"
8. User taps "Save Now" → Credentials saved → Activity finishes.

**Alternative**: User taps "Later" → Dialog closes → Can continue editing.

### Run Speed Test

1. EditResourceActivity open with SMB resource.
2. Speed test results card hidden (no previous test).
3. User taps `btnTest` → Connection test + speed test combined.
4. Progress messages:
   - "Testing connection..."
   - "Analyzing speed..."
   - "Testing read speed..."
   - "Testing write speed..."
5. Test creates 1MB temp file → Uploads → Downloads → Deletes → Measures times.
6. Result dialog: "Connection Test Success! Read: 45.2 Mbps, Write: 32.1 Mbps".
7. Speed test results card becomes visible:
   - Read Speed: 45.20 Mbps
   - Write Speed: 32.10 Mbps
   - Recommended Threads: 3
   - Last Tested: 2026-01-06 14:23:15
8. User can run test again anytime (updates results).

### Toggle Read-Only Mode

1. EditResourceActivity open with writable SMB resource.
2. Currently: `cbReadOnlyMode` unchecked, `switchIsDestination` enabled + checked.
3. User checks `cbReadOnlyMode`.
4. ViewModel: `updateReadOnlyMode(true)`.
5. State: `canBeDestination = false` (read-only resources cannot be destinations).
6. UI: `switchIsDestination` disabled + unchecked.
7. User taps `btnSave` → Saves → Finishes.
8. Next usage: Resource shows read-only in MainActivity, cannot be used as copy/move target.

### Clear Trash Folder

1. EditResourceActivity open with resource that has deleted files.
2. `btnClearTrash` visible at bottom (red button).
3. User taps button → ViewModel emits `ConfirmClearTrash(count=15)` event.
4. Dialog: "Clear Trash? Delete 15 files from trash? This action cannot be undone."
5. User taps "Clear Trash" → ViewModel:
   - Scans `.trash/` folder.
   - Deletes all files.
   - Emits `TrashCleared(count=15)` event.
6. Toast: "Trash cleared: 15 files".
7. `btnClearTrash` becomes hidden (no trash files remaining).

### Google Drive Re-Authentication

1. EditResourceActivity open with Google Drive resource.
2. Token expired (not detected initially).
3. User taps `btnTest` → ViewModel tests connection.
4. Result: Token invalid → Emits `RequestCloudReAuthentication` event.
5. Dialog: "Sign in to restore connection?"
6. User taps "Sign In Now" → `launchGoogleSignIn()`.
7. Google Sign-In activity opens → User selects account → Grants permissions.
8. Returns to EditResourceActivity → `handleGoogleSignInResult()`.
9. Result: `AuthResult.Success` → Toast "Authentication successful: user@gmail.com".
10. User can now browse resource (token refreshed).

### Reset All Changes

1. EditResourceActivity open with multiple changes:
   - Name changed: "Photos" → "My Photos".
   - Comment added: "Family photos".
   - PIN added: "1234".
   - SMB password changed.
   - Media types changed (unchecked AUDIO).
2. `btnReset` enabled (has changes).
3. User taps `btnReset` → `viewModel.resetToOriginal()`.
4. All fields revert to original values:
   - Name: "Photos"
   - Comment: "" (empty)
   - PIN: "" (empty)
   - SMB password: original value
   - Media types: all 7 checked
5. `btnReset` disabled (no changes).
6. User can re-edit or tap `btnSave` with no changes (no-op).

### Change Destination Status

1. EditResourceActivity open with writable local resource.
2. Currently: Not a destination (8 destinations exist, limit 10).
3. User enables `switchIsDestination`.
4. ViewModel: `updateIsDestination(true)`.
5. User taps `btnSave` → Destination count increases to 9.
6. MainActivity: Resource shows in "Destinations" list.

**Edge Case**: If 10 destinations exist:
1. Switch disabled (`canBeDestination = false`).
2. User cannot enable destination status.
3. Must disable another destination first (in its EditResourceActivity).

### Edit SFTP Resource Path

1. EditResourceActivity open with SFTP resource.
2. Current path: `/data/media`.
3. User changes path: `/home/user/photos`.
4. TextWatcher → `viewModel.updateSftpPath("/home/user/photos")`.
5. `hasSftpCredentialsChanges = true` → `btnReset` enabled.
6. User taps `btnTest` → Tests connection to new path.
7. Dialog: "Connection Test Success!" (with file list from new path).
8. User taps "Save Now" in auto-save prompt → Saves → Finishes.

### Change Slideshow Interval

1. EditResourceActivity open.
2. Current interval: 5 seconds.
3. User taps `etSlideshowInterval` dropdown → Selects "30".
4. Dropdown item click → `viewModel.updateSlideshowInterval(30)`.
5. User taps `btnSave` → Saves.
6. Next slideshow in PlayerActivity: Images advance every 30 seconds.

**Manual Input**:
1. User types "120" directly into field.
2. Field loses focus → Validates: 120 ∈ [1, 3600] ✓
3. `viewModel.updateSlideshowInterval(120)`.
4. Save → Slideshow uses 2-minute interval.

**Invalid Input**:
1. User types "9999" (too high).
2. Field loses focus → Validates: 9999 > 3600 → Clamps to 3600.
3. Field updates: "3600" (auto-correction).
4. `viewModel.updateSlideshowInterval(3600)`.

---

## Testing Considerations

### Unit Tests

- **ViewModel**: Test state updates, change flag logic, validation.
- **TextWatcher Pattern**: Verify no recursive triggering during programmatic updates.
- **Slideshow Validation**: Test clamping for boundary values (0, 1, 3600, 3601, -5).
- **Destination Logic**: Test slot availability calculations (0-10 destinations).

### Integration Tests

- **Connection Tests**: Mock SMB/SFTP/FTP/Cloud clients, verify test results.
- **Speed Tests**: Mock file operations, verify speed calculations.
- **Credentials Save**: Verify encrypted storage in NetworkCredentialsRepository.
- **Trash Clear**: Create temp `.trash/` folder, verify deletion.

### UI Tests

- **Espresso**: Test field updates, button states, dialog interactions.
- **Screenshot Tests**: Capture all resource types (Local/SMB/SFTP/FTP/Cloud) with different states.
- **TextWatcher Behavior**: Verify cursor position preserved during typing + programmatic updates.

### Edge Cases

- **Empty Media Types**: Verify ViewModel prevents saving with no types selected.
- **Read-Only + Destination**: Verify destination switch disabled when read-only enabled.
- **Expired Cloud Token**: Mock expired token, verify re-auth flow triggers.
- **Trash Folder Missing**: Verify Clear Trash button hidden if no `.trash/` folder.
- **Concurrent Edits**: Test behavior if resource deleted/modified in another activity.

---

## Known Issues & Limitations

1. **OneDrive/Dropbox Re-Auth**: Not yet implemented in EditResourceActivity (only Google Drive).
2. **Speed Test Accuracy**: Network conditions affect results, single measurement may not be representative.
3. **TextWatcher Cursor Jump**: Rare edge case if state update happens during user typing (mitigated but not 100% solved).
4. **Trash Folder Size**: No size limit shown, user doesn't know how much space trash consumes.
5. **SSH Key Editing**: SFTP SSH key not editable (must delete + re-add resource to change key).
6. **Credentials Visibility**: Password fields use `inputType=textPassword` but no toggle to show/hide (use endIconMode in layout but not hooked up).
7. **Port Validation**: No validation for port range (user can enter invalid ports like 0 or 99999).
8. **Domain Field**: SMB domain field always shown but rarely needed (could be optional with expand button).

---

## Future Enhancements

- **Batch Edit**: Edit multiple resources simultaneously (common settings like media types).
- **Credentials Import/Export**: Export encrypted credentials to backup file.
- **Advanced Speed Test**: Multiple runs with average, graph over time.
- **Trash Folder Stats**: Show total size, oldest file, newest file.
- **SSH Key Viewer**: Display SSH key fingerprint, allow replacement without deleting resource.
- **Port Presets**: Dropdown for common ports (21, 22, 139, 445, 2049).
- **Connection History**: Log test results over time, show success rate.
- **Automatic Backups**: Auto-backup resource settings before save.
- **Cloud Provider Switch**: Allow changing Google Drive folder without deleting resource.
- **Credentials Strength Meter**: Visual indicator for password strength.

---
