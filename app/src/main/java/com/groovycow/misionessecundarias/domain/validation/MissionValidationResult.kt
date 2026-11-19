package com.groovycow.misionessecundarias.domain.validation

import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField

/** Resultado de validar un borrador con errores indexados por campo. */
data class MissionValidationResult(
    /** Errores encontrados; vacío cuando el borrador es válido. */
    val fieldErrors: Map<MissionField, MissionError>,
) {
    /** Indica si el borrador puede guardarse. */
    val isValid: Boolean
        get() = fieldErrors.isEmpty()
}
