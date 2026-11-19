package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Edita los datos descriptivos de una misión publicada sin participantes aceptados (HU-032).
 */
class UpdateMissionDetailsUseCase(
    private val missionRepository: MissionRepository,
    private val applicationRepository: ApplicationRepository,
) {
    /**
     * Valida las reglas del negocio y persiste los cambios descriptivos.
     *
     * @param missionId Misión propia que se quiere editar.
     * @param update Nuevos datos descriptivos.
     * @return Misión actualizada o el error de dominio/de repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(
        missionId: String,
        update: MissionDetailsUpdate,
    ): Result<Mission> =
        try {
            validateFields(update)
            val mission =
                missionRepository.getMissionById(missionId)
                    ?: throw MissionValidationException(MissionError.NotFound)
            if (mission.status != PUBLISHED_STATUS) {
                throw MissionValidationException(MissionError.NotEditable)
            }
            val hasAcceptedParticipants =
                applicationRepository
                    .getMissionApplicants(missionId)
                    .any { it.status == ACCEPTED_STATUS }
            if (hasAcceptedParticipants) {
                throw MissionValidationException(MissionError.HasAcceptedParticipants)
            }
            Result.success(missionRepository.updateMissionDetails(missionId, update.sanitized()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }

    private fun validateFields(update: MissionDetailsUpdate) {
        val incomplete =
            update.title.isBlank() ||
                update.description.isBlank() ||
                update.requirements.isBlank()
        if (incomplete) throw MissionValidationException(MissionError.InvalidDetails)
    }

    private fun MissionDetailsUpdate.sanitized(): MissionDetailsUpdate =
        copy(
            title = title.trim(),
            description = description.trim(),
            requirements = requirements.trim(),
            category = category?.trim()?.takeIf(String::isNotEmpty),
            reward = reward?.trim()?.takeIf(String::isNotEmpty),
        )

    private companion object {
        const val PUBLISHED_STATUS = "Publicada"
        const val ACCEPTED_STATUS = "Aceptado"
    }
}
