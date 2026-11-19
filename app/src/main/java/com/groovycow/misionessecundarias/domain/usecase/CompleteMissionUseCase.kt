package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.model.checklistProgressPercent
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Completa una misión propia en ejecución cuando el progreso llegó al 100 % (HU-064).
 *
 * El progreso se recalcula con el checklist actual, no con lo que muestre la pantalla, y no se
 * exige ningún porcentaje de rendición. Completar una misión ya completada es idempotente.
 */
class CompleteMissionUseCase(
    private val missionRepository: MissionRepository,
    private val checklistRepository: ChecklistRepository,
) {
    /**
     * @param missionId Identificador de la misión que el publicador quiere completar.
     * @return Misión completada, o el error tipado/de repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(missionId: String): Result<Mission> {
        return try {
            val mission =
                missionRepository.getMissionById(missionId)
                    ?: throw MissionValidationException(MissionError.MissionNotFound)

            if (mission.status == COMPLETED_STATUS) {
                Result.success(mission)
            } else {
                validate(mission)
                Result.success(missionRepository.completeMission(missionId))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun validate(mission: Mission) {
        val error =
            when {
                mission.status != IN_PROGRESS_STATUS -> MissionError.NotCompletable
                checklistProgressPercent(checklistRepository.getItems(mission.id)) < FULL_PROGRESS -> MissionError.ProgressIncomplete
                else -> null
            }
        error?.let { throw MissionValidationException(it) }
    }

    private companion object {
        const val IN_PROGRESS_STATUS = "En progreso"
        const val COMPLETED_STATUS = "Completada"
        const val FULL_PROGRESS = 100
    }
}
