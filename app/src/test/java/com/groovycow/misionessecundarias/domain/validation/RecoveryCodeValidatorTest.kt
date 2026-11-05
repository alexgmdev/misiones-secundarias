package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryCodeValidatorTest {
    private val validator = RecoveryCodeValidator()

    @Test
    fun numericCodesOfSixToTenDigitsAreValid() {
        listOf("849201", "1234567890", " 849201 ").forEach {
            assertEquals(it, ValidationResult.Valid, validator.validate(it))
        }
    }

    @Test
    fun emptyOrBlankCodeIsInvalid() {
        assertEquals(ValidationResult.Invalid(FieldError.InvalidCode), validator.validate(""))
        assertEquals(ValidationResult.Invalid(FieldError.InvalidCode), validator.validate("   "))
    }

    @Test
    fun nonNumericOrOutOfRangeCodeIsInvalid() {
        listOf("84a201", "84 201", "12345", "12345678901", "-84920").forEach {
            assertEquals(it, ValidationResult.Invalid(FieldError.InvalidCode), validator.validate(it))
        }
    }

    @Test
    fun normalizeTrimsSurroundingSpaces() {
        assertEquals("849201", validator.normalize("  849201 "))
    }
}
