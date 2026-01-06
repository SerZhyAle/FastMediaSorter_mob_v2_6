# Epic 3: Media Player & Viewer Engine - Technical Specification
*Derived from: [Tactical Plan: Epic 3](../00_strategy_epic3_player.md)*  
*Reference: [Player Activity](../04_player_activity.md), [Player Logic](../detailed_logic/04a_player_logic.md), [Common Pitfalls](../21_common_pitfalls.md)*

**Purpose**: Design and implement unified PlayerActivity from scratch supporting all media types (images, video, audio, PDF, TXT, EPUB) with proper architecture.

**Development Approach**: Clean implementation WITHOUT copying existing V1 code. Use this spec as requirements document.

**Estimated Time**: 5-7 days  
**Prerequisites**: Epic 2 completed (BrowseActivity passes file list)  
**Output**: Production-ready media player with ExoPlayer, PhotoView, PDF renderer, text viewer

---

## 1. PlayerActivity Foundation

### 1.1 Unified Player Layout
- [ ] Create `res/layout/activity_player.xml` (supports ALL media types):
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black">
    
    <!-- Top Command Panel (buttons for all media types) -->
    <HorizontalScrollView
        android:id="@+id/topCommandPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#80000000"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent">
        
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:padding="@dimen/spacing_small">
            
            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="@dimen/icon_size"
                android:layout_height="@dimen/icon_size"
                android:src="@drawable/ic_arrow_back"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/back"
                android:tint="@android:color/white"/>
            
            <ImageButton
                android:id="@+id/btnEdit"
                android:layout_width="@dimen/icon_size"
                android:layout_height="@dimen/icon_size"
                android:src="@drawable/ic_edit"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/edit"
                android:tint="@android:color/white"
                android:visibility="gone"/>
            
            <ImageButton
                android:id="@+id/btnRotate"
                android:layout_width="@dimen/icon_size"
                android:layout_height="@dimen/icon_size"
                android:src="@drawable/ic_rotate_right"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/rotate"
                android:tint="@android:color/white"
                android:visibility="gone"/>
            
            <ImageButton
                android:id="@+id/btnShare"
                android:layout_width="@dimen/icon_size"
                android:layout_height="@dimen/icon_size"
                android:src="@drawable/ic_share"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/share"
                android:tint="@android:color/white"/>
            
            <ImageButton
                android:id="@+id/btnDelete"
                android:layout_width="@dimen/icon_size"
                android:layout_height="@dimen/icon_size"
                android:src="@drawable/ic_delete"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/delete"
                android:tint="@android:color/white"/>
                
        </LinearLayout>
    </HorizontalScrollView>
    
    <!-- Container for different media types -->
    <FrameLayout
        android:id="@+id/mediaContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/topCommandPanel"
        app:layout_constraintBottom_toBottomOf="parent">
        
        <!-- Image Viewer (PhotoView for zoom/pan) -->
        <com.github.chrisbanes.photoview.PhotoView
            android:id="@+id/imageView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"/>
        
        <!-- Video/Audio Player (ExoPlayer) -->
        <androidx.media3.ui.PlayerView
            android:id="@+id/videoPlayerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"
            app:show_timeout="3000"
            app:controller_layout_id="@layout/exoplayer_controls"/>
        
        <!-- PDF Viewer (RecyclerView with pages) -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/pdfPagesRecyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"/>
        
        <!-- Text Viewer (for TXT/LOG/JSON files) -->
        <ScrollView
            android:id="@+id/textScrollView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"
            android:padding="@dimen/spacing_medium">
            
            <TextView
                android:id="@+id/textContent"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@android:color/white"
                android:textSize="14sp"
                android:fontFamily="monospace"/>
        </ScrollView>
        
        <!-- EPUB Viewer (WebView or custom) -->
        <WebView
            android:id="@+id/epubWebView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"/>
            
    </FrameLayout>
    
    <!-- Touch Zones for Previous/Next navigation (for images) -->
    <LinearLayout
        android:id="@+id/touchZonesOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:visibility="gone">
        
        <View
            android:id="@+id/touchZonePrevious"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@android:color/transparent"/>
        
        <View
            android:id="@+id/touchZoneNext"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@android:color/transparent"/>
    </LinearLayout>
    
    <!-- Loading indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:visibility="gone"/>
        
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 1.2 ExoPlayer Custom Controls
- [ ] Create `res/layout/exoplayer_controls.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="#80000000"
    android:padding="@dimen/spacing_normal">
    
    <!-- Play/Pause button -->
    <ImageButton
        android:id="@id/exo_play_pause"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:tint="@android:color/white"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>
    
    <!-- Rewind 10s -->
    <ImageButton
        android:id="@+id/btnRewind10"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:src="@drawable/ic_replay_10"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/rewind_10s"
        android:tint="@android:color/white"
        app:layout_constraintStart_toEndOf="@id/exo_play_pause"
        app:layout_constraintTop_toTopOf="@id/exo_play_pause"
        app:layout_constraintBottom_toBottomOf="@id/exo_play_pause"
        android:layout_marginStart="@dimen/spacing_small"/>
    
    <!-- Forward 30s -->
    <ImageButton
        android:id="@+id/btnForward30"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:src="@drawable/ic_forward_30"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/forward_30s"
        android:tint="@android:color/white"
        app:layout_constraintStart_toEndOf="@id/btnRewind10"
        app:layout_constraintTop_toTopOf="@id/exo_play_pause"
        app:layout_constraintBottom_toBottomOf="@id/exo_play_pause"
        android:layout_marginStart="@dimen/spacing_small"/>
    
    <!-- Speed control -->
    <TextView
        android:id="@+id/btnSpeed"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="1.0x"
        android:textColor="@android:color/white"
        android:textSize="14sp"
        android:padding="@dimen/spacing_small"
        android:background="?attr/selectableItemBackgroundBorderless"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/exo_play_pause"
        app:layout_constraintBottom_toBottomOf="@id/exo_play_pause"/>
    
    <!-- Seekbar -->
    <com.google.android.material.slider.Slider
        android:id="@id/exo_progress"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toBottomOf="@id/exo_play_pause"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="@dimen/spacing_small"/>
    
    <!-- Time labels -->
    <TextView
        android:id="@id/exo_position"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textSize="12sp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/exo_progress"/>
    
    <TextView
        android:id="@id/exo_duration"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textSize="12sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/exo_progress"/>
        
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 1.3 PlayerActivity Architecture Requirements

**ANTI-PATTERN (V1)**: 2700-line God Class с вложенной логикой для всех типов медиа

**CORRECT APPROACH**: Декомпозиция на helper managers с четкой ответственностью

- [ ] Create `ui/player/PlayerActivity.kt` extending `BaseActivity<ActivityPlayerBinding>`

**Required Structure**:
```
PlayerActivity
  ├─ VideoPlayerManager (injected via Hilt)
  ├─ TextViewerManager (injected via Hilt)
  ├─ PdfRendererManager (injected via Hilt)
  ├─ PlayerUiStateCoordinator (local instance)
  └─ PlayerViewModel (viewModels() delegate)
```

**Intent Contract**:
```kotlin
// Incoming data from BrowseActivity
EXTRA_FILE_PATH: String          // Current file path
EXTRA_FILE_LIST: ArrayList<String>  // All files in folder
EXTRA_POSITION: Int              // Current position in list
```

**State Management Requirements**:
1. Observe `viewModel.currentFile: StateFlow<MediaFile?>` for file changes
2. Observe `viewModel.uiState: StateFlow<PlayerUiState>` for loading/error states
3. Use `repeatOnLifecycle(Lifecycle.State.STARTED)` for safe collection
4. Launch separate coroutines for each Flow (avoid blocking)

**Media Type Routing Logic**:
```
when (mediaFile.type) {
  IMAGE -> Show PhotoView + touchZones, load via Glide
  VIDEO -> Show PlayerView, delegate to VideoPlayerManager
  AUDIO -> Show PlayerView (same as video), delegate to VideoPlayerManager
  PDF -> Show RecyclerView, delegate to PdfRendererManager
  TXT -> Show ScrollView, delegate to TextViewerManager
  GIF -> Show PhotoView, load via Glide.asGif()
  EPUB -> Show WebView (optional for Epic 3)
  OTHER -> Show error message
}
```

**View Visibility Rules**:
- Always call `hideAllViews()` BEFORE showing new media type
- Update UI controls via `uiStateCoordinator.updateForMediaType(type)`
- Show/hide touchZones ONLY for IMAGE/GIF types
- Show/hide edit/rotate buttons ONLY for IMAGE/GIF types

**Lifecycle Management**:
```kotlin
onCreate() {
  initializeManagers()      // Setup all helpers
  setupClickListeners()     // Button/zone clicks
  observeData()            // StateFlow collection
  loadInitialFile()        // From intent extras
}

onDestroy() {
  videoPlayerManager.release()  // CRITICAL: release ExoPlayer
  pdfRendererManager.release()  // CRITICAL: close PdfRenderer
}
```

**Error Handling**:
- Catch file loading errors in ViewModel, emit `PlayerUiState.Error`
- Show Snackbar for user-facing errors (not Toast)
- Log errors with Timber.e() with exception details

**Performance Considerations**:
- Use `DiskCacheStrategy.RESOURCE` for Glide (Epic 1 config)
- Lazy-load PDF pages (don't render all at once)
- Limit text file size to 1MB (show truncation message)
- Cancel ongoing loads when navigating to next file

### 1.4 PlayerViewModel Contract

**ANTI-PATTERN (V1)**: Прямая работа с File API в ViewModel, отсутствие UseCase слоя

**CORRECT APPROACH**: Вся бизнес-логика через UseCase, ViewModel только координирует UI state

- [ ] Create `ui/player/PlayerViewModel.kt` с аннотацией `@HiltViewModel`

**Required Dependencies (constructor injection)**:
```kotlin
@Inject constructor(
    private val getMediaFileUseCase: GetMediaFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
)
```

**State Contract**:
```kotlin
sealed class PlayerUiState {
    data object Loading : PlayerUiState()
    data object Success : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
```

**Exposed StateFlows**:
```kotlin
val currentFile: StateFlow<MediaFile?>    // Currently displayed file
val uiState: StateFlow<PlayerUiState>     // Loading/Success/Error state
```

**Public Methods Specification**:

1. **`loadFile(filePath: String, files: List<String>, position: Int)`**
   - Store fileList and currentPosition internally
   - Emit `PlayerUiState.Loading`
   - Call `getMediaFileUseCase(filePath)`
   - On success: emit file to `_currentFile`, emit `PlayerUiState.Success`
   - On error: emit `PlayerUiState.Error(message)`
   - Launch in `viewModelScope`

2. **`navigateToNext()`**
   - Check `currentPosition < fileList.size - 1`
   - If true: increment position, call `loadFile()` with next file
   - If false: do nothing (already at end)

3. **`navigateToPrevious()`**
   - Check `currentPosition > 0`
   - If true: decrement position, call `loadFile()` with previous file
   - If false: do nothing (already at start)

4. **`deleteCurrentFile()`**
   - Get current file from `_currentFile.value`
   - Call `deleteFileUseCase(file.path)`
   - On success: automatically call `navigateToNext()`
   - On error: emit `PlayerUiState.Error`
   - Launch in `viewModelScope`

5. **`rotateImage(degrees: Float)`** (Epic 3 optional)
   - Placeholder for image rotation feature
   - Implementation deferred to image editing epic

**Critical Implementation Notes**:
- Use `MutableStateFlow` privately, expose as `StateFlow` (immutable)
- Always call `.asStateFlow()` for public exposure
- NEVER expose mutable state directly to UI
- Use `viewModelScope.launch` for all coroutines (auto-cancellation on clear)
- Store fileList/currentPosition as private var (not StateFlow)

**Error Handling Strategy**:
- Catch exceptions from UseCases, convert to `PlayerUiState.Error`
- Include file path in error messages for debugging
- Log errors with `Timber.e(exception, "Context")`

---

## 2. Video Player (ExoPlayer Integration)

### 2.1 VideoPlayerManager Interface Specification

**ANTI-PATTERN (V1)**: ExoPlayer создавался каждый раз при навигации (memory leak риск)

**CORRECT APPROACH**: Одна инстанция ExoPlayer на весь lifecycle PlayerActivity

- [ ] Create `ui/player/helpers/VideoPlayerManager.kt` с Hilt injection

**Required Dependencies**:
```kotlin
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")
```

**Interface Contract**:

```kotlin
class VideoPlayerManager @Inject constructor() {
    fun initialize(context: Context, view: PlayerView)
    fun playVideo(path: String)
    fun playAudio(path: String)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun release()  // CRITICAL: Must be called in onDestroy()
}
```

**Implementation Requirements**:

1. **`initialize()`**:
   - Create `ExoPlayer.Builder(context).build()`
   - Attach to `PlayerView` via `view.player = exoPlayer`
   - Add `Player.Listener` для отслеживания состояний:
     - `STATE_ENDED`: Auto-navigate to next file (опционально)
     - `STATE_READY`: Hide loading indicator
     - `STATE_BUFFERING`: Show loading indicator
     - `STATE_IDLE`: Initial state
   - Log state changes с `Timber.d()`

2. **`playVideo()` / `playAudio()`**:
   - Parse path to `MediaItem.fromUri(path)`
   - Call `exoPlayer.setMediaItem(mediaItem)`
   - Call `exoPlayer.prepare()`
   - Set `playWhenReady = true` для autoplay
   - Audio/Video используют ОДИНАКОВУЮ логику (ExoPlayer handles both)

3. **`pause()` / `resume()`**:
   - Direct calls to `exoPlayer.pause()` и `exoPlayer.play()`

4. **`seekTo()`**:
   - Forward to `exoPlayer.seekTo(positionMs)`

5. **`setPlaybackSpeed()`**:
   - Call `exoPlayer.setPlaybackSpeed(speed)`
   - Typical values: 0.5x, 1.0x, 1.25x, 1.5x, 2.0x

6. **`release()`** - CRITICAL:
   - Call `exoPlayer?.release()`
   - Set `exoPlayer = null` to prevent leaks
   - Set `playerView = null`
   - MUST be called in `PlayerActivity.onDestroy()`

**ExoPlayer Controls Integration**:
- Use custom controls layout: `exoplayer_controls.xml`
- PlayerView attribute: `app:controller_layout_id="@layout/exoplayer_controls"`
- Timeout: `app:show_timeout="3000"` (3 seconds)

**Error Handling**:
- Listen to `onPlayerError()` in `Player.Listener`
- Emit errors to ViewModel via callback или SharedFlow
- Show user-friendly error message ("Failed to play video")

---

## 3. Image Viewer (PhotoView + Glide)

### 3.1 Glide Configuration
- [ ] Create `util/image/AppGlideModule.kt`:
```kotlin
package com.sza.fastmediasorter.util.image

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class FastMediaSorterGlideModule : AppGlideModule() {
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Set disk cache size to 2GB
        val diskCacheSizeBytes = 1024 * 1024 * 2048L // 2GB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "image_cache", diskCacheSizeBytes))
        
        // Default request options
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        )
    }
}
```

### 3.2 Large Image Handling
- [ ] Add PhotoView to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}
```

---

## 4. Document Viewers

### 4.1 PdfRendererManager Interface Specification

**ANTI-PATTERN (V1)**: Рендеринг всех страниц PDF сразу → memory exhaustion для больших PDF

**CORRECT APPROACH**: Lazy rendering - рендерить только видимые страницы в RecyclerView

- [ ] Create `ui/player/helpers/PdfRendererManager.kt`

**Interface Contract**:
```kotlin
class PdfRendererManager @Inject constructor() {
    fun initialize(context: Context, view: RecyclerView)
    suspend fun renderPdf(path: String)
    fun release()  // CRITICAL: Must be called in onDestroy()
}
```

**Implementation Strategy**:

1. **Lazy Rendering Approach** (RECOMMENDED):
   - Open `PdfRenderer` once
   - Create RecyclerView adapter с `pageCount` items
   - Render page bitmap ONLY in `onBindViewHolder()`
   - Recycle bitmaps when views are recycled
   - Use `LinearLayoutManager` for vertical scroll

2. **PDF Renderer Setup**:
   ```kotlin
   val file = File(path)
   val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
   val renderer = PdfRenderer(descriptor)
   ```

3. **Page Rendering**:
   ```kotlin
   val page = renderer.openPage(pageIndex)
   val bitmap = Bitmap.createBitmap(
       page.width, 
       page.height, 
       Bitmap.Config.ARGB_8888
   )
   page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
   page.close()  // CRITICAL: Close after rendering
   ```

4. **Memory Management**:
   - Set `android:largeHeap="true"` в `AndroidManifest.xml` (optional)
   - Use `BitmapPool` для reusing bitmaps (advanced)
   - Call `bitmap.recycle()` когда page уходит off-screen

5. **RecyclerView Adapter Requirements**:
   - ViewHolder с `ImageView` для отображения bitmap
   - `getItemCount()` = `renderer.pageCount`
   - `onBindViewHolder()`: Render page, set to ImageView
   - Handle orientation: fit page width to screen width

6. **Thread Safety**:
   - PDF rendering на `Dispatchers.IO` (CPU-intensive)
   - RecyclerView update на `Dispatchers.Main`

7. **`release()` Method** - CRITICAL:
   - Close `pdfRenderer?.close()`
   - Close `fileDescriptor?.close()`
   - Set both to `null`
   - MUST be called in `PlayerActivity.onDestroy()`

**Error Handling**:
- Catch `IOException` when opening PDF
- Catch `SecurityException` для encrypted PDFs
- Show error: "Unable to open PDF" или "Password-protected PDF not supported"

**Performance Notes**:
- Rendering 100-page PDF takes ~5-10 seconds for all pages
- Lazy rendering renders single page in ~50-100ms
- Prefer lazy approach for better UX

### 4.2 TextViewerManager Interface Specification

**ANTI-PATTERN (V1)**: Загрузка больших файлов полностью в память → OutOfMemoryError

**CORRECT APPROACH**: Ограничение размера + streaming для больших файлов

- [ ] Create `ui/player/helpers/TextViewerManager.kt`

**Interface Contract**:
```kotlin
class TextViewerManager @Inject constructor() {
    fun initialize(view: TextView)
    suspend fun displayTextFile(path: String)
}
```

**Implementation Requirements**:

1. **File Size Handling**:
   - Check `file.length()` before reading
   - If `> 1MB (1_000_000 bytes)`: Read first 1MB ONLY
   - Append message: `"\n\n[File too large, showing first 1MB only]"`
   - If `<= 1MB`: Read full file via `file.readText()`

2. **Thread Safety**:
   - File reading on `Dispatchers.IO`
   - TextView update on `Dispatchers.Main`
   - Use `withContext()` for thread switching

3. **Encoding Detection** (optional for Epic 3):
   - Default: UTF-8
   - Future: Auto-detect via BOM или heuristics

4. **TextView Configuration**:
   - Set `android:fontFamily="monospace"` for code files
   - Set `android:textSize="14sp"`
   - Enable horizontal scroll if needed

5. **Error Handling**:
   - Catch `IOException`, `OutOfMemoryError`
   - Log with `Timber.e(exception, "Failed to read text file")`
   - Show placeholder: "Unable to display file"

**Supported File Types**:
- `.txt`, `.log`, `.json`, `.xml`, `.csv`
- Text-based config files: `.properties`, `.ini`, `.conf`
- Code files: `.kt`, `.java`, `.py`, `.js` (syntax highlighting - future)

---

## 5. Image Editing

### 5.1 Image Edit Dialog Layout
- [ ] Create `res/layout/dialog_image_edit.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="@dimen/spacing_medium">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/edit_image"
        android:textSize="18sp"
        android:textStyle="bold"/>
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="@dimen/spacing_medium">
        
        <Button
            android:id="@+id/btnRotate90"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/rotate_90"
            android:layout_margin="@dimen/spacing_small"/>
        
        <Button
            android:id="@+id/btnRotate180"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/rotate_180"
            android:layout_margin="@dimen/spacing_small"/>
    </LinearLayout>
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/brightness"
        android:layout_marginTop="@dimen/spacing_medium"/>
    
    <com.google.android.material.slider.Slider
        android:id="@+id/brightnessSlider"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:valueFrom="-100"
        android:valueTo="100"
        android:value="0"
        android:stepSize="10"/>
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="@dimen/spacing_medium">
        
        <Button
            android:id="@+id/btnCancel"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@android:string/cancel"
            style="@style/Widget.Material3.Button.OutlinedButton"/>
        
        <Button
            android:id="@+id/btnSave"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/save"
            android:layout_marginStart="@dimen/spacing_small"/>
    </LinearLayout>
    
</LinearLayout>
```

---

## 6. UI State Coordinator Helper

### 6.1 PlayerUiStateCoordinator Specification

**PURPOSE**: Управление видимостью UI элементов в зависимости от типа медиа

**ANTI-PATTERN (V1)**: UI logic размазана по всему PlayerActivity

**CORRECT APPROACH**: Централизованная логика в отдельном coordinator

- [ ] Create `ui/player/helpers/PlayerUiStateCoordinator.kt`

**Interface Contract**:
```kotlin
class PlayerUiStateCoordinator {
    // NOT injected - created locally in PlayerActivity
    
    constructor(binding: ActivityPlayerBinding)  // Pass binding in constructor
    
    fun toggleCommandPanel()
    fun updateForMediaType(type: MediaType)
}
```

**Command Panel Toggle Logic**:
```
toggleCommandPanel() {
  commandPanelVisible = !commandPanelVisible
  topCommandPanel.visibility = if (visible) View.VISIBLE else View.GONE
}
```
- Called when user taps mediaContainer
- Auto-hide after 3 seconds (optional)

**Media Type UI Rules**:

| Media Type | Edit Button | Rotate Button | Touch Zones | Command Panel |
|------------|-------------|---------------|-------------|---------------|
| IMAGE      | ✅ Show     | ✅ Show       | ✅ Show     | ✅ Show       |
| GIF        | ✅ Show     | ✅ Show       | ✅ Show     | ✅ Show       |
| VIDEO      | ❌ Hide     | ❌ Hide       | ❌ Hide     | ✅ Show       |
| AUDIO      | ❌ Hide     | ❌ Hide       | ❌ Hide     | ✅ Show       |
| PDF        | ❌ Hide     | ❌ Hide       | ❌ Hide     | ✅ Show       |
| TXT        | ❌ Hide     | ❌ Hide       | ❌ Hide     | ✅ Show       |
| EPUB       | ❌ Hide     | ❌ Hide       | ❌ Hide     | ✅ Show       |
| OTHER      | ❌ Hide     | ❌ Hide       | ❌ Hide     | ❌ Hide       |

**Implementation Notes**:
- Use `when (type)` для routing logic
- Group similar types: `MediaType.IMAGE, MediaType.GIF ->`
- Always show Share/Delete buttons (available for all types)
- Touch zones ONLY for image navigation (left=previous, right=next)

**Auto-Hide Command Panel** (optional):
- Start countdown timer on panel show
- Hide after 3 seconds of inactivity
- Reset timer on user touch
- Cancel timer in coordinator's cleanup method

---

## 7. Required Drawable Icons

- [ ] Create in `res/drawable/`:
  - `ic_replay_10.xml` - Rewind 10s icon
  - `ic_forward_30.xml` - Forward 30s icon
  - `ic_rotate_right.xml` - Rotate image
  - `ic_share.xml` - Share icon
  - `ic_delete.xml` - Delete icon

Example `ic_replay_10.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M11.99,5V1l-5,5 5,5V7c3.31,0 6,2.69 6,6s-2.69,6 -6,6 -6,-2.69 -6,-6h-2c0,4.42 3.58,8 8,8s8,-3.58 8,-8 -3.58,-8 -8,-8zM10.89,16h-0.85v-3.26l-1.01,0.31v-0.69l1.77,-0.63h0.09V16zM15.17,14.24c0,0.32 -0.03,0.6 -0.1,0.82 -0.07,0.23 -0.17,0.42 -0.29,0.57 -0.12,0.15 -0.27,0.26 -0.44,0.33 -0.17,0.07 -0.37,0.1 -0.59,0.1 -0.22,0 -0.41,-0.03 -0.59,-0.1 -0.18,-0.07 -0.33,-0.18 -0.46,-0.33 -0.13,-0.15 -0.23,-0.34 -0.3,-0.57 -0.07,-0.22 -0.11,-0.5 -0.11,-0.82V13.5c0,-0.32 0.03,-0.6 0.1,-0.82 0.07,-0.23 0.17,-0.42 0.29,-0.57 0.12,-0.15 0.27,-0.26 0.44,-0.33 0.17,-0.07 0.37,-0.1 0.59,-0.1 0.22,0 0.41,0.03 0.59,0.1 0.18,0.07 0.33,0.18 0.46,0.33 0.13,0.15 0.23,0.34 0.3,0.57 0.07,0.22 0.11,0.5 0.11,0.82V14.24zM14.32,13.38c0,-0.19 -0.01,-0.35 -0.04,-0.48 -0.03,-0.13 -0.07,-0.23 -0.12,-0.31 -0.05,-0.08 -0.11,-0.14 -0.19,-0.17 -0.08,-0.03 -0.16,-0.05 -0.25,-0.05 -0.09,0 -0.17,0.02 -0.25,0.05 -0.08,0.03 -0.14,0.09 -0.19,0.17 -0.05,0.08 -0.09,0.18 -0.12,0.31 -0.03,0.13 -0.04,0.29 -0.04,0.48v0.97c0,0.19 0.01,0.35 0.04,0.48 0.03,0.13 0.07,0.24 0.12,0.32 0.05,0.08 0.11,0.14 0.19,0.17 0.08,0.03 0.16,0.05 0.25,0.05 0.09,0 0.17,-0.02 0.25,-0.05 0.08,-0.03 0.14,-0.09 0.19,-0.17 0.05,-0.08 0.09,-0.19 0.11,-0.32 0.03,-0.13 0.04,-0.29 0.04,-0.48V13.38z"/>
</vector>
```

---

## 8. Completion Checklist

**UI**:
- [ ] PlayerActivity layout supports all media types (image/video/audio/pdf/txt/epub)
- [ ] ExoPlayer custom controls with speed, rewind, forward buttons
- [ ] Touch zones for image navigation (previous/next)
- [ ] Command panel with edit/rotate/share/delete buttons

**Functionality**:
- [ ] ExoPlayer plays video/audio with lifecycle management
- [ ] PhotoView enables zoom/pan for images
- [ ] PDF renderer displays pages in RecyclerView
- [ ] Text viewer handles large files (1MB limit)
- [ ] GIF playback via Glide

**Managers (Decomposed Logic)**:
- [ ] VideoPlayerManager handles ExoPlayer lifecycle
- [ ] TextViewerManager handles text file rendering
- [ ] PdfRendererManager handles PDF page rendering
- [ ] PlayerUiStateCoordinator manages UI state per media type

**Success Criteria**: PlayerActivity displays all media types correctly, video/audio playback works, images support zoom/pan, PDF pages render correctly.

**Next**: Epic 4 (Network Protocols) for remote file access.
