package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias para el registro obligatorio de bitácora multiautor. */
class SaveMissionLogbookUseCaseTest {
    private val logbookRepository = mockk<LogbookRepository>()
    private val useCase = SaveMissionLogbookUseCase(logbookRepository)

    @Test
    fun savesPublisherLogbookSuccessfullyWithParticipants() =
        runTest {
            val participant =
                MissionApplicant(
                    "a1",
                    "m1",
                    "s1",
                    "Aceptado",
                    "2026-11-20T10:00:00Z",
                    ApplicantProfile("Juan Pérez"),
                )
            val expectedLogbook =
                MissionLogbook(
                    "l1",
                    "m1",
                    "p1",
                    "publicador",
                    "Excelente evento comunitario",
                    listOf("s1"),
                    listOf("Juan Pérez"),
                )
            coEvery {
                logbookRepository.savePublisherLogbook(
                    "m1",
                    "Excelente evento comunitario",
                    listOf(participant),
                )
            } returns expectedLogbook

            val result = useCase("m1", "  Excelente evento comunitario  ", listOf(participant))

            assertEquals(expectedLogbook, result.getOrNull())
            coVerify {
                logbookRepository.savePublisherLogbook(
                    "m1",
                    "Excelente evento comunitario",
                    listOf(participant),
                )
            }
        }

    @Test
    fun savesParticipantLogbookSuccessfully() =
        runTest {
            val expectedLogbook = MissionLogbook("l2", "m1", "s1", "participante", "Fue una gran experiencia participar")
            coEvery { logbookRepository.saveParticipantLogbook("m1", "Fue una gran experiencia participar") } returns expectedLogbook

            val result = useCase.saveParticipantEntry("m1", " Fue una gran experiencia participar ")

            assertEquals(expectedLogbook, result.getOrNull())
            coVerify(exactly = 1) { logbookRepository.saveParticipantLogbook("m1", "Fue una gran experiencia participar") }
        }

    @Test
    fun rejectsEmptyOrBlankPublisherLogbookEntry() =
        runTest {
            val result = useCase("m1", "   ", emptyList())

            assertEquals(MissionError.LogbookEntryRequired, result.errorOrNull())
            coVerify(exactly = 0) { logbookRepository.savePublisherLogbook(any(), any(), any()) }
        }

    @Test
    fun rejectsEmptyOrBlankParticipantLogbookEntry() =
        runTest {
            val result = useCase.saveParticipantEntry("m1", "   ")

            assertEquals(MissionError.LogbookEntryRequired, result.errorOrNull())
            coVerify(exactly = 0) { logbookRepository.saveParticipantLogbook(any(), any()) }
        }

    @Test
    fun wrapsRepositoryException() =
        runTest {
            coEvery { logbookRepository.savePublisherLogbook(any(), any(), any()) } throws IllegalStateException("Error de conexión")

            val result = useCase("m1", "Relato", emptyList())

            assertTrue(result.isFailure)
        }

    private fun Result<MissionLogbook>.errorOrNull(): MissionError? = (exceptionOrNull() as? MissionValidationException)?.error
}
