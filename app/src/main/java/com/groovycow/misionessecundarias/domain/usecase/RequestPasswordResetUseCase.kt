package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.ValidationResult

/**
 * Solicita el código de recuperación de contraseña sin divulgar si la cuenta existe: cualquier respuesta
 * del servidor se reporta como éxito neutro; solo se informa la falta de conexión.
 *
 * @property repository Repositorio de autenticación.
 * @property emailValidator Validador del formato del correo.
 */
class RequestPasswordResetUseCase(
    private val repository: AuthRepository,
    private val emailValidator: EmailValidator = EmailValidator(),
) {
    /**
     * Envía el código al correo indicado.
     *
     * @param email Correo digitado (se recortan los espacios laterales).
     * @return [Result.success] salvo si el correo es inválido (no se llama al backend) o no hay conexión
     * ([AuthError.NoConnection]); la existencia de la cuenta nunca se revela.
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        val normalized = emailValidator.normalize(email)
        if (emailValidator.validate(normalized) is ValidationResult.Invalid) {
            return Result.failure(AuthException(AuthError.ServerError))
        }
        return authCatching { repository.sendPasswordResetCode(normalized) }
            .recoverCatching { failure ->
                if ((failure as? AuthException)?.error == AuthError.NoConnection) throw failure
            }
    }
}
