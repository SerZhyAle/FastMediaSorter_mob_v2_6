import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

// Load local.properties for API keys
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

android {
    namespace = "com.sza.fastmediasorter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sza.fastmediasorter"
        minSdk = 28
        targetSdk = 35
        versionCode = 26010623
        versionName = "2.601.062.355"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject Keys from local.properties
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_CLIENT_ID", "")}\"")
        buildConfigField("String", "DROPBOX_KEY", "\"${localProperties.getProperty("DROPBOX_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    // Security
    implementation(libs.security.crypto)
}
