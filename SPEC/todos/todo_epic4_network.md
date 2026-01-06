# Epic 4: Network Protocols - Detailed TODO
*Derived from: [Tactical Plan: Epic 4](../00_strategy_epic4_network.md)*

## 1. Security & Credentials

### 1.1 Secure Storage
- [ ] Implement `NetworkCredentialsRepository`
- [ ] Use `EncryptedSharedPreferences` (Jetpack Security)
- [ ] Store: Host, Port, Username, Password (Encrypted), SSH Key Path

## 2. SMB Implementation (Samba)

### 2.1 SMB Client
- [ ] Integrate `SMBJ` library
- [ ] Create `SmbClient` wrapper
- [ ] Implement Connection Pooling (reuse sessions)
- [ ] Implement `checkConnection()`

### 2.2 SMB Scanner
- [ ] Create `SmbMediaScanner`
- [ ] Implement recursive listing (with depth limit)
- [ ] **Universal Access**: Bypass filter if `allFiles` enabled

### 2.3 SMB Operations
- [ ] Implement `SmbOperationStrategy`
- [ ] Streaming: Implement `SmbInputStream` for ExoPlayer/Glide

## 3. SFTP Implementation (SSH)

### 3.1 SFTP Client
- [ ] Integrate `SSHJ` library
- [ ] Implement Password Auth
- [ ] Implement Key Auth (load private key)
- [ ] Handle `KnownHosts` (accept all or verify)

### 3.2 SFTP Operations
- [ ] Create `SftpMediaScanner`
- [ ] Create `SftpOperationStrategy`

## 4. FTP Implementation

### 4.1 FTP Client
- [ ] Integrate `commons-net` (Apache)
- [ ] Implement Active/Passive mode toggle
- [ ] Handle encoding (UTF-8 vs CP1251 legacy servers)

## 5. Unified Network Cache

### 5.1 Remote Edit Pipeline
- [ ] Implement "Download-Edit-Upload" flow
- [ ] Create `UnifiedFileCache` manager
- [ ] Logic:
  1. Download remote file to local temp
  2. Open in Editor
  3. On Save -> Upload back to remote
  4. **Conflict**: Last Write Wins (for Single User)

### 5.2 Cache Eviction
- [ ] Implement LRU eviction policy (e.g., max 500MB)
- [ ] Clear cache on app exit (optional)
