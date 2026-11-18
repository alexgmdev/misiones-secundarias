package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

/**
 * Obtiene los solucionadores presentes en el panel de ejecución en vivo (HU-051).
 *
 * Solo se consideran participantes los postulantes con estado "Aceptado"; los
 * postulados pendientes y los rechazados no aparecen en el panel.
 */
class GetLivePanelParticipantsUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(missionId: String): Result<List<MissionApplicant>> =
        runCatching {
            repository.getMissionApplicants(missionId)
                .filter { it.status == ApplicationDecision.ACCEPT.databaseValue }
        }
}
