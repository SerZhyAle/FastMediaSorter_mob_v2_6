# 16a. Offline Mode & Sync Conflicts (Addition to 16)

**Last Updated**: January 6, 2026  
**Purpose**: Define offline behavior and conflict resolution for all resource types

---

## Offline Mode Policy

### Detection

```kotlin
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: StateFlow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()!!
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, isCurrentlyOnline())
}
```

### Offline Behavior by Resource Type

| Resource Type | Offline Capability | Strategy |
|---------------|-------------------|----------|
| **Local** | ✅ Full | No network required |
| **SMB/SFTP/FTP** | ❌ None | Show "Offline" banner, disable operations |
| **Cloud** | ⚠️ Partial | Show cached files, queue modifications |

---

## Conflict Detection

### File Versioning

```kotlin
@Entity(tableName = "file_versions")
data class FileVersionEntity(
    @PrimaryKey
    val filePath: String,
    val lastKnownModifiedTime: Long,
    val lastKnownSize: Long,
    val lastKnownETag: String?,  // For cloud providers
    val lastSyncedAt: Long
)

class ConflictDetector @Inject constructor(
    private val versionDao: FileVersionDao
) {
    
    suspend fun detectConflict(
        filePath: String,
        remoteModifiedTime: Long,
        remoteSize: Long
    ): ConflictType? {
        val localVersion = versionDao.getVersion(filePath) ?: return null
        
        return when {
            remoteModifiedTime > localVersion.lastKnownModifiedTime -> {
                ConflictType.RemoteNewer
            }
            remoteSize != localVersion.lastKnownSize -> {
                ConflictType.SizeChanged
            }
            else -> null
        }
    }
}

sealed class ConflictType {
    object RemoteNewer : ConflictType()
    object SizeChanged : ConflictType()
}
```

---

## Conflict Resolution UI

```kotlin
sealed class ConflictResolution {
    object KeepLocal : ConflictResolution()
    object KeepRemote : ConflictResolution()
    object KeepBoth : ConflictResolution()  // Rename local with timestamp
}

class ConflictDialog : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.conflict_detected_title)
            .setMessage(getString(R.string.conflict_detected_message, fileName))
            .setPositiveButton(R.string.keep_local) { _, _ ->
                viewModel.resolveConflict(ConflictResolution.KeepLocal)
            }
            .setNegativeButton(R.string.keep_remote) { _, _ ->
                viewModel.resolveConflict(ConflictResolution.KeepRemote)
            }
            .setNeutralButton(R.string.keep_both) { _, _ ->
                viewModel.resolveConflict(ConflictResolution.KeepBoth)
            }
            .create()
    }
}
```

---

## Operation Queueing (Offline)

```kotlin
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operation: OperationType,
    val sourcePath: String,
    val destinationPath: String?,
    val resourceId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

class OfflineOperationQueue @Inject constructor(
    private val pendingOpsDao: PendingOperationsDao,
    private val networkMonitor: NetworkMonitor
) {
    
    init {
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    processPendingOperations()
                }
            }
        }
    }
    
    suspend fun queueOperation(operation: PendingOperationEntity) {
        if (networkMonitor.isOnline.value) {
            executeImmediately(operation)
        } else {
            pendingOpsDao.insert(operation)
            Timber.d("Operation queued for offline: ${operation.operation}")
        }
    }
    
    private suspend fun processPendingOperations() {
        val pending = pendingOpsDao.getAllPending()
        Timber.d("Processing ${pending.size} pending operations")
        
        pending.forEach { op ->
            try {
                executeOperation(op)
                pendingOpsDao.delete(op)
            } catch (e: Exception) {
                pendingOpsDao.update(op.copy(retryCount = op.retryCount + 1))
            }
        }
    }
}
```

---

## References

- [16_core_logic_and_rules.md](16_core_logic_and_rules.md) - Main business logic
- [31a_api_rate_limiting.md](31a_api_rate_limiting.md) - Network resilience
