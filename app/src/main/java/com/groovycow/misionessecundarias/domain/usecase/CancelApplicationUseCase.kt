package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class CancelApplicationUseCase(
    private val applicationRepository: ApplicationRepository
) {
    suspend operator fun invoke(applicationId: String): Result<Unit> =
        runCatching { applicationRepository.cancelApplication(applicationId) }
}
