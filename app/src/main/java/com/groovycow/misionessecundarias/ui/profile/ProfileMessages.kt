package com.groovycow.misionessecundarias.ui.profile

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField

/** Textos en español de los errores y etiquetas del perfil. */
object ProfileMessages {
    /**
     * Mensaje de un error de validación de campo.
     *
     * @param field Campo del formulario.
     * @param error Error de validación.
     */
    fun fieldError(
        field: ProfileField,
        error: FieldError,
    ): String =
        when (error) {
            FieldError.NicknameLength -> "El nickname debe tener entre 3 y 20 caracteres"
            FieldError.NicknameInvalidChars -> "El nickname solo admite letras, números y guion bajo"
            FieldError.DescriptionTooLong -> "Máximo 280 caracteres"
            FieldError.SelectionRequired ->
                when (field) {
                    ProfileField.Gender -> "Selecciona tu género"
                    ProfileField.Avatar -> "Elige un avatar"
                    else -> "Agrega una foto de perfil"
                }
            else -> "Este campo es obligatorio"
        }

    /**
     * Mensaje de un error del perfil o la foto.
     *
     * @param error Error tipado.
     */
    fun error(error: ProfileError): String =
        when (error) {
            ProfileError.NicknameTaken -> "El nickname ya está en uso"
            ProfileError.UnsupportedPhotoFormat -> "Formato no soportado. Usa JPG o PNG"
            ProfileError.PhotoTooLarge -> "La foto supera 2 MB"
            ProfileError.NoConnection -> "Sin conexión. Intenta de nuevo"
            ProfileError.SessionRequired, ProfileError.ServerError -> "Ocurrió un error. Intenta de nuevo"
        }

    /**
     * Etiqueta del género.
     *
     * @param gender Género.
     */
    fun genderLabel(gender: Gender): String =
        when (gender) {
            Gender.FEMENINO -> "Femenino"
            Gender.MASCULINO -> "Masculino"
            Gender.PREFIERO_NO_DECIR -> "Prefiero no decir"
        }

    /**
     * Descripción accesible del avatar.
     *
     * @param avatar Avatar del catálogo.
     */
    fun avatarLabel(avatar: AvatarId): String =
        when (avatar) {
            AvatarId.BOSQUE -> "Avatar bosque"
            AvatarId.RIO -> "Avatar río"
            AvatarId.SOL -> "Avatar sol"
            AvatarId.TIERRA -> "Avatar tierra"
            AvatarId.ESTRELLA -> "Avatar estrella"
            AvatarId.FLOR -> "Avatar flor"
            AvatarId.CORAZON -> "Avatar corazón"
            AvatarId.BRUJULA -> "Avatar brújula"
        }
}
