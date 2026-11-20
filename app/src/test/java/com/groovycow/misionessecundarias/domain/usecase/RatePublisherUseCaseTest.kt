package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.data.repository.ApplicationStatusDto
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.PublisherRating
import com.groovycow.misionessecundarias.domain.model.RatingError
import com.groovycow.misionessecundarias.domain.model.RatingValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.repository.RatingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas de HU-124: calificar el desempeño del publicador. */
class RatePublisherUseCaseTest {
    private val ratingRepository = mockk<RatingRepository>()
    private val missionRepository = mockk<MissionRepository>()
    private val applicationRepository = mockk<ApplicationRepository>()
    private val useCase =
        RatePublisherUseCase(ratingRepository, missionRepository, applicationRepository)

    @Test
    fun ratesPublisherAfterCompletedMissionAsAcceptedSolver() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMyApplicationStatus("m1") } returns
                ApplicationStatusDto(id = "a1", status = "Aceptado")
            coEvery { ratingRepository.ratePublisher("m1", 5) } returns rating(5)

            val result = useCase("m1", 5)

            assertEquals(5, result.getOrNull()?.stars)
            coVerify(exactly = 1) { ratingRepository.ratePublisher("m1", 5) }
        }

    @Test
    fun rejectsScoreOutsideOneToFive() =
        runTest {
            val result = useCase("m1", 6)

            assertEquals(RatingError.OutOfRange, result.errorOrNull())
            coVerify(exactly = 0) { ratingRepository.ratePublisher(any(), any()) }
        }

    @Test
    fun rejectsMissionThatIsNotCompleted() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission(status = "En progreso")

            val result = useCase("m1", 4)

            assertEquals(RatingError.MissionNotCompleted, result.errorOrNull())
        }

    @Test
    fun rejectsSolverWithoutAcceptedApplication() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMyApplicationStatus("m1") } returns
                ApplicationStatusDto(id = "a1", status = "Postulado")

            val result = useCase("m1", 4)

            assertEquals(RatingError.NotAcceptedParticipant, result.errorOrNull())
            coVerify(exactly = 0) { ratingRepository.ratePublisher(any(), any()) }
        }

    @Test
    fun rejectsMissingMission() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns null

            val result = useCase("m1", 4)

            assertEquals(RatingError.MissionNotFound, result.errorOrNull())
        }

    @Test
    fun propagatesAlreadyRatedErrorFromRepository() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMyApplicationStatus("m1") } returns
                ApplicationStatusDto(id = "a1", status = "Aceptado")
            coEvery { ratingRepository.ratePublisher("m1", 4) } throws
                RatingValidationException(RatingError.AlreadyRated)

            val result = useCase("m1", 4)

            assertEquals(RatingError.AlreadyRated, result.errorOrNull())
        }

    private fun mission(status: String = "Completada") =
        Mission(
            id = "m1",
            publisherId = "p1",
            title = "Siembra",
            description = "Jornada",
            modality = "remota",
            dateTime = "2026-11-15T10:00:00-05:00",
            maxParticipants = 2,
            status = status,
        )

    private fun rating(stars: Int) =
        PublisherRating(
            id = "r1",
            missionId = "m1",
            solverId = "s1",
            publisherId = "p1",
            stars = stars,
        )

    private fun Result<PublisherRating>.errorOrNull(): RatingError? = (exceptionOrNull() as? RatingValidationException)?.error
}
