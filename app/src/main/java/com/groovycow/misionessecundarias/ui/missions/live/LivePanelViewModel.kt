package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.usecase.GetLivePanelParticipantsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del panel de ejecución en vivo (HU-051). */
data class LivePanelUiState(
    val isLoading: Boolean = false,
    val participants: List<MissionApplicant> = emptyList(),
    val error: String? = null,
)

/** Carga y mantiene actualizada la lista de solucionadores presentes en la misión. */
class LivePanelViewModel(
    private val getParticipants: GetLivePanelParticipantsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LivePanelUiState())
    val uiState: StateFlow<LivePanelUiState> = _uiState.asStateFlow()

    /** Primera carga: muestra el indicador de progreso y limpia errores previos. */
    fun load(missionId: String) {
        viewModelScope.launch {
            _uiState.value = LivePanelUiState(isLoading = true)
            fetch(missionId)
        }
    }

    /**
     * Actualización silenciosa para reflejar incorporaciones o retiros.
     *
     * Si falla, conserva los avatares ya visibles y no muestra error.
     */
    fun refresh(missionId: String) {
        viewModelScope.launch {
            getParticipants(missionId).onSuccess { participants ->
                _uiState.value = LivePanelUiState(participants = participants)
            }
        }
    }

    private suspend fun fetch(missionId: String) {
        getParticipants(missionId).fold(
            onSuccess = { participants ->
                _uiState.value = LivePanelUiState(participants = participants)
            },
            onFailure = {
                _uiState.value =
                    LivePanelUiState(error = "No fue posible cargar el panel. Inténtalo nuevamente.")
            },
        )
    }
}

/** Iniciales (máximo dos) para dibujar el avatar; "?" si el nombre está vacío. */
fun participantInitials(fullName: String): String {
    val initials =
        fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
    return initials.ifEmpty { "?" }
}
