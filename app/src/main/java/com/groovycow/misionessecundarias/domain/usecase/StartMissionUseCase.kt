package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Inicia la ejecución de una misión propia (HU-050).
 *
 * Exige que la misión esté publicada o asignada, que ya haya llegado la fecha y hora
 * programadas y que exista el mínimo de participantes aceptados. Si todo se cumple,
 * cambia el estado a "En progreso".
 */
class StartMissionUseCase(
    private val missionRepository: MissionRepository,
    private val applicationRepository: ApplicationRepository,
    private val clock: Clock,
) {
    /**
     * @param missionId Identificador de la misión que el publicador quiere iniciar.
     * @return Misión en progreso, o el error tipado/de repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(missionId: String): Result<Mission> {
        return try {
            val mission =
                missionRepository.getMissionById(missionId)
                    ?: throw MissionValidationException(MissionError.MissionNotFound)

            if (mission.status == IN_PROGRESS_STATUS) {
                Result.success(mission)
            } else {
                validate(mission)
                Result.success(missionRepository.startMission(missionId))
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
                mission.status !in STARTABLE_STATUSES -> MissionError.NotStartable
                !hasStarted(mission) -> MissionError.StartTooEarly
                acceptedParticipants(mission.id) < MIN_ACCEPTED_PARTICIPANTS -> MissionError.NotEnoughParticipants
                else -> null
            }
        error?.let { throw MissionValidationException(it) }
    }

    private fun hasStarted(mission: Mission): Boolean {
        val start =
            runCatching { OffsetDateTime.parse(mission.dateTime) }.getOrNull()
                ?: throw MissionValidationException(MissionError.InvalidDateTime)
        return !clock.instant().isBefore(start.toInstant())
    }

    private suspend fun acceptedParticipants(missionId: String): Int =
        applicationRepository.getMissionApplicants(missionId)
            .count { it.status == ApplicationDecision.ACCEPT.databaseValue }

    companion object {
        /** Mínimo de participantes aceptados para poder iniciar una misión. */
        const val MIN_ACCEPTED_PARTICIPANTS = 1

        const val IN_PROGRESS_STATUS = "En progreso"
        private val STARTABLE_STATUSES = setOf("Publicada", "Asignada")
    }
}
