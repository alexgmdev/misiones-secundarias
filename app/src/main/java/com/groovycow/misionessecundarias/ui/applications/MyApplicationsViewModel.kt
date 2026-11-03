package com.groovycow.misionessecundarias.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
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
)

class MyApplicationsViewModel(
    private val getMyApplicationsUseCase: GetMyApplicationsUseCase,
    private val cancelApplicationUseCase: CancelApplicationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = MyApplicationsUiState(isLoading = true)

            getMyApplicationsUseCase().fold(
                onSuccess = { applications ->
                    _uiState.value = MyApplicationsUiState(applications = applications)
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
}
