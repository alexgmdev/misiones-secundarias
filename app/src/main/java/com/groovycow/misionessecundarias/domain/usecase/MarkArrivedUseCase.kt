package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class MarkArrivedUseCase(private val repository: ApplicationRepository) {
    suspend operator fun invoke(
        missionId: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit> = repository.markArrived(missionId, latitude, longitude)
}
