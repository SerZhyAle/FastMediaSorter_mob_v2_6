# 11. ViewModels

All ViewModels use Hilt `@HiltViewModel` injection and extend `ViewModel`.

### 1. MainViewModel
**Package:** `com.sza.fastmediasorter.ui.main`  
**Purpose:** MainActivity business logic

**StateFlow/SharedFlow:**
- `resources: StateFlow<List<MediaResource>>` - All resources
- `filteredResources: StateFlow<List<MediaResource>>` - Filtered by tabs/search
- `isLoading: StateFlow<Boolean>` - Loading state
- `error: SharedFlow<String>` - Error events

**UseCases Injected:**
- `GetAllResourcesUseCase`
- `DeleteResourceUseCase`
- `UpdateResourceUseCase`
- `StartPlayerUseCase`

### 2. WelcomeViewModel
**Package:** `com.sza.fastmediasorter.ui.welcome`  
**Purpose:** Onboarding flow

**Methods:**
- `isWelcomeCompleted(): Boolean` - Check if user finished onboarding
- `markWelcomeCompleted()` - Set flag after last page
- `requestPermissions()` - Trigger storage permission flow

### 3. AddResourceViewModel
**Package:** `com.sza.fastmediasorter.ui.addresource`  
**Purpose:** Add new resource

**StateFlow:**
- `testConnectionState: StateFlow<ConnectionTestState>` - Connection test result
- `discoveredResources: StateFlow<List<MediaResource>>` - Network scan results
- `isScanning: StateFlow<Boolean>` - Discovery in progress

**UseCases:**
- `AddResourceUseCase`
- `TestConnectionUseCase`
- `DiscoverNetworkResourcesUseCase`
- `AuthenticateCloudUseCase`

### 4. EditResourceViewModel
**Package:** `com.sza.fastmediasorter.ui.editresource`  
**Purpose:** Edit existing resource

**StateFlow:**
- `resource: StateFlow<MediaResource?>` - Current resource
- `resourceStats: StateFlow<ResourceStats>` - File count, cache size, last scan

**UseCases:**
- `GetResourceByIdUseCase`
- `UpdateResourceUseCase`
- `DeleteResourceUseCase`
- `TestConnectionUseCase`
- `GetResourceStatsUseCase`

### 5. BrowseViewModel
**Package:** `com.sza.fastmediasorter.ui.browse`  
**Purpose:** File browser

**StateFlow:**
- `mediaFiles: StateFlow<List<MediaFile>>` - All files in folder
- `filteredFiles: StateFlow<List<MediaFile>>` - After filters/sort
- `selectedFiles: StateFlow<Set<MediaFile>>` - Multi-selection
- `isLoading: StateFlow<Boolean>`
- `filterState: StateFlow<FileFilter>` - Current filter
- `sortMode: StateFlow<SortMode>` - Current sort
- `displayMode: StateFlow<DisplayMode>` - Grid/List

**SharedFlow:**
- `events: SharedFlow<BrowseEvent>` - One-time events (error, success, etc.)

**UseCases:**
- `GetMediaFilesUseCase`
- `CopyFileUseCase`
- `MoveFileUseCase`
- `DeleteFileUseCase`
- `RenameFileUseCase`
- `UndoOperationUseCase`
- `GetDestinationsUseCase`

### 6. PlayerViewModel
**Package:** `com.sza.fastmediasorter.ui.player`  
**Purpose:** Media player

**StateFlow:**
- `currentFile: StateFlow<MediaFile?>` - Currently playing
- `allFiles: StateFlow<List<MediaFile>>` - Playlist
- `currentIndex: StateFlow<Int>` - Position in playlist
- `isPlaying: StateFlow<Boolean>` - Slideshow/video playback state
- `settings: StateFlow<AppSettings>` - User preferences

**SharedFlow:**
- `playerEvents: SharedFlow<PlayerEvent>` - Navigation, errors, file changes

**UseCases:**
- `CopyFileUseCase`
- `MoveFileUseCase`
- `DeleteFileUseCase`
- `RenameFileUseCase`
- `EditImageUseCase`
- `EditGifUseCase`
- `UndoOperationUseCase`
- `GetDestinationsUseCase`
- `TranslateTextUseCase` (for translation overlay)

### 7. SettingsViewModel
**Package:** `com.sza.fastmediasorter.ui.settings`  
**Purpose:** Settings management

**StateFlow:**
- `settings: StateFlow<AppSettings>` - All app settings
- `cacheSize: StateFlow<Long>` - Current cache size in bytes
- `destinations: StateFlow<List<MediaResource>>` - Destination resources

**UseCases:**
- `GetSettingsUseCase`
- `UpdateSettingsUseCase`
- `ClearCacheUseCase`
- `CalculateOptimalCacheSizeUseCase`
- `GetDestinationsUseCase`
- `ReorderDestinationsUseCase`

### 8. GoogleDriveFolderPickerViewModel
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** Google Drive folder navigation

**StateFlow:**
- `folders: StateFlow<List<CloudFolderItem>>` - Current folder contents
- `currentPath: StateFlow<List<CloudFolderItem>>` - Breadcrumb navigation stack
- `isLoading: StateFlow<Boolean>`
- `selectedFolder: StateFlow<CloudFolderItem?>` - User selection

**Methods:**
- `loadFolders()` - Fetch folders in current path
- `navigateIntoFolder(folder)` - Go deeper
- `navigateBack()` - Go up one level
- `selectFolder(folder)` - Mark as selected

### 9. OneDriveFolderPickerViewModel
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** OneDrive folder navigation

**Features:** Identical to `GoogleDriveFolderPickerViewModel` but uses OneDrive API

### 10. DropboxFolderPickerViewModel
**Package:** `com.sza.fastmediasorter.ui.cloudfolders`  
**Purpose:** Dropbox folder navigation

**Features:** Identical to `GoogleDriveFolderPickerViewModel` but uses Dropbox API
