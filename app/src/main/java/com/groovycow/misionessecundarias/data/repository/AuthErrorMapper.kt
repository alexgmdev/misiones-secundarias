package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AuthError
import java.io.IOException

/** Operación de autenticación en curso; el mismo código de servidor significa cosas distintas según la operación. */
enum class AuthOperation {
    /** Inicio de sesión (correo o celular). */
    LOGIN,

    /** Verificación del código de recuperación. */
    VERIFY,

    /** Cualquier otra operación (registro, cambio de clave, etc.). */
    OTHER,
}

/** Traduce errores del servidor y de red a [AuthError]. Es puro y no depende del SDK de Supabase. */
class AuthErrorMapper {
    /**
     * Traduce una respuesta de error HTTP de GoTrue o de la Edge Function.
     *
     * @param errorCode Código de error de GoTrue (por ejemplo `user_already_exists`), o null si no vino.
     * @param statusCode Código de estado HTTP.
     * @param operation Operación durante la cual ocurrió el error.
     * @return El [AuthError] correspondiente.
     */
    fun mapRest(
        errorCode: String?,
        statusCode: Int,
        operation: AuthOperation,
    ): AuthError =
        when {
            errorCode == "user_already_exists" || errorCode == "email_exists" -> AuthError.EmailInUse
            errorCode == "weak_password" -> AuthError.WeakPassword
            errorCode == "same_password" -> AuthError.SamePassword
            errorCode == "otp_expired" -> AuthError.InvalidOrExpiredCode
            errorCode == "invalid_credentials" -> mapInvalidCredentials(operation)
            statusCode >= SERVER_ERROR_MIN_STATUS -> AuthError.ServerError
            operation == AuthOperation.LOGIN -> AuthError.InvalidCredentials
            else -> AuthError.ServerError
        }

    /**
     * Traduce una excepción de transporte (sin respuesta HTTP).
     *
     * @param throwable Excepción capturada.
     * @return [AuthError.NoConnection] si la causa es de red o tiempo de espera; [AuthError.ServerError] en otro caso.
     */
    fun mapTransport(throwable: Throwable): AuthError =
        if (generateSequence(throwable) { it.cause }.any { it is IOException }) {
            AuthError.NoConnection
        } else {
            AuthError.ServerError
        }

    private fun mapInvalidCredentials(operation: AuthOperation): AuthError =
        when (operation) {
            AuthOperation.LOGIN -> AuthError.InvalidCredentials
            AuthOperation.VERIFY -> AuthError.InvalidOrExpiredCode
            AuthOperation.OTHER -> AuthError.ServerError
        }

    private companion object {
        const val SERVER_ERROR_MIN_STATUS = 500
    }
}
