package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = SignInUseCase(repository, PhoneValidator())

    private fun errorOf(result: Result<Unit>): AuthError = (result.exceptionOrNull() as AuthException).error

    @Test
    fun identifierWithAtSignsInWithEmailAndNeverTouchesPhoneLogin() =
        runTest {
            coEvery { repository.signIn("ana@example.com", "Clave1234") } returns Unit

            val result = useCase("ana@example.com", "Clave1234")

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.signIn("ana@example.com", "Clave1234") }
            coVerify(exactly = 0) { repository.signInWithPhone(any(), any()) }
        }

    @Test
    fun emailIsTrimmedBeforeSignIn() =
        runTest {
            coEvery { repository.signIn("ana@example.com", "Clave1234") } returns Unit

            assertTrue(useCase("  ana@example.com ", "Clave1234").isSuccess)
            coVerify(exactly = 1) { repository.signIn("ana@example.com", "Clave1234") }
        }

    @Test
    fun freeFormatPhoneIsNormalizedBeforeSending() =
        runTest {
            coEvery { repository.signInWithPhone("3001234567", "Clave1234") } returns Unit

            val result = useCase("300 123 4567", "Clave1234")

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.signInWithPhone("3001234567", "Clave1234") }
            coVerify(exactly = 0) { repository.signIn(any(), any()) }
        }

    @Test
    fun phonePrefixedWithCountryCodeIsNormalized() =
        runTest {
            coEvery { repository.signInWithPhone("3109999999", "Clave1234") } returns Unit

            assertTrue(useCase("+57 310-999-9999", "Clave1234").isSuccess)
            coVerify(exactly = 1) { repository.signInWithPhone("3109999999", "Clave1234") }
        }

    @Test
    fun invalidPhoneFormatFailsWithInvalidCredentialsWithoutNetwork() =
        runTest {
            val result = useCase("12345", "Clave1234")

            assertEquals(AuthError.InvalidCredentials, errorOf(result))
            coVerify(exactly = 0) { repository.signIn(any(), any()) }
            coVerify(exactly = 0) { repository.signInWithPhone(any(), any()) }
        }

    @Test
    fun wrongPasswordOnEmailIsInvalidCredentials() =
        runTest {
            coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.InvalidCredentials)

            assertEquals(AuthError.InvalidCredentials, errorOf(useCase("ana@example.com", "mala")))
        }

    @Test
    fun unknownPhoneIsInvalidCredentialsLikeWrongPassword() =
        runTest {
            coEvery { repository.signInWithPhone(any(), any()) } throws AuthException(AuthError.InvalidCredentials)

            assertEquals(AuthError.InvalidCredentials, errorOf(useCase("3109999999", "Clave1234")))
        }

    @Test
    fun noConnectionIsPropagated() =
        runTest {
            coEvery { repository.signInWithPhone(any(), any()) } throws AuthException(AuthError.NoConnection)

            assertEquals(AuthError.NoConnection, errorOf(useCase("3001234567", "Clave1234")))
        }

    @Test
    fun serverErrorIsPropagated() =
        runTest {
            coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.ServerError)

            assertEquals(AuthError.ServerError, errorOf(useCase("ana@example.com", "Clave1234")))
        }

    @Test
    fun unrelatedAuthErrorsAreReducedToServerError() =
        runTest {
            coEvery { repository.signIn(any(), any()) } throws AuthException(AuthError.EmailInUse)

            assertEquals(AuthError.ServerError, errorOf(useCase("ana@example.com", "Clave1234")))
        }

    @Test
    fun unexpectedExceptionIsReducedToServerError() =
        runTest {
            coEvery { repository.signInWithPhone(any(), any()) } throws IllegalStateException("boom")

            assertEquals(AuthError.ServerError, errorOf(useCase("3001234567", "Clave1234")))
        }
}
