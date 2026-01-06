# 26. Database Schema & Migrations

**Last Updated**: January 6, 2026  
**Current Version**: Room DB v1  
**Technology**: Room Persistence Library 2.6.1

This document provides complete database schema specification including all tables, relationships, indexes, and migration history.

---

## Overview

FastMediaSorter uses Room (SQLite wrapper) for local data persistence. The database stores:
- Resource configurations (local folders, SMB/SFTP/FTP shares, cloud folders)
- Network credentials (encrypted)
- User favorites
- Playback positions (audiobook mode)
- Thumbnail cache metadata
- Full-text search index

**Database Name**: `fastmediasorter_v2.db`  
**Location**: `/data/data/com.sza.fastmediasorter/databases/`

---

## Entity-Relationship Diagram

**Full diagram**: [diagrams/database_er_diagram.md](diagrams/database_er_diagram.md)

**Summary**:
- **resources** (Main table): PK id, name, path, type, credentialsId (FK), cloudProvider, cloudFolderId, etc.
- **network_credentials**: PK id, type, server, port, username, password (encrypted), domain, shareName, sshPrivateKey
- **favorites**: PK id, uri, resourceId (FK), displayName, mediaType, size, dateModified, addedTimestamp
- **playback_positions**: PK filePath, position, duration, lastPlayedAt, isCompleted
- **thumbnail_cache**: PK filePath, localCachePath, lastAccessedAt, fileSize
- **resources_fts**: Virtual FTS4 table for full-text search on name + path

**Relationships**:
- resources ← network_credentials (1:N optional via credentialsId)
- resources → favorites (1:N via resourceId)

---

## Table Definitions

### 1. `resources` - Main Resource Table

**Purpose**: Stores all media sources (local folders, SMB/SFTP/FTP shares, cloud folders).

**Kotlin Entity**: `ResourceEntity.kt`

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | - | Unique resource ID |
| `name` | TEXT | NOT NULL | - | Display name |
| `path` | TEXT | NOT NULL | - | Full path/URI (local: `/storage/...`, SMB: `smb://...`) |
| `type` | TEXT | NOT NULL | - | `LOCAL`, `SMB`, `SFTP`, `FTP`, `CLOUD` |
| `credentialsId` | TEXT | NULLABLE, FK→network_credentials.credentialId | NULL | Reference to credentials (SMB/SFTP only) |
| `cloudProvider` | TEXT | NULLABLE | NULL | `GOOGLE_DRIVE`, `ONEDRIVE`, `DROPBOX` |
| `cloudFolderId` | TEXT | NULLABLE | NULL | Cloud-specific folder ID |
| `supportedMediaTypesFlags` | INTEGER | NOT NULL | 0b1111 | Bitmask: IMAGE(1), VIDEO(2), AUDIO(4), GIF(8), TEXT(16), PDF(32), EPUB(64) |
| `sortMode` | TEXT | NOT NULL | `NAME_ASC` | `NAME_ASC`, `NAME_DESC`, `DATE_ASC`, `DATE_DESC`, `SIZE_ASC`, `SIZE_DESC` |
| `displayMode` | TEXT | NOT NULL | `LIST` | `LIST`, `GRID_2`, `GRID_3`, `GRID_4` |
| `lastViewedFile` | TEXT | NULLABLE | NULL | Last opened file path |
| `lastScrollPosition` | INTEGER | NOT NULL | 0 | RecyclerView scroll position |
| `fileCount` | INTEGER | NOT NULL | 0 | Cached file count (updated on scan) |
| `lastAccessedDate` | INTEGER | NOT NULL | current_timestamp | Last opened timestamp (ms) |
| `slideshowInterval` | INTEGER | NOT NULL | 10 | Slideshow delay (seconds) |
| `isDestination` | INTEGER | NOT NULL | 0 | Boolean: Is quick destination (0/1) |
| `destinationOrder` | INTEGER | NOT NULL | -1 | Destination sort order (1-10, -1 = not destination) |
| `destinationColor` | INTEGER | NOT NULL | 0xFF4CAF50 | Color for destination button |
| `isWritable` | INTEGER | NOT NULL | 0 | Boolean: Supports file operations (0/1) |
| `isReadOnly` | INTEGER | NOT NULL | 0 | Boolean: Read-only mode (0/1) |
| `isAvailable` | INTEGER | NOT NULL | 1 | Boolean: Resource availability (0/1) |
| `showCommandPanel` | INTEGER | NULLABLE | NULL | Boolean: Show command panel (NULL = use global setting) |
| `createdDate` | INTEGER | NOT NULL | current_timestamp | Creation timestamp (ms) |
| `lastBrowseDate` | INTEGER | NULLABLE | NULL | Last opened in BrowseActivity (ms) |
| `lastSyncDate` | INTEGER | NULLABLE | NULL | Last network sync (ms) |
| `scanSubdirectories` | INTEGER | NOT NULL | 0 | Boolean: Recursive scan (0/1) |
| `disableThumbnails` | INTEGER | NOT NULL | 0 | Boolean: Disable thumbnails (0/1) |
| `displayOrder` | INTEGER | NOT NULL | 0 | Resource list sort order |
| `accessPin` | TEXT | NULLABLE | NULL | PIN for access (encrypted) |
| `comment` | TEXT | NULLABLE | NULL | User comment |
| `read_speed_mbps` | REAL | NULLABLE | NULL | Measured read speed (Mbps) |
| `write_speed_mbps` | REAL | NULLABLE | NULL | Measured write speed (Mbps) |
| `recommended_threads` | INTEGER | NULLABLE | NULL | Optimal thread count |
| `last_speed_test_date` | INTEGER | NULLABLE | NULL | Last speed test timestamp (ms) |

**Indexes**:
```sql
CREATE INDEX idx_resources_display_order_name ON resources(displayOrder, name);
CREATE INDEX idx_resources_type_display_order_name ON resources(type, displayOrder, name);
CREATE INDEX idx_resources_is_destination_order ON resources(isDestination, destinationOrder);
CREATE INDEX idx_resources_media_types ON resources(supportedMediaTypesFlags);
```

**Business Rules**:
- `destinationOrder`: Max 10 destinations (1-10), enforced in ViewModel
- `credentialsId`: Must exist in `network_credentials.credentialId` if not NULL
- `cloudProvider`: Required when `type = CLOUD`, NULL otherwise
- `fileCount`: Auto-updated by scan operations, not user-editable
- `disableThumbnails`: Auto-enabled when `fileCount > 10000`

---

### 2. `network_credentials` - Network Authentication

**Purpose**: Stores encrypted credentials for SMB/SFTP/FTP resources (separated for security).

**Kotlin Entity**: `NetworkCredentialsEntity.kt`

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | - | Auto-generated ID |
| `credentialId` | TEXT | NOT NULL, UNIQUE | - | UUID (referenced by resources.credentialsId) |
| `type` | TEXT | NOT NULL | - | `SMB`, `SFTP`, `FTP` |
| `server` | TEXT | NOT NULL | - | Server address/IP |
| `port` | INTEGER | NOT NULL | - | 445 (SMB), 22 (SFTP), 21 (FTP) |
| `username` | TEXT | NOT NULL | - | Login username |
| `password` | TEXT | NOT NULL | - | **Encrypted with Android Keystore** |
| `domain` | TEXT | NOT NULL | "" | SMB domain (Windows auth) |
| `shareName` | TEXT | NULLABLE | NULL | SMB share name |
| `sshPrivateKey` | TEXT | NULLABLE | NULL | SFTP private key (encrypted PEM format) |
| `createdDate` | INTEGER | NOT NULL | current_timestamp | Creation timestamp (ms) |

**Indexes**: None (small table, UUID lookups are fast enough)

**Security**:
- `password`: Encrypted using `CryptoHelper` with Android Keystore (AES-256-GCM)
- `sshPrivateKey`: Also encrypted with same method
- Decryption happens in-memory via `@Ignore val password: String` getter

**Special Handling**:
- Entity has `@Ignore` property that auto-decrypts password on access
- Migration case: Plaintext passwords are detected and re-encrypted on first read

---

### 3. `favorites` - User Favorites

**Purpose**: Stores user-favorited files for quick access (cross-resource aggregation).

**Kotlin Entity**: `FavoritesEntity.kt`

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT | - | Auto-generated ID |
| `uri` | TEXT | NOT NULL, UNIQUE | - | Full file path/URI |
| `resourceId` | INTEGER | NOT NULL, FK→resources.id | - | Parent resource ID |
| `displayName` | TEXT | NOT NULL | - | File display name |
| `mediaType` | INTEGER | NOT NULL | - | Media type flag (1=Image, 2=Video, etc) |
| `size` | INTEGER | NOT NULL | - | File size (bytes) |
| `lastKnownPath` | TEXT | NOT NULL | - | Redundant path for fallback |
| `dateModified` | INTEGER | NOT NULL | - | File modification date (ms) |
| `addedTimestamp` | INTEGER | NOT NULL | current_timestamp | Favorite added date (ms) |
| `thumbnailPath` | TEXT | NULLABLE | NULL | Local thumbnail cache path |

**Indexes**:
```sql
CREATE UNIQUE INDEX idx_favorites_uri ON favorites(uri);
CREATE INDEX idx_favorites_addedTimestamp ON favorites(addedTimestamp);
```

**Business Rules**:
- `uri` uniqueness prevents duplicate favorites
- `resourceId` FK: Cascade delete when resource deleted
- Sorted by `addedTimestamp DESC` (most recent first)

---

### 4. `playback_positions` - Resume Playback

**Purpose**: Stores playback positions for audio/video files (audiobook mode).

**Kotlin Entity**: `PlaybackPositionEntity.kt`

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| `filePath` | TEXT | PRIMARY KEY | - | Full file path |
| `position` | INTEGER | NOT NULL | - | Playback position (milliseconds) |
| `duration` | INTEGER | NOT NULL | - | File duration (milliseconds) |
| `lastPlayedAt` | INTEGER | NOT NULL | current_timestamp | Last played timestamp (ms) |
| `isCompleted` | INTEGER | NOT NULL | 0 | Boolean: >95% watched (0/1) |

**Indexes**: None (PK on filePath is sufficient)

**Business Rules**:
- `isCompleted`: Auto-set to 1 when `position > 0.95 * duration`
- Max 10,000 entries (FIFO cleanup: trim to 9,000 when limit reached)
- Cleanup triggered by `PlaybackPositionRepository.cleanupOldPositions()`

---

### 5. `thumbnail_cache` - Network Thumbnail Cache

**Purpose**: Metadata for locally cached network video thumbnails (Glide handles images).

**Kotlin Entity**: `ThumbnailCacheEntity.kt`

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| `filePath` | TEXT | PRIMARY KEY | - | Original file path (network) |
| `localCachePath` | TEXT | NOT NULL | - | Local cache file path |
| `lastAccessedAt` | INTEGER | NOT NULL | current_timestamp | Last access timestamp (ms) |
| `fileSize` | INTEGER | NOT NULL | - | Thumbnail file size (bytes) |

**Indexes**:
```sql
CREATE INDEX idx_thumbnail_cache_lastAccessedAt ON thumbnail_cache(lastAccessedAt);
```

**Business Rules**:
- Cache cleanup: Remove entries not accessed for 30 days
- Cleanup triggered by `CleanupThumbnailCacheWorker` (WorkManager, daily)
- Physical files in `/data/data/.../cache/thumbnails/`

---

### 6. `resources_fts` - Full-Text Search Index

**Purpose**: FTS4 virtual table for fast resource name/path search.

**Kotlin Entity**: `ResourceFtsEntity.kt`

| Column | Type | Description |
|--------|------|-------------|
| `name` | TEXT | Indexed resource name |
| `path` | TEXT | Indexed resource path |

**FTS Configuration**:
```sql
CREATE VIRTUAL TABLE resources_fts USING fts4(
    content='resources',
    name,
    path
);
```

**Query Example**:
```kotlin
// DAO method
@Query("""
    SELECT r.* 
    FROM resources r
    JOIN resources_fts fts ON r.id = fts.docid
    WHERE fts.name MATCH :query
""")
fun searchResourcesFts(query: String): List<ResourceEntity>
```

**Triggers** (Auto-managed by FTS4):
- `INSERT` on `resources` → Update FTS index
- `UPDATE` on `resources` → Rebuild FTS index for row
- `DELETE` on `resources` → Remove from FTS index

---

## Type Converters

Room requires TypeConverters for non-primitive types. All converters in `Converters.kt`:

### ResourceType Enum
```kotlin
enum class ResourceType {
    LOCAL, SMB, SFTP, FTP, CLOUD
}

// Converter
@TypeConverter
fun fromResourceType(type: ResourceType): String = type.name

@TypeConverter
fun toResourceType(value: String): ResourceType = ResourceType.valueOf(value)
```

### SortMode Enum
```kotlin
enum class SortMode {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC
}
```

### DisplayMode Enum
```kotlin
enum class DisplayMode {
    LIST, GRID_2, GRID_3, GRID_4
}
```

### CloudProvider Enum
```kotlin
enum class CloudProvider {
    GOOGLE_DRIVE, ONEDRIVE, DROPBOX
}
```

---

## Database Migrations

### Current Status
**Version**: 1 (initial schema)  
**Migration Strategy**: `fallbackToDestructiveMigration()` (data loss on upgrade)

### Implemented Schema (Version 1)
All features (Cloud, Speed Test, PIN) are included in the initial Version 1 schema.

### Future Migrations (Planned)
No pending migrations. The following are placeholders for potential future updates.
```kotlin
<!-- Migrations 1-4 integrated into Version 1 -->

### Migration Best Practices
1. **Always increment version** in `@Database` annotation
2. **Export schema** (`exportSchema = true`) for version tracking
3. **Test migrations** with `MigrationTestHelper` (instrumentation tests)
4. **Never delete columns** (SQLite limitation) - use `@Ignore` instead
5. **Provide rollback plan** for critical migrations

---

## Database Initialization

### Hilt Module Configuration
```kotlin
// DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fastmediasorter_v2.db"
        )
            .fallbackToDestructiveMigration() // TODO: Replace with proper migrations
            // .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
    
    @Provides
    fun provideResourceDao(database: AppDatabase): ResourceDao = database.resourceDao()
    
    @Provides
    fun provideNetworkCredentialsDao(database: AppDatabase): NetworkCredentialsDao = 
        database.networkCredentialsDao()
    
    @Provides
    fun provideFavoritesDao(database: AppDatabase): FavoritesDao = database.favoritesDao()
    
    @Provides
    fun providePlaybackPositionDao(database: AppDatabase): PlaybackPositionDao = 
        database.playbackPositionDao()
    
    @Provides
    fun provideThumbnailCacheDao(database: AppDatabase): ThumbnailCacheDao = 
        database.thumbnailCacheDao()
}
```

---

## Query Optimization Guidelines

### 1. Use Indexes Effectively
```kotlin
// ✅ GOOD: Uses idx_resources_is_destination_order
resourceDao.getDestinations() // WHERE isDestination = 1 ORDER BY destinationOrder

// ✅ GOOD: Uses idx_resources_display_order_name
resourceDao.getAllResources() // ORDER BY displayOrder, name

// ❌ BAD: No index on lastBrowseDate
// SELECT * FROM resources WHERE lastBrowseDate > ? // Full table scan
```

### 2. Batch Operations
```kotlin
// ✅ GOOD: Single transaction
@Transaction
suspend fun updateMultipleResources(updates: List<ResourceEntity>) {
    updates.forEach { resourceDao.update(it) }
}

// ❌ BAD: Multiple transactions
updates.forEach { resourceDao.update(it) } // Each call is separate transaction
```

### 3. Avoid N+1 Queries
```kotlin
// ✅ GOOD: Single JOIN query
@Query("""
    SELECT r.*, c.username, c.server 
    FROM resources r 
    LEFT JOIN network_credentials c ON r.credentialsId = c.credentialId
    WHERE r.type IN ('SMB', 'SFTP')
""")
fun getNetworkResourcesWithCredentials(): List<ResourceWithCredentials>

// ❌ BAD: N+1 problem
val resources = resourceDao.getAllResources() // 1 query
resources.forEach { resource ->
    val creds = credentialsDao.getByCredentialId(resource.credentialsId) // N queries
}
```

### 4. Limit Result Sets
```kotlin
// ✅ GOOD: Pagination
@Query("SELECT * FROM resources ORDER BY displayOrder LIMIT :limit OFFSET :offset")
suspend fun getResourcesPaged(limit: Int, offset: Int): List<ResourceEntity>

// ❌ BAD: Loading all data
@Query("SELECT * FROM resources")
suspend fun getAllResources(): List<ResourceEntity> // 1000+ rows in memory
```

---

## Database Testing Strategy

### 1. DAO Tests (Instrumentation)
```kotlin
@RunWith(AndroidJUnit4::class)
class ResourceDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var resourceDao: ResourceDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        resourceDao = database.resourceDao()
    }
    
    @Test
    fun insertAndRetrieveResource() = runTest {
        val resource = ResourceEntity(name = "Test", path = "/test", type = ResourceType.LOCAL)
        val id = resourceDao.insert(resource)
        
        val retrieved = resourceDao.getResourceByIdSync(id)
        assertEquals("Test", retrieved?.name)
    }
    
    @After
    fun teardown() {
        database.close()
    }
}
```

### 2. Migration Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    fun migrate1To2() {
        // Create DB at version 1
        val db = helper.createDatabase("test_db", 1)
        db.execSQL("INSERT INTO resources (name, path, type) VALUES ('Test', '/path', 'LOCAL')")
        db.close()
        
        // Migrate to version 2
        helper.runMigrationsAndValidate("test_db", 2, true, MIGRATION_1_2)
        
        // Verify columns exist
        val dbV2 = helper.runMigrationsAndValidate("test_db", 2, true)
        val cursor = dbV2.query("SELECT cloudProvider FROM resources")
        assertTrue(cursor.moveToFirst())
        assertNull(cursor.getString(0)) // cloudProvider should be NULL
    }
}
```

---

## Troubleshooting

### Common Issues

#### 1. Migration Not Applied
**Symptom**: `IllegalStateException: A migration from X to Y was required but not found`

**Solution**:
```kotlin
// Add migration to database builder
.addMigrations(MIGRATION_X_Y)
```

#### 2. Column Not Found
**Symptom**: `SQLiteException: no such column: cloudProvider`

**Solution**: Database schema mismatch. Options:
- Clear app data (loses all data)
- Implement proper migration
- Use `fallbackToDestructiveMigration()` (dev only)

#### 3. FTS Index Out of Sync
**Symptom**: Search returns outdated results

**Solution**:
```kotlin
@Query("INSERT INTO resources_fts(resources_fts) VALUES('rebuild')")
suspend fun rebuildFtsIndex()
```

#### 4. Credentials Decryption Failure
**Symptom**: `CryptoException` when accessing `NetworkCredentialsEntity.password`

**Solution**: Keystore was cleared (app reinstall, device encryption change)
- Re-prompt user for credentials
- Use `plaintext` fallback detection (see entity getter)

---

## Performance Benchmarks

Target query performance (measured on Pixel 4, Android 11):

| Operation | Target | Notes |
|-----------|--------|-------|
| Get all resources | < 16 ms | Critical for MainActivity |
| Get destinations | < 8 ms | Frequently accessed |
| Search resources (FTS) | < 50 ms | User-initiated |
| Insert resource | < 10 ms | One-time operation |
| Update resource | < 10 ms | Infrequent |
| Get favorites | < 20 ms | Widget data source |
| Playback position lookup | < 5 ms | Per file open |

**Profiling Command**:
```bash
adb shell am broadcast -a com.sza.fastmediasorter.DEBUG_DATABASE_PROFILING
```

---

## Security Considerations

### 1. Encrypted Fields
- `network_credentials.password`: AES-256-GCM (Android Keystore)
- `network_credentials.sshPrivateKey`: AES-256-GCM
- `resources.accessPin`: SHA-256 hash (verify only, never decrypt)

### 2. SQL Injection Prevention
Room uses parameterized queries automatically. Never concatenate user input into raw SQL:

```kotlin
// ✅ SAFE: Parameterized query
@Query("SELECT * FROM resources WHERE name = :name")
fun getByName(name: String): ResourceEntity?

// ❌ UNSAFE: String concatenation (Room won't compile this)
// @RawQuery
// fun getByName(name: String) = 
//     database.query("SELECT * FROM resources WHERE name = '$name'")
```

### 3. Backup Exclusion
`AndroidManifest.xml`:
```xml
<application android:allowBackup="false" android:fullBackupContent="false">
    <!-- Prevents sensitive data from cloud backup -->
</application>
```

---

## Future Enhancements

### Planned Features (v2.0)

1. **Multi-Resource Sync**
   - Track file operations across resources
   - Conflict resolution for bidirectional sync

2. **Resource Groups**
   - New table: `resource_groups` (parent-child hierarchy)
   - Use case: Group all SMB shares from same server

3. **File History**
   - New table: `file_operations_log`
   - Track all copy/move/delete operations for undo beyond current session

4. **Smart Collections**
   - New table: `smart_collections`
   - Auto-generated playlists based on rules (e.g., "Recently Added Videos")

5. **Collaborative Favorites**
   - Cloud-sync favorites across devices (Firebase Firestore)

---

## Reference Files

### Source Code Locations
- **Entities**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/db/*Entity.kt`
- **DAOs**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/db/*Dao.kt`
- **Database**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/db/AppDatabase.kt`
- **Converters**: `app_v2/src/main/java/com/sza/fastmediasorter/data/local/db/Converters.kt`
- **DI Module**: `app_v2/src/main/java/com/sza/fastmediasorter/core/di/DatabaseModule.kt`

### Testing
- **DAO Tests**: `app_v2/src/androidTest/java/com/sza/fastmediasorter/data/local/dao/*Test.kt`
- **Migration Tests**: (To be created)

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
