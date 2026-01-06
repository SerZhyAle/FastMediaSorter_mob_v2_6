# Architecture Decision Records (ADR)

**Version**: 1.0  
**Last Updated**: 2026-01-06  
**Purpose**: Document critical architectural decisions with context and rationale

---

## ADR Format

Each decision uses this template:
- **Status**: Accepted / Rejected / Superseded
- **Context**: Problem/requirement that triggered decision
- **Decision**: What was decided
- **Rationale**: Why this option was chosen
- **Consequences**: Trade-offs and implications
- **Alternatives Considered**: What other options were evaluated

---

## ADR-001: Use Clean Architecture with MVVM

**Status**: ✅ Accepted  
**Date**: 2024-11-15  
**Related Specs**: [01_architecture_overview.md](01_architecture_overview.md)

### Context
Need scalable architecture for complex app with multiple data sources (local, SMB, SFTP, FTP, cloud providers).

### Decision
Adopt Clean Architecture with three layers:
- **UI**: Activities/Fragments + ViewModels (MVVM)
- **Domain**: UseCases + domain models (business logic)
- **Data**: Repositories + Room + network clients

### Rationale
- **Separation of Concerns**: UI logic separate from business logic
- **Testability**: Domain layer has zero Android dependencies
- **Maintainability**: Clear dependency direction (UI → Domain → Data)
- **Scalability**: Easy to add new protocols/cloud providers

### Consequences
**Positive**:
- High testability (80% unit test coverage achievable)
- Easy to refactor individual layers
- Multiple developers can work in parallel

**Negative**:
- More boilerplate (UseCases for every operation)
- Steeper learning curve for junior developers

### Alternatives Considered
1. **MVC**: Too much logic in Activities, hard to test
2. **MVP**: Presenter tied to View lifecycle, memory leaks
3. **Jetpack Compose + MVI**: Too bleeding-edge for production app in 2024

---

## ADR-002: Use XML Layouts, Not Jetpack Compose

**Status**: ✅ Accepted  
**Date**: 2024-11-15  
**Related Specs**: [15_ui_guidelines.md](15_ui_guidelines.md)

### Context
Choose UI framework: XML + View Binding vs Jetpack Compose.

### Decision
Use traditional XML layouts with View Binding.

### Rationale
- **Stability**: XML is battle-tested, zero breaking changes
- **Performance**: View recycling in RecyclerView well-optimized
- **Tooling**: Layout Editor mature, drag-and-drop support
- **Developer Familiarity**: Larger talent pool knows XML
- **Interoperability**: Easy to integrate third-party XML-based libraries (PhotoView, ExoPlayer UI)

### Consequences
**Positive**:
- Rock-solid stability, no Compose version migration headaches
- Fast compilation times
- Better IDE performance (Layout Editor vs Compose preview)

**Negative**:
- More verbose (XML + Binding vs Compose DSL)
- No real-time preview during coding
- Miss out on Compose Material3 components

### Alternatives Considered
1. **Full Compose**: Too unstable for production (frequent breaking changes)
2. **Hybrid XML + Compose**: Increased complexity, two UI paradigms

---

## ADR-003: Use SMBJ 0.12.1 (Not 0.13+)

**Status**: ✅ Accepted  
**Date**: 2024-12-01  
**Related Specs**: [27_protocol_smb.md](27_protocol_smb.md)

### Context
SMBJ 0.13+ has native library dependencies (`libpenguin.so`) causing crashes on ARM devices.

### Decision
Pin SMBJ to version 0.12.1 with BouncyCastle 1.78.1 for crypto.

### Rationale
- **Stability**: 0.12.1 is pure Java, no native dependencies
- **Compatibility**: Works on all Android architectures (ARM, x86, ARM64)
- **Security**: BouncyCastle provides SMB3 encryption without native code

### Consequences
**Positive**:
- Zero crashes related to missing native libraries
- APK size smaller (no `.so` files)
- Easy to debug (pure Java stack traces)

**Negative**:
- Miss out on performance improvements in 0.13+
- Need to manually track security updates

### Alternatives Considered
1. **SMBJ 0.13+**: Native library crashes on ARM32 devices
2. **jCIFS-NG**: Less maintained, no SMB3 support

---

## ADR-004: Use Hilt for Dependency Injection

**Status**: ✅ Accepted  
**Date**: 2024-11-15  
**Related Specs**: [01_architecture_overview.md](01_architecture_overview.md)

### Context
Need DI framework for managing dependencies (ViewModels, Repositories, UseCases).

### Decision
Use Hilt (official Android DI library built on Dagger).

### Rationale
- **Android-First**: Built specifically for Android (Activity/Fragment injection)
- **ViewModel Support**: `@HiltViewModel` annotation for zero-boilerplate injection
- **Compile-Time Safety**: Dagger's compile-time validation catches errors early
- **WorkManager Integration**: Native support for injecting into Workers

### Consequences
**Positive**:
- No manual dependency graph setup
- Scopes (Singleton, Activity, ViewModel) built-in
- Easy to test (Hilt testing library)

**Negative**:
- Longer build times (annotation processing)
- Steep learning curve (Dagger concepts)

### Alternatives Considered
1. **Koin**: Runtime DI, slower and error-prone
2. **Manual DI**: Too much boilerplate, hard to maintain
3. **Dagger 2**: More complex than Hilt without Android-specific features

---

## ADR-005: Use Room for Local Database

**Status**: ✅ Accepted  
**Date**: 2024-11-15  
**Related Specs**: [02_data_model.md](02_data_model.md)

### Context
Need local persistence for resources, file metadata cache, favorites.

### Decision
Use Room Persistence Library (SQLite ORM).

### Rationale
- **Type Safety**: Compile-time SQL validation
- **Coroutines Support**: Suspend functions for async queries
- **Migration Support**: Schema versioning with automatic/manual migrations
- **Testing**: In-memory database for unit tests

### Consequences
**Positive**:
- No raw SQL strings, refactor-safe
- Automatic type converters (Enum → String)
- Flow/LiveData support for reactive queries

**Negative**:
- Build time overhead (annotation processing)
- Learning curve for complex queries

### Alternatives Considered
1. **Raw SQLite**: Too error-prone, no type safety
2. **Realm**: Abandoned by MongoDB, unclear future
3. **ObjectBox**: Less mature, smaller community

---

## ADR-006: Use ExoPlayer for Media Playback

**Status**: ✅ Accepted  
**Date**: 2024-11-20  
**Related Specs**: [13_media_playback_engine.md](13_media_playback_engine.md)

### Context
Need robust media player for video/audio from multiple sources (local, network, cloud).

### Decision
Use ExoPlayer (Media3 1.2.1) instead of MediaPlayer API.

### Rationale
- **Network Support**: Native HTTP/HTTPS streaming
- **Format Support**: Wider codec support than MediaPlayer
- **Customization**: Full control over UI, buffering, quality
- **Stability**: Better error handling and recovery

### Consequences
**Positive**:
- HLS/DASH streaming support (future-proof)
- Custom cache implementation possible
- Better performance on large files

**Negative**:
- Larger library size (~2MB)
- More complex API than MediaPlayer

### Alternatives Considered
1. **MediaPlayer**: Limited format support, poor network handling
2. **VLC Android**: Too heavy, GPL license issues

---

## ADR-007: Use Glide for Image Loading

**Status**: ✅ Accepted  
**Date**: 2024-11-20  
**Related Specs**: [15_ui_guidelines.md](15_ui_guidelines.md)

### Context
Need efficient image loading with caching for thumbnails and previews.

### Decision
Use Glide 4.16.0 with custom ModelLoaders for network protocols.

### Rationale
- **Disk Cache**: Persistent cache survives app restarts
- **Memory Management**: Automatic bitmap pooling, OOM prevention
- **Lifecycle Aware**: Cancels requests on Fragment/Activity destroy
- **Customization**: Easy to add custom data fetchers (SMB, SFTP, cloud URLs)

### Consequences
**Positive**:
- Fast scrolling in RecyclerView (no jank)
- Configurable cache size (default 2GB)
- Placeholder/error image support

**Negative**:
- Annotation processing overhead
- Learning curve for advanced features

### Alternatives Considered
1. **Picasso**: Simpler but less flexible, no disk cache control
2. **Coil**: Kotlin-first but less mature, smaller community
3. **Fresco**: Too complex, Facebook-specific optimizations

---

## ADR-008: Use Strategy Pattern for File Operations

**Status**: ✅ Accepted  
**Date**: 2024-12-15  
**Related Specs**: [20_file_operation_handlers.md](20_file_operation_handlers.md)

### Context
File operations (copy/move/delete) work differently across protocols (local, SMB, SFTP, FTP, cloud). Avoiding code duplication (local→SMB, local→SFTP, SMB→cloud, etc. = 36 combinations).

### Decision
Implement `FileOperationStrategy` interface with protocol-specific implementations:
- `LocalOperationStrategy`
- `SmbOperationStrategy`
- `SftpOperationStrategy`
- `FtpOperationStrategy`
- `CloudOperationStrategy`

### Rationale
- **Eliminate Duplication**: Single `copyFile()` method, strategy determines protocol
- **Extensibility**: Easy to add new protocols (WebDAV, S3)
- **Maintainability**: Bug fix in one strategy, not 36 places

### Consequences
**Positive**:
- ~3,286 lines of code removed
- Automatic cross-protocol routing
- Easier to test (mock strategy)

**Negative**:
- Slight performance overhead (interface dispatch)
- Abstract interface harder to understand for new developers

### Alternatives Considered
1. **Inheritance Hierarchy**: Too rigid, deep class hierarchies
2. **Conditional Logic**: Massive when/if chains, unmaintainable
3. **Visitor Pattern**: Over-engineered for this use case

---

## ADR-009: Use Soft Delete for File Operations

**Status**: ✅ Accepted  
**Date**: 2024-12-01  
**Related Specs**: [20_file_operation_handlers.md](20_file_operation_handlers.md)

### Context
Users need "undo" for file operations, especially delete/move.

### Decision
Implement soft delete: move files to `.trash/` folder instead of permanent deletion.

### Rationale
- **User Safety**: Accidental deletions recoverable
- **Cross-Protocol**: Works on local, SMB, SFTP, cloud
- **Simple Implementation**: Move operation instead of complex undo stack

### Consequences
**Positive**:
- Undo takes <1s (restore from trash)
- Works offline (queued restore)
- Consistent UX across all resource types

**Negative**:
- Storage overhead (trash folder takes space)
- Need periodic cleanup job (delete trash >7 days)

### Alternatives Considered
1. **No Undo**: Too risky, user complaints
2. **Complex Undo Stack**: Hard to implement for network operations
3. **Recycle Bin UI**: Too complex for MVP

---

## ADR-010: Use Git Main-Only Workflow

**Status**: ✅ Accepted  
**Date**: 2025-01-06  
**Related Specs**: [00_project_rules.md](../spec_v2/00_project_rules.md)

### Context
Single developer project, feature branches add overhead without benefit.

### Decision
Work directly on `main` branch:
- Commit frequently (multiple times per day)
- Push after each logical unit of work
- Use descriptive commit messages: `feat: add SMB connection pooling`

### Rationale
- **Simplicity**: No branch management overhead
- **No Merge Conflicts**: Single developer, no parallel work
- **Faster Iteration**: No PR review delay

### Consequences
**Positive**:
- Faster development velocity
- Simpler Git history (linear)
- No wasted time on branch cleanup

**Negative**:
- Can't easily isolate broken experiments (use `git stash` instead)
- Main branch occasionally unstable (mitigate with frequent commits)

### Alternatives Considered
1. **Feature Branches**: Overhead for solo developer
2. **Gitflow**: Way too complex for one person

---

## ADR-011: Use Paging3 for Large File Lists

**Status**: ✅ Accepted  
**Date**: 2024-12-01  
**Related Specs**: [03_screen_browse.md](03_screen_browse.md)

### Context
Folders with 10,000+ files cause OOM and slow UI.

### Decision
Use Paging3 library for lazy loading (100 items per page) when file count >1,000.

### Rationale
- **Memory Efficiency**: Only loads visible items + buffer
- **Smooth Scrolling**: No jank when scrolling through large lists
- **Network Optimization**: Doesn't fetch all 10k files at once

### Consequences
**Positive**:
- Supports infinite scroll
- Works with Room database and network sources
- Automatic placeholder/loading states

**Negative**:
- More complex than simple `RecyclerView.Adapter`
- Need separate `PagingMediaFileAdapter` class

### Alternatives Considered
1. **Manual Pagination**: Too much boilerplate
2. **Load All in Memory**: OOM on devices with <4GB RAM

---

## ADR-012: Use Firebase for Analytics & Crashlytics

**Status**: ✅ Accepted  
**Date**: 2025-01-06  
**Related Specs**: [36_analytics_strategy.md](36_analytics_strategy.md)

### Context
Need crash reporting and usage analytics for post-release monitoring.

### Decision
Use Firebase Analytics + Firebase Crashlytics (opt-in only).

### Rationale
- **Free Tier**: Sufficient for indie app
- **Integration**: Single SDK for both analytics and crashes
- **Privacy Compliant**: GDPR-ready with opt-in
- **Google Play Integration**: Automatic pre-launch reports

### Consequences
**Positive**:
- Real-time crash alerts
- Custom event tracking (file operations, connections)
- Performance monitoring built-in

**Negative**:
- Google dependency (vendor lock-in)
- Privacy concerns (mitigated by opt-in)

### Alternatives Considered
1. **Sentry**: Better crash analysis but expensive for solo dev
2. **AppCenter**: Microsoft deprecating, uncertain future
3. **Self-Hosted**: Too much infrastructure overhead

---

## ADR-013: Use EncryptedSharedPreferences for Credentials

**Status**: ✅ Accepted  
**Date**: 2024-12-01  
**Related Specs**: [27_protocol_smb.md](27_protocol_smb.md)

### Context
Need to store SMB/SFTP/FTP passwords and OAuth tokens securely.

### Decision
Use Android Jetpack Security library (`EncryptedSharedPreferences`).

### Rationale
- **Encryption**: AES-256 with hardware-backed keys (Android Keystore)
- **Easy to Use**: Drop-in replacement for `SharedPreferences`
- **Biometric Support**: Can require fingerprint for access (future)

### Consequences
**Positive**:
- Credentials encrypted at rest
- No plaintext passwords in memory dumps
- Transparent encryption/decryption

**Negative**:
- Slight performance overhead (<10ms per read)
- Data lost if user factory resets device (by design)

### Alternatives Considered
1. **Plaintext SharedPreferences**: Insecure, data leakage risk
2. **SQL Cipher**: Overkill for key-value storage
3. **Custom Encryption**: Error-prone, don't roll your own crypto

---

## ADR-014: Use WorkManager for Background Tasks

**Status**: ✅ Accepted  
**Date**: 2024-12-01  
**Related Specs**: [32_performance_metrics.md](32_performance_metrics.md)

### Context
Need to run background tasks (file sync, thumbnail generation, cache cleanup) even when app closed.

### Decision
Use WorkManager for all deferred/periodic background work.

### Rationale
- **Battery Friendly**: Respects Doze mode, batches work
- **Guaranteed Execution**: Survives app/device restarts
- **Constraints**: Only run on WiFi, when charging, etc.
- **Retry Logic**: Exponential backoff built-in

### Consequences
**Positive**:
- OS-friendly (no battery drain complaints)
- Works on all Android versions (API 14+)
- Hilt integration for dependency injection

**Negative**:
- No precise timing (OS decides when to run)
- Can be delayed up to 15 minutes in Doze

### Alternatives Considered
1. **JobScheduler**: WorkManager is built on top of it (better API)
2. **AlarmManager**: Deprecated for background work, battery drain
3. **Foreground Service**: Only for user-initiated long tasks (file uploads)

---

## ADR-015: Use Circuit Breaker for Network Resilience

**Status**: ✅ Accepted  
**Date**: 2025-01-06  
**Related Specs**: [14a_network_resilience.md](14a_network_resilience.md)

### Context
Repeated network failures (e.g., SMB server down) spam user with errors and waste battery.

### Decision
Implement Circuit Breaker pattern:
- **CLOSED**: Normal operation
- **OPEN**: After 5 failures, reject requests for 30s
- **HALF_OPEN**: Test recovery with 1 request after timeout

### Rationale
- **Fail Fast**: Don't waste time retrying dead connections
- **Battery Savings**: No continuous retry loops
- **Better UX**: Single "Service unavailable" message, not 100 errors

### Consequences
**Positive**:
- Faster failure detection (<1s vs 30s timeout per retry)
- Automatic recovery detection
- Per-resource circuit breaker (SMB server down doesn't affect SFTP)

**Negative**:
- Adds complexity (state machine)
- Potential false positives (transient failures trip circuit)

### Alternatives Considered
1. **Exponential Backoff Only**: Doesn't stop repeated failures
2. **Manual Retry Button**: Poor UX, requires user intervention

---

## Summary

**Total Decisions**: 15  
**All Accepted**: ✅  
**Critical Decisions**: ADR-001 (Architecture), ADR-003 (SMBJ version), ADR-008 (Strategy pattern)

**Maintenance**:
- Add new ADR when making significant architectural choices
- Update status if decision superseded (e.g., if switch from XML to Compose)
- Review ADRs during Epic milestones to validate assumptions
