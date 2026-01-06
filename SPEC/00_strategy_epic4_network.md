# Tactical Plan: Epic 4 - Network Protocols

**Goal**: Extend file management to remote servers.
**Deliverable**: SMB, SFTP, and FTP support with local-like experience.
**Prerequisite**: Epic 3 Complete.

---

## 1. Security & Credentials

### 1.1 Secure Storage
- **Action**: Implement `NetworkCredentialsRepository`.
- **Implementation**:
  - Use `EncryptedSharedPreferences` (Jetpack Security).
  - Store: ID, Host, Username, Password, KeyPath.

### 1.2 Credential Management UI
- **Action**: Update `AddResourceActivity`.
- **Features**:
  - Add fields for User/Pass/Port based on Resource Type.
  - "Test Connection" button.
  - **Network Type Detection (WiFi vs Mobile Data)**:
    - **Rationale**: Warn users when connecting to network resources over mobile data (potential data charges).
    - **Implementation**:
      ```kotlin
      @Singleton
      class NetworkTypeMonitor @Inject constructor(
          @ApplicationContext private val context: Context
      ) {
          fun getCurrentNetworkType(): NetworkType {
              val cm = context.getSystemService<ConnectivityManager>()
              val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
              
              return when {
                  capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                  capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.MOBILE
                  capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                  else -> NetworkType.NONE
              }
          }
      }
      ```
    - **UI Warning**:
      ```kotlin
      fun checkNetworkBeforeConnection() {
          if (networkTypeMonitor.getCurrentNetworkType() == NetworkType.MOBILE) {
              MaterialAlertDialogBuilder(this)
                  .setTitle(R.string.mobile_data_warning_title)
                  .setMessage(R.string.mobile_data_warning_message)
                  .setPositiveButton(R.string.continue_anyway) { _, _ ->
                      proceedWithConnection()
                  }
                  .setNegativeButton(R.string.cancel, null)
                  .show()
          } else {
              proceedWithConnection()
          }
      }
      ```
    - **Settings Option**: "Warn when using mobile data" (default: enabled).

---

## 2. SMB Implementation (Samba)

### 2.1 Dependencies
- **Action**: Add `smbj`.
- **Config**: Ensure compatibility with Android (disable certain Java checks if needed).

### 2.2 SMB Client
- **Action**: Implement `SmbClient` wrapper.
- **Login**: `connect(host, auth)`.
- **Logic**: Use Connection Pooling (keep session alive for X seconds).

### 2.3 SMB Operations
- **Action**: Implement `SmbOperationStrategy` and `SmbMediaScanner`.
- **Scanner**: List files using SMBJ `diskShare.list(path)`.
- **Operations**:
  - Streaming input stream for playback.
  - Copy/Move/Delete implementations.

---

## 3. SFTP Implementation (SSH)

### 3.1 Dependencies
- **Action**: Add `sshj`.
- **Config**: BouncyCastle provider if necessary for older algorithms.

### 3.2 SFTP Client
- **Action**: Implement `SftpClient` wrapper.
- **Auth**: Support Password AND Private Key (PEM/PPK).
- **Scanner**: List files via `sftpClient.ls()`.

### 3.3 SFTP Operations
- **Action**: Implement `SftpOperationStrategy`.
- **Feature**: "Download-Edit-Upload" loop for modifications.

---

## 4. FTP Implementation

### 4.1 Dependencies
- **Action**: Add `commons-net`.
- **Client**: `FTPClient`.

### 4.2 FTP Client
- **Action**: Implement `FtpClient` wrapper.
- **Modes**: Support Active and Passive mode switching.
- **Encoding**: Handle UTF-8 vs ASCII explicitly.

---

## 5. Unified Network Cache

### 5.1 Cache Architecture
- **Action**: Implement `UnifiedFileCache`.
- **Logic**:
  - Cache location: `context.cacheDir/network_cache/`.
  - Naming: `Hash(RemotePath + ModifiedTime)`.
  - LRU Eviction: Max 500MB (configurable).

### 5.2 Edit Pipeline
- **Action**: Implement Remote Editing Flow.
- **Steps**:
  1. Download remote file to Edit Cache.
  2. Open local file in Editor (Epic 3).
  3. On Save: Upload back to remote.
  4. Update Cache Entry.
- **Conflict Strategy**: "Last Write Wins".
  - Rationale: For a single user, the risk of concurrent edits is low. Simplifies logic significantly compared to version checking.
  - Fallback: If upload fails, save to specialized "Pending Uploads" local folder.
