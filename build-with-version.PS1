$versionCode = "12230103"
$versionName = "2.51.2230.103"
$buildFilePath = "app_v2\build.gradle.kts"
$backupFilePath = "app_v2\build.gradle.kts.backup"

# Create a backup of the build.gradle.kts file
Copy-Item -Path $buildFilePath -Destination $backupFilePath -Force

# Update the build.gradle.kts file with the new version name
(Get-Content $buildFilePath) -replace 'versionName = ".*?"', "versionName = `"$versionName`"" | Set-Content $buildFilePath

# Start the Gradle build process
& gradlew build