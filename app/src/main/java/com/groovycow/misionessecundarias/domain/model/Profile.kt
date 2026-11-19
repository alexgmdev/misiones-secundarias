package com.groovycow.misionessecundarias.domain.model

/**
 * Perfil del usuario autenticado (incluye el estado de completitud).
 *
 * @property id Identificador del usuario.
 * @property nickname Apodo público, o null si aún no lo define.
 * @property gender Género, o null.
 * @property description Descripción opcional (máximo 280 caracteres).
 * @property avatarId Avatar predefinido, o null.
 * @property photoUrl URL pública de la foto, o null.
 * @property isComplete Verdadero cuando nickname, género, avatar y foto están definidos.
 */
data class Profile(
    val id: String,
    val nickname: String?,
    val gender: Gender?,
    val description: String?,
    val avatarId: AvatarId?,
    val photoUrl: String?,
    val isComplete: Boolean,
)

/**
 * Perfil visible para otros usuarios: solo los campos públicos.
 *
 * @property id Identificador del usuario.
 * @property nickname Apodo público, o null.
 * @property gender Género, o null.
 * @property description Descripción, o null.
 * @property avatarId Avatar predefinido, o null.
 * @property photoUrl URL pública de la foto, o null.
 */
data class PublicProfile(
    val id: String,
    val nickname: String?,
    val gender: Gender?,
    val description: String?,
    val avatarId: AvatarId?,
    val photoUrl: String?,
)

/**
 * Datos capturados en el formulario de perfil (sin validar).
 *
 * @property nickname Apodo digitado.
 * @property gender Género elegido, o null.
 * @property description Descripción digitada (puede estar vacía).
 * @property avatarId Avatar elegido, o null.
 */
data class ProfileDraft(
    val nickname: String,
    val gender: Gender?,
    val description: String,
    val avatarId: AvatarId?,
)

/**
 * Datos validados que se escriben en `profiles`.
 *
 * @property nickname Apodo sin espacios laterales.
 * @property gender Género.
 * @property description Descripción, o null si quedó vacía.
 * @property avatarId Avatar.
 * @property photoUrl Nueva URL de foto, o null para no modificarla.
 */
data class ProfileUpdate(
    val nickname: String,
    val gender: Gender,
    val description: String?,
    val avatarId: AvatarId,
    val photoUrl: String?,
)
