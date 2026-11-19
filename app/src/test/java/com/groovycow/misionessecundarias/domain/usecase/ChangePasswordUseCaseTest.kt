package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePasswordUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = ChangePasswordUseCase(repository, PasswordValidator())

    private fun errorOf(result: Result<Unit>): AuthError = (result.exceptionOrNull() as AuthException).error

    @Test
    fun successUpdatesPasswordAndThenSignsOut() =
        runTest {
            coEvery { repository.updatePassword("Clave1234") } returns Unit
            coEvery { repository.signOut() } returns Unit

            assertTrue(useCase("Clave1234").isSuccess)
            coVerifyOrder {
                repository.updatePassword("Clave1234")
                repository.signOut()
            }
        }

    @Test
    fun weakPasswordFailsWithoutCallingTheBackend() =
        runTest {
            val result = useCase("abc123")

            assertEquals(AuthError.WeakPassword, errorOf(result))
            coVerify(exactly = 0) { repository.updatePassword(any()) }
            coVerify(exactly = 0) { repository.signOut() }
        }

    @Test
    fun samePasswordFromServerIsReportedAndDoesNotSignOut() =
        runTest {
            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.SamePassword)

            assertEquals(AuthError.SamePassword, errorOf(useCase("Clave1234")))
            coVerify(exactly = 0) { repository.signOut() }
        }

    @Test
    fun serverWeakPasswordNetworkAndUnexpectedErrorsAreMapped() =
        runTest {
            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.WeakPassword)
            assertEquals(AuthError.WeakPassword, errorOf(useCase("Clave1234")))

            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.NoConnection)
            assertEquals(AuthError.NoConnection, errorOf(useCase("Clave1234")))

            coEvery { repository.updatePassword(any()) } throws AuthException(AuthError.InvalidCredentials)
            assertEquals(AuthError.ServerError, errorOf(useCase("Clave1234")))
        }

    @Test
    fun failingSignOutDoesNotUndoASuccessfulChange() =
        runTest {
            coEvery { repository.updatePassword(any()) } returns Unit
            coEvery { repository.signOut() } throws AuthException(AuthError.NoConnection)

            assertTrue(useCase("Clave1234").isSuccess)
        }
}
