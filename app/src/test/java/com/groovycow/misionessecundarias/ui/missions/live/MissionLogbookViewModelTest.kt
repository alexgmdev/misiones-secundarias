package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.ApplicantProfile
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.SaveMissionLogbookUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Pruebas unitarias para el ViewModel de bitácora de misión. */
class MissionLogbookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saveMissionLogbookUseCase = mockk<SaveMissionLogbookUseCase>()
    private val participant = MissionApplicant("a1", "m1", "s1", "Aceptado", "2026-11-20T10:00:00Z", ApplicantProfile("Juan Pérez"))
    private val expectedLogbook = MissionLogbook("l1", "m1", "p1", "publicador", "Excelente vivencia", listOf("s1"), listOf("Juan Pérez"))

    @Before
    fun setUp() {
        clearMocks(saveMissionLogbookUseCase)
    }

    @Test
    fun onEntryChangedUpdatesState() {
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("Texto inicial")

        assertEquals("Texto inicial", viewModel.uiState.value.entry)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun emptyEntryShowsValidationErrorWithoutCallingUseCase() {
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("   ")

        viewModel.saveLogbook("m1", listOf(participant))

        assertEquals(MissionError.LogbookEntryRequired, viewModel.uiState.value.error)
        coVerify(exactly = 0) { saveMissionLogbookUseCase(any(), any(), any()) }
    }

    @Test
    fun saveLogbookSuccessExposesSavedLogbook() {
        coEvery { saveMissionLogbookUseCase("m1", "Excelente vivencia", listOf(participant)) } returns Result.success(expectedLogbook)
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("Excelente vivencia")

        viewModel.saveLogbook("m1", listOf(participant))

        assertEquals(expectedLogbook, viewModel.uiState.value.savedLogbook)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun networkFailureMapsToNoConnection() {
        coEvery {
            saveMissionLogbookUseCase(
                "m1",
                "Excelente vivencia",
                listOf(participant),
            )
        } returns Result.failure(IllegalStateException("x", IOException()))
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("Excelente vivencia")

        viewModel.saveLogbook("m1", listOf(participant))

        assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.savedLogbook)
    }

    @Test
    fun validationExceptionIsExposed() {
        coEvery { saveMissionLogbookUseCase("m1", "Texto", listOf(participant)) } returns
            Result.failure(MissionValidationException(MissionError.LogbookEntryRequired))
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("Texto")

        viewModel.saveLogbook("m1", listOf(participant))

        assertEquals(MissionError.LogbookEntryRequired, viewModel.uiState.value.error)
    }

    @Test
    fun dismissErrorClearsError() {
        val viewModel = MissionLogbookViewModel(saveMissionLogbookUseCase)
        viewModel.onEntryChanged("")
        viewModel.saveLogbook("m1", emptyList())

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}
