
Write-Host "Detecting Java installation..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

Write-Host "Running Gradle with --info..."
& .\gradlew.bat :app:app:assembleDebug --info
