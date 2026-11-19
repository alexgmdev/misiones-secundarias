package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.ProfileUpdate
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import com.groovycow.misionessecundarias.domain.validation.DescriptionValidator
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.NicknameValidator
import com.groovycow.misionessecundarias.domain.validation.ProfileDraftValidator
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class UpdateProfileUseCaseTest {
    private val repository = mockk<ProfileRepository>()
    private val useCase = UpdateProfileUseCase(repository, ProfileDraftValidator(NicknameValidator(), DescriptionValidator()))
    private val draft = ProfileDraft(" Aventurero ", Gender.MASCULINO, "  ", AvatarId.entries[1])

    private fun errorOf(result: Result<Unit>): ProfileError = (result.exceptionOrNull() as ProfileException).error

    @Test
    fun invalidFieldsFailWithoutBackendCalls() =
        runTest {
            val result = useCase(ProfileDraft("", null, "", null))
            val errors = (result.exceptionOrNull() as ProfileValidationException).errors
            assertEquals(FieldError.Required, errors[ProfileField.Nickname])
            assertEquals(setOf(ProfileField.Nickname, ProfileField.Gender, ProfileField.Avatar), errors.keys)
            coVerify(exactly = 0) { repository.isNicknameAvailable(any()) }
            coVerify(exactly = 0) { repository.updateProfile(any()) }
        }

    @Test
    fun nicknameOfAnotherUserIsRejected() =
        runTest {
            coEvery { repository.isNicknameAvailable("Aventurero") } returns false
            assertEquals(ProfileError.NicknameTaken, errorOf(useCase(draft)))
            coVerify(exactly = 0) { repository.updateProfile(any()) }
        }

    @Test
    fun ownNicknameIsAvailableAndUpdateIsSentTrimmedWithoutPhoto() =
        runTest {
            val sent = slot<ProfileUpdate>()
            coEvery { repository.isNicknameAvailable("Aventurero") } returns true
            coEvery { repository.updateProfile(capture(sent)) } returns Unit
            assertEquals(Result.success(Unit), useCase(draft))
            assertEquals(ProfileUpdate("Aventurero", Gender.MASCULINO, null, AvatarId.entries[1], null), sent.captured)
        }

    @Test
    fun newPhotoUrlIsForwarded() =
        runTest {
            val sent = slot<ProfileUpdate>()
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.updateProfile(capture(sent)) } returns Unit
            useCase(draft, newPhotoUrl = "u?v=1")
            assertEquals("u?v=1", sent.captured.photoUrl)
        }

    @Test
    fun descriptionIsKeptWhenPresent() =
        runTest {
            val sent = slot<ProfileUpdate>()
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.updateProfile(capture(sent)) } returns Unit
            useCase(draft.copy(description = "Hola"))
            assertEquals("Hola", sent.captured.description)
            assertNull(null)
        }

    @Test
    fun networkErrorIsPropagated() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } throws ProfileException(ProfileError.NoConnection)
            assertEquals(ProfileError.NoConnection, errorOf(useCase(draft)))
        }

    @Test
    fun checksAvailabilityBeforeUpdating() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.updateProfile(any()) } returns Unit
            useCase(draft)
            coVerifyOrder {
                repository.isNicknameAvailable(any())
                repository.updateProfile(any())
            }
        }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } throws CancellationException("c")
            useCase(draft)
            Unit
        }
}
