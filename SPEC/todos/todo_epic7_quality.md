# Epic 7: Polish & User Experience - Detailed TODO
*Derived from: [Tactical Plan: Epic 7](../00_strategy_epic7_quality.md)*

## 1. Settings Ecosystem

### 1.1 Settings Screen
- [ ] Implement `SettingsActivity` using `PreferenceFragmentCompat`
- [ ] Define XML Preference hierarchies
- [ ] Sections: General, Player, Network, Cache, About
- [ ] **Universal Access (Epic 7 Update)**: Ensure "Work with all files" switch is present

### 1.2 Preference DataStore
- [ ] Migrate any SharedPreferences to DataStore (Proto or Preferences)
- [ ] Ensure instantaneous UI updates on change

## 2. Localization & Accessibility

### 2.1 Language Support
- [ ] Extract all hardcoded strings
- [ ] Create `values-ru/strings.xml` (Russian)
- [ ] Create `values-uk/strings.xml` (Ukrainian)
- [ ] **Validate**: Switch system language and verify app updates

### 2.2 Accessibility Compliance
- [ ] Audit Content Descriptions on all ImageButtons
- [ ] Verify Touch Target sizes (min 48dp)
- [ ] Verify Color Contrast (Material Theme Builder)

## 3. Stress Testing & Optimization

### 3.1 Performance Tuning
- [ ] Profile Memory Usage (LeakCanary)
- [ ] Optimize RecyclerView scrolling (Pre-fetching)
- [ ] **SLA**: Verify Cold Start < 500ms
- [ ] **SLA**: Verify Scroll FPS is stable 60fps
- [ ] **SLA**: Verify Network Timeout < 10s

### 3.2 Large Dataset Test
- [ ] Run "Golden Set" test (1000+ files)
- [ ] Verify no ANRs (Application Not Responding)
- [ ] Monitor Battery usage during sync
