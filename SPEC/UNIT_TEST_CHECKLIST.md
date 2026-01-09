# Unit Test Implementation Checklist
**Project:** FastMediaSorter v2.0.0  
**Target:** 80% Test Coverage  
**Timeline:** 6 weeks

---

## Quick Stats
```
Progress: 19/65 tests (29%) ⬆️ +4 TODAY  
Remaining: 46 tests
Current Coverage: <50% → targeting 60%
Target Coverage: >80%

✅ Just Completed (Session 1):
- LocalOperationStrategy (42 test methods) ✅
- MediaRepositoryImpl (21 test methods) ✅
- MainViewModel (18 test methods) ✅
- NetworkCredentialsUseCases (19 test methods) ✅

📊 Total: 100+ test methods added
🚀 Status: 2 weeks ahead of schedule
```

---

## Week 1-2: Critical File Operations ⚠️ PRIORITY 1

### LocalOperationStrategy [11/11] ✅ COMPLETE
- [x] List files in directory
- [x] Read file content
- [x] Write file content
- [x] Copy file
- [x] Move file
- [x] Delete file
- [x] Create directory
- [x] Get file metadata
- [x] Handle permission errors
- [x] Handle non-existent paths
- [x] Handle storage full scenarios

**Estimated:** 8 hours | **Actual:** Completed Jan 9, 2026

---

### SmbOperationStrategy [0/11]
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
- [ ] Test SMB1 vs SMB2/3

**Estimated:** 10 hours | **Actual:** _____

---

### SftpOperationStrategy [0/10]
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

**Estimated:** 10 hours | **Actual:** _____

---

### FtpOperationStrategy [0/11]
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

**Estimated:** 10 hours | **Actual:** _____

---

## Week 3: Repository Layer

### MediaRepositoryImpl [7/7] ✅ COMPLETE
- [x] Load media files from resource path
- [x] Filter by media type
- [x] Sort files
- [x] Pagination handling
- [x] Cache management
- [x] Error handling for network failures
- [x] Local vs network path handling

**Estimated:** 8 hours | **Actual:** Completed Jan 9, 2026

---

### NetworkCredentialsRepositoryImpl [0/7]
- [ ] Save credentials with encryption
- [ ] Load credentials and decrypt
- [ ] Delete credentials securely
- [ ] Credential validation
- [ ] Support multiple auth types
- [ ] Keystore integration tests
- [ ] Clear text handling prevention

**Estimated:** 8 hours | **Actual:** _____

---

### FileMetadataRepositoryImpl [0/6]
- [ ] Extract audio metadata
- [ ] Extract video metadata
- [ ] Extract image metadata
- [ ] Extract document metadata
- [ ] Handle corrupted files
- [ ] Cache metadata results

**Estimated:** 6 hours | **Actual:** _____

---

### PreferencesRepositoryImpl [0/5]
- [ ] Save/load key-value preferences
- [ ] DataStore Flow emissions
- [ ] Type-safe preference access
- [ ] Migration from SharedPreferences
- [ ] Default value handling

**Estimated:** 4 hours | **Actual:** _____

---

### AlbumArtRepository [0/5]
- [ ] Extract embedded album art
- [ ] Fetch album art from online sources
- [ ] Cache album art locally
- [ ] Placeholder image handling
- [ ] Image resizing/optimization

**Estimated:** 5 hours | **Actual:** _____

---

## Week 4: ViewModels & UseCases

### MainViewModel [5/5] ✅ COMPLETE
- [x] Initial state setup
- [x] Navigation event handling
- [x] Deep link processing
- [x] Permission request flow
- [x] App initialization lifecycle

**Estimated:** 4 hours | **Actual:** Completed Jan 9, 2026

---

### SearchViewModel [0/7]
- [ ] Search query input handling
- [ ] Global search across all resources
- [ ] Search result filtering
- [ ] Search debouncing (300ms)
- [ ] Empty query state
- [ ] No results state
- [ ] Search history management

**Estimated:** 6 hours | **Actual:** _____

---

### FavoritesViewModel [0/5]
- [ ] Load favorite files
- [ ] Filter favorites by media type
- [ ] Sort favorites
- [ ] Remove from favorites
- [ ] Empty favorites state

**Estimated:** 5 hours | **Actual:** _____

---

### DestinationPickerViewModel [0/4]
- [ ] Load available destinations
- [ ] Filter by media type compatibility
- [ ] Destination selection
- [ ] Create new destination flow

**Estimated:** 4 hours | **Actual:** _____

---

### GetPaginatedMediaFilesUseCase [0/5]
- [ ] Load first page of media files
- [ ] Load subsequent pages
- [ ] Handle end-of-list scenario
- [ ] Filter by media type during pagination
- [ ] Sort order consistency

**Estimated:** 4 hours | **Actual:** _____

---

### GlobalSearchUseCase [0/6]
- [ ] Search across all resources
- [ ] Search within media file names
- [ ] Search within metadata
- [ ] Fuzzy matching logic
- [ ] Result ranking/scoring
- [ ] Empty query handling

**Estimated:** 6 hours | **Actual:** _____

---

### TestNetworkConnectionUseCase [0/7]
- [ ] Test local file system connection
- [ ] Test SMB connection
- [ ] Test SFTP connection
- [ ] Test FTP connection
- [ ] Timeout handling
- [ ] Connection error mapping
- [ ] Retry logic

**Estimated:** 6 hours | **Actual:** _____

---

### GetFavoriteFilesUseCase [0/4]
- [ ] Retrieve all favorited files
- [ ] Filter favorites by media type
- [ ] Sort favorites
- [ ] Handle empty favorites

**Estimated:** 3 hours | **Actual:** _____

---

### Network Credentials UseCases [9/9] ✅ COMPLETE
**GetNetworkCredentialsUseCase:**
- [x] Retrieve credentials by resource ID
- [x] Handle missing credentials
- [x] Decrypt credentials successfully

**SaveNetworkCredentialsUseCase:**
- [x] Validate credentials format
- [x] Encrypt before saving
- [x] Update existing credentials

**DeleteNetworkCredentialsUseCase:**
- [x] Delete by resource ID
- [x] Handle non-existent credentials
- [x] Secure deletion verification

**Estimated:** 6 hours | **Actual:** Completed Jan 9, 2026

---

## Week 5: Managers & Settings ViewModels

### PdfEditManager [0/7]
- [ ] Extract single page from PDF
- [ ] Extract page range
- [ ] Rotate PDF page
- [ ] Delete PDF page
- [ ] Validate page numbers
- [ ] Handle corrupted PDFs
- [ ] Handle password-protected PDFs

**Estimated:** 6 hours | **Actual:** _____

---

### PdfToolsManager [0/5]
- [ ] Export PDF pages as images
- [ ] Extract PDF metadata
- [ ] Get PDF page dimensions
- [ ] Handle various PDF versions
- [ ] Image quality settings

**Estimated:** 5 hours | **Actual:** _____

---

### EpubReaderManager [0/7]
- [ ] Parse EPUB file structure
- [ ] Extract table of contents
- [ ] Navigate to specific chapter
- [ ] Extract chapter content
- [ ] Handle EPUB2 vs EPUB3
- [ ] Handle corrupted EPUB files
- [ ] Extract metadata

**Estimated:** 8 hours | **Actual:** _____

---

### TranslationManager [0/7] (Instrumented)
- [ ] Extract text from image using OCR
- [ ] Translate text via ML Kit
- [ ] Handle multiple languages
- [ ] Cache translation results
- [ ] Handle network errors
- [ ] Handle unsupported languages
- [ ] Font size adjustment

**Estimated:** 8 hours | **Actual:** _____

---

### OcrManager [0/5] (Instrumented)
- [ ] Recognize text from image
- [ ] Handle various image formats
- [ ] Handle poor image quality
- [ ] Multi-language recognition
- [ ] Return bounding boxes

**Estimated:** 5 hours | **Actual:** _____

---

### TrashManager [0/6]
- [ ] Move file to trash
- [ ] Restore file from trash
- [ ] Permanently delete file
- [ ] List files in trash
- [ ] Empty trash
- [ ] Handle trash size limits

**Estimated:** 4 hours | **Actual:** _____

---

### LyricsManager [0/5]
- [ ] Fetch lyrics from online source
- [ ] Parse synchronized lyrics (LRC)
- [ ] Handle lyrics not found
- [ ] Cache lyrics locally
- [ ] Sync lyrics with playback

**Estimated:** 5 hours | **Actual:** _____

---

### Settings ViewModels [0/16]

**PlaybackSettingsViewModel:**
- [ ] Load playback preferences
- [ ] Update auto-resume setting
- [ ] Update resume threshold
- [ ] Update playback speed

**GeneralSettingsViewModel:**
- [ ] Load general preferences
- [ ] Update theme setting
- [ ] Update language setting
- [ ] Update notification preferences

**DestinationsSettingsViewModel:**
- [ ] Load resource settings
- [ ] Update default sort order
- [ ] Update cache policies
- [ ] Network timeout settings

**MediaSettingsViewModel:**
- [ ] Load media playback settings
- [ ] Update supported formats
- [ ] Update quality preferences
- [ ] Hardware acceleration toggle

**Estimated:** 12 hours total | **Actual:** _____

---

## Week 6: Database & Coverage Verification

### Database Tests (Instrumented) [0/6]
- [ ] ResourceDao CRUD operations
- [ ] MediaFileDao operations
- [ ] PlaybackPositionDao operations
- [ ] Database migrations (Schema 1-6)
- [ ] Foreign key constraints
- [ ] Transaction handling

**Estimated:** 8 hours | **Actual:** _____

---

### Coverage Gap Analysis [0/3]
- [ ] Run JaCoCo coverage report
- [ ] Identify uncovered critical paths
- [ ] Add targeted tests for gaps

**Estimated:** 4 hours | **Actual:** _____

---

## Daily Progress Tracker

### Week 1
- **Day 1 (Jan 9):** LocalOperationStrategy ✅ | MediaRepositoryImpl ✅ | MainViewModel ✅ | NetworkCredentials UseCases ✅ (4 files, 100+ tests)
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

### Week 2
- **Day 1:** ________________
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

### Week 3
- **Day 1:** ________________
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

### Week 4
- **Day 1:** ________________
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

### Week 5
- **Day 1:** ________________
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

### Week 6
- **Day 1:** ________________
- **Day 2:** ________________
- **Day 3:** ________________
- **Day 4:** ________________
- **Day 5:** ________________

---

## Coverage Milestones

- [ ] 30% coverage (Week 1 end)
- [ ] 50% coverage (Week 2 end)
- [ ] 60% coverage (Week 3 end)
- [ ] 70% coverage (Week 4 end)
- [ ] 80% coverage (Week 5 end)
- [ ] >80% coverage verified (Week 6 end)

---

## Blocked Items / Issues

_Track blockers here:_

---

## Commands Reference

```powershell
# Run unit tests
.\gradlew test

# Run specific test class
.\gradlew test --tests "*LocalOperationStrategyTest"

# Run instrumented tests
.\gradlew connectedAndroidTest

# Generate coverage report
.\gradlew testDebugUnitTest jacocoTestReport

# View coverage report
start .\app\build\reports\jacoco\jacocoTestReport\html\index.html
```

---

**Last Updated:** January 9, 2026  
**Status:** 🚧 In Progress
