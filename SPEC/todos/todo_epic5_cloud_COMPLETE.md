# Epic 5: Cloud Storage Integration - Technical Specification
*Derived from: [Tactical Plan: Epic 5](../00_strategy_epic5_cloud.md)*  
*Reference: [Cloud Integration](../detailed_logic/09_cloud_integration_logic.md), [Dependencies](../24_dependencies.md)*

**Purpose**: Design and implement OAuth authentication and file operations for Google Drive, OneDrive, Dropbox from scratch.

**Development Approach**: Clean implementation WITHOUT copying V1 code. V1 OAuth имеет hardcoded secrets и неполную обработку token refresh.

**Estimated Time**: 10-12 days (including OAuth console setup)  
**Prerequisites**: Epic 1-4 completed (database, network protocols, credentials)  
**Output**: Production-ready cloud storage access with proper OAuth2 token management

**CRITICAL**: Developer MUST complete OAuth console setup BEFORE writing code. Нельзя продолжать без client IDs.

---

## 1. Google Drive Integration

### 1.1 Google Cloud Console Setup (MANUAL STEPS)

**CRITICAL**: Developer must complete these steps BEFORE coding:

#### Step 1: Create Project in Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"NEW PROJECT"**
3. Project name: `FastMediaSorter` (or your app name)
4. Click **"CREATE"**
5. Wait for project creation (30-60 seconds)

#### Step 2: Enable Google Drive API
1. In left sidebar: **"APIs & Services"** → **"Library"**
2. Search for `Google Drive API`
3. Click **"Google Drive API"** card
4. Click **"ENABLE"** button
5. Wait for activation (10-20 seconds)

#### Step 3: Create OAuth Consent Screen
1. In left sidebar: **"APIs & Services"** → **"OAuth consent screen"**
2. User Type: Select **"External"** → Click **"CREATE"**
3. Fill required fields:
   - **App name**: `FastMediaSorter`
   - **User support email**: Your email
   - **Developer contact email**: Your email
4. Click **"SAVE AND CONTINUE"**
5. **Scopes**: Click **"ADD OR REMOVE SCOPES"**
   - Find and check: `https://www.googleapis.com/auth/drive.readonly`
   - Find and check: `https://www.googleapis.com/auth/drive.file`
   - Click **"UPDATE"** → **"SAVE AND CONTINUE"**
6. **Test users**: Click **"ADD USERS"** → Enter your Google email → **"ADD"**
7. Click **"SAVE AND CONTINUE"** → **"BACK TO DASHBOARD"**

#### Step 4: Get SHA-1 Fingerprint (CRITICAL FOR ANDROID)
Open terminal in project root:
```powershell
# Debug keystore (for development)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Production keystore (for release)
keytool -list -v -keystore path/to/your/release.keystore -alias your_alias
```
**Copy the SHA-1 value** (example: `A1:B2:C3:D4:E5:F6:...`)

#### Step 5: Create Android OAuth Client ID
1. In left sidebar: **"APIs & Services"** → **"Credentials"**
2. Click **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**
3. Application type: **"Android"**
4. Fill fields:
   - **Name**: `FastMediaSorter Android`
   - **Package name**: `com.sza.fastmediasorter` (MUST match app/build.gradle.kts)
   - **SHA-1 certificate fingerprint**: Paste SHA-1 from Step 4
5. Click **"CREATE"**
6. Dialog appears: **"OAuth client created"** → Click **"OK"**
7. **IMPORTANT**: You do NOT need to download JSON for Android OAuth (unlike Web OAuth)

#### Step 6: Get Client ID
1. In **"Credentials"** page, find your Android client
2. Click the Android client name
3. **Copy "Client ID"** (format: `XXXXXX-YYYYYY.apps.googleusercontent.com`)
4. Save this value → Will be used in `strings.xml`

#### Step 7: Add to `res/values/strings.xml`
```xml
<string name="google_client_id">YOUR_CLIENT_ID_FROM_STEP_6</string>
```

---

### 1.2 GoogleDriveClient Interface Specification

**ANTI-PATTERN (V1)**: Client ID hardcoded в коде → утечка через decompilation

**CORRECT APPROACH**: Client ID в `strings.xml`, SHA-1 fingerprint в Google Console

- [ ] Create `data/cloud/drive/GoogleDriveClient.kt`

**Required Dependencies**:
```kotlin
implementation("com.google.android.gms:play-services-auth:21.2.0")
implementation("com.google.api-client:google-api-client-android:2.2.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0")
implementation("com.google.http-client:google-http-client-gson:1.44.1")
```

**Auth Result Sealed Class**:
```kotlin
sealed class DriveAuthResult {
    data class Success(val email: String, val account: GoogleSignInAccount) : DriveAuthResult()
    data class Error(val message: String) : DriveAuthResult()
    data object Cancelled : DriveAuthResult()
}
```

**GoogleDriveClient Interface Contract**:
```kotlin
class GoogleDriveClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun initialize(activity: Activity)
    fun getSignInIntent(): Intent?
    suspend fun handleSignInResult(data: Intent?): DriveAuthResult
    suspend fun listFiles(folderId: String = "root"): List<DriveFileInfo>
    suspend fun downloadFile(fileId: String): ByteArray?
    suspend fun uploadFile(fileName: String, mimeType: String, content: ByteArray, folderId: String): String?
    suspend fun deleteFile(fileId: String): Boolean
    suspend fun signOut()
}

data class DriveFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: Long,
    val thumbnailLink: String?,
    val isFolder: Boolean
)
```

**Implementation Architecture**:

1. **OAuth Flow** (Activity-based):
   ```
   User clicks "Connect Google Drive"
     → Call initialize(activity)
     → Get intent via getSignInIntent()
     → startActivityForResult(intent, RC_SIGN_IN)
     → onActivityResult: call handleSignInResult(data)
     → Success: Store account, build Drive service
   ```

2. **`initialize()` Requirements**:
   - Create `GoogleSignInOptions` with:
     - `requestEmail()`
     - `requestScopes(Scope(DriveScopes.DRIVE_READONLY), Scope(DriveScopes.DRIVE_FILE))`
     - `requestServerAuthCode(context.getString(R.string.google_client_id))`
   - Build `GoogleSignIn.getClient(activity, options)`
   - Store client instance

3. **`handleSignInResult()` Requirements**:
   - Call `GoogleSignIn.getSignedInAccountFromIntent(data).await()`
   - Get account: check if null → return `DriveAuthResult.Error`
   - Build `GoogleAccountCredential` with scopes
   - Build `Drive` service:
     ```kotlin
     Drive.Builder(
         AndroidHttp.newCompatibleTransport(),
         JacksonFactory.getDefaultInstance(),
         credential
     ).setApplicationName("FastMediaSorter").build()
     ```
   - Store driveService instance
   - Return `DriveAuthResult.Success(email, account)`

4. **`listFiles()` Requirements**:
   - Use Drive API v3: `files().list()`
   - Query: `'$folderId' in parents and trashed = false`
   - Fields: `files(id, name, mimeType, size, modifiedTime, thumbnailLink)`
   - PageSize: 1000 (max per request)
   - Handle pagination if needed (optional for Epic 5)
   - Map to `DriveFileInfo` objects
   - Detect folders: `mimeType == "application/vnd.google-apps.folder"`

5. **`downloadFile()` Requirements**:
   - Call `files().get(fileId).executeMediaAsInputStream()`
   - Read to `ByteArray`: `inputStream.use { it.readBytes() }`
   - Return null on error
   - Handle large files (>10MB): Use chunked download (advanced)

6. **`uploadFile()` Requirements**:
   - Create `File` metadata with `name` and `parents = [folderId]`
   - Create `ByteArrayContent(mimeType, content)`
   - Call `files().create(metadata, mediaContent).setFields("id").execute()`
   - Return file ID

7. **`deleteFile()` Requirements**:
   - Call `files().delete(fileId).execute()`
   - Return true on success, false on exception

8. **`signOut()` Requirements**:
   - Call `googleSignInClient.signOut().await()`
   - Set `driveService = null`

**Error Handling**:
- Catch `ApiException` from Google Sign-In
- Catch `IOException` from Drive API calls
- Catch `GoogleAuthException` for auth issues
- Wrap in appropriate result types
- Log with `Timber.e(exception, "Context")`

**Token Management**:
- Google Sign-In SDK handles token refresh automatically
- Tokens stored in AccountManager (system-level)
- No manual token refresh needed

**Testing Strategy**:
- Test with personal Google account first
- Verify scopes are granted in Google Account settings
- Check file listing works
- Test upload/download with small files

**Known Issues from V1**:
- Hardcoded client ID → FIXED: use `strings.xml`
- Missing SHA-1 in console → FIXED: detailed setup instructions
- No error handling for revoked tokens → FIXED: proper error sealed classes

### 1.3 Google Drive Dependencies
- [ ] Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // Google Drive API
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.44.1")
}
```

---

## 2. OneDrive Integration

### 2.1 Azure Portal Setup (MANUAL STEPS)

#### Step 1: Register App in Azure Portal
1. Go to [Azure Portal](https://portal.azure.com/)
2. Search for **"App registrations"** → Click
3. Click **"+ New registration"**
4. Fill fields:
   - **Name**: `FastMediaSorter`
   - **Supported account types**: **"Accounts in any organizational directory and personal Microsoft accounts"**
   - **Redirect URI**: Select **"Public client/native (mobile & desktop)"**
   - URI: `msauth://com.sza.fastmediasorter/SIGNATURE_HASH`
     - Replace `com.sza.fastmediasorter` with your package name
     - Replace `SIGNATURE_HASH` with Base64-encoded SHA-1 (see Step 2)
5. Click **"Register"**

#### Step 2: Get Signature Hash for Redirect URI
```powershell
# Get SHA-1 from keystore (same as Google Drive Step 4)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Convert SHA-1 to Base64 (manual conversion needed)
# Example SHA-1: A1:B2:C3:D4:E5:F6:...
# Remove colons, convert hex to bytes, then Base64 encode
# Result: something like "AbCdEfGh12345678=="
```

**Online tool for conversion**: https://tomeko.net/online_tools/hex_to_base64.php
- Input: SHA-1 without colons (e.g., `A1B2C3D4E5F6...`)
- Output: Base64 string → Use this for SIGNATURE_HASH

#### Step 3: Configure API Permissions
1. In app page, left sidebar: **"API permissions"**
2. Click **"+ Add a permission"**
3. Select **"Microsoft Graph"**
4. Select **"Delegated permissions"**
5. Find and check:
   - `Files.Read` (read files)
   - `Files.ReadWrite` (read/write files)
   - `offline_access` (refresh token)
6. Click **"Add permissions"**

#### Step 4: Get Application (client) ID
1. In app **"Overview"** page
2. Copy **"Application (client) ID"** (format: `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`)
3. Save this value → Will be used in `auth_config.json`

#### Step 5: Create `auth_config.json`
- [ ] Create `app/src/main/res/raw/auth_config.json`:
```json
{
  "client_id": "YOUR_APPLICATION_CLIENT_ID_FROM_STEP_4",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "msauth://com.sza.fastmediasorter/YOUR_SIGNATURE_HASH",
  "broker_redirect_uri_registered": false,
  "account_mode": "SINGLE",
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

**IMPORTANT**:
- Replace `YOUR_APPLICATION_CLIENT_ID_FROM_STEP_4` with actual client ID
- Replace `YOUR_SIGNATURE_HASH` with Base64-encoded SHA-1 from Step 2
- Package name must match `AndroidManifest.xml`

---

### 2.2 OneDrive Client Implementation
- [ ] Create `data/cloud/onedrive/OneDriveClient.kt`:
```kotlin
package com.sza.fastmediasorter.data.cloud.onedrive

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.sza.fastmediasorter.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class OneDriveAuthResult {
    data class Success(val email: String, val accessToken: String) : OneDriveAuthResult()
    data class Error(val message: String) : OneDriveAuthResult()
}

@Singleton
class OneDriveClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var msalApp: IPublicClientApplication? = null
    private var accessToken: String? = null
    private val httpClient = OkHttpClient()
    
    private val scopes = arrayOf(
        "Files.Read",
        "Files.ReadWrite",
        "offline_access"
    )
    
    /**
     * Initialize MSAL client
     */
    suspend fun initialize(activity: Activity): Boolean = withContext(Dispatchers.IO) {
        try {
            msalApp = PublicClientApplication.create(
                activity,
                R.raw.auth_config // auth_config.json in res/raw/
            )
            Timber.d("OneDrive MSAL initialized")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MSAL")
            false
        }
    }
    
    /**
     * Authenticate user (OAuth)
     */
    suspend fun authenticate(activity: Activity): OneDriveAuthResult = suspendCancellableCoroutine { continuation ->
        val app = msalApp ?: run {
            continuation.resume(OneDriveAuthResult.Error("MSAL not initialized"))
            return@suspendCancellableCoroutine
        }
        
        app.acquireToken(activity, scopes, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                accessToken = authenticationResult.accessToken
                val email = authenticationResult.account.username
                
                Timber.d("OneDrive authenticated: $email")
                continuation.resume(OneDriveAuthResult.Success(email, accessToken!!))
            }
            
            override fun onError(exception: MsalException) {
                Timber.e(exception, "OneDrive auth failed")
                continuation.resume(OneDriveAuthResult.Error(exception.message ?: "Auth failed"))
            }
            
            override fun onCancel() {
                Timber.d("OneDrive auth cancelled")
                continuation.resume(OneDriveAuthResult.Error("Cancelled"))
            }
        })
    }
    
    /**
     * List files in folder
     */
    suspend fun listFiles(folderId: String = "root"): List<OneDriveFileInfo> =
        withContext(Dispatchers.IO) {
            try {
                val token = accessToken ?: throw Exception("Not authenticated")
                
                val request = Request.Builder()
                    .url("https://graph.microsoft.com/v1.0/me/drive/items/$folderId/children")
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val items = json.optJSONArray("value") ?: return@withContext emptyList()
                
                (0 until items.length()).map { i ->
                    val item = items.getJSONObject(i)
                    OneDriveFileInfo(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        size = item.optLong("size", 0L),
                        modifiedTime = item.optString("lastModifiedDateTime"),
                        downloadUrl = item.optString("@microsoft.graph.downloadUrl"),
                        isFolder = item.has("folder")
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to list OneDrive files")
                emptyList()
            }
        }
    
    /**
     * Download file
     */
    suspend fun downloadFile(downloadUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = httpClient.newCall(request).execute()
            response.body?.bytes()
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file")
            null
        }
    }
    
    /**
     * Upload file
     */
    suspend fun uploadFile(
        fileName: String,
        content: ByteArray,
        folderId: String = "root"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = accessToken ?: throw Exception("Not authenticated")
            
            val requestBody = content.toRequestBody("application/octet-stream".toMediaType())
            val request = Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me/drive/items/$folderId:/$fileName:/content")
                .addHeader("Authorization", "Bearer $token")
                .put(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            
            Timber.d("Uploaded file: ${json.optString("id")}")
            json.optString("id")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            null
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = accessToken ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me/drive/items/$fileId")
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()
            
            val response = httpClient.newCall(request).execute()
            Timber.d("Deleted file: $fileId")
            response.isSuccessful
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file")
            false
        }
    }
    
    /**
     * Sign out
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        msalApp?.let { app ->
            app.accounts.forEach { account ->
                app.removeAccount(account)
            }
        }
        accessToken = null
        Timber.d("OneDrive signed out")
    }
}

data class OneDriveFileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: String,
    val downloadUrl: String?,
    val isFolder: Boolean
)
```

### 2.3 OneDrive Dependencies
- [ ] Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // Microsoft Authentication Library (MSAL)
    implementation("com.microsoft.identity.client:msal:5.0.0")
    
    // OkHttp for Graph API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

---

## 3. Dropbox Integration

### 3.1 Dropbox App Console Setup (MANUAL STEPS)

#### Step 1: Create Dropbox App
1. Go to [Dropbox App Console](https://www.dropbox.com/developers/apps)
2. Click **"Create app"**
3. Choose API: **"Scoped access"**
4. Choose access type: **"Full Dropbox"** (or "App folder" for limited access)
5. Name your app: `FastMediaSorter`
6. Click **"Create app"**

#### Step 2: Configure Permissions
1. In app page, go to **"Permissions"** tab
2. Enable these scopes:
   - `files.metadata.read` (read file metadata)
   - `files.content.read` (read file content)
   - `files.content.write` (write files)
3. Click **"Submit"** at bottom

#### Step 3: Get App Key
1. In **"Settings"** tab
2. Find **"App key"** (format: `xxxxxxxxxxxxx`)
3. Copy this value → Will be used in `strings.xml`

#### Step 4: Add to `res/values/strings.xml`
```xml
<string name="dropbox_app_key">YOUR_APP_KEY_FROM_STEP_3</string>
```

#### Step 5: Add to `AndroidManifest.xml`
- [ ] In `app/src/main/AndroidManifest.xml`, add inside `<application>`:
```xml
<activity
    android:name="com.dropbox.core.android.AuthActivity"
    android:configChanges="orientation|keyboard"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="db-YOUR_APP_KEY" />
    </intent-filter>
</activity>
```
**Replace `YOUR_APP_KEY`** with actual app key (e.g., `db-xxxxxxxxxxxxx`)

---

### 3.2 Dropbox Client Implementation
- [ ] Create `data/cloud/dropbox/DropboxClient.kt`:
```kotlin
package com.sza.fastmediasorter.data.cloud.dropbox

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import com.sza.fastmediasorter.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DropboxAuthResult {
    data class Success(val accessToken: String) : DropboxAuthResult()
    data class Error(val message: String) : DropboxAuthResult()
}

@Singleton
class DropboxClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var dbxClient: DbxClientV2? = null
    private var accessToken: String? = null
    
    /**
     * Start OAuth flow
     * Call this when user clicks "Connect Dropbox"
     */
    fun startOAuth() {
        val appKey = context.getString(R.string.dropbox_app_key)
        Auth.startOAuth2Authentication(context, appKey)
        Timber.d("Dropbox OAuth started")
    }
    
    /**
     * Finish OAuth flow
     * Call this in onResume() of your Activity
     */
    suspend fun finishOAuth(): DropboxAuthResult = withContext(Dispatchers.IO) {
        try {
            val token = Auth.getAccessToken()
            if (token == null) {
                return@withContext DropboxAuthResult.Error("No access token")
            }
            
            accessToken = token
            
            val config = DbxRequestConfig.newBuilder("FastMediaSorter").build()
            dbxClient = DbxClientV2(config, token)
            
            Timber.d("Dropbox authenticated")
            DropboxAuthResult.Success(token)
            
        } catch (e: Exception) {
            Timber.e(e, "Dropbox auth failed")
            DropboxAuthResult.Error(e.message ?: "Auth failed")
        }
    }
    
    /**
     * List files in folder
     */
    suspend fun listFiles(folderPath: String = ""): List<DropboxFileInfo> =
        withContext(Dispatchers.IO) {
            try {
                val client = dbxClient ?: throw Exception("Not authenticated")
                
                val result = client.files().listFolder(folderPath)
                result.entries.map { metadata ->
                    when (metadata) {
                        is FileMetadata -> DropboxFileInfo(
                            id = metadata.id,
                            name = metadata.name,
                            path = metadata.pathLower ?: "",
                            size = metadata.size,
                            modifiedTime = metadata.serverModified?.time ?: 0L,
                            isFolder = false
                        )
                        is FolderMetadata -> DropboxFileInfo(
                            id = metadata.id,
                            name = metadata.name,
                            path = metadata.pathLower ?: "",
                            size = 0L,
                            modifiedTime = 0L,
                            isFolder = true
                        )
                        else -> null
                    }
                }.filterNotNull()
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to list Dropbox files")
                emptyList()
            }
        }
    
    /**
     * Download file
     */
    suspend fun downloadFile(filePath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val client = dbxClient ?: throw Exception("Not authenticated")
            
            client.files().download(filePath).inputStream.use { it.readBytes() }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file")
            null
        }
    }
    
    /**
     * Upload file
     */
    suspend fun uploadFile(
        filePath: String,
        content: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        try {
            val client = dbxClient ?: throw Exception("Not authenticated")
            
            ByteArrayInputStream(content).use { inputStream ->
                val metadata = client.files()
                    .uploadBuilder(filePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
                
                Timber.d("Uploaded file: ${metadata.id}")
                metadata.id
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            null
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dbxClient ?: throw Exception("Not authenticated")
            client.files().deleteV2(filePath)
            Timber.d("Deleted file: $filePath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file")
            false
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        accessToken = null
        dbxClient = null
        Timber.d("Dropbox signed out")
    }
}

data class DropboxFileInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val modifiedTime: Long,
    val isFolder: Boolean
)
```

### 3.3 Dropbox Dependencies
- [ ] Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // Dropbox SDK
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
}
```

---

## 4. Unified Cloud Manager

### 4.1 CloudProvider Enum
- [ ] Update `domain/model/CloudProvider.kt`:
```kotlin
package com.sza.fastmediasorter.domain.model

enum class CloudProvider {
    GOOGLE_DRIVE,
    ONEDRIVE,
    DROPBOX;
    
    companion object {
        fun fromString(value: String?): CloudProvider? {
            return when (value?.uppercase()) {
                "GOOGLE_DRIVE" -> GOOGLE_DRIVE
                "ONEDRIVE" -> ONEDRIVE
                "DROPBOX" -> DROPBOX
                else -> null
            }
        }
    }
}
```

### 4.2 CloudMediaScanner
- [ ] Create `data/scanner/CloudMediaScanner.kt`:
```kotlin
package com.sza.fastmediasorter.data.scanner

import com.sza.fastmediasorter.data.cloud.drive.GoogleDriveClient
import com.sza.fastmediasorter.data.cloud.onedrive.OneDriveClient
import com.sza.fastmediasorter.data.cloud.dropbox.DropboxClient
import com.sza.fastmediasorter.domain.model.CloudProvider
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import timber.log.Timber
import javax.inject.Inject

class CloudMediaScanner @Inject constructor(
    private val googleDriveClient: GoogleDriveClient,
    private val oneDriveClient: OneDriveClient,
    private val dropboxClient: DropboxClient
) {
    
    suspend fun scanFolder(
        provider: CloudProvider,
        folderId: String
    ): List<MediaFile> {
        return when (provider) {
            CloudProvider.GOOGLE_DRIVE -> scanGoogleDrive(folderId)
            CloudProvider.ONEDRIVE -> scanOneDrive(folderId)
            CloudProvider.DROPBOX -> scanDropbox(folderId)
        }
    }
    
    private suspend fun scanGoogleDrive(folderId: String): List<MediaFile> {
        val files = googleDriveClient.listFiles(folderId)
        return files
            .filter { !it.isFolder }
            .mapNotNull { file ->
                val type = detectMediaType(file.name)
                if (type != MediaType.OTHER) {
                    MediaFile(
                        path = file.id, // Use file ID as path for cloud
                        name = file.name,
                        size = file.size,
                        date = file.modifiedTime,
                        type = type,
                        duration = 0,
                        thumbnailUrl = file.thumbnailLink
                    )
                } else null
            }
    }
    
    private suspend fun scanOneDrive(folderId: String): List<MediaFile> {
        val files = oneDriveClient.listFiles(folderId)
        return files
            .filter { !it.isFolder }
            .mapNotNull { file ->
                val type = detectMediaType(file.name)
                if (type != MediaType.OTHER) {
                    MediaFile(
                        path = file.id,
                        name = file.name,
                        size = file.size,
                        date = 0L, // Parse modifiedTime string if needed
                        type = type,
                        duration = 0,
                        thumbnailUrl = null
                    )
                } else null
            }
    }
    
    private suspend fun scanDropbox(folderPath: String): List<MediaFile> {
        val files = dropboxClient.listFiles(folderPath)
        return files
            .filter { !it.isFolder }
            .mapNotNull { file ->
                val type = detectMediaType(file.name)
                if (type != MediaType.OTHER) {
                    MediaFile(
                        path = file.path,
                        name = file.name,
                        size = file.size,
                        date = file.modifiedTime,
                        type = type,
                        duration = 0,
                        thumbnailUrl = null
                    )
                } else null
            }
    }
    
    private fun detectMediaType(fileName: String): MediaType {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            in listOf("jpg", "jpeg", "png", "webp", "bmp") -> MediaType.IMAGE
            "gif" -> MediaType.GIF
            in listOf("mp4", "mkv", "avi", "mov") -> MediaType.VIDEO
            in listOf("mp3", "wav", "flac", "m4a") -> MediaType.AUDIO
            "pdf" -> MediaType.PDF
            else -> MediaType.OTHER
        }
    }
}
```

---

## 5. OAuth Activity UI

### 5.1 Cloud Connection Dialog
- [ ] Create `res/layout/dialog_cloud_connection.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="@dimen/spacing_large">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/connect_cloud_storage"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginBottom="@dimen/spacing_medium"/>
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/select_cloud_provider"
        android:textSize="14sp"
        android:layout_marginBottom="@dimen/spacing_normal"/>
    
    <Button
        android:id="@+id/btnConnectGoogleDrive"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/google_drive"
        android:drawableStart="@drawable/ic_google_drive"
        android:drawablePadding="@dimen/spacing_small"
        android:layout_marginBottom="@dimen/spacing_small"
        style="@style/Widget.Material3.Button.OutlinedButton"/>
    
    <Button
        android:id="@+id/btnConnectOneDrive"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/onedrive"
        android:drawableStart="@drawable/ic_onedrive"
        android:drawablePadding="@dimen/spacing_small"
        android:layout_marginBottom="@dimen/spacing_small"
        style="@style/Widget.Material3.Button.OutlinedButton"/>
    
    <Button
        android:id="@+id/btnConnectDropbox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/dropbox"
        android:drawableStart="@drawable/ic_dropbox"
        android:drawablePadding="@dimen/spacing_small"
        android:layout_marginBottom="@dimen/spacing_medium"
        style="@style/Widget.Material3.Button.OutlinedButton"/>
    
    <Button
        android:id="@+id/btnCancel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@android:string/cancel"
        android:layout_gravity="end"
        style="@style/Widget.Material3.Button.TextButton"/>
    
</LinearLayout>
```

---

## 6. Completion Checklist

**Google Drive**:
- [ ] Google Cloud Console project created
- [ ] Drive API enabled
- [ ] OAuth consent screen configured with scopes
- [ ] SHA-1 fingerprint obtained from keystore
- [ ] Android OAuth client ID created with package name + SHA-1
- [ ] Client ID added to `strings.xml`
- [ ] GoogleDriveClient implementation with authentication, list, download, upload, delete

**OneDrive**:
- [ ] Azure Portal app registration created
- [ ] Redirect URI configured with `msauth://` scheme + signature hash
- [ ] API permissions granted (Files.Read, Files.ReadWrite, offline_access)
- [ ] Application (client) ID obtained
- [ ] `auth_config.json` created in `res/raw/` with client ID and redirect URI
- [ ] OneDriveClient implementation with MSAL authentication, Graph API calls

**Dropbox**:
- [ ] Dropbox App Console app created
- [ ] Permissions enabled (files.metadata.read, files.content.read/write)
- [ ] App key obtained
- [ ] App key added to `strings.xml`
- [ ] `AuthActivity` intent-filter added to `AndroidManifest.xml`
- [ ] DropboxClient implementation with OAuth2 PKCE flow

**Unified**:
- [ ] CloudProvider enum with GOOGLE_DRIVE/ONEDRIVE/DROPBOX
- [ ] CloudMediaScanner for unified file scanning
- [ ] Cloud connection dialog with 3 provider buttons

**Success Criteria**: Successfully authenticate with all 3 cloud providers, list files, download/upload files, OAuth flow completes without errors.

**Next**: Epic 6 (Advanced Features) for ML Kit OCR, search, widgets.
