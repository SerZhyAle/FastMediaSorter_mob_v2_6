# Implementation Status Matrix
*Last Updated: January 6, 2026*

Mapping between architectural epics (`epics_reference/`) and real codebase (`app_v2/`).

---

## Epic 1: Foundation & Architecture
**Reference**: [todo_epic1_foundation.md](todos/todo_epic1_foundation.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Android Studio Project | ✅ Done | `app_v2/` | Existing production project |
| 1.2 | Version Control | ✅ Done | `.git/` | Active Git repo |
| 1.3 | Dependency Management | ✅ Done | `gradle/libs.versions.toml` | Version catalog exists |
| 1.4 | Pre-commit Hooks | ⏸️ Deferred | N/A | Manual code review only |
| 2.1 | Hilt DI | ✅ Done | `FastMediaSorterApp.kt` | `@HiltAndroidApp` configured |
| 2.2 | Base UI Logic | ✅ Done | `ui/base/` | BaseActivity, BaseFragment exist |
| 2.3 | Timber Logging | ✅ Done | `FastMediaSorterApp.kt` | Configured in DEBUG |
| 3.1 | Room Database | ✅ Done | `data/db/AppDatabase.kt` | Version 6 |
| 3.2 | Hilt Database Module | ✅ Done | `di/DatabaseModule.kt` | Provides DAOs |
| 3.3 | Core Entities | ✅ Done | `data/db/entity/` | ResourceEntity, FileOperationHistoryEntity |
| 3.4 | DAO Interfaces | ✅ Done | `data/db/dao/` | ResourceDao, etc. |
| 3.5 | Test Data Strategy | ⏸️ Deferred | N/A | No formal "Golden Set" directory |
| 4.1-4.4 | Domain Contracts | ✅ Done | `domain/model/`, `domain/repository/` | Result, MediaFile, repositories |

**Epic 1 Score**: 11/14 tasks (78%) - Core architecture complete, testing infrastructure deferred

---

## Epic 2: Local File Management
**Reference**: [todo_epic2_local.md](todos/todo_epic2_local.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Storage Permissions | ✅ Done | `ui/main/PermissionsHandler.kt` | Multi-version support |
| 1.2 | Onboarding Flow | ✅ Done | `ui/welcome/` | ViewPager with permissions |
| 2.1 | LocalMediaScanner | ✅ Done | `data/scanner/LocalMediaScanner.kt` | MediaStore + fallback |
| 2.2 | GetMediaFilesUseCase | ✅ Done | `domain/usecase/GetMediaFilesUseCase.kt` | Sorting implemented |
| 2.3 | Pagination Support | ✅ Done | `data/scanner/` | Paging3 integration (>1000 files) |
| 3.1 | Resource List (MainActivity) | ✅ Done | `ui/main/MainActivity.kt` | RecyclerView + FAB |
| 3.2 | Add Resource (Local) | ✅ Done | `ui/resource/AddResourceActivity.kt` | Folder picker |
| 3.3 | File Browser (BrowseActivity) | ✅ Done | `ui/browse/BrowseActivity.kt` | Grid/Linear adapters |
| 4.1 | Strategy Pattern | ✅ Done | `data/fileops/strategy/` | BaseFileOperationHandler |
| 4.2 | Local Strategy | ✅ Done | `LocalOperationStrategy.kt` | java.io + NIO |
| 4.3 | Undo System | ✅ Done | `domain/usecase/undo/` | `.trash/` soft-delete |
| 4.4 | Favorites | ✅ Done | `data/db/entity/FileMetadataEntity.kt` | `isFavorite` flag |
| 5.x | Universal File Support | ✅ Done | `MediaType.OTHER` | All files mode |

**Epic 2 Score**: 13/13 tasks (100%) - Local file management complete

---

## Epic 3: Media Player & Viewer Engine
**Reference**: [todo_epic3_player.md](todos/todo_epic3_player.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | PlayerActivity Framework | 🚧 Refactor | `ui/player/PlayerActivity.kt` | God Class (2,700 lines) → 7 helpers extracted (Phase 3) |
| 1.2 | Glide Image Loading | ✅ Done | `util/image/` | Custom ModelLoaders for network |
| 1.3 | PhotoView Integration | ✅ Done | `layout/activity_player.xml` | Zoom/pan gestures |
| 2.1 | ExoPlayer Integration | ✅ Done | `ui/player/helpers/VideoPlayerManager.kt` | media3 wrapper |
| 2.2 | Video Controls | ✅ Done | `ExoPlayerControlsManager.kt` | Seekbar, speed, gestures |
| 2.3 | Audio Player Features | ⏸️ Deferred | N/A | Background service not implemented |
| 3.1 | PDF Viewer | ✅ Done | `util/PdfRenderer.kt` | Native Android renderer |
| 3.2 | Text Viewer | ✅ Done | `ui/player/helpers/TextViewerManager.kt` | Encoding detection |
| 4.1 | Image Editor | ✅ Done | `ui/editor/ImageEditActivity.kt` | Rotate, crop, brightness |
| 4.2 | GIF Support | ✅ Done | Glide built-in | Speed control not available |

**Epic 3 Score**: 8/10 tasks (80%) - Core player complete, refactoring in progress

**Active Issues**:
- PlayerActivity decomposition (Phase 3 completed Dec 2024)
- Touch zones blocking video controls (fixed Build .xxx)
- Audio background playback (low priority)

---

## Epic 4: Network Protocols
**Reference**: [todo_epic4_network.md](todos/todo_epic4_network.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Secure Credentials Storage | ✅ Done | `data/repository/NetworkCredentialsRepository.kt` | EncryptedSharedPreferences |
| 2.1 | SMB Client | ✅ Done | `data/network/smb/SmbClient.kt` | SMBJ 0.12.1 + BouncyCastle |
| 2.2 | SMB Scanner | ✅ Done | `data/scanner/SmbMediaScanner.kt` | Recursive listing |
| 2.3 | SMB Operations | ✅ Done | `data/fileops/strategy/SmbOperationStrategy.kt` | Streaming support |
| 3.1 | SFTP Client | ✅ Done | `data/network/sftp/SftpClient.kt` | SSHJ 0.37.0 + EdDSA |
| 3.2 | SFTP Operations | ✅ Done | `SftpOperationStrategy.kt` | Scanner + strategy pattern |
| 4.1 | FTP Client | ✅ Done | `data/network/ftp/FtpClient.kt` | Commons Net 3.10.0 |
| 4.1.1 | FTP PASV Timeout | 🚧 Known Issue | N/A | Active mode fallback implemented |
| 5.1 | Remote Edit Pipeline | ✅ Done | `domain/usecase/NetworkImageEditUseCase.kt` | Download-Edit-Upload |
| 5.2 | UnifiedFileCache | ✅ Done | `data/cache/UnifiedFileCache.kt` | Build 2.51.2172.336 |

**Epic 4 Score**: 9/10 tasks (90%) - Network protocols operational

**Active Issues**:
- FTP PASV mode timeouts (workaround exists)
- SMB connection pool cleanup (45s idle timeout)

---

## Epic 5: Cloud Integration
**Reference**: [todo_epic5_cloud.md](todos/todo_epic5_cloud.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Deep Linking & OAuth Redirects | ✅ Done | `AndroidManifest.xml` | Custom schemes configured |
| 1.2 | Secure Token Storage | ✅ Done | `data/repository/NetworkCredentialsRepository.kt` | Token refresh logic |
| 2.1 | Google Drive API | 🚧 In Progress | `data/cloud/GoogleDriveClient.kt` | **OAuth Android client setup required** |
| 2.2 | Google Drive Operations | 🚧 Partial | `CloudOperationStrategy.kt` | Basic list/download works |
| 3.1 | OneDrive MSAL | ⏸️ Deferred | N/A | Not started |
| 3.2 | OneDrive Graph API | ⏸️ Deferred | N/A | Not started |
| 4.1 | Dropbox SDK | ⏸️ Deferred | N/A | Not started |
| 4.2 | Dropbox Operations | ⏸️ Deferred | N/A | Not started |

**Epic 5 Score**: 2/8 tasks (25%) - Google Drive partial, others deferred

**Critical Blocker**: Google Drive Phase 3 requires SHA-1 fingerprint in Google Cloud Console (see `dev/TODO_V2.md` Current Tasks)

---

## Epic 6: Advanced Capabilities
**Reference**: [todo_epic6_advanced.md](todos/todo_epic6_advanced.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Text Recognition (OCR) | ✅ Done | `util/ocr/TesseractManager.kt` | Tesseract4Android (not ML Kit) |
| 1.2 | On-Device Translation | ⏸️ Deferred | N/A | Not implemented |
| 1.3 | Google Lens Overlay | ⏸️ Deferred | N/A | Not implemented |
| 2.1 | Recursive Search Engine | ✅ Done | `domain/usecase/SearchUseCase.kt` | DB + live scan |
| 2.2 | Document Indexing | ⏸️ Deferred | N/A | No FTS4 table |
| 3.1 | App Widgets | ⏸️ Deferred | N/A | Not implemented |
| 3.2 | App Shortcuts | ⏸️ Deferred | N/A | Static shortcuts not defined |

**Epic 6 Score**: 2/7 tasks (28%) - Core features only

---

## Epic 7: Polish & User Experience
**Reference**: [todo_epic7_quality.md](todos/todo_epic7_quality.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Settings Screen | ✅ Done | `ui/settings/SettingsActivity.kt` | PreferenceFragmentCompat |
| 1.2 | DataStore Migration | ⏸️ Deferred | N/A | Still using SharedPreferences |
| 2.1 | Localization (EN/RU/UK) | ✅ Done | `res/values-ru/`, `res/values-uk/` | All strings extracted |
| 2.2 | Accessibility Compliance | ⏸️ Partial | N/A | Basic compliance, no full audit |
| 3.1 | Performance Tuning | 🚧 Ongoing | N/A | Cold start ~300-400ms, scrolling optimized |
| 3.2 | Large Dataset Test | ✅ Done | N/A | Pagination handles 1000+ files |

**Epic 7 Score**: 4/6 tasks (66%) - Core UX complete, polish items deferred

**Active Work**:
- Slideshow improvements (Dec 20, 2024)
- 16 KB page size compatibility (Dec 20, 2024)

---

## Epic 8: Release Engineering
**Reference**: [todo_epic8_release.md](todos/todo_epic8_release.md)

| Task ID | Description | Status | Location | Notes |
|---------|-------------|--------|----------|-------|
| 1.1 | Automated Testing | ⏸️ Partial | `app_v2/src/test/` | <50% coverage |
| 1.2 | Manual Verification | 🚧 Ongoing | N/A | Ad-hoc device testing |
| 2.1 | ProGuard / R8 | ✅ Done | `app_v2/build.gradle.kts` | Minification enabled |
| 2.2 | Signing & Keys | ✅ Done | `keystore.properties` | Upload key configured |
| 3.1 | User Manual | ✅ Done | Root docs | README, FAQ, HOW_TO (EN/RU/UK) |
| 3.2 | Store Assets | ✅ Done | `store_assets/` | Screenshots, feature graphic |

**Epic 8 Score**: 4/6 tasks (66%) - Production-ready, testing gaps

**Release Status**: Currently at Build 2.51.2201.xxx (versioning via `build-with-version.ps1`)

---

## Overall Project Status

| Epic | Completion | Priority | Notes |
|------|------------|----------|-------|
| Epic 1: Foundation | 78% | Critical | ✅ Core architecture solid |
| Epic 2: Local Files | 100% | Critical | ✅ Complete |
| Epic 3: Player | 80% | High | 🚧 Refactoring ongoing |
| Epic 4: Network | 90% | High | ✅ Operational (minor issues) |
| Epic 5: Cloud | 25% | Medium | 🚧 Google Drive OAuth blocker |
| Epic 6: Advanced | 28% | Low | ⏸️ Nice-to-have features |
| Epic 7: Polish | 66% | Medium | 🚧 Incremental improvements |
| Epic 8: Release | 66% | Critical | ✅ Production-ready |

**Overall Completion**: ~70% of planned features

---

## Active Development Priorities (Q1 2026)

From `dev/TODO_V2.md`:

1. **Google Drive Phase 3**: Complete OAuth Android client setup (SHA-1 fingerprint)
2. **Testing & Validation**: Verify refactored strategy pattern handlers across all protocols
3. **Pagination Testing**: Real-world 1000+ file scenarios
4. **Network Undo**: Production testing of `.trash/` soft-delete system
5. **FTP Stability**: Address PASV mode edge cases

---

## Legend

- ✅ **Done**: Fully implemented and production-tested
- 🚧 **In Progress**: Active development or refactoring
- ⏸️ **Deferred**: Planned but low priority
- ❌ **Blocked**: External dependency or blocker
- 🔴 **Known Issue**: Bug or limitation tracked in TODO_V2.md
