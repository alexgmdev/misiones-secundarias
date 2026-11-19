package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.GenerateVerificationCodeUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Pruebas unitarias para el ViewModel de generación de código de verificación. */
class GenerateCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val generateVerificationCodeUseCase = mockk<GenerateVerificationCodeUseCase>()

    @Before
    fun setUp() {
        clearMocks(generateVerificationCodeUseCase)
    }

    @Test
    fun successExposesVerificationCode() {
        coEvery { generateVerificationCodeUseCase("m1") } returns Result.success("CALI2026")
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)

        viewModel.generateCode("m1")

        assertEquals("CALI2026", viewModel.uiState.value.verificationCode)
        assertFalse(viewModel.uiState.value.isGenerating)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun notCompletedErrorIsExposed() {
        coEvery { generateVerificationCodeUseCase("m1") } returns
            Result.failure(MissionValidationException(MissionError.MissionNotCompleted))
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)

        viewModel.generateCode("m1")

        assertEquals(MissionError.MissionNotCompleted, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.verificationCode)
    }

    @Test
    fun networkFailureMapsToNoConnection() {
        coEvery { generateVerificationCodeUseCase("m1") } returns Result.failure(IllegalStateException("x", IOException()))
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)

        viewModel.generateCode("m1")

        assertEquals(MissionError.NoConnection, viewModel.uiState.value.error)
    }

    @Test
    fun unknownFailureMapsToServerError() {
        coEvery { generateVerificationCodeUseCase("m1") } returns Result.failure(IllegalStateException("error"))
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)

        viewModel.generateCode("m1")

        assertEquals(MissionError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun dismissErrorClearsError() {
        coEvery { generateVerificationCodeUseCase("m1") } returns
            Result.failure(MissionValidationException(MissionError.MissionNotCompleted))
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)
        viewModel.generateCode("m1")

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun duplicateCallsWhileGeneratingAreIgnored() {
        coEvery { generateVerificationCodeUseCase("m1") } returns Result.success("CALI2026")
        val viewModel = GenerateCodeViewModel(generateVerificationCodeUseCase)
        viewModel.generateCode("m1")

        viewModel.generateCode("m1")

        coVerify(exactly = 1) { generateVerificationCodeUseCase("m1") }
    }
}
