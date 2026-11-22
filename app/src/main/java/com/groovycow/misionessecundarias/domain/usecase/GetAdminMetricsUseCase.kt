package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AdminError
import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import com.groovycow.misionessecundarias.domain.model.AdminValidationException
import com.groovycow.misionessecundarias.domain.repository.AdminRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Consulta las métricas operativas globales si el usuario autenticado posee rol de Moderador o Administrador.
 */
class GetAdminMetricsUseCase(
    private val adminRepository: AdminRepository,
) {
    /**
     * @return Métricas operativas globales en caso de éxito, o la excepción tipada (ej. AccessDenied).
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(): Result<AdminMetrics> {
        return try {
            val role = adminRepository.getUserRole().trim().lowercase()
            if (role != MODERATOR_ROLE && role != ADMIN_ROLE) {
                throw AdminValidationException(AdminError.AccessDenied)
            }

            Result.success(adminRepository.getOperationalMetrics())
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
