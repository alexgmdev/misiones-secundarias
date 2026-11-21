package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ModerationError
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ModerationValidationException
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus
import com.groovycow.misionessecundarias.domain.model.ReporterProfile
import com.groovycow.misionessecundarias.domain.repository.ModerationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias para la cola de moderación y ordenamiento por Prioridad Alta. */
class GetModerationReportsUseCaseTest {
    private val moderationRepository = mockk<ModerationRepository>()
    private val useCase = GetModerationReportsUseCase(moderationRepository)

    private val reportLow =
        ModerationReport(
            id = "r1",
            reporterId = "u1",
            targetType = "Misión",
            targetId = "m1",
            reason = "Spam",
            priority = "Baja",
            createdAt = "2026-11-20T10:00:00Z",
            reporterProfile = ReporterProfile("Ana"),
        )
    private val reportHigh =
        ModerationReport(
            id = "r2",
            reporterId = "u2",
            targetType = "Usuario",
            targetId = "u3",
            reason = "Lenguaje inapropiado",
            priority = "Alta",
            createdAt = "2026-11-21T11:00:00Z",
            reporterProfile = ReporterProfile("Carlos"),
        )
    private val reportMedium =
        ModerationReport(
            id = "r3",
            reporterId = "u3",
            targetType = "Comentario",
            targetId = "c1",
            reason = "Contenido duplicado",
            priority = "Media",
            createdAt = "2026-11-21T12:00:00Z",
            reporterProfile = ReporterProfile("Beatriz"),
        )

    @Test
    fun ordersHighPriorityReportsFirstWhenSortingByPriorityHighFirst() =
        runTest {
            coEvery { moderationRepository.getUserRole() } returns "moderator"
            coEvery { moderationRepository.getReports(any(), any(), any(), ReportSortOrder.PRIORITY_HIGH_FIRST) } returns
                listOf(reportLow, reportMedium, reportHigh)

            val result = useCase(sortOrder = ReportSortOrder.PRIORITY_HIGH_FIRST)

            val reports = result.getOrNull()
            assertTrue(reports != null)
            assertEquals("r2", reports!![0].id)
            assertEquals(ReportPriority.ALTA, reports[0].reportPriority)
            assertEquals("r3", reports[1].id)
            assertEquals("r1", reports[2].id)
            coVerify(exactly = 1) { moderationRepository.getReports(any(), any(), any(), ReportSortOrder.PRIORITY_HIGH_FIRST) }
        }

    @Test
    fun forwardsStatusAndContentFiltersAndOrdersByOldestDate() =
        runTest {
            val high1 = reportHigh.copy(createdAt = "2026-11-21T12:00:00Z")
            val high2 = reportHigh.copy(id = "r4", createdAt = "2026-11-20T12:00:00Z")
            coEvery { moderationRepository.getUserRole() } returns "moderator"
            coEvery {
                moderationRepository.getReports(
                    ReportPriority.ALTA,
                    ReportStatus.RESUELTO,
                    "Comentario",
                    ReportSortOrder.DATE_OLDEST_FIRST,
                )
            } returns listOf(high1, high2)

            val result =
                useCase(
                    priorityFilter = ReportPriority.ALTA,
                    statusFilter = ReportStatus.RESUELTO,
                    targetTypeFilter = "Comentario",
                    sortOrder = ReportSortOrder.DATE_OLDEST_FIRST,
                )

            assertEquals(listOf("r4", "r2"), result.getOrThrow().map { it.id })
            coVerify(exactly = 1) {
                moderationRepository.getReports(
                    ReportPriority.ALTA,
                    ReportStatus.RESUELTO,
                    "Comentario",
                    ReportSortOrder.DATE_OLDEST_FIRST,
                )
            }
        }

    @Test
    fun deniesAccessToNonModeratorUsers() =
        runTest {
            listOf("user", "guest", "publisher", "").forEach { role ->
                coEvery { moderationRepository.getUserRole() } returns role

                val result = useCase()

                assertEquals(role, ModerationError.AccessDenied, result.errorOrNull())
            }
            coVerify(exactly = 0) { moderationRepository.getReports(any(), any(), any(), any()) }
        }

    @Test
    fun wrapsRepositoryFailure() =
        runTest {
            coEvery { moderationRepository.getUserRole() } returns "moderator"
            coEvery { moderationRepository.getReports(any(), any(), any(), any()) } throws IllegalStateException("Error de servidor")

            val result = useCase()

            assertTrue(result.isFailure)
        }

    private fun Result<List<ModerationReport>>.errorOrNull(): ModerationError? =
        (exceptionOrNull() as? ModerationValidationException)?.error
}
