package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CreateMissionUseCaseTest {
    private lateinit var repository: MissionRepository
    private lateinit var useCase: CreateMissionUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateMissionUseCase(repository)
    }

    @Test
    fun shouldReturnCreatedMissionWhenRepositorySucceeds() =
        runTest {
            coEvery { repository.createMission(draft) } returns mission

            val result = useCase(draft)

            assertTrue(result.isSuccess)
            assertEquals(mission, result.getOrNull())
            coVerify(exactly = 1) { repository.createMission(draft) }
        }

    @Test
    fun shouldReturnFailureWhenRepositoryThrows() =
        runTest {
            val exception = IllegalStateException("No se pudo guardar")
            coEvery { repository.createMission(draft) } throws exception

            val result = useCase(draft)

            assertTrue(result.isFailure)
            assertSame(exception, result.exceptionOrNull())
        }

    @Test
    fun shouldRethrowCancellation() =
        runTest {
            val cancellation = CancellationException("cancelled")
            coEvery { repository.createMission(draft) } throws cancellation

            try {
                useCase(draft)
                throw AssertionError("La cancelación debe propagarse")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }

    private companion object {
        val draft =
            CreateMissionDraft(
                title = "Siembra",
                description = "Descripción",
                requirements = "Guantes",
                category = null,
                modality = MissionModality.CAMPO,
                date = LocalDate.of(2026, 11, 15),
                time = LocalTime.of(10, 0),
                meetingPoint = "Parque Dog Cali",
                latitude = 3.45,
                longitude = -76.53,
                maxParticipants = 2,
            )
        val mission =
            Mission(
                id = "mission-id",
                publisherId = "publisher-id",
                title = draft.title,
                description = draft.description,
                category = null,
                requirements = draft.requirements,
                modality = MissionModality.CAMPO.databaseValue,
                dateTime = "2026-11-15T10:00:00-05:00",
                location = "0101000020E6100000",
                meetingPoint = draft.meetingPoint,
                maxParticipants = 2,
                status = "Borrador",
                publisherProfile = null,
            )
    }
}
