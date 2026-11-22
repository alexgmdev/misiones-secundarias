package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AppliedMission
import com.groovycow.misionessecundarias.domain.model.HistoryRole
import com.groovycow.misionessecundarias.domain.model.LogbookAuthorProfile
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class GetMissionHistoryUseCaseTest {
    private val missions = mockk<MissionRepository>()
    private val applications = mockk<ApplicationRepository>()
    private val useCase = GetMissionHistoryUseCase(missions, applications)

    private fun mission(
        id: String,
        date: String,
        status: String = "Publicada",
    ) = Mission(
        id = id,
        publisherId = "me",
        title = "Misión $id",
        description = "Desc $id",
        requirements = "Req $id",
        modality = "Presencial",
        dateTime = date,
        maxParticipants = 3,
        status = status,
    )

    private fun application(
        missionId: String,
        date: String,
        status: String = "Aceptado",
    ) = MyApplication(
        id = "a$missionId",
        missionId = missionId,
        status = status,
        appliedAt = "2026-01-01T00:00:00Z",
        mission = AppliedMission(missionId, "Misión $missionId", date, "Completada"),
    )

    @Test
    fun mixesRolesSortedByDateDescending() =
        runTest {
            coEvery { missions.getMyMissions() } returns listOf(mission("m1", "2026-03-01T10:00:00Z"))
            coEvery { applications.getMyApplications() } returns listOf(application("m2", "2026-05-01T10:00:00Z"))
            coEvery { missions.getMissionById("m2") } returns mission("m2", "2026-05-01T10:00:00Z", "Completada")

            val result = useCase().getOrThrow()

            assertEquals(listOf("m2", "m1"), result.map { it.missionId })
            assertEquals(listOf(HistoryRole.SOLVER, HistoryRole.PUBLISHER), result.map { it.role })
            assertEquals("Desc m2", result[0].description)
            assertEquals("Req m2", result[0].requirements)
        }

    @Test
    fun excludesDraftsAndNonAcceptedApplications() =
        runTest {
            coEvery { missions.getMyMissions() } returns
                listOf(mission("m1", "2026-03-01T10:00:00Z", "Borrador"), mission("m3", "2026-02-01T10:00:00Z"))
            coEvery { applications.getMyApplications() } returns
                listOf(application("m4", "2026-05-01T10:00:00Z", "Pendiente"), application("m5", "2026-04-01T10:00:00Z", "Rechazado"))

            val result = useCase().getOrThrow()

            assertEquals(listOf("m3"), result.map { it.missionId })
            coVerify(exactly = 0) { missions.getMissionById(any()) }
        }

    @Test
    fun deduplicatesByMissionIdPublisherWins() =
        runTest {
            coEvery { missions.getMyMissions() } returns listOf(mission("m1", "2026-03-01T10:00:00Z"))
            coEvery { applications.getMyApplications() } returns listOf(application("m1", "2026-03-01T10:00:00Z"))

            val result = useCase().getOrThrow()

            assertEquals(1, result.size)
            assertEquals(HistoryRole.PUBLISHER, result[0].role)
        }

    @Test
    fun emptyHistory() =
        runTest {
            coEvery { missions.getMyMissions() } returns emptyList()
            coEvery { applications.getMyApplications() } returns emptyList()

            assertTrue(useCase().getOrThrow().isEmpty())
        }

    @Test
    fun detailFailureForAppliedMissionKeepsEntryWithoutDescription() =
        runTest {
            coEvery { missions.getMyMissions() } returns emptyList()
            coEvery { applications.getMyApplications() } returns listOf(application("m2", "2026-05-01T10:00:00Z"))
            coEvery { missions.getMissionById("m2") } throws IOException("sin red")

            val entry = useCase().getOrThrow().single()

            assertNull(entry.description)
            assertEquals("Misión m2", entry.title)
        }

    @Test
    fun repositoryErrorBecomesFailure() =
        runTest {
            coEvery { missions.getMyMissions() } throws IOException("boom")

            assertTrue(useCase().exceptionOrNull() is IOException)
        }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() =
        runTest {
            coEvery { missions.getMyMissions() } throws CancellationException("cancel")
            useCase()
        }

    @Test
    fun logbookSectionsSplitOfficialAndParticipants() =
        runTest {
            val logbooks = mockk<LogbookRepository>()
            coEvery { logbooks.getLogbooksForMission("m1") } returns
                listOf(
                    logbook("publicador", "Todo salió bien", "Ana"),
                    logbook("participante", "Gran experiencia", "Luis"),
                    logbook("participante", "Aprendí mucho", null),
                )

            val sections = GetMissionLogbookSectionsUseCase(logbooks)("m1").getOrThrow()

            assertEquals("Todo salió bien", sections.official)
            assertEquals(listOf("Luis", "Participante"), sections.participants.map { it.authorName })
            assertEquals("Gran experiencia", sections.participants[0].text)
        }

    @Test
    fun logbookSectionsEmptyAndFailure() =
        runTest {
            val logbooks = mockk<LogbookRepository>()
            coEvery { logbooks.getLogbooksForMission("m1") } returns emptyList()
            coEvery { logbooks.getLogbooksForMission("m2") } throws IOException("x")
            val sectionsUseCase = GetMissionLogbookSectionsUseCase(logbooks)

            val empty = sectionsUseCase("m1").getOrThrow()
            assertNull(empty.official)
            assertTrue(empty.participants.isEmpty())
            assertTrue(sectionsUseCase("m2").isFailure)
        }

    private fun logbook(
        role: String,
        entry: String,
        author: String?,
    ) = MissionLogbook(
        id = entry,
        missionId = "m1",
        authorId = "u",
        role = role,
        entry = entry,
        authorProfile = author?.let { LogbookAuthorProfile(it) },
    )
}
