package com.groovycow.misionessecundarias.domain.validation

/** Resultado de validar un campo de formulario. */
sealed interface ValidationResult {
    /** El valor es válido. */
    data object Valid : ValidationResult

    /**
     * El valor es inválido.
     *
     * @property error Motivo del rechazo.
     */
    data class Invalid(val error: FieldError) : ValidationResult
}
