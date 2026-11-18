package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de HU-064: completar la misión al alcanzar el 100 % de progreso. */
class CompleteMissionUseCaseTest {
    private val missionRepository = mockk<MissionRepository>()
    private val checklistRepository = mockk<ChecklistRepository>()
    private val useCase = CompleteMissionUseCase(missionRepository, checklistRepository)

    @Test
    fun completesMissionWhenEveryTaskIsDone() =
        runTest {
            val completed = mission("Completada")
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { checklistRepository.getItems("m1") } returns listOf(item(true), item(true))
            coEvery { missionRepository.completeMission("m1") } returns completed

            val result = useCase("m1")

            assertEquals(completed, result.getOrNull())
            coVerify(exactly = 1) { missionRepository.completeMission("m1") }
        }

    @Test
    fun rejectsCompletionBelowHundredPercent() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { checklistRepository.getItems("m1") } returns listOf(item(true), item(false))

            val result = useCase("m1")

            assertEquals(MissionError.ProgressIncomplete, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.completeMission(any()) }
        }

    @Test
    fun rejectsCompletionWithoutTasksBecauseProgressIsZero() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { checklistRepository.getItems("m1") } returns emptyList()

            val result = useCase("m1")

            assertEquals(MissionError.ProgressIncomplete, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.completeMission(any()) }
        }

    @Test
    fun rejectsCompletionFromStatusesOtherThanInProgress() =
        runTest {
            listOf("Borrador", "Publicada", "Asignada", "Cancelada", "Suspendida").forEach { status ->
                coEvery { missionRepository.getMissionById("m1") } returns mission(status)

                val result = useCase("m1")

                assertEquals(status, MissionError.NotCompletable, result.errorOrNull())
            }
            coVerify(exactly = 0) { missionRepository.completeMission(any()) }
        }

    @Test
    fun alreadyCompletedMissionSucceedsWithoutUpdating() =
        runTest {
            val completed = mission("Completada")
            coEvery { missionRepository.getMissionById("m1") } returns completed

            val result = useCase("m1")

            assertEquals(completed, result.getOrNull())
            coVerify(exactly = 0) { missionRepository.completeMission(any()) }
        }

    @Test
    fun reportsMissingMission() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns null

            assertEquals(MissionError.MissionNotFound, useCase("m1").errorOrNull())
        }

    @Test
    fun wrapsRepositoryFailure() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { checklistRepository.getItems("m1") } returns listOf(item(true))
            coEvery { missionRepository.completeMission("m1") } throws IllegalStateException("sin permiso")

            assertTrue(useCase("m1").isFailure)
        }

    @Test
    fun wrapsChecklistFailure() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { checklistRepository.getItems("m1") } throws IllegalStateException("sin red")

            assertTrue(useCase("m1").isFailure)
            coVerify(exactly = 0) { missionRepository.completeMission(any()) }
        }

    private fun Result<Mission>.errorOrNull(): MissionError? = (exceptionOrNull() as? MissionValidationException)?.error

    private fun item(done: Boolean) = ChecklistItem(id = "i-$done-${System.nanoTime()}", missionId = "m1", title = "Tarea", isDone = done)

    private fun mission(status: String) =
        Mission(
            id = "m1",
            publisherId = "publisher-1",
            title = "Limpieza del río",
            description = "Recoger residuos",
            modality = "campo",
            dateTime = "2026-11-20T14:00:00Z",
            maxParticipants = 5,
            status = status,
        )
}
