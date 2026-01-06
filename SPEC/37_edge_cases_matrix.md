# Edge Cases Matrix

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Status**: Ready for Implementation

---

## 1. Matrix Structure

Each row: **Feature** × **Edge Case** → **Expected Behavior** + **Priority** + **Test Coverage**

---

## 2. File Operations Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Copy File** | Source deleted mid-operation | Fail with error, partial copy cleaned | MUST | ✅ |
| **Copy File** | Destination disk full | Stop, show disk space error, rollback | MUST | ✅ |
| **Copy File** | File >2GB on FAT32 destination | Block with warning before starting | MUST | ⏳ |
| **Copy File** | Same source and destination | Block with "Same location" error | MUST | ✅ |
| **Copy File** | Destination has same filename | Show overwrite dialog (Skip/Overwrite/Rename) | MUST | ✅ |
| **Move File** | Network interruption mid-move | Rollback or mark as partial, allow retry | MUST | ✅ |
| **Move File** | Cross-protocol (SMB→Cloud) | Convert to copy+delete, atomic transaction | MUST | ⏳ |
| **Move File** | Move folder to its subfolder | Block with "Invalid destination" error | MUST | ✅ |
| **Delete File** | File locked by another process | Show "File in use" error, retry option | MUST | ⏳ |
| **Delete File** | Deleted file already gone | Treat as success (idempotent) | SHOULD | ✅ |
| **Batch Operations** | 1000 files, 50% fail | Complete successful ones, report failed list | MUST | ⏳ |

---

## 3. Network Protocol Edge Cases

### SMB

| Edge Case | Expected Behavior | Priority | Test |
|-----------|-------------------|----------|------|
| Server requires domain, user didn't provide | Show domain input field on auth error | MUST | ⏳ |
| SMB1 vs SMB2/3 negotiation fails | Try fallback, show protocol version warning | SHOULD | ❌ |
| Share name with spaces (`\\server\My Share`) | Encode properly, don't break path parsing | MUST | ✅ |
| Connection dropped, 10 files in transfer | Pause, show reconnect dialog, resume | MUST | ⏳ |
| Kerberos auth required | Show "Kerberos not supported" error | NICE | ❌ |

### SFTP

| Edge Case | Expected Behavior | Priority | Test |
|-----------|-------------------|----------|------|
| SSH key auth instead of password | Support key file selection in settings | SHOULD | ⏳ |
| Server changes host key | Show security warning, allow trust/reject | MUST | ⏳ |
| Non-standard port (not 22) | Allow port configuration in UI | MUST | ✅ |
| `~/.ssh/known_hosts` conflicts | Store host keys in app-private storage | MUST | ✅ |
| Connection timeout on large file list | Use chunked listing with progress | SHOULD | ⏳ |

### FTP

| Edge Case | Expected Behavior | Priority | Test |
|-----------|-------------------|----------|------|
| PASV mode timeout (firewall blocked) | Fallback to active mode automatically | MUST | ✅ |
| FTP over TLS (FTPS) | Support FTPS as protocol option | SHOULD | ⏳ |
| Anonymous FTP (no credentials) | Allow empty username/password | NICE | ❌ |
| ASCII vs Binary transfer mode | Always use Binary for media files | MUST | ✅ |
| LIST command returns non-standard format | Use MLSD if available, parse best-effort | SHOULD | ⏳ |

### Cloud (Google Drive/OneDrive/Dropbox)

| Edge Case | Expected Behavior | Priority | Test |
|-----------|-------------------|----------|------|
| OAuth token expired mid-operation | Auto-refresh token, retry operation | MUST | ✅ |
| User revokes app permissions | Detect 401, show re-auth dialog | MUST | ⏳ |
| Rate limit exceeded (429 Too Many Requests) | Exponential backoff, show "Slow down" message | MUST | ✅ |
| File shared by someone else (read-only) | Disable edit/delete buttons, show badge | MUST | ⏳ |
| Duplicate filename in cloud folder | Cloud handles it (appends " (1)"), reflect in UI | SHOULD | ⏳ |
| Upload file already exists with same content | Skip upload (compare hash), mark as complete | SHOULD | ❌ |

---

## 4. Media Player/Viewer Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Video Player** | Codec not supported by device | Show "Codec unsupported" error with details | MUST | ⏳ |
| **Video Player** | 4K video on low-end device | Auto-reduce quality or warn before loading | SHOULD | ⏳ |
| **Video Player** | Video file corrupted (partial download) | Show error, offer to re-download | MUST | ⏳ |
| **Video Player** | Subtitles file (`.srt`) in same folder | Auto-detect and load subtitles | NICE | ❌ |
| **Image Viewer** | Image >50MB (OOM risk) | Downsample before loading, show warning | MUST | ✅ |
| **Image Viewer** | EXIF rotation tag incorrect | Use EXIF tag first, allow manual rotation | MUST | ✅ |
| **Image Viewer** | Animated GIF >10MB | Show first frame only, warn about size | MUST | ✅ |
| **Image Viewer** | WebP/AVIF format unsupported on old Android | Fallback to Glide, show format warning | SHOULD | ⏳ |
| **PDF Viewer** | Password-protected PDF | Show password input dialog | SHOULD | ⏳ |
| **PDF Viewer** | PDF with 1000+ pages | Paginate, load pages on demand | MUST | ⏳ |
| **Audio Player** | Network stream buffers slowly | Show buffering progress, allow pause | MUST | ⏳ |

---

## 5. Image Editor Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Crop** | Crop area smaller than 50x50px | Block with "Crop too small" error | MUST | ⏳ |
| **Rotate** | Rotate 20MB image on 2GB RAM device | Downsample before rotation, warn user | MUST | ⏳ |
| **Filter** | Apply filter to 8K image | Process in chunks, show progress bar | SHOULD | ❌ |
| **Save Edited** | Original file deleted during edit | Prompt to save to new location | MUST | ⏳ |
| **Save Edited** | Disk space insufficient for output | Check before processing, show error early | MUST | ⏳ |
| **Undo Stack** | 50 undo operations (memory limit) | Limit to 20 operations, FIFO eviction | SHOULD | ⏳ |

---

## 6. Resource Management Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Add Resource** | Path doesn't exist (typo in SMB path) | Validate before saving, show error | MUST | ✅ |
| **Add Resource** | Duplicate resource (same path) | Block with "Already exists" error | MUST | ✅ |
| **Add Resource** | 10+ resources added | Show warning about performance impact | NICE | ❌ |
| **Delete Resource** | Resource in use (files being copied) | Block delete, show "In use" message | MUST | ⏳ |
| **Edit Resource** | Change credentials while connected | Disconnect, reconnect with new credentials | MUST | ⏳ |
| **Resource Color** | Same color for 3 resources | Allow, but suggest different colors | NICE | ❌ |

---

## 7. Database Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Migration** | Migration 5→6 fails mid-way | Rollback to backup, show restore dialog | MUST | ✅ |
| **Migration** | Database corrupted (disk failure) | Detect corruption, offer factory reset | MUST | ⏳ |
| **Cache** | Cache size exceeds limit (5GB) | Auto-evict oldest entries (LRU) | MUST | ✅ |
| **Cache** | Cache entry for deleted file | Clean up orphaned cache on startup | SHOULD | ⏳ |
| **Favorites** | Favorite file moved/renamed | Mark as "Not found", allow re-link | MUST | ⏳ |

---

## 8. UI/UX Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Browse View** | 10,000 files in folder | Use Paging3, load 100 at a time | MUST | ✅ |
| **Browse View** | Filenames with emoji (🎉.jpg) | Display correctly, handle in operations | MUST | ⏳ |
| **Browse View** | File modified time in future | Display as-is, log warning | NICE | ❌ |
| **Search** | Search query with special regex chars | Escape properly, don't crash | MUST | ⏳ |
| **Long Press** | Long press on 5 files, then rotate device | Preserve selection across config change | SHOULD | ⏳ |
| **Thumbnail** | Thumbnail generation fails | Show placeholder icon, retry on tap | MUST | ✅ |

---

## 9. Permission Edge Cases (Android)

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Storage** | User denies `READ_EXTERNAL_STORAGE` | Show rationale dialog, redirect to settings | MUST | ✅ |
| **Storage** | Scoped Storage (Android 11+) restriction | Use SAF for restricted folders, explain to user | MUST | ✅ |
| **Network** | `INTERNET` permission revoked (impossible?) | Should never happen, but check at runtime | NICE | ❌ |
| **Notifications** | User disables notifications (Android 13+) | Disable upload progress notifications gracefully | SHOULD | ⏳ |

---

## 10. Localization Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Language** | Switch language mid-session | Recreate Activity to apply new locale | MUST | ✅ |
| **Language** | Ukrainian translation missing for new string | Fallback to English, log missing key | MUST | ✅ |
| **Date Format** | User locale uses DD/MM/YYYY (UK) | Respect locale for date display | MUST | ✅ |
| **File Size** | Display KB/MB/GB in Russian | Use localized number format (1 000 МБ) | SHOULD | ⏳ |

---

## 11. Offline Mode Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Browse** | Offline, cached list is stale | Show cached list with "Offline" badge | MUST | ✅ |
| **Preview** | Offline, image not in cache | Show "Not available offline" placeholder | MUST | ✅ |
| **File Operation** | Queue operation, device restarts before sync | Restore queue from Room DB, continue | SHOULD | ⏳ |
| **Cloud Sync** | 50 pending operations, network restored | Process queue in order, limit concurrency to 3 | MUST | ⏳ |

---

## 12. Battery & Performance Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Background Sync** | Device in Doze mode | Defer sync until maintenance window | MUST | ⏳ |
| **Background Sync** | Battery <15% | Pause non-critical syncs, show notification | SHOULD | ⏳ |
| **Large Transfer** | Copy 5GB file, user locks screen | Continue in foreground service, show notification | MUST | ⏳ |
| **Thumbnail Generation** | Generate 1000 thumbnails | Use WorkManager, limit to 5 concurrent tasks | MUST | ✅ |

---

## 13. Security Edge Cases

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Credentials** | SMB password contains special chars (`@#$%`) | Encode properly, test with edge chars | MUST | ⏳ |
| **OAuth Token** | OAuth token stolen (device compromised) | Token expires in 1 hour, refresh required | MUST | ✅ |
| **File Access** | App tries to access `/system` or `/root` | Block with "Permission denied", log attempt | MUST | ✅ |
| **TLS** | MITM attack on FTPS connection | Certificate pinning (optional), warn user | NICE | ❌ |

---

## 14. Race Conditions

| Feature | Edge Case | Expected Behavior | Priority | Test |
|---------|-----------|-------------------|----------|------|
| **Parallel Scans** | User switches resource while scan in progress | Cancel old scan, start new scan | MUST | ✅ |
| **File Delete** | File deleted while being copied | Copy fails, show error, don't crash | MUST | ✅ |
| **Favorites** | Add to favorites while file being deleted | Check existence before adding, show warning | SHOULD | ⏳ |
| **Cache Invalidation** | File edited in external app during view | Detect change (modified time), reload preview | SHOULD | ⏳ |

---

## 15. Test Coverage Summary

| Priority | Total Cases | Tested | Not Tested | Coverage |
|----------|-------------|--------|------------|----------|
| **MUST** | 52 | 32 | 20 | 62% |
| **SHOULD** | 28 | 8 | 20 | 29% |
| **NICE** | 12 | 0 | 12 | 0% |
| **TOTAL** | 92 | 40 | 52 | 43% |

---

## 16. Priority Definitions

- **MUST**: Critical for MVP, blocks release if fails
- **SHOULD**: Important for production quality, fix before v1.0
- **NICE**: Quality-of-life, can defer to v1.1+

---

## 17. Testing Checklist

**Before Epic 1 (Foundation)**:
- ✅ Database migration failure → rollback
- ⏳ Disk full during file operation

**Before Epic 2 (Local Files)**:
- ✅ Copy to same location
- ✅ Overwrite existing file dialog
- ⏳ Batch operation partial failure

**Before Epic 3 (Player)**:
- ⏳ Video codec unsupported
- ✅ Image >50MB OOM protection
- ✅ Animated GIF size warning

**Before Epic 4 (Network)**:
- ✅ FTP PASV fallback to active mode
- ⏳ SFTP connection timeout on large directory
- ⏳ SMB connection dropped mid-transfer

**Before Epic 5 (Cloud)**:
- ✅ OAuth token refresh
- ✅ Rate limit exponential backoff
- ⏳ User revokes app permissions

**Before Epic 7 (Quality)**:
- ⏳ Complete all MUST test cases (target: 90% coverage)
- ⏳ Complete all SHOULD test cases (target: 60% coverage)

---

## 18. Implementation Notes

### How to Handle Edge Cases in Code

```kotlin
// Example: Copy file with comprehensive edge case handling
suspend fun copyFile(source: MediaFile, destination: Resource): Result<Unit> {
    // Edge Case 1: Same source and destination
    if (source.resourceId == destination.id && source.parentPath == destination.path) {
        return Result.Error("Cannot copy to same location")
    }
    
    // Edge Case 2: Check disk space before starting
    val requiredSpace = source.size
    val availableSpace = destination.getAvailableSpace()
    if (availableSpace < requiredSpace) {
        return Result.Error("Insufficient disk space: ${availableSpace.formatFileSize()} available, ${requiredSpace.formatFileSize()} required")
    }
    
    // Edge Case 3: Check FAT32 limit (4GB)
    if (destination.isExFAT == false && requiredSpace > 4_294_967_296L) {
        return Result.Error("File too large for FAT32 file system (max 4GB)")
    }
    
    // Edge Case 4: Handle existing file
    val existingFile = destination.checkFileExists(source.name)
    if (existingFile != null) {
        val userChoice = showOverwriteDialog(source.name)
        when (userChoice) {
            OverwriteChoice.SKIP -> return Result.Success(Unit)
            OverwriteChoice.RENAME -> source.name = generateUniqueName(source.name)
            OverwriteChoice.OVERWRITE -> {} // Continue
        }
    }
    
    // Edge Case 5: Network interruption during transfer
    return try {
        withContext(Dispatchers.IO) {
            copyWithRetry(source, destination, maxRetries = 3)
        }
    } catch (e: IOException) {
        // Edge Case 6: Source deleted mid-operation
        if (e.message?.contains("ENOENT") == true) {
            return Result.Error("Source file no longer exists")
        }
        Result.Error("Copy failed: ${e.message}")
    } finally {
        // Edge Case 7: Clean up partial copy on failure
        cleanupPartialCopy(destination, source.name)
    }
}
```

---

## 19. User-Facing Error Messages

All edge cases must have clear, actionable error messages:

```xml
<!-- strings.xml -->
<string name="error_same_location">Cannot copy to the same location</string>
<string name="error_disk_full">Not enough space. Free up %1$s and try again.</string>
<string name="error_file_too_large_fat32">File is too large for this drive (FAT32 limit: 4GB)</string>
<string name="error_file_in_use">File is currently in use. Close other apps and retry.</string>
<string name="error_codec_unsupported">Video codec not supported on this device</string>
<string name="warning_mobile_data">Using mobile data. Large file may incur charges.</string>
```

---

## 20. Next Steps

1. **Review with QA team**: Identify additional edge cases
2. **Prioritize untested MUST cases**: Focus on 20 critical gaps
3. **Automate edge case tests**: Add to CI/CD pipeline
4. **Document workarounds**: For NICE-to-have cases not implemented
