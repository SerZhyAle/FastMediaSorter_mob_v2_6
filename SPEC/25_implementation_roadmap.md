# 25. Implementation Roadmap

## Overview

This document provides a **step-by-step development plan** for implementing FastMediaSorter v2 from scratch. It breaks down the project into phases with priorities and implementation sequences.

---

## Phase 0: Project Setup

### Objective
Create project structure, configure build system, set up dependency injection and database foundation.

### Tasks

#### Android Studio Project Creation

1. **Create New Project**
   - Open Android Studio → New Project → Empty Activity
   - Package name: `com.sza.fastmediasorter`
   - Language: Kotlin
   - Minimum SDK: API 28 (Android 9.0)
   - Build configuration: Gradle Kotlin DSL

2. **Configure Gradle**
   - Create `gradle/libs.versions.toml` (see [24_dependencies.md](24_dependencies.md))
   - Update `build.gradle.kts` (app level):
     ```kotlin
     plugins {
         id("com.android.application")
         id("org.jetbrains.kotlin.android")
         id("com.google.devtools.ksp")
         id("com.google.dagger.hilt.android")
     }
     
     android {
         namespace = "com.sza.fastmediasorter"
         compileSdk = 34
         
         defaultConfig {
             applicationId = "com.sza.fastmediasorter"
             minSdk = 28
             targetSdk = 34
             versionCode = 1
             versionName = "1.0.0"
         }
         
         buildFeatures {
             viewBinding = true
         }
         
         compileOptions {
             sourceCompatibility = JavaVersion.VERSION_17
             targetCompatibility = JavaVersion.VERSION_17
         }
     }
     
     dependencies {
         // Core
         implementation(libs.androidx.core.ktx)
         implementation(libs.androidx.appcompat)
         implementation(libs.material)
         
         // Hilt
         implementation(libs.hilt.android)
         ksp(libs.hilt.compiler)
         
         // Room
         implementation(libs.androidx.room.runtime)
         implementation(libs.androidx.room.ktx)
         ksp(libs.androidx.room.compiler)
         
         // Lifecycle
         implementation(libs.androidx.lifecycle.viewmodel.ktx)
         implementation(libs.androidx.lifecycle.runtime.ktx)
         
         // Coroutines
         implementation(libs.kotlinx.coroutines.android)
         
         // Timber (logging)
         implementation(libs.timber)
     }
     ```

3. **Configure Manifest**
   - Add permissions (storage, network)
   - Configure application class for Hilt
   - Set up FileProvider

4. **Version Control**
   - Initialize git: `git init`
   - Create `.gitignore` (exclude `build/`, `local.properties`, `*.iml`)
   - Initial commit: "Project setup"

#### Architecture Foundation

1. **Create Package Structure**
   ```
   com.sza.fastmediasorter/
   ├── FastMediaSorterApp.kt
   ├── ui/
   ├── domain/
   │   ├── usecase/
   │   └── repository/
   ├── data/
   │   ├── local/
   │   ├── network/
   │   ├── cloud/
   │   └── repository/
   ├── core/
   │   ├── ui/
   │   └── util/
   ├── di/
   └── widget/
   ```

2. **Create Application Class**
   ```kotlin
   @HiltAndroidApp
   class FastMediaSorterApp : Application() {
       override fun onCreate() {
           super.onCreate()
           if (BuildConfig.DEBUG) {
               Timber.plant(Timber.DebugTree())
           }
       }
   }
   ```

3. **Create BaseActivity**
   - Implement ViewBinding abstraction
   - Add locale support
   - Add screen awake management
   - See [07_base_and_widget.md](07_base_and_widget.md) for full implementation

4. **Set Up Room Database**
   - Create `AppDatabase.kt` (version 1, empty)
   - Create `DatabaseModule.kt` for Hilt injection
   - Test database creation in instrumentation test

**Deliverables**:
- ✅ Compilable project with Hilt + Room
- ✅ BaseActivity implementation
- ✅ Git repository initialized

---

## Phase 1: Local File Management

### Objective
Implement core functionality: browse local folders, view files, basic file operations.

**MVP Features**: View local images, copy/move files, favorites

### Database & Data Layer

#### Room Entities & DAOs

1. **Create Entities**
   ```kotlin
   @Entity(tableName = "resources")
   data class ResourceEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val name: String,
       val path: String,
       val type: ResourceType,
       val color: Int,
       val isDestination: Boolean = false,
       val createdAt: Long = System.currentTimeMillis()
   )
   
   @Entity(tableName = "favorites")
   data class FavoriteEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val resourceId: Long,
       val filePath: String,
       val addedAt: Long = System.currentTimeMillis()
   )
   ```

2. **Create DAOs**
   ```kotlin
   @Dao
   interface ResourceDao {
       @Query("SELECT * FROM resources ORDER BY name ASC")
       fun getAllResources(): Flow<List<ResourceEntity>>
       
       @Insert
       suspend fun insertResource(resource: ResourceEntity): Long
       
       @Delete
       suspend fun deleteResource(resource: ResourceEntity)
   }
   
   @Dao
   interface FavoriteDao {
       @Query("SELECT * FROM favorites WHERE resourceId = :resourceId")
       fun getFavoritesForResource(resourceId: Long): Flow<List<FavoriteEntity>>
       
       @Insert(onConflict = OnConflictStrategy.REPLACE)
       suspend fun addFavorite(favorite: FavoriteEntity)
       
       @Delete
       suspend fun removeFavorite(favorite: FavoriteEntity)
   }
   ```

3. **Update AppDatabase**
   ```kotlin
   @Database(
       entities = [ResourceEntity::class, FavoriteEntity::class],
       version = 1,
       exportSchema = true
   )
   abstract class AppDatabase : RoomDatabase() {
       abstract fun resourceDao(): ResourceDao
       abstract fun favoriteDao(): FavoriteDao
   }
   ```

4. **Create Repository Interfaces** (domain layer)
   ```kotlin
   interface ResourceRepository {
       fun getAllResources(): Flow<List<Resource>>
       suspend fun addResource(resource: Resource): Result<Long>
       suspend fun deleteResource(resource: Resource): Result<Unit>
   }
   ```

5. **Implement Repositories** (data layer)
   ```kotlin
   class ResourceRepositoryImpl @Inject constructor(
       private val resourceDao: ResourceDao
   ) : ResourceRepository {
       override fun getAllResources(): Flow<List<Resource>> {
           return resourceDao.getAllResources().map { entities ->
               entities.map { it.toDomainModel() }
           }
       }
       // ... other methods
   }
   ```

#### Local File Scanner

1. **Create MediaFile Domain Model**
   ```kotlin
   data class MediaFile(
       val path: String,
       val name: String,
       val size: Long,
       val date: Long,
       val type: MediaType,
       val duration: Long? = null,
       val thumbnailUrl: String? = null
   )
   
   enum class MediaType {
       IMAGE, VIDEO, AUDIO, GIF, TEXT, PDF, EPUB, UNKNOWN
   }
   ```

2. **Implement LocalMediaScanner**
   ```kotlin
   class LocalMediaScanner @Inject constructor() {
       suspend fun scanFolder(
           folderPath: String,
           includeSubfolders: Boolean = true
       ): Result<List<MediaFile>> = withContext(Dispatchers.IO) {
           try {
               val folder = File(folderPath)
               if (!folder.exists() || !folder.isDirectory) {
                   return@withContext Result.failure(IOException("Invalid folder"))
               }
               
               val files = if (includeSubfolders) {
                   folder.walkTopDown()
                       .filter { it.isFile }
                       .toList()
               } else {
                   folder.listFiles()?.filter { it.isFile } ?: emptyList()
               }
               
               val mediaFiles = files
                   .filter { isMediaFile(it.extension) }
                   .map { it.toMediaFile() }
               
               Result.success(mediaFiles)
           } catch (e: Exception) {
               Result.failure(e)
           }
       }
   }
   ```

3. **Create UseCases**
   ```kotlin
   class GetMediaFilesUseCase @Inject constructor(
       private val scanner: LocalMediaScanner,
       private val resourceRepository: ResourceRepository
   ) {
       suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
           val resource = resourceRepository.getResourceById(resourceId).getOrNull()
               ?: return Result.failure(IllegalArgumentException("Resource not found"))
           
           return scanner.scanFolder(resource.path)
       }
   }
   ```

#### MainActivity UI

1. **Create Layout** (`activity_main.xml`)
   - Toolbar with title, settings button
   - RecyclerView for resource list
   - FloatingActionButton for adding resources
   - Empty state TextView

2. **Create ResourceAdapter**
   ```kotlin
   class ResourceAdapter(
       private val onClick: (Resource) -> Unit,
       private val onLongClick: (Resource) -> Unit
   ) : ListAdapter<Resource, ResourceViewHolder>(ResourceDiffCallback()) {
       // Standard RecyclerView adapter implementation
   }
   ```

3. **Create MainViewModel**
   ```kotlin
   @HiltViewModel
   class MainViewModel @Inject constructor(
       private val resourceRepository: ResourceRepository
   ) : ViewModel() {
       val resources: StateFlow<List<Resource>> = resourceRepository.getAllResources()
           .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
       
       fun deleteResource(resource: Resource) {
           viewModelScope.launch {
               resourceRepository.deleteResource(resource)
           }
       }
   }
   ```

4. **Implement MainActivity**
   ```kotlin
   @AndroidEntryPoint
   class MainActivity : BaseActivity<ActivityMainBinding>() {
       private val viewModel: MainViewModel by viewModels()
       private lateinit var adapter: ResourceAdapter
       
       override fun getViewBinding(): ActivityMainBinding {
           return ActivityMainBinding.inflate(layoutInflater)
       }
       
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           setupRecyclerView()
           observeResources()
           setupFab()
       }
       
       private fun setupRecyclerView() {
           adapter = ResourceAdapter(
               onClick = { resource -> openBrowseActivity(resource) },
               onLongClick = { resource -> showResourceOptions(resource) }
           )
           binding.recyclerView.adapter = adapter
       }
       
       private fun observeResources() {
           lifecycleScope.launch {
               viewModel.resources.collect { resources ->
                   adapter.submitList(resources)
               }
           }
       }
   }
   ```

### Browse & View

#### BrowseActivity

1. **Create Layout** (`activity_browse.xml`)
   - Toolbar with resource name, back button
   - RecyclerView with GridLayoutManager
   - Bottom toolbar: Sort, Filter, View mode toggle

2. **Create MediaFileAdapter**
   - Grid item layout with thumbnail, filename, size
   - Use Glide for image loading
   - Handle click → open PlayerActivity

3. **Create BrowseViewModel**
   ```kotlin
   @HiltViewModel
   class BrowseViewModel @Inject constructor(
       private val getMediaFilesUseCase: GetMediaFilesUseCase
   ) : ViewModel() {
       private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
       val files: StateFlow<List<MediaFile>> = _files.asStateFlow()
       
       private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
       val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
       
       fun loadFiles(resourceId: Long) {
           viewModelScope.launch {
               _uiState.value = BrowseUiState.Loading
               
               getMediaFilesUseCase(resourceId)
                   .onSuccess { fileList ->
                       _files.value = fileList
                       _uiState.value = BrowseUiState.Success
                   }
                   .onFailure { error ->
                       _uiState.value = BrowseUiState.Error(error.message ?: "Unknown error")
                   }
           }
       }
   }
   ```

4. **Implement BrowseActivity**
   - Load files in onCreate
   - Handle grid/list view toggle
   - Implement basic sorting (name, date, size)

#### PlayerActivity (Basic)

1. **Create Layout** (`activity_player_unified.xml`)
   - PhotoView for images
   - ExoPlayer PlayerView for video/audio
   - TextView for text files
   - Navigation buttons (Previous/Next)
   - Bottom toolbar with action buttons

2. **Create PlayerViewModel**
   ```kotlin
   @HiltViewModel
   class PlayerViewModel @Inject constructor(
       private val getMediaFilesUseCase: GetMediaFilesUseCase
   ) : ViewModel() {
       private val _currentFile = MutableStateFlow<MediaFile?>(null)
       val currentFile: StateFlow<MediaFile?> = _currentFile.asStateFlow()
       
       private var allFiles: List<MediaFile> = emptyList()
       private var currentIndex = 0
       
       fun loadFiles(resourceId: Long, initialFilePath: String) {
           viewModelScope.launch {
               getMediaFilesUseCase(resourceId).onSuccess { files ->
                   allFiles = files
                   currentIndex = files.indexOfFirst { it.path == initialFilePath }
                   if (currentIndex != -1) {
                       _currentFile.value = files[currentIndex]
                   }
               }
           }
       }
       
       fun navigateToNext() {
           if (currentIndex < allFiles.size - 1) {
               currentIndex++
               _currentFile.value = allFiles[currentIndex]
           }
       }
       
       fun navigateToPrevious() {
           if (currentIndex > 0) {
               currentIndex--
               _currentFile.value = allFiles[currentIndex]
           }
       }
   }
   ```

3. **Implement Basic Display**
   - Images: Load with Glide into PhotoView (zoom/pan enabled)
   - Videos: Initialize ExoPlayer with Media3
   - Text: Read file and display in TextView
   - Swipe gestures: Left/Right for navigation

#### File Operations (Copy/Move)

1. **Create LocalOperationStrategy**
   ```kotlin
   class LocalOperationStrategy @Inject constructor() : FileOperationStrategy {
       override suspend fun copy(
           source: MediaFile,
           destination: String
       ): Result<FileOperationResult> = withContext(Dispatchers.IO) {
           try {
               val sourceFile = File(source.path)
               val destFile = File(destination, source.name)
               
               sourceFile.copyTo(destFile, overwrite = false)
               
               Result.success(FileOperationResult.Success(
                   originalPath = source.path,
                   newPath = destFile.absolutePath,
                   operation = OperationType.COPY
               ))
           } catch (e: Exception) {
               Result.failure(e)
           }
       }
       
       override suspend fun move(
           source: MediaFile,
           destination: String
       ): Result<FileOperationResult> {
           // Copy then delete source
       }
       
       override suspend fun delete(file: MediaFile): Result<Unit> {
           // Move to .trash/ folder (soft delete)
       }
   }
   ```

2. **Create FileOperationHandler**
   ```kotlin
   class FileOperationHandler @Inject constructor(
       private val localStrategy: LocalOperationStrategy
   ) {
       suspend fun copyFile(
           source: MediaFile,
           destinationResource: Resource
       ): Result<FileOperationResult> {
           return localStrategy.copy(source, destinationResource.path)
       }
   }
   ```

3. **Add Copy/Move UI**
   - Bottom sheet dialog showing destination list
   - Progress indicator during operation
   - Toast notification on success
   - Undo Snackbar

**Phase 1 Deliverables**:
- ✅ Working local file browser
- ✅ Image viewer with zoom
- ✅ Video playback (basic)
- ✅ Copy/move files between folders
- ✅ Favorites system

---

## Phase 2: Network Protocols

### Objective
Add SMB, SFTP, FTP support with connection pooling and caching.

### SMB Protocol

#### SMB Client Setup

1. **Add Dependencies**
   ```kotlin
   implementation("com.hierynomus:smbj:0.12.1")
   implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
   implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
   ```

2. **Create SmbClient**
   - Connection method with credentials
   - Test connection method
   - List files method
   - Download file with progress
   - Upload file with progress
   - See [20_protocol_implementations.md](20_protocol_implementations.md#smb-server-message-block)

3. **Create NetworkCredentialsRepository**
   - Store SMB credentials encrypted (EncryptedSharedPreferences)
   - Get credentials by resource ID

4. **Implement Connection Pool**
   - Pool size: 5 connections
   - Idle timeout: 45 seconds
   - Thread-safe with Mutex

#### SMB Scanner & Operations

1. **Create SmbMediaScanner**
   ```kotlin
   class SmbMediaScanner @Inject constructor(
       private val smbClient: SmbClient
   ) {
       suspend fun scanFolder(
           server: String,
           shareName: String,
           remotePath: String,
           credentials: NetworkCredentials
       ): Result<List<MediaFile>> {
           return smbClient.connect(server, shareName, credentials)
               .flatMap { connection ->
                   smbClient.listFiles(remotePath)
               }
       }
   }
   ```

2. **Create SmbOperationStrategy**
   - Implement FileOperationStrategy interface
   - Download → edit → upload pattern for edits

3. **Update FileOperationHandler**
   - Routing logic: detect source/destination resource types
   - SMB→Local: Download via SmbClient
   - Local→SMB: Upload via SmbClient
   - SMB→SMB: Server-side copy if same server, else download+upload

#### SMB UI Integration

1. **Update AddResourceActivity**
   - Add SMB resource type selection
   - Form fields: Server, Share name, Username, Password, Domain, Port
   - Test connection button
   - Save credentials encrypted

2. **Test SMB Integration**
   - Add SMB resource
   - Browse files (use network cache)
   - View images from SMB
   - Copy file from SMB to local
   - Copy file from local to SMB

### SFTP & FTP

#### SFTP Implementation

1. **Add Dependencies**
   ```kotlin
   implementation("com.hierynomus:sshj:0.37.0")
   implementation("net.i2p.crypto:eddsa:0.3.0")
   ```

2. **Create SftpClient**
   - Password authentication
   - Public key authentication
   - File operations (list, download, upload)

3. **Create SftpMediaScanner & SftpOperationStrategy**

4. **Update AddResourceActivity** for SFTP

#### FTP Implementation

1. **Add Dependencies**
   ```kotlin
   implementation("commons-net:commons-net:3.10.0")
   ```

2. **Create FtpClient**
   - PASV mode with active mode fallback
   - Connection timeout handling
   - **Critical**: Never call `completePendingCommand()` after exceptions

3. **Create FtpMediaScanner & FtpOperationStrategy**

4. **Update AddResourceActivity** for FTP

#### Network Optimization

1. **Implement UnifiedFileCache**
   - 24-hour expiration
   - Cache key: hash(path) + size
   - Shared across all network operations
   - See [22_cache_strategies.md](22_cache_strategies.md#unifiedfilecache-network-downloads)

2. **Integrate Glide with NetworkFileModelLoader**
   - Custom ModelLoader for network files
   - DiskCacheStrategy.RESOURCE
   - Thumbnail pre-caching

3. **Add Progress Indicators**
   - Determinate progress for downloads/uploads
   - Indeterminate for file listing

### Testing & Polish

#### Protocol Testing

1. **Manual Testing**
   - SMB: Test on Windows/Linux Samba shares
   - SFTP: Test with OpenSSH server
   - FTP: Test PASV and active modes

2. **Edge Cases**
   - Connection timeout handling
   - Resume after network interruption
   - Large file transfers (100MB+)
   - Special characters in filenames

3. **Performance Testing**
   - 1000+ files in folder
   - Thumbnail loading speed
   - Memory usage monitoring

#### Network UI Polish

1. **Add Connection Status Indicators**
   - Toolbar icon: connected/disconnected
   - Retry button on connection failure

2. **Improve Error Messages**
   - Specific messages for: timeout, auth failure, network unavailable
   - Actionable error dialogs with retry option

3. **Add Settings**
   - Network timeout duration
   - Connection pool size
   - Parallel download limit

**Phase 2 Deliverables**:
- ✅ SMB browsing and file operations
- ✅ SFTP support
- ✅ FTP support with PASV/active fallback
- ✅ Network caching system
- ✅ Progress indicators

---

## Phase 3: Cloud Storage

### Objective
Integrate Google Drive, OneDrive, Dropbox with OAuth authentication.

### Google Drive

#### OAuth Setup

1. **Google Cloud Console Setup**
   - Create new project
   - Enable Google Drive API
   - Create OAuth 2.0 Client ID (Android type)
   - Add package name: `com.sza.fastmediasorter`
   - Generate SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore`
   - Add SHA-1 to OAuth client

2. **Add Dependencies**
   ```kotlin
   implementation("com.google.android.gms:play-services-auth:21.0.0")
   implementation("com.google.api-client:google-api-client-android:2.2.0")
   implementation("com.google.apis:google-api-services-drive:v3-rev20231212-2.0.0")
   ```

3. **Implement GoogleDriveClient**
   - Authentication with GoogleSignIn
   - List files in folder
   - Download file (REST API)
   - Upload file (multipart)
   - See [20_protocol_implementations.md](20_protocol_implementations.md#google-drive-rest-api)

#### Google Drive Folder Picker

1. **Create GoogleDriveFolderPickerActivity**
   - Show folder tree navigation
   - Breadcrumb navigation
   - Select folder button
   - Return selected folder ID

2. **Update AddResourceActivity**
   - Add Google Drive resource type
   - Launch picker on button click
   - Store folder ID + name

#### Google Drive Integration

1. **Create CloudMediaScanner**
   - Unified interface for all cloud providers
   - Google Drive implementation

2. **Create CloudOperationStrategy**
   - Download → local operation → upload pattern

3. **Test Google Drive**
   - Authenticate
   - Browse folder
   - View files (thumbnailUrl for images)
   - Download and view local copy

### OneDrive & Dropbox

#### OneDrive (MSAL)

1. **Add Dependencies**
   ```kotlin
   implementation("com.microsoft.identity.client:msal:4.10.0")
   ```

2. **Create msal_config.json**
   - Client ID from Azure Portal
   - Redirect URI: `msauth://com.sza.fastmediasorter/...`

3. **Implement OneDriveClient**
   - MSAL authentication
   - Graph API calls (`graph.microsoft.com/v1.0`)

4. **Create OneDriveFolderPickerActivity**

#### Dropbox

1. **Add Dependencies**
   ```kotlin
   implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
   ```

2. **Dropbox App Console**
   - Create app
   - Get app key
   - Add to gradle.properties

3. **Implement DropboxClient**
   - OAuth 2.0 with custom scheme
   - Files API v2

4. **Create DropboxFolderPickerActivity**

#### Cloud Polish

1. **Unified Cloud UI**
   - Show cloud provider icon in resource list
   - Display account email in EditResourceActivity

2. **Offline Handling**
   - Cache last-fetched file list
   - Show "Offline mode" indicator
   - Retry button

**Phase 3 Deliverables**:
- ✅ Google Drive integration
- ✅ OneDrive integration
- ✅ Dropbox integration
- ✅ OAuth authentication flows
- ✅ Cloud folder pickers

---

## Phase 4: Advanced Features

### Objective
Pagination, image editing, document viewer, OCR/translation.

### Pagination & Performance

#### Paging3 Implementation

1. **Add Dependencies**
   ```kotlin
   implementation("androidx.paging:paging-runtime-ktx:3.2.1")
   ```

2. **Create MediaFilePagingSource**
   - Load pages of 50 items
   - Implement all scanner variants (Local, SMB, SFTP, etc.)

3. **Update BrowseViewModel**
   - Auto-switch to Paging when file count > 1000
   - Expose Paging Flow

4. **Create PagingMediaFileAdapter**

5. **Test with Large Folder**
   - Create test folder with 5000+ files
   - Verify smooth scrolling
   - Check memory usage

#### Performance Optimization

1. **I/O Buffer Optimization**
   - Increase buffer size to 64KB
   - Apply to all file transfer operations

2. **RecyclerView Optimization**
   - Implement onViewRecycled in adapters
   - Cancel Glide requests explicitly
   - Free ConnectionThrottleManager slots

3. **SAF Parallel Scanning**
   - Two-phase BFS strategy
   - Parallel file scanning with limited concurrency
   - See [21_common_pitfalls.md](21_common_pitfalls.md#7-saf-storage-access-framework-performance)

#### Memory & Battery

1. **Memory Leak Prevention**
   - LeakCanary integration (debug builds)
   - Fix any detected leaks

2. **Battery Optimization**
   - WorkManager for background operations
   - Stop network operations when app in background

### Image Editing & Documents

#### Image Editing

1. **Add Dependencies**
   ```kotlin
   implementation("com.burhanrashid52:photoeditor:3.0.2")
   ```

2. **Create ImageEditActivity**
   - Rotation: 90°, 180°, 270°
   - Flip: Horizontal, Vertical
   - Filters: Grayscale, Sepia, Negative
   - Brightness, Contrast, Saturation sliders

3. **Implement NetworkImageEditUseCase**
   - Download → edit → upload pattern

4. **Add Edit Button in PlayerActivity**

#### PDF Viewer

1. **Add Dependencies**
   ```kotlin
   implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")
   ```

2. **Add PDF Support to PlayerActivity**
   - Detect .pdf extension
   - Show PDFView widget
   - Page navigation (swipe, buttons)
   - Zoom controls

3. **PDF Thumbnail Generation**
   - Render first page to bitmap
   - Cache in pdf_thumbnails/
   - Show in BrowseActivity grid

#### EPUB Reader

1. **Add Dependencies**
   ```kotlin
   implementation("com.folioreader:folioreader:0.7.0")
   ```

2. **Integrate FolioReader**
   - Launch reader from PlayerActivity
   - Chapter navigation
   - Font size control
   - Theme support (light/dark)

#### Text Viewer

1. **Enhanced Text Display**
   - Syntax highlighting for code files (.java, .kt, .xml, .json)
   - Line numbers
   - Word wrap toggle

### OCR & Translation

#### OCR Implementation

1. **Add Dependencies**
   ```kotlin
   implementation("com.google.mlkit:text-recognition:16.0.0")
   implementation("cz.adaptech:tesseract4android:4.7.0")
   ```

2. **Implement Hybrid OCR**
   - Try ML Kit first (fast, on-device)
   - Fallback to Tesseract for Cyrillic
   - Download trained data files (.traineddata)

3. **Add OCR Button in PlayerActivity**
   - Show overlay with recognized text
   - Copy text to clipboard
   - Share text via intent

#### Translation

1. **Add Dependencies**
   ```kotlin
   implementation("com.google.mlkit:translate:17.0.2")
   ```

2. **Implement Translation Flow**
   - Auto-detect source language
   - User selects target language
   - Download language models on-demand
   - Show translated text in dialog

#### Lyrics Support

1. **Implement SearchLyricsUseCase**
   - API: `api.lyrics.ovh/v1/{artist}/{title}`
   - Parse metadata from audio file (artist, title)
   - Fallback to filename parsing

2. **Show Lyrics in PlayerActivity**
   - Button in audio player controls
   - Scrollable text view
   - Cache lyrics locally

**Phase 4 Deliverables**:
- ✅ Pagination for 1000+ files
- ✅ Image editing (rotation, filters, adjustments)
- ✅ PDF viewer with zoom
- ✅ EPUB reader integration
- ✅ OCR with hybrid strategy
- ✅ Translation support
- ✅ Lyrics display

---

## Phase 5: UI/UX Polish

### Objective
Settings, keyboard navigation, widgets, themes, localization.

#### Settings Activity

1. **Create 4 Settings Fragments**
   - GeneralSettingsFragment (theme, language, cache)
   - MediaSettingsFragment (thumbnail size, scan depth)
   - PlaybackSettingsFragment (slideshow interval, auto-advance)
   - DestinationsSettingsFragment (manage quick-sort destinations)

2. **Implement SharedPreferences**
   - Use Hilt @Singleton SettingsRepository
   - Expose Flow for reactive updates

#### Keyboard Navigation

1. **Add Key Event Handling**
   - PlayerActivity: Arrow keys (Previous/Next), Space (Play/Pause), Delete (Delete file)
   - BrowseActivity: Arrow keys (Navigate grid), Enter (Open file)

2. **Focus Management**
   - Proper focus order with requestFocus()
   - Visual focus indicators

#### Home Screen Widgets

1. **Create 3 Widget Types**
   - FavoritesWidget: Show 5 recent favorites
   - ResourceLaunchWidget: Quick launch button for resource
   - ContinueReadingWidget: Resume last-viewed file

2. **Implement Widget Providers**
   - Update on data change via WorkManager
   - Handle click intents

3. **Create ResourceLaunchWidgetConfigActivity**

#### Themes & Localization

1. **Themes**
   - Light theme (default)
   - Dark theme (follow system or manual)
   - MaterialComponents theme configuration

2. **Localization**
   - strings.xml (English - base)
   - strings-ru.xml (Russian)
   - strings-uk.xml (Ukrainian)
   - Test language switching in settings

3. **RTL Support**
   - Test with Arabic locale
   - Fix layout issues

#### Accessibility

1. **Content Descriptions**
   - Add to all ImageViews, ImageButtons
   - Meaningful labels for screen readers

2. **Touch Target Sizes**
   - Minimum 48dp × 48dp for all clickable elements

3. **Color Contrast**
   - Verify WCAG AA compliance
   - Test with TalkBack enabled

**Phase 5 Deliverables**:
- ✅ Complete settings system
- ✅ Keyboard navigation support
- ✅ 3 home screen widgets
- ✅ Light/dark themes
- ✅ 3 language localizations
- ✅ Accessibility compliance

---

## Phase 6: Testing & Release

### Objective
Comprehensive testing, bug fixes, documentation, release preparation.

#### Testing

1. **Unit Tests**
   - UseCases (mock repositories)
   - ViewModels (mock UseCases)
   - Utility functions

2. **Instrumentation Tests**
   - Room database migrations
   - UI flows (add resource → browse → view)

3. **Manual Testing**
   - Test on 3+ devices (different manufacturers, Android versions)
   - Test all protocols (Local, SMB, SFTP, FTP, Google Drive, OneDrive, Dropbox)
   - Test edge cases: airplane mode, low storage, 10,000+ files

#### Bug Fixes

1. **Critical Bugs** (P0)
   - App crashes
   - Data loss
   - Security vulnerabilities

2. **High Priority** (P1)
   - Network timeouts not handled
   - UI freezes
   - Memory leaks

3. **Medium Priority** (P2)
   - UI glitches
   - Missing error messages

#### Documentation

1. **User Documentation**
   - README.md (project overview)
   - QUICK_START.md (first-time setup)
   - HOW_TO.md (feature tutorials)
   - FAQ.md
   - TROUBLESHOOTING.md
   - Localize all docs (ru, uk)

2. **Developer Documentation**
   - Architecture overview
   - Build instructions
   - Contribution guidelines

#### Release Preparation

1. **Version Configuration**
   - Set versionName = "1.0.0"
   - Set versionCode = 1

2. **ProGuard Configuration**
   - Enable minification for release build
   - Test release APK

3. **Generate Signed APK**
   - Create keystore
   - Configure signing in build.gradle.kts
   - Build release APK

4. **Google Play Store**
   - Create app listing
   - Add screenshots (phone, tablet)
   - Write app description (en, ru, uk)
   - Upload APK
   - Submit for review

**Phase 6 Deliverables**:
- ✅ Comprehensive test suite
- ✅ Bug-free release candidate
- ✅ Complete documentation
- ✅ Published to Play Store

---

## Optional Enhancements (Post-Launch)

### Phase 7: Advanced Features (Future)

1. **Compose Migration**
   - Gradual migration starting with Settings screens
   - Rewrite dialogs in Compose
   - Eventually migrate all activities

2. **Multi-Select Operations**
   - Select multiple files in grid
   - Batch copy/move/delete

3. **Search Functionality**
   - Search by filename, date, size
   - Full-text search in documents

4. **File Versioning**
   - Track file modifications
   - Show version history
   - Restore previous versions

5. **Sync Service**
   - Background sync between local and network/cloud
   - Conflict resolution
   - Offline queue

6. **Advanced Video Editor**
   - Trim, cut, merge videos
   - Add text overlays
   - Export to different formats

---

## Risk Mitigation

### High-Risk Areas

1. **OAuth Setup Complexity**
   - **Risk**: Google/Microsoft/Dropbox OAuth configuration errors
   - **Mitigation**: Follow official documentation step-by-step, use debug keystore SHA-1 initially

2. **Network Protocol Edge Cases**
   - **Risk**: Rare server configurations cause crashes
   - **Mitigation**: Test with multiple server implementations, add extensive try-catch

3. **16 KB Page Size Compatibility**
   - **Risk**: Native libraries not aligned for Android 15+
   - **Mitigation**: Configure `android.bundle.enableNativeLibraryAlignment=true` from Phase 0

4. **Large File Performance**
   - **Risk**: 100MB+ files cause UI freezes or OOM
   - **Mitigation**: Stream processing, chunked uploads/downloads, progress indicators

5. **Database Migrations**
   - **Risk**: Users lose data during schema upgrade
   - **Mitigation**: Write migration tests, test upgrade from every previous version

### Contingency Plans

- **Behind Schedule**: Cut optional features (EPUB, Translation, Widgets) from MVP
- **Protocol Issues**: Release with fewer protocols initially (Local + SMB only)
- **Testing Delays**: Extend Phase 6, delay release

---

## Success Metrics (Post-Launch)

### Initial Release
- 0 critical crashes (crash-free rate > 99%)
- < 10 bug reports
- App rating > 4.0 stars

### First Month
- 1,000+ downloads
- 50+ active users (DAU)
- Feature usage: 80% browse, 60% copy/move, 30% cloud

### Three Months
- 10,000+ downloads
- 500+ active users
- Positive reviews: 70%+

---

## Related Documentation

- [17. Architecture Patterns](17_architecture_patterns.md) - Implementation patterns to follow
- [18. Development Workflows](18_development_workflows.md) - Build, test, deploy processes
- [21. Common Pitfalls](21_common_pitfalls.md) - Avoid known issues from day 1
- [23. Code Conventions](23_code_conventions.md) - Coding standards to maintain
- [24. Dependencies](24_dependencies.md) - All required libraries with versions

---

## Conclusion

This roadmap provides a **realistic, phased approach** to implementing FastMediaSorter v2 from scratch. Each phase builds on the previous, allowing for early validation of core concepts before investing in advanced features.

**Key Success Factors**:
1. ✅ Follow Clean Architecture principles from Phase 0
2. ✅ Test each protocol thoroughly before moving to next
3. ✅ Keep UI simple in early phases, polish in Phase 5
4. ✅ Use this spec + copilot-instructions.md as development guide
5. ✅ Commit code regularly, deploy test builds frequently

With disciplined execution and proper planning, the MVP can be successfully completed following this phased approach.
