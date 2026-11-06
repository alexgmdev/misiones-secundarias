package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PublishMissionUseCaseTest {
    private lateinit var repository: MissionRepository
    private lateinit var useCase: PublishMissionUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase =
            PublishMissionUseCase(
                repository,
                Clock.fixed(Instant.parse("2026-11-05T17:00:00Z"), ZoneId.of("America/Bogota")),
            )
    }

    @Test
    fun shouldRejectDraftWithoutMeetingPointWithoutCallingRepository() =
        runTest {
            val result = useCase(validDraft.copy(meetingPoint = null))

            val exception = result.exceptionOrNull() as MissionValidationException
            assertEquals(MissionError.MeetingPointRequired, exception.error)
            coVerify(exactly = 0) { repository.publishMission(any()) }
        }

    @Test
    fun shouldRejectExpiredDraftWithoutCallingRepository() =
        runTest {
            val result = useCase(validDraft.copy(dateTime = "2026-11-04T10:00:00-05:00"))

            val exception = result.exceptionOrNull() as MissionValidationException
            assertEquals(MissionError.DateInPast, exception.error)
            coVerify(exactly = 0) { repository.publishMission(any()) }
        }

    @Test
    fun shouldRejectIncompleteDraftWithoutCallingRepository() =
        runTest {
            val result = useCase(validDraft.copy(requirements = null))

            val exception = result.exceptionOrNull() as MissionValidationException
            assertEquals(MissionError.DraftIncomplete, exception.error)
            coVerify(exactly = 0) { repository.publishMission(any()) }
        }

    @Test
    fun shouldPublishValidDraft() =
        runTest {
            val published = validDraft.copy(status = "Publicada")
            coEvery { repository.publishMission(validDraft.id) } returns published

            val result = useCase(validDraft)

            assertTrue(result.isSuccess)
            assertEquals(published, result.getOrNull())
            coVerify(exactly = 1) { repository.publishMission(validDraft.id) }
        }

    @Test
    fun shouldReturnRepositoryFailure() =
        runTest {
            val exception = IllegalStateException("No fue posible publicar")
            coEvery { repository.publishMission(validDraft.id) } throws exception

            val result = useCase(validDraft)

            assertSame(exception, result.exceptionOrNull())
        }

    @Test
    fun shouldRethrowCancellation() =
        runTest {
            val cancellation = CancellationException("cancelled")
            coEvery { repository.publishMission(validDraft.id) } throws cancellation

            try {
                useCase(validDraft)
                throw AssertionError("La cancelación debe propagarse")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }

    @Test
    fun shouldBeIdempotentWhenMissionIsAlreadyPublished() =
        runTest {
            val published = validDraft.copy(status = "Publicada")

            val result = useCase(published)

            assertEquals(published, result.getOrNull())
            coVerify(exactly = 0) { repository.publishMission(any()) }
        }

    private companion object {
        val validDraft =
            Mission(
                id = "mission-id",
                publisherId = "publisher-id",
                title = "Siembra",
                description = "Jornada de siembra comunitaria",
                category = null,
                requirements = "Guantes",
                modality = "campo",
                dateTime = "2026-11-15T10:00:00-05:00",
                location = "0101000020E6100000",
                meetingPoint = "Parque Dog Cali",
                maxParticipants = 2,
                status = "Borrador",
                publisherProfile = null,
            )
    }
}
