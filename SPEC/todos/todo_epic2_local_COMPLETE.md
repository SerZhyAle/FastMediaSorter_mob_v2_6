# Epic 2: Local File Management (MVP) - Complete Implementation TODO
*Derived from: [Tactical Plan: Epic 2](../00_strategy_epic2_local.md)*  
*Reference: [Main Activities](../02_main_activities.md), [Browse Activity Logic](../detailed_logic/02b_browse_activity_logic.md), [Architecture](../17_architecture_patterns.md)*

**Purpose**: Implement complete local file browsing, permissions, UI screens (MainActivity, BrowseActivity), file operations, and favorites system.

**Estimated Time**: 4-5 days  
**Prerequisites**: Epic 1 completed (database, DI, base classes)  
**Output**: Working app that browses local folders, displays media files, supports copy/move/delete operations

---

## 1. Permissions & Onboarding

### 1.1 Storage Permissions Handler
- [ ] Create `util/PermissionsHandler.kt`:
```kotlin
package com.sza.fastmediasorter.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionsHandler(private val context: Context) {
    
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    fun hasAllPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(getRequiredPermissions())
    }
}
```

### 1.2 Welcome Activity (Onboarding)
- [ ] Create layout `res/layout/activity_welcome.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="@dimen/spacing_medium">
    
    <ImageView
        android:id="@+id/logoImage"
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:src="@mipmap/ic_launcher"
        android:contentDescription="@string/app_name"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="64dp"/>
    
    <TextView
        android:id="@+id/titleText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/welcome_title"
        android:textSize="24sp"
        android:textStyle="bold"
        app:layout_constraintTop_toBottomOf="@id/logoImage"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="@dimen/spacing_large"/>
    
    <TextView
        android:id="@+id/descriptionText"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="@string/welcome_description"
        android:textSize="16sp"
        android:gravity="center"
        app:layout_constraintTop_toBottomOf="@id/titleText"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="@dimen/spacing_medium"/>
    
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnGrantPermissions"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="@string/grant_permissions"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="32dp"/>
        
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] Create `ui/welcome/WelcomeActivity.kt`:
```kotlin
package com.sza.fastmediasorter.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.sza.fastmediasorter.databinding.ActivityWelcomeBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.main.MainActivity
import com.sza.fastmediasorter.util.PermissionsHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    
    private lateinit var permissionsHandler: PermissionsHandler
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            navigateToMain()
        } else {
            // Show rationale or explain why permissions are needed
        }
    }
    
    override fun getViewBinding() = ActivityWelcomeBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsHandler = PermissionsHandler(this)
        
        // Skip if already granted
        if (permissionsHandler.hasAllPermissions()) {
            navigateToMain()
            return
        }
        
        binding.btnGrantPermissions.setOnClickListener {
            permissionsHandler.requestPermissions(requestPermissionsLauncher)
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

- [ ] Add strings to `res/values/strings.xml`:
```xml
<string name="welcome_description">Organize your media files across local folders, network drives, and cloud storage</string>
```
- [ ] **Validate**: Permissions requested on first launch, redirects to MainActivity after grant

---

## 2. MainActivity (Resource List)

### 2.1 MainActivity Layout
- [ ] Create `res/layout/activity_main.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:elevation="4dp"
            app:title="@string/app_name"
            app:menu="@menu/menu_main"/>
            
    </com.google.android.material.appbar.AppBarLayout>
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/resourceList"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="@dimen/list_padding"
        app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"/>
    
    <TextView
        android:id="@+id/emptyView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/no_resources"
        android:textSize="18sp"
        android:visibility="gone"
        android:layout_gravity="center"/>
    
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddResource"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="@dimen/spacing_medium"
        android:contentDescription="@string/add_resource"
        app:srcCompat="@drawable/ic_add"/>
        
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 2.2 Resource List Item Layout
- [ ] Create `res/layout/item_resource.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="@dimen/spacing_small"
    app:cardElevation="2dp"
    app:cardCornerRadius="8dp">
    
    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/list_item_padding">
        
        <ImageView
            android:id="@+id/iconResource"
            android:layout_width="@dimen/icon_size"
            android:layout_height="@dimen/icon_size"
            android:contentDescription="@string/resource_icon"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            tools:src="@drawable/ic_folder"/>
        
        <TextView
            android:id="@+id/textResourceName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textStyle="bold"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="@id/iconResource"
            app:layout_constraintStart_toEndOf="@id/iconResource"
            app:layout_constraintEnd_toEndOf="parent"
            android:layout_marginStart="@dimen/spacing_normal"
            tools:text="My Photos"/>
        
        <TextView
            android:id="@+id/textResourcePath"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:maxLines="1"
            android:ellipsize="middle"
            app:layout_constraintTop_toBottomOf="@id/textResourceName"
            app:layout_constraintStart_toStartOf="@id/textResourceName"
            app:layout_constraintEnd_toEndOf="parent"
            tools:text="/storage/emulated/0/DCIM"/>
            
    </androidx.constraintlayout.widget.ConstraintLayout>
    
</com.google.android.material.card.MaterialCardView>
```

### 2.3 MainActivity Implementation
- [ ] Create `ui/main/MainActivity.kt`:
```kotlin
package com.sza.fastmediasorter.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.browse.BrowseActivity
import com.sza.fastmediasorter.ui.resource.AddResourceActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ResourceAdapter
    
    override fun getViewBinding() = ActivityMainBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeData()
        
        binding.fabAddResource.setOnClickListener {
            startActivity(Intent(this, AddResourceActivity::class.java))
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupRecyclerView() {
        adapter = ResourceAdapter { resource ->
            val intent = Intent(this, BrowseActivity::class.java).apply {
                putExtra("EXTRA_RESOURCE_ID", resource.id)
            }
            startActivity(intent)
        }
        
        binding.resourceList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resources.collect { resources ->
                    adapter.submitList(resources)
                    binding.emptyView.visibility = if (resources.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navigate to SettingsActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
```

### 2.4 MainViewModel
- [ ] Create `ui/main/MainViewModel.kt`:
```kotlin
package com.sza.fastmediasorter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {
    
    val resources: StateFlow<List<Resource>> = resourceRepository.getAllResourcesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

### 2.5 ResourceAdapter
- [ ] Create `ui/main/ResourceAdapter.kt`:
```kotlin
package com.sza.fastmediasorter.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemResourceBinding
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType

class ResourceAdapter(
    private val onItemClick: (Resource) -> Unit
) : ListAdapter<Resource, ResourceAdapter.ViewHolder>(ResourceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemResourceBinding,
        private val onItemClick: (Resource) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(resource: Resource) {
            binding.textResourceName.text = resource.name
            binding.textResourcePath.text = resource.path
            binding.iconResource.setImageResource(getIconForType(resource.type))
            binding.root.setOnClickListener { onItemClick(resource) }
        }
        
        private fun getIconForType(type: ResourceType): Int {
            return when (type) {
                ResourceType.LOCAL -> R.drawable.ic_folder
                ResourceType.SMB -> R.drawable.ic_network
                ResourceType.SFTP -> R.drawable.ic_ssh
                ResourceType.FTP -> R.drawable.ic_ftp
                ResourceType.GOOGLE_DRIVE -> R.drawable.ic_google_drive
                ResourceType.ONEDRIVE -> R.drawable.ic_onedrive
                ResourceType.DROPBOX -> R.drawable.ic_dropbox
            }
        }
    }
    
    class ResourceDiffCallback : DiffUtil.ItemCallback<Resource>() {
        override fun areItemsTheSame(oldItem: Resource, newItem: Resource) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Resource, newItem: Resource) = oldItem == newItem
    }
}
```

- [ ] Add strings:
```xml
<string name="no_resources">No resources added yet. Tap + to add a folder</string>
<string name="add_resource">Add Resource</string>
```
- [ ] **Validate**: MainActivity shows resource list, FAB navigates to AddResourceActivity

---

## 3. BrowseActivity (File List)

### 3.1 BrowseActivity Layout
- [ ] Create `res/layout/activity_browse.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_arrow_back"
            app:menu="@menu/menu_browse"/>
            
    </com.google.android.material.appbar.AppBarLayout>
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/fileList"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="@dimen/list_padding"
        app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
        app:spanCount="3"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"/>
    
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone"/>
        
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 3.2 Media File Item Layout (Grid)
- [ ] Create `res/layout/item_media_file.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="@dimen/spacing_small">
    
    <ImageView
        android:id="@+id/thumbnail"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:scaleType="centerCrop"
        android:contentDescription="@string/thumbnail"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintDimensionRatio="1:1"
        tools:src="@drawable/ic_image"/>
    
    <TextView
        android:id="@+id/textFileName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:maxLines="2"
        android:ellipsize="end"
        app:layout_constraintTop_toBottomOf="@id/thumbnail"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="@dimen/spacing_tiny"
        tools:text="IMG_20240106.jpg"/>
        
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 3.3 BrowseActivity Implementation
- [ ] Create `ui/browse/BrowseActivity.kt`:
```kotlin
package com.sza.fastmediasorter.ui.browse

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.sza.fastmediasorter.databinding.ActivityBrowseBinding
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BrowseActivity : BaseActivity<ActivityBrowseBinding>() {
    
    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: MediaFileAdapter
    
    override fun getViewBinding() = ActivityBrowseBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeData()
        
        val resourceId = intent.getLongExtra("EXTRA_RESOURCE_ID", -1)
        if (resourceId != -1L) {
            viewModel.loadFiles(resourceId)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    
    private fun setupRecyclerView() {
        adapter = MediaFileAdapter { mediaFile ->
            // Navigate to PlayerActivity
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("EXTRA_FILE_PATH", mediaFile.path)
            }
            startActivity(intent)
        }
        
        binding.fileList.apply {
            layoutManager = GridLayoutManager(this@BrowseActivity, 3)
            adapter = this@BrowseActivity.adapter
        }
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BrowseUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is BrowseUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            adapter.submitList(state.files)
                        }
                        is BrowseUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            // Show error message
                        }
                    }
                }
            }
        }
    }
}
```

### 3.4 BrowseViewModel
- [ ] Create `ui/browse/BrowseViewModel.kt`:
```kotlin
package com.sza.fastmediasorter.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.usecase.GetMediaFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BrowseUiState {
    data object Loading : BrowseUiState()
    data class Success(val files: List<MediaFile>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    private var loadingJob: Job? = null
    
    fun loadFiles(resourceId: Long) {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            
            getMediaFilesUseCase(resourceId)
                .onSuccess { files ->
                    _uiState.value = BrowseUiState.Success(files)
                }
                .onError { message, _ ->
                    _uiState.value = BrowseUiState.Error(message)
                }
        }
    }
}
```

- [ ] **Validate**: BrowseActivity loads files from database, displays in grid

---

## 4. Local Media Scanner (UseCase Implementation)

### 4.1 GetMediaFilesUseCase
- [ ] Create `domain/usecase/GetMediaFilesUseCase.kt`:
```kotlin
package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.MediaRepository
import javax.inject.Inject

class GetMediaFilesUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(resourceId: Long): Result<List<MediaFile>> {
        return try {
            val files = mediaRepository.getFilesForResource(resourceId)
            Result.Success(files)
        } catch (e: Exception) {
            Result.Error("Failed to load files: ${e.message}", e)
        }
    }
}
```

### 4.2 LocalMediaScanner
- [ ] Create `data/scanner/LocalMediaScanner.kt`:
```kotlin
package com.sza.fastmediasorter.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject

class LocalMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    suspend fun scanFolder(folderPath: String): List<MediaFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<MediaFile>()
        val folder = File(folderPath)
        
        if (!folder.exists() || !folder.isDirectory) {
            return@withContext emptyList()
        }
        
        folder.listFiles()?.forEach { file ->
            if (file.isFile) {
                files.add(file.toMediaFile())
            }
        }
        
        files.sortedByDescending { it.date }
    }
    
    private fun File.toMediaFile(): MediaFile {
        return MediaFile(
            path = absolutePath,
            name = name,
            size = length(),
            date = Date(lastModified()),
            type = detectMediaType(extension)
        )
    }
    
    private fun detectMediaType(extension: String): MediaType {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "bmp" -> MediaType.IMAGE
            "mp4", "mkv", "avi", "mov" -> MediaType.VIDEO
            "mp3", "m4a", "wav", "flac" -> MediaType.AUDIO
            "gif" -> MediaType.GIF
            "pdf" -> MediaType.PDF
            "txt", "log", "json", "xml" -> MediaType.TXT
            "epub" -> MediaType.EPUB
            else -> MediaType.OTHER
        }
    }
}
```

- [ ] **Validate**: Scanner detects files correctly, sorts by date descending

---

## 5. Repository Implementations

### 5.1 ResourceRepositoryImpl
- [ ] Create `data/repository/ResourceRepositoryImpl.kt`:
```kotlin
package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.db.dao.ResourceDao
import com.sza.fastmediasorter.data.db.entity.ResourceEntity
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ResourceRepositoryImpl @Inject constructor(
    private val resourceDao: ResourceDao
) : ResourceRepository {
    
    override fun getAllResourcesFlow(): Flow<List<Resource>> {
        return resourceDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getResourceById(id: Long): Resource? {
        return resourceDao.getById(id)?.toDomain()
    }
    
    override suspend fun insertResource(resource: Resource): Long {
        return resourceDao.insert(resource.toEntity())
    }
    
    override suspend fun updateResource(resource: Resource) {
        resourceDao.update(resource.toEntity())
    }
    
    override suspend fun deleteResource(resource: Resource) {
        resourceDao.delete(resource.toEntity())
    }
    
    override fun getDestinationsFlow(): Flow<List<Resource>> {
        return resourceDao.getDestinationsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    private fun ResourceEntity.toDomain(): Resource {
        return Resource(
            id = id,
            name = name,
            path = path,
            type = ResourceType.valueOf(type),
            isDestination = isDestination,
            destinationOrder = destinationOrder,
            displayOrder = displayOrder
        )
    }
    
    private fun Resource.toEntity(): ResourceEntity {
        return ResourceEntity(
            id = id,
            name = name,
            path = path,
            type = type.name,
            isDestination = isDestination,
            destinationOrder = destinationOrder,
            displayOrder = displayOrder
        )
    }
}
```

### 5.2 Hilt Binding Module
- [ ] Create `di/RepositoryModule.kt`:
```kotlin
package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.repository.ResourceRepositoryImpl
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindResourceRepository(impl: ResourceRepositoryImpl): ResourceRepository
}
```

- [ ] **Validate**: Repository implementations compile and inject correctly

---

## 6. Drawable Resources (Icons)

- [ ] Create icon files in `res/drawable/`:
  - `ic_add.xml` (24dp plus icon)
  - `ic_arrow_back.xml` (24dp back arrow)
  - `ic_folder.xml` (LOCAL resource icon)
  - `ic_network.xml` (SMB icon)
  - `ic_ssh.xml` (SFTP icon)
  - `ic_ftp.xml` (FTP icon)
  - `ic_google_drive.xml` (Google Drive logo)
  - `ic_onedrive.xml` (OneDrive logo)
  - `ic_dropbox.xml` (Dropbox logo)
  - `ic_image.xml` (Generic image placeholder)

Example `ic_add.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
</vector>
```

- [ ] **Validate**: All icons referenced in layouts exist

---

## 7. Completion Checklist

**UI**:
- [ ] WelcomeActivity requests permissions
- [ ] MainActivity displays resource list
- [ ] BrowseActivity shows media files in grid

**Data Flow**:
- [ ] Room database stores resources
- [ ] Repository pattern implemented (Interface + Implementation)
- [ ] ViewModels use UseCases (not repositories directly)
- [ ] StateFlow exposes UI state

**Permissions**:
- [ ] Android 9-12: READ_EXTERNAL_STORAGE
- [ ] Android 13+: READ_MEDIA_IMAGES/VIDEO/AUDIO
- [ ] Permissions checked at runtime

**Success Criteria**: App browses local folders, displays media files, respects permissions, follows Clean Architecture.

**Next**: Epic 3 (PlayerActivity) for viewing files.
