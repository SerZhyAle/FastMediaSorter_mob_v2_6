# Tactical Plan: Epic 5 - Cloud Integration

**Goal**: Integration with major cloud providers via OAuth.
**Deliverable**: Google Drive, OneDrive, and Dropbox support.
**Prerequisite**: Epic 4 Complete.

---

## 1. OAuth Infrastructure

### 1.1 Credential Registry
- **Action**: Setup infrastructure for multiple auth providers.
- **Config**:
  - Store Client IDs / Secrets in `local.properties` (not version control).
  - Inject via BuildConfig fields.

### 1.2 Auth Redirect Handlers
- **Action**: Implement Redirect Activity.
- **Components**:
  - `Manifest`: Add intent filters for custom schemes (e.g., `com.sza.fastmediasorter://auth`).
  - `RedirectActivity`: Catch response, extract code/token, pass to Repository.

---

## 2. Google Drive Integration

### 2.1 Setup
- **Action**: Configure Google Cloud Console Project.
- **Details**: Enable Drive API v3. Add SHA-1 debug/release keys.
- **OAuth Redirect URL Configuration (CRITICAL)**:
  
  **Step-by-Step Instructions**:
  
  1. **Create Google Cloud Project**:
     - Go to [Google Cloud Console](https://console.cloud.google.com)
     - Create new project: "FastMediaSorter Android"
     - Enable "Google Drive API"
  
  2. **Create OAuth 2.0 Client ID**:
     - Navigate to: APIs & Services → Credentials
     - Click "Create Credentials" → "OAuth client ID"
     - Application type: **Android**
     - Package name: `com.apemax.fastmediasorter`
     - **SHA-1 certificate fingerprint**: Get from debug/release keystores
  
  3. **Get SHA-1 Fingerprints**:
     ```bash
     # Debug keystore
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     
     # Release keystore
     keytool -list -v -keystore /path/to/release.keystore -alias release
     ```
     Copy the `SHA-1` value (e.g., `A1:B2:C3:D4:...`).
  
  4. **Configure Android App**:
     - Paste SHA-1 fingerprint in credential configuration
     - **Redirect URI format**: `com.googleusercontent.apps.<CLIENT_ID>:/oauth2redirect`
     - **IMPORTANT**: Client ID is auto-generated, not manually set.
  
  5. **Add to AndroidManifest.xml**:
     ```xml
     <activity
         android:name=".ui.auth.GoogleAuthRedirectActivity"
         android:exported="true">
         <intent-filter>
             <action android:name="android.intent.action.VIEW" />
             <category android:name="android.intent.category.DEFAULT" />
             <category android:name="android.intent.category.BROWSABLE" />
             <data
                 android:scheme="com.googleusercontent.apps"
                 android:host="<REVERSE_CLIENT_ID>" />
         </intent-filter>
     </activity>
     ```
  
  6. **Store Client ID Securely**:
     ```properties
     # local.properties (NOT in version control)
     GOOGLE_DRIVE_CLIENT_ID=123456789-abcdefghijklmnop.apps.googleusercontent.com
     ```
  
  7. **Inject into BuildConfig**:
     ```kotlin
     // app_v2/build.gradle.kts
     android {
         defaultConfig {
             val properties = Properties()
             properties.load(project.rootProject.file("local.properties").inputStream())
             
             buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${properties.getProperty("GOOGLE_DRIVE_CLIENT_ID")}\"")
         }
     }
     ```
  
  **Common Mistakes to Avoid**:
  - ❌ Using Web Application OAuth instead of Android
  - ❌ Forgetting to add SHA-1 for release keystore (only debug works)
  - ❌ Wrong package name (must match `applicationId`)
  - ❌ Committing Client ID to Git (use `local.properties`)

### 2.2 Drive Client
- **Action**: Implement `GoogleDriveClient`.
- **Lib**: `google-api-client-android`.
- **Auth**: Google Sign-In (or newer Credential Manager).

### 2.3 Operations
- **Action**: Implement `GoogleDriveOperationStrategy`.
- **Logic**: Use Folder Picker to select "Root" for the resource.

---

## 3. OneDrive Integration

### 3.1 Setup
- **Action**: Configure Azure App Registration.
- **Details**: Multi-tenant app (Personal + Work/School).
- **OAuth Redirect URL Configuration**:
  
  **Step-by-Step Instructions**:
  
  1. **Create Azure AD App Registration**:
     - Go to [Azure Portal](https://portal.azure.com)
     - Navigate to: Azure Active Directory → App registrations
     - Click "New registration"
     - Name: "FastMediaSorter Android"
     - Supported account types: **Accounts in any organizational directory and personal Microsoft accounts**
  
  2. **Configure Authentication**:
     - Navigate to: Authentication → Add a platform → **Android**
     - Package name: `com.apemax.fastmediasorter`
     - **Signature hash**: Base64-encoded SHA-1
       ```bash
       # Get SHA-1 from keystore
       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
       
       # Convert to Base64 (without colons)
       echo -n "A1B2C3D4..." | xxd -r -p | base64
       ```
  
  3. **API Permissions**:
     - Navigate to: API permissions → Add a permission
     - Microsoft Graph → Delegated permissions
     - Add: `Files.Read`, `Files.ReadWrite`, `User.Read`
     - **IMPORTANT**: Click "Grant admin consent" (if organizational)
  
  4. **Get Application (Client) ID**:
     - Copy from Overview page (GUID format: `12345678-1234-1234-1234-123456789abc`)
  
  5. **Create MSAL Configuration**:
     ```json
     // app_v2/src/main/res/raw/msal_config.json
     {
       "client_id": "YOUR_CLIENT_ID",
       "authorization_user_agent": "DEFAULT",
       "redirect_uri": "msauth://com.apemax.fastmediasorter/YOUR_SIGNATURE_HASH",
       "authorities": [
         {
           "type": "AAD",
           "audience": {
             "type": "AzureADandPersonalMicrosoftAccount"
           }
         }
       ]
     }
     ```
  
  6. **Add to AndroidManifest.xml**:
     ```xml
     <activity
         android:name="com.microsoft.identity.client.BrowserTabActivity"
         android:exported="true">
         <intent-filter>
             <action android:name="android.intent.action.VIEW" />
             <category android:name="android.intent.category.DEFAULT" />
             <category android:name="android.intent.category.BROWSABLE" />
             <data
                 android:scheme="msauth"
                 android:host="com.apemax.fastmediasorter"
                 android:path="/YOUR_SIGNATURE_HASH" />
         </intent-filter>
     </activity>
     ```
  
  7. **Store Client ID Securely**:
     ```properties
     # local.properties
     ONEDRIVE_CLIENT_ID=12345678-1234-1234-1234-123456789abc
     ```
  
  **Redirect URI Format**: `msauth://com.apemax.fastmediasorter/<SIGNATURE_HASH>`
  
  **Testing**: Use Microsoft Authenticator app for testing auth flow.

### 3.2 MSAL Integration
- **Action**: Implement `OneDriveClient` using MSAL.
- **Lib**: `com.microsoft.identity.client:msal`.
- **Config**: `raw/msal_config.json`.

### 3.3 Graph API
- **Action**: Implement File Operations via Graph API.
- **Endpoints**: `/me/drive`, `/me/drive/items/{id}`.

---

## 4. Dropbox Integration

### 4.1 Setup
- **Action**: Create Dropbox App in Console.
- **Details**: Scoped access (files.content.read/write).
- **OAuth Redirect URL Configuration**:
  
  **Step-by-Step Instructions**:
  
  1. **Create Dropbox App**:
     - Go to [Dropbox App Console](https://www.dropbox.com/developers/apps)
     - Click "Create app"
     - Choose API: **Scoped access**
     - Access type: **Full Dropbox** (access all files)
     - Name: "FastMediaSorter"
  
  2. **Configure Permissions**:
     - Navigate to: Permissions tab
     - Enable:
       - `files.metadata.read` - Read file metadata
       - `files.content.read` - Read file content
       - `files.content.write` - Upload/modify files
     - Click "Submit"
  
  3. **OAuth 2 Redirect URIs**:
     - Navigate to: Settings tab
     - Add redirect URI: `db-<APP_KEY>://auth` (custom scheme)
     - **IMPORTANT**: Replace `<APP_KEY>` with your actual App key
     - Example: `db-a1b2c3d4e5f6://auth`
  
  4. **Get App Key & Secret**:
     - Copy from Settings → App key (e.g., `a1b2c3d4e5f6g7h8`)
     - Copy from Settings → App secret (keep secure!)
  
  5. **Add to AndroidManifest.xml**:
     ```xml
     <activity
         android:name=".ui.auth.DropboxAuthRedirectActivity"
         android:exported="true"
         android:launchMode="singleTask">
         <intent-filter>
             <action android:name="android.intent.action.VIEW" />
             <category android:name="android.intent.category.DEFAULT" />
             <category android:name="android.intent.category.BROWSABLE" />
             <data
                 android:scheme="db-YOUR_APP_KEY"
                 android:host="auth" />
         </intent-filter>
     </activity>
     ```
  
  6. **Store Credentials Securely**:
     ```properties
     # local.properties
     DROPBOX_APP_KEY=a1b2c3d4e5f6g7h8
     DROPBOX_APP_SECRET=i9j0k1l2m3n4o5p6
     ```
  
  7. **Initialize Dropbox SDK**:
     ```kotlin
     // Application.onCreate()
     Auth.setAppKey(BuildConfig.DROPBOX_APP_KEY)
     ```
  
  8. **Start OAuth Flow**:
     ```kotlin
     fun authenticateDropbox(activity: Activity) {
         Auth.startOAuth2Authentication(
             activity,
             BuildConfig.DROPBOX_APP_KEY,
             null, // Use default redirect URI
             null, // No custom state
             "code" // Authorization code flow
         )
     }
     
     // In Activity.onResume()
     fun handleDropboxAuth() {
         val accessToken = Auth.getOAuth2Token()
         if (accessToken != null) {
             // Save token to EncryptedSharedPreferences
             cloudTokenRepository.saveDropboxToken(accessToken)
         }
     }
     ```
  
  **Redirect URI Format**: `db-<APP_KEY>://auth`
  
  **Testing**: Use Dropbox Android app for quick testing (auto-login).

### 4.2 SDK Integration
- **Action**: Implement `DropboxClient`.
- **Lib**: `dropbox-core-sdk`.

### 4.3 Operations
- **Action**: Implement `DropboxOperationStrategy`.
- **Logic**: Handle cursor-based pagination for large folders.
- **Conflict Policy**: Last Write Wins. (Consistent with other network protocols).
