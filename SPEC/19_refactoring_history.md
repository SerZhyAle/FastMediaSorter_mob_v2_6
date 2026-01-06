# 19. Refactoring History

## Overview

This document captures major refactorings completed during FastMediaSorter v2 development. Each refactoring includes the problem, solution, and measurable impact.

---

## Phase 1: Quick Wins

**Timeline**: October 2024  
**Status**: ✅ Completed  
**Impact**: -391 lines

### Changes

1. **Removed Duplicate Utility Functions**
   - Consolidated 3 variants of `formatFileSize()` into single extension
   - Unified timestamp formatting across activities

2. **Extracted Common Dialog Logic**
   - Created `BaseDialogFragment` for repeated patterns
   - Reduced boilerplate in 12+ dialog classes

3. **Removed Dead Code**
   - Deleted unused `LegacyPlayerActivity` remnants
   - Removed commented-out experimental features

### Lessons Learned

- Start with low-risk, high-visibility changes
- Document which code is "experimental" vs "deprecated"
- Regular code reviews catch duplications early

---

## Phase 2: Strategy Pattern for File Operations

**Timeline**: November 2024  
**Status**: ✅ Completed  
**Impact**: -3,286 lines, +205 lines (net: -3,081 lines)

### Problem

**Quadratic Code Duplication**: With 5 protocols (Local, SMB, SFTP, FTP, Cloud), file operations required 5 × 5 = 25 handler combinations:

```
LocalToLocalHandler     SmbToLocalHandler      SftpToLocalHandler
LocalToSmbHandler       SmbToSmbHandler        SftpToSmbHandler
LocalToSftpHandler      SmbToSftpHandler       SftpToSftpHandler
LocalToFtpHandler       SmbToFtpHandler        SftpToFtpHandler
LocalToCloudHandler     SmbToCloudHandler      SftpToCloudHandler
```

Each handler: ~130 lines of code = **3,250 lines total**

**Maintenance Nightmare**: Adding FTP support required creating 10 new handlers (5 "from FTP" + 5 "to FTP").

### Solution

**Single Strategy Interface** with protocol-specific implementations:

```kotlin
interface FileOperationStrategy {
    suspend fun copy(source: MediaFile, destination: String): Result<FileOperationResult>
    suspend fun move(source: MediaFile, destination: String): Result<FileOperationResult>
    suspend fun delete(file: MediaFile): Result<Unit>
}
```

**Implementations**:
- `LocalOperationStrategy` (150 lines)
- `SmbOperationStrategy` (180 lines)
- `SftpOperationStrategy` (175 lines)
- `FtpOperationStrategy` (160 lines)
- `CloudOperationStrategy` (190 lines)

**Total**: 855 lines (vs. 3,250 lines before)

**Routing Logic**:
```kotlin
class FileOperationHandler @Inject constructor(
    private val localStrategy: LocalOperationStrategy,
    private val smbStrategy: SmbOperationStrategy,
    // ... other strategies
) {
    suspend fun copyFile(source: MediaFile, destination: Resource): Result<FileOperationResult> {
        val sourceStrategy = getStrategy(source.resourceType)
        val destStrategy = getStrategy(destination.resourceType)
        
        // Automatic cross-protocol handling
        return when {
            source.resourceType == destination.resourceType -> 
                sourceStrategy.copy(source, destination.path)
            else -> 
                downloadThenUpload(source, destination, sourceStrategy, destStrategy)
        }
    }
}
```

### Results

**Metrics**:
- **Code Reduction**: 71% fewer lines (-3,081 lines net)
- **Bug Fixes**: 1 fix now applies to all protocols
- **New Protocol Cost**: 1 new strategy class (~170 lines) vs. 10 handlers (~1,300 lines)

**Developer Experience**:
- Adding FTP support: 1 day vs. estimated 1 week before
- Cross-protocol operations work automatically
- Testability improved: mock single strategy vs. 25 handlers

### Migration Steps

1. **Created Interface** (1 hour)
2. **Implemented LocalOperationStrategy** (2 hours) - Used as template
3. **Copied pattern for other protocols** (4 hours)
4. **Updated FileOperationHandler** to use strategies (3 hours)
5. **Deleted old handlers** (1 hour)
6. **Testing** (8 hours) - All protocol combinations

**Total**: 19 hours vs. 40+ hours estimated for new protocol with old system

---

## Phase 3: PlayerActivity Decomposition

**Timeline**: November 2024  
**Status**: ✅ Completed  
**Impact**: -930 lines, +680 lines (net: -250 lines), improved maintainability

### Problem

**God Class**: `PlayerActivity.kt` = 2,700 lines with mixed responsibilities:
- Video/audio playback (ExoPlayer lifecycle)
- Image display (zoom, pan, rotation)
- Text/PDF/EPUB viewing
- File operations (copy, move, delete)
- Undo management
- Settings management
- UI state coordination
- Touch zone handling
- Keyboard shortcuts

**Symptoms**:
- 45-minute IDE indexing on file open
- Impossible to understand method call chains
- Every change risked breaking 3+ features
- Merge conflicts on every PR
- New developers: 2+ weeks to understand structure

### Solution

**Extracted 7 Focused Managers**:

#### 1. TextViewerManager (145 lines)
**Responsibility**: Text/PDF/EPUB display logic

```kotlin
class TextViewerManager(
    private val textViewer: TextView,
    private val pdfView: PDFView,
    private val epubReader: EpubReaderView
) {
    fun displayTextFile(file: MediaFile) { /* ... */ }
    fun displayPdfFile(file: MediaFile, page: Int = 0) { /* ... */ }
    fun displayEpubFile(file: MediaFile, chapter: Int = 0) { /* ... */ }
}
```

#### 2. PlayerUiStateCoordinator (180 lines)
**Responsibility**: UI state synchronization (buttons, overlays, toolbars)

```kotlin
class PlayerUiStateCoordinator(
    private val binding: ActivityPlayerUnifiedBinding
) {
    fun showVideoControls() { /* ... */ }
    fun hideImageControls() { /* ... */ }
    fun updateButtonStates(mediaType: MediaType) { /* ... */ }
}
```

#### 3. VideoPlayerManager (220 lines)
**Responsibility**: ExoPlayer lifecycle management

```kotlin
class VideoPlayerManager(
    private val context: Context,
    private val playerView: PlayerView
) {
    fun initializePlayer(mediaFile: MediaFile) { /* ... */ }
    fun play() { /* ... */ }
    fun pause() { /* ... */ }
    fun release() { /* ... */ }
}
```

#### 4. UndoOperationManager (135 lines)
**Responsibility**: Undo/redo logic, trash folder operations

```kotlin
class UndoOperationManager @Inject constructor(
    private val fileOperationHandler: FileOperationHandler
) {
    suspend fun undoLastOperation(): Result<String> { /* ... */ }
    fun hasUndoableOperation(): Boolean { /* ... */ }
}
```

#### 5. MediaDisplayCoordinator (160 lines)
**Responsibility**: Image/GIF rendering with Glide

```kotlin
class MediaDisplayCoordinator(
    private val imageView: PhotoView,
    private val gifView: ImageView
) {
    fun displayImage(file: MediaFile) { /* ... */ }
    fun displayGif(file: MediaFile) { /* ... */ }
}
```

#### 6. PlayerSettingsManager (95 lines)
**Responsibility**: Per-resource settings (slideshow interval, scan depth)

```kotlin
class PlayerSettingsManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun getSlideshowInterval(resourceId: Long): Int { /* ... */ }
    suspend fun getAutoAdvanceEnabled(resourceId: Long): Boolean { /* ... */ }
}
```

#### 7. ExoPlayerControlsManager (125 lines)
**Responsibility**: Custom playback controls (speed, forward/rewind)

```kotlin
class ExoPlayerControlsManager(
    private val player: ExoPlayer,
    private val controlsView: View
) {
    fun showSpeedControl() { /* ... */ }
    fun forward30Seconds() { /* ... */ }
    fun rewind10Seconds() { /* ... */ }
}
```

### Refactored PlayerActivity Structure

```kotlin
class PlayerActivity : BaseActivity<ActivityPlayerUnifiedBinding>() {
    
    // Managers injected via constructor
    @Inject lateinit var textViewerManager: TextViewerManager
    @Inject lateinit var uiStateCoordinator: PlayerUiStateCoordinator
    @Inject lateinit var videoPlayerManager: VideoPlayerManager
    // ... other managers
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeManagers()
        loadMediaFile()
    }
    
    private fun initializeManagers() {
        // Consolidated initialization
        textViewerManager.initialize(binding.textViewer)
        videoPlayerManager.initialize(binding.playerView)
        // ...
    }
    
    private fun displayMedia(file: MediaFile) {
        when (file.type) {
            MediaType.VIDEO -> videoPlayerManager.play(file)
            MediaType.IMAGE -> mediaDisplayCoordinator.displayImage(file)
            MediaType.TEXT -> textViewerManager.displayTextFile(file)
            // ...
        }
    }
    
    override fun onDestroy() {
        releaseResources()
        super.onDestroy()
    }
    
    private fun releaseResources() {
        videoPlayerManager.release()
        // ... other cleanup
    }
}
```

**New PlayerActivity size**: 720 lines (manageable, single-file review possible)

### Results

**Metrics**:
- **Code Reduction**: Net -250 lines (removed duplication during extraction)
- **Average Method Length**: 45 lines → 18 lines
- **Cyclomatic Complexity**: 87 → 23 per class
- **IDE Indexing**: 45s → 8s

**Developer Experience**:
- **Onboarding**: 2 weeks → 3 days to understand player logic
- **Bug Localization**: "Video controls not working" → Check `ExoPlayerControlsManager` (125 lines) vs. entire PlayerActivity (2,700 lines)
- **Testing**: Each manager unit-testable in isolation

**Example Bug Fix**: "Touch zones block video controls"
- **Before**: Search 2,700 lines, risk breaking image zoom
- **After**: `PlayerUiStateCoordinator.adjustTouchZonesForVideo()` (12 lines changed)

### Migration Steps

1. **Identified Responsibilities** (2 hours) - Analyzed method call graphs
2. **Created Manager Interfaces** (3 hours)
3. **Extracted Managers One-by-One** (16 hours total)
   - TextViewerManager: 2h
   - VideoPlayerManager: 3h
   - PlayerUiStateCoordinator: 4h
   - Others: 1-2h each
4. **Refactored PlayerActivity** (6 hours) - Consolidated initialization/cleanup
5. **Hilt DI Integration** (2 hours)
6. **Testing** (10 hours) - All media types, all file operations

**Total**: 39 hours, worth every minute for maintainability gains

---

## Phase 4: BaseConnectionPool

**Timeline**: December 2024  
**Status**: ✅ Infrastructure Complete, Full Migration Deferred  
**Impact**: +205 lines (infrastructure)

### Problem

**Duplicate Connection Pooling Logic**:
- `SmbClient`: Custom connection pool with `Mutex` and `MutableMap`
- `SftpClient`: Different pooling strategy with `ConcurrentHashMap`
- No idle connection cleanup
- Each protocol reimplemented same patterns

### Solution

**Generic Connection Pool**:

```kotlin
abstract class BaseConnectionPool<T : Any>(
    private val maxConnections: Int = 5,
    private val idleTimeout: Duration = 45.seconds
) {
    private val connections = ConcurrentHashMap<String, PooledConnection<T>>()
    private val mutex = Mutex()
    
    protected abstract suspend fun createConnection(key: String): T
    protected abstract suspend fun isValid(connection: T): Boolean
    protected abstract suspend fun closeConnection(connection: T)
    
    suspend fun getConnection(key: String): T = mutex.withLock {
        val pooled = connections[key]
        if (pooled != null && isValid(pooled.connection)) {
            pooled.lastUsed = System.currentTimeMillis()
            return pooled.connection
        }
        
        val newConnection = createConnection(key)
        connections[key] = PooledConnection(newConnection, System.currentTimeMillis())
        scheduleCleanup()
        return newConnection
    }
    
    private fun scheduleCleanup() {
        // Remove connections idle > 45 seconds
    }
}
```

**Usage Example**:

```kotlin
class SmbConnectionPool : BaseConnectionPool<SMBClient>() {
    override suspend fun createConnection(key: String): SMBClient {
        // Parse key: "server:share:user"
        return SMBClient().connect(server, share, username, password)
    }
    
    override suspend fun isValid(connection: SMBClient): Boolean {
        return connection.isConnected && !connection.getSession().isSessionClosed
    }
    
    override suspend fun closeConnection(connection: SMBClient) {
        connection.close()
    }
}
```

### Results

**Partial Implementation**:
- Infrastructure created (205 lines)
- `SmbClient` **not yet migrated** - existing pool works well
- `SftpClient` **not yet migrated** - protocol-specific complexities (key authentication)

**Reason for Deferral**:
- Existing pools stable and tested
- Full migration requires extensive testing (20+ hours estimated)
- Risk/reward not justified for current stability

**Future Work**:
- Migrate SMB when adding SMB3 encryption
- Migrate SFTP when adding connection multiplexing
- Add FTP connection pooling (currently creates new connection per operation)

---

## Lessons Learned

### 1. Measure Before Refactoring

Always capture metrics **before** starting:
- Lines of code
- Cyclomatic complexity
- Test coverage
- Build time
- Developer pain points (surveys)

**Example**: Phase 2 savings (71% code reduction) justified 19-hour investment

### 2. Refactor in Small Steps

**Anti-pattern**: "Big Bang" refactoring (rewrite entire activity in one PR)

**Better**: Extract one manager, test, merge, repeat

**Phase 3 Example**:
- 7 managers extracted across 7 PRs
- Each PR: 2-3 hours dev + 1 hour review
- Rollback risk minimal (old code still works)

### 3. Don't Over-Abstract Too Early

**Phase 4**: BaseConnectionPool created, but full migration deferred
- **Reason**: Protocol-specific nuances not yet fully understood
- **Right call**: Create infrastructure, use when proven necessary

**Rule**: "Three strikes and refactor" - wait for 3+ duplicates before abstracting

### 4. Preserve Old Code During Migration

**Example**: Strategy Pattern migration kept old handlers for 2 weeks
- Feature flag: `USE_STRATEGY_PATTERN = true/false`
- A/B testing in production
- Easy rollback on critical bugs

### 5. Document "Why", Not Just "What"

Each refactoring document includes:
- ❌ "Extracted VideoPlayerManager" (what)
- ✅ "Extracted VideoPlayerManager to reduce PlayerActivity complexity from 2,700 lines to 720 lines, reducing IDE indexing from 45s to 8s" (why + impact)

---

## Refactoring Backlog

### Planned (Not Started)

1. **ViewPager2 Migration** (See `dev/VIEWPAGER2_REFACTORING_VISION.md`)
   - Replace custom swipe navigation with ViewPager2
   - Estimated: 30 hours

2. **Unified File Operation Handler** (Phase 3-6)
   - Further consolidate handler logic
   - Estimated: 15 hours

3. **Compose Migration** (2026 Q2)
   - Gradual migration from XML to Jetpack Compose
   - Start with: Settings screens, Dialogs
   - Estimated: 80+ hours over 3 months

---

## Related Documentation

- [17. Architecture Patterns](17_architecture_patterns.md) - Strategy Pattern details
- [18. Development Workflows](18_development_workflows.md) - Testing refactored code
- [21. Common Pitfalls](21_common_pitfalls.md) - Issues discovered during refactoring
