package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.CompleteMissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la acción "Completar Misión" (HU-064). */
data class CompleteMissionUiState(
    /** Indica que la solicitud está en curso. */
    val isCompleting: Boolean = false,
    /** Misión ya completada tras una respuesta exitosa. */
    val completedMission: Mission? = null,
    /** Motivo por el que no se pudo completar. */
    val error: MissionError? = null,
)

/** Ejecuta el cierre de una misión y expone el resultado a la pantalla. */
class CompleteMissionViewModel(
    private val completeMissionUseCase: CompleteMissionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompleteMissionUiState())
    val uiState: StateFlow<CompleteMissionUiState> = _uiState.asStateFlow()

    /** Completa la misión y bloquea solicitudes duplicadas mientras espera al backend. */
    fun complete(missionId: String) {
        if (_uiState.value.isCompleting || _uiState.value.completedMission != null) return
        _uiState.value = CompleteMissionUiState(isCompleting = true)

        viewModelScope.launch {
            completeMissionUseCase(missionId).fold(
                onSuccess = { mission -> _uiState.value = CompleteMissionUiState(completedMission = mission) },
                onFailure = { exception -> _uiState.value = CompleteMissionUiState(error = exception.toMissionError()) },
            )
        }
    }

    /** Cierra el aviso de error para permitir un nuevo intento. */
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
