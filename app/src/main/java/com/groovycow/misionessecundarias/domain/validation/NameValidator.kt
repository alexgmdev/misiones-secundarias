package com.groovycow.misionessecundarias.domain.validation

/** Valida nombres y apellidos: obligatorios tras quitar espacios y de máximo 60 caracteres. */
class NameValidator {
    /**
     * Valida el nombre o apellido.
     *
     * @param input Texto digitado.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required] o [FieldError.NameTooLong].
     */
    fun validate(input: String): ValidationResult {
        val name = input.trim()
        return when {
            name.isEmpty() -> ValidationResult.Invalid(FieldError.Required)
            name.length > MAX_LENGTH -> ValidationResult.Invalid(FieldError.NameTooLong)
            else -> ValidationResult.Valid
        }
    }

    private companion object {
        const val MAX_LENGTH = 60
    }
}
