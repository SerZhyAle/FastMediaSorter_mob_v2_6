# Tactical Plan: Epic 6 - Advanced Capabilities

**Goal**: High-value features leveraging the core engine.
**Deliverable**: OCR, Translation, Search, and Widgets.
**Prerequisite**: Epic 5 Complete.

---

## 1. Intelligence Suite (ML Kit)

### 1.1 Text Recognition (OCR)
- **Action**: Implement OCR feature.
- **Lib**: `com.google.mlkit:text-recognition`.
- **UI**:
  - "Scan Text" button in Image Viewer.
  - Overlay detected text blocks on image.
  - "Copy All" action.

### 1.2 On-Device Translation
- **Action**: Implement Translation.
- **Lib**: `com.google.mlkit:translate`.
- **Logic**:
  - Download language models (manage disk space).
  - Translate text from OCR or Text Viewer.

### 1.3 Lens-Style Overlay
- **Action**: Create interactive overlay.
- **UI**: Selectable text regions on images/PDFs.

---

## 2. Global Search

### 2.1 Indexing Engine
- **Action**: Implement Search Logic.
- **Algorithm**:
  - Local: Recursive DB query options.
  - Network: Server-side search features (if supported by protocol) or recursive crawl (warning: slow).

### 2.2 Search UI
- **Action**: Implement Global Search Screen.
- **Features**:
  - Filter by Type (Image, Video, Doc).
  - Filter by Date/Size.
  - Recent searches history.

---

## 3. Desktop Integration (Widgets)

### 3.1 Resource Launch Widget
- **Action**: Create App Widget.
- **Features**:
  - Configurable: User selects a Resource (Shortcut).
  - Tap: Opens that specific folder in BrowseActivity directly.

### 3.2 App Actions (Shortcuts)
- **Action**: Implement Static/Dynamic Shortcuts.
- **Static**: "Add New Resource".
- **Dynamic**: "Last visited folder".
