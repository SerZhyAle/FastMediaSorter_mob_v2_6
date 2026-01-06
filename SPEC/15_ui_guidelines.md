# 15. UI Guidelines

## Design System
- **Framework:** Material Design 3 (Material You).
- **Colors:** Dynamic colors (Android 12+) or App-specific TEAL/PURPLE theme.
- **Dark Mode:** Fully supported and tested.

## Components
- **Buttons:** MaterialButton (Filled for primary action, Outlined used for secondary, Text for tertiary).
- **Dialogs:** MaterialAlertDialogBuilder.
- **Lists:** RecyclerView with DiffUtil.
- **Loaders:** CircularProgressIndicator.

## Spacing & Density
- **Density Level**: High / Compact.
- **Margins/Padding**: Use minimal standard spacing (4dp/8dp) instead of wide (16dp/24dp) where possible.
- **Layouts**: Prefer visible borders/dividers to separate tight content over using whitespace separation.

## Accessibility
- All ImageButtons must have `contentDescription`.
- Touch targets min 48x48dp.
- Text contrast compliant.

---

## Accessibility Testing Checklist

### TalkBack Scenarios for All Screens

#### MainActivity (Resource List)

**TalkBack Flow**:
1. Screen announced: "Resources, 5 items"
2. First resource focused: "Local DCIM folder, button, double tap to open"
3. Swipe right: "SMB Media Share, button, double tap to open"
4. Action menu: "More actions for Local DCIM, button, double tap to open menu"
5. FAB: "Add new resource, button"

**Implementation**:
```xml
<com.google.android.material.card.MaterialCardView
    android:contentDescription="@string/resource_item_description"
    android:focusable="true"
    android:clickable="true">
    
    <TextView
        android:id="@+id/textViewResourceName"
        android:importantForAccessibility="no" /> <!-- Parent has description -->
    
    <ImageButton
        android:id="@+id/buttonMoreActions"
        android:contentDescription="@string/more_actions_for_resource" />
</com.google.android.material.card.MaterialCardView>
```

#### BrowseActivity (File List)

**TalkBack Flow**:
1. Screen announced: "Browsing Local DCIM, 127 files"
2. First file: "IMG underscore 20250106.jpg, image, 2.4 megabytes, January 6 2026"
3. Long press: "Selected IMG underscore 20250106.jpg, 1 item selected"
4. Bottom bar: "Copy to destination, button, disabled"
5. After selecting destination: "Copy to destination, button"

**Implementation**:
```kotlin
binding.recyclerView.accessibilityDelegate = object : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: AccessibilityNodeInfo
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        
        val itemCount = adapter.itemCount
        info.contentDescription = getString(
            R.string.browsing_accessibility_description,
            currentResource.name,
            itemCount
        )
    }
}

// File item accessibility
holder.itemView.contentDescription = getString(
    R.string.file_item_description,
    file.name,
    file.type.name.lowercase(),
    file.size.formatFileSize(),
    file.date.formatDate()
)
```

#### PlayerActivity (Media Viewer)

**TalkBack Flow**:
1. Video mode: "Playing video IMG underscore 20250106.mp4, 0 minutes 5 seconds out of 2 minutes 30 seconds"
2. Play/Pause: "Pause video, button"
3. Previous/Next: "Previous file, button" / "Next file, button"
4. Edit button: "Edit image, button"
5. Progress bar: Custom accessibility with time announcement

**Implementation**:
```kotlin
// Video player controls
binding.buttonPlayPause.contentDescription = if (isPlaying) {
    getString(R.string.pause_video)
} else {
    getString(R.string.play_video)
}

// ExoPlayer accessibility
playerView.controllerShowTimeoutMs = 5000
playerView.useController = true
playerView.setContentDescription(
    getString(
        R.string.video_player_description,
        currentFile.name,
        formatDuration(player.currentPosition),
        formatDuration(player.duration)
    )
)
```

#### OperationsActivity (File Operations Progress)

**TalkBack Flow**:
1. Screen: "Copy operation in progress"
2. Progress: "Copying file 5 of 10, IMG underscore 20250106.jpg, 45 percent complete"
3. Pause button: "Pause operation, button"
4. Cancel: "Cancel operation, button, warning, this action cannot be undone"

**Implementation**:
```kotlin
progressBar.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(host, event)
        
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
            event.text.add(
                getString(
                    R.string.operation_progress_description,
                    currentFileIndex,
                    totalFiles,
                    currentFileName,
                    progressBar.progress
                )
            )
        }
    }
})
```

### Keyboard Navigation Map

#### MainActivity

| Key | Action |
|-----|--------|
| **Tab** | Navigate between resources |
| **Enter/Space** | Open selected resource |
| **Menu** | Open context menu for selected resource |
| **Ctrl+N** | Add new resource (if supported) |
| **Escape** | Close dialogs |

**Implementation**:
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_TAB -> {
            // Focus next resource
            binding.recyclerView.focusSearch(View.FOCUS_DOWN)?.requestFocus()
            true
        }
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
            // Open focused resource
            val position = (binding.recyclerView.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition()
            viewModel.openResource(position)
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

#### BrowseActivity

| Key | Action |
|-----|--------|
| **Tab** | Navigate between files |
| **Enter** | Open file in PlayerActivity |
| **Ctrl+A** | Select all files |
| **Ctrl+C** | Copy selected files |
| **Ctrl+V** | Paste files |
| **Delete** | Delete selected files |
| **Backspace** | Navigate to parent folder |
| **F2** | Rename selected file |
| **F5** | Refresh file list |

**Implementation**:
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return when {
        event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_A -> {
            viewModel.selectAll()
            true
        }
        event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_C -> {
            viewModel.copySelectedFiles()
            true
        }
        keyCode == KeyEvent.KEYCODE_DEL -> {
            viewModel.deleteSelectedFiles()
            true
        }
        keyCode == KeyEvent.KEYCODE_F2 -> {
            viewModel.renameFile()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

#### PlayerActivity

| Key | Action |
|-----|--------|
| **Space** | Play/Pause video |
| **Left Arrow** | Seek backward 10s |
| **Right Arrow** | Seek forward 10s |
| **Up Arrow** | Volume up |
| **Down Arrow** | Volume down |
| **M** | Toggle mute |
| **F** | Toggle fullscreen |
| **Escape** | Exit PlayerActivity |
| **Page Up/Down** | Previous/Next file |

**Implementation**:
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_SPACE -> {
            togglePlayPause()
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            seekBackward()
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            seekForward()
            true
        }
        KeyEvent.KEYCODE_PAGE_UP -> {
            viewModel.navigateToPrevious()
            true
        }
        KeyEvent.KEYCODE_PAGE_DOWN -> {
            viewModel.navigateToNext()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

### Color Contrast Verification Matrix

#### Material Design 3 Theme Contrast Ratios

| Element | Background | Foreground | Contrast Ratio | WCAG AA | WCAG AAA |
|---------|------------|------------|----------------|---------|----------|
| **Primary Text** | Surface (Light) | OnSurface | 15.8:1 | ✅ | ✅ |
| **Primary Text** | Surface (Dark) | OnSurface | 14.2:1 | ✅ | ✅ |
| **Secondary Text** | Surface (Light) | OnSurfaceVariant | 7.5:1 | ✅ | ✅ |
| **Buttons (Primary)** | Primary | OnPrimary | 4.8:1 | ✅ | ❌ |
| **Buttons (Secondary)** | SecondaryContainer | OnSecondaryContainer | 8.2:1 | ✅ | ✅ |
| **Error Messages** | ErrorContainer | OnErrorContainer | 6.1:1 | ✅ | ✅ |
| **Disabled Text** | Surface | OnSurface (38% alpha) | 3.5:1 | ⚠️ | ❌ |

**WCAG Standards**:
- **AA**: 4.5:1 for normal text, 3:1 for large text (18pt+)
- **AAA**: 7:1 for normal text, 4.5:1 for large text

#### Custom Color Validation

```kotlin
@ColorInt
fun validateContrast(@ColorInt foreground: Int, @ColorInt background: Int): Float {
    val foregroundLuminance = ColorUtils.calculateLuminance(foreground)
    val backgroundLuminance = ColorUtils.calculateLuminance(background)
    
    val lighter = max(foregroundLuminance, backgroundLuminance)
    val darker = min(foregroundLuminance, backgroundLuminance)
    
    return (lighter + 0.05f) / (darker + 0.05f)
}

// Usage in resource color picker
fun validateResourceColor(@ColorInt color: Int): Boolean {
    val surfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE)
    val contrast = validateContrast(color, surfaceColor)
    
    if (contrast < 4.5f) {
        Toast.makeText(
            context,
            getString(R.string.color_contrast_warning, String.format("%.1f", contrast)),
            Toast.LENGTH_LONG
        ).show()
        return false
    }
    
    return true
}
```

#### Manual Testing Checklist

```markdown
# Accessibility Manual Test Suite

## TalkBack Testing
- [ ] All screens announce title correctly
- [ ] All buttons have descriptive labels
- [ ] Images have meaningful contentDescription
- [ ] Selected state announced ("selected" / "not selected")
- [ ] Error messages read aloud automatically
- [ ] Progress updates announced during long operations
- [ ] No focus traps (can navigate to all elements)

## Keyboard Navigation Testing
- [ ] Tab key navigates in logical order
- [ ] Enter/Space activates buttons
- [ ] Escape closes dialogs
- [ ] All shortcuts work (Ctrl+A, Ctrl+C, etc.)
- [ ] Focus indicator visible on all focusable elements

## Color Contrast Testing
- [ ] All text readable in Light mode
- [ ] All text readable in Dark mode
- [ ] Error states have sufficient contrast
- [ ] Disabled states distinguishable from enabled
- [ ] Use Accessibility Scanner app to validate

## Touch Target Size Testing
- [ ] All buttons ≥48x48dp (use Layout Inspector)
- [ ] List items have sufficient padding
- [ ] Close icons in dialogs large enough
- [ ] FAB touch target includes shadow area

## Screen Reader Testing (Alternative Text)
- [ ] Images describe content ("Photo of sunset") not type ("Image")
- [ ] Buttons describe action ("Delete file") not UI ("Trash icon")
- [ ] Status messages are role="status" or use LiveRegion
```

#### Automated Accessibility Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun checkContentDescriptions() {
        onView(withId(R.id.fabAddResource))
            .check(matches(withContentDescription(R.string.add_new_resource)))
        
        onView(withId(R.id.imageButtonMoreActions))
            .check(matches(withContentDescription(not(isEmptyOrNullString()))))
    }
    
    @Test
    fun checkTouchTargetSize() {
        onView(withId(R.id.buttonMoreActions))
            .check { view, _ ->
                assertTrue(
                    "Touch target too small: ${view.width}x${view.height}",
                    view.width >= 48.dp && view.height >= 48.dp
                )
            }
    }
    
    @Test
    fun checkColorContrast() {
        onView(withId(R.id.textViewResourceName))
            .check { view, _ ->
                val textView = view as TextView
                val textColor = textView.currentTextColor
                val backgroundColor = (view.parent as View).solidColor
                
                val contrast = validateContrast(textColor, backgroundColor)
                assertTrue(
                    "Insufficient contrast: $contrast:1",
                    contrast >= 4.5f
                )
            }
    }
}
```

---

## Navigation
- Explicit Intents.
- No fragments navigation component graph (custom ViewPager2 or Activity stack).
