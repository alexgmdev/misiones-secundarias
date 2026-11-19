package com.groovycow.misionessecundarias.ui.missions

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyMissionsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** Pruebas de la lista de misiones y del filtro "Mis misiones". */
class MissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMissions = mockk<GetMissionsUseCase>()
    private val getMyMissions = mockk<GetMyMissionsUseCase>()
    private val published = listOf(mission("p1", "Publicada"), mission("p2", "Publicada"))
    private val mine = listOf(mission("m1", "Borrador"), mission("m2", "En progreso"))

    @Test
    fun loadMissionsShowsPublishedMissions() {
        coEvery { getMissions() } returns Result.success(published)

        val viewModel = MissionViewModel(getMissions, getMyMissions)

        assertEquals(published, viewModel.uiState.value.missions)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadMyMissionsReplacesListWithOwnMissions() {
        coEvery { getMissions() } returns Result.success(published)
        coEvery { getMyMissions() } returns Result.success(mine)
        val viewModel = MissionViewModel(getMissions, getMyMissions)

        viewModel.loadMyMissions()

        assertEquals(mine, viewModel.uiState.value.missions)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun switchingBackToAllShowsPublishedMissionsAgain() {
        coEvery { getMissions() } returns Result.success(published)
        coEvery { getMyMissions() } returns Result.success(mine)
        val viewModel = MissionViewModel(getMissions, getMyMissions)
        viewModel.loadMyMissions()

        viewModel.loadMissions()

        assertEquals(published, viewModel.uiState.value.missions)
    }

    @Test
    fun loadMyMissionsFailureShowsErrorAndNoMissions() {
        coEvery { getMissions() } returns Result.success(published)
        coEvery { getMyMissions() } returns Result.failure(IllegalStateException("sin sesión"))
        val viewModel = MissionViewModel(getMissions, getMyMissions)

        viewModel.loadMyMissions()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(emptyList<Mission>(), viewModel.uiState.value.missions)
    }

    private fun mission(
        id: String,
        status: String,
    ) = Mission(
        id = id,
        publisherId = "publisher-1",
        title = "Misión $id",
        description = "Descripción",
        modality = "campo",
        dateTime = "2026-11-20T14:00:00Z",
        maxParticipants = 5,
        status = status,
    )
}
