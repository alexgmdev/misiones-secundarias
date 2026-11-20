package com.groovycow.misionessecundarias.domain.model

/** Errores tipados del perfil; la capa de UI los traduce a mensajes en español. */
sealed interface ProfileError {
    /** El nickname ya pertenece a otro usuario. */
    data object NicknameTaken : ProfileError

    /** La imagen elegida no es JPG ni PNG. */
    data object UnsupportedPhotoFormat : ProfileError

    /** La foto no se pudo reducir a 2 MB o menos. */
    data object PhotoTooLarge : ProfileError

    /** No hay sesión activa. */
    data object SessionRequired : ProfileError

    /** No hay conexión con el servidor o se agotó el tiempo de espera. */
    data object NoConnection : ProfileError

    /** Error inesperado del servidor. */
    data object ServerError : ProfileError
}

/**
 * Excepción de los repositorios de perfil que transporta un [ProfileError] tipado.
 *
 * @property error Error que originó la excepción.
 */
class ProfileException(val error: ProfileError) : Exception(error.toString())
