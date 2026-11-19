package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/** Valida los requisitos previos y publica un borrador existente. */
class PublishMissionUseCase(
    private val repository: MissionRepository,
    private val clock: Clock,
) {
    /**
     * Publica [mission] si sigue siendo un borrador completo y vigente.
     *
     * @param mission Borrador devuelto por Supabase al crearlo.
     * @return Misión publicada, o el error tipado/de repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(mission: Mission): Result<Mission> {
        return try {
            if (mission.status == PUBLISHED_STATUS) {
                Result.success(mission)
            } else {
                validate(mission)
                Result.success(repository.publishMission(mission.id))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun validate(mission: Mission) {
        val missingRequiredData =
            mission.title.isBlank() ||
                mission.description.isBlank() ||
                mission.requirements.isNullOrBlank() ||
                mission.maxParticipants < 2
        val validationError =
            when {
                mission.status != DRAFT_STATUS -> MissionError.NotDraft
                missingRequiredData -> MissionError.DraftIncomplete
                mission.modality.equals(FIELD_MODALITY, ignoreCase = true) && mission.meetingPoint.isNullOrBlank() -> {
                    MissionError.MeetingPointRequired
                }
                mission.modality.equals(FIELD_MODALITY, ignoreCase = true) && mission.location.isNullOrBlank() -> {
                    MissionError.DraftIncomplete
                }
                else -> dateError(mission)
            }
        validationError?.let { throw MissionValidationException(it) }
    }

    private fun dateError(mission: Mission): MissionError? {
        val startDate =
            runCatching {
                OffsetDateTime.parse(mission.dateTime).atZoneSameInstant(CALI_ZONE).toLocalDate()
            }.getOrNull() ?: return MissionError.InvalidDateTime
        return if (startDate.isBefore(LocalDate.now(clock.withZone(CALI_ZONE)))) MissionError.DateInPast else null
    }

    private companion object {
        const val DRAFT_STATUS = "Borrador"
        const val PUBLISHED_STATUS = "Publicada"
        const val FIELD_MODALITY = "campo"
        val CALI_ZONE: ZoneId = ZoneId.of("America/Bogota")
    }
}
