# 42. Gradle Configuration Templates

**Purpose**: Standardize the build configuration from day one.
**Usage**: Copy these contents into the respective files in the project root.

---

## 1. Root `build.gradle.kts`

```kotlin
// <project_root>/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}
```

---

## 2. App Module `build.gradle.kts`

```kotlin
// <project_root>/app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "com.sza.fastmediasorter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sza.fastmediasorter"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Inject Keys from local.properties
        val properties = java.util.Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${properties.getProperty("GOOGLE_CLIENT_ID", "")}\"")
        buildConfigField("String", "DROPBOX_KEY", "\"${properties.getProperty("DROPBOX_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // Change to release in production
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network & Image
    implementation(libs.retrofit)
    implementation(libs.glide)

    // Media
    implementation(libs.exoplayer)
}
```

---

## 3. Version Catalog (`libs.versions.toml`)

```toml
# <project_root>/gradle/libs.versions.toml
[versions]
agp = "8.3.0"
kotlin = "1.9.22"
coreKtx = "1.12.0"
appcompat = "1.6.1"
material = "1.11.0"
activity = "1.8.2"
constraintlayout = "2.1.4"
hilt = "2.50"
ksp = "1.9.22-1.0.17"
room = "2.6.1"
retrofit = "2.9.0"
glide = "4.16.0"
exoplayer = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "exoplayer" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
parcelize = { id = "org.jetbrains.kotlin.plugin.parcelize", version.ref = "kotlin" }
```

---

## 4. `gradle.properties`

```properties
# <project_root>/gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

# Caching to improve build speed
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.parallel=true

# Suppress warnings
android.defaults.buildfeatures.buildconfig=true
android.nonTransitiveRClass=true
```
