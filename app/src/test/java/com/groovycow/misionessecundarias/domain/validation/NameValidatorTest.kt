package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class NameValidatorTest {
    private val validator = NameValidator()

    @Test
    fun emptyNameIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate(""))
    }

    @Test
    fun blankNameIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate("   "))
    }

    @Test
    fun nameWithSurroundingSpacesIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate("  Juan "))
    }

    @Test
    fun nameOfSixtyCharactersIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate("a".repeat(60)))
    }

    @Test
    fun nameLongerThanSixtyCharactersIsTooLong() {
        assertEquals(ValidationResult.Invalid(FieldError.NameTooLong), validator.validate("a".repeat(61)))
    }

    @Test
    fun trailingSpacesDoNotCountTowardsLimit() {
        assertEquals(ValidationResult.Valid, validator.validate("a".repeat(60) + "   "))
    }
}
