# FastMediaSorter v2 🚀

![Status](https://img.shields.io/badge/Status-Production%20Ready-success?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple?style=flat-square&logo=kotlin)
![Android](https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

## About the Project

**FastMediaSorter v2** is a powerful Android application for quick and convenient sorting of media files (images, videos, GIFs, audio). It is designed as a single center for managing files from various sources: local device folders, network drives (SMB, SFTP, FTP), and cloud storage (Google Drive, OneDrive, Dropbox).

The key idea of v2 is to combine viewing, playback, and organization of files in one intuitive interface, eliminating the shortcomings and limitations of the previous version.

## Table of Contents
- [Download](#download-)
- [Key Features](#key-features)
- [Screenshots](#screenshots-)
- [Usage Scenarios](#usage-scenarios-)
- [Documentation](#documentation--документация-)
- [Build Instructions](#build-instructions)
- [Tech Stack](#technology-stack)

## Download 📥

> **[Download Latest APK](https://github.com/SerZhyAle/FastMediaSorter_mob_v2/releases)**

## Screenshots 📱

<!-- Place screenshots here. Example structure: -->
<!-- 
| Main Screen | Player | Settings |
|:-----------:|:------:|:--------:|
| <img src="..." width="200"> | <img src="..." width="200"> | <img src="..." width="200"> |
-->
*(Screenshots coming soon)*

## Key Features

*   🗂️ **Unified Interface:** View and manage files from all sources in one window.
*   ⚡ **Fast Sorting:** Copy or move files to pre-configured destination folders with one click.
*   ⭐ **Favorites System:** Mark important files as favorites and access them quickly from a dedicated tab that aggregates favorites across all sources.
*   🔒 **PIN Protection:** Secure individual resources with access PIN codes to prevent unauthorized browsing and editing.
*   ⚙️ **Per-Resource Configuration:** Customize slideshow interval, scan depth (subdirectories), and thumbnail generation for each folder individually.
*   🖥️ **Network and Cloud Support:** Work with files on your network drives (SMB), SFTP servers, FTP, and in cloud storage (Google Drive, Dropbox, OneDrive).
*   🖼️ **Flexible Viewing:** Display files as a customizable grid or detailed list with pagination support for large collections (1000+ files).
*   ▶️ **Built-in Player:** Playback of video and audio, viewing images and GIFs without leaving the app. Supports slideshow and full-screen zooming.
*   🎵 **Lyrics Support:** View song lyrics for the currently playing track. Automatically searches by metadata (Artist/Title) using `api.lyrics.ovh`, with fallback to filename parsing.
*   ✏️ **Image Editing:** Rotate, flip, apply filters (grayscale, sepia, negative), adjust brightness/contrast/saturation - for both local and network files.
*   ⌨️ **Keyboard Navigation:** Full keyboard support for all activities (arrow keys, function keys, shortcuts).
*   🔍 **Sorting and Filtering:** Order files by name, date, size, and duration. Apply filters for quick search.
*   ↩️ **Undo & Trash:** Ability to undo the last action (copy, move, delete) with soft-delete to `.trash/` folder. Includes "Empty Trash" functionality for resources.
*   🎨 **Modern Interface:** Support for light and dark themes, intuitive controls, Material Design 3.
*   💾 **Smart Caching:** Two-stage video metadata loading (1MB initial, 5MB extended) and configurable thumbnail cache (2GB default, up to 16GB).
*   📄 **Document Viewer:** Built-in viewer for Text files (.txt, .md, .log, .json, .xml) and PDF documents with zoom, pan, and gesture navigation.
*   � **EPUB E-Book Reader:** Native EPUB reader with chapter navigation, table of contents, font size control, in-book search, and dark/light theme support. Works with local and network files.
*   �📥 **Download & Open:** Download network files (SMB/SFTP/FTP) to local storage and open them in external apps with progress tracking.
*   🌐 **Auto-Translation:** Instantly translate text from images, PDFs, and text files using a **Hybrid OCR System** (Google ML Kit + Tesseract) for superior accuracy in both Latin and Cyrillic scripts.

## Supported Media Formats 🎞️

FastMediaSorter v2 supports a wide range of formats:

*   **Images:** JPG, JPEG, PNG, GIF, BMP, WEBP, HEIC, HEIF
*   **Video:** MP4, MKV, AVI, MOV, WMV, FLV, WEBM, M4V, 3GP, MPG, MPEG
*   **Audio:** MP3, FLAC, AAC, OGG, M4A, WMA, OPUS
*   **Documents:** TXT, MD, LOG, JSON, XML, PDF, **EPUB**

## Usage Scenarios 💡

Here are a few ways FastMediaSorter v2 can help you:

### 1. 📸 Organizing Camera Photos
Connect your phone or open a local camera folder. Set up a "Best Photos" destination folder. Open the viewer, quickly swipe through thousands of photos, and tap the destination button to instantly copy the best shots.

### 2. 🏠 Network Backup (NAS)
Add your home NAS via SMB. Browse your local media files. Select multiple files or a range, and "Move" them to your NAS for safe keeping, freeing up space on your device.

### 3. ☁️ Cloud Management
Connect your Google Drive, Dropbox, or OneDrive account. Browse your cloud files without downloading them all. Delete unwanted files or organize them into folders directly in the cloud.

### 4. 📺 Slideshow & Presentation
Open a folder with family photos or presentation slides. Hit "Play" to start a slideshow. Use the per-resource settings to adjust the slide duration to your liking.

### 5. ⭐ Managing Favorites
Mark important files with the star button while browsing. Later, tap the "Favorites" tab in the main menu to instantly access all your favorite files from all sources in one place - perfect for creating a curated collection of your best media.

## Documentation / Документация 📚

Detailed guides are available in multiple languages:
(Подробные руководства доступны на нескольких языках:)

*   🇺🇸 **English:** [How-To Guides](HOW_TO.md) | [Quick Start](QUICK_START.md)
*   🇷🇺 **Русский:** [Руководства](HOW_TO_RU.md) | [Быстрый Старт](QUICK_START_RU.md)
*   🇺🇦 **Українська:** [Посібники](HOW_TO_UK.md) | [Швидкий Старт](QUICK_START_UK.md)

Also check out:
*   [FAQ](FAQ.md)
*   [Troubleshooting](TROUBLESHOOTING.md)

## Build Instructions

### Requirements
*   Android Studio Hedgehog (2023.1.1) or newer
*   JDK 17+
*   Android SDK 34
*   Minimum Android version: 9.0 (API 28)

### Build
1.  Clone the repository:
    ```bash
    git clone https://github.com/yourusername/FastMediaSorter_mob_v2.git
    cd FastMediaSorter_mob_v2
    ```
2.  Open the project in Android Studio.
3.  Wait for Gradle synchronization to complete.
4.  Run the app on an emulator or physical device.

## First Steps (Quick Usage Guide) 🚀

1.  **Adding a Folder (Resource):**
    *   On the main screen, press the button with the "Plus" (+) icon to add a new resource.
    *   Select the resource type (e.g., "Local Folder").
    *   Use scanning or add the folder manually. After adding, it will appear in the list on the main screen.

2.  **Viewing Files:**
    *   Double-tap (or long press) on the added resource in the list.
    *   The browse screen will open, where you will see all media files from this folder as a list or grid.
    *   Use the buttons on the top panel for sorting, filtering, or switching view.

3.  **Playback and Sorting:**
    *   Tap on any file to open it in the full-screen player.
    *   Use swipes left/right or touch zones for navigation between files.
    *   For operations (copy, move), use the corresponding touch zones or buttons on the control panel.

4.  **Configuring Destination Folders (Destinations):**
    *   In settings, on the "Destinations" tab, you can specify up to 30 folders that will be used for quick sorting.
    *   Alternatively, enable "Is Destination" in any resource's edit screen to add it to the quick sort list.
    *   After that, buttons for quick copying or moving files to these folders will appear on the player screen.

## Technology Stack

-   **Language**: Kotlin
-   **Architecture**: Clean Architecture, MVVM
-   **UI**: Android View System (XML), Material Design 3
-   **Asynchrony**: Kotlin Coroutines & Flow
-   **DI**: Hilt (Dagger)
-   **Database**: Room (version 6 with cloud provider support)
-   **Navigation**: AndroidX Navigation Component
-   **Media**: ExoPlayer (Media3 1.2.1)
-   **Image Loading**: Glide 4.15.1 with custom NetworkFileModelLoader
-   **Network Protocols**: 
    -   SMB: SMBJ 0.12.1 with BouncyCastle 1.78.1
    -   SFTP: SSHJ 0.37.0 with EdDSA 0.3.0
    -   FTP: Apache Commons Net 3.10.0
-   **Cloud**: Google Drive API, OneDrive (MSAL), Dropbox API with OAuth 2.0
-   **OCR & Translation**: Google ML Kit (Text Recognition, Translation), Tesseract4Android (Cyrillic OCR)
-   **Search & Lyrics**: api.lyrics.ovh (JSON API)

## Project Status

✅ **Production Ready** - Core functionality fully implemented and tested:
- ✅ Local file operations (copy, move, delete, undo)
- ✅ Network protocols (SMB, SFTP, FTP)
- ✅ Cloud storage integration (Google Drive, OneDrive, Dropbox with OAuth authentication)
- ✅ Image editing (rotation, flip, filters, adjustments)
- ✅ Pagination for large file collections (1000+ files)
- ✅ Keyboard navigation across all screens
- ✅ Smart caching with two-stage metadata loading
- ✅ Soft-delete with trash folder support
- ✅ Favorites system with cross-resource aggregation

🚧 **In Progress**:
- Performance optimization for 5000+ file collections
- UnifiedFileOperationHandler refactoring (Phase 3-6)

## Build Version

Current: **v2.25.1212.0158** (December 12, 2025)
Format: `Y.YM.MDDH.Hmm` (e.g., `2.51.2161.854` for 2025/12/16 18:54)

### Recent Updates (Build 0158)
- ✅ **I/O Performance Optimization**: Increased buffer sizes from 8KB to 64KB across all transfer operations
  - InputStreamExt.copyToWithProgress, LocalTransferProvider, SftpClient
  - Cloud clients: GoogleDriveRestClient, OneDriveRestClient, DropboxClient
  - Reduces syscall overhead significantly for network file transfers
- ✅ **RecyclerView Optimization**: Added `onViewRecycled()` to `PagingMediaFileAdapter`
  - Explicit Glide request cancellation when views are recycled
  - Frees ConnectionThrottleManager slots immediately for network resources
- ✅ **Glide Module**: Already optimized with DiskCacheStrategy.RESOURCE, 40% memory cache, configurable disk cache

### Previous Updates (Build 0136)
- ✅ **SAF Parallel Scanning**: ~3x speedup for folders with subfolders (WhatsApp: 20s → 7s)
  - 2-phase strategy: BFS folder discovery → parallel file scanning
  - Uses `networkParallelism` setting from Settings (limited 2-8 for SAF)
  - Prevents deadlock through separation of discovery and scanning phases
- ✅ **Slideshow Button Color Fix**: Command Panel slideshow button now changes color (red when active)
  - Added `CommandPanelController.updateSlideshowButtonColor()` with `imageTintList` for ImageButton
  - Synchronized with `updateSlideShowButton()` in PlayerActivity
- ✅ **Log Level Adjustment**: "Failed to extract embedded cover art" → warning (was error)

### Previous Updates (Build 2303 - Dec 6, 2025)
- ✅ **Favorites System**: 
  - Added star button in Browse view for quick favorite marking
  - Favorites tab in main menu opens dedicated Browse window with all favorites from all sources
  - Automatic tab restoration when returning from Favorites view
  - Fixed favorite icon update using DiffUtil payload for efficient UI updates
- ✅ **Player Stability**: Fixed video restart issue when toggling favorites during playback - now only metadata updates without interrupting playback
- ✅ **Thumbnail Loading**: 
  - Fixed deferred thumbnail loading mechanism in Browse view
  - Added `childCount > 0` check before triggering thumbnail load
  - Implemented `post {}` delayed execution for RecyclerView items not yet bound
- ✅ **UI Improvements**: 
  - Enhanced logging for thumbnail loading diagnostics
  - Fixed favorite button icon state updates without full ViewHolder rebind
  - Proper DiffUtil.ItemCallback with getChangePayload() for FAVORITE_CHANGED events

### Previous Updates (Build 0325)
- ✅ **File Operations Robustness**:
  - Implemented `UnifiedFileOperationHandler` for consistent behavior across all protocols (Local, SMB, FTP, SFTP, Cloud).
  - Fixed SFTP -> FTP transfers by using temporary files to prevent OutOfMemoryError.
  - Improved Move/Delete reliability with better error handling and "undo" logic that respects file boundaries.
- ✅ **Compilation Fixes**: Resolved all lingering compilation errors in `FavoritesUseCase`, `PlayerActivity`, and `ImageLoadingManager`.
- ✅ **Build Script**: Modified to launch Logcat (debugger) in a new window asynchronously, without blocking the build process.

### Previous Updates (Build 0025)
- ✅ Added per-resource configuration (slideshow interval, subdirectories, thumbnails)
- ✅ Added "Empty Trash" functionality for resources
- ✅ Fixed media type filter persistence across Browse/Player navigation
- ✅ Fixed filter dialog showing all checkboxes when specific filter active
- ✅ Restored filter state from resource.supportedMediaTypes on resource load
- ✅ Added filter synchronization when returning from Player to Browse
- ✅ Optimized command panel button layout (40dp buttons, -6dp margins)

---

## Contributing 🤝

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License 📄

Distributed under the MIT License. See `LICENSE` for more information.

*This file was generated based on project documentation.*