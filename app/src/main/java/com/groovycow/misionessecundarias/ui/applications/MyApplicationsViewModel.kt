package com.groovycow.misionessecundarias.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetActivePausesUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyApplicationsUiState(
    val isLoading: Boolean = false,
    val applications: List<MyApplication> = emptyList(),
    val error: String? = null,
    val cancellingApplicationId: String? = null,
    val actionMessage: String? = null,
    val isActionError: Boolean = false,
    val activePauses: Map<String, Set<PauseKind>> = emptyMap(),
)

class MyApplicationsViewModel(
    private val getMyApplicationsUseCase: GetMyApplicationsUseCase,
    private val cancelApplicationUseCase: CancelApplicationUseCase,
    private val getActivePausesUseCase: GetActivePausesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = MyApplicationsUiState(isLoading = true)

            getMyApplicationsUseCase().fold(
                onSuccess = { applications ->
                    _uiState.value =
                        MyApplicationsUiState(
                            applications = applications,
                            activePauses = fetchPauses(applications),
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        MyApplicationsUiState(
                            error =
                                exception.message
                                    ?: "No fue posible consultar tus postulaciones.",
                        )
                },
            )
        }
    }

    /** Actualiza en silencio las pausas activas de las misiones en las que el usuario fue aceptado. */
    fun refreshPauses() {
        viewModelScope.launch {
            val pauses = fetchPauses(_uiState.value.applications)
            _uiState.value = _uiState.value.copy(activePauses = pauses)
        }
    }

    private suspend fun fetchPauses(applications: List<MyApplication>): Map<String, Set<PauseKind>> {
        val inProgressIds =
            applications
                .filter { it.status == "Aceptado" && it.mission.status == "En progreso" }
                .map { it.missionId }
        return getActivePausesUseCase(inProgressIds).getOrDefault(_uiState.value.activePauses)
    }

    fun cancelApplication(applicationId: String) {
        if (_uiState.value.cancellingApplicationId != null) return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    cancellingApplicationId = applicationId,
                    actionMessage = null,
                    isActionError = false,
                )

            cancelApplicationUseCase(applicationId).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            applications =
                                _uiState.value.applications.filterNot {
                                    it.id == applicationId
                                },
                            cancellingApplicationId = null,
                            actionMessage = "La postulación fue cancelada correctamente.",
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            cancellingApplicationId = null,
                            actionMessage =
                                exception.message
                                    ?: "No fue posible cancelar la postulación.",
                            isActionError = true,
                        )
                },
            )
        }
    }

    fun dismissNotice() {
        _uiState.value =
            _uiState.value.copy(
                error = null,
                actionMessage = null,
                isActionError = false,
            )
    }
}
