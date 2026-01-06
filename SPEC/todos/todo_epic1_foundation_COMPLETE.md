# Epic 1: Foundation & Architecture - Complete Implementation TODO
*Derived from: [Tactical Plan: Epic 1](../00_strategy_epic1_foundation.md)*  
*Reference: [Architecture Patterns](../17_architecture_patterns.md), [Dependencies](../24_dependencies.md), [Database Schema](../26_database_schema.md)*

**Purpose**: Establish complete project foundation with Clean Architecture, dependency injection, database, and resource system ready for production.

**Estimated Time**: 2-3 days  
**Prerequisites**: Android Studio Iguana+, JDK 17, Android SDK 35  
**Output**: Compilable app with architecture skeleton, database, and resource management

---

## 1. Project Initialization

### 1.1 Android Studio Project Setup
- [ ] Create new "Empty Activity" project
  - Package: `com.sza.fastmediasorter`
  - Language: Kotlin
  - Min SDK: 28 (Android 9.0), Target SDK: 35
  - Build System: Gradle Kotlin DSL
- [ ] **Validate**: Build succeeds, app launches on emulator (Build time < 30s)

### 1.2 Version Control
- [ ] Initialize Git: `git init`
- [ ] Create `.gitignore`:
```
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
keystore.properties
test_media/
```
- [ ] Initial commit: `git commit -m "feat: Initial Android project"`
- [ ] **Validate**: `git status` shows clean tree

### 1.3 Gradle Configuration (Complete Build Setup)
- [ ] Create `gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "1.9.24"
hilt = "2.50"
room = "2.6.1"
coroutines = "1.8.0"
timber = "5.0.1"
glide = "4.16.0"
exoplayer = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.12.0" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version = "1.6.1" }
material = { group = "com.google.android.material", name = "material", version = "1.12.0" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version = "2.7.0" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version = "2.7.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] Update `app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
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
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    buildFeatures {
        viewBinding = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.timber)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
}
```
- [ ] **Validate**: Gradle sync succeeds, all dependencies resolve

---

## 2. Core Architecture Wiring

### 2.1 Application Class & Hilt Setup
- [ ] Create `FastMediaSorterApp.kt`:
```kotlin
package com.sza.fastmediasorter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FastMediaSorterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("FastMediaSorter initialized - Version ${BuildConfig.VERSION_NAME}")
    }
}
```

- [ ] Update `AndroidManifest.xml`:
```xml
<application
    android:name=".FastMediaSorterApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.FastMediaSorter">
```
- [ ] **Validate**: App launches, log shows "FastMediaSorter initialized"

### 2.2 Base UI Classes (ViewBinding Support)
- [ ] Create `ui/base/BaseActivity.kt`:
```kotlin
package com.sza.fastmediasorter.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("ViewBinding accessed before onCreate or after onDestroy")
    
    abstract fun getViewBinding(): VB
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
```

- [ ] Create `ui/base/BaseFragment.kt`:
```kotlin
package com.sza.fastmediasorter.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("ViewBinding accessed outside lifecycle")
    
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```
- [ ] **Validate**: Base classes compile, ViewBinding lifecycle managed properly

---

## 3. Data Layer Foundation (Room Database)

### 3.1 Database Entities
- [ ] Create `data/db/entity/ResourceEntity.kt`:
```kotlin
package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val type: String, // LOCAL, SMB, SFTP, FTP, CLOUD
    val credentialsId: String? = null, // FK to network_credentials
    val sortMode: String = "NAME_ASC",
    val displayMode: String = "LIST",
    val displayOrder: Int = 0,
    val isDestination: Boolean = false,
    val destinationOrder: Int = -1,
    val destinationColor: Int = 0xFF4CAF50.toInt(),
    val createdDate: Long = System.currentTimeMillis(),
    val lastAccessedDate: Long = System.currentTimeMillis()
)
```

- [ ] Create `data/db/entity/NetworkCredentialsEntity.kt`:
```kotlin
package com.sza.fastmediasorter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "network_credentials")
data class NetworkCredentialsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val credentialId: String = UUID.randomUUID().toString(),
    val type: String, // SMB, SFTP, FTP
    val server: String,
    val port: Int,
    val username: String,
    val password: String, // TODO: Encrypt with EncryptedSharedPreferences
    val domain: String = "",
    val shareName: String? = null,
    val createdDate: Long = System.currentTimeMillis()
)
```

### 3.2 DAO Interfaces
- [ ] Create `data/db/dao/ResourceDao.kt`:
```kotlin
package com.sza.fastmediasorter.data.db.dao

import androidx.room.*
import com.sza.fastmediasorter.data.db.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY displayOrder, name")
    fun getAllFlow(): Flow<List<ResourceEntity>>
    
    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getById(id: Long): ResourceEntity?
    
    @Insert
    suspend fun insert(resource: ResourceEntity): Long
    
    @Update
    suspend fun update(resource: ResourceEntity)
    
    @Delete
    suspend fun delete(resource: ResourceEntity)
    
    @Query("SELECT * FROM resources WHERE isDestination = 1 ORDER BY destinationOrder")
    fun getDestinationsFlow(): Flow<List<ResourceEntity>>
}
```

- [ ] Create `data/db/dao/NetworkCredentialsDao.kt`:
```kotlin
package com.sza.fastmediasorter.data.db.dao

import androidx.room.*
import com.sza.fastmediasorter.data.db.entity.NetworkCredentialsEntity

@Dao
interface NetworkCredentialsDao {
    @Query("SELECT * FROM network_credentials WHERE credentialId = :credentialId")
    suspend fun getByCredentialId(credentialId: String): NetworkCredentialsEntity?
    
    @Insert
    suspend fun insert(credentials: NetworkCredentialsEntity): Long
    
    @Update
    suspend fun update(credentials: NetworkCredentialsEntity)
    
    @Delete
    suspend fun delete(credentials: NetworkCredentialsEntity)
}
```

### 3.3 AppDatabase
- [ ] Create `data/db/AppDatabase.kt`:
```kotlin
package com.sza.fastmediasorter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sza.fastmediasorter.data.db.dao.NetworkCredentialsDao
import com.sza.fastmediasorter.data.db.dao.ResourceDao
import com.sza.fastmediasorter.data.db.entity.NetworkCredentialsEntity
import com.sza.fastmediasorter.data.db.entity.ResourceEntity

@Database(
    entities = [
        ResourceEntity::class,
        NetworkCredentialsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao
    abstract fun networkCredentialsDao(): NetworkCredentialsDao
}
```

### 3.4 Hilt Database Module
- [ ] Create `di/DatabaseModule.kt`:
```kotlin
package com.sza.fastmediasorter.di

import android.content.Context
import androidx.room.Room
import com.sza.fastmediasorter.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fastmediasorter_v2.db"
        )
            .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
            .build()
    }
    
    @Provides
    fun provideResourceDao(database: AppDatabase) = database.resourceDao()
    
    @Provides
    fun provideNetworkCredentialsDao(database: AppDatabase) = database.networkCredentialsDao()
}
```
- [ ] **Validate**: Database compiles, Hilt generates module code

---

## 4. Domain Layer (Clean Architecture)

### 4.1 Domain Models (Pure Kotlin)
- [ ] Create `domain/model/MediaFile.kt`:
```kotlin
package com.sza.fastmediasorter.domain.model

import java.util.Date

data class MediaFile(
    val path: String,
    val name: String,
    val size: Long,
    val date: Date,
    val type: MediaType,
    val duration: Long? = null, // milliseconds for video/audio
    val thumbnailUrl: String? = null // for cloud files
)

enum class MediaType {
    IMAGE, VIDEO, AUDIO, GIF, PDF, TXT, EPUB, OTHER
}
```

- [ ] Create `domain/model/Resource.kt`:
```kotlin
package com.sza.fastmediasorter.domain.model

data class Resource(
    val id: Long,
    val name: String,
    val path: String,
    val type: ResourceType,
    val isDestination: Boolean = false,
    val destinationOrder: Int = -1
)

enum class ResourceType {
    LOCAL, SMB, SFTP, FTP, GOOGLE_DRIVE, ONEDRIVE, DROPBOX
}
```

### 4.2 Repository Interfaces
- [ ] Create `domain/repository/ResourceRepository.kt`:
```kotlin
package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface ResourceRepository {
    fun getAllResourcesFlow(): Flow<List<Resource>>
    suspend fun getResourceById(id: Long): Resource?
    suspend fun insertResource(resource: Resource): Long
    suspend fun updateResource(resource: Resource)
    suspend fun deleteResource(resource: Resource)
    fun getDestinationsFlow(): Flow<List<Resource>>
}
```

### 4.3 Result Wrapper
- [ ] Create `domain/model/Result.kt`:
```kotlin
package com.sza.fastmediasorter.domain.model

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, throwable)
        return this
    }
}
```

---

## 5. Resources & Theming

### 5.1 Dimensions (res/values/dimens.xml)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Spacing (Compact Design) -->
    <dimen name="spacing_tiny">2dp</dimen>
    <dimen name="spacing_small">4dp</dimen>
    <dimen name="spacing_normal">8dp</dimen>
    <dimen name="spacing_medium">16dp</dimen>
    <dimen name="spacing_large">24dp</dimen>
    
    <!-- List Items -->
    <dimen name="list_item_padding">4dp</dimen>
    <dimen name="list_padding">8dp</dimen>
    
    <!-- Touch Targets -->
    <dimen name="touch_target_min">48dp</dimen>
    <dimen name="icon_size">36dp</dimen>
</resources>
```

### 5.2 Localization (3 Languages)
- [ ] Create `res/values/strings.xml` (English):
```xml
<resources>
    <string name="app_name">FastMediaSorter</string>
    <string name="welcome_title">Welcome</string>
    <string name="permissions_required">Storage permissions required</string>
    <string name="grant_permissions">Grant Permissions</string>
    <string name="error_unknown">Unknown error occurred</string>
    <string name="retry">Retry</string>
</resources>
```

- [ ] Create `res/values-ru/strings.xml` (Russian):
```xml
<resources>
    <string name="app_name">FastMediaSorter</string>
    <string name="welcome_title">Добро пожаловать</string>
    <string name="permissions_required">Требуются разрешения хранилища</string>
    <string name="grant_permissions">Предоставить разрешения</string>
    <string name="error_unknown">Произошла неизвестная ошибка</string>
    <string name="retry">Повторить</string>
</resources>
```

- [ ] Create `res/values-uk/strings.xml` (Ukrainian):
```xml
<resources>
    <string name="app_name">FastMediaSorter</string>
    <string name="welcome_title">Ласкаво просимо</string>
    <string name="permissions_required">Потрібні дозволи сховища</string>
    <string name="grant_permissions">Надати дозволи</string>
    <string name="error_unknown">Сталася невідома помилка</string>
    <string name="retry">Повторити</string>
</resources>
```
- [ ] **Validate**: Change device language, verify strings update

### 5.3 Material Theme (res/values/themes.xml)
```xml
<resources>
    <style name="Theme.FastMediaSorter" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">#4CAF50</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorSecondary">#FFC107</item>
        <item name="colorError">#F44336</item>
    </style>
</resources>
```

---

## 6. Test Data ("Golden Set")
- [ ] Create `test_media/` directory (add to .gitignore)
- [ ] Prepare edge case files:
  - `long_filename_255_chars.txt` (255-character filename)
  - `large_image_20MB.jpg` (20MB+ image)
  - `corrupted.pdf` (0 bytes PDF)
  - `unicode_тест_中文_العربية.txt`
  - `stress_test/` folder with 1000 small files
- [ ] **Validate**: App handles all test files without crashes

---

## 7. Completion Checklist

**Architecture**:
- [ ] Clean Architecture layers (UI/Domain/Data) established
- [ ] Hilt DI configured and working
- [ ] Room database with ResourceEntity & NetworkCredentialsEntity

**Code Quality**:
- [ ] Base classes for Activity/Fragment with ViewBinding
- [ ] Timber logging (DEBUG only)
- [ ] All public APIs have KDoc comments

**Resources**:
- [ ] Material 3 theme with semantic colors
- [ ] 3 language localizations (EN/RU/UK)
- [ ] Dimensions file (no hardcoded dp values)

**Build**:
- [ ] Project builds successfully
- [ ] Version Catalog centralized in TOML
- [ ] ViewBinding enabled and working

**Success Criteria**: Compilable app with architecture skeleton, ready for Epic 2 (Local File Management).
