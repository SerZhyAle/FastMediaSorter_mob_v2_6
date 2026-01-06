# Epic 8: Release Engineering - Detailed TODO
*Derived from: [Tactical Plan: Epic 8](../00_strategy_epic8_release.md)*

## 1. Quality Assurance

### 1.1 Automated Testing
- [ ] Implement Unit Tests for ViewModels
- [ ] Implement Repository Tests (Mock Network)
- [ ] **Goal**: >80% Code Coverage on Domain layer

### 1.2 Manual Verification
- [ ] Execute `30_testing_strategy.md` checklist
- [ ] Test on Physical Devices (Low-end vs High-end)
- [ ] Test Network Edge Cases (Airplane mode, Flaky WiFi)

## 2. Production Hardening

### 2.1 ProGuard / R8
- [ ] Enable Minification (`minifyEnabled true`)
- [ ] Add rules for Reflection-heavy libs (Gson, Retrofit)
- [ ] Verify Release build does not crash

### 2.2 Signing & Keys
- [ ] Generate Upload Key
- [ ] Configure signing config in `build.gradle.kts` (load from env vars)

## 3. Documentation & Assets

### 3.1 User Manual
- [ ] Create internal "Help" screen or generic local HTML
- [ ] FAQ section

### 3.2 Store Assets
- [ ] Generate Screenshots (Phone, 7" Tablet, 10" Tablet)
- [ ] Create Feature Graphic
- [ ] Write Store Description (EN, RU, UK)
- [ ] Create Privacy Policy URL (GitHub Pages)
