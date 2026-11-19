package com.groovycow.misionessecundarias.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyMissionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MissionUiState(
    val isLoading: Boolean = true,
    val missions: List<Mission> = emptyList(),
    val error: String? = null,
)

class MissionViewModel(
    private val getMissionsUseCase: GetMissionsUseCase,
    private val getMyMissionsUseCase: GetMyMissionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = _uiState.asStateFlow()

    init {
        loadMissions()
    }

    fun loadMissions() = load(getMissionsUseCase::invoke)

    /** Carga solo las misiones creadas por el usuario actual (filtro "Mis misiones"). */
    fun loadMyMissions() = load(getMyMissionsUseCase::invoke)

    private fun load(query: suspend () -> Result<List<Mission>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            query().fold(
                onSuccess = { missions ->
                    _uiState.value = MissionUiState(isLoading = false, missions = missions)
                },
                onFailure = { exception ->
                    _uiState.value = MissionUiState(isLoading = false, error = exception.message)
                },
            )
        }
    }
}
