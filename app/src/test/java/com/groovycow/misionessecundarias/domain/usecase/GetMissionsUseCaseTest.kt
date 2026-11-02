package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetMissionsUseCaseTest {

    private lateinit var repository: MissionRepository
    private lateinit var useCase: GetMissionsUseCase

    @Before
    fun setUp() {
        // Arrange (Configuración inicial)
        repository = mockk()
        useCase = GetMissionsUseCase(repository)
    }

    @Test
    fun `should return success with list of missions when repository succeeds`() = runTest {
        // Arrange
        val expectedMissions = listOf(
            Mission(
                id = "1",
                publisherId = "pub1",
                title = "Test",
                description = "Desc",
                category = "campo",
                requirements = null,
                modality = "Presencial", // Asegúrate de poner el valor que requiera
                dateTime = "2026-10-31",
                location = null,
                maxParticipants = 2,
                status = "Publicada", // ¡Aquí está el que faltaba!
                publisherProfile = null
            )
        )
        coEvery { repository.getPublishedMissions() } returns expectedMissions

        // Act
        val result = useCase.invoke()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedMissions, result.getOrNull())
    }

    @Test
    fun `should return failure when repository throws exception`() = runTest {
        // Arrange
        val exception = Exception("Network Error")
        coEvery { repository.getPublishedMissions() } throws exception

        // Act
        val result = useCase.invoke()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}