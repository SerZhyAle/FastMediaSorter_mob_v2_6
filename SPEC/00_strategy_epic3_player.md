# Tactical Plan: Epic 3 - Media Player & Viewer Engine

**Goal**: Universal media consumption capability.
**Deliverable**: Integrated viewer for Images, Video, Audio, and Documents.
**Prerequisite**: Epic 2 Complete.

---

## 1. Media Display Core

### 1.1 PlayerActivity Framework
- **Action**: Create `PlayerActivity`.
- **Logic**:
  - Receives `currentFile` and `playlist` (list of files) via Intent/Singleton.
  - Implements System UI visibility toggling (Immersive Mode).
  - Manages common UI overlays (Command Panel).

### 1.2 Image Viewer
- **Action**: Integrate PhotoView.
- **Features**:
  - Double-tap zoom.
  - Pinch-to-zoom.
  - Swipe left/right to navigate playlist.
- **Glide config**: Custom configuration for high-res image loading.

---

## 2. Video & Audio Engine

### 2.1 ExoPlayer Integration
- **Action**: Add `media3-exoplayer` dependency.
- **Implementation**:
  - Initialize ExoPlayer in `PlayerActivity`.
  - Handle Lifecycle (pause on background, release on destroy).

### 2.2 Video Controls
- **Action**: Implement custom PlayerControlView.
- **Features**:
  - Seek bar.
  - Play/Pause.
  - Speed control (0.5x - 2.0x).
  - Gestures: Brightness (Left), Volume (Right).

### 2.3 Audio Player Manager
- **Action**: Create `AudioPlayerManager` (or `AudioService` if background play needed).
- **UI**:
  - Show Album Art (from metadata).
  - Show Waveform (visualizer).
  - Notification controls (Play/Pause/Next).

---

## 3. Document Engine

### 3.1 PDF Viewer
- **Action**: Integrate PdfRenderer (native Android) or library.
- **Features**:
  - Render pages to Bitmap.
  - Horizontal swipe for page navigation.
  - Pinch zoom on individual pages.

### 3.2 Text Viewer
- **Action**: Implement simple Text Editor/Viewer.
- **Logic**:
  - Read file as String (handle Charset detection).
  - Display in `EditText` (editable) or `TextView`.
  - Support syntax highlighting for JSON/XML (optional, nice to have).

### 3.3 EPUB Integration
- **Action**: Integrate EPUB Reader.
- **Implementation**:
  - Usage of WebView or specific parsing library.
  - Chapter navigation.

---

## 4. Editing Capabilities

### 4.1 Image Editor
- **Action**: Implement `ImageEditActivity` or Dialog.
- **Features**:
  - Rotate (90/180/270).
  - Crop (Fixed aspect ratios + Free).
  - Basic Filters (BW, Sepia).
  - Save: Overwrite or Save Copy.

### 4.2 GIF Editor
- **Action**: Implement GIF manipulation.
- **Features**:
  - Extract frames (GifDecoder).
  - Playback speed adjustment.
  - Save result.
