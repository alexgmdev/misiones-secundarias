package com.groovycow.misionessecundarias.ui.profile.me

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
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

class MyProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyProfile = mockk<GetMyProfileUseCase>()

    private fun profile(description: String? = "Me gusta ayudar") =
        Profile(
            id = "u1",
            nickname = "AventureroCali",
            gender = Gender.FEMENINO,
            description = description,
            avatarId = AvatarId.SOL,
            photoUrl = "https://x/foto.jpg",
            isComplete = true,
        )

    @Test
    fun loadShowsProfileData() {
        coEvery { getMyProfile() } returns Result.success(profile())
        val vm = MyProfileViewModel(getMyProfile)

        vm.load()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("AventureroCali", state.profile?.nickname)
        assertEquals(Gender.FEMENINO, state.profile?.gender)
        assertEquals("Me gusta ayudar", state.profile?.description)
    }

    @Test
    fun profileWithoutDescriptionLoadsWithoutFailing() {
        coEvery { getMyProfile() } returns Result.success(profile(description = null))
        val vm = MyProfileViewModel(getMyProfile)

        vm.load()

        assertNull(vm.uiState.value.profile?.description)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() {
        coEvery { getMyProfile() } returns Result.failure(ProfileException(ProfileError.ServerError))
        val vm = MyProfileViewModel(getMyProfile)

        vm.load()
        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
        assertNull(vm.uiState.value.profile)
        assertFalse(vm.uiState.value.isLoading)

        coEvery { getMyProfile() } returns Result.success(profile())
        vm.retry()

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.profile != null)
        coVerify(exactly = 2) { getMyProfile() }
    }

    @Test
    fun unknownFailureMapsToServerError() {
        coEvery { getMyProfile() } returns Result.failure(IllegalStateException("boom"))
        val vm = MyProfileViewModel(getMyProfile)

        vm.load()

        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
    }

    @Test
    fun reloadReflectsUpdatedProfile() {
        coEvery { getMyProfile() } returns Result.success(profile())
        val vm = MyProfileViewModel(getMyProfile)
        vm.load()

        coEvery { getMyProfile() } returns Result.success(profile().copy(nickname = "ExplorerCali"))
        vm.load()

        assertEquals("ExplorerCali", vm.uiState.value.profile?.nickname)
    }

    @Test
    fun levelIsAlwaysLevelOne() {
        assertEquals("Nivel 1", MyProfileUiState.LEVEL_LABEL)
    }
}
