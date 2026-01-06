# File Operations Logic

## 1. Safety & Permissions

### Read-Only Resources
Resources marked with `isReadOnly = true` MUST NOT allow any modification operations.
- **Blocked Operations**: Delete, Move (from), Rename, Edit (save).
- **Allowed Operations**: Copy (from), View, Share.
- **UI Enforcement**:
  - Delete/Move/Rename buttons hidden or disabled in BrowseActivity.
  - "ReadOnly" badge shown in Resource list.
  - Context menus filter out destructive actions.

### Write Protection
Before any write operation, the system must verify:
1. Resource `isWritable` flag.
2. Resource `isReadOnly` flag (overrides `isWritable`).
3. Underlying file system permissions (SAF/Network).

## 2. Conflict Resolution

Strategies for handling file name collisions during Copy/Move operations:
- **Smart Rename**: Auto-append counter (e.g., `file (1).jpg`) to avoid overwrite.
- **Skip**: Ignore existing files.
- **Overwrite**: Replace existing files (requires explicit confirmation).

## 3. Deletion Rules

- **Soft Delete (Trash)**:
  - Default behavior.
  - Files moved to `.trash` hidden folder within the same resource.
  - Preserves hierarchy if possible or flats structure.
  - **Undo**: Possible for 5 minutes (via Snackbar/Session).

- **Permanent Delete**:
  - Triggered if `.trash` is unavailable or user explicitly requests "Delete Permanently".
  - Irreversible.
