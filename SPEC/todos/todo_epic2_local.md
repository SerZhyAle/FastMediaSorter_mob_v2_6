# Epic 2: Local File Management (MVP) - Detailed TODO
*Derived from: [Tactical Plan: Epic 2](../00_strategy_epic2_local.md)*

## 1. Permissions & Onboarding

### 1.1 Storage Permissions
- [ ] Implement `StoragePermissionManager` helper class
- [ ] Check `READ_EXTERNAL_STORAGE` (Android < 13)
- [ ] Check `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` (Android 13+)
- [ ] Handle `MANAGE_EXTERNAL_STORAGE` for "All Files" access (if targeted)
- [ ] **Validate**: App requests correct permissions on different Android versions (10, 11, 13)

### 1.2 Onboarding Flow
- [ ] Create "Welcome" activity/screen
- [ ] Implement ViewPager or Layout for app introduction
- [ ] Add "Request Permissions" button
- [ ] Implement logic to proceed to Main Activity ONLY after permissions granted
- [ ] **Validate**: User cannot bypass permission screen

## 2. Local Discovery Engine

### 2.1 Media Scanner
- [ ] Implement `LocalMediaScanner` class
- [ ] Implement `ContentResolver` query for MediaStore (Images/Video/Audio)
- [ ] Implement `java.io.File` fallback for folder walking (non-indexed)
- [ ] Create `MediaType` enum mapping (ext -> type)
- [ ] **Universal Access (Epic 2 Update)**: 
  - [ ] Implement logic to bypass filters if `workWithAllFiles` is true
  - [ ] Map unknown extensions to `MediaType.OTHER`

### 2.2 GetMediaFiles UseCase
- [ ] Create `GetMediaFilesUseCase(resource: Resource)`
- [ ] Implement sorting logic (Date Descending default)
- [ ] Connect to `LocalMediaScanner`

### 2.3 Pagination Support
- [ ] Add `limit` and `offset` parameters to scanner repositories
- [ ] **Validate**: Scanning 1000+ files does not block main thread

## 3. UI Core: Browsing

### 3.1 Resource List (MainActivity)
- [ ] Implement `ResourceAdapter` (RecyclerView)
- [ ] Create `MainViewModel` to expose resources
- [ ] Add Floating Action Button (FAB) for "Add Resource"
- [ ] **Density Rule**: Ensure compact list items (4dp padding)

### 3.2 Add Resource (Local)
- [ ] Implement `AddResourceActivity`
- [ ] Add "Local Folder" selection flow
- [ ] Implement folder picker (System or Custom)
- [ ] Save new resource to DB via `ResourceRepository`

### 3.3 File Browser (BrowseActivity)
- [ ] Implement `BrowseActivity`
- [ ] Create `BrowseViewModel` (loads files by resourceId)
- [ ] Implement `MediaFileAdapter` (Grid/Linear support)
- [ ] Integrate Glide for thumbnail loading
- [ ] **Universal Access**: Render `MediaType.OTHER` with generic icon

## 4. File Operations System

### 4.1 Strategy Pattern
- [ ] Define `FileOperationStrategy` interface (`copy`, `move`, `delete`, `rename`)

### 4.2 Local Strategy
- [ ] Implement `LocalOperationStrategy` using `java.io` / NIO
- [ ] Ensure all IO happens on `Dispatchers.IO`

### 4.3 Undo System
- [ ] Implement "Recycle Bin" logic (move to `.trash`)
- [ ] Create index for original paths
- [ ] Implement "Restore" function

### 4.4 Favorites
- [ ] Add `isFavorite` flag to DB/Metadata
- [ ] Add Heart icon overlay in UI grid

## 5. Universal File Support ("All Files")

### 5.1 Configuration
- [ ] Implement "Work with all files" toggle in Settings
- [ ] Implement override in Resource settings

### 5.2 UI Adaptation
- [ ] Update BrowseActivity to show/hide `OTHER` files based on flag
- [ ] Update PlayerActivity to skip `OTHER` files in playlist
