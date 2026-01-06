# 30. Testing Strategy

**Last Updated**: January 6, 2026  
**Purpose**: Comprehensive testing strategy for FastMediaSorter v2.

This document defines testing levels, coverage targets, mock strategies, test organization, and CI/CD integration.

---

## Overview

Testing strategy for production-grade Android app with complex architecture:
- **Clean Architecture**: 3 layers (UI, Domain, Data) require different testing approaches
- **Network Protocols**: SMB/SFTP/FTP/Cloud require integration tests
- **Media Processing**: Image/video operations need instrumentation tests
- **Database**: Room DB requires migration tests

### Testing Pyramid

```
         /\
        /  \  E2E Tests (5%)
       /    \  - Critical user journeys
      /------\  - Slow, expensive
     /        \ UI Tests (15%)
    /          \ - Activity/Fragment
   /            \ - User interactions
  /--------------\ Integration Tests (30%)
 /                \ - Repository + Network
/                  \ - Database + DAO
/--------------------\ Unit Tests (50%)
       Fast, Isolated      - UseCases, ViewModels
                            - Business logic
```

### Coverage Targets

| Layer | Target | Priority Tests |
|-------|--------|----------------|
| **Domain (UseCases)** | 85%+ | All business logic, error paths |
| **Data (Repositories)** | 75%+ | CRUD operations, network calls |
| **UI (ViewModels)** | 80%+ | State management, event emission |
| **UI (Activities/Fragments)** | 60%+ | Critical user flows |

---

## Table of Contents

1. [Unit Tests (Domain Layer)](#1-unit-tests-domain-layer)
2. [Unit Tests (ViewModel)](#2-unit-tests-viewmodel)
3. [Integration Tests (Repository + Network)](#3-integration-tests-repository--network)
4. [Integration Tests (Database)](#4-integration-tests-database)
5. [UI Tests (Instrumentation)](#5-ui-tests-instrumentation)
6. [Test Doubles Strategy](#6-test-doubles-strategy)
7. [Test Organization](#7-test-organization)
8. [CI/CD Integration](#8-cicd-integration)

---

## 1. Unit Tests (Domain Layer)

### Testing UseCases

**Goal**: Verify business logic in isolation (no Android dependencies).

**Pattern**:
```kotlin
class GetMediaFilesUseCaseTest {
    
    // Fake repository (no mocking framework needed)
    private lateinit var fakeRepository: FakeResourceRepository
    private lateinit var useCase: GetMediaFilesUseCase
    
    @Before
    fun setup() {
        fakeRepository = FakeResourceRepository()
        useCase = GetMediaFilesUseCase(fakeRepository)
    }
    
    @Test
    fun `invoke returns files from repository`() = runTest {
        // Arrange
        val resource = MediaResource(id = 1, name = "Test", type = ResourceType.LOCAL, path = "/test")
        val expectedFiles = listOf(
            MediaFile(name = "file1.jpg", path = "/test/file1.jpg", size = 1024, type = FileType.IMAGE),
            MediaFile(name = "file2.mp4", path = "/test/file2.mp4", size = 2048, type = FileType.VIDEO)
        )
        fakeRepository.setFilesForResource(resource, expectedFiles)
        
        // Act
        val result = useCase(resource)
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedFiles, result.getOrNull())
    }
    
    @Test
    fun `invoke handles network errors`() = runTest {
        // Arrange
        val resource = MediaResource(id = 1, name = "SMB", type = ResourceType.SMB, path = "//server/share")
        fakeRepository.setError(ConnectionTimeoutException("server", 445, 30000))
        
        // Act
        val result = useCase(resource)
        
        // Assert
        assertTrue(result is Result.Failure)
        assertTrue(result.exceptionOrNull() is ConnectionTimeoutException)
    }
    
    @Test
    fun `invoke filters by file type`() = runTest {
        // Arrange
        val resource = MediaResource(id = 1, name = "Test", type = ResourceType.LOCAL, path = "/test")
        val allFiles = listOf(
            MediaFile(name = "image.jpg", type = FileType.IMAGE),
            MediaFile(name = "video.mp4", type = FileType.VIDEO),
            MediaFile(name = "doc.pdf", type = FileType.OTHER)
        )
        fakeRepository.setFilesForResource(resource, allFiles)
        
        // Act
        val result = useCase(resource, fileTypeFilter = setOf(FileType.IMAGE))
        
        // Assert
        val files = result.getOrNull()!!
        assertEquals(1, files.size)
        assertEquals("image.jpg", files.first().name)
    }
}
```

**Benefits**:
- Fast (<1ms per test)
- No Android SDK required
- Easy to test edge cases

---

### Testing File Operations UseCase

```kotlin
class FileOperationUseCaseTest {
    
    private lateinit var fakeSourceRepository: FakeResourceRepository
    private lateinit var fakeDestRepository: FakeResourceRepository
    private lateinit var useCase: FileOperationUseCase
    
    @Before
    fun setup() {
        fakeSourceRepository = FakeResourceRepository()
        fakeDestRepository = FakeResourceRepository()
        useCase = FileOperationUseCase(fakeSourceRepository, fakeDestRepository)
    }
    
    @Test
    fun `copyFile copies from source to destination`() = runTest {
        // Arrange
        val sourceResource = MediaResource(id = 1, type = ResourceType.LOCAL, path = "/source")
        val destResource = MediaResource(id = 2, type = ResourceType.LOCAL, path = "/dest")
        val file = MediaFile(name = "file.jpg", path = "/source/file.jpg", size = 1024)
        
        fakeSourceRepository.addFile(sourceResource, file)
        
        // Act
        val result = useCase.copyFile(file, sourceResource, destResource)
        
        // Assert
        assertTrue(result.isSuccess)
        assertTrue(fakeDestRepository.hasFile(destResource, "file.jpg"))
        assertTrue(fakeSourceRepository.hasFile(sourceResource, "file.jpg")) // Original still exists
    }
    
    @Test
    fun `moveFile removes from source`() = runTest {
        // Arrange
        val sourceResource = MediaResource(id = 1, type = ResourceType.LOCAL, path = "/source")
        val destResource = MediaResource(id = 2, type = ResourceType.LOCAL, path = "/dest")
        val file = MediaFile(name = "file.jpg", path = "/source/file.jpg", size = 1024)
        
        fakeSourceRepository.addFile(sourceResource, file)
        
        // Act
        val result = useCase.moveFile(file, sourceResource, destResource)
        
        // Assert
        assertTrue(result.isSuccess)
        assertTrue(fakeDestRepository.hasFile(destResource, "file.jpg"))
        assertFalse(fakeSourceRepository.hasFile(sourceResource, "file.jpg")) // Moved
    }
    
    @Test
    fun `copyFile handles disk full error`() = runTest {
        // Arrange
        val sourceResource = MediaResource(id = 1, type = ResourceType.LOCAL, path = "/source")
        val destResource = MediaResource(id = 2, type = ResourceType.LOCAL, path = "/dest")
        val file = MediaFile(name = "large.mp4", path = "/source/large.mp4", size = 5_000_000_000) // 5GB
        
        fakeDestRepository.setAvailableSpace(1_000_000_000) // 1GB
        
        // Act
        val result = useCase.copyFile(file, sourceResource, destResource)
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DiskFullException)
    }
}
```

---

## 2. Unit Tests (ViewModel)

### Testing State Updates

**Goal**: Verify ViewModel updates StateFlow correctly.

**Setup**:
```kotlin
dependencies {
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'app.cash.turbine:turbine:1.0.0' // Flow testing library
}
```

**Pattern**:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    
    // Test dispatcher
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)
    
    private lateinit var fakeGetMediaFilesUseCase: FakeGetMediaFilesUseCase
    private lateinit var viewModel: BrowseViewModel
    
    @Before
    fun setup() {
        fakeGetMediaFilesUseCase = FakeGetMediaFilesUseCase()
        viewModel = BrowseViewModel(fakeGetMediaFilesUseCase)
    }
    
    @Test
    fun `loadFiles updates uiState with files`() = runTest {
        // Arrange
        val resource = MediaResource(id = 1, name = "Test", type = ResourceType.LOCAL, path = "/test")
        val expectedFiles = listOf(
            MediaFile(name = "file1.jpg", size = 1024),
            MediaFile(name = "file2.mp4", size = 2048)
        )
        fakeGetMediaFilesUseCase.setResult(Result.Success(expectedFiles))
        
        // Act & Assert (using Turbine)
        viewModel.uiState.test {
            // Initial state
            assertEquals(BrowseUiState(), awaitItem())
            
            // Trigger load
            viewModel.loadFiles(resource)
            
            // Loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.error)
            
            // Success state
            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(expectedFiles, successState.files)
            assertNull(successState.error)
        }
    }
    
    @Test
    fun `loadFiles sets error on failure`() = runTest {
        // Arrange
        val resource = MediaResource(id = 1, name = "Test", type = ResourceType.SMB, path = "//server/share")
        val error = ConnectionTimeoutException("server", 445, 30000)
        fakeGetMediaFilesUseCase.setResult(Result.Failure(error))
        
        // Act & Assert
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.loadFiles(resource)
            
            awaitItem() // Loading
            
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(error, errorState.error)
            assertTrue(errorState.files.isEmpty())
        }
    }
}
```

---

### Testing Events (SharedFlow)

```kotlin
@Test
fun `deleteFile emits confirmation event`() = runTest {
    // Arrange
    val file = MediaFile(name = "test.jpg")
    
    // Act & Assert
    viewModel.events.test {
        viewModel.deleteFile(file)
        
        val event = awaitItem()
        assertTrue(event is BrowseEvent.ShowDeleteConfirmation)
        assertEquals(file, (event as BrowseEvent.ShowDeleteConfirmation).file)
    }
}

@Test
fun `confirmDelete emits success toast on success`() = runTest {
    // Arrange
    val file = MediaFile(name = "test.jpg")
    fakeDeleteUseCase.setResult(Result.Success(Unit))
    
    // Act & Assert
    viewModel.events.test {
        viewModel.confirmDelete(file)
        
        val event = awaitItem()
        assertTrue(event is BrowseEvent.ShowToast)
        assertTrue((event as BrowseEvent.ShowToast).message.contains("deleted"))
    }
}
```

---

## 3. Integration Tests (Repository + Network)

### Testing Real Repository with Fake Network Client

**Goal**: Verify Repository correctly calls network client and transforms data.

**Pattern**:
```kotlin
class ResourceRepositoryImplTest {
    
    private lateinit var database: AppDatabase
    private lateinit var resourceDao: ResourceDao
    private lateinit var fakeSmbClient: FakeSmbClient
    private lateinit var repository: ResourceRepositoryImpl
    
    @Before
    fun setup() {
        // Use in-memory database
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        resourceDao = database.resourceDao()
        fakeSmbClient = FakeSmbClient()
        repository = ResourceRepositoryImpl(resourceDao, fakeSmbClient, /* ... */)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `addResource inserts into database`() = runTest {
        // Arrange
        val resource = MediaResource(
            id = 0, // Auto-generate
            name = "Test SMB",
            type = ResourceType.SMB,
            path = "//server/share",
            isDestination = false
        )
        
        // Act
        val id = repository.addResource(resource)
        
        // Assert
        assertTrue(id > 0)
        val retrieved = repository.getResourceById(id)
        assertEquals("Test SMB", retrieved?.name)
    }
    
    @Test
    fun `getAllResources returns Flow of resources`() = runTest {
        // Arrange
        val resource1 = MediaResource(name = "Resource 1", type = ResourceType.LOCAL, path = "/path1")
        val resource2 = MediaResource(name = "Resource 2", type = ResourceType.LOCAL, path = "/path2")
        repository.addResource(resource1)
        repository.addResource(resource2)
        
        // Act
        val resources = repository.getAllResources().first()
        
        // Assert
        assertEquals(2, resources.size)
    }
}
```

---

### Testing Network Operations with Real Protocol

**Goal**: Verify protocol implementation works (SMB/SFTP/FTP).

**Setup**: Requires test server (Docker container or local setup).

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest // Marks as integration test
class SmbClientIntegrationTest {
    
    companion object {
        private const val TEST_SERVER = "192.168.1.100"
        private const val TEST_SHARE = "testshare"
        private const val TEST_USERNAME = "testuser"
        private const val TEST_PASSWORD = "testpass"
    }
    
    private lateinit var smbClient: SmbClient
    
    @Before
    fun setup() {
        smbClient = SmbClient()
    }
    
    @After
    fun teardown() {
        smbClient.disconnect()
    }
    
    @Test
    fun testConnection_success() = runTest {
        // Act
        val result = smbClient.testConnection(TEST_SERVER, TEST_SHARE, TEST_USERNAME, TEST_PASSWORD)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun testConnection_wrongPassword_fails() = runTest {
        // Act
        val result = smbClient.testConnection(TEST_SERVER, TEST_SHARE, TEST_USERNAME, "wrongpass")
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun listFiles_returnsFiles() = runTest {
        // Arrange
        smbClient.connect(TEST_SERVER, TEST_SHARE, TEST_USERNAME, TEST_PASSWORD)
        
        // Act
        val result = smbClient.listFiles("/")
        
        // Assert
        assertTrue(result is SmbResult.Success)
        val files = (result as SmbResult.Success).data
        assertTrue(files.isNotEmpty())
    }
}
```

**Note**: Integration tests are slow (network latency) and require infrastructure. Run in CI only.

---

## 4. Integration Tests (Database)

### Testing Room Migrations

**Critical**: Database migration failures can cause data loss.

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    fun migrate1To2() {
        // Create DB version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO resources VALUES(1, 'Test', 'LOCAL', '/test', '#FF0000', 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL)")
            close()
        }
        
        // Migrate to version 2
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        
        // Verify data integrity
        helper.getMigrationDatabase(TEST_DB, 2).apply {
            val cursor = query("SELECT * FROM resources WHERE id = 1")
            assertTrue(cursor.moveToFirst())
            assertEquals("Test", cursor.getString(cursor.getColumnIndex("name")))
            close()
        }
    }
    
    @Test
    fun migrateAll() {
        // Test complete migration path
        helper.createDatabase(TEST_DB, 1).close()
        
        AppDatabase.build(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TEST_DB
        ).apply {
            openHelper.writableDatabase.close()
        }
    }
}
```

### Migration Fallback Strategy

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .addMigrations(*ALL_MIGRATIONS)
    .fallbackToDestructiveMigration()  // ONLY for debug builds
    .addCallback(object : RoomDatabase.Callback() {
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            Timber.e("Destructive migration occurred!")
            // Log to crash reporting
            // Show user warning about data loss
        }
    })
    .build()
```

### Pre-Migration Backup

```kotlin
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun backupBeforeMigration(): File? {
        try {
            val dbFile = context.getDatabasePath("app.db")
            val backupFile = File(context.filesDir, "backups/app_pre_migration_${System.currentTimeMillis()}.db")
            backupFile.parentFile?.mkdirs()
            
            dbFile.copyTo(backupFile, overwrite = true)
            Timber.d("Database backed up to: ${backupFile.path}")
            
            return backupFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to backup database")
            return null
        }
    }
}
```

---

## 4. Integration Tests (Database)

### Testing Room DAO

**Goal**: Verify SQL queries work correctly.

```kotlin
@RunWith(AndroidJUnit4::class)
class ResourceDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var resourceDao: ResourceDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        resourceDao = database.resourceDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertAndGetById() = runTest {
        // Arrange
        val resource = ResourceEntity(
            id = 1,
            name = "Test",
            type = ResourceType.LOCAL,
            path = "/test",
            displayOrder = 0,
            isDestination = false
        )
        
        // Act
        resourceDao.insert(resource)
        val retrieved = resourceDao.getById(1).first()
        
        // Assert
        assertEquals("Test", retrieved?.name)
        assertEquals(ResourceType.LOCAL, retrieved?.type)
    }
    
    @Test
    fun getAll_returnsFlowOfResources() = runTest {
        // Arrange
        resourceDao.insert(ResourceEntity(id = 1, name = "R1", type = ResourceType.LOCAL, path = "/1", displayOrder = 0))
        resourceDao.insert(ResourceEntity(id = 2, name = "R2", type = ResourceType.LOCAL, path = "/2", displayOrder = 1))
        
        // Act
        val resources = resourceDao.getAll().first()
        
        // Assert
        assertEquals(2, resources.size)
        assertEquals("R1", resources[0].name)
        assertEquals("R2", resources[1].name)
    }
    
    @Test
    fun deleteById_removesResource() = runTest {
        // Arrange
        resourceDao.insert(ResourceEntity(id = 1, name = "Test", type = ResourceType.LOCAL, path = "/test", displayOrder = 0))
        
        // Act
        resourceDao.deleteById(1)
        val retrieved = resourceDao.getById(1).first()
        
        // Assert
        assertNull(retrieved)
    }
    
    @Test
    fun updateDisplayOrder_changesOrder() = runTest {
        // Arrange
        resourceDao.insert(ResourceEntity(id = 1, name = "Test", type = ResourceType.LOCAL, path = "/test", displayOrder = 0))
        
        // Act
        resourceDao.updateDisplayOrder(1, 5)
        val retrieved = resourceDao.getById(1).first()
        
        // Assert
        assertEquals(5, retrieved?.displayOrder)
    }
}
```

---

### Testing Database Migrations

**Goal**: Ensure schema migrations don't lose data.

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    fun migrate1To2_containsCorrectData() {
        // Create DB version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO resources (id, name, type, path, displayOrder, isDestination)
                VALUES (1, 'Test', 'LOCAL', '/test', 0, 0)
            """)
            close()
        }
        
        // Run migration 1→2
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        
        // Verify data preserved
        helper.openDatabase(TEST_DB, 2).apply {
            val cursor = query("SELECT * FROM resources WHERE id = 1")
            assertTrue(cursor.moveToFirst())
            assertEquals("Test", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            cursor.close()
            close()
        }
    }
}
```

---

## 5. UI Tests (Instrumentation)

### Testing Activities with Espresso

**Goal**: Verify critical user flows work end-to-end.

**Setup**:
```kotlin
dependencies {
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'com.google.dagger:hilt-android-testing:2.50'
}
```

**Pattern**:
```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class MainActivityTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun addLocalResource_showsInList() {
        // Click FAB
        onView(withId(R.id.fab_add_resource)).perform(click())
        
        // Select "Local Folder"
        onView(withText("Local Folder")).perform(click())
        
        // Enter name
        onView(withId(R.id.et_resource_name)).perform(typeText("Test Resource"))
        
        // Enter path
        onView(withId(R.id.et_resource_path)).perform(typeText("/sdcard/Pictures"))
        
        // Click Save
        onView(withId(R.id.btn_save)).perform(click())
        
        // Verify resource appears in list
        onView(withText("Test Resource")).check(matches(isDisplayed()))
    }
    
    @Test
    fun clickResource_opensBrowseActivity() {
        // Add test resource
        // ... (setup code)
        
        // Click resource
        onView(withText("Test Resource")).perform(click())
        
        // Verify BrowseActivity opened
        onView(withId(R.id.recycler_files)).check(matches(isDisplayed()))
    }
}
```

---

### Testing with Hilt Test Modules

**Goal**: Replace real dependencies with fakes in UI tests.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideFakeResourceRepository(): ResourceRepository = FakeResourceRepository().apply {
        // Pre-populate with test data
        addResource(MediaResource(id = 1, name = "Test Resource", type = ResourceType.LOCAL, path = "/test"))
    }
}

@UninstallModules(RepositoryModule::class) // Remove production module
@HiltAndroidTest
class MainActivityWithFakeDataTest {
    // Tests run with fake repository
}
```

---

## 6. Test Doubles Strategy

### Fake vs Mock

| Aspect | Fake | Mock |
|--------|------|------|
| Implementation | Working implementation (simplified) | Framework-generated (Mockito) |
| Behavior Verification | State verification | Interaction verification |
| Reusability | Reusable across tests | Created per test |
| Example | `FakeResourceRepository` | `mock<ResourceRepository>()` |

**Recommendation**: Prefer **Fakes** for repositories (state verification). Use **Mocks** for complex interactions.

---

### Fake Repository Pattern

```kotlin
class FakeResourceRepository : ResourceRepository {
    
    private val resources = mutableListOf<MediaResource>()
    private val files = mutableMapOf<Long, List<MediaFile>>()
    private var error: FmsException? = null
    
    override fun getAllResources(): Flow<List<MediaResource>> = flow {
        error?.let { throw it }
        emit(resources.toList())
    }
    
    override suspend fun addResource(resource: MediaResource): Long {
        error?.let { throw it }
        val id = resources.size + 1L
        resources.add(resource.copy(id = id))
        return id
    }
    
    override suspend fun deleteResource(id: Long) {
        error?.let { throw it }
        resources.removeIf { it.id == id }
    }
    
    // Test helpers
    fun setError(e: FmsException) {
        error = e
    }
    
    fun clearError() {
        error = null
    }
    
    fun setFilesForResource(resource: MediaResource, files: List<MediaFile>) {
        this.files[resource.id] = files
    }
    
    fun getFilesForResource(resourceId: Long): List<MediaFile> = files[resourceId] ?: emptyList()
}
```

**Benefits**:
- No mocking framework needed
- Fast execution
- Reusable in many tests
- Easy to debug

---

### Mock with Mockito

```kotlin
dependencies {
    testImplementation 'org.mockito:mockito-core:5.5.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.1.0'
}

@Test
fun `deleteFile calls repository delete`() = runTest {
    // Arrange
    val mockRepository = mock<FileRepository> {
        on { deleteFile(any()) } doReturn Result.Success(Unit)
    }
    val viewModel = BrowseViewModel(mockRepository)
    
    // Act
    viewModel.confirmDelete(testFile)
    
    // Assert
    verify(mockRepository).deleteFile(testFile)
}
```

**Use Cases**:
- Verify interaction (method called with specific args)
- Complex setup (many dependencies)

---

## 7. Test Organization

### Directory Structure

```
app_v2/
├── src/
│   ├── main/
│   │   └── java/com/apemax/fastmediasorter/
│   ├── test/  # Unit tests (no Android dependencies)
│   │   └── java/com/apemax/fastmediasorter/
│   │       ├── domain/
│   │       │   ├── usecase/
│   │       │   │   ├── GetMediaFilesUseCaseTest.kt
│   │       │   │   ├── FileOperationUseCaseTest.kt
│   │       │   │   └── ...
│   │       ├── ui/
│   │       │   ├── main/
│   │       │   │   └── MainViewModelTest.kt
│   │       │   ├── browse/
│   │       │   │   └── BrowseViewModelTest.kt
│   │       └── util/
│   │           └── RetryPolicyTest.kt
│   └── androidTest/  # Integration & UI tests (requires device/emulator)
│       └── java/com/apemax/fastmediasorter/
│           ├── data/
│           │   ├── local/
│           │   │   ├── dao/
│           │   │   │   ├── ResourceDaoTest.kt
│           │   │   │   └── ...
│           │   │   └── migration/
│           │   │       └── MigrationTest.kt
│           │   └── repository/
│           │       └── ResourceRepositoryImplTest.kt
│           ├── network/
│           │   ├── SmbClientIntegrationTest.kt
│           │   ├── SftpClientIntegrationTest.kt
│           │   └── ...
│           └── ui/
│               ├── main/
│               │   └── MainActivityTest.kt
│               └── browse/
│                   └── BrowseActivityTest.kt
```

---

### Test Naming Conventions

**Pattern**: `methodName_stateUnderTest_expectedBehavior`

**Examples**:
```kotlin
// ✅ GOOD
@Test
fun addResource_validInput_returnsId()

@Test
fun loadFiles_networkTimeout_setsErrorState()

@Test
fun deleteFile_permissionDenied_emitsErrorEvent()

// ❌ BAD
@Test
fun test1() // What does this test?

@Test
fun testDelete() // What scenario?
```

---

### Test Categories (JUnit5 Tags)

```kotlin
// Fast unit tests (run always)
@Tag("unit")
class GetMediaFilesUseCaseTest

// Slow integration tests (run in CI only)
@Tag("integration")
@Tag("network")
class SmbClientIntegrationTest

// UI tests (run nightly)
@Tag("ui")
class MainActivityTest
```

**Run specific category**:
```bash
./gradlew test --tests "*" --include-tags unit
./gradlew connectedAndroidTest --tests "*" --include-tags integration
```

---

## 8. CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew :app_v2:testDebugUnitTest
    
    - name: Generate coverage report
      run: ./gradlew :app_v2:jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./app_v2/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: app_v2/build/test-results/

  instrumentation-test:
    runs-on: macos-latest # macOS for faster emulator
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run instrumentation tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 34
        target: google_apis
        arch: x86_64
        script: ./gradlew :app_v2:connectedDebugAndroidTest
    
    - name: Upload instrumentation test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: instrumentation-test-results
        path: app_v2/build/reports/androidTests/
```

---

### Coverage Configuration (JaCoCo)

**app_v2/build.gradle.kts**:
```kotlin
plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(files(
        fileTree("build/intermediates/javac/debug") {
            exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*_Factory.*",
                "**/*_MembersInjector.*"
            )
        }
    ))
    executionData.setFrom(files("build/jacoco/testDebugUnitTest.exec"))
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
```

**Run locally**:
```bash
./gradlew :app_v2:jacocoTestReport
# Open: app_v2/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## Best Practices Summary

### ✅ DO

1. **Write Tests First for Critical Logic**
```kotlin
@Test
fun `deleteFile moves to trash instead of permanent delete`()
```

2. **Use Fakes for Repositories**
```kotlin
class FakeResourceRepository : ResourceRepository { /* ... */ }
```

3. **Test Error Paths**
```kotlin
@Test
fun `loadFiles handles network timeout`()
```

4. **Use Descriptive Test Names**
```kotlin
@Test
fun addResource_duplicatePath_throwsDuplicateResourceException()
```

5. **Isolate Tests (No Shared State)**
```kotlin
@Before
fun setup() {
    repository = FakeResourceRepository() // Fresh instance per test
}
```

---

### ❌ DON'T

1. **Don't Test Android Framework**
```kotlin
// ❌ BAD
@Test
fun `TextView setText works`() // Android already tested this
```

2. **Don't Use Real Network in Unit Tests**
```kotlin
// ❌ BAD
@Test
fun test() {
    httpClient.get("https://api.example.com") // Slow, flaky
}
```

3. **Don't Ignore Flaky Tests**
```kotlin
// ❌ BAD
@Ignore("Flaky test") // Fix or delete, don't ignore
```

4. **Don't Test Implementation Details**
```kotlin
// ❌ BAD
verify(repository).privateHelperMethod() // Test public API only
```

5. **Don't Write Overly Complex Tests**
```kotlin
// ❌ BAD: 200 lines of setup for 1 assertion
```

---

## Reference Files

### Source Code Examples
- **Unit Tests**: `app_v2/src/test/java/`
- **Integration Tests**: `app_v2/src/androidTest/java/`
- **Fakes**: `app_v2/src/test/java/com/apemax/fastmediasorter/fake/`

### Related Documents
- [27. API Contracts & Interfaces](27_api_contracts.md) - Testing section for each API
- [29. Error Handling Strategy](29_error_handling.md) - Testing error scenarios
- [26. Database Schema & Migrations](26_database_schema.md) - DAO testing strategy

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
