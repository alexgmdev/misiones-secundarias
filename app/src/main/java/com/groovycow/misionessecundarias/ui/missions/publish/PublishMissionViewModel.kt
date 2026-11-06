package com.groovycow.misionessecundarias.ui.missions.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.PublishMissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de una acción de publicación de misión. */
data class PublishMissionUiState(
    /** Borrador actual, se conserva si la publicación falla. */
    val draft: Mission? = null,
    /** Indica que la solicitud de publicación está en curso. */
    val isPublishing: Boolean = false,
    /** Misión después de una publicación exitosa. */
    val publishedMission: Mission? = null,
    /** Error de validación, red o servidor. */
    val error: MissionError? = null,
)

/** Ejecuta y representa la publicación de un borrador. */
class PublishMissionViewModel(
    private val publishMissionUseCase: PublishMissionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublishMissionUiState())

    /** Estado actual de la publicación. */
    val uiState: StateFlow<PublishMissionUiState> = _uiState.asStateFlow()

    /** Publica [mission] y bloquea solicitudes duplicadas mientras espera al backend. */
    fun publish(mission: Mission) {
        if (_uiState.value.isPublishing || _uiState.value.publishedMission != null) return
        _uiState.value = PublishMissionUiState(draft = mission, isPublishing = true)

        viewModelScope.launch {
            publishMissionUseCase(mission).fold(
                onSuccess = { published ->
                    _uiState.value = PublishMissionUiState(publishedMission = published)
                },
                onFailure = { exception ->
                    _uiState.value =
                        PublishMissionUiState(
                            draft = mission,
                            error = exception.toMissionError(),
                        )
                },
            )
        }
    }

    /** Restablece el estado para la siguiente misión creada. */
    fun reset() {
        _uiState.value = PublishMissionUiState()
    }

    private fun Throwable.toMissionError(): MissionError =
        when (this) {
            is MissionValidationException -> error
            else -> {
                if (hasIOExceptionCause()) MissionError.NoConnection else MissionError.ServerError
            }
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
