package com.groovycow.misionessecundarias.domain.validation

import java.time.Clock
import java.time.LocalDate

/**
 * Valida la fecha de nacimiento: obligatoria, no futura y con 18 años cumplidos a la fecha del [clock].
 *
 * @property clock Reloj usado para obtener "hoy" (inyectable para pruebas deterministas).
 */
class BirthDateValidator(private val clock: Clock) {
    /**
     * Valida la fecha de nacimiento.
     *
     * @param birthDate Fecha elegida, o null si no se eligió.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required],
     * [FieldError.FutureBirthDate] o [FieldError.UnderAge].
     */
    fun validate(birthDate: LocalDate?): ValidationResult {
        val today = LocalDate.now(clock)
        return when {
            birthDate == null -> ValidationResult.Invalid(FieldError.Required)
            birthDate.isAfter(today) -> ValidationResult.Invalid(FieldError.FutureBirthDate)
            birthDate.isAfter(today.minusYears(MIN_AGE_YEARS)) -> ValidationResult.Invalid(FieldError.UnderAge)
            else -> ValidationResult.Valid
        }
    }

    private companion object {
        const val MIN_AGE_YEARS = 18L
    }
}
