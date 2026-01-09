# FastMediaSorter v2 - Development TODO

**Last Updated:** January 9, 2026  
**Status:** Active Development - Release Phase  
**Version:** v2.0.0-dev

---

## 📊 Critical Path to Release

### Priority 1: Testing & QA (Epic 8)
- [ ] Automated Unit Tests (>80% coverage goal)
- [ ] Manual Device Testing (Android 9.0 - 14+)
- [ ] Network Edge Case Testing
- [ ] Performance SLA Validation
- [ ] Store Preparation

### Priority 2: Cloud Integration (Post-Release - v2.1)
- [ ] Google Drive OAuth Setup
- [ ] OneDrive Integration
- [ ] Dropbox Integration

---

## 🚧 Epic 5: Cloud Integration (DEFERRED TO v2.1)

### 5.1 Google Drive - Phase 3
- [ ] Complete OAuth Android client setup in Google Cloud Console
- [ ] Add SHA-1 fingerprint from signing keystore
- [ ] Test OAuth flow end-to-end
- [ ] Implement refresh token handling
- [ ] Test file operations (list, download, upload)

### 5.2 OneDrive Integration
- [ ] Add MSAL (Microsoft Authentication Library) dependency
- [ ] Register app in Azure AD portal
- [ ] Implement MSAL OAuth flow
- [ ] Create OneDriveClient class
- [ ] Implement OneDriveOperationStrategy
- [ ] Test Graph API file operations

### 5.3 Dropbox Integration
- [ ] Add Dropbox Core SDK dependency
- [ ] Register app in Dropbox Developer Console
- [ ] Implement OAuth 2.0 flow
- [ ] Create DropboxClient class
- [ ] Implement DropboxOperationStrategy
- [ ] Test file operations

### 5.4 Cloud Folder Pickers
- [ ] GoogleDriveFolderPickerActivity
- [ ] OneDriveFolderPickerActivity
- [ ] DropboxFolderPickerActivity
- [ ] Cloud authentication status indicators in AddResourceActivity

**Reference:** `SPEC/06_cloud_pickers.md`, `SPEC/43_oauth_setup_guide.md`

---

## 🧪 Epic 8: Testing & Release Engineering

### 8.1 Automated Testing ⚠️ CRITICAL

**Current Status:** <50% coverage  
**Goal:** >80% coverage on domain layer

**Test Coverage Summary:**
```
✅ Completed Tests: 15
⚠️ Missing Tests: 50+
🔵 Low Priority Tests: 6

Category Breakdown:
├── ViewModels:      5/17  (29%) - 12 missing
├── Repositories:    2/8   (25%) - 6 missing  
├── UseCases:        5/13  (38%) - 8 missing
├── File Operations: 0/5   (0%)  - ALL MISSING ⚠️ CRITICAL
├── Managers:        0/12  (0%)  - 12 missing
└── Domain Models:   3/3   (100%) ✅
```

**Critical Priorities:**
1. **FileOperationStrategy Tests** - Core functionality, zero coverage
2. **Repository Tests** - Data layer foundation
3. **ViewModel Tests** - User-facing logic
4. **UseCase Tests** - Business rules
5. **Manager Tests** - Feature-specific logic

**Unit Tests Needed:**

#### ViewModel Tests
- [x] BrowseViewModel ✅
- [x] PlayerViewModel ✅
- [x] AddResourceViewModel ✅
- [x] EditResourceViewModel ✅
- [x] SettingsViewModel ✅
- [ ] MainViewModel ⚠️ **MISSING**
- [ ] SearchViewModel ⚠️ **MISSING**
- [ ] FavoritesViewModel ⚠️ **MISSING**
- [ ] DestinationPickerViewModel ⚠️ **MISSING**
- [ ] PlaybackSettingsViewModel ⚠️ **MISSING**
- [ ] GeneralSettingsViewModel ⚠️ **MISSING**
- [ ] DestinationsSettingsViewModel ⚠️ **MISSING**
- [ ] MediaSettingsViewModel ⚠️ **MISSING**
- [ ] BaseCloudFolderPickerViewModel (abstract) ⚠️ **MISSING**
- [ ] GoogleDriveFolderPickerViewModel ⚠️ **MISSING**
- [ ] OneDriveFolderPickerViewModel ⚠️ **MISSING**
- [ ] DropboxFolderPickerViewModel ⚠️ **MISSING**

#### Repository Tests
- [x] ResourceRepositoryImpl ✅
- [x] PlaybackPositionRepository ✅
- [ ] MediaRepositoryImpl ⚠️ **MISSING**
- [ ] FileMetadataRepositoryImpl ⚠️ **MISSING**
- [ ] PreferencesRepositoryImpl ⚠️ **MISSING**
- [ ] NetworkCredentialsRepositoryImpl ⚠️ **MISSING**
- [ ] StressTestRepositoryImpl (debug only) 🔵 **LOW PRIORITY**
- [ ] AlbumArtRepository ⚠️ **MISSING**

#### UseCase Tests
- [x] AddResourceUseCase ✅
- [x] GetResourcesUseCase ✅
- [x] UpdateResourceUseCase ✅
- [x] DeleteResourceUseCase ✅
- [x] GetMediaFilesUseCase ✅
- [ ] GetPaginatedMediaFilesUseCase ⚠️ **MISSING**
- [ ] GetFavoriteFilesUseCase ⚠️ **MISSING**
- [ ] GlobalSearchUseCase ⚠️ **MISSING**
- [ ] GetNetworkCredentialsUseCase ⚠️ **MISSING**
- [ ] SaveNetworkCredentialsUseCase ⚠️ **MISSING**
- [ ] DeleteNetworkCredentialsUseCase ⚠️ **MISSING**
- [ ] TestNetworkConnectionUseCase ⚠️ **MISSING**
- [ ] GenerateStressDataUseCase (debug only) 🔵 **LOW PRIORITY**

#### FileOperationStrategy Tests ⚠️ **CRITICAL - ALL MISSING**
- [ ] LocalOperationStrategy
  - Test file listing, reading, writing
  - Test error handling for permissions
  - Test directory operations
- [ ] SmbOperationStrategy
  - Test SMB connection with credentials
  - Test anonymous access
  - Test workgroup vs domain scenarios
  - Mock jCIFS library interactions
- [ ] SftpOperationStrategy
  - Test SFTP connection with password
  - Test key-based authentication
  - Mock JSch library interactions
- [ ] FtpOperationStrategy
  - Test FTP active/passive mode
  - Test FTPS with TLS
  - Mock Apache Commons Net interactions
- [ ] CloudOperationStrategy
  - Test Google Drive operations
  - Test OneDrive operations
  - Test Dropbox operations

#### Manager Tests (May require instrumented tests for Android dependencies)
- [ ] PdfEditManager ⚠️ **MISSING**
  - Test PDF page extraction
  - Test PDF rotation
  - Test PDF page deletion
- [ ] PdfToolsManager ⚠️ **MISSING**
  - Test PDF export to images
  - Test PDF metadata extraction
- [ ] EpubReaderManager ⚠️ **MISSING**
  - Test EPUB parsing
  - Test chapter navigation
  - Test TOC extraction
- [ ] TranslationManager ⚠️ **MISSING**
  - Test OCR text extraction
  - Test translation API calls
  - Mock ML Kit dependencies
- [ ] OcrManager ⚠️ **MISSING**
  - Test image text recognition
  - Mock ML Kit Text Recognition
- [ ] LyricsManager ⚠️ **MISSING**
  - Test lyrics fetching
  - Test lyrics synchronization
- [ ] VideoPlayerManager 🔵 **LOW PRIORITY** (requires ExoPlayer mocks)
- [ ] AudioPlayerManager 🔵 **LOW PRIORITY** (requires ExoPlayer mocks)
- [ ] TesseractManager 🔵 **LOW PRIORITY** (requires Tesseract OCR mocks)
- [ ] TouchZoneGestureManager 🔵 **LOW PRIORITY** (UI gesture handling)
- [ ] TrashManager ⚠️ **MISSING**
- [ ] GoogleDriveCredentialsManager ⚠️ **MISSING**

#### Domain Model Tests
- [x] SearchFilterTest ✅
- [x] ResultTest ✅
- [x] MediaExtensionsTest ✅
- [ ] Additional model validation tests 🔵 **LOW PRIORITY**

**Instrumented Tests Needed:**
- [ ] Database Tests
  - [ ] ResourceDao CRUD operations
  - [ ] MediaFileDao operations
  - [ ] Migration tests (Schema 1-6)
- [ ] UI Tests
  - [ ] MainActivity navigation
  - [ ] BrowseActivity file operations
  - [ ] PlayerActivity media playback
  - [ ] AddResourceActivity form validation

**Test Commands:**
```powershell
# Unit tests
.\gradlew test

# Instrumented tests  
.\gradlew connectedAndroidTest

# Coverage report
.\gradlew jacocoTestReport
```

### 8.2 Manual QA Testing

**Device Matrix:**
- [ ] Low-end device (Android 9.0, 2GB RAM)
- [ ] Mid-range device (Android 11.0, 4GB RAM)
- [ ] High-end device (Android 14+, 8GB RAM)
- [ ] Tablet (7" and 10")

**Network Protocol Testing:**
- [ ] Local storage (SD card, internal)
- [ ] SMB/CIFS (Windows shares)
  - [ ] Test anonymous access
  - [ ] Test authenticated access
  - [ ] Test workgroup vs domain
- [ ] SFTP (SSH File Transfer)
  - [ ] Test password authentication
  - [ ] Test key-based authentication
  - [ ] Test non-standard ports
- [ ] FTP/FTPS
  - [ ] Test active vs passive mode
  - [ ] Test TLS/SSL connections
  - [ ] Test anonymous FTP

**Edge Case Testing:**
- [ ] Airplane mode transitions
- [ ] Flaky WiFi scenarios
- [ ] Connection drops during file operations
- [ ] Large file uploads/downloads (>1GB)
- [ ] 1000+ file collections (pagination stress test)
- [ ] Rapid file operations (race conditions)
- [ ] Low storage scenarios
- [ ] Battery optimization testing

**Performance SLA Validation:**
- [ ] Cold start time < 500ms
- [ ] Scroll FPS stable at 60fps
- [ ] Network timeout < 10s
- [ ] Memory usage reasonable on low-end devices
- [ ] No ANR (Application Not Responding) errors
- [ ] Battery drain acceptable during sync

**Accessibility Audit:**
- [ ] TalkBack navigation test
- [ ] Touch target sizes (48dp minimum)
- [ ] Color contrast compliance (WCAG 2.1 AA)
- [ ] Content descriptions complete

**Security Audit:**
- [ ] Credential encryption verified
- [ ] No hardcoded secrets
- [ ] ProGuard rules secure
- [ ] Permissions properly scoped
- [ ] Network security config validated

### 8.3 Store Preparation

- [ ] Final Release Build
  - [ ] Verify minification works
  - [ ] Test release APK thoroughly
  - [ ] Check APK size (<200MB goal)
- [ ] Store Listing (All Languages: EN/RU/UK)
  - [ ] App title (50 chars max)
  - [ ] Short description (80 chars max)
  - [ ] Full description (4000 chars max)
  - [ ] Screenshots (Phone, 7" Tablet, 10" Tablet)
  - [ ] Feature graphic (1024x500px)
  - [ ] Privacy Policy URL
  - [ ] Terms of Service URL
- [ ] Beta Testing Track
  - [ ] Internal testing (team members)
  - [ ] Closed testing (limited users)
  - [ ] Open testing (public beta)
- [ ] Release Notes
  - [ ] English version
  - [ ] Russian version
  - [ ] Ukrainian version
- [ ] Pre-launch Report Review
  - [ ] Address all blocking issues
  - [ ] Review compatibility warnings
  - [ ] Check accessibility scan results

**Reference:** `SPEC/35_release_checklist.md`

---

## 🐛 Known Issues & Technical Debt

### High Priority Issues
- None currently blocking release

### Medium Priority Issues
- **FTP PASV Mode:** Occasional timeouts on some servers (active mode fallback works)
- **Test Coverage:** Currently <50%, need >80% for confidence

### Low Priority Issues
- **DataStore Migration:** Still using SharedPreferences (works fine, not urgent)

---

## 🎯 Next Steps (Week by Week)

### Week 1-2 (Jan 9-22, 2026)
**Focus: Testing Foundation**
1. Implement ViewModel unit tests (5 major ViewModels)
2. Add Repository tests with mocking
3. Complete UseCase tests (8 use cases)
4. Set up test coverage reporting
5. Target: Reach 60% coverage

### Week 3-4 (Jan 23 - Feb 5, 2026)
**Focus: Testing**
1. Add FileOperationStrategy tests
2. Complete Manager tests
3. Target: Reach 75% coverage
4. Begin manual device testing

### Week 5 (Feb 6-12, 2026)
**Focus: QA & Polish**
1. Execute full manual QA checklist
2. Fix critical bugs discovered
3. Complete documentation
4. Prepare store assets
5. Target: 80%+ coverage, zero critical bugs

### Week 6 (Feb 13-19, 2026)
**Focus: Beta Testing**
1. Deploy to internal testing track
2. Collect beta feedback
3. Address high-priority issues
4. Performance optimization

### Week 7 (Feb 20-26, 2026)
**Focus: Release Preparation**
1. Final regression testing
2. Store listing review (all languages)
3. Generate final release build
4. Submit to Google Play Store

### Post-Release (March 2026+)
**Focus: Monitoring & v2.1 Planning**
1. Monitor crash reports
2. Address user feedback
3. Begin Epic 5 (Cloud Integration)
4. Plan advanced EPUB features (bookmarks, highlights)
5. Plan PDF merge/split/reorder UI

---

## 📚 Reference Documents

### Primary SPEC Files
- `SPEC/index.md` - Master specification index
- `SPEC/00_project_rules.md` - Development rules
- `SPEC/17_architecture_patterns.md` - Clean Architecture guide
- `SPEC/30_testing_strategy.md` - Testing guide
- `SPEC/35_release_checklist.md` - Pre-launch validation
- `SPEC/04_player_activity.md` - PlayerActivity specifications

### Code Reference
- `OLD_CODE_for_information_only/app_v2/` - Legacy code for reference ONLY
  - **DO NOT copy code directly**
  - Use as reference for feature understanding
  - Reimplement following current architecture

### Build Commands
```powershell
# Quick build
.\quick-build.ps1

# Debug build
.\debug-build.ps1

# Release build
.\build-with-version.ps1

# Run tests
.\gradlew test
.\gradlew connectedAndroidTest
```

---

## ⚠️ Important Notes

1. **DO NOT copy code from OLD_CODE_for_information_only/** - Always implement from SPEC following Clean Architecture
2. All "DEFERRED" features are non-blocking for initial release
3. Focus on testing to reach >80% coverage goal
4. Cloud integration (Epic 5) planned for v2.1
5. Target release: Late February 2026

---

## ✅ Completed Features (v2.0.0)

All Priority 2 missing features from app_v2 have been completed:
- EPUB UI Integration (WebView reader, chapter navigation, TOC)
- Slideshow Mode (auto-advance, countdown, pause/resume)
- In-Document Search (text/PDF/EPUB search with highlighting)
- Background Audio Playback (MediaSessionService, notification controls)
- PDF Tools UI (extract pages, export as images, rotate, delete)
- Translation Enhancements (OCR overlay, tap-to-translate, font size control)

---