# Store Assets Checklist for FastMediaSorter v2

## Status: 🟢 Build Working - Ready for Screenshots

### ✅ Completed Tasks

- [x] Create store_assets directory structure
- [x] Write Play Store descriptions (English, Russian, Ukrainian)
- [x] Create README.md with guidelines
- [x] Create screenshot capture guide
- [x] Create automated screenshot script
- [x] Create production keystore generation script
- [x] Copy existing screenshots from V1

### ⏳ Pending Tasks

#### Screenshots (Priority: HIGH)
- [ ] Start Android emulator
- [ ] Launch FastMediaSorter v2 app
- [ ] Add test data (local folders with media)
- [ ] Run `capture-screenshots.ps1` script
- [ ] Capture 8 key screenshots:
  - [ ] 01_main_screen.png - Main Resources List
  - [ ] 02_grid_view.png - Media Grid/Browser
  - [ ] 03_image_viewer.png - Image Viewer
  - [ ] 04_video_player.png - Video Player
  - [ ] 05_add_resource.png - Add Resource Dialog
  - [ ] 06_network_config.png - Network Configuration
  - [ ] 07_settings.png - Settings Screen
  - [ ] 08_favorites.png - Favorites Tab
- [ ] Post-process screenshots:
  - [ ] Resize to 1080 x 2340 px if needed
  - [ ] Remove sensitive status bar info
  - [ ] Optimize PNG compression
- [ ] (Optional) Add device frames
- [ ] (Optional) Add text overlays with feature highlights

#### Graphics (Priority: HIGH)
- [ ] Design feature graphic (1024 x 500 px)
  - App name + tagline
  - Key visual (folder icon with media)
  - Feature badges (SMB, SFTP, Cloud)
  - Color scheme: Green (#4CAF50), Amber (#FFC107)
- [ ] Create high-res app icon (512 x 512 px)
  - Use existing launcher icon as base
  - Scale up to 512x512 with proper quality
- [ ] (Optional) Record promo video
  - 30-120 seconds
  - Show key features in action
  - Upload to YouTube

#### Keystore & Signing (Priority: HIGH)
- [ ] Run `generate-production-keystore.ps1`
- [ ] Back up keystore file to secure location (USB drive, password manager)
- [ ] Verify keystore.properties is in .gitignore
- [ ] Test signing with production keystore

#### Build (Priority: HIGH) - ✅ UNBLOCKED
- [x] Resolve Hilt/KSP build issue (Fixed - cleared Gradle cache)
- [x] Build debug APK successfully
- [x] Build signed release APK (app-release.apk)
- [ ] Test signed APK on multiple devices
- [x] ProGuard/R8 rules configured
- [x] Check APK size: **152.7 MB** ✅ (target: < 200 MB)

#### Play Console Setup (Priority: MEDIUM)
- [ ] Create/Access Google Play Console account
- [ ] Create new app listing
- [ ] Fill in app details:
  - [ ] App name: FastMediaSorter v2
  - [ ] Category: Productivity
  - [ ] Content rating: Everyone
  - [ ] Target age: 3+
- [ ] Upload store descriptions (EN, RU, UK)
- [ ] Upload screenshots
- [ ] Upload feature graphic
- [ ] Upload high-res icon
- [ ] Add privacy policy URL
- [ ] Add terms of service URL
- [ ] Set pricing: Free

#### Testing Track (Priority: MEDIUM)
- [ ] Create Internal Testing track
- [ ] Upload signed APK to Internal Testing
- [ ] Add test users (email addresses)
- [ ] Distribute to testers
- [ ] Collect feedback
- [ ] Fix reported issues
- [ ] Upload updated APK

#### Release (Priority: LOW - Do Last)
- [ ] Write release notes (EN, RU, UK)
- [ ] Submit to Production review
- [ ] Monitor review status
- [ ] Respond to review queries if any
- [ ] Publish app when approved
- [ ] Monitor crash reports
- [ ] Monitor user reviews

## Current Blockers

### ✅ RESOLVED: Build System Issue
**Problem:** Hilt/KSP/AGP8 compilation failure (FIXED 2026-01-08)

**Resolution:** The issue was caused by stale Gradle cache files. Fixed by:
1. Removing `.gradle` directory
2. Removing `app/.gradle` directory  
3. Removing `app/app/build` directory
4. Running `gradlew.bat :app:app:assembleDebug --rerun-tasks`

**Current Status:** Debug APK builds successfully. All 32 unit tests pass.

## Timeline Estimate

Assuming build issue is resolved:

- **Screenshots:** 2-3 hours
  - 1 hour: Capture and initial review
  - 1 hour: Post-processing and optimization
  - 1 hour: Optional device frames/overlays

- **Graphics:** 2-4 hours
  - 2 hours: Feature graphic design
  - 1 hour: High-res icon creation
  - 1 hour: Optional promo video

- **Keystore & Signing:** 30 minutes
  - 15 minutes: Generate keystore
  - 15 minutes: Test signing

- **Build:** 1-2 hours (after issue resolved)
  - 30 minutes: Build signed APK
  - 1 hour: Testing on devices
  - 30 minutes: Final verification

- **Play Console Setup:** 1-2 hours
  - 1 hour: Create listing and upload assets
  - 30 minutes: Fill in metadata
  - 30 minutes: Review and publish to Internal Testing

**Total: 7-12 hours of work** (excluding build issue resolution time)

## Resources Created

### Documentation
- [store_assets/README.md](store_assets/README.md) - Comprehensive guide
- [store_assets/SCREENSHOT_GUIDE.md](store_assets/SCREENSHOT_GUIDE.md) - Screenshot instructions
- [CHECKLIST.md](store_assets/CHECKLIST.md) - This file

### Store Listings
- [play_store_description_en.txt](store_assets/play_store_description_en.txt) - English listing
- [play_store_description_ru.txt](store_assets/play_store_description_ru.txt) - Russian listing
- [play_store_description_uk.txt](store_assets/play_store_description_uk.txt) - Ukrainian listing

### Scripts
- [capture-screenshots.ps1](capture-screenshots.ps1) - Automated screenshot capture
- [generate-production-keystore.ps1](generate-production-keystore.ps1) - Keystore generation

### Directories
- `store_assets/screenshots/` - Screenshot storage
- `store_assets/graphics/` - Graphic assets storage

## Next Immediate Steps

1. **Start emulator**: `emulator -avd Pixel_5_API_35`
2. **Launch app** (when build issue is resolved)
3. **Run screenshot script**: `.\capture-screenshots.ps1`
4. **Design feature graphic** using Canva/Figma
5. **Generate production keystore**: `.\generate-production-keystore.ps1`

## Notes

- Privacy Policy and Terms of Service URLs are already hosted on GitHub Pages
- App descriptions highlight v2 improvements over v1
- All three languages (EN, RU, UK) are ready for submission
- Existing launcher icons (192x192) need to be upscaled to 512x512 for store
- Consider A/B testing different feature graphics after initial launch

---
**Last Updated:** 2026-01-07 23:40 UTC
**Created By:** AI Assistant
**Status:** Ready for screenshot capture (pending emulator start)
