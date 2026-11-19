package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailValidatorTest {
    private val validator = EmailValidator()

    @Test
    fun emptyEmailIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate(""))
    }

    @Test
    fun blankEmailIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate("   "))
    }

    @Test
    fun emailWithoutDomainIsInvalid() {
        assertEquals(ValidationResult.Invalid(FieldError.InvalidEmail), validator.validate("juan@"))
    }

    @Test
    fun emailWithoutTldIsInvalid() {
        assertEquals(ValidationResult.Invalid(FieldError.InvalidEmail), validator.validate("juan@example"))
    }

    @Test
    fun emailWithInnerSpacesIsInvalid() {
        assertEquals(ValidationResult.Invalid(FieldError.InvalidEmail), validator.validate("ju an@example.com"))
    }

    @Test
    fun validEmailIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate("juan@example.com"))
    }

    @Test
    fun emailWithSurroundingSpacesIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate(" juan@example.com "))
    }

    @Test
    fun normalizeTrimsSurroundingSpaces() {
        assertEquals("juan@example.com", validator.normalize(" juan@example.com "))
    }
}
