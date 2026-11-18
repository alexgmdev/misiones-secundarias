package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

/** Obtiene las postulaciones de una misión perteneciente al usuario actual. */
class GetMissionApplicantsUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(missionId: String): Result<List<MissionApplicant>> =
        runCatching { repository.getMissionApplicants(missionId) }
}
