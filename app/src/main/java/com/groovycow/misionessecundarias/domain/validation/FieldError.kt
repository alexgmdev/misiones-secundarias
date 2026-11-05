package com.groovycow.misionessecundarias.domain.validation

/** Errores de validación por campo; la capa de UI los traduce a mensajes en español. */
enum class FieldError {
    /** El campo está vacío o solo tiene espacios. */
    Required,

    /** El nombre o apellido supera la longitud máxima. */
    NameTooLong,

    /** El correo no tiene formato `local@dominio.tld`. */
    InvalidEmail,

    /** El celular no cumple `^3\d{9}$` tras normalizar. */
    InvalidPhone,

    /** La contraseña tiene menos de 8 caracteres. */
    PasswordTooShort,

    /** La contraseña no incluye mayúscula, minúscula y número. */
    PasswordMissingClasses,

    /** La confirmación no coincide con la contraseña. */
    PasswordMismatch,

    /** La fecha de nacimiento es posterior a hoy. */
    FutureBirthDate,

    /** El usuario no tiene 18 años cumplidos. */
    UnderAge,

    /** El código de recuperación está vacío o no es numérico (6 a 10 dígitos). */
    InvalidCode,
}
