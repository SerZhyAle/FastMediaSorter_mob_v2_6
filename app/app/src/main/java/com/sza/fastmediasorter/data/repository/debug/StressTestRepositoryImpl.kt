package com.sza.fastmediasorter.data.repository.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.StressTestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class StressTestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StressTestRepository {

    override suspend fun generateFiles(
        directoryPath: String,
        count: Int,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    return@withContext Result.Error("Could not create directory: $directoryPath")
                }
            }

            // Create a small bitmap to reuse/save
            val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)
            
            for (i in 1..count) {
                if (i % 100 == 0) onProgress(i)
                
                val isImage = i % 2 == 0 // 50% images, 50% text
                
                if (isImage) {
                    val filename = "stress_test_img_${System.currentTimeMillis()}_$i.jpg"
                    val file = File(directory, filename)
                    createDummyImage(file, colors.random(), i)
                } else {
                    val filename = "stress_test_doc_${System.currentTimeMillis()}_$i.txt"
                    val file = File(directory, filename)
                    file.writeText("This is dummy file number $i for stress testing.\n" +
                            "Generated at ${System.currentTimeMillis()}\n" +
                            "Payload: ${"x".repeat(100)}")
                }
                
                // Small yield to allow cancellation if needed
                if (i % 500 == 0) {
                     kotlinx.coroutines.yield()
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Stress test generation failed", e)
        }
    }

    private fun createDummyImage(file: File, color: Int, number: Int) {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        
        val paint = Paint().apply {
            this.color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("#$number", 10f, 50f, paint)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
        }
        bitmap.recycle()
    }
}
