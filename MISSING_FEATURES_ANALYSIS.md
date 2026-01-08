# Missing Features Analysis - FastMediaSorter v2.6

## Date: January 8, 2026
## Status: Comprehensive Activity-by-Activity Analysis

---

## ✅ COMPLETED ACTIVITIES (Match SPEC)

### 1. MainActivity ✓
**Status:** FULLY IMPLEMENTED
- ✅ All control buttons present (Exit, Add, Filter, Refresh, Settings, Toggle View, Favorites, Start Player)
- ✅ Resource type tabs (ALL, LOCAL, SMB, FTP_SFTP, CLOUD)
- ✅ RecyclerView with adaptive layout manager (Grid/List, phone/tablet)
- ✅ Empty state view with proper messaging
- ✅ Error state view with retry button
- ✅ Filter warning bar at bottom
- ✅ Scan progress overlay with progress bar
- ✅ Loading progress indicator
- ✅ Lifecycle management (onCreate, onResume, onPause, onDestroy)
- ✅ Proper state observation and event handling

**No action required** - MainActivity is complete according to SPEC.

---

## ⚠️ PARTIALLY IMPLEMENTED ACTIVITIES

### 2. WelcomeActivity
**Status:** BASIC STRUCTURE EXISTS, MISSING ENHANCED CONTENT

**Existing:**
- ✅ ViewPager2 with 4 pages
- ✅ Navigation buttons (Previous, Next, Skip, Finish)
- ✅ Page indicators
- ✅ Permission handling flow

**Missing:**
- ❌ **Page 1:** Version info display, enhanced app logo presentation
- ❌ **Page 2:** Visual resource types explanation with icons (Local/Network/SFTP/FTP/Cloud)
- ❌ **Page 3:** Touch zones demonstration (3x3 grid overlay) - CRITICAL FEATURE
- ❌ **Page 4:** Destinations feature overview with detailed instructions

**Implementation Required:**
1. Create `TouchZonesOverlayView` custom view for Page 3
2. Add version info TextView to Page 1
3. Create resource types visual grid for Page 2
4. Enhance Page 4 with destination setup instructions

**Priority:** MEDIUM (affects UX but app is functional without it)

---

### 3. PlayerActivity
**Status:** BASIC IMPLEMENTATION, MISSING MAJOR FEATURES

**Existing:**
- ✅ ViewPager2 for horizontal file navigation
- ✅ Basic image viewing
- ✅ Video playback with ExoPlayer
- ✅ Fullscreen mode

**Missing:**
- ❌ **Top Command Panel** (HorizontalScrollView) - CRITICAL
  - Missing ALL media-type-specific buttons
  - Text file buttons: btnSearchTextCmd, btnTranslateTextCmd, btnTextSettingsCmd
  - PDF buttons: btnSearchPdfCmd, btnEditPdf, btnTranslatePdfCmd, btnOcrPdfCmd, btnGoogleLensPdfCmd
  - EPUB buttons: btnSearchEpubCmd, btnTranslateEpubCmd, btnEpubTextSettingsCmd, btnOcrEpubCmd
  - Image buttons: btnTranslateImageCmd, btnImageTextSettingsCmd, btnOcrImageCmd, btnGoogleLensImageCmd
  - Audio buttons: btnLyricsCmd
  - Common buttons: btnRenameCmd, btnEditCmd, btnCopyTextCmd, btnEditTextCmd, btnUndoCmd
  - Right side buttons: btnDeleteCmd, btnFavorite, btnShareCmd, btnInfoCmd, btnFullscreenCmd, btnSlideshowCmd, btnPreviousCmd, btnNextCmd
  
- ❌ **Touch Zones System** (3x3 grid gesture detection) - CRITICAL
  - Top-left, top-center, top-right zones
  - Middle-left, center, middle-right zones
  - Bottom-left, bottom-center, bottom-right zones
  - Gesture handlers for tap/swipe/long-press in each zone
  
- ❌ **Media-specific features:**
  - OCR integration for images/PDFs/EPUBs
  - Translation services integration
  - Google Lens integration
  - PDF tools (rotate, reorder pages)
  - Text editing mode
  - Lyrics viewer for audio
  
**Implementation Required:**
1. Create `activity_player_unified.xml` layout with command panel
2. Add `TouchZonesGestureDetector` class
3. Implement button visibility logic based on media type
4. Add all 40+ command buttons with proper icons
5. Implement OCR, translation, Google Lens integrations
6. Add PDF tools dialog
7. Implement touch zone gesture handlers

**Priority:** HIGH (core player functionality missing)

**Estimated Effort:** 3-5 days

---

### 4. AddResourceActivity
**Status:** SIMPLIFIED VERSION, MISSING COMPLEX FORMS

**Existing:**
- ✅ Basic cards for Local/SMB/SFTP/FTP/Cloud
- ✅ Folder picker for local resources
- ✅ Basic network credentials dialog
- ✅ Toolbar with back navigation

**Missing:**
- ❌ **Copy Mode Detection:**
  - EXTRA_COPY_RESOURCE_ID handling
  - Pre-fill fields from existing resource
  - AddResourceHelper integration
  
- ❌ **Preselected Tab Routing:**
  - EXTRA_PRESELECTED_TAB intent extra
  - Auto-navigate to specific resource type form
  - Defer with binding.root.post {} pattern
  
- ❌ **Local Folder Section:**
  - btnScan button for automatic folder discovery
  - btnAddManually button
  - rvResourcesToAdd RecyclerView for batch add
  - cbLocalScanSubdirectories checkbox
  - cbLocalWorkWithAllFiles checkbox
  - cbLocalAddToDestinations checkbox
  - cbLocalReadOnlyMode checkbox
  - tilLocalPinCode TextInputLayout with 6-digit validation
  - btnAddToResources button
  
- ❌ **SMB Network Section:**
  - btnScanNetwork button for network discovery
  - tilSmbServer with IpAddressEditText (custom widget)
  - tilSmbUsername, tilSmbPassword fields
  - tilSmbDomain field (Windows domain support)
  - btnSmbTest button for connection testing
  - tilSmbShareName, tilSmbResourceName fields
  - tilSmbComment field
  - tilSmbPinCode field (6-digit)
  - Media type checkboxes grid (IMAGE/VIDEO/AUDIO/GIF/TEXT/PDF/EPUB)
  - cbSmbScanSubdirectories checkbox
  - cbSmbWorkWithAllFiles checkbox
  - cbSmbAddToDestinations checkbox
  - cbSmbReadOnlyMode checkbox
  - btnSmbSaveResource button
  
- ❌ **SFTP/FTP Section:**
  - rbSftp, rbFtp radio buttons
  - tilSftpServer, tilSftpPort (default 22), tilSftpUsername, tilSftpPassword fields
  - cbSftpUseSshKey checkbox
  - tilSftpSshKeyPath field with file picker
  - btnSftpTest button
  - All same options as SMB (media types, subdirectories, etc.)
  
- ❌ **Cloud Storage Section:**
  - Google Drive authentication button
  - Dropbox authentication button (OAuth flow)
  - OneDrive authentication button (OAuth flow)
  - Authentication status indicators
  - Folder picker navigation after auth
  - updateCloudStorageStatus() function
  
- ❌ **Configuration Change Handling:**
  - updateResourceTypeGridColumns() for orientation changes
  - Landscape: 2 columns, Portrait: 1 column

**Implementation Required:**
1. Add all detailed form layouts for each resource type
2. Implement copy mode with AddResourceHelper
3. Add preselected tab routing logic
4. Create IpAddressEditText custom widget
5. Implement network scan functionality
6. Add OAuth flows for Dropbox/OneDrive
7. Create Google Drive/Dropbox/OneDrive folder pickers
8. Add connection test functionality for SMB/SFTP
9. Implement batch resource adding
10. Add media type checkbox validation

**Priority:** HIGH (affects core functionality)

**Estimated Effort:** 5-7 days

---

### 5. BrowseActivity
**Status:** BASIC IMPLEMENTATION, NEEDS FULL SPEC VERIFICATION

**Existing:**
- ✅ Control buttons bar
- ✅ RecyclerView with grid layout
- ✅ Operations panel (Copy, Move, Rename, Delete, Undo, Share)
- ✅ Scroll buttons (FAB to top/bottom)
- ✅ Error and loading states

**Need to Verify Against SPEC (lines 650-1470 in 02_main_activities.md):**
- ⚠️ All 9 control buttons: btnBack, btnSort, btnFilter, btnRefresh, btnToggleView, btnSelectAll, btnDeselectAll, btnPlay, (+ btnStopScan)
- ⚠️ Sort dialog options (Name, Date, Size, Type, Extension)
- ⚠️ Filter dialog with media type selection
- ⚠️ Progress layout with file count and stop scan button
- ⚠️ Error state layout with specific error messages
- ⚠️ Path navigation breadcrumb (for network resources)
- ⚠️ Multi-select mode with selection count
- ⚠️ Context menu on long-press
- ⚠️ Keyboard navigation support

**Action Required:**
1. Read SPEC lines 650-1470 completely
2. Compare each UI element and feature
3. Create detailed checklist of missing items
4. Implement missing functionality

**Priority:** MEDIUM-HIGH

**Estimated Effort:** 2-3 days

---

### 6. SettingsActivity
**Status:** TAB STRUCTURE EXISTS, NEED FRAGMENT VERIFICATION

**Existing:**
- ✅ Toolbar with back navigation
- ✅ TabLayout with 5 tabs
- ✅ ViewPager2 with adapter
- ✅ Basic fragment structure

**Need to Verify:**
- ⚠️ **GeneralSettingsFragment:** All options from SPEC
  - Language selection (English/Russian/Ukrainian)
  - Theme (Light/Dark/System)
  - Display mode (Grid/List/Auto)
  - Grid columns slider (1-10)
  - File name display (Full/Truncated)
  - Show hidden files
  - Work with all files switch
  - Date format selection
  - Sync settings (enable, interval, sync now button)
  - Behavior settings (favorites, prevent sleep, confirm delete/move)
  - Cache management (size limit, auto management, clear cache)
  - App version info
  
- ⚠️ **MediaSettingsFragment:** 5 sub-tabs
  - Images: Thumbnail quality, load full res, auto-rotate, EXIF data, JPEG compression, cache strategy
  - Videos: Quality, hardware acceleration, auto-play, controls, seek increment, thumbnails, preview duration
  - Audio: Waveform display, style, color, background playback, audio focus
  - Documents: PDF page cache, render quality, text encoding
  - Other: RAR/ZIP handling, compression settings
  
- ⚠️ **PlaybackSettingsFragment:** Slideshow and player settings
  - Slideshow interval, shuffle, repeat
  - Touch zones configuration
  - Video playback settings
  - Auto-advance settings
  
- ⚠️ **DestinationsFragment:** Quick move/copy targets
  - List of configured destinations
  - Add/Edit/Delete destination
  - Default destination setting
  
- ⚠️ **NetworkSettingsFragment:** Network and cloud settings
  - Connection timeout
  - Retry attempts
  - Cache settings for network files
  - Offline mode
  - Cloud sync settings

**Action Required:**
1. Check each fragment implementation
2. Verify all PreferenceScreen entries exist
3. Ensure proper data binding to SharedPreferences/DataStore
4. Test settings persistence

**Priority:** MEDIUM

**Estimated Effort:** 3-4 days

---

### 7. EditResourceActivity
**Status:** UNKNOWN - NEEDS FULL ANALYSIS

**Need to Check:**
- ⚠️ All edit forms for each resource type
- ⚠️ Password management UI
- ⚠️ Destination configuration
- ⚠️ Resource type switching capability
- ⚠️ PIN code editing
- ⚠️ Media type selection
- ⚠️ Read-only mode toggle

**Action Required:**
1. Read EditResourceActivity implementation
2. Compare with SPEC section in 03_resource_management.md
3. Create detailed gap analysis
4. Implement missing features

**Priority:** MEDIUM

**Estimated Effort:** 2-3 days

---

## 📊 SUMMARY STATISTICS

### Completion Status:
- ✅ Fully Complete: 1/8 (12.5%) - MainActivity only
- ⚠️ Partially Complete: 6/8 (75%) - Needs enhancement
- ❌ Unknown/Unchecked: 1/8 (12.5%) - EditResourceActivity

### Priority Breakdown:
- 🔴 HIGH Priority: PlayerActivity, AddResourceActivity (core functionality)
- 🟡 MEDIUM-HIGH Priority: BrowseActivity
- 🟠 MEDIUM Priority: WelcomeActivity, SettingsActivity, EditResourceActivity

### Estimated Total Effort:
- PlayerActivity: 3-5 days
- AddResourceActivity: 5-7 days
- BrowseActivity: 2-3 days
- SettingsActivity: 3-4 days
- EditResourceActivity: 2-3 days
- WelcomeActivity: 1-2 days
- **TOTAL: 16-24 days** (3-5 weeks for single developer)

---

## 🎯 RECOMMENDED IMPLEMENTATION ORDER

### Phase 1: Core Functionality (Week 1-2)
1. **AddResourceActivity** - Critical for adding resources
   - Implement all network forms (SMB/SFTP/FTP)
   - Add cloud OAuth flows
   - Implement copy mode
   - Add all validation and testing features

2. **PlayerActivity** - Core media viewing
   - Add command panel with all buttons
   - Implement touch zones system
   - Add OCR/Translation integrations
   - Implement media-specific features

### Phase 2: Enhanced Features (Week 3)
3. **BrowseActivity** - File browsing enhancements
   - Complete SPEC verification
   - Add missing sort/filter options
   - Enhance multi-select mode
   - Add keyboard navigation

4. **EditResourceActivity** - Resource editing
   - Implement all edit forms
   - Add password management
   - Complete destination configuration

### Phase 3: Settings & Polish (Week 4-5)
5. **SettingsActivity** - Settings completion
   - Verify all fragments
   - Add missing preference options
   - Test settings persistence

6. **WelcomeActivity** - Onboarding enhancement
   - Add touch zones demonstration
   - Enhance page content
   - Add version info

---

## 📝 NOTES FOR IMPLEMENTATION

### Critical Custom Components Needed:
1. **IpAddressEditText** - Custom EditText for IP address input with auto-formatting
2. **TouchZonesOverlayView** - Custom View for displaying 3x3 grid with gesture zones
3. **TouchZonesGestureDetector** - Gesture detector for PlayerActivity touch zones
4. **ResourcePasswordManager** - PIN code verification and management
5. **AddResourceHelper** - Copy mode pre-fill logic
6. **Network scan utilities** - SMB/SFTP network discovery
7. **OAuth clients** - Dropbox, OneDrive authentication

### Integration Points:
- OCR service (ML Kit or Tesseract)
- Translation service (Google Translate API or ML Kit)
- Google Lens integration
- PDF manipulation library (PDFBox or similar)
- Cloud storage SDKs (Google Drive API, Dropbox SDK, OneDrive API)

### Testing Requirements:
- Test all network resource types (SMB, SFTP, FTP)
- Test OAuth flows for cloud storage
- Test touch zones in different orientations
- Test media-specific command buttons
- Verify all settings persistence
- Test copy mode functionality
- Verify keyboard navigation

---

## 🔍 NEXT STEPS

1. **Immediate:** Start with AddResourceActivity - highest impact
2. **Review:** Read full SPEC sections for PlayerActivity and BrowseActivity
3. **Plan:** Create detailed implementation tickets for each missing feature
4. **Implement:** Follow recommended phase order
5. **Test:** Comprehensive testing after each activity completion
6. **Document:** Update this analysis as features are completed

---

## ✅ COMPLETION CHECKLIST

Use this checklist to track progress:

### AddResourceActivity:
- [ ] Copy mode detection and AddResourceHelper
- [ ] Preselected tab routing
- [ ] Local folder section with all options
- [ ] SMB form with network scan and test
- [ ] SFTP/FTP form with SSH key support
- [ ] Cloud storage OAuth flows
- [ ] IpAddressEditText custom widget
- [ ] Media type checkboxes validation
- [ ] Connection testing functionality
- [ ] Batch resource adding

### PlayerActivity:
- [ ] Top command panel HorizontalScrollView
- [ ] All 40+ command buttons
- [ ] Touch zones gesture detector
- [ ] Media-type button visibility logic
- [ ] OCR integration
- [ ] Translation integration
- [ ] Google Lens integration
- [ ] PDF tools dialog
- [ ] Text editing mode
- [ ] Lyrics viewer

### BrowseActivity:
- [ ] Complete SPEC verification (lines 650-1470)
- [ ] All control buttons implemented
- [ ] Sort dialog with all options
- [ ] Filter dialog enhancements
- [ ] Path navigation breadcrumb
- [ ] Multi-select improvements
- [ ] Keyboard navigation

### SettingsActivity:
- [ ] GeneralSettingsFragment complete
- [ ] MediaSettingsFragment all sub-tabs
- [ ] PlaybackSettingsFragment
- [ ] DestinationsFragment
- [ ] NetworkSettingsFragment
- [ ] Settings persistence verified

### WelcomeActivity:
- [ ] Touch zones overlay View (Page 3)
- [ ] Version info display (Page 1)
- [ ] Resource types visuals (Page 2)
- [ ] Destinations instructions (Page 4)

### EditResourceActivity:
- [ ] All edit forms
- [ ] Password management
- [ ] Destination configuration
- [ ] Complete SPEC verification

---

*This analysis was generated on January 8, 2026 based on comprehensive comparison between current implementation and SPEC documentation.*

*Document will be updated as features are implemented and verified.*
