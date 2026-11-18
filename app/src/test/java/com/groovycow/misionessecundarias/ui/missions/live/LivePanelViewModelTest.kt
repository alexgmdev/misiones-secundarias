package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.usecase.GetLivePanelParticipantsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Pruebas de HU-051: estado del panel de ejecución en vivo y avatares. */
class LivePanelViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getParticipants = mockk<GetLivePanelParticipantsUseCase>()

    @Test
    fun loadExposesAcceptedParticipants() {
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana, luis))
        val viewModel = LivePanelViewModel(getParticipants)

        viewModel.load("mission-1")

        assertEquals(listOf(ana, luis), viewModel.uiState.value.participants)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadFailureShowsErrorWithoutParticipants() {
        coEvery { getParticipants("mission-1") } returns Result.failure(IllegalStateException("sin red"))
        val viewModel = LivePanelViewModel(getParticipants)

        viewModel.load("mission-1")

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(emptyList<MissionApplicant>(), viewModel.uiState.value.participants)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshReflectsParticipantWhoLeft() {
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana, luis))
        val viewModel = LivePanelViewModel(getParticipants).also { it.load("mission-1") }
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana))

        viewModel.refresh("mission-1")

        assertEquals(listOf(ana), viewModel.uiState.value.participants)
    }

    @Test
    fun refreshReflectsNewParticipant() {
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana))
        val viewModel = LivePanelViewModel(getParticipants).also { it.load("mission-1") }
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana, luis))

        viewModel.refresh("mission-1")

        assertEquals(listOf(ana, luis), viewModel.uiState.value.participants)
    }

    @Test
    fun failedRefreshKeepsVisibleAvatarsAndHidesError() {
        coEvery { getParticipants("mission-1") } returns Result.success(listOf(ana, luis))
        val viewModel = LivePanelViewModel(getParticipants).also { it.load("mission-1") }
        coEvery { getParticipants("mission-1") } returns Result.failure(IllegalStateException("sin red"))

        viewModel.refresh("mission-1")

        assertEquals(listOf(ana, luis), viewModel.uiState.value.participants)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun initialsUseFirstLettersOfFirstTwoWords() {
        assertEquals("AS", participantInitials("Ana Solucionadora"))
        assertEquals("LP", participantInitials("luis  pérez gómez"))
        assertEquals("M", participantInitials("  maría "))
    }

    @Test
    fun initialsFallbackForBlankName() {
        assertEquals("?", participantInitials("   "))
        assertEquals("?", participantInitials(""))
    }

    private companion object {
        val ana = participant("a1", "Ana Solucionadora")
        val luis = participant("a2", "Luis Pérez")

        fun participant(
            id: String,
            name: String,
        ) = MissionApplicant(
            id = id,
            missionId = "mission-1",
            solverId = "solver-$id",
            status = "Aceptado",
            appliedAt = "2026-11-07T10:00:00Z",
            profile = ApplicantProfile(name),
        )
    }
}
