# FastMediaSorter v2 - Master Development Specification

**Purpose**: Complete development guide and task specification for implementing FastMediaSorter v2 from scratch.

**Status**: Production-Ready Application (Build v2.25.1212+)  
**Last Updated**: January 9, 2026

---

## 📋 Active TODO List

**All remaining development tasks are consolidated in:** [TODO_CONSOLIDATED.md](TODO_CONSOLIDATED.md)

This single file contains:
- Current project status (85% complete)
- Active development priorities (Epic 8: Release Engineering)
- Remaining work for Epics 5, 6, 8
- Known issues and technical debt
- Timeline and next steps

---

## Quick Reference

### Project Type
**Production Android App** - Media management for local folders, network drives (SMB/SFTP/FTP), and cloud storage (Google Drive/OneDrive/Dropbox)

### Technology Stack
- **Language**: Kotlin 1.9.0
- **Architecture**: Clean Architecture (UI → Domain → Data), MVVM
- **UI**: Android View System (XML), Material Design 3
- **DI**: Hilt 2.50
- **Database**: Room 2.6.1 (version 1 schema - feature complete)
- **Media**: ExoPlayer (Media3 1.2.1)
- **Image Loading**: Glide 4.16.0
- **Network**: SMBJ 0.12.1, SSHJ 0.37.0, Apache Commons Net 3.10.0
- **Cloud**: Google Drive API, OneDrive MSAL, Dropbox API

### Key Capabilities
- File operations across 5 protocols (Local, SMB, SFTP, FTP, Cloud)
- Built-in media player (video/audio/images/GIF)
- Document viewer (TXT/PDF/EPUB) with OCR and translation
- Image editing (rotation, filters, adjustments)
- Pagination for 1000+ file collections
- Favorites system with cross-resource aggregation
- Undo/trash system with soft-delete

---

## Table of Contents

### 1. Introduction & Core Flow

- [00. Strategic Plan](00_strategy.md) - **Epical steps and vision for global dominance**
- [00. Project Rules & Manifesto](00_project_rules.md) - **Non-negotiable development rules, limits, and standards**
- [00. Overview & Architecture](00_overview.md) - Activity flow, launch modes, intent extras, state preservation
- [01. Onboarding Activities](01_onboarding_activities.md) - WelcomeActivity, permissions, first-run flow

### 2. Main Workflows

- [02. Main Activities](02_main_activities.md) - MainActivity, BrowseActivity (UI & Logic)
  - [Detailed Logic: MainActivity](detailed_logic/02a_main_activity_logic.md) - Complete behavior spec
  - [Detailed Logic: BrowseActivity](detailed_logic/02b_browse_activity_logic.md) - Browse behaviors and state management
  - [Detailed Logic: AddResourceActivity](detailed_logic/02c_add_resource_activity_logic.md) - Add/Copy/Preselect modes & flows
    - [SFTP/FTP Behaviors](detailed_logic/02c_sftp_ftp_behaviors.md) - Protocol switching, SSH keys, connection testing
    - [Cloud OAuth Behaviors](detailed_logic/02c_cloud_oauth_behaviors.md) - Google Drive/Dropbox/OneDrive authentication
    - [ViewModel Methods](detailed_logic/02c_viewmodel_behaviors.md) - Core data loading and scanning logic
    - [Error Handling & Security](detailed_logic/02c_error_logging_security.md) - Error presentation and security logging
  - [Detailed Logic: EditResourceActivity](detailed_logic/02d_edit_resource_activity_logic.md) - Resource editing logic
    - [Form Fields Details](detailed_logic/02d_form_fields_details.md) - Input validation and UI logic
    - [observeData Complete](detailed_logic/02d_observedata_complete.md) - UI state observation details
    - [ViewModel Complete](detailed_logic/02d_viewmodel_complete.md) - Core methods and credential management
- [03. Resource Management](03_resource_management.md) - Add/Edit resources (Local, SMB, Cloud), connection handling
  - [Reference: Missing Behaviors Analysis](MISSING_BEHAVIORS_ADD_EDIT_RESOURCE.md) - Catalog of implemented behaviors
- [04. Player Activity](04_player_activity.md) - Core media player/editor (Images, Video, Audio, Text, PDF)
  - [Detailed Logic: PlayerManager](detailed_logic/04a_player_logic.md) - Player logic decomposition

### 3. Settings & Configuration

- [05. Settings Activity](05_settings_activity.md) - All settings fragments (General, Media, Playback, Destinations)
  - [Detailed Logic: SettingsActivity](detailed_logic/05_settings_activity_logic.md)
- [06. Cloud Pickers](06_cloud_pickers.md) - Google Drive, OneDrive, Dropbox folder selection
- [07. Base Classes & Widget](07_base_and_widget.md) - BaseActivity, BaseFragment, Widget Configuration

### 4. UI Components & Resources

- [08. Dialogs](08_dialogs.md) - Catalog of all 20+ application dialogs
  - [Detailed Logic: Dialog Helpers](detailed_logic/05_dialogs_logic.md)
- [09. Adapters](09_adapters.md) - RecyclerView and ViewPager adapters
- [10. Custom Views](10_custom_views.md) - Specialized UI views (Overlays, Waveform, etc)
- [15. UI Guidelines](15_ui_guidelines.md) - Design system, colors, accessibility

### 5. Logic & Architecture

- [11. ViewModels](11_viewmodels.md) - Hilt ViewModels definitions
- [12. Managers](12_managers.md) - Helper classes extracted from PlayerActivity
- [13. Data Models](13_data_models.md) - Room Entities, MediaFile, Enums
- [14. Network Operations](14_network_operations.md) - SMB/SFTP/Cloud protocol handling details
- [16. Core Logic & Rules](16_core_logic_and_rules.md) - Business logic, algorithms, edge cases
  - [Sorting & Filtering](detailed_logic/core_rules/sorting_and_filtering.md)
  - [File Operations](detailed_logic/core_rules/file_operations.md)
  - [Platform Specifics](detailed_logic/core_rules/platform_specifics.md)
  - [Data Persistence](detailed_logic/core_rules/data_persistence.md)

### 6. Development & Implementation

- [17. Architecture Patterns](17_architecture_patterns.md) - Clean Architecture, MVVM, Strategy Pattern, UseCases
- [18. Development Workflows](18_development_workflows.md) - Build, testing, database migrations, task tracking
- [19. Refactoring History](19_refactoring_history.md) - Completed refactorings (Phase 1-4) with lessons learned
- [20. Protocol Implementations](20_protocol_implementations.md) - SMB, SFTP, FTP, Google Drive, Pagination code examples
- [21. Common Pitfalls](21_common_pitfalls.md) - 8 critical issues discovered during development with solutions
- [22. Cache Strategies](22_cache_strategies.md) - Glide, PDF, UnifiedFileCache management
- [23. Code Conventions](23_code_conventions.md) - Naming, style, logging, DI patterns
- [24. Dependencies Reference](24_dependencies.md) - Complete list of 80+ libraries with versions and best practices
- [25. Implementation Roadmap](25_implementation_roadmap.md) - Step-by-step development plan with task breakdown
- [26. Database Schema & Migrations](26_database_schema.md) - Complete DB specification, ER diagram, migrations
- [27. API Contracts & Interfaces](27_api_contracts.md) - Repository interfaces, DAOs, UseCase signatures
- [28. State Management Strategy](28_state_management.md) - Flow/StateFlow usage, ViewModel patterns, event handling
- [29. Error Handling Strategy](29_error_handling.md) - Exception hierarchy, Result types, retry policies
- [30. Testing Strategy](30_testing_strategy.md) - Testing Pyramid, coverage targets, CI/CD integration
- [31. Security Requirements](31_security_requirements.md) - Credential encryption, network security, permissions
- [32. Performance Metrics & Optimization](32_performance_metrics.md) - Targets, optimization techniques, profiling tools
- [33. Navigation Graph & Deep Links](33_navigation_graph.md) - Activity navigation, intent extras, deep links
- [34. Localization & Internationalization](34_localization_guide.md) - 3 languages (en/ru/uk), string resources, formatting
- [35. Release Checklist & Pre-Launch Validation](35_release_checklist.md) - Code quality, validation tests, deployment steps

---

## Visual Assets

### Diagrams Directory
**Location**: [diagrams/](diagrams/)

Reusable visual representations referenced throughout specification:
- [**database_er_diagram.md**](diagrams/database_er_diagram.md) - Room DB entity relationships
- [**navigation_flow.md**](diagrams/navigation_flow.md) - Activity navigation paths
- [**navigation_mermaid.md**](diagrams/navigation_mermaid.md) - Interactive Mermaid flowchart
- [**project_structure.md**](diagrams/project_structure.md) - Clean Architecture layers

**See**: [diagrams/README.md](diagrams/README.md) for usage instructions.

### Application Assets
**Location**: [assets/](assets/)

Graphical resources copied from application for development reference:
- **icons/**: PNG icons (cloud providers, touch zones, destination badges)
- **launcher/**: App launcher icons

**Complete catalog**: [assets/README.md](assets/README.md) - Icon catalog and guidelines.

---

## Critical Resources

### User Documentation
- [README.md](external_docs/README.md) - Project overview
- [QUICK_START.md](external_docs/QUICK_START.md) - First-time user guide
- [HOW_TO.md](external_docs/HOW_TO.md) - Detailed feature tutorials
- [FAQ.md](external_docs/FAQ.md) - Common questions
- [TROUBLESHOOTING.md](external_docs/TROUBLESHOOTING.md) - Problem-solving guide

### Developer Documentation
- [.github/copilot-instructions.md](external_docs/.github/copilot-instructions.md) - AI coding agent instructions
- [dev/TODO_V2.md](external_docs/dev/TODO_V2.md) - Task tracking
- [dev/build-with-version.ps1](external_docs/dev/build-with-version.ps1) - Build script
- [docs/16KB_PAGE_SIZE_FIX.md](external_docs/docs/16KB_PAGE_SIZE_FIX.md) - Android 15+ fix

### Strategic Plans
- [dev/TACTICAL_PHASE_*.md](external_docs/dev/) - Implementation phases
- [dev/DEVELOPMENT_ROADMAP_Q1_Q2_2026.md](external_docs/dev/DEVELOPMENT_ROADMAP_Q1_Q2_2026.md) - 2026 goals

---

## Project Structure Overview

**Full diagram**: [diagrams/project_structure.md](diagrams/project_structure.md)

**Summary**:
- **app_v2/**: Production codebase (UI → Domain → Data)
- **dev/**: Build scripts, TODO, strategic plans
- **docs/**: Technical documentation
- **spec_v2/**: This specification

---

## Key Architectural Decisions

### 1. Clean Architecture Layers
**Dependency Flow**: UI → Domain (UseCases) → Data (Repository interfaces)

### 2. Strategy Pattern for File Operations
**Solution**: Single `FileOperationStrategy` interface with 5 implementations
**Impact**: Automatic cross-protocol routing

### 3. PlayerActivity Decomposition
**Solution**: Extracted 7 focused managers from original God Class
**Impact**: Improved maintainability and build performance

### 4. UnifiedFileCache
**Solution**: Single shared cache with 24-hour expiration
**Impact**: Reduced network traffic and redundant downloads

---

## Version & Build Information

**Version Format**: `Y.YM.MDDH.Hmm` (e.g., `2.60.1071.422`)
**Build Command**: `.\build-with-version.ps1`
**Database Schema**: Room 2.6.1 (version 1 schema - feature complete)

---

## Getting Started with Development

### Prerequisites
1. Android Studio Hedgehog (2025.2.2)+
2. JDK 17+
3. Android SDK 34
4. Minimum test device: Android 9.0 (API 28)

### First Build
```powershell
# Clone repository
git clone https://github.com/SerZhyAle/FastMediaSorter_mob_v2.git
cd FastMediaSorter_mob_v2

# Build with auto-versioning
.\build-with-version.ps1
```

### Reading Order for New Developers
1. [00. Project Rules & Manifesto](00_project_rules.md)
2. [17. Architecture Patterns](17_architecture_patterns.md)
3. [18. Development Workflows](18_development_workflows.md)
4. [23. Code Conventions](23_code_conventions.md)
5. [21. Common Pitfalls](21_common_pitfalls.md)
6. [02. Main Activities](02_main_activities.md)

---

## Support & Contribution

### Reporting Issues
- **GitHub Issues**: [github.com/SerZhyAle/FastMediaSorter_mob_v2/issues](https://github.com/SerZhyAle/FastMediaSorter_mob_v2/issues)

### Contribution Guidelines
1. Read [00. Project Rules & Manifesto](00_project_rules.md)
2. Check [dev/TODO_V2.md](external_docs/dev/TODO_V2.md)
3. Create feature branch
4. Follow Clean Architecture
5. Add tests

---

## License

MIT License - See LICENSE file in repository root
