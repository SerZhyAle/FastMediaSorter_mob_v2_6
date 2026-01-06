# 41. Layout Specifications (High Density)

**Goal**: Define strict layout rules to maximize information density and minimize whitespace.
**Principal Rule**: "Information over Decoration".

---

## 1. Global View Attributes

### 1.1 Spacing & Margins
- **Standard Padding**: `4dp` (Tight) or `8dp` (Comfortable). **Avoid** `16dp` unless necessary for touch targets.
- **Card Margins**: `0dp` (Full width) or `4dp` (Card style).
- **Element Separation**: Use `DividerItemDecoration` (1dp line) instead of whitespace.

### 1.2 Touch Targets
- **Visual Size**: Can be small (e.g., 24dp icon).
- **Clickable Area**: MUST be at least **48x48dp** (via padding or `TouchDelegate`).
- **Implementation**: Use `android:padding="12dp"` on a `24dp` ImageButton to achieve 48dp target.

### 1.3 List Items (RecyclerView)
- **Height**: Wrap content, min-height `48dp` (standard) or `56dp` (two-line).
- **Density**:
  - **Single Line**: Icon (24dp) + Text (14sp) + Action (24dp). Height ~48dp.
  - **Single Line Compact**: Height ~40dp (Desktop-like density for file lists).
- **Text Sizes**:
  - Primary: `14sp` or `16sp`.
  - Secondary: `12sp`.
  - Tertiary/Metadata: `10sp` (Caps).

---

## 2. Main Activity (Resource List)

### 2.1 Resource Card (`item_resource.xml`)
- **Root**: `MaterialCardView`
- **Layout**: `ConstraintLayout`
- **Height**: Fixed `64dp` (High Density Two-Line).
- **Structure**:
  - **Icon**: Left, 32dp size, 8dp start margin.
  - **Title**: Top-Left aligned to Icon, `16sp` Bold.
  - **Path/Subtitle**: Below Title, `12sp` Grey, truncated middle.
  - **Action Button**: Right aligned, `ImageButton`, 8dp end margin.
- **Visuals**:
  - Corner Radius: `8dp`.
  - Elevation: `2dp` (Low profile).

---

## 3. Browse Activity (File Grid)

### 3.1 Grid Item (`item_media_file_grid.xml`)
- **Aspect Ratio**: 1:1 (Square).
- **Spacing**: `2dp` between items.
- **Overlay**: Gradient scrim at bottom for text visibility.
- **Text**:
  - Filename: Single line, `11sp`, white, truncate end.
  - Metadata: Hidden in grid, shown on long-press or details view.

### 3.2 List Item (`item_media_file_list.xml`)
- **Height**: `48dp` (Compact).
- **Structure**:
  - **Thumbnail**: Left, 40x40dp, rounded corners `4dp`.
  - **Name**: Top, `14sp`, truncate middle.
  - **Details**: Bottom, `11sp` (e.g., "DOC, 2.4MB, 12.05.2025").
  - **Selection Checkbox**: Right side, visible only in Selection Mode.

---

## 4. Player Activity

### 4.1 HUD (Head-Up Display)
- **Command Panel**:
  - **Position**: Top edge.
  - **Background**: Semi-transparent black (`#80000000`).
  - **Height**: Wrap content (approx `50dp`).
  - **Buttons**: Horizontal `LinearLayout`, button spacing `0dp`, padding `4dp`.
- **Bottom Controls**:
  - **Seekbar**: `-20dp` margin bottom to hug edge.
  - **Metadata**: Floating text over image bottom-left.

---

## 5. Dialogs & Menus

### 5.1 Bottom Sheets
- **Behavior**: Peek height 50%.
- **List Items**: Dense (`48dp` height).
- **Header**: Compact (`56dp` height).

### 5.2 Context Menus
- **Width**: `200dp` min.
- **Item Height**: `40dp`.
- **Icon**: `20dp` size, `12dp` start margin.
- **Text**: `14sp`, `16dp` start margin from icon.
