package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelRecoveryUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = CancelRecoveryUseCase(repository)

    @Test
    fun signsOutTheTemporarySession() =
        runTest {
            coEvery { repository.signOut() } returns Unit

            assertTrue(useCase().isSuccess)
            coVerify(exactly = 1) { repository.signOut() }
        }

    @Test
    fun signOutFailureIsReturnedAsFailureWithoutThrowing() =
        runTest {
            coEvery { repository.signOut() } throws AuthException(AuthError.NoConnection)

            assertTrue(useCase().isFailure)
        }
}
