# 32. Performance Metrics & Optimization

**Last Updated**: January 6, 2026  
**Purpose**: Performance metrics, benchmarks, and optimization guidelines for FastMediaSorter v2.

This document defines target timings, memory limits, profiling strategies, and optimization techniques.

---

## Overview

Performance is critical for media management app handling:
- **Large file lists**: 1000+ files per folder
- **Network operations**: SMB/SFTP/FTP with variable latency
- **Image/video processing**: Thumbnails, editing, playback
- **Database queries**: Frequent reads/writes to Room DB

### Performance Goals

| Category | Metric | Target | Max Acceptable |
|----------|--------|--------|----------------|
| **App Launch** | Cold start | < 1.5s | < 2.5s |
| **Image Thumbnails** | Loading time | < 300ms | < 500ms |
| **Database Queries** | Main thread queries | < 16ms (1 frame) | < 32ms |
| **Network Connection** | SMB/SFTP connect | < 2s | < 5s |
| **File Operations** | Copy 10MB file | < 3s (local) | < 10s (network) |
| **Memory Usage** | 1000 files in list | < 200MB | < 350MB |
| **APK Size** | Release build | < 25MB | < 35MB |

---

## Table of Contents

1. [App Startup Optimization](#1-app-startup-optimization)
2. [Image Loading Performance](#2-image-loading-performance)
3. [Database Query Optimization](#3-database-query-optimization)
4. [Network Performance](#4-network-performance)
5. [Memory Management](#5-memory-management)
6. [APK Size Optimization](#6-apk-size-optimization)
7. [Profiling Tools](#7-profiling-tools)
8. [Benchmarking Strategy](#8-benchmarking-strategy)

---

## 1. App Startup Optimization

### Cold Start Measurement

**Baseline**: 1.2s on Pixel 6 Pro (Android 13)

**Breakdown**:
- Application.onCreate: 200ms
- MainActivity.onCreate: 150ms
- First frame: 350ms
- Room DB initialization: 300ms
- Layout inflation: 200ms

---

### Optimization Techniques

#### 1.1. Lazy Initialization

**Problem**: Heavy objects created on app startup.

**Solution**: Defer non-critical initialization.

```kotlin
@HiltAndroidApp
class FastMediaSorterApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Critical: Logging, crash reporting
        initializeLogging()
        initializeCrashReporting()
        
        // Deferred: Network clients, image loader
        GlobalScope.launch(Dispatchers.IO) {
            delay(1000) // Wait 1s after launch
            initializeNetworkClients()
            preloadImageCache()
        }
    }
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }
}
```

---

#### 1.2. Content Provider Elimination

**Problem**: ContentProvider initialization blocks app startup.

**Solution**: Use Hilt instead of AndroidX Startup.

```kotlin
// ❌ BAD: ContentProvider initialization
class MyInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Blocks startup
        heavyInitialization()
    }
}

// ✅ GOOD: Hilt eager initialization
@InstallIn(SingletonComponent::class)
@Module
object EagerInitModule {
    @Provides
    @EagerInit
    fun provideEagerInit(): EagerInit = EagerInit()
}
```

---

#### 1.3. StrictMode Detection (Debug Only)

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
    
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
}
```

**Detects**:
- Disk reads/writes on main thread
- Network calls on main thread
- Leaked activities/SQLite cursors

---

### Target: < 1.5s Cold Start

**Checklist**:
- [ ] No disk I/O on main thread
- [ ] No network calls before first frame
- [ ] Room DB queries off main thread
- [ ] Lazy initialization for non-critical components
- [ ] ViewBinding (faster than findViewById)

---

## 2. Image Loading Performance

### Glide Configuration

**Custom AppGlideModule**:
```kotlin
@GlideModule
class FastMediaSorterGlideModule : AppGlideModule() {
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Disk cache size (2GB default)
        val cacheSize = SettingsRepository.getThumbnailCacheSize()
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "image_cache", cacheSize))
        
        // Memory cache (10% of available heap)
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f) // Cache 2 screens of images
            .build()
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        
        // Bitmap pool (reuse bitmaps)
        builder.setBitmapPool(LruBitmapPool(calculator.bitmapPoolSize.toLong()))
        
        // Log level (debug only)
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(Log.DEBUG)
        }
    }
    
    override fun isManifestParsingEnabled(): Boolean = false // Faster startup
}
```

---

### Thumbnail Loading Pattern

**Target**: < 300ms per thumbnail

```kotlin
// Standard image loading
Glide.with(context)
    .load(file.path)
    .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache thumbnails
    .placeholder(R.drawable.placeholder_image)
    .error(R.drawable.error_image)
    .override(200, 200) // Resize to thumbnail size
    .centerCrop()
    .into(imageView)

// Network image (SMB/SFTP/Cloud)
Glide.with(context)
    .load(SmbFile(file.path, credentials)) // Custom ModelLoader
    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache original + thumbnail
    .timeout(10_000) // 10s timeout
    .into(imageView)
```

---

### Network Image Custom ModelLoader

```kotlin
class SmbModelLoader(
    private val smbClient: SmbClient
) : ModelLoader<SmbFile, InputStream> {
    
    override fun buildLoadData(
        model: SmbFile,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(
            ObjectKey(model.path),
            SmbDataFetcher(model, smbClient)
        )
    }
    
    override fun handles(model: SmbFile): Boolean = true
}

class SmbDataFetcher(
    private val file: SmbFile,
    private val smbClient: SmbClient
) : DataFetcher<InputStream> {
    
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val stream = smbClient.openFile(file.path)
            callback.onDataReady(stream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }
    
    override fun cleanup() {
        // Close stream if needed
    }
    
    override fun cancel() {
        // Cancel network request
    }
    
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    
    override fun getDataSource(): DataSource = DataSource.REMOTE
}
```

---

### Thumbnail Prefetching

**Strategy**: Preload next 10 images in background.

```kotlin
class BrowseViewModel @Inject constructor(
    private val glide: RequestManager
) : ViewModel() {
    
    fun prefetchThumbnails(files: List<MediaFile>, startIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val endIndex = (startIndex + 10).coerceAtMost(files.size)
            files.subList(startIndex, endIndex).forEach { file ->
                glide.load(file.path)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .preload(200, 200)
            }
        }
    }
}

// Trigger on scroll
recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        
        if (lastVisiblePosition > 0) {
            viewModel.prefetchThumbnails(files, lastVisiblePosition + 1)
        }
    }
})
```

---

## 3. Database Query Optimization

### Room Query Performance

**Target**: < 16ms for main queries (60 FPS)

---

#### 3.1. Indexes

**Without Index**: Sequential scan (O(n))
```kotlin
@Query("SELECT * FROM resources WHERE name = :name") // SLOW (no index)
```

**With Index**: Binary search (O(log n))
```kotlin
@Entity(
    tableName = "resources",
    indices = [Index(value = ["name"])] // Fast lookup
)
data class ResourceEntity(...)
```

**Benchmark**:
- Without index: 50ms (10,000 rows)
- With index: 2ms (10,000 rows)

---

#### 3.2. Query Optimization

**Use EXPLAIN QUERY PLAN**:
```sql
EXPLAIN QUERY PLAN SELECT * FROM resources WHERE type = 'SMB' AND isDestination = 1;
```

**Bad Plan**:
```
SCAN TABLE resources (~1000 rows)
```

**Good Plan** (with indexes):
```
SEARCH TABLE resources USING INDEX idx_resources_type_destination (~10 rows)
```

---

#### 3.3. Pagination for Large Results

**Problem**: Loading 10,000 files freezes UI.

**Solution**: Use Paging3.

```kotlin
@Dao
interface MediaFileDao {
    
    @Query("SELECT * FROM media_files WHERE resourceId = :resourceId ORDER BY name ASC")
    fun getFilesPaged(resourceId: Long): PagingSource<Int, MediaFileEntity>
}

// ViewModel
val files: Flow<PagingData<MediaFile>> = repository
    .getFilesPaged(resourceId)
    .cachedIn(viewModelScope)

// Activity
lifecycleScope.launch {
    viewModel.files.collectLatest { pagingData ->
        adapter.submitData(pagingData)
    }
}
```

**Benefits**:
- Load 50 items at a time
- Smooth scrolling (no jank)
- Memory efficient

---

### Query Profiling

**Enable SQL logging (debug only)**:
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .setQueryCallback({ sqlQuery, bindArgs ->
        Timber.d("SQL: $sqlQuery | Args: $bindArgs")
    }, Executors.newSingleThreadExecutor())
    .build()
```

---

## 4. Network Performance

### Connection Pooling

**SMB Connection Pool**:
```kotlin
class SmbConnectionPool {
    
    private val pool = ConcurrentHashMap<Long, SmbConnection>()
    private val maxIdleTime = 45_000L // 45s
    
    suspend fun <T> executeWithConnection(
        resource: MediaResource,
        block: suspend (SmbClient) -> T
    ): T = withContext(Dispatchers.IO) {
        val connection = pool.getOrPut(resource.id) {
            createConnection(resource)
        }
        
        // Check if connection is stale
        if (connection.isStale()) {
            connection.reconnect()
        }
        
        block(connection.client)
    }
    
    // Cleanup idle connections (background task)
    fun cleanupIdleConnections() {
        val now = System.currentTimeMillis()
        pool.entries.removeAll { (_, connection) ->
            if (now - connection.lastUsed > maxIdleTime) {
                connection.close()
                true
            } else {
                false
            }
        }
    }
}
```

**Benefits**:
- Reuse connections (avoid handshake overhead)
- Automatic reconnection on stale connections
- Periodic cleanup

---

### Parallel File Downloads

**Problem**: Sequential downloads slow.

**Solution**: Parallel downloads with limited concurrency.

```kotlin
suspend fun downloadFiles(files: List<MediaFile>): List<Result<ByteArray>> = 
    coroutineScope {
        files.map { file ->
            async(Dispatchers.IO) {
                retryWithPolicy {
                    httpClient.download(file.url)
                }
            }
        }.awaitAll()
    }

// With concurrency limit
val semaphore = Semaphore(5) // Max 5 parallel downloads

suspend fun downloadFilesWithLimit(files: List<MediaFile>): List<Result<ByteArray>> =
    coroutineScope {
        files.map { file ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    httpClient.download(file.url)
                }
            }
        }.awaitAll()
    }
```

---

### Network Timeout Configuration

| Operation | Timeout | Retry |
|-----------|---------|-------|
| **Connect** | 10s | 2x |
| **Read** | 30s | 1x |
| **Write** | 60s | 1x |
| **File list** | 30s | 2x |

**OkHttp Configuration**:
```kotlin
val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()
```

---

## 5. Memory Management

### Memory Budgets

**Target**: < 200MB for 1000 files

**Breakdown**:
- App code: 30MB
- Room database: 20MB
- Image cache (memory): 50MB (10% of heap)
- RecyclerView (50 visible items): 30MB
- Network buffers: 20MB
- OS overhead: 50MB

**Total**: ~200MB

---

### Leak Detection (Debug Only)

**LeakCanary Integration**:
```kotlin
dependencies {
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}
```

**Automatic leak detection**: No code needed.

**Common Leaks**:
1. **Activity reference in ViewModel**: Use `Application` context
2. **Static context references**: Avoid static `Activity`/`Fragment`
3. **Background thread with Activity reference**: Use `WeakReference`

---

### Memory Profiling

**Heap Dump Analysis**:
1. Android Studio → Profiler → Memory
2. Trigger suspected leak scenario
3. Click "Dump Java Heap"
4. Analyze retained objects

**Target**: No `Activity` instances retained after navigation.

---

### Bitmap Memory Optimization

**Problem**: High-resolution images consume memory.

**Solutions**:

1. **Downsampling**:
```kotlin
val options = BitmapFactory.Options().apply {
    inJustDecodeBounds = true // Query dimensions only
}
BitmapFactory.decodeFile(file.path, options)

// Calculate sample size
options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
options.inJustDecodeBounds = false

val bitmap = BitmapFactory.decodeFile(file.path, options)
```

2. **RGB_565 Format** (lossy, 50% smaller):
```kotlin
options.inPreferredConfig = Bitmap.Config.RGB_565 // No alpha channel
```

3. **Bitmap Reuse**:
```kotlin
options.inBitmap = reusableBitmap // Reuse memory
```

---

## 6. APK Size Optimization

### Current Size: 18MB (release)

**Breakdown**:
- Code (DEX): 6MB
- Resources (images, XML): 3MB
- Native libs (SMBJ, SSHJ, BouncyCastle): 7MB
- Assets: 2MB

---

### Optimization Techniques

#### 6.1. ProGuard/R8

**Enabled**:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true // Code shrinking
        isShrinkResources = true // Remove unused resources
    }
}
```

**Savings**: ~30% code size

---

#### 6.2. Split APKs by ABI

**build.gradle.kts**:
```kotlin
android {
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false // Separate APKs per ABI
        }
    }
}
```

**Benefit**: arm64-v8a APK only 12MB (vs 18MB universal)

---

#### 6.3. WebP Images

**Convert PNG/JPG → WebP**:
```bash
cwebp input.png -o output.webp -q 80
```

**Savings**: 25-35% smaller than PNG/JPG

---

#### 6.4. Vector Drawables

**Replace PNG icons with XML vectors**:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,2L2,7v10l10,5 10,-5V7L12,2z" />
</vector>
```

**Benefit**: Scalable, tiny file size (<1KB)

---

#### 6.5. Dynamic Feature Modules

**Split rarely-used features**:
- PDF viewer (optional)
- GIF editor (optional)

**Delivered on-demand** via Google Play.

---

## 7. Profiling Tools

### Android Studio Profiler

**CPU Profiler**:
1. Record → Perform action → Stop
2. Analyze flame chart
3. Find hot methods (>5% CPU time)

**Target**: No single method > 100ms on main thread

---

**Memory Profiler**:
1. Monitor allocations during scenario
2. Check for memory leaks (retained `Activity`)
3. Analyze allocation tracker (which methods allocate most)

---

**Network Profiler**:
1. Monitor requests during sync
2. Check request count (avoid chatty APIs)
3. Analyze response sizes (use compression)

---

### Benchmarking (Macrobenchmark)

**Test cold start time**:
```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.apemax.fastmediasorter",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

**Output**:
```
timeToInitialDisplayMs   min 1245.3,   median 1312.5,   max 1456.2
timeToFullDisplayMs      min 1589.2,   median 1645.8,   max 1702.1
```

---

## 8. Benchmarking Strategy

### Performance Test Suite

**Critical Paths to Benchmark**:
1. **Cold start**: App launch → first frame
2. **File list loading**: Open folder → display 1000 files
3. **Thumbnail loading**: Scroll RecyclerView → all thumbnails visible
4. **File copy**: Copy 10MB file local → local
5. **Network sync**: Scan SMB share → display files
6. **Database query**: Search 10,000 files by name

---

### Regression Prevention

**CI Performance Tests**:
```yaml
- name: Run performance tests
  run: ./gradlew :app_v2:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.apemax.fastmediasorter.benchmark.StartupBenchmark

- name: Compare results
  run: |
    python scripts/compare_benchmark.py \
      --baseline benchmarks/baseline.json \
      --current benchmarks/current.json \
      --threshold 10% # Fail if >10% regression
```

---

## 9. JMH Benchmark Suite

### Setup JMH for Android

```gradle
// app_v2/build.gradle.kts
plugins {
    id("me.champeau.jmh") version "0.7.1"
}

dependencies {
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    iterations = 3
    fork = 1
    warmupIterations = 2
}
```

### Critical Algorithm Benchmarks

#### File Path Parsing

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class FilePathBenchmark {
    
    private val smbPath = "smb://192.168.1.100/share/folder/subfolder/file.jpg"
    private val localPath = "/storage/emulated/0/DCIM/Camera/IMG_20250106_123456.jpg"
    
    @Benchmark
    fun parseLocalPath(): String {
        return localPath.substringAfterLast('/')
    }
    
    @Benchmark
    fun parseSmbPath(): String {
        return smbPath.substringAfterLast('/')
    }
    
    @Benchmark
    fun parseWithRegex(): String {
        val regex = Regex("([^/]+)$")
        return regex.find(localPath)?.value ?: ""
    }
}
```

**Expected Results**:
- `substringAfterLast`: ~0.5 μs
- Regex: ~50 μs (100x slower)

#### Thumbnail Generation

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class ThumbnailBenchmark {
    
    private lateinit var bitmap: Bitmap
    
    @Setup
    fun setup() {
        bitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.test_image_4k
        )
    }
    
    @Benchmark
    fun generateThumbnailDefault(): Bitmap {
        return ThumbnailUtils.extractThumbnail(bitmap, 200, 200)
    }
    
    @Benchmark
    fun generateThumbnailOptimized(): Bitmap {
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bitmap.width, bitmap.height, 200, 200)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return Bitmap.createScaledBitmap(bitmap, 200, 200, true)
    }
    
    @TearDown
    fun tearDown() {
        bitmap.recycle()
    }
}
```

#### Database Bulk Insert

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class DatabaseBenchmark {
    
    private lateinit var database: AppDatabase
    private val testResources = List(1000) { 
        ResourceEntity(
            id = it.toLong(),
            name = "Resource $it",
            type = ResourceType.LOCAL,
            path = "/test/path/$it"
        )
    }
    
    @Setup
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    
    @Benchmark
    fun insertSequential() {
        testResources.forEach { database.resourceDao().insert(it) }
    }
    
    @Benchmark
    fun insertBatch() {
        database.resourceDao().insertAll(testResources)
    }
    
    @Benchmark
    fun insertTransaction() {
        database.runInTransaction {
            testResources.forEach { database.resourceDao().insert(it) }
        }
    }
    
    @TearDown
    fun tearDown() {
        database.close()
    }
}
```

**Expected Results**:
- Sequential: ~1000ms (1ms per insert)
- Batch: ~50ms (20x faster)
- Transaction: ~100ms (10x faster)

### Running Benchmarks

```bash
# Run all benchmarks
./gradlew jmh

# Run specific benchmark
./gradlew jmh --jmh-includes='.*FilePathBenchmark.*'

# Generate JSON report
./gradlew jmh --jmh-result-format=JSON --jmh-result=benchmarks/results.json
```

### Regression Detection in CI

```python
# scripts/compare_benchmark.py
import json
import sys

def compare_benchmarks(baseline_file, current_file, threshold=0.10):
    with open(baseline_file) as f:
        baseline = json.load(f)
    with open(current_file) as f:
        current = json.load(f)
    
    regressions = []
    
    for bench_name in baseline:
        if bench_name not in current:
            continue
        
        baseline_time = baseline[bench_name]['primaryMetric']['score']
        current_time = current[bench_name]['primaryMetric']['score']
        
        regression = (current_time - baseline_time) / baseline_time
        
        if regression > threshold:
            regressions.append({
                'name': bench_name,
                'baseline': baseline_time,
                'current': current_time,
                'regression': f'{regression * 100:.1f}%'
            })
    
    if regressions:
        print("❌ Performance regressions detected:")
        for r in regressions:
            print(f"  {r['name']}: {r['baseline']:.2f}ms → {r['current']:.2f}ms ({r['regression']})")
        sys.exit(1)
    else:
        print("✅ No performance regressions")
        sys.exit(0)

if __name__ == '__main__':
    compare_benchmarks(
        baseline_file=sys.argv[1],
        current_file=sys.argv[2],
        threshold=float(sys.argv[3])
    )
```

### Memory Profiler Snapshots

#### Baseline Snapshots

```bash
# Create baseline memory snapshot
adb shell am start -W com.apemax.fastmediasorter/.ui.MainActivity
adb shell am dumpheap com.apemax.fastmediasorter /data/local/tmp/baseline.hprof
adb pull /data/local/tmp/baseline.hprof benchmarks/memory/baseline.hprof
```

#### Automated Comparison

```kotlin
// In instrumentation test
@Test
fun compareMemoryUsage() {
    val baselineSnapshot = loadSnapshot("benchmarks/memory/baseline.hprof")
    
    // Perform operation
    browseViewModel.loadMediaFiles(1000)
    
    val currentSnapshot = captureHeapDump()
    
    val baselineMemory = baselineSnapshot.totalMemory
    val currentMemory = currentSnapshot.totalMemory
    
    val increase = (currentMemory - baselineMemory).toFloat() / baselineMemory
    
    assertTrue(
        "Memory increased by ${increase * 100}%, threshold is 20%",
        increase < 0.20
    )
}
```

#### Leak Detection

```kotlin
// LeakCanary configuration
class FastMediaSorterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            LeakCanary.config = LeakCanary.config.copy(
                dumpHeap = true,
                retainedVisibleThreshold = 5
            )
        }
    }
}
```

### Continuous Monitoring

```yaml
# .github/workflows/performance.yml
name: Performance Tests

on:
  pull_request:
    branches: [ main ]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run JMH benchmarks
        run: ./gradlew jmh --jmh-result-format=JSON --jmh-result=current.json
      
      - name: Download baseline
        uses: actions/download-artifact@v3
        with:
          name: benchmark-baseline
          path: baseline.json
      
      - name: Compare results
        run: python scripts/compare_benchmark.py baseline.json current.json 0.10
      
      - name: Comment PR
        if: failure()
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '⚠️ Performance regression detected! Check the logs for details.'
            })
```

---

## Performance Checklist

### App Launch
- [ ] Cold start < 1.5s
- [ ] No disk I/O on main thread
- [ ] StrictMode clean (debug)

### Image Loading
- [ ] Thumbnails < 300ms
- [ ] Glide disk cache enabled
- [ ] Custom ModelLoaders for network files

### Database
- [ ] Queries < 16ms on main thread
- [ ] Indexes on all WHERE columns
- [ ] Pagination for large lists

### Network
- [ ] Connection pooling enabled
- [ ] Timeouts configured (10s connect, 30s read)
- [ ] Parallel downloads (max 5 concurrent)

### Memory
- [ ] App memory < 200MB (1000 files)
- [ ] No memory leaks (LeakCanary)
- [ ] Bitmap reuse enabled

### APK Size
- [ ] Release APK < 25MB
- [ ] ProGuard/R8 enabled
- [ ] Split APKs by ABI

---

## 9. Background Task Prioritization

### WorkManager Task Priority Matrix

| Task Type | Priority | Network | Battery | Frequency | Constraints |
|-----------|----------|---------|---------|-----------|-------------|
| **Thumbnail Generation** | LOW | Any | Any | On-demand | None |
| **Trash Cleanup** | LOW | Any | Charging preferred | Weekly | None |
| **Health Check** | MEDIUM | WiFi preferred | >15% | Daily | None |
| **Cloud Sync (Manual)** | HIGH | WiFi/Mobile | >15% | On-demand | NetworkType.CONNECTED |
| **Cloud Sync (Auto)** | MEDIUM | WiFi only | >30% | 6 hours | NetworkType.UNMETERED |
| **File Index Update** | MEDIUM | WiFi preferred | >20% | 12 hours | None |
| **Cache Cleanup** | LOW | Any | Any | Daily | StorageNotLow |

### Priority Implementation

```kotlin
class BackgroundTaskPriority {
    companion object {
        const val PRIORITY_HIGH = 1      // User-initiated tasks
        const val PRIORITY_MEDIUM = 5    // Automatic maintenance
        const val PRIORITY_LOW = 10      // Deferrable cleanup
    }
}

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudRepository: CloudRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Check battery level before starting
        if (!hasSufficientBattery()) {
            return Result.retry()
        }
        
        // Perform sync with progress updates
        return try {
            cloudRepository.syncAll { progress ->
                setProgress(workDataOf("progress" to progress))
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    
    private fun hasSufficientBattery(): Boolean {
        val batteryManager = applicationContext.getSystemService<BatteryManager>()
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        
        return when (inputData.getInt("priority", BackgroundTaskPriority.PRIORITY_MEDIUM)) {
            BackgroundTaskPriority.PRIORITY_HIGH -> batteryLevel > 15
            BackgroundTaskPriority.PRIORITY_MEDIUM -> batteryLevel > 30
            else -> batteryLevel > 50
        }
    }
}
```

### Battery Consumption Targets

| Task | Duration | Battery Impact | Target |
|------|----------|----------------|--------|
| **Thumbnail Gen (100 files)** | 30s | <0.5% | <1% |
| **Cloud Sync (1GB)** | 5 min | <2% | <3% |
| **Health Check (5 resources)** | 10s | <0.1% | <0.2% |
| **Cache Cleanup** | 20s | <0.3% | <0.5% |
| **File Index (1000 files)** | 1 min | <0.5% | <1% |

**Measurement**: Use Battery Historian to validate actual consumption.

### Background Sync Policies

#### Auto-Sync Policy

```kotlin
object AutoSyncPolicy {
    
    fun scheduleAutoSync(context: Context, resourceId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
            .setRequiresBatteryNotLow(true)                // >15% battery
            .setRequiresStorageNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "resource_id" to resourceId,
                "priority" to BackgroundTaskPriority.PRIORITY_MEDIUM
            ))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "cloud_sync_$resourceId",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
    
    fun cancelAutoSync(context: Context, resourceId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("cloud_sync_$resourceId")
    }
}
```

#### Doze Mode Handling

```kotlin
class DozeAwareScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun scheduleDozeFriendlyTask(task: WorkRequest) {
        val powerManager = context.getSystemService<PowerManager>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            powerManager?.isDeviceIdleMode == true) {
            // Defer non-critical tasks during Doze
            Timber.d("Device in Doze mode, deferring task")
            return
        }
        
        WorkManager.getInstance(context).enqueue(task)
    }
}
```

#### User-Configurable Sync Settings

```xml
<!-- res/xml/settings_sync.xml -->
<PreferenceScreen>
    <SwitchPreferenceCompat
        app:key="auto_sync_enabled"
        app:title="@string/settings_auto_sync"
        app:summary="@string/settings_auto_sync_summary"
        app:defaultValue="false" />
    
    <ListPreference
        app:key="sync_frequency"
        app:title="@string/settings_sync_frequency"
        app:entries="@array/sync_frequency_entries"
        app:entryValues="@array/sync_frequency_values"
        app:defaultValue="6"
        app:dependency="auto_sync_enabled" />
    
    <SwitchPreferenceCompat
        app:key="sync_wifi_only"
        app:title="@string/settings_sync_wifi_only"
        app:summary="@string/settings_sync_wifi_only_summary"
        app:defaultValue="true"
        app:dependency="auto_sync_enabled" />
    
    <SeekBarPreference
        app:key="min_battery_level"
        app:title="@string/settings_min_battery"
        app:summary="@string/settings_min_battery_summary"
        app:min="15"
        app:max="50"
        app:defaultValue="30"
        app:dependency="auto_sync_enabled" />
</PreferenceScreen>
```

### Monitoring Background Tasks

```kotlin
@Singleton
class BackgroundTaskMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun observeWorkInfo(workName: String): Flow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(workName)
            .map { it.firstOrNull() }
    }
    
    fun logTaskMetrics(workInfo: WorkInfo) {
        val duration = workInfo.outputData.getLong("duration_ms", 0)
        val batteryStart = workInfo.outputData.getInt("battery_start", 0)
        val batteryEnd = workInfo.outputData.getInt("battery_end", 0)
        
        FirebaseAnalytics.getInstance(context).logEvent("background_task_completed") {
            param("task_name", workInfo.tags.first())
            param("duration_ms", duration)
            param("battery_consumed", batteryStart - batteryEnd)
            param("result", workInfo.state.name)
        }
    }
}
```

---

## Reference Files

### Source Code
- **Glide Module**: `util/FastMediaSorterGlideModule.kt`
- **Connection Pool**: `data/network/SmbConnectionPool.kt`
- **Profiling**: `benchmark/StartupBenchmark.kt`

### Related Documents
- [26. Database Schema & Migrations](26_database_schema.md) - Query optimization
- [22. Cache Strategies](22_cache_strategies.md) - Glide cache configuration
- [24. Dependencies Reference](24_dependencies.md) - Performance libraries

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
