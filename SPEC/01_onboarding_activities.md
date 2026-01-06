# 1. Onboarding

## WelcomeActivity
**Package:** `com.sza.fastmediasorter.ui.welcome`  
**Purpose:** First-launch onboarding experience with permission requests  
**ViewModel:** `WelcomeViewModel`  
**Layout:** `activity_welcome.xml`

**UI Elements:**

**Root Container:**
- ConstraintLayout (match_parent × match_parent)
  - Background: ?attr/colorPrimary
  - Contains all UI elements

**Top Navigation Bar** (`layoutTopNav` - LinearLayout):
- Width: match_parent (0dp constrained)
- Height: wrap_content
- Orientation: horizontal
- Padding: 8dp all sides
- Gravity: center_vertical
- Position: constrained to top of parent
- Contains 4 buttons with Space between them

- `btnPrevious` - MaterialButton
  - Style: @style/Widget.MaterialComponents.Button.TextButton
  - Size: wrap_content × wrap_content
  - Text: @string/previous ("Previous")
  - TextColor: ?attr/colorOnPrimary
  - Visibility: gone (shows on pages 2-4)
  - Action: Navigate to previous page
  
- Space (flexible spacer)
  - Width: 0dp with weight=1
  - Height: 1dp
  - Purpose: Pushes buttons to edges

- `btnNext` - MaterialButton
  - Style: @style/Widget.MaterialComponents.Button.TextButton
  - Size: wrap_content × wrap_content
  - Text: @string/next ("Next")
  - TextColor: ?attr/colorOnPrimary
  - Visibility: visible (hidden on page 4)
  - Action: Navigate to next page
  
- `btnFinish` - MaterialButton
  - Style: @style/Widget.MaterialComponents.Button.TextButton
  - Size: wrap_content × wrap_content
  - Text: @string/finish ("Finish")
  - TextColor: ?attr/colorOnPrimary
  - Visibility: gone (shows only on page 4)
  - Action: Complete onboarding, request permissions
  
- `btnSkip` - MaterialButton
  - Style: @style/Widget.MaterialComponents.Button.TextButton
  - Size: wrap_content × wrap_content
  - Text: @string/skip ("Skip")
  - TextColor: ?attr/colorOnPrimary
  - Visibility: visible on all pages
  - Action: Immediately complete onboarding without viewing all pages

**Main Content:**
- `viewPager` - ViewPager2
  - Width: 0dp (constrained start→end)
  - Height: 0dp (constrained top→bottom)
  - MarginTop: 8dp
  - MarginBottom: 8dp
  - Constrained between layoutTopNav (top) and layoutIndicator (bottom)
  - Adapter: WelcomePagerAdapter
  - PageChangeCallback: Updates currentPage variable, calls updateUI()
  - 4 pages with horizontal swipe navigation:
    1. **Page 1 - Welcome:**
       - Icon: @mipmap/ic_launcher (app icon)
       - Title: @string/welcome_title_1
       - Description: @string/welcome_description_1
       - Content: App introduction with logo, welcome message, version info
    2. **Page 2 - Resource Types:**
       - Icon: @drawable/resource_types
       - Title: @string/welcome_title_2
       - Description: @string/welcome_description_2
       - Content: Explanation of resource types (Local/Network/Cloud with icons)
    3. **Page 3 - Touch Zones:**
       - Icon: @mipmap/ic_launcher
       - Title: @string/welcome_title_3
       - Description: @string/welcome_description_3
       - showTouchZonesScheme: true
       - Content: Touch zones demonstration (3x3 grid overlay with gesture examples)
    4. **Page 4 - Destinations:**
       - Icon: @drawable/destinations
       - Title: @string/welcome_title_4
       - Description: @string/welcome_description_4
       - Content: Destinations feature overview with setup instructions

**Bottom Section:**
- `layoutIndicator` - LinearLayout
  - Width: wrap_content
  - Height: wrap_content
  - Orientation: horizontal
  - MarginBottom: 24dp
  - Position: Constrained to bottom center (start→end, bottom→parent)
  - Contains: 4 dynamically created indicator Views
  - Each indicator:
    - Size: @dimen/indicator_size × @dimen/indicator_size
    - Margins: @dimen/indicator_margin on left and right
    - Background: @drawable/indicator_inactive (default)
    - Active page background: @drawable/indicator_active
    - Purpose: Visual representation of current page (dots)

**Features:**
- **4-Page Onboarding Tutorial:**
  - Page 1: App introduction with logo, welcome message, version info
  - Page 2: Resource types explanation (Local/Network/SFTP/FTP/Cloud with icons)
  - Page 3: Touch zones demonstration (3x3 grid overlay with gesture examples)
  - Page 4: Destinations feature overview with setup instructions
- **Progressive Navigation:**
  - Next button advances to next page with slide animation
  - Previous button returns to previous page
  - Skip button bypasses entire onboarding
  - Finish button appears only on last page
- **Permission Requests:**
  - Storage access (READ_EXTERNAL_STORAGE/MANAGE_EXTERNAL_STORAGE) on page 4
  - Notification permission (Android 13+) for background operations
  - Network access for SMB/SFTP/Cloud features
- **First-Launch Detection:**
  - Checks SharedPreferences for `is_welcome_completed` flag
  - Automatically skipped on subsequent app launches
  - Can be reset in Settings → About → "Show Welcome"
- **Accessibility:**
  - TalkBack support for all text content
  - Page indicators with content descriptions
  - Keyboard navigation (Tab/Shift+Tab, Space to advance)

**Functionality:**
- **Onboarding Flow Control:**
  - ViewModel tracks current page index (0-3)
  - State restoration on configuration changes
  - Analytics logging for each page view duration
- **Permission Handling:**
  - Requests permissions via ActivityResultLauncher
  - Handles "Don't ask again" scenario with explanation dialog
  - Gracefully degrades features if permissions denied
- **Completion Actions:**
  - Sets `is_welcome_completed = true` in SharedPreferences
  - Navigates to MainActivity with FLAG_ACTIVITY_CLEAR_TASK
  - Initializes default settings (grid columns, slideshow interval)
  - Triggers initial resource scan if permissions granted

**Flow:** After completion → `MainActivity` (sets welcome flag, initializes app state)
