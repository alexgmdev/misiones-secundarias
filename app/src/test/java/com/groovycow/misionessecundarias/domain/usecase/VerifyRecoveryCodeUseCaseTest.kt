package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.RecoveryCodeValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyRecoveryCodeUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = VerifyRecoveryCodeUseCase(repository, RecoveryCodeValidator())

    private fun errorOf(result: Result<Unit>): AuthError = (result.exceptionOrNull() as AuthException).error

    @Test
    fun validCodeIsVerifiedWithTrimmedValues() =
        runTest {
            coEvery { repository.verifyRecoveryCode("pablo@example.com", "849201") } returns Unit

            assertTrue(useCase("pablo@example.com", " 849201 ").isSuccess)
            coVerify(exactly = 1) { repository.verifyRecoveryCode("pablo@example.com", "849201") }
        }

    @Test
    fun nonNumericCodeFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("pablo@example.com", "84a201")

            assertEquals(AuthError.InvalidOrExpiredCode, errorOf(result))
            coVerify(exactly = 0) { repository.verifyRecoveryCode(any(), any()) }
        }

    @Test
    fun rejectedCodeIsInvalidOrExpired() =
        runTest {
            coEvery { repository.verifyRecoveryCode(any(), any()) } throws AuthException(AuthError.InvalidOrExpiredCode)

            assertEquals(AuthError.InvalidOrExpiredCode, errorOf(useCase("pablo@example.com", "111111")))
        }

    @Test
    fun errorsAreReducedToInvalidCodeNoConnectionOrServerError() =
        runTest {
            coEvery { repository.verifyRecoveryCode(any(), any()) } throws AuthException(AuthError.NoConnection)
            assertEquals(AuthError.NoConnection, errorOf(useCase("pablo@example.com", "849201")))

            coEvery { repository.verifyRecoveryCode(any(), any()) } throws AuthException(AuthError.WeakPassword)
            assertEquals(AuthError.ServerError, errorOf(useCase("pablo@example.com", "849201")))

            coEvery { repository.verifyRecoveryCode(any(), any()) } throws IllegalStateException("boom")
            assertEquals(AuthError.ServerError, errorOf(useCase("pablo@example.com", "849201")))
        }
}
