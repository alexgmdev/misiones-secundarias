package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class ChangeCurrentPasswordUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = ChangeCurrentPasswordUseCase(repository, PasswordValidator())

    private fun errorOf(result: Result<Unit>): AuthError = (result.exceptionOrNull() as AuthException).error

    private fun fieldErrorsOf(result: Result<Unit>): Map<ChangePasswordField, FieldError> =
        (result.exceptionOrNull() as ChangePasswordValidationException).errors

    private fun stubSession() {
        every { repository.currentUserEmail() } returns "ana@correo.com"
        coEvery { repository.signIn(any(), any()) } returns Unit
        coEvery { repository.updatePassword(any()) } returns Unit
    }

    private fun verifyNoBackendCalls() {
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
        coVerify(exactly = 0) { repository.updatePassword(any()) }
        coVerify(exactly = 0) { repository.signOut() }
    }

    @Test
    fun emptyFieldsAreRequiredAndDoNotCallTheBackend() =
        runTest {
            val result = useCase("", "", "")

            assertEquals(
                mapOf(
                    ChangePasswordField.Current to FieldError.Required,
                    ChangePasswordField.New to FieldError.Required,
                    ChangePasswordField.Confirmation to FieldError.Required,
                ),
                fieldErrorsOf(result),
            )
            verifyNoBackendCalls()
        }

    @Test
    fun shortNewPasswordFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("Actual123", "Ab1", "Ab1")

            assertEquals(FieldError.PasswordTooShort, fieldErrorsOf(result)[ChangePasswordField.New])
            verifyNoBackendCalls()
        }

    @Test
    fun newPasswordWithoutRequiredClassesFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("Actual123", "abcdefghi", "abcdefghi")

            assertEquals(FieldError.PasswordMissingClasses, fieldErrorsOf(result)[ChangePasswordField.New])
            verifyNoBackendCalls()
        }

    @Test
    fun mismatchedConfirmationFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("Actual123", "Nueva1234", "Nueva12345")

            assertEquals(FieldError.PasswordMismatch, fieldErrorsOf(result)[ChangePasswordField.Confirmation])
            verifyNoBackendCalls()
        }

    @Test
    fun newPasswordEqualToCurrentFailsWithSamePasswordWithoutCallingTheBackend() =
        runTest {
            val result = useCase("Actual123", "Actual123", "Actual123")

            assertEquals(AuthError.SamePassword, errorOf(result))
            verifyNoBackendCalls()
        }

    @Test
    fun missingSessionEmailFailsWithServerError() =
        runTest {
            every { repository.currentUserEmail() } returns null

            assertEquals(AuthError.ServerError, errorOf(useCase("Actual123", "Nueva1234", "Nueva1234")))
            verifyNoBackendCalls()
        }

    @Test
    fun invalidCredentialsOnReauthenticationBecomeIncorrectCurrentPassword() =
        runTest {
            stubSession()
            coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.InvalidCredentials)

            assertEquals(AuthError.IncorrectCurrentPassword, errorOf(useCase("Otra12345", "Nueva1234", "Nueva1234")))
            coVerify(exactly = 0) { repository.updatePassword(any()) }
        }

    @Test
    fun successReauthenticatesThenUpdatesWithoutSigningOut() =
        runTest {
            stubSession()

            assertTrue(useCase("Actual123", "Nueva1234", "Nueva1234").isSuccess)
            coVerifyOrder {
                repository.signIn("ana@correo.com", "Actual123")
                repository.updatePassword("Nueva1234")
            }
            coVerify(exactly = 0) { repository.signOut() }
        }

    @Test
    fun networkErrorsAreMappedInBothSteps() =
        runTest {
            stubSession()
            coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.NoConnection)
            assertEquals(AuthError.NoConnection, errorOf(useCase("Actual123", "Nueva1234", "Nueva1234")))

            stubSession()
            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.NoConnection)
            assertEquals(AuthError.NoConnection, errorOf(useCase("Actual123", "Nueva1234", "Nueva1234")))
        }

    @Test
    fun serverAndUnexpectedErrorsBecomeServerError() =
        runTest {
            stubSession()
            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.ServerError)
            assertEquals(AuthError.ServerError, errorOf(useCase("Actual123", "Nueva1234", "Nueva1234")))

            coEvery { repository.updatePassword(any()) } throws IllegalStateException("boom")
            assertEquals(AuthError.ServerError, errorOf(useCase("Actual123", "Nueva1234", "Nueva1234")))
        }

    @Test
    fun cancellationIsRethrown() =
        runTest {
            stubSession()
            coEvery { repository.signIn(any(), any()) } throws CancellationException("cancelado")

            try {
                useCase("Actual123", "Nueva1234", "Nueva1234")
                fail("Debía relanzar CancellationException")
            } catch (expected: CancellationException) {
                assertEquals("cancelado", expected.message)
            }
        }
}
