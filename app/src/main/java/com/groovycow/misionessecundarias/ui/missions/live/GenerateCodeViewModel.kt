package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.GenerateVerificationCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la generación de código de verificación. */
data class GenerateCodeUiState(
    /** Indica que la solicitud está en curso. */
    val isGenerating: Boolean = false,
    /** Código de verificación generado exitosamente ('CALI2026'). */
    val verificationCode: String? = null,
    /** Motivo por el que no se pudo generar el código. */
    val error: MissionError? = null,
)

/** Maneja la acción de generar el código de verificación de cierre. */
class GenerateCodeViewModel(
    private val generateVerificationCodeUseCase: GenerateVerificationCodeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GenerateCodeUiState())
    val uiState: StateFlow<GenerateCodeUiState> = _uiState.asStateFlow()

    /** Genera el código único de verificación para la misión. */
    fun generateCode(missionId: String) {
        if (_uiState.value.isGenerating || _uiState.value.verificationCode != null) return
        _uiState.value = GenerateCodeUiState(isGenerating = true)

        viewModelScope.launch {
            generateVerificationCodeUseCase(missionId).fold(
                onSuccess = { code -> _uiState.value = GenerateCodeUiState(verificationCode = code) },
                onFailure = { exception -> _uiState.value = GenerateCodeUiState(error = exception.toMissionError()) },
            )
        }
    }

    /** Cierra el aviso de error. */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun Throwable.toMissionError(): MissionError =
        when {
            this is MissionValidationException -> error
            hasIOExceptionCause() -> MissionError.NoConnection
            else -> MissionError.ServerError
        }

    private fun Throwable.hasIOExceptionCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }
}
