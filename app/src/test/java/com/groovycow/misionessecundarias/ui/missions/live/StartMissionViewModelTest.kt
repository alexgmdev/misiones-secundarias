package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.StartMissionUseCase
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

/** Pruebas de HU-050: estado de la acción "Iniciar Misión". */
class StartMissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val startMission = mockk<StartMissionUseCase>()
    private val inProgress =
        Mission(
            id = "m1",
            publisherId = "publisher-1",
            title = "Limpieza del río",
            description = "Recoger residuos",
            modality = "campo",
            dateTime = "2026-11-20T14:00:00Z",
            maxParticipants = 5,
            status = "En progreso",
        )

    @Test
    fun successExposesStartedMission() {
        coEvery { startMission("m1") } returns Result.success(inProgress)
        val viewModel = StartMissionViewModel(startMission)

        viewModel.start("m1")

        assertEquals(inProgress, viewModel.uiState.value.startedMission)
        assertFalse(viewModel.uiState.value.isStarting)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun businessErrorIsExposedWithoutStartedMission() {
        coEvery { startMission("m1") } returns
            Result.failure(MissionValidationException(MissionError.NotEnoughParticipants))
        val viewModel = StartMissionViewModel(startMission)

        viewModel.start("m1")

        assertEquals(MissionError.NotEnoughParticipants, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.startedMission)
    }

    @Test
    fun networkFailureMapsToNoConnection() {
        coEvery { startMission("m1") } returns Result.failure(IllegalStateException("x", IOException()))
        val viewModel = StartMissionViewModel(startMission)

        viewModel.start("m1")

        assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
    }

    @Test
    fun unknownFailureMapsToServerError() {
        coEvery { startMission("m1") } returns Result.failure(IllegalStateException("boom"))
        val viewModel = StartMissionViewModel(startMission)

        viewModel.start("m1")

        assertEquals(MissionError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun dismissErrorAllowsRetry() {
        coEvery { startMission("m1") } returns
            Result.failure(MissionValidationException(MissionError.StartTooEarly))
        val viewModel = StartMissionViewModel(startMission)
        viewModel.start("m1")

        viewModel.dismissError()
        coEvery { startMission("m1") } returns Result.success(inProgress)
        viewModel.start("m1")

        assertNull(viewModel.uiState.value.error)
        assertEquals(inProgress, viewModel.uiState.value.startedMission)
    }

    @Test
    fun secondStartAfterSuccessDoesNotCallUseCaseAgain() {
        coEvery { startMission("m1") } returns Result.success(inProgress)
        val viewModel = StartMissionViewModel(startMission)
        viewModel.start("m1")

        viewModel.start("m1")

        coVerify(exactly = 1) { startMission("m1") }
    }

    @Test
    fun consumeStartedMissionResetsState() {
        coEvery { startMission("m1") } returns Result.success(inProgress)
        val viewModel = StartMissionViewModel(startMission)
        viewModel.start("m1")

        viewModel.consumeStartedMission()

        assertNull(viewModel.uiState.value.startedMission)
    }
}
