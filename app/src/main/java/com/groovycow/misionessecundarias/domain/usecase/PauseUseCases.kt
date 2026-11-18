package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import kotlin.coroutines.cancellation.CancellationException

/** Obtiene las pausas activas de una o varias misiones. */
class GetActivePausesUseCase(private val repository: PauseRepository) {
    suspend operator fun invoke(missionIds: List<String>): Result<Map<String, Set<PauseKind>>> =
        try {
            Result.success(if (missionIds.isEmpty()) emptyMap() else repository.getActivePauses(missionIds))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}

/** Activa o desactiva una pausa de la misión; los postulantes aceptados la ven como "en pausa". */
class SetMissionPauseUseCase(private val repository: PauseRepository) {
    suspend operator fun invoke(
        missionId: String,
        kind: PauseKind,
        isActive: Boolean,
    ): Result<Unit> =
        try {
            Result.success(repository.setPause(missionId, kind, isActive))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
