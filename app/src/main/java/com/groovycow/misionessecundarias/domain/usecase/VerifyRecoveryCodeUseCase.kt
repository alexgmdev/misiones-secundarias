package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.RecoveryCodeValidator
import com.groovycow.misionessecundarias.domain.validation.ValidationResult

/**
 * Verifica el código de recuperación enviado por correo. Un código incorrecto y uno vencido se reportan igual.
 *
 * @property repository Repositorio de autenticación.
 * @property codeValidator Validador del formato numérico del código.
 */
class VerifyRecoveryCodeUseCase(
    private val repository: AuthRepository,
    private val codeValidator: RecoveryCodeValidator,
) {
    /**
     * Verifica el código.
     *
     * @param email Correo al que se envió el código.
     * @param code Código digitado.
     * @return [Result.success] si el código es válido; [Result.failure] con una [AuthException] cuyo error es solo
     * [AuthError.InvalidOrExpiredCode], [AuthError.NoConnection] o [AuthError.ServerError]. Un código no numérico
     * falla sin llamar al backend.
     */
    suspend operator fun invoke(
        email: String,
        code: String,
    ): Result<Unit> {
        if (codeValidator.validate(code) is ValidationResult.Invalid) {
            return Result.failure(AuthException(AuthError.InvalidOrExpiredCode))
        }
        return authCatching { repository.verifyRecoveryCode(email, codeValidator.normalize(code)) }
            .recoverCatching { throw AuthException(it.toVerifyError()) }
    }

    private fun Throwable.toVerifyError(): AuthError =
        when (val error = (this as? AuthException)?.error) {
            AuthError.InvalidOrExpiredCode, AuthError.NoConnection -> error
            else -> AuthError.ServerError
        }
}
