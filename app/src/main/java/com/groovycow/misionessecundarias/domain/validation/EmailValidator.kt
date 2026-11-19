package com.groovycow.misionessecundarias.domain.validation

/** Valida el formato del correo electrónico (`local@dominio.tld`) tras quitar espacios laterales. */
class EmailValidator {
    /**
     * Normaliza el correo quitando espacios laterales.
     *
     * @param input Texto digitado.
     * @return Correo sin espacios al inicio ni al final.
     */
    fun normalize(input: String): String = input.trim()

    /**
     * Valida el correo.
     *
     * @param input Texto digitado.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required] o [FieldError.InvalidEmail].
     */
    fun validate(input: String): ValidationResult {
        val email = normalize(input)
        return when {
            email.isEmpty() -> ValidationResult.Invalid(FieldError.Required)
            !EMAIL_REGEX.matches(email) -> ValidationResult.Invalid(FieldError.InvalidEmail)
            else -> ValidationResult.Valid
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
