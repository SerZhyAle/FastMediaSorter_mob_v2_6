# Navigation Flow Diagram

**Last Updated**: January 6, 2026  
**Architecture**: Activity-based navigation (6 Activities)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MainActivity                                │
│  - Resource list (RecyclerView)                                    │
│  - FAB: Add Resource                                               │
│  - Overflow: Settings, Info                                        │
└─────────┬───────────────────────────────────────────────┬──────────┘
          │                                               │
          │ Click resource                               │ Overflow menu
          ↓                                               ↓
┌─────────────────────────┐                   ┌──────────────────────┐
│    BrowseActivity       │                   │  SettingsActivity    │
│  - File list            │                   │  - Preferences       │
│  - Sort/Filter          │                   │  - Cache management  │
│  - Select mode          │                   │  - About             │
└────┬──────────┬─────────┘                   └──────────────────────┘
     │          │
     │          │ Long-press: Edit Resource
     │          ↓
     │ ┌──────────────────────────┐
     │ │  EditResourceActivity    │
     │ │  - Name, Path, Type      │
     │ │  - Credentials (if SMB)  │
     │ │  - Test Connection       │
     │ └──────────────────────────┘
     │
     │ Click file
     ↓
┌─────────────────────────────┐
│     PlayerActivity          │
│  - Image/Video viewer       │
│  - Swipe left/right         │
│  - Actions: Copy/Move/Del   │
│  - Overflow: Edit, Info     │
└─────────────────────────────┘
          │
          │ Overflow: Info
          ↓
┌──────────────────────────┐
│    InfoActivity          │
│  - File details (Exif)   │
│  - Size, Date, Location  │
└──────────────────────────┘
```

## Navigation Paths

**Main Flow**:
```
MainActivity → BrowseActivity → PlayerActivity → InfoActivity
     ↓              ↓                ↓
SettingsActivity   EditResourceActivity   (Back stack)
```

**Deep Link Paths**:
- `fastmediasorter://open` → MainActivity
- `fastmediasorter://browse?resourceId=42` → BrowseActivity
- `fastmediasorter://player?resourceId=42&fileIndex=5` → PlayerActivity
