# Quick Build Script for Development
# Simplified version without version updates

Write-Host "=== FastMediaSorter v2 - Quick Build ===" -ForegroundColor Cyan

# Auto-detect JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    Write-Host "Detecting Java installation..." -ForegroundColor Yellow
    
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
            Write-Host "Found Java: $env:JAVA_HOME" -ForegroundColor Green
            break
        }
    }
    
    if (-not $env:JAVA_HOME) {
        Write-Host "Could not find Java installation." -ForegroundColor Red
        Write-Host "Please install JDK 17+ or set JAVA_HOME manually." -ForegroundColor Yellow
        Write-Host "Download: https://adoptium.net/" -ForegroundColor Cyan
        exit 1
    }
}

# Auto-detect ANDROID_HOME if not set
if (-not $env:ANDROID_HOME) {
    Write-Host "Detecting Android SDK..." -ForegroundColor Yellow
    
    $androidLocations = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:USERPROFILE\AppData\Local\Android\Sdk",
        "$env:ProgramFiles\Android\Sdk",
        "C:\Android\Sdk"
    )
    
    foreach ($loc in $androidLocations) {
        if (Test-Path "$loc\platform-tools") {
            $env:ANDROID_HOME = $loc
            Write-Host "Found Android SDK: $env:ANDROID_HOME" -ForegroundColor Green
            break
        }
    }
    
    if (-not $env:ANDROID_HOME) {
        Write-Host "Could not find Android SDK." -ForegroundColor Red
        Write-Host "Please install Android SDK or set ANDROID_HOME manually." -ForegroundColor Yellow
        exit 1
    }
}

# Navigate to app directory
Set-Location -Path "$PSScriptRoot\app"

Write-Host "Building debug APK..." -ForegroundColor Yellow

# Run Gradle build
& .\gradlew.bat assembleDebug --console=plain

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful!" -ForegroundColor Green
    
    # Find APK
    $apk = Get-ChildItem -Path "app\build\outputs\apk\debug" -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($apk) {
        $size = [math]::Round($apk.Length / 1MB, 2)
        Write-Host "APK: $($apk.Name) ($size MB)" -ForegroundColor Cyan
        Write-Host "Path: $($apk.FullName)" -ForegroundColor Gray
    }
} else {
    Write-Host "`nBuild failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
