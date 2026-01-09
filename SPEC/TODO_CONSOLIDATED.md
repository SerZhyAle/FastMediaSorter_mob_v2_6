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

### Priority 2: Missing Features from v1/app_v2
- [ ] EPUB UI Integration
- [ ] Slideshow Mode
- [ ] In-Document Search
- [ ] Background Audio Playback
- [ ] PDF Tools UI (Extract/Merge/Split)

### Priority 3: Cloud Integration (Post-Release)
- [ ] Google Drive OAuth Setup
- [ ] OneDrive Integration
- [ ] Dropbox Integration

---

## 🚧 Epic 5: Cloud Integration (DEFERRED)

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

## 🔧 Epic 6: Advanced Features

### 6.1 EPUB Reader UI Integration ⚠️ HIGH PRIORITY

**Backend:** ✅ Complete (EpubReaderManager - 336 lines)  
**UI Status:** ❌ Not Integrated

**Missing Components:**
- [ ] Add WebView to PlayerActivity layout (`epubWebView`)
- [ ] Create EpubViewerHelper/Manager class
  - [ ] WebView initialization with JavaScript enabled
  - [ ] HTML content rendering with CSS theming
  - [ ] Image resource serving from EPUB ZIP
  - [ ] Scroll position tracking
- [ ] Add EPUB Controls Layout (`epubControlsLayout`)
  - [ ] btnEpubPrevChapter - Previous chapter button
  - [ ] btnEpubNextChapter - Next chapter button
  - [ ] tvEpubChapterIndicator - Chapter counter (e.g., "5/12")
  - [ ] btnEpubToc - Table of contents button
  - [ ] btnEpubFontSizeDecrease - Decrease font button
  - [ ] btnEpubFontSizeIncrease - Increase font button
  - [ ] btnExitEpubFullscreen - Exit fullscreen button
- [ ] Wire up EPUB operations
  - [ ] Chapter navigation (prev/next with swipe gestures)
  - [ ] Font size control (8-48px range)
  - [ ] Font family selection (Serif/Sans/Mono)
  - [ ] Table of Contents dialog
  - [ ] Fullscreen mode toggle
  - [ ] Reading position save/restore
- [ ] Integrate with PlayerViewModel
  - [ ] Add EPUB viewing state
  - [ ] Handle chapter navigation events
  - [ ] Manage font preferences
- [ ] Add string resources for EPUB UI
  - [ ] Chapter indicators
  - [ ] Font size labels
  - [ ] Table of contents headers
  - [ ] Error messages

**Reference:** `OLD_CODE_for_information_only/app_v2/.../EpubViewerManager.kt` (1600 lines)

### 6.2 In-Document Search

- [ ] Create SearchHelper base class
- [ ] Implement PdfSearchHelper
  - [ ] Text extraction from PDF pages
  - [ ] Search query processing
  - [ ] Result highlighting
- [ ] Implement EpubSearchHelper
  - [ ] Cross-chapter text search
  - [ ] Result navigation
  - [ ] Chapter context display
- [ ] Implement TextSearchHelper
  - [ ] Efficient text file search
  - [ ] Multi-line pattern matching
  - [ ] Result highlighting in TextView
- [ ] Create Search UI Panel
  - [ ] Search input field
  - [ ] Result counter (e.g., "3/15")
  - [ ] Prev/Next navigation buttons
  - [ ] Clear search button
  - [ ] Case sensitivity toggle
- [ ] Wire up search buttons in PlayerActivity
  - [ ] btnSearchTextCmd
  - [ ] btnSearchPdfCmd
  - [ ] btnSearchEpubCmd

**Current Status:** Buttons exist but functionality not implemented

### 6.3 PDF Tools UI - Phase 2

**Backend:** ✅ Complete (PdfEditManager - 383 lines)  
**Current UI:** Rotate Left/Right, Delete pages implemented  
**Missing UI:**

- [ ] Wire up Extract Pages button in PdfToolsDialog
  - [ ] Show file picker for output location
  - [ ] Progress indicator during extraction
  - [ ] Success/error feedback
- [ ] Add Merge PDFs feature
  - [ ] Create MergePdfsDialog
  - [ ] Multi-file picker for source PDFs
  - [ ] Page order configuration
  - [ ] Output file naming
- [ ] Add Split PDF feature
  - [ ] Create SplitPdfDialog
  - [ ] Split options: by page count, by ranges, individual pages
  - [ ] Output directory selection
  - [ ] Filename pattern configuration
- [ ] Add Reorder Pages feature
  - [ ] Drag-and-drop page reordering in grid
  - [ ] Visual feedback during drag
  - [ ] Save reordered PDF
- [ ] Add Save Modified PDF feature
  - [ ] File picker for save location
  - [ ] Filename suggestion
  - [ ] Overwrite confirmation

### 6.4 Slideshow Mode ⚠️ MISSING FROM CURRENT APP

**Status:** Was implemented in app_v2, missing in current app

**Features to Implement:**
- [ ] Add Slideshow controls to PlayerActivity
  - [ ] Play/Pause button
  - [ ] Interval selector (5s/10s/15s/30s/60s)
  - [ ] Random shuffle toggle
  - [ ] Skip videos checkbox
- [ ] Create SlideshowManager class
  - [ ] Timer-based auto-advance
  - [ ] Circular progress indicator
  - [ ] State management (playing/paused)
  - [ ] File type filtering
- [ ] Add Slideshow settings
  - [ ] Default interval preference
  - [ ] Auto-start option
  - [ ] Transition effects (optional)
- [ ] Wire up keyboard/gesture controls
  - [ ] Spacebar: Pause/Resume
  - [ ] Middle touch zone: Pause/Resume
- [ ] Add string resources and icons
  - [ ] Slideshow controls labels
  - [ ] Interval options
  - [ ] Status messages

**Reference:** Check `SPEC/04_player_activity.md` for slideshow specifications

### 6.5 Background Audio Playback (Low Priority)

**Status:** Not implemented in current app

- [ ] Create MediaPlaybackService extending MediaSessionService
  - [ ] Media notification with controls
  - [ ] Lock screen controls
  - [ ] Bluetooth/headphone controls
- [ ] Implement AudioFocusManager
  - [ ] Handle phone calls
  - [ ] Handle other app audio
  - [ ] Duck on notifications
- [ ] Add Service binding to PlayerActivity
  - [ ] Seamless transition to background
  - [ ] Restore playback position
  - [ ] Handle service lifecycle
- [ ] Add background playback settings
  - [ ] Enable/disable toggle
  - [ ] Battery optimization warning
- [ ] Add notification controls
  - [ ] Play/Pause
  - [ ] Previous/Next track
  - [ ] Album art display

**Note:** Low demand feature, can be deferred to v2.1

### 6.6 Translation Enhancements (Optional)

- [ ] Google Lens-style overlay for image translation
  - [ ] Transparent overlay with text boxes
  - [ ] Tap box to translate
  - [ ] Copy translated text
- [ ] Translation settings dialog
  - [ ] Source language selection
  - [ ] Target language selection
  - [ ] Font size control
  - [ ] Persistent settings

---

## 🧪 Epic 8: Testing & Release Engineering

### 8.1 Automated Testing ⚠️ CRITICAL

**Current:** <50% coverage  
**Goal:** >80% coverage on domain layer

**Unit Tests Needed:**
- [ ] ViewModel Tests
  - [x] BrowseViewModel
  - [x] PlayerViewModel  
  - [ ] AddResourceViewModel
  - [ ] EditResourceViewModel
  - [ ] SettingsViewModel
- [ ] Repository Tests
  - [ ] ResourceRepositoryImpl
  - [ ] MediaFileRepositoryImpl
  - [ ] SettingsRepositoryImpl
  - [ ] PlaybackPositionRepositoryImpl
- [ ] UseCase Tests
  - [ ] AddResourceUseCase (exists)
  - [ ] GetResourcesUseCase (exists)
  - [ ] UpdateResourceUseCase (exists)
  - [ ] DeleteResourceUseCase (exists)
  - [ ] GetMediaFilesUseCase
  - [ ] CopyFileUseCase
  - [ ] MoveFileUseCase
  - [ ] DeleteFileUseCase
- [ ] FileOperationStrategy Tests
  - [ ] LocalFileStrategy
  - [ ] SmbFileStrategy
  - [ ] SftpFileStrategy
  - [ ] FtpFileStrategy
- [ ] Manager Tests
  - [ ] PdfEditManager
  - [ ] PdfToolsManager
  - [ ] EpubReaderManager
  - [ ] TranslationManager
  - [ ] OcrManager

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
- **EPUB UI Missing:** Backend complete but no UI integration

### Low Priority Issues
- **DataStore Migration:** Still using SharedPreferences (works fine, not urgent)
- **Audio Background Playback:** Not implemented (low demand)
- **Slideshow Mode:** Missing from current app (was in app_v2)

---

## 📋 Missing Features from app_v2

These features were present in `OLD_CODE_for_information_only/app_v2` but are missing in the current app:

### High Priority (Should be implemented)
1. **EPUB Viewer UI** (backend exists, no UI)
   - WebView-based reader
   - Chapter navigation
   - Font controls
   - Table of contents
   - Reference: `app_v2/.../EpubViewerManager.kt` (1600 lines)

2. **Slideshow Mode**
   - Auto-advance timer
   - Interval selection
   - Random shuffle
   - Progress indicator
   - Reference: Check app_v2 PlayerActivity

3. **In-Document Search**
   - PDF text search
   - EPUB cross-chapter search
   - Text file search
   - Result navigation

### Medium Priority (Nice to have)
4. **Advanced PDF UI**
   - Extract pages (backend ready)
   - Merge PDFs (backend ready)
   - Split PDF (backend ready)
   - Reorder pages (backend ready)

5. **Translation Overlay**
   - Google Lens-style boxes
   - Tap to translate
   - Visual overlay

### Low Priority (Can defer)
6. **Background Audio Service**
   - MediaSessionService
   - Lock screen controls
   - Notification controls

7. **Advanced EPUB Features**
   - Bookmarks
   - Highlights
   - Reading statistics
   - Export notes

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
**Focus: EPUB UI & Testing**
1. Integrate EPUB UI into PlayerActivity
2. Add FileOperationStrategy tests
3. Complete Manager tests
4. Target: Reach 75% coverage
5. Begin manual device testing

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
4. Plan Slideshow Mode implementation
5. Plan In-Document Search implementation

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
3. Focus on testing first, then missing high-priority features
4. EPUB UI integration is highest priority after testing
5. Cloud integration (Epic 5) planned for v2.1
6. Target release: Late February 2026

---

**Last Review:** January 9, 2026  
**Next Review:** January 16, 2026
