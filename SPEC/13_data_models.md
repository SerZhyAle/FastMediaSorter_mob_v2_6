# 13. Data Models

## 1. Domain Models

These are the core data structures used throughout the app's business logic (`domain` layer).

### MediaResource

**Role:** Represents a folder source (Local or Network).
**Key Fields:**

- `id`: Long
- `name`: String
- `path`: String
- `type`: ResourceType
- `credentialsId`: String? (Link to encrypted credentials)
- `supportedMediaTypes`: Set<MediaType> (Image, Video, Audio, etc.)
- `sortMode`: SortMode
- `displayMode`: DisplayMode (List/Grid)
- `isDestination`: Boolean (Quick copy/move target)
- `destinationOrder`: Int?
- `destinationColor`: Int (UI color)
- `isReadOnly`: Boolean
- `scanSubdirectories`: Boolean
- `accessPin`: String?
- `networkPerformance`: `readSpeedMbps`, `writeSpeedMbps`

### MediaFile

**Role:** Represents a single file item in the browser/player.
**Key Fields:**

- `path`: String (URI or File path)
- `name`: String
- `size`: Long
- `type`: MediaType
- `dateModified`: Long
- `duration`: Long? (Video/Audio)
- `width`/`height`: Int? (Image/Video)
- `exifData`: Orientation, Date, Lat/Long
- `cloudMetadata`: `thumbnailUrl`, `webViewUrl`, `isFavorite`

### AppSettings

**Role:** Global user preferences.
**Key Categories:**

- **UI**: `isResourceGridMode`, `theme`, `language`
- **Media Support**: `supportImages`, `supportVideos`, `loadFullSizeImages`, `imageSizeMax`
- **Network**: `networkParallelism` (threads), `backgroundSyncIntervalHours`
- **Safety**: `enableSafeMode` (confirms delete/move), `enableUndo`
- **Player**: `slideshowInterval`, `defaultSortMode`, `showCommandPanel`

### FileFilter

**Role:** Criteria for filtering file lists.
**Fields:**

- `nameContains`: String?
- `dateRange`: `minDate`, `maxDate`
- `sizeRange`: `minSizeMb`, `maxSizeMb`
- `mediaTypes`: Set<MediaType>?

### UndoOperation

**Role:** Stores details of a file operation to allow reversal.
**Fields:**

- `type`: `COPY`, `MOVE`, `RENAME`, `DELETE`
- `sourceFiles`: List<String>
- `destinationFolder`: String?
- `oldNames`: List<Pair<String, String>>? (For Rename)

---

## 2. Database Entities (Room)

These map directly to SQLite tables (`data` layer).

### ResourceEntity (Table: `resources`)

**Role:** Persistence for `MediaResource`.
**Fields:**

- `id`: Primary Key (Auto-generate)
- `name`: Text
- `path`: Text
- `type`: Text (Enum converter)
- `credentials_id`: Text (Nullable)
- `configuration_json`: Text (Stores specific logic flags like `scanSubdirectories`)
- `ui_state_json`: Text (Stores `lastScrollPosition`, `sortMode`)

> **Note:** The mapping between `ResourceEntity` and `MediaResource` involves parsing JSON fields for extensible configuration storage.

---

## 3. Enums

### ResourceType

`LOCAL`, `SMB`, `SFTP`, `FTP`, `CLOUD`

### MediaType

`IMAGE`, `VIDEO`, `AUDIO`, `GIF`, `TEXT`, `PDF`, `EPUB`

### SortMode

`MANUAL`, `NAME_ASC/DESC`, `DATE_ASC/DESC`, `SIZE_ASC/DESC`, `TYPE_ASC/DESC`, `RANDOM`
