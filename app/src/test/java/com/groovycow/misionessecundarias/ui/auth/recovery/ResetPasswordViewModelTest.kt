package com.groovycow.misionessecundarias.ui.auth.recovery

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.CancelRecoveryUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordUseCase
import com.groovycow.misionessecundarias.domain.usecase.RequestPasswordResetUseCase
import com.groovycow.misionessecundarias.domain.usecase.VerifyRecoveryCodeUseCase
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.RecoveryCodeValidator
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val requestReset = mockk<RequestPasswordResetUseCase>()
    private val verifyCode = mockk<VerifyRecoveryCodeUseCase>()
    private val changePassword = mockk<ChangePasswordUseCase>()
    private val cancelRecovery = mockk<CancelRecoveryUseCase>(relaxed = true)

    private fun newViewModel() =
        ResetPasswordViewModel(
            email = "pablo@example.com",
            requestReset = requestReset,
            verifyCode = verifyCode,
            changePassword = changePassword,
            cancelRecovery = cancelRecovery,
            codeValidator = RecoveryCodeValidator(),
            passwordValidator = PasswordValidator(),
        )

    private fun ResetPasswordViewModel.fill(
        code: String = "849201",
        password: String = "Clave1234",
        confirmation: String = password,
    ) {
        onCodeChange(code)
        onPasswordChange(password)
        onConfirmationChange(confirmation)
    }

    private fun failure(error: AuthError) = Result.failure<Unit>(AuthException(error))

    @Test
    fun emptyOrNonNumericCodeShowsInvalidCodeAndDoesNotCallBackend() =
        runTest {
            val viewModel = newViewModel()
            viewModel.fill(code = "")
            viewModel.save()
            runCurrent()
            assertEquals(FieldError.InvalidCode, viewModel.uiState.value.fieldErrors[ResetField.Code])

            viewModel.fill(code = "84a201")
            viewModel.save()
            runCurrent()
            assertEquals(FieldError.InvalidCode, viewModel.uiState.value.fieldErrors[ResetField.Code])
            coVerify(exactly = 0) { verifyCode(any(), any()) }
            coVerify(exactly = 0) { changePassword(any()) }
        }

    @Test
    fun weakPasswordShowsErrorAndDoesNotCallBackend() =
        runTest {
            val viewModel = newViewModel()
            viewModel.fill(password = "abc123")

            viewModel.save()
            runCurrent()

            assertEquals(FieldError.PasswordTooShort, viewModel.uiState.value.fieldErrors[ResetField.Password])
            coVerify(exactly = 0) { verifyCode(any(), any()) }
        }

    @Test
    fun mismatchedConfirmationShowsMismatch() =
        runTest {
            val viewModel = newViewModel()
            viewModel.fill(password = "Clave1234", confirmation = "Clave12345")

            viewModel.save()
            runCurrent()

            assertEquals(FieldError.PasswordMismatch, viewModel.uiState.value.fieldErrors[ResetField.Confirmation])
            coVerify(exactly = 0) { changePassword(any()) }
        }

    @Test
    fun invalidOrExpiredCodeShowsErrorKeepsDataAndDoesNotChangePassword() =
        runTest {
            coEvery { verifyCode("pablo@example.com", "111111") } returns failure(AuthError.InvalidOrExpiredCode)
            val viewModel = newViewModel()
            viewModel.fill(code = "111111")

            viewModel.save()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(AuthError.InvalidOrExpiredCode, state.error)
            assertEquals("111111", state.code)
            assertEquals("Clave1234", state.password)
            assertFalse(state.codeVerified)
            assertFalse(state.isPasswordChanged)
            coVerify(exactly = 0) { changePassword(any()) }
        }

    @Test
    fun successVerifiesThenChangesPassword() =
        runTest {
            coEvery { verifyCode("pablo@example.com", "849201") } returns Result.success(Unit)
            coEvery { changePassword("Clave1234") } returns Result.success(Unit)
            val viewModel = newViewModel()
            viewModel.fill()

            viewModel.save()
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.isPasswordChanged)
            assertTrue(state.codeVerified)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun serverErrorWhileSavingKeepsDataAndShowsError() =
        runTest {
            coEvery { verifyCode(any(), any()) } returns failure(AuthError.ServerError)
            val viewModel = newViewModel()
            viewModel.fill()

            viewModel.save()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(AuthError.ServerError, state.error)
            assertEquals("849201", state.code)
            assertEquals("Clave1234", state.confirmation)
        }

    @Test
    fun failureAfterVerifyingOnlyRetriesThePasswordWithoutReverifyingTheCode() =
        runTest {
            coEvery { verifyCode(any(), any()) } returns Result.success(Unit)
            coEvery { changePassword(any()) } returns failure(AuthError.ServerError)
            val viewModel = newViewModel()
            viewModel.fill()
            viewModel.save()
            runCurrent()
            assertTrue(viewModel.uiState.value.codeVerified)
            assertEquals(AuthError.ServerError, viewModel.uiState.value.error)

            coEvery { changePassword(any()) } returns Result.success(Unit)
            viewModel.save()
            runCurrent()

            coVerify(exactly = 1) { verifyCode(any(), any()) }
            coVerify(exactly = 2) { changePassword("Clave1234") }
            assertTrue(viewModel.uiState.value.isPasswordChanged)
        }

    @Test
    fun samePasswordErrorIsExposed() =
        runTest {
            coEvery { verifyCode(any(), any()) } returns Result.success(Unit)
            coEvery { changePassword(any()) } returns failure(AuthError.SamePassword)
            val viewModel = newViewModel()
            viewModel.fill()

            viewModel.save()
            runCurrent()

            assertEquals(AuthError.SamePassword, viewModel.uiState.value.error)
        }

    @Test
    fun secondTapWhileLoadingDoesNotSendASecondRequest() =
        runTest {
            val gate = CompletableDeferred<Result<Unit>>()
            coEvery { verifyCode(any(), any()) } coAnswers { gate.await() }
            val viewModel = newViewModel()
            viewModel.fill()

            viewModel.save()
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            viewModel.save()
            gate.complete(failure(AuthError.InvalidOrExpiredCode))
            runCurrent()

            coVerify(exactly = 1) { verifyCode(any(), any()) }
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun resendIsDisabledWithCountdownForSixtySecondsAndThenEnabled() =
        runTest {
            val viewModel = newViewModel()
            assertEquals(60, viewModel.uiState.value.resendAvailableIn)

            advanceTimeBy(30_001)
            assertEquals(30, viewModel.uiState.value.resendAvailableIn)

            viewModel.resend()
            runCurrent()
            coVerify(exactly = 0) { requestReset(any()) }

            advanceTimeBy(30_000)
            assertEquals(0, viewModel.uiState.value.resendAvailableIn)
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.resendAvailableIn)
        }

    @Test
    fun resendAfterCooldownRequestsANewCodeAndRestartsTheCountdown() =
        runTest {
            coEvery { requestReset("pablo@example.com") } returns Result.success(Unit)
            val viewModel = newViewModel()
            advanceTimeBy(60_001)

            viewModel.resend()
            runCurrent()

            coVerify(exactly = 1) { requestReset("pablo@example.com") }
            assertEquals(60, viewModel.uiState.value.resendAvailableIn)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun resendWithoutConnectionShowsErrorAndDoesNotRestartCountdown() =
        runTest {
            coEvery { requestReset(any()) } returns failure(AuthError.NoConnection)
            val viewModel = newViewModel()
            advanceTimeBy(60_001)

            viewModel.resend()
            runCurrent()

            assertEquals(AuthError.NoConnection, viewModel.uiState.value.error)
            assertEquals(0, viewModel.uiState.value.resendAvailableIn)
        }

    @Test
    fun editingFieldsClearsItsErrors() =
        runTest {
            val viewModel = newViewModel()
            viewModel.save()
            runCurrent()
            assertTrue(viewModel.uiState.value.fieldErrors.containsKey(ResetField.Code))

            viewModel.onCodeChange("1")

            assertFalse(viewModel.uiState.value.fieldErrors.containsKey(ResetField.Code))
        }

    @Test
    fun abandoningAfterVerifyingTheCodeSignsOutTheTemporarySession() =
        runTest {
            coEvery { verifyCode(any(), any()) } returns Result.success(Unit)
            coEvery { changePassword(any()) } returns failure(AuthError.ServerError)
            val viewModel = newViewModel()
            viewModel.fill()
            viewModel.save()
            runCurrent()
            assertTrue(viewModel.uiState.value.codeVerified)

            viewModel.onAbandon()
            runCurrent()

            coVerify(exactly = 1) { cancelRecovery() }
        }

    @Test
    fun abandoningBeforeVerifyingOrAfterChangingThePasswordDoesNothing() =
        runTest {
            val unverified = newViewModel()
            unverified.onAbandon()
            runCurrent()
            coVerify(exactly = 0) { cancelRecovery() }

            coEvery { verifyCode(any(), any()) } returns Result.success(Unit)
            coEvery { changePassword(any()) } returns Result.success(Unit)
            val changed = newViewModel()
            changed.fill()
            changed.save()
            runCurrent()
            assertTrue(changed.uiState.value.isPasswordChanged)

            changed.onAbandon()
            runCurrent()
            coVerify(exactly = 0) { cancelRecovery() }
        }

    @Test
    fun failingSignOutOnAbandonIsIgnored() =
        runTest {
            coEvery { verifyCode(any(), any()) } returns Result.success(Unit)
            coEvery { changePassword(any()) } returns failure(AuthError.ServerError)
            coEvery { cancelRecovery() } returns failure(AuthError.NoConnection)
            val viewModel = newViewModel()
            viewModel.fill()
            viewModel.save()
            runCurrent()
            val before = viewModel.uiState.value

            viewModel.onAbandon()
            runCurrent()

            assertEquals(before, viewModel.uiState.value)
        }
}
