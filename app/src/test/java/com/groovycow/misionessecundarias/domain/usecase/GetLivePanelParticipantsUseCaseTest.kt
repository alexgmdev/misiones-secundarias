package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de HU-051: solo los solucionadores aceptados aparecen en el panel en vivo. */
class GetLivePanelParticipantsUseCaseTest {
    private val repository = mockk<ApplicationRepository>()

    @Test
    fun returnsOnlyAcceptedParticipants() =
        runTest {
            coEvery { repository.getMissionApplicants("mission-1") } returns
                listOf(
                    applicant("a1", "Postulado"),
                    applicant("a2", "Aceptado"),
                    applicant("a3", "Rechazado"),
                    applicant("a4", "Aceptado"),
                )

            val result = GetLivePanelParticipantsUseCase(repository)("mission-1")

            assertEquals(listOf("a2", "a4"), result.getOrNull()?.map { it.id })
        }

    @Test
    fun returnsEmptyListWhenNobodyWasAccepted() =
        runTest {
            coEvery { repository.getMissionApplicants("mission-1") } returns
                listOf(applicant("a1", "Postulado"), applicant("a3", "Rechazado"))

            val result = GetLivePanelParticipantsUseCase(repository)("mission-1")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull().orEmpty().isEmpty())
        }

    @Test
    fun wrapsRepositoryFailureInResult() =
        runTest {
            coEvery { repository.getMissionApplicants("mission-1") } throws IllegalStateException("sin red")

            val result = GetLivePanelParticipantsUseCase(repository)("mission-1")

            assertTrue(result.isFailure)
        }

    private fun applicant(
        id: String,
        status: String,
    ) = MissionApplicant(
        id = id,
        missionId = "mission-1",
        solverId = "solver-$id",
        status = status,
        appliedAt = "2026-11-07T10:00:00Z",
        profile = ApplicantProfile("Persona $id"),
    )
}
