package com.groovycow.misionessecundarias.ui.auth.register

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.RegistrationData
import com.groovycow.misionessecundarias.domain.usecase.RegisterUseCase
import com.groovycow.misionessecundarias.domain.validation.BirthDateValidator
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.NameValidator
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val registerUseCase = mockk<RegisterUseCase>()
    private val clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC)

    private fun newViewModel() =
        RegisterViewModel(
            registerUseCase,
            NameValidator(),
            EmailValidator(),
            PhoneValidator(),
            BirthDateValidator(clock),
            PasswordValidator(),
        )

    private fun RegisterViewModel.fillValid() {
        onFirstNameChange("Juan")
        onLastNameChange("Pérez")
        onEmailChange(" juan@example.com ")
        onPhoneChange("+57 300-123-4567")
        onBirthDateChange(LocalDate.of(1990, 1, 15))
        onPasswordChange("Clave1234")
        onConfirmationChange("Clave1234")
    }

    @Test
    fun allEmptyFieldsShowRequiredAndDoNotCallBackend() {
        val viewModel = newViewModel()

        viewModel.register()

        val errors = viewModel.uiState.value.fieldErrors
        assertEquals(RegisterField.entries.toSet(), errors.keys)
        assertTrue(errors.values.all { it == FieldError.Required })
        coVerify(exactly = 0) { registerUseCase(any()) }
    }

    @Test
    fun blankLastNameShowsRequiredOnlyOnThatField() {
        val viewModel = newViewModel()
        viewModel.fillValid()
        viewModel.onLastNameChange("   ")

        viewModel.register()

        assertEquals(mapOf(RegisterField.LastName to FieldError.Required), viewModel.uiState.value.fieldErrors)
        coVerify(exactly = 0) { registerUseCase(any()) }
    }

    @Test
    fun invalidEmailPhonePasswordAndMismatchAreReportedTogether() {
        val viewModel = newViewModel()
        viewModel.fillValid()
        viewModel.onEmailChange("juan@")
        viewModel.onPhoneChange("2001234567")
        viewModel.onPasswordChange("Ab1")
        viewModel.onConfirmationChange("otra")

        viewModel.register()

        assertEquals(
            mapOf(
                RegisterField.Email to FieldError.InvalidEmail,
                RegisterField.Phone to FieldError.InvalidPhone,
                RegisterField.Password to FieldError.PasswordTooShort,
                RegisterField.Confirmation to FieldError.PasswordMismatch,
            ),
            viewModel.uiState.value.fieldErrors,
        )
    }

    @Test
    fun underAgeAndFutureBirthDatesAreReported() {
        val viewModel = newViewModel()
        viewModel.fillValid()

        viewModel.onBirthDateChange(LocalDate.of(2008, 11, 4))
        viewModel.register()
        assertEquals(FieldError.UnderAge, viewModel.uiState.value.fieldErrors[RegisterField.BirthDate])

        viewModel.onBirthDateChange(LocalDate.of(2027, 1, 1))
        viewModel.register()
        assertEquals(FieldError.FutureBirthDate, viewModel.uiState.value.fieldErrors[RegisterField.BirthDate])
    }

    @Test
    fun editingAFieldClearsItsError() {
        val viewModel = newViewModel()
        viewModel.register()

        viewModel.onEmailChange("a@b.co")

        assertFalse(RegisterField.Email in viewModel.uiState.value.fieldErrors)
        assertTrue(RegisterField.Phone in viewModel.uiState.value.fieldErrors)
    }

    @Test
    fun validFormCallsUseCaseWithNormalizedDataAndMarksRegistered() {
        coEvery { registerUseCase(any()) } returns Result.success(Unit)
        val viewModel = newViewModel()
        viewModel.fillValid()

        viewModel.register()

        val expected =
            RegistrationData("Juan", "Pérez", "juan@example.com", "3001234567", LocalDate.of(1990, 1, 15), "Clave1234")
        coVerify(exactly = 1) { registerUseCase(expected) }
        assertTrue(viewModel.uiState.value.isRegistered)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun secondTapWhileLoadingDoesNotSendASecondRequest() {
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { registerUseCase(any()) } coAnswers { gate.await() }
        val viewModel = newViewModel()
        viewModel.fillValid()

        viewModel.register()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.register()
        gate.complete(Result.success(Unit))

        coVerify(exactly = 1) { registerUseCase(any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failureExposesTypedErrorKeepsFieldsAndAllowsRetry() {
        coEvery { registerUseCase(any()) } returns Result.failure(AuthException(AuthError.NoConnection))
        val viewModel = newViewModel()
        viewModel.fillValid()

        viewModel.register()

        val state = viewModel.uiState.value
        assertEquals(AuthError.NoConnection, state.error)
        assertEquals("Juan", state.firstName)
        assertEquals("Clave1234", state.password)
        assertFalse(state.isLoading)
        assertFalse(state.isRegistered)
    }

    @Test
    fun emailInUseAndUnexpectedFailuresAreMapped() {
        val viewModel = newViewModel()
        viewModel.fillValid()

        coEvery { registerUseCase(any()) } returns Result.failure(AuthException(AuthError.EmailInUse))
        viewModel.register()
        assertEquals(AuthError.EmailInUse, viewModel.uiState.value.error)

        coEvery { registerUseCase(any()) } returns Result.failure(IllegalStateException("boom"))
        viewModel.register()
        assertEquals(AuthError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun editingAFieldClearsPreviousServerError() {
        coEvery { registerUseCase(any()) } returns Result.failure(AuthException(AuthError.PhoneInUse))
        val viewModel = newViewModel()
        viewModel.fillValid()
        viewModel.register()

        viewModel.onPhoneChange("3109876543")

        assertNull(viewModel.uiState.value.error)
    }
}
