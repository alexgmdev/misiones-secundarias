package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileUpdate
import com.groovycow.misionessecundarias.domain.model.PublicProfile

/** Acceso a los perfiles. Las implementaciones lanzan [com.groovycow.misionessecundarias.domain.model.ProfileException] ante fallos. */
interface ProfileRepository {
    /** Indica si el usuario autenticado ya completó su perfil (`profile_completed`). */
    suspend fun isProfileComplete(): Boolean

    /** Devuelve el perfil del usuario autenticado. */
    suspend fun getMyProfile(): Profile

    /**
     * Indica si un nickname está libre para el usuario actual (su propio nickname cuenta como libre).
     *
     * @param nickname Apodo a consultar, sin espacios laterales.
     */
    suspend fun isNicknameAvailable(nickname: String): Boolean

    /**
     * Sube la foto de perfil (JPEG) a `{uid}/foto.jpg`, reemplazando la anterior.
     *
     * @param jpeg Bytes de una imagen JPEG de máximo 2 MB.
     * @return URL pública con el parámetro `?v=` para invalidar cachés.
     */
    suspend fun uploadPhoto(jpeg: ByteArray): String

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * @param update Datos validados; `photoUrl` nulo deja la foto sin cambios.
     */
    suspend fun updateProfile(update: ProfileUpdate)

    /**
     * Consulta el perfil público de otro usuario (solo columnas públicas).
     *
     * @param userId Identificador del usuario.
     */
    suspend fun getPublicProfile(userId: String): PublicProfile
}
