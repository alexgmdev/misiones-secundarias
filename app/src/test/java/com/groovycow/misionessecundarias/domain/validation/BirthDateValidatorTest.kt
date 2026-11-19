package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BirthDateValidatorTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC)
    private val validator = BirthDateValidator(clock)

    @Test
    fun missingDateIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate(null))
    }

    @Test
    fun seventeenYearsAnd364DaysIsUnderAge() {
        assertEquals(ValidationResult.Invalid(FieldError.UnderAge), validator.validate(LocalDate.of(2008, 11, 4)))
    }

    @Test
    fun futureDateIsRejected() {
        assertEquals(ValidationResult.Invalid(FieldError.FutureBirthDate), validator.validate(LocalDate.of(2026, 11, 4)))
    }

    @Test
    fun exactlyEighteenTodayIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate(LocalDate.of(2008, 11, 3)))
    }

    @Test
    fun adultIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate(LocalDate.of(1990, 1, 15)))
    }
}
