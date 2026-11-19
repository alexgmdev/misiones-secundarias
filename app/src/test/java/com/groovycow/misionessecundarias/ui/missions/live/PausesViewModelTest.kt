package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import com.groovycow.misionessecundarias.domain.usecase.GetActivePausesUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetMissionPauseUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pruebas de HU-054 y HU-055: casillas de pausa del publicador. */
class PausesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<PauseRepository>()

    private fun viewModel(initial: Set<PauseKind> = emptySet()): PausesViewModel {
        coEvery { repository.getActivePauses(listOf("m1")) } returns
            if (initial.isEmpty()) emptyMap() else mapOf("m1" to initial)
        return PausesViewModel(GetActivePausesUseCase(repository), SetMissionPauseUseCase(repository))
            .also { it.load("m1") }
    }

    @Test
    fun loadShowsActivePauses() {
        val viewModel = viewModel(setOf(PauseKind.SOCIALIZATION))

        assertEquals(setOf(PauseKind.SOCIALIZATION), viewModel.uiState.value.active)
    }

    @Test
    fun loadWithoutPausesLeavesEverythingOff() {
        assertTrue(viewModel().uiState.value.active.isEmpty())
    }

    @Test
    fun togglingActivatesHydrationPause() {
        val viewModel = viewModel()
        coEvery { repository.setPause("m1", PauseKind.HYDRATION, true) } returns Unit

        viewModel.toggle("m1", PauseKind.HYDRATION)

        assertEquals(setOf(PauseKind.HYDRATION), viewModel.uiState.value.active)
        assertFalse(viewModel.uiState.value.isBusy)
        coVerify(exactly = 1) { repository.setPause("m1", PauseKind.HYDRATION, true) }
    }

    @Test
    fun togglingAnActivePauseDeactivatesIt() {
        val viewModel = viewModel(setOf(PauseKind.SOCIALIZATION, PauseKind.HYDRATION))
        coEvery { repository.setPause("m1", PauseKind.HYDRATION, false) } returns Unit

        viewModel.toggle("m1", PauseKind.HYDRATION)

        assertEquals(setOf(PauseKind.SOCIALIZATION), viewModel.uiState.value.active)
    }

    @Test
    fun severalPausesCanBeActiveAtTheSameTime() {
        val viewModel = viewModel()
        coEvery { repository.setPause(any(), any(), true) } returns Unit

        viewModel.toggle("m1", PauseKind.SOCIALIZATION)
        viewModel.toggle("m1", PauseKind.SHORT_BREAK)

        assertEquals(setOf(PauseKind.SOCIALIZATION, PauseKind.SHORT_BREAK), viewModel.uiState.value.active)
    }

    @Test
    fun failedToggleKeepsPreviousStateAndShowsError() {
        val viewModel = viewModel()
        coEvery { repository.setPause("m1", PauseKind.HYDRATION, true) } throws IllegalStateException("sin permiso")

        viewModel.toggle("m1", PauseKind.HYDRATION)

        assertTrue(viewModel.uiState.value.active.isEmpty())
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun loadFailureKeepsVisiblePausesAndSetsError() {
        val viewModel = viewModel(setOf(PauseKind.SOCIALIZATION))
        coEvery { repository.getActivePauses(listOf("m1")) } throws IllegalStateException("sin red")

        viewModel.load("m1")

        assertEquals(setOf(PauseKind.SOCIALIZATION), viewModel.uiState.value.active)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun dismissErrorClearsMessage() {
        val viewModel = viewModel()
        coEvery { repository.setPause(any(), any(), any()) } throws IllegalStateException("x")
        viewModel.toggle("m1", PauseKind.HYDRATION)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}
