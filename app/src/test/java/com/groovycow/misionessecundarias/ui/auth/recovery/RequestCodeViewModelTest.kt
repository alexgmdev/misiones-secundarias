package com.groovycow.misionessecundarias.ui.auth.recovery

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.RequestPasswordResetUseCase
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.FieldError
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

class RequestCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase = mockk<RequestPasswordResetUseCase>()

    private fun newViewModel() = RequestCodeViewModel(useCase, EmailValidator())

    @Test
    fun emptyEmailShowsRequiredAndDoesNotCallBackend() {
        val viewModel = newViewModel()

        viewModel.sendCode()

        assertEquals(FieldError.Required, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isCodeSent)
        coVerify(exactly = 0) { useCase(any()) }
    }

    @Test
    fun malformedEmailShowsInvalidEmailAndDoesNotCallBackend() {
        val viewModel = newViewModel()
        viewModel.onEmailChange("pablo@")

        viewModel.sendCode()

        assertEquals(FieldError.InvalidEmail, viewModel.uiState.value.emailError)
        coVerify(exactly = 0) { useCase(any()) }
    }

    @Test
    fun validEmailSendsTrimmedEmailAndMarksCodeSent() {
        coEvery { useCase("pablo@example.com") } returns Result.success(Unit)
        val viewModel = newViewModel()
        viewModel.onEmailChange("  pablo@example.com ")

        viewModel.sendCode()

        coVerify(exactly = 1) { useCase("pablo@example.com") }
        val state = viewModel.uiState.value
        assertTrue(state.isCodeSent)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.emailError)
    }

    @Test
    fun noConnectionStaysOnScreenKeepsEmailAndShowsError() {
        coEvery { useCase(any()) } returns Result.failure(AuthException(AuthError.NoConnection))
        val viewModel = newViewModel()
        viewModel.onEmailChange("pablo@example.com")

        viewModel.sendCode()

        val state = viewModel.uiState.value
        assertEquals(AuthError.NoConnection, state.error)
        assertEquals("pablo@example.com", state.email)
        assertFalse(state.isCodeSent)
        assertFalse(state.isLoading)
    }

    @Test
    fun unexpectedFailureIsShownAsServerError() {
        coEvery { useCase(any()) } returns Result.failure(IllegalStateException("boom"))
        val viewModel = newViewModel()
        viewModel.onEmailChange("pablo@example.com")

        viewModel.sendCode()

        assertEquals(AuthError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun secondTapWhileLoadingDoesNotSendASecondRequest() {
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { useCase(any()) } coAnswers { gate.await() }
        val viewModel = newViewModel()
        viewModel.onEmailChange("pablo@example.com")

        viewModel.sendCode()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.sendCode()
        gate.complete(Result.success(Unit))

        coVerify(exactly = 1) { useCase(any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun editingEmailClearsErrors() {
        val viewModel = newViewModel()
        viewModel.sendCode()

        viewModel.onEmailChange("p")

        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.error)
    }
}
