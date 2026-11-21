package com.groovycow.misionessecundarias.domain.model

/** Errores tipados que se producen durante el acceso y gestión de la cola de moderación. */
sealed interface ModerationError {
    /** Acceso denegado: el usuario no tiene rol de moderador. */
    data object AccessDenied : ModerationError

    /** Se requiere iniciar sesión. */
    data object SessionRequired : ModerationError

    /** No hay conexión a internet. */
    data object NoConnection : ModerationError

    /** Ocurrió un error en el servidor. */
    data object ServerError : ModerationError
}

/** Excepción lanzada cuando ocurre un error en la capa de moderación. */
class ModerationValidationException(val error: ModerationError) : RuntimeException(error.toString())
