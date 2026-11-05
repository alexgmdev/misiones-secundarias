package com.groovycow.misionessecundarias.domain.validation

/** Valida que el código de recuperación sea numérico (de 6 a 10 dígitos) tras quitar espacios laterales. */
class RecoveryCodeValidator {
    /**
     * Normaliza el código quitando espacios laterales.
     *
     * @param input Texto digitado.
     * @return Código sin espacios al inicio ni al final.
     */
    fun normalize(input: String): String = input.trim()

    /**
     * Valida el código.
     *
     * @param input Texto digitado.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.InvalidCode].
     */
    fun validate(input: String): ValidationResult =
        if (CODE_REGEX.matches(normalize(input))) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(FieldError.InvalidCode)
        }

    private companion object {
        val CODE_REGEX = Regex("^\\d{6,10}$")
    }
}
