package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.StartMissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la acción "Iniciar Misión" (HU-050). */
data class StartMissionUiState(
    /** Indica que la solicitud de inicio está en curso. */
    val isStarting: Boolean = false,
    /** Misión ya en progreso tras un inicio exitoso. */
    val startedMission: Mission? = null,
    /** Motivo por el que no se pudo iniciar. */
    val error: MissionError? = null,
)

/** Ejecuta el inicio de una misión y expone el resultado a la pantalla. */
class StartMissionViewModel(
    private val startMissionUseCase: StartMissionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartMissionUiState())
    val uiState: StateFlow<StartMissionUiState> = _uiState.asStateFlow()

    /** Inicia la misión y bloquea solicitudes duplicadas mientras espera al backend. */
    fun start(missionId: String) {
        if (_uiState.value.isStarting || _uiState.value.startedMission != null) return
        _uiState.value = StartMissionUiState(isStarting = true)

        viewModelScope.launch {
            startMissionUseCase(missionId).fold(
                onSuccess = { mission ->
                    _uiState.value = StartMissionUiState(startedMission = mission)
                },
                onFailure = { exception ->
                    _uiState.value = StartMissionUiState(error = exception.toMissionError())
                },
            )
        }
    }

    /** Cierra el aviso de error para permitir un nuevo intento. */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** Limpia el resultado después de navegar al panel. */
    fun consumeStartedMission() {
        _uiState.value = StartMissionUiState()
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
