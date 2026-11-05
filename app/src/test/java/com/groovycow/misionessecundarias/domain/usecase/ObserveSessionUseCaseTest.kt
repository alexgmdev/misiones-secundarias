package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSessionUseCaseTest {
    @Test
    fun emitsTheStatesOfTheRepositoryInOrder() =
        runTest {
            val repository = mockk<AuthRepository>()
            every { repository.observeSession() } returns
                flowOf(SessionState.LOADING, SessionState.AUTHENTICATED)

            val states = ObserveSessionUseCase(repository)().toList()

            assertEquals(listOf(SessionState.LOADING, SessionState.AUTHENTICATED), states)
        }

    @Test
    fun emitsUnauthenticatedWhenThereIsNoSession() =
        runTest {
            val repository = mockk<AuthRepository>()
            every { repository.observeSession() } returns
                flowOf(SessionState.LOADING, SessionState.UNAUTHENTICATED)

            val states = ObserveSessionUseCase(repository)().toList()

            assertEquals(SessionState.UNAUTHENTICATED, states.last())
        }
}
