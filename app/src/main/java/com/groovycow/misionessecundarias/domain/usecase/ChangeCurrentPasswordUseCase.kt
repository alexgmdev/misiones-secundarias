package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.ValidationResult

/** Campos del formulario de cambio de contraseña desde Ajustes. */
enum class ChangePasswordField {
    /** Contraseña actual. */
    Current,

    /** Nueva contraseña. */
    New,

    /** Confirmación de la nueva contraseña. */
    Confirmation,
}

/**
 * Fallo de validación local del cambio de contraseña (no se llamó al backend).
 *
 * @property errors Error de validación por campo.
 */
class ChangePasswordValidationException(val errors: Map<ChangePasswordField, FieldError>) : Exception(errors.toString())

/**
 * Cambia la contraseña con la sesión activa (HU-010): valida el formulario, reautentica con la contraseña
 * actual para comprobarla y luego actualiza la clave. No cierra la sesión.
 *
 * @property repository Repositorio de autenticación.
 * @property passwordValidator Validador de la robustez de la contraseña y su confirmación.
 */
class ChangeCurrentPasswordUseCase(
    private val repository: AuthRepository,
    private val passwordValidator: PasswordValidator,
) {
    /**
     * Valida los tres campos sin llamar al backend.
     *
     * @param current Contraseña actual digitada.
     * @param new Nueva contraseña digitada.
     * @param confirmation Confirmación digitada.
     * @return Errores por campo; vacío si el formulario es válido.
     */
    fun validate(
        current: String,
        new: String,
        confirmation: String,
    ): Map<ChangePasswordField, FieldError> =
        buildMap {
            if (current.isEmpty()) put(ChangePasswordField.Current, FieldError.Required)
            record(ChangePasswordField.New, passwordValidator.validate(new))
            if (!containsKey(ChangePasswordField.New)) {
                record(ChangePasswordField.Confirmation, passwordValidator.validateConfirmation(new, confirmation))
            } else if (confirmation.isEmpty()) {
                put(ChangePasswordField.Confirmation, FieldError.Required)
            }
        }

    /**
     * Cambia la contraseña.
     *
     * @param current Contraseña actual digitada.
     * @param new Nueva contraseña digitada.
     * @param confirmation Confirmación digitada.
     * @return [Result.success] si la clave cambió (la sesión sigue activa); [Result.failure] con una
     * [ChangePasswordValidationException] (validación local), o con una [AuthException] cuyo error es
     * [AuthError.SamePassword], [AuthError.IncorrectCurrentPassword], [AuthError.WeakPassword],
     * [AuthError.NoConnection] o [AuthError.ServerError]. Las fallas locales no llaman al backend.
     */
    suspend operator fun invoke(
        current: String,
        new: String,
        confirmation: String,
    ): Result<Unit> {
        val errors = validate(current, new, confirmation)
        return when {
            errors.isNotEmpty() -> Result.failure(ChangePasswordValidationException(errors))
            new == current -> Result.failure(AuthException(AuthError.SamePassword))
            else -> reauthenticateAndUpdate(current, new)
        }
    }

    private suspend fun reauthenticateAndUpdate(
        current: String,
        new: String,
    ): Result<Unit> {
        val email = repository.currentUserEmail() ?: return Result.failure(AuthException(AuthError.ServerError))
        val reauth = authCatching { repository.signIn(email, current) }
        val outcome = if (reauth.isSuccess) authCatching { repository.updatePassword(new) } else reauth
        return outcome.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(AuthException(if (reauth.isSuccess) it.toUpdateError() else it.toReauthError())) },
        )
    }

    private fun MutableMap<ChangePasswordField, FieldError>.record(
        field: ChangePasswordField,
        result: ValidationResult,
    ) {
        if (result is ValidationResult.Invalid) put(field, result.error)
    }

    private fun Throwable?.toReauthError(): AuthError =
        when (val error = (this as? AuthException)?.error) {
            AuthError.InvalidCredentials -> AuthError.IncorrectCurrentPassword
            AuthError.NoConnection -> error
            else -> AuthError.ServerError
        }

    private fun Throwable.toUpdateError(): AuthError =
        when (val error = (this as? AuthException)?.error) {
            AuthError.WeakPassword, AuthError.SamePassword, AuthError.NoConnection -> error
            else -> AuthError.ServerError
        }
}
