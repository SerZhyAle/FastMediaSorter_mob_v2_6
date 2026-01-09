# Missing Unit Tests Analysis
**Generated:** January 9, 2026  
**Project:** FastMediaSorter v2.0.0  
**Status:** Pre-Release Testing Phase

---

## Executive Summary

**Current Test Coverage:** <50%  
**Target Coverage:** >80%  
**Total Tests Needed:** ~50 test files  
**Critical Gap:** FileOperationStrategy layer (0% coverage)

### Coverage by Layer
| Layer | Completed | Missing | Coverage | Priority |
|-------|-----------|---------|----------|----------|
| Domain Models | 3 | 0 | 100% ✅ | - |
| ViewModels | 5 | 12 | 29% | HIGH |
| Repositories | 2 | 6 | 25% | HIGH |
| UseCases | 5 | 8 | 38% | MEDIUM |
| File Operations | 0 | 5 | 0% ⚠️ | **CRITICAL** |
| Managers | 0 | 12 | 0% | MEDIUM |

---

## 1. ViewModel Tests (12 Missing)

### 1.1 MainViewModel ⚠️ HIGH PRIORITY
**Location:** `ui/main/MainViewModel.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Initial state setup
- [ ] Navigation event handling
- [ ] Deep link processing
- [ ] Permission request flow
- [ ] App initialization lifecycle

**Dependencies to Mock:**
- Resource loading state
- Navigation events
- Permission status

**Estimated Effort:** 4 hours

---

### 1.2 SearchViewModel ⚠️ HIGH PRIORITY
**Location:** `ui/search/SearchViewModel.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Search query input handling
- [ ] Global search across all resources
- [ ] Search result filtering
- [ ] Search debouncing (300ms delay)
- [ ] Empty query state
- [ ] No results state
- [ ] Search history management

**Dependencies to Mock:**
- GlobalSearchUseCase
- SearchFilter model

**Estimated Effort:** 6 hours

---

### 1.3 FavoritesViewModel ⚠️ MEDIUM PRIORITY
**Location:** `ui/favorites/FavoritesViewModel.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Load favorite files from repository
- [ ] Filter favorites by media type
- [ ] Sort favorites (name, date, size)
- [ ] Remove from favorites
- [ ] Empty favorites state

**Dependencies to Mock:**
- GetFavoriteFilesUseCase
- MediaRepository

**Estimated Effort:** 5 hours

---

### 1.4 DestinationPickerViewModel ⚠️ MEDIUM PRIORITY
**Location:** `ui/browse/DestinationPickerViewModel.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Load available destinations
- [ ] Filter by media type compatibility
- [ ] Destination selection
- [ ] Create new destination flow

**Dependencies to Mock:**
- GetResourcesUseCase
- Resource filtering logic

**Estimated Effort:** 4 hours

---

### 1.5-1.7 Settings ViewModels ⚠️ MEDIUM PRIORITY

#### PlaybackSettingsViewModel
**Test Cases:**
- [ ] Load playback preferences
- [ ] Update auto-resume setting
- [ ] Update resume threshold
- [ ] Update playback speed
- [ ] Gesture control settings

**Estimated Effort:** 3 hours

#### GeneralSettingsViewModel
**Test Cases:**
- [ ] Load general preferences
- [ ] Update theme setting
- [ ] Update language setting
- [ ] Update notification preferences
- [ ] Cache management

**Estimated Effort:** 3 hours

#### DestinationsSettingsViewModel
**Test Cases:**
- [ ] Load resource settings
- [ ] Update default sort order
- [ ] Update cache policies
- [ ] Network timeout settings

**Estimated Effort:** 3 hours

#### MediaSettingsViewModel
**Test Cases:**
- [ ] Load media playback settings
- [ ] Update supported formats
- [ ] Update quality preferences
- [ ] Hardware acceleration toggle

**Estimated Effort:** 3 hours

---

### 1.8-1.11 Cloud Folder Picker ViewModels 🔵 LOW PRIORITY (v2.1)
**Status:** Deferred to cloud integration phase

- BaseCloudFolderPickerViewModel (abstract)
- GoogleDriveFolderPickerViewModel
- OneDriveFolderPickerViewModel
- DropboxFolderPickerViewModel

**Estimated Effort:** 8 hours (when cloud features are implemented)

---

## 2. Repository Tests (6 Missing)

### 2.1 MediaRepositoryImpl ⚠️ HIGH PRIORITY
**Location:** `data/repository/MediaRepositoryImpl.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Load media files from resource path
- [ ] Filter by media type (audio/video/image/document)
- [ ] Sort files (name, date, size)
- [ ] Pagination handling (loadMore)
- [ ] Cache management
- [ ] Error handling for network failures
- [ ] Local vs network path handling

**Dependencies to Mock:**
- FileOperationStrategy (Local, SMB, SFTP, FTP)
- MediaFileDao
- File metadata extraction

**Estimated Effort:** 8 hours

---

### 2.2 FileMetadataRepositoryImpl ⚠️ HIGH PRIORITY
**Location:** `data/repository/FileMetadataRepositoryImpl.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Extract audio metadata (title, artist, album, duration)
- [ ] Extract video metadata (resolution, codec, duration)
- [ ] Extract image metadata (dimensions, EXIF data)
- [ ] Extract document metadata (page count, author)
- [ ] Handle corrupted files gracefully
- [ ] Cache metadata results

**Dependencies to Mock:**
- MediaMetadataRetriever (Android)
- ExifInterface (Android)
- PDF/EPUB parsers

**Estimated Effort:** 6 hours

---

### 2.3 PreferencesRepositoryImpl ⚠️ MEDIUM PRIORITY
**Location:** `data/repository/PreferencesRepositoryImpl.kt`  
**Complexity:** Low

**Test Cases Needed:**
- [ ] Save/load key-value preferences
- [ ] DataStore Flow emissions
- [ ] Type-safe preference access
- [ ] Migration from SharedPreferences (if applicable)
- [ ] Default value handling

**Dependencies to Mock:**
- DataStore<Preferences>

**Estimated Effort:** 4 hours

---

### 2.4 NetworkCredentialsRepositoryImpl ⚠️ HIGH PRIORITY
**Location:** `data/repository/NetworkCredentialsRepositoryImpl.kt`  
**Complexity:** High (Security Critical)

**Test Cases Needed:**
- [ ] Save credentials with encryption
- [ ] Load credentials and decrypt
- [ ] Delete credentials securely
- [ ] Credential validation
- [ ] Support multiple auth types (password, key-based)
- [ ] Keystore integration tests
- [ ] Clear text handling prevention

**Dependencies to Mock:**
- Android Keystore
- EncryptedSharedPreferences
- NetworkCredentialsDao

**Estimated Effort:** 8 hours

---

### 2.5 AlbumArtRepository ⚠️ MEDIUM PRIORITY
**Location:** `data/coverart/AlbumArtRepository.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Extract embedded album art from audio files
- [ ] Fetch album art from online sources (Last.fm, MusicBrainz)
- [ ] Cache album art locally
- [ ] Placeholder image handling
- [ ] Image resizing/optimization

**Dependencies to Mock:**
- MediaMetadataRetriever
- HTTP client for online sources
- Image cache

**Estimated Effort:** 5 hours

---

### 2.6 StressTestRepositoryImpl 🔵 LOW PRIORITY
**Location:** `data/repository/debug/StressTestRepositoryImpl.kt`  
**Purpose:** Debug/testing only  
**Estimated Effort:** 2 hours

---

## 3. UseCase Tests (8 Missing)

### 3.1 GetPaginatedMediaFilesUseCase ⚠️ HIGH PRIORITY
**Location:** `domain/usecase/GetPaginatedMediaFilesUseCase.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Load first page of media files
- [ ] Load subsequent pages correctly
- [ ] Handle end-of-list scenario
- [ ] Filter by media type during pagination
- [ ] Sort order consistency across pages

**Dependencies to Mock:**
- MediaRepository

**Estimated Effort:** 4 hours

---

### 3.2 GetFavoriteFilesUseCase ⚠️ MEDIUM PRIORITY
**Location:** `domain/usecase/GetFavoriteFilesUseCase.kt`  
**Complexity:** Low

**Test Cases Needed:**
- [ ] Retrieve all favorited files
- [ ] Filter favorites by media type
- [ ] Sort favorites
- [ ] Handle empty favorites

**Dependencies to Mock:**
- MediaRepository or PreferencesRepository

**Estimated Effort:** 3 hours

---

### 3.3 GlobalSearchUseCase ⚠️ MEDIUM PRIORITY
**Location:** `domain/usecase/GlobalSearchUseCase.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Search across all resources
- [ ] Search within media file names
- [ ] Search within metadata (artist, album, tags)
- [ ] Fuzzy matching logic
- [ ] Result ranking/scoring
- [ ] Empty query handling

**Dependencies to Mock:**
- ResourceRepository
- MediaRepository
- Search algorithm

**Estimated Effort:** 6 hours

---

### 3.4-3.6 Network Credentials UseCases ⚠️ MEDIUM PRIORITY

**Test Cases for Each:**

#### GetNetworkCredentialsUseCase
- [ ] Retrieve credentials by resource ID
- [ ] Handle missing credentials
- [ ] Decrypt credentials successfully

#### SaveNetworkCredentialsUseCase
- [ ] Validate credentials format
- [ ] Encrypt before saving
- [ ] Update existing credentials
- [ ] Create new credentials

#### DeleteNetworkCredentialsUseCase
- [ ] Delete by resource ID
- [ ] Handle non-existent credentials
- [ ] Secure deletion verification

**Dependencies to Mock:**
- NetworkCredentialsRepository

**Total Estimated Effort:** 6 hours (2 hours each)

---

### 3.7 TestNetworkConnectionUseCase ⚠️ HIGH PRIORITY
**Location:** `domain/usecase/TestNetworkConnectionUseCase.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Test local file system connection
- [ ] Test SMB connection with valid/invalid credentials
- [ ] Test SFTP connection with password/key auth
- [ ] Test FTP connection (active/passive modes)
- [ ] Timeout handling
- [ ] Connection error message mapping
- [ ] Retry logic

**Dependencies to Mock:**
- FileOperationStrategy implementations
- Network connectivity

**Estimated Effort:** 6 hours

---

### 3.8 GenerateStressDataUseCase 🔵 LOW PRIORITY
**Purpose:** Debug/testing only  
**Estimated Effort:** 2 hours

---

## 4. FileOperationStrategy Tests ⚠️ **CRITICAL - ALL MISSING**

### 4.1 LocalOperationStrategy ⚠️ CRITICAL
**Location:** `data/operation/LocalOperationStrategy.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] List files in directory
- [ ] Read file content
- [ ] Write file content
- [ ] Copy file
- [ ] Move file
- [ ] Delete file
- [ ] Create directory
- [ ] Get file metadata (size, modified date)
- [ ] Handle permission errors
- [ ] Handle non-existent paths
- [ ] Handle storage full scenarios

**Dependencies to Mock:**
- File I/O (use temporary test directories)
- Android Storage Access Framework (if needed)

**Estimated Effort:** 8 hours

---

### 4.2 SmbOperationStrategy ⚠️ CRITICAL
**Location:** `data/operation/SmbOperationStrategy.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Connect with username/password
- [ ] Connect with anonymous access
- [ ] Connect to workgroup share
- [ ] Connect to domain share
- [ ] List files in SMB share
- [ ] Download file from SMB
- [ ] Upload file to SMB
- [ ] Handle authentication failures
- [ ] Handle connection timeouts
- [ ] Handle SMB protocol errors
- [ ] Test SMB1 vs SMB2/3 compatibility

**Dependencies to Mock:**
- jCIFS-NG library (SmbFile)
- Network socket connections

**Estimated Effort:** 10 hours

---

### 4.3 SftpOperationStrategy ⚠️ CRITICAL
**Location:** `data/operation/SftpOperationStrategy.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Connect with password authentication
- [ ] Connect with SSH key authentication
- [ ] Handle passphrase-protected keys
- [ ] List files in SFTP directory
- [ ] Download file via SFTP
- [ ] Upload file via SFTP
- [ ] Handle authentication failures
- [ ] Handle connection timeouts
- [ ] Handle SSH protocol errors
- [ ] Test non-standard ports

**Dependencies to Mock:**
- JSch library (Session, ChannelSftp)
- SSH connections

**Estimated Effort:** 10 hours

---

### 4.4 FtpOperationStrategy ⚠️ CRITICAL
**Location:** `data/operation/FtpOperationStrategy.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Connect in active mode
- [ ] Connect in passive mode (PASV)
- [ ] Connect with explicit TLS (FTPS)
- [ ] Connect with implicit TLS
- [ ] List files in FTP directory
- [ ] Download file via FTP
- [ ] Upload file via FTP
- [ ] Handle authentication failures
- [ ] Handle connection timeouts
- [ ] Handle firewall/NAT issues
- [ ] Test FTP vs FTPS

**Dependencies to Mock:**
- Apache Commons Net (FTPClient)
- Network connections

**Estimated Effort:** 10 hours

---

### 4.5 CloudOperationStrategy 🔵 LOW PRIORITY (v2.1)
**Location:** `data/operation/CloudOperationStrategy.kt`  
**Purpose:** Google Drive, OneDrive, Dropbox integration  
**Status:** Deferred to v2.1

**Estimated Effort:** 12 hours (when implemented)

---

## 5. Manager Tests (12 Missing)

### 5.1 PdfEditManager ⚠️ MEDIUM PRIORITY
**Location:** `pdf/PdfEditManager.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Extract single page from PDF
- [ ] Extract page range from PDF
- [ ] Rotate PDF page
- [ ] Delete PDF page
- [ ] Validate page numbers
- [ ] Handle corrupted PDFs
- [ ] Handle password-protected PDFs

**Dependencies to Mock:**
- PDFBox or iText library
- File I/O

**Test Type:** Unit tests possible with PDF library mocks  
**Estimated Effort:** 6 hours

---

### 5.2 PdfToolsManager ⚠️ MEDIUM PRIORITY
**Location:** `pdf/PdfToolsManager.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Export PDF pages as images (PNG/JPG)
- [ ] Extract PDF metadata (title, author, page count)
- [ ] Get PDF page dimensions
- [ ] Handle various PDF versions
- [ ] Image quality/resolution settings

**Dependencies to Mock:**
- PDFBox or iText library
- Image export utilities

**Test Type:** Unit tests with PDF mocks  
**Estimated Effort:** 5 hours

---

### 5.3 EpubReaderManager ⚠️ MEDIUM PRIORITY
**Location:** `epub/EpubReaderManager.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Parse EPUB file structure
- [ ] Extract table of contents (TOC)
- [ ] Navigate to specific chapter
- [ ] Extract chapter content (HTML)
- [ ] Handle EPUB2 vs EPUB3 formats
- [ ] Handle corrupted EPUB files
- [ ] Extract metadata (title, author, cover)

**Dependencies to Mock:**
- EPUBLib or similar EPUB parser
- ZIP file handling

**Test Type:** Unit tests with EPUB parser mocks  
**Estimated Effort:** 8 hours

---

### 5.4 TranslationManager ⚠️ HIGH PRIORITY
**Location:** `translation/TranslationManager.kt`  
**Complexity:** High

**Test Cases Needed:**
- [ ] Extract text from image using OCR
- [ ] Translate text via Google ML Kit
- [ ] Handle multiple languages
- [ ] Cache translation results
- [ ] Handle network errors during translation
- [ ] Handle unsupported languages
- [ ] Font size adjustment for overlay

**Dependencies to Mock:**
- Google ML Kit Text Recognition
- Google ML Kit Translation
- Network requests

**Test Type:** Instrumented tests (ML Kit requires Android runtime)  
**Estimated Effort:** 8 hours (mostly instrumented)

---

### 5.5 OcrManager ⚠️ MEDIUM PRIORITY
**Location:** `ocr/OcrManager.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Recognize text from image
- [ ] Handle various image formats
- [ ] Handle poor image quality
- [ ] Multi-language text recognition
- [ ] Return bounding boxes for detected text

**Dependencies to Mock:**
- Google ML Kit Text Recognition

**Test Type:** Instrumented tests  
**Estimated Effort:** 5 hours

---

### 5.6 LyricsManager ⚠️ LOW PRIORITY
**Location:** `lyrics/LyricsManager.kt`  
**Complexity:** Medium

**Test Cases Needed:**
- [ ] Fetch lyrics from online source
- [ ] Parse synchronized lyrics (LRC format)
- [ ] Handle lyrics not found
- [ ] Cache lyrics locally
- [ ] Sync lyrics with audio playback

**Dependencies to Mock:**
- HTTP client
- LRC parser

**Test Type:** Unit tests with network mocks  
**Estimated Effort:** 5 hours

---

### 5.7 TrashManager ⚠️ MEDIUM PRIORITY
**Location:** `data/operation/TrashManager.kt`  
**Complexity:** Low

**Test Cases Needed:**
- [ ] Move file to trash
- [ ] Restore file from trash
- [ ] Permanently delete file
- [ ] List files in trash
- [ ] Empty trash
- [ ] Handle trash size limits

**Dependencies to Mock:**
- File operations

**Test Type:** Unit tests  
**Estimated Effort:** 4 hours

---

### 5.8 GoogleDriveCredentialsManager ⚠️ LOW PRIORITY (v2.1)
**Location:** `data/cloud/helpers/GoogleDriveCredentialsManager.kt`  
**Status:** Deferred to cloud integration

**Estimated Effort:** 5 hours

---

### 5.9-5.12 Low Priority Managers 🔵

- **VideoPlayerManager** - Requires ExoPlayer integration testing
- **AudioPlayerManager** - Requires ExoPlayer integration testing
- **TesseractManager** - Requires Tesseract OCR native library
- **TouchZoneGestureManager** - UI gesture handling, low test priority

**Total Estimated Effort:** 12 hours (if needed)

---

## 6. Instrumented Tests Needed

**Location:** `app/src/androidTest/java/`

### 6.1 Database Tests ⚠️ HIGH PRIORITY
- [ ] ResourceDao CRUD operations
- [ ] MediaFileDao operations
- [ ] PlaybackPositionDao operations
- [ ] Database migrations (Schema 1 → 6)
- [ ] Foreign key constraints
- [ ] Transaction handling

**Estimated Effort:** 8 hours

---

### 6.2 UI Tests 🔵 MEDIUM PRIORITY
- [ ] MainActivity navigation flow
- [ ] BrowseActivity file operations
- [ ] PlayerActivity media playback
- [ ] AddResourceActivity form validation
- [ ] Settings screens interaction

**Estimated Effort:** 12 hours

---

## 7. Implementation Priority Roadmap

### Week 1-2: Foundation (Critical Path)
**Goal:** Establish file operation testing framework

1. **LocalOperationStrategy** tests (8h) ⚠️
2. **SmbOperationStrategy** tests (10h) ⚠️
3. **SftpOperationStrategy** tests (10h) ⚠️
4. **FtpOperationStrategy** tests (10h) ⚠️
5. **MediaRepositoryImpl** tests (8h) ⚠️

**Total:** 46 hours (~1.5 weeks for 2 developers)

---

### Week 3: Repository Layer
**Goal:** Complete data layer testing

1. **NetworkCredentialsRepositoryImpl** tests (8h) ⚠️
2. **FileMetadataRepositoryImpl** tests (6h) ⚠️
3. **PreferencesRepositoryImpl** tests (4h)
4. **AlbumArtRepository** tests (5h)

**Total:** 23 hours

---

### Week 4: ViewModels & UseCases
**Goal:** Cover business logic and presentation

1. **MainViewModel** tests (4h)
2. **SearchViewModel** tests (6h)
3. **FavoritesViewModel** tests (5h)
4. **GetPaginatedMediaFilesUseCase** tests (4h)
5. **GlobalSearchUseCase** tests (6h)
6. **TestNetworkConnectionUseCase** tests (6h)
7. Network credential UseCases (6h)

**Total:** 37 hours

---

### Week 5: Managers & Polish
**Goal:** Feature-specific logic and edge cases

1. **PdfEditManager** tests (6h)
2. **PdfToolsManager** tests (5h)
3. **EpubReaderManager** tests (8h)
4. **TranslationManager** tests (8h - instrumented)
5. **OcrManager** tests (5h - instrumented)
6. **TrashManager** tests (4h)
7. Settings ViewModels (12h total)

**Total:** 48 hours

---

### Week 6: Database & Final Coverage
**Goal:** Instrumented tests and 80% coverage verification

1. Database DAO tests (8h)
2. Migration tests (4h)
3. Fill coverage gaps identified by JaCoCo
4. Integration test scenarios

**Total:** ~20 hours + gap filling

---

## 8. Testing Best Practices

### 8.1 Mocking Strategy
- **Use MockK** for Kotlin-friendly mocking
- **Use Turbine** for Flow testing
- **Use Robolectric** for Android framework dependencies (where possible)
- **Use MockWebServer** for network operation testing

### 8.2 Test Structure (AAA Pattern)
```kotlin
@Test
fun `test description in backticks`() {
    // Arrange
    val mockData = createTestData()
    every { mockRepository.getData() } returns flowOf(mockData)
    
    // Act
    val result = useCase.execute()
    
    // Assert
    result.test {
        assertEquals(expected, awaitItem())
        awaitComplete()
    }
}
```

### 8.3 Test Naming Convention
- Use backticks for descriptive test names
- Format: `given_when_then` or descriptive sentence
- Example: `when loading resources then emit success state`

### 8.4 Coverage Measurement
```powershell
# Generate coverage report
.\gradlew testDebugUnitTest jacocoTestReport

# View report
start .\app\build\reports\jacoco\jacocoTestReport\html\index.html
```

---

## 9. Total Effort Estimation

| Category | Tests | Hours | Priority |
|----------|-------|-------|----------|
| FileOperationStrategy | 4 | 38 | CRITICAL |
| Repositories | 6 | 31 | HIGH |
| ViewModels | 12 | 50 | HIGH |
| UseCases | 8 | 31 | MEDIUM |
| Managers | 12 | 59 | MEDIUM |
| Database (Instrumented) | - | 12 | HIGH |
| **Total** | **42+** | **~221** | - |

**Estimated Timeline:** 5-6 weeks with 2 developers (40h/week each)

---

## 10. Risk Assessment

### High Risk Areas
1. **FileOperationStrategy** - Zero coverage on critical functionality
2. **NetworkCredentialsRepository** - Security-critical code
3. **TranslationManager** - Complex ML Kit integration

### Medium Risk Areas
1. Settings ViewModels - User-facing configuration
2. Search functionality - Complex query logic
3. EPUB/PDF managers - File format parsing

### Mitigation Strategies
1. Start with critical path (file operations)
2. Use integration tests for complex scenarios
3. Manual QA for ML Kit and media playback features
4. Increase instrumented test coverage for Android-specific code

---

## 11. Acceptance Criteria

**Definition of Done for Testing Epic:**
- [ ] >80% code coverage on domain layer (ViewModels, UseCases, Repositories)
- [ ] >60% code coverage on data layer (FileOperationStrategy, Managers)
- [ ] All critical business logic covered by unit tests
- [ ] Database migrations tested in instrumented tests
- [ ] CI/CD pipeline runs tests on every commit
- [ ] JaCoCo report generated and reviewed
- [ ] Zero critical bugs found in manual QA after automated tests pass

---

## 12. Next Steps

1. **Immediate:** Start with LocalOperationStrategy tests (foundation)
2. **This Week:** Complete all FileOperationStrategy tests
3. **Next Week:** Repository layer tests
4. **Ongoing:** Track coverage improvements daily
5. **Review:** Weekly test coverage report review

---

## Appendix A: Test Dependencies

Add to `app/build.gradle.kts`:
```kotlin
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

androidTestImplementation("io.mockk:mockk-android:1.13.8")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```

---

**Document Status:** ✅ Complete  
**Last Updated:** January 9, 2026  
**Owner:** Development Team  
**Next Review:** Weekly during testing phase
