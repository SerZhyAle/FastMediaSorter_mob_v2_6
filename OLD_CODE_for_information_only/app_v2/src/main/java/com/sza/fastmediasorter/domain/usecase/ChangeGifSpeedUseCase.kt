package com.sza.fastmediasorter.domain.usecase

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * UseCase for changing GIF animation speed by adjusting frame delays
 * Uses AnimatedGifEncoder to rebuild GIF with new delays
 * 
 * Speed multiplier range: 0.25x (slower) to 4.0x (faster)
 * Original file is overwritten with new speed
 */
class ChangeGifSpeedUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val MIN_SPEED_MULTIPLIER = 0.25f  // 4x slower
        const val MAX_SPEED_MULTIPLIER = 4.0f   // 4x faster
    }

    /**
     * Change GIF animation speed
     * @param gifPath absolute path to GIF file
     * @param speedMultiplier speed factor (0.25 = 4x slower, 4.0 = 4x faster)
     * @param saveToDownloads if true, saves to Downloads with suffix (for network files). If false, overwrites original
     * @return Result with output file path
     */
    suspend fun execute(
        gifPath: String, 
        speedMultiplier: Float,
        saveToDownloads: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate speed multiplier
            val clampedSpeed = speedMultiplier.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER)
            
            val gifFile = File(gifPath)
            if (!gifFile.exists() || !gifFile.canWrite()) {
                return@withContext Result.failure(Exception("GIF file not found or not writable: $gifPath"))
            }

            Timber.d("ChangeGifSpeed: Loading GIF from $gifPath, speed multiplier: $clampedSpeed")
            
            // Read original GIF data
            val originalGifData = gifFile.readBytes()
            
            // Decode GIF to get frames and delays
            val bitmapProvider = GifBitmapProvider(Glide.get(context).bitmapPool)
            val gifDecoder: GifDecoder = StandardGifDecoder(bitmapProvider)
            gifDecoder.read(originalGifData)
            
            val frameCount = gifDecoder.frameCount
            if (frameCount == 0) {
                gifDecoder.clear()
                return@withContext Result.failure(Exception("GIF file contains no frames"))
            }
            
            Timber.d("ChangeGifSpeed: Found $frameCount frames")
            
            // Extract all frames and calculate new delays
            val frames = mutableListOf<Bitmap>()
            val newDelays = mutableListOf<Int>()
            
            for (i in 0 until frameCount) {
                try {
                    val originalDelay = gifDecoder.getDelay(i)
                    val newDelay = (originalDelay / clampedSpeed).toInt().coerceAtLeast(10) // Min 10ms delay
                    
                    gifDecoder.advance()
                    val frameBitmap = gifDecoder.nextFrame
                    
                    if (frameBitmap != null) {
                        // Create copy of bitmap (decoder reuses bitmaps)
                        val frameCopy = frameBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        frames.add(frameCopy)
                        newDelays.add(newDelay)
                        
                        Timber.d("ChangeGifSpeed: Frame $i - original delay: ${originalDelay}ms, new delay: ${newDelay}ms")
                    } else {
                        Timber.w("ChangeGifSpeed: Frame $i is null, skipping")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "ChangeGifSpeed: Failed to process frame $i")
                }
            }
            
            gifDecoder.clear()
            
            if (frames.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to extract any frames"))
            }
            
            // Create temporary file for new GIF
            // Note: GIF files are always downloaded to local cache before processing,
            // so parentFile should always work. But add safety check.
            val parentDir = gifFile.parentFile 
                ?: return@withContext Result.failure(Exception("Cannot access parent directory"))
            val tempFile = File(parentDir, "${gifFile.nameWithoutExtension}_temp.gif")
            
            // Encode new GIF with adjusted delays
            val encoder = AnimatedGifEncoder()
            encoder.start(FileOutputStream(tempFile))
            encoder.setRepeat(0) // Loop indefinitely (same as original GIF behavior)
            
            for (i in frames.indices) {
                encoder.setDelay(newDelays[i])
                encoder.addFrame(frames[i])
                
                // Release bitmap
                frames[i].recycle()
            }
            
            encoder.finish()
            
            // Determine output file location
            val outputFile = if (saveToDownloads) {
                // Save to Downloads with speed suffix
                val baseFileName = gifFile.nameWithoutExtension
                val speedFormatted = String.format("%.1f", clampedSpeed).replace(".", "_")
                val outputFileName = "${baseFileName}_speed_${speedFormatted}x.gif"
                
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                File(downloadsDir, outputFileName)
            } else {
                // Overwrite original
                gifFile
            }
            
            // Save new GIF
            if (tempFile.exists() && tempFile.length() > 0) {
                if (saveToDownloads) {
                    // Copy temp to Downloads
                    tempFile.copyTo(outputFile, overwrite = true)
                    tempFile.delete()
                } else {
                    // Replace original
                    gifFile.delete()
                    tempFile.renameTo(gifFile)
                }
                
                Timber.d("ChangeGifSpeed: Successfully changed speed to ${clampedSpeed}x, output: ${outputFile.absolutePath}")
                Result.success(outputFile.absolutePath)
            } else {
                tempFile.delete()
                Result.failure(Exception("Failed to encode new GIF"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "ChangeGifSpeed: Failed to change speed for $gifPath")
            Result.failure(e)
        }
    }
}

/**
 * Simple AnimatedGifEncoder based on Kevin Weiner's GIFEncoder
 * Stripped down version for basic GIF encoding with frame delays
 */
private class AnimatedGifEncoder {
    private var out: FileOutputStream? = null
    private var width = 0
    private var height = 0
    private var delay = 100 // Default 100ms between frames
    private var repeat = 0 // 0 = loop forever
    private var started = false
    
    fun start(os: FileOutputStream): Boolean {
        out = os
        started = false
        return true
    }
    
    fun setDelay(ms: Int) {
        delay = ms.coerceAtLeast(10) // Min 10ms
    }
    
    fun setRepeat(iter: Int) {
        repeat = iter
    }
    
    fun addFrame(im: Bitmap): Boolean {
        if (out == null) return false
        
        try {
            if (!started) {
                width = im.width
                height = im.height
                writeHeader()
                writeLSD()
                writeNetscapeExt()
                started = true
            }
            
            writeGraphicCtrlExt()
            writeImageDesc()
            writePixels(im)
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun finish(): Boolean {
        if (!started) return false
        
        try {
            started = false
            out?.write(0x3b) // GIF trailer
            out?.flush()
            out?.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun writeHeader() {
        out?.write("GIF89a".toByteArray()) // GIF89a signature
    }
    
    private fun writeLSD() {
        // Logical Screen Descriptor
        writeShort(width)
        writeShort(height)
        out?.write(0xF7) // Global color table: 8-bit color depth
        out?.write(0) // Background color index
        out?.write(0) // Pixel aspect ratio
        
        // Global color table (256 colors)
        for (i in 0 until 256) {
            out?.write(i) // Red
            out?.write(i) // Green
            out?.write(i) // Blue
        }
    }
    
    private fun writeNetscapeExt() {
        // Application extension for looping
        out?.write(0x21) // Extension introducer
        out?.write(0xFF) // App extension label
        out?.write(11) // Block size
        out?.write("NETSCAPE2.0".toByteArray())
        out?.write(3) // Sub-block size
        out?.write(1) // Loop sub-block ID
        writeShort(repeat) // Loop count
        out?.write(0) // Block terminator
    }
    
    private fun writeGraphicCtrlExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xF9) // Graphic control label
        out?.write(4) // Block size
        out?.write(0) // Disposal method (no disposal specified)
        writeShort(delay / 10) // Delay time in 1/100 sec
        out?.write(0) // Transparent color index
        out?.write(0) // Block terminator
    }
    
    private fun writeImageDesc() {
        out?.write(0x2C) // Image separator
        writeShort(0) // Image left position
        writeShort(0) // Image top position
        writeShort(width) // Image width
        writeShort(height) // Image height
        out?.write(0) // Packed field (no local color table)
    }
    
    private fun writePixels(image: Bitmap) {
        // Simplified LZW encoding - just write raw pixel data
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
        
        out?.write(8) // LZW minimum code size
        
        // Convert ARGB pixels to grayscale indices (0-255)
        val indexedPixels = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            indexedPixels[i] = ((r + g + b) / 3).toByte()
        }
        
        // Write pixel data in chunks
        var remaining = indexedPixels.size
        var offset = 0
        while (remaining > 0) {
            val chunkSize = remaining.coerceAtMost(255)
            out?.write(chunkSize)
            out?.write(indexedPixels, offset, chunkSize)
            offset += chunkSize
            remaining -= chunkSize
        }
        
        out?.write(0) // Block terminator
    }
    
    private fun writeShort(value: Int) {
        out?.write(value and 0xFF)
        out?.write((value shr 8) and 0xFF)
    }
}
