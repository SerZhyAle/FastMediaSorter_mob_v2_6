# 18. Development Workflows

## Build System

### Standard Debug Build

```powershell
# Build debug APK
.\gradlew.bat :app_v2:assembleDebug

# Output location
app_v2/build/outputs/apk/debug/app_v2-debug.apk
```

### Auto-Versioned Build

```powershell
# Build with automatic version increment
.\build-with-version.ps1
```

**Version Format**: `Y.YM.MDDH.Hmm`
- **Y**: Last digit of year (2025 → 2)
- **YM**: Year + Month (2025/12 → 51)
- **MDDH**: Month + Day + Hour (12/16 18:xx → 2161)
- **Hmm**: Hour + Minute (18:54 → 854)

**Example**: `2.51.2161.854` = Build on 2025/12/16 at 18:54

**Script Behavior**:
1. Updates `versionCode` (increments by 1)
2. Updates `versionName` with timestamp
3. Commits changes to git
4. Runs Gradle build
5. **On failure**: Reverts all changes automatically

### Clean Build

```powershell
# Clean all build artifacts
.\gradlew.bat :app_v2:clean

# Clean + rebuild
.\gradlew.bat :app_v2:clean :app_v2:assembleDebug
```

---

## Database Migrations

### Current Schema

**Version**: 6  
**File**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/AppDatabase.kt`

### Migration History

#### Migration 5 → 6
**Date**: November 2024  
**Changes**: Added cloud storage support

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add cloud provider column
        database.execSQL(
            "ALTER TABLE resources ADD COLUMN cloudProvider TEXT DEFAULT NULL"
        )
        
        // Add cloud folder ID column
        database.execSQL(
            "ALTER TABLE resources ADD COLUMN cloudFolderId TEXT DEFAULT NULL"
        )
    }
}
```

### Creating New Migration

**Step 1**: Update Entity
```kotlin
@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // ... existing fields ...
    val newField: String? = null // New field with default value
)
```

**Step 2**: Increment Database Version
```kotlin
@Database(
    entities = [ResourceEntity::class, /* ... */],
    version = 7, // Increment from 6 to 7
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() { /* ... */ }
```

**Step 3**: Create Migration Object
```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE resources ADD COLUMN newField TEXT DEFAULT NULL"
        )
    }
}
```

**Step 4**: Register Migration
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
    .addMigrations(
        MIGRATION_5_6,
        MIGRATION_6_7 // Add new migration
    )
    .build()
```

### Migration Best Practices

1. **Always provide default values** for new columns
2. **Test migration** on device with old database version
3. **Never delete old migrations** - users might upgrade from any version
4. **Export schema** for documentation (`exportSchema = true`)
5. **Use transactions** - Room handles this automatically

---

## Testing Network Protocols

### SMB (Server Message Block)

**Dependencies**:
```kotlin
smbj = "0.12.1"
bouncycastle = "1.78.1" // CRITICAL: prevents libpenguin.so crash
```

**Test Connection**:
```kotlin
val result = smbClient.testConnection(
    server = "192.168.1.100",
    shareName = "Media",
    username = "user",
    password = "pass",
    domain = "", // Optional
    port = 445
)
```

**Common Issues**:
- **Native crash**: Ensure BouncyCastle 1.78.1 (not 1.77 or older)
- **Connection timeout**: Check firewall, verify port 445 open
- **Access denied**: Verify SMB3 encryption enabled on server

---

### SFTP (SSH File Transfer Protocol)

**Dependencies**:
```kotlin
sshj = "0.37.0"
eddsa = "0.3.0" // Required for Curve25519 key support
```

**Test Connection**:
```kotlin
val result = sftpClient.testConnection(
    host = "sftp.example.com",
    port = 22,
    username = "user",
    password = "pass"
)
```

**Supported Authentication**:
- Password
- Public key (RSA, DSA, ECDSA, Ed25519)

**Common Issues**:
- **Host key verification**: First connection requires accepting fingerprint
- **Key format**: Use OpenSSH format, not PuTTY (.ppk)

---

### FTP (File Transfer Protocol)

**Dependencies**:
```kotlin
commons-net = "3.10.0"
```

**Test Connection**:
```kotlin
val result = ftpClient.testConnection(
    host = "ftp.example.com",
    port = 21,
    username = "user",
    password = "pass"
)
```

**Known Issues**:
- **PASV mode timeouts**: Firewall blocks data connection ports
- **Solution**: Active mode fallback implemented
- **NEVER call** `completePendingCommand()` after exceptions

**Implementation**:
```kotlin
try {
    ftpClient.enterLocalPassiveMode()
    ftpClient.retrieveFile(remotePath, outputStream)
} catch (e: Exception) {
    // Fallback to active mode
    ftpClient.enterLocalActiveMode()
    ftpClient.retrieveFile(remotePath, outputStream)
}
```

---

### Cloud Storage (OAuth 2.0)

#### Google Drive

**Prerequisites**:
1. Create OAuth 2.0 Client ID in [Google Cloud Console](https://console.cloud.google.com)
2. Type: Android
3. Package name: `com.sza.fastmediasorter`
4. SHA-1 fingerprint: Run `keytool -list -v -keystore keystore.jks`

**Test Authentication**:
```kotlin
when (val result = googleDriveClient.authenticate(activity)) {
    is AuthResult.Success -> println("Logged in: ${result.email}")
    is AuthResult.Error -> println("Auth failed: ${result.message}")
    AuthResult.Cancelled -> println("User cancelled")
}
```

#### OneDrive (MSAL)

**Configuration**: `app_v2/src/main/res/raw/msal_config.json`

```json
{
  "client_id": "your-client-id",
  "redirect_uri": "msauth://com.sza.fastmediasorter/WdRIvjP3wXJ5jte7TPUOqtT59es=",
  "authorities": [{
    "type": "AAD",
    "audience": { "type": "AzureADandPersonalMicrosoftAccount" }
  }]
}
```

#### Dropbox

**App Key**: Stored in `gradle.properties`
```properties
dropboxAppKey=your-app-key-here
```

**Manifest Registration**:
```xml
<activity android:name="com.dropbox.core.android.AuthActivity"
    android:launchMode="singleTask">
    <intent-filter>
        <data android:scheme="db-${dropboxAppKey}" />
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
</activity>
```

---

## Task Tracking

### TODO File Structure

**Location**: `dev/TODO_V2.md`

**Sections**:
1. **Recent Fixes & Completions** - Top of file, documents completed work with build number
2. **Current Development Tasks** - Active work in progress
3. **High Priority** - Critical bugs, blocking issues
4. **Medium Priority** - Improvements, minor bugs
5. **Low Priority** - Nice-to-have features

**Format**:
```markdown
### ✅ Feature Name - Build .XXX (Date)
**Problem**: Description of what was broken
**Solution**: What was implemented
**Changes**: List of modified files
**Benefits**: Impact of the fix
```

---

## Testing Checklist

### Before Each Release

- [ ] Run `.\gradlew.bat :app_v2:test` - Unit tests
- [ ] Test on physical device (Android 9+)
- [ ] Verify each protocol: Local, SMB, SFTP, FTP, Google Drive, OneDrive, Dropbox
- [ ] Test file operations: Copy, Move, Delete, Undo
- [ ] Test media playback: Video, Audio, Images, GIF
- [ ] Test document viewing: TXT, PDF, EPUB
- [ ] Test pagination: Browse resource with 1000+ files
- [ ] Check memory usage: Open large images, play long videos
- [ ] Test offline mode: Airplane mode with cached files
- [ ] Verify 16KB page size compatibility (Android 15+)

### Performance Benchmarks

- **Cold start**: < 2 seconds on mid-range device
- **File list load** (1000 files): < 3 seconds
- **Pagination scroll**: 60 FPS with no frame drops
- **Image load** (network): < 1 second with caching
- **Video playback**: Instant start with buffering < 2s

---

## Debugging Tools

### Logcat Filtering

```bash
# App-specific logs only
adb logcat -s FastMediaSorter

# Timber logs
adb logcat *:E FastMediaSorter:D

# Clear logcat
adb logcat -c
```

### Database Inspection

```bash
# Pull database from device
adb pull /data/data/com.sza.fastmediasorter/databases/app_database .

# Open with sqlite3
sqlite3 app_database
sqlite> .tables
sqlite> SELECT * FROM resources;
```

### Network Traffic Monitoring

Use Android Studio's **Network Profiler**:
1. Run app on device
2. Open Profiler tab
3. Select Network
4. Monitor HTTP/HTTPS requests to cloud APIs

---

## Related Documentation

- [16. Core Logic & Rules](16_core_logic_and_rules.md) - Business logic implementation
- [19. Refactoring History](19_refactoring_history.md) - Past refactorings and lessons learned
- [21. Common Pitfalls](21_common_pitfalls.md) - Known issues and solutions
- [24. Dependencies](24_dependencies.md) - Complete dependency list with versions
