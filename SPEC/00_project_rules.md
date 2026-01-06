# 00. Project Rules & Manifesto

**Purpose**: This document establishes the non-negotiable "Constitution" of the project. All developers must adhere to these rules to maintain quality, scalability, and consistency.

---

## 1. Codebase Constraints

### File Size Limits
- **Recommended**: Keep Model files and Classes under **1000 lines**.
- **Reasoning**: Large files are hard to read, maintain, and test.
- **Action**: If a file exceeds this limit, refactor by extracting logic into:
  - Helper Classes / Managers
  - UseCases (Business Logic)
  - Extension Functions
  - Delegate Classes

### Language
- **Code & Comments**: STRICTLY **English**.
  - All variable names, function names, class names.
  - All inline comments and KDoc.
  - Git commit messages.
- **Reasoning**: Universal understanding and compatibility with AI tools.

### Standard Naming Conventions
- **Adherence**: Strictly follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Code Style](https://developer.android.com/kotlin/style-guide).
- **Patterns**:
  - Classes: `PascalCase` (e.g., `MediaRepository`).
  - Functions/Variables: `camelCase` (e.g., `getMediaFiles`, `isLoading`).
  - Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`).
  - Resource IDs: `snake_case` (e.g., `btn_submit`, `ic_arrow_back`).
  - Layout Files: `component_type_name.xml` (e.g., `activity_main.xml`, `item_media_file.xml`).

---

## 2. Localization & Internationalization

### Multi-Language Support
- **Requirement**: Interface and Documentation must support at least **3 languages**:
  1. **English (en)** - Base/Default
  2. **Russian (ru)**
  3. **Ukrainian (uk)**
- **Scalability**: The architecture must allow adding new languages (e.g., German, Spanish) without changing code logic. Use standard Android resource qualifiers (`values-de`, `values-es`).

### Implementation Rules
- **No Hardcoded Strings**: NEVER use string literals in UI code. Always use `R.string.*`.
- **Plurals**: Use `<plurals>` for quantity-dependent strings.
- **RTL Support**: Design layouts to be compatible with Right-to-Left languages (use `start`/`end` instead of `left`/`right`).

---

## 3. Design & Styling

### Universal Styles
- **Rule**: Use common styles and themes throughout the application.
- **Implementation**:
  - Define atomic styles in `styles.xml` / `themes.xml`.
  - Use Semantic Colors (`colorPrimary`, `colorOnSurface`, `colorError`) instead of hex codes.
  - Reintroduce components (Buttons, Cards, TextFields) via strict Style usage.
- **Goal**: Changing a theme attribute should update the entire app's look consistency.

### UI Density Principles
- **Maximize Screen Real Estate**:
  - **Rule**: Minimize whitespace between elements. The UI should be compact and information-dense.
  - **Avoid**: Excessive padding, large empty margins, or "airy" modern design trends that waste space.
  - **Goal**: Show as much relevant content as possible on a single screen without scrolling.
- **Compact Components**:
  - Prefer "Dense" variants of Material components.
  - Use smaller icon sizes and text scaling where appropriate to fit more data.

---

## 4. Architectural Recommendations (The "Antigravity" Standard)

### Strict Clean Architecture
- **Layer Separation**:
  - **UI (Presentation)**: View Models, Activities, Fragments. NO business logic.
  - **Domain**: UseCases, Models, Repository Interfaces. Pure Kotlin, NO Android dependencies.
  - **Data**: Repository Implementations, Room Entities, Network Clients, Data Sources.
- **Dependency Rule**: UI → Domain ← Data.

### Unidirectional Data Flow (UDF)
- **Pattern**: `ViewModel` holds `StateFlow`. Activity/Fragment observes `State`.
- **Flow**:
  1. UI sends **Event** (User Action) to ViewModel.
  2. ViewModel updates **State** (via UseCase results).
  3. UI observes State change and re-renders.
- **Benefit**: Predictable state management and easier debugging.

### No Hardcoded Resources
- **Dimens**: Use `dimens.xml` for all sizes (margins, paddings, text sizes).
- **Colors**: Use `colors.xml` and Theme Attributes.
- **Reasoning**: Consistent spacing and easy design system updates.

### Library Selection & Best Practices
- **Standard over Exotic**: Prefer standard Android Jetpack libraries AND widely adopted community standards (e.g., Retrofit, Glide, Hilt, Room) over niche or experimental solutions.
- **Popularity Metric**: Choose libraries with high GitHub stars, active maintenance, and strong community support.
- **Reasoning**: Ensures long-term maintainability, easier onboarding for new devs, reduced risk of abandonment, and better StackOverflow/AI support.

### Feature Slicing
- Group code by **Feature** first, not by Type.
  - *Good*: `ui/player/` contains Activity, ViewModel, Managers, Adapters for Player.
  - *Bad*: `adapters/` contains all adapters for the entire app.

### Offline-First
- The app must remain functional without an internet connection.
- Cache network responses (Images, File Metadata).
- Queue actions (like uploads) to run when connection is restored.

---

## 5. Workflow & Discipline

### Continuous Integration (Local)
- **Commit Rule**: Make a git commit **immediately** after every successful build/verification cycle.
  - *Pattern*: `Code -> Build -> Test (Green) -> Commit`.
  - *Goal*: Never lose working state. Small, frequent commits are better than massive chunks.

### Version Control Strategy (Single Developer)
- **Main Branch Only**: Work exclusively on `main` branch.
  - All development happens directly on `main`
  - No feature branches
  - No pull requests
  - Direct commit and push workflow
- **Reasoning**: 
  - Zero merge conflicts (solo developer)
  - No code review overhead
  - Maximum development velocity
  - Simplified mental model
- **Commit Discipline**:
  - Commit after every logical unit: feature implementation, bugfix, refactoring session
  - Small, focused commits with clear messages
  - Format: `type: description` (e.g., `feat: add SMB connection`, `fix: resolve FTP timeout`, `refactor: extract PlayerManager`)
- **Push Policy**: 
  - Push immediately after commit OR
  - Push at end of work session (minimum once per day)
- **Backup Strategy**: Use `.backups/` folder for experimental changes instead of Git branches

### Documentation First (Living Specifications)
- **Sync Rule**: Documentation is NOT an afterthought. It is part of the "Definition of Done".
- **Reflect Back Rule**: If you discover a need to change the design, add a new feature, or deviate from the plan during development, **you must update the specification files first (or concurrently)**.
- **Goal**: The `spec_v2` folder must always reflect the *actual* state of the project, not just the *initial* state.
- **Constraint**: Code and Documentation must never contradict each other.

### Development Journaling
- **Task Tracking**: Maintain a living status file (e.g., `dev/TODO_V2.md` or `task.md`) to track the defined plan.
  - *Requirement*: Mark items as In Progress `[/]` or Done `[x]` in real-time.
- **Change Logging**: Record the flow of changes and decisions in a dedicated journal (e.g., `docs/current.log` or generic `CHANGELOG.md`).
  - *Goal*: Context preservation for future maintainers (or your future self).

### Communication Protocol
- **Ambiguity Rule**: If a task is unclear or has multiple viable approaches: **STOP and ASK**.
- **Proposal Requirement**: When asking for clarification, always provide **Options**:
  - *Option A*: The quick/hacky way (and its risks).
  - *Option B*: The robust/slow way (and its costs).
  - *Recommendation*: "I recommend B because..."
- **Goal**: Empower the stakeholder to make informed decisions without diving into code.

---

## 6. Artifact Management & Safety

### Centralized Documentation
- **Rule**: All project documentation, specifications, and architecture diagrams must be collected in a **single dedicated subfolder** (currently `spec_v2/`).
- **Goal**: Maintain a Single Source of Truth. Avoid scattered `README.md` files or orphaned Google Docs.
- **Reference**: External developer docs should be copied into `spec_v2/external_docs/` for self-contained distribution.

### Build Log Retention
- **Rule**: All logs from build processes, compilation errors, and CI/CD runs must be stored in a dedicated logs folder (e.g., `docs/logs/builds/` or `.logs/`).
- **Standard**: Filenames should include timestamps: `build_YYYY-MM-DD_HH-mm.log`.
- **Reasoning**: Facilitates debugging regarding regression issues or environment changes over time.

### Development Backups
- **Rule**: Before any major manual refactoring or destructive operation, create a snapshot of the affected files in a special **backups subfolder** (e.g., `.backups/PreRefactor_Name/`).
- **Scope**: Source code, Resource files, Schema definitions.
- **Goal**: Provide an immediate "Undo" capability independent of Git commits, specifically for experimental changes.

### Debugging Protocol (`current.log`)
- **Rule**: `docs/current.log` is the designated interface for User-Developer debugging interactions.
- **Workflow**:
  1. User encounters specific behavior/error.
  2. User pastes the relevant logcat/stacktrace/debug info into `docs/current.log`.
  3. Developer analyzes `docs/current.log` as the primary source of truth for the issue.
- **Maintenance**: This file is transient and can be cleared after the issue is resolved, or archived if significant.

### Specification Package (`spec_v2`)
- **Role**: The `spec_v2` folder acts as the **primary storage** for all specification documents and resources required to start development.
- **Organization Rule**: Do not dump files in the root of `spec_v2`. Use dedicated subfolders for:
  - `diagrams/`: Visual flows and architecture metrics.
  - `assets/`: Icons, UI mocks, and graphical resources.
  - `external_docs/`: Copies of external dependencies/manuals for self-contained context.
  - `detailed_logic/`: Granular behavior specifications.
- **Reasoning**: Ensures that a new developer can pick up this single folder and have a structured, uncluttered view of the entire project scope.
