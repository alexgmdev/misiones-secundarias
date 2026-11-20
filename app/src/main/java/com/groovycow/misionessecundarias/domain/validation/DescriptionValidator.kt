package com.groovycow.misionessecundarias.domain.validation

/** Valida la descripción del perfil: opcional y de máximo 280 caracteres. */
class DescriptionValidator {
    /**
     * Valida la descripción.
     *
     * @param input Texto digitado (puede estar vacío).
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.DescriptionTooLong].
     */
    fun validate(input: String): ValidationResult =
        if (input.length > MAX_LENGTH) ValidationResult.Invalid(FieldError.DescriptionTooLong) else ValidationResult.Valid

    companion object {
        /** Longitud máxima permitida. */
        const val MAX_LENGTH = 280
    }
}
