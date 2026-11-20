package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AdminError
import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import com.groovycow.misionessecundarias.domain.model.AdminValidationException
import com.groovycow.misionessecundarias.domain.repository.AdminRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias para la consulta de métricas con control de acceso por rol. */
class GetAdminMetricsUseCaseTest {
    private val adminRepository = mockk<AdminRepository>()
    private val useCase = GetAdminMetricsUseCase(adminRepository)
    private val metrics =
        AdminMetrics(
            totalMissions = 10,
            activeMissions = 4,
            completedMissions = 5,
            totalUsers = 25,
            activeSolvers = 8,
        )

    @Test
    fun grantsAccessToModerator() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "moderator"
            coEvery { adminRepository.getOperationalMetrics() } returns metrics

            val result = useCase()

            assertEquals(metrics, result.getOrNull())
            coVerify(exactly = 1) { adminRepository.getOperationalMetrics() }
        }

    @Test
    fun grantsAccessToAdmin() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "admin"
            coEvery { adminRepository.getOperationalMetrics() } returns metrics

            val result = useCase()

            assertEquals(metrics, result.getOrNull())
            coVerify(exactly = 1) { adminRepository.getOperationalMetrics() }
        }

    @Test
    fun deniesAccessToStandardUser() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "user"

            val result = useCase()

            assertEquals(AdminError.AccessDenied, result.errorOrNull())
            coVerify(exactly = 0) { adminRepository.getOperationalMetrics() }
        }

    @Test
    fun deniesAccessToOtherRoles() =
        runTest {
            listOf("guest", "publisher", "", "unknown").forEach { role ->
                coEvery { adminRepository.getUserRole() } returns role

                val result = useCase()

                assertEquals(role, AdminError.AccessDenied, result.errorOrNull())
            }
            coVerify(exactly = 0) { adminRepository.getOperationalMetrics() }
        }

    @Test
    fun wrapsRepositoryFailure() =
        runTest {
            coEvery { adminRepository.getUserRole() } returns "moderator"
            coEvery { adminRepository.getOperationalMetrics() } throws IllegalStateException("Error de servidor")

            val result = useCase()

            assertTrue(result.isFailure)
        }

    private fun Result<AdminMetrics>.errorOrNull(): AdminError? = (exceptionOrNull() as? AdminValidationException)?.error
}
