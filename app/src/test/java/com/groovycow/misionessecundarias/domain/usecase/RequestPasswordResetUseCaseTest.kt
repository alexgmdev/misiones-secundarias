package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestPasswordResetUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = RequestPasswordResetUseCase(repository)

    @Test
    fun registeredEmailSucceedsAndSendsTheTrimmedEmail() =
        runTest {
            coEvery { repository.sendPasswordResetCode("pablo@example.com") } returns Unit

            assertTrue(useCase("  pablo@example.com ").isSuccess)
            coVerify(exactly = 1) { repository.sendPasswordResetCode("pablo@example.com") }
        }

    @Test
    fun anyServerSideFailureIsReportedAsTheSameNeutralSuccess() =
        runTest {
            val failures =
                listOf(
                    AuthException(AuthError.InvalidCredentials),
                    AuthException(AuthError.ServerError),
                    AuthException(AuthError.EmailInUse),
                    IllegalStateException("boom"),
                )
            failures.forEach { failure ->
                coEvery { repository.sendPasswordResetCode("nadie@example.com") } throws failure

                assertTrue("$failure debe ser éxito neutro", useCase("nadie@example.com").isSuccess)
            }
        }

    @Test
    fun noConnectionIsTheOnlyFailureReported() =
        runTest {
            coEvery { repository.sendPasswordResetCode(any()) } throws AuthException(AuthError.NoConnection)

            val result = useCase("pablo@example.com")

            assertEquals(AuthError.NoConnection, (result.exceptionOrNull() as AuthException).error)
        }

    @Test
    fun invalidEmailFormatFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("pablo@")

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { repository.sendPasswordResetCode(any()) }
        }
}
