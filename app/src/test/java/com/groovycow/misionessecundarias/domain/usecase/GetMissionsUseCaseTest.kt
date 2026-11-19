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
        repository = mockk()
        useCase = GetMissionsUseCase(repository)
    }

    @Test
    fun shouldReturnSuccessWithListOfMissionsWhenRepositorySucceeds() =
        runTest {
            val expectedMissions =
                listOf(
                    Mission(
                        id = "1",
                        publisherId = "pub1",
                        title = "Test",
                        description = "Desc",
                        category = null,
                        requirements = null,
                        modality = "campo",
                        dateTime = "2026-10-31",
                        location = null,
                        maxParticipants = 2,
                        status = "Publicada",
                        publisherProfile = null,
                    ),
                )
            coEvery { repository.getPublishedMissions() } returns expectedMissions

            val result = useCase.invoke()

            assertTrue(result.isSuccess)
            assertEquals(expectedMissions, result.getOrNull())
        }

    @Test
    fun shouldReturnFailureWhenRepositoryThrowsException() =
        runTest {
            val exception = Exception("Network Error")
            coEvery { repository.getPublishedMissions() } throws exception

            val result = useCase.invoke()

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
