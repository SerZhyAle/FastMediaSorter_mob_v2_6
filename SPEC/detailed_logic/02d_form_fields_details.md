# EditResourceActivity - Form Fields Details

**Source**: Items 13-22 from MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md  
**File**: `EditResourceActivity.kt` (719 lines)

---

## Form Field TextWatchers

### 13. etResourceName TextWatcher

**Location**: `EditResourceActivity.setupTextWatchers()`

**Implementation**:
```kotlin
private fun setupTextWatchers() {
    // Resource Name
    etResourceName.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(s: Editable?) {
            val newName = s?.toString() ?: ""
            
            // Only update if text differs (prevents infinite loop)
            if (newName != viewModel.state.value.currentResource?.name) {
                viewModel.updateName(newName)
            }
        }
    })
}
```

**Infinite Loop Prevention**: Checks if text differs before calling ViewModel update (otherwise `observeData` would re-set text → trigger listener again).

---

### 14. etComment TextWatcher

**Implementation**:
```kotlin
private fun setupTextWatchers() {
    // ...
    
    // Resource Comment
    etComment.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(s: Editable?) {
            val newComment = s?.toString() ?: ""
            
            // Only update if text differs
            if (newComment != viewModel.state.value.currentResource?.comment) {
                viewModel.updateComment(newComment)
            }
        }
    })
}
```

---

### 15. etAccessPassword TextWatcher

**Implementation**:
```kotlin
private fun setupTextWatchers() {
    // ...
    
    // Access PIN/Password
    etAccessPassword.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(s: Editable?) {
            val newPin = s?.toString() ?: ""
            
            // Only update if text differs
            if (newPin != viewModel.state.value.currentResource?.accessPin) {
                viewModel.updateAccessPin(newPin)
            }
        }
    })
}
```

**Purpose**: Sets optional PIN for resource access (prompts user when browsing this resource).

---

### 16. etSlideshowInterval (AutoCompleteTextView)

**Field Type**: `AutoCompleteTextView` with predefined interval suggestions.

**Setup**:
```kotlin
private fun setupSlideshowIntervalField() {
    // Dropdown options: 1, 3, 5, 10, 30, 60 seconds
    val intervals = arrayOf("1", "3", "5", "10", "30", "60")
    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intervals)
    
    etSlideshowInterval.setAdapter(adapter)
    
    // Item click from dropdown
    etSlideshowInterval.setOnItemClickListener { parent, view, position, id ->
        val selected = intervals[position].toIntOrNull() ?: 3
        viewModel.updateSlideshowInterval(selected)
        
        Timber.d("Slideshow interval selected from dropdown: $selected")
    }
    
    // Focus change (manual text entry)
    etSlideshowInterval.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            // Parse user input
            val input = etSlideshowInterval.text.toString().toIntOrNull()
            
            // Clamp to valid range: 1-3600 seconds (1 sec to 1 hour)
            val clamped = input?.coerceIn(1, 3600) ?: 3
            
            // Update ViewModel
            viewModel.updateSlideshowInterval(clamped)
            
            // Update field with clamped value (if user entered invalid)
            if (input != clamped) {
                etSlideshowInterval.setText(clamped.toString(), false) // false = don't show dropdown
                
                Toast.makeText(
                    this,
                    R.string.slideshow_interval_clamped,
                    Toast.LENGTH_SHORT
                ).show()
                
                Timber.d("Slideshow interval clamped: $input -> $clamped")
            }
        }
    }
}
```

**AutoCompleteTextView.setText(text, filter)**:
- `setText(text, true)` - Shows dropdown suggestions
- `setText(text, false)` - Sets text without triggering dropdown

**Clamping Logic**: Ensures interval is between 1-3600 seconds. Shows toast if user entered invalid value.

---

### 17. Media Type Checkboxes (7 checkboxes)

**Checkboxes**:
1. `cbSupportImages` - IMAGE
2. `cbSupportVideos` - VIDEO
3. `cbSupportAudio` - AUDIO
4. `cbSupportGif` - GIF
5. `cbSupportText` - TEXT
6. `cbSupportPdf` - PDF
7. `cbSupportEpub` - EPUB

**Setup**:
```kotlin
private fun setupMediaTypeCheckboxes() {
    // Images
    cbSupportImages.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.IMAGE, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.IMAGE, added = false)
        }
    }
    
    // Videos
    cbSupportVideos.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.VIDEO, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.VIDEO, added = false)
        }
    }
    
    // Audio
    cbSupportAudio.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.AUDIO, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.AUDIO, added = false)
        }
    }
    
    // GIF
    cbSupportGif.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.GIF, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.GIF, added = false)
        }
    }
    
    // Text
    cbSupportText.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.TEXT, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.TEXT, added = false)
        }
    }
    
    // PDF
    cbSupportPdf.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.PDF, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.PDF, added = false)
        }
    }
    
    // EPUB
    cbSupportEpub.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            viewModel.updateSupportedMediaTypes(MediaType.EPUB, added = true)
        } else {
            viewModel.updateSupportedMediaTypes(MediaType.EPUB, added = false)
        }
    }
}
```

**updateSupportedMediaTypes() Method**:
```kotlin
// In ViewModel
fun updateSupportedMediaTypes(type: MediaType, added: Boolean) {
    val current = _state.value.currentResource ?: return
    
    val updatedTypes = if (added) {
        current.supportedMediaTypes + type
    } else {
        current.supportedMediaTypes - type
    }
    
    updateCurrentResource(current.copy(supportedMediaTypes = updatedTypes))
    
    Timber.d("Media type ${if (added) "added" else "removed"}: $type")
}
```

---

### 18. cbScanSubdirectories Listener

**Implementation**:
```kotlin
private fun setupFlagCheckboxes() {
    // Scan Subdirectories
    cbScanSubdirectories.setOnCheckedChangeListener { _, isChecked ->
        viewModel.updateScanSubdirectories(isChecked)
        
        Timber.d("Scan subdirectories toggled: $isChecked")
    }
}
```

**ViewModel Method**:
```kotlin
fun updateScanSubdirectories(scan: Boolean) {
    val current = _state.value.currentResource ?: return
    updateCurrentResource(current.copy(scanSubdirectories = scan))
}
```

---

### 19. cbDisableThumbnails Listener

**Implementation**:
```kotlin
private fun setupFlagCheckboxes() {
    // ...
    
    // Disable Thumbnails (for slow networks)
    cbDisableThumbnails.setOnCheckedChangeListener { _, isChecked ->
        viewModel.updateDisableThumbnails(isChecked)
        
        Timber.d("Disable thumbnails toggled: $isChecked")
    }
}
```

**ViewModel Method**:
```kotlin
fun updateDisableThumbnails(disable: Boolean) {
    val current = _state.value.currentResource ?: return
    updateCurrentResource(current.copy(disableThumbnails = disable))
}
```

**Purpose**: Disables thumbnail loading when browsing resource (useful for slow SMB/SFTP connections). Files shown with generic type icons instead.

---

### 20. cbReadOnlyMode Listener

**Implementation**:
```kotlin
private fun setupFlagCheckboxes() {
    // ...
    
    // Read-Only Mode
    cbReadOnlyMode.setOnCheckedChangeListener { _, isChecked ->
        viewModel.updateReadOnlyMode(isChecked)
        
        Timber.d("Read-only mode toggled: $isChecked")
    }
}
```

**ViewModel Method**:
```kotlin
fun updateReadOnlyMode(readOnly: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // If enabling read-only, force isDestination = false
    val updated = if (readOnly) {
        current.copy(
            isReadOnly = true,
            isDestination = false
        )
    } else {
        current.copy(isReadOnly = false)
    }
    
    updateCurrentResource(updated)
    
    Timber.d("Read-only mode: $readOnly, forced isDestination=false")
}
```

**Mutual Exclusion**: Read-only resources cannot be destinations (copy/move targets).

---

### 21. switchIsDestination Listener

**Implementation**:
```kotlin
private fun setupDestinationSwitch() {
    switchIsDestination.setOnCheckedChangeListener { _, isChecked ->
        // Log user action
        userActionLogger.logAction(
            action = if (isChecked) "AddToDestinations" else "RemoveFromDestinations",
            screen = "EditResource",
            details = viewModel.state.value.currentResource?.name
        )
        
        // Validate if can be destination
        if (isChecked && !viewModel.state.value.canBeDestination) {
            // Cannot add as destination (max limit reached or read-only)
            Toast.makeText(
                this,
                R.string.cannot_add_destination_limit_reached,
                Toast.LENGTH_SHORT
            ).show()
            
            // Revert switch (will be handled by observeData)
            viewModel.updateIsDestination(false)
            
            Timber.w("Cannot add destination: canBeDestination=false")
            return@setOnCheckedChangeListener
        }
        
        // Update ViewModel
        viewModel.updateIsDestination(isChecked)
        
        Timber.d("Destination switch toggled: $isChecked")
    }
}
```

**ViewModel Method**:
```kotlin
fun updateIsDestination(isDestination: Boolean) {
    val current = _state.value.currentResource ?: return
    
    // Validate can be destination
    if (isDestination && (!_state.value.canBeDestination || current.isReadOnly)) {
        Timber.w("Cannot set as destination: canBeDestination=${_state.value.canBeDestination}, isReadOnly=${current.isReadOnly}")
        return
    }
    
    updateCurrentResource(current.copy(isDestination = isDestination))
}
```

**Validation Rules**:
1. If already destination → can always remove (toggle off)
2. If read-only → cannot add as destination
3. If `destinationsCount >= maxDestinations` (10) → cannot add as destination
4. Otherwise → can add as destination

---

### 22. TextWatcher Management Methods

**Purpose**: Dynamically add/remove listeners to prevent infinite loops during programmatic updates.

**addTextWatchers() Method**:
```kotlin
private fun addTextWatchers() {
    // Resource Name
    etResourceName.addTextChangedListener(resourceNameWatcher)
    
    // Comment
    etComment.addTextChangedListener(commentWatcher)
    
    // Access PIN
    etAccessPassword.addTextChangedListener(accessPinWatcher)
    
    // Slideshow Interval (AutoCompleteTextView has item click listener, not TextWatcher)
    
    Timber.d("Text watchers added")
}
```

**removeTextWatchers() Method**:
```kotlin
private fun removeTextWatchers() {
    // Resource Name
    etResourceName.removeTextChangedListener(resourceNameWatcher)
    
    // Comment
    etComment.removeTextChangedListener(commentWatcher)
    
    // Access PIN
    etAccessPassword.removeTextChangedListener(accessPinWatcher)
    
    Timber.d("Text watchers removed")
}
```

**Watcher References** (stored as properties):
```kotlin
private lateinit var resourceNameWatcher: TextWatcher
private lateinit var commentWatcher: TextWatcher
private lateinit var accessPinWatcher: TextWatcher
```

**Initialization**:
```kotlin
private fun initializeTextWatchers() {
    // Resource Name Watcher
    resourceNameWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val newName = s?.toString() ?: ""
            if (newName != viewModel.state.value.currentResource?.name) {
                viewModel.updateName(newName)
            }
        }
    }
    
    // Comment Watcher
    commentWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val newComment = s?.toString() ?: ""
            if (newComment != viewModel.state.value.currentResource?.comment) {
                viewModel.updateComment(newComment)
            }
        }
    }
    
    // Access PIN Watcher
    accessPinWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val newPin = s?.toString() ?: ""
            if (newPin != viewModel.state.value.currentResource?.accessPin) {
                viewModel.updateAccessPin(newPin)
            }
        }
    }
}
```

**Usage in observeData**:
```kotlin
private fun observeData() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                // Remove watchers before updating fields
                removeTextWatchers()
                
                // Update fields programmatically
                state.currentResource?.let { resource ->
                    etResourceName.setText(resource.name)
                    etComment.setText(resource.comment)
                    etAccessPassword.setText(resource.accessPin)
                }
                
                // Re-add watchers after updates
                addTextWatchers()
            }
        }
    }
}
```

**Pattern**: Prevents TextWatcher.afterTextChanged() from firing during programmatic setText() (which would call ViewModel again → infinite loop).

---

## Summary

**Items Documented**: 13-22 (10 behaviors)

**Form Fields**:
- 3 TextWatchers with loop prevention (items 13-15)
- AutoCompleteTextView with dropdown + focus clamping (item 16)
- 7 media type checkboxes (item 17)
- 3 flag checkboxes (items 18-20)
- Destination switch with validation (item 21)
- TextWatcher management methods (item 22)

**Key Patterns**:
- **Loop Prevention**: Only call ViewModel if text differs from current state
- **Listener Management**: Remove listeners before programmatic updates, re-add after
- **AutoCompleteTextView**: Separate logic for dropdown selection (setOnItemClickListener) and manual entry (onFocusChangeListener)
- **Clamping**: Slideshow interval constrained to 1-3600 seconds
- **Mutual Exclusion**: Read-only mode forces isDestination=false
- **Validation**: Destination switch checks canBeDestination flag before allowing toggle on

**Total TextWatchers**: 11 listeners across all fields:
1. etResourceName
2. etComment
3. etAccessPassword
4. 7 media type checkboxes (via setOnCheckedChangeListener)
5. cbScanSubdirectories
6. cbDisableThumbnails
7. cbReadOnlyMode
8. switchIsDestination
