package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.RegistrationData
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RegisterUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = RegisterUseCase(repository)
    private val data =
        RegistrationData(
            firstName = "Juan",
            lastName = "Pérez",
            email = "juan@example.com",
            phone = "3001234567",
            birthDate = LocalDate.of(1990, 1, 15),
            password = "Clave1234",
        )

    private fun errorOf(result: Result<Unit>): AuthError = (result.exceptionOrNull() as AuthException).error

    @Test
    fun phoneAlreadyInUseFailsWithoutCallingSignUp() =
        runTest {
            coEvery { repository.isPhoneAvailable("3001234567") } returns false

            val result = useCase(data)

            assertEquals(AuthError.PhoneInUse, errorOf(result))
            coVerify(exactly = 0) { repository.signUp(any()) }
        }

    @Test
    fun validDataSignsUpAndSucceeds() =
        runTest {
            coEvery { repository.isPhoneAvailable("3001234567") } returns true
            coEvery { repository.signUp(data) } returns Unit

            val result = useCase(data)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.signUp(data) }
        }

    @Test
    fun emailInUseIsPropagated() =
        runTest {
            coEvery { repository.isPhoneAvailable(any()) } returns true
            coEvery { repository.signUp(any()) } throws AuthException(AuthError.EmailInUse)

            assertEquals(AuthError.EmailInUse, errorOf(useCase(data)))
        }

    @Test
    fun signUpWithoutConnectionIsPropagated() =
        runTest {
            coEvery { repository.isPhoneAvailable(any()) } returns true
            coEvery { repository.signUp(any()) } throws AuthException(AuthError.NoConnection)

            assertEquals(AuthError.NoConnection, errorOf(useCase(data)))
        }

    @Test
    fun phoneCheckWithoutConnectionFailsWithNoConnection() =
        runTest {
            coEvery { repository.isPhoneAvailable(any()) } throws AuthException(AuthError.NoConnection)

            assertEquals(AuthError.NoConnection, errorOf(useCase(data)))
            coVerify(exactly = 0) { repository.signUp(any()) }
        }

    @Test
    fun serverErrorStaysServerErrorWhenPhoneIsStillAvailable() =
        runTest {
            coEvery { repository.isPhoneAvailable(any()) } returns true
            coEvery { repository.signUp(any()) } throws AuthException(AuthError.ServerError)

            assertEquals(AuthError.ServerError, errorOf(useCase(data)))
        }

    @Test
    fun serverErrorBecomesPhoneInUseWhenRaceTookThePhone() =
        runTest {
            coEvery { repository.isPhoneAvailable(any()) } returnsMany listOf(true, false)
            coEvery { repository.signUp(any()) } throws AuthException(AuthError.ServerError)

            assertEquals(AuthError.PhoneInUse, errorOf(useCase(data)))
        }
}
