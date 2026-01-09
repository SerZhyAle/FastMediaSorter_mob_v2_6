# FastMediaSorter v2 - Developer TODO
**Last Updated:** January 9, 2026  
**For:** Developers, Unit Test Implementation  
**Status:** Week 1 - Unit Testing Phase

---

## 📋 Development Overview

This document tracks **developer implementation tasks**, primarily focused on unit test coverage. For manual QA testing, see [TESTING_TODO.md](TESTING_TODO.md).

**Timeline:** 6 weeks (Jan 9 - Feb 19, 2026)  
**Current Phase:** Week 1 - File Operation Tests  
**Goal:** >80% test coverage before release

---

## 🎯 Test Coverage Goals

**Current Status:** 28% (168 tests across 19 files)  
**Target:** 80%+ (estimated 250+ tests)

### Coverage by Layer
```
✅ Domain Models:      100% (3/3)   ← Complete
📈 ViewModels:          35% (6/17)  ← 11 missing
📈 Repositories:       37.5% (3/8)  ← 5 missing
🚀 UseCases:            61% (8/13)  ← 5 missing
⚡ File Operations:     20% (1/5)   ← 4 missing (CRITICAL)
⚠️ Managers:             0% (0/12)  ← 12 missing
```

---

## 📅 Week 1-2: File Operation Strategies (Jan 9-22)

### ✅ Completed (Jan 9)
- [x] **LocalOperationStrategy** (42 tests) - Foundation complete
- [x] **MediaRepositoryImpl** (21 tests) - Caching validated
- [x] **MainViewModel** (18 tests) - Navigation verified
- [x] **NetworkCredentials UseCases** (19 tests) - Security tested

### 🔥 Priority 1: Network File Operations (Jan 10-15)

#### SmbOperationStrategy (30+ tests)
**File:** `data/operation/SmbOperationStrategyTest.kt`  
**Estimated Effort:** 10 hours

**Test Cases:**
- [ ] Connect to SMB share (anonymous)
- [ ] Connect with username/password
- [ ] Connect with domain credentials
- [ ] List files in SMB directory
- [ ] Read file from SMB share
- [ ] Write file to SMB share
- [ ] Copy file within SMB
- [ ] Move file on SMB
- [ ] Delete file on SMB
- [ ] Create directory on SMB
- [ ] Get file metadata
- [ ] Test SMB1/SMB2/SMB3 compatibility
- [ ] Test workgroup vs domain
- [ ] Handle connection timeout
- [ ] Handle authentication failure
- [ ] Handle permission denied
- [ ] Test streaming large files (>100MB)
- [ ] Test connection pool cleanup (45s idle)

**Dependencies to Mock:**
- `com.hierynomus.smbj` (SMBJ library)
- `SmbClient` class
- Network connectivity

---

#### SftpOperationStrategy (25+ tests)
**File:** `data/operation/SftpOperationStrategyTest.kt`  
**Estimated Effort:** 10 hours

**Test Cases:**
- [ ] Connect with password authentication
- [ ] Connect with SSH key authentication
- [ ] Connect with passphrase-protected key
- [ ] List files in SFTP directory
- [ ] Read file from SFTP
- [ ] Write file to SFTP
- [ ] Copy file on SFTP server
- [ ] Move file on SFTP server
- [ ] Delete file on SFTP server
- [ ] Create directory
- [ ] Get file metadata
- [ ] Test non-standard port (2222)
- [ ] Handle connection timeout
- [ ] Handle authentication failure
- [ ] Handle key format errors
- [ ] Test file permissions
- [ ] Test symbolic link handling

**Dependencies to Mock:**
- `net.schmizz.sshj` (SSHJ library)
- `SftpClient` class
- SSH key loading

---

#### FtpOperationStrategy (28+ tests)
**File:** `data/operation/FtpOperationStrategyTest.kt`  
**Estimated Effort:** 10 hours

**Test Cases:**
- [ ] Connect with anonymous access
- [ ] Connect with username/password
- [ ] Connect with FTPS (explicit TLS)
- [ ] Connect with FTPS (implicit TLS)
- [ ] Test active mode connection
- [ ] Test passive mode connection
- [ ] List files in FTP directory
- [ ] Read file from FTP
- [ ] Write file to FTP
- [ ] Copy file on FTP server
- [ ] Move file on FTP server
- [ ] Delete file on FTP server
- [ ] Create directory
- [ ] Get file metadata
- [ ] Handle PASV timeout (known issue)
- [ ] Verify fallback to active mode
- [ ] Handle connection timeout
- [ ] Handle authentication failure
- [ ] Test ASCII vs BINARY mode
- [ ] Test transfer progress callback

**Dependencies to Mock:**
- `org.apache.commons.net.ftp` (Commons Net)
- `FtpClient` class
- Network connectivity

---

## 📅 Week 3-4: Repository & UseCase Tests (Jan 23 - Feb 5)

### Priority 2: Repository Tests (20+ hours)

#### FileMetadataRepositoryImpl (6 tests)
**Estimated:** 4 hours
- [ ] Extract audio metadata (artist, album, duration)
- [ ] Extract video metadata (resolution, codec)
- [ ] Extract image EXIF data
- [ ] Extract PDF metadata (pages, author)
- [ ] Handle corrupted files
- [ ] Cache metadata results

#### PreferencesRepositoryImpl (5 tests)
**Estimated:** 3 hours
- [ ] Save/load preferences
- [ ] DataStore Flow emissions
- [ ] Default value handling
- [ ] Type-safe access
- [ ] Migration from SharedPreferences

#### NetworkCredentialsRepositoryImpl (7 tests)
**Estimated:** 6 hours (Security Critical)
- [ ] Save credentials with encryption
- [ ] Load and decrypt credentials
- [ ] Delete credentials securely
- [ ] Credential validation
- [ ] Support multiple auth types
- [ ] Keystore integration
- [ ] Prevent clear text handling

#### AlbumArtRepository (5 tests)
**Estimated:** 4 hours
- [ ] Extract embedded album art
- [ ] Fetch from online sources (Last.fm)
- [ ] Cache album art locally
- [ ] Placeholder handling
- [ ] Image resizing/optimization

---

### Priority 3: UseCase Tests (15+ hours)

#### GetPaginatedMediaFilesUseCase (4 tests)
**Estimated:** 3 hours
- [ ] Load first page
- [ ] Load subsequent pages
- [ ] Handle end-of-list
- [ ] Maintain sort order across pages

#### GetFavoriteFilesUseCase (3 tests)
**Estimated:** 2 hours
- [ ] Retrieve all favorites
- [ ] Filter by media type
- [ ] Sort favorites

#### GlobalSearchUseCase (6 tests)
**Estimated:** 5 hours
- [ ] Search across all resources
- [ ] Search in file names
- [ ] Search in metadata
- [ ] Fuzzy matching
- [ ] Result ranking
- [ ] Empty query handling

#### TestNetworkConnectionUseCase (6 tests)
**Estimated:** 5 hours
- [ ] Test local connection
- [ ] Test SMB connection
- [ ] Test SFTP connection
- [ ] Test FTP connection
- [ ] Timeout handling
- [ ] Error message mapping

---

## 📅 Week 4-5: ViewModel Tests (Jan 30 - Feb 12)

### Priority 4: Missing ViewModels (40+ hours)

#### SearchViewModel (7 tests)
**Estimated:** 5 hours
- [ ] Search query input
- [ ] Global search execution
- [ ] Result filtering
- [ ] Search debouncing (300ms)
- [ ] Empty query state
- [ ] No results state
- [ ] Search history

#### FavoritesViewModel (5 tests)
**Estimated:** 4 hours
- [ ] Load favorites
- [ ] Filter by media type
- [ ] Sort favorites
- [ ] Remove from favorites
- [ ] Empty state

#### DestinationPickerViewModel (4 tests)
**Estimated:** 3 hours
- [ ] Load destinations
- [ ] Filter by compatibility
- [ ] Destination selection
- [ ] Create new destination

#### PlaybackSettingsViewModel (3 tests)
**Estimated:** 2 hours
- [ ] Load playback preferences
- [ ] Update auto-resume
- [ ] Update playback speed

#### GeneralSettingsViewModel (3 tests)
**Estimated:** 2 hours
- [ ] Load preferences
- [ ] Update theme
- [ ] Update language

#### DestinationsSettingsViewModel (3 tests)
**Estimated:** 2 hours
- [ ] Load resource settings
- [ ] Update sort order
- [ ] Update cache policies

#### MediaSettingsViewModel (3 tests)
**Estimated:** 2 hours
- [ ] Load media settings
- [ ] Update supported formats
- [ ] Update quality preferences

---

## ⚙️ Settings-General V1 Feature Parity

**Priority:** HIGH - Essential for V1→V2 migration  
**Reference:** [SETTINGS_COMPARISON_V1_V2.md](SETTINGS_COMPARISON_V1_V2.md)  
**File:** `app/app/src/main/java/com/sza/fastmediasorter/ui/settings/GeneralSettingsFragment.kt`

### Missing Features from V1 (6 items)

#### 1. Default Username/Password for Network Connections
**Priority:** HIGH
- [x] Add "Default Username" text input field
- [x] Add "Default Password" password input field
- [ ] Store in SharedPreferences (encrypted)
- [ ] Pre-fill when adding new SMB/FTP/SFTP resources


#### 2. User Guide Button
**Priority:** HIGH
- [ ] Add "User Guide" button to Settings-General
- [ ] Launch WelcomeActivity on button click
- [ ] Add analytics event for user guide relaunches

#### 3. Keep Screen On During Sorting
**Priority:** MEDIUM
- [ ] Verify "Prevent Sleep" switch matches V1 "Keep Screen On"
- [ ] Ensure flag applied during file operations
- [ ] Update labels if needed for clarity

#### 4. View Logs & Session Logs
**Priority:** MEDIUM
- [ ] Add "View Logs" button to Settings-General
- [ ] Add "View Session Logs" button to Settings-General
- [ ] Use dialog with scrollable ond opend to copy text content and button "copy to clipboard" to display logs. We must already have such dialog activity for many needs. For example to show detailed errors and test results


#### 5. Copy Logs to Clipboard
**Priority:** MEDIUM
- [ ] Add "Copy Logs to Clipboard" button
- [ ] Format logs for sharing (timestamp, level, message)
- [ ] Show toast confirmation after copy


#### 6. Request Media Access Button
**Priority:** MEDIUM
- [ ] Add "Request Media Access" button to Settings-General
- [ ] Trigger Android media permissions dialog
- [ ] Handle permission result callback


---

### Priority 5: Manager Tests

**Note:** Many managers require instrumented tests (Android dependencies).

#### PdfEditManager (4 tests)
**Estimated:** 4 hours
- [ ] Extract PDF pages
- [ ] Merge PDFs
- [ ] Delete PDF pages
- [ ] Rotate PDF pages

#### PdfToolsManager (3 tests)
**Estimated:** 3 hours
- [ ] Export PDF to images
- [ ] Extract PDF metadata
- [ ] Optimize PDF size

#### EpubReaderManager (3 tests)
**Estimated:** 3 hours
- [ ] Parse EPUB structure
- [ ] Extract TOC
- [ ] Read chapter content

#### TranslationManager (3 tests)
**Estimated:** 3 hours
- [ ] OCR text extraction
- [ ] Translate text
- [ ] Cache translations

#### OcrManager (3 tests)
**Estimated:** 3 hours
- [ ] Recognize text in image
- [ ] Handle multiple languages
- [ ] Error handling for poor quality

#### LyricsManager (3 tests)
**Estimated:** 3 hours
- [ ] Fetch lyrics from source
- [ ] Synchronize lyrics with playback
- [ ] Cache lyrics locally

#### TrashManager (3 tests)
**Estimated:** 2 hours
- [ ] Move file to trash
- [ ] Restore file from trash
- [ ] Permanently delete

---

## 🐛 Bug Fixes & Implementation Tasks

**Priority:** HIGH - User testing findings  
**Source:** [USER_TESTING_Progress_found_issues.md](USER_TESTING_Progress_found_issues.md)

### 1. Permission Request Message Incorrect
**Priority:** HIGH  
**File:** `app/src/main/AndroidManifest.xml`

**Issue:** Permission dialog shows "Allow FastMediaSorter to access music and audio on this device?" but app needs access to documents, texts, and all other file types.

**Tasks:**
- [ ] Research Android 13+ granular media permissions
- [ ] Determine if READ_MEDIA_AUDIO can be replaced with broader permission
- [ ] Update manifest permission declarations
- [ ] Test permission request on Android 13+, 14+
- [ ] Update permission rationale text in code
- [ ] Verify all file types (docs, PDFs, EPUBs, etc.) accessible after permission grant

### 2. Welcome Screen - Post-Setup Guidance
**Priority:** MEDIUM  
**File:** `app/src/main/java/com/sza/fastmediasorter/ui/welcome/WelcomeActivity.kt`

**Issue:** Last screen of Welcome should guide users to check Settings and choose file types first. Consider auto-opening Settings after Welcome completion.

**Tasks:**
- [ ] Add text to final Welcome screen mentioning next steps (Settings → File Types)
- [ ] Update strings.xml with guidance text (EN/RU/UK)
- [ ] Implement "Open Settings" button on final Welcome screen (optional auto-redirect)
- [ ] Add SharedPreferences flag to track first-time setup completion
- [ ] Launch GeneralSettingsFragment or DestinationsSettingsFragment after Welcome

### 3. Theme Switching Loop Bug in Settings-General
**Priority:** HIGH  
**File:** `app/src/main/java/com/sza/fastmediasorter/ui/settings/GeneralSettingsFragment.kt`

**Issue:** Changing color scheme from "System" to "Dark" causes infinite toggle loop between themes.

**Tasks:**
- [ ] Debug theme preference listener in GeneralSettingsFragment
- [ ] Check for recursive preference change callbacks
- [ ] Verify AppCompatDelegate.setDefaultNightMode() calls
- [ ] Add debounce/flag to prevent re-entrant theme changes
- [ ] Test all theme transitions (System↔Light, System↔Dark, Light↔Dark)

### 4. App Icon, Name, and Author Metadata
**Priority:** MEDIUM  
**Files:** `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `app/src/main/res/mipmap-*/`

**Issue:** App icon must match previous version. Application name should be "Fast Media Sorter" (no version suffix). Author should be "sza".

**Tasks:**
- [ ] Copy app icon from V1 to V2 (all densities: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- [ ] Update AndroidManifest.xml android:icon to match V1 icon resource
- [ ] Set android:label to "Fast Media Sorter" (remove version references)
- [ ] Update strings.xml app_name to "Fast Media Sorter" (EN/RU/UK)
- [ ] Set author metadata in build.gradle.kts or manifest (manifestPlaceholders or meta-data)
- [ ] Verify icon displays correctly on launcher (all Android versions)
- [ ] Verify app name in Settings → Apps matches "Fast Media Sorter"

---

## 🧪 Instrumented Tests (Deferred)

These require Android emulator/device:

### Database Tests
- [ ] ResourceDao CRUD operations
- [ ] FileMetadataDao operations
- [ ] Schema migration tests (1→6)
- [ ] Transaction tests

### UI Tests
- [ ] MainActivity navigation flow
- [ ] AddResourceActivity form validation
- [ ] BrowseActivity file selection
- [ ] PlayerActivity playback controls
- [ ] SettingsActivity preference changes

---

## 🛠️ Development Commands

### Run Tests
```powershell
# Run all unit tests
.\gradlew test

# Run specific test class
.\gradlew test --tests "*SmbOperationStrategyTest"

# Run tests in specific package
.\gradlew test --tests "com.sza.fastmediasorter.data.operation.*"

# Run with info logging
.\gradlew test --info
```

### Generate Coverage Report
```powershell
# Generate JaCoCo report
.\gradlew testDebugUnitTest jacocoTestReport

# View report
start .\app\build\reports\jacoco\jacocoTestReport\html\index.html
```

### Run Instrumented Tests
```powershell
# Run on connected device/emulator
.\gradlew connectedAndroidTest

# Run specific instrumented test
.\gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sza.fastmediasorter.DatabaseMigrationTest
```

---

## 📝 Testing Patterns & Guidelines

### File Operation Tests
```kotlin
// Use TemporaryFolder for isolation
@Rule @JvmField val tempFolder = TemporaryFolder()

// Test both success and failure paths
// Verify original file state after operations
// Test progress callbacks
```

### Repository Tests
```kotlin
// Mock all dependencies (DAOs, scanners)
// Test caching behavior explicitly
// Verify exact parameters in scanner calls
// Test all resource type branches
```

### ViewModel Tests
```kotlin
// Use InstantTaskExecutorRule for LiveData
@Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

// Use StandardTestDispatcher for coroutines
private val testDispatcher = StandardTestDispatcher()

// Test state transitions across time
// Verify Flow emissions update state correctly
```

### UseCase Tests
```kotlin
// Simple delegation testing
// Test integration scenarios
// Mock repository dependencies
// Verify result types (Success, Error)
```

---

## 🎯 Weekly Milestones

| Week | Goal | Tests | Coverage |
|------|------|-------|----------|
| **Week 1** ✅ | File Operations | +100 tests | 40-50% |
| **Week 2** | Repository Layer | +40 tests | 55-60% |
| **Week 3** | UseCases | +30 tests | 65-70% |
| **Week 4** | ViewModels | +40 tests | 75-80% |
| **Week 5** | Managers | +30 tests | 80-85% |
| **Week 6** | Polish & Instrumented | +20 tests | 85%+ |

---

## ✅ Definition of Done

A test file is considered complete when:

- [ ] All happy path scenarios tested
- [ ] Error conditions covered (exceptions, failures)
- [ ] Edge cases included (empty data, large datasets, null values)
- [ ] State transitions verified
- [ ] All dependencies properly mocked
- [ ] Tests follow AAA pattern (Arrange-Act-Assert)
- [ ] Test names are descriptive (use backticks)
- [ ] Tests are isolated (no shared state)
- [ ] Tests pass consistently
- [ ] Code review completed

---

## 🚀 Next Steps

**Immediate (Jan 10):**
1. Begin SmbOperationStrategy tests
2. Set up jCIFS-NG mocking
3. Target: 30+ tests in 10 hours

**This Week:**
1. Complete 3 network strategy tests (SMB, SFTP, FTP)
2. Aim for 50-60% overall coverage
3. Establish network mocking patterns

**Long Term:**
1. Maintain 2x velocity (ahead of schedule)
2. Reach 80%+ coverage by Week 5
3. Enable confident release in Week 6

---

**See Also:**
- [PROJECT_STATUS.md](PROJECT_STATUS.md) - Current development state
- [TESTING_TODO.md](TESTING_TODO.md) - Manual QA testing checklist
