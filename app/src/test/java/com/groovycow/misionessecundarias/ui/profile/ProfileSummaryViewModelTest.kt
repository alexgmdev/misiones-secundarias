package com.groovycow.misionessecundarias.ui.profile

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyProfile = mockk<GetMyProfileUseCase>()

    private fun profile(
        nickname: String,
        avatar: AvatarId,
        photo: String?,
    ) = Profile("u1", nickname, Gender.MASCULINO, null, avatar, photo, true)

    @Test
    fun startsEmpty() {
        val vm = ProfileSummaryViewModel(getMyProfile)

        assertNull(vm.uiState.value.nickname)
        assertNull(vm.uiState.value.avatarId)
        assertNull(vm.uiState.value.photoUrl)
    }

    @Test
    fun refreshUpdatesNicknameAvatarAndPhoto() {
        coEvery { getMyProfile() } returns Result.success(profile("AventureroCali", AvatarId.SOL, "https://x/a.jpg"))
        val vm = ProfileSummaryViewModel(getMyProfile)
        vm.refresh()
        assertEquals("AventureroCali", vm.uiState.value.nickname)

        coEvery { getMyProfile() } returns Result.success(profile("ExplorerCali", AvatarId.RIO, "https://x/b.jpg"))
        vm.refresh()

        val state = vm.uiState.value
        assertEquals("ExplorerCali", state.nickname)
        assertEquals(AvatarId.RIO, state.avatarId)
        assertEquals("https://x/b.jpg", state.photoUrl)
    }

    @Test
    fun refreshFailureKeepsPreviousSummary() {
        coEvery { getMyProfile() } returns Result.success(profile("AventureroCali", AvatarId.SOL, null))
        val vm = ProfileSummaryViewModel(getMyProfile)
        vm.refresh()

        coEvery { getMyProfile() } returns Result.failure(ProfileException(ProfileError.NoConnection))
        vm.refresh()

        assertEquals("AventureroCali", vm.uiState.value.nickname)
    }
}
