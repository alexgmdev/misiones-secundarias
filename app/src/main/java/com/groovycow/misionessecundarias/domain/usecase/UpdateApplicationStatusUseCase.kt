package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

/** Acepta o rechaza una postulación pendiente de una misión propia. */
class UpdateApplicationStatusUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(
        applicationId: String,
        decision: ApplicationDecision,
    ): Result<Unit> = runCatching { repository.updateApplicationStatus(applicationId, decision) }
}
