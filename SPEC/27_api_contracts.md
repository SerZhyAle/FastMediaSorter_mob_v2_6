# 27. API Contracts & Interfaces

**Last Updated**: January 6, 2026  
**Purpose**: Complete catalog of all Repository, DAO, and UseCase interfaces with method signatures.

This document serves as the authoritative API reference for all architectural layers in FastMediaSorter v2.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
│  Activities, Fragments, ViewModels                          │
└────────────────────┬────────────────────────────────────────┘
                     │ inject UseCases
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer                               │
│  ┌──────────────────┐        ┌─────────────────────────┐   │
│  │   UseCases       │◄───────┤ Repository Interfaces   │   │
│  │  (35 classes)    │depends │   (7 interfaces)        │   │
│  └──────────────────┘        └─────────────────────────┘   │
└────────────────────────────────────┬───────────────────────┘
                                     │ implement
                                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│  ┌────────────────────────┐   ┌───────────────────────┐    │
│  │ Repository Impls       │───┤   DAO Interfaces      │    │
│  │  (7 classes)           │use│   (5 interfaces)      │    │
│  └────────────────────────┘   └───────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Table of Contents

1. [Repository Interfaces (Domain Layer)](#1-repository-interfaces-domain-layer)
2. [DAO Interfaces (Data Layer)](#2-dao-interfaces-data-layer)
3. [UseCase Classes (Domain Layer)](#3-usecase-classes-domain-layer)
4. [Protocol-Specific Clients](#4-protocol-specific-clients)
5. [Strategy Pattern Interfaces](#5-strategy-pattern-interfaces)
6. [Callback Interfaces](#6-callback-interfaces)

---

## 1. Repository Interfaces (Domain Layer)

All interfaces located in `domain/repository/`. Implemented in `data/repository/`.

### 1.1. ResourceRepository

**Purpose**: Main repository for resource CRUD operations and queries.

**Location**: `domain/repository/ResourceRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getAllResources()` | `Flow<List<MediaResource>>` | - | Observable list of all resources |
| `getAllResourcesSync()` | `suspend List<MediaResource>` | - | One-time fetch of all resources |
| `getResourceById()` | `suspend MediaResource?` | `id: Long` | Get single resource by ID |
| `getResourcesByType()` | `Flow<List<MediaResource>>` | `type: ResourceType` | Filter by type (LOCAL/SMB/etc) |
| `getDestinations()` | `Flow<List<MediaResource>>` | - | Get quick destinations (max 10) |
| `getFilteredResources()` | `suspend List<MediaResource>` | `filterByType`, `filterByMediaType`, `filterByName`, `sortMode` | Complex filtering at DB level |
| `addResource()` | `suspend Long` | `resource: MediaResource` | Insert new resource, returns ID |
| `updateResource()` | `suspend Unit` | `resource: MediaResource` | Update existing resource |
| `swapResourceDisplayOrders()` | `suspend Unit` | `resource1`, `resource2` | Atomic swap for reordering |
| `deleteResource()` | `suspend Unit` | `resourceId: Long` | Delete resource by ID |
| `searchResources()` | `suspend List<MediaResource>` | `query: String` | FTS search by name/path |

**Usage Example**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {
    val resources: StateFlow<List<MediaResource>> = 
        resourceRepository.getAllResources()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

---

### 1.2. SettingsRepository

**Purpose**: Application settings persistence (DataStore).

**Location**: `domain/repository/SettingsRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getSettings()` | `Flow<AppSettings>` | - | Observable settings |
| `updateSettings()` | `suspend Unit` | `settings: AppSettings` | Update all settings |
| `resetToDefaults()` | `suspend Unit` | - | Reset to default values |
| `setPlayerFirstRun()` | `suspend Unit` | `isFirstRun: Boolean` | Set first-run flag |
| `isPlayerFirstRun()` | `suspend Boolean` | - | Check first-run flag |
| `saveLastUsedResourceId()` | `suspend Unit` | `resourceId: Long` | Remember last opened resource |
| `getLastUsedResourceId()` | `suspend Long` | - | Get last opened resource ID |
| `setResourceGridMode()` | `suspend Unit` | `isGridMode: Boolean` | Set global grid mode |

**AppSettings Data Class**:
```kotlin
data class AppSettings(
    val theme: Theme = Theme.SYSTEM,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val displayMode: DisplayMode = DisplayMode.LIST,
    val maxRecipients: Int = 10,
    val showHiddenFiles: Boolean = false,
    val enableThumbnails: Boolean = true,
    val slideshowInterval: Int = 5,
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val autoRotate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showCommandPanel: Boolean = true,
    val cacheSize: Long = 2_000_000_000L, // 2 GB
    // ... ~30 more settings
)
```

---

### 1.3. FavoritesRepository

**Purpose**: User favorites management.

**Location**: `domain/repository/FavoritesRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getAllFavorites()` | `Flow<List<FavoritesEntity>>` | - | Observable favorites list |
| `isFavorite()` | `Flow<Boolean>` | `uri: String` | Observable favorite status |
| `isFavoriteSync()` | `suspend Boolean` | `uri: String` | One-time favorite check |
| `addFavorite()` | `suspend Unit` | `entity: FavoritesEntity` | Add to favorites |
| `removeFavorite()` | `suspend Unit` | `uri: String` | Remove by URI |
| `removeFavoriteById()` | `suspend Unit` | `id: Long` | Remove by ID |

---

### 1.4. PlaybackPositionRepository

**Purpose**: Audio/video resume playback positions (audiobook mode).

**Location**: `domain/repository/PlaybackPositionRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getPosition()` | `suspend Long?` | `filePath: String` | Get saved position (ms), null if none |
| `savePosition()` | `suspend Unit` | `filePath`, `position`, `duration` | Save position, auto-mark completed at >95% |
| `markAsCompleted()` | `suspend Unit` | `filePath: String` | Mark as watched/listened |
| `deletePosition()` | `suspend Unit` | `filePath: String` | Clear saved position |
| `cleanupOldPositions()` | `suspend Unit` | - | FIFO cleanup (10k → 9k entries) |
| `deleteAllPositions()` | `suspend Unit` | - | Clear all (cache clear) |

**Business Logic**:
- Auto-marks `isCompleted = true` when `position > 0.95 * duration`
- Returns `0` for completed files (restart from beginning)
- Enforces 10,000 entry limit with FIFO eviction

---

### 1.5. NetworkCredentialsRepository

**Purpose**: Encrypted credential storage for SMB/SFTP/FTP.

**Location**: `domain/repository/NetworkCredentialsRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getCredentialById()` | `suspend NetworkCredentialsEntity?` | `credentialId: String` | Get by UUID |
| `getAllCredentials()` | `Flow<List<NetworkCredentialsEntity>>` | - | Observable all credentials |
| `addCredential()` | `suspend String` | `entity: NetworkCredentialsEntity` | Insert, returns UUID |
| `updateCredential()` | `suspend Unit` | `entity: NetworkCredentialsEntity` | Update existing |
| `deleteCredential()` | `suspend Unit` | `credentialId: String` | Delete by UUID |
| `getCredentialsByType()` | `suspend List<NetworkCredentialsEntity>` | `type: String` | Filter by SMB/SFTP/FTP |

**Security Note**:
- `NetworkCredentialsEntity.password` is auto-decrypted via `@Ignore` getter
- Uses `CryptoHelper` with Android Keystore (AES-256-GCM)

---

### 1.6. ThumbnailCacheRepository

**Purpose**: Network video thumbnail cache metadata.

**Location**: `domain/repository/ThumbnailCacheRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getCachedThumbnail()` | `suspend ThumbnailCacheEntity?` | `filePath: String` | Get cache entry |
| `saveThumbnail()` | `suspend Unit` | `filePath`, `localCachePath`, `fileSize` | Store metadata |
| `updateLastAccessed()` | `suspend Unit` | `filePath: String` | Touch timestamp |
| `deleteOldEntries()` | `suspend Unit` | `olderThan: Long` | Cleanup by timestamp |
| `deleteAllCache()` | `suspend Unit` | - | Clear all cache |
| `getCacheSize()` | `suspend Long` | - | Total cache size (bytes) |

---

### 1.7. MediaStoreRepository

**Purpose**: Android MediaStore queries for local files.

**Location**: `domain/repository/MediaStoreRepository.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getFoldersWithMedia()` | `suspend List<FolderInfo>` | `allowedTypes: Set<MediaType>` | Scan MediaStore for folders |
| `getFilesInFolder()` | `suspend List<MediaFile>` | `folderPath`, `allowedTypes`, `recursive` | Get files in folder |
| `getStandardFolders()` | `suspend List<FolderInfo>` | - | DCIM, Pictures, Downloads, etc |

**Data Classes**:
```kotlin
data class FolderInfo(
    val path: String,
    val name: String,
    val fileCount: Int,
    val containedTypes: Set<MediaType>
)
```

---

## 2. DAO Interfaces (Data Layer)

All DAOs located in `data/local/db/`. Used by Repository implementations.

### 2.1. ResourceDao

**Purpose**: Room DAO for `resources` table.

**Location**: `data/local/db/ResourceDao.kt`

**Type**: `abstract class` (contains both abstract and implemented methods)

**Methods** (35 total):

#### Core CRUD
| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `insert()` | `suspend Long` | `resource: ResourceEntity` | Insert + FTS update, returns ID |
| `update()` | `suspend Unit` | `resource: ResourceEntity` | Update + FTS rebuild |
| `delete()` | `suspend Unit` | `resource: ResourceEntity` | Delete + FTS cleanup |
| `deleteById()` | `suspend Unit` | `id: Long` | Delete by ID |
| `deleteAllResources()` | `suspend Unit` | - | Clear table |

#### Query Operations
| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `getAllResources()` | `Flow<List<ResourceEntity>>` | - | ORDER BY displayOrder, name |
| `getAllResourcesSync()` | `suspend List<ResourceEntity>` | - | One-time fetch |
| `getResourceById()` | `Flow<ResourceEntity?>` | `id: Long` | Observable single resource |
| `getResourceByIdSync()` | `suspend ResourceEntity?` | `id: Long` | One-time fetch |
| `getResourcesByType()` | `Flow<List<ResourceEntity>>` | `type: ResourceType` | Filter by type |
| `getDestinations()` | `Flow<List<ResourceEntity>>` | - | WHERE isDestination = 1 |
| `getResourcesRaw()` | `List<ResourceEntity>` | `query: SupportSQLiteQuery` | Dynamic query builder |
| `searchResourcesFts()` | `List<ResourceEntity>` | `query: String` | FTS search |

#### Atomic Operations
| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `swapDisplayOrders()` | `suspend Unit` | `id1`, `id2`, `order1`, `order2` | Single transaction swap |

**Implementation Pattern** (public method wraps protected methods + FTS):
```kotlin
suspend fun insert(resource: ResourceEntity): Long {
    val id = insertResource(resource) // protected @Insert
    insertFts(id, resource.name, resource.path) // FTS update
    return id
}
```

---

### 2.2. NetworkCredentialsDao

**Purpose**: Room DAO for `network_credentials` table.

**Location**: `data/local/db/NetworkCredentialsDao.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `insert()` | `suspend Unit` | `entity: NetworkCredentialsEntity` | Insert credential |
| `update()` | `suspend Unit` | `entity: NetworkCredentialsEntity` | Update credential |
| `delete()` | `suspend Unit` | `entity: NetworkCredentialsEntity` | Delete credential |
| `getCredentialById()` | `suspend NetworkCredentialsEntity?` | `credentialId: String` | Get by UUID |
| `getAllCredentials()` | `Flow<List<NetworkCredentialsEntity>>` | - | Observable all |
| `getCredentialsByType()` | `suspend List<NetworkCredentialsEntity>` | `type: String` | Filter by SMB/SFTP/FTP |

---

### 2.3. FavoritesDao

**Purpose**: Room DAO for `favorites` table.

**Location**: `data/local/db/FavoritesDao.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `insert()` | `suspend Unit` | `entity: FavoritesEntity` | REPLACE on conflict |
| `deleteByUri()` | `suspend Unit` | `uri: String` | Remove by URI |
| `deleteById()` | `suspend Unit` | `id: Long` | Remove by ID |
| `getAllFavorites()` | `Flow<List<FavoritesEntity>>` | - | ORDER BY addedTimestamp DESC |
| `isFavorite()` | `Flow<Boolean>` | `uri: String` | Observable status |
| `isFavoriteSync()` | `suspend Boolean` | `uri: String` | One-time check |

---

### 2.4. PlaybackPositionDao

**Purpose**: Room DAO for `playback_positions` table.

**Location**: `data/local/db/PlaybackPositionDao.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `insert()` | `suspend Unit` | `entity: PlaybackPositionEntity` | REPLACE on conflict |
| `getPosition()` | `suspend PlaybackPositionEntity?` | `filePath: String` | Get saved position |
| `deletePosition()` | `suspend Unit` | `filePath: String` | Clear position |
| `deleteAllPositions()` | `suspend Unit` | - | Clear all |
| `getOldestPositions()` | `suspend List<PlaybackPositionEntity>` | `limit: Int` | FIFO cleanup helper |
| `deletePositions()` | `suspend Unit` | `filePaths: List<String>` | Batch delete |

---

### 2.5. ThumbnailCacheDao

**Purpose**: Room DAO for `thumbnail_cache` table.

**Location**: `data/local/db/ThumbnailCacheDao.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `insert()` | `suspend Unit` | `entity: ThumbnailCacheEntity` | REPLACE on conflict |
| `getCachedThumbnail()` | `suspend ThumbnailCacheEntity?` | `filePath: String` | Get entry |
| `updateLastAccessed()` | `suspend Unit` | `filePath: String`, `timestamp: Long` | Touch entry |
| `deleteOldEntries()` | `suspend Unit` | `olderThan: Long` | WHERE lastAccessedAt < |
| `deleteAllCache()` | `suspend Unit` | - | Clear all |
| `getTotalCacheSize()` | `suspend Long` | - | SUM(fileSize) |

---

## 3. UseCase Classes (Domain Layer)

All UseCases located in `domain/usecase/`. Injected into ViewModels.

### UseCase Naming Convention
- **Pattern**: `VerbNounUseCase`
- **Examples**: `GetResourcesUseCase`, `MoveFileUseCase`, `RotateImageUseCase`

### 3.1. Resource Management UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `GetResourcesUseCase` | Fetch resources with filters | `invoke(filterType, filterMediaType, sortMode)` | ResourceRepository |
| `AddResourceUseCase` | Create new resource | `invoke(resource: MediaResource)` | ResourceRepository |
| `UpdateResourceUseCase` | Update resource | `invoke(resource: MediaResource)` | ResourceRepository |
| `DeleteResourceUseCase` | Delete resource | `invoke(resourceId: Long)` | ResourceRepository |
| `GetDestinationsUseCase` | Get quick destinations | `invoke(): Flow<List<MediaResource>>` | ResourceRepository, SettingsRepository |
| `ScanLocalFoldersUseCase` | Scan MediaStore | `invoke(): Result<List<MediaResource>>` | MediaStoreRepository, ResourceRepository |

---

### 3.2. File Operation UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `FileOperationUseCase` | Copy/move/delete files | `copy()`, `move()`, `delete()`, `rename()`, `undo()` | Strategy pattern (5 implementations) |
| `CleanupTrashFoldersUseCase` | Delete `.trash/` folders | `invoke()` | FileOperationUseCase |
| `DownloadNetworkFileUseCase` | Download to local cache | `execute(path, progressCallback)` | Protocol clients |

**FileOperationUseCase Interface**:
```kotlin
interface FileOperationStrategy {
    suspend fun copy(source: String, destination: String): FileOperationResult
    suspend fun move(source: String, destination: String): FileOperationResult
    suspend fun delete(path: String): FileOperationResult
    suspend fun rename(path: String, newName: String): FileOperationResult
}

// Implementations:
// - LocalOperationStrategy
// - SmbOperationStrategy
// - SftpOperationStrategy
// - FtpOperationStrategy
// - CloudOperationStrategy
```

---

### 3.3. Media Scanning UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `GetMediaFilesUseCase` | Scan resource for files | `invoke(resource, sortMode, progressCallback)` | MediaScannerFactory |
| `MediaScannerFactory` | Protocol scanner factory | `createScanner(resourceType)` | 5 protocol scanners |
| `DiscoverNetworkResourcesUseCase` | Network discovery | `execute(): Flow<NetworkHost>` | Socket scanning |

**MediaScannerFactory Pattern**:
```kotlin
class MediaScannerFactory @Inject constructor(
    private val localScanner: LocalMediaScanner,
    private val smbScanner: SmbMediaScanner,
    private val sftpScanner: SftpMediaScanner,
    private val ftpScanner: FtpMediaScanner,
    private val cloudScanner: CloudMediaScanner
) {
    fun createScanner(type: ResourceType): MediaScanner = when (type) {
        ResourceType.LOCAL -> localScanner
        ResourceType.SMB -> smbScanner
        ResourceType.SFTP -> sftpScanner
        ResourceType.FTP -> ftpScanner
        ResourceType.CLOUD -> cloudScanner
    }
}
```

---

### 3.4. Image Editing UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `RotateImageUseCase` | Rotate image 90° | `execute(imagePath, clockwise): Result<Unit>` | Android Bitmap |
| `FlipImageUseCase` | Flip horizontal/vertical | `execute(imagePath, horizontal)` | Android Bitmap |
| `AdjustImageUseCase` | Brightness/contrast/saturation | `execute(imagePath, adjustments)` | ColorMatrix |
| `ApplyImageFilterUseCase` | Apply filters | `execute(imagePath, filter)` | ColorMatrix |
| `NetworkImageEditUseCase` | Edit network images | `execute(resource, path, operation)` | Download → Edit → Upload |

**Adjustments Data Class**:
```kotlin
data class Adjustments(
    val brightness: Float = 0f,  // -100 to +100
    val contrast: Float = 1f,    // 0.0 to 3.0
    val saturation: Float = 1f   // 0.0 to 2.0 (0 = grayscale)
)
```

---

### 3.5. Video/GIF Processing UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `ExtractVideoMetadataUseCase` | Get video metadata | `execute(videoPath): VideoMetadata` | MediaMetadataRetriever |
| `ExtractGifFramesUseCase` | Extract GIF frames | `execute(gifPath): List<Bitmap>` | GifDecoder |
| `ChangeGifSpeedUseCase` | Change GIF speed | `execute(gifPath, speedMultiplier)` | Frame delay manipulation |
| `SaveGifFirstFrameUseCase` | Save first frame as JPEG | `execute(gifPath, outputPath)` | GifDecoder + Bitmap |

---

### 3.6. Audio UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `SearchAudioCoverUseCase` | Find album art online | `execute(filename): String?` | iTunes API (Retrofit) |
| `SearchLyricsUseCase` | Find lyrics online | `execute(artist, title): String?` | Lyrics API |

---

### 3.7. Settings UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `ExportSettingsUseCase` | Export to JSON file | `execute(outputPath)` | SettingsRepository |
| `ImportSettingsUseCase` | Import from JSON file | `execute(inputPath)` | SettingsRepository |
| `CalculateOptimalCacheSizeUseCase` | Calculate cache size | `execute(): Long` | Storage stats |

---

### 3.8. Network UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `SmbOperationsUseCase` | SMB file operations | `copy()`, `move()`, `delete()`, `testConnection()` | SmbClient |
| `NetworkSpeedTestUseCase` | Test read/write speed | `execute(resource): SpeedTestResult` | Protocol clients |
| `SyncNetworkResourcesUseCase` | Background sync | `execute(resourceId)` | WorkManager |
| `ScheduleNetworkSyncUseCase` | Schedule periodic sync | `execute(resourceId, interval)` | WorkManager |

**SpeedTestResult**:
```kotlin
data class SpeedTestResult(
    val readSpeedMbps: Double,
    val writeSpeedMbps: Double,
    val recommendedThreads: Int,
    val pingMs: Long
)
```

---

### 3.9. Metadata Extraction UseCases

| UseCase | Purpose | Key Methods | Dependencies |
|---------|---------|-------------|--------------|
| `ExtractExifMetadataUseCase` | Extract EXIF data | `execute(imagePath): ExifMetadata` | ExifInterface |

**ExifMetadata**:
```kotlin
data class ExifMetadata(
    val width: Int,
    val height: Int,
    val dateTime: String?,
    val camera: String?,
    val location: LatLng?,
    val orientation: Int
)
```

---

## 4. Protocol-Specific Clients

Network protocol clients in `data/network/`.

### 4.1. SmbClient

**Purpose**: SMB/CIFS file operations.

**Location**: `data/network/SmbClient.kt`

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| `connect()` | `Result<SmbConnection>` | `server`, `share`, `username`, `password`, `domain`, `port` | Establish connection |
| `disconnect()` | `Unit` | `connection: SmbConnection` | Close connection |
| `listFiles()` | `Result<List<SmbFile>>` | `connection`, `path` | List directory |
| `readFile()` | `Result<InputStream>` | `connection`, `path` | Read file stream |
| `writeFile()` | `Result<Unit>` | `connection`, `path`, `data` | Write file |
| `deleteFile()` | `Result<Unit>` | `connection`, `path` | Delete file |
| `createDirectory()` | `Result<Unit>` | `connection`, `path` | Create folder |

**Connection Pooling**:
- Max 5 connections per share
- 45s idle timeout
- Thread-safe with `Mutex`

---

### 4.2. SftpClient

**Purpose**: SFTP file operations.

**Location**: `data/network/SftpClient.kt`

**Methods**: Similar to SmbClient (connect, disconnect, listFiles, etc.)

**SSH Key Support**:
- Ed25519 (Curve25519)
- RSA 2048+
- PEM format (encrypted with CryptoHelper)

---

### 4.3. FtpClient

**Purpose**: FTP file operations.

**Location**: `data/network/FtpClient.kt`

**Methods**: Similar to SmbClient

**Special Handling**:
- PASV mode with active mode fallback
- Never call `completePendingCommand()` after exceptions
- Binary transfer mode enforced

---

### 4.4. Cloud Clients

| Client | Purpose | Authentication | API |
|--------|---------|----------------|-----|
| `GoogleDriveRestClient` | Google Drive operations | OAuth 2.0 (Google Sign-In) | Drive API v3 |
| `OneDriveRestClient` | OneDrive operations | MSAL OAuth | Graph API |
| `DropboxRestClient` | Dropbox operations | OAuth 2.0 | Dropbox API v2 |

**Common Methods**:
- `authenticate(activity): AuthResult`
- `listFiles(folderId): Result<List<CloudFile>>`
- `downloadFile(fileId, outputStream): Result<Unit>`
- `uploadFile(folderId, fileName, inputStream): Result<String>`
- `deleteFile(fileId): Result<Unit>`

---

## 5. Strategy Pattern Interfaces

### 5.1. FileOperationStrategy

**Purpose**: Unified interface for file operations across protocols.

**Location**: `domain/usecase/FileOperationStrategy.kt`

**Interface**:
```kotlin
interface FileOperationStrategy {
    suspend fun copy(
        source: String,
        destination: String,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult
    
    suspend fun move(
        source: String,
        destination: String,
        progressCallback: ByteProgressCallback? = null
    ): FileOperationResult
    
    suspend fun delete(path: String): FileOperationResult
    
    suspend fun rename(path: String, newName: String): FileOperationResult
    
    fun supportsUndo(): Boolean
}
```

**Implementations**:
1. `LocalOperationStrategy` - Local file system
2. `SmbOperationStrategy` - SMB shares
3. `SftpOperationStrategy` - SFTP servers
4. `FtpOperationStrategy` - FTP servers
5. `CloudOperationStrategy` - Cloud storage

**FileOperationResult**:
```kotlin
sealed class FileOperationResult {
    data class Success(
        val path: String,
        val undoInfo: UndoInfo? = null
    ) : FileOperationResult()
    
    data class Failure(
        val error: String,
        val exception: Throwable? = null
    ) : FileOperationResult()
}

data class UndoInfo(
    val operation: Operation,
    val originalPath: String,
    val newPath: String,
    val timestamp: Long
)

enum class Operation { COPY, MOVE, DELETE, RENAME }
```

---

## 6. Callback Interfaces

### 6.1. ScanProgressCallback

**Purpose**: Report file scan progress to UI.

**Location**: `domain/usecase/ScanProgressCallback.kt`

**Methods**:
```kotlin
interface ScanProgressCallback {
    fun onProgress(current: Int, total: Int, currentFile: String)
    fun onComplete(totalFiles: Int)
    fun onError(message: String)
}
```

**Usage**:
```kotlin
viewModel.scanResource(resource, object : ScanProgressCallback {
    override fun onProgress(current: Int, total: Int, currentFile: String) {
        progressBar.progress = (current * 100 / total)
        statusText.text = "$current / $total: $currentFile"
    }
    override fun onComplete(totalFiles: Int) {
        Toast.makeText(context, "Found $totalFiles files", LENGTH_SHORT).show()
    }
    override fun onError(message: String) {
        showErrorDialog(message)
    }
})
```

---

### 6.2. ByteProgressCallback

**Purpose**: Report byte-level progress for file operations.

**Location**: `domain/usecase/ByteProgressCallback.kt`

**Methods**:
```kotlin
interface ByteProgressCallback {
    fun onProgress(bytesTransferred: Long, totalBytes: Long)
    fun onComplete()
    fun onError(message: String)
}
```

---

## Usage Examples

### Example 1: ViewModel with Repository

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val fileOperationUseCase: FileOperationUseCase,
    private val favoritesUseCase: FavoritesUseCase
) : ViewModel() {
    
    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files: StateFlow<List<MediaFile>> = _files.asStateFlow()
    
    fun loadFiles(resource: MediaResource) {
        viewModelScope.launch {
            getMediaFilesUseCase.invoke(resource, object : ScanProgressCallback {
                override fun onProgress(current: Int, total: Int, currentFile: String) {
                    // Update progress
                }
                override fun onComplete(totalFiles: Int) {
                    _files.value = /* result */
                }
                override fun onError(message: String) {
                    // Handle error
                }
            })
        }
    }
    
    fun copyFile(file: MediaFile, destination: MediaResource) {
        viewModelScope.launch {
            when (val result = fileOperationUseCase.copy(file.path, destination.path)) {
                is FileOperationResult.Success -> {
                    // Show success
                }
                is FileOperationResult.Failure -> {
                    // Show error
                }
            }
        }
    }
}
```

---

### Example 2: Direct Repository Usage

```kotlin
class MyService @Inject constructor(
    private val resourceRepository: ResourceRepository
) {
    suspend fun getAllLocalResources(): List<MediaResource> {
        return resourceRepository.getFilteredResources(
            filterByType = setOf(ResourceType.LOCAL),
            filterByMediaType = null,
            filterByName = null,
            sortMode = SortMode.NAME_ASC
        )
    }
}
```

---

### Example 3: UseCase Composition

```kotlin
@HiltViewModel
class EditResourceViewModel @Inject constructor(
    private val getResourcesUseCase: GetResourcesUseCase,
    private val updateResourceUseCase: UpdateResourceUseCase,
    private val networkSpeedTestUseCase: NetworkSpeedTestUseCase
) : ViewModel() {
    
    fun testAndSaveSpeed(resource: MediaResource) {
        viewModelScope.launch {
            val result = networkSpeedTestUseCase.execute(resource)
            val updated = resource.copy(
                readSpeedMbps = result.readSpeedMbps,
                writeSpeedMbps = result.writeSpeedMbps,
                recommendedThreads = result.recommendedThreads
            )
            updateResourceUseCase.invoke(updated)
        }
    }
}
```

---

## Testing Strategies

### 1. Repository Tests (Fake Implementation)

```kotlin
class FakeResourceRepository : ResourceRepository {
    private val resources = mutableListOf<MediaResource>()
    
    override fun getAllResources(): Flow<List<MediaResource>> = 
        flowOf(resources.toList())
    
    override suspend fun addResource(resource: MediaResource): Long {
        resources.add(resource)
        return resources.size.toLong()
    }
    
    // ... implement all methods
}

// In ViewModel test:
@Test
fun `addResource updates list`() = runTest {
    val repository = FakeResourceRepository()
    val viewModel = MainViewModel(repository)
    
    viewModel.addResource(testResource)
    
    assertEquals(1, repository.getAllResourcesSync().size)
}
```

---

### 2. UseCase Tests (Mock Repository)

```kotlin
@Test
fun `GetResourcesUseCase filters by type`() = runTest {
    val mockRepository = mockk<ResourceRepository>()
    coEvery { 
        mockRepository.getFilteredResources(any(), any(), any(), any()) 
    } returns listOf(testResource)
    
    val useCase = GetResourcesUseCase(mockRepository)
    val result = useCase.invoke(
        filterType = setOf(ResourceType.LOCAL),
        sortMode = SortMode.NAME_ASC
    )
    
    assertEquals(1, result.size)
    coVerify { 
        mockRepository.getFilteredResources(
            setOf(ResourceType.LOCAL), 
            any(), 
            any(), 
            SortMode.NAME_ASC
        ) 
    }
}
```

---

### 3. DAO Tests (Instrumentation)

```kotlin
@RunWith(AndroidJUnit4::class)
class ResourceDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ResourceDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = database.resourceDao()
    }
    
    @Test
    fun insertAndRetrieve() = runTest {
        val resource = ResourceEntity(name = "Test", path = "/test", type = ResourceType.LOCAL)
        val id = dao.insert(resource)
        
        val retrieved = dao.getResourceByIdSync(id)
        assertEquals("Test", retrieved?.name)
    }
}
```

---

## Migration Guide for New APIs

### Adding a New Repository Method

1. **Define in Repository Interface** (`domain/repository/`):
```kotlin
interface ResourceRepository {
    suspend fun getResourcesByTag(tag: String): List<MediaResource>
}
```

2. **Implement in RepositoryImpl** (`data/repository/`):
```kotlin
class ResourceRepositoryImpl @Inject constructor(
    private val dao: ResourceDao
) : ResourceRepository {
    override suspend fun getResourcesByTag(tag: String): List<MediaResource> {
        return dao.getResourcesByTag(tag).map { it.toDomain() }
    }
}
```

3. **Add DAO Method** (`data/local/db/`):
```kotlin
@Query("SELECT * FROM resources WHERE comment LIKE '%' || :tag || '%'")
abstract suspend fun getResourcesByTag(tag: String): List<ResourceEntity>
```

4. **Create UseCase** (`domain/usecase/`):
```kotlin
class GetResourcesByTagUseCase @Inject constructor(
    private val repository: ResourceRepository
) {
    suspend operator fun invoke(tag: String): List<MediaResource> {
        return repository.getResourcesByTag(tag)
    }
}
```

5. **Inject into ViewModel**:
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getResourcesByTagUseCase: GetResourcesByTagUseCase
) : ViewModel()
```

---

## Best Practices

### 1. Repository Layer
- ✅ Return `Flow` for observable data, `suspend` for one-time operations
- ✅ Handle exceptions in Repository, return `Result<T>` to UI layer
- ✅ Map DB entities to domain models in Repository (never expose entities to UI)

### 2. UseCase Layer
- ✅ Single Responsibility: One UseCase = One business operation
- ✅ Depend only on Repository interfaces (never implementations)
- ✅ Return domain models or sealed classes (never Room entities)

### 3. DAO Layer
- ✅ Use `Flow` for data that changes frequently
- ✅ Use `@Transaction` for multi-table operations
- ✅ Prefer query methods over `@RawQuery` (type safety)

### 4. Naming Conventions
- Repository: `EntityRepository` (e.g., `ResourceRepository`)
- DAO: `EntityDao` (e.g., `ResourceDao`)
- UseCase: `VerbNounUseCase` (e.g., `GetResourcesUseCase`)

---

## Reference Files

### Source Code Locations
- **Repository Interfaces**: `app_v2/src/main/java/com/sza/fastmediasorter/domain/repository/`
- **Repository Implementations**: `app_v2/src/main/java/com/sza/fastmediasorter/data/repository/`
- **DAOs**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/db/`
- **UseCases**: `app_v2/src/main/java/com/sza/fastmediasorter/domain/usecase/`
- **Protocol Clients**: `app_v2/src/main/java/com/sza/fastmediasorter/data/network/`

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
