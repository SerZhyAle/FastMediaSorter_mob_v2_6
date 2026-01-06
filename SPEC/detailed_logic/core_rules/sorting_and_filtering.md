# Sorting and Filtering Logic

## 1. File Sorting (MediaFile)

Implemented in `BrowseFileListManager` and `BrowseViewModel`.

- **Name**: Case-insensitive (`name.lowercase()`).
- **Date**: Uses `createdDate` timestamp.
- **Size**: Uses file size in bytes.
- **Type**: Uses `MediaType` enum ordinal (Image, Video, Audio, etc).
- **Random**: Uses `shuffled()` on the list.
- **Manual**: Preserves original order (no sorting applied).

> [!NOTE]
> File Lists are deduplicated by `path` before sorting. Duplicates are silently dropped.

## 2. Resource Sorting (Folders/Drives)

Implemented in `ResourceRepositoryImpl`.

- **Name**: `String.CASE_INSENSITIVE_ORDER`.
- **Date**: Uses `createdDate`.
- **Size**: Uses `fileCount` (number of files in the resource).
- **Type**: Uses `ResourceType` (Local, SMB, Cloud).

## 3. Filtering Strategies

- _TODO: Define filtering logic (Name contains, Date range, Size range)._
