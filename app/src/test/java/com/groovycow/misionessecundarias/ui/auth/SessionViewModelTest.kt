package com.groovycow.misionessecundarias.ui.auth

import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModelFor(states: MutableStateFlow<SessionState>): SessionViewModel {
        val useCase = mockk<ObserveSessionUseCase>()
        every { useCase() } returns states
        return SessionViewModel(useCase)
    }

    @Test
    fun staysLoadingWhileSessionIsLoadingFromStorage() {
        val viewModel = viewModelFor(MutableStateFlow(SessionState.LOADING))

        assertEquals(StartDestination.LOADING, viewModel.startDestination.value)
        assertEquals(false, viewModel.isLoggedIn.value)
        assertEquals(false, viewModel.isSessionResolved.value)
    }

    @Test
    fun goesToLoginWhenThereIsNoSession() {
        val viewModel = viewModelFor(MutableStateFlow(SessionState.UNAUTHENTICATED))

        assertEquals(StartDestination.LOGIN, viewModel.startDestination.value)
        assertEquals(false, viewModel.isLoggedIn.value)
        assertEquals(true, viewModel.isSessionResolved.value)
    }

    @Test
    fun goesToMainMenuWhenThereIsASession() {
        val viewModel = viewModelFor(MutableStateFlow(SessionState.AUTHENTICATED))

        assertEquals(StartDestination.MAIN_MENU, viewModel.startDestination.value)
        assertEquals(true, viewModel.isLoggedIn.value)
        assertEquals(true, viewModel.isSessionResolved.value)
    }

    @Test
    fun decidesOnlyOnceAfterLoading() {
        val states = MutableStateFlow(SessionState.LOADING)
        val viewModel = viewModelFor(states)

        states.value = SessionState.UNAUTHENTICATED
        states.value = SessionState.AUTHENTICATED

        assertEquals(StartDestination.LOGIN, viewModel.startDestination.value)
        assertEquals(true, viewModel.isLoggedIn.value)
    }

    @Test
    fun exposesLogoutAfterTheInitialDestinationWasResolved() {
        val states = MutableStateFlow(SessionState.AUTHENTICATED)
        val viewModel = viewModelFor(states)

        states.value = SessionState.UNAUTHENTICATED

        assertEquals(StartDestination.MAIN_MENU, viewModel.startDestination.value)
        assertEquals(false, viewModel.isLoggedIn.value)
    }

    @Test
    fun updatesTheBooleanStateExplicitlyAfterLoginAndLogout() {
        val viewModel = viewModelFor(MutableStateFlow(SessionState.UNAUTHENTICATED))

        viewModel.updateLoginState(true)
        assertEquals(true, viewModel.isLoggedIn.value)

        viewModel.updateLoginState(false)
        assertEquals(false, viewModel.isLoggedIn.value)
    }
}
