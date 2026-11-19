package com.groovycow.misionessecundarias.ui.missions.rating

import com.groovycow.misionessecundarias.domain.model.PublisherRating
import com.groovycow.misionessecundarias.domain.model.RatingError
import com.groovycow.misionessecundarias.domain.model.RatingValidationException
import com.groovycow.misionessecundarias.domain.usecase.RatePublisherUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class RatePublisherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase = mockk<RatePublisherUseCase>()
    private val viewModel = RatePublisherViewModel(useCase)

    @Test
    fun openPreparesDialogWithFiveStars() {
        viewModel.open("m1")

        assertEquals("m1", viewModel.uiState.value.missionId)
        assertEquals(5, viewModel.uiState.value.stars)
        assertFalse(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun selectedStarsAreClampedToOneToFive() {
        viewModel.open("m1")

        viewModel.selectStars(0)
        assertEquals(1, viewModel.uiState.value.stars)

        viewModel.selectStars(9)
        assertEquals(5, viewModel.uiState.value.stars)
    }

    @Test
    fun successfulSubmitMarksRatingAsCompleted() =
        runTest {
            viewModel.open("m1")
            viewModel.selectStars(4)
            coEvery { useCase("m1", 4) } returns Result.success(rating())

            viewModel.submit()

            assertTrue(viewModel.uiState.value.isCompleted)
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals("¡Gracias! Tu calificación quedó registrada.", viewModel.uiState.value.message)
            coVerify(exactly = 1) { useCase("m1", 4) }
        }

    @Test
    fun domainFailureShowsSpanishMessageAndKeepsDialogOpen() =
        runTest {
            viewModel.open("m1")
            coEvery { useCase("m1", 5) } returns
                Result.failure(RatingValidationException(RatingError.AlreadyRated))

            viewModel.submit()

            assertFalse(viewModel.uiState.value.isCompleted)
            assertTrue(viewModel.uiState.value.isError)
            assertEquals("Ya calificaste esta misión", viewModel.uiState.value.message)
        }

    @Test
    fun networkFailureShowsConnectionMessage() =
        runTest {
            viewModel.open("m1")
            coEvery { useCase("m1", 5) } returns Result.failure(IOException("offline"))

            viewModel.submit()

            assertTrue(viewModel.uiState.value.isError)
            assertEquals("Sin conexión. Intenta de nuevo", viewModel.uiState.value.message)
        }

    @Test
    fun dismissClearsState() {
        viewModel.open("m1")

        viewModel.dismiss()

        assertNull(viewModel.uiState.value.missionId)
    }

    private fun rating() =
        PublisherRating(
            id = "r1",
            missionId = "m1",
            solverId = "s1",
            publisherId = "p1",
            stars = 4,
        )
}
