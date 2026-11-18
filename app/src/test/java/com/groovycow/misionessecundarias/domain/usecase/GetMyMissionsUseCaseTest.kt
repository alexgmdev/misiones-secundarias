package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del filtro "Mis misiones": misiones creadas por el usuario en cualquier estado. */
class GetMyMissionsUseCaseTest {
    private val repository = mockk<MissionRepository>()

    @Test
    fun returnsOwnMissionsInEveryStatus() =
        runTest {
            val own = listOf(mission("m1", "Borrador"), mission("m2", "Publicada"), mission("m3", "En progreso"))
            coEvery { repository.getMyMissions() } returns own

            val result = GetMyMissionsUseCase(repository)()

            assertEquals(own, result.getOrNull())
            coVerify(exactly = 0) { repository.getPublishedMissions() }
        }

    @Test
    fun returnsEmptyListWhenUserHasNoMissions() =
        runTest {
            coEvery { repository.getMyMissions() } returns emptyList()

            val result = GetMyMissionsUseCase(repository)()

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull().orEmpty().isEmpty())
        }

    @Test
    fun wrapsRepositoryFailureInResult() =
        runTest {
            coEvery { repository.getMyMissions() } throws IllegalStateException("sin sesión")

            val result = GetMyMissionsUseCase(repository)()

            assertTrue(result.isFailure)
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
