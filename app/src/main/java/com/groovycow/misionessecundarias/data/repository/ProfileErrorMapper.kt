package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.ProfileError
import java.io.IOException

/** Traduce errores de PostgREST, Storage y red a [ProfileError]. Es puro y no depende del SDK de Supabase. */
class ProfileErrorMapper {
    /**
     * Traduce una respuesta de error HTTP.
     *
     * @param body Cuerpo o descripción del error (puede incluir el código SQL, p. ej. `23505`), o null.
     * @param statusCode Código de estado HTTP.
     * @return El [ProfileError] correspondiente.
     */
    fun mapRest(
        body: String?,
        statusCode: Int,
    ): ProfileError =
        when {
            body?.contains(UNIQUE_VIOLATION) == true -> ProfileError.NicknameTaken
            statusCode == PAYLOAD_TOO_LARGE -> ProfileError.PhotoTooLarge
            statusCode == UNAUTHORIZED -> ProfileError.SessionRequired
            else -> ProfileError.ServerError
        }

    /**
     * Traduce una excepción de transporte (sin respuesta HTTP).
     *
     * @param throwable Excepción capturada.
     * @return [ProfileError.NoConnection] si la causa es de red o tiempo de espera; [ProfileError.ServerError] en otro caso.
     */
    fun mapTransport(throwable: Throwable): ProfileError =
        if (generateSequence(throwable) { it.cause }.any { it is IOException }) {
            ProfileError.NoConnection
        } else {
            ProfileError.ServerError
        }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
        const val PAYLOAD_TOO_LARGE = 413
        const val UNAUTHORIZED = 401
    }
}
