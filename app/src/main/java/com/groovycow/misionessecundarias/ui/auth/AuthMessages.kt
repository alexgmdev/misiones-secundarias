package com.groovycow.misionessecundarias.ui.auth

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.validation.FieldError

/** Confirmación única tras solicitar el código de recuperación; idéntica exista o no la cuenta. */
const val RECOVERY_CODE_SENT_MESSAGE = "Si el correo está registrado, recibirás un código"

/**
 * Traduce un [FieldError] al mensaje en español que se muestra bajo el campo.
 *
 * @return Texto para el usuario, sin detalles técnicos.
 */
@Suppress("CyclomaticComplexMethod")
fun FieldError.toMessage(): String =
    when (this) {
        FieldError.Required -> "Este campo es obligatorio"
        FieldError.NameTooLong -> "Máximo 60 caracteres"
        FieldError.InvalidEmail -> "Correo electrónico inválido"
        FieldError.InvalidPhone -> "Celular inválido: debe iniciar en 3 y tener 10 dígitos"
        FieldError.PasswordTooShort -> "La contraseña debe tener al menos 8 caracteres"
        FieldError.PasswordMissingClasses -> "La contraseña debe incluir mayúscula, minúscula y número"
        FieldError.PasswordMismatch -> "Las contraseñas no coinciden"
        FieldError.FutureBirthDate -> "La fecha de nacimiento no puede ser futura"
        FieldError.UnderAge -> "Debes ser mayor de 18 años"
        FieldError.InvalidCode -> "Ingresa el código numérico"
        FieldError.NicknameLength -> "El nickname debe tener entre 3 y 20 caracteres"
        FieldError.NicknameInvalidChars -> "El nickname solo admite letras, números y guion bajo"
        FieldError.DescriptionTooLong -> "Máximo 280 caracteres"
        FieldError.SelectionRequired -> "Este campo es obligatorio"
    }

/**
 * Traduce un [AuthError] al mensaje en español que se muestra al usuario.
 *
 * @return Texto para el usuario, sin detalles técnicos.
 */
fun AuthError.toMessage(): String =
    when (this) {
        AuthError.InvalidCredentials -> "Credenciales incorrectas"
        AuthError.IncorrectCurrentPassword -> "La contraseña actual es incorrecta"
        AuthError.EmailInUse -> "El correo ya está en uso"
        AuthError.PhoneInUse -> "El celular ya está en uso"
        AuthError.WeakPassword -> "La contraseña es demasiado débil"
        AuthError.InvalidOrExpiredCode -> "Código inválido o expirado"
        AuthError.SamePassword -> "La nueva contraseña debe ser diferente a la anterior"
        AuthError.NoConnection -> "Sin conexión. Intenta de nuevo"
        AuthError.ServerError -> "Ocurrió un error. Intenta de nuevo"
    }
