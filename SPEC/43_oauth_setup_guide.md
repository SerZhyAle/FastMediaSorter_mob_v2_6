# 43. OAuth Setup Guide

**Goal**: Step-by-step visual configuration for Cloud Providers.
**Note**: Screenshots are represented by placeholders.

---

## 1. Google Drive Setup (Google Cloud Console)

**URL**: [console.cloud.google.com](https://console.cloud.google.com)

### Step 1: Create Project
1. Select "New Project".
2. Name: `FastMediaSorter-App`.
3. Location: `No Organization`.

> ![Screenshot: New Project Button](assets/oauth_screenshots/google_01_new_project.png)

### Step 2: Enable API
1. Go to **APIs & Services > Library**.
2. Search for `Google Drive API`.
3. Click **Enable**.

### Step 3: Configure Consent Screen
1. Go to **APIs & Services > OAuth consent screen**.
2. Type: **External** (unless you have a G-Suite org).
3. App Name: `Fast Media Sorter`.
4. Support Email: Your email.
5. **Scopes**: Add `.../auth/drive` or `.../auth/drive.file`.
6. **Test Users**: Add your own Google email.

> ![Screenshot: Consent Screen Config](assets/oauth_screenshots/google_02_consent.png)

### Step 4: Create Credentials
1. Go to **Credentials > Create Credentials > OAuth client ID**.
2. Application Type: **Android**.
3. Package Name: `com.sza.fastmediasorter`.
4. **SHA-1 Fingerprint**:
   Run `./gradlew signingReport` in terminal.
   Copy the `SHA1` from `debug` variant.
   
   *Example*: `5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:CD:EF`

> ![Screenshot: Android Client Config](assets/oauth_screenshots/google_03_credentials.png)

---

## 2. OneDrive Setup (Azure Portal)

**URL**: [portal.azure.com](https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)

### Step 1: Register App
1. Go to **App Registrations > New Registration**.
2. Name: `FastMediaSorter`.
3. Accounts: **Accounts in any organizational directory and personal Microsoft accounts**.

> ![Screenshot: Azure Registration](assets/oauth_screenshots/azure_01_register.png)

### Step 2: Add Platform
1. Select **Authentication > Add a platform > Android**.
2. Package Name: `com.sza.fastmediasorter`.
3. **Signature Hash**:
   This requires a Base64 encoded SHA-1.
   
   Command:
   ```bash
   keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
   ```

> ![Screenshot: Android Platform Config](assets/oauth_screenshots/azure_02_platform.png)

---

## 3. Dropbox Setup (App Console)

**URL**: [dropbox.com/developers/apps](https://www.dropbox.com/developers/apps)

### Step 1: Create App
1. Click **Create app**.
2. API: **Scoped Access**.
3. Type: **Full Dropbox** (or App Folder).
4. Name: `FastMediaSorter_<UniqueName>`.

> ![Screenshot: Dropbox Create App](assets/oauth_screenshots/dropbox_01_create.png)

### Step 2: Permissions
1. Go to **Permissions** tab.
2. Check: `files.content.read`, `files.content.write`, `files.metadata.read`.
3. Click **Submit**.

### Step 3: Keys
1. Go to **Settings**.
2. Copy **App key** and **App secret** to your `local.properties`.

> ![Screenshot: Dropbox Settings](assets/oauth_screenshots/dropbox_02_keys.png)
