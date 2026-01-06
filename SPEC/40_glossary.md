# Glossary

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Purpose**: Canonical definitions for all project terminology

---

## Core Concepts

### Resource
**Definition**: Any registered media source available for browsing in the app.  
**Types**: Local folder, SMB share, SFTP path, FTP site, Google Drive folder, OneDrive folder, Dropbox folder.  
**Storage**: `ResourceEntity` in Room database.  
**Example**: `SMB resource: \\192.168.1.100\media` or `Local: /storage/emulated/0/DCIM`

**Key Properties**:
- `id` (Long): Unique identifier
- `name` (String): User-facing label
- `type` (ResourceType enum): LOCAL, SMB, SFTP, FTP, GOOGLE_DRIVE, ONEDRIVE, DROPBOX
- `path` (String): Location (local path, network share, cloud folder ID)
- `color` (Int): UI accent color for visual identification
- `isDestination` (Boolean): Quick copy/move target

**Related Terms**: Destination, MediaSource, Storage Location

---

### Destination
**Definition**: Resource marked for quick file operations via long-press menu.  
**Limit**: Maximum 10 destinations per app instance.  
**UI**: Shown as colored buttons in bottom sheet during copy/move operations.  
**Flag**: `ResourceEntity.isDestination = true`

**Related Terms**: Resource, Quick Copy Target

---

### MediaFile
**Definition**: Domain model representing a single file (image/video/audio/document) from any resource.  
**Not Persisted**: Transient object, not stored in database (only cached metadata in `MediaFileEntity`).

**Key Properties**:
- `path` (String): Full path including resource location
- `name` (String): Filename with extension
- `size` (Long): File size in bytes
- `date` (Long): Last modified timestamp (epoch millis)
- `type` (MediaType enum): IMAGE, VIDEO, AUDIO, GIF, PDF, TEXT
- `duration` (Long?): For video/audio, length in milliseconds
- `thumbnailUrl` (String?): Cloud-specific thumbnail URL (Google Drive, OneDrive)

**Related Terms**: FileMetadata, MediaEntity

---

### MediaType
**Definition**: Enum categorizing file types for filtering and preview.

**Values**:
- `IMAGE`: JPG, PNG, WebP, HEIC, BMP
- `VIDEO`: MP4, MKV, AVI, MOV, WebM
- `AUDIO`: MP3, FLAC, WAV, AAC, OGG
- `GIF`: Animated GIF files (special handling for memory)
- `PDF`: PDF documents
- `TEXT`: TXT, JSON, XML, LOG files

**Detection**: By file extension (case-insensitive).

**Related Terms**: FileType, MimeType

---

### Scanner
**Definition**: Protocol-specific component for discovering files in a resource.  
**Interface**: `MediaScanner` with `scanFolder(resourceId, path, progressCallback)` method.

**Implementations**:
- `LocalMediaScanner`: Uses `java.io.File` for local storage
- `SmbMediaScanner`: Uses `SmbClient` for network shares
- `SftpMediaScanner`: Uses `SftpClient` for SSH file transfer
- `FtpMediaScanner`: Uses `FtpClient` for FTP sites
- `CloudMediaScanner`: Uses cloud-specific APIs (Drive, OneDrive, Dropbox)

**Pagination**: All scanners support `scanFolderPaged(offset, limit)` for large directories.

**Related Terms**: FileLister, DirectoryExplorer

---

## Network Protocols

### SMB (Server Message Block)
**Definition**: Network file-sharing protocol for Windows/Samba servers.  
**Library**: SMBJ 0.12.1 (pure Java, no native dependencies).  
**Versions**: SMB 2.x and SMB 3.x supported (SMB 1.0 disabled for security).  
**Authentication**: Username/password, optional domain.  
**Port**: Default 445 (configurable).

**Related Terms**: CIFS, Samba, Windows Share

---

### SFTP (SSH File Transfer Protocol)
**Definition**: Secure file transfer over SSH.  
**Library**: SSHJ 0.37.0 with EdDSA 0.3.0 for Curve25519 keys.  
**Authentication**: Password or SSH private key file.  
**Port**: Default 22 (configurable).  
**Security**: Host key verification (stored in app-private `known_hosts`).

**Related Terms**: SSH, Secure Copy (SCP)

---

### FTP (File Transfer Protocol)
**Definition**: Traditional file transfer protocol.  
**Library**: Apache Commons Net 3.10.0.  
**Modes**: PASV (passive) preferred, fallback to active mode.  
**Security**: Optional FTPS (FTP over TLS).  
**Port**: Default 21 (configurable).

**Known Issue**: PASV mode timeouts require automatic fallback to active mode.

**Related Terms**: FTPS, Active Mode, Passive Mode

---

## Cloud Providers

### Google Drive
**Definition**: Google's cloud storage service.  
**API**: Google Drive API v3.  
**Authentication**: OAuth 2.0 with Android OAuth client.  
**Library**: `com.google.android.gms:play-services-auth` + Drive API client.  
**Token**: Refresh token stored in `EncryptedSharedPreferences`.  
**Rate Limit**: 100 requests/minute per user.

**Folder ID**: Drive-specific folder identifier (e.g., `1A2B3C4D5E6F7G8H9I`).

**Related Terms**: GDrive, Google Cloud Storage (different service)

---

### OneDrive
**Definition**: Microsoft's cloud storage service.  
**API**: Microsoft Graph API.  
**Authentication**: MSAL (Microsoft Authentication Library) for Android.  
**Library**: `com.microsoft.identity.client:msal:4.9.0`.  
**Token**: Access token auto-refreshed by MSAL.  
**Rate Limit**: 150 requests/minute per user.

**Folder ID**: Graph API folder identifier (e.g., `01BYE5RZ6QN3VZQIPQZR...`).

**Related Terms**: OneDrive for Business, SharePoint (different APIs)

---

### Dropbox
**Definition**: Cloud storage and file synchronization service.  
**API**: Dropbox API v2.  
**Authentication**: OAuth 2.0.  
**Library**: Dropbox SDK v6.0.0.  
**Token**: OAuth token stored securely.  
**Rate Limit**: 100 requests/minute per user.

**Folder Path**: Standard filesystem path (e.g., `/Photos/Vacation`).

**Related Terms**: Dropbox Business

---

## Architecture Terms

### Clean Architecture
**Definition**: Architectural pattern separating code into three layers with strict dependency rules.  
**Layers**: UI → Domain → Data (dependencies point inward).  
**Benefit**: High testability, maintainability, scalability.

**Related Terms**: Hexagonal Architecture, Onion Architecture

---

### MVVM (Model-View-ViewModel)
**Definition**: UI design pattern separating presentation logic from view.  
**Components**:
- **View**: Fragment/Activity observing ViewModel state
- **ViewModel**: Exposes `StateFlow`/`SharedFlow` for UI state and events
- **Model**: Domain layer (UseCases, Repositories)

**Android Implementation**: `ViewModel` class with `viewModelScope` for coroutines.

**Related Terms**: MVP, MVC, MVI

---

### UseCase
**Definition**: Single-responsibility class encapsulating one business operation.  
**Location**: `domain/usecase/` package.  
**Naming**: `VerbNounUseCase` (e.g., `GetMediaFilesUseCase`, `CopyFileUseCase`).  
**Dependencies**: Only repository interfaces, never implementations.

**Example**:
```kotlin
class GetMediaFilesUseCase @Inject constructor(
    private val repository: MediaFileRepository
) {
    suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
        return repository.getMediaFiles(resourceId)
    }
}
```

**Related Terms**: Interactor, Service

---

### Repository
**Definition**: Abstraction over data sources (Room database, network clients, cloud APIs).  
**Pattern**: Repository Pattern.  
**Interface**: Defined in domain layer, implemented in data layer.  
**Purpose**: Hide data source details from domain logic.

**Example**: `ResourceRepository` interface with `LocalResourceRepository` and `RemoteResourceRepository` implementations.

**Related Terms**: DAO (Data Access Object), Data Source

---

### Strategy Pattern
**Definition**: Design pattern for protocol-specific file operations.  
**Interface**: `FileOperationStrategy` with `copyFile()`, `moveFile()`, `deleteFile()`.  
**Implementations**: `LocalOperationStrategy`, `SmbOperationStrategy`, `SftpOperationStrategy`, `FtpOperationStrategy`, `CloudOperationStrategy`.  
**Benefit**: Eliminates code duplication across 36 protocol combinations.

**Related Terms**: Polymorphism, Interface

---

## Data Persistence

### Room
**Definition**: Android Jetpack SQLite ORM library.  
**Version**: 2.6.1.  
**Components**: `@Entity` (table), `@Dao` (queries), `@Database` (schema).  
**Migrations**: Schema version 6, migrations in `AppDatabase.kt`.

**Related Terms**: SQLite, ORM

---

### Entity
**Definition**: Room database table representation (annotated with `@Entity`).

**Key Entities**:
- `ResourceEntity`: Resources table
- `MediaFileEntity`: File metadata cache
- `FavoriteEntity`: Favorite files
- `FileVersionEntity`: Version tracking for conflict detection
- `PendingOperationEntity`: Offline operation queue

**Related Terms**: Table, Model

---

### DAO (Data Access Object)
**Definition**: Interface for Room database queries (annotated with `@Dao`).  
**Methods**: `@Query`, `@Insert`, `@Update`, `@Delete`.  
**Return Types**: `Flow<T>` for reactive queries, `suspend fun` for one-shot queries.

**Example**:
```kotlin
@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources")
    fun getAllResources(): Flow<List<ResourceEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity): Long
}
```

**Related Terms**: Repository, Query

---

## UI Components

### ViewBinding
**Definition**: Type-safe view access in Activities/Fragments.  
**Generation**: Auto-generated from XML layouts.  
**Usage**: `binding.textViewTitle.text = "Hello"` instead of `findViewById()`.

**Related Terms**: DataBinding (more complex), findViewById() (deprecated)

---

### StateFlow
**Definition**: Kotlin coroutine-based observable state holder.  
**Usage**: ViewModel exposes `StateFlow<T>` for UI state (e.g., loading, error, data).  
**Collection**: Fragment/Activity uses `lifecycleScope.launch { flow.collect { } }`.

**Related Terms**: LiveData (older alternative), Flow

---

### SharedFlow
**Definition**: Kotlin coroutine-based event emitter for one-time events.  
**Usage**: ViewModel emits events (e.g., `ShowError`, `NavigateToPlayer`).  
**Collection**: UI collects in `repeatOnLifecycle(STARTED)`.

**Example**:
```kotlin
sealed class BrowseEvent {
    data class ShowError(val message: String) : BrowseEvent()
    data class NavigateToPlayer(val file: MediaFile) : BrowseEvent()
}

// ViewModel
private val _events = MutableSharedFlow<BrowseEvent>()
val events: SharedFlow<BrowseEvent> = _events
```

**Related Terms**: SingleLiveEvent (anti-pattern), EventBus

---

### Paging3
**Definition**: Jetpack library for lazy loading large datasets.  
**Usage**: Activated when file count >1,000 (see `PAGINATION_THRESHOLD`).  
**Components**: `PagingSource`, `PagingDataAdapter`, `PagingData`.  
**Benefit**: Memory-efficient scrolling through 10,000+ files.

**Related Terms**: RecyclerView, Pagination

---

## File Operations

### Soft Delete
**Definition**: Move file to `.trash/` folder instead of permanent deletion.  
**Purpose**: Enable undo for delete operations.  
**Cleanup**: `CleanupTrashFoldersUseCase` runs weekly via WorkManager.  
**Retention**: Files in trash deleted after 7 days.

**Related Terms**: Recycle Bin, Undo Stack

---

### Chunked Transfer
**Definition**: Split large file uploads/downloads into 5MB chunks.  
**Threshold**: Files >100MB use chunked transfer.  
**Classes**: `ChunkedDownloader`, `ChunkedUploader`.  
**Benefit**: Resume support, memory-efficient (no full file in RAM).

**Related Terms**: Multipart Upload, Streaming

---

### File Size Limits
**Definition**: Maximum file sizes for operations to prevent OOM.

| Operation | Limit | Reason |
|-----------|-------|--------|
| Preview Image | 50MB | Bitmap memory limit |
| Edit Image | 20MB | Processing memory |
| Edit GIF | 10MB | Animation frames memory |
| Upload/Download | 2GB | Storage constraints |

**Enforcement**: `MemoryMonitor` checks available memory before operations.

**Related Terms**: OOM (Out of Memory), Memory Management

---

## Performance & Optimization

### Thumbnail Cache
**Definition**: Glide disk cache for image previews.  
**Location**: `<app_data>/cache/image_cache/`.  
**Size**: Default 2GB (configurable in settings).  
**Policy**: LRU eviction (Least Recently Used).  
**Persistence**: Survives app restarts.

**Related Terms**: Glide, Disk Cache

---

### Connection Pool
**Definition**: Reusable pool of network connections (SMB/SFTP) to avoid reconnection overhead.  
**Implementation**: `BaseConnectionPool` with `Mutex` for thread safety.  
**Timeout**: Idle connections closed after 45 seconds.

**Related Terms**: Keep-Alive, Connection Reuse

---

### Circuit Breaker
**Definition**: Pattern to prevent repeated failed network requests.  
**States**: CLOSED (normal) → OPEN (failures detected, reject requests) → HALF_OPEN (test recovery).  
**Thresholds**: Trip after 5 failures, stay open for 30s, close after 2 successes.

**Related Terms**: Fail Fast, Network Resilience

---

## Testing

### Unit Test
**Definition**: Test for single class/function in isolation (no Android dependencies).  
**Tools**: JUnit, MockK.  
**Target**: 80% coverage for UseCases and ViewModels.

**Related Terms**: Integration Test, Mocking

---

### Integration Test
**Definition**: Test for interaction between components (e.g., Repository + Room).  
**Tools**: Room Testing, Hilt Testing.  
**Target**: 70% coverage for Repositories.

**Related Terms**: E2E Test, Unit Test

---

### UI Test
**Definition**: Test for user interactions (button clicks, scrolling).  
**Tools**: Espresso, UI Automator.  
**Target**: 50% coverage for critical flows (browse, play, copy).

**Related Terms**: Instrumentation Test, Functional Test

---

### Migration Test
**Definition**: Test for Room database schema migrations.  
**Tool**: `MigrationTestHelper`.  
**Requirement**: 100% coverage (all migrations 1→2, 2→3, ..., 5→6).

**Related Terms**: Schema Migration, Database Test

---

## Localization

### String Resource
**Definition**: Localized text in `strings.xml` files.  
**Languages**: English (`values/`), Russian (`values-ru/`), Ukrainian (`values-uk/`).  
**Usage**: `getString(R.string.key_name)` in code.

**Example**:
```xml
<!-- values/strings.xml -->
<string name="browse_title">Browse Files</string>

<!-- values-ru/strings.xml -->
<string name="browse_title">Обзор файлов</string>
```

**Related Terms**: i18n, l10n

---

### Locale
**Definition**: User's language and region settings.  
**Detection**: `Locale.getDefault()` in code.  
**Override**: User can change language in app settings.

**Related Terms**: Language, Regionalization

---

## Security

### EncryptedSharedPreferences
**Definition**: Encrypted key-value storage using Android Keystore.  
**Encryption**: AES-256 with hardware-backed keys.  
**Usage**: Store SMB/SFTP passwords, OAuth tokens.  
**Library**: `androidx.security:security-crypto:1.1.0-alpha06`.

**Related Terms**: Android Keystore, Secure Storage

---

### OAuth 2.0
**Definition**: Authorization protocol for cloud services.  
**Flow**: Authorization Code flow with PKCE.  
**Tokens**: Access token (short-lived, 1 hour) + Refresh token (long-lived, 90 days).  
**Storage**: Refresh token in `EncryptedSharedPreferences`.

**Related Terms**: JWT, Authorization Code Grant

---

## Acronyms

| Acronym | Full Name | Definition |
|---------|-----------|------------|
| **ADR** | Architecture Decision Record | Document for significant architectural choices |
| **API** | Application Programming Interface | Contract for external service (Drive API, OneDrive API) |
| **CIFS** | Common Internet File System | Older name for SMB protocol |
| **DAO** | Data Access Object | Room interface for database queries |
| **DI** | Dependency Injection | Design pattern (Hilt implementation) |
| **DRM** | Digital Rights Management | Copy protection for media files |
| **EXIF** | Exchangeable Image File Format | Metadata in JPEG images (rotation, GPS) |
| **FTP** | File Transfer Protocol | Network file transfer protocol |
| **FTPS** | FTP Secure | FTP over TLS/SSL |
| **GIF** | Graphics Interchange Format | Animated image format |
| **HTTP** | Hypertext Transfer Protocol | Web protocol |
| **HTTPS** | HTTP Secure | HTTP over TLS |
| **JVM** | Java Virtual Machine | Runtime for Kotlin/Java code |
| **KPI** | Key Performance Indicator | Measurable metric (DAU, retention) |
| **LRU** | Least Recently Used | Cache eviction policy |
| **MIME** | Multipurpose Internet Mail Extensions | File type identifier (e.g., `image/jpeg`) |
| **MSAL** | Microsoft Authentication Library | OAuth library for OneDrive |
| **MVVM** | Model-View-ViewModel | UI design pattern |
| **OOM** | Out of Memory | Android crash when RAM exhausted |
| **ORM** | Object-Relational Mapping | Database abstraction (Room) |
| **PASV** | Passive Mode | FTP transfer mode |
| **PDF** | Portable Document Format | Document file type |
| **PKCE** | Proof Key for Code Exchange | OAuth security extension |
| **RGB** | Red Green Blue | Color model (RGB_565 = 16-bit color) |
| **SAF** | Storage Access Framework | Android API for file access |
| **SFTP** | SSH File Transfer Protocol | Secure file transfer over SSH |
| **SKU** | Stock Keeping Unit | Product variant (also Azure pricing tier) |
| **SMB** | Server Message Block | Windows file-sharing protocol |
| **SQL** | Structured Query Language | Database query language |
| **SSH** | Secure Shell | Encrypted remote access protocol |
| **TLS** | Transport Layer Security | Encryption for HTTPS/FTPS |
| **TTL** | Time To Live | Cache expiration time |
| **UI** | User Interface | Visual elements (Activities, Fragments) |
| **URI** | Uniform Resource Identifier | File/resource location string |
| **UX** | User Experience | How user interacts with app |
| **WebP** | Web Picture Format | Modern image format (better compression) |
| **XML** | Extensible Markup Language | Format for layouts and resources |

---

## Usage Notes

### Inconsistent Terms to Avoid
- ❌ "MediaResource" → Use **Resource**
- ❌ "File" (ambiguous) → Use **MediaFile** (domain) or **FileEntity** (database)
- ❌ "Storage" → Use **Resource** (for sources) or **Repository** (for data layer)
- ❌ "Cloud Folder" → Use **Resource** with `type = GOOGLE_DRIVE/ONEDRIVE/DROPBOX`

### Capitalization
- **Proper Names**: Google Drive, OneDrive, Dropbox (capitalize)
- **Generic Terms**: cloud storage, network share, local folder (lowercase)
- **Protocols**: SMB, SFTP, FTP (all caps)
- **Classes**: `PascalCase` (e.g., `MediaFileRepository`)
- **Variables**: `camelCase` (e.g., `resourceId`)

---

## Maintenance

**Update This Glossary When**:
- New feature introduces ambiguous terminology
- Team discussion reveals confusion about a term
- Refactoring changes canonical name for a concept

**Review Frequency**: Before each Epic milestone
