package com.groovycow.misionessecundarias.ui.applications

import com.groovycow.misionessecundarias.domain.model.AppliedMission
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetActivePausesUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pruebas de HU-054 y HU-055: el postulante aceptado ve que la misión está en pausa. */
class MyApplicationsViewModelPausesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val applications = mockk<ApplicationRepository>()
    private val pauses = mockk<PauseRepository>()

    private fun viewModel() =
        MyApplicationsViewModel(
            GetMyApplicationsUseCase(applications),
            CancelApplicationUseCase(applications),
            GetActivePausesUseCase(pauses),
        )

    @Test
    fun acceptedApplicantOfInProgressMissionSeesActivePauses() {
        coEvery { applications.getMyApplications() } returns listOf(application("a1", "m1", "Aceptado", "En progreso"))
        coEvery { pauses.getActivePauses(listOf("m1")) } returns mapOf("m1" to setOf(PauseKind.HYDRATION))
        val viewModel = viewModel()

        viewModel.loadApplications()

        assertEquals(mapOf("m1" to setOf(PauseKind.HYDRATION)), viewModel.uiState.value.activePauses)
    }

    @Test
    fun onlyAcceptedApplicationsOfInProgressMissionsAreQueried() {
        coEvery { applications.getMyApplications() } returns
            listOf(
                application("a1", "m1", "Aceptado", "En progreso"),
                application("a2", "m2", "Postulado", "En progreso"),
                application("a3", "m3", "Aceptado", "Publicada"),
                application("a4", "m4", "Rechazado", "En progreso"),
            )
        coEvery { pauses.getActivePauses(any()) } returns emptyMap()
        val viewModel = viewModel()

        viewModel.loadApplications()

        coVerify(exactly = 1) { pauses.getActivePauses(listOf("m1")) }
    }

    @Test
    fun noPauseQueryWhenNoMissionIsInProgress() {
        coEvery { applications.getMyApplications() } returns listOf(application("a1", "m1", "Postulado", "Publicada"))
        val viewModel = viewModel()

        viewModel.loadApplications()

        assertTrue(viewModel.uiState.value.activePauses.isEmpty())
        coVerify(exactly = 0) { pauses.getActivePauses(any()) }
    }

    @Test
    fun refreshPausesPicksUpNewPause() {
        coEvery { applications.getMyApplications() } returns listOf(application("a1", "m1", "Aceptado", "En progreso"))
        coEvery { pauses.getActivePauses(listOf("m1")) } returns emptyMap()
        val viewModel = viewModel().also { it.loadApplications() }
        coEvery { pauses.getActivePauses(listOf("m1")) } returns mapOf("m1" to setOf(PauseKind.SOCIALIZATION))

        viewModel.refreshPauses()

        assertEquals(setOf(PauseKind.SOCIALIZATION), viewModel.uiState.value.activePauses["m1"])
    }

    @Test
    fun refreshPausesDropsPauseThatEnded() {
        coEvery { applications.getMyApplications() } returns listOf(application("a1", "m1", "Aceptado", "En progreso"))
        coEvery { pauses.getActivePauses(listOf("m1")) } returns mapOf("m1" to setOf(PauseKind.HYDRATION))
        val viewModel = viewModel().also { it.loadApplications() }
        coEvery { pauses.getActivePauses(listOf("m1")) } returns emptyMap()

        viewModel.refreshPauses()

        assertTrue(viewModel.uiState.value.activePauses.isEmpty())
    }

    @Test
    fun failedPauseQueryKeepsApplicationsAndPreviousPauses() {
        coEvery { applications.getMyApplications() } returns listOf(application("a1", "m1", "Aceptado", "En progreso"))
        coEvery { pauses.getActivePauses(listOf("m1")) } returns mapOf("m1" to setOf(PauseKind.HYDRATION))
        val viewModel = viewModel().also { it.loadApplications() }
        coEvery { pauses.getActivePauses(listOf("m1")) } throws IllegalStateException("sin red")

        viewModel.refreshPauses()

        assertEquals(1, viewModel.uiState.value.applications.size)
        assertEquals(setOf(PauseKind.HYDRATION), viewModel.uiState.value.activePauses["m1"])
    }

    private fun application(
        id: String,
        missionId: String,
        status: String,
        missionStatus: String,
    ) = MyApplication(
        id = id,
        missionId = missionId,
        status = status,
        appliedAt = "2026-11-07T10:00:00Z",
        mission =
            AppliedMission(
                id = missionId,
                title = "Misión $missionId",
                dateTime = "2026-11-20T14:00:00Z",
                status = missionStatus,
            ),
    )
}
