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
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class CompleteInitialProfileUseCaseTest {
    private val repository = mockk<ProfileRepository>()
    private val useCase =
        CompleteInitialProfileUseCase(repository, ProfileDraftValidator(NicknameValidator(), DescriptionValidator()))
    private val draft = ProfileDraft("AventureroCali", Gender.FEMENINO, "", AvatarId.entries.first())
    private val photo = byteArrayOf(1, 2, 3)

    private fun verifyNoBackendCalls() {
        coVerify(exactly = 0) { repository.isNicknameAvailable(any()) }
        coVerify(exactly = 0) { repository.uploadPhoto(any()) }
        coVerify(exactly = 0) { repository.updateProfile(any()) }
    }

    @Test
    fun missingFieldsFailWithoutBackend() =
        runTest {
            val result = useCase(ProfileDraft("", null, "", null), null)
            val errors = (result.exceptionOrNull() as ProfileValidationException).errors
            assertEquals(FieldError.SelectionRequired, errors[ProfileField.Photo])
            assertEquals(4, errors.size)
            verifyNoBackendCalls()
        }

    @Test
    fun nicknameTakenStopsBeforeUploading() =
        runTest {
            coEvery { repository.isNicknameAvailable("AventureroCali") } returns false
            val result = useCase(draft, photo)
            assertEquals(ProfileError.NicknameTaken, (result.exceptionOrNull() as ProfileException).error)
            coVerify(exactly = 0) { repository.uploadPhoto(any()) }
            coVerify(exactly = 0) { repository.updateProfile(any()) }
        }

    @Test
    fun successValidatesThenChecksThenUploadsThenUpdates() =
        runTest {
            val sent = slot<ProfileUpdate>()
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.uploadPhoto(photo) } returns "https://x/foto.jpg?v=5"
            coEvery { repository.updateProfile(capture(sent)) } returns Unit
            assertEquals(Result.success(Unit), useCase(draft, photo))
            coVerifyOrder {
                repository.isNicknameAvailable("AventureroCali")
                repository.uploadPhoto(photo)
                repository.updateProfile(any())
            }
            assertEquals(
                ProfileUpdate("AventureroCali", Gender.FEMENINO, null, AvatarId.entries.first(), "https://x/foto.jpg?v=5"),
                sent.captured,
            )
        }

    @Test
    fun uploadFailureDoesNotUpdateProfile() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.uploadPhoto(any()) } throws ProfileException(ProfileError.NoConnection)
            val result = useCase(draft, photo)
            assertEquals(ProfileError.NoConnection, (result.exceptionOrNull() as ProfileException).error)
            coVerify(exactly = 0) { repository.updateProfile(any()) }
        }

    @Test
    fun serverErrorOnUpdateIsPropagated() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } returns true
            coEvery { repository.uploadPhoto(any()) } returns "u"
            coEvery { repository.updateProfile(any()) } throws ProfileException(ProfileError.ServerError)
            val result = useCase(draft, photo)
            assertEquals(ProfileError.ServerError, (result.exceptionOrNull() as ProfileException).error)
        }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() =
        runTest {
            coEvery { repository.isNicknameAvailable(any()) } throws CancellationException("c")
            useCase(draft, photo)
            Unit
        }
}
