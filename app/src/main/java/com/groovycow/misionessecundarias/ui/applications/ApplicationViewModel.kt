package com.groovycow.misionessecundarias.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CheckMissionOwnershipUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkArrivedUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkOnWayUseCase
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
    // Nuevos estados para HU-046 y HU-047
    val isAccepted: Boolean = false,
    val isOnWay: Boolean = false,
    val hasArrived: Boolean = false,
    val isUpdatingStatus: Boolean = false,
)

class ApplicationViewModel(
    private val applyToMissionUseCase: ApplyToMissionUseCase,
    private val checkMissionOwnershipUseCase: CheckMissionOwnershipUseCase,
    private val markOnWayUseCase: MarkOnWayUseCase,
    private val markArrivedUseCase: MarkArrivedUseCase,
    private val applicationRepository: ApplicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    fun loadMission(
        publisherId: String,
        missionId: String,
    ) {
        _uiState.value = ApplicationUiState(isCheckingOwnership = true)
        viewModelScope.launch {
            checkMissionOwnershipUseCase(publisherId).fold(
                onSuccess = { isPublisher ->
                    if (isPublisher) {
                        _uiState.value = _uiState.value.copy(isCheckingOwnership = false, isPublisher = true)
                    } else {
                        // Es un usuario normal, consultamos si está aceptado
                        val status = applicationRepository.getMyApplicationStatus(missionId)
                        _uiState.value =
                            _uiState.value.copy(
                                isCheckingOwnership = false,
                                isPublisher = false,
                                isAccepted = status?.status == "Aceptado",
                                isOnWay = status?.isOnWay ?: false,
                                hasArrived = status?.arrivedAt != null,
                                wasSubmitted = status != null,
                            )
                    }
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
                            isSubmitting =
                            false,
                            wasSubmitted = true,
                            message =
                                "Tu postulación fue registrada correctamente.",
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting =
                            false,
                            message =
                                exception.message
                                    ?: "No fue posible registrar la postulación.",
                            isError = true,
                        )
                },
            )
        }
    }

    fun markOnWay(missionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true)
            markOnWayUseCase(missionId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isUpdatingStatus = false, isOnWay = true)
                },
                onFailure = {
                    _uiState.value =
                        _uiState.value.copy(
                            isUpdatingStatus = false,
                            message = "Error al actualizar: ${it.message}",
                            isError = true,
                        )
                },
            )
        }
    }

    fun markArrived(
        missionId: String,
        lat: Double?,
        lon: Double?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true)
            markArrivedUseCase(missionId, lat, lon).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isUpdatingStatus = false, hasArrived = true)
                },
                onFailure = {
                    _uiState.value =
                        _uiState.value.copy(
                            isUpdatingStatus = false,
                            message = "Error al registrar llegada: ${it.message}",
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
