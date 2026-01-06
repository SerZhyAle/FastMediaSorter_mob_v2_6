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
$now = Get-Date
$year = $now.ToString("yy")
$month = $now.ToString("MM")
$day = $now.ToString("dd")
$hour = $now.ToString("HH")
$minute = $now.ToString("mm")

$versionName = "2.$($year.Substring(1))$month.$($day)$($hour.Substring(0,1)).$($hour.Substring(1))$minute"
$versionCode = [int]("$year$month$day$hour")

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
} else {
    Write-Host "Build failed with exit code $LASTEXITCODE" -ForegroundColor Red
}

# Return to project root
Set-Location $scriptDir