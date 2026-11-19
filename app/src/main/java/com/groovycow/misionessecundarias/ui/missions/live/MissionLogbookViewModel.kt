package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.SaveMissionLogbookUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la bitácora obligatoria de vivencia de la misión (HU - Evaluación y Cierre: Publicador + Participantes). */
data class LogbookUiState(
    /** Texto redactado por el usuario para la bitácora ('Añadir Bitácora'). */
    val entry: String = "",
    /** Indica si se está guardando la bitácora. */
    val isSaving: Boolean = false,
    /** Registro de bitácora ya guardado exitosamente por el usuario. */
    val savedLogbook: MissionLogbook? = null,
    /** Error de validación o servidor. */
    val error: MissionError? = null,
)

/** Maneja la edición y guardado de la bitácora de vivencia de la misión. */
class MissionLogbookViewModel(
    private val saveMissionLogbookUseCase: SaveMissionLogbookUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogbookUiState())
    val uiState: StateFlow<LogbookUiState> = _uiState.asStateFlow()

    /** Actualiza el relato redactado en el campo de texto. */
    fun onEntryChanged(newEntry: String) {
        _uiState.value = _uiState.value.copy(entry = newEntry, error = null)
    }

    /** Guarda la bitácora oficial del publicador vinculando los participantes. */
    fun saveLogbook(
        missionId: String,
        participants: List<MissionApplicant>,
    ) {
        if (_uiState.value.isSaving || _uiState.value.savedLogbook != null) return
        val currentEntry = _uiState.value.entry

        if (currentEntry.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(error = MissionError.LogbookEntryRequired)
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            saveMissionLogbookUseCase(missionId, currentEntry, participants).fold(
                onSuccess = { logbook ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            savedLogbook = logbook,
                            error = null,
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            error = exception.toMissionError(),
                        )
                },
            )
        }
    }

    /** Guarda las palabras / experiencia de un participante solucionador. */
    fun saveParticipantLogbook(missionId: String) {
        if (_uiState.value.isSaving || _uiState.value.savedLogbook != null) return
        val currentEntry = _uiState.value.entry

        if (currentEntry.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(error = MissionError.LogbookEntryRequired)
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            saveMissionLogbookUseCase.saveParticipantEntry(missionId, currentEntry).fold(
                onSuccess = { logbook ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            savedLogbook = logbook,
                            error = null,
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            error = exception.toMissionError(),
                        )
                },
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
