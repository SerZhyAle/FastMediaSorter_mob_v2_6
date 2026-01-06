# Core Logic & Rules

This document serves as the repository for **Business Logic**, **Algorithms**, and **Hard Constraints** of the application. It abstracts the "how" from the code, focusing on the rules that must be preserved when rewriting the application.

Detailed specifications are broken down into the following modules:

## 1. [Sorting and Filtering Logic](detailed_logic/core_rules/sorting_and_filtering.md)

- **Content**: Business logic for sorting files and resources, deduplication rules, and filtering strategies.
- **Key Implementations**: `BrowseFileListManager`, `ResourceRepositoryImpl`.

## 2. [File Operations Logic](detailed_logic/core_rules/file_operations.md)

- **Content**: Conflict resolution (Copy/Move), atomic operations, and deletion strategies (Trash vs Permanent).
- **Key Implementations**: `FileOperationUseCase`, `CopyToDialog`.

## 3. [Platform Specifics & Legacy Quirks](detailed_logic/core_rules/platform_specifics.md)

- **Content**: Android version differences (SAF vs File API), vendor-specific workarounds (Samsung/Xiaomi).
- **Key Implementations**: `ExoPlayer` quirks, `DocumentFile` handling.

## 4. [Data Persistence](detailed_logic/core_rules/data_persistence.md)

- **Content**: Database schemas rules, SharedPreferences usage, and Caching strategies (`UnifiedFileCache`).

## 5. Universal Access Logic (Work with All Files)

- **Concept**: User can opt-in to manage non-media files.
- **Rules**:
  - **Scanner**: When `workWithAllFiles` is TRUE:
    - Filters: Bypass standard extension filters (Audio/Video/Image/Doc).
    - Identification: If extension is not recognized, assign `MediaType.OTHER`.
    - Metadata: Extract only basic file info (Name, Size, Date, MimeType from extension).
  - **Player**:
    - Playlist: `MediaType.OTHER` is excluded from playback queues.
    - Navigation: Skip `OTHER` files when pressing Next/Prev.
  - **Browsing**:
    - Display: Use generic "File" icon for `OTHER`.
    - Operations: Allow Rename, Delete, Move, Copy, Share.
    - Preview: On click, attempt `Intent.ACTION_VIEW` via external app or show "No preview" toast.
