package com.groovycow.misionessecundarias.ui.admin

import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import com.groovycow.misionessecundarias.domain.usecase.GetAdminMetricsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminPanelViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAdminMetricsUseCase = mockk<GetAdminMetricsUseCase>()

    @Test
    fun successfulAccessExposesOperationalDashboardMetrics() {
        val metrics =
            AdminMetrics(
                totalMissions = 10,
                activeMissions = 4,
                completedMissions = 5,
                totalUsers = 25,
                activeSolvers = 8,
            )
        coEvery { getAdminMetricsUseCase() } returns Result.success(metrics)

        val viewModel = AdminPanelViewModel(getAdminMetricsUseCase)

        assertEquals(AdminPanelUiState.Success(metrics), viewModel.uiState.value)
        coVerify(exactly = 1) { getAdminMetricsUseCase() }
    }
}
