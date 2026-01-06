# Platform Specifics & Legacy Quirks

> [!WARNING]
> This section contains rules written in blood. Do not ignore platform-specific overrides.

## 1. Android Version Differences

### Android 11+ (API 30+) - Storage Access Framework (SAF)
- **Direct File Access**: Blocked for shared storage (DCIM, Pictures, etc.) without `MANAGE_EXTERNAL_STORAGE`.
- **Requirement**: Use `MediaStore` API for scanning/reading.
- **Write Operations**: MUST use `DocumentFile` API via URI for Copy/Move/Delete/Rename in shared storage.
- **Performance**: SAF is slower than File API. Use bulk operations where possible.
- **Scanning**:
  - **DO NOT** use recursive `File.listFiles()`.
  - **DO** use `ContentResolver.query(MediaStore.Files.getContentUri("external"), ...)` with `LIKE` selection for path filtering.

### Android 10 (API 29)
- Legacy Storage model available via `requestLegacyExternalStorage="true"`.
- Use File API where possible for performance.

## 2. Vendor-Specific Hacks

- **Samsung**:
  - `DocumentFile.listConfigFiles()` may return duplicates.
  - SD Card root path variances.
- **Xiaomi/MIUI**:
  - Aggressive battery optimization kills background sync/scan.
  - Requires explicit "Autostart" permission for WorkManager reliability.

## 3. Media Format Quirks

- **PDF**:
  - Use `PdfRenderer` (native) for 16KB page size compatibility (Android 15+).
  - Avoid heavy 3rd party libraries if native suffices.
- **ExoPlayer**:
  - Some MKV subtitles require custom extractors.
