# Tactical Plan: Epic 1 - Foundation & Architecture

**Goal**: Establish the "Skeleton" of the application.
**Deliverable**: Compilable Hilt-based project with Database and Base Architecture.

---

## 1. Project Initialization

### 1.1 Android Studio Project Setup
- **Action**: Create new "Empty Activity" project in Android Studio.
- **Config**:
  - Package: `com.sza.fastmediasorter`
  - Language: Kotlin
  - Min SDK: 28 (Android 9.0)
  - Build System: Gradle Kotlin DSL
- **Verification**: Project compiles and runs "Hello World" on emulator.

### 1.2 Version Control Initialization
- **Action**: Initialize Git repository.
- **Config**:
  - Create `.gitignore` tailored for Android (exclude `.gradle`, `/build`, `local.properties`).
  - Perform initial commit.

### 1.3 Dependency Management
- **Action**: Set up Version Catalog.
- **File**: `gradle/libs.versions.toml`
- **Details**: Move all hardcoded versions from `build.gradle.kts` to the TOML file. Define libraries for:
  - Core: `androidx.core`, `appcompat`, `material`
  - DI: `hilt`
  - DB: `room`
  - Async: `coroutines`
  - Log: `timber`

### 1.4 Single-Developer Workflow Protection
- **Action**: Configure Pre-commit Hooks.
- **Reason**: Automate self-discipline since there is no second reviewer.
- **Implementation**:
  - Setup `ktlint` or `spotless` in Gradle.
  - Hook: Fail commit if code formatting is incorrect.
  - Hook: Warn if `TODO` exists in commit.

---

## 2. Core Architecture Wiring

### 2.1 Hilt Dependency Injection
- **Action**: configuration Hilt in project.
- **Steps**:
  - Add Hilt plugins and dependencies in gradle files.
  - Create `FastMediaSorterApp` extending `Application` annotated with `@HiltAndroidApp`.
  - Update `AndroidManifest.xml` to use the custom Application class.

### 2.2 Base UI Logic
- **Action**: Create abstract base classes.
- **Classes**:
  - `BaseActivity<VB>`: Handles ViewBinding setup, abstract `getViewBinding()`.
  - `BaseFragment<VB>`: Handles ViewBinding lifecycle (create in `onViewCreated`, destroy in `onDestroyView`).

### 2.3 Logging & Utils
- **Action**: Configure Timber.
- **Implementation**: Plant `Timber.DebugTree` in `Application.onCreate` only for DEBUG builds.

---

## 3. Data Layer Foundation

### 3.1 Room Database Setup
- **Action**: Initialize Room Database.
- **Implementation**:
  - Create abstract class `AppDatabase` extending `RoomDatabase`.
  - Annotate with `@Database(entities = [...], version = 1)`.
  - **Database Version Strategy**:
    - **CRITICAL**: Always increment version when schema changes (add column, add table, add index).
    - Enable `exportSchema = true` in `@Database` annotation to generate JSON schema files.
    - Store schema history in `app_v2/schemas/` for migration testing.
    - **Never skip versions**: Even for experiments, use sequential version numbers.
    - **Testing**: Use `MigrationTestHelper` to validate all migrations before release.
  
  **Example**:
  ```kotlin
  @Database(
      entities = [
          ResourceEntity::class,
          MediaFileEntity::class,
          FavoriteEntity::class
      ],
      version = 6,  // Increment on every schema change
      exportSchema = true  // Generate schema JSON in app_v2/schemas/
  )
  abstract class AppDatabase : RoomDatabase() {
      abstract fun resourceDao(): ResourceDao
      abstract fun mediaFileDao(): MediaFileDao
      abstract fun favoriteDao(): FavoriteDao
      
      companion object {
          // All migrations defined here
          val MIGRATION_1_2 = object : Migration(1, 2) {
              override fun migrate(database: SupportSQLiteDatabase) {
                  database.execSQL("ALTER TABLE resources ADD COLUMN isDestination INTEGER NOT NULL DEFAULT 0")
              }
          }
          
          val MIGRATION_5_6 = object : Migration(5, 6) {
              override fun migrate(database: SupportSQLiteDatabase) {
                  database.execSQL("ALTER TABLE resources ADD COLUMN cloudProvider TEXT")
                  database.execSQL("ALTER TABLE resources ADD COLUMN cloudFolderId TEXT")
              }
          }
          
          private val ALL_MIGRATIONS = arrayOf(
              MIGRATION_1_2,
              // ... MIGRATION_2_3, MIGRATION_3_4, etc.
              MIGRATION_5_6
          )
      }
  }
  ```
  
  **Migration Testing (See `30_testing_strategy.md` for details)**:
  ```kotlin
  @Test
  fun migrateAll() {
      // Test full migration path from version 1 to current
      helper.createDatabase(TEST_DB, 1).close()
      
      AppDatabase.build(context, TEST_DB).apply {
          openHelper.writableDatabase.close()
      }
  }
  ```
  
  - **Single Dev Policy**: Since there are no merge conflicts, maintain a linear version history. Avoid branching for migrations unless experimental.

### 3.2 Hilt Database Module
- **Action**: Provide Database components via DI.
- **File**: `di/DatabaseModule.kt`
- **Provides**: `@Singleton` functions for `AppDatabase` and all DAOs.

### 3.3 Core Entities
- **Action**: Define initial data models.
- **Entities**:
  - `ResourceEntity`: Represents a folder/server connection (id, path, type, sortOrder).
  - `FileOperationHistoryEntity`: Audit log for file ops (optional for MVP, but good for foundation).

### 3.4 DAO Interfaces
- **Action**: Create Data Access Objects.
- **Interfaces**:
  - `ResourceDao`: `getAll()`, `insert()`, `delete()`, `update()`.

### 3.5 Test Data Strategy ("Golden Set")
- **Action**: Prepare a standard testing dataset.
- **Location**: `mock_data/` in project root (gitignored if large, or documented structure).
- **Contents**:
  - Text file with 255-char filename.
  - Image with heavy resolution (20MB+).
  - Corrupted PDF (0 bytes).
  - Folder with 1000 empty files.
- **Goal**: Ensure consistent local testing conditions.

---

## 4. Domain Contracts

### 4.1 Domain Models
- **Action**: Define pure Kotlin data classes.
- **Models**:
  - `MediaFile`: Unified representation of any file (path, name, size, date, type).
  - `Resource`: Domain representation of `ResourceEntity`.

### 4.2 Repository Interfaces
- **Action**: Define interfaces for data access.
- **Interfaces**:
  - `ResourceRepository`: Expose Flow<List<Resource>> and suspend functions for CRUD.
  - `SettingsRepository`: Interface for preference management.

### 4.3 Result/Error Handling
- **Action**: implementation Result wrapper.
- **Class**: `sealed class Result<out T>` (Success, Failure, Loading).

### 4.4 Error UI Contract
- **Action**: Define standard error presentation rules.
- **Standard**:
  - **Fatal** (No Network/DB Failure): Full-screen Error View with "Retry" button.
  - **Transient** (Copy Failed): Snackbar with "Retry" action.
  - **Background** (Cache update failed): Silent log or subtle Toast.
- **Deliverable**: `ErrorUiState` sealed class shared across ViewModels.
