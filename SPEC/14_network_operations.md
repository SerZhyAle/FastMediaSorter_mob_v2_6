# 14. Network Operations

## Protocols

### SMB (Server Message Block)
- **Library:** `smbj` (com.hierynomus:smbj) or `jcifs-ng`.
- **Discovery:** NetBIOS broadcasting on local subnet.
- **Authentication:** Username/Password/Domain.
- **Features:** Listing shares, file streaming, random access.

### SFTP (SSH File Transfer Protocol)
- **Library:** `jsch` or `sshj`.
- **Authentication:** Username/Password.
- **Port:** Default 22.

### FTP (File Transfer Protocol)
- **Library:** Apache Commons Net (`commons-net`).
- **Modes:** Passive / Active.
- **Port:** Default 21.

### Cloud Providers
- **Google Drive:** REST API v3. OAuth2.
- **OneDrive:** MS Graph API. OAuth2.
- **Dropbox:** Dropbox API v2. OAuth2.

## Handlers & Managers

### NetworkFileManager
**Package:** `com.sza.fastmediasorter.ui.player.helpers`
**Role:**
- Downloads remote files to local temp cache for editing/viewing.
- Uploads modified files back to source.
- Manages connection lifecycle (connect/disconnect).

### ConnectionThrottleManager
**Role:**
- Rate limits requests to avoid API bans (Google Drive).
- Manages concurrency for SMB operations.

### MediaScanner
**Role:**
- Recursive folder traversal.
- Extracts metadata (Size, Date, Dimensions).
- Updates Room database.
