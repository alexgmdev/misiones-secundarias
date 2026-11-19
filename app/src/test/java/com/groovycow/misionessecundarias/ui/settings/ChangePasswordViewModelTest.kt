package com.groovycow.misionessecundarias.ui.settings

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.usecase.ChangeCurrentPasswordUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordField
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChangePasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val viewModel = ChangePasswordViewModel(ChangeCurrentPasswordUseCase(repository, PasswordValidator()))

    private fun fill(
        current: String = "Actual123",
        new: String = "Nueva1234",
        confirmation: String = "Nueva1234",
    ) {
        viewModel.onCurrentChange(current)
        viewModel.onNewChange(new)
        viewModel.onConfirmationChange(confirmation)
    }

    private fun stubSession() {
        every { repository.currentUserEmail() } returns "ana@correo.com"
        coEvery { repository.signIn(any(), any()) } returns Unit
        coEvery { repository.updatePassword(any()) } returns Unit
    }

    @Test
    fun emptyFieldsShowRequiredPerFieldWithoutCallingTheBackend() {
        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(FieldError.Required, state.fieldErrors[ChangePasswordField.Current])
        assertEquals(FieldError.Required, state.fieldErrors[ChangePasswordField.New])
        assertEquals(FieldError.Required, state.fieldErrors[ChangePasswordField.Confirmation])
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun editingAFieldClearsItsErrorAndTheGeneralError() {
        viewModel.submit()

        viewModel.onCurrentChange("A")

        val state = viewModel.uiState.value
        assertNull(state.fieldErrors[ChangePasswordField.Current])
        assertEquals(FieldError.Required, state.fieldErrors[ChangePasswordField.New])
    }

    @Test
    fun mismatchedConfirmationIsReportedWithoutCallingTheBackend() {
        fill(confirmation = "Nueva12345")

        viewModel.submit()

        assertEquals(FieldError.PasswordMismatch, viewModel.uiState.value.fieldErrors[ChangePasswordField.Confirmation])
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun successShowsTheDialogAndDoesNotSignOut() {
        stubSession()
        fill()

        viewModel.submit()

        val state = viewModel.uiState.value
        assertTrue(state.isChanged)
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify(exactly = 0) { repository.signOut() }
    }

    @Test
    fun incorrectCurrentPasswordIsReportedAndKeepsTheData() {
        stubSession()
        coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.InvalidCredentials)
        fill(current = "Otra12345")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(AuthError.IncorrectCurrentPassword, state.error)
        assertFalse(state.isChanged)
        assertEquals("Otra12345", state.current)
    }

    @Test
    fun samePasswordIsReportedAsAnError() {
        fill(current = "Actual123", new = "Actual123", confirmation = "Actual123")

        viewModel.submit()

        assertEquals(AuthError.SamePassword, viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun networkAndServerErrorsKeepTheTypedData() {
        stubSession()
        coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.NoConnection)
        fill()

        viewModel.submit()

        assertEquals(AuthError.NoConnection, viewModel.uiState.value.error)
        assertEquals("Nueva1234", viewModel.uiState.value.new)
        assertEquals("Nueva1234", viewModel.uiState.value.confirmation)

        coEvery { repository.updatePassword(any()) } throws IllegalStateException("boom")
        viewModel.submit()

        assertEquals(AuthError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun whileLoadingASecondSubmitIsIgnored() {
        stubSession()
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.signIn(any(), any()) } coAnswers { gate.await() }
        fill()

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.submit()
        gate.complete(Unit)

        coVerify(exactly = 1) { repository.signIn(any(), any()) }
        assertTrue(viewModel.uiState.value.isChanged)
    }
}
