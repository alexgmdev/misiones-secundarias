package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageApplicantsUseCasesTest {
    private val repository = mockk<ApplicationRepository>()

    @Test
    fun getApplicantsReturnsRepositoryData() =
        runTest {
            val expected = listOf(applicant)
            coEvery { repository.getMissionApplicants("mission-1") } returns expected

            val result = GetMissionApplicantsUseCase(repository)("mission-1")

            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun updateStatusDelegatesAcceptedDecision() =
        runTest {
            coEvery {
                repository.updateApplicationStatus("application-1", ApplicationDecision.ACCEPT)
            } returns Unit

            val result =
                UpdateApplicationStatusUseCase(repository)(
                    "application-1",
                    ApplicationDecision.ACCEPT,
                )

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.updateApplicationStatus("application-1", ApplicationDecision.ACCEPT)
            }
        }

    @Test
    fun ownershipFailureIsReturned() =
        runTest {
            val failure = IllegalStateException("Sin sesión")
            coEvery { repository.isCurrentUser("publisher-1") } throws failure

            val result = CheckMissionOwnershipUseCase(repository)("publisher-1")

            assertEquals(failure, result.exceptionOrNull())
        }

    private companion object {
        val applicant =
            MissionApplicant(
                id = "application-1",
                missionId = "mission-1",
                solverId = "solver-1",
                status = "Postulado",
                appliedAt = "2026-11-07T10:00:00Z",
                profile = ApplicantProfile("Ana Solucionadora"),
            )
    }
}
