package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.RegistrationData
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
     * Consulta si un celular está libre (RPC `telefono_disponible`, que solo devuelve un booleano).
     *
     * @param phone Celular normalizado (10 dígitos).
     * @return `true` si ningún usuario lo tiene registrado.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si falla la consulta.
     */
    suspend fun isPhoneAvailable(phone: String): Boolean

    /**
     * Registra una cuenta nueva con su perfil e inicia sesión (Confirm email desactivado).
     *
     * @param data Datos de registro con el celular ya normalizado.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si el correo ya existe, hay error de red o del servidor.
     */
    suspend fun signUp(data: RegistrationData)

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
     * Inicia sesión con celular y contraseña mediante la Edge Function `login-telefono`, que devuelve solo la sesión
     * (nunca el correo); la sesión recibida se importa al cliente de autenticación.
     *
     * @param phone Celular normalizado (10 dígitos).
     * @param password Contraseña.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si las credenciales son incorrectas,
     * el celular está bloqueado por intentos fallidos (mismo error), o hay error de red o del servidor.
     */
    suspend fun signInWithPhone(
        phone: String,
        password: String,
    )

    /**
     * Solicita a Supabase que envíe por correo un código numérico (OTP) de recuperación de contraseña.
     *
     * @param email Correo de la cuenta (sin espacios laterales).
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si hay error de red o del servidor.
     */
    suspend fun sendPasswordResetCode(email: String)

    /**
     * Verifica el código de recuperación (`verifyEmailOtp` de tipo recuperación); si es válido el cliente
     * queda con una sesión temporal que permite cambiar la contraseña.
     *
     * @param email Correo al que se envió el código.
     * @param code Código numérico digitado.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException con `InvalidOrExpiredCode` si el
     * código es incorrecto o expiró, o por error de red o del servidor.
     */
    suspend fun verifyRecoveryCode(
        email: String,
        code: String,
    )

    /**
     * Actualiza la contraseña del usuario de la sesión de recuperación (`updateUser { password }`).
     *
     * @param newPassword Nueva contraseña.
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si la clave es débil, igual a la
     * anterior, o hay error de red o del servidor.
     */
    suspend fun updatePassword(newPassword: String)

    /**
     * Cierra la sesión actual.
     *
     * @throws com.groovycow.misionessecundarias.domain.model.AuthException si falla la operación.
     */
    suspend fun signOut()
}
