package com.sza.fastmediasorter.domain.repository

import com.sza.fastmediasorter.domain.model.Result

interface StressTestRepository {
    suspend fun generateFiles(
        directoryPath: String,
        count: Int,
        onProgress: (Int) -> Unit
    ): Result<Unit>
}
