# 23. Code Conventions

## Overview

Consistent code style ensures maintainability, readability, and reduces cognitive load when switching between files. FastMediaSorter v2 follows Kotlin official style guide with project-specific additions.

---

## Language Policy

### Code and Comments

**Rule**: All code, variable names, class names, comments, and commit messages MUST be in **English**.

```kotlin
// ✅ GOOD
class MediaFileAdapter(
    private val onClick: (MediaFile) -> Unit
) : ListAdapter<MediaFile, MediaFileViewHolder>(MediaFileDiffCallback()) {
    // Creates view holder for media file item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaFileViewHolder {
        // ...
    }
}

// ❌ BAD (Russian/Ukrainian comments)
class MediaFileAdapter(
    private val onClick: (MediaFile) -> Unit
) : ListAdapter<MediaFile, MediaFileViewHolder>(MediaFileDiffCallback()) {
    // Создаёт холдер для элемента медиа-файла
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaFileViewHolder {
        // ...
    }
}
```

**Rationale**:
- Open-source community expects English
- AI tools (GitHub Copilot, ChatGPT) work better with English
- Non-native speakers can use translation tools for English, but not for comments in Cyrillic

### User-Facing Strings

**Rule**: All user-facing text MUST use string resources with localizations.

**Supported Locales**: `en` (English), `ru` (Russian), `uk` (Ukrainian)

```kotlin
// ✅ GOOD
showToast(getString(R.string.file_deleted_successfully))

// ❌ BAD (hardcoded text)
showToast("File deleted successfully")
showToast("Файл удалён успешно")
```

**String Resource Structure**:

```xml
<!-- values/strings.xml (English) -->
<string name="file_deleted_successfully">File deleted successfully</string>
<string name="error_network_connection">Network connection error</string>

<!-- values-ru/strings.xml (Russian) -->
<string name="file_deleted_successfully">Файл успешно удалён</string>
<string name="error_network_connection">Ошибка сетевого подключения</string>

<!-- values-uk/strings.xml (Ukrainian) -->
<string name="file_deleted_successfully">Файл успішно видалено</string>
<string name="error_network_connection">Помилка мережевого з'єднання</string>
```

---

## Naming Conventions

### Classes

**Pattern**: `PascalCase` with descriptive suffix

| Type | Suffix | Example |
|------|--------|---------|
| Activity | `Activity` | `BrowseActivity`, `PlayerActivity` |
| Fragment | `Fragment` | `GeneralSettingsFragment` |
| ViewModel | `ViewModel` | `BrowseViewModel` |
| UseCase | `UseCase` | `GetMediaFilesUseCase` |
| Repository | `Repository` | `MediaRepository` |
| Repository Impl | `RepositoryImpl` | `MediaRepositoryImpl` |
| Adapter | `Adapter` | `MediaFileAdapter` |
| ViewHolder | `ViewHolder` | `MediaFileViewHolder` |
| Dialog | `Dialog` | `ConfirmDeleteDialog` |
| Manager | `Manager` | `VideoPlayerManager` |
| Client | `Client` | `SmbClient`, `SftpClient` |

```kotlin
// ✅ GOOD
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel()

// ❌ BAD
class Browse // Missing ViewModel suffix
class VMBrowse // Non-standard prefix
class BrowseVM // Abbreviation
```

### UseCases

**Pattern**: `VerbNounUseCase`

```kotlin
// ✅ GOOD
GetMediaFilesUseCase
MoveFileUseCase
DeleteFileUseCase
CopyFileUseCase
MarkAsFavoriteUseCase
GetThumbnailUseCase

// ❌ BAD
MediaFilesUseCase // Missing verb
FileHandler // Not a UseCase
ManageFile // Vague verb
```

### Variables

**Pattern**: `camelCase` with descriptive names

```kotlin
// ✅ GOOD
private val mediaFiles = mutableListOf<MediaFile>()
private var currentFileIndex = 0
private val isLoading = MutableStateFlow(false)

// ❌ BAD
private val mf = mutableListOf<MediaFile>() // Too short
private var idx = 0 // Abbreviation
private val _loading = MutableStateFlow(false) // Leading underscore for non-backing fields
```

**Backing Fields** (private mutable + public immutable):

```kotlin
// ✅ GOOD
private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

// ❌ BAD
private val uiStateMutable = MutableStateFlow(...) // Redundant suffix
val uiStateFlow: StateFlow<...> // Redundant suffix
```

### Constants

**Pattern**: `UPPER_SNAKE_CASE` in companion object

```kotlin
// ✅ GOOD
companion object {
    private const val PAGINATION_THRESHOLD = 1000
    private const val REQUEST_CODE_PERMISSIONS = 101
    const val EXTRA_RESOURCE_ID = "extra_resource_id" // Intent extra
}

// ❌ BAD
private const val paginationThreshold = 1000 // camelCase
private const val PaginationThreshold = 1000 // PascalCase
```

### Functions

**Pattern**: `camelCase` starting with verb

```kotlin
// ✅ GOOD
fun loadMediaFiles(resourceId: Long)
fun deleteFile(file: MediaFile)
private suspend fun downloadAndCache(file: MediaFile): File
fun isVideoFile(filename: String): Boolean

// ❌ BAD
fun media() // Missing verb
fun filesLoad() // Verb at end
fun get() // Too generic
```

---

## Sealed Classes

**Prefer over Enums** for events, results, and state representation.

### Event Classes

**Pattern**: Present tense, descriptive names

```kotlin
// ✅ GOOD
sealed class BrowseEvent {
    data class Error(val message: String) : BrowseEvent()
    data class FileDeleted(val filename: String) : BrowseEvent()
    object NavigateBack : BrowseEvent()
}

// ❌ BAD
sealed class BrowseEvents // Plural name
sealed class BrowseAction // Wrong conceptual name
sealed class BrowseResult // Confusing with Result<T>

enum class BrowseEventType { ERROR, FILE_DELETED } // Enum without data
```

### Result Classes

**Pattern**: Use `Result<T>` or custom sealed class

```kotlin
// ✅ GOOD: Standard Result
suspend fun loadFiles(): Result<List<MediaFile>> {
    return try {
        Result.success(repository.getFiles())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ✅ GOOD: Custom sealed class with more states
sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val message: String, val code: Int) : CloudResult<Nothing>()
    object NotAuthenticated : CloudResult<Nothing>()
    object NetworkUnavailable : CloudResult<Nothing>()
}

// ❌ BAD: Boolean + nullable
fun loadFiles(): Pair<Boolean, List<MediaFile>?> // Unclear what Boolean means
```

---

## Coroutines

### Dispatchers

**Rule**: Always specify dispatcher explicitly

```kotlin
// ✅ GOOD
viewModelScope.launch(Dispatchers.IO) {
    val files = repository.getFiles() // Network/disk operation
    withContext(Dispatchers.Main) {
        _files.value = files // UI update
    }
}

// ❌ BAD
viewModelScope.launch { // Defaults to Dispatchers.Main
    val files = repository.getFiles() // Blocks UI thread!
}
```

**Dispatcher Rules**:
- `Dispatchers.Main` - UI updates, StateFlow emissions
- `Dispatchers.IO` - Network, file operations, database queries
- `Dispatchers.Default` - CPU-intensive work (image processing, sorting)

### Structured Concurrency

```kotlin
// ✅ GOOD: Cancel previous job before starting new one
private var loadingJob: Job? = null

fun loadFiles() {
    loadingJob?.cancel()
    loadingJob = viewModelScope.launch { /* ... */ }
}

// ✅ GOOD: Use coroutineScope for parallel work
suspend fun loadMultipleResources(ids: List<Long>): List<List<MediaFile>> = coroutineScope {
    ids.map { id ->
        async { repository.getFiles(id) }
    }.awaitAll()
}

// ❌ BAD: Uncontrolled launches
fun loadFiles() {
    repeat(10) {
        viewModelScope.launch { /* ... */ } // 10 parallel jobs, no cancellation
    }
}
```

---

## Logging (Timber)

### Format

**Pattern**: Action description + context data

```kotlin
// ✅ GOOD
Timber.d("Loading files: resourceId=$resourceId, protocol=${resource.type}")
Timber.d("Files loaded: count=${files.size}, duration=${elapsed}ms")
Timber.e(exception, "Failed to delete file: path=${file.path}")

// ❌ BAD
Timber.d("Loading...") // No context
Timber.d("Done") // Vague
Timber.e("Error!") // No exception reference
```

### Log Levels

| Level | Usage |
|-------|-------|
| `Timber.v()` | Verbose - Detailed flow (loops, iterations) |
| `Timber.d()` | Debug - General info (method entry, completion) |
| `Timber.i()` | Info - Significant events (user actions, state changes) |
| `Timber.w()` | Warning - Recoverable errors (fallback used, retry) |
| `Timber.e()` | Error - Exceptions (with stack trace) |

```kotlin
// ✅ GOOD
Timber.d("Downloading file: path=$remotePath, size=$fileSize")
try {
    downloadFile(remotePath)
    Timber.i("Download completed: $remotePath")
} catch (e: IOException) {
    Timber.w("Download failed, using cached version: $remotePath")
    useCachedFile()
} catch (e: Exception) {
    Timber.e(e, "Critical download error: $remotePath")
    throw e
}

// ❌ BAD
println("Downloading $remotePath") // Use Timber, not println
Log.d("TAG", "Download complete") // Use Timber, not Android Log
Timber.e("Error downloading") // Missing exception and context
```

---

## Dependency Injection (Hilt)

### Constructor Injection

**Rule**: Always prefer constructor injection over field injection

```kotlin
// ✅ GOOD
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {
    // UseCases immediately available
}

// ❌ BAD
@HiltViewModel
class BrowseViewModel : ViewModel() {
    @Inject
    lateinit var getMediaFilesUseCase: GetMediaFilesUseCase // Field injection
    
    init {
        // Can't use UseCase here - not yet injected
    }
}
```

### Module Organization

**Pattern**: One module per responsibility

```kotlin
// ✅ GOOD
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideSmbClient(): SmbClient = SmbClient()
    
    @Provides
    @Singleton
    fun provideSftpClient(): SftpClient = SftpClient()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository
}

// ❌ BAD
@Module
@InstallIn(SingletonComponent::class)
object AppModule { // Everything in one module
    @Provides fun provideSmbClient(): SmbClient = SmbClient()
    @Provides fun provideMediaRepository(...): MediaRepository = MediaRepositoryImpl(...)
    @Provides fun provideGlide(...): RequestManager = Glide.with(...)
}
```

---

## Code Organization

### File Length

**Target**: < 500 lines per file  
**Hard Limit**: 800 lines (consider splitting above this)

**Example**: PlayerActivity refactored from 2,700 lines to:
- `PlayerActivity.kt` - 720 lines
- `VideoPlayerManager.kt` - 220 lines
- `TextViewerManager.kt` - 145 lines
- ... (7 managers total)

### Method Length

**Target**: < 20 lines per method  
**Hard Limit**: 40 lines (extract helper methods above this)

```kotlin
// ✅ GOOD: Small, focused methods
fun loadMediaFiles(resourceId: Long) {
    showLoading()
    
    viewModelScope.launch {
        val result = getMediaFilesUseCase(resourceId)
        handleLoadResult(result)
    }
}

private fun handleLoadResult(result: Result<List<MediaFile>>) {
    result.onSuccess { files ->
        _files.value = files
        hideLoading()
    }.onFailure { error ->
        showError(error.message ?: "Unknown error")
    }
}

// ❌ BAD: 60+ line method
fun loadMediaFiles(resourceId: Long) {
    // 10 lines of validation
    // 20 lines of network logic
    // 15 lines of UI updates
    // 15 lines of error handling
}
```

### Import Organization

**Order** (enforced by Android Studio):
1. Android framework imports
2. Third-party libraries
3. Project imports

```kotlin
// ✅ GOOD
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.hierynomus.smbj.SMBClient
import timber.log.Timber

import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import com.sza.fastmediasorter.data.model.MediaFile

// ❌ BAD: Mixed order, wildcard imports
import com.sza.fastmediasorter.domain.usecase.*
import timber.log.Timber
import android.os.Bundle
```

---

## Documentation Comments

### Class-Level

```kotlin
/**
 * Manages video/audio playback using ExoPlayer.
 * 
 * Responsibilities:
 * - Player initialization and lifecycle
 * - Playback controls (play, pause, seek)
 * - Custom controls (speed, forward, rewind)
 * 
 * @property context Application context for player initialization
 * @property playerView View to render video output
 */
class VideoPlayerManager(
    private val context: Context,
    private val playerView: PlayerView
) {
    // ...
}
```

### Method-Level (when non-obvious)

```kotlin
/**
 * Downloads file with PASV mode, fallback to active mode on timeout.
 * 
 * PASV mode uses random high ports which may be blocked by firewalls.
 * Active mode uses client-initiated connections (works through NAT).
 * 
 * @param remotePath Remote file path on FTP server
 * @param outputStream Destination stream for downloaded data
 * @param progressCallback Called with (bytesDownloaded, totalBytes)
 * @return Result.success on completion, Result.failure on error
 */
suspend fun downloadFile(
    remotePath: String,
    outputStream: OutputStream,
    progressCallback: (Long, Long) -> Unit
): Result<Unit> {
    // Implementation
}
```

**When to Skip**:
- Self-explanatory methods (`fun getTotalSize(): Long`)
- Standard patterns (getters, setters)
- Overridden methods with clear parent documentation

---

## Testing Conventions

### Test File Naming

**Pattern**: `ClassNameTest.kt`

```
BrowseViewModelTest.kt
GetMediaFilesUseCaseTest.kt
SmbClientTest.kt
```

### Test Method Naming

**Pattern**: `methodName_scenario_expectedResult`

```kotlin
class BrowseViewModelTest {
    
    @Test
    fun loadFiles_withValidResourceId_returnsFileList() {
        // Arrange
        val resourceId = 1L
        coEvery { getMediaFilesUseCase(resourceId) } returns Result.success(testFiles)
        
        // Act
        viewModel.loadFiles(resourceId)
        
        // Assert
        assertEquals(testFiles, viewModel.files.value)
    }
    
    @Test
    fun loadFiles_withNetworkError_showsErrorMessage() {
        // ...
    }
}
```

---

## Related Documentation

- [17. Architecture Patterns](17_architecture_patterns.md) - Architectural conventions
- [18. Development Workflows](18_development_workflows.md) - Build and testing practices
- [24. Dependencies](24_dependencies.md) - Library choices and rationale
