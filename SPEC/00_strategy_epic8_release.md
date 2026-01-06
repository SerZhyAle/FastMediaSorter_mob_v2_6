# Tactical Plan: Epic 8 - Release Engineering

**Goal**: Prepare for public distribution.
**Deliverable**: Signed AAB, Store Assets, Documentation.
**Prerequisite**: Epic 7 Complete.

---

## 1. Quality Assurance

### 1.1 Automated Testing
- **Action**: Execute Test Suite.
- **Unit**: Run all JUnit tests (Domain/Data).
- **UI**: Run Espresso tests for critical flows.

### 1.2 Stress Testing
- **Action**: Performance verification.
- **Scenario**:
  - Load folder with 10,000 items.
  - Connect to slow SMB share.
  - Rapidly switch files in viewer.

### 1.3 Memory Profiling
- **Action**: Leak detection.
- **Tool**: LeakCanary (Debug build).
- **Target**: Fix Activity/Fragment leaks.

---

## 2. Production Hardening

### 2.1 Obfuscation (R8)
- **Action**: Configure ProGuard rules.
- **Files**: `proguard-rules.pro`.
- **Keep**: Serialization classes (`@SerializedName`), JNI callbacks.
- **Verify**: Release build runs without crashing.

### 2.2 Signing
- **Action**: Generate Signing Keys.
- **Store**: Securely store KeyStore file and passwords (1Password/LastPass).
- **Gradle**: Configure `signingConfigs` for release build type.

---

## 3. Documentation & Assets

### 3.1 User Docs
- **Action**: Finalize Markdown docs.
- **Files**: `README.md`, `QUICK_START.md`, `TROUBLESHOOTING.md` (add solutions for common connection errors).

### 3.2 Store Assets
- **Action**: Generate Visuals.
- **Items**:
  - App Icon (Adaptive).
  - Feature Graphic (1024x500).
  - Screenshots (Phone, 7" Tablet, 10" Tablet).
  - Description (EN, RU, UK).

### 3.3 Legal
- **Action**: Generate Policy Docs.
- **Files**: Privacy Policy URL (hosted page), Terms of Service.
