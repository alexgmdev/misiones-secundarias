package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.AdminRepository
import kotlin.coroutines.cancellation.CancellationException

/** Comprueba si el rol autenticado puede acceder al panel administrativo. */
class CanAccessAdminPanelUseCase(
    private val adminRepository: AdminRepository,
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val role = adminRepository.getUserRole().trim().lowercase()
            Result.success(role == MODERATOR_ROLE || role == ADMIN_ROLE)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val MODERATOR_ROLE = "moderator"
        const val ADMIN_ROLE = "admin"
    }
}
