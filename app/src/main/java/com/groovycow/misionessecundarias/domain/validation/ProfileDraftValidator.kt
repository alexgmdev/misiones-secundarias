package com.groovycow.misionessecundarias.domain.validation

import com.groovycow.misionessecundarias.domain.model.ProfileDraft

/** Campos del formulario de perfil. */
enum class ProfileField {
    /** Apodo. */
    Nickname,

    /** Género. */
    Gender,

    /** Descripción. */
    Description,

    /** Avatar predefinido. */
    Avatar,

    /** Foto de perfil. */
    Photo,
}

/**
 * Valida el formulario de perfil completo sin llamar al backend.
 *
 * @property nicknameValidator Validador del apodo.
 * @property descriptionValidator Validador de la descripción.
 */
class ProfileDraftValidator(
    private val nicknameValidator: NicknameValidator,
    private val descriptionValidator: DescriptionValidator,
) {
    /**
     * Valida el borrador.
     *
     * @param draft Datos digitados.
     * @param hasPhoto Si ya existe una foto (elegida ahora o guardada antes).
     * @return Errores por campo; vacío si el formulario es válido.
     */
    fun validate(
        draft: ProfileDraft,
        hasPhoto: Boolean,
    ): Map<ProfileField, FieldError> =
        buildMap {
            (nicknameValidator.validate(draft.nickname) as? ValidationResult.Invalid)?.let { put(ProfileField.Nickname, it.error) }
            if (draft.gender == null) put(ProfileField.Gender, FieldError.SelectionRequired)
            (descriptionValidator.validate(draft.description) as? ValidationResult.Invalid)?.let {
                put(ProfileField.Description, it.error)
            }
            if (draft.avatarId == null) put(ProfileField.Avatar, FieldError.SelectionRequired)
            if (!hasPhoto) put(ProfileField.Photo, FieldError.SelectionRequired)
        }
}
