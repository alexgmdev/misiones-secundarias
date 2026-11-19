package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FilterMissionsUseCaseTest {
    private lateinit var useCase: FilterMissionsUseCase
    private lateinit var missions: List<Mission>

    @Before
    fun setUp() {
        useCase = FilterMissionsUseCase()
        missions =
            listOf(
                Mission(
                    id = "1", title = "Limpieza de parque", description = "Recoger basura",
                    modality = "campo", category = "Ecología", publisherId = "p1", dateTime = "", maxParticipants = 5,
                    status = "Publicada", location = "SRID=4326;POINT(-76.53 3.45)",
                ),
                Mission(
                    id = "2", title = "Diseño de logo", description = "Crear imagen corporativa",
                    modality = "remota", category = "Diseño", publisherId = "p2", dateTime = "", maxParticipants = 2,
                    status = "Publicada", location = null,
                ),
                Mission(
                    id = "3", title = "Ayuda con mercado", description = "Llevar bolsas pesadas",
                    modality = "campo", category = "Social", publisherId = "p3", dateTime = "", maxParticipants = 1,
                    status = "Publicada", location = "SRID=4326;POINT(-76.54 3.46)",
                ),
            )
    }

    @Test
    fun `filter by search query matches title`() {
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "parque",
                selectedModality = null,
                selectedCategory = null,
                selectedDistance = null,
                userLat = null,
                userLon = null,
            )
        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun `filter by search query matches description`() {
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "bolsas",
                selectedModality = null,
                selectedCategory = null,
                selectedDistance = null,
                userLat = null,
                userLon = null,
            )
        assertEquals(1, result.size)
        assertEquals("3", result.first().id)
    }

    @Test
    fun `filter by modality returns only remote missions`() {
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "",
                selectedModality = "remota",
                selectedCategory = null,
                selectedDistance = null,
                userLat = null,
                userLon = null,
            )
        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
    }

    @Test
    fun `filter by category returns correct mission`() {
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "",
                selectedModality = null,
                selectedCategory = "Social",
                selectedDistance = null,
                userLat = null,
                userLon = null,
            )
        assertEquals(1, result.size)
        assertEquals("3", result.first().id)
    }

    @Test
    fun `filter by distance returns missions within range`() {
        // Usuario en Cali (3.45, -76.53) - La misión 1 está muy cerca
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "",
                selectedModality = null,
                selectedCategory = null,
                selectedDistance = 10,
                userLat = 3.45,
                userLon = -76.53,
            )
        // Debería retornar misión 1 (muy cerca). Misión 3 está a ~1.5km. Misión 2 no tiene location.
        assertEquals(2, result.size)
    }

    @Test
    fun `filter combines search and category`() {
        val result =
            useCase.invoke(
                missions = missions,
                searchQuery = "ayuda",
                selectedModality = null,
                selectedCategory = "Social",
                selectedDistance = null,
                userLat = null,
                userLon = null,
            )
        assertEquals(1, result.size)
        assertEquals("3", result.first().id)
    }
}
