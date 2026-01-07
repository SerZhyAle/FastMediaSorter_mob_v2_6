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
    Write-Host "Please start the emulator first." -ForegroundColor Yellow
    exit 1
}

Write-Host "Device found. Ready to capture screenshots." -ForegroundColor Green
Write-Host ""
Write-Host "Navigate to each screen in the app and press Enter to capture..." -ForegroundColor Yellow
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
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review screenshots in $outputDir"
Write-Host "2. Edit/crop if needed (target: 1080 x 2340 px)"
Write-Host "3. Add device frames (optional)"
Write-Host "4. Upload to Play Console"
