package com.groovycow.misionessecundarias.domain.model

/**
 * Errores tipados del flujo de autenticación (registro, login y recuperación).
 *
 * La capa de UI los traduce a mensajes en español; los tests verifican estos valores, no textos.
 */
sealed interface AuthError {
    /** Correo o contraseña (o celular) incorrectos. */
    data object InvalidCredentials : AuthError

    /** El correo ya está registrado. */
    data object EmailInUse : AuthError

    /** El celular ya está registrado. */
    data object PhoneInUse : AuthError

    /** La contraseña no cumple la política de robustez. */
    data object WeakPassword : AuthError

    /** El código de recuperación es inválido o ya expiró. */
    data object InvalidOrExpiredCode : AuthError

    /** La nueva contraseña es igual a la anterior. */
    data object SamePassword : AuthError

    /** No hay conexión con el servidor o se agotó el tiempo de espera. */
    data object NoConnection : AuthError

    /** Error inesperado del servidor. */
    data object ServerError : AuthError
}

/**
 * Excepción lanzada por los repositorios de autenticación que transporta un [AuthError] tipado.
 *
 * @property error Error de autenticación que originó la excepción.
 */
class AuthException(val error: AuthError) : Exception(error.toString())
