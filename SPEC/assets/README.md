# Application Assets Catalog

**Last Updated**: January 6, 2026  
**Purpose**: Reference guide for all graphical resources used in FastMediaSorter v2.

This directory contains copies of key visual assets from the application for development reference.

---

## Directory Structure

```
assets/
├── icons/                  # PNG icons and images
│   ├── destinations.png           # Destination badge overlay
│   ├── ic_provider_dropbox.png    # Dropbox cloud provider icon
│   ├── ic_provider_google_drive.png  # Google Drive provider icon
│   ├── ic_provider_onedrive.png   # OneDrive provider icon
│   ├── resource_types.png         # Resource type selection guide
│   └── touch_zones_scheme.png     # Touch gesture zones diagram
├── launcher/               # App launcher icons (XXXHDPI)
│   ├── ic_launcher.png                 # Main launcher icon (192x192)
│   ├── ic_launcher_adaptive_back.png   # Adaptive icon background
│   └── ic_launcher_adaptive_fore.png   # Adaptive icon foreground
└── README.md              # This file
```

**Note**: Vector drawables (XML) are not copied here. Refer to `app_v2/src/main/res/drawable/*.xml` for source files.

---

## Icon Categories

### 1. Resource Type Icons

**Location**: `app_v2/src/main/res/drawable/ic_resource_*.xml`

| Icon | File | Usage | Color |
|------|------|-------|-------|
| 🗂️ Local | `ic_resource_local.xml` | Local folder resources | Primary |
| 🌐 SMB | `ic_resource_smb.xml` | SMB/CIFS network shares | Blue |
| 🔐 SFTP | `ic_resource_sftp.xml` | SFTP/SSH file transfer | Green |
| 📁 FTP | `ic_resource_ftp.xml` | FTP protocol | Orange |
| ☁️ Cloud | `ic_resource_cloud.xml` | Cloud storage (Drive/OneDrive/Dropbox) | Purple |
| ⭐ Favorites | `ic_resource_favorites.xml` | Favorites aggregation | Yellow |

**Usage Example**:
```xml
<!-- MainActivity resource list item -->
<ImageView
    android:id="@+id/img_resource_icon"
    android:src="@drawable/ic_resource_local"
    android:tint="?attr/colorPrimary" />
```

---

### 2. Cloud Provider Icons

**Location**: `assets/icons/` (PNG) + `app_v2/src/main/res/drawable/`

| Provider | PNG File | Size | Usage |
|----------|----------|------|-------|
| Google Drive | `ic_provider_google_drive.png` | 48x48 | OAuth sign-in button |
| OneDrive | `ic_provider_onedrive.png` | 48x48 | OAuth sign-in button |
| Dropbox | `ic_provider_dropbox.png` | 48x48 | OAuth sign-in button |

**Usage Example**:
```xml
<!-- AddResourceActivity cloud provider selection -->
<ImageView
    android:id="@+id/img_google_drive"
    android:src="@drawable/ic_provider_google_drive"
    android:contentDescription="@string/provider_google_drive" />
```

---

### 3. Media Type Icons

**Location**: `app_v2/src/main/res/drawable/ic_*.xml`

| Type | Icon | File | Description |
|------|------|------|-------------|
| Image | 🖼️ | `ic_image.xml` | Static images (JPG, PNG, WEBP) |
| Video | 🎬 | `ic_video.xml` | Video files (MP4, MKV, AVI) |
| Audio | 🎵 | `ic_audio.xml` / `ic_music_note.xml` | Audio files (MP3, M4A, FLAC) |
| GIF | 🎞️ | `ic_gif.xml` | Animated GIF images |
| Book | 📖 | `ic_book.xml` | EPUB/PDF documents |

**Placeholder Icons**:
- `ic_image_placeholder.xml` - Loading state for images
- `ic_video_placeholder.xml` - Loading state for videos
- `ic_image_error.xml` - Image load error
- `ic_video_error.xml` - Video load error

**Usage Example**:
```kotlin
// BrowseActivity MediaFileAdapter
when (file.type) {
    MediaType.IMAGE -> holder.icon.setImageResource(R.drawable.ic_image)
    MediaType.VIDEO -> holder.icon.setImageResource(R.drawable.ic_video)
    MediaType.AUDIO -> holder.icon.setImageResource(R.drawable.ic_audio)
    MediaType.GIF -> holder.icon.setImageResource(R.drawable.ic_gif)
}
```

---

### 4. Action Icons

**Location**: `app_v2/src/main/res/drawable/ic_*.xml`

**Navigation**:
- `ic_arrow_back.xml` - Back button (24dp)
- `ic_arrow_forward.xml` - Forward navigation
- `ic_arrow_upward.xml` - Scroll to top
- `ic_arrow_downward.xml` - Scroll to bottom
- `ic_chevron_right.xml` - Expand/Next (16dp)

**File Operations**:
- `ic_save.xml` - Save file/changes
- `ic_share.xml` - Share file
- `ic_rename.xml` - Rename file
- `ic_swap_horizontal.xml` - Move/Swap
- `ic_swap_vert.xml` - Sort order toggle

**Selection**:
- `ic_select_all.xml` - Select all items
- `ic_deselect_all.xml` - Deselect all
- `ic_check.xml` - Confirm/Check (24dp)
- `ic_check_circle.xml` - Selected state (24dp)
- `ic_cancel.xml` - Cancel/Close

**Media Controls**:
- `ic_play.xml` - Play media
- `ic_repeat.xml` - Repeat all
- `ic_repeat_one.xml` - Repeat one
- `ic_speed.xml` - Playback speed
- `ic_fullscreen.xml` - Enter fullscreen
- `ic_fullscreen_exit.xml` - Exit fullscreen

**View Controls**:
- `ic_view_list.xml` - List display mode
- `ic_view_grid.xml` - Grid display mode
- `ic_zoom_in.xml` - Zoom in
- `ic_zoom_out.xml` - Zoom out

**Other**:
- `ic_refresh.xml` - Refresh/Reload
- `ic_clear.xml` - Clear input
- `ic_more_vert.xml` - Overflow menu (3 dots)
- `ic_settings.xml` - Settings gear
- `ic_help_outline_24.xml` - Help/Info
- `ic_add_24.xml` - Add new item (FAB)

---

### 5. Feature Icons

**Location**: `app_v2/src/main/res/drawable/ic_*.xml`

| Feature | Icon | File | Description |
|---------|------|------|-------------|
| Favorites | ⭐ | `ic_star_outline.xml` / `ic_star_filled.xml` | Toggle favorite |
| OCR | 🔍📄 | `ic_ocr.xml` | Text recognition |
| Translate | 🌐 | `ic_translate.xml` | Text translation |
| Google Lens | 🔍🖼️ | `ic_google_lens.xml` | Image search |
| Microphone | 🎤 | `ic_microphone.xml` | Voice input |
| Folder | 📁 | `ic_folder.xml` / `ic_folder_24.xml` | Directory icon |
| Folder Open | 📂 | `ic_folder_open_24.xml` | Expanded directory |

---

### 6. Launcher Icons

**Location**: `assets/launcher/` + `app_v2/src/main/res/mipmap-*/`

**Files**:
- `ic_launcher.png` - Legacy launcher icon (48x48 to 192x192 across densities)
- `ic_launcher_adaptive_back.png` - Adaptive icon background layer
- `ic_launcher_adaptive_fore.png` - Adaptive icon foreground layer

**Adaptive Icon Configuration** (`mipmap-anydpi-v26/ic_launcher.xml`):
```xml
<adaptive-icon>
    <background android:drawable="@mipmap/ic_launcher_adaptive_back"/>
    <foreground android:drawable="@mipmap/ic_launcher_adaptive_fore"/>
</adaptive-icon>
```

**Design Specs**:
- **Safe zone**: 66dp diameter circle (centered)
- **Full icon**: 108dp x 108dp canvas
- **Background**: Solid color or simple gradient
- **Foreground**: Recognizable silhouette/logo

---

### 7. Special Assets

**Location**: `assets/icons/` (PNG)

#### destinations.png
**Purpose**: Visual badge overlay for destination resources  
**Size**: Varies  
**Usage**: Displayed on resource cards marked as destinations (up to 10)

#### resource_types.png
**Purpose**: Reference image showing all 6 resource types  
**Usage**: Documentation, onboarding tutorials

#### touch_zones_scheme.png
**Purpose**: Touch gesture zones diagram for PlayerActivity  
**Usage**: Help screen, gesture tutorial  
**Zones**:
- Top-left: Previous file
- Top-right: Next file
- Bottom-left: Brightness control
- Bottom-right: Volume control
- Center: Toggle toolbar

**Related XML**:
- `touch_zones_numbered.xml` - Debug overlay with zone numbers
- `touch_zones_with_labels.xml` - Debug overlay with zone labels
- `touch_zones_video_image.xml` - Gesture hints for video/image

---

## Background Shapes

**Location**: `app_v2/src/main/res/drawable/bg_*.xml`

| Shape | File | Description |
|-------|------|-------------|
| Circle Dark | `bg_circle_dark.xml` | Dark circular background |
| Circle Light | `bg_circle_light.xml` | Light circular background |
| Circle Themed | `bg_circle_themed.xml` | Theme-aware circle |
| Rounded Dark | `bg_rounded_dark.xml` | Dark rounded rectangle |
| Dialog Backgrounds | `bg_copy_dialog.xml`, `bg_move_dialog.xml`, `bg_delete_dialog.xml` | Colored dialog backgrounds |
| Scroll Button | `bg_scroll_button.xml` | Floating scroll button shape |
| Widget Background | `widget_background.xml` | Home screen widget background |

---

## Badges & Indicators

**Location**: `app_v2/src/main/res/drawable/`

| Badge | File | Usage |
|-------|------|-------|
| Badge Background | `badge_background.xml` | Generic badge shape |
| Destination Badge | `badge_destination_background.xml` | Destination resource marker |
| Filter Badge | `badge_filter_background.xml` | Active filter indicator |
| Destination Border | `destination_border.xml` | Border for destination cards |
| Indicator Active | `indicator_active.xml` | ViewPager active dot |
| Indicator Inactive | `indicator_inactive.xml` | ViewPager inactive dot |

---

## Color Usage Guidelines

### Resource Type Colors

| Type | Light Theme | Dark Theme | Hex |
|------|-------------|------------|-----|
| Local | Primary Blue | Primary Blue | `#2196F3` |
| SMB | Blue | Light Blue | `#1976D2` / `#64B5F6` |
| SFTP | Green | Light Green | `#388E3C` / `#81C784` |
| FTP | Orange | Light Orange | `#F57C00` / `#FFB74D` |
| Cloud | Purple | Light Purple | `#7B1FA2` / `#BA68C8` |
| Favorites | Yellow | Light Yellow | `#FBC02D` / `#FFF176` |

### Icon Tinting

**Standard Icons** (24dp):
```xml
<ImageView
    android:src="@drawable/ic_action"
    android:tint="?attr/colorOnSurface" />
```

**Colored Icons** (resource types):
```kotlin
// MainActivity ResourceAdapter
binding.icon.setColorFilter(
    when (resource.type) {
        ResourceType.LOCAL -> getColor(R.color.type_local)
        ResourceType.SMB -> getColor(R.color.type_smb)
        // ...
    }
)
```

---

## Animation Resources

**Location**: `app_v2/src/main/res/anim/`

**Common Animations**:
- Fade in/out
- Slide in/out (left, right, up, down)
- Scale animations
- Rotate animations

**Usage**: Activity transitions, ViewPager page transforms, RecyclerView item animations

---

## Reference Images

### Touch Zones Scheme
![Touch Zones](icons/touch_zones_scheme.png)

**Gesture Zones** (PlayerActivity):
1. **Top-Left Quarter**: Previous file (swipe left equivalent)
2. **Top-Right Quarter**: Next file (swipe right equivalent)
3. **Bottom-Left**: Brightness control (swipe up/down)
4. **Bottom-Right**: Volume control (swipe up/down)
5. **Center Tap**: Toggle bottom toolbar visibility

---

## Development Guidelines

### Adding New Icons

1. **Vector Drawables (XML)**: Preferred for scalability
   ```xml
   <!-- app_v2/src/main/res/drawable/ic_new_icon.xml -->
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="24dp"
       android:height="24dp"
       android:viewportWidth="24"
       android:viewportHeight="24">
       <path
           android:fillColor="@android:color/white"
           android:pathData="M12,2L2,7v10l10,5 10,-5V7L12,2z" />
   </vector>
   ```

2. **PNG Assets**: Use only when vector not feasible (photos, complex gradients)
   - **MDPI**: 24px (baseline)
   - **HDPI**: 36px (1.5x)
   - **XHDPI**: 48px (2x)
   - **XXHDPI**: 72px (3x)
   - **XXXHDPI**: 96px (4x)

3. **Naming Convention**:
   - Prefix: `ic_` (icon), `bg_` (background), `img_` (image)
   - Lowercase with underscores: `ic_resource_local.xml`

### Icon Sizes

| Context | Size | Example |
|---------|------|---------|
| Toolbar Action | 24dp | Back button, overflow menu |
| FAB | 24dp | Add resource icon |
| List Item Icon | 24dp / 40dp | Resource type icons |
| Bottom Navigation | 24dp | Navigation bar icons |
| Dialog Icon | 48dp | Confirmation dialogs |
| Launcher Icon | 48dp - 192dp | App icon (adaptive) |

### Accessibility

**Content Descriptions**:
```xml
<ImageView
    android:src="@drawable/ic_resource_local"
    android:contentDescription="@string/resource_type_local" />
```

**Touch Targets**:
- Minimum: 48dp x 48dp
- Recommended: 56dp x 56dp for primary actions

---

## Source File Locations

**Application Resources**:
- Vector Drawables: `app_v2/src/main/res/drawable/*.xml`
- PNG Assets: `app_v2/src/main/res/drawable/*.png`
- Launcher Icons: `app_v2/src/main/res/mipmap-*hdpi/ic_launcher*.png`
- Animations: `app_v2/src/main/res/anim/*.xml`

**Specification Copy** (this directory):
- Key PNG icons: `spec_v2/assets/icons/*.png`
- Launcher icons: `spec_v2/assets/launcher/*.png`

---

## Related Specification Documents

- [15. UI Guidelines](../15_ui_guidelines.md) - Material Design principles, color palette
- [10. Custom Views](../10_custom_views.md) - Custom drawable implementations
- [02. Main Activities](../02_main_activities.md) - Activity screenshots and UI flows

---

**Maintained By**: FastMediaSorter Development Team  
**Last Review**: January 6, 2026
