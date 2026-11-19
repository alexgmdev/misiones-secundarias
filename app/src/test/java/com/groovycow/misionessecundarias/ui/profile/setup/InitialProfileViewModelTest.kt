package com.groovycow.misionessecundarias.ui.profile.setup

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.CompleteInitialProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.CompressProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.usecase.ProfileValidationException
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InitialProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val complete = mockk<CompleteInitialProfileUseCase>()
    private val compress = mockk<CompressProfilePhotoUseCase>()
    private val jpeg = byteArrayOf(1, 2, 3)

    private fun viewModel() = InitialProfileViewModel(complete, compress)

    private fun InitialProfileViewModel.fillValid() {
        onNicknameChange("AventureroCali")
        onGenderChange(Gender.FEMENINO)
        onAvatarChange(AvatarId.SOL)
        onPhotoPicked("content://foto")
    }

    @Test
    fun saveWithoutDataShowsFieldErrorsWithoutBackend() {
        coEvery { complete(any(), any()) } returns
            Result.failure(
                ProfileValidationException(
                    mapOf(
                        ProfileField.Nickname to FieldError.Required,
                        ProfileField.Gender to FieldError.SelectionRequired,
                        ProfileField.Avatar to FieldError.SelectionRequired,
                        ProfileField.Photo to FieldError.SelectionRequired,
                    ),
                ),
            )
        val vm = viewModel()

        vm.save()

        val state = vm.uiState.value
        assertEquals(4, state.fieldErrors.size)
        assertEquals(FieldError.SelectionRequired, state.fieldErrors[ProfileField.Photo])
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
    }

    @Test
    fun editingAFieldClearsItsError() {
        coEvery { complete(any(), any()) } returns
            Result.failure(ProfileValidationException(mapOf(ProfileField.Nickname to FieldError.Required)))
        val vm = viewModel()
        vm.save()

        vm.onNicknameChange("ab")

        assertNull(vm.uiState.value.fieldErrors[ProfileField.Nickname])
    }

    @Test
    fun saveSuccessEmitsSavedAndSendsDraftWithPhoto() {
        coEvery { compress("content://foto") } returns Result.success(jpeg)
        coEvery { complete(any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.fillValid()
        vm.onDescriptionChange("")

        vm.save()

        assertTrue(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.isLoading)
        coVerify {
            complete(ProfileDraft("AventureroCali", Gender.FEMENINO, "", AvatarId.SOL), jpeg)
        }
    }

    @Test
    fun nicknameTakenKeepsDataAndShowsError() {
        coEvery { compress(any()) } returns Result.success(jpeg)
        coEvery { complete(any(), any()) } returns Result.failure(ProfileException(ProfileError.NicknameTaken))
        val vm = viewModel()
        vm.fillValid()

        vm.save()

        val state = vm.uiState.value
        assertEquals(ProfileError.NicknameTaken, state.error)
        assertEquals("AventureroCali", state.nickname)
        assertEquals(AvatarId.SOL, state.avatarId)
        assertFalse(state.isSaved)
    }

    @Test
    fun serverErrorKeepsData() {
        coEvery { compress(any()) } returns Result.success(jpeg)
        coEvery { complete(any(), any()) } returns Result.failure(RuntimeException("boom"))
        val vm = viewModel()
        vm.fillValid()

        vm.save()

        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
        assertEquals("AventureroCali", vm.uiState.value.nickname)
        assertEquals("content://foto", vm.uiState.value.photoUri)
    }

    @Test
    fun noConnectionIsReported() {
        coEvery { compress(any()) } returns Result.success(jpeg)
        coEvery { complete(any(), any()) } returns Result.failure(ProfileException(ProfileError.NoConnection))
        val vm = viewModel()
        vm.fillValid()

        vm.save()

        assertEquals(ProfileError.NoConnection, vm.uiState.value.error)
    }

    @Test
    fun unsupportedPhotoFormatShowsPhotoError() {
        coEvery { compress(any()) } returns Result.failure(ProfileException(ProfileError.UnsupportedPhotoFormat))
        val vm = viewModel()

        vm.onPhotoPicked("content://gif")

        assertEquals(ProfileError.UnsupportedPhotoFormat, vm.uiState.value.photoError)
        assertNull(vm.uiState.value.photoUri)
    }

    @Test
    fun photoTooLargeShowsPhotoError() {
        coEvery { compress(any()) } returns Result.failure(ProfileException(ProfileError.PhotoTooLarge))
        val vm = viewModel()

        vm.onPhotoPicked("content://enorme")

        assertEquals(ProfileError.PhotoTooLarge, vm.uiState.value.photoError)
    }

    @Test
    fun validPhotoClearsPreviousPhotoError() {
        coEvery { compress("content://gif") } returns Result.failure(ProfileException(ProfileError.UnsupportedPhotoFormat))
        coEvery { compress("content://ok") } returns Result.success(jpeg)
        val vm = viewModel()
        vm.onPhotoPicked("content://gif")

        vm.onPhotoPicked("content://ok")

        assertNull(vm.uiState.value.photoError)
        assertEquals("content://ok", vm.uiState.value.photoUri)
    }

    @Test
    fun failedPickKeepsPreviousValidPhoto() {
        coEvery { compress("content://ok") } returns Result.success(jpeg)
        coEvery { compress("content://gif") } returns Result.failure(ProfileException(ProfileError.UnsupportedPhotoFormat))
        val vm = viewModel()
        vm.onPhotoPicked("content://ok")

        vm.onPhotoPicked("content://gif")

        assertEquals("content://ok", vm.uiState.value.photoUri)
        assertEquals(ProfileError.UnsupportedPhotoFormat, vm.uiState.value.photoError)
    }

    @Test
    fun isLoadingPreventsDoubleSubmit() {
        coEvery { compress(any()) } returns Result.success(jpeg)
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { complete(any(), any()) } coAnswers { gate.await() }
        val vm = viewModel()
        vm.fillValid()

        vm.save()
        assertTrue(vm.uiState.value.isLoading)
        vm.save()
        gate.complete(Result.success(Unit))

        coVerify(exactly = 1) { complete(any(), any()) }
        assertTrue(vm.uiState.value.isSaved)
    }
}
