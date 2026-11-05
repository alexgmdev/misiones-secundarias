package com.groovycow.misionessecundarias.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationUiState(
    val isSubmitting: Boolean = false,
    val wasSubmitted: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class ApplicationViewModel(
    private val applyToMissionUseCase: ApplyToMissionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    fun applyToMission(missionId: String) {
        if (_uiState.value.isSubmitting || _uiState.value.wasSubmitted) return

        viewModelScope.launch {
            _uiState.value = ApplicationUiState(isSubmitting = true)

            applyToMissionUseCase(missionId).fold(
                onSuccess = {
                    _uiState.value =
                        ApplicationUiState(
                            wasSubmitted = true,
                            message = "Tu postulación fue registrada correctamente.",
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        ApplicationUiState(
                            message = exception.message ?: "No fue posible registrar la postulación.",
                            isError = true,
                        )
                },
            )
        }
    }

    fun reset() {
        _uiState.value = ApplicationUiState()
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
