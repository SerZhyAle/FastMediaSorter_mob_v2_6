# 🔧 Troubleshooting Guide

Common problems and solutions for FastMediaSorter v2.

---

## Connection Issues

### ❌ "Cannot connect to SMB server"

**Possible causes:**
1. **Wrong network** - Phone must be on same Wi-Fi as NAS
2. **Wrong address format** - Try both formats:
   - `\\192.168.1.100\share`
   - `smb://192.168.1.100/share`
3. **Firewall blocking** - Check NAS firewall settings
4. **SMB version mismatch** - Some NAS require SMB v1 (deprecated)

**Solution:**
- Test connection from PC first
- Check NAS logs for connection attempts
- Try IP address instead of hostname
- Verify username/password

---

### ❌ "SFTP connection timeout"

**Possible causes:**
1. Wrong port (default: 22)
2. SSH server not running
3. Firewall blocking

**Solution:**
```
1. Test with SSH client on PC first:
   ssh username@192.168.1.100
2. Check if SSH service is running
3. Verify port in Settings
```

---

### ❌ "Google Drive sign-in failed"

**Solution:**
1. Clear app data: Settings → Apps → FastMediaSorter → Clear Data
2. Reinstall the app
3. Check Google account settings → Security → Third-party apps

---

### ❌ "OneDrive sign-in failed"

**Solution:**
1. Check Microsoft account status
2. Clear app data: Settings → Apps → FastMediaSorter → Clear Data
3. Check Microsoft account settings → Privacy → Apps and services

---

### ❌ "Dropbox sign-in failed"

**Solution:**
1. Check Dropbox account status
2. Clear app data: Settings → Apps → FastMediaSorter → Clear Data
3. Check Dropbox account settings → Security → Connected apps

---

## Performance Issues

### ❌ "App is slow / laggy"

**For large folders (5000+ files):**
1. Settings → Edit folder → Enable **"Disable thumbnails"**
2. Use **filters** to reduce visible files
3. Close other apps to free RAM

**For network folders:**
1. Check Wi-Fi signal strength
2. Reduce thumbnail cache size
3. Enable **"Scan subdirectories"** = OFF if not needed

---

### ❌ "Thumbnails not loading"

**Local files:**
- Check storage permissions
- Clear thumbnail cache
- Restart app

**Network files:**
- Scroll slower (thumbnails load on-demand)
- Check network speed
- Increase cache size in Settings

---

## File Operation Errors

### ❌ "Copy failed: Permission denied"

**Local files:**
- Grant storage permissions: Settings → Apps → Permissions
- Check if folder is read-only
- Try moving to different location

**Network files:**
- Check username has write permissions
- Verify share settings on NAS

---

### ❌ "Cannot delete file"

**Possible causes:**
1. File is open in another app
2. No write permission
3. File is system-protected

**Solution:**
- Close other apps
- Check folder permissions
- For network: verify user has delete rights

---

### ❌ "Move operation failed"

**Cross-protocol moves** (e.g., Local → SMB):
- These are actually **copy + delete**
- Requires free space on target
- May take longer for large files

**Solution:**
- Check available space
- Use Copy instead of Move for safety
- Wait for full operation to complete

---

## App Crashes

### ❌ "App crashes when opening player"

**Common causes:**
1. Corrupted video file
2. Unsupported codec
3. File too large (>4GB)

**Solution:**
- Try playing file in different app to verify
- Check file format (supported: MP4, MKV, AVI, MOV)
- Clear app cache

---

### ❌ "Media file not playing or no sound"

**Problem:** Video loads but shows black screen, or plays without sound.

**Solution:**
1. Tap the **ⓘ (Info)** button in top toolbar
2. Tap **"Open in External Player"**
3. Select a specialized player (e.g., VLC, MX Player)

This uses the *Secondary Player* feature to hand off unsupported codecs to other apps.

---

### ❌ "App crashes on startup"

**Solution:**
1. Clear app cache: Settings → Apps → FastMediaSorter → Clear Cache
2. If persists: Clear app data (⚠️ loses settings)
3. Reinstall app as last resort

---

## UI / Display Issues

### ❌ "Touch Zones not working"

**Check if enabled:**
Settings → Playback → **"Show touch zones hint on first run"** = ON

**Make visible:**
Settings → Playback → **"Always show touch zones overlay"** = ON

---

### ❌ "Command panel buttons too small"

**Solution:**
Settings → Playback → **"Compact mode"** = OFF

This doubles the size of all buttons and spacing.

---

### ❌ "Dark theme not working"

The app follows **system theme**:
- Android Settings → Display → Dark theme = ON

---

## Data Issues

### ❌ "Favorites disappeared"

Favorites are stored **locally**:
- Cleared app data? → Favorites lost
- New device? → Need to re-mark

**Prevention:**
- Use Settings → Backup to save settings
- Favorites are NOT included in backup (future feature)

---

### ❌ "Trash folder keeps growing"

Deleted files go to `.trash/` folder and stay there until manually emptied.

**Solution:**
1. Settings → Quick Sort → **"Clear Trash"**
2. Or manually delete `.trash/` folders

---

## Still Having Issues?

### Check Logs
1. Settings → General → **"Enable detailed errors"** = ON
2. Reproduce the issue
3. Check logcat output

### Report a Bug
Include this information:
- Android version
- Device model
- Steps to reproduce
- Error message (screenshot)

**Submit:** [GitHub Issues](https://github.com/SerZhyAle/FastMediaSorter_mob_v2/issues)

---

---
    
## Content Issues

### ❌ "Cannot see Text or PDF files"

**Solution:**
1. Check **Settings** → **General**
2. Ensure **"Support Text Files"** and **"Support PDF Files"** are enabled.
3. Check **Filters** on main screen (funnel icon) to ensure they are selected.
4. **Rescan** the folder (pull-to-refresh).

---

## Known Limitations

- ⚠️ **No RAW photo support** (CR2, NEF, ARW)
- ⚠️ **Network undo unavailable** (files are hard-deleted)
- ⚠️ **Dropbox/OneDrive incomplete** (foundation only)
- ⚠️ **No multi-device sync** (favorites are local)

---

**Last updated:** December 2025  
**Version:** v2.25.1206
