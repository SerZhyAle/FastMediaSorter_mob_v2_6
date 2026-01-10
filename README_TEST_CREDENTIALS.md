# Network Test Credentials Setup

## Overview

This guide explains how to configure network credentials for SMB, SFTP, and FTP integration tests.

---

## Files

- **`test_network_creds.json.template`** - Template file (committed to git)
- **`test_network_creds.json`** - Your actual credentials (gitignored, not committed)

---

## Setup Instructions

### 1. Create Your Credentials File

```powershell
# Copy template to create your credentials file
Copy-Item test_network_creds.json.template test_network_creds.json
```

### 2. Edit Credentials

Open `test_network_creds.json` and fill in your test server details:

#### SMB Configuration
```json
"smb": {
  "enabled": true,              // Set to true to run SMB tests
  "host": "192.168.1.100",      // Your SMB server IP
  "port": 445,                  // Default SMB port
  "shareName": "TestShare",     // Share name (without \\)
  "workgroup": "WORKGROUP",     // Windows workgroup
  "domain": "",                 // Domain (leave empty for workgroup)
  "username": "testuser",       // SMB username
  "password": "testpassword",   // SMB password
  "testFolder": "/test_media",  // Folder with test files
  "anonymousAccess": false,     // True for guest access
  "smbVersion": "SMB2"          // SMB1, SMB2, or SMB3
}
```

#### SFTP Configuration
```json
"sftp": {
  "enabled": true,                          // Set to true to run SFTP tests
  "host": "192.168.1.101",                  // Your SFTP server IP
  "port": 22,                               // SSH port (usually 22)
  "username": "sftpuser",                   // SSH username
  "password": "sftppassword",               // SSH password (if using password auth)
  "privateKeyPath": "/path/to/id_rsa",     // SSH key path (if using key auth)
  "privateKeyPassphrase": "",               // Key passphrase (if key is encrypted)
  "testFolder": "/home/sftpuser/test_media", // Folder with test files
  "authType": "password"                    // "password" or "key"
}
```

#### FTP Configuration
```json
"ftp": {
  "enabled": true,              // Set to true to run FTP tests
  "host": "192.168.1.102",      // Your FTP server IP
  "port": 21,                   // FTP port (21 for plain, 990 for implicit TLS)
  "username": "ftpuser",        // FTP username
  "password": "ftppassword",    // FTP password
  "testFolder": "/test_media",  // Folder with test files
  "usePassiveMode": true,       // Use PASV mode (recommended)
  "useTLS": false,              // Enable FTPS
  "tlsMode": "explicit",        // "explicit" (FTPES) or "implicit" (FTPS)
  "anonymousAccess": false      // True for anonymous FTP
}
```

### 3. Prepare Test Files

On each test server, create a `test_media` folder with these files:
- **test_small.txt** - Small text file (~1KB)
- **test_medium.jpg** - Medium image (~100KB)
- **test_large.mp4** - Large video (~10MB)

---

## Usage in Tests

### Load Credentials in Test

```kotlin
import com.google.gson.Gson
import java.io.File

class SmbOperationStrategyTest {
    
    private data class NetworkTestConfig(
        val smb: SmbConfig,
        val sftp: SftpConfig,
        val ftp: FtpConfig,
        val settings: Settings
    )
    
    private data class SmbConfig(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val shareName: String,
        val username: String,
        val password: String,
        val testFolder: String
    )
    
    private data class Settings(
        val skipRealNetworkTests: Boolean
    )
    
    private val testConfig: NetworkTestConfig by lazy {
        val configFile = File("test_network_creds.json")
        if (!configFile.exists()) {
            throw IllegalStateException("test_network_creds.json not found. Copy from template.")
        }
        Gson().fromJson(configFile.readText(), NetworkTestConfig::class.java)
    }
    
    @Test
    fun `test SMB connection with real server`() {
        // Skip if real network tests disabled
        assumeTrue(testConfig.smb.enabled && !testConfig.settings.skipRealNetworkTests)
        
        // Use credentials from config
        val result = smbStrategy.connect(
            host = testConfig.smb.host,
            port = testConfig.smb.port,
            username = testConfig.smb.username,
            password = testConfig.smb.password
        )
        
        assertTrue(result.isSuccess)
    }
}
```

### Mock Network Tests (Default)

Most tests should **mock** network calls and not require real servers:

```kotlin
@Test
fun `test SMB connection with mock`() {
    // Mock the SMBJ client
    val mockClient = mock<SmbClient>()
    whenever(mockClient.connect(any())).thenReturn(mock())
    
    // Test the strategy logic without real network
    val result = smbStrategy.listFiles("/test")
    
    verify(mockClient).connect(any())
}
```

---

## Git Configuration

Ensure `test_network_creds.json` is in `.gitignore`:

```gitignore
# Test credentials (never commit!)
test_network_creds.json

# But keep the template
!test_network_creds.json.template
```

---

## Security Notes

⚠️ **NEVER commit `test_network_creds.json` to git!**

- Use test accounts with limited permissions
- Don't use production credentials
- Rotate test passwords regularly
- Keep test servers isolated from production

---

## Testing Modes

### Mode 1: Mock Tests (Default)
- `skipRealNetworkTests: true`
- All network calls are mocked
- Fast, no server needed
- Run in CI/CD

### Mode 2: Real Server Tests
- `skipRealNetworkTests: false`
- Actual network connections
- Requires test servers
- Run manually before release

---

## CI/CD Integration

For GitHub Actions / CI pipelines:

```yaml
# .github/workflows/test.yml
- name: Create test credentials
  run: |
    echo '{"smb":{"enabled":false},"sftp":{"enabled":false},"ftp":{"enabled":false},"settings":{"skipRealNetworkTests":true}}' > test_network_creds.json

- name: Run tests
  run: ./gradlew test
```

---

## Troubleshooting

### "test_network_creds.json not found"
→ Copy from template: `Copy-Item test_network_creds.json.template test_network_creds.json`

### "Connection refused"
→ Check `host` and `port` values
→ Ensure firewall allows connections
→ Verify server is running

### "Authentication failed"
→ Check `username` and `password`
→ For SFTP: verify `authType` matches your setup
→ For SMB: check `workgroup` vs `domain`

### Tests are slow
→ Set `skipRealNetworkTests: true` to use mocks
→ Only enable real tests when needed

---

**See Also:**
- [DEV_TODO.md](SPEC/DEV_TODO.md) - Development task list
- [TESTING_TODO.md](SPEC/TESTING_TODO.md) - QA testing checklist
