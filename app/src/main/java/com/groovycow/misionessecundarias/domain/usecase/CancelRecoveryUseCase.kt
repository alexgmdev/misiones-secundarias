package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.AuthRepository

/**
 * Cancela una recuperación abandonada cerrando la sesión temporal que dejó la verificación del código.
 *
 * @property repository Repositorio de autenticación.
 */
class CancelRecoveryUseCase(
    private val repository: AuthRepository,
) {
    /**
     * Cierra la sesión temporal.
     *
     * @return [Result.success] si se cerró; [Result.failure] si falló (el llamador puede ignorarlo).
     */
    suspend operator fun invoke(): Result<Unit> = authCatching { repository.signOut() }
}
