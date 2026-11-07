package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

/** Comprueba si la sesión actual pertenece al publicador indicado. */
class CheckMissionOwnershipUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(publisherId: String): Result<Boolean> {
        return runCatching { repository.isCurrentUser(publisherId) }
    }
}
