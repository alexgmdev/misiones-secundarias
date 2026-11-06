package com.groovycow.misionessecundarias.ui.missions.create

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MissionModalitySelectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsModalityChoicesBeforeAnyMissionForm() {
        setMissionScreen()

        composeTestRule.onNodeWithText("¿Qué tipo de misión quieres crear?").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Punto de encuentro").assertCountEquals(0)
    }

    @Test
    fun selectingFieldMissionShowsMeetingPointAndCoordinates() {
        setMissionScreen()

        composeTestRule.onNodeWithText("Misión de Campo").performClick()
        composeTestRule.onNodeWithText("Punto de encuentro").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Latitud").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Longitud").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingRemoteMissionHidesPhysicalLocationFields() {
        setMissionScreen()

        composeTestRule.onNodeWithText("Misión Remota").performClick()

        composeTestRule.onNodeWithText("Misión Remota").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Punto de encuentro").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Latitud").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Longitud").assertCountEquals(0)
    }

    private fun setMissionScreen() {
        val repository = UnusedMissionRepository()
        val clock = Clock.fixed(Instant.parse("2026-11-05T17:00:00Z"), ZoneId.of("America/Bogota"))
        val createMissionViewModel =
            CreateMissionViewModel(
                CreateMissionUseCase(repository),
                MissionDraftValidator(clock),
            )
        composeTestRule.setContent {
            MaterialTheme {
                CreateMissionScreen(
                    onBack = {},
                    onDraftSaved = {},
                    viewModel = createMissionViewModel,
                )
            }
        }
    }
}

private class UnusedMissionRepository : MissionRepository {
    override suspend fun createMission(draft: CreateMissionDraft): Mission = error("No se espera guardar misiones en esta prueba")

    override suspend fun publishMission(id: String): Mission = error("No se espera publicar misiones en esta prueba")

    override suspend fun getPublishedMissions(): List<Mission> = emptyList()

    override suspend fun getMissionById(id: String): Mission? = null
}
