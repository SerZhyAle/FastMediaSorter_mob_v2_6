# 6. Player Activity

## PlayerActivity
**Package:** `com.sza.fastmediasorter.ui.player`  
**Purpose:** Media viewer/player with file operations  
**ViewModel:** `PlayerViewModel`  
**Layout:** `activity_player_unified.xml` (supports all media types)

**UI Elements:**

**Top Command Panel** (`topCommandPanel` - HorizontalScrollView):
- Initially hidden, shows on tap/hover
- Semi-transparent black background
- All buttons: 36x36dp with 4dp padding
- Contains context-specific buttons based on media type

**Universal Buttons (always visible when panel shown):**
- `btnBack` - ImageButton (36dp)
  - Icon: ic_arrow_back (white tint)
  - Action: Return to BrowseActivity
  - Returns list of modified files
  - ContentDescription: "Back"

**Text File Buttons** (visible for .txt, .log, .xml, .json, etc.):
- `btnSearchTextCmd` - ImageButton (36dp)
  - Icon: ic_menu_search (white tint)
  - Action: Show search panel
  - ContentDescription: "Search"
  - Visibility: gone (shown for text files)
- `btnTranslateTextCmd` - ImageButton (36dp)
  - Icon: ic_translate
  - Action: Translate text content
  - Visibility: gone (shown for text files)
- `btnTextSettingsCmd` - ImageButton (36dp)
  - Icon: ic_book (white tint)
  - Action: Font size, encoding options
  - ContentDescription: "Translation settings"
  - Visibility: gone (shown for text files)

**PDF File Buttons:**
- `btnSearchPdfCmd` - ImageButton (36dp)
  - Icon: ic_menu_search (white tint)
  - Action: Search in PDF
  - Visibility: gone (shown for PDF files)
- `btnEditPdf` - ImageButton (36dp)
  - Icon: ic_edit (white tint)
  - Action: Open PDF Tools dialog (Rotate, Reorder pages)
  - Visibility: gone (shown for PDF files)
- `btnTranslatePdfCmd` - ImageButton (36dp)
  - Icon: ic_translate
  - Action: Translate PDF text
  - Visibility: gone (shown for PDF files)
- `btnPdfTextSettingsCmd` - ImageButton (36dp)
  - Icon: ic_book (white tint)
  - Action: PDF translation settings
  - Visibility: gone (shown for PDF files)
- `btnOcrPdfCmd` - TextView (36dp height, wrap_content width)
  - Text: "OCR" (14sp, bold, white)
  - Background: selectableItemBackgroundBorderless
  - Action: OCR recognition for PDF
  - Padding: 8dp horizontal
  - Visibility: gone (shown for PDF files)
- `btnGoogleLensPdfCmd` - ImageButton (36dp)
  - Icon: ic_google_lens (white tint)
  - Action: Google Lens for PDF
  - ContentDescription: "Google Lens"
  - Visibility: gone (shown for PDF files)

**EPUB Buttons:**
- `btnSearchEpubCmd` - ImageButton (36dp)
  - Icon: ic_menu_search (white tint)
  - Action: Search in EPUB
  - Visibility: gone (shown for EPUB files)
- `btnTranslateEpubCmd` - ImageButton (36dp)
  - Icon: ic_translate
  - Action: Translate EPUB text
  - Visibility: gone (shown for EPUB files)
- `btnEpubTextSettingsCmd` - ImageButton (36dp)
  - Icon: ic_book (white tint)
  - Action: EPUB settings
  - ContentDescription: "Translation settings"
  - Visibility: gone (shown for EPUB files)
- `btnOcrEpubCmd` - ImageButton (36dp)
  - Icon: ic_ocr (white tint)
  - Action: OCR for EPUB
  - ContentDescription: "OCR"
  - Visibility: gone (shown for EPUB files)

**Image/GIF Buttons:**
- `btnTranslateImageCmd` - ImageButton (36dp)
  - Icon: ic_translate
  - Action: Translate text in image
  - ContentDescription: "Translate"
  - Visibility: gone (shown for images/GIFs)
- `btnImageTextSettingsCmd` - ImageButton (36dp)
  - Icon: ic_book (white tint)
  - Action: Translation settings
  - Visibility: gone (shown for images/GIFs)
- `btnOcrImageCmd` - ImageButton (36dp)
  - Icon: ic_ocr (white tint)
  - Action: OCR text recognition
  - Visibility: gone (shown for images/GIFs)
- `btnGoogleLensImageCmd` - ImageButton (36dp)
  - Icon: ic_google_lens (white tint)
  - Action: Google Lens for image
  - ContentDescription: "Google Lens"
  - Visibility: gone (shown for images/GIFs)

**Audio Buttons:**
- `btnLyricsCmd` - ImageButton (36dp)
  - Icon: ic_lyrics
  - Action: Show lyrics viewer
  - Visibility: gone (shown for audio files)

**Common File Operation Buttons:**
- `btnRenameCmd` - ImageButton (36dp)
  - Icon: ic_edit (white tint)
  - Action: Show RenameDialog
  - ContentDescription: "Rename"
- `btnEditCmd` - ImageButton (36dp)
  - Icon: ic_edit (white tint)
  - Action: Edit image/GIF (ImageEditDialog)
  - Visibility: gone (shown for images/GIFs)
- `btnCopyTextCmd` - ImageButton (36dp)
  - Icon: ic_content_copy (white tint)
  - Action: Copy text to clipboard
  - Visibility: gone (shown for text/PDF/EPUB)
- `btnEditTextCmd` - ImageButton (36dp)
  - Icon: ic_edit (white tint)
  - Action: Enable text editing mode
  - Visibility: gone (shown for text files)
- `btnUndoCmd` - ImageButton (36dp)
  - Icon: ic_undo (white tint)
  - Action: Undo last operation
  - ContentDescription: "Undo"
- `btnOverflowMenu` - ImageButton (72dp width, 36dp height)
  - Icon: ic_more_vert (3 vertical dots, white tint)
  - Action: Show overflow menu with actions that don't fit in landscape
  - ContentDescription: "More options"
  - Visibility: gone in landscape, visible in portrait mode
  - **Overflow Menu Contents** (PopupMenu):
    - "Share" - Share current file
    - "Info" - Show FileInfoDialog
    - "Fullscreen" - Toggle fullscreen mode
    - "Slideshow" - Start/Stop slideshow
    - "Previous" - Previous file (if not visible)
    - "Next" - Next file (if not visible)
    - Menu items visibility depends on current media type and state

**Separated Group (right side of panel):**
- `btnDeleteCmd` - ImageButton (36dp)
  - Icon: ic_delete (white tint)
  - Action: Delete current file
  - ContentDescription: "Delete"
- `btnFavorite` - ImageButton (36dp)
  - Icon: ic_favorite / ic_favorite_border (toggles)
  - Action: Toggle favorite status
  - ContentDescription: "Favorite"
- `btnShareCmd` - ImageButton (36dp)
  - Icon: ic_share (white tint)
  - Action: Share file with other apps
  - ContentDescription: "Share"
- `btnInfoCmd` - ImageButton (36dp)
  - Icon: ic_info (white tint)
  - Action: Show FileInfoDialog
  - ContentDescription: "Information"
- `btnFullscreenCmd` - ImageButton (36dp)
  - Icon: ic_fullscreen / ic_fullscreen_exit (toggles, white tint)
  - Action: Toggle fullscreen mode
  - ContentDescription: "Fullscreen"
- `btnSlideshowCmd` - ImageButton (36dp)
  - Icon: ic_slideshow (white tint)
  - Action: Start slideshow for images
  - Visibility: gone (shown for images)
- `btnPreviousCmd` - ImageButton (36dp)
  - Icon: ic_arrow_back (white tint)
  - Action: Previous file in playlist
  - ContentDescription: "Previous"
- `btnNextCmd` - ImageButton (36dp)
  - Icon: ic_arrow_forward (white tint)
  - Action: Next file in playlist
  - ContentDescription: "Next"

**Main Content Area** (`mediaContentArea` - FrameLayout):
- Full-screen container where all media views overlap
- Visibility and content changes based on media type

**For Images:**
- `imageView` - ImageView (Basic image display, used for simple images)
  - ScaleType: fitCenter
  - ContentDescription: "Image"
- `photoView` - PhotoView (Advanced zoom/pan support for images)
  - Pinch zoom, double-tap zoom
  - Pan when zoomed
  - EXIF orientation support
  - Replaces imageView for interactive viewing

**For Videos:**
- `playerView` - PlayerView (ExoPlayer)
  - Video surface
  - Seek bar
  - Play/Pause button
  - Time indicators (current/total)
  - Mute/Unmute button
  - Speed control overlay
  - Subtitles (if available)

**For Audio:**
- `playerView` - PlayerView (ExoPlayer): Audio playback engine
- `audioCoverArtView` - ImageView: Album cover art display (match_parent)
- `audioInfoOverlay` - FrameLayout: Audio metadata overlay
  - `audioFileName` - TextView: Track name (18sp, white, bold)
  - `audioFileInfo` - TextView: Artist/Album/Duration info (14sp, gray)

- `pdfFullscreenOverlay` - FrameLayout (Full-screen PDF viewer)
  - Background: #FF000000 (solid black)
  - Visibility: gone (shown on PDF page long-tap or zoom gesture)
  - Clickable: true (blocks touches to underlying views)
  - Components:
    - `pdfFullscreenPhotoView` - PhotoView: Zoomable full-screen PDF page
      - Supports pinch zoom (1x-5x)
      - Pan when zoomed
      - Double-tap to zoom in/out
      - **Rendering:** Current page bitmap scaled to screen size
    - `btnExitPdfFullscreen` - ImageButton: Exit fullscreen (top-right corner)
      - Size: 36dp
      - Icon: ic_fullscreen_exit (white tint)
      - Background: bg_circle_dark
      - Position: 8dp margin from top and right
      - Action: Hide overlay, return to pdfControlsLayout view
  - **Entry Triggers:**
    - Long press on PDF page thumbnail in sidebar
    - Tap on rendered PDF page when zoom > 1.5x
    - Double-tap on PDF page in pdfRendererView
  - **Exit Triggers:**
    - btnExitPdfFullscreen click
    - Back button press
    - Swipe down gesture when at top of zoomed content

**For PDF:**
- `pdfRendererView` - Custom View
  - Page rendering via PdfRenderer
  - `rvPdfPages` - RecyclerView: Page thumbnails sidebar
  - `tvPdfPageNumber` - TextView: "Page 5 / 23"
  - `btnPdfPrevPage` / `btnPdfNextPage` - ImageButtons
  - Zoom controls
- `pdfControlsLayout` - LinearLayout (Horizontal, bottom overlay)
  - `btnPdfPrevPage` - ImageButton: Previous page (36dp)
  - `btnPdfZoomOut` - ImageButton: Zoom out (36dp)
  - `tvPdfPageIndicator` - TextView: Page counter (e.g., "5/42")
  - `btnPdfZoomIn` - ImageButton: Zoom in (36dp)
  - `btnTranslatePdf` - ImageButton: Translate PDF content (36dp)
  - `btnTranslationFontDecrease` - ImageButton: Decrease translation font (36dp)
  - `btnTranslationFontIncrease` - ImageButton: Increase translation font (36dp)
  - `btnGoogleLensPdf` - ImageButton: Google Lens for PDF (36dp)
  - `btnSearchPdf` - ImageButton: Search in PDF (36dp)
  - `btnPdfNextPage` - ImageButton: Next page (36dp)
- `pdfFullscreenOverlay` - FrameLayout (Full-screen PDF viewer)
  - `pdfFullscreenPhotoView` - PhotoView: Zoomable full-screen PDF page
  - `btnExitPdfFullscreen` - ImageButton: Exit fullscreen (top-right corner)

**For Text:**
- `textViewerContainer` - FrameLayout (Text file viewer container)
  - `btnCloseTextViewer` - Button: Close text viewer (MaterialButton)
  - `textScrollView` - ScrollView: Scrollable text container
    - `tvTextContent` - TextView: Text file content (monospace for code, 14sp)
  - `textEditContainer` - LinearLayout (Text editing mode, hidden by default)
    - `etTextContent` - EditText: Editable text area (match_parent height)
    - Bottom action buttons (LinearLayout Horizontal):
      - `btnCancelEdit` - Button: Cancel editing ("Cancel")
      - `btnSaveText` - Button: Save changes ("Save")

**For EPUB:**
- `epubWebView` - WebView: EPUB content renderer (match_parent)
- `btnExitEpubFullscreen` - ImageButton: Exit EPUB fullscreen mode (top-right, 48dp, hidden in normal mode)
- `epubControlsLayout` - LinearLayout (Horizontal, bottom overlay for EPUB navigation)
  - `btnEpubPrevChapter` - ImageButton: Previous chapter (36dp, ic_arrow_back)
  - `tvEpubChapterIndicator` - TextView: Chapter counter (e.g., "Chapter 5/12", 14sp)
  - `btnEpubToc` - ImageButton: Table of contents (36dp, ic_menu)
  - `btnEpubFontSizeDecrease` - ImageButton: Decrease font (36dp, "-")
  - `btnEpubFontSizeIncrease` - ImageButton: Increase font (36dp, "+")
  - `btnEpubNextChapter` - ImageButton: Next chapter (36dp, ic_arrow_forward)

**For Images (specific overlay buttons):**
- `btnTranslateImage` - ImageButton: Translate text in image (floating, 48dp, ic_translate, positioned bottom-right)
- `btnGoogleLensImage` - ImageButton: Google Lens for image (floating, 48dp, ic_google_lens)
- `btnOcrImage` - ImageButton: OCR text recognition (floating, 48dp, ic_ocr)

**Lyrics Viewer** (for Audio files):
- `lyricsViewerContainer` - FrameLayout (Lyrics overlay container)
  - Background: ?android:attr/colorBackground (theme color)
  - Elevation: 16dp (above all content)
  - Visibility: gone (shown by btnLyricsCmd)
  - Clickable: true (blocks touches to underlying views)
  - Header buttons (LinearLayout Horizontal):
    - `btnTranslateLyrics` - ImageButton: Translate lyrics (36dp, ic_translate)
      - Position: Top-right, 64dp from right edge (left of close button)
      - Action: Translate tvLyricsContent text to target language
      - Shows translated text in translationOverlay (bottom sheet)
      - **Requires:** Non-empty lyrics text
    - Spacer: Pushes buttons to edges
    - `btnCloseLyricsViewer` - ImageButton: Close lyrics (36dp, ic_close)
      - Position: Top-right corner
      - Action: Hide lyricsViewerContainer
  - `lyricsScrollView` - ScrollView: Scrollable lyrics container
    - Margin top: 56dp (below header buttons)
    - Padding: 16dp
    - Contains:
      - `tvLyricsContent` - TextView: Lyrics text (16sp, white, centered)
        - Line spacing: +4dp
        - Text selectable: true
        - Scroll horizontally: false
  - **Data Source:**
    - .lrc files (LRC format with timestamps)
    - Embedded ID3v2 USLT tags (MP3/M4A)
    - External .txt files (same name as audio file)
    - Manual paste from clipboard
  - **Auto-scroll:** Syncs with audio playback position (for .lrc files)
  - **Entry:** btnLyricsCmd click in command panel

**For GIF:**
- `gifView` - ImageView with GIF support
  - Auto-play option
  - Frame-by-frame control
  - Speed adjustment

**Translation Overlay:**
- `translationLensOverlay` - TranslationOverlayView: Google Lens-style overlay with translation boxes over original image/PDF text
- `translationOverlayView` - TranslationOverlayView
  - Semi-transparent blocks over original text
  - Translated text in white rounded rectangles
  - Tap to bring block to front
- `translationOverlay` - FrameLayout (Full-screen overlay for text translations)
  - `translationOverlayBackground` - View: Semi-transparent black background
  - Container with close button and scrollable content:
    - `btnCloseTranslation` - ImageButton: Close translation view (top-right)
    - `translationScrollView` - ScrollView
      - `tvTranslatedText` - TextView: Translated text content (large, white text)

**Search Panel** (for Text/PDF/EPUB):
- `searchPanel` - LinearLayout (Horizontal, top overlay with gray background)
  - `etSearchQuery` - EditText: Search input field (weight: 1, hint: "Search...")
  - `tvSearchCounter` - TextView: Results counter (e.g., "3/15", 60dp width)
  - `btnSearchPrev` - ImageButton: Previous result (36dp, ic_arrow_up)
  - `btnSearchNext` - ImageButton: Next result (36dp, ic_arrow_down)
  - `btnCloseSearch` - ImageButton: Close search panel (36dp, ic_close)

**Touch Zone Overlays:**
- `touchZoneOverlayView` - TouchZoneOverlayView
  - 3x3 grid with labels
  - Semi-transparent
  - Toggle visibility via settings

- `touchZonesOverlay` - FrameLayout (2-zone layout for images/videos)
  - `touchZonePrevious` - View: Left half of screen (previous file)
  - `touchZoneNext` - View: Right half of screen (next file)

- `touchZones3Overlay` - FrameLayout (3-zone layout for images)
  - `touchZone3Previous` - View: Left zone (previous file)
  - `touchZone3Gestures` - View: Center zone (zoom/pan gestures)
  - `touchZone3Next` - View: Right zone (next file)

- `audioTouchZonesOverlay` - FrameLayout (9-zone grid for audio files)
  - Layout: 3x3 grid with labeled zones
  - Zones (all TextView with centered text):
    - Top row: `zoneBack` (Back), `zoneCopy` (Copy), `zoneRename` (Rename)
    - Middle row: `zonePrevious` (◄ Prev), `zoneMove` (Move), `zoneNext` (Next ►)
    - Bottom row: `zoneCommandPanel` (☰), `zoneDelete` (Delete), `zoneSlideshow` (▶)
  - Each zone: White text on semi-transparent background
  - Size: Each zone ~1/3 of screen width/height

- `touchZonesOverlayNew` - TouchZoneOverlayView
  - Custom configurable touch zone view
  - Supports user-defined zone layouts

- `tvFileNameOverlay` - TextView
  - Position: Top-center of screen
  - Text: Current file name (ellipsize middle for long names)
  - Background: Semi-transparent black rounded rectangle (#B0000000)
  - Padding: 12dp horizontal, 6dp vertical
  - TextColor: White
  - TextSize: 14sp
  - MaxWidth: 80% of screen width
  - Auto-hide after 2 seconds of inactivity
  - Appears on: File change, swipe navigation, touch zone navigation
  - Animation: Fade in (200ms), fade out (200ms)

**Bottom Command Panel** (`bottomCommandPanel` - LinearLayout Horizontal):
- Always visible (unless auto-hide enabled)
- Background: colorSurface
- Elevation: 8dp
- Buttons (all 40x40dp):

- `btnPreviousFile` - ImageButton: Previous file in playlist
- `btnPlayPause` - ImageButton: Pause/Resume slideshow
- `btnNextFile` - ImageButton: Next file in playlist

- Separator (vertical line)

- `btnCopyFile` - ImageButton: Show CopyToDialog
- `btnMoveFile` - ImageButton: Show MoveToDialog
- `btnDeleteFile` - ImageButton: Show DeleteDialog
- `btnRenameFile` - ImageButton: Show RenameDialog
- `btnEditFile` - ImageButton: Show ImageEditDialog/GifEditorDialog
- `btnInfoFile` - ImageButton: Show FileInfoDialog
- `btnShareFile` - ImageButton: Share file
- `btnOpenWith` - ImageButton: Open with external app
- `btnUndoOperation` - ImageButton: Undo last operation

**Quick Destination Buttons Panel** (`layoutQuickDestinations`):
- Positioned at right edge of bottom panel
- Up to 10 small circular buttons (32x32dp)
- Colors match destination resources
- Numbers 1-10 inside
- Long press → Choose copy or move
- Short press → Uses last operation type

**Copy/Move Quick Panels** (Bottom slide-up panels):
- `copyToPanel` - LinearLayout (Vertical)
  - Position: Bottom of screen, slides up on swipe
  - Background: #CC004D00 (semi-transparent dark green)
  - Height: wrap_content (max 50% screen height)
  - Components:
    - `copyToPanelHeader` - LinearLayout (Horizontal)
      - Height: 48dp
      - Background: selectableItemBackground (clickable)
      - Contains:
        - `copyToPanelIndicator` - TextView: "▼" (12sp, white)
        - Title: "Copy to" (12sp, white)
      - **Click Action:** Toggle panel collapse/expand
      - **State Persistence:** Collapsed state saved to AppSettings.copyPanelCollapsed
    - `copyToButtonsGrid` - LinearLayout (Vertical)
      - Dynamically populated with destination buttons (max 10 or maxRecipients)
      - **Button Distribution:** Smart layout calculation:
        - ≤5 buttons: 5x1 row
        - 6-10 buttons: 5x2 rows
        - 11-15 buttons: 5x3 rows (if maxRecipients > 10)
      - Each button: MaterialButton with resource icon + short name (8-10 chars)
      - Button color: destination.destinationColor with auto-contrast text
      - Action: Copy current file to selected destination
      - **Current Resource Excluded:** Cannot copy to same resource

- `moveToPanel` - LinearLayout (Vertical)
  - Identical structure to copyToPanel
  - Background: #CC00004D (semi-transparent dark blue)
  - Components:
    - `moveToPanelHeader` - LinearLayout
      - `moveToPanelIndicator` - TextView: "▼"
      - Title: "Move to"
      - **Click Action:** Toggle panel collapse/expand
      - **State Persistence:** Collapsed state saved to AppSettings.movePanelCollapsed
    - `moveToButtonsGrid` - LinearLayout
      - Move action instead of copy
  - Swipe down to dismiss

**File Info Overlay** (`layoutFileInfo` - LinearLayout Vertical):
- Position: Top-left corner
- Semi-transparent background
- Fades in/out with command panels
- Contains:
  - `tvFileName` - TextView: File name (truncated)
  - `tvFileSize` - TextView: File size (human-readable)
  - `tvFileDate` - TextView: Date modified
  - `tvFileDuration` - TextView: Duration (for video/audio)
  - `tvFileResolution` - TextView: Resolution (for images/videos)
  - `tvFilePosition` - TextView: "5 / 123" (position in playlist)

**Slideshow Controls Overlay** (`layoutSlideshowControls`):
- Position: Bottom-center (above command panel)
- Contains:
  - `progressSlideshow` - ProgressBar (Horizontal)
    - Shows time until next file
  - `tvSlideshowTimer` - TextView: "Next in 5s"
  - `btnPauseSlideshow` - ImageButton (Small): Pause/Resume

**Slideshow Countdown** (`tvCountdown` - TextView):
- Position: Top-right corner with 16dp margin
- Text: "3..", "2..", "1.." (counts down before slide advance)
- TextSize: 14sp
- TextColor: #888888 (gray)
- Visibility: gone (shown only during last 3 seconds before next slide)
- Elevation: 10dp
- Behavior: Appears 3 seconds before auto-advance, counts down with 1-second ticks

**Loading Indicator:**
- `progressBarLoading` - ProgressBar (Circular)
  - Centered on screen
  - Shows during: file loading, network download

**Error State:**
- `layoutError` - LinearLayout Vertical
  - Centered on screen
  - Contains:
    - `tvErrorMessage` - TextView: Error description
    - `btnRetryLoad` - MaterialButton: "Retry"
    - `btnSkipFile` - MaterialButton: "Skip File"

**Supported Media Types:**
- Images: JPG, PNG, BMP, WEBP (PhotoView with zoom/pan)
- Videos: MP4, MKV, AVI, MOV, WEBM (ExoPlayer)
- Audio: MP3, WAV, OGG, FLAC (ExoPlayer with waveform)
- GIFs: Animated playback with speed control
- Documents: PDF (PdfRenderer with page navigation), TXT (text viewer)
- EPUB: E-book reader (if enabled)

**Small Controls Mode:**
- **Purpose:** Compact UI for small screens or user preference
- **Activation:** Settings → "Small Controls" toggle
- **Changes Applied:**
  - All buttons scaled to 50% (SMALL_CONTROLS_SCALE = 0.5f)
  - Original button heights cached for restore
  - Original margins/paddings preserved
  - Applied to: Command panel buttons, destination buttons, PDF controls
- **Button Height Calculation:** `originalHeight * 0.5f`
- **Persistence:** State tracked in `smallControlsApplied` boolean
- **Restore:** Returns all buttons to original cached dimensions

**Features:**
- **Media Display:**
  - Images: PhotoView with pinch zoom (up to 5x), double-tap zoom, pan when zoomed
  - Videos: ExoPlayer with adaptive streaming, subtitle support, hardware acceleration
  - Audio: Waveform visualization, album cover art, lyrics viewer
  - PDF: Page-by-page rendering with PdfRenderer, thumbnail sidebar, zoom/pan
  - Text: Syntax highlighting for code (JSON/XML/Java/Kotlin), font size adjustment, encoding detection (UTF-8/CP1251/ISO-8859-1)
  - EPUB: WebView-based reader with chapter navigation, font customization
  - GIF: Animated playback with speed control, frame extraction
- **Slideshow Mode:**
  - Auto-advance: Timer-based navigation (5-60s intervals)
  - Pause/Resume: Space key or middle-center touch zone
  - Random Shuffle: Randomizes playlist order
  - Progress Indicator: Circular progress bar with countdown
  - Skip Video: Auto-skips videos in image slideshow (optional)
- **Touch Zone System (Configurable 2/3/9-zone layouts):**
  - **2-Zone (Images/Videos):**
    - Left half: Previous file
    - Right half: Next file
  - **3-Zone (Images):**
    - Left: Previous file
    - Center: Zoom/Pan gestures
    - Right: Next file
  - **9-Zone Grid (All media types):**
    - Top row: Delete | File Info | Rotate
    - Middle row: Previous | Pause/Play | Next
    - Bottom row: Copy Dest-1 | Share | Move Dest-1
  - **9-Zone Audio Variant:**
    - Top: Back | Copy | Rename
    - Middle: Previous | Move | Next
    - Bottom: Commands | Delete | Slideshow
  - Zone overlay toggle: Settings → Show Touch Zone Overlay
- **Command Panel (Context-Sensitive):**
  - Universal: Back, Previous, Next, Info, Share, Favorite, Fullscreen
  - Images/GIFs: Edit, Rotate, Crop, Filters, OCR, Google Lens, Translate
  - Videos: Play/Pause, Seek, Mute, Speed (0.25x-2x), Repeat, Subtitle
  - Audio: Play/Pause, Lyrics, Waveform Toggle
  - PDF: Page Nav, Zoom, Search, OCR, Google Lens, Translate
  - Text: Search, Font Size, Encoding, Edit, Save, Translate
  - EPUB: Chapter Nav, TOC, Font Size, Search, Translate
- **File Operations:**
  - Copy: Quick panel with 10 destinations or full CopyToDialog
  - Move: Quick panel with 10 destinations or full MoveToDialog
  - Delete: Soft-delete to .trash folder (undo-able)
  - Rename: RenameDialog with validation
  - Share: Android share intent (supports single file)
  - Undo: Restores last operation (1 level)
  - Open With: External app chooser
- **Video Controls:**
  - ExoPlayerControlsManager setups custom buttons in PlayerView overlay:
    - **exo_prev_file:** Navigate to previous file in playlist
    - **exo_next_file:** Navigate to next file in playlist
    - **exo_repeat:** Toggle repeat mode (OFF → ONE → OFF)
      - Icon updates: ic_repeat (OFF), ic_repeat_one (ONE)
    - **exo_speed:** Show playback speed dialog (0.25x-2x)
    - **btnRewind10:** Seek backward 10 seconds (audiobook mode)
    - **btnForward30:** Seek forward 30 seconds (audiobook mode)
  - **Subtitle Customization:** CaptionStyleCompat with user font/size from TranslationSettings
  - Seek Bar: Scrubbing with thumbnail preview (ExoPlayer StyledControls)
  - Playback Speed: 0.25x, 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
  - Subtitles: Auto-detect .srt/.vtt files, selectable tracks
  - Repeat: Single file loop
  - Gesture Controls: Swipe up/down on right side for volume, left side for brightness
- **Image Editing (ImageEditDialog):**
  - Rotate: 90°/180°/270°
  - Flip: Horizontal/Vertical
  - Crop: Freeform or aspect ratio (1:1, 4:3, 16:9)
  - Filters: Brightness, Contrast, Saturation, Sepia, Grayscale
  - Save: Overwrites original or saves as new file
- **GIF Editing (GifEditorDialog):**
  - Frame Extraction: Save specific frame as PNG
  - Speed Adjustment: 0.5x, 1x, 2x playback speed
  - Reverse: Plays GIF in reverse order
  - Frame-by-Frame: Manual navigation with slider
- **PDF Navigation:**
  - Page Thumbnails: Sidebar with clickable previews
  - Jump to Page: Input dialog for direct navigation
  - Zoom: Pinch zoom up to 5x
  - **Search:**
    - SearchControlsManager handles all search operations
    - **Search Panel Components:**
      - etSearchQuery: EditText with IME_ACTION_SEARCH
      - tvSearchCounter: "3/15" format (current/total matches)
      - btnSearchPrev: Previous match navigation (ic_arrow_up)
      - btnSearchNext: Next match navigation (ic_arrow_down)
      - btnCloseSearch: Close panel and clear search
    - **Search Triggers:**
      - btnSearchPdfCmd / btnSearchTextCmd / btnSearchEpubCmd: Show search panel
      - Text change in etSearchQuery: Auto-perform search
      - IME action SEARCH: Perform search and hide keyboard
    - **Search Workflow:**
      1. Show searchPanel (isVisible = true)
      2. Focus etSearchQuery + show keyboard
      3. Type query → Auto-search after each character
      4. Display tvSearchCounter with match count
      5. Navigate with btnSearchNext/Prev
      6. btnCloseSearch: Clear search, hide panel, hide keyboard
    - **Media Type Handling:**
      - TEXT: textViewerManager.searchText(query) → highlight matches in TextView
      - PDF: pdfViewerManager.searchInPdf(query) → extract text and find positions
      - EPUB: epubViewerManager.searchInEpub(query) → search across chapters
    - **Result Navigation:**
      - PDF: Previous/Next updates page and scrolls to match
      - TEXT: Highlights match (no scroll navigation yet)
      - EPUB: Previous/Next loads chapter and jumps to match
    - **Search State:**
      - Counter updated after each search
      - Cleared on query empty or btnCloseSearch
      - Preserved during file navigation (remains open)
    - Full-text search with result highlighting
  - Search: Full-text search with result highlighting
  - Fullscreen: Tap page for distraction-free reading
- **Text Viewer:**
  - Syntax Highlighting: JSON, XML, Java, Kotlin, Python, JavaScript
  - Font Size: 8sp-24sp with +/- buttons
  - Encoding Selection: UTF-8, CP1251, ISO-8859-1, Windows-1252
  - Edit Mode: Convert TextView to EditText, save button appears
  - Search: Find text with prev/next navigation
- **Translation Overlay (Images/PDF/Text):**
  - **Two Overlay Types:**
    1. **translationLensOverlay** (Google Lens style):
       - TranslationOverlayView with positioned text blocks
       - Semi-transparent rectangles over original text
       - Translated text in white rounded boxes
       - Each block has boundingBox (Rect), originalText, translatedText, confidence
       - Tap block: Bring to front (z-order)
       - **Visibility:** Images, PDF pages
    2. **translationOverlay** (Simple overlay):
       - CardView at bottom with scrollable translated text
       - Background: #B0000000 (semi-transparent black)
       - Max height: 200dp
       - Components:
         - btnCloseTranslation: Close button (top-right)
         - translationScrollView: Scrollable container
         - tvTranslatedText: Full translated text (14sp, white)
       - **Click Actions:**
         - translationOverlay.onClick: Toggle expand/collapse (changes height)
         - btnCloseTranslation.onClick: Hide overlay, reset button color
         - translationOverlayBackground.onClick: Hide overlay (click outside)
       - **Visibility:** Text files, EPUB
  - **Font Size Adjustment:**
    - Horizontal swipe on translationScrollView:
      - Swipe right: Increase font size (+2sp, max 72sp)
      - Swipe left: Decrease font size (-2sp, min 6sp)
    - btnTranslationFontIncrease / btnTranslationFontDecrease (PDF controls)
    - Default: 14sp, persists during session
  - **Button State Indicators:**
    - btnTranslateImage / btnTranslateImageCmd turn RED (#F44336) when translation active
    - Reset to white tint when translation hidden
  - **Toggle Behavior:**
    - Click translate button when visible: Hide overlay (stopTranslation)
    - Click translate button when hidden: Start OCR + translation
  - Google Lens-style: Semi-transparent boxes over original text
  - Translated text: White rounded rectangles with target language
  - **Long-Press Actions:**
    - btnTranslatePdfCmd: Show translation settings dialog
    - btnTranslateImage: Show translation settings dialog
    - btnTranslateImageCmd: Show translation settings dialog
    - btnTranslateTextCmd: Show translation settings dialog
    - btnTranslateEpubCmd: Show translation settings dialog
    - btnOcrImageCmd: Show OCR settings dialog
    - btnOcrPdfCmd: Show OCR settings dialog
    - btnOcrEpubCmd: Show OCR settings dialog
    - **Settings Dialogs:**
      - Translation: Source language, target language, font size, font family
      - OCR: Tesseract language data download, confidence threshold
  - Tap box: Bring to front, copy to clipboard
  - Languages: 100+ via Google Translate API
  - Settings: Source/Target language, font size, overlay opacity
- **EPUB Reader:**
  - **Chapter Navigation:**
    - btnEpubPrevChapter: Previous chapter (44dp, ic_media_previous, green tint)
    - btnEpubNextChapter: Next chapter (44dp, ic_media_next, green tint)
    - tvEpubChapterIndicator: "Chapter 5/12" (16sp, white, bold)
      - Background: bg_rounded_dark
      - Padding: 12dp horizontal, 4dp vertical
  - **Table of Contents:**
    - btnEpubToc: Show TOC dialog (44dp, ic_menu_agenda)
    - Dialog lists all chapters with click navigation
    - Current chapter highlighted
  - **Font Customization:**
    - btnEpubFontSizeDecrease: Decrease font (-2sp, min 6sp)
    - btnEpubFontSizeIncrease: Increase font (+2sp, max 72sp)
    - Default: 16sp for EPUB content
    - JavaScript injection to update WebView font size
    - Font family: Serif/Sans-serif from settings
    - Line height: 1.2-2.0 multiplier
  - **Fullscreen Mode:**
    - btnExitEpubFullscreen: Exit button (36dp, top-right)
    - Hides epubControlsLayout when entering fullscreen
    - Shows btnExitEpubFullscreen (initially hidden)
    - Tap screen: Toggle controls visibility
  - **WebView Configuration:**
    - JavaScript enabled for font customization
    - DOM storage enabled for reading position
    - Load EPUB HTML content from extracted temp directory
    - CSS injection for theme (light/dark from system)
  - **Reading Position:**
    - Saved to SharedPreferences on chapter change
    - Restored on EPUB reload
    - Format: "book_uri:chapter_index:scroll_position"
  - Chapter Navigation: Prev/Next buttons with chapter indicator
  - Table of Contents: Dialog with chapter list
  - Font Customization: Size, family (Serif/Sans-serif), line height
  - Search: Full-text search across chapters
  - Bookmarks: Save reading position
- **Fullscreen Mode:**
  - Hides system bars (status bar, navigation bar)
  - Immersive sticky mode (swipe from edge to reveal)
  - Auto-hides command panel after 3s inactivity
  - Tap screen to toggle panel visibility
- **Playlist Management:**
  - Receives List<MediaFile> from BrowseActivity
  - Supports navigation via Previous/Next buttons or swipe gestures
  - Position indicator: "5 / 123" shown in top-left corner
  - Returns modified files list on exit (for cache invalidation)
  - **Universal Access Filter:**
    - If `WorkWithAllFiles` is enabled, playlist **excludes** `MediaType.OTHER` (non-media files).
    - Player automatically skips these files during next/prev navigation.
- **Network File Handling:**
  - NetworkFileManager handles remote file operations
  - **Download Strategy:**
    - Images/PDFs/EPUB/Text: Downloads to UnifiedFileCache before display
    - Videos/Audio: Streams directly via ExoPlayer DataSource (no download)
  - **Progress Tracking:** Shows notification with progress bar and cancel button
  - **Retry Logic:** 3 attempts with exponential backoff on connection failure
  - **Connection Throttling:** Limits concurrent operations via ConnectionThrottleManager
  - **Resource Key Tracking:** Prevents connection pool exhaustion (SMB/SFTP)
  - **Edit-and-Upload Workflow:**
    1. Downloads original file to temp cache
    2. User edits locally (rotation, filters, text changes)
    3. Uploads modified file back to server
    4. Cleans temp cache
    5. Invalidates Glide cache with new signature
  - Streaming: Videos/Audio streamed directly (no download)

**Functionality:**
- **Media Loading Pipeline:**
  - ViewModel receives MediaFile from intent
  - NetworkFileManager checks if remote (SMB/SFTP/FTP/Cloud)
  - Remote files: Downloads to cache or streams URL
  - Local files: Loads directly via URI
  - **ImageLoadingManager:**
    - Glide wrapper for high-performance loading
    - **Encryption Support:** Decrypts encrypted files via custom ModelLoader
    - **Thumbnail Strategy:** DiskCacheStrategy.RESOURCE for fast preview
    - **Full-Size Strategy:** DiskCacheStrategy.NONE for live edits
    - Priority.HIGH for current image, Priority.NORMAL for preload
    - ObjectKey signature with lastModified + fileSize for cache invalidation
    - Preloads next 3 images in background with Job cancellation on destroy
  - ExoPlayer initialized for video/audio
- **Touch Zone Detection:**
  - TouchZoneGestureManager monitors touch events with two GestureDetectors:
    - **Image Touch Detector:** Handles PhotoView/ImageView taps (lower priority after zoom)
    - **General Touch Detector:** Handles PlayerView and document viewers
  - **Fullscreen Mode (Images/Videos):**
    - Always uses 9-zone grid (even if touch zones disabled in settings)
    - Only way to exit fullscreen via touch
    - Top row: Delete | Info | Rotate
    - Middle row: Previous | Play/Pause | Next
    - Bottom row: Copy Dest-1 | Share | Move Dest-1
  - **Command Panel Mode (Images):**
    - 2-zone layout (left=Previous, right=Next)
    - Requires touch zones enabled in settings
    - Disabled when overlays (translation/OCR/search) visible
  - **Audio Fullscreen:**
    - 9-zone grid with labels (audioTouchZonesOverlay)
    - Top: Back | Copy | Rename
    - Middle: Previous | Move | Next
    - Bottom: Commands | Delete | Slideshow
  - **PDF/EPUB/Video:**
    - Upper 75% (video) or 66% (audio) active for zones
    - Reserves lower portion for player controls
  - **Zone Overlay Toggle:** Settings → "Show Touch Zone Overlay" for visual feedback
  - Divides screen into zones based on layout (2/3/9)
  - Maps zone to action (next/prev/play/delete/etc.)
  - Executes action via ViewModel command
  - **Touch Listeners Priority:**
    - Root view (binding.root): Handles general gestures with lowest priority
      - Consumes events ONLY for touch zone detection in fullscreen
      - Returns false to allow child views to handle first
    - PlayerView (video/audio): Medium priority
      - Fullscreen mode: Delegates to touch zone detector
      - Command panel mode: Let PlayerView handle controls (returns false)
      - Audio with 9-zone overlay visible: Consumes all touches
    - PhotoView (zoomable images): High priority
      - Let overlays (translation/OCR/search) handle touches first
      - PhotoView gesture detection after overlays checked
      - Touch zone detection with imageTouchGestureDetector (onSingleTapConfirmed)
    - ImageView (simple images): Similar to PhotoView
    - Overlays (translation/search/lyrics): Highest priority
      - Blocks underlying view touches when visible
      - Clickable + focusable = true
  - **Event Consumption Rules:**
    - Touch zones in fullscreen: Consume (return true)
    - Overlays visible: Don't consume from photo/image views (return false)
    - Command panel visible: Don't consume (let buttons handle)
    - PDF/EPUB rendering: Don't consume (allow zoom/pan)
  - Visual feedback: Ripple effect on touch
- **File Operation Workflow:**
  - User triggers operation (button or touch zone)
  - PlayerViewModel calls UnifiedFileOperationHandler
  - Handler executes operation (copy/move/delete)
  - Result stored for undo
  - UI updated: Toast notification + file list refresh
  - Returns result to BrowseActivity on exit
- **Slideshow Controller:**
  - SlideshowController manages timer and countdown display
  - Timer advances to next file after interval (configurable 5-60s)
  - **Countdown Display:** Shows "3..", "2..", "1.." in last 3 seconds before advance
  - **Auto-advance Logic:**
    - Pauses on video playback when `playToEndInSlideshow=true`
    - Skips documents (PDF/TXT/EPUB) when `skipDocuments=true`
    - Callback to ViewModel: `onSlideAdvance()` returns boolean for rescheduling
  - **Play-to-End Mode:** Waits for ExoPlayer STATE_ENDED event instead of timer
  - Lifecycle-aware: Pauses on background, resumes on foreground
  - Progress bar with countdown visible during slideshow
  - Resumes after manual navigation (preserves slideshow state)
- **Translation Workflow:**
  - TranslationManager uses two engines:
    - **ML Kit Text Recognition:** Fast Latin script OCR
    - **Tesseract OCR:** Cyrillic script detection with language-specific models
  - **Cyrillic Enhancement:** Applies Latin→Cyrillic character conversion after OCR
  - Supported Cyrillic languages: Russian, Ukrainian, Bulgarian, Belarusian, Macedonian
  - **Translation Flow:**
    1. Detects text in media (OCR for images, text extraction for PDF)
    2. Auto-detects source language with ML Kit LanguageIdentification
    3. Downloads translation model if not cached (user consent required)
    4. Translates text to target language (from settings)
    5. Creates TranslatedTextBlock with originalText, translatedText, boundingBox, confidence
  - Overlays translated blocks on image/PDF via TranslationOverlayView
  - **Google Lens Style:** Semi-transparent white rectangles with translated text
  - User can tap block to copy, bring to front, or adjust position
  - **Font Settings:** Size (Small/Medium/Large), family (Serif/Sans-serif), applied to overlay
  - **PDF Translation:** Page-by-page with caching for visited pages
- **PDF Rendering:**
  - PdfViewerManager uses Android PdfRenderer API
  - Opens PDF via ParcelFileDescriptor (local/cached file)
  - Renders current page to Bitmap with configurable DPI
  - **Page Caching Strategy:**
    - Current page: Full resolution (72-300 DPI based on zoom)
    - Adjacent pages: Pre-rendered to cache (background thread)
    - Cache size: 5 pages (current + 2 prev + 2 next)
    - LRU eviction for memory management
  - **Zoom Implementation:**
    - PhotoView wraps rendered Bitmap
    - Pinch zoom: 1x-5x scale range
    - Re-renders page at higher DPI on zoom > 2x (sharpening)
  - **Fullscreen Mode:**
    - Tap page → Shows fullscreen PhotoView with page Bitmap
    - Hides PDF controls overlay
    - Exit via btnExitPdfFullscreen or back gesture
  - **Search:**
    - PdfTextExtractor parses text layer from PDF
    - Finds matching text positions (Rect coordinates)
    - Highlights matches on Bitmap overlay
    - Navigation: prev/next result with tvSearchCounter ("3/15")
  - Search: Extracts text via PdfTextExtractor
- **Text Viewer:**
  - TextViewerManager loads file content as String
  - **Encoding Detection:** Auto-detects via charset detector library
    - Supported: UTF-8, CP1251, ISO-8859-1, Windows-1252
    - Fallback: UTF-8 if detection confidence < 0.5
  - **Edit Mode Workflow:**
    1. btnEditTextCmd click: Hides textScrollView (read-only TextView)
    2. Shows textEditContainer (EditText with save/cancel buttons)
    3. Copies tvTextContent.text to etTextContent
    4. User modifies text
    5. btnSaveText: Validates changes, writes to file with original encoding
    6. btnCancelEdit: Discards changes, returns to read mode
    7. Show keyboard automatically on edit mode entry
  - **Font Size Adjustment:**
    - Horizontal swipe gestures on textScrollView
    - Swipe right: Increase font size (+2sp, max 72sp)
    - Swipe left: Decrease font size (-2sp, min 6sp)
    - Default: 14sp
    - Persists during session (resets on activity destroy)
  - **Close Behavior:**
    - btnCloseTextViewer: Different action based on context
      - For OCR results (currentFile == null): Just hide overlay, restore previous view
      - For text files: Hide viewer AND exit fullscreen mode
    - Click outside text content (on textViewerContainer background): Dismiss OCR results only
    - Vertical swipe at scroll edges (OCR only):
      - Swipe up at bottom: Close OCR viewer
      - Swipe down at top: Close OCR viewer
  - **Network Files:**
    - Downloads to cache first
    - Edits cached copy
    - Uploads modified file on save
    - Shows progress during upload
  - **Syntax Highlighting:** Applies color spans for JSON/XML/Java/Kotlin (via external library)
  - Converts TextView to EditText in edit mode
  - User modifies text
  - Save: Writes back to file (preserves encoding)
  - Network files: Downloads, edits, uploads
- **EPUB Handling:**
  - Unzips EPUB to temp directory
  - Parses content.opf for chapter list
  - Loads chapter HTML into WebView
  - JavaScript for font customization
  - Stores reading position in preferences
- **Video Playback:**
  - VideoPlayerManager initializes ExoPlayer with custom LoadControl:
    - Network (SMB/SFTP/FTP): MIN_BUFFER_MS=50s, MAX_BUFFER_MS=120s
    - Cloud: MIN_BUFFER_MS=60s, MAX_BUFFER_MS=180s (slower connections)
    - BUFFER_FOR_PLAYBACK_MS: 10s (network), 15s (cloud)
  - Sets MediaItem from URI or streaming URL
  - Configures adaptive streaming (DASH/HLS)
  - **EOF Retry Logic:** Max 3 retries on End-of-File exceptions with exponential backoff
  - **Playback Health Monitoring:** Detects stuck playback (white noise) every 2 seconds
  - **Fallback to MediaPlayer:** If ExoPlayer fails 2x stuck checks, switches to Android MediaPlayer
  - **Audiobook Mode:** Auto-saves playback position every 5 seconds to PlaybackPositionRepository
  - Subtitle Style: Customizable via TranslationFontSize/FontFamily from settings
  - Handles lifecycle (pause on background)
  - Releases player on exit with connection throttle cleanup
- **Gesture Controls:**
  - PlayerGestureHelper + TouchZoneGestureManager detect:
    - **Swipe Gestures:**
      - Swipe left (horizontal): Next file
      - Swipe right (horizontal): Previous file
      - Swipe up on right half (vertical): Volume increase (video/audio)
      - Swipe down on right half (vertical): Volume decrease (video/audio)
      - Swipe up on left half (vertical): Brightness increase (images/video)
      - Swipe down on left half (vertical): Brightness decrease (images/video)
      - **Thresholds:** Distance > 100px, velocity > 100px/s
    - **Double-Tap:**
      - Images: Zoom in/out toggle (1x ↔ 2.5x)
      - Video: Play/Pause toggle
      - PDF: Fullscreen page view
    - **Long-Press:**
      - Images: Show edit dialog
      - PDF page: Fullscreen page view
      - Video: Show playback speed dialog
    - **Touch Zone Tap:**
      - Mapped to specific actions based on zone (see Touch Zone System)
    - **Pinch Zoom:**
      - Images (PhotoView): 1x-5x scale
      - PDF pages: 1x-5x scale (triggers high-DPI re-render at >2x)
    - **Font Size Swipes (Text/Translation):**
      - Horizontal swipe on textScrollView or translationScrollView
      - Right: Increase (+2sp), Left: Decrease (-2sp)
      - Only horizontal component checked (ignore vertical scroll)
  - Swipe left/right: Next/Previous file
    - Swipe up (right half): Volume increase
    - Swipe down (right half): Volume decrease
    - Swipe up (left half): Brightness increase
    - Swipe down (left half): Brightness decrease
    - Double-tap: Zoom/Pause toggle
- **Undo System:**
  - UndoOperationManager stores last operation
  - Undo button restores from .trash folder
  - 1-level undo (only last operation)
  - Disabled if no undo available
- **Command Panel Animation:**
  - CommandPanelController manages panel visibility and adaptive layout
  - **Landscape Mode:** All buttons visible in horizontal scroll view
  - **Portrait Mode:** Overflow menu button (72dp width) hides less-used actions
  - **Adaptive Button Visibility:**
    - Text files: Search, Translate, Text Settings, Copy Text, Edit Text buttons
    - PDF files: Search, Translate, OCR, Google Lens, PDF Settings buttons
    - EPUB files: Search, Translate, EPUB Settings, OCR buttons
    - Images/GIFs: Translate, OCR, Google Lens, Edit buttons
    - Audio files: Lyrics button
    - All types: Back, Rename, Delete, Favorite, Share, Info, Fullscreen, Previous, Next
  - **Language Badge:** Translation buttons show source language badge (e.g., "EN", "RU")
  - Fades in on tap (alpha 0→1, 200ms)
  - Auto-hides after 3s inactivity
  - Paused during file operations
- **Keyboard Navigation:**
  - PlayerKeyboardHandler intercepts key events
  - Arrow keys: Navigate files
  - Space: Pause/Play
  - Delete: Delete file
  - F2: Rename
  - R: Rotate image
  - 1-9: Jump to % (video)
  - Escape: Exit player
- **Analytics:**
  - Tracks media type access frequency
  - Logs playback duration
  - Reports most used operations
  - Monitors crash rates per media type

**Dialogs Used:**
- `PlayerSettingsDialog`: Quick settings
- `ImageEditDialog`: Image editing tools
- `GifEditorDialog`: GIF manipulation
- `CopyToDialog`, `MoveToDialog`, `DeleteDialog`, `RenameDialog`
- `FileInfoDialog`: Detailed metadata
- `MaterialProgressDialog`: Long operations

**Keyboard Navigation:**
- PlayerKeyboardHandler intercepts key events via onKeyDown
- **Supported Keys:**
  - **Arrow Left/Right**: Previous/Next file
  - **Arrow Up/Down**: 
    - Video/Audio: Volume up/down
    - Images: Brightness up/down (when not in zoom mode)
  - **Space**: 
    - Video/Audio: Pause/Play toggle
    - Slideshow: Pause/Resume
    - Images: Toggle slideshow
  - **Enter**: 
    - PDF: Next page
    - EPUB: Next chapter
    - Images: Start slideshow
  - **Delete**: Delete current file (shows confirmation)
  - **R**: Rotate image 90° clockwise
  - **F2**: Show rename dialog
  - **I**: Show file info dialog
  - **F**: Toggle fullscreen mode
  - **S**: Toggle slideshow
  - **C**: Copy to first destination
  - **M**: Move to first destination
  - **Escape**: 
    - Close overlays (translation/OCR/search/lyrics) if visible
    - Exit fullscreen if active
    - Otherwise: Exit player (back to BrowseActivity)
  - **1-9**: 
    - Video: Jump to 10%-90% of duration
    - Images: Quick destination buttons (if available)
  - **0**: Video: Jump to start (0%)
  - **Page Up/Down**: 
    - PDF: Previous/Next page
    - EPUB: Previous/Next chapter
  - **Home/End**: 
    - PDF: First/Last page
    - Video: Jump to start/end
- **Key Combinations:**
  - Ctrl+C: Copy current text selection (Text viewer)
  - Ctrl+V: Paste (Text editor mode)
  - Ctrl+S: Save text file (Text editor mode)
  - Ctrl+F: Show search panel
- Arrow Left/Right: Previous/Next file
- Space: Pause/play slideshow
- Delete: Delete file
- R: Rotate image
- F2: Rename
- I: File info
- Escape: Exit player
- 1-9: Jump to 10%-90% (video)
- M: Mute/unmute
