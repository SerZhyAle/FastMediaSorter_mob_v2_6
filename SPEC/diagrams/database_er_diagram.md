# Database Entity-Relationship Diagram

**Room DB Version**: 6  
**Last Updated**: January 6, 2026

```
┌─────────────────────────┐
│   resources             │◄──────┐
├─────────────────────────┤       │
│ PK id                   │       │
│    name                 │       │ 1:N (Optional)
│    path                 │       │
│    type                 │       │
│    credentialsId (FK)   │───────┼──────┐
│    cloudProvider        │       │      │
│    cloudFolderId        │       │      │
│    supportedMediaTypes  │       │      │
│    sortMode             │       │      │
│    displayMode          │       │      │
│    isDestination        │       │      │
│    destinationOrder     │       │      │
│    fileCount            │       │      │
│    ...                  │       │      │
└─────────────────────────┘       │      │
                                  │      │
┌─────────────────────────┐       │      │
│ network_credentials     │◄──────┘      │
├─────────────────────────┤              │
│ PK id                   │              │
│    credentialId (UUID)  │              │
│    type (SMB/SFTP)      │              │
│    server               │              │
│    port                 │              │
│    username             │              │
│    password (encrypted) │              │
│    domain               │              │
│    shareName            │              │
│    sshPrivateKey        │              │
└─────────────────────────┘              │
                                         │
┌─────────────────────────┐              │
│   favorites             │              │
├─────────────────────────┤              │
│ PK id                   │              │
│    uri                  │              │
│    resourceId (FK)      │──────────────┘
│    displayName          │
│    mediaType            │
│    size                 │
│    dateModified         │
│    addedTimestamp       │
│    thumbnailPath        │
└─────────────────────────┘

┌─────────────────────────┐
│ playback_positions      │
├─────────────────────────┤
│ PK filePath             │
│    position (ms)        │
│    duration (ms)        │
│    lastPlayedAt         │
│    isCompleted          │
└─────────────────────────┘

┌─────────────────────────┐
│  thumbnail_cache        │
├─────────────────────────┤
│ PK filePath             │
│    localCachePath       │
│    lastAccessedAt       │
│    fileSize             │
└─────────────────────────┘

┌─────────────────────────┐
│  resources_fts          │ (FTS4 Virtual Table)
├─────────────────────────┤
│    name                 │
│    path                 │
└─────────────────────────┘
```

## Relationships

- **resources** ← **network_credentials**: 1:N optional (resources.credentialsId → network_credentials.id)
- **resources** → **favorites**: 1:N (favorites.resourceId → resources.id)
- **playback_positions**: Standalone (no foreign keys, indexed by filePath)
- **thumbnail_cache**: Standalone (no foreign keys, indexed by filePath)
- **resources_fts**: Virtual table (FTS4 for full-text search on resources.name + resources.path)
