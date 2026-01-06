package com.sza.fastmediasorter.data.cloud.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.sza.fastmediasorter.data.cloud.CloudResult
import com.sza.fastmediasorter.data.cloud.GoogleDriveRestClient
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

/**
 * ExoPlayer DataSource for streaming media from Google Drive.
 * Supports range requests for seeking in video/audio files.
 */
class CloudDataSource(
    private val client: GoogleDriveRestClient
) : BaseDataSource(/* isNetwork = */ true) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var opened = false
    private var currentFileId: String? = null

    override fun open(dataSpec: DataSpec): Long {
        Timber.d("CloudDataSource.open: uri=${dataSpec.uri}")
        
        try {
            // Notify listeners that transfer is initializing
            transferInitializing(dataSpec)
            
            // Extract fileId from cloud:// URI
            // Format: cloud://googledrive/{fileId}
            val fileId = extractFileId(dataSpec.uri)
            currentFileId = fileId
            
            Timber.d("CloudDataSource.open: Extracted fileId=$fileId")
            
            // Get authenticated download stream from GoogleDriveRestClient
            val result = runBlocking {
                client.getFileInputStream(fileId, dataSpec.position, dataSpec.length)
            }
            
            when (result) {
                is CloudResult.Success -> {
                    inputStream = result.data
                    
                    // Calculate bytes remaining
                    bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                        dataSpec.length
                    } else {
                        // Unknown length - will read until end
                        C.LENGTH_UNSET.toLong()
                    }
                    
                    opened = true
                    
                    // Notify listeners that transfer started
                    transferStarted(dataSpec)
                    
                    Timber.d("CloudDataSource.open: Stream opened successfully, bytesRemaining=$bytesRemaining")
                    return bytesRemaining
                }
                is CloudResult.Error -> {
                    Timber.e("CloudDataSource.open: Failed to open stream - ${result.message}")
                    throw IOException("Failed to open cloud stream: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CloudDataSource.open: Exception during open")
            throw IOException("Failed to open cloud data source", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }
        
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        
        return try {
            val stream = inputStream ?: throw IOException("Input stream is null")
            
            val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
                length
            } else {
                minOf(bytesRemaining, length.toLong()).toInt()
            }
            
            val bytesRead = stream.read(buffer, offset, bytesToRead)
            
            if (bytesRead == -1) {
                if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                    Timber.w("CloudDataSource.read: Stream ended prematurely")
                }
                return C.RESULT_END_OF_INPUT
            }
            
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining -= bytesRead
            }
            
            bytesTransferred(bytesRead)
            return bytesRead
        } catch (e: Exception) {
            Timber.e(e, "CloudDataSource.read: Exception during read")
            throw IOException("Failed to read from cloud stream", e)
        }
    }

    override fun getUri(): Uri? {
        return currentFileId?.let { Uri.parse("cloud://googledrive/$it") }
    }

    override fun close() {
        Timber.d("CloudDataSource.close: Closing stream")
        
        try {
            inputStream?.close()
        } catch (e: Exception) {
            Timber.e(e, "CloudDataSource.close: Exception during close")
        } finally {
            inputStream = null
            bytesRemaining = 0
            if (opened) {
                opened = false
                transferEnded()
            }
            currentFileId = null
        }
    }

    private fun extractFileId(uri: Uri): String {
        // Format: cloud://googledrive/{fileId} or cloud:///googledrive/{fileId}
        val path = uri.path ?: throw IOException("Invalid cloud URI: no path")
        val fileId = path.substringAfterLast("/")
        
        if (fileId.isEmpty()) {
            throw IOException("Invalid cloud URI: no fileId in path")
        }
        
        return fileId
    }
}

/**
 * Factory for creating CloudDataSource instances.
 */
class CloudDataSourceFactory(
    private val client: GoogleDriveRestClient
) : DataSource.Factory {
    
    override fun createDataSource(): DataSource {
        return CloudDataSource(client)
    }
}
