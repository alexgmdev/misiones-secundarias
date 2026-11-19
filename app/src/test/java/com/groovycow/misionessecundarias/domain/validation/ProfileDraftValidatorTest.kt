package com.groovycow.misionessecundarias.domain.validation

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDraftValidatorTest {
    private val validator = ProfileDraftValidator(NicknameValidator(), DescriptionValidator())
    private val valid = ProfileDraft("Aventurero", Gender.FEMENINO, "", AvatarId.entries.first())

    @Test
    fun emptyDraftReportsEveryRequiredField() {
        val errors = validator.validate(ProfileDraft("", null, "", null), hasPhoto = false)
        assertEquals(
            mapOf(
                ProfileField.Nickname to FieldError.Required,
                ProfileField.Gender to FieldError.SelectionRequired,
                ProfileField.Avatar to FieldError.SelectionRequired,
                ProfileField.Photo to FieldError.SelectionRequired,
            ),
            errors,
        )
    }

    @Test
    fun validDraftHasNoErrorsAndDescriptionIsOptional() {
        assertTrue(validator.validate(valid, hasPhoto = true).isEmpty())
    }

    @Test
    fun photoNotRequiredWhenCallerSaysSo() {
        assertTrue(validator.validate(valid, hasPhoto = true).isEmpty())
        assertEquals(setOf(ProfileField.Photo), validator.validate(valid, hasPhoto = false).keys)
    }

    @Test
    fun reportsNicknameAndDescriptionErrors() {
        val errors = validator.validate(valid.copy(nickname = "ab", description = "x".repeat(281)), hasPhoto = true)
        assertEquals(FieldError.NicknameLength, errors[ProfileField.Nickname])
        assertEquals(FieldError.DescriptionTooLong, errors[ProfileField.Description])
    }
}
