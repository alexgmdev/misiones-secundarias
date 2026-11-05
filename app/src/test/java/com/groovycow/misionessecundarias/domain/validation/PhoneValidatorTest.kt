package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneValidatorTest {
    private val validator = PhoneValidator()

    @Test
    fun validPhoneIsUnchanged() {
        assertEquals("3001234567", validator.normalize("3001234567"))
    }

    @Test
    fun freeFormatWithPrefixSpacesAndDashesIsNormalized() {
        assertEquals("3001234567", validator.normalize("+57 300-123-4567"))
    }

    @Test
    fun prefixWithoutPlusIsRemoved() {
        assertEquals("3001234567", validator.normalize("573001234567"))
    }

    @Test
    fun phoneNotStartingWithThreeIsRejected() {
        assertNull(validator.normalize("2001234567"))
    }

    @Test
    fun shortPhoneIsRejected() {
        assertNull(validator.normalize("300123"))
    }

    @Test
    fun nonDigitCharactersAreRejected() {
        assertNull(validator.normalize("300abc4567"))
    }

    @Test
    fun nullInputIsRejected() {
        assertNull(validator.normalize(null))
    }

    @Test
    fun blankPhoneIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate("  "))
    }

    @Test
    fun invalidPhoneReportsInvalidPhone() {
        assertEquals(ValidationResult.Invalid(FieldError.InvalidPhone), validator.validate("2001234567"))
        assertEquals(ValidationResult.Invalid(FieldError.InvalidPhone), validator.validate("300123"))
    }

    @Test
    fun freeFormatPhoneIsValid() {
        assertEquals(ValidationResult.Valid, validator.validate("+57 300-123-4567"))
    }
}
