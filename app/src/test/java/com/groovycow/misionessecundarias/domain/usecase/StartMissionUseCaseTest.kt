package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Pruebas de HU-050: iniciar la ejecución de una misión propia. */
class StartMissionUseCaseTest {
    private val missionRepository = mockk<MissionRepository>()
    private val applicationRepository = mockk<ApplicationRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC)
    private val useCase = StartMissionUseCase(missionRepository, applicationRepository, clock)

    @Test
    fun startsMissionWhenTimeReachedAndParticipantsAccepted() =
        runTest {
            val inProgress = mission(status = "En progreso")
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns listOf(applicant("Aceptado"))
            coEvery { missionRepository.startMission("m1") } returns inProgress

            val result = useCase("m1")

            assertEquals(inProgress, result.getOrNull())
            coVerify(exactly = 1) { missionRepository.startMission("m1") }
        }

    @Test
    fun startsAssignedMissionAtExactScheduledTime() =
        runTest {
            val exact = mission(status = "Asignada", dateTime = "2026-11-20T15:00:00Z")
            coEvery { missionRepository.getMissionById("m1") } returns exact
            coEvery { applicationRepository.getMissionApplicants("m1") } returns listOf(applicant("Aceptado"))
            coEvery { missionRepository.startMission("m1") } returns exact.copy(status = "En progreso")

            val result = useCase("m1")

            assertEquals("En progreso", result.getOrNull()?.status)
        }

    @Test
    fun rejectsStartBeforeScheduledTime() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission(dateTime = "2026-11-20T15:00:01Z")

            val result = useCase("m1")

            assertEquals(MissionError.StartTooEarly, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.startMission(any()) }
        }

    @Test
    fun rejectsStartWithoutAcceptedParticipants() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns
                listOf(applicant("Postulado"), applicant("Rechazado"))

            val result = useCase("m1")

            assertEquals(MissionError.NotEnoughParticipants, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.startMission(any()) }
        }

    @Test
    fun rejectsStartWhenMissionHasNoApplicants() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns emptyList()

            val result = useCase("m1")

            assertEquals(MissionError.NotEnoughParticipants, result.errorOrNull())
        }

    @Test
    fun rejectsStartFromNonStartableStatuses() =
        runTest {
            listOf("Borrador", "Completada", "Cancelada", "Suspendida").forEach { status ->
                coEvery { missionRepository.getMissionById("m1") } returns mission(status = status)

                val result = useCase("m1")

                assertEquals(status, MissionError.NotStartable, result.errorOrNull())
            }
            coVerify(exactly = 0) { missionRepository.startMission(any()) }
        }

    @Test
    fun alreadyInProgressMissionSucceedsWithoutUpdating() =
        runTest {
            val inProgress = mission(status = "En progreso")
            coEvery { missionRepository.getMissionById("m1") } returns inProgress

            val result = useCase("m1")

            assertEquals(inProgress, result.getOrNull())
            coVerify(exactly = 0) { missionRepository.startMission(any()) }
        }

    @Test
    fun reportsMissingMission() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns null

            val result = useCase("m1")

            assertEquals(MissionError.MissionNotFound, result.errorOrNull())
        }

    @Test
    fun reportsUnreadableDateTime() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission(dateTime = "no-es-fecha")

            val result = useCase("m1")

            assertEquals(MissionError.InvalidDateTime, result.errorOrNull())
        }

    @Test
    fun wrapsRepositoryFailure() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns listOf(applicant("Aceptado"))
            coEvery { missionRepository.startMission("m1") } throws IllegalStateException("sin permiso")

            val result = useCase("m1")

            assertTrue(result.isFailure)
        }

    private fun Result<Mission>.errorOrNull(): MissionError? = (exceptionOrNull() as? MissionValidationException)?.error

    private fun mission(
        status: String = "Publicada",
        dateTime: String = "2026-11-20T14:00:00Z",
    ) = Mission(
        id = "m1",
        publisherId = "publisher-1",
        title = "Limpieza del río",
        description = "Recoger residuos",
        modality = "campo",
        dateTime = dateTime,
        maxParticipants = 5,
        status = status,
    )

    private fun applicant(status: String) =
        MissionApplicant(
            id = "app-$status",
            missionId = "m1",
            solverId = "solver-$status",
            status = status,
            appliedAt = "2026-11-07T10:00:00Z",
            profile = ApplicantProfile("Ana Solucionadora"),
        )
}
