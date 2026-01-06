# 9. RecyclerView & ViewPager Adapters

## Content Adapters

### 1. ResourceAdapter

**Package:** `com.sza.fastmediasorter.ui.main`
**Usage:** `MainActivity`
**Key Features:**

- Dual ViewType: `VIEW_TYPE_GRID` (Cards) / `VIEW_TYPE_LIST` (Rows).
- **Payload Updates:** Efficiently updates only changed fields (e.g., file count) via DiffUtil payloads.
- **Context Menu:** Handles long-press actions (Edit, Delete, Move Up/Down).
- **Visuals:** Shows resource type icons (SMB/Cloud/Local) and "IVAGTPE" media type badges.

**Callbacks** (provided in constructor):

1. **onItemClick(resource: MediaResource)**
   - Triggered: Single tap on resource card.
   - Action: Select resource → Open BrowseActivity.
   
2. **onItemLongClick(resource: MediaResource)**
   - Triggered: Long press on resource card.
   - Action: Check PIN protection → Open EditResourceActivity.
   
3. **onEditClick(resource: MediaResource)**
   - Triggered: Click on Edit button in resource card.
   - Action: Check PIN protection → Open EditResourceActivity.
   
4. **onCopyFromClick(resource: MediaResource)**
   - Triggered: Click on Copy button in resource card.
   - Action: Select resource → Enter copy mode (shows destination picker).
   
5. **onDeleteClick(resource: MediaResource)**
   - Triggered: Click on Delete button in resource card.
   - Action: Show confirmation dialog → Delete resource from DB.
   
6. **onMoveUpClick(resource: MediaResource)**
   - Triggered: Click on Move Up button (up arrow) in resource card.
   - Action: Swap position with previous resource (manual ordering).
   - Availability: Only shown when `sortMode == MANUAL`.
   
7. **onMoveDownClick(resource: MediaResource)**
   - Triggered: Click on Move Down button (down arrow) in resource card.
   - Action: Swap position with next resource (manual ordering).
   - Availability: Only shown when `sortMode == MANUAL`.

**View Binding**:
- **Grid Mode**: Compact cards (3 columns phone, 5 columns tablet), icon + name only.
- **List Mode**: Detailed cards, shows full info (name, path, file count, last scan date, media type badges, destination marker).

**Selection Highlight**:
- Selected resource card has highlighted background (uses `setSelectedResource(resourceId)`).
- Background color: `?attr/colorControlHighlight` with 20% alpha.

**Media Type Badges**: 
- "IVAGTPE" format: Images, Videos, Audio, GIFs, Text, PDF, EPUB counts.
- Example: "I:150 V:20 A:5" displayed in small text under resource name.

### 2. MediaFileAdapter

**Package:** `com.sza.fastmediasorter.ui.browse`
**Usage:** `BrowseActivity` (Standard list)
**Key Features:**

- **Selection:** Multi-select with "Shift-Select" (range selection) capability.
- **Thumbnails:** Glide integration with `NetworkFileData` for auth-protected network images.
- **Cloud Support:** Handles Google Drive thumbnail tokens.
- **View Modes:** Grid (thumbnails only) vs List (details).

### 3. PagingMediaFileAdapter

**Package:** `com.sza.fastmediasorter.ui.browse`
**Usage:** `BrowseActivity` (Large directories > 1000 files)
**Key Features:**

- Extends `PagingDataAdapter`.
- Optimized for 10k+ file lists.
- Uses placeholders during scroll.

### 4. PagingLoadStateAdapter

**Package:** `com.sza.fastmediasorter.ui.browse`
**Usage:** Footer loader for Paging lists.
**Key Features:** Shows "Loading..." spinner or "Retry" button at the bottom of the list.

### 5. DestinationAdapter

**Package:** `com.sza.fastmediasorter.ui.dialog`
**Usage:** `CopyToDialog`, `MoveToDialog`
**Key Features:**

- Lists available destination resources for quick file operations.
- Shows destination index (1-10) and color code.
- Filterable list (if search is implemented).

---

## Cloud Folder Adapters

Located in `com.sza.fastmediasorter.ui.cloudfolders`. Used in Picker Activities.

- **`GoogleDriveFolderAdapter`**: Lists Drive folders. Handles parent/child navigation.
- **`OneDriveFolderAdapter`**: Layout and logic for OneDrive SDK items.
- **`DropboxFolderAdapter`**: For Dropbox API V2.

---

## Settings & Onboarding

### 1. SettingsPagerAdapter

**Package:** `com.sza.fastmediasorter.ui.settings`
**Usage:** `SettingsActivity` main tabs (General, Media, Playback, Destinations).

### 2. MediaCategoryPagerAdapter

**Package:** `com.sza.fastmediasorter.ui.settings`
**Usage:** Nested tabs within "Media" settings (Image, Video, Audio, Docs).

### 3. WelcomePagerAdapter

**Package:** `com.sza.fastmediasorter.ui.welcome`
**Usage:** First-run onboarding slides.
