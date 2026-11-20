package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias para la generación de código de verificación de cierre. */
class GenerateVerificationCodeUseCaseTest {
    private val missionRepository = mockk<MissionRepository>()
    private val useCase = GenerateVerificationCodeUseCase(missionRepository)

    @Test
    fun generatesCodeWhenMissionIsInProgress() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("En progreso")
            coEvery { missionRepository.generateVerificationCode("m1") } returns "CALI2026"

            val result = useCase("m1")

            assertEquals("CALI2026", result.getOrNull())
            coVerify(exactly = 1) { missionRepository.generateVerificationCode("m1") }
        }

    @Test
    fun generatesCodeWhenMissionIsCompleted() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("Completada")
            coEvery { missionRepository.generateVerificationCode("m1") } returns "CALI2026"

            val result = useCase("m1")

            assertEquals("CALI2026", result.getOrNull())
            coVerify(exactly = 1) { missionRepository.generateVerificationCode("m1") }
        }

    @Test
    fun generatesCodeWhenMissionIsRendidaAprobada() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("Rendida aprobada")
            coEvery { missionRepository.generateVerificationCode("m1") } returns "CALI2026"

            val result = useCase("m1")

            assertEquals("CALI2026", result.getOrNull())
            coVerify(exactly = 1) { missionRepository.generateVerificationCode("m1") }
        }

    @Test
    fun rejectsCodeGenerationWhenMissionStatusNotAllowed() =
        runTest {
            listOf("Borrador", "Publicada", "Asignada", "Cancelada", "Suspendida").forEach { status ->
                coEvery { missionRepository.getMissionById("m1") } returns mission(status)

                val result = useCase("m1")

                assertEquals(status, MissionError.MissionNotCompleted, result.errorOrNull())
            }
            coVerify(exactly = 0) { missionRepository.generateVerificationCode(any()) }
        }

    @Test
    fun reportsMissingMission() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns null

            assertEquals(MissionError.MissionNotFound, useCase("m1").errorOrNull())
            coVerify(exactly = 0) { missionRepository.generateVerificationCode(any()) }
        }

    @Test
    fun wrapsRepositoryFailure() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission("Completada")
            coEvery { missionRepository.generateVerificationCode("m1") } throws IllegalStateException("Error al generar")

            assertTrue(useCase("m1").isFailure)
        }

    private fun Result<String>.errorOrNull(): MissionError? = (exceptionOrNull() as? MissionValidationException)?.error

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
