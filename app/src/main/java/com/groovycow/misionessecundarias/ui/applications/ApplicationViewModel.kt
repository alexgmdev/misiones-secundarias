package com.groovycow.misionessecundarias.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CheckMissionOwnershipUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationUiState(
    val isSubmitting: Boolean = false,
    val wasSubmitted: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val isCheckingOwnership: Boolean = true,
    val isPublisher: Boolean = false,
)

class ApplicationViewModel(
    private val applyToMissionUseCase: ApplyToMissionUseCase,
    private val checkMissionOwnershipUseCase: CheckMissionOwnershipUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    fun loadMission(publisherId: String) {
        _uiState.value = ApplicationUiState(isCheckingOwnership = true)
        viewModelScope.launch {
            checkMissionOwnershipUseCase(publisherId).fold(
                onSuccess = { isPublisher ->
                    _uiState.value =
                        _uiState.value.copy(
                            isCheckingOwnership = false,
                            isPublisher = isPublisher,
                        )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isCheckingOwnership = false)
                },
            )
        }
    }

    fun applyToMission(missionId: String) {
        if (_uiState.value.isSubmitting || _uiState.value.wasSubmitted) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, message = null, isError = false)

            applyToMissionUseCase(missionId).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            wasSubmitted = true,
                            message = "Tu postulación fue registrada correctamente.",
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            message = exception.message ?: "No fue posible registrar la postulación.",
                            isError = true,
                        )
                },
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
