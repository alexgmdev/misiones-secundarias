package com.groovycow.misionessecundarias.domain.validation

/** Normaliza y valida celulares colombianos (`^3\d{9}$` tras quitar espacios, guiones y prefijo `+57`/`57`). */
class PhoneValidator {
    /**
     * Normaliza el celular quitando espacios, guiones, paréntesis y el prefijo opcional `+57`/`57`.
     *
     * @param input Texto digitado, o null.
     * @return El celular de 10 dígitos que inicia en 3, o null si no cumple el formato.
     */
    fun normalize(input: String?): String? {
        if (input == null) return null
        val compact = input.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
        val withoutPlus = compact.removePrefix("+")
        val local = if (withoutPlus.length == COUNTRY_PREFIX_TOTAL_LENGTH) withoutPlus.removePrefix(COUNTRY_PREFIX) else withoutPlus
        return local.takeIf { PHONE_REGEX.matches(it) }
    }

    /**
     * Valida el celular.
     *
     * @param input Texto digitado.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required] o [FieldError.InvalidPhone].
     */
    fun validate(input: String): ValidationResult =
        when {
            input.isBlank() -> ValidationResult.Invalid(FieldError.Required)
            normalize(input) == null -> ValidationResult.Invalid(FieldError.InvalidPhone)
            else -> ValidationResult.Valid
        }

    private companion object {
        const val COUNTRY_PREFIX = "57"
        const val COUNTRY_PREFIX_TOTAL_LENGTH = 12
        val PHONE_REGEX = Regex("^3\\d{9}$")
    }
}
