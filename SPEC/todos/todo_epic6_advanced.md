# Epic 6: Advanced Capabilities - Detailed TODO
*Derived from: [Tactical Plan: Epic 6](../00_strategy_epic6_advanced.md)*

## 1. Intelligence Suite (ML Kit)

### 1.1 Text Recognition (OCR)
- [ ] Add `com.google.mlkit:text-recognition` dependency
- [ ] Implement `OcrManager`
- [ ] UI: "Scan Text" overlap on Image Viewer
- [ ] Action: Copy text to clipboard

### 1.2 On-Device Translation
- [ ] Add `com.google.mlkit:translate` dependency
- [ ] Implement `TranslationManager`
- [ ] UI: "Translate" button for Text/PDF/Images
- [ ] Logic: Download language models (wifi only check)

### 1.3 Google Lens Style Overlay
- [ ] Create `LensOverlayView`
- [ ] Highlight detected text blocks on images
- [ ] Tap to translate/copy

## 2. Global Search

### 2.1 Recursive Search Engine
- [ ] Implement `SearchUseCase`
- [ ] Logic: Crawl DB for Resources + Recursive scan for specified depth
- [ ] UI: Unified Search Bar in MainActivity
- [ ] Filters: By Type, Date, Size

### 2.2 Document Indexing (Optional)
- [ ] Implement basic text indexing for local PDF/Txt files
- [ ] Store index in FTS4 (Room) table

## 3. Desktop Integration

### 3.1 App Widgets
- [ ] Create `ResourceLaunchWidget`
- [ ] Config: User selects a "Details" shortcut (e.g., straight to "Downloads" folder)

### 3.2 App Shortcuts
- [ ] Define static shortcuts in `shortcuts.xml`
- [ ] Implement dynamic shortcuts (pinned folders) using `ShortcutManager`

## 4. Universal Access (Epic 6 Extension)

### 4.1 Search Update
- [ ] Ensure Search covers `MediaType.OTHER` if `allFiles` is enabled
