# 28. State Management Strategy

**Last Updated**: January 6, 2026  
**Purpose**: Comprehensive guide for managing state in FastMediaSorter v2 using Kotlin Coroutines Flow.

This document establishes patterns for reactive state management, event handling, and configuration survival in MVVM architecture.

---

## Overview

FastMediaSorter uses **Kotlin Coroutines Flow** as the primary state management mechanism, replacing LiveData for better coroutine integration and type safety.

### Key Principles

1. **Single Source of Truth**: ViewModel holds UI state, Repository holds data
2. **Unidirectional Data Flow**: UI → ViewModel → UseCase → Repository → DB → Flow → ViewModel → UI
3. **Separation of State and Events**: `StateFlow` for UI state, `SharedFlow` for one-time events
4. **Configuration Survival**: `SavedStateHandle` for process death recovery

---

## Table of Contents

1. [Flow Types Comparison](#1-flow-types-comparison)
2. [ViewModel State Patterns](#2-viewmodel-state-patterns)
3. [Event Handling](#3-event-handling)
4. [Configuration Changes](#4-configuration-changes)
5. [Loading States](#5-loading-states)
6. [Error Handling Integration](#6-error-handling-integration)
7. [Best Practices](#7-best-practices)
8. [Anti-Patterns to Avoid](#8-anti-patterns-to-avoid)

---

## 1. Flow Types Comparison

### When to Use Each Type

| Type | Use Case | Behavior | Example |
|------|----------|----------|---------|
| **StateFlow** | UI state that always has a value | Hot, replays latest value to new collectors | Current resource list, loading state |
| **SharedFlow** | One-time events | Hot, configurable replay | Navigation, toasts, dialogs |
| **Flow** (cold) | Data streams from Repository | Cold, starts on collection | Database queries, network requests |

### StateFlow vs LiveData

| Feature | StateFlow | LiveData |
|---------|-----------|----------|
| Coroutine-native | ✅ Yes | ❌ No |
| Type-safe null | ✅ Non-nullable by default | ❌ Nullable by default |
| Lifecycle-aware | ❌ No (use `repeatOnLifecycle`) | ✅ Yes |
| Initial value | ✅ Required | ❌ Optional |
| Threading | Any dispatcher | Main thread only |
| Testing | ✅ Easy with `TestScope` | ❌ Requires `InstantTaskExecutorRule` |

**Decision**: Use StateFlow for all new code.

---

## 2. ViewModel State Patterns

### Pattern 1: Simple StateFlow

**Use Case**: Single piece of UI state.

```kotlin
@HiltViewModel
class SimpleViewModel @Inject constructor(
    private val repository: ResourceRepository
) : ViewModel() {
    
    // Private mutable, public immutable
    private val _resources = MutableStateFlow<List<MediaResource>>(emptyList())
    val resources: StateFlow<List<MediaResource>> = _resources.asStateFlow()
    
    init {
        loadResources()
    }
    
    private fun loadResources() {
        viewModelScope.launch {
            repository.getAllResources().collect { resources ->
                _resources.value = resources
            }
        }
    }
}
```

**Activity/Fragment**:
```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: SimpleViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resources.collect { resources ->
                    adapter.submitList(resources)
                }
            }
        }
    }
}
```

---

### Pattern 2: UI State Data Class

**Use Case**: Multiple related state pieces (recommended for complex screens).

```kotlin
data class BrowseUiState(
    val files: List<MediaFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val displayMode: DisplayMode = DisplayMode.LIST,
    val selectedCount: Int = 0
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    fun loadFiles(resource: MediaResource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = getMediaFilesUseCase(resource)
            
            _uiState.update { 
                it.copy(
                    files = result.getOrElse { emptyList() },
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
    
    fun setSortMode(sortMode: SortMode) {
        _uiState.update { it.copy(sortMode = sortMode) }
    }
}
```

**Activity**:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            binding.progressBar.isVisible = state.isLoading
            binding.errorText.text = state.error
            binding.errorText.isVisible = state.error != null
            adapter.submitList(state.files)
        }
    }
}
```

**Benefits**:
- Atomic state updates (all fields change together)
- Easy to test (just compare data classes)
- Clear state shape

---

### Pattern 3: Derived State (Computed Properties)

**Use Case**: State computed from other state.

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    
    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files: StateFlow<List<MediaFile>> = _files.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    // Derived state (computed)
    val currentFile: StateFlow<MediaFile?> = combine(
        _files,
        _currentIndex
    ) { files, index ->
        files.getOrNull(index)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
    
    val hasNext: StateFlow<Boolean> = combine(
        _files,
        _currentIndex
    ) { files, index ->
        index < files.size - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val hasPrevious: StateFlow<Boolean> = _currentIndex.map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
```

**Benefits**:
- Automatic updates when source state changes
- No manual synchronization
- Memory-efficient with `WhileSubscribed`

---

### Pattern 4: Loading from Repository (stateIn)

**Use Case**: Convert cold Flow from Repository to hot StateFlow.

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ResourceRepository
) : ViewModel() {
    
    // Cold Flow → Hot StateFlow
    val resources: StateFlow<List<MediaResource>> = repository
        .getAllResources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Stop 5s after last collector
            initialValue = emptyList()
        )
}
```

**SharingStarted Strategies**:

| Strategy | Behavior | Use Case |
|----------|----------|----------|
| `Lazily` | Never stops | Critical data (user settings) |
| `Eagerly` | Starts immediately | Pre-load data |
| `WhileSubscribed(5000)` | Stops 5s after last collector | Most screens (balance memory/responsiveness) |

**Recommendation**: Use `WhileSubscribed(5000)` for most cases.

---

## 3. Event Handling

### The Problem with StateFlow for Events

❌ **Anti-Pattern** (State-based events):
```kotlin
// BAD: Event in UI state
data class UiState(
    val showErrorDialog: Boolean = false, // ❌ Wrong
    val errorMessage: String? = null
)
```

**Problem**: If user rotates screen, `showErrorDialog = true` triggers dialog again.

---

### Solution: SharedFlow for One-Time Events

✅ **Correct Pattern**:

```kotlin
// Sealed class for type-safe events
sealed class BrowseEvent {
    data class ShowError(val message: String) : BrowseEvent()
    data class NavigateToPlayer(val file: MediaFile) : BrowseEvent()
    object ShowDeleteConfirmation : BrowseEvent()
    data class ShowToast(val message: String) : BrowseEvent()
}

@HiltViewModel
class BrowseViewModel @Inject constructor() : ViewModel() {
    
    // State
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    // Events (one-time)
    private val _events = MutableSharedFlow<BrowseEvent>()
    val events: SharedFlow<BrowseEvent> = _events.asSharedFlow()
    
    fun deleteFile(file: MediaFile) {
        viewModelScope.launch {
            _events.emit(BrowseEvent.ShowDeleteConfirmation)
        }
    }
    
    fun confirmDelete(file: MediaFile) {
        viewModelScope.launch {
            val result = deleteUseCase(file)
            if (result.isSuccess) {
                _events.emit(BrowseEvent.ShowToast("Deleted successfully"))
            } else {
                _events.emit(BrowseEvent.ShowError("Delete failed"))
            }
        }
    }
}
```

**Activity**:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseEvent.ShowError -> showErrorDialog(event.message)
                is BrowseEvent.NavigateToPlayer -> navigateToPlayer(event.file)
                is BrowseEvent.ShowDeleteConfirmation -> showDeleteDialog()
                is BrowseEvent.ShowToast -> Toast.makeText(this@BrowseActivity, event.message, LENGTH_SHORT).show()
            }
        }
    }
}
```

**Benefits**:
- Events not replayed on configuration change
- Type-safe with sealed classes
- Clear separation of state vs events

---

### Event Emission Pattern

```kotlin
// ❌ BAD: Exposing MutableSharedFlow
val events: MutableSharedFlow<Event> // Allows external emission

// ✅ GOOD: Private mutable, public immutable
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()
```

---

## 4. Configuration Changes

### Problem: Process Death

Android can kill app process in background. State in ViewModel is lost, but Activity/Fragment is recreated.

### Solution 1: SavedStateHandle (Small Data)

**Use Case**: Primitives, Parcelables (< 1 MB).

```kotlin
@HiltViewModel
class EditResourceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _resourceId = savedStateHandle.getStateFlow("resource_id", -1L)
    val resourceId: StateFlow<Long> = _resourceId
    
    fun setResourceId(id: Long) {
        savedStateHandle["resource_id"] = id
    }
    
    // For complex objects, use JSON
    private val _resource = savedStateHandle.getStateFlow<String?>("resource_json", null)
    
    fun saveResource(resource: MediaResource) {
        savedStateHandle["resource_json"] = Json.encodeToString(resource)
    }
}
```

**Benefits**:
- Survives process death
- Automatic restoration
- Up to 1 MB data

---

### Solution 2: Repository Cache (Large Data)

**Use Case**: Large lists, images.

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: ResourceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Save only ID in SavedStateHandle
    private val resourceId: Long = savedStateHandle["resource_id"] ?: -1L
    
    // Reload from repository on recreate
    val files: StateFlow<List<MediaFile>> = flow {
        if (resourceId != -1L) {
            val resource = repository.getResourceById(resourceId)
            if (resource != null) {
                emit(getMediaFilesUseCase(resource))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

---

### Solution 3: ViewModel with Arguments

**Use Case**: Navigation with arguments.

```kotlin
// Navigation
findNavController().navigate(
    BrowseFragmentDirections.actionToEditResource(resourceId = 42)
)

// ViewModel
@HiltViewModel
class EditResourceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ResourceRepository
) : ViewModel() {
    
    private val resourceId: Long = checkNotNull(savedStateHandle["resourceId"])
    
    val resource: StateFlow<MediaResource?> = flow {
        emit(repository.getResourceById(resourceId))
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
```

---

## 5. Loading States

### Pattern: Loading/Success/Error State

```kotlin
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

@HiltViewModel
class MyViewModel @Inject constructor(
    private val useCase: GetDataUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow<UiState<List<Data>>>(UiState.Idle)
    val state: StateFlow<UiState<List<Data>>> = _state.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            
            runCatching {
                useCase.execute()
            }.onSuccess { data ->
                _state.value = UiState.Success(data)
            }.onFailure { error ->
                _state.value = UiState.Error(error.message ?: "Unknown error", error)
            }
        }
    }
}
```

**UI Handling**:
```kotlin
lifecycleScope.launch {
    viewModel.state.collect { state ->
        when (state) {
            is UiState.Idle -> { /* Show empty state */ }
            is UiState.Loading -> { progressBar.isVisible = true }
            is UiState.Success -> {
                progressBar.isVisible = false
                adapter.submitList(state.data)
            }
            is UiState.Error -> {
                progressBar.isVisible = false
                showErrorDialog(state.message)
            }
        }
    }
}
```

---

## 6. Error Handling Integration

### Pattern: Result Type with State

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getMediaFilesUseCase: GetMediaFilesUseCase
) : ViewModel() {
    
    data class BrowseState(
        val files: List<MediaFile> = emptyList(),
        val isLoading: Boolean = false,
        val error: ErrorState? = null
    )
    
    sealed class ErrorState {
        data class NetworkError(val message: String, val canRetry: Boolean = true) : ErrorState()
        data class PermissionError(val permission: String) : ErrorState()
        data class UnknownError(val message: String) : ErrorState()
    }
    
    private val _state = MutableStateFlow(BrowseState())
    val state: StateFlow<BrowseState> = _state.asStateFlow()
    
    fun loadFiles(resource: MediaResource) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            when (val result = getMediaFilesUseCase(resource)) {
                is Result.Success -> {
                    _state.update { it.copy(files = result.data, isLoading = false) }
                }
                is Result.Failure -> {
                    val error = when (result.exception) {
                        is IOException -> ErrorState.NetworkError(result.message, canRetry = true)
                        is SecurityException -> ErrorState.PermissionError("STORAGE")
                        else -> ErrorState.UnknownError(result.message)
                    }
                    _state.update { it.copy(isLoading = false, error = error) }
                }
            }
        }
    }
}
```

---

## 7. Best Practices

### ✅ DO

1. **Expose Immutable Flow to UI**
```kotlin
private val _state = MutableStateFlow(...)
val state: StateFlow<...> = _state.asStateFlow()
```

2. **Use `StateFlow.update` for Thread-Safe Updates**
```kotlin
_state.update { currentState ->
    currentState.copy(count = currentState.count + 1)
}
```

3. **Use `repeatOnLifecycle` in Activities/Fragments**
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { /* Update UI */ }
    }
}
```

4. **Separate State and Events**
```kotlin
val state: StateFlow<UiState> // For state
val events: SharedFlow<Event> // For one-time actions
```

5. **Use Data Classes for Complex State**
```kotlin
data class UiState(
    val field1: Type1,
    val field2: Type2,
    // ...
)
```

---

### ❌ DON'T

1. **Don't Expose Mutable Flow**
```kotlin
// ❌ BAD
val state: MutableStateFlow<...> // Anyone can emit
```

2. **Don't Use StateFlow for Events**
```kotlin
// ❌ BAD
data class UiState(
    val showDialog: Boolean // Will retrigger on rotation
)
```

3. **Don't Collect in `onCreate` Without Lifecycle**
```kotlin
// ❌ BAD (keeps collecting even when paused)
viewModel.state.collect { ... }
```

4. **Don't Block Main Thread**
```kotlin
// ❌ BAD
val data = runBlocking { repository.getData() }
```

5. **Don't Use LiveData for New Code**
```kotlin
// ❌ BAD (use StateFlow instead)
val state = MutableLiveData<UiState>()
```

---

## 8. Anti-Patterns to Avoid

### Anti-Pattern 1: Collecting in onResume

```kotlin
// ❌ BAD: New collection on every resume
override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
        viewModel.state.collect { /* ... */ }
    }
}

// ✅ GOOD: Use repeatOnLifecycle
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { /* ... */ }
        }
    }
}
```

---

### Anti-Pattern 2: Stateful Events

```kotlin
// ❌ BAD: Boolean event in state
data class UiState(
    val navigateToPlayer: Boolean = false
)

// After handling navigation:
viewModel.clearNavigationFlag() // Manual cleanup required

// ✅ GOOD: Use SharedFlow
sealed class Event {
    data class NavigateToPlayer(val file: MediaFile) : Event()
}
```

---

### Anti-Pattern 3: Multiple MutableStateFlows for Related Data

```kotlin
// ❌ BAD: Inconsistent intermediate states
private val _name = MutableStateFlow("")
private val _age = MutableStateFlow(0)
private val _email = MutableStateFlow("")

fun updateUser() {
    _name.value = "Alice"
    _age.value = 30
    _email.value = "alice@example.com"
    // Brief moment where name=Alice but age still 0
}

// ✅ GOOD: Single data class
data class UserState(
    val name: String = "",
    val age: Int = 0,
    val email: String = ""
)

private val _state = MutableStateFlow(UserState())

fun updateUser() {
    _state.value = UserState(name = "Alice", age = 30, email = "alice@example.com")
    // Atomic update
}
```

---

## Testing State Management

### 1. Testing StateFlow

```kotlin
@Test
fun `loadResources updates state`() = runTest {
    val repository = FakeResourceRepository()
    val viewModel = MainViewModel(repository)
    
    // Collect state
    val states = mutableListOf<List<MediaResource>>()
    val job = launch {
        viewModel.resources.collect { states.add(it) }
    }
    
    // Trigger action
    viewModel.loadResources()
    advanceUntilIdle()
    
    // Assert
    assertEquals(2, states.size) // Initial empty + loaded
    assertTrue(states.last().isNotEmpty())
    
    job.cancel()
}
```

---

### 2. Testing Events (SharedFlow)

```kotlin
@Test
fun `deleteFile emits confirmation event`() = runTest {
    val viewModel = BrowseViewModel(mockUseCase)
    
    val events = mutableListOf<BrowseEvent>()
    val job = launch {
        viewModel.events.collect { events.add(it) }
    }
    
    viewModel.deleteFile(testFile)
    advanceUntilIdle()
    
    assertEquals(1, events.size)
    assertTrue(events.first() is BrowseEvent.ShowDeleteConfirmation)
    
    job.cancel()
}
```

---

### 3. Testing UI State Data Class

```kotlin
@Test
fun `loadFiles updates state correctly`() = runTest {
    val viewModel = BrowseViewModel(mockUseCase)
    
    // Initial state
    assertEquals(BrowseUiState(), viewModel.uiState.value)
    
    // Trigger load
    viewModel.loadFiles(testResource)
    advanceUntilIdle()
    
    // Assert final state
    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertTrue(finalState.files.isNotEmpty())
    assertNull(finalState.error)
}
```

---

## Migration Guide: LiveData → StateFlow

### Before (LiveData)
```kotlin
class OldViewModel : ViewModel() {
    private val _data = MutableLiveData<List<Item>>()
    val data: LiveData<List<Item>> = _data
    
    fun loadData() {
        _data.value = repository.getData()
    }
}

// Activity
viewModel.data.observe(this) { data ->
    adapter.submitList(data)
}
```

### After (StateFlow)
```kotlin
class NewViewModel : ViewModel() {
    private val _data = MutableStateFlow<List<Item>>(emptyList())
    val data: StateFlow<List<Item>> = _data.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _data.value = repository.getData()
        }
    }
}

// Activity
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect { data ->
            adapter.submitList(data)
        }
    }
}
```

---

## Summary Decision Matrix

| Scenario | Solution | Example |
|----------|----------|---------|
| UI state with value | `StateFlow` | Resource list, loading flag |
| UI state without initial value | `StateFlow` with nullable/sealed class | Selected item, error |
| One-time events | `SharedFlow` | Navigation, toasts, dialogs |
| Data from Repository | Cold Flow → `stateIn()` | Database queries |
| Derived state | `combine()` + `stateIn()` | File count, has next/previous |
| Process death | `SavedStateHandle` | Current resource ID |
| Complex screen state | UI State data class | Loading + data + error |

---

## Reference Files

### Source Code Examples
- **StateFlow Example**: `ui/main/MainViewModel.kt`
- **Events Example**: `ui/browse/BrowseViewModel.kt`
- **UI State Class**: `ui/player/PlayerViewModel.kt`
- **SavedStateHandle**: `ui/editresource/EditResourceViewModel.kt`

### Related Documents
- [27. API Contracts & Interfaces](27_api_contracts.md) - Repository Flow types
- [21. Common Pitfalls](21_common_pitfalls.md) - Parallel loading race conditions

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
