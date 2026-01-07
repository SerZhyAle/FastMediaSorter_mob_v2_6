# FastMediaSorter v2 Build Script
# Auto-generates version based on timestamp

# Auto-detect JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    $javaLocations = @(
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "$env:ProgramFiles\Java\jdk-17",
        "$env:ProgramFiles\Java\jdk-21",
        "$env:ProgramFiles\Eclipse Adoptium\jdk-17*",
        "$env:ProgramFiles\Microsoft\jdk-17*",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-17*"
    )
    
    foreach ($loc in $javaLocations) {
        $resolved = Resolve-Path $loc -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved -and (Test-Path "$resolved\bin\java.exe")) {
            $env:JAVA_HOME = $resolved.Path
            Write-Host "Auto-detected JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
            break
        }
    }
    
    if (-not $env:JAVA_HOME) {
        Write-Host "ERROR: Could not find Java installation." -ForegroundColor Red
        Write-Host "Please install JDK 17+ or set JAVA_HOME manually." -ForegroundColor Yellow
        exit 1
    }
}

# Auto-detect ANDROID_HOME if not set
if (-not $env:ANDROID_HOME) {
    $androidLocations = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:USERPROFILE\AppData\Local\Android\Sdk",
        "$env:ProgramFiles\Android\Sdk",
        "C:\Android\Sdk"
    )
    
    foreach ($loc in $androidLocations) {
        if (Test-Path "$loc\platform-tools") {
            $env:ANDROID_HOME = $loc
            Write-Host "Auto-detected ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
            break
        }
    }
    
    if (-not $env:ANDROID_HOME) {
        Write-Host "ERROR: Could not find Android SDK." -ForegroundColor Red
        Write-Host "Please install Android SDK or set ANDROID_HOME manually." -ForegroundColor Yellow
        exit 1
    }
}

# Generate version from current date/time: Y.YM.MDDH.Hmm
# Interpretation adjusted for 2.60.1071.422 example (2026-01-07 14:22):
# Y  (2)  = Decade (2 of 26)
# YM (60) = Year Unit (6) + Month Tens (0)
# MDDH (1071) = Month Unit (1) + Day (07) + Hour Tens (1)
# Hmm (422) = Hour Unit (4) + Minute (22)

$now = Get-Date

# Extract components
$yearFull = $now.ToString("yy") # "26"
$monthFull = $now.ToString("MM") # "01"
$dayFull = $now.ToString("dd")   # "07"
$hourFull = $now.ToString("HH")  # "14"
$minuteFull = $now.ToString("mm") # "22"

# Split components
$y1 = $yearFull.Substring(0, 1)  # "2"
$y2 = $yearFull.Substring(1, 1)  # "6"

$m1 = $monthFull.Substring(0, 1) # "0"
$m2 = $monthFull.Substring(1, 1) # "1"

$h1 = $hourFull.Substring(0, 1)  # "1"
$h2 = $hourFull.Substring(1, 1)  # "4"

# Format: Y.YM.MDDH.Hmm
# Group 1: Y1
# Group 2: Y2 + M1
# Group 3: M2 + DD + H1
# Group 4: H2 + mm
$versionName = "$y1.$y2$m1.$m2$dayFull$h1.$h2$minuteFull"

# VersionCode: YYMMDDHHmm
$versionCode = [int]("$yearFull$monthFull$dayFull$hourFull$minuteFull")

Write-Host "Building FastMediaSorter v$versionName (code: $versionCode)" -ForegroundColor Cyan

# Paths relative to project root
$buildFilePath = "app\app\build.gradle.kts"
$backupFilePath = "app\app\build.gradle.kts.backup"

# Ensure we're in the right directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Check if build file exists
if (-not (Test-Path $buildFilePath)) {
    Write-Host "ERROR: Build file not found at $buildFilePath" -ForegroundColor Red
    exit 1
}

# Create a backup of the build.gradle.kts file
Copy-Item -Path $buildFilePath -Destination $backupFilePath -Force

# Update the build.gradle.kts file with the new version
$content = Get-Content $buildFilePath -Raw
$content = $content -replace 'versionCode = \d+', "versionCode = $versionCode"
$content = $content -replace 'versionName = ".*?"', "versionName = `"$versionName`""
$content | Set-Content $buildFilePath -NoNewline

Write-Host "Updated version in build.gradle.kts" -ForegroundColor Green

# Start the Gradle build process
Set-Location "app"
Write-Host "Starting Gradle build..." -ForegroundColor Yellow
& .\gradlew assembleRelease

# Check build result
if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
}
else {
    Write-Host "Build failed with exit code $LASTEXITCODE" -ForegroundColor Red
}

# Return to project root
Set-Location $scriptDir