package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

/** Obtiene las misiones creadas por el usuario actual, sin importar su estado. */
class GetMyMissionsUseCase(private val repository: MissionRepository) {
    suspend operator fun invoke(): Result<List<Mission>> {
        return try {
            Result.success(repository.getMyMissions())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
