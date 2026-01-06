# 17. Architecture Patterns

## Overview

FastMediaSorter v2 follows **Clean Architecture** principles with strict separation of concerns across three layers: UI, Domain, and Data.

---

## Three-Layer Structure

```
UI Layer (ui/)
  ↓ (observes StateFlow/SharedFlow)
Domain Layer (domain/)
  ↓ (depends on Repository interfaces)
Data Layer (data/)
```

### Dependency Rule

**UI** → **Domain** (via UseCases) → **Data** (via Repository interfaces)

**Critical**: Upper layers never depend on lower layer implementations, only on interfaces.

---

## Key Design Patterns

### 1. ViewModels (MVVM)

**Purpose**: Manage UI state and business logic orchestration

**Pattern**:
```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {
    
    // State exposure via StateFlow
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    // One-time events via SharedFlow
    private val _events = MutableSharedFlow<BrowseEvent>()
    val events: SharedFlow<BrowseEvent> = _events.asSharedFlow()
    
    fun loadFiles(resourceId: Long) {
        viewModelScope.launch {
            getMediaFilesUseCase(resourceId)
                .onSuccess { files -> _uiState.value = BrowseUiState.Success(files) }
                .onFailure { error -> _events.emit(BrowseEvent.Error(error.message)) }
        }
    }
}
```

**Rules**:
- Annotate with `@HiltViewModel`
- Inject UseCases via constructor (NEVER repositories directly)
- Expose `StateFlow` for continuous state
- Expose `SharedFlow` for one-time events
- Use `viewModelScope` for coroutine lifecycle management

---

### 2. UseCases (Single Responsibility)

**Purpose**: Encapsulate single business operation

**Pattern**:
```kotlin
class GetMediaFilesUseCase @Inject constructor(
    private val mediaRepository: MediaRepository // Interface, not implementation
) {
    suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
        return try {
            val files = mediaRepository.getFilesForResource(resourceId)
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Naming Convention**: `VerbNounUseCase`
- ✅ `GetMediaFilesUseCase`, `MoveFileUseCase`, `DeleteFileUseCase`
- ❌ `MediaUseCase`, `FileHandler`, `FilesManager`

**Rules**:
- One public method: `operator fun invoke()` or `suspend operator fun invoke()`
- Only depend on repository **interfaces** from `domain/repository/`
- Return `Result<T>` or custom sealed classes
- No Android framework dependencies (Context, Activity, etc.)

---

### 3. Events (Sealed Classes)

**Purpose**: Type-safe ViewModel→View communication for one-time actions

**Pattern**:
```kotlin
sealed class BrowseEvent {
    data class Error(val message: String) : BrowseEvent()
    data class FileDeleted(val filename: String) : BrowseEvent()
    data class NavigateToPlayer(val fileIndex: Int) : BrowseEvent()
    object ShowUndoSnackbar : BrowseEvent()
}

// In Activity/Fragment
lifecycleScope.launch {
    viewModel.events.collect { event ->
        when (event) {
            is BrowseEvent.Error -> showErrorDialog(event.message)
            is BrowseEvent.FileDeleted -> showToast("Deleted: ${event.filename}")
            is BrowseEvent.NavigateToPlayer -> openPlayer(event.fileIndex)
            BrowseEvent.ShowUndoSnackbar -> showUndoSnackbar()
        }
    }
}
```

**Why Sealed Classes?**
- Compile-time exhaustive `when` checks
- Type-safe data passing
- Better than `Pair<EventType, Any?>`
- IDE auto-completion support

---

### 4. Strategy Pattern (File Operations)

**Problem**: Quadratic code duplication (5 protocols × 5 protocols = 25 handler combinations)

**Solution**: Single interface with protocol-specific implementations

```kotlin
interface FileOperationStrategy {
    suspend fun copy(
        source: MediaFile, 
        destination: String
    ): Result<FileOperationResult>
    
    suspend fun move(
        source: MediaFile, 
        destination: String
    ): Result<FileOperationResult>
    
    suspend fun delete(file: MediaFile): Result<Unit>
}
```

**Implementations**:
- `LocalOperationStrategy` - Local file system
- `SmbOperationStrategy` - SMB network shares
- `SftpOperationStrategy` - SFTP servers
- `FtpOperationStrategy` - FTP servers
- `CloudOperationStrategy` - Google Drive/OneDrive/Dropbox

**Factory Selection**:
```kotlin
fun getStrategy(resourceType: ResourceType): FileOperationStrategy {
    return when (resourceType) {
        ResourceType.LOCAL, ResourceType.SAF -> LocalOperationStrategy()
        ResourceType.SMB -> SmbOperationStrategy()
        ResourceType.SFTP -> SftpOperationStrategy()
        ResourceType.FTP -> FtpOperationStrategy()
        ResourceType.CLOUD -> CloudOperationStrategy()
    }
}
```

**Impact**: 
- Eliminated ~3,286 lines of duplicated code
- Automatic cross-protocol routing (e.g., SMB→Local, Cloud→SFTP)
- Single source of truth for each protocol

---

### 5. Repository Pattern

**Purpose**: Abstract data source details from domain layer

```kotlin
// Domain layer interface (domain/repository/)
interface MediaRepository {
    suspend fun getFilesForResource(resourceId: Long): List<MediaFile>
    suspend fun markAsFavorite(fileId: Long): Result<Unit>
}

// Data layer implementation (data/repository/)
class MediaRepositoryImpl @Inject constructor(
    private val mediaDao: MediaDao,
    private val localScanner: LocalMediaScanner,
    private val smbScanner: SmbMediaScanner
) : MediaRepository {
    override suspend fun getFilesForResource(resourceId: Long): List<MediaFile> {
        // Implementation details hidden from domain layer
    }
}
```

**Hilt Binding**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository
}
```

---

## Architectural Benefits

### 1. Testability
- UseCases can be tested with mocked repositories
- ViewModels can be tested with mocked UseCases
- No Android framework dependencies in domain layer

### 2. Maintainability
- Clear boundaries between layers
- Changes in data layer don't affect domain/UI
- Single Responsibility Principle enforced

### 3. Scalability
- Easy to add new protocols (just implement `FileOperationStrategy`)
- Easy to add new features (create new UseCase)
- No ripple effects across layers

### 4. Team Collaboration
- Different teams can work on different layers independently
- Clear contracts via interfaces
- Reduced merge conflicts

---

## Anti-Patterns to Avoid

### ❌ Direct Repository Injection in ViewModels
```kotlin
// WRONG
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val mediaRepository: MediaRepository // Direct dependency
)
```

**Problem**: Bypasses domain layer, mixes business logic with UI logic

**Fix**: Always inject UseCases

---

### ❌ Android Framework in Domain Layer
```kotlin
// WRONG
class GetMediaFilesUseCase @Inject constructor(
    private val context: Context // Android dependency
)
```

**Problem**: Makes domain layer untestable, violates Clean Architecture

**Fix**: Pass required data as parameters, not framework objects

---

### ❌ God Classes (2,700-line Activities)
**Problem**: Mixed responsibilities, hard to test, hard to maintain

**Fix**: Extract managers/helpers (see Phase 3 refactoring in [19_refactoring_history.md](19_refactoring_history.md))

---

## Related Documentation

- [11. ViewModels](11_viewmodels.md) - Complete ViewModel catalog
- [13. Data Models](13_data_models.md) - Room entities, domain models
- [19. Refactoring History](19_refactoring_history.md) - Strategy Pattern implementation details
- [23. Code Conventions](23_code_conventions.md) - Naming and style rules
