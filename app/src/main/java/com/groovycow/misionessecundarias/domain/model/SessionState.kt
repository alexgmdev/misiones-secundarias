package com.groovycow.misionessecundarias.domain.model

/** Estado de la sesión del usuario al arrancar la aplicación. */
enum class SessionState {
    /** Se está leyendo la sesión guardada en el dispositivo. */
    LOADING,

    /** Existe una sesión guardada (incluso sin red). */
    AUTHENTICATED,

    /** No hay sesión guardada. */
    UNAUTHENTICATED,
}
