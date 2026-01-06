# Epic 1: Foundation & Architecture - Detailed TODO
*Derived from: [Tactical Plan: Epic 1](../00_strategy_epic1_foundation.md)*
*Reference: [Architecture Patterns](../17_architecture_patterns.md), [Dependencies](../24_dependencies.md), [Database Schema](../26_database_schema.md)*

**Purpose**: Establish complete project foundation with Clean Architecture, dependency injection, database, and resource system.

**Estimated Time**: 2-3 days  
**Prerequisites**: Android Studio Iguana+, JDK 17, Android SDK 35

---

## 1. Project Initialization

### 1.1 Android Studio Project Setup
- [ ] Create new "Empty Activity" project in Android Studio
  - [ ] Package: `com.sza.fastmediasorter`
  - [ ] Language: Kotlin
  - [ ] Min SDK: 28 (Android 9.0)
  - [ ] Target SDK: 35 (Android 15)
  - [ ] Build System: Gradle Kotlin DSL
  - [ ] Namespace: `com.sza.fastmediasorter`
- [ ] **Validate**: Project compiles and runs "Hello World" on emulator
- [ ] **Validate**: Build time < 30 seconds for clean build

### 1.2 Version Control Initialization
- [ ] Initialize Git repository
- [ ] Create `.gitignore` tailored for Android (exclude `.gradle`, `/build`, `local.properties`)
- [ ] Perform initial commit

### 1.3 Dependency Management
- [ ] Set up Version Catalog in `gradle/libs.versions.toml`
- [ ] Move hardcoded versions from `build.gradle.kts` to TOML
- [ ] Define libraries:
  - [ ] Core: `androidx.core`, `appcompat`, `material`
  - [ ] DI: `hilt`
  - [ ] DB: `room`
  - [ ] Async: `coroutines`
  - [ ] Log: `timber`

### 1.4 Single-Developer Workflow Protection
- [ ] Configure Pre-commit Hooks (Manual or via spotless/ktlint)
- [ ] **Validate**: Commit fails if code formatting is incorrect
- [ ] **Validate**: Warnings generated for `TODO` comments

## 2. Core Architecture Wiring

### 2.1 Hilt Dependency Injection
- [ ] Add Hilt plugins and dependencies in gradle files
- [ ] Create `FastMediaSorterApp` class:
  - [ ] Extend `Application`
  - [ ] Annotate with `@HiltAndroidApp`
- [ ] Update `AndroidManifest.xml` to use `FastMediaSorterApp`

### 2.2 Base UI Logic
- [ ] Create `BaseActivity<VB>` abstract class
  - [ ] Implement `getViewBinding()` abstract method
  - [ ] Handle ViewBinding setup
- [ ] Create `BaseFragment<VB>` abstract class
  - [ ] Handle ViewBinding lifecycle in `onViewCreated` and `onDestroyView`

### 2.3 Logging & Utils
- [ ] Configure Timber
  - [ ] Plant `Timber.DebugTree` in `Application.onCreate` (DEBUG only)

## 3. Data Layer Foundation

### 3.1 Room Database Setup
- [ ] Create `AppDatabase` abstract class extending `RoomDatabase`
- [ ] Annotate with `@Database(entities = [...], version = 1)`

### 3.2 Hilt Database Module
- [ ] Create `di/DatabaseModule.kt`
- [ ] Provide `@Singleton` instance of `AppDatabase`
- [ ] Provide DAO instances

### 3.3 Core Entities
- [ ] Define `ResourceEntity` data class
  - [ ] Fields: id, path, type, sortOrder
- [ ] Define `FileOperationHistoryEntity` data class

### 3.4 DAO Interfaces
- [ ] Create `ResourceDao` interface
  - [ ] `getAll()`
  - [ ] `insert()`
  - [ ] `delete()`
  - [ ] `update()`

### 3.5 Test Data Strategy ("Golden Set")
- [ ] Create `mock_data/` directory in project root
- [ ] Create/Place test files:
  - [ ] Text file with 255-char filename
  - [ ] Large image (20MB+)
  - [ ] Corrupted PDF (0 bytes)
  - [ ] Folder with 1000 empty files

## 4. Domain Contracts

### 4.1 Domain Models
- [ ] Define `MediaFile` data class (pure Kotlin)
- [ ] Define `Resource` domain model

### 4.2 Repository Interfaces
- [ ] Define `ResourceRepository` interface
  - [ ] Expose `Flow<List<Resource>>`
  - [ ] Suspend functions for CRUD
- [ ] Define `SettingsRepository` interface

### 4.3 Result/Error Handling
- [ ] Create sealed class `Result<out T>` (Success, Failure, Loading)

### 4.4 Error UI Contract
- [ ] Define `ErrorUiState` sealed class
- [ ] Document error handling rules (Fatal, Transient, Background) in code comments or README
