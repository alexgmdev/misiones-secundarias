package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticación. Las implementaciones lanzan
 * [com.groovycow.misionessecundarias.domain.model.AuthException] ante errores.
 */
interface AuthRepository {
    /**
     * Observa el estado de la sesión persistida.
     *
     * @return Flujo de [SessionState]: LOADING mientras se lee el almacenamiento, luego AUTHENTICATED o UNAUTHENTICATED.
     */
    fun observeSession(): Flow<SessionState>

    /**
     * Inicia sesión con correo y contraseña.
     *
     * @param email Correo del usuario.
     * @param password Contraseña.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si falla la autenticación.
     */
    suspend fun signIn(
        email: String,
        password: String,
    )

    /**
     * Cierra la sesión actual.
     *
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si falla la operación.
     */
    suspend fun signOut()
}
