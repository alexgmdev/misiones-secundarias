
package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class GetPublicProfileUseCase(
    private val repository: ApplicationRepository,
) {
    suspend operator fun invoke(userId: String): Result<PublicProfile?> =
        runCatching {
            repository.getPublicProfile(userId)
        }
}
