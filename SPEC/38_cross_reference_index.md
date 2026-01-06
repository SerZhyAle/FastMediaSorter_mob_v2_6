# Cross-Reference Index

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Purpose**: Quick lookup for "Where is X defined?"

---

## 1. By Feature

| Feature | Primary Spec | Related Specs | Implementation Notes |
|---------|--------------|---------------|---------------------|
| **Browse Files** | [03_screen_browse.md](03_screen_browse.md) | [04_screen_browse_details.md](04_screen_browse_details.md), [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Uses Paging3 for 1000+ files |
| **Media Player** | [06_screen_player.md](06_screen_player.md) | [13_media_playback_engine.md](13_media_playback_engine.md) | ExoPlayer with custom controls |
| **Image Viewer** | [06_screen_player.md](06_screen_player.md) | [10_image_editing_requirements.md](10_image_editing_requirements.md) | PhotoView for pinch-zoom |
| **PDF Viewer** | [06_screen_player.md](06_screen_player.md) | [11_pdf_viewing.md](11_pdf_viewing.md) | PdfRenderer API |
| **Image Editor** | [10_image_editing_requirements.md](10_image_editing_requirements.md) | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Crop, rotate, filter, text overlay |
| **File Operations** | [07_screen_operations.md](07_screen_operations.md) | [16_core_logic_and_rules.md](16_core_logic_and_rules.md), [20_file_operation_handlers.md](20_file_operation_handlers.md) | Copy, move, delete with undo |
| **SMB Connection** | [08_screen_add_network.md](08_screen_add_network.md) | [17_network_protocols.md](17_network_protocols.md), [27_protocol_smb.md](27_protocol_smb.md) | SMBJ 0.12.1, connection pooling |
| **SFTP Connection** | [08_screen_add_network.md](08_screen_add_network.md) | [17_network_protocols.md](17_network_protocols.md), [28_protocol_sftp.md](28_protocol_sftp.md) | SSHJ 0.37.0, SSH key support |
| **FTP Connection** | [08_screen_add_network.md](08_screen_add_network.md) | [17_network_protocols.md](17_network_protocols.md), [29_protocol_ftp.md](29_protocol_ftp.md) | Apache Commons Net 3.10.0 |
| **Google Drive** | [09_screen_add_cloud.md](09_screen_add_cloud.md) | [18_cloud_providers.md](18_cloud_providers.md), [24_cloud_google_drive.md](24_cloud_google_drive.md) | OAuth 2.0, Drive API v3 |
| **OneDrive** | [09_screen_add_cloud.md](09_screen_add_cloud.md) | [18_cloud_providers.md](18_cloud_providers.md), [25_cloud_onedrive.md](25_cloud_onedrive.md) | MSAL for Android |
| **Dropbox** | [09_screen_add_cloud.md](09_screen_add_cloud.md) | [18_cloud_providers.md](18_cloud_providers.md), [26_cloud_dropbox.md](26_cloud_dropbox.md) | Dropbox SDK v6 |
| **Favorites** | [12_favorites.md](12_favorites.md) | [03_screen_browse.md](03_screen_browse.md) | Room DB with FavoriteEntity |
| **Widget** | [05_widget.md](05_widget.md) | [12_favorites.md](12_favorites.md) | Destinations + Favorites quick access |
| **Settings** | [33_settings_and_preferences.md](33_settings_and_preferences.md) | Multiple | SharedPreferences + PreferenceScreen |
| **Localization** | [34_localization_guide.md](34_localization_guide.md) | All UI specs | English, Russian, Ukrainian |
| **Testing** | [30_testing_strategy.md](30_testing_strategy.md) | All specs | Unit, Integration, UI tests |
| **Performance** | [32_performance_metrics.md](32_performance_metrics.md) | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | <1s browse, 60fps scrolling |
| **Analytics** | [36_analytics_strategy.md](36_analytics_strategy.md) | [35_release_checklist.md](35_release_checklist.md) | Firebase Analytics + Crashlytics |

---

## 2. By Class/Component

### Domain Layer (UseCases)

| Class/Interface | Defined In | Purpose | Dependencies |
|-----------------|------------|---------|--------------|
| `GetMediaFilesUseCase` | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Load files from resource | ResourceRepository |
| `CopyFileUseCase` | [20_file_operation_handlers.md](20_file_operation_handlers.md) | Copy file between resources | FileOperationStrategy |
| `MoveFileUseCase` | [20_file_operation_handlers.md](20_file_operation_handlers.md) | Move file with undo support | FileOperationStrategy |
| `DeleteFileUseCase` | [20_file_operation_handlers.md](20_file_operation_handlers.md) | Soft delete to `.trash/` | FileOperationStrategy |
| `EditImageUseCase` | [10_image_editing_requirements.md](10_image_editing_requirements.md) | Crop, rotate, filter image | Bitmap processing |
| `AddResourceUseCase` | [08_screen_add_network.md](08_screen_add_network.md) | Add network/cloud resource | ResourceRepository |
| `TestConnectionUseCase` | [17_network_protocols.md](17_network_protocols.md) | Validate network credentials | Protocol clients |
| `AuthenticateCloudUseCase` | [18_cloud_providers.md](18_cloud_providers.md) | OAuth 2.0 authentication | Cloud clients |
| `ScanFolderUseCase` | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Discover media files | MediaScanner |

### Data Layer (Repositories)

| Class/Interface | Defined In | Purpose | Data Source |
|-----------------|------------|---------|-------------|
| `ResourceRepository` | [19_repository_layer.md](19_repository_layer.md) | Manage resources (CRUD) | Room DAO + network clients |
| `MediaFileRepository` | [19_repository_layer.md](19_repository_layer.md) | File metadata cache | Room DAO + file scanners |
| `NetworkCredentialsRepository` | [27_protocol_smb.md](27_protocol_smb.md) | Store SMB/SFTP credentials | EncryptedSharedPreferences |
| `CloudTokenRepository` | [18_cloud_providers.md](18_cloud_providers.md) | OAuth token persistence | EncryptedSharedPreferences |
| `FavoriteRepository` | [12_favorites.md](12_favorites.md) | Favorites CRUD | Room DAO |

### Network Clients

| Class/Interface | Defined In | Protocol | Key Methods |
|-----------------|------------|----------|-------------|
| `SmbClient` | [27_protocol_smb.md](27_protocol_smb.md) | SMB 2/3 | `connect()`, `listFiles()`, `copyFile()` |
| `SftpClient` | [28_protocol_sftp.md](28_protocol_sftp.md) | SFTP | `connect()`, `listFiles()`, `download()` |
| `FtpClient` | [29_protocol_ftp.md](29_protocol_ftp.md) | FTP/FTPS | `connect()`, `listFiles()`, `upload()` |
| `GoogleDriveClient` | [24_cloud_google_drive.md](24_cloud_google_drive.md) | Google Drive API | `authenticate()`, `listFiles()`, `download()` |
| `OneDriveClient` | [25_cloud_onedrive.md](25_cloud_onedrive.md) | OneDrive API | `authenticate()`, `listFiles()`, `upload()` |
| `DropboxClient` | [26_cloud_dropbox.md](26_cloud_dropbox.md) | Dropbox API | `authenticate()`, `listFiles()`, `metadata()` |

### UI Components

| Class/Interface | Defined In | Purpose | Key Features |
|-----------------|------------|---------|--------------|
| `BrowseViewModel` | [03_screen_browse.md](03_screen_browse.md) | Browse screen logic | StateFlow, Paging3, SearchView |
| `PlayerViewModel` | [06_screen_player.md](06_screen_player.md) | Media playback logic | ExoPlayer lifecycle, navigation |
| `OperationsViewModel` | [07_screen_operations.md](07_screen_operations.md) | File operation queue | Progress tracking, undo stack |
| `MediaFileAdapter` | [03_screen_browse.md](03_screen_browse.md) | RecyclerView adapter | Multi-select, thumbnail loading |
| `PagingMediaFileAdapter` | [03_screen_browse.md](03_screen_browse.md) | Paging3 adapter | For 1000+ files |
| `TextViewerManager` | [06_screen_player.md](06_screen_player.md) | Text file viewer | Syntax highlighting |
| `VideoPlayerManager` | [06_screen_player.md](06_screen_player.md) | ExoPlayer wrapper | Custom controls |

### Database Entities

| Entity | Defined In | Purpose | Key Columns |
|--------|------------|---------|-------------|
| `ResourceEntity` | [02_data_model.md](02_data_model.md) | Resource definition | `id`, `name`, `type`, `path`, `color` |
| `MediaFileEntity` | [02_data_model.md](02_data_model.md) | Cached file metadata | `path`, `size`, `date`, `type`, `thumbnailUrl` |
| `FavoriteEntity` | [12_favorites.md](12_favorites.md) | Favorite files | `fileId`, `addedDate` |
| `FileVersionEntity` | [16a_offline_sync_conflicts.md](16a_offline_sync_conflicts.md) | Version tracking | `filePath`, `lastKnownModifiedTime`, `eTag` |
| `PendingOperationEntity` | [16a_offline_sync_conflicts.md](16a_offline_sync_conflicts.md) | Offline operation queue | `operation`, `sourcePath`, `destPath` |

---

## 3. By Error Code

| Error Code | Defined In | Meaning | User Action |
|------------|------------|---------|-------------|
| `ERROR_NETWORK_TIMEOUT` | [21_common_pitfalls.md](21_common_pitfalls.md) | Connection timeout | Check network, retry |
| `ERROR_AUTH_FAILED` | [17_network_protocols.md](17_network_protocols.md) | Invalid credentials | Re-enter username/password |
| `ERROR_DISK_FULL` | [37_edge_cases_matrix.md](37_edge_cases_matrix.md) | Insufficient space | Free up space on device |
| `ERROR_FILE_NOT_FOUND` | [20_file_operation_handlers.md](20_file_operation_handlers.md) | Source file missing | File deleted or moved |
| `ERROR_PERMISSION_DENIED` | [21_common_pitfalls.md](21_common_pitfalls.md) | No access rights | Check folder permissions |
| `ERROR_OAUTH_EXPIRED` | [18_cloud_providers.md](18_cloud_providers.md) | Token expired | Re-authenticate |
| `ERROR_RATE_LIMIT` | [31a_api_rate_limiting.md](31a_api_rate_limiting.md) | Too many requests | Wait and retry (auto) |
| `ERROR_CODEC_UNSUPPORTED` | [13_media_playback_engine.md](13_media_playback_engine.md) | Video codec not supported | Try on different device |
| `ERROR_OOM` | [32a_memory_large_files.md](32a_memory_large_files.md) | Out of memory | File too large for device |
| `ERROR_CIRCUIT_BREAKER_OPEN` | [14a_network_resilience.md](14a_network_resilience.md) | Service unavailable | Wait 30s, auto-retry |

---

## 4. By Architecture Pattern

### Clean Architecture Layers

| Layer | Package | Purpose | Specs |
|-------|---------|---------|-------|
| **UI** | `com.app.fastmediasorter.ui` | Activities, Fragments, ViewModels | [03-09](03_screen_browse.md) (screens) |
| **Domain** | `com.app.fastmediasorter.domain` | UseCases, domain models | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) |
| **Data** | `com.app.fastmediasorter.data` | Repositories, Room, network | [19_repository_layer.md](19_repository_layer.md) |

### Design Patterns

| Pattern | Where Used | Defined In | Purpose |
|---------|------------|------------|---------|
| **MVVM** | All screens | [01_architecture_overview.md](01_architecture_overview.md) | UI-ViewModel separation |
| **Repository Pattern** | Data layer | [19_repository_layer.md](19_repository_layer.md) | Abstract data sources |
| **UseCase Pattern** | Domain layer | [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Single-responsibility operations |
| **Strategy Pattern** | File operations | [20_file_operation_handlers.md](20_file_operation_handlers.md) | Protocol-specific implementations |
| **Factory Pattern** | Client creation | [17_network_protocols.md](17_network_protocols.md) | Protocol client instantiation |
| **Observer Pattern** | StateFlow/LiveData | [01_architecture_overview.md](01_architecture_overview.md) | Reactive UI updates |
| **Circuit Breaker** | Network resilience | [14a_network_resilience.md](14a_network_resilience.md) | Fail fast, recover gracefully |
| **Connection Pool** | SMB/SFTP | [27_protocol_smb.md](27_protocol_smb.md) | Reuse connections |

---

## 5. By Dependency (External Library)

| Library | Version | Purpose | Specs |
|---------|---------|---------|-------|
| **Hilt** | 2.50 | Dependency injection | [01_architecture_overview.md](01_architecture_overview.md) |
| **Room** | 2.6.1 | Local database | [02_data_model.md](02_data_model.md) |
| **ExoPlayer** | Media3 1.2.1 | Video/audio playback | [13_media_playback_engine.md](13_media_playback_engine.md) |
| **Glide** | 4.16.0 | Image loading/caching | [15_ui_guidelines.md](15_ui_guidelines.md) |
| **SMBJ** | 0.12.1 | SMB protocol | [27_protocol_smb.md](27_protocol_smb.md) |
| **SSHJ** | 0.37.0 | SFTP protocol | [28_protocol_sftp.md](28_protocol_sftp.md) |
| **Apache Commons Net** | 3.10.0 | FTP protocol | [29_protocol_ftp.md](29_protocol_ftp.md) |
| **Google Drive API** | 2.2.0 | Google Drive integration | [24_cloud_google_drive.md](24_cloud_google_drive.md) |
| **MSAL** | 4.9.0 | OneDrive auth | [25_cloud_onedrive.md](25_cloud_onedrive.md) |
| **Dropbox SDK** | 6.0.0 | Dropbox integration | [26_cloud_dropbox.md](26_cloud_dropbox.md) |
| **PhotoView** | 2.3.0 | Image zoom/pan | [06_screen_player.md](06_screen_player.md) |
| **Timber** | 5.0.1 | Logging | [21_common_pitfalls.md](21_common_pitfalls.md) |
| **Firebase Analytics** | 32.7.0 | Usage tracking | [36_analytics_strategy.md](36_analytics_strategy.md) |
| **Firebase Crashlytics** | 32.7.0 | Crash reporting | [36_analytics_strategy.md](36_analytics_strategy.md) |

---

## 6. By Testing Strategy

| Test Type | Defined In | Tools | Coverage Target |
|-----------|------------|-------|-----------------|
| **Unit Tests** | [30_testing_strategy.md](30_testing_strategy.md) | JUnit, MockK | 80% for UseCases |
| **Integration Tests** | [30_testing_strategy.md](30_testing_strategy.md) | Room Testing, Hilt | 70% for Repositories |
| **UI Tests** | [30_testing_strategy.md](30_testing_strategy.md) | Espresso | 50% critical flows |
| **Migration Tests** | [30_testing_strategy.md](30_testing_strategy.md) | MigrationTestHelper | 100% all migrations |
| **Edge Case Tests** | [37_edge_cases_matrix.md](37_edge_cases_matrix.md) | All above | 90% MUST cases |

---

## 7. By User Scenario

| User Story | Primary Screen | Related Specs | Epic |
|------------|----------------|---------------|------|
| "Browse local photos" | Browse | [03_screen_browse.md](03_screen_browse.md), [16_core_logic_and_rules.md](16_core_logic_and_rules.md) | Epic 2 |
| "View video from SMB share" | Player | [06_screen_player.md](06_screen_player.md), [27_protocol_smb.md](27_protocol_smb.md) | Epic 4 |
| "Edit image and save" | Player (Edit mode) | [10_image_editing_requirements.md](10_image_editing_requirements.md) | Epic 3 |
| "Copy files from OneDrive to local" | Operations | [07_screen_operations.md](07_screen_operations.md), [25_cloud_onedrive.md](25_cloud_onedrive.md) | Epic 5 |
| "Add to favorites" | Browse (context menu) | [12_favorites.md](12_favorites.md) | Epic 6 |
| "Quick access via widget" | Home screen | [05_widget.md](05_widget.md) | Epic 6 |

---

## 8. By Performance Metric

| Metric | Target | Defined In | Measurement |
|--------|--------|------------|-------------|
| **App Start Time** | <2s (cold) | [32_performance_metrics.md](32_performance_metrics.md) | Firebase Performance |
| **Browse Load (1000 files)** | <1s | [32_performance_metrics.md](32_performance_metrics.md) | Custom trace |
| **Scrolling FPS** | 60fps | [32_performance_metrics.md](32_performance_metrics.md) | GPU profiler |
| **Thumbnail Load** | <200ms/image | [32_performance_metrics.md](32_performance_metrics.md) | Glide metrics |
| **Video Playback Start** | <500ms | [13_media_playback_engine.md](13_media_playback_engine.md) | ExoPlayer analytics |
| **Network Connection** | <5s | [14a_network_resilience.md](14a_network_resilience.md) | Health check |

---

## 9. By Settings Key

| Setting | Key | Type | Default | Spec |
|---------|-----|------|---------|------|
| **Analytics Enabled** | `analytics_enabled` | Boolean | `false` | [36_analytics_strategy.md](36_analytics_strategy.md) |
| **Thumbnail Cache Size** | `thumbnail_cache_size_mb` | Long | `2048` | [32_performance_metrics.md](32_performance_metrics.md) |
| **Auto-Rotate Images** | `auto_rotate_images` | Boolean | `true` | [33_settings_and_preferences.md](33_settings_and_preferences.md) |
| **Warn Mobile Data** | `warn_mobile_data` | Boolean | `true` | [14a_network_resilience.md](14a_network_resilience.md) |
| **Language** | `language` | String | `"system"` | [34_localization_guide.md](34_localization_guide.md) |
| **Default Sort Order** | `default_sort_order` | String | `"name_asc"` | [03_screen_browse.md](03_screen_browse.md) |

---

## 10. Usage Instructions

### "Where is [feature] implemented?"
1. Check **Section 1 (By Feature)** for primary spec
2. Follow links to related specs for details
3. Check **Section 2 (By Class)** for code location

### "Where is [error] handled?"
1. Check **Section 3 (By Error Code)**
2. Find related spec for error handling logic
3. Check [21_common_pitfalls.md](21_common_pitfalls.md) for mitigation

### "Which library is used for [protocol]?"
1. Check **Section 5 (By Dependency)**
2. Find version and primary spec
3. Read protocol-specific spec (27-29 for network, 24-26 for cloud)

### "How is [pattern] applied?"
1. Check **Section 4 (By Architecture Pattern)**
2. Find pattern definition spec
3. Check usage examples in code

---

## 11. Maintenance

**Update This Index When**:
- New spec file created → Add to Section 1
- New class/interface added → Add to Section 2
- New error code introduced → Add to Section 3
- New dependency added → Add to Section 5

**Review Frequency**: Before each Epic milestone
