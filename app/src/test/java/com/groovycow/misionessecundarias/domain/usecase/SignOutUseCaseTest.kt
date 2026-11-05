package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SignOutUseCaseTest {
    private val repository = mockk<AuthRepository>()

    @Test
    fun closesTheCurrentSession() =
        runTest {
            coEvery { repository.signOut() } returns Unit

            val result = SignOutUseCase(repository)()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.signOut() }
        }

    @Test
    fun exposesRepositoryFailure() =
        runTest {
            coEvery { repository.signOut() } throws IllegalStateException("falló")

            val result = SignOutUseCase(repository)()

            assertTrue(result.isFailure)
        }
}
