package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator

/**
 * Define la nueva contraseña tras verificar el código y cierra la sesión temporal de recuperación,
 * de modo que el usuario deba iniciar sesión con la nueva clave.
 *
 * @property repository Repositorio de autenticación.
 * @property passwordValidator Validador de la robustez de la contraseña.
 */
class ChangePasswordUseCase(
    private val repository: AuthRepository,
    private val passwordValidator: PasswordValidator,
) {
    /**
     * Actualiza la contraseña y cierra la sesión.
     *
     * @param newPassword Nueva contraseña (la coincidencia con la confirmación la valida el ViewModel).
     * @return [Result.success] si la clave cambió (aunque falle el cierre de sesión posterior);
     * [Result.failure] con una [AuthException] cuyo error es [AuthError.WeakPassword], [AuthError.SamePassword],
     * [AuthError.NoConnection] o [AuthError.ServerError]. Una clave débil falla sin llamar al backend.
     */
    suspend operator fun invoke(newPassword: String): Result<Unit> {
        if (passwordValidator.unmetRules(newPassword).isNotEmpty()) {
            return Result.failure(AuthException(AuthError.WeakPassword))
        }
        return authCatching { repository.updatePassword(newPassword) }
            .recoverCatching { throw AuthException(it.toChangeError()) }
            .onSuccess { authCatching { repository.signOut() } }
    }

    private fun Throwable.toChangeError(): AuthError =
        when (val error = (this as? AuthException)?.error) {
            AuthError.WeakPassword, AuthError.SamePassword, AuthError.NoConnection -> error
            else -> AuthError.ServerError
        }
}
