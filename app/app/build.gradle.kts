import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

// Load local.properties for API keys
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

// Load keystore.properties for release signing
val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.sza.fastmediasorter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sza.fastmediasorter"
        minSdk = 28
        targetSdk = 35
        versionCode = 26010714
        versionName = "2.60.1071.400"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject Keys from local.properties
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_CLIENT_ID", "")}\"")
        buildConfigField("String", "DROPBOX_KEY", "\"${localProperties.getProperty("DROPBOX_KEY", "")}\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            val storeFile = storeFilePath?.let { file(rootProject.file(it)) }
            
            // Only configure signing if keystore exists
            if (storeFile?.exists() == true) {
                this.storeFile = storeFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Only apply signing if keystore is configured
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
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

// Room schema export for migration testing
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    // DI - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Database - Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.play.services)

    // DataStore
    implementation(libs.datastore.preferences)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.common)

    // Logging
    implementation(libs.timber)

    // Image Loading
    implementation(libs.glide)

    // Media Player (for later epics)
    implementation(libs.exoplayer)
    implementation(libs.exoplayer.ui)
    implementation(libs.media3.session)

    // Network Protocols (Epic 4)
    implementation(libs.smbj)
    implementation(libs.sshj)
    implementation(libs.commons.net)
    implementation(libs.okhttp)
    implementation(libs.gson)

    // Security
    implementation(libs.security.crypto)

    // ML Kit - OCR and Translation
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)
    
    // Tesseract OCR (offline, better Cyrillic support)
    implementation(libs.tesseract4android) {
        exclude(group = "cz.adaptech.tesseract4android", module = "tesseract4android-openmp")
    }

    // Cloud Storage - Google Drive
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Cloud Storage - OneDrive
    implementation(libs.msal)

    // Cloud Storage - Dropbox
    implementation(libs.dropbox.core.sdk)
    implementation(libs.dropbox.android.sdk)
}

// ktlint configuration
ktlint {
    version.set("1.0.1")
    android.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
