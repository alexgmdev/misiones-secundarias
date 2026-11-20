package com.groovycow.misionessecundarias.ui.missions.edit

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.UpdateMissionDetailsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class EditMissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase = mockk<UpdateMissionDetailsUseCase>()
    private val viewModel = EditMissionViewModel(useCase)

    @Test
    fun loadFillsFieldsFromMission() {
        viewModel.load(mission())

        val state = viewModel.uiState.value
        assertEquals("m1", state.missionId)
        assertEquals("Siembra", state.title)
        assertEquals("Guantes", state.requirements)
        assertEquals("Hidratación", state.reward)
    }

    @Test
    fun blankFieldsAreRejectedWithoutCallingUseCase() {
        viewModel.load(mission())
        viewModel.onDescriptionChange(" ")

        viewModel.save()

        assertEquals(MissionError.InvalidDetails, viewModel.uiState.value.error)
        coVerify(exactly = 0) { useCase(any(), any()) }
    }

    @Test
    fun successfulSaveExposesUpdatedMission() =
        runTest {
            val updated = mission().copy(title = "Siembra corregida")
            coEvery { useCase("m1", any()) } returns Result.success(updated)
            viewModel.load(mission())
            viewModel.onTitleChange("Siembra corregida")

            viewModel.save()

            assertEquals(updated, viewModel.uiState.value.savedMission)
            assertFalse(viewModel.uiState.value.isSaving)
            coVerify(exactly = 1) {
                useCase("m1", match<MissionDetailsUpdate> { it.title == "Siembra corregida" })
            }
        }

    @Test
    fun acceptedParticipantsErrorIsShown() =
        runTest {
            coEvery { useCase("m1", any()) } returns
                Result.failure(MissionValidationException(MissionError.HasAcceptedParticipants))
            viewModel.load(mission())

            viewModel.save()

            assertEquals(MissionError.HasAcceptedParticipants, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.savedMission)
        }

    @Test
    fun networkFailureKeepsFormAndShowsConnectionError() =
        runTest {
            coEvery { useCase("m1", any()) } returns Result.failure(IOException("offline"))
            viewModel.load(mission())

            viewModel.save()

            assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
            assertEquals("Siembra", viewModel.uiState.value.title)
        }

    private fun mission() =
        Mission(
            id = "m1",
            publisherId = "p1",
            title = "Siembra",
            description = "Jornada de siembra",
            requirements = "Guantes",
            category = "Medio ambiente",
            modality = "remota",
            dateTime = "2026-11-15T10:00:00-05:00",
            reward = "Hidratación",
            maxParticipants = 4,
            status = "Publicada",
        )
}
