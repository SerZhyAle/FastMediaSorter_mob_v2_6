# FastMediaSorter v2 - TODO Archive

**Last Updated**: January 9, 2026  
**Status**: CONSOLIDATED into `../TODO_CONSOLIDATED.md`

---

## ⚠️ IMPORTANT NOTICE

**All TODO items have been consolidated into a single file:**

📄 **[../TODO_CONSOLIDATED.md](../TODO_CONSOLIDATED.md)**

This folder now serves as an **archive** of completed epics and historical development tracking.

---

## Why Consolidation?

As the project reached 85% completion with Epics 1-4 and 7 complete, maintaining separate TODO files became redundant. The new consolidated file provides:

- ✅ Single source of truth for all remaining work
- ✅ Clear prioritization (Release > Cloud > Advanced)
- ✅ Active development focus (Epic 8: Release Engineering)
- ✅ Known issues and technical debt tracking
- ✅ Next steps with timeline

---

## Archive Contents

This folder previously contained:

### Completed Epics (Now Archived)
- ~~`todo_epic1_foundation_COMPLETE.md`~~ - Foundation complete (100%)
- ~~`todo_epic2_local_COMPLETE.md`~~ - Local file management complete (100%)
- ~~`todo_epic3_player_COMPLETE.md`~~ - Player activity complete (100%)
- ~~`todo_epic4_network_COMPLETE.md`~~ - Network protocols complete (100%)
- ~~`todo_epic5_cloud_COMPLETE.md`~~ - Cloud integration complete (25%)

### Individual Epic TODOs (Now Consolidated)
- ~~`todo_epic6_advanced.md`~~ - Merged into TODO_CONSOLIDATED.md
- ~~`todo_epic7_quality.md`~~ - Merged into TODO_CONSOLIDATED.md
- ~~`todo_epic8_release.md`~~ - Merged into TODO_CONSOLIDATED.md

---

## Current Development Status

**Active Epic:** Epic 8 - Release Engineering (66% complete)

**Next Priorities:**
1. Complete automated testing suite
2. Execute full manual QA checklist
3. Store preparation and final release

**Deferred to Post-Release:**
- Epic 5: Cloud Integration (Google Drive/OneDrive/Dropbox)
- Epic 6: Advanced Features (OCR/Translation polish)

---

## For Active Development

**👉 See:** [TODO_CONSOLIDATED.md](../TODO_CONSOLIDATED.md)

All active tasks, priorities, and next steps are now tracked in the consolidated file.

### 📋 Planned
- [ ] Epic 5: Cloud Integration (OAuth setup guides)
- [ ] Epic 6: Advanced Capabilities
- [ ] Epic 7: Polish & UX
- [ ] Epic 8: Release Engineering

---

## Key Improvements in COMPLETE Versions

### 1. **Complete Code Examples**
Вместо абстрактных инструкций типа "Create MainActivity", теперь есть полный рабочий код:
```kotlin
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val viewModel: MainViewModel by viewModels()
    // ... full implementation
}
```

### 2. **XML Layouts**
Все UI screens имеют готовые XML layouts с Material Design 3:
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <com.google.android.material.appbar.AppBarLayout>
        <!-- Toolbar with menu -->
    </com.google.android.material.appbar.AppBarLayout>
    <androidx.recyclerview.widget.RecyclerView/>
    <com.google.android.material.floatingactionbutton.FloatingActionButton/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 3. **Gradle Configuration**
Полная настройка сборки с Version Catalog (TOML):
```toml
[versions]
agp = "8.7.3"
kotlin = "1.9.24"
hilt = "2.50"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.12.0" }
# ... complete dependency catalog
```

### 4. **Resources (Dimensions, Themes, Localization)**
Готовые XML ресурсы:
- `dimens.xml` - Spacing, touch targets, compact design
- `themes.xml` - Material 3 theme
- `strings.xml` (EN/RU/UK) - 3 языка с Unicode

### 5. **Validation Criteria**
Каждая задача имеет проверяемые критерии:
- ✅ **Validate**: Project compiles and runs
- ✅ **Validate**: ViewBinding lifecycle managed properly
- ✅ **Validate**: Database compiles, Hilt generates code
- ✅ **Validate**: Change device language, verify strings update

### 6. **Estimated Time & Prerequisites**
Каждый Epic указывает:
- **Estimated Time**: 2-3 дня
- **Prerequisites**: Epic 1 completed
- **Output**: Compilable app с описанием функциональности

---

## How to Use These TODOs

### For New Project (From Scratch)
1. Start with `todo_epic1_foundation_COMPLETE.md`
2. Follow step-by-step, create each file/class
3. Validate after each section (checkboxes)
4. Move to Epic 2 after Epic 1 success criteria met

### For Existing FastMediaSorter Project
1. Use `../implementation_status.md` to see what's already done
2. Epics 1-2 are ~100% complete in `app_v2/`
3. Use these TODOs as reference for missing features (e.g., Epic 5: Cloud)
4. Cross-reference with actual code in `app_v2/`

### For Contributors
1. Read Epic overview (Purpose, Time, Prerequisites)
2. Check existing code in `app_v2/` (avoid duplication)
3. Follow code style from examples (Kotlin conventions, Clean Architecture)
4. All code must be in English (comments, variables, classes)
5. All UI strings must support EN/RU/UK

---

## Coverage Status

| Epic | Has COMPLETE Version | XML Layouts | Code Examples | Gradle Setup | OAuth Guides |
|------|---------------------|-------------|---------------|--------------|--------------|
| Epic 1 | ✅ Yes | ✅ Full | ✅ Full | ✅ Complete | N/A |
| Epic 2 | ✅ Yes | ✅ Full | ✅ Full | ✅ Complete | N/A |
| Epic 3 | ⏳ Partial | ❌ Missing | ⏳ Partial | ✅ Inherited | N/A |
| Epic 4 | ❌ No | ❌ Missing | ⏳ Partial | ✅ Inherited | N/A |
| Epic 5 | ❌ No | ❌ Missing | ❌ Missing | ✅ Inherited | ❌ Missing |
| Epic 6 | ❌ No | ❌ Missing | ❌ Missing | ✅ Inherited | N/A |
| Epic 7 | ❌ No | ⏳ Partial | ❌ Missing | ✅ Inherited | N/A |
| Epic 8 | ❌ No | N/A | ❌ Missing | ⏳ Partial | N/A |

**Overall Readiness**: 25% (2/8 Epics fully specified)

---

## Next Steps (Priority)

### High Priority (Блокеры для разработки)
1. **Epic 3 COMPLETE**: PlayerActivity layouts (activity_player.xml, video controls, image viewer)
2. **Epic 4 COMPLETE**: SMB/SFTP/FTP code examples с connection pooling
3. **Epic 5 COMPLETE**: OAuth setup guides (Google Cloud Console, Azure Portal, Dropbox)

### Medium Priority
4. **Epic 7 COMPLETE**: Settings layouts (PreferenceScreen XML)
5. **Epic 6 COMPLETE**: ML Kit OCR integration examples

### Low Priority
6. **Epic 8 COMPLETE**: ProGuard rules, Store assets generation

---

## Related Documentation

- [../implementation_status.md](../implementation_status.md) - Mapping Epics → Real codebase
- [../00_overview.md](../00_overview.md) - Activity flow, launch modes
- [../17_architecture_patterns.md](../17_architecture_patterns.md) - Clean Architecture guide
- [../24_dependencies.md](../24_dependencies.md) - Complete dependency reference
- [../26_database_schema.md](../26_database_schema.md) - Full database spec

---

## Feedback & Improvements

Если вы обнаружили:
- ❌ **Неточности в коде** - создайте issue с тегом `documentation`
- 💡 **Улучшения** - предложите PR с обновленными TODO
- 🐛 **Баги в примерах** - укажите файл и строку

**Цель**: Каждый Epic должен быть **production-ready specification**, достаточной для реализации без дополнительных вопросов.

---

**Last Updated**: January 6, 2026  
**Status**: 2/8 Epics fully specified, 6 in progress
