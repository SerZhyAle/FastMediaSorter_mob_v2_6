# 10. Custom Views

## Player Overlays

### 1. TouchZoneOverlayView

**Package:** `com.sza.fastmediasorter.ui.player`
**Usage:** `PlayerActivity`
**Role:** Visual guide for gesture controls.
**Features:**

- Draws a semitransparent grid (3x3).
- Displays labels for each zone (e.g., "Prev", "Play", "Next").
- Visibility controlled by user settings (Show on start / Always show).

### 2. TranslationOverlayView

**Package:** `com.sza.fastmediasorter.ui.player.views`
**Usage:** `PlayerActivity` (Translation Mode)
**Role:** Renders translated text blocks over the original image.
**Features:**

- Draws backgrounds behind text for legibility.
- Auto-scales text to fit original bounding boxes.
- Interactive: Tape to bring-to-front or copy text.

## Input Widgets

### 3. IpAddressEditText

**Package:** `com.sza.fastmediasorter.ui.addresource.widgets`
**Usage:** `AddResourceActivity`
**Role:** specialized input for IPv4 addresses.
**Features:**

- Input validation (0-255).
- Auto-advances between octets.

### 4. NetworkPathEditText

**Package:** `com.sza.fastmediasorter.ui.addresource.widgets`
**Usage:** `AddResourceActivity`
**Role:** Input for SMB/SFTP paths.
**Features:**

- Prefix selection (smb://, sftp://).
- Path validation.
