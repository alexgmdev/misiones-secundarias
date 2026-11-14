package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class MarkOnWayUseCase(private val repository: ApplicationRepository) {
    suspend operator fun invoke(missionId: String): Result<Unit> = repository.markOnWay(missionId)
}
