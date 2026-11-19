package com.groovycow.misionessecundarias.ui.settings

import com.groovycow.misionessecundarias.domain.usecase.SignOutUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signOutUseCase = mockk<SignOutUseCase>()

    @Test
    fun successfulSignOutEmitsAOneTimeEvent() =
        runTest {
            coEvery { signOutUseCase() } returns Result.success(Unit)
            val viewModel = SettingsViewModel(signOutUseCase)
            val event =
                async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.events.first()
                }

            viewModel.signOut()

            assertEquals(SettingsEvent.SignedOut, event.await())
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertNull(viewModel.uiState.value.error)
            assertTrue(viewModel.events.replayCache.isEmpty())
            coVerify(exactly = 1) { signOutUseCase() }
        }

    @Test
    fun failedSignOutShowsAndDismissesTheError() {
        coEvery { signOutUseCase() } returns Result.failure(IllegalStateException("falló"))
        val viewModel = SettingsViewModel(signOutUseCase)

        viewModel.signOut()

        assertTrue(viewModel.uiState.value.error != null)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}
