# Tactical Plan: Epic 7 - Polish & User Experience

**Goal**: Refine the application for production quality.
**Deliverable**: Settings, Localization, Accessibility.
**Prerequisite**: Epic 6 Complete.

---

## 1. Settings Ecosystem

### 1.1 Settings Architecture
- **Action**: Implement Settings Screens.
- **Structure**:
  - `GeneralSettings`: Theme, Language.
  - `MediaSettings`: Default scan depth, Thumbnail quality.
  - `NetworkSettings`: Timeout values, connection pool size.

### 1.2 Theme Engine
- **Action**: Implement Theme Switching.
- **Options**: System Default, Force Light, Force Dark, AMOLED Black.
- **Verify**: Check contrast in all modes.

---

## 2. Localization

### 2.1 Languages
- **Action**: Hard-enable 3 languages.
- **Locales**:
  - Key: `res/values/strings.xml` (Default/English).
  - `res/values-ru/strings.xml` (Russian).
  - `res/values-uk/strings.xml` (Ukrainian).

### 2.2 RTL Support
- **Action**: Verify Right-To-Left layout.
- **Check**: Padded start/end instead of left/right. Arrow icons mapping (auto-mirror).

---

## 3. Accessibility

### 3.1 Content Descriptions
- **Action**: Audit all ImageButtons/ImageViews.
- **Fix**: Ensure `android:contentDescription` is meaningful and localized.

### 3.2 Touch Targets
- **Action**: Verify sizing.
- **Fix**: All interactive elements must be min 48x48dp (clickable area).

### 3.3 TalkBack Navigation
- **Action**: Manual Verification.
- **Steps**: Enable TalkBack, navigate critical flows (Add Resource -> Browse -> Play).
