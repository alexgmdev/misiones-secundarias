package com.groovycow.misionessecundarias.ui.profile.publicprofile

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.usecase.GetPublicProfileUseCase
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

class PublicProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPublicProfile = mockk<GetPublicProfileUseCase>()

    private fun profile(photoUrl: String? = "https://x/foto.jpg") =
        PublicProfile(
            id = "u2",
            nickname = "CarlosM",
            gender = Gender.MASCULINO,
            description = "Me gusta ayudar",
            avatarId = AvatarId.RIO,
            photoUrl = photoUrl,
        )

    @Test
    fun loadShowsPublicProfile() {
        coEvery { getPublicProfile("u2") } returns Result.success(profile())
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("CarlosM", state.profile?.nickname)
        assertEquals(Gender.MASCULINO, state.profile?.gender)
        assertEquals(AvatarId.RIO, state.profile?.avatarId)
    }

    @Test
    fun profileWithoutPhotoLoadsWithoutFailing() {
        coEvery { getPublicProfile("u2") } returns Result.success(profile(photoUrl = null))
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")

        assertNull(vm.uiState.value.profile?.photoUrl)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() {
        coEvery { getPublicProfile("u2") } returns Result.failure(ProfileException(ProfileError.ServerError))
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")
        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
        assertNull(vm.uiState.value.profile)

        coEvery { getPublicProfile("u2") } returns Result.success(profile())
        vm.retry()

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.profile != null)
        coVerify(exactly = 2) { getPublicProfile("u2") }
    }

    @Test
    fun noConnectionIsReported() {
        coEvery { getPublicProfile("u2") } returns Result.failure(ProfileException(ProfileError.NoConnection))
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")

        assertEquals(ProfileError.NoConnection, vm.uiState.value.error)
    }

    @Test
    fun unknownFailureMapsToServerError() {
        coEvery { getPublicProfile("u2") } returns Result.failure(IllegalStateException("boom"))
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")

        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
    }

    @Test
    fun loadingAnotherUserReplacesPreviousProfile() {
        coEvery { getPublicProfile("u2") } returns Result.success(profile())
        coEvery { getPublicProfile("u3") } returns Result.success(profile().copy(id = "u3", nickname = "LauraP"))
        val vm = PublicProfileViewModel(getPublicProfile)
        vm.load("u2")

        vm.load("u3")

        assertEquals("LauraP", vm.uiState.value.profile?.nickname)
    }

    @Test
    fun loadingSameUserTwiceDoesNotRefetch() {
        coEvery { getPublicProfile("u2") } returns Result.success(profile())
        val vm = PublicProfileViewModel(getPublicProfile)

        vm.load("u2")
        vm.load("u2")

        coVerify(exactly = 1) { getPublicProfile("u2") }
    }
}
