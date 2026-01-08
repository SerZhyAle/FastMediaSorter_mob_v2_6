package com.sza.fastmediasorter.domain.usecase.debug

import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.repository.StressTestRepository
import javax.inject.Inject

class GenerateStressDataUseCase @Inject constructor(
    private val repository: StressTestRepository
) {
    suspend operator fun invoke(
        directoryPath: String,
        count: Int,
        onProgress: (Int) -> Unit
    ): Result<Unit> {
        return repository.generateFiles(directoryPath, count, onProgress)
    }
}
