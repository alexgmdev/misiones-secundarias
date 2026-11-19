package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository

class GetMissionsUseCase(private val repository: MissionRepository) {
    suspend operator fun invoke(): Result<List<Mission>> {
        return try {
            Result.success(repository.getPublishedMissions())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
