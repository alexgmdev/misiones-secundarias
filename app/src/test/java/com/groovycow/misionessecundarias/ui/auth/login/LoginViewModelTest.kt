package com.groovycow.misionessecundarias.ui.auth.login

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.SignInUseCase
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

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signInUseCase = mockk<SignInUseCase>()

    private fun newViewModel() = LoginViewModel(signInUseCase)

    @Test
    fun emptyFieldsShowRequiredPerFieldAndDoNotCallBackend() {
        val viewModel = newViewModel()

        viewModel.signIn()

        val errors = viewModel.uiState.value.fieldErrors
        assertEquals(setOf(LoginField.Identifier, LoginField.Password), errors.keys)
        assertTrue(errors.values.all { it == FieldError.Required })
        coVerify(exactly = 0) { signInUseCase(any(), any()) }
    }

    @Test
    fun blankIdentifierOnlyFlagsThatField() {
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("   ")
        viewModel.onPasswordChange("Clave1234")

        viewModel.signIn()

        assertEquals(mapOf(LoginField.Identifier to FieldError.Required), viewModel.uiState.value.fieldErrors)
        coVerify(exactly = 0) { signInUseCase(any(), any()) }
    }

    @Test
    fun missingPasswordOnlyFlagsThatField() {
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("ana@example.com")

        viewModel.signIn()

        assertEquals(mapOf(LoginField.Password to FieldError.Required), viewModel.uiState.value.fieldErrors)
        coVerify(exactly = 0) { signInUseCase(any(), any()) }
    }

    @Test
    fun validCredentialsSignInAndMarkSignedIn() {
        coEvery { signInUseCase("ana@example.com", "Clave1234") } returns Result.success(Unit)
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("ana@example.com")
        viewModel.onPasswordChange("Clave1234")

        viewModel.signIn()

        coVerify(exactly = 1) { signInUseCase("ana@example.com", "Clave1234") }
        val state = viewModel.uiState.value
        assertTrue(state.isSignedIn)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun handledSignInDoesNotRemainTrueWhenLoginScreenIsOpenedAgain() {
        coEvery { signInUseCase(any(), any()) } returns Result.success(Unit)
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("ana@example.com")
        viewModel.onPasswordChange("Clave1234")
        viewModel.signIn()

        viewModel.onSignInHandled()

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun secondTapWhileLoadingDoesNotSendASecondRequest() {
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { signInUseCase(any(), any()) } coAnswers { gate.await() }
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("3001234567")
        viewModel.onPasswordChange("Clave1234")

        viewModel.signIn()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.signIn()
        gate.complete(Result.success(Unit))

        coVerify(exactly = 1) { signInUseCase(any(), any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failureExposesTypedErrorKeepsFieldsAndStaysSignedOut() {
        coEvery { signInUseCase(any(), any()) } returns Result.failure(AuthException(AuthError.InvalidCredentials))
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("3001234567")
        viewModel.onPasswordChange("mala")

        viewModel.signIn()

        val state = viewModel.uiState.value
        assertEquals(AuthError.InvalidCredentials, state.error)
        assertEquals("3001234567", state.identifier)
        assertEquals("mala", state.password)
        assertFalse(state.isSignedIn)
        assertFalse(state.isLoading)
    }

    @Test
    fun noConnectionAndUnexpectedFailuresAreMapped() {
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("ana@example.com")
        viewModel.onPasswordChange("Clave1234")

        coEvery { signInUseCase(any(), any()) } returns Result.failure(AuthException(AuthError.NoConnection))
        viewModel.signIn()
        assertEquals(AuthError.NoConnection, viewModel.uiState.value.error)

        coEvery { signInUseCase(any(), any()) } returns Result.failure(IllegalStateException("boom"))
        viewModel.signIn()
        assertEquals(AuthError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun editingAFieldClearsPreviousErrors() {
        coEvery { signInUseCase(any(), any()) } returns Result.failure(AuthException(AuthError.InvalidCredentials))
        val viewModel = newViewModel()
        viewModel.onIdentifierChange("3001234567")
        viewModel.onPasswordChange("mala")
        viewModel.signIn()

        viewModel.onPasswordChange("otra")

        assertNull(viewModel.uiState.value.error)
        viewModel.signIn()
        viewModel.onIdentifierChange("")
        viewModel.signIn()
        assertTrue(viewModel.uiState.value.fieldErrors.containsKey(LoginField.Identifier))
        viewModel.onIdentifierChange("x")
        assertFalse(viewModel.uiState.value.fieldErrors.containsKey(LoginField.Identifier))
    }
}
