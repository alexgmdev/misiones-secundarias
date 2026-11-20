package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Pruebas de HU-032: edición de los datos descriptivos de una misión publicada. */
class UpdateMissionDetailsUseCaseTest {
    private val missionRepository = mockk<MissionRepository>()
    private val applicationRepository = mockk<ApplicationRepository>()
    private val useCase = UpdateMissionDetailsUseCase(missionRepository, applicationRepository)

    private val validUpdate =
        MissionDetailsUpdate(
            title = "  Siembra corregida  ",
            description = "  Nueva descripción  ",
            requirements = "  Ropa cómoda y gorra  ",
            category = " ",
            reward = "  Hidratación  ",
        )

    @Test
    fun updatesDetailsWhenMissionIsPublishedAndHasNoAcceptedParticipants() =
        runTest {
            val updated = mission().copy(title = "Siembra corregida")
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns
                listOf(applicant("Postulado"), applicant("Rechazado"))
            coEvery { missionRepository.updateMissionDetails("m1", any()) } returns updated

            val result = useCase("m1", validUpdate)

            assertEquals(updated, result.getOrNull())
            coVerify(exactly = 1) {
                missionRepository.updateMissionDetails(
                    "m1",
                    MissionDetailsUpdate(
                        title = "Siembra corregida",
                        description = "Nueva descripción",
                        requirements = "Ropa cómoda y gorra",
                        category = null,
                        reward = "Hidratación",
                    ),
                )
            }
        }

    @Test
    fun rejectsBlankDescriptiveFields() =
        runTest {
            val result = useCase("m1", validUpdate.copy(description = " "))

            assertEquals(MissionError.InvalidDetails, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.updateMissionDetails(any(), any()) }
        }

    @Test
    fun rejectsMissionThatIsNotPublished() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission(status = "Borrador")

            val result = useCase("m1", validUpdate)

            assertEquals(MissionError.NotEditable, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.updateMissionDetails(any(), any()) }
        }

    @Test
    fun rejectsMissionWithAcceptedParticipants() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns listOf(applicant("Aceptado"))

            val result = useCase("m1", validUpdate)

            assertEquals(MissionError.HasAcceptedParticipants, result.errorOrNull())
            coVerify(exactly = 0) { missionRepository.updateMissionDetails(any(), any()) }
        }

    @Test
    fun rejectsMissingMission() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns null

            val result = useCase("m1", validUpdate)

            assertEquals(MissionError.NotFound, result.errorOrNull())
        }

    @Test
    fun propagatesRepositoryFailure() =
        runTest {
            coEvery { missionRepository.getMissionById("m1") } returns mission()
            coEvery { applicationRepository.getMissionApplicants("m1") } returns emptyList()
            coEvery { missionRepository.updateMissionDetails("m1", any()) } throws IllegalStateException("db")

            val result = useCase("m1", validUpdate)

            try {
                result.getOrThrow()
                fail("Se esperaba una excepción del repositorio")
            } catch (exception: IllegalStateException) {
                assertEquals("db", exception.message)
            }
        }

    private fun mission(status: String = "Publicada") =
        Mission(
            id = "m1",
            publisherId = "p1",
            title = "Siembra",
            description = "Jornada",
            category = null,
            requirements = "Guantes",
            modality = "campo",
            dateTime = "2026-11-15T10:00:00-05:00",
            location = "0101000020E6100000",
            meetingPoint = "Parque Dog Cali",
            maxParticipants = 2,
            status = status,
        )

    private fun applicant(status: String) =
        MissionApplicant(
            id = "a-$status",
            missionId = "m1",
            solverId = "s1",
            status = status,
            appliedAt = "2026-11-10T10:00:00Z",
            profile = ApplicantProfile(fullName = "Solucionador"),
        )

    private fun Result<Mission>.errorOrNull(): MissionError? = (exceptionOrNull() as? MissionValidationException)?.error
}
