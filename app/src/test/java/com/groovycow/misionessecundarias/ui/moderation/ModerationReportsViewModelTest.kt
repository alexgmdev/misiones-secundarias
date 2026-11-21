package com.groovycow.misionessecundarias.ui.moderation

import com.groovycow.misionessecundarias.domain.model.ModerationError
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ModerationValidationException
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReporterProfile
import com.groovycow.misionessecundarias.domain.usecase.GetModerationReportsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Pruebas unitarias para el ViewModel de la cola de moderación. */
class ModerationReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getModerationReportsUseCase = mockk<GetModerationReportsUseCase>()
    private val reportHigh = ModerationReport("r1", "u1", "Misión", "m1", "Inapropiado", priority = "Alta", reporterProfile = ReporterProfile("Juan"))

    @Before
    fun setUp() {
        clearMocks(getModerationReportsUseCase)
    }

    @Test
    fun loadReportsSuccessExposesSuccessState() {
        coEvery { getModerationReportsUseCase(any(), any(), any(), any()) } returns Result.success(listOf(reportHigh))

        val viewModel = ModerationReportsViewModel(getModerationReportsUseCase)

        val state = viewModel.uiState.value as ModerationReportsUiState.Success
        assertEquals(1, state.reports.size)
        assertEquals("r1", state.reports[0].id)
    }

    @Test
    fun accessDeniedExposesAccessDeniedState() {
        coEvery { getModerationReportsUseCase(any(), any(), any(), any()) } returns
            Result.failure(ModerationValidationException(ModerationError.AccessDenied))

        val viewModel = ModerationReportsViewModel(getModerationReportsUseCase)

        assertEquals(ModerationReportsUiState.AccessDenied, viewModel.uiState.value)
    }

    @Test
    fun updateSortOrderTriggersNewQuery() {
        coEvery { getModerationReportsUseCase(any(), any(), any(), any()) } returns Result.success(listOf(reportHigh))

        val viewModel = ModerationReportsViewModel(getModerationReportsUseCase)
        viewModel.updateSortOrder(ReportSortOrder.DATE_NEWEST_FIRST)

        val state = viewModel.uiState.value as ModerationReportsUiState.Success
        assertEquals(ReportSortOrder.DATE_NEWEST_FIRST, state.sortOrder)
    }

    @Test
    fun networkFailureExposesNoConnectionErrorState() {
        coEvery { getModerationReportsUseCase(any(), any(), any(), any()) } returns Result.failure(IllegalStateException("x", IOException()))

        val viewModel = ModerationReportsViewModel(getModerationReportsUseCase)

        assertEquals(ModerationReportsUiState.Error(ModerationError.NoConnection), viewModel.uiState.value)
    }
}
