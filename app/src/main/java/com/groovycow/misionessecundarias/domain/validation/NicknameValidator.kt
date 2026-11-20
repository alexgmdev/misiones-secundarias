package com.groovycow.misionessecundarias.domain.validation

/** Valida el nickname: obligatorio, de 3 a 20 caracteres y solo letras, números y guion bajo. */
class NicknameValidator {
    /**
     * Normaliza el nickname quitando espacios laterales.
     *
     * @param input Texto digitado.
     */
    fun normalize(input: String): String = input.trim()

    /**
     * Valida el nickname (la unicidad se comprueba en el backend).
     *
     * @param input Texto digitado.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required],
     * [FieldError.NicknameLength] o [FieldError.NicknameInvalidChars].
     */
    fun validate(input: String): ValidationResult {
        val nickname = normalize(input)
        return when {
            nickname.isEmpty() -> ValidationResult.Invalid(FieldError.Required)
            nickname.length !in LENGTH_RANGE -> ValidationResult.Invalid(FieldError.NicknameLength)
            !CHARS_REGEX.matches(nickname) -> ValidationResult.Invalid(FieldError.NicknameInvalidChars)
            else -> ValidationResult.Valid
        }
    }

    private companion object {
        val LENGTH_RANGE = 3..20
        val CHARS_REGEX = Regex("^[A-Za-z0-9_]+$")
    }
}
