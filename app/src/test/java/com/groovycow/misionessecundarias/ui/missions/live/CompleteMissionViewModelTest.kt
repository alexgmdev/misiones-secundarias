package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.CompleteMissionUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Pruebas de HU-064: estado de la acción "Completar Misión". */
class CompleteMissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val completeMission = mockk<CompleteMissionUseCase>()
    private val completed =
        Mission(
            id = "m1",
            publisherId = "publisher-1",
            title = "Limpieza del río",
            description = "Recoger residuos",
            modality = "campo",
            dateTime = "2026-11-20T14:00:00Z",
            maxParticipants = 5,
            status = "Completada",
        )

    @Test
    fun successExposesCompletedMission() {
        coEvery { completeMission("m1") } returns Result.success(completed)
        val viewModel = CompleteMissionViewModel(completeMission)

        viewModel.complete("m1")

        assertEquals(completed, viewModel.uiState.value.completedMission)
        assertFalse(viewModel.uiState.value.isCompleting)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun incompleteProgressErrorIsExposed() {
        coEvery { completeMission("m1") } returns
            Result.failure(MissionValidationException(MissionError.ProgressIncomplete))
        val viewModel = CompleteMissionViewModel(completeMission)

        viewModel.complete("m1")

        assertEquals(MissionError.ProgressIncomplete, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.completedMission)
    }

    @Test
    fun networkFailureMapsToNoConnection() {
        coEvery { completeMission("m1") } returns Result.failure(IllegalStateException("x", IOException()))
        val viewModel = CompleteMissionViewModel(completeMission)

        viewModel.complete("m1")

        assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
    }

    @Test
    fun unknownFailureMapsToServerError() {
        coEvery { completeMission("m1") } returns Result.failure(IllegalStateException("boom"))
        val viewModel = CompleteMissionViewModel(completeMission)

        viewModel.complete("m1")

        assertEquals(MissionError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun dismissErrorAllowsRetry() {
        coEvery { completeMission("m1") } returns
            Result.failure(MissionValidationException(MissionError.ProgressIncomplete))
        val viewModel = CompleteMissionViewModel(completeMission)
        viewModel.complete("m1")

        viewModel.dismissError()
        coEvery { completeMission("m1") } returns Result.success(completed)
        viewModel.complete("m1")

        assertNull(viewModel.uiState.value.error)
        assertEquals(completed, viewModel.uiState.value.completedMission)
    }

    @Test
    fun secondCompleteAfterSuccessDoesNotCallUseCaseAgain() {
        coEvery { completeMission("m1") } returns Result.success(completed)
        val viewModel = CompleteMissionViewModel(completeMission)
        viewModel.complete("m1")

        viewModel.complete("m1")

        coVerify(exactly = 1) { completeMission("m1") }
    }
}
