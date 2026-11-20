
package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantPublicProfile
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class GetApplicantPublicProfileUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(userId: String): Result<ApplicantPublicProfile?> =
        runCatching {
            repository.getPublicProfile(userId)
        }
}
