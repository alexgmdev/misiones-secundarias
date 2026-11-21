package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.AdminRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanAccessAdminPanelUseCaseTest {
    private val adminRepository = mockk<AdminRepository>()
    private val useCase = CanAccessAdminPanelUseCase(adminRepository)

    @Test
    fun moderatorCanSeeAdminPanelEntry() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "moderator"

            assertTrue(useCase().getOrThrow())
        }

    @Test
    fun standardUserCannotSeeAdminPanelEntry() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "user"

            assertFalse(useCase().getOrThrow())
        }

    @Test
    fun adminCanSeeAdminPanelEntry() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "admin"

            assertTrue(useCase().getOrThrow())
        }
}
