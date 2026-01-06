package com.sza.fastmediasorter.utils

import com.bumptech.glide.load.DataSource
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Diagnostic utility for tracking Glide disk cache hits/misses.
 * Helps debug cache persistence issues between app launches.
 * 
 * Usage: Call recordLoad() in Glide RequestListener.onResourceReady()
 * Call logStats() periodically or when leaving browse screen.
 */
object GlideCacheStats {
    
    private val diskCacheHits = AtomicInteger(0)
    private val memoryCacheHits = AtomicInteger(0)
    private val networkLoads = AtomicInteger(0)
    private val localLoads = AtomicInteger(0)
    
    /**
     * Record a successful Glide load and its source.
     * @param dataSource The source from Glide's onResourceReady callback
     */
    fun recordLoad(dataSource: DataSource) {
        when (dataSource) {
            DataSource.RESOURCE_DISK_CACHE,
            DataSource.DATA_DISK_CACHE -> diskCacheHits.incrementAndGet()
            DataSource.MEMORY_CACHE -> memoryCacheHits.incrementAndGet()
            DataSource.REMOTE -> networkLoads.incrementAndGet()
            DataSource.LOCAL -> localLoads.incrementAndGet()
        }
    }
    
    /**
     * Reset all counters (call when entering browse screen).
     */
    fun reset() {
        diskCacheHits.set(0)
        memoryCacheHits.set(0)
        networkLoads.set(0)
        localLoads.set(0)
    }
    
    /**
     * Log current statistics. Call when leaving browse screen or periodically.
     */
    fun logStats() {
        val disk = diskCacheHits.get()
        val memory = memoryCacheHits.get()
        val network = networkLoads.get()
        val local = localLoads.get()
        val total = disk + memory + network + local
        
        if (total == 0) {
            Timber.i("=== GLIDE CACHE STATS: No loads recorded ===")
            return
        }
        
        val diskPercent = (disk * 100.0 / total)
        val memoryPercent = (memory * 100.0 / total)
        
        Timber.i("=== GLIDE CACHE STATS ===")
        Timber.i("Total loads: $total")
        Timber.i("Disk cache hits: $disk (%.1f%%)".format(diskPercent))
        Timber.i("Memory cache hits: $memory (%.1f%%)".format(memoryPercent))
        Timber.i("Network/remote loads: $network")
        Timber.i("Local file loads: $local")
        
        if (disk == 0 && total > 10) {
            Timber.w("WARNING: Zero disk cache hits! Cache may not be persisting between sessions.")
        }
        Timber.i("=========================")
    }
    
    /**
     * Get summary string for quick display (e.g., in debug overlay).
     */
    fun getSummary(): String {
        val disk = diskCacheHits.get()
        val memory = memoryCacheHits.get()
        val other = networkLoads.get() + localLoads.get()
        return "Cache: D=$disk M=$memory O=$other"
    }
}
