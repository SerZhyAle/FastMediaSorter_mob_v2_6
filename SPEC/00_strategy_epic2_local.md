# Tactical Plan: Epic 2 - Local File Management (MVP)

**Goal**: Fully functional local file manager.
**Deliverable**: App capable of browsing, viewing, and managing local files.
**Prerequisite**: Epic 1 Complete.

---

## 1. Permissions & Onboarding

### 1.1 Storage Permissions
- **Action**: Implement Android Storage Permission logic.
- **Logic**:
  - Check `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` depending on Android version.
  - Handle `MANAGE_EXTERNAL_STORAGE` (if targeted for All Files Access use-case, though Play Store restricted).
  - Create `StoragePermissionManager` helper.
  - **Scoped Storage Edge Cases (Android 11+)**:
    - **Problem**: Apps cannot access `/Android/data`, `/Android/obb`, or other apps' private directories.
    - **Solution**: Use Storage Access Framework (SAF) for restricted folders.
    - **Implementation**:
      ```kotlin
      fun requestFolderAccess(activity: Activity) {
          val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
          activity.startActivityForResult(intent, REQUEST_CODE_SAF)
      }
      
      fun handleSafResult(uri: Uri) {
          // Persist permission
          contentResolver.takePersistableUriPermission(
              uri,
              Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          )
          
          // Save URI to resource
          resourceRepository.addSafResource(uri.toString())
      }
      ```
    - **Plan B for Inaccessible Folders**:
      - Detect when folder is inaccessible (`SecurityException` or empty list with known files).
      - Show user dialog: "This folder requires special permission. Use folder picker instead."
      - Fallback: Offer to browse parent folder or use SAF picker.
      - **DO NOT** crash or hang on permission denied.

### 1.2 Onboarding Flow
- **Action**: Create a "Welcome" screen.
- **Features**:
  - Simple ViewPager or single Layout explaining app purpose.
  - Request Permissions button.
  - Proceed to Main Activity only after critical permissions granted.

---

## 2. Local Discovery Engine

### 2.1 Media Scanner
- **Action**: Implement `LocalMediaScanner`.
- **Logic**:
  - Use `ContentResolver` (MediaStore) for initial fast scan of Images/Video/Audio.
  - Implement fallback `java.io.File` walking for non-indexed folders (if permission allows).
  - Map various file extensions to `MediaType` enum.

### 2.2 GetMediaFiles UseCase
- **Action**: Create Domain logic for fetching files.
- **UseCase**: `GetMediaFilesUseCase(resource: Resource)`.
- **Logic**:
  - Identify if resource is LOCAL.
  - Call `LocalMediaScanner`.
  - Sort results (Default: Date Descending).

### 2.3 Pagination Support
- **Action**: Structure scanner for pagination.
- **Logic**: Support `limit` and `offset` in scanner methods to handle folders with 1000+ files efficiently.

---

## 3. UI Core: Browsing

### 3.1 Resource List (MainActivity)
- **Action**: Implement the starting screen.
- **Components**:
  - `ResourceAdapter`: RecyclerView adapter for "Shortcuts" (Folders/SMB shares).
  - `MainViewModel`: Expose list of Resources from Repository.
  - FAB: "Add Resource" button.

### 3.2 Add Resource (Local)
- **Action**: Implement `AddResourceActivity`.
- **Features**:
  - Select "Local Folder" type.
  - User input: Folder Path (or system folder picker).
  - Save to DB via `ResourceRepository`.

### 3.3 File Browser (BrowseActivity)
- **Action**: Implement the main grid view.
- **Components**:
  - `BrowseViewModel`: Takes `resourceId`, loads files via UseCase.
  - `MediaFileAdapter`: Grid/Linear Switchable adapter.
  - Thumbnail Loading: Integrate Glide.

---

## 4. File Operations System

### 4.1 Strategy Pattern
- **Action**: Define `FileOperationStrategy` interface.
- **Methods**: `copy()`, `move()`, `delete()`, `rename()`.

### 4.2 Local Strategy
- **Action**: Implement `LocalOperationStrategy`.
- **Logic**:
  - Use standard `java.io` or `NIO` file operations.
  - Ensure operations are performed on IO Dyspatcher.

### 4.3 Undo System
- **Action**: Implement "Recycle Bin" logic.
- **Logic**:
  - "Delete" acts as "Move to .trash hidden folder".
  - Maintain an index of original paths.
  - "Undo" moves from .trash back to original path.

### 4.4 Favorites
- **Action**: Implement "Favorite" marking.
- **DB**: Add `isFavorite` column to metadata or separate `FavoriteEntity`.
- **UI**: Heart icon overlay on grid items.

---

## 5. Universal File Support ("All Files")

### 5.1 Configuration Logic
- **Action**: Implement "Work with all files" override.
- **Levels**: Global (Settings) OR Resource-Specific.
- **Rule**: If enabled, all specific "Supported Media Types" filters are ignored (effectively all checked + unknown types).

### 5.2 Scanner Update
- **Action**: Modify `LocalMediaScanner`.
- **Logic**:
  - If `allFiles` enabled: Remove file extension filters.
  - Map unknown extensions to `MediaType.OTHER`.
  - Extract basic metadata (Size, Date, Name) for `OTHER` types.

### 5.3 UI Adaptation
- **BrowseActivity**: Render `OTHER` files with generic file icon (no thumbnail). Allow Rename/Delete/Copy/Move.
- **PlayerActivity**: Filter out `OTHER` types from playlist (auto-skip).
