# Vector Drawable Quick Reference

**Source**: `app_v2/src/main/res/drawable/*.xml`  
**Total**: 80+ vector icons

This is a quick reference for vector drawable icons. Full catalog with usage examples in [README.md](README.md).

---

## Resource Type Icons (6)

```
ic_resource_local.xml       - 🗂️ Local folders
ic_resource_smb.xml         - 🌐 SMB network shares
ic_resource_sftp.xml        - 🔐 SFTP/SSH
ic_resource_ftp.xml         - 📁 FTP protocol
ic_resource_cloud.xml       - ☁️ Cloud storage (Drive/OneDrive/Dropbox)
ic_resource_favorites.xml   - ⭐ Favorites aggregation
```

---

## Media Type Icons (5)

```
ic_image.xml                - 🖼️ Static images (JPG/PNG/WEBP)
ic_video.xml                - 🎬 Video files (MP4/MKV/AVI)
ic_audio.xml                - 🎵 Audio files (MP3/M4A/FLAC)
ic_music_note.xml           - 🎵 Alternative audio icon
ic_gif.xml                  - 🎞️ Animated GIF
ic_book.xml                 - 📖 EPUB/PDF documents
```

**Placeholders**:
```
ic_image_placeholder.xml    - Image loading state
ic_video_placeholder.xml    - Video loading state
ic_image_error.xml          - Image load error
ic_video_error.xml          - Video load error
error_placeholder.xml       - Generic error placeholder
```

---

## Navigation Icons (5)

```
ic_arrow_back.xml           - ← Back button
ic_arrow_forward.xml        - → Forward navigation
ic_arrow_upward.xml         - ↑ Scroll to top
ic_arrow_downward.xml       - ↓ Scroll to bottom
ic_chevron_right.xml        - › Expand/Next (smaller)
ic_back.xml                 - Alternative back icon
```

---

## File Operation Icons (5)

```
ic_save.xml                 - 💾 Save file/changes
ic_share.xml                - 📤 Share file
ic_rename.xml               - ✏️ Rename file
ic_swap_horizontal.xml      - ↔️ Move/Swap
ic_swap_vert.xml            - ⇅ Sort order toggle
```

---

## Selection Icons (5)

```
ic_select_all.xml           - ☑️ Select all items
ic_deselect_all.xml         - ☐ Deselect all
ic_check.xml                - ✓ Confirm/Check
ic_check_circle.xml         - ✓⭕ Selected state (filled circle)
ic_cancel.xml               - ✕ Cancel/Close
```

---

## Media Control Icons (6)

```
ic_play.xml                 - ▶️ Play media
ic_repeat.xml               - 🔁 Repeat all
ic_repeat_one.xml           - 🔂 Repeat one
ic_speed.xml                - ⏩ Playback speed
ic_fullscreen.xml           - ⛶ Enter fullscreen
ic_fullscreen_exit.xml      - ⛶ Exit fullscreen
```

---

## View Control Icons (6)

```
ic_view_list.xml            - ☰ List display mode
ic_view_grid.xml            - ⊞ Grid display mode
ic_zoom_in.xml              - 🔍+ Zoom in
ic_zoom_out.xml             - 🔍- Zoom out
ic_refresh.xml              - ↻ Refresh/Reload
ic_clear.xml                - ✕ Clear input
```

---

## General Action Icons (6)

```
ic_add_24.xml               - ➕ Add new item (FAB)
ic_more_vert.xml            - ⋮ Overflow menu (3 vertical dots)
ic_settings.xml             - ⚙️ Settings gear
ic_help_outline_24.xml      - ❓ Help/Info
ic_folder.xml               - 📁 Folder/Directory
ic_folder_24.xml            - 📁 Folder (24dp variant)
ic_folder_open_24.xml       - 📂 Expanded folder
```

---

## Feature Icons (6)

```
ic_star_outline.xml         - ☆ Favorite (not selected)
ic_star_filled.xml          - ★ Favorite (selected)
ic_favorite.xml             - ❤️ Favorite heart
ic_ocr.xml                  - 🔍📄 Text recognition
ic_translate.xml            - 🌐 Text translation
ic_google_lens.xml          - 🔍🖼️ Google Lens image search
ic_microphone.xml           - 🎤 Voice input
```

---

## Background Shapes (10)

```
bg_circle_dark.xml          - Dark circular background
bg_circle_light.xml         - Light circular background
bg_circle_themed.xml        - Theme-aware circle
bg_rounded_dark.xml         - Dark rounded rectangle
bg_copy_dialog.xml          - Blue copy dialog background
bg_move_dialog.xml          - Orange move dialog background
bg_delete_dialog.xml        - Red delete dialog background
bg_progress_dialog.xml      - Progress dialog background
bg_rename_dialog.xml        - Rename dialog background
bg_scroll_button.xml        - Floating scroll button shape
```

---

## Badges & Indicators (7)

```
badge_background.xml        - Generic badge shape
badge_destination_background.xml - Destination resource marker
badge_filter_background.xml - Active filter indicator
destination_border.xml      - Border for destination resource cards
indicator_active.xml        - ViewPager active dot (filled)
indicator_inactive.xml      - ViewPager inactive dot (outline)
spinner_background.xml      - Spinner dropdown background
```

---

## Widget Backgrounds (2)

```
widget_background.xml       - Home screen widget background
widget_item_background.xml  - Widget list item background
```

---

## Touch Zone Overlays (4)

Debug/tutorial overlays for PlayerActivity gestures:

```
touch_zones_numbered.xml          - Zone numbers (1-5)
touch_zones_numbered_simple.xml   - Simplified numbering
touch_zones_with_labels.xml       - Zone labels (text)
touch_zones_video_image.xml       - Video/image gesture hints
```

**Note**: These are debug tools, not visible in production.

---

## Color Coding by Category

| Category | Primary Color | Usage |
|----------|---------------|-------|
| Resource Types | Various | Type-specific colors (blue/green/orange/purple) |
| Media Types | OnSurface | Neutral, tinted by theme |
| Actions | OnSurface | Standard material icons |
| Favorites | Yellow (#FBC02D) | Star icons |
| Dialogs | Context-specific | Copy=Blue, Move=Orange, Delete=Red |

---

## Standard Icon Sizes

- **Toolbar/Action**: 24dp
- **List Item**: 24dp - 40dp
- **FAB**: 24dp (icon), 56dp (button)
- **Dialog**: 48dp
- **Touch Target**: Minimum 48x48dp

---

## XML Structure Example

```xml
<!-- app_v2/src/main/res/drawable/ic_resource_local.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnSurface">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M10,4H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8L10,4z" />
</vector>
```

**Key Attributes**:
- `android:width/height`: Display size (24dp standard)
- `android:viewportWidth/Height`: Canvas size (24 for Material icons)
- `android:tint`: Theme-aware color (`?attr/colorOnSurface`)
- `android:pathData`: SVG path commands

---

## Usage in Layouts

### Static Tint
```xml
<ImageView
    android:src="@drawable/ic_folder"
    android:tint="?attr/colorPrimary" />
```

### Programmatic Tint
```kotlin
binding.icon.setImageResource(R.drawable.ic_resource_local)
binding.icon.setColorFilter(getColor(R.color.type_local), PorterDuff.Mode.SRC_IN)
```

### With Content Description
```xml
<ImageView
    android:src="@drawable/ic_share"
    android:contentDescription="@string/action_share" />
```

---

**Quick Access**: All vector drawables at `app_v2/src/main/res/drawable/*.xml`  
**Full Documentation**: [README.md](README.md)
