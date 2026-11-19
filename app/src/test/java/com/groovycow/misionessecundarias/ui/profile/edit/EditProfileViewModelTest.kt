package com.groovycow.misionessecundarias.ui.profile.edit

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.ProfileValidationException
import com.groovycow.misionessecundarias.domain.usecase.UpdateProfileUseCase
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyProfile = mockk<GetMyProfileUseCase>()
    private val updateProfile = mockk<UpdateProfileUseCase>()

    private val profile =
        Profile(
            id = "u1",
            nickname = "AventureroCali",
            gender = Gender.FEMENINO,
            description = "Me gusta ayudar",
            avatarId = AvatarId.SOL,
            photoUrl = "https://x/foto.jpg",
            isComplete = true,
        )

    private fun loadedViewModel(): EditProfileViewModel {
        coEvery { getMyProfile() } returns Result.success(profile)
        return EditProfileViewModel(getMyProfile, updateProfile).also { it.load() }
    }

    @Test
    fun loadPrefillsCurrentData() {
        val state = loadedViewModel().uiState.value

        assertTrue(state.isLoaded)
        assertEquals("AventureroCali", state.nickname)
        assertEquals(Gender.FEMENINO, state.gender)
        assertEquals("Me gusta ayudar", state.description)
        assertEquals(AvatarId.SOL, state.avatarId)
        assertEquals("https://x/foto.jpg", state.photoUrl)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() {
        coEvery { getMyProfile() } returns Result.failure(ProfileException(ProfileError.NoConnection))
        val vm = EditProfileViewModel(getMyProfile, updateProfile)
        vm.load()
        assertFalse(vm.uiState.value.isLoaded)
        assertEquals(ProfileError.NoConnection, vm.uiState.value.loadError)

        coEvery { getMyProfile() } returns Result.success(profile)
        vm.load()

        assertTrue(vm.uiState.value.isLoaded)
        assertNull(vm.uiState.value.loadError)
    }

    @Test
    fun successfulEditSavesDraftAndEmitsSaved() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.success(Unit)
        vm.onNicknameChange("ExplorerCali")
        vm.onDescriptionChange("Nueva descripción")

        vm.save()

        coVerify {
            updateProfile(ProfileDraft("ExplorerCali", Gender.FEMENINO, "Nueva descripción", AvatarId.SOL), null)
        }
        assertTrue(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun emptyNicknameShowsRequiredAndDoesNotSave() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns
            Result.failure(ProfileValidationException(mapOf(ProfileField.Nickname to FieldError.Required)))
        vm.onNicknameChange("")

        vm.save()

        assertEquals(FieldError.Required, vm.uiState.value.fieldErrors[ProfileField.Nickname])
        assertFalse(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun editingFieldClearsItsError() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns
            Result.failure(ProfileValidationException(mapOf(ProfileField.Nickname to FieldError.Required)))
        vm.onNicknameChange("")
        vm.save()

        vm.onNicknameChange("Ab")

        assertNull(vm.uiState.value.fieldErrors[ProfileField.Nickname])
    }

    @Test
    fun ownNicknameWithDifferentCaseIsSavedWithoutTakenError() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.success(Unit)
        vm.onNicknameChange("aventurerocali")

        vm.save()

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.isSaved)
    }

    @Test
    fun nicknameTakenByAnotherShowsError() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.failure(ProfileException(ProfileError.NicknameTaken))
        vm.onNicknameChange("explorercali")

        vm.save()

        assertEquals(ProfileError.NicknameTaken, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun noConnectionKeepsEditedValues() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.failure(ProfileException(ProfileError.NoConnection))
        vm.onNicknameChange("ExplorerCali")

        vm.save()

        assertEquals(ProfileError.NoConnection, vm.uiState.value.error)
        assertEquals("ExplorerCali", vm.uiState.value.nickname)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun unexpectedFailureMapsToServerError() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.failure(IllegalStateException("boom"))

        vm.save()

        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
    }

    @Test
    fun saveWhileLoadingIsIgnored() {
        val vm = loadedViewModel()
        coEvery { updateProfile(any(), any()) } returns Result.success(Unit)
        vm.save()
        vm.save()

        coVerify(exactly = 1) { updateProfile(any(), any()) }
    }

    @Test
    fun saveBeforeLoadedIsIgnored() {
        val vm = EditProfileViewModel(getMyProfile, updateProfile)

        vm.save()

        coVerify(exactly = 0) { updateProfile(any(), any()) }
    }
}
