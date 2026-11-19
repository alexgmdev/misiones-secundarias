package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Registra las entradas de bitácora del publicador y de los participantes de una misión (HU - Evaluación y Cierre).
 */
class SaveMissionLogbookUseCase(
    private val logbookRepository: LogbookRepository,
) {
    /**
     * Guarda la bitácora oficial del publicador de la misión.
     *
     * @param missionId Identificador de la misión.
     * @param entry Relato redactado por el publicador.
     * @param participants Lista de solucionadores que participaron/completaron la misión.
     * @return Registro de la bitácora guardada.
     */
    suspend operator fun invoke(
        missionId: String,
        entry: String,
        participants: List<MissionApplicant>,
    ): Result<MissionLogbook> {
        return try {
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) {
                throw MissionValidationException(MissionError.LogbookEntryRequired)
            }

            Result.success(logbookRepository.savePublisherLogbook(missionId, trimmed, participants))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Guarda la vivencia/comentario individual de un participante solucionador.
     *
     * @param missionId Identificador de la misión.
     * @param entry Palabras/relato del participante.
     * @return Registro de la bitácora del participante guardada.
     */
    suspend fun saveParticipantEntry(
        missionId: String,
        entry: String,
    ): Result<MissionLogbook> {
        return try {
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) {
                throw MissionValidationException(MissionError.LogbookEntryRequired)
            }

            Result.success(logbookRepository.saveParticipantLogbook(missionId, trimmed))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
