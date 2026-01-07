# Screenshot Capture Guide for FastMediaSorter v2

## Quick Capture (When Emulator is Running)

### Automated Capture Script:
```powershell
# Run this from project root when emulator is active
.\capture-screenshots.ps1
```

### Manual Capture:
```powershell
# Capture current screen
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
& "$env:ANDROID_HOME\platform-tools\adb.exe" exec-out screencap -p > "store_assets\screenshots\screenshot_${timestamp}.png"
```

## Key Screens to Capture

### 1. Main Screen (Resources List) ⭐
**File:** `01_main_screen.png`
- Show the green toolbar with "Resources" title
- Display the FAB (floating action button) with "+"
- Show 2-3 resource cards if available
- Include search and favorites icons in toolbar

### 2. Resource Grid View ⭐
**File:** `02_grid_view.png`
- Display media files in grid layout
- Show thumbnails loading
- Include filter/sort options
- Show selection controls

### 3. Image Viewer ⭐
**File:** `03_image_viewer.png`
- Display a photo in full screen
- Show viewer controls (previous/next arrows)
- Display filename and position (e.g., "5/20")

### 4. Video Player ⭐
**File:** `04_video_player.png`
- Show video playing
- Display ExoPlayer controls
- Show progress bar and time indicators

### 5. Add Resource Dialog ⭐
**File:** `05_add_resource.png`
- Show resource type selection:
  - Local Folder
  - SMB (Network)
  - SFTP
  - FTP
  - WebDAV
  - Google Drive
  - Dropbox
  - OneDrive

### 6. Network Configuration
**File:** `06_network_config.png`
- Show SMB or SFTP connection form
- Display fields: host, port, username, path
- Show security options (PIN protection)

### 7. Settings Screen
**File:** `07_settings.png`
- Display app settings categories
- Show theme selection
- Cache configuration
- Language options

### 8. Favorites Tab
**File:** `08_favorites.png`
- Show favorites from multiple sources
- Display star icon on items
- Show "No favorites" empty state if needed

## Screenshot Specifications

### Resolution: 1080 x 2340 px (18.5:9 aspect ratio)
- Matches modern Android phones
- Fills entire screen

### Format:
- PNG (24-bit, no alpha)
- JPEG (quality 90+)

### Post-Processing:
1. Crop to exact size if needed
2. Remove sensitive status bar info:
   - Time → Generic "9:41" or remove
   - Battery → Full or remove
   - Network indicators → Wi-Fi only
3. Ensure good lighting (not dark mode for main listings)
4. Add device frame (optional):
   - Use tools like Device Art Generator
   - Or mockup templates from Canva/Figma

## Emulator Setup for Best Screenshots

### Start Emulator:
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd Pixel_5_API_35 -no-snapshot-load
```

### Configure Display:
- Resolution: 1080 x 2340
- DPI: 440 (xxhdpi)
- Remove navigation bar: Settings → System → Gestures → System Navigation → Gesture navigation

### Prepare Test Data:
1. Add local folder with 20+ images
2. Configure SMB connection (mock or real)
3. Add some favorites
4. Ensure good sample media files

## Tools

### Screenshot Capture:
- ADB command (primary method)
- Emulator built-in screenshot (`Ctrl+S`)
- Android Studio Device File Explorer

### Post-Processing:
- **GIMP** (free): Crop, resize, add frames
- **Canva** (free): Add device frames, text overlays
- **Figma** (free): Professional mockups
- **Photopea** (free online): Quick edits

### Device Frames:
- Android Device Art Generator: https://developer.android.com/distribute/marketing-tools/device-art-generator
- Mockuphone: https://mockuphone.com/
- Smartmockups: https://smartmockups.com/

## Best Practices

### DO:
✓ Use high-quality sample images (landscapes, people, objects)
✓ Show realistic usage scenarios
✓ Capture at native resolution
✓ Use consistent lighting/theme across screenshots
✓ Show app functionality clearly
✓ Include call-to-action text overlays (optional)

### DON'T:
✗ Show copyrighted media (use royalty-free images)
✗ Display personal information
✗ Include error messages or crashes
✗ Use pixelated or low-quality images
✗ Show inappropriate content
✗ Include other app logos without permission

## Screenshot Order for Play Store

Upload screenshots in this order (most important first):
1. Resource Grid View (shows main functionality)
2. Image Viewer (core feature)
3. Main Screen (entry point)
4. Video Player (media playback)
5. Add Resource (configuration)
6. Network Config (advanced feature)
7. Favorites (organization)
8. Settings (customization)

## Sample Text Overlays (Optional)

Add these as text overlays on screenshots for better conversion:

### Screenshot 1:
"Browse 1000s of Photos & Videos"
"Lightning-Fast Sorting"

### Screenshot 2:
"Powerful Image Viewer"
"Zoom, Pan, Slideshow"

### Screenshot 3:
"All Your Media in One Place"
"Local • Network • Cloud"

### Screenshot 4:
"Play Videos Seamlessly"
"Support All Formats"

### Screenshot 5:
"Connect to Any Source"
"SMB • SFTP • FTP • Drive • Dropbox"

## Localization

Capture screenshots for each language:
- English (primary)
- Russian (optional)
- Ukrainian (optional)

Or use text overlays that can be translated separately.

## Automated Screenshot Script

Save as `capture-screenshots.ps1` in project root:

```powershell
# FastMediaSorter v2 Screenshot Capture Script
$ErrorActionPreference = "Stop"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$outputDir = "store_assets\screenshots"

# Check device connection
Write-Host "Checking device connection..." -ForegroundColor Cyan
$devices = & $adb devices
if ($devices -notmatch "device$") {
    Write-Host "ERROR: No device/emulator connected!" -ForegroundColor Red
    exit 1
}

Write-Host "Device found. Ready to capture screenshots." -ForegroundColor Green
Write-Host ""
Write-Host "Navigate to each screen and press Enter to capture..." -ForegroundColor Yellow
Write-Host ""

# Capture screenshots with prompts
$screens = @(
    "01_main_screen - Main Resources List",
    "02_grid_view - Media Grid/Browser",
    "03_image_viewer - Image Viewer",
    "04_video_player - Video Player",
    "05_add_resource - Add Resource Dialog",
    "06_network_config - Network Configuration",
    "07_settings - Settings Screen",
    "08_favorites - Favorites Tab"
)

foreach ($screen in $screens) {
    $filename = $screen.Split(" ")[0]
    $description = $screen.Substring($filename.Length + 3)
    
    Write-Host "[$filename] $description" -ForegroundColor Cyan
    Write-Host "Press Enter when ready to capture..." -NoNewline
    Read-Host
    
    $outputPath = Join-Path $outputDir "$filename.png"
    & $adb exec-out screencap -p > $outputPath
    
    if (Test-Path $outputPath) {
        $size = (Get-Item $outputPath).Length / 1KB
        Write-Host "✓ Captured: $filename.png ($([math]::Round($size))KB)" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to capture $filename" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "Screenshot capture complete!" -ForegroundColor Green
Write-Host "Screenshots saved to: $outputDir" -ForegroundColor Cyan
```

## Next Steps

1. Start emulator
2. Launch FastMediaSorter app
3. Prepare test data (add resources with media)
4. Run capture script or manually capture each screen
5. Review and edit screenshots
6. Add device frames (optional)
7. Add text overlays (optional)
8. Upload to Play Console
