# Project Structure Diagram

**Last Updated**: January 6, 2026  
**Architecture**: Clean Architecture (UI → Domain → Data)

```
app_v2/                          # Active production codebase
├── src/main/
│   ├── java/com/sza/fastmediasorter/
│   │   ├── ui/                  # Activities, Fragments, Adapters
│   │   │   ├── main/           # MainActivity (resource list)
│   │   │   ├── browse/         # BrowseActivity (file grid/list)
│   │   │   ├── player/         # PlayerActivity + 7 helper managers
│   │   │   ├── settings/       # SettingsActivity + 4 fragments
│   │   │   ├── addresource/    # AddResourceActivity
│   │   │   ├── editresource/   # EditResourceActivity
│   │   │   ├── welcome/        # WelcomeActivity (onboarding)
│   │   │   └── cloudfolders/   # Cloud folder pickers
│   │   ├── domain/              # UseCases (single-responsibility)
│   │   │   ├── usecase/        # Business logic encapsulation
│   │   │   └── repository/     # Repository interfaces (no implementations)
│   │   ├── data/                # Repository implementations, Room, Network
│   │   │   ├── local/          # Room database (version 6)
│   │   │   ├── network/        # SMB/SFTP/FTP clients
│   │   │   ├── cloud/          # Google Drive/OneDrive/Dropbox clients
│   │   │   └── repository/     # Repository implementations
│   │   ├── core/                # BaseActivity, extensions, utilities
│   │   ├── widget/              # Home screen widgets (Favorites, Launch, Continue Reading)
│   │   └── di/                  # Hilt modules (Network, Database, Repository)
│   └── res/                     # Layouts, drawables, strings (en/ru/uk)
├── build.gradle.kts
└── AndroidManifest.xml

dev/                             # Build scripts, TODO, strategic plans
docs/                            # Technical documentation
spec_v2/                         # This specification directory
├── diagrams/                    # Reusable visual assets
│   ├── database_er_diagram.md  # Room DB entity relationships
│   ├── navigation_flow.md      # Activity navigation paths
│   ├── navigation_mermaid.md   # Mermaid graph
│   └── project_structure.md    # This file
├── detailed_logic/              # Extended behavior specs
└── *.md                         # 35 specification documents
```

## Layer Dependencies

```
┌──────────────────────────────────────────────────────┐
│ UI Layer (Activities, Fragments, ViewModels)        │
│ - Observes StateFlow/SharedFlow from ViewModels     │
│ - Zero business logic                                │
└───────────────────────┬──────────────────────────────┘
                        │
                        ↓ Uses
┌──────────────────────────────────────────────────────┐
│ Domain Layer (UseCases, Repository interfaces)      │
│ - Single-responsibility UseCases                     │
│ - No Android dependencies                            │
└───────────────────────┬──────────────────────────────┘
                        │
                        ↓ Implements
┌──────────────────────────────────────────────────────┐
│ Data Layer (Repository implementations, Room, Net)  │
│ - Room database (local persistence)                 │
│ - Network clients (SMB/SFTP/FTP/Cloud)              │
│ - Strategy pattern for file operations              │
└──────────────────────────────────────────────────────┘
```

## Key Directories

| Directory | Purpose | Key Files |
|-----------|---------|-----------|
| `ui/main/` | Resource list screen | MainActivity, MainViewModel, ResourceAdapter |
| `ui/browse/` | File browsing | BrowseActivity, BrowseViewModel, MediaFileAdapter |
| `ui/player/` | Media playback | PlayerActivity + 7 managers (VideoPlayer, TextViewer, etc.) |
| `domain/usecase/` | Business logic | 35 UseCases (GetMediaFiles, CopyFile, MoveFile, etc.) |
| `data/local/` | Room DB | AppDatabase, ResourceDao, FavoritesDao, etc. |
| `data/network/` | Network clients | SmbClient, SftpClient, FtpClient |
| `data/cloud/` | Cloud APIs | GoogleDriveClient, OneDriveClient, DropboxClient |
| `di/` | Hilt DI | NetworkModule, DatabaseModule, RepositoryModule |
