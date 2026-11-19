package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class DescriptionValidatorTest {
    private val validator = DescriptionValidator()

    @Test
    fun emptyIsAllowed() {
        assertEquals(ValidationResult.Valid, validator.validate(""))
    }

    @Test
    fun boundaries() {
        assertEquals(ValidationResult.Valid, validator.validate("a".repeat(280)))
        assertEquals(ValidationResult.Invalid(FieldError.DescriptionTooLong), validator.validate("a".repeat(281)))
    }
}
