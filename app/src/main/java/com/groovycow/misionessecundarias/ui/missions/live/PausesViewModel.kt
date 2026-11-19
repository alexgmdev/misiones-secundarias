package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.usecase.GetActivePausesUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetMissionPauseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de las pausas que el publicador activa en la misión en ejecución. */
data class PausesUiState(
    val active: Set<PauseKind> = emptySet(),
    val isBusy: Boolean = false,
    val error: String? = null,
)

/** Activa o desactiva las pausas (socialización, hidratación, pausa breve) de una misión propia. */
class PausesViewModel(
    private val getActivePauses: GetActivePausesUseCase,
    private val setMissionPause: SetMissionPauseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PausesUiState())
    val uiState: StateFlow<PausesUiState> = _uiState.asStateFlow()

    /** Carga las pausas activas; si falla, conserva lo visible y muestra el error solo en la primera carga. */
    fun load(missionId: String) {
        viewModelScope.launch {
            if (_uiState.value.isBusy) return@launch
            getActivePauses(listOf(missionId)).fold(
                onSuccess = { byMission ->
                    if (!_uiState.value.isBusy) {
                        _uiState.value = PausesUiState(active = byMission[missionId].orEmpty())
                    }
                },
                onFailure = { _uiState.value = _uiState.value.copy(error = LOAD_ERROR) },
            )
        }
    }

    /** Activa [kind] si estaba apagada y la apaga si estaba activa. */
    fun toggle(
        missionId: String,
        kind: PauseKind,
    ) {
        if (_uiState.value.isBusy) return
        val activate = kind !in _uiState.value.active
        _uiState.value = _uiState.value.copy(isBusy = true, error = null)
        viewModelScope.launch {
            setMissionPause(missionId, kind, activate).fold(
                onSuccess = {
                    val updated = if (activate) _uiState.value.active + kind else _uiState.value.active - kind
                    _uiState.value = PausesUiState(active = updated)
                },
                onFailure = { _uiState.value = _uiState.value.copy(isBusy = false, error = SAVE_ERROR) },
            )
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private companion object {
        const val LOAD_ERROR = "No fue posible cargar las pausas. Inténtalo nuevamente."
        const val SAVE_ERROR = "No fue posible guardar la pausa. Inténtalo nuevamente."
    }
}
