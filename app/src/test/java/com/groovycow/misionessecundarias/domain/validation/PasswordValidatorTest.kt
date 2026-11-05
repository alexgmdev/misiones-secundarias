package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordValidatorTest {
    private val validator = PasswordValidator()

    @Test
    fun validPasswordHasNoUnmetRules() {
        assertEquals(emptySet<PasswordRule>(), validator.unmetRules("Clave1234"))
    }

    @Test
    fun shortPasswordUnmetRulesIncludeLength() {
        assertEquals(setOf(PasswordRule.MIN_LENGTH), validator.unmetRules("Ab1cdef"))
    }

    @Test
    fun missingUppercaseIsReported() {
        assertEquals(setOf(PasswordRule.UPPERCASE), validator.unmetRules("abcdefg1"))
    }

    @Test
    fun missingLowercaseIsReported() {
        assertEquals(setOf(PasswordRule.LOWERCASE), validator.unmetRules("ABCDEFG1"))
    }

    @Test
    fun missingDigitIsReported() {
        assertEquals(setOf(PasswordRule.DIGIT), validator.unmetRules("Abcdefgh"))
    }

    @Test
    fun emptyPasswordIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validate(""))
    }

    @Test
    fun tooShortPasswordReportsLengthError() {
        assertEquals(ValidationResult.Invalid(FieldError.PasswordTooShort), validator.validate("Ab1"))
    }

    @Test
    fun passwordsMissingAClassReportClassesError() {
        listOf("abcdefg1", "ABCDEFG1", "Abcdefgh").forEach {
            assertEquals(ValidationResult.Invalid(FieldError.PasswordMissingClasses), validator.validate(it))
        }
    }

    @Test
    fun validPasswordIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validate("Clave1234"))
    }

    @Test
    fun matchingConfirmationIsAccepted() {
        assertEquals(ValidationResult.Valid, validator.validateConfirmation("Clave1234", "Clave1234"))
    }

    @Test
    fun confirmationIsCaseSensitive() {
        assertEquals(
            ValidationResult.Invalid(FieldError.PasswordMismatch),
            validator.validateConfirmation("Clave1234", "clave1234"),
        )
    }

    @Test
    fun emptyConfirmationIsRequired() {
        assertEquals(ValidationResult.Invalid(FieldError.Required), validator.validateConfirmation("Clave1234", ""))
    }
}
