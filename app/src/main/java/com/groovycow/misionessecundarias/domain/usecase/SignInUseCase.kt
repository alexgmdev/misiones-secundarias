package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator

/**
 * Inicia sesión con correo o celular: si el identificador contiene "@" es un correo; si no, un celular
 * que se normaliza y se autentica mediante la Edge Function `login-telefono`.
 *
 * @property repository Repositorio de autenticación.
 * @property phoneValidator Normalizador de celulares.
 */
class SignInUseCase(
    private val repository: AuthRepository,
    private val phoneValidator: PhoneValidator,
) {
    /**
     * Ejecuta el inicio de sesión.
     *
     * @param identifier Correo o celular (formato libre) digitado.
     * @param password Contraseña.
     * @return [Result.success] si la sesión quedó iniciada; [Result.failure] con una [AuthException] cuyo error
     * es solo uno de [AuthError.InvalidCredentials], [AuthError.NoConnection] o [AuthError.ServerError].
     */
    suspend operator fun invoke(
        identifier: String,
        password: String,
    ): Result<Unit> =
        authCatching {
            if (identifier.contains(EMAIL_MARKER)) {
                repository.signIn(identifier.trim(), password)
            } else {
                val phone = phoneValidator.normalize(identifier) ?: throw AuthException(AuthError.InvalidCredentials)
                repository.signInWithPhone(phone, password)
            }
        }.recoverCatching { throw AuthException(it.toLoginError()) }

    private fun Throwable.toLoginError(): AuthError =
        when (val error = (this as? AuthException)?.error) {
            AuthError.InvalidCredentials, AuthError.NoConnection -> error
            else -> AuthError.ServerError
        }

    private companion object {
        const val EMAIL_MARKER = "@"
    }
}
