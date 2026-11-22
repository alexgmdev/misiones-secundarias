package com.groovycow.misionessecundarias.ui.admin

import com.groovycow.misionessecundarias.domain.usecase.CanAccessAdminPanelUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminMenuViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val canAccessAdminPanelUseCase = mockk<CanAccessAdminPanelUseCase>()

    @Test
    fun moderatorRoleShowsTheAdminPanelEntry() {
        coEvery { canAccessAdminPanelUseCase() } returns Result.success(true)
        val viewModel = AdminMenuViewModel(canAccessAdminPanelUseCase)

        viewModel.refreshAccess()

        assertTrue(viewModel.canShowAdminPanel.value)
    }

    @Test
    fun standardUserRoleHidesTheAdminPanelEntry() {
        coEvery { canAccessAdminPanelUseCase() } returns Result.success(false)
        val viewModel = AdminMenuViewModel(canAccessAdminPanelUseCase)

        viewModel.refreshAccess()

        assertFalse(viewModel.canShowAdminPanel.value)
    }
}
