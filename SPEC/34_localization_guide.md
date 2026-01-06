# 34. Localization & Internationalization

**Last Updated**: January 6, 2026  
**Purpose**: Localization strategy, string resources organization, RTL support, and multi-language maintenance for FastMediaSorter v2.

This document defines translation workflows, plurals handling, date/number formatting, and best practices for international audiences.

---

## Overview

FastMediaSorter supports **3 languages**:
- **English (en)** - Default
- **Russian (ru)** - Full translation
- **Ukrainian (uk)** - Full translation

### Localization Scope

| Component | Localization Status | Notes |
|-----------|---------------------|-------|
| **App UI** | ✅ Fully localized | All Activities, dialogs, toasts |
| **User Documentation** | ✅ Fully localized | README, QUICK_START, FAQ, HOW_TO, TROUBLESHOOTING |
| **Store Listings** | ✅ Fully localized | Google Play descriptions |
| **In-App Help** | ✅ Fully localized | Tooltips, rationales |
| **Error Messages** | ✅ Fully localized | User-facing errors |
| **Technical Logs** | ❌ English only | Timber logs, stack traces |

---

## Table of Contents

1. [String Resources Organization](#1-string-resources-organization)
2. [Plurals & Quantities](#2-plurals--quantities)
3. [Date & Number Formatting](#3-date--number-formatting)
4. [RTL Support](#4-rtl-support)
5. [Translation Workflow](#5-translation-workflow)
6. [Testing Localization](#6-testing-localization)
7. [String Resource Naming](#7-string-resource-naming)

---

## 1. String Resources Organization

### Directory Structure

```
app_v2/src/main/res/
├── values/               # English (default)
│   ├── strings.xml
│   ├── strings_errors.xml
│   ├── strings_settings.xml
│   └── plurals.xml
├── values-ru/            # Russian
│   ├── strings.xml
│   ├── strings_errors.xml
│   ├── strings_settings.xml
│   └── plurals.xml
└── values-uk/            # Ukrainian
    ├── strings.xml
    ├── strings_errors.xml
    ├── strings_settings.xml
    └── plurals.xml
```

---

### strings.xml (General UI)

**File**: `values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- App Name -->
    <string name="app_name">FastMediaSorter</string>
    
    <!-- MainActivity -->
    <string name="main_title">Resources</string>
    <string name="main_fab_add">Add Resource</string>
    <string name="main_empty_state">No resources yet.\nTap + to add one.</string>
    <string name="main_menu_settings">Settings</string>
    <string name="main_menu_about">About</string>
    
    <!-- BrowseActivity -->
    <string name="browse_title">Files</string>
    <string name="browse_sort_name">Sort by Name</string>
    <string name="browse_sort_date">Sort by Date</string>
    <string name="browse_sort_size">Sort by Size</string>
    <string name="browse_filter_all">All Files</string>
    <string name="browse_filter_images">Images Only</string>
    <string name="browse_filter_videos">Videos Only</string>
    <string name="browse_empty_state">No files found</string>
    
    <!-- PlayerActivity -->
    <string name="player_action_copy">Copy</string>
    <string name="player_action_move">Move</string>
    <string name="player_action_delete">Delete</string>
    <string name="player_action_share">Share</string>
    <string name="player_action_edit">Edit</string>
    <string name="player_action_rotate_left">Rotate Left</string>
    <string name="player_action_rotate_right">Rotate Right</string>
    <string name="player_action_info">Info</string>
    
    <!-- EditResourceActivity -->
    <string name="edit_resource_title_add">Add Resource</string>
    <string name="edit_resource_title_edit">Edit Resource</string>
    <string name="edit_resource_name">Name</string>
    <string name="edit_resource_path">Path</string>
    <string name="edit_resource_type">Type</string>
    <string name="edit_resource_username">Username</string>
    <string name="edit_resource_password">Password</string>
    <string name="edit_resource_domain">Domain (optional)</string>
    <string name="edit_resource_port">Port</string>
    <string name="edit_resource_test_connection">Test Connection</string>
    <string name="edit_resource_mark_destination">Mark as Destination</string>
    <string name="edit_resource_save">Save</string>
    
    <!-- Dialogs -->
    <string name="dialog_delete_title">Delete File?</string>
    <string name="dialog_delete_message">This will move the file to trash. You can undo this action.</string>
    <string name="dialog_delete_confirm">Delete</string>
    <string name="dialog_cancel">Cancel</string>
    
    <!-- Toasts -->
    <string name="toast_file_copied">File copied successfully</string>
    <string name="toast_file_moved">File moved successfully</string>
    <string name="toast_file_deleted">File deleted</string>
    <string name="toast_action_undo">Undo</string>
    
    <!-- Common -->
    <string name="action_ok">OK</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_retry">Retry</string>
    <string name="action_settings">Settings</string>
    <string name="loading">Loading…</string>
</resources>
```

---

### strings_errors.xml (Error Messages)

**File**: `values/strings_errors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Storage Errors -->
    <string name="error_permission_denied">Permission denied. Grant storage access in Settings.</string>
    <string name="error_file_not_found">File not found. It may have been deleted.</string>
    <string name="error_disk_full">Not enough storage space. Free up %1$d MB.</string>
    <string name="error_invalid_file">File is corrupted or unsupported format.</string>
    
    <!-- Network Errors -->
    <string name="error_connection_timeout">Connection timed out. Check network and retry.</string>
    <string name="error_authentication_failed">Login failed. Check username/password.</string>
    <string name="error_server_unreachable">Server unreachable. Check network connection.</string>
    <string name="error_connection_lost">Connection lost. Operation aborted.</string>
    
    <!-- Cloud Errors -->
    <string name="error_token_expired">Session expired. Please sign in again.</string>
    <string name="error_quota_exceeded">Cloud storage quota exceeded. Free up space or upgrade plan.</string>
    <string name="error_rate_limit">Too many requests. Try again in %1$d seconds.</string>
    <string name="error_cloud_file_not_found">File not found in cloud storage.</string>
    
    <!-- Media Errors -->
    <string name="error_unsupported_codec">Video codec not supported on this device.</string>
    <string name="error_out_of_memory">File too large to process. Try a smaller file.</string>
    <string name="error_corrupted_media">Media file is corrupted or damaged.</string>
    
    <!-- Database Errors -->
    <string name="error_migration_failed">App update failed. Try reinstalling.</string>
    <string name="error_duplicate_resource">Resource already exists.</string>
</resources>
```

---

### strings_settings.xml (Settings Screen)

**File**: `values/strings_settings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Settings Categories -->
    <string name="settings_category_display">Display</string>
    <string name="settings_category_cache">Cache</string>
    <string name="settings_category_network">Network</string>
    <string name="settings_category_about">About</string>
    
    <!-- Display Settings -->
    <string name="settings_theme">Theme</string>
    <string name="settings_theme_light">Light</string>
    <string name="settings_theme_dark">Dark</string>
    <string name="settings_theme_auto">System Default</string>
    
    <string name="settings_grid_columns">Grid Columns</string>
    <string name="settings_thumbnail_quality">Thumbnail Quality</string>
    
    <!-- Cache Settings -->
    <string name="settings_cache_size">Thumbnail Cache Size</string>
    <string name="settings_cache_size_summary">%1$s used of %2$s</string>
    <string name="settings_clear_cache">Clear Cache</string>
    <string name="settings_clear_cache_confirm">Clear %1$s of cached thumbnails?</string>
    
    <!-- Network Settings -->
    <string name="settings_auto_sync">Auto-Sync</string>
    <string name="settings_auto_sync_summary">Automatically sync network resources on app start</string>
    <string name="settings_connection_timeout">Connection Timeout</string>
    <string name="settings_connection_timeout_summary">%1$d seconds</string>
    
    <!-- About -->
    <string name="settings_version">Version</string>
    <string name="settings_privacy_policy">Privacy Policy</string>
    <string name="settings_terms_of_service">Terms of Service</string>
    <string name="settings_licenses">Open Source Licenses</string>
</resources>
```

---

### plurals.xml (Quantities)

**File**: `values/plurals.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Files Count -->
    <plurals name="files_count">
        <item quantity="one">%d file</item>
        <item quantity="other">%d files</item>
    </plurals>
    
    <!-- Selected Count -->
    <plurals name="selected_count">
        <item quantity="one">%d selected</item>
        <item quantity="other">%d selected</item>
    </plurals>
    
    <!-- Days Ago -->
    <plurals name="days_ago">
        <item quantity="one">%d day ago</item>
        <item quantity="other">%d days ago</item>
    </plurals>
    
    <!-- Minutes Remaining -->
    <plurals name="minutes_remaining">
        <item quantity="one">%d minute remaining</item>
        <item quantity="other">%d minutes remaining</item>
    </plurals>
</resources>
```

---

### Russian Translation Example

**File**: `values-ru/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FastMediaSorter</string>
    
    <!-- MainActivity -->
    <string name="main_title">Ресурсы</string>
    <string name="main_fab_add">Добавить ресурс</string>
    <string name="main_empty_state">Нет ресурсов.\nНажмите +, чтобы добавить.</string>
    <string name="main_menu_settings">Настройки</string>
    <string name="main_menu_about">О программе</string>
    
    <!-- BrowseActivity -->
    <string name="browse_title">Файлы</string>
    <string name="browse_sort_name">Сортировка по имени</string>
    <string name="browse_sort_date">Сортировка по дате</string>
    <string name="browse_sort_size">Сортировка по размеру</string>
    <string name="browse_filter_all">Все файлы</string>
    <string name="browse_filter_images">Только изображения</string>
    <string name="browse_filter_videos">Только видео</string>
    <string name="browse_empty_state">Файлы не найдены</string>
    
    <!-- ... (rest of translations) -->
</resources>
```

---

### Ukrainian Translation Example

**File**: `values-uk/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FastMediaSorter</string>
    
    <!-- MainActivity -->
    <string name="main_title">Ресурси</string>
    <string name="main_fab_add">Додати ресурс</string>
    <string name="main_empty_state">Немає ресурсів.\nНатисніть +, щоб додати.</string>
    <string name="main_menu_settings">Налаштування</string>
    <string name="main_menu_about">Про програму</string>
    
    <!-- ... (rest of translations) -->
</resources>
```

---

## 2. Plurals & Quantities

### English Plurals (2 forms)

```xml
<plurals name="files_count">
    <item quantity="one">%d file</item>
    <item quantity="other">%d files</item>
</plurals>
```

**Usage**:
```kotlin
val count = 5
val text = resources.getQuantityString(R.plurals.files_count, count, count)
// Result: "5 files"
```

---

### Russian Plurals (3 forms)

```xml
<plurals name="files_count">
    <item quantity="one">%d файл</item>      <!-- 1, 21, 31, ... -->
    <item quantity="few">%d файла</item>     <!-- 2-4, 22-24, ... -->
    <item quantity="many">%d файлов</item>   <!-- 0, 5-20, 25-30, ... -->
    <item quantity="other">%d файлов</item>  <!-- Fallback -->
</plurals>
```

**Examples**:
- 1 файл (one)
- 2 файла (few)
- 5 файлов (many)
- 21 файл (one)

---

### Ukrainian Plurals (4 forms)

```xml
<plurals name="files_count">
    <item quantity="one">%d файл</item>      <!-- 1, 21, 31, ... -->
    <item quantity="few">%d файли</item>     <!-- 2-4, 22-24, ... -->
    <item quantity="many">%d файлів</item>   <!-- 0, 5-20, 25-30, ... -->
    <item quantity="other">%d файлів</item>  <!-- Fallback -->
</plurals>
```

---

### Best Practices for Plurals

**✅ DO**:
```kotlin
// Use getQuantityString
val text = resources.getQuantityString(R.plurals.files_count, count, count)
```

**❌ DON'T**:
```kotlin
// Hardcode plural logic
val text = if (count == 1) "$count file" else "$count files" // Breaks for Russian/Ukrainian
```

---

## 3. Date & Number Formatting

### Date Formatting

**Problem**: Hardcoded formats break localization.

**❌ BAD**:
```kotlin
val date = "12/25/2025" // US format, ambiguous
```

**✅ GOOD**:
```kotlin
val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
val date = formatter.format(Date())
// English: "Dec 25, 2025"
// Russian: "25 дек. 2025 г."
// Ukrainian: "25 груд. 2025 р."
```

---

### Relative Time

**Use DateUtils**:
```kotlin
val timeAgo = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS
)
// English: "5 minutes ago"
// Russian: "5 минут назад"
// Ukrainian: "5 хвилин тому"
```

---

### Number Formatting

**File Size**:
```kotlin
fun formatFileSize(bytes: Long, context: Context): String {
    return android.text.format.Formatter.formatFileSize(context, bytes)
}
// English: "1.5 MB"
// Russian: "1,5 МБ" (comma decimal separator)
// Ukrainian: "1,5 МБ"
```

---

### Currency (If Needed)

```kotlin
val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
val price = formatter.format(9.99)
// English (US): "$9.99"
// Russian: "9,99 ₽"
// Ukrainian: "9,99 ₴"
```

---

## 4. RTL Support

### Current Status

**RTL languages NOT supported** (Arabic, Hebrew).

If adding RTL support:

---

### Enable RTL in Manifest

```xml
<application
    android:supportsRtl="true"
    ...>
```

---

### Use Start/End Instead of Left/Right

**❌ BAD**:
```xml
<TextView
    android:layout_marginLeft="16dp"
    android:paddingRight="8dp" />
```

**✅ GOOD**:
```xml
<TextView
    android:layout_marginStart="16dp"
    android:paddingEnd="8dp" />
```

**Effect**: In RTL mode, `start` becomes right, `end` becomes left.

---

### Test RTL Layout

**Developer Options → Force RTL Layout**

Or programmatically:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
}
```

---

## 5. Translation Workflow

### Step 1: Extract New Strings

**Command**:
```bash
./gradlew :app_v2:extractStrings
```

**Output**: List of untranslated strings.

---

### Step 2: Update Translations

**Tools**:
1. **Manual**: Edit `values-ru/strings.xml`, `values-uk/strings.xml`
2. **Crowdin**: Import XML → Translate → Export XML
3. **Google Translate API**: Batch translation (draft only, review required)

---

### Step 3: Validate Translations

**Android Lint**:
```bash
./gradlew :app_v2:lint
```

**Checks**:
- Missing translations (incomplete `values-ru/strings.xml`)
- Unused strings (`<string>` in XML but not used in code)
- String format mismatches (`%1$s` missing in translation)

---

### Step 4: Translation Review Checklist

- [ ] All new strings translated
- [ ] Plurals have correct forms (one/few/many/other)
- [ ] Placeholder order matches (`%1$s %2$d` → `%2$d %1$s` if needed)
- [ ] No truncated text (UI buttons, labels)
- [ ] Context-appropriate tone (formal vs informal)
- [ ] Technical terms consistent (e.g., "SMB" not translated)

---

## 6. Testing Localization

### Manual Testing

**Change device language**:
1. Settings → System → Languages → Add language → Russian
2. Restart app
3. Verify all screens translated

---

### Automated Testing

**Espresso Test**:
```kotlin
@RunWith(Parameterized::class)
class LocalizationTest(
    private val locale: Locale
) {
    
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun locales() = listOf(
            arrayOf(Locale.ENGLISH),
            arrayOf(Locale("ru")),
            arrayOf(Locale("uk"))
        )
    }
    
    @Before
    fun setLocale() {
        LocaleUtils.setLocale(InstrumentationRegistry.getInstrumentation().targetContext, locale)
    }
    
    @Test
    fun mainActivity_allStringsTranslated() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Verify no default English strings visible
        onView(withText("Resources")).check(doesNotExist()) // English
        
        // Verify localized string visible
        when (locale.language) {
            "ru" -> onView(withText("Ресурсы")).check(matches(isDisplayed()))
            "uk" -> onView(withText("Ресурси")).check(matches(isDisplayed()))
        }
    }
}
```

---

### Screenshot Testing (Optional)

**Capture screenshots in all languages**:
```kotlin
@Test
fun captureScreenshots() {
    val locales = listOf("en", "ru", "uk")
    
    locales.forEach { locale ->
        LocaleUtils.setLocale(context, Locale(locale))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        Screenshot.capture("main_screen_$locale")
    }
}
```

---

## 7. String Resource Naming

### Naming Convention

**Pattern**: `<screen>_<component>_<description>`

**Examples**:
- `main_title` - MainActivity title
- `browse_sort_name` - BrowseActivity sort by name option
- `player_action_delete` - PlayerActivity delete action
- `dialog_delete_title` - Delete confirmation dialog title
- `toast_file_copied` - Toast message for file copied
- `error_connection_timeout` - Connection timeout error message
- `settings_cache_size` - Settings cache size preference

---

### Category Prefixes

| Prefix | Category | Example |
|--------|----------|---------|
| `main_` | MainActivity | `main_title` |
| `browse_` | BrowseActivity | `browse_filter_images` |
| `player_` | PlayerActivity | `player_action_share` |
| `edit_resource_` | EditResourceActivity | `edit_resource_username` |
| `settings_` | SettingsActivity | `settings_theme` |
| `dialog_` | Dialogs | `dialog_cancel` |
| `toast_` | Toast messages | `toast_file_deleted` |
| `error_` | Error messages | `error_disk_full` |
| `action_` | Generic actions | `action_ok`, `action_retry` |

---

### Avoid Hardcoded Strings

**❌ BAD**:
```kotlin
button.text = "Click Me"
Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
```

**✅ GOOD**:
```kotlin
button.text = getString(R.string.button_click_me)
Toast.makeText(context, R.string.toast_file_saved, Toast.LENGTH_SHORT).show()
```

---

### Lint Check for Hardcoded Strings

**Enable in `build.gradle.kts`**:
```kotlin
android {
    lint {
        warningsAsErrors = true
        error("HardcodedText") // Fail build on hardcoded strings
    }
}
```

---

## 7. Internationalization Edge Cases

### 7.1. RTL Layout Support (Future: Arabic, Hebrew)

**Current Status**: Not implemented (English/Russian/Ukrainian are LTR).

**Preparation for RTL**:

```xml
<!-- Layout guidelines for RTL compatibility -->
<LinearLayout
    android:layoutDirection="locale" <!-- Respect system direction -->
    android:layout_marginStart="16dp" <!-- Use Start/End instead of Left/Right -->
    android:layout_marginEnd="16dp">
    
    <ImageView
        android:layout_gravity="start" <!-- Not "left" -->
        android:scaleType="fitStart" />
</LinearLayout>
```

#### Testing RTL Without Translation

```kotlin
// In debug menu or developer settings
@SuppressLint("AppBundleLocaleChanges")
fun enablePseudoRtl() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val config = resources.configuration
        config.setLayoutDirection(Locale("ar")) // Force RTL
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}
```

#### RTL-Specific Adjustments

```xml
<!-- res/values-ldrtl/dimens.xml (LTR-RTL specific dimensions) -->
<resources>
    <dimen name="player_control_margin_start">16dp</dimen>
    <dimen name="player_control_margin_end">48dp</dimen>
</resources>
```

#### Vector Drawable Mirroring

```xml
<!-- Icons that should flip in RTL (back arrow, forward arrow) -->
<vector
    android:autoMirrored="true" <!-- Automatically flips in RTL -->
    android:width="24dp"
    android:height="24dp">
    <path android:pathData="M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z" />
</vector>
```

### 7.2. Plural Forms & Numeral Variations

#### Russian Plural Rules

Russian has **3 plural forms**:
- **one**: 1, 21, 31, 41, ... (один файл)
- **few**: 2-4, 22-24, 32-34, ... (два файла, три файла, четыре файла)
- **many**: 0, 5-20, 25-30, ... (пять файлов, десять файлов)

```xml
<!-- values-ru/plurals.xml -->
<resources>
    <plurals name="files_count">
        <item quantity="one">%d файл</item>      <!-- 1, 21, 31... -->
        <item quantity="few">%d файла</item>     <!-- 2-4, 22-24... -->
        <item quantity="many">%d файлов</item>   <!-- 0, 5-20, 25-30... -->
        <item quantity="other">%d файлов</item>  <!-- Fallback -->
    </plurals>
    
    <plurals name="minutes_duration">
        <item quantity="one">%d минута</item>
        <item quantity="few">%d минуты</item>
        <item quantity="many">%d минут</item>
        <item quantity="other">%d минут</item>
    </plurals>
</resources>
```

#### Ukrainian Plural Rules

Ukrainian follows similar rules to Russian but with slight differences:

```xml
<!-- values-uk/plurals.xml -->
<resources>
    <plurals name="files_count">
        <item quantity="one">%d файл</item>
        <item quantity="few">%d файли</item>     <!-- Note: файли, not файла -->
        <item quantity="many">%d файлів</item>
        <item quantity="other">%d файлів</item>
    </plurals>
</resources>
```

#### Complex Plural Examples

```kotlin
// Format file count with size
fun formatFilesSummary(count: Int, totalSize: Long): String {
    val filesText = resources.getQuantityString(
        R.plurals.files_count,
        count,
        count
    )
    
    val sizeText = when {
        totalSize < 1024 -> getString(R.string.size_bytes, totalSize)
        totalSize < 1024 * 1024 -> getString(R.string.size_kb, totalSize / 1024)
        totalSize < 1024 * 1024 * 1024 -> getString(R.string.size_mb, totalSize / (1024 * 1024))
        else -> getString(R.string.size_gb, totalSize / (1024 * 1024 * 1024))
    }
    
    return getString(R.string.files_summary, filesText, sizeText)
}

// strings.xml:
// <string name="files_summary">%1$s, %2$s total</string>

// values-ru/strings.xml:
// <string name="files_summary">%1$s, %2$s всего</string>
```

#### Duration Plurals

```kotlin
fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> {
            resources.getQuantityString(R.plurals.seconds_duration, seconds, seconds)
        }
        seconds < 3600 -> {
            val minutes = seconds / 60
            resources.getQuantityString(R.plurals.minutes_duration, minutes, minutes)
        }
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            getString(
                R.string.hours_minutes_duration,
                resources.getQuantityString(R.plurals.hours_duration, hours, hours),
                resources.getQuantityString(R.plurals.minutes_duration, minutes, minutes)
            )
        }
    }
}
```

### 7.3. Date & Time Formatting Per Locale

#### Locale-Aware Date Formatting

```kotlin
object DateFormatter {
    
    // Respects user's locale (DD/MM/YYYY vs MM/DD/YYYY vs YYYY-MM-DD)
    fun formatDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        return formatter.format(Date(timestamp))
    }
    
    // Full date with time
    fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val formatter = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            locale
        )
        return formatter.format(Date(timestamp))
    }
    
    // Relative time (Today, Yesterday, 3 days ago)
    fun formatRelative(timestamp: Long, context: Context): String {
        val now = System.currentTimeMillis()
        
        return when {
            DateUtils.isToday(timestamp) -> {
                context.getString(R.string.date_today)
            }
            DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> {
                context.getString(R.string.date_yesterday)
            }
            now - timestamp < 7 * DateUtils.DAY_IN_MILLIS -> {
                DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    now,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE
                ).toString()
            }
            else -> {
                formatDate(timestamp)
            }
        }
    }
}
```

#### Locale-Specific Examples

| Locale | Date Format | Time Format | Example |
|--------|-------------|-------------|---------|
| **en_US** | MM/DD/YYYY | 12-hour (AM/PM) | 01/06/2026, 2:30 PM |
| **en_GB** | DD/MM/YYYY | 24-hour | 06/01/2026, 14:30 |
| **ru_RU** | DD.MM.YYYY | 24-hour | 06.01.2026, 14:30 |
| **uk_UA** | DD.MM.YYYY | 24-hour | 06.01.2026, 14:30 |

#### Number Formatting Per Locale

```kotlin
object NumberFormatter {
    
    fun formatFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
        val numberFormat = NumberFormat.getInstance(locale)
        
        return when {
            bytes < 1024 -> {
                "$bytes ${context.getString(R.string.size_unit_bytes)}"
            }
            bytes < 1024 * 1024 -> {
                val kb = bytes.toDouble() / 1024
                "${numberFormat.format(kb)} ${context.getString(R.string.size_unit_kb)}"
            }
            bytes < 1024 * 1024 * 1024 -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                "${numberFormat.format(mb)} ${context.getString(R.string.size_unit_mb)}"
            }
            else -> {
                val gb = bytes.toDouble() / (1024 * 1024 * 1024)
                "${numberFormat.format(gb)} ${context.getString(R.string.size_unit_gb)}"
            }
        }
    }
    
    // Grouping separator differs by locale
    // en_US: 1,000,000 (comma)
    // ru_RU: 1 000 000 (space)
    // de_DE: 1.000.000 (period)
    fun formatLargeNumber(number: Long, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getInstance(locale).format(number)
    }
}
```

### 7.4. Currency Formatting (Future Enhancement)

If app adds paid features (e.g., Pro version):

```kotlin
fun formatPrice(amountInCents: Long, currencyCode: String, locale: Locale): String {
    val amount = amountInCents / 100.0
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = Currency.getInstance(currencyCode)
    return formatter.format(amount)
}

// Examples:
// en_US: $9.99
// ru_RU: 799,00 ₽
// uk_UA: 299,00 ₴
```

### 7.5. Sorting & Collation

```kotlin
// Locale-aware string sorting
fun sortFilesByName(files: List<MediaFile>, locale: Locale = Locale.getDefault()): List<MediaFile> {
    val collator = Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY // Ignore case and accents
    }
    
    return files.sortedWith { a, b ->
        collator.compare(a.name, b.name)
    }
}

// Cyrillic sorting:
// ru_RU: А, Б, В, Г, ...
// en_US: A, B, C, D, ... (would fail for Cyrillic)
```

### 7.6. Testing Edge Cases

```kotlin
@RunWith(AndroidJUnit4::class)
class LocalizationEdgeTest {
    
    @Test
    fun testRussianPlurals() {
        val resources = createConfigurationContext(Locale("ru")).resources
        
        // Test "one" form
        assertEquals("1 файл", resources.getQuantityString(R.plurals.files_count, 1, 1))
        assertEquals("21 файл", resources.getQuantityString(R.plurals.files_count, 21, 21))
        
        // Test "few" form
        assertEquals("2 файла", resources.getQuantityString(R.plurals.files_count, 2, 2))
        assertEquals("23 файла", resources.getQuantityString(R.plurals.files_count, 23, 23))
        
        // Test "many" form
        assertEquals("5 файлов", resources.getQuantityString(R.plurals.files_count, 5, 5))
        assertEquals("11 файлов", resources.getQuantityString(R.plurals.files_count, 11, 11))
    }
    
    @Test
    fun testDateFormatting() {
        val timestamp = 1704571200000L // 2026-01-06 12:00:00 UTC
        
        val usDate = DateFormatter.formatDate(timestamp, Locale.US)
        assertEquals("01/06/2026", usDate)
        
        val ruDate = DateFormatter.formatDate(timestamp, Locale("ru", "RU"))
        assertEquals("06.01.2026", ruDate)
    }
}
```

---

## Localization Checklist

### Pre-Release
- [ ] All UI strings in `strings.xml` (no hardcoded text)
- [ ] Plurals defined for all countable items
- [ ] Dates/numbers formatted with system APIs
- [ ] Russian translation complete (100%)
- [ ] Ukrainian translation complete (100%)
- [ ] Lint checks pass (`./gradlew lint`)
- [ ] Manual testing in all 3 languages
- [ ] Screenshot testing passed (optional)

### Ongoing Maintenance
- [ ] New strings added to all language files
- [ ] Translations reviewed by native speakers
- [ ] User feedback on translation quality monitored
- [ ] Documentation (README, FAQ) kept in sync

---

## Reference Files

### Source Code
- **String Resources**: `app_v2/src/main/res/values*/strings*.xml`
- **Locale Utils**: `util/LocaleUtils.kt`

### User Documentation
- **README**: `README.md` (en), `Readme_RU.md` (ru), `Readme_UK.md` (uk)
- **Quick Start**: `QUICK_START.md`, `QUICK_START_RU.md`, `QUICK_START_UK.md`
- **FAQ**: `FAQ.md`, `FAQ_RU.md`, `FAQ_UK.md`
- **Troubleshooting**: `TROUBLESHOOTING.md`, `TROUBLESHOOTING_RU.md`, `TROUBLESHOOTING_UK.md`

### Related Documents
- [33. Navigation Graph & Deep Links](33_navigation_graph.md) - Activity descriptions
- [29. Error Handling Strategy](29_error_handling.md) - Error message localization

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
