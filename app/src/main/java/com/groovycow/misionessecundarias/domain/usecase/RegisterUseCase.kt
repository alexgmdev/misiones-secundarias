package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.RegistrationData
import com.groovycow.misionessecundarias.domain.repository.AuthRepository

/**
 * Registra una cuenta nueva: verifica que el celular esté libre y luego crea la cuenta.
 *
 * @property repository Repositorio de autenticación.
 */
class RegisterUseCase(private val repository: AuthRepository) {
    /**
     * Ejecuta el registro.
     *
     * @param data Datos validados, con el celular normalizado.
     * @return [Result.success] si la cuenta quedó creada y con sesión; [Result.failure] con una
     * [AuthException] ([AuthError.PhoneInUse], [AuthError.EmailInUse], [AuthError.NoConnection], [AuthError.ServerError]...).
     */
    suspend operator fun invoke(data: RegistrationData): Result<Unit> =
        authCatching {
            if (!repository.isPhoneAvailable(data.phone)) throw AuthException(AuthError.PhoneInUse)
            try {
                repository.signUp(data)
            } catch (e: AuthException) {
                throw if (e.error == AuthError.ServerError && phoneTakenMeanwhile(data.phone)) AuthException(AuthError.PhoneInUse) else e
            }
        }

    /**
     * Carrera de unicidad: si el alta falló con error de servidor y el celular ya no está libre,
     * la causa fue la restricción UNIQUE del teléfono.
     */
    private suspend fun phoneTakenMeanwhile(phone: String): Boolean =
        authCatching { repository.isPhoneAvailable(phone) }.getOrNull() == false
}
