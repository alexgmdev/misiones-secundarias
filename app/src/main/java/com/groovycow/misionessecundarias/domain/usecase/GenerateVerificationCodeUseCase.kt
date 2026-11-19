package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Genera el código único de verificación de cierre de una misión
 * tras haber pasado a estado 'Completada' o 'Rendida aprobada'.
 */
class GenerateVerificationCodeUseCase(
    private val missionRepository: MissionRepository,
) {
    /**
     * @param missionId Identificador de la misión finalizada.
     * @return Clave única ('CALI2026') en caso de éxito, o la excepción correspondiente.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(missionId: String): Result<String> {
        return try {
            validateMissionForCodeGeneration(missionId)
            Result.success(missionRepository.generateVerificationCode(missionId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun validateMissionForCodeGeneration(missionId: String) {
        val mission =
            missionRepository.getMissionById(missionId)
                ?: throw MissionValidationException(MissionError.MissionNotFound)

        if (
            mission.status != IN_PROGRESS_STATUS &&
            mission.status != COMPLETED_STATUS &&
            mission.status != RENDERED_APPROVED_STATUS
        ) {
            throw MissionValidationException(MissionError.MissionNotCompleted)
        }
    }

    private companion object {
        const val IN_PROGRESS_STATUS = "En progreso"
        const val COMPLETED_STATUS = "Completada"
        const val RENDERED_APPROVED_STATUS = "Rendida aprobada"
    }
}
