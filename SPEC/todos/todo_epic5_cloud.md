# Epic 5: Cloud Integration - Detailed TODO
*Derived from: [Tactical Plan: Epic 5](../00_strategy_epic5_cloud.md)*

## 1. OAuth Infrastructure

### 1.1 Deep Linking & Redirects
- [ ] Configure `AndroidManifest.xml` for custom URL schemes (e.g., `fastmediasorter://oauth`)
- [ ] Implement `OAuthRedirectActivity` to catch callbacks
- [ ] Verify redirects work for Google, MSAL, and Dropbox schemes

### 1.2 Secure Token Storage
- [ ] Extend `NetworkCredentialsRepository`
- [ ] Implement `TokenManager` (Save, Load, Refresh tokens)
- [ ] **Security**: Encrypt Refresh Tokens (Never store plain text)

## 2. Google Drive

### 2.1 API Integration
- [ ] Action: Register project in Google Cloud Console
- [ ] Add `google-api-client-android` and `google-api-services-drive` libs
- [ ] Implement `GoogleDriveClient` (List, Download, Upload)
- [ ] Scope: `drive.file` (Recommended) or `drive` (Full access)

### 2.2 Google Drive Operations
- [ ] Create `GoogleDriveOperationStrategy`
- [ ] Implement `GoogleDriveMediaScanner`
- [ ] **Conflict Resolution**: Last Write Wins (Simple Overwrite)

## 3. OneDrive (Microsoft Graph)

### 3.1 MSAL Integration
- [ ] Register App in Azure Portal
- [ ] Add `msal` library
- [ ] Configure `raw/auth_config.json`
- [ ] Implement Sign-in/Sign-out flow

### 3.2 Graph API
- [ ] Implement `OneDriveClient` using Graph SDK or REST
- [ ] Endpoints: `/me/drive/root/children`
- [ ] Implement `OneDriveOperationStrategy`

## 4. Dropbox

### 4.1 SDK Integration
- [ ] Register App in Dropbox Console
- [ ] Add `dropbox-core-sdk`
- [ ] Implement Authentication flow (PKCE)

### 4.2 Dropbox Operations
- [ ] Implement `DropboxClient`
- [ ] Implement `DropboxOperationStrategy`
- [ ] **Universal Access**: Filter bypass logic for `allFiles` mode
