package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Application
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class ApplyToMissionUseCase(
    private val applicationRepository: ApplicationRepository
) {
    suspend operator fun invoke(missionId: String): Result<Application> =
        runCatching { applicationRepository.applyToMission(missionId) }
}
