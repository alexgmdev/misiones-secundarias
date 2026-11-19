package com.groovycow.misionessecundarias.ui.missions

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.usecase.GetMissionApplicantsUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateApplicationStatusUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ApplicantsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getApplicants = mockk<GetMissionApplicantsUseCase>()
    private val updateStatus = mockk<UpdateApplicationStatusUseCase>()

    @Test
    fun loadApplicantsExposesTheOrderedList() {
        val expected = listOf(applicant)
        coEvery { getApplicants("mission-1") } returns Result.success(expected)
        val viewModel = ApplicantsViewModel(getApplicants, updateStatus)

        viewModel.loadApplicants("mission-1")

        assertEquals(expected, viewModel.uiState.value.applicants)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun acceptingApplicantUpdatesVisibleStatus() {
        coEvery {
            updateStatus("application-1", ApplicationDecision.ACCEPT)
        } returns Result.success(Unit)
        val viewModel = loadedViewModel()

        viewModel.decide("application-1", ApplicationDecision.ACCEPT)

        assertEquals("Aceptado", viewModel.uiState.value.applicants.single().status)
        assertEquals("La postulación fue aceptada.", viewModel.uiState.value.actionMessage)
        coVerify(exactly = 1) {
            updateStatus("application-1", ApplicationDecision.ACCEPT)
        }
    }

    @Test
    fun rejectionFailurePreservesPendingStatusAndShowsError() {
        coEvery {
            updateStatus("application-1", ApplicationDecision.REJECT)
        } returns Result.failure(IllegalStateException("No autorizado"))
        val viewModel = loadedViewModel()

        viewModel.decide("application-1", ApplicationDecision.REJECT)

        assertEquals("Postulado", viewModel.uiState.value.applicants.single().status)
        assertEquals("No autorizado", viewModel.uiState.value.actionMessage)
        assertEquals(true, viewModel.uiState.value.isActionError)
    }

    @Test
    fun httpFailureDoesNotExposeAuthorizationToken() {
        val exception =
            IllegalStateException(
                "permission denied for table applications URL: https://example.test " +
                    "Headers: [Authorization=Bearer secret-token]",
            )
        coEvery {
            updateStatus("application-1", ApplicationDecision.ACCEPT)
        } returns Result.failure(exception)
        val viewModel = loadedViewModel()

        viewModel.decide("application-1", ApplicationDecision.ACCEPT)

        val message = viewModel.uiState.value.actionMessage.orEmpty()
        assertEquals(
            "No fue posible guardar la decisión porque faltan permisos en la base de datos.",
            message,
        )
        assertFalse(message.contains("secret-token"))
        assertTrue(viewModel.uiState.value.isActionError)
    }

    private fun loadedViewModel(): ApplicantsViewModel {
        coEvery { getApplicants("mission-1") } returns Result.success(listOf(applicant))
        return ApplicantsViewModel(getApplicants, updateStatus).also {
            it.loadApplicants("mission-1")
        }
    }

    private companion object {
        val applicant =
            MissionApplicant(
                id = "application-1",
                missionId = "mission-1",
                solverId = "solver-1",
                status = "Postulado",
                appliedAt = "2026-11-07T10:00:00Z",
                profile = ApplicantProfile("Ana Solucionadora"),
            )
    }
}
