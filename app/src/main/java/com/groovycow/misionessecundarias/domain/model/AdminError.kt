package com.groovycow.misionessecundarias.domain.model

/** Errores asociados al acceso y consulta del panel administrativo. */
sealed interface AdminError {
    /** Acceso denegado: el usuario no tiene rol de moderador ni administrador. */
    data object AccessDenied : AdminError

    /** Se requiere iniciar sesión. */
    data object SessionRequired : AdminError

    /** No hay conexión a internet. */
    data object NoConnection : AdminError

    /** Ocurrió un error en el servidor. */
    data object ServerError : AdminError
}

/** Excepción lanzada cuando ocurre un error de validación o acceso en el área administrativa. */
class AdminValidationException(val error: AdminError) : RuntimeException(error.toString())
