package com.groovycow.misionessecundarias.ui.missions.publish

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.PublishMissionUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PublishMissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val publishMissionUseCase = mockk<PublishMissionUseCase>()

    @Test
    fun successfulPublicationExposesPublishedMission() {
        val viewModel = newViewModel()
        val published = draft.copy(status = "Publicada")
        coEvery { publishMissionUseCase(draft) } returns Result.success(published)

        viewModel.publish(draft)

        assertEquals(published, viewModel.uiState.value.publishedMission)
        assertFalse(viewModel.uiState.value.isPublishing)
        coVerify(exactly = 1) { publishMissionUseCase(draft) }
    }

    @Test
    fun publicationGuardPreventsDuplicateRequest() {
        val viewModel = newViewModel()
        val gate = CompletableDeferred<Result<Mission>>()
        coEvery { publishMissionUseCase(draft) } coAnswers { gate.await() }

        viewModel.publish(draft)
        viewModel.publish(draft)

        assertTrue(viewModel.uiState.value.isPublishing)
        coVerify(exactly = 1) { publishMissionUseCase(draft) }

        gate.complete(Result.success(draft.copy(status = "Publicada")))

        assertEquals("Publicada", viewModel.uiState.value.publishedMission?.status)
    }

    @Test
    fun networkFailurePreservesDraftAndShowsConnectionError() {
        val viewModel = newViewModel()
        coEvery { publishMissionUseCase(draft) } returns Result.failure(IOException("offline"))

        viewModel.publish(draft)

        assertEquals(draft.id, viewModel.uiState.value.draft?.id)
        assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
    }

    @Test
    fun validationFailureShowsSpecificMessageError() {
        val viewModel = newViewModel()
        coEvery { publishMissionUseCase(draft) } returns
            Result.failure(MissionValidationException(MissionError.MeetingPointRequired))

        viewModel.publish(draft)

        assertEquals(MissionError.MeetingPointRequired, viewModel.uiState.value.error)
        assertEquals(draft.id, viewModel.uiState.value.draft?.id)
    }

    private fun newViewModel() = PublishMissionViewModel(publishMissionUseCase)

    private companion object {
        val draft =
            Mission(
                id = "mission-id",
                publisherId = "publisher-id",
                title = "Siembra",
                description = "Descripción",
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
