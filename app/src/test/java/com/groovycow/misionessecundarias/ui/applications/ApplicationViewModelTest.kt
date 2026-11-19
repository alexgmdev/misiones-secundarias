package com.groovycow.misionessecundarias.ui.applications

import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CheckMissionOwnershipUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkArrivedUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkOnWayUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ApplicationViewModel
    private val applyToMissionUseCase: ApplyToMissionUseCase = mockk()
    private val checkMissionOwnershipUseCase: CheckMissionOwnershipUseCase = mockk()
    private val markOnWayUseCase: MarkOnWayUseCase = mockk()
    private val markArrivedUseCase: MarkArrivedUseCase = mockk()
    private val applicationRepository: ApplicationRepository = mockk()

    @Before
    fun setUp() {
        viewModel =
            ApplicationViewModel(
                applyToMissionUseCase,
                checkMissionOwnershipUseCase,
                markOnWayUseCase,
                markArrivedUseCase,
                applicationRepository,
            )
    }

    @Test
    fun `markOnWay updates state correctly on success`() =
        runTest {
            coEvery { markOnWayUseCase(any()) } returns Result.success(Unit)

            viewModel.markOnWay("mission123")

            assertTrue(viewModel.uiState.value.isOnWay)
            assertFalse(viewModel.uiState.value.isUpdatingStatus)
        }

    @Test
    fun `markArrived updates state correctly on success`() =
        runTest {
            coEvery { markArrivedUseCase(any(), any(), any()) } returns Result.success(Unit)

            viewModel.markArrived("mission123", 3.45, -76.53)

            assertTrue(viewModel.uiState.value.hasArrived)
            assertFalse(viewModel.uiState.value.isUpdatingStatus)
        }

    @Test
    fun `markOnWay handles failure correctly`() =
        runTest {
            coEvery { markOnWayUseCase(any()) } returns Result.failure(Exception("Network error"))

            viewModel.markOnWay("mission123")

            assertFalse(viewModel.uiState.value.isOnWay)
            assertFalse(viewModel.uiState.value.isUpdatingStatus)
            assertTrue(viewModel.uiState.value.isError)
        }
}
