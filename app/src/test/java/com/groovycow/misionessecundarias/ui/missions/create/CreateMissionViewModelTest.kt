package com.groovycow.misionessecundarias.ui.missions.create

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CreateMissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createMissionUseCase = mockk<CreateMissionUseCase>()
    private val validator =
        MissionDraftValidator(
            Clock.fixed(Instant.parse("2026-11-05T17:00:00Z"), ZoneId.of("America/Bogota")),
        )

    @Test
    fun invalidDraftShowsFieldErrorsAndDoesNotCallUseCase() {
        val viewModel = newViewModel()
        viewModel.onModalitySelected(MissionModality.CAMPO)

        viewModel.saveDraft()

        assertEquals(MissionError.Required, viewModel.uiState.value.fieldErrors[MissionField.TITLE])
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify(exactly = 0) { createMissionUseCase(any()) }
    }

    @Test
    fun validDraftCallsUseCaseAndExposesSavedMission() {
        val viewModel = newViewModel()
        viewModel.fillValidDraft()
        coEvery { createMissionUseCase(any()) } returns Result.success(createdMission)

        viewModel.saveDraft()

        assertEquals(createdMission, viewModel.uiState.value.savedMission)
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify(exactly = 1) { createMissionUseCase(any()) }
    }

    @Test
    fun savingGuardPreventsDuplicateSubmission() {
        val viewModel = newViewModel()
        viewModel.fillValidDraft()
        val gate = CompletableDeferred<Result<Mission>>()
        coEvery { createMissionUseCase(any()) } coAnswers { gate.await() }

        viewModel.saveDraft()
        viewModel.saveDraft()

        assertTrue(viewModel.uiState.value.isSaving)
        coVerify(exactly = 1) { createMissionUseCase(any()) }

        gate.complete(Result.success(createdMission))

        assertEquals(createdMission, viewModel.uiState.value.savedMission)
    }

    @Test
    fun networkFailureKeepsDraftAndSetsConnectionError() {
        val viewModel = newViewModel()
        viewModel.fillValidDraft()
        coEvery { createMissionUseCase(any()) } returns Result.failure(IOException("offline"))

        viewModel.saveDraft()

        assertEquals("Siembra en Parque Dog", viewModel.uiState.value.title)
        assertEquals(MissionError.NoConnection, viewModel.uiState.value.submissionError)
        assertNull(viewModel.uiState.value.savedMission)
    }

    @Test
    fun unexpectedFailureSetsServerErrorWithoutClearingDraft() {
        val viewModel = newViewModel()
        viewModel.fillValidDraft()
        coEvery { createMissionUseCase(any()) } returns Result.failure(IllegalStateException("server"))

        viewModel.saveDraft()

        assertEquals("Siembra en Parque Dog", viewModel.uiState.value.title)
        assertEquals(MissionError.ServerError, viewModel.uiState.value.submissionError)
    }

    @Test
    fun missingSessionShowsAuthenticationErrorWithoutClearingDraft() {
        val viewModel = newViewModel()
        viewModel.fillValidDraft()
        coEvery { createMissionUseCase(any()) } returns Result.failure(MissionSessionRequiredException())

        viewModel.saveDraft()

        assertEquals(MissionError.SessionRequired, viewModel.uiState.value.submissionError)
        assertEquals("Siembra en Parque Dog", viewModel.uiState.value.title)
        assertNull(viewModel.uiState.value.savedMission)
    }

    private fun newViewModel() = CreateMissionViewModel(createMissionUseCase, validator)

    private fun CreateMissionViewModel.fillValidDraft() {
        onModalitySelected(MissionModality.CAMPO)
        onTitleChange("Siembra en Parque Dog")
        onDescriptionChange("Jornada de siembra comunitaria")
        onRequirementsChange("Guantes")
        onCategoryChange("")
        onMeetingPointChange("Parque Dog Cali")
        onLatitudeChange("3.45")
        onLongitudeChange("-76.53")
        onMaxParticipantsChange("2")
        onDateChange(LocalDate.of(2026, 11, 15))
        onTimeChange(LocalTime.of(10, 0))
    }

    private companion object {
        val createdMission =
            Mission(
                id = "mission-id",
                publisherId = "publisher-id",
                title = "Siembra en Parque Dog",
                description = "Jornada de siembra comunitaria",
                category = null,
                requirements = "Guantes",
                modality = "campo",
                dateTime = "2026-11-15T10:00:00-05:00",
                location = "0101000020E6100000",
                meetingPoint = "Parque Dog Cali",
                maxParticipants = 2,
                status = "Borrador",
                publisherProfile = null,
            )
    }
}
