package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class NicknameValidatorTest {
    private val validator = NicknameValidator()

    private fun invalid(error: FieldError) = ValidationResult.Invalid(error)

    @Test
    fun emptyOrBlankIsRequired() {
        assertEquals(invalid(FieldError.Required), validator.validate(""))
        assertEquals(invalid(FieldError.Required), validator.validate("   "))
    }

    @Test
    fun lengthBoundaries() {
        assertEquals(invalid(FieldError.NicknameLength), validator.validate("ab"))
        assertEquals(ValidationResult.Valid, validator.validate("abc"))
        assertEquals(ValidationResult.Valid, validator.validate("a".repeat(20)))
        assertEquals(invalid(FieldError.NicknameLength), validator.validate("a".repeat(21)))
    }

    @Test
    fun invalidCharactersAreRejected() {
        assertEquals(invalid(FieldError.NicknameInvalidChars), validator.validate("Aventurero Cali!"))
        assertEquals(invalid(FieldError.NicknameInvalidChars), validator.validate("ñandú_123"))
        assertEquals(invalid(FieldError.NicknameInvalidChars), validator.validate("ab-cd"))
    }

    @Test
    fun lettersDigitsAndUnderscoreAreValid() {
        assertEquals(ValidationResult.Valid, validator.validate("Aventurero_Cali9"))
    }

    @Test
    fun surroundingSpacesAreTrimmed() {
        assertEquals("abc", validator.normalize("  abc "))
        assertEquals(ValidationResult.Valid, validator.validate(" abc "))
    }
}
