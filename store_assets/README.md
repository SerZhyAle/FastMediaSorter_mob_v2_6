# Google Play Store Assets Preparation Guide

## Overview
This directory contains all assets needed for Google Play Store listing.

## Directory Structure
```
store_assets/
├── play_store_description_en.txt    # English store listing
├── play_store_description_ru.txt    # Russian store listing
├── play_store_description_uk.txt    # Ukrainian store listing
├── screenshots/                      # App screenshots (to be added)
│   ├── phone/                       # Phone screenshots (2-8 required)
│   ├── 7-inch-tablet/              # 7" tablet screenshots (optional)
│   └── 10-inch-tablet/             # 10" tablet screenshots (optional)
├── graphics/                         # Promotional graphics
│   ├── feature_graphic.png         # 1024 x 500 px (required)
│   ├── icon_512.png               # 512 x 512 px (required)
│   └── promo_video_url.txt        # YouTube video URL (optional)
└── README.md                        # This file
```

## Requirements

### 1. Screenshots
**Phone (Required):**
- 2-8 screenshots
- JPEG or 24-bit PNG (no alpha)
- Minimum dimension: 320px
- Maximum dimension: 3840px
- Max aspect ratio: 2:1

**Recommended size:** 1080 x 2340 px (modern phone aspect ratio)

**Key screens to capture:**
1. Main screen (Resources list with add button)
2. Resource grid/browser view
3. Image viewer with controls
4. Video player
5. Settings screen
6. Network configuration (SMB/SFTP)
7. Cloud integration (Drive/Dropbox)
8. Favorites tab

### 2. Feature Graphic
- **Size:** 1024 x 500 px
- **Format:** JPEG or 24-bit PNG (no alpha)
- **Purpose:** Displayed at top of store listing
- **Content:** App branding, key features, call-to-action

### 3. App Icon (High-res)
- **Size:** 512 x 512 px
- **Format:** 32-bit PNG (with alpha)
- **Purpose:** Play Store listing icon
- **Note:** Different from launcher icon (already created at 192x192)

### 4. Promo Video (Optional)
- YouTube video URL
- 30 seconds to 2 minutes recommended
- Show key features and usage

## App Metadata

### Category
**Primary:** Productivity
**Tags:** media, file manager, photo organizer, video player, cloud storage

### Content Rating
Target rating: **Everyone**
- No violence, adult content, or gambling
- Standard media management app

### Target Age
**Age:** 3+ (General Audience)

### Price
**Free** (no in-app purchases)

### App Details
- **Version:** 2.60.1071.400
- **Version Code:** 26010714
- **Min SDK:** Android 9.0 (API 28)
- **Target SDK:** Android 15 (API 35)

## Store Listing Links

### Privacy Policy
https://serzhyale.github.io/FastMediaSorter_mob_v2/PRIVACY_POLICY.html

### Terms of Service
https://serzhyale.github.io/FastMediaSorter_mob_v2/TERMS_OF_SERVICE.html

## Release Notes Template

### Version 2.60 (Build 26010714)
```
🎉 Major Update: FastMediaSorter v2!

✨ What's New:
• Complete redesign with Material Design 3
• Enhanced favorites system across all sources
• PIN protection for individual resources
• Built-in EPUB e-book reader
• PDF and text document viewer
• Auto-translation with hybrid OCR
• Music lyrics support
• Download network files locally
• Undo actions with trash management

🚀 Improvements:
• Faster thumbnail generation
• Better network protocol support
• Improved video player
• Enhanced image editor
• Optimized for large collections (10K+ files)

📱 New Features:
• Per-resource configuration
• WebDAV support
• Batch operations
• Keyboard navigation
• Dark theme improvements
```

## Screenshot Capture Instructions

### Using Android Emulator:
1. Launch app on emulator (already running on emulator-5554)
2. Navigate to each key screen
3. Press `Ctrl + S` or use emulator screenshot button
4. Screenshots saved to: `C:\Users\<username>\Pictures\`

### Using Physical Device:
1. Enable Developer Options
2. Connect via ADB
3. Use: `adb shell screencap -p /sdcard/screenshot.png`
4. Pull: `adb pull /sdcard/screenshot.png`

### Post-Processing:
- Resize to 1080 x 2340 px if needed
- Remove status bar sensitive info (time, battery)
- Add device frame (optional but recommended)
- Optimize PNG compression

## Feature Graphic Design Guidelines

### Content Suggestions:
- App name: "FastMediaSorter v2"
- Tagline: "Your All-in-One Media Manager"
- Key visual: Folder icon with media thumbnails
- Feature badges: "SMB • SFTP • FTP • Cloud"
- Color scheme: Green primary (#4CAF50), Amber secondary (#FFC107)

### Design Tools:
- Canva (free templates available)
- Figma (community templates)
- GIMP (open source)
- Adobe Photoshop/Illustrator

## Localization

Store listing available in:
- English (en-US) ✓
- Russian (ru-RU) ✓
- Ukrainian (uk-UA) ✓

## Next Steps

1. ✓ Create store descriptions (DONE)
2. ☐ Capture 8 high-quality screenshots
3. ☐ Design feature graphic (1024 x 500)
4. ☐ Create high-res icon (512 x 512)
5. ☐ Record promo video (optional)
6. ☐ Generate production keystore
7. ☐ Build signed release APK
8. ☐ Upload to Play Console (Internal Testing first)
9. ☐ Submit for review

## Testing Before Release

### Internal Testing Checklist:
- [ ] Install on multiple devices/Android versions
- [ ] Test all network protocols (SMB, SFTP, FTP)
- [ ] Verify cloud integration (Drive, Dropbox, OneDrive)
- [ ] Check permissions flow
- [ ] Test with 10,000+ file collection
- [ ] Verify image editing on network files
- [ ] Test video playback (all formats)
- [ ] Check EPUB reader
- [ ] Verify PDF viewer
- [ ] Test OCR translation
- [ ] Check dark theme
- [ ] Test keyboard navigation
- [ ] Verify undo/trash functionality
- [ ] Test PIN protection
- [ ] Check favorites system

## Resources

- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [Store Listing Best Practices](https://developer.android.com/distribute/best-practices/launch/store-listing)
- [Screenshot Guidelines](https://support.google.com/googleplay/android-developer/answer/9866151)
- [App Content Guidelines](https://support.google.com/googleplay/android-developer/answer/9898353)
