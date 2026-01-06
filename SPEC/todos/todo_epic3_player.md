# Epic 3: Media Player & Viewer Engine - Detailed TODO
*Derived from: [Tactical Plan: Epic 3](../00_strategy_epic3_player.md)*

## 1. Media Display Core

### 1.1 PlayerActivity Framework
- [ ] Create `PlayerActivity`
- [ ] Implement `PlayerViewModel`
- [ ] Setup `PlayerView` (ExoPlayer) and `ImageView` container
- [ ] Implement Fullscreen toggle logic (immersive mode)
- [ ] **Universal Access**: Add logic to auto-skip `MediaType.OTHER` files

### 1.2 Image Loading Engine (Glide)
- [ ] Configure Glide `AppGlideModule`
- [ ] Implement Custom ModelLoader for Encrypted files (if encryption used)
- [ ] Implement "Large Image" handling (downsampling vs region decoding)
- [ ] **Validate**: High-res images (20MB+) load without OOM

### 1.3 PhotoView Integration
- [ ] Integrate `PhotoView` library
- [ ] Enable Zoom/Pan gestures
- [ ] Handle Double-tap to zoom

## 2. Video & Audio Engine

### 2.1 ExoPlayer Integration
- [ ] Dependency: Add `androidx.media3` dependencies
- [ ] Create `ExoPlayerManager` wrapper class
- [ ] Implement Lifecycle handling (pause on stop, release on destroy)
- [ ] Configure `DefaultTrackSelector` for quality selection

### 2.2 Video Controls
- [ ] Custom Control View (Seekbar, Play/Pause, Speed)
- [ ] Implement "Swipe for Brightness/Volume" gestures
- [ ] Add Subtitle support

### 2.3 Audio Player Features
- [ ] Implement Background Playback Service (`MediaSessionService`)
- [ ] Add Notification controls (Play/Pause, Next/Prev)
- [ ] Implement Waveform visualization (using `Amplitudo` or similar)
- [ ] Add "Lyrics Viewer" basics

## 3. Document Engine

### 3.1 PDF Viewer
- [ ] Integrate `PdfRenderer` (native Android) or library
- [ ] Implement Page-by-page rendering
- [ ] Add Zoom support for pages

### 3.2 Text Viewer
- [ ] Read file content (handle large files via stream)
- [ ] Detect Encoding (BOM, or heuristics)
- [ ] Display in generic TextView (monospace)

## 4. Editing Capabilities

### 4.1 Image Editor
- [ ] Create `ImageEditFragment` / Dialog
- [ ] Implement Rotate (90 degrees)
- [ ] Implement Crop (using `uCrop` or custom overlay)
- [ ] Save overwrites original or copy

### 4.2 GIF Support
- [ ] Integrate `glide` GIF support (automatic)
- [ ] Implement Speed Control (if library allows) or frame extraction logic
