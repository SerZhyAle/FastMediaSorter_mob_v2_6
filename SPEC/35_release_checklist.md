# 35. Release Checklist & Pre-Launch Validation

**Last Updated**: January 6, 2026  
**Purpose**: Comprehensive pre-release checklist for FastMediaSorter v2.

This document defines validation steps, code review requirements, performance profiling, security audit, and Google Play submission guidelines.

---

## Overview

Release process ensures production-ready quality:
- **Code Quality**: Lint checks, unit tests, static analysis
- **Performance**: Profiling, benchmarks, memory leaks
- **Security**: Credential encryption, ProGuard, permissions
- **Functionality**: Manual testing, edge cases, regression tests
- **Compliance**: Privacy policy, terms of service, store listing

### Release Types

| Type | Frequency | Distribution | Validation |
|------|-----------|--------------|------------|
| **Debug** | Daily | Internal (dev team) | Basic smoke tests |
| **Alpha** | Weekly | Internal testers (5-10 users) | Regression tests |
| **Beta** | Bi-weekly | Closed beta (50-100 users) | Full validation |
| **Production** | Monthly | Google Play (public) | **All checks below** |

---

## Table of Contents

1. [Code Quality Validation](#1-code-quality-validation)
2. [Testing Validation](#2-testing-validation)
3. [Performance Validation](#3-performance-validation)
4. [Security Validation](#4-security-validation)
5. [Localization Validation](#5-localization-validation)
6. [Google Play Submission](#6-google-play-submission)
7. [Post-Release Monitoring](#7-post-release-monitoring)

---

## 1. Code Quality Validation

### 1.1. Lint Checks

**Run Android Lint**:
```bash
./gradlew :app_v2:lint
```

**Critical Issues to Fix**:
- [ ] No hardcoded strings (`HardcodedText`)
- [ ] No unused resources (`UnusedResources`)
- [ ] No missing translations (`MissingTranslation`)
- [ ] No security warnings (`TrustAllCertificates`, `SetJavaScriptEnabled`)
- [ ] No performance warnings (`Wakelock`, `Recycle`)

**Report**: `app_v2/build/reports/lint-results.html`

**Threshold**: 0 errors, < 5 warnings

---

### 1.2. Static Analysis (Detekt)

**Install Detekt**:
```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/config/detekt.yml")
}
```

**Run**:
```bash
./gradlew detekt
```

**Checks**:
- Complexity (cyclomatic complexity < 15)
- Code smells (long methods, too many parameters)
- Naming conventions
- Potential bugs (unused variables, null safety)

**Report**: `app_v2/build/reports/detekt/detekt.html`

---

### 1.3. Dependency Vulnerability Scan

**OWASP Dependency-Check**:
```kotlin
plugins {
    id("org.owasp.dependencycheck") version "8.4.0"
}

dependencyCheck {
    format = "HTML"
    suppressionFile = "config/dependency-check-suppressions.xml"
}
```

**Run**:
```bash
./gradlew dependencyCheckAnalyze
```

**Check for**:
- Known CVEs in dependencies (SMBJ, SSHJ, etc.)
- Outdated libraries with security patches

**Report**: `build/reports/dependency-check-report.html`

---

### 1.4. Code Coverage

**Run Unit + Integration Tests**:
```bash
./gradlew :app_v2:testDebugUnitTest :app_v2:connectedDebugAndroidTest
./gradlew :app_v2:jacocoTestReport
```

**Coverage Targets**:
- [ ] Domain (UseCases): **> 85%**
- [ ] Data (Repositories): **> 75%**
- [ ] UI (ViewModels): **> 80%**

**Report**: `app_v2/build/reports/jacoco/jacocoTestReport/html/index.html`

---

## 2. Testing Validation

### 2.1. Unit Tests

**Run all unit tests**:
```bash
./gradlew :app_v2:testDebugUnitTest
```

**Must Pass**:
- [ ] All 150+ unit tests pass (0 failures)
- [ ] No flaky tests (run 3 times, all pass)

---

### 2.2. Integration Tests

**Run on real device/emulator**:
```bash
./gradlew :app_v2:connectedDebugAndroidTest
```

**Critical Paths**:
- [ ] Room DAO tests (insert/query/delete)
- [ ] Database migration tests (1→2→3→4)
- [ ] SMB/SFTP/FTP connection tests (requires test server)

---

### 2.3. UI Tests (Smoke Tests)

**Manual Testing Checklist**:

**MainActivity**:
- [ ] Launch app (cold start < 1.5s)
- [ ] Add local resource
- [ ] Add SMB resource (test connection)
- [ ] Edit resource
- [ ] Delete resource
- [ ] Swipe to reorder resources

**BrowseActivity**:
- [ ] Open local resource (100 files)
- [ ] Open network resource (SMB/SFTP)
- [ ] Sort by name/date/size
- [ ] Filter images/videos/all
- [ ] Switch list/grid display mode
- [ ] Select multiple files
- [ ] Copy to destination

**PlayerActivity**:
- [ ] Open image (zoom/pan)
- [ ] Open video (play/pause/seek)
- [ ] Swipe left/right between files
- [ ] Copy file to destination
- [ ] Move file to destination
- [ ] Delete file (undo)
- [ ] Rotate image
- [ ] Edit image (brightness/contrast)
- [ ] Share file

**SettingsActivity**:
- [ ] Change theme (light/dark/auto)
- [ ] Adjust cache size
- [ ] Clear cache
- [ ] Toggle auto-sync

---

### 2.4. Edge Cases

**Must Test**:
- [ ] Empty resource (no files)
- [ ] Large resource (1000+ files, pagination)
- [ ] Corrupted media file (handle error gracefully)
- [ ] Network timeout (SMB/SFTP)
- [ ] Disk full (copy operation fails)
- [ ] Permission denied (storage access)
- [ ] Process death (Activity recreated)
- [ ] Configuration change (rotation)
- [ ] Background→Foreground (state restored)

---

## 3. Performance Validation

### 3.1. Startup Performance

**Measure cold start**:
```bash
adb shell am force-stop com.apemax.fastmediasorter
adb shell am start -W com.apemax.fastmediasorter/.ui.main.MainActivity
```

**Output**:
```
TotalTime: 1245
```

**Target**: < 1500ms (1.5s)

---

### 3.2. Memory Profiling

**Android Studio Profiler**:
1. Launch app
2. Navigate: MainActivity → BrowseActivity (1000 files) → PlayerActivity
3. Monitor memory graph

**Checks**:
- [ ] No memory leaks (Activities released after navigation)
- [ ] Memory usage < 200MB for 1000 files
- [ ] No OutOfMemory crashes

**LeakCanary** (debug build):
- [ ] Zero leaks detected

---

### 3.3. Janky Frames (UI Performance)

**Run with GPU Profiling**:
```bash
adb shell dumpsys gfxinfo com.apemax.fastmediasorter reset
# Use app for 30s
adb shell dumpsys gfxinfo com.apemax.fastmediasorter
```

**Check**:
- [ ] < 5% janky frames (>16ms render time)

---

### 3.4. APK Size

**Build release APK**:
```bash
./gradlew :app_v2:assembleRelease
```

**Check size**:
```bash
ls -lh app_v2/build/outputs/apk/release/*.apk
```

**Target**: < 25MB (universal APK)

**Optimization**:
- [ ] ProGuard/R8 enabled
- [ ] Unused resources removed
- [ ] Split APKs by ABI (optional)

---

## 4. Security Validation

### 4.1. Credential Encryption

**Verify**:
- [ ] Passwords encrypted with Android Keystore (AES-256-GCM)
- [ ] OAuth tokens stored in EncryptedSharedPreferences
- [ ] No plaintext passwords in database

**Test**:
```bash
adb shell run-as com.apemax.fastmediasorter cat databases/app_database.db | strings | grep password
# Should find no readable passwords
```

---

### 4.2. Network Security

**Check**:
- [ ] Network Security Config enabled
- [ ] HTTPS enforced for cloud APIs (googleapis.com, graph.microsoft.com, api.dropboxapi.com)
- [ ] Cleartext traffic blocked for production domains
- [ ] SSL/TLS enabled for SMB/SFTP

**Test**:
```bash
adb logcat | grep "Cleartext HTTP traffic"
# Should find no cleartext traffic to production APIs
```

---

### 4.3. ProGuard Validation

**Check obfuscation**:
```bash
unzip -l app_v2/build/outputs/apk/release/app-release.apk | grep classes.dex
```

**Verify**:
- [ ] Class names obfuscated (e.g., `a.b.c` instead of `com.apemax.fastmediasorter.ui.MainActivity`)
- [ ] No hardcoded secrets in APK

**Decompile APK** (optional):
```bash
apktool d app-release.apk -o decompiled
grep -r "password\|api_key\|secret" decompiled/
# Should find no hardcoded secrets
```

---

### 4.4. Permission Audit

**Check AndroidManifest.xml**:
- [ ] Only necessary permissions declared
- [ ] No dangerous permissions without rationale
- [ ] MANAGE_EXTERNAL_STORAGE only if needed (optional permission)

**Test Permission Flow**:
- [ ] Request permission → Show rationale → Grant → Success
- [ ] Request permission → Deny → Show error → Redirect to Settings

---

## 5. Localization Validation

### 5.1. Translation Completeness

**Check all languages**:
- [ ] English (en): 100% (baseline)
- [ ] Russian (ru): 100%
- [ ] Ukrainian (uk): 100%

**Run Lint**:
```bash
./gradlew :app_v2:lint
```

**Fix**:
- Missing translations (`MissingTranslation`)
- Unused strings (`UnusedResources`)

---

### 5.2. Manual Testing

**Test in each language**:
1. Settings → System → Languages → Russian
2. Restart app
3. Navigate all screens
4. **Check**:
   - [ ] No English fallback strings visible
   - [ ] No truncated text (buttons, labels)
   - [ ] Plurals correct (1 файл, 2 файла, 5 файлов)

**Repeat for Ukrainian**

---

### 5.3. Screenshot Verification

**Capture screenshots**:
- MainActivity (resource list)
- BrowseActivity (file list)
- PlayerActivity (image viewer)
- SettingsActivity (preferences)

**Check**:
- [ ] All text translated
- [ ] Layout not broken (text fits)

---

## 6. Google Play Submission

### 6.1. Build Signed APK/AAB

**Generate signing key** (first time only):
```bash
keytool -genkey -v -keystore release.keystore -alias fastmediasorter -keyalg RSA -keysize 2048 -validity 10000
```

**Build signed AAB**:
```bash
./gradlew :app_v2:bundleRelease
```

**Output**: `app_v2/build/outputs/bundle/release/app-release.aab`

---

### 6.2. Store Listing

**English**:
- [ ] Title: "FastMediaSorter - Media Organizer"
- [ ] Short description (80 chars): "Organize photos/videos from local folders, SMB, SFTP, cloud storage"
- [ ] Full description (4000 chars): See `store_assets/description_en.txt`
- [ ] Keywords: media, photo, video, organizer, SMB, SFTP, cloud
- [ ] Screenshots (8 required): Phone + Tablet
- [ ] Feature graphic (1024x500)
- [ ] App icon (512x512)

**Russian**:
- [ ] Title: "FastMediaSorter - Менеджер Медиа"
- [ ] Short description: "Управление фото/видео из локальных папок, SMB, SFTP, облака"
- [ ] Full description: See `store_assets/description_ru.txt`
- [ ] Screenshots: Localized (Russian UI)

**Ukrainian**:
- [ ] Title: "FastMediaSorter - Медіа Організатор"
- [ ] Short description: "Керування фото/відео з локальних папок, SMB, SFTP, хмари"
- [ ] Full description: See `store_assets/description_uk.txt`
- [ ] Screenshots: Localized (Ukrainian UI)

---

### 6.3. Privacy Policy & Terms

**Required Links**:
- [ ] Privacy Policy: `https://fastmediasorter.com/PRIVACY_POLICY.html`
- [ ] Terms of Service: `https://fastmediasorter.com/TERMS_OF_SERVICE.html`

**Content Requirements**:
- [ ] Data collection disclosure (network credentials stored locally, encrypted)
- [ ] Third-party services (Google Drive, OneDrive, Dropbox OAuth)
- [ ] User rights (data export, deletion)

---

### 6.4. Content Rating

**Complete questionnaire**:
- [ ] Violence: None
- [ ] Sexual Content: None
- [ ] Profanity: None
- [ ] Drugs/Alcohol: None
- [ ] Gambling: None

**Result**: PEGI 3, ESRB Everyone

---

### 6.5. Target SDK & Permissions

**Check**:
- [ ] compileSdk: 35 (Android 15)
- [ ] targetSdk: 35 (required by Google Play)
- [ ] minSdk: 24 (Android 7.0)

**Permission Declaration**:
- [ ] All permissions have `<uses-permission>` tag
- [ ] Dangerous permissions justified in description
- [ ] MANAGE_EXTERNAL_STORAGE explained (optional, for full file access)

---

### 6.6. Upload to Google Play Console

1. Open [Google Play Console](https://play.google.com/console)
2. Select app → Production → Create new release
3. Upload `app-release.aab`
4. Fill release notes (see Section 6.7)
5. Review → Roll out to Production (or Staged Rollout)

---

### 6.7. Release Notes

**Format** (for each language):

**English**:
```markdown
Version 2.51.2161 (2025-01-06)

🎉 What's New:
- Google Drive integration (Phase 3): OAuth authentication, file browsing, download
- Pagination support: Smooth scrolling for 1000+ files
- Performance improvements: 20% faster thumbnail loading

🐛 Bug Fixes:
- Fixed FTP PASV mode timeout (fallback to active mode)
- Fixed SMB connection pool leak (auto-cleanup after 45s)
- Fixed PlayerActivity memory leak (ExoPlayer release on destroy)

🔧 Technical:
- Upgraded Glide to 4.16.0
- Upgraded Room to 2.6.1
- Migrated LiveData → StateFlow (better coroutine support)
```

**Russian**:
```markdown
Версия 2.51.2161 (2025-01-06)

🎉 Что нового:
- Интеграция Google Drive (Фаза 3): OAuth авторизация, просмотр файлов, загрузка
- Поддержка пагинации: Плавная прокрутка для 1000+ файлов
- Улучшение производительности: На 20% быстрее загрузка миниатюр

🐛 Исправления:
- Исправлен таймаут FTP PASV режима (переключение на активный режим)
- Исправлена утечка пула соединений SMB (автоочистка через 45с)
- Исправлена утечка памяти PlayerActivity (освобождение ExoPlayer при destroy)

🔧 Технические:
- Обновлен Glide до 4.16.0
- Обновлен Room до 2.6.1
- Миграция LiveData → StateFlow (лучшая поддержка корутин)
```

---

## 7. Post-Release Monitoring

### 7.1. Crash Reporting (Firebase Crashlytics)

**Monitor first 48 hours**:
- [ ] Crash-free rate: > 99%
- [ ] ANR rate: < 0.5%
- [ ] Top crashes: 0 critical

**Dashboard**: [Firebase Console](https://console.firebase.google.com)

---

### 7.2. Performance Monitoring

**Track**:
- [ ] Cold start time: < 1.5s (P95)
- [ ] Screen rendering: < 16ms (P95)
- [ ] Network request success rate: > 95%

**Dashboard**: Firebase Performance Monitoring

---

### 7.3. User Feedback

**Monitor**:
- Google Play reviews (respond within 24h)
- GitHub Issues (respond within 48h)
- Email support (respond within 72h)

**Critical Issues**:
- Data loss bugs → Hotfix within 24h
- Crash on launch → Hotfix within 12h
- Security vulnerabilities → Hotfix ASAP

---

### 7.4. Staged Rollout

**Recommendation**: Use staged rollout for major releases.

**Rollout Plan**:
- Day 1: 5% of users
- Day 3: 20% of users (if crash-free rate > 99%)
- Day 5: 50% of users
- Day 7: 100% of users

**Rollback Criteria**:
- Crash-free rate < 98%
- Critical bug reports > 10
- Negative reviews > 10% of downloads

---

## Pre-Release Checklist (Master List)

### Code Quality
- [ ] Lint: 0 errors, < 5 warnings
- [ ] Detekt: No critical issues
- [ ] Dependency scan: No high-severity CVEs
- [ ] Code coverage: Domain 85%+, Data 75%+, ViewModel 80%+

### Testing
- [ ] Unit tests: 150+ passing (0 failures)
- [ ] Integration tests: All passing
- [ ] UI smoke tests: Manual checklist complete
- [ ] Edge cases: All tested (empty state, 1000+ files, network timeout, etc.)

### Performance
- [ ] Cold start: < 1.5s
- [ ] Memory usage: < 200MB (1000 files)
- [ ] Janky frames: < 5%
- [ ] APK size: < 25MB
- [ ] LeakCanary: 0 leaks

### Security
- [ ] Credentials encrypted (Android Keystore)
- [ ] Network Security Config enabled
- [ ] ProGuard obfuscation verified
- [ ] No hardcoded secrets in APK
- [ ] Permission rationales provided

### Localization
- [ ] English: 100%
- [ ] Russian: 100%
- [ ] Ukrainian: 100%
- [ ] Manual testing in all 3 languages
- [ ] Screenshots localized

### Google Play
- [ ] Signed AAB built
- [ ] Store listing complete (3 languages)
- [ ] Privacy Policy published
- [ ] Terms of Service published
- [ ] Content rating completed
- [ ] Release notes written (3 languages)
- [ ] Screenshots uploaded (8 per language)
- [ ] Feature graphic uploaded

### Post-Release
- [ ] Firebase Crashlytics monitoring enabled
- [ ] Performance monitoring enabled
- [ ] Staged rollout plan defined
- [ ] Rollback criteria defined
- [ ] Support channels monitored

---

## 8. Staged Rollout & Rollback Strategy

### 8.1. Staged Rollout Plan

**Objective**: Minimize risk by gradually increasing user exposure to new release.

#### Rollout Schedule

| Stage | User % | Duration | Validation Criteria |
|-------|--------|----------|---------------------|
| **Internal Testing** | 0.1% (5-10 users) | 24 hours | Manual testing, no crashes |
| **Alpha** | 1% (~100 users) | 48 hours | Crash-free rate >99%, <10 user complaints |
| **Beta** | 5% (~500 users) | 72 hours | Performance metrics stable, <5% error rate |
| **Production Phase 1** | 25% | 7 days | ANR rate <0.5%, no critical bugs |
| **Production Phase 2** | 50% | 7 days | Same as Phase 1 |
| **Production Phase 3** | 100% | Permanent | Continuous monitoring |

#### Google Play Console Configuration

```bash
# Using Play Console UI:
1. Navigate to: Release → Production → Create new release
2. Upload signed AAB
3. Set rollout percentage: 5% → 25% → 50% → 100%
4. Monitor for 72 hours before increasing
```

#### Automated Rollout Progression

```yaml
# .github/workflows/rollout.yml
name: Staged Rollout

on:
  workflow_dispatch:
    inputs:
      percentage:
        description: 'Rollout percentage'
        required: true
        type: choice
        options:
          - '5'
          - '25'
          - '50'
          - '100'

jobs:
  rollout:
    runs-on: ubuntu-latest
    steps:
      - name: Increase rollout
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJson: ${{ secrets.PLAY_STORE_JSON }}
          packageName: com.apemax.fastmediasorter
          track: production
          inAppUpdatePriority: 2
          userFraction: ${{ github.event.inputs.percentage }}
          
      - name: Notify team
        run: |
          curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
            -d "{\"text\": \"🚀 Rollout increased to ${{ github.event.inputs.percentage }}%\"}"
```

### 8.2. Rollback Criteria (Automatic)

**Trigger automatic rollback if ANY of these occur**:

| Metric | Threshold | Action |
|--------|-----------|--------|
| **Crash Rate** | >1% (100x baseline of 0.01%) | Immediate rollback |
| **ANR Rate** | >0.5% | Rollback within 1 hour |
| **1-Star Reviews** | >20% of new reviews | Review manually, rollback if confirmed |
| **User Complaints** | >50 reports of same issue | Investigate → rollback if critical |
| **Critical Bug** | Data loss, security vulnerability | Immediate rollback + hotfix |
| **Performance Regression** | App start time >3s (baseline 1.5s) | Rollback within 4 hours |

#### Firebase Crashlytics Alert Rules

```kotlin
// In Firebase Console → Crashlytics → Alerts
// Create alert: "Crash-free users drops below 99%"

{
  "alertName": "Critical Crash Rate",
  "condition": {
    "crashFreeUsersPercent": {
      "lessThan": 99.0,
      "duration": "1h"
    }
  },
  "actions": [
    {
      "type": "email",
      "recipients": ["dev@apemax.com"]
    },
    {
      "type": "webhook",
      "url": "https://api.github.com/repos/apemax/fastmediasorter/dispatches",
      "payload": {
        "event_type": "rollback_trigger",
        "client_payload": {
          "reason": "crash_rate_exceeded"
        }
      }
    }
  ]
}
```

#### Automated Rollback Script

```python
# scripts/rollback.py
import requests
import sys

PLAY_CONSOLE_API = "https://www.googleapis.com/androidpublisher/v3"
SERVICE_ACCOUNT_JSON = "secrets/play-console-service-account.json"
PACKAGE_NAME = "com.apemax.fastmediasorter"

def rollback_to_previous_version():
    """
    Halt current rollout and revert to previous stable version.
    """
    # Authenticate
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_JSON,
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    
    # Get current production track
    service = build('androidpublisher', 'v3', credentials=credentials)
    tracks = service.edits().tracks().list(
        packageName=PACKAGE_NAME,
        editId=edit_id
    ).execute()
    
    production_track = next(t for t in tracks['tracks'] if t['track'] == 'production')
    current_version = production_track['releases'][0]['versionCodes'][0]
    previous_version = production_track['releases'][1]['versionCodes'][0]
    
    # Halt current rollout
    service.edits().tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track='production',
        body={
            'releases': [{
                'versionCodes': [previous_version],
                'status': 'completed',
                'userFraction': 1.0
            }]
        }
    ).execute()
    
    print(f"✅ Rolled back from v{current_version} to v{previous_version}")
    
    # Commit edit
    service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id).execute()

if __name__ == '__main__':
    rollback_to_previous_version()
```

### 8.3. Manual Rollback Procedure

**When to manually rollback**:
- Critical security vulnerability discovered
- Major feature completely broken
- Data corruption reports
- Legal/compliance issue

**Steps**:

1. **Halt Rollout Immediately**:
   ```bash
   # In Play Console:
   Production → Manage → Halt rollout
   # OR via API:
   python scripts/rollback.py
   ```

2. **Notify Users**:
   ```kotlin
   // In-app message (if users already updated)
   FirebaseRemoteConfig.getInstance().setDefaults(mapOf(
       "force_downgrade_message" to "Critical issue detected. Please uninstall and reinstall previous version from website."
   ))
   ```

3. **Upload Previous Stable Version**:
   ```bash
   # Re-upload last known good AAB
   bundletool build-bundle \
     --modules=app_v2/release/app-release.aab \
     --output=rollback.aab
   ```

4. **Expedited Review Request**:
   - Contact Google Play support
   - Explain criticality
   - Request expedited review (usually 1-2 hours for emergencies)

5. **Post-Mortem**:
   - Document root cause
   - Update testing strategy
   - Add regression test

### 8.4. Emergency Hotfix Procedure

**For critical bugs that cannot wait for full release cycle**:

#### Hotfix Workflow

```bash
# 1. Create hotfix branch from last release tag
git checkout -b hotfix/v2.5.1 v2.5.0

# 2. Apply minimal fix
# Edit only necessary files, no refactoring

# 3. Test hotfix
./gradlew :app_v2:testDebugUnitTest
./gradlew :app_v2:connectedDebugAndroidTest

# 4. Bump patch version
# v2.5.0 → v2.5.1

# 5. Build signed release
./gradlew :app_v2:bundleRelease

# 6. Upload to Play Console (Production track)
# Set to 100% rollout immediately (skip staging if critical)

# 7. Merge hotfix back to main
git checkout main
git merge hotfix/v2.5.1
git push origin main

# 8. Tag hotfix
git tag -a v2.5.1 -m "Hotfix: Critical crash in file operations"
git push origin v2.5.1
```

#### Hotfix Validation Checklist

- [ ] Fix addresses ONLY the critical issue (no feature changes)
- [ ] Unit tests pass
- [ ] Manual smoke test completed
- [ ] ProGuard rules don't break fix
- [ ] Release notes clearly state "Emergency hotfix"
- [ ] Play Console description updated with fix details

#### Hotfix Examples

**Example 1: Data Loss Bug**
```kotlin
// BEFORE (v2.5.0): Bug caused file deletion on network error
catch (e: IOException) {
    deleteFile(file) // BUG: Deletes file on network error
}

// AFTER (v2.5.1): Hotfix preserves file
catch (e: IOException) {
    Timber.e(e, "Network error, file preserved")
    return Result.Error(e.message)
}
```

**Example 2: Crash on Startup**
```kotlin
// BEFORE (v2.5.0): NullPointerException on cold start
val resources = resourceDao.getAllResources().value!! // CRASH

// AFTER (v2.5.1): Null-safe
val resources = resourceDao.getAllResources().value ?: emptyList()
```

### 8.5. Rollback Communication Plan

#### Internal Notification (Immediate)

```markdown
**Subject**: 🚨 ROLLBACK INITIATED - FastMediaSorter v2.5.0

**Reason**: Crash rate exceeded 1% (baseline 0.01%)

**Action Taken**:
- Halted v2.5.0 rollout at 25%
- Reverted to v2.4.9 (stable)
- 75% of users unaffected

**Next Steps**:
1. Root cause analysis (assigned: @developer)
2. Regression test creation
3. Hotfix v2.5.1 planned for tomorrow

**Timeline**:
- Rollback completed: 2026-01-06 14:35 UTC
- Hotfix ETA: 2026-01-07 10:00 UTC
```

#### User Communication (If Necessary)

```markdown
**Play Store Update Description**:

We've temporarily paused the latest update while we address a stability issue. Most users are unaffected. If you experienced crashes, please reinstall the app. We apologize for the inconvenience and are working on a fix.

**In-App Message** (for affected users):
"An issue was detected with the latest version. We've rolled back to the previous stable release. Your data is safe. Update will be available soon."
```

---

## Reference Files

### Build Scripts
- **Version Script**: `dev/build-with-version.ps1`
- **ProGuard Rules**: `app_v2/proguard-rules.pro`
- **Signing Config**: `keystore.properties`

### Store Assets
- **Screenshots**: `store_assets/screenshots/`
- **Feature Graphic**: `store_assets/feature_graphic.png`
- **Descriptions**: `store_assets/description_*.txt`

### Documentation
- **Privacy Policy**: `PRIVACY_POLICY.html` (en/ru/uk)
- **Terms of Service**: `TERMS_OF_SERVICE.html` (en/ru/uk)

### Related Documents
- [30. Testing Strategy](30_testing_strategy.md) - Testing guidelines
- [31. Security Requirements](31_security_requirements.md) - Security checklist
- [32. Performance Metrics & Optimization](32_performance_metrics.md) - Performance targets
- [34. Localization & Internationalization](34_localization_guide.md) - Translation validation

---

**Document Version**: 1.0  
**Maintained By**: FastMediaSorter Development Team
