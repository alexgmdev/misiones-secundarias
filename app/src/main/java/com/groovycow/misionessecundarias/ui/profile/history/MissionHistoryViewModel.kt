package com.groovycow.misionessecundarias.ui.profile.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MissionHistoryEntry
import com.groovycow.misionessecundarias.domain.model.MissionLogbookSections
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.usecase.GetMissionHistoryUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionLogbookSectionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Estado de las secciones de bitácora de una misión expandida.
 *
 * @property isLoading `true` mientras se consultan.
 * @property sections Secciones cargadas, o null si aún no hay o falló la carga.
 * @property failed `true` si la última carga falló (se muestra el estado vacío).
 */
data class LogbookUiState(
    val isLoading: Boolean = false,
    val sections: MissionLogbookSections? = null,
    val failed: Boolean = false,
)

/**
 * Estado del historial de misiones (HU-011).
 *
 * @property entries Misiones de la línea temporal.
 * @property isLoading `true` mientras se carga.
 * @property error Error de la última carga, o null.
 * @property expandedId Misión expandida, o null.
 * @property logbooks Bitácoras cargadas por misión (caché).
 */
data class MissionHistoryUiState(
    val entries: List<MissionHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: ProfileError? = null,
    val expandedId: String? = null,
    val logbooks: Map<String, LogbookUiState> = emptyMap(),
)

/**
 * ViewModel del historial de misiones (HU-011): carga la línea temporal y las bitácoras al expandir.
 *
 * @param getHistory Caso de uso del historial.
 * @param getLogbooks Caso de uso de las secciones de bitácora.
 */
class MissionHistoryViewModel(
    private val getHistory: GetMissionHistoryUseCase,
    private val getLogbooks: GetMissionLogbookSectionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MissionHistoryUiState())

    /** Estado observable. */
    val uiState: StateFlow<MissionHistoryUiState> = _uiState.asStateFlow()

    /** Carga el historial. */
    fun load() {
        _uiState.value = MissionHistoryUiState(isLoading = true)
        viewModelScope.launch {
            getHistory()
                .onSuccess { _uiState.value = MissionHistoryUiState(entries = it) }
                .onFailure { _uiState.value = MissionHistoryUiState(error = it.toProfileError()) }
        }
    }

    /** Reintenta la carga tras un error. */
    fun retry() = load()

    /**
     * Expande o contrae una misión; al expandir carga su bitácora si no está en caché.
     *
     * @param missionId Misión tocada.
     */
    fun toggle(missionId: String) {
        val expanding = _uiState.value.expandedId != missionId
        _uiState.update { it.copy(expandedId = if (expanding) missionId else null) }
        val cached = _uiState.value.logbooks[missionId]
        if (expanding && (cached == null || cached.failed)) loadLogbook(missionId)
    }

    private fun loadLogbook(missionId: String) {
        setLogbook(missionId, LogbookUiState(isLoading = true))
        viewModelScope.launch {
            getLogbooks(missionId)
                .onSuccess { setLogbook(missionId, LogbookUiState(sections = it)) }
                .onFailure { setLogbook(missionId, LogbookUiState(failed = true)) }
        }
    }

    private fun setLogbook(
        missionId: String,
        state: LogbookUiState,
    ) = _uiState.update { it.copy(logbooks = it.logbooks + (missionId to state)) }

    private fun Throwable.toProfileError(): ProfileError =
        if (generateSequence(this) { it.cause }.any { it is IOException }) ProfileError.NoConnection else ProfileError.ServerError
}
